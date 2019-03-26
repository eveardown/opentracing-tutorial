/**
 * Copyright Estafet Ltd. 2019. All rights reserved.
 */
package lesson03.exercise;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;

import io.dropwizard.Application;
import io.dropwizard.Configuration;
import io.dropwizard.setup.Environment;

/**
 * Microservice to print a message.
 *
 * <p>his microservice must be run in its own terminal window. To run this microservice:</p>
 * <pre>
 *     $ container_id="$(docker ps --filter "ancestor=jaegertracing/all-in-one:1.7" --format='{{.ID}}')"
 *     $ container_ip="$(docker inspect -f '{{range .NetworkSettings.Networks}}{{.IPAddress}}{{end}}' ${container_id})"
 *     $ export JAEGER_ENDPOINT=http://${ontainer_ip}:14268/api/traces
 *     $ ./run.sh lesson03.exercise.PublisherStep1 server
 * </pre>
 */
public class PublisherStep1 extends Application<Configuration> {

    /**
     * The class to print the message.
     *
     */
    @Path("/publish")
    @Produces(MediaType.TEXT_PLAIN)
    public class PublisherResource {

        /**
         * @param helloString
         *          The message to print.
         * @param httpHeaders
         *          The HTTP headers in the request.
         * @return
         *          The string "@code{published}".
         */
        @GET
        public String publish(@QueryParam("helloStr") final String helloString,
                              @Context final HttpHeaders httpHeaders) {
            System.out.println(helloString);
            return "published";
        }
    }


    /**
     * Run the server.
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
        environment.jersey().register(new PublisherResource());
    }


    /**
     * Entry point to start the server.
     * @param args
     *          The server arguments.
     * @throws Exception
     *          If an error occurs.
     */
    public static void main(final String[] args) throws Exception {
        System.setProperty("dw.server.applicationConnectors[0].port", "10082");
        System.setProperty("dw.server.adminConnectors[0].port", "11082");
        new PublisherStep1().run(args);
     }
}
