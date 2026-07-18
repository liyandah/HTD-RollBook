package org.salvationarmy.whatsapp.repository;

import org.salvationarmy.whatsapp.entity.SoldierRegistration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SoldierRegistrationRepository extends JpaRepository<SoldierRegistration, Long> {
}
