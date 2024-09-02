package com.outsider.mop.chat

import io.r2dbc.spi.ConnectionFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.core.io.ClassPathResource
import org.springframework.data.mongodb.repository.config.EnableReactiveMongoRepositories
import org.springframework.r2dbc.connection.init.CompositeDatabasePopulator
import org.springframework.r2dbc.connection.init.ConnectionFactoryInitializer
import org.springframework.r2dbc.connection.init.ResourceDatabasePopulator
@ComponentScan(basePackages = ["com.outsider.mop"])
@SpringBootApplication
@EnableReactiveMongoRepositories(basePackages = ["com.outsider.mop.user.repository"])
class ChatKotlinApplication

fun main(args: Array<String>) {
	runApplication<ChatKotlinApplication>(*args)
}

@Configuration
class Config {
	@Bean
	fun initializer(connectionFactory: ConnectionFactory): ConnectionFactoryInitializer {
		val initializer = ConnectionFactoryInitializer()
		initializer.setConnectionFactory(connectionFactory)
		val populator = CompositeDatabasePopulator()
		populator.addPopulators(ResourceDatabasePopulator(ClassPathResource("./sql/schema.sql")))
		initializer.setDatabasePopulator(populator)
		return initializer
	}
}
