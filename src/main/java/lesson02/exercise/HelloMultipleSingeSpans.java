/**
 * Copyright Estafet Ltd. 2019. All rights reserved.
 */
package lesson02.exercise;

import com.google.common.collect.ImmutableMap;

import io.jaegertracing.Configuration;
import io.jaegertracing.Configuration.ReporterConfiguration;
import io.jaegertracing.Configuration.SamplerConfiguration;
import io.opentracing.Span;
import io.opentracing.Tracer;

/**
 * Create multiple spans.
 *
 * <p>This example creates multiple spans.</p>
 *
 * <p>This example is based on {@code lesson01.exercise.HelloSingleSpanRealTracer}, with the following changes:</p>
 *
 * <ul>
 *   <li>The {@link #formatString(Span, String)} method wraps the {@link String#format(String, Object[])} call that
 * creates the message.
 *   <li>The {@link #printHello(Span, String)} method wraps the {@code System.out.println()} call that prints the log
 *   message.</li>
 * </ul>
 *
 * The running requirements are the same as those for {@code lesson01.exercise.HelloSingleSpanRealTracer}.
 *
 * <p>Run the example, for example:</p>
 * <pre>
 *     $ ./run.sh lesson02.exercise.HelloMultipleSingeSpans Steve
 * </pre>
 *
 * <p>The output should be like this:</p>
 * <pre>
 * 16:11:59.975 [main] DEBUG com.uber.jaeger.Configuration - Using the HTTP Sender to send spans directly to the endpoint.
 * 16:12:00.104 [main] INFO com.uber.jaeger.Configuration - Initialized tracer=Tracer(version=Java-0.26.0, serviceName=hello-world, reporter=CompositeReporter(reporters=[RemoteReporter(queueProcessor=RemoteReporter.QueueProcessor(open=true), sender=HttpSender(), closeEnqueueTimeout=1000), LoggingReporter(logger=Logger[com.uber.jaeger.reporters.LoggingReporter])]), sampler=ConstSampler(decision=true, tags={sampler.type=const, sampler.param=true}), ipv4=2130706433, tags={hostname=localhost.localdomain, jaeger.version=Java-0.26.0, ip=127.0.0.1}, zipkinSharedRpcSpan=false, baggageSetter=com.uber.jaeger.baggage.BaggageSetter@61dc03ce, expandExceptionLogs=false)
 * 16:12:00.118 [main] INFO com.uber.jaeger.reporters.LoggingReporter - Span reported: 21befbd1278bb56d:21befbd1278bb56d:0:1 - formatString
 * Hello, Steve!
 * 16:12:00.119 [main] INFO com.uber.jaeger.reporters.LoggingReporter - Span reported: 83af8617113b7048:83af8617113b7048:0:1 - printHello
 * 16:12:00.119 [main] INFO com.uber.jaeger.reporters.LoggingReporter - Span reported: 3f96f2baa5eac08c:3f96f2baa5eac08c:0:1 - say-hello
 * </pre>
 *
 * <p>Three separate traces for the {@code hello-world} service should appear in the Jaeger UI at
 * {@code http://${ip_address}:16686}.</p>
 *
 */
public class HelloMultipleSingeSpans {

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
        new HelloMultipleSingeSpans(tracer).sayHello(helloTo);
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
    private final Tracer tracer;

    /**
     * Create from an instance of {@link io.opentracing.Tracer}.
     * @param theTracer
     *          The tracer to use.
     */
    private HelloMultipleSingeSpans(final Tracer theTracer) {
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
        final Span span = tracer.buildSpan("formatString").start();
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
        final Span span = tracer.buildSpan("printHello").start();
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
     * <p>This method generates three separate traces like this:</p>
     *
     * <pre>
     * 3 Traces
     *
     * hello-world: printHello 83af861                                                                            0.79ms
     * 1 Span
     * hello-world (1)                                                                                  Today 4:12:00 pm
     *                                                                                                     4 minutes ago
     *
     * hello-world: formatString 21befbd                                                                          7.46ms
     * 1 Span
     * hello-world (1)                                                                                  Today 4:12:00 pm
     *                                                                                                     4 minutes ago
     *
     * hello-world: say-hello 3f96f2b                                                                            11.57ms
     * 1 Span
     * hello-world (1)                                                                                  Today 4:12:00 pm
     *                                                                                                     4 minutes ago
     * </pre>
     * @param helloTo
     *          The parameter for the message.
     */
    private void sayHello(final String helloTo) {

        // Create the Span.
        final Span span = tracer.buildSpan("say-hello").start();

        // Tag the span with the message parameter.
        span.setTag("hello-to", helloTo);

        // Create the Span for formatting the message.
        final String helloString = formatString(span, helloTo);

        // Create the span for printing the message.
        printHello(span, helloString);

        // Close the root span.
        span.finish();
    }
}