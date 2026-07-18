package org.salvationarmy.whatsapp.service;

import org.salvationarmy.whatsapp.dto.EventRequest;
import org.salvationarmy.whatsapp.dto.EventResponse;
import org.salvationarmy.whatsapp.entity.Event;
import org.salvationarmy.whatsapp.repository.EventRepository;
import org.salvationarmy.whatsapp.repository.PaymentRepository;
import org.salvationarmy.whatsapp.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class EventService {

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    public List<EventResponse> getAllEvents() {
        return eventRepository.findAll().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public List<EventResponse> getActiveEvents() {
        return eventRepository.findByStatus(Event.EventStatus.ACTIVE).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public EventResponse getEventById(UUID id) {
        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Event not found"));
        return toResponse(event);
    }

    @Transactional
    public EventResponse createEvent(EventRequest request, UUID createdByUserId) {
        Event event = new Event();
        event.setName(request.getName());
        event.setDescription(request.getDescription());
        event.setEventType(request.getEventType());
        event.setStartDate(request.getStartDate());
        event.setEndDate(request.getEndDate());
        event.setLocation(request.getLocation());
        event.setStatus(request.getStatus() != null ? 
                Event.EventStatus.valueOf(request.getStatus()) : Event.EventStatus.ACTIVE);
        event.setCreatedByUserId(createdByUserId);

        Event saved = eventRepository.save(event);
        return toResponse(saved);
    }

    @Transactional
    public EventResponse updateEvent(UUID id, EventRequest request) {
        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Event not found"));

        event.setName(request.getName());
        event.setDescription(request.getDescription());
        event.setEventType(request.getEventType());
        event.setStartDate(request.getStartDate());
        event.setEndDate(request.getEndDate());
        event.setLocation(request.getLocation());
        if (request.getStatus() != null) {
            event.setStatus(Event.EventStatus.valueOf(request.getStatus()));
        }

        Event saved = eventRepository.save(event);
        return toResponse(saved);
    }

    @Transactional
    public void deleteEvent(UUID id) {
        if (!eventRepository.existsById(id)) {
            throw new RuntimeException("Event not found");
        }
        eventRepository.deleteById(id);
    }

    private EventResponse toResponse(Event event) {
        EventResponse.EventResponseBuilder builder = EventResponse.builder()
                .id(event.getId())
                .name(event.getName())
                .description(event.getDescription())
                .eventType(event.getEventType())
                .startDate(event.getStartDate())
                .endDate(event.getEndDate())
                .location(event.getLocation())
                .status(event.getStatus().name())
                .createdByUserId(event.getCreatedByUserId())
                .createdAt(event.getCreatedAt())
                .updatedAt(event.getUpdatedAt());

        if (event.getCreatedBy() != null) {
            builder.createdByName(event.getCreatedBy().getFullName());
        } else if (event.getCreatedByUserId() != null) {
            userRepository.findById(event.getCreatedByUserId())
                    .ifPresent(user -> builder.createdByName(user.getFullName()));
        }

        return builder.build();
    }
}
