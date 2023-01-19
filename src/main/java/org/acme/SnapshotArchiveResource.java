package org.acme;

import org.acme.exceptions.InvalidRequestException;
import org.acme.service.MongoGridFSService;
import org.bson.types.ObjectId;
import org.jboss.resteasy.reactive.RestForm;
import org.jboss.resteasy.reactive.multipart.FileUpload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
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

public class SnapshotArchiveResource {

    private final static Logger LOG = LoggerFactory.getLogger(SnapshotArchiveResource.class);

    @Context
    UriInfo uriInfo;

    @Inject
    MongoGridFSService mongoGridFSService;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAll(@QueryParam("ticketNo") String ticket) {
        MultivaluedMap<String, String> queryParameters = uriInfo.getQueryParameters(true);
        System.out.println("PARAMS: " + queryParameters);

        Map<String, String> queryParams = new HashMap<>();

        queryParams.put("ticketNumber", queryParameters.getFirst("ticketNumber"));
        queryParams.put("startDate", queryParameters.getFirst("startDate"));
        queryParams.put("endDate", queryParameters.getFirst("endDate"));


        return Response.ok(mongoGridFSService.listAllFiles(queryParams)).build();
    }


    @GET
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getFileInfo(@PathParam("id") String id) {
        return mongoGridFSService.getFileInfo(id)
                .map(info -> Response.ok(info).build())
                .orElseThrow(() -> new InvalidRequestException("ID " + id + " not found"));
    }

    @DELETE
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteById(@PathParam("id") String id) {
        mongoGridFSService.deleteFile(id);
        return Response.ok().build();
    }

    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public Response uploadFile(@RestForm("file") FileUpload fileData, @QueryParam("ticketNumber") String ticketNumber) {
        ObjectId id = mongoGridFSService.uploadFile(fileData.uploadedFile(), fileData.fileName(), ticketNumber);
        UriBuilder builder = uriInfo.getAbsolutePathBuilder();
        builder.path(id.toString());
        return Response.created(builder.build()).entity(id.toString()).build();
    }

    @GET
    @Path("/exists/{hash}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response hashExist(@PathParam("hash") String hash) {
        return Response.ok(mongoGridFSService.hashExists(hash)).build();
    }

    @GET
    @Path("/download/{id}")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public Response downloadFile(@PathParam("id") String id) {

        // NOT SUPPORTED in RESTEASY REACTIVE!!! UGH!!
        // Had to create new Writer class
        StreamingOutput stream = new StreamingOutput() {
            @Override
            public void write(OutputStream output) throws WebApplicationException {
                mongoGridFSService.downloadFile(id, output);
            }
        };

        // Get the filename so we can set the filename in the download headers
        Optional<MongoGridFSService.FileInfo> info = mongoGridFSService.getFileInfo(id);
        String fileName = info.map(MongoGridFSService.FileInfo::getFileName).orElse("download.bin");

        return Response.ok(stream).header("Content-Disposition", "attachment; filename=" + fileName).build();
    }
}