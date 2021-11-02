package ru.newsystems.UploadFileNaumenSD;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;

@SpringBootApplication
public class UploadFileNaumenSdApplication {

	public static void main(String[] args) {
		SpringApplication app = new SpringApplication(UploadFileNaumenSdApplication.class);
		app.setDefaultProperties(Collections.singletonMap("server.port", "1980"));
		app.run(args);
	}

	@Bean
	public RestTemplate restTemplate() {
		return new RestTemplate();
	}

}
