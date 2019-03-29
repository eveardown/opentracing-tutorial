package lesson02.exercise;

import com.google.common.collect.ImmutableMap;

import io.opentracing.Scope;
import io.opentracing.Tracer;
import lib.Tracing;

public class HelloActive {

    private final Tracer tracer;

    private HelloActive(final Tracer tracer) {
        this.tracer = tracer;
    }

    private void sayHello(final String helloTo) {
        try (Scope scope = tracer.buildSpan("say-hello").startActive(true)) {
            scope.span().setTag("hello-to", helloTo);

            final String helloStr = formatString(helloTo);
            printHello(helloStr);
        }
    }

    private  String formatString(final String helloTo) {
        try (Scope scope = tracer.buildSpan("formatString").startActive(true)) {
            final String helloStr = String.format("Hello, %s!", helloTo);
            scope.span().log(ImmutableMap.of("event", "string-format", "value", helloStr));
            return helloStr;
        }
    }

    private void printHello(final String helloStr) {
        try (Scope scope = tracer.buildSpan("printHello").startActive(true)) {
            System.out.println(helloStr);
            scope.span().log(ImmutableMap.of("event", "println"));
        }
    }

    public static void main(final String[] args) {
        if (args.length != 1) {
            throw new IllegalArgumentException("Expecting one argument");
        }

        final String helloTo = args[0];
        final Tracer tracer = Tracing.init("hello-world");
        new HelloActive(tracer).sayHello(helloTo);
    }
}
