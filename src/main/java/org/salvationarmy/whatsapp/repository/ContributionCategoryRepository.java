package org.salvationarmy.whatsapp.repository;

import org.salvationarmy.whatsapp.entity.ContributionCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ContributionCategoryRepository extends JpaRepository<ContributionCategory, UUID> {
    List<ContributionCategory> findByActiveTrue();
    List<ContributionCategory> findByTypeAndActiveTrue(ContributionCategory.CategoryType type);
    boolean existsByName(String name);
}
