package org.acme;

import org.acme.exceptions.InvalidRequestException;
import org.acme.service.MongoGridFSService;
import org.bson.types.ObjectId;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.resteasy.reactive.RestForm;
import org.jboss.resteasy.reactive.multipart.FileUpload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonObject;
import javax.ws.rs.*;
import javax.ws.rs.core.*;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 *
 */
@Path("/archive")
@Tag(name = "Snapshot Archive Resource", description = "JAX-RS Resource that handles file operations with MongoDB")
public class SnapshotArchiveResource {

    private final static Logger LOG = LoggerFactory.getLogger(SnapshotArchiveResource.class);

    @Context
    UriInfo uriInfo;

    @Inject
    MongoGridFSService mongoGridFSService;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Retrieves a listing of all the files", description = "Retrieves a listing of all the files using the optional parameters")
    public Response getAll(@Parameter(description = "Help Desk Ticket Number") @QueryParam("ticketNumber") String ticketNumber,
                           @Parameter(description = "Start Date (MM-DD-YYYY)") @QueryParam("startDate") String startDate,
                           @Parameter(description = "End Date (MM-DD-YYYY)") @QueryParam("endDate") String endDate,
                           @Parameter(description = "Limit the number of results") @QueryParam("limit") String recordLimit,
                           @Parameter(description = "Skips the specified records") @QueryParam("skip") String skipRecord) {

        Map<String, String> queryParams = new HashMap<>();
        queryParams.put("ticketNumber", ticketNumber);
        queryParams.put("startDate", startDate);
        queryParams.put("endDate", endDate);
        queryParams.put("limit", recordLimit);
        queryParams.put("skip", skipRecord);


        return Response.ok(mongoGridFSService.listAllFiles(queryParams)).build();
    }


    @GET
    @Path("/count")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Retrieves total count of all the files", description = "Retrieves a total count of all the files stored in the system")
    public Response getTotalCount() {
        long count = mongoGridFSService.totalFileCount();
        JsonObject json = Json.createObjectBuilder().add("totalRecords", count).build();
        return Response.ok(json.toString()).build();
    }

    @GET
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Retrieves info for a single file", description = "Retrieves a JSON info for the specified file")
    public Response getFileInfo(@Parameter(description = "ID of the File") @PathParam("id") String id) {
        return mongoGridFSService.getFileInfo(id)
                .map(info -> Response.ok(info).build())
                .orElseThrow(() -> new InvalidRequestException("ID " + id + " not found"));
    }

    @DELETE
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Deletes a file", description = "Removes the specified file from the DB")
    public Response deleteById(@Parameter(description = "ID of the File") @PathParam("id") String id) {
        mongoGridFSService.deleteFile(id);
        return Response.ok().build();
    }

    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Operation(summary = "Uploads a file", description = "Uploads a file with the optional ticketNumber stored with it")
    public Response uploadFile(@RestForm("file") FileUpload fileData, @QueryParam("ticketNumber") String ticketNumber) {
        ObjectId id = mongoGridFSService.uploadFile(fileData.uploadedFile(), fileData.fileName(), ticketNumber);
        UriBuilder builder = uriInfo.getAbsolutePathBuilder();
        builder.path(id.toString());
        JsonObject json = Json.createObjectBuilder().add("id", id.toString()).build();
        return Response.created(builder.build()).entity(json.toString()).build();
    }

    @GET
    @Path("/exists/{hash}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Checks for the existence of the hash", description = "Checks for the existence of the hash already in the system")
    public Response hashExist(@Parameter(description = "SHA-256 hash") @PathParam("hash") String hash) {
        return Response.ok(mongoGridFSService.hashExists(hash)).build();
    }

    @GET
    @Path("/download/{id}")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    @Operation(summary = "Downloads a file", description = "Downloads the specified file from the system")
    public Response downloadFile(@Parameter(description = "ID of the File") @PathParam("id") String id) {

        // NOT SUPPORTED in RESTEASY REACTIVE!!! UGH!!
        // Had to create new Writer class
        // This allows streaming of the byte data directly
        // to the client without having to store it locally
        // and growing memory
        StreamingOutput stream = output -> mongoGridFSService.downloadFile(id, output);

        // Get the filename so we can set the filename in the download headers
        Optional<MongoGridFSService.FileInfo> info = mongoGridFSService.getFileInfo(id);
        String fileName = info.map(MongoGridFSService.FileInfo::getFileName).orElse("download.bin");

        return Response.ok(stream).header("Content-Disposition", "attachment; filename=" + fileName).build();
    }
}