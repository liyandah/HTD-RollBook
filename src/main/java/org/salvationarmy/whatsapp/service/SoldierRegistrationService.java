package org.salvationarmy.whatsapp.service;

import lombok.extern.slf4j.Slf4j;
import org.salvationarmy.whatsapp.entity.SoldierRegistration;
import org.salvationarmy.whatsapp.repository.SoldierRegistrationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Map;

@Service
@Slf4j
public class SoldierRegistrationService {

    @Autowired
    private SoldierRegistrationRepository repository;

    public void saveFromDialogflow(Map<String, Object> parameters, String sessionId) {
        String firstName = getString(parameters, "firstName");
        String lastName = getString(parameters, "lastName");

        log.info("Saving registration for user {}.{} (Session: {})", firstName,
                lastName != null && !lastName.isEmpty() ? lastName.charAt(0) : '?', sessionId);

        SoldierRegistration registration = SoldierRegistration.builder()
                .corpsId(getString(parameters, "corpsId"))
                .firstName(firstName)
                .lastName(lastName)
                .dob(parseDate(parameters.get("dob")))
                .idNumber(getString(parameters, "idNumber"))
                .favoriteSong(getString(parameters, "favoriteSong"))
                .bibleVerse(getString(parameters, "bibleVerse"))
                .dialogflowSession(sessionId)
                .build();

        repository.save(registration);
        log.info("Saved registration successfully for session {}", sessionId);
    }

    private String getString(Map<String, Object> params, String key) {
        if (!params.containsKey(key))
            return null;
        Object val = params.get(key);
        if (val == null)
            return null;
        String str = val.toString().trim();
        return (str.isEmpty() || "skip".equalsIgnoreCase(str)) ? null : str;
    }

    private LocalDate parseDate(Object dobObj) {
        if (dobObj == null)
            return null;
        String dobStr = dobObj.toString();
        if (dobStr.isEmpty() || "skip".equalsIgnoreCase(dobStr))
            return null;

        try {
            // Dialogflow often sends full ISO string "1999-02-19T12:00:00+02:00"
            if (dobStr.contains("T")) {
                return LocalDate.parse(dobStr.substring(0, dobStr.indexOf("T")));
            }
            // Or just yyyy-MM-dd
            return LocalDate.parse(dobStr);
        } catch (Exception e) {
            log.warn("Failed to parse DOB: {}. Error: {}", dobStr, e.getMessage());
            return null;
        }
    }
}
