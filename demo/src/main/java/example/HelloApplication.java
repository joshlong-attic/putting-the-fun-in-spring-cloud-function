package example;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.util.function.Function;

@SpringBootApplication
public class HelloApplication {

	@Bean
	public Function<String, HelloResponse> hello() {
		return value -> new HelloResponse("Hello world");
	}

	public static void main(String[] args) throws Exception {
		SpringApplication.run(HelloApplication.class, args);
	}
}

@Data
@RequiredArgsConstructor
class HelloResponse {

	private final String message;
}