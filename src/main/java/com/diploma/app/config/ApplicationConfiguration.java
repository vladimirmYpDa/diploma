package com.diploma.app.config;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@EnableAutoConfiguration
@EnableJpaRepositories(basePackages = {"com.diploma.app.repository"})
@PropertySource("classpath:application.properties")
@ComponentScan(basePackages = {"com.diploma.app"})
public class ApplicationConfiguration {
}
