/**
 * Copyright Estafet Ltd. 2019. All rights reserved.
 */
package lesson03.exercise;

import java.io.IOException;

import com.google.common.collect.ImmutableMap;

import io.opentracing.Scope;
import io.opentracing.Span;
import io.opentracing.Tracer;
import io.opentracing.propagation.Format;
import io.opentracing.tag.Tags;
import lib.Debug;
import lib.Tracing;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Propagate the tracing context to the formatter and publisher microservices.
 *
 * <p>In {@link lesson03.exercise.HelloMicroServicesAppStep1}, the tracing context is not propagated to the new
 * formatter and publisher microservices.</p>
 *
 * <p>This means there is a trace with three spans, all from hello-world service. Now, there are two microservices
 * participating in the transaction, which need to be in the trace. To continue the trace over the process boundaries
 * and RPC calls, the OpenTracing API provides two methods on the {@link io.opentracing.Tracer} interface to do that:</p>
 *
 * <ul>
 * <li>{@link io.opentracing.Tracer#inject(io.opentracing.SpanContext, Format, Object)}, and </li>
 * <li>{@link io.opentracing.Tracer#extract(Format, Object)}.
 * </ul>
 *
 * <p>The {@code format} parameter refers to one of the three standard encodings the OpenTracing API defines:</p>
 *
 * <dl>
 *  <dt>TEXT_MAP</dt>
 *  <dd>The span context is encoded as a collection of string key-value pairs.</dd>
 *  <dt>BINARY</dt>
 *  <dd>The span context is encoded as an opaque byte array.</dd>
 *  <dt>HTTP_HEADERS</dt>
 *  <dd>This is similar to {@code TEXT_MAP}, except that the keys must be legal HTTP header names, i.e. encoded in
 *  accordance with the last paragraph of
 *  <a href="https://tools.ietf.org/html/rfc7230#section-3.2.4">section 3.2.4 of RFC 7230</a>.</dd>
   </dl>
 * <p>This example is based on {@link lesson03.exercise.HelloMicroServicesAppStep1} with the following changes:</p>
 *
 * <ul>
 *   <li>The active tracing context is propagated to the formatter and publisher microservices.</li>
 * </ul>
 *
 * <p>Run the example, for example:</p>
 * <pre>
 *     $ ./run.sh lesson03.exercise.HelloMicroServicesAppStep2 Steve
 * </pre>
 *
 * <p>The output should be like this:</p>
 * <pre>
 * 11:15:13.344 [main] INFO com.uber.jaeger.reporters.LoggingReporter - Span reported: 9e6d94e89ab4dcf6:31e67bd79f091eff:9e6d94e89ab4dcf6:1 - formatString
 * Hello, Steve!
 * 11:15:13.346 [main] INFO com.uber.jaeger.reporters.LoggingReporter - Span reported: 9e6d94e89ab4dcf6:23caeb944bd614a0:9e6d94e89ab4dcf6:1 - formatString
 * 11:15:13.346 [main] INFO com.uber.jaeger.reporters.LoggingReporter - Span reported: 9e6d94e89ab4dcf6:9e6d94e89ab4dcf6:0:1 - say-hello
 * </pre>
 *
 * <p>One hierarchical trace for the {@code hello-world} service should appear in the Jaeger UI at
 * {@code http://${ip_address}:16686}.</p>
 *
 */
public class HelloMicroServicesAppStep2 {

    /**
     * Program entry point.
     * @param args
     *          The program arguments. The only argument is the parameter for the message.
     */
    public static void main(final String[] args) {
        if (args.length != 1) {
            throw new IllegalArgumentException("Expecting one argument");
        }
        final String helloTo = args[0];

        // No need to close a Tracer - when it is built a shutdown hook to close it is added to the Runtime.
        final Tracer tracer = Tracing.init("hello-world");
        new HelloMicroServicesAppStep2(tracer).sayHello(helloTo);
    }

    /**
     * The {@link Tracer} instance.
     */
    private final Tracer tracer;

    /**
     * The HTTP client.
     */
    private final OkHttpClient client;

    /**
     * Create from an instance of {@link Tracer}.
     * @param theTracer
     *          The tracer to use.
     */
    private HelloMicroServicesAppStep2(final Tracer theTracer) {
        tracer = theTracer;
        client = new OkHttpClient();
    }

    /**
     * Make a request to an HTTP server running on {@code localhost}.
     *
     * <p>The HTTP client is <a href="http://square.github.io/okhttp/">OkHttp</a>.</p>
     *
     * @param port
     *          The port to connect to.
     * @param path
     *          The path to connect with.
     * @param queryParameter
     *          The query parameter to send.
     * @param value
     *          The value of the query parameter.
     *
     * @return
     *          The response to the request.
     * @throws RuntimeException
     *          When an error occurs.
     */
    private String getHttp(final int port, final String path, final String queryParameter, final String value) {
        try {

            // Create the URL.
            final HttpUrl url = new HttpUrl.Builder().scheme("http")
                                                     .host("localhost")
                                                     .port(port)
                                                     .addPathSegment(path)
                                                     .addQueryParameter(queryParameter, value)
                                                     .build();
            final Request.Builder requestBuilder = new Request.Builder().url(url);

            final Span activeSpan = tracer.activeSpan();

            if (activeSpan != null) {
                // Propagate the active tracing context to the microservice.

                Tags.SPAN_KIND.set(activeSpan, Tags.SPAN_KIND_CLIENT);
                Tags.HTTP_METHOD.set(activeSpan, "GET");
                Tags.HTTP_URL.set(activeSpan, url.toString());

                tracer.inject(activeSpan.context(),
                              Format.Builtin.HTTP_HEADERS,
                              Tracing.requestBuilderCarrier(requestBuilder));

                Debug.debugSpan("client:getHttp", activeSpan);
            }

            // Create the request.
            final Request request = requestBuilder.build();

            // Send the request.
            final Response response = client.newCall(request).execute();

            Debug.debug(Debug.getOperationName(activeSpan),  "client:getHttp",  "Received response.");

            if (response.code() != 200) {
                throw new RuntimeException("Bad HTTP result: " + response);
            }

            // Get the response,
            return response.body().string();
        }
        catch (final IOException e) {
            throw new RuntimeException("Failed to send request", e);
        }
    }

    /**
     * Wrap the message formatting in a {@link Span}.
     * <p>The {@link Span} is stored in a {@link Scope} object in thread local storage.</p>
     * @param helloTo
     *          The parameter for the message.
     * @return
     *          The formatted message.
     */
    private String formatString(final String helloTo) {
        try (final Scope scope = tracer.buildSpan("formatString").startActive(true)) {
            Debug.debugScope("format", "client:formatString", "Operation start.", scope);

            // Call the format microservice
            final String helloString = getHttp(ServicePorts.FORMATTER_SERVICE_PORT, "format", "helloTo", helloTo);
            scope.span().log(ImmutableMap.of("event", "string-format", "value", helloString));
            Debug.debugScope("format", "client:formatString", "Operation finushed.", scope);
            return helloString;
        }
    }

    /**
     * Wrap the message printing in a {@link Span}.
     * <p>The {@link Span} is stored in a {@link Scope} object in thread local storage.</p>
     * @param helloString
     *          The message to print.
     */
    private void printHello(final String helloString) {
        try (final Scope scope = tracer.buildSpan("printHello").startActive(true)) {
            Debug.debugScope("println", "client:printHello", "Operation start", scope);
            final String result = getHttp(ServicePorts.PUBLISHER_SERVICE_PORT, "publish", "helloStr", helloString);
            scope.span().log(ImmutableMap.of("event", "println", "result", result));
            Debug.debugScope("println", "client:printHello", "Operation finished.", scope);
        }
    }

    /**
     * Log the message.
     *
     * <p>{@link ImmutableMap#of(Object, Object, Object, Object)} takes arguments of key1, value1, ... keyn, valuen.</p>
     *
     * @param helloTo
     *          The parameter for the message.
     */
    private void sayHello(final String helloTo) {

        // Create the active Span in a Scope.
        try (final Scope scope = tracer.buildSpan("say-hello").startActive(true)) {

            // Tag the span with the message parameter.
            scope.span().setTag("hello-to", helloTo);

            Debug.debugScope("sayHello", "client:sayHello", "Operation start.", scope);

            // Create the child span for formatting the message.
            final String helloString = formatString(helloTo);

            // Create the child span for printing the message.
            printHello(helloString);
            Debug.debugScope("sayHello", "client:sayHello", "Operation finished.", scope);
        }
    }
}