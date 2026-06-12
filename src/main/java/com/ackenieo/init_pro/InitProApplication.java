package com.ackenieo.init_pro;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.ackenieo.init_pro.infrastructure.mapper")
public class InitProApplication {

	public static void main(String[] args) {
		SpringApplication.run(InitProApplication.class, args);
	}

}
