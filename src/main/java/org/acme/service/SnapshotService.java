package org.acme.service;

import org.acme.model.SnapshotEntity;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.resteasy.reactive.multipart.FileUpload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.transaction.Transactional;
import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class SnapshotService {

    @Inject
    FileService fileService;


    private final static Logger LOG = LoggerFactory.getLogger(SnapshotService.class);

    public List<SnapshotEntity> getAll() {
        return SnapshotEntity.listAll();
    }

    public Optional<SnapshotEntity> findById(Long id) {
        return SnapshotEntity.findByIdOptional(id);
    }

    public Optional<SnapshotEntity> findByHash(String hash) {
        return SnapshotEntity.findByHashOptional(hash);
    }

    /**
     * Remove from filesystem and database
     *
     * @param id
     * @return True if succeeded, false otherwise
     *
     */
    public boolean deleteById(Long id) {
       return true;
    }

    /**
     * Remove from filesystem and database
     *
     * @param hash shah256 hash
     * @return True if succeeded, false otherwise
     *
     */
    public boolean deleteByHash(String hash) {
        return true;
    }

    @Transactional
    public void saveFile(FileUpload fileData) {

        // Process incoming file
        FileService.UploadResult result = fileService.processAndSaveFileData(fileData);

        // Write the results to the database
        SnapshotEntity entity = SnapshotEntity.builder()
                .fileSize(result.getFileSize())
                .fileName(result.getFileName())
                .sha256Hash(result.getSha256Hash())
                .uploadDate(result.getUploadDate())
                .build();

        entity.persist();

        // write information into database


    }
}
