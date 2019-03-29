package lesson02.exercise;

import com.google.common.collect.ImmutableMap;

import io.opentracing.Scope;
import io.opentracing.Tracer;
import lib.Tracing;

/**
 * This example uses distributed tracing.
 *
 */
public class HelloActive {

    /**
     * The {@link Tracer} for distributed tracing.
     */
    private final Tracer tracer;

    /**
     * Construct from a {@link Tracer}.
     * @param theTracer
     *          The tracer to use.
     */
    private HelloActive(final Tracer theTracer) {
        tracer = theTracer;
    }

    /**
     * Format and display the message.
     * @param helloTo
     *          The recipient of the greeting.
     */
    private void sayHello(final String helloTo) {
        try (Scope scope = tracer.buildSpan("say-hello").startActive(true)) {
            scope.span().setTag("hello-to", helloTo);

            final String helloStr = formatString(helloTo);
            printHello(helloStr);
        }
    }

    /**
     * Format the greeting message.
     * @param helloTo
     *          The recipient of the greeting.
     * @return
     *          The formatted greeting message.
     */
    private  String formatString(final String helloTo) {
        try (Scope scope = tracer.buildSpan("formatString").startActive(true)) {
            final String helloStr = String.format("Hello, %s!", helloTo);
            scope.span().log(ImmutableMap.of("event", "string-format", "value", helloStr));
            return helloStr;
        }
    }

    /**
     * Display the greeting message.
     * @param helloStr
     *          The greeting message to display.
     */
    private void printHello(final String helloStr) {
        try (Scope scope = tracer.buildSpan("printHello").startActive(true)) {
            System.out.println(helloStr);
            scope.span().log(ImmutableMap.of("event", "println"));
        }
    }

    /**
     * The program entry point.
     * @param args
     *          The command line arguments. The only argument is the recipient of the greeting.
     *
     */
    public static void main(final String[] args) {
        if (args.length != 1) {
            throw new IllegalArgumentException("Expecting one argument");
        }

        final String helloTo = args[0];
        final Tracer tracer = Tracing.init("hello-world");
        new HelloActive(tracer).sayHello(helloTo);
    }
}
