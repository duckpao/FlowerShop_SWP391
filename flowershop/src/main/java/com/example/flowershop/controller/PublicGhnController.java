package com.example.flowershop.controller;

import com.example.flowershop.service.GhnService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/public/ghn")
public class PublicGhnController {
    private final GhnService ghn;
    public PublicGhnController(GhnService ghn) { this.ghn = ghn; }

    /** Step 1: the provinces of the two-level model (34 since 01/07/2025). */
    @GetMapping("/provinces")
    public List<GhnService.Option> provinces() { return ghn.provinces(); }

    /** Step 2: wards directly under a province; there is no district tier. */
    @GetMapping("/provinces/{id}/wards")
    public List<GhnService.Option> wards(@PathVariable int id) { return ghn.wards(id); }
}
