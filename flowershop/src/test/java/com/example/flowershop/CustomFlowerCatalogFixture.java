package com.example.flowershop;

import com.example.flowershop.entity.CustomFlowerOption;
import com.example.flowershop.entity.CustomFlowerOption.Group;
import com.example.flowershop.repository.CustomFlowerOptionRepository;
import java.util.UUID;

final class CustomFlowerCatalogFixture {
    static void seed(CustomFlowerOptionRepository repository) {
        add(repository, Group.FLOWER, "Rose");
        add(repository, Group.OCCASION, "Birthday");
        add(repository, Group.OCCASION, "Khác");
        add(repository, Group.WRAPPING, "Paper");
        add(repository, Group.WRAPPING, "Giấy kraft");
        add(repository, Group.ACCESSORY, "Ribbon");
        add(repository, Group.ACCESSORY, "Ruy băng");
        add(repository, Group.ACCESSORY, "Thiệp chúc mừng");
    }

    static CustomFlowerOption add(CustomFlowerOptionRepository repository, Group group, String label) {
        return repository.saveAndFlush(CustomFlowerOption.builder().id(UUID.randomUUID().toString())
                .optionGroup(group).label(label).disabled(false).sortOrder(10).build());
    }
}
