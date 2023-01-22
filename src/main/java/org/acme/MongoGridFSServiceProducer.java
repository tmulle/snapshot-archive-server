package org.acme;

import com.mongodb.client.MongoClient;
import org.acme.service.MongoGridFSService;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.Produces;

/**
 * Producer of the MongoGrid service in the system
 *
 * @author tmulle
 */
public class MongoGridFSServiceProducer {

    @ConfigProperty(name = "quarkus.mongodb.database") String databaseName;
    @ConfigProperty(name = "gridfs.bucketName") String fileBucketName;
    @ConfigProperty(name = "gridfs.chunkSize") int fileChunkSize;

    @Inject
    MongoClient client;

    @Produces
    @ApplicationScoped
    MongoGridFSService getMongoService() {
        return new MongoGridFSService(databaseName, fileBucketName, fileChunkSize, client);
    }

}
