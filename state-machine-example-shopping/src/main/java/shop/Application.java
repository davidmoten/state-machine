package shop;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;

@SpringBootApplication
public class Application {

	@Bean
	@Primary
	public ObjectMapper objectMapper() {
		return new ObjectMapper() //
				.setVisibility(PropertyAccessor.FIELD, Visibility.PUBLIC_ONLY)
				.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS) //
				.registerModule(new Jdk8Module());
	}

	public static void main(String[] args) {
		SpringApplication.run(Application.class, args);
	}

}