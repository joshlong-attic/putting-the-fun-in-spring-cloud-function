package example;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.util.function.Function;

@SpringBootApplication
public class HelloApplication {

	@Bean
	public Function<In, Out> function () {
		return incoming -> {
			System.out.println("INCOMING: " + incoming.getIncoming());
			return new Out(incoming.getIncoming().toUpperCase());
		};
	}

	public static void main(String[] args) throws Exception {
		SpringApplication.run(HelloApplication.class, args);
	}
}

@Data
@NoArgsConstructor
@AllArgsConstructor
class In {

	private String incoming;
}

@Data
@AllArgsConstructor
@NoArgsConstructor
class Out {
	private String outgoing;
}