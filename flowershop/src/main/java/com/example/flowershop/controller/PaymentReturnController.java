package com.example.flowershop.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.util.UriComponentsBuilder;

/** Supports gateway sessions already issued with the backend as their return address. */
@Controller
public class PaymentReturnController {
    @Value("${app.frontend-base-url:http://localhost:5173}")
    private String frontendBaseUrl;

    @GetMapping("/payment/{result:success|error|cancel}")
    public ModelAndView returnToFrontend(@PathVariable("result") String result, HttpServletRequest request) {
        var target = UriComponentsBuilder.fromUriString(frontendBaseUrl.strip().replaceAll("/+$", ""))
                .path("/payment/" + result).query(request.getQueryString()).build(true).toUri();
        // When the React build is served by Spring itself, render it without redirecting in a loop.
        int targetPort = target.getPort() < 0 ? ("https".equals(target.getScheme()) ? 443 : 80) : target.getPort();
        if (request.getScheme().equals(target.getScheme()) && request.getServerName().equalsIgnoreCase(target.getHost())
                && request.getServerPort() == targetPort) return new ModelAndView("forward:/index.html");
        return new ModelAndView("redirect:" + target.toASCIIString());
    }
}
