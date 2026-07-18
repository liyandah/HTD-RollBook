package org.salvationarmy.whatsapp.repository;

import org.salvationarmy.whatsapp.entity.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ProjectRepository extends JpaRepository<Project, UUID> {
    List<Project> findByStatus(Project.ProjectStatus status);
    List<Project> findByStatusOrderByCreatedAtDesc(Project.ProjectStatus status);
}
