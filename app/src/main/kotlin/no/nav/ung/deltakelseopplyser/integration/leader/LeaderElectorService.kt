package no.nav.ung.deltakelseopplyser.integration.leader

import org.json.JSONObject
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.http.HttpHeaders.CONTENT_TYPE
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate
import java.net.InetAddress
import java.time.Duration


/**
 * Service for finne ut om denne instansen er leder.
 *
 */

@Service
class LeaderElectorService(
    @Value("\${ELECTOR_GET_URL}") private val leaderElectorURL: String,
    private val restTemplateBuilder: RestTemplateBuilder,
) {
    private companion object {
        private val logger: Logger = LoggerFactory.getLogger(LeaderElectorService::class.java)
    }

    fun erLeader(): Boolean {
        return try {
            val response = restTemplate().exchange(leaderElectorURL, HttpMethod.GET, null, String::class.java)
            val jsonObject = JSONObject(response.body ?: "{}")
            val leaderName = jsonObject.optString("name", "")
            val hostname = InetAddress.getLocalHost().hostName
            leaderName == hostname
        } catch (e: Exception) {
            logger.warn("Failed to check leader status: ${e.message}", e)
            false
        }
    }

    private fun restTemplate(
    ): RestTemplate {
        return restTemplateBuilder
            .connectTimeout(Duration.ofSeconds(20))
            .readTimeout(Duration.ofSeconds(20))
            .defaultHeader(CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .defaultMessageConverters()
            .build()
    }
}