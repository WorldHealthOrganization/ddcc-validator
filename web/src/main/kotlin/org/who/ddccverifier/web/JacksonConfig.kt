package org.who.ddccverifier.web

import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.context.FhirVersionEnum
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.json.JsonMapper
import com.fasterxml.jackson.databind.module.SimpleModule
import org.hl7.fhir.r4.model.Bundle
import org.hl7.fhir.r4.model.Composition
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder
import org.who.ddccverifier.trust.pathcheck.KeyUtils
import java.security.PublicKey

@Configuration
open class JacksonConfig {
    @Bean
    open fun jsonCustomizer(): Jackson2ObjectMapperBuilderCustomizer? {
        return Jackson2ObjectMapperBuilderCustomizer {
            builder: Jackson2ObjectMapperBuilder ->
                builder
                    .serializationInclusion(JsonInclude.Include.NON_NULL)
                    .indentOutput(true)
                    .serializers(
                        BundleSerializer(),
                        PublicKeySerializer()
                    )
        }
    }

    class BundleSerializer : JsonSerializer<Bundle>() {
        override fun serialize(dt: Bundle, json: JsonGenerator, prov: SerializerProvider?) {
            json.writeString(
                FhirContext.forCached(FhirVersionEnum.R4).newJsonParser()
                    .encodeResourceToString(dt)
            )
        }
        override fun handledType() = Bundle::class.java
    }

    class PublicKeySerializer : JsonSerializer<PublicKey>() {
        override fun serialize(pk: PublicKey, json: JsonGenerator, prov: SerializerProvider?) {
            json.writeString(KeyUtils.pemFromPublicKey(pk))
        }
        override fun handledType() = PublicKey::class.java
    }
}