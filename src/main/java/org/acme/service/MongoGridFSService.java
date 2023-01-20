package org.acme.service;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.gridfs.GridFSBucket;
import com.mongodb.client.gridfs.GridFSBuckets;
import com.mongodb.client.gridfs.GridFSFindIterable;
import com.mongodb.client.gridfs.model.GridFSFile;
import com.mongodb.client.gridfs.model.GridFSUploadOptions;
import com.mongodb.client.model.Filters;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.acme.exceptions.InvalidRequestException;
import org.acme.util.MarkableFileInputStream;
import org.apache.commons.codec.digest.DigestUtils;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.io.*;
import java.nio.file.Path;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Service which communicates with a MongoGridFs server to store/retrive files
 *
 * @author tmulle
 */
@ApplicationScoped
public class MongoGridFSService {

    private final static Logger LOG = LoggerFactory.getLogger(MongoGridFSService.class);

    // Communicates with the Grid iteself
    private GridFSBucket gridFSBucket;

//    @ConfigProperty(name = "quarkus.mongodb.database")
    private String databaseName;

//    @ConfigProperty(name = "gridfs.bucketName")
    private String bucketName;

//    @ConfigProperty(name = "gridFSChunkSize", defaultValue = "1048576")
    private Integer chunkSize;

//    @Inject
    private MongoClient client;

    private MongoDatabase database;

    @Inject
    public MongoGridFSService(@ConfigProperty(name = "quarkus.mongodb.database") String databaseName,
                              @ConfigProperty(name = "gridfs.bucketName") String fileBucketName,
                              @ConfigProperty(name = "gridFSChunkSize", defaultValue = "1048576") int fileChunkSize,
                              MongoClient client) {

        database = client.getDatabase(databaseName);
        if (fileBucketName != null && !fileBucketName.isEmpty()) {
            gridFSBucket = GridFSBuckets.create(database, fileBucketName);
        } else {
            gridFSBucket = GridFSBuckets.create(database);
        }

        chunkSize = fileChunkSize;
        this.client = client;
    }

    /**
     * Upload to Grid
     *
     * @param file Path of the file
     * @return ObjectId of new file
     */
    public ObjectId uploadFile(Path file, String fileName, String ticketNumber)  {
        Objects.requireNonNull(file, "Path is required");
        Objects.requireNonNull(fileName, "Filename is required");

        try {
            InputStream inputStream = new MarkableFileInputStream(new FileInputStream(file.toFile()));
            return uploadFile(inputStream, fileName, ticketNumber);
        } catch (FileNotFoundException e) {
            throw new RuntimeException("File not found: " + file.toFile(), e);
        }

    }



    /**
     * Upload file to Mongo GridFS
     *
     * @param inputStream
     * @param fileName
     * @return ObjectId of uploaded file
     */
    public ObjectId uploadFile(InputStream inputStream, String fileName, String ticketNumber) {

        Objects.requireNonNull(inputStream, "InputStream is required");
        Objects.requireNonNull(fileName, "Filename is required");

        try {
            // Generate a hash and store in metadata
            String sha256 = DigestUtils.sha256Hex(inputStream);
            inputStream.mark(0); //reset stream
            inputStream.reset();

            // Check if we already have the hash
            // if so return error
            if (hashExists(sha256)) throw new InvalidRequestException("A documents already exists with hash: " + sha256);

            // We are unique
            // Create the meta
            Document metaData = new Document();
            metaData.append("sha256", sha256);

            // Append ticket number if supplied
            if (ticketNumber != null && !ticketNumber.isEmpty()) {
                metaData.append("ticketNumber", ticketNumber);
            }

            // Create the options
            GridFSUploadOptions options = new GridFSUploadOptions()
                    .chunkSizeBytes(chunkSize)
                    .metadata(metaData);

            // Upload to server
            return gridFSBucket.uploadFromStream(fileName, inputStream, options);
        } catch (Exception e) {
            throw new RuntimeException("Error uploading file", e);
        }
    }

    /**
     * Return a list of FileInfo for each record in the Grid
     *
     * @return List of FileInfo objects or empty
     */
    public List<FileInfo> listAllFiles(Map<String, String> queryParams) {

        // Holds the results
        List<FileInfo> items = new ArrayList<>();

        // Default empty query
        Bson query = Filters.empty();

        int recordLimit = 0;
        int skipRecord = 0;

        // Build up query from the params
        if (queryParams != null || queryParams.isEmpty()) {

            // Holds all the conditions
            List<Bson> filters = new ArrayList<>();

            // Check for ticketNumber
            String ticketNumber = queryParams.get("ticketNumber");
            if (ticketNumber != null && !ticketNumber.isEmpty()) {
                filters.add(Filters.eq("metadata.ticketNumber", ticketNumber));
            }

            String startDate = queryParams.get("startDate");
            String endDate = queryParams.get("endDate");

            // if specifying one, you need the other
            if ((startDate != null && endDate == null) || (startDate == null && endDate != null)) {
                throw new InvalidRequestException("Both startDate and endDate are required");
            }

            if (startDate != null && endDate != null) {

                // Convert to date objects
                SimpleDateFormat sdf = new SimpleDateFormat("MM-dd-yyyy");
                sdf.setLenient(false);
                try {
                    Date start = sdf.parse(startDate);
                    Date end = sdf.parse(endDate);
                    filters.add(Filters.gte("uploadDate", start));
                    filters.add(Filters.lte("uploadDate", end));

                } catch (ParseException e) {
                    throw new InvalidRequestException("Cannot parse either startDate or endDate values");
                }
            }

            // read any limit
            String limitParam = queryParams.get("limit");
            if (limitParam != null && !limitParam.isEmpty()) {
                try {
                    recordLimit = Integer.parseInt(limitParam);
                } catch (NumberFormatException nfe) {
                    throw new InvalidRequestException("limit param must be a whole number");
                }
            }

            // read any skip count
            String skipParam = queryParams.get("skip");
            if (skipParam != null && !skipParam.isEmpty()) {
                try {
                    skipRecord = Integer.parseInt(skipParam);
                } catch (NumberFormatException nfe) {
                    throw new InvalidRequestException("skip param must be a whole number");
                }
            }

            // Build the filter chain
            if (!filters.isEmpty()) {
                query = Filters.and(filters);
            }
        }

        LOG.info("Running query with Filters: {}", query);

        // run the query
        GridFSFindIterable gridFSFiles = gridFSBucket.find(query);

        // Are we limiting the return size
        if (recordLimit > 0) {
            gridFSFiles.limit(recordLimit);
        }

        // Skipping any records?
        if (skipRecord > 0) {
            gridFSFiles.skip(skipRecord);
        }

        gridFSFiles.forEach(file -> {
            FileInfo info = new FileInfo();
            info.fileName = file.getFilename();
            info.size = file.getLength();
            info.uploadDate = file.getUploadDate();
            info.id = file.getObjectId().toString();
            info.metaData = file.getMetadata();
            items.add(info);
        });

        return items;
    }

    /**
     * Delete a file from the Grid
     * @param id
     */
    public void deleteFile(String id) {
        Objects.requireNonNull(id, "Id is required");
        gridFSBucket.delete(new ObjectId(id));
    }

    /**
     * Download the file by ObjectID
     *
     * @param id ObjectId string
     * @param outputStream OutputStream to write the data
     */
    public void downloadFile(String id, OutputStream outputStream) {
        Objects.requireNonNull(id, "Id is required");
        Objects.requireNonNull(outputStream, "OutputStream is required");

        gridFSBucket.downloadToStream(new ObjectId(id), outputStream);
    }

    /**
     * Get file info for a single record
     *
     * @param id ObjectId hash
     * @return Optional with a FileInfo
     */
    public Optional<FileInfo> getFileInfo(String id) {
        Objects.requireNonNull(id, "Id is required");

        Bson bson = Filters.eq("_id", new ObjectId(id));
        GridFSFile file = gridFSBucket.find(bson).first();
        LOG.info("Found file: {}", file);
        FileInfo info = null;
        if (file != null) {
            info = new FileInfo();
            info.fileName = file.getFilename();
            info.size = file.getLength();
            info.uploadDate = file.getUploadDate();
            info.id = file.getObjectId().toString();
            info.metaData = file.getMetadata();
        }

        return Optional.ofNullable(info);
    }

    /**
     * Returns if a hash is already stored in the db
     *
     * @param hash SHA256 hash
     * @return true or false
     */
    public boolean hashExists(String hash) {
        Objects.requireNonNull(hash, "Hash is required");
        long count = database.getCollection("fs.files").countDocuments(Filters.eq("metadata.sha256", hash));
        return count > 0;
    }

    /**
     * Get the total file count
     * @return
     */
    public long totalFileCount() {
        return database.getCollection("fs.files").countDocuments();
    }

    @NoArgsConstructor
    @Getter
    public static class FileInfo {

        String id;

        String fileName;
        Long size;
        Date uploadDate;

        Map<String, Object> metaData;

    }
}
