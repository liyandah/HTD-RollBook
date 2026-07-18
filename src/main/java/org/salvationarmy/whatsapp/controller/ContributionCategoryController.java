package org.salvationarmy.whatsapp.controller;

import org.salvationarmy.whatsapp.dto.ContributionCategoryResponse;
import org.salvationarmy.whatsapp.entity.ContributionCategory;
import org.salvationarmy.whatsapp.service.ContributionCategoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/contribution-categories")
public class ContributionCategoryController {

    @Autowired
    private ContributionCategoryService categoryService;

    @GetMapping
    public ResponseEntity<List<ContributionCategoryResponse>> getAllCategories() {
        return ResponseEntity.ok(categoryService.getAllCategories());
    }

    @GetMapping("/active")
    public ResponseEntity<List<ContributionCategoryResponse>> getActiveCategories() {
        return ResponseEntity.ok(categoryService.getActiveCategories());
    }

    @GetMapping("/type/{type}")
    public ResponseEntity<List<ContributionCategoryResponse>> getCategoriesByType(
            @PathVariable String type) {
        try {
            ContributionCategory.CategoryType categoryType = ContributionCategory.CategoryType.valueOf(type.toUpperCase());
            return ResponseEntity.ok(categoryService.getCategoriesByType(categoryType));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }
}
