package org.salvationarmy.whatsapp.service;

import org.salvationarmy.whatsapp.dto.ContributionCategoryResponse;
import org.salvationarmy.whatsapp.entity.ContributionCategory;
import org.salvationarmy.whatsapp.repository.ContributionCategoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ContributionCategoryService {

    @Autowired
    private ContributionCategoryRepository categoryRepository;

    public List<ContributionCategoryResponse> getAllCategories() {
        return categoryRepository.findAll().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public List<ContributionCategoryResponse> getActiveCategories() {
        return categoryRepository.findByActiveTrue().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public List<ContributionCategoryResponse> getCategoriesByType(ContributionCategory.CategoryType type) {
        return categoryRepository.findByTypeAndActiveTrue(type).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public ContributionCategoryResponse getCategoryById(UUID id) {
        ContributionCategory category = categoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Category not found"));
        return toResponse(category);
    }

    private ContributionCategoryResponse toResponse(ContributionCategory category) {
        return ContributionCategoryResponse.builder()
                .id(category.getId())
                .name(category.getName())
                .type(category.getType().name())
                .description(category.getDescription())
                .active(category.getActive())
                .createdAt(category.getCreatedAt())
                .updatedAt(category.getUpdatedAt())
                .build();
    }
}
