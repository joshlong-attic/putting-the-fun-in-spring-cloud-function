package example;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.java.Log;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.util.function.Function;

@Log
@SpringBootApplication
public class HelloApplication {

	@Bean
	Function<In, Out> function() {
		return incoming -> {
			log.info("incoming request payload: " + incoming.getIncoming());
			Out out = new Out(incoming.getIncoming().toUpperCase());
			log.info("outgoing response payload: " + out.getOutgoing());
			return out;
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
@NoArgsConstructor
@AllArgsConstructor
class Out {
	private String outgoing;
}