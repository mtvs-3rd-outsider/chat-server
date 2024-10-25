import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
	id("org.springframework.boot") version "3.0.3"
	id("io.spring.dependency-management") version "1.1.0"
	kotlin("jvm") version "1.7.22"
	kotlin("plugin.spring") version "1.7.22"
}

group = "com.example.kotlin"
version = "0.0.1-SNAPSHOT"
java.sourceCompatibility = JavaVersion.VERSION_17


repositories {
	mavenCentral()
}

dependencies {
	implementation("org.springframework.boot:spring-boot-starter")
	implementation("org.springframework.boot:spring-boot-starter-actuator")
	implementation("org.springframework.boot:spring-boot-starter-thymeleaf")
	implementation("org.springframework.boot:spring-boot-starter-webflux")
	implementation("org.springframework.boot:spring-boot-starter-data-r2dbc")
	implementation("org.springframework.boot:spring-boot-starter-rsocket")
	implementation("io.r2dbc:r2dbc-h2")

	implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
	implementation("com.github.javafaker:javafaker:1.0.2")
// https://mvnrepository.com/artifact/io.asyncer/r2dbc-mysql
	implementation("io.asyncer:r2dbc-mysql:1.2.0")
	implementation("io.github.cdimascio:java-dotenv:5.2.2")
// https://mvnrepository.com/artifact/io.github.cdimascio/java-dotenv

	implementation("org.jetbrains.kotlin:kotlin-reflect")
	implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
	implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core")
	implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactive")
	implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor")
// https://mvnrepository.com/artifact/com.mysql/mysql-connector-j
	implementation("com.mysql:mysql-connector-j:8.3.0")
	testImplementation("org.springframework.boot:spring-boot-starter-test")
	testImplementation("app.cash.turbine:turbine:0.4.1")

	runtimeOnly("com.h2database:h2")
	implementation("org.springframework.kafka:spring-kafka")
	implementation("org.apache.kafka:kafka-clients")
	implementation("org.springframework.boot:spring-boot-starter-data-redis-reactive:3.3.4")
	implementation("org.jetbrains:markdown:0.2.2")
	testImplementation("org.springframework.boot:spring-boot-starter-test") {
		exclude(group = "org.junit.vintage", module = "junit-vintage-engine")
	}
	// Spring Security와 RSocket 통합
//	implementation("org.springframework.boot:spring-boot-starter-security")
//	implementation("org.springframework.security:spring-security-messaging")
	implementation("org.springframework.security:spring-security-rsocket")
	implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")
	// JWT를 사용하려면 다음 의존성 추가
	implementation("io.jsonwebtoken:jjwt-api:0.11.5")
	runtimeOnly("io.jsonwebtoken:jjwt-impl:0.11.5")
	runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.11.5") // JSON 처리
}

tasks.withType<Test> {
	useJUnitPlatform()
}

tasks.withType<KotlinCompile> {
	kotlinOptions {
		freeCompilerArgs = listOf("-Xjsr305=strict")
		jvmTarget = "17"
	}
}
