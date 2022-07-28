package org.who.ddccverifier.web

import org.springframework.boot.builder.SpringApplicationBuilder
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer

class ServletInitializer : SpringBootServletInitializer() {
	override fun configure(application: SpringApplicationBuilder): SpringApplicationBuilder {
		return application.sources(
			JacksonConfig::class.java,
			WebApplication::class.java,
			RestProcessor::class.java
		)
	}
}
