package com.example.flowershop.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.util.*;

@Service
public class DeliveryAreaService {
    private final List<String> areas;

    public DeliveryAreaService(@Value("${app.delivery.allowed-cities:Hà Nội}") String configuration) {
        var result = new LinkedHashSet<String>();
        for (String item : configuration.split(";")) {
            if (item.isBlank()) continue;
            if (item.strip().length()>100) throw new IllegalArgumentException("Delivery city exceeds 100 characters");
            result.add(item.strip());
        }
        areas = List.copyOf(result);
    }
    public List<String> list() { return areas; }
    public void requireAllowed(String city, String district, String ward) {
        if (city == null || !areas.contains(city.strip()))
            throw new IllegalArgumentException("Địa chỉ nằm ngoài khu vực giao hàng đang hỗ trợ.");
    }
}
