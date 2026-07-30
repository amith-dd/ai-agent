package com.example.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class MypersonalassistApplication {

	public static void main(String[] args) {
		SpringApplication.run(MypersonalassistApplication.class, args);
	}

}
