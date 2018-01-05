package example;

import org.springframework.cloud.function.adapter.aws.SpringBootRequestHandler;

/**
 * Look ma! No AWS! (or much anything else, really.)
 */
public class HelloHandler extends SpringBootRequestHandler<In, Out> {
}
