/**
 * Copyright Estafet Ltd. 2019. All rights reserved.
 */
package lesson03.exercise;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;

import com.google.common.collect.ImmutableMap;

import io.opentracing.Scope;
import io.opentracing.Span;
import io.opentracing.Tracer;
import lib.Tracing;

/**
 * The class to format the message.
 *
 */
@Path("/format")
@Produces(MediaType.TEXT_PLAIN)
public class FormatterStep2Resource {

    /**
     * The distributed tracing tracer.
     */
    private final Tracer tracer;

    /**
     * Construct from a distributed tracing tracer
     * @param theTracer
     *          The tracer to use.
     */
    FormatterStep2Resource(final Tracer theTracer) {
        tracer = theTracer;
    }

    /**
     * Create the formatted message.
     * @param helloTo
     *          The parameter for the message.
     * @param httpHeaders
     *          The HTTP headers in the request.
     * @return
     *          The formatted message.
     */
    @GET
    public String format(@QueryParam("helloTo") final String helloTo, @Context final HttpHeaders httpHeaders) {

        //  Start logging on the server in the context of the client span.
        try (final Scope scope = Tracing.startServerSpan(tracer, httpHeaders, "format")) {

            Tracing.debugScope("format", "server:format", "operationStart", scope);

            // Log the successful format operation.
            final String helloString = String.format("Hello, %s!", helloTo);
            final Span span = scope.span();
            span.log(ImmutableMap.of("event", "server-string-format", "value", helloString));

            Tracing.debug("format", "server:format", " operation finished.");
            return helloString;
        }
    }

}
