package com.example.flowershop.controller;

import com.example.flowershop.service.GhnService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/public/ghn")
public class PublicGhnController {
    private final GhnService ghn;
    public PublicGhnController(GhnService ghn) { this.ghn = ghn; }

    @GetMapping("/provinces")
    public List<GhnService.Option> provinces() { return ghn.provinces(); }

    @GetMapping("/provinces/{id}/districts")
    public List<GhnService.Option> districts(@PathVariable int id) { return ghn.districts(id); }

    @GetMapping("/districts/{id}/wards")
    public List<GhnService.Option> wards(@PathVariable int id) { return ghn.wards(id); }
}