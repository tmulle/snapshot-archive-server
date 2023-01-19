package org.acme;


import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.json.Json;
import javax.json.JsonObject;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

@Provider
public class GeneralExceptionHandler implements ExceptionMapper<Exception> {

    private final static Logger LOG = LoggerFactory.getLogger(GeneralExceptionHandler.class);

    @Override
    public Response toResponse(Exception e) {
        Throwable rootCause = ExceptionUtils.getRootCause(e);
        JsonObject error = Json.createObjectBuilder().add("error", rootCause.getMessage()).build();

        LOG.error("General Exception", e);

        return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(error.toString()).build();
    }
}