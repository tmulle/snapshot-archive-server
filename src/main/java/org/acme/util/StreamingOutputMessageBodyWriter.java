package org.acme.util;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.StreamingOutput;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

/**
 * Provides the StreamingOutput support for using RestEasyReactive
 *
 * Link: https://github.com/quarkusio/quarkus/issues/28385
 */
@Provider
public class StreamingOutputMessageBodyWriter implements MessageBodyWriter<StreamingOutput> {

    @Override
    public boolean isWriteable(Class<?> aClass, Type type, Annotation[] annotations, MediaType mediaType) {
        return StreamingOutput.class.isAssignableFrom(aClass);
    }

    @Override
    public void writeTo(StreamingOutput blob, Class<?> aClass, Type type, Annotation[] annotations, MediaType mediaType, MultivaluedMap<String, Object> httpHeaders, OutputStream outputStream) throws IOException, WebApplicationException {
        blob.write(outputStream);
    }
}