/**
 * Copyright Estafet Ltd. 2019. All rights reserved.
 */
package lesson02.exercise;

import com.google.common.collect.ImmutableMap;

import io.jaegertracing.Configuration;
import io.jaegertracing.Configuration.ReporterConfiguration;
import io.jaegertracing.Configuration.SamplerConfiguration;
import io.opentracing.Scope;
import io.opentracing.Span;
import io.opentracing.Tracer;

/**
 * Propagate the tracing context.
 *
 * <p>This example propagates the tracing context.</p>
 *
 * <p>This example is based on {@link lesson02.exercise.HelloHierarchicalSpans}.</p>
 *
 * <p>The {@link lesson02.exercise.HelloHierarchicalSpans} class has the following issues:</p>
 * <ol>
 * <li>The Span object is passed as the first argument to each function.</li>
 * <li>Verbose try/finally code must be used to finish the spans.</li>
 * </ol>
 *
 * The OpenTracing API for Java provides Active spans to avoid passing the span through the code and allow the code to
 * access spans via the {@link #tracer} field.
 *
 * <ul>
 *   <li>Use the {@link io.opentracing.Tracer.SpanBuilder#startActive(boolean)} method  instead of the
 *   {@link io.opentracing.Tracer.SpanBuilder#start()} method. This makes the span "active" by storing it in
 *   thread-local storage.</li>
 * </ul>
 *
 * <p>The boolean parameter to the {@link io.opentracing.Tracer.SpanBuilder#startActive(boolean)} method tells the Scope
 * that once it is closed it should finish the Span it represents.</p>
 * <p>The {@link io.opentracing.Tracer.SpanBuilder#startActive(boolean)} method automatically creates a {@code childOf}
 * reference to the previously active span. This means that code does not have call the
 * {@link io.opentracing.Tracer.SpanBuilder#asChildOf(io.opentracing.SpanContext)} method explicitly.</p>
 *
 * <p>
 * The {@link io.opentracing.Tracer.SpanBuilder#startActive(boolean)} method returns a {@link Scope} object.
 * {@code Scope} contains the current active span. The {@link Scope#span()} method accesses the current span. When the
 * {@code Scope} is closed, the previous {@code Scope} becomes current.</p>
 *
 * <p>{@code Scope} is auto-closable, which allows code to use the {@code try-with-resource} syntax, which makes code
 * simpler and easier to understand.</p>
 *
 * The running requirements are the same as those for {@link lesson02.exercise.HelloHierarchicalSpans}.
 *
 * <p>Run the example, for example:</p>
 * <pre>
 *     $ ./run.sh lesson02.exercise.HelloHierarchicalSpans Steve
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
public class HelloInProcessContext {

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

        final Tracer tracer = initTracer("hello-world");
        new HelloInProcessContext(tracer).sayHello(helloTo);
    }

    /**
     * Initialise the Jaeger tracer.
     * @param service
     *          The name of the service to trace.
     * @return
     *          The tracer implementation (@link com.uber.jaeger.Tracer).
     */
    private static Tracer initTracer(final String service) {

        // The sampler always makes the same decision for all traces. It samples all traces. If the parameter were
        // zero, it would sample no traces.
        final SamplerConfiguration samplerConfiguration = SamplerConfiguration.fromEnv().withType("const")
                                                                                        .withParam(new Integer(1));
        // The reporter configuration species what is reported. In this case,
        final ReporterConfiguration reporterConfiguration = ReporterConfiguration.fromEnv().withLogSpans(Boolean.TRUE);

        // The configuration encapsulates the configuration for sampling and reporting.
        final Configuration configuration = new Configuration(service).withSampler(samplerConfiguration)
                                                                      .withReporter(reporterConfiguration);

        // Create the tracer from the configuration.
        return configuration.getTracer();
    }

    /**
     * The opentracing tracer instance.
     */
    private final io.opentracing.Tracer tracer;

    /**
     * Create from an instance of {@link io.opentracing.Tracer}.
     * @param theTracer
     *          The tracer to use.
     */
    private HelloInProcessContext(final io.opentracing.Tracer theTracer) {
        tracer = theTracer;
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
            final String helloString = String.format("Hello, %s!", helloTo);
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
            System.out.println(helloString);
            scope.span().log(ImmutableMap.of("event", "println"));
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