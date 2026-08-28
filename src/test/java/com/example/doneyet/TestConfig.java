package com.example.doneyet;

import com.example.doneyet.security.RateLimiter;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;

import org.springframework.boot.jdbc.DataSourceBuilder;

@TestConfiguration
public class TestConfig {
    @Bean
    @Primary
    public DataSource testDataSource() {
        return DataSourceBuilder.create()
                .driverClassName("org.h2.Driver")
                .url("jdbc:h2:mem:testdb")
                .username("sa")
                .password("")
                .build();
    }

    @Bean
    @Primary
    public RateLimiter testRateLimiter() {
        // Always allow in tests
        return new RateLimiter() {
            @Override
            public boolean isLoginAllowed(String email, String ipAddress) {
                return true;
            }

            @Override
            public boolean isRegistrationAllowed(String ipAddress) {
                return true;
            }
        };
    }
}
