package org.who.ddccverifier.web

import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.context.FhirVersionEnum
import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.module.SimpleModule
import org.hl7.fhir.r4.model.Composition
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.who.ddccverifier.trust.pathcheck.KeyUtils
import java.security.PublicKey

@Configuration
open class JacksonConfig {
    @Bean
    @Primary
    open fun serializingObjectMapper(): ObjectMapper {
        val objectMapper = ObjectMapper()
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)

        val javaTimeModule = SimpleModule()
        javaTimeModule.addSerializer(Composition::class.java, CompositionSerializer())
        javaTimeModule.addSerializer(PublicKey::class.java, PublicKeySerializer())
        objectMapper.registerModule(javaTimeModule)

        return objectMapper
    }

    class CompositionSerializer : JsonSerializer<Composition?>() {
        override fun serialize(dt: Composition?, json: JsonGenerator, prov: SerializerProvider?) {
            json.writeString(
                FhirContext.forCached(FhirVersionEnum.R4).newJsonParser()
                    .encodeResourceToString(dt)
            )
        }
    }

    class PublicKeySerializer : JsonSerializer<PublicKey>() {
        override fun serialize(pk: PublicKey, json: JsonGenerator, prov: SerializerProvider?) {
            json.writeString(KeyUtils.pemFromPublicKey(pk))
        }
    }
}