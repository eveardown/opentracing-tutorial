/**
 * Copyright Estafet Ltd. 2019. All rights reserved.
 */
package lesson04.exercise;

import io.dropwizard.Application;
import io.dropwizard.Configuration;
import io.dropwizard.setup.Environment;
import io.opentracing.Tracer;
import lesson03.exercise.PublisherStep1;
import lib.ServicePorts;
import lib.Tracing;

/**
 * Microservice to publish the message.
 *
 * <p>This microservice is based on {@link PublisherStep1}, with the following changes:
 * <ol>
 *     <li>Tracing is implemented.</li>
 * </ol>
 *
 * <p>This microservice must be run in its own terminal window. To run this microservice:</p>
 * <pre>
 *     $ container_id="$(docker ps --filter "ancestor=jaegertracing/all-in-one:1.7" --format='{{.ID}}')"
 *     $ container_ip="$(docker inspect -f '{{range .NetworkSettings.Networks}}{{.IPAddress}}{{end}}' ${container_id})"
 *     $ export JAEGER_ENDPOINT=http://${ontainer_ip}:14268/api/traces
 *     $ ./run.sh lesson03.exercise.PublisherStep2 server
 * </pre>
 *
 */
public class Publisher extends Application<Configuration> {

    /**
     * The distributed tracing tracer.
     */
    private final Tracer tracer;

    /**
     * Construct from a distributed tracing tracer.
     * @param theTracer
     *          The tracer to use.
     */
    private Publisher(final Tracer theTracer) {
        tracer = theTracer;
    }

    /**
     * Run the server for the service.
     * @param configuration
     *          The server configuration.
     * @param environment
     *          The server environment.
     * @throws Exception
     *          If an error occurs.
     * @see io.dropwizard.Application#run(io.dropwizard.Configuration, io.dropwizard.setup.Environment)
     */
    @Override
    public void run(final Configuration configuration, final Environment environment) throws Exception {
        environment.jersey().register(new PublisherResource(tracer));
    }

    /**
     * Entry point for the server.
     * @param args
     *          The command line arguments.
     * @throws Exception
     *          If an error occurs.
     */
    public static void main(final String[] args) throws Exception {
        System.setProperty("dw.server.applicationConnectors[0].port",
                           String.valueOf(ServicePorts.PUBLISHER_SERVICE_PORT));
        System.setProperty("dw.server.adminConnectors[0].port", String.valueOf(ServicePorts.PUBLISHER_ADMIN_PORT));

        // These two lines of code cannot be in a try-with-resources statement because no traces will be sent to
        // the Jaeger agent.

        final Tracer tracer = Tracing.init("publisher");
        new Publisher(tracer).run(args);
    }
}
