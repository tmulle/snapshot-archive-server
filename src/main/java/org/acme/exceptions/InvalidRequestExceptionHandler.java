package org.acme.exceptions;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.json.Json;
import javax.json.JsonObject;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

@Provider
public class InvalidRequestExceptionHandler implements ExceptionMapper<InvalidRequestException> {

    private final static Logger LOG = LoggerFactory.getLogger(InvalidRequestExceptionHandler.class);

    @Override
    public Response toResponse(InvalidRequestException e) {

        JsonObject error = Json.createObjectBuilder().add("error", e.getMessage()).build();
        LOG.error("Invalid Request Exception", e);

        return Response.status(Response.Status.BAD_REQUEST)
                .entity(error.toString()).build();
    }
}