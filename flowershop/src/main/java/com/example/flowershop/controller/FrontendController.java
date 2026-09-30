package com.example.flowershop.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/** Only known React routes are forwarded; API and missing assets keep their own responses. */
@Controller
public class FrontendController {
    @GetMapping({"/", "/login", "/account", "/admin", "/admin/shops", "/admin/approvals",
        "/admin/users", "/admin/profile", "/shop-admin", "/shop-admin/staff", "/shop-admin/products",
        "/shop-admin/shop", "/shop-admin/profile", "/shops/{id}", "/products/{id}",
        "/cart", "/home", "/payment/success", "/payment/error", "/payment/cancel"})
    public String index() { return "forward:/index.html"; }
}
