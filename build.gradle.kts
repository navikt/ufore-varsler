plugins {
	kotlin("jvm") version "2.4.10"
	kotlin("plugin.spring") version "2.4.10"
	id("org.springframework.boot") version "4.1.1"
	id("io.spring.dependency-management") version "1.1.7"
}

group = "no.nav.ufore"
version = "0.0.1-SNAPSHOT"

ext["tomcat.version"] = "11.0.22"
ext["postgresql.version"] = "42.7.11"

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(25)
	}
}

repositories {
	mavenCentral()
	maven("https://github-package-registry-mirror.gc.nav.no/cached/maven-release")
}

dependencies {
	implementation("org.springframework.boot:spring-boot-starter-actuator")
	implementation("org.springframework.boot:spring-boot-starter-flyway")
	implementation("org.springframework.boot:spring-boot-starter-jdbc")
	implementation("org.springframework.boot:spring-boot-starter-kafka")
	implementation("org.springframework.boot:spring-boot-starter-webmvc")
	implementation("org.flywaydb:flyway-database-postgresql")
	implementation("org.jetbrains.kotlin:kotlin-reflect")
	implementation("no.nav.tms.varsel:kotlin-builder:2.2.0")
	implementation("io.getunleash:unleash-client-java:12.2.3")
	implementation("tools.jackson.module:jackson-module-kotlin")
	implementation("net.logstash.logback:logstash-logback-encoder:9.0")
	implementation("io.micrometer:micrometer-registry-prometheus")
	implementation("io.prometheus:prometheus-metrics-exporter-pushgateway")
	implementation("com.nimbusds:nimbus-jose-jwt:10.9.1")
	runtimeOnly("com.h2database:h2")
	runtimeOnly("org.postgresql:postgresql")
	runtimeOnly("com.google.cloud.sql:postgres-socket-factory:1.30.0")
	testImplementation("org.springframework.boot:spring-boot-starter-kafka-test")
	testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
	testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
	testImplementation("com.ninja-squad:springmockk:5.0.1")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

kotlin {
	compilerOptions {
		freeCompilerArgs.addAll("-Xjsr305=strict", "-Xannotation-default-target=param-property")
	}
}

tasks.withType<Test> {
	useJUnitPlatform()

	testLogging {
		showStandardStreams = true
	}
}
