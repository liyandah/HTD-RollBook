package org.salvationarmy.whatsapp.repository;

import org.salvationarmy.whatsapp.entity.RegistrationProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface RegistrationProfileRepository extends JpaRepository<RegistrationProfile, UUID> {
    Optional<RegistrationProfile> findByUserId(UUID userId);
}
