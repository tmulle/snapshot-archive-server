package org.acme;

import org.acme.service.MongoGridFSService;
import org.bson.types.ObjectId;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
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
import java.util.HashMap;
import java.util.Map;

/**
 * REST endpoint that allows files to be uploaded/downloaded from a MongoDB GridFS backend
 *
 * @author tmulle
 */
@Path("/archive")
@Tag(name = "Snapshot Archive Resource", description = "JAX-RS Resource that handles file operations with MongoDB")
public class SnapshotArchiveResource {


    @Context
    UriInfo uriInfo;

    @Inject
    MongoGridFSService mongoGridFSService;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Retrieves a listing of all the files", description = "Retrieves a listing of all the files using the optional parameters")
    @APIResponses(value = {
            @APIResponse(responseCode = "200",
                    description = "Successully retrieved records",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON)),
            @APIResponse(responseCode = "400",
                    description = "An error with one of the parameters",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON))
    })
    public Response getAll(@Parameter(description = "Help Desk Ticket Number") @QueryParam("ticketNumber") String ticketNumber,
                           @Parameter(description = "Start Date (MM-DD-YYYY)") @QueryParam("startDate") String startDate,
                           @Parameter(description = "End Date (MM-DD-YYYY)") @QueryParam("endDate") String endDate,
                           @Parameter(description = "Limit the number of results") @QueryParam("limit") String recordLimit,
                           @Parameter(description = "Skips the specified number of records") @QueryParam("skip") String skipRecord,
                           @Parameter(description = "Comma separated list of fields to sort on") @QueryParam("sortyBy") String sortFields,
                           @Parameter(description = "Sort direction: ASC or DESC") @QueryParam("sortDir") String sortDir,
                           @Parameter(description = "Name of file") @QueryParam("filename") String filename) {

        // Build up the param map to pass into the service
        Map<String, String> queryParams = new HashMap<>();
        queryParams.put("ticketNumber", ticketNumber);
        queryParams.put("startDate", startDate);
        queryParams.put("endDate", endDate);
        queryParams.put("limit", recordLimit);
        queryParams.put("skip", skipRecord);
        queryParams.put("sortFields", sortFields);
        queryParams.put("sortDir", sortDir);
        queryParams.put("filename", filename);


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
    @APIResponses(value = {
            @APIResponse(responseCode = "200",
                    description = "Successully retrieved record",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON)),
            @APIResponse(responseCode = "400",
                    description = "An error with the format of parameter, or record not found",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON))
    })
    public Response getFileInfo(@Parameter(description = "ID of the File") @PathParam("id") String id) {
        return Response.ok(mongoGridFSService.getFileInfo(id)).build();
    }

    @DELETE
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Deletes a file", description = "Removes the specified file from the DB")
    @APIResponses(value = {
            @APIResponse(responseCode = "200",
                    description = "Successully deleted record",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON)),
            @APIResponse(responseCode = "400",
                    description = "An error with the format of parameter, or record not found",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON))
    })
    public Response deleteById(@Parameter(description = "ID of the File") @PathParam("id") String id) {
        mongoGridFSService.deleteFile(id);
        return Response.ok().build();
    }

    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Operation(summary = "Uploads a file", description = "Uploads a file with the optional ticketNumber stored with it")
    @APIResponses(value = {
            @APIResponse(responseCode = "201",
                    description = "Success file upload and return the ID of the newly created record in the body as well as setting the Location header",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON)),
            @APIResponse(responseCode = "400",
                    description = "An error with the format of parameters or file was already uploaded",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON))
    })
    public Response uploadFile(@RestForm("file") @Schema(implementation = UploadItemSchema.class) FileUpload fileData,
                               @Parameter(description = "Associated Help Desk Ticket Number") @QueryParam("ticketNumber") String ticketNumber) {



        // Store the ticket number
        Map<String, Object> metaData = new HashMap<>();
        if (ticketNumber != null) {
            metaData.put("ticketNumber", ticketNumber);
        }

        // Perform the upload
        ObjectId id = mongoGridFSService.uploadFile(fileData.uploadedFile(), fileData.fileName(), metaData);

        // Build the location header response
        UriBuilder builder = uriInfo.getAbsolutePathBuilder();
        builder.path(id.toString());

        // Build the response JSON for the body
        JsonObject json = Json.createObjectBuilder().add("id", id.toString()).build();

        // Add the response to the body and set the location header
        return Response.created(builder.build()).entity(json.toString()).build();
    }

    @GET
    @Path("/exists/hash/{hash}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Checks for the existence of the hash", description = "Checks for the existence of the hash already in the system")
    @APIResponses(value = {
            @APIResponse(responseCode = "20",
                    description = "True if the hash exists, false otherwise",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON)),
            @APIResponse(responseCode = "400",
                    description = "An error with the format of parameter",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON))
    })
    public Response hashExist(@Parameter(description = "SHA-256 hash") @PathParam("hash") String hash) {
        boolean hashExists = mongoGridFSService.hashExists(hash);
        JsonObject jsonObject = Json.createObjectBuilder().add("exists", hashExists).build();
        return Response.ok(jsonObject.toString()).build();
    }

    @GET
    @Path("/exists/id/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Checks for the existence of the file id", description = "Checks for the existence of the file id in the system")
    @APIResponses(value = {
            @APIResponse(responseCode = "20",
                    description = "True if the id exists, false otherwise",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON)),
            @APIResponse(responseCode = "400",
                    description = "An error with the format of parameter",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON))
    })
    public Response fileIDExists(@Parameter(description = "File ID") @PathParam("id") String id) {
        boolean hashExists = mongoGridFSService.fileIDExists(id);
        JsonObject jsonObject = Json.createObjectBuilder().add("exists", hashExists).build();
        return Response.ok(jsonObject.toString()).build();
    }

    @GET
    @Path("/exists/name/{filename}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Checks for the existence of the file name", description = "Checks for the existence of the filename in the system")
    @APIResponses(value = {
            @APIResponse(responseCode = "20",
                    description = "True if the filename exists, false otherwise",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON)),
            @APIResponse(responseCode = "400",
                    description = "An error with the format of parameter",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON))
    })
    public Response filenameExists(@Parameter(description = "Filename to look for") @PathParam("filename") String filename) {
        boolean hashExists = mongoGridFSService.filenameExists(filename);
        JsonObject jsonObject = Json.createObjectBuilder().add("exists", hashExists).build();
        return Response.ok(jsonObject.toString()).build();
    }

    @GET
    @Path("/download/{id}")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    @Operation(summary = "Downloads a file", description = "Downloads the specified file from the system")
    @APIResponses(value = {
            @APIResponse(responseCode = "200",
                    description = "Downloads the file",
                    content = @Content(mediaType = MediaType.APPLICATION_OCTET_STREAM)),
            @APIResponse(responseCode = "400",
                    description = "An error with the format of parameters or record not found",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON))
    })
    public Response downloadFile(@Parameter(description = "ID of the File") @PathParam("id") String id) {

        // NOT SUPPORTED in RESTEASY REACTIVE!!! UGH!!
        // Had to create new Writer class
        // This allows streaming of the byte data directly
        // to the client without having to store it locally
        // and growing memory
        StreamingOutput stream = output -> mongoGridFSService.downloadFile(id, output);

        // Get the filename so we can set the filename in the download headers
        MongoGridFSService.FileInfo info = mongoGridFSService.getFileInfo(id);
        String fileName = info.getFilename();

        return Response.ok(stream).header("Content-Disposition", "attachment; filename=" + fileName).build();
    }

    /**
     * This is used for OpenAPI only so it can generate a
     * proper schema to be used in the Swagger-UI
     * <p>
     * I think it shouldn't be necessary in Quarkus
     * looks like a bug
     * https://stackoverflow.com/questions/74465077/how-to-view-a-fileupload-response-body-as-an-upload-button-when-seen-through-swa
     */
    @Schema(type = SchemaType.STRING, format = "binary")
    public static class UploadItemSchema {

    }
}