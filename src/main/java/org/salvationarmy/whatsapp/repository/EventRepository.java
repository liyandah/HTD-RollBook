package org.salvationarmy.whatsapp.repository;

import org.salvationarmy.whatsapp.entity.Event;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface EventRepository extends JpaRepository<Event, UUID> {
    List<Event> findByStatus(Event.EventStatus status);
    List<Event> findByStatusOrderByCreatedAtDesc(Event.EventStatus status);
    List<Event> findByEventType(String eventType);
}
