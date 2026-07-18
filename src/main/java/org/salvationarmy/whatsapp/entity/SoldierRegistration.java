package org.salvationarmy.whatsapp.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "soldier_registration")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SoldierRegistration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "corps_id")
    private String corpsId;

    @Column(name = "first_name", nullable = false)
    private String firstName;

    @Column(name = "last_name", nullable = false)
    private String lastName;

    @Column(name = "corps_name")
    private String corpsName;

    @Column(name = "gender")
    private String gender;

    @Column(name = "dob")
    private LocalDate dob;

    @Column(name = "id_number")
    private String idNumber;

    @Column(name = "marital_status")
    private String maritalStatus;

    @Column(name = "number_of_children")
    private Integer numberOfChildren;

    @Column(name = "address")
    private String address;

    @Column(name = "favorite_song")
    private String favoriteSong;

    @Column(name = "bible_verse")
    private String bibleVerse;

    @Column(name = "dialogflow_session", columnDefinition = "TEXT")
    private String dialogflowSession;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
