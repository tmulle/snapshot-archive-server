package org.acme.service;

import lombok.*;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.resteasy.reactive.multipart.FileUpload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Date;

@ApplicationScoped
public class FileService {
    private final static Logger LOG = LoggerFactory.getLogger(FileService.class);

    @ConfigProperty(name = "upload.file.location")
    String uploadLocation;


    public UploadResult processAndSaveFileData(FileUpload fileData) {

        LOG.info("Incoming File");
        LOG.info("File Location: {}", fileData.uploadedFile());
        LOG.info("File Name: {}", fileData.fileName());
        LOG.info("File Size: {}", fileData.size());

        // save to desired file location
        try {
            File dest = new File(uploadLocation + '/' + fileData.fileName());

            InputStream inputStream = Files.newInputStream(fileData.uploadedFile());
            FileUtils.copyInputStreamToFile(inputStream, dest);

            inputStream = Files.newInputStream(dest.toPath());
            String sha256Hex = DigestUtils.sha256Hex(inputStream);
            LOG.info("SHA-256: {}", sha256Hex);

            return UploadResult.builder().fileName(fileData.fileName()).fileSize(fileData.size()).sha256Hash(sha256Hex).uploadDate(new Date()).build();

        } catch (Exception e)  {
            throw new RuntimeException(e);
        }
    }

    @Builder
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @ToString
    static class UploadResult {
        String fileName;
        Long fileSize;
        String sha256Hash;
        Date uploadDate;
    }
}
