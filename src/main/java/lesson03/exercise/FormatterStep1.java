/**
 * Copyright Estafet Ltd. 2019. All rights reserved.
 */
package lesson03.exercise;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import io.dropwizard.Application;
import io.dropwizard.Configuration;
import io.dropwizard.setup.Environment;

/**
 * Microservice to format the message.
 *
 * <p>This microservice must be run in its own terminal window. To run this microservice:</p>
 * <pre>
 *     $ container_id="$(docker ps --filter "ancestor=jaegertracing/all-in-one:1.7" --format='{{.ID}}')"
 *     $ container_ip="$(docker inspect -f '{{range .NetworkSettings.Networks}}{{.IPAddress}}{{end}}' ${container_id})"
 *     $ export JAEGER_ENDPOINT=http://${ontainer_ip}:14268/api/traces
 *     $ ./run.sh lesson03.exercise.FormatterStep1 server
 * </pre>
 *
 */
public class FormatterStep1 extends Application< Configuration> {

    /**
     * Class to format the string.
     *
     */
    @Path("/format")
    @Produces(MediaType.TEXT_PLAIN)
    public static class FormatterResource {

        /**
         * Format the string.
         * @param helloTo
         *          The parameter for the message.
         * @return
         *          The formatted message.
         */
        @GET
        public String format(@QueryParam("helloTo") final String helloTo) {
            final String helloString = String.format("Hello, %s!", helloTo);

            System.out.println("FormatterStep1 returns \"" + helloString + "\".");
            return helloString;
        }
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
        environment.jersey().register(new FormatterResource());
    }

    /**
     * Entry point for the server.
     * @param args
     *          The command line arguments.
     * @throws Exception
     *          If an error occurs.
     */
    public static void main(final String[] args) throws Exception {
        System.setProperty("dw.server.applicationConnectors[0].port", "10081");
        System.setProperty("dw.server.adminConnectors[0].port", "11081");
        new FormatterStep1().run(args);
    }
}