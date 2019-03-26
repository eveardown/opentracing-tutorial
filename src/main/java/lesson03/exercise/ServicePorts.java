/**
 * Copyright Estafet Ltd. 2019. All rights reserved.
 */
package lesson03.exercise;

/**
 * The service ports.
 *
 */
public final class ServicePorts {


    /**
     * The port that the formatter microservice is listening on.
     */
    public final static int FORMATTER_SERVICE_PORT = 10081;

    /**
     * The admin console port for the formatter service.
     */
    public final static int FORMATTER_ADMIN_PORT = FORMATTER_SERVICE_PORT + 1000;

    /**
     * The port that the publisher microservice is listening on.
     */
    public final static int PUBLISHER_SERVICE_PORT = FORMATTER_SERVICE_PORT + 10;

    /**
     * The admin console port for the publisher service.
     */
    public final static int PUBLISHER_ADMIN_PORT = PUBLISHER_SERVICE_PORT + 1000;
    /**
     * Cannot instantiate.
     */
    private ServicePorts () {
        super();
    }

}
