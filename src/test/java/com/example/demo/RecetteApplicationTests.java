package com.example.demo;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootTest
@EnableJpaRepositories(basePackages = "com.example.demo.repositoryMysql")
@EntityScan(basePackages = "com.example.demo.entitiesMysql")
class RecetteApplicationTests {

	@Test
	void contextLoads() {
	}

}
