package lesson01.exercise;

/**
 * Initial logging attempt.
 *
 * <p>This example prints a message to {@code stdout}.</p>
 *
 */
public class HelloNoTracing {

    /**
     * Program entry point.
     * @param args
     *          The program arguments.
     *          The only argument is the parameter for the message.
     */
    public static void main(final String[] args) {

        // Verify there is exactly one argument.
        if (args.length != 1) {
            throw new IllegalArgumentException("Expecting one argument - the message parameter.");
        }

        // Get the message parameter.
        final String helloTo = args[0];

        // Print the message to stdout.
        new HelloNoTracing().sayHello(helloTo);
    }

    /**
     * Print a message with a parameter.
     * @param helloTo
     *          The parameter for the message.
     */
    private void sayHello(final String helloTo) {
        final String helloStr = String.format("Hello, %s!", helloTo);
        System.out.println(helloStr);
    }
}