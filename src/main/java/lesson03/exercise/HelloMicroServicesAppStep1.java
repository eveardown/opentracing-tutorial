/**
 * Copyright Estafet Ltd. 2019. All rights reserved.
 */
package lesson03.exercise;

import java.io.IOException;

import com.google.common.collect.ImmutableMap;

import io.jaegertracing.internal.JaegerTracer;
import io.opentracing.Scope;
import io.opentracing.Span;
import lib.Tracing;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Call microservices from the {@link #formatString(String)} and {@link #printHello(String)} methods.
 *
 * <p>This example calls microservices to format the message and print the message.</p>
 *
 * <p>This example is based on {@link lesson02.exercise.HelloInProcessContext} with the following changes:</p>
 *
 * <ul>
 *   <li>Formatting the message and printing the message are delegated to microservices,</li>
 * </ul>
 *
 * The running requirements are:
 *
 * <ol>
 *  <li>Start the formatter service in a bash shell as per the instructions in {@link FormatterStep1} .</li>
 *  <li>Start the publisher service in a bash shell as per the instructions in {@link PublisherStep1} .</li>
 * </ol>
 *
 * <p>Run the example, for example:</p>
 * <pre>
 *     $ ./run.sh lesson03.exercise Steve
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
 * @see FormatterStep1
 * @see PublisherStep1
 */
public class HelloMicroServicesAppStep1 {

    /**
     * The port that the formatter microservice is listening on.
     */
    private static int FORMATTER_PORT = 10081;

    /**
     * The port that the publisher microservice is listening on.
     */
    private static int PUBLISHER_PORT = 10082;

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

        try (final JaegerTracer tracer = Tracing.init("hello-world")) {
            new HelloMicroServicesAppStep1(tracer).sayHello(helloTo);
        }
    }

    /**
     * The opentracing tracer instance.
     */
    private final io.opentracing.Tracer tracer;

    /**
     * The HTTP client.
     */
    private final OkHttpClient client;

    /**
     * Create from an instance of {@link io.opentracing.Tracer}.
     * @param theTracer
     *          The tracer to use.
     */
    private HelloMicroServicesAppStep1(final io.opentracing.Tracer theTracer) {
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
            // Create the request.
            final Request.Builder requestBuilder = new Request.Builder().url(url);
            final Request request = requestBuilder.build();

            // Send the request.
            final Response response = client.newCall(request).execute();

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

            // Call the format microservice
            final String helloString = getHttp(FORMATTER_PORT, "format", "helloTo", helloTo);
            scope.span().log(ImmutableMap.of("event", "string-format", "value", helloString));
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
            final String result = getHttp(PUBLISHER_PORT, "publish", "helloStr", helloString);
            scope.span().log(ImmutableMap.of("event", "println", "result", result));
        }
    }

    /**
     * Log the message.
     *
     * <p>{@link ImmutableMap#of(Object, Object, Object, Object)} takes arguments of key1, value1, ... keyn, valuen.</p>
     *
     * <p>This method generates one trace containing three spans like this:</p>
     *
     * <pre>
     * 1 Trace
     *
     * hello-world: say-hello d7fefc0                                                                            11.54ms
     * 3 Spans hello-world (3)                                                                         Today 10:16:40 am
     *                                                                                                     6 minutes ago
     * </pre>
     * @param helloTo
     *          The parameter for the message.
     */
    private void sayHello(final String helloTo) {

        // Create the active Span in a Scope.
        try (final Scope scope = tracer.buildSpan("say-hello").startActive(true)) {

            // Tag the span with the message parameter.
            scope.span().setTag("hello-to", helloTo);

            // Create the child span for formatting the message.
            final String helloString = formatString(helloTo);

            // Create the child span for printing the message.
            printHello(helloString);
        }
    }
}