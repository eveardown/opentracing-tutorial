/**
 * Copyright Estafet Ltd. 2019. All rights reserved.
 */
package lesson01.exercise;

import io.opentracing.Span;
import io.opentracing.Tracer;
import io.opentracing.Tracer.SpanBuilder;
import io.opentracing.util.GlobalTracer;

/**
 * Create a trace that consists of only one span.
 *
 * <p>Tracing needs an instance of {@link io.opentracing.Tracer}. This example uses the global {@code Tracer} instance
 * returned by ${link io.opentracing.util.GlobalTracer#get()}.
 *
 * <p>This example uses the following basic features of the OpenTracing API:</p>
 * <ul>
 *   <li>The ${link io.opentracing.util.GlobalTracer#get()} is used to create a {@link SpanBuilder}.</li>
 *   <li>The {@code io.opentracing.SpanBuilder} creates a @{link Span}. The {@link Span} is not registered with the
 * {@link io.opentracing.ScopeManager}.</li>
 *   <li>The span is given an operation name, "{@code say-hello}".</li>
 *   <li>The span must be finished by calling the {@link Span#finish()} method.</li>
 *   <li>The start and end time stamps of the span are captured automatically by the tracer implementation.</li>
 * </ul>
 *
 * <p>No traces are generated because {@link io.opentracing.util.GlobalTracer#get()} returns a no-op tracer by
 * default.</p>
 *
 */
public class HelloSingleSpan1 {

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

        // Get the global tracer implementation.
        //
        // The GlobalTracer.get() method returns a no-op tracer by default, so no/ traces will appear in the tracing UI.
        final Tracer globalTracer = GlobalTracer.get();

        // Log the message.
        new HelloSingleSpan1(globalTracer).sayHello(helloTo);
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
    private HelloSingleSpan1(final io.opentracing.Tracer theTracer) {
        tracer = theTracer;
    }

    /**
     * Log the message
     * @param helloTo
     *          The parameter for the message.
     */
    private void sayHello(final String helloTo) {

        // Create a SpanBuilder for a Span with the given operation name.
        final SpanBuilder spanBuilder = tracer.buildSpan("say-hello");

        Span span = null;

        try {
            // Create the Span. The Span is NOT
            span = spanBuilder.start();

            // Log the message to stdout.
            final String helloStr = String.format("Hello, %s!", "fred");
            System.out.println(helloStr);
        }
        finally {

            // The span has to be closed manually.
            if (span != null ) {
                span.finish();
            }
        }
    }
}