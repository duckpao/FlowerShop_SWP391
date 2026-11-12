package com.example.flowershop.service;

import com.example.flowershop.entity.CustomFlowerOption;
import com.example.flowershop.entity.CustomFlowerOption.Group;
import com.example.flowershop.repository.CustomFlowerOptionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

@Service
@Transactional(readOnly = true)
public class CustomFlowerCatalogService {
    public record Option(String id, Group group, String label, boolean disabled) {}
    private final CustomFlowerOptionRepository options;

    public CustomFlowerCatalogService(CustomFlowerOptionRepository options) { this.options = options; }

    public List<Option> catalog() {
        return options.catalog().stream().map(o -> new Option(o.getId(), o.getOptionGroup(), o.getLabel(), o.isDisabled())).toList();
    }

    /** Resolve against current database rows, including requests sent outside the UI. */
    public void validate(CustomFlowerOrderService.Preferences preferences) {
        var catalog = options.catalog();
        validateOne(catalog, preferences.flowers(), Set.of(Group.FLOWER));
        validateOne(catalog, preferences.occasion(), Set.of(Group.OCCASION));
        if (preferences.wrapping() != null && !preferences.wrapping().isBlank()) {
            var selected = new HashSet<String>();
            for (String label : preferences.wrapping().split(",", -1)) {
                validateOne(catalog, label, Set.of(Group.WRAPPING, Group.ACCESSORY));
                if (label.isBlank() || !selected.add(label.strip()))
                    throw new IllegalArgumentException("Danh sách giấy gói/phụ kiện không hợp lệ.");
            }
        }
    }

    private void validateOne(List<CustomFlowerOption> catalog, String label, Set<Group> groups) {
        if (label == null || label.isBlank()) return;
        boolean available = catalog.stream().anyMatch(o -> groups.contains(o.getOptionGroup())
                && o.getLabel().equals(label.strip()) && !o.isDisabled());
        if (!available) throw new IllegalArgumentException("Lựa chọn không tồn tại hoặc đã tạm ngưng: " + label.strip());
    }
}
