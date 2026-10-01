package com.example.flowershop.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/** Only known React routes are forwarded; API and missing assets keep their own responses. */
@Controller
public class FrontendController {
    @GetMapping({
        "/", "/login", "/account", "/home", "/favorites", "/cart", "/checkout",
        "/invitations/accept",
        "/products", "/products/**",
        "/shops/**",
        "/orders", "/orders/**",
        "/admin", "/admin/**",
        "/shop-admin", "/shop-admin/**",
        "/payment/**"
    })
    public String index() { return "forward:/index.html"; }
}
