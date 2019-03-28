package lib;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

import io.opentracing.Scope;
import io.opentracing.Span;

/**
 * This class allows for debug messages for troubleshooting.
 *
 * <p>This class has to use {@code io.jaegertracing.internal} classes to access the internal state of tracing objects.
 * This will probably break if the tracing dependencies are changed.</p>
 *
 */
public final class Debug {

    /**
     * Cannot instantiate.
     */
    private Debug() {
        super();
    }

    /**
     * Display {@link Scope} details for debugging purposes.
     * @param operationName
     *          The name of the operation in progress.
     * @param messagePrefix
     *          The prefix for each debug message
     * @param message
     *          The message to print.
     * @param scope
     *          The scope to show.
     */
    public static void debugScope(final String operationName,
                                  final String messagePrefix,
                                  final String message,
                                  final Scope scope) {

        debug(operationName, messagePrefix, "scope is " + scope.getClass().getName() + " : " + scope.toString() + " .");
        debugSpan(messagePrefix, scope.span());
    }

    /**
     * Debug a span.
     * @param messagePrefix
     *          The message prefix for each debug message.
     * @param span
     *          The span to debug.
     */
    public static void debugSpan(final String messagePrefix, final Span span) {

        final String operationName = getOperationName(span);
        debug(operationName, messagePrefix, "span is " + span.getClass().getName() + " : " + span.toString() + " .");
        debug(operationName, messagePrefix, "span context is " + span.context().toString() + " .");

        final io.jaegertracing.internal.JaegerSpan implementation =
                                                            io.jaegertracing.internal.JaegerSpan.class.cast(span);
        final Map<String, Object> tags = implementation.getTags();

        if (tags.size() != 0) {
            debug(operationName, messagePrefix, "span tags are:");
            tags.entrySet().forEach(entry -> {
                debug(operationName, messagePrefix, "Key : " + entry.getKey() + " : Value : " + entry.getValue());
            });
        }
        else {
            debug(operationName, messagePrefix, "There are no tags in the span.");
        }

        final List<io.jaegertracing.internal.Reference> references = implementation.getReferences();

        if (references.size() != 0) {
            debug(operationName, messagePrefix, "span references are:");
            references.stream().forEach(r -> debug(operationName, messagePrefix, r.toString()));
        }
        else {
            debug(operationName, messagePrefix, "There are no references in the span.");
        }
    }

    /**
     * Get the timestamp for a message.
     * @return
     *          The timestamp in the format "{@code HH:mm:ss.SSS}".
     */
    public static String timestamp() {
        final Instant instant = Instant.now();
        final long timeStampMillis = instant.toEpochMilli();
        final ZoneId zone = ZoneId.systemDefault();
        final DateTimeFormatter df = DateTimeFormatter.ofPattern("HH:mm:ss.SSS").withZone(zone);
        final String timestampString = df.format(Instant.ofEpochMilli(timeStampMillis));

        return timestampString;
    }

    /**
     * Get the operation name from a {@link Span}.
     * @param span
     *          The span.
     * @return
     *          The operation name.
     */
    public static String getOperationName(final Span span) {

        final io.jaegertracing.internal.JaegerSpan implementation =
                                                            io.jaegertracing.internal.JaegerSpan.class.cast(span);

        return implementation.getOperationName();
    }

    /**
     * Print a debug message.
     * @param operationName
     *          The name of the operation in progress.
     * @param messagePrefix
     *          The prefix for the message.
     * @param message
     *          The message to print.
     */
    public static void debug(final String operationName, final String messagePrefix, final String message) {
        final String prefix = timestamp() + "  " + operationName + "  " + messagePrefix + ": ";
        System.out.println(prefix + message);
    }
}
