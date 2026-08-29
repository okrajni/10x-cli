package com.example.doneyet.config;

import javax.sql.DataSource;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import java.net.URI;

@Configuration
public class DataSourceConfig {

  @Bean
  @Primary
  public DataSource dataSource() {
    String databaseUrl = System.getenv("DATABASE_URL");

    if (databaseUrl != null && !databaseUrl.isEmpty()) {
      return createDataSourceFromDatabaseUrl(databaseUrl);
    }

    // Fallback to Spring Boot's datasource configuration (using application.properties)
    return null;
  }

  private DataSource createDataSourceFromDatabaseUrl(String databaseUrl) {
    try {
      URI uri = new URI(databaseUrl);

      String host = uri.getHost();
      int port = uri.getPort() != -1 ? uri.getPort() : 5432;
      String database = uri.getPath().substring(1); // Remove leading slash
      String username = uri.getUserInfo().split(":")[0];
      String password = uri.getUserInfo().split(":")[1];

      // Add SSL parameters for Fly.io Managed Postgres
      String jdbcUrl = String.format(
        "jdbc:postgresql://%s:%d/%s?sslmode=require&ssl=true",
        host,
        port,
        database
      );

      return DataSourceBuilder.create()
        .driverClassName("org.postgresql.Driver")
        .url(jdbcUrl)
        .username(username)
        .password(password)
        .build();
    } catch (Exception e) {
      throw new RuntimeException("Failed to parse DATABASE_URL: " + e.getMessage(), e);
    }
  }
}
