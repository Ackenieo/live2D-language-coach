package com.ackenieo.init_pro;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@MapperScan(basePackages = {"com.ackenieo.init_pro.user", "com.ackenieo.init_pro.conversation", "com.ackenieo.init_pro.evaluation", "com.ackenieo.init_pro.realtime"},
            annotationClass = org.apache.ibatis.annotations.Mapper.class)
@EnableAsync
public class InitProApplication {

	public static void main(String[] args) {
		SpringApplication.run(InitProApplication.class, args);
	}

}
