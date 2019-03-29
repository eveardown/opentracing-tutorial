/**
 * Copyright Estafet Ltd. 2019. All rights reserved.
 */
package lesson01.exercise;

import com.google.common.collect.ImmutableMap;

import io.jaegertracing.Configuration;
import io.jaegertracing.Configuration.ReporterConfiguration;
import io.jaegertracing.Configuration.SamplerConfiguration;
import io.jaegertracing.internal.samplers.ConstSampler;
import io.opentracing.Span;
import io.opentracing.Tracer;
import io.opentracing.Tracer.SpanBuilder;

/**
 * Use tags and logs to differentiate traces.
 *
 * <p>This example implements tags and logs to differentiate between traces.</p>
 *
 * <p>A tag is a key-value pair that provides metadata about the span. A log contains a timestamp and some data, and is
 * associated with span from which it was logged.</p>
 *
 * <p>Tags are intended to describe attributes of the span that apply to the whole duration of the span.
 * For example, if a span represents an HTTP request, then the request URL is a good choice for a tag because it is
 * relevant at all points in time during the span. If the server were to respond with a redirect URL, the redirect URL
 * should be logged because it occurs at a specific point during the span. The OpenTracing Specification provides
 * <a href="https://github.com/opentracing/specification/blob/master/semantic_conventions.md">Semantic Conventions</a>
 * for recommended tags and log fields.</p>
 *
 * <p>This example is based on {@link HelloSingleSpanRealTracer}, with the following changes:</p>
 *
 * <ul>
 *   <li>The {@link #sayHello(String)} method uses tags and logs to generates one trace with a tag and two logs:</li>
 * </ul>
 * <pre>
 * say-hello                                                       Service: hello-world Duration:13.99ms Start Time: 0ms
 *     Tags:
 *          sampler.type=const
 *          hello-to=Steve
 *          sampler.param=true
 *     Process: hostname=localhost.localdomain ip=127.0.0.1 jaeger.version=Java-0.26.0
 *     Logs (2)
 *       12ms: event=string-format value=Hello, Steve!
 *       14ms: event=println
 * </pre>
 *
 * The running requirements are the same as those for {@link HelloSingleSpanRealTracer}.
 *
 * <p>Run the example, for example:</p>
 * <pre>
 *     $ ./run.sh lesson01.exercise.HelloSingleSpanTagAndLog Steve
 * </pre>
 *
 * <p>Traces for the {@code hello-world} service should appear in the Jaeger UI at {@code http://${ip_address}:16686}.</p>
 *
 */
public class HelloSingleSpanTagAndLog {

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
        new HelloSingleSpanTagAndLog(tracer).sayHello(helloTo);
    }

    /**
     * Initialise the Jaeger tracer.
     * @param service
     *          The name of the service to trace.
     * @return
     *          The tracer implementation (@link io.jaegertracing.Tracer).
     */
    private static Tracer initTracer(final String service) {

        // The sampler always makes the same decision for all traces. It samples all traces. If the parameter were
        // zero, it would sample no traces.
        final SamplerConfiguration samplerConfiguration = SamplerConfiguration.fromEnv().withType(ConstSampler.TYPE)
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
    private HelloSingleSpanTagAndLog(final io.opentracing.Tracer theTracer) {
        tracer = theTracer;
    }

    /**
     * Log the message.
     *
     * <p>{@link ImmutableMap#of(Object, Object, Object, Object)} takes arguments of key1, value1, ... keyn, valuen.</p>
     *
     * <p>This method uses tagging and logging to generate a trace like this:</p>
     *
     * <pre>
     * say-hello                                                   Service: hello-world Duration:13.99ms Start Time: 0ms
     *     Tags:
     *          sampler.type=const
     *          hello-to=Steve
     *          sampler.param=true
     *     Process: hostname=localhost.localdomain ip=127.0.0.1 jaeger.version=Java-0.26.0
     *     Logs (2)
     *       12ms: event=string-format value=Hello, Steve!
     *       14ms: event=println
     * </pre>
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

            // Tag the span with the message parameter.
            span.setTag("hello-to", helloTo);

            // Log the formatting of the message.
            final String helloString = String.format("Hello, %s!", helloTo);
            span.log(ImmutableMap.of("event", "string-format", "value", helloString));

            // Log the printing of the message. The message is automatically traced.
            System.out.println(helloString);
            span.log(ImmutableMap.of("event", "println"));
        }
        finally {

            // The span has to be closed manually.
            if (span != null) {
                span.finish();
            }
        }
    }
}