/**
 * Copyright Estafet Ltd. 2019. All rights reserved.
 */
package lib;

import java.util.Iterator;
import java.util.Map;

import okhttp3.Request;

/**
 * A wrapper class for the HTTP request headers object.
 *
 * <p>This class allows the request headers to be part of the tracing context that is propagated over the wire.</p>
 *
 */
public class RequestBuilderCarrier implements io.opentracing.propagation.TextMap {
    /**
     * The request builder object.
     */
    private final Request.Builder builder;

    /**
     * Constructor.
     * @param theBuilder
     *          The request builder.
     */
    RequestBuilderCarrier(final Request.Builder theBuilder) {
        builder = theBuilder;
    }

    /**
     * Do not allow read operations.
     * @return
     *          Returns nothing - always throws an {@link UnsupportedOperationException} exception.
     *
     * @see io.opentracing.propagation.TextMap#iterator()
     */
    @Override
    public Iterator<Map.Entry<String, String>> iterator() {
        throw new UnsupportedOperationException("carrier is write-only");
    }

    /**
     * Add a key, value pair.
     * @param key
     *          The key to add.
     * @param value
     *          The value to add.
     * @see io.opentracing.propagation.TextMap#put(java.lang.String, java.lang.String)
     */
    @Override
    public void put(final String key, final String value) {
        builder.addHeader(key, value);
    }
}
