/**
 * Copyright Estafet Ltd. 2019. All rights reserved.
 */
package lesson02.exercise;

import com.google.common.collect.ImmutableMap;

import io.jaegertracing.Configuration;
import io.jaegertracing.Configuration.ReporterConfiguration;
import io.jaegertracing.Configuration.SamplerConfiguration;
import io.opentracing.Span;
import io.opentracing.SpanContext;
import io.opentracing.Tracer;

/**
 * Create Hierarchical spans.
 *
 * <p>This example creates hierarchical spans.</p>
 * <p>{@link lesson02.exercise.HelloMultipleSingeSpans} has a major issue:</p>
 *
 * <p>We got three spans, but there is a problem here. If we search for the spans in the UI each one will represent a
 * standalone trace with a single span. That's not what we wanted!</p>
 *
 * <p>What we really wanted was to establish causal relationship between the two new spans to the root span started in
 * {@link #main(String[])}). We can do that by passing an additional option {@code asChildOf} to the span builder.</p>
 *
 * <p>A trace can be represented as a <a href="https://en.wikipedia.org/wiki/Directed_acyclic_graph">directed acyclic graph<a>
 * (DAG), where nodes are the {@code Span}s and edges are the causal relationships between the spans. The
 * {@code childOf} option is used to create one such edge between the {@code Span} and the root {@code Span}.</p>
 *
 * <p>In the API, the DAG edges are represented by the {@link io.jaegertracing.internal.Reference} type that
 * consists of a {@link SpanContext} and a label (the {@code type} field of {@code io.jaegertracing.internal.Reference}).
 * The {@link SpanContext} object is an immutable thread-safe portion of the {@link Span} that can be used to establish
 * references, or to propagate the span over the wire. The label describes the nature of the relationship.</p>
 *
 * <p>A {@code childOf} relationship means that the root {@code Span} has a logical dependency on the child span before
 * the root {@code Span} can complete its operation.</p>
 *
 * <p>Another standard reference type in OpenTracing is {@code followsFrom}, which means the root {@code Span} is the
 * child span's ancestor in the DAG, but does not depend on the completion of the child span. An  example would be if
 * the child represents a best-efforts, fire-and-forget request to some other service.</p>
 *
 * <p>This example is based on {@link lesson02.exercise.HelloMultipleSingeSpans}, with the following changes:</p>
 *
 * <ul>
 *   <li>The {@link #formatString(Span, String)} and the {@link #printHello(Span, String)} methods create the
 *  {@link Span} using the {@link io.opentracing.Tracer.SpanBuilder#asChildOf(Span)} method to correctly link the new
 *  {@link Span} to its parent {@link Span}.</li>
 * </ul>
 *
 * The running requirements are the same as those for {@link lesson02.exercise.HelloMultipleSingeSpans}.
 *
 * <p>Run the example, for example:</p>
 * <pre>
 *     $ ./run.sh lesson02.exercise.HelloHierarchicalSpans Steve
 * </pre>
 *
 * <p>The output should be like this:</p>
 * <pre>
 * 11:17:19.101 [main] INFO com.uber.jaeger.reporters.LoggingReporter - Span reported: ec539db14ed1963e:bb87bd4b1a1873e1:ec539db14ed1963e:1 - formatString
 * Hello, Steve!
 * 11:17:19.103 [main] INFO com.uber.jaeger.reporters.LoggingReporter - Span reported: ec539db14ed1963e:c5887a07211646d3:ec539db14ed1963e:1 - formatString
 * 11:17:19.103 [main] INFO com.uber.jaeger.reporters.LoggingReporter - Span reported: ec539db14ed1963e:ec539db14ed1963e:0:1 - say-hello
 * </pre>
 *
 * <p>One hierarchical trace for the {@code hello-world} service should appear in the Jaeger UI at
 * {@code http://${ip_address}:16686}.</p>
 *
 */
public class HelloHierarchicalSpans {

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
        new HelloHierarchicalSpans(tracer).sayHello(helloTo);
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
    private HelloHierarchicalSpans(final io.opentracing.Tracer theTracer) {
        tracer = theTracer;
    }

    /**
     * Wrap the message formatting in a {@link Span}.
     * @param rootSpan
     *          The root {@link Span}.
     * @param helloTo
     *          The parameter for the message.
     * @return
     *          The formatted message.
     */
    private String formatString(final Span rootSpan, final String helloTo) {
        final Span span = tracer.buildSpan("formatString").asChildOf(rootSpan).start();
        try {
            final String helloString = String.format("Hello, %s!", helloTo);
            span.log(ImmutableMap.of("event", "string-format", "value", helloString));
            return helloString;
        }
        finally {
            span.finish();
        }
    }

    /**
     * Wrap the message printing in a {@link Span}.
     * @param rootSpan
     *          The root {@link Span}.
     * @param helloString
     *          The message to print.
     */
    private void printHello(final Span rootSpan, final String helloString) {
        final Span span = tracer.buildSpan("printHello").asChildOf(rootSpan).start();
        try {
            System.out.println(helloString);
            span.log(ImmutableMap.of("event", "println"));
        }
        finally {
            span.finish();
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

        // Create the Span.
        final Span rootSpan = tracer.buildSpan("say-hello").start();

        // Tag the span with the message parameter.
        rootSpan.setTag("hello-to", helloTo);

        // Create the child span for formatting the message.
        final String helloString = formatString(rootSpan, helloTo);

        // Create the child span for printing the message.
        printHello(rootSpan, helloString);

        // Close the root span.
        rootSpan.finish();
    }
}