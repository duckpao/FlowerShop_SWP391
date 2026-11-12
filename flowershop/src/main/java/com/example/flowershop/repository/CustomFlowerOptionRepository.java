package com.example.flowershop.repository;

import com.example.flowershop.entity.CustomFlowerOption;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public class CustomFlowerOptionRepository extends HibernateRepository<CustomFlowerOption> {
    public CustomFlowerOptionRepository() { super(CustomFlowerOption.class); }

    public List<CustomFlowerOption> catalog() {
        return findAll(Sort.by("optionGroup", "sortOrder", "id"));
    }
}
