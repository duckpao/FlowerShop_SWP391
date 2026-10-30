package com.example.flowershop.controller;

import com.example.flowershop.service.GhnService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/public/ghn")
public class PublicGhnController {
    private final GhnService ghn;
    public PublicGhnController(GhnService ghn) { this.ghn = ghn; }

    /** Step 1: all provinces. */
    @GetMapping("/provinces")
    public List<GhnService.Option> provinces() { return ghn.provinces(); }

    /** Step 2: districts of a province. */
    @GetMapping("/provinces/{id}/districts")
    public List<GhnService.Option> districts(@PathVariable(name = "id") int id) { return ghn.districts(id); }

    /** Step 3: wards of a district; the id is a ward code. */
    @GetMapping("/districts/{id}/wards")
    public List<GhnService.Option> wards(@PathVariable(name = "id") int id) { return ghn.wards(id); }
}
