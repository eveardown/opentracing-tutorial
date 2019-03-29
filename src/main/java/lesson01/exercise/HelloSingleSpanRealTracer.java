/**
 * Copyright Estafet Ltd. 2019. All rights reserved.
 */
package lesson01.exercise;

import io.jaegertracing.Configuration;
import io.jaegertracing.Configuration.ReporterConfiguration;
import io.jaegertracing.Configuration.SamplerConfiguration;
import io.opentracing.Span;
import io.opentracing.Tracer;
import io.opentracing.Tracer.SpanBuilder;

/**
 * Use a real {@link io.opentracing.Tracer} implementation.
 *
 * <p>This example uses the <href="http://github.com/uber/jaeger-client-java">JaegerJava Client</a>.</p>
 *
 * <p>This example is based on {@link HelloSingleSpan1}, with the following changes:</p>
 *
 * <ul>
 *   <li>A static helper method, {@link #initTracer(String)}, to initialise the {@link io.opentracing.Tracer} instance.</li>
 *   <li>The tracer name is now the service name, "{@code hello-world}".</li>
 *   <li>The {@link #main(String[])} method uses the {@code initTracer()} method.</li>
 * </ul>
 *
 * The {@code jaegertracing/all-in-one} docker container must be running:
 *
 * <pre>
 *     $ docker --log-level debug run -d --rm \
 *       -p5775:5775/udp \
 *       -p6831:6831/udp \
 *       -p6832:6832/udp \
 *       -p5778:5778 \
 *       -p16686:16686 \
 *       -p14268:14268 \
 *       -p9411:9411 \
 *       jaegertracing/all-in-one:1.7
 *    56b6b53f3233 ...
 *    $
 * </pre>
 *
 * <p><strong>NOTE</strong>: The tracing agent's address needs to be specified to theJaeger Java client., e.g.:</p>
 *
 * <pre>
 *     $ container_id="56b6b53f3233"
 *     $ ip_address=
 *     $ ip_address="$(docker inspect -f '{{range .NetworkSettings.Networks}}{{.IPAddress}}{{end}}' "${container_id}")"
 *     $ export JAEGER_ENDPOINT="http://${ip_address}:14268/api/traces"
 * </pre>
 *
 * <p>Run the example, for example:</p>
 * <pre>
 *     $ ./run.sh lesson01.exercise.HelloSingleSpanRealTracer Steve
 * </pre>
 *
 * <p>The output should look like this:</p>
 * <pre>
 * $ ./run.sh lesson01.exercise.HelloSingleSpanRealTracer Steve
 * 14:03:44.985 [main] DEBUG com.uber.jaeger.Configuration - Using the HTTP Sender to send spans directly to the endpoint.
 * 14:03:45.115 [main] INFO com.uber.jaeger.Configuration - Initialized tracer=Tracer(version=Java-0.26.0, serviceName=hello-world, reporter=CompositeReporter(reporters=[RemoteReporter(queueProcessor=RemoteReporter.QueueProcessor(open=true), sender=HttpSender(), closeEnqueueTimeout=1000), LoggingReporter(logger=Logger[com.uber.jaeger.reporters.LoggingReporter])]), sampler=ConstSampler(decision=true, tags={sampler.type=const, sampler.param=true}), ipv4=2130706433, tags={hostname=localhost.localdomain, jaeger.version=Java-0.26.0, ip=127.0.0.1}, zipkinSharedRpcSpan=false, baggageSetter=com.uber.jaeger.baggage.BaggageSetter@61dc03ce, expandExceptionLogs=false)
 * Hello, Steve!
 * 14:03:45.122 [main] INFO com.uber.jaeger.reporters.LoggingReporter - Span reported: df9b190ef568ed92:df9b190ef568ed92:0:1 - say-hello
 * lesson01.exercise.HelloSingleSpanRealTracer ran OK.
 * $
 *
 * </pre>
 *
 * <p>Traces for the {@code hello-world} service should appear in the Jaeger UI at {@code http://${ip_address}:16686}.</p>
 *
 */
public class HelloSingleSpanRealTracer {

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
        new HelloSingleSpanRealTracer(tracer).sayHello(helloTo);
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
    private HelloSingleSpanRealTracer(final io.opentracing.Tracer theTracer) {
        tracer = theTracer;
    }

    /**
     * Log the message.
     *
     * <p>This will create one trace.</p>
     *
     * @param helloTo
     *          The parameter for the message.
     */
    private void sayHello(final String helloTo) {

        // Create a SpanBuilder for a Span with the given operation name.
        final SpanBuilder spanBuilder = tracer.buildSpan("say-hello");

        Span span = null;

        try {
            // Create the Span.
            span = spanBuilder.start();

            // Log the message to stdout. The message is automatically traced.
            final String helloStr = String.format("Hello, %s!", helloTo);
            System.out.println(helloStr);
        }
        finally {

            // The span has to be closed manually.
            if (span != null) {
                span.finish();
            }
        }
    }
}