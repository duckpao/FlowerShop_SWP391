package com.example.flowershop.dto.auth;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/** Supported ASCII email syntax is checked by ValidEmail's composed constraints. */
public class EmailDomainValidator implements ConstraintValidator<ValidEmail, String> {
    private static final Set<String> TLDS = loadTlds();
    private static Set<String> loadTlds() {
        try (var stream = EmailDomainValidator.class.getResourceAsStream("/email-tlds.txt")) {
            if (stream == null) throw new IllegalStateException("Missing email-tlds.txt");
            return new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).lines()
                    .filter(line -> !line.isBlank() && !line.startsWith("#"))
                    .map(line -> line.strip().toLowerCase(Locale.ROOT)).collect(Collectors.toUnmodifiableSet());
        } catch (java.io.IOException e) { throw new IllegalStateException(e); }
    }
    @Override public boolean isValid(String email, ConstraintValidatorContext context) {
        if (email == null || email.length() > 254) return false;
        int at = email.indexOf('@');
        int dot = email.lastIndexOf('.');
        return at > 0 && at <= 64 && dot > at + 1 && dot < email.length() - 1
                && TLDS.contains(email.substring(dot + 1).toLowerCase(Locale.ROOT));
    }
}
