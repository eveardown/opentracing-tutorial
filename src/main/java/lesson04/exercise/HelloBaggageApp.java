/**
 * Copyright Estafet Ltd. 2019. All rights reserved.
 */
package lesson04.exercise;

import java.io.IOException;

import com.google.common.collect.ImmutableMap;

import io.opentracing.Scope;
import io.opentracing.Span;
import io.opentracing.Tracer;
import io.opentracing.propagation.Format;
import io.opentracing.tag.Tags;
import lib.Debug;
import lib.ServicePorts;
import lib.Tracing;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Propagate the tracing context and baggage to the formatter and publisher microservices.
 *
 * <p>The {@link lesson03.exercise.HelloMicroServicesAppStep2} application propagates only the span context to
 * the microservices.</p>
 *
 * <p>Propagating the span context can be generalised to propagate more than the span context. OpenTracing
 * instrumentation, supports general purpose distributed context propagation where some metadata is associated with the
 * transaction and made available anywhere in the distributed call graph. In OpenTracing, this metadata is called
 * "baggage", to highlight the fact that it is carried by all RPC requests, just like baggage.</p>
 *
 * <p>This example is based on {@link lesson03.exercise.HelloMicroServicesAppStep2} and will use baggage to customise
 * the greeting <em>without</em> changing the public API of the {@link Formatter} service.</p>
 *
 * <p>Changing the public API of the {@code Formatter} service would implement the same functionality. However, that is
 * exactly the point of this exercise - no changes are needed to any APIs on the path from the root span in
 * {@link HelloBaggageApp} to the server-side span in the {@link Formatter} service, three levels down.</p>
 *
 * <p>If this were a much larger application with a much deeper call tree, say 10 levels (i.e. eight services between
 * this class and the {@code Formatter} service), using baggage would require
 * <em>no</em> changes to any of the eight services in the path. If changing the API were the only way to pass the data,
 * all eight services in the path would need to be modified.</p>
 * <p>Some of the possible applications of baggage include:</p>
 *
 * <ul>
 * <li>Passing the tenancy in multi-tenant systems.</li>
 * <li>Passing identity of the top caller.</li>
 * <li>Passing fault injection instructions for chaos engineering.</li>
 * <li>Passing request-scoped dimensions for other monitoring data, such as separating metrics for production and test
 * traffic.</li>
 * </ul>
 *
 * <p>
 * Although baggage is a very powerful mechanism, it is also dangerous. Baggage is passed over the wire for
 * <em>every</em> call in a path, so it must be used with caution. Jaeger client libraries implement centrally
 * controlled baggage restrictions so that only blessed services can put blessed keys in the baggage, and allows
 * restrictions on the value length.</p>
 */
public class HelloBaggageApp {

    /**
     * Program entry point.
     * @param args
     *          The program arguments. The only argument is the parameter for the message.
     */
    public static void main(final String[] args) {
        if (args.length != 2) {
            throw new IllegalArgumentException("Expecting two arguments.");
        }
        final String helloTo = args[0];
        final String greeting = args[1];

        // No need to close a Tracer - when it is built, a shutdown hook to close it is added to the Runtime.
        final Tracer tracer = Tracing.init("hello-world");
        new HelloBaggageApp(tracer).sayHello(helloTo, greeting);
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
    private HelloBaggageApp(final Tracer theTracer) {
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
     * @param greeting
     *          The custom greeting.
     */
    private void sayHello(final String helloTo, final String greeting) {

        // Create the active Span in a Scope.
        try (final Scope scope = tracer.buildSpan("say-hello").startActive(true)) {

            // Tag the span with the message parameter.
            final Span activeSpan = scope.span();
            activeSpan.setTag("hello-to", helloTo);
            activeSpan.setBaggageItem("greeting", greeting);

            Debug.debugScope("sayHello", "client:sayHello", "Operation start.", scope);

            // Create the child span for formatting the message.
            final String helloString = formatString(helloTo);

            // Create the child span for printing the message.
            printHello(helloString);
            Debug.debugScope("sayHello", "client:sayHello", "Operation finished.", scope);
        }
    }
}