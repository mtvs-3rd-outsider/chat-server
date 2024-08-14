package com.example.kotlin;

import io.r2dbc.spi.ConnectionFactories;
import io.r2dbc.spi.ConnectionFactory;
import io.r2dbc.spi.ConnectionFactoryOptions;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static io.r2dbc.spi.ConnectionFactoryOptions.*;

@Configuration
public class R2DBCConfiguration   {
    @Bean
    public ConnectionFactory connectionFactory() {
        ConnectionFactory connectionFactory = ConnectionFactories.get(ConnectionFactoryOptions.builder()
                .option(DRIVER, "mysql")
                .option(HOST, "localhost")
                .option(USER, "root")
                .option(PASSWORD, "1234")
                .option(PORT, 3306)
                .option(DATABASE, "forcasthub")
                .build());
        return connectionFactory;
    }
}
