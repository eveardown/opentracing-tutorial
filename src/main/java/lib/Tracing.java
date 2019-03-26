package lib;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.core.MultivaluedMap;

import io.jaegertracing.Configuration;
import io.jaegertracing.Configuration.ReporterConfiguration;
import io.jaegertracing.Configuration.SamplerConfiguration;
import io.jaegertracing.internal.JaegerTracer;
import io.jaegertracing.internal.Reference;
import io.jaegertracing.internal.samplers.ConstSampler;
import io.opentracing.Scope;
import io.opentracing.Span;
import io.opentracing.SpanContext;
import io.opentracing.Tracer;
import io.opentracing.propagation.Format;
import io.opentracing.propagation.TextMap;
import io.opentracing.propagation.TextMapExtractAdapter;
import io.opentracing.tag.Tags;
import okhttp3.Request;

/**
 * This class helper methods to initialise tracing and set up the {@link io.opentracing.Span} for each request.
 *
 */
public final class Tracing {

    /**
     * Cannot instantiate.
     */
    private Tracing() {
        super();
    }

    /**
     * Initialise tracing for a service.
     * @param service
     *          The name of the service to initialise tracing for.
     * @return
     *          The tracing implementation.
     */
    public static JaegerTracer init(final String service) {

        // The sampler always makes the same decision for all traces. It samples all traces. If the parameter were
        // zero, it would sample no traces.
        final SamplerConfiguration samplerConfiguration = SamplerConfiguration.fromEnv()
                                                                              .withType(ConstSampler.TYPE)
                                                                              .withParam(new Integer(1));

        // The reporter configuration species what is reported. In this case,
        final ReporterConfiguration reporterConfiguration = ReporterConfiguration.fromEnv().withLogSpans(Boolean.TRUE);

        // The configuration encapsulates the configuration for sampling and reporting.
        final Configuration configuration = new Configuration(service).withSampler(samplerConfiguration)
                                                                      .withReporter(reporterConfiguration);

        // Create the tracer from the configuration.
        final JaegerTracer jaegerTracer = configuration.getTracer();
        Tracing.debug(service, "init", "Created tracer: " + jaegerTracer.toString() + "for service " + service + ".");
        return jaegerTracer;
    }

    /**
     * Create a scope for the request.
     * @param tracer
     *          The {@io.opentracing.Tracer} to use.
     * @param httpHeaders
     *          The HTTP headers in the request.
     * @param operationName
     *          The name of the operation.
     * @return
     *          A {@link Scope} for tracing the request.
     */
    public static Scope startServerSpan(final Tracer tracer,
                                        final javax.ws.rs.core.HttpHeaders httpHeaders,
                                        final String operationName) {
        // format the headers for extraction
        final MultivaluedMap<String, String> requestHeaders = httpHeaders.getRequestHeaders();

        // Get the first value of each Key.
        final Set<String> headerNames = requestHeaders.keySet();
        final HashMap<String, String> contextHeaders = new HashMap<>(headerNames.size());
        for (final String key : headerNames) {
            final String value = requestHeaders.get(key).get(0);

            try {
                Tracing.debug(operationName, "startServerSpan", "Adding context header: " + key + " : " +
                                   URLDecoder.decode(value, "UTF-8") + " .");
            }
            catch (@SuppressWarnings("unused") final UnsupportedEncodingException uee) {
                System.out.println("startServerSpan: Adding context header: " + key + " : " +
                                   " Can't decode value as UTF-8.");
            }
            contextHeaders.put(key, value);
        }

        Tracer.SpanBuilder spanBuilder = null;
        try {
            final SpanContext parentSpanContext = tracer.extract(Format.Builtin.HTTP_HEADERS,
                                                                 new TextMapExtractAdapter(contextHeaders));
            if (parentSpanContext == null) {
                Tracing.debug(operationName, "startServerSpan", "No parent context .");
                spanBuilder = tracer.buildSpan(operationName);
            }
            else {
                Tracing.debug(operationName,
                              "startServerSpan",
                              "Got parent context " + parentSpanContext.toString() + ".");
                spanBuilder = tracer.buildSpan(operationName).asChildOf(parentSpanContext);
            }
        }
        catch (@SuppressWarnings("unused") final IllegalArgumentException e) {
            System.out.println("startServerSpan: Invalid serialized state (corrupt, wrong version, etc) .");
            spanBuilder = tracer.buildSpan(operationName);
        }

        final Scope scope = spanBuilder.withTag(Tags.SPAN_KIND.getKey(), Tags.SPAN_KIND_SERVER)
                                       .startActive(true);
        Tracing.debugScope(operationName, "startServerScope", "started active scope", scope);

        return scope;
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
     * Build the "carrier" object that allows header values to be written to it.
     * @param builder
     *          The request builder to use,
     * @return
     *          The {@link TextMap} containing the headers.
     */
    public static TextMap requestBuilderCarrier(final Request.Builder builder) {
        return new TextMap() {
            @Override
            public Iterator<Map.Entry<String, String>> iterator() {
                throw new UnsupportedOperationException("carrier is write-only");
            }

            @Override
            public void put(final String key, final String value) {
                builder.addHeader(key, value);
            }
        };
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

        final List<Reference> references = implementation.getReferences();

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
