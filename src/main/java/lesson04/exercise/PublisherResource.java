/**
 * Copyright Estafet Ltd. 2019. All rights reserved.
 */
package lesson04.exercise;

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
import lib.Debug;
import lib.Tracing;

/**
 * The class to publish the message.
 *
 */
@Path("/publish")
@Produces(MediaType.TEXT_PLAIN)
public class PublisherResource {

    /**
     * The distributed tracing tracer.
     */
    final Tracer tracer;

    /**
     * Construct from a distributed tracing tracer
     * @param theTracer
     *          The tracer to use.
     */
    PublisherResource(final Tracer theTracer) {
        tracer = theTracer;
    }

    /**
     * @param helloString
     *          The message to print.
     * @param httpHeaders
     *          The HTTP headers in the request.
     * @return
     *          The string "@code{published}".
     */
    @GET
    public String publish(@QueryParam("helloStr") final String helloString, @Context final HttpHeaders httpHeaders) {

        //  Start logging on the server in the context of the client span.
        try (Scope scope = Tracing.startServerSpan(tracer, httpHeaders, "publish")) {

            Debug.debugScope("publish", "server:publish", "Operation start.", scope);

            System.out.println(helloString);
            // Log the successful publish operation.
            final Span span = scope.span();
            span.log(ImmutableMap.of("event", "server-println", "value", helloString));

            Debug.debug("publish", "server:publish", "Operation finished.");
            return "published";
        }
    }

}
