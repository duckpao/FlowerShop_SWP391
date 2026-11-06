package com.example.flowershop;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.web.bind.annotation.*;

import static org.assertj.core.api.Assertions.assertThat;

/** HTTP bindings must also work when the IDE compiles without javac -parameters. */
class ControllerParameterBindingTests {
    @Test
    void namedHttpArgumentsDeclareTheirNamesExplicitly() throws Exception {
        var scanner = new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(RestController.class));
        int checked = 0;
        for (var bean : scanner.findCandidateComponents("com.example.flowershop")) {
            for (var method : Class.forName(bean.getBeanClassName()).getDeclaredMethods()) {
                for (var parameter : method.getParameters()) {
                    String name = null;
                    if (parameter.isAnnotationPresent(PathVariable.class)) {
                        var a = parameter.getAnnotation(PathVariable.class);
                        name = a.name().isBlank() ? a.value() : a.name();
                    } else if (parameter.isAnnotationPresent(RequestParam.class)) {
                        var a = parameter.getAnnotation(RequestParam.class);
                        name = a.name().isBlank() ? a.value() : a.name();
                    } else if (parameter.isAnnotationPresent(RequestHeader.class)) {
                        var a = parameter.getAnnotation(RequestHeader.class);
                        name = a.name().isBlank() ? a.value() : a.name();
                    } else if (parameter.isAnnotationPresent(CookieValue.class)) {
                        var a = parameter.getAnnotation(CookieValue.class);
                        name = a.name().isBlank() ? a.value() : a.name();
                    } else if (parameter.isAnnotationPresent(RequestPart.class)) {
                        var a = parameter.getAnnotation(RequestPart.class);
                        name = a.name().isBlank() ? a.value() : a.name();
                    }
                    if (name == null) continue;
                    // Unnamed maps intentionally receive all values, rather than one named argument.
                    if (java.util.Map.class.isAssignableFrom(parameter.getType()) && name.isBlank()) continue;
                    assertThat(name).as("Explicit HTTP name for %s parameter %s", method, parameter).isNotBlank();
                    checked++;
                }
            }
        }
        assertThat(checked).isGreaterThan(100);
    }
}
