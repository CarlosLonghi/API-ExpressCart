package br.com.expresscart;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
public class ExpressCartApplication {

	public static void main(String[] args) {
		SpringApplication.run(ExpressCartApplication.class, args);
	}

}

