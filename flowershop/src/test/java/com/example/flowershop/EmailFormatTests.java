package com.example.flowershop;
import jakarta.validation.Validation;
import com.example.flowershop.dto.auth.LoginRequest;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
class EmailFormatTests {
    @Test void rejectMalformedEmailsAndAcceptNormalAddresses() {
        try(var factory=Validation.buildDefaultValidatorFactory()) {
            var validator=factory.getValidator();
            for(String email:java.util.List.of("", "a", "a@", "a@localhost", "a@@example.com", ".a@example.com", "a..b@example.com", "a@-example.com", "a@example..com", "a b@example.com"))
                assertThat(validator.validate(new LoginRequest(email,"password"))).as(email).isNotEmpty();
            for(String email:java.util.List.of("a@example.com", "first.last+tag@sub.example.com"))
                assertThat(validator.validate(new LoginRequest(email,"password"))).as(email).isEmpty();
        }
    }
}
