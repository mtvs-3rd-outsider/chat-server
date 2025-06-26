package com.example.kotlin.chat

import io.r2dbc.spi.ConnectionFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.io.ClassPathResource
import org.springframework.r2dbc.connection.init.CompositeDatabasePopulator
import org.springframework.r2dbc.connection.init.ConnectionFactoryInitializer
import org.springframework.r2dbc.connection.init.ResourceDatabasePopulator
import reactor.core.publisher.Hooks
import java.lang.management.ManagementFactory
import org.springframework.boot.ApplicationRunner
import org.slf4j.LoggerFactory

@SpringBootApplication(scanBasePackages = ["com.example.kotlin.chat"])
class ChatKotlinApplication {
	companion object {
		private val logger = LoggerFactory.getLogger(ChatKotlinApplication::class.java)
	}
	
	@Bean
	fun startupRunner() = ApplicationRunner {
		logger.info("Chat Server Application Started Successfully!")
		logger.info("Application is running on port: ${it.nonOptionArgs}")
	}
}

fun main(args: Array<String>) {
	try {
		println("=== CHAT SERVER STARTING ===")
		ManagementFactory.getRuntimeMXBean()
			.getInputArguments()
			.forEach(System.out::println);
		Hooks.onOperatorDebug();
		println("=== CALLING runApplication ===")
		runApplication<ChatKotlinApplication>(*args)
		println("=== APPLICATION STARTED SUCCESSFULLY ===")
	} catch (e: Exception) {
		println("=== STARTUP ERROR ===")
		e.printStackTrace()
		System.exit(1)
	}
}

@Configuration
class Config {
	// Temporarily disabled to debug startup issue
	// @Bean
	// fun initializer(connectionFactory: ConnectionFactory): ConnectionFactoryInitializer {
	// 	val initializer = ConnectionFactoryInitializer()
	// 	initializer.setConnectionFactory(connectionFactory)
	// 	val populator = CompositeDatabasePopulator()
	// 	populator.addPopulators(ResourceDatabasePopulator(ClassPathResource("./sql/schema.sql")))
	// 	initializer.setDatabasePopulator(populator)
	// 	return initializer
	// }
}
