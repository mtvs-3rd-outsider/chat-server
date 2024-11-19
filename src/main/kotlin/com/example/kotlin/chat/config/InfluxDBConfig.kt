import com.influxdb.client.InfluxDBClient
import com.influxdb.client.InfluxDBClientFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class InfluxDBConfig(
    @Value("\${influx.url}") private val url: String,
    @Value("\${influx.token}") private val token: String,
    @Value("\${influx.org}") private val org: String,
    @Value("\${influx.bucket}") private val bucket: String
) {

    @Bean
    fun influxDBClient(): InfluxDBClient {
        return InfluxDBClientFactory.create(url, token.toCharArray(), org, bucket)
    }
}
