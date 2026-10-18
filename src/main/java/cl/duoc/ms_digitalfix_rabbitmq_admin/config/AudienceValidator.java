package cl.duoc.ms_digitalfix_rabbitmq_admin.config;

import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.List;

public class AudienceValidator implements OAuth2TokenValidator<Jwt> {

    private final List<String> expectedAudiences;

    public AudienceValidator(List<String> expectedAudiences) {
        this.expectedAudiences = expectedAudiences;
    }

    @Override
    public OAuth2TokenValidatorResult validate(Jwt jwt) {
        boolean coincide = jwt.getAudience().stream().anyMatch(expectedAudiences::contains);

        if (coincide) {
            return OAuth2TokenValidatorResult.success();
        }

        OAuth2Error error = new OAuth2Error(
                "invalid_token",
                "El token no fue emitido para esta API. Audiencia recibida: "
                        + jwt.getAudience() + ". Esperada alguna de: " + expectedAudiences,
                null);

        return OAuth2TokenValidatorResult.failure(error);
    }
}
