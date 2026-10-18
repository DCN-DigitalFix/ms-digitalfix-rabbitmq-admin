package cl.duoc.ms_digitalfix_rabbitmq_admin.config;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "digitalfix")
public record SecurityProperties(Security security, Cors cors) {

    public record Security(List<String> audiences) {}

    public record Cors(List<String> allowedOrigins) {}
}
