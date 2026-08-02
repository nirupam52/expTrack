package com.exptrack.category.repository;

import java.util.List;

import com.exptrack.category.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository extends JpaRepository<Category, Integer> {

	List<Category> findByUserIdOrderByName(Integer userId);
}
