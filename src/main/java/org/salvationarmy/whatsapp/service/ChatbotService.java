package org.salvationarmy.whatsapp.service;

import lombok.extern.slf4j.Slf4j;
import org.salvationarmy.whatsapp.entity.ChatMessage;
import org.salvationarmy.whatsapp.entity.ChatSession;
import org.salvationarmy.whatsapp.entity.RecordStatus;
import org.salvationarmy.whatsapp.entity.SoldierRecord;
import org.salvationarmy.whatsapp.entity.SoldierRegistration;
import org.salvationarmy.whatsapp.repository.ChatMessageRepository;
import org.salvationarmy.whatsapp.repository.ChatSessionRepository;
import org.salvationarmy.whatsapp.repository.SoldierRecordRepository;
import org.salvationarmy.whatsapp.repository.SoldierRegistrationRepository;
import org.salvationarmy.whatsapp.util.MembershipClassifier;
import org.salvationarmy.whatsapp.util.RegistrationFieldValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.time.Period;
import java.time.Year;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ChatbotService {

    @Autowired
    private ChatSessionRepository chatSessionRepository;

    @Autowired
    private ChatMessageRepository chatMessageRepository;

    @Autowired
    private SoldierRegistrationRepository soldierRegistrationRepository;

    @Autowired
    private SoldierRecordRepository soldierRecordRepository;

    @Autowired
    private org.salvationarmy.whatsapp.service.FileService fileService;

    @Value("${bot.fixed-corps-id:}")
    private String fixedCorpsId;

    private static final String DEFAULT_CORPS_NAME = "Highfield Temple";

    private static final Pattern NAME_PATTERN = Pattern.compile("^[a-zA-Z\\s\\-']+$");
    private static final Pattern ID_PATTERN = Pattern.compile(".*\\d{6,}.*"); // Contains 6+ digits

    /**
     * Process user message and return bot response
     */
    @Transactional
    public BotResponse processMessage(String sessionId, String userMessage) {
        // Normalize message
        String message = userMessage.trim().toLowerCase();

        // Handle global commands
        if ("reset".equals(message)) {
            return handleReset(sessionId);
        }
        if ("register someone else".equals(message) || "register family member".equals(message) || "register another person".equals(message)) {
            return handleRegisterAnother(sessionId);
        }
        if ("finish".equals(message)) {
            return new BotResponse("Thank you. Your registration session is complete.", "COMPLETE", "COMPLETE");
        }
        if ("cancel".equals(message) || message.contains("cancel")) {
            return handleCancel(sessionId);
        }

        // Get or create session
        ChatSession session = getOrCreateSession(sessionId);

        // Save user message
        saveMessage(sessionId, "USER", userMessage);

        // Process based on current state
        BotResponse response;
        switch (session.getState()) {
            case "START":
                response = handleStart(session, userMessage);
                break;
            case "ASK_FIRST_NAME":
                response = handleFirstName(session, userMessage);
                break;
            case "ASK_LAST_NAME":
                response = handleLastName(session, userMessage);
                break;
            case "ASK_CORPS":
                response = handleCorps(session, userMessage);
                break;
            case "ASK_GENDER":
                response = handleGender(session, userMessage);
                break;
            case "ASK_DOB":
                response = handleDob(session, userMessage);
                break;
            case "ASK_CHILDREN_COUNT":
                response = handleChildrenCount(session, userMessage);
                break;
            case "ASK_MARITAL_STATUS":
                response = handleMaritalStatus(session, userMessage);
                break;
            case "ASK_ADDRESS":
                response = handleAddress(session, userMessage);
                break;
            case "ASK_PHONE":
                response = handlePhone(session, userMessage);
                break;
            case "ASK_ID_NUMBER":
                response = handleIdNumber(session, userMessage);
                break;
            case "ASK_NEXT_OF_KIN_NAME":
                response = handleNextOfKinName(session, userMessage);
                break;
            case "ASK_NEXT_OF_KIN_PHONE":
                response = handleNextOfKinPhone(session, userMessage);
                break;
            case "ASK_FAVORITE_SONG":
                response = handleFavoriteSong(session, userMessage);
                break;
            case "ASK_FAVORITE_VERSE":
                response = handleFavoriteVerse(session, userMessage);
                break;
            case "RESUME_CHOICE":
                response = handleResumeChoice(session, userMessage);
                break;
            case "ASK_WARD":
                response = handleWard(session, userMessage);
                break;
            case "ASK_BRIGADE":
                response = handleBrigade(session, userMessage);
                break;
            case "ASK_DEPARTMENT":
                response = handleDepartment(session, userMessage);
                break;
            case "ASK_FAMILY_SAME_SURNAME":
                response = handleFamilySameSurname(session, userMessage);
                break;
            case "ASK_FAMILY_RELATION_TYPE":
                response = handleFamilyRelationType(session, userMessage);
                break;
            case "ASK_FAMILY_FIRST_NAME":
                response = handleFamilyFirstName(session, userMessage);
                break;
            case "ASK_FAMILY_SAME_ADDRESS":
                response = handleFamilySameAddress(session, userMessage);
                break;
            case "ASK_FAMILY_SAME_CORPS":
                response = handleFamilySameCorps(session, userMessage);
                break;
            case "ASK_CERT_IMAGE":
                response = handleCertImage(session, userMessage);
                break;
            case "ASK_PERSON_IMAGE":
                response = handlePersonImage(session, userMessage);
                break;
            case "VERIFIED_NOTIFICATION":
                response = handleVerifiedNotification(session, userMessage);
                break;
            case "COMPLETE":
                response = checkForVerification(session);
                break;
            default:
                response = new BotResponse("I'm not sure what to do. Type REGISTER to begin registration.",
                                         session.getState(), session.getStatus());
        }

        // Save bot response
        enrichResponseWithMemberMeta(session, response);
        saveMessage(sessionId, "BOT", response.getReplyText());

        return response;
    }

    private void enrichResponseWithMemberMeta(ChatSession session, BotResponse response) {
        Optional<SoldierRecord> recordOpt = findSessionRecord(session);
        if (recordOpt.isEmpty()) {
            if (session.getFirstName() != null) response.setMemberFirstName(session.getFirstName());
            if (session.getLastName() != null) response.setMemberLastName(session.getLastName());
            if (session.getDepartment() != null) response.setMemberDepartment(session.getDepartment());
            return;
        }
        SoldierRecord record = recordOpt.get();
        if (record.getStatus() != null) response.setMemberStatus(record.getStatus().name());
        if (record.getFirstName() != null) response.setMemberFirstName(record.getFirstName());
        if (record.getFamilyName() != null) response.setMemberLastName(record.getFamilyName());
        if (record.getRecordCode() != null) response.setMemberRecordCode(record.getRecordCode());
        if (record.getDepartment() != null) response.setMemberDepartment(record.getDepartment());
        if (record.getPersonImagePath() != null) response.setPersonImagePath(record.getPersonImagePath());
    }

    private ChatSession getOrCreateSession(String sessionId) {
        Optional<ChatSession> existing = chatSessionRepository.findBySessionId(sessionId);
        if (existing.isPresent()) {
            return existing.get();
        }

        ChatSession session = new ChatSession();
        session.setSessionId(sessionId);
        session.setState("START");
        session.setStatus("ACTIVE");
        session.setCorpsName(DEFAULT_CORPS_NAME);
        if (fixedCorpsId != null && !fixedCorpsId.trim().isEmpty()) {
            try {
                session.setCorpsId(Integer.parseInt(fixedCorpsId.trim()));
            } catch (NumberFormatException e) {
                log.warn("Invalid bot.fixed-corps-id value: {}", fixedCorpsId);
            }
        }
        return chatSessionRepository.save(session);
    }

    private BotResponse handleStart(ChatSession session, String input) {
        String normalized = input == null ? "" : input.trim().toLowerCase();
        if (!(normalized.contains("register") || normalized.contains("start") || normalized.contains("hi"))) {
            return new BotResponse(
                    "Welcome to HT-E Roll Book. Type REGISTER to start registration.",
                    "START",
                    "ACTIVE"
            );
        }

        Optional<SoldierRecord> existing = findSessionRecord(session);
        if (existing.isPresent()) {
            SoldierRecord record = existing.get();
            session.setRecordId(record.getId());
            if (record.getStatus() == RecordStatus.PENDING || record.getStatus() == RecordStatus.IN_PROGRESS) {
                String nextState = determinePreApprovalMissingState(record);
                session.setState(nextState);
                chatSessionRepository.save(session);
                if ("COMPLETE".equals(nextState)) {
                    return new BotResponse("Verification in progress. Please wait for Admin approval.", "COMPLETE", "COMPLETE");
                }
                return new BotResponse(promptForState(nextState), nextState, "ACTIVE");
            }
            if (record.getStatus() == RecordStatus.DECLINED) {
                session.setState("RESUME_CHOICE");
                chatSessionRepository.save(session);
                return new BotResponse(
                        "Your previous entry needs correction. Let's update your details. Continue Registration or Start New?",
                        "RESUME_CHOICE",
                        "DECLINED",
                        record.getHouseholdAdminNotes(),
                        Arrays.asList("Continue Registration", "Start New Registration")
                );
            }
            if (record.getStatus() == RecordStatus.VERIFIED) {
                return notifyChatOfVerification(session, record);
            }
        }

        session.setState("ASK_FIRST_NAME");
        chatSessionRepository.save(session);
        return new BotResponse("Welcome! Let's get started. What is your First Name?",
                              "ASK_FIRST_NAME", "ACTIVE");
    }

    private BotResponse handleResumeChoice(ChatSession session, String input) {
        String normalized = input == null ? "" : input.trim().toLowerCase();
        if (normalized.contains("start new")) {
            return handleReset(session.getSessionId());
        }
        if (!normalized.contains("continue")) {
            return new BotResponse(
                    "Please choose Continue Registration or Start New.",
                    "RESUME_CHOICE",
                    "ACTIVE",
                    null,
                    Arrays.asList("Continue Registration", "Start New Registration")
            );
        }

        Optional<SoldierRecord> recordOpt = findSessionRecord(session);
        if (recordOpt.isEmpty()) {
            session.setState("ASK_FIRST_NAME");
            chatSessionRepository.save(session);
            return new BotResponse("Let's start fresh. What is your First Name?", "ASK_FIRST_NAME", "ACTIVE");
        }

        SoldierRecord record = recordOpt.get();
        String nextState = determineNextMissingState(record);
        session.setState(nextState);
        session.setRecordId(record.getId());
        chatSessionRepository.save(session);
        return new BotResponse(promptForState(nextState), nextState, "ACTIVE");
    }

    private BotResponse handleRegisterAnother(String sessionId) {
        Optional<ChatSession> sessionOpt = chatSessionRepository.findBySessionId(sessionId);
        if (sessionOpt.isEmpty()) {
            return new BotResponse("Session not found. Type REGISTER to start.", "START", "ACTIVE");
        }
        ChatSession session = sessionOpt.get();
        if (session.getRecordId() != null && session.getOriginalRegistrantId() == null) {
            session.setOriginalRegistrantId(session.getRecordId());
        }
        // Keep household defaults (surname, address, corps), clear person-specific fields.
        session.setFirstName(null);
        session.setGender(null);
        session.setDob(null);
        session.setAge(null);
        session.setMaritalStatus(null);
        session.setChildrenCount(null);
        session.setPhoneNumber(null);
        session.setIdNumber(null);
        session.setNextOfKinName(null);
        session.setNextOfKinPhone(null);
        session.setFavoriteSong(null);
        session.setBibleVerse(null);
        session.setWard(null);
        session.setBrigade(null);
        session.setDepartment(null);
        session.setRecordId(null);
        session.setPersonImageUploaded(false);
        session.setCertImageUploaded(false);
        session.setFamilyMode(true);
        session.setFamilyRelationType(null);
        session.setAskMaritalAfterAddress(false);
        session.setStatus("ACTIVE");
        session.setState("ASK_FAMILY_RELATION_TYPE");
        chatSessionRepository.save(session);
        return new BotResponse(
                "Are you registering a Family Member or a Friend?",
                "ASK_FAMILY_RELATION_TYPE",
                "ACTIVE",
                null,
                Arrays.asList("Family", "Friend")
        );
    }

    private BotResponse handleFamilyRelationType(ChatSession session, String input) {
        String normalized = input == null ? "" : input.trim().toLowerCase();
        if (normalized.startsWith("fam")) {
            session.setFamilyRelationType("Family");
            session.setState("ASK_FAMILY_SAME_SURNAME");
            chatSessionRepository.save(session);
            return new BotResponse(
                    "Does this person have the same surname (" + (session.getLastName() != null ? session.getLastName() : "your surname") + ")?",
                    "ASK_FAMILY_SAME_SURNAME",
                    "ACTIVE",
                    null,
                    Arrays.asList("Yes", "No")
            );
        }
        if (normalized.startsWith("fri")) {
            session.setFamilyRelationType("Friend");
            session.setState("ASK_FIRST_NAME");
            chatSessionRepository.save(session);
            return new BotResponse("What is your Friend's First Name?", "ASK_FIRST_NAME", "ACTIVE");
        }
        return new BotResponse("Please choose Family or Friend.", "ASK_FAMILY_RELATION_TYPE", "ACTIVE", null, Arrays.asList("Family", "Friend"));
    }

    private BotResponse handleFirstName(ChatSession session, String input) {
        if (!isValidName(input)) {
            return new BotResponse("Please enter a valid first name (letters only, no numbers or special characters).", 
                                 "ASK_FIRST_NAME", "ACTIVE");
        }

        session.setFirstName(input.trim());
        session.setState("ASK_LAST_NAME");
        chatSessionRepository.save(session);
        return new BotResponse("Great. And your Surname?",
                              "ASK_LAST_NAME", "ACTIVE");
    }

    private BotResponse handleLastName(ChatSession session, String input) {
        if (!isValidName(input)) {
            return new BotResponse("Please enter a valid last name (letters only, no numbers or special characters).", 
                                 "ASK_LAST_NAME", "ACTIVE");
        }

        session.setLastName(input.trim());
        session.setState("ASK_GENDER");
        chatSessionRepository.save(session);
        return new BotResponse("Are you Male or Female?",
                              "ASK_GENDER", "ACTIVE");
    }

    private BotResponse handleCorps(ChatSession session, String input) {
        session.setCorpsName(DEFAULT_CORPS_NAME);
        session.setState("ASK_WARD");
        chatSessionRepository.save(session);
        return new BotResponse("What is your Ward?", "ASK_WARD", "ACTIVE");
    }

    private BotResponse handleGender(ChatSession session, String input) {
        String genderNorm = MembershipClassifier.normalizeGender(input);
        if (!"MALE".equals(genderNorm) && !"FEMALE".equals(genderNorm)) {
            return new BotResponse("Please choose one: Male or Female.", "ASK_GENDER", "ACTIVE");
        }
        session.setGender(genderNorm);
        session.setState("ASK_DOB");
        chatSessionRepository.save(session);
        return new BotResponse("What is your Date of Birth? (Please use DD/MM/YYYY format)", "ASK_DOB", "ACTIVE");
    }

    private BotResponse handleDob(ChatSession session, String input) {
        LocalDate dob = parseDate(input);
        if (dob == null) {
            return new BotResponse("Invalid date format. Please enter your date of birth as DD/MM/YYYY or YYYY-MM-DD (e.g., 15/03/1990 or 1990-03-15)", 
                                 "ASK_DOB", "ACTIVE");
        }

        if (dob.isAfter(LocalDate.now())) {
            return new BotResponse("Date of birth cannot be in the future. Please enter a valid date.", 
                                 "ASK_DOB", "ACTIVE");
        }

        session.setDob(dob);
        int age = calculateAge(dob);
        session.setAge(age);
        session.setState("ASK_CHILDREN_COUNT");
        chatSessionRepository.save(session);
        return new BotResponse("How many children do you have? (If none, type 0)", "ASK_CHILDREN_COUNT", "ACTIVE");
    }

    private BotResponse handleChildrenCount(ChatSession session, String input) {
        Integer children = parseChildrenCount(input);
        if (children == null) {
            return new BotResponse("Please enter a valid number for children (0, 1, 2 ...).", "ASK_CHILDREN_COUNT", "ACTIVE");
        }
        session.setChildrenCount(children);
        if (Boolean.TRUE.equals(session.getFamilyMode()) && !isBlank(session.getAddress())) {
            session.setState("ASK_FAMILY_SAME_ADDRESS");
            chatSessionRepository.save(session);
            return new BotResponse(
                    "Do they live at the same address (" + session.getAddress() + ")?",
                    "ASK_FAMILY_SAME_ADDRESS",
                    "ACTIVE",
                    null,
                    Arrays.asList("Yes", "No")
            );
        }
        session.setState("ASK_ADDRESS");
        chatSessionRepository.save(session);
        return new BotResponse("Thank you. What is your Home Address?", "ASK_ADDRESS", "ACTIVE");
    }

    private BotResponse handleMaritalStatus(ChatSession session, String input) {
        String maritalNorm = MembershipClassifier.normalizeMarital(input);
        if (!"SINGLE".equals(maritalNorm) && !"MARRIED".equals(maritalNorm) && !"WIDOW".equals(maritalNorm)) {
            return new BotResponse("Please reply with Single, Married, or Widowed.", "ASK_MARITAL_STATUS", "ACTIVE");
        }
        session.setMaritalStatus(maritalNorm);
        session.setAskMaritalAfterAddress(false);
        session.setCorpsName(DEFAULT_CORPS_NAME);
        session.setState("ASK_WARD");
        chatSessionRepository.save(session);
        return new BotResponse("What is your Ward?", "ASK_WARD", "ACTIVE");
    }

    private BotResponse handleAddress(ChatSession session, String input) {
        if (input == null || input.trim().isEmpty()) {
            return new BotResponse("Please provide your home address.", "ASK_ADDRESS", "ACTIVE");
        }
        session.setAddress(input.trim());
        if (Boolean.TRUE.equals(session.getAskMaritalAfterAddress())) {
            session.setState("ASK_MARITAL_STATUS");
            chatSessionRepository.save(session);
            return new BotResponse("What is their Marital Status? (Single, Married, Widowed)", "ASK_MARITAL_STATUS", "ACTIVE");
        }
        session.setCorpsName(DEFAULT_CORPS_NAME);
        session.setState("ASK_WARD");
        chatSessionRepository.save(session);
        return new BotResponse("What is your Ward?", "ASK_WARD", "ACTIVE");
    }

    private BotResponse handlePhone(ChatSession session, String input) {
        String phone = input == null ? "" : input.trim();
        if (RegistrationFieldValidator.isMissingRequiredField(phone)) {
            return new BotResponse("Please provide your Contact/Mobile Number.", "ASK_PHONE", "ACTIVE");
        }
        session.setPhoneNumber(phone);
        session.setState("ASK_NEXT_OF_KIN_NAME");
        chatSessionRepository.save(session);
        return new BotResponse("In case of emergency, who is your Next of Kin?", "ASK_NEXT_OF_KIN_NAME", "ACTIVE");
    }

    private BotResponse handleIdNumber(ChatSession session, String input) {
        String id = input == null ? "" : input.trim();
        if (id.isEmpty()) {
            return new BotResponse("Please enter your National ID Number.", "ASK_ID_NUMBER", "ACTIVE");
        }
        session.setIdNumber(id);
        session.setState("ASK_PHONE");
        chatSessionRepository.save(session);
        return new BotResponse("Thank you. What is your Contact Number?", "ASK_PHONE", "ACTIVE");
    }

    private BotResponse handleNextOfKinName(ChatSession session, String input) {
        String name = input == null ? "" : input.trim();
        if (RegistrationFieldValidator.isMissingRequiredField(name)) {
            return new BotResponse("Please provide your Next of Kin name.", "ASK_NEXT_OF_KIN_NAME", "ACTIVE");
        }
        session.setNextOfKinName(name);
        session.setState("ASK_NEXT_OF_KIN_PHONE");
        chatSessionRepository.save(session);
        return new BotResponse("What is their Phone Number?", "ASK_NEXT_OF_KIN_PHONE", "ACTIVE");
    }

    private BotResponse handleNextOfKinPhone(ChatSession session, String input) {
        String phone = input == null ? "" : input.trim();
        if (RegistrationFieldValidator.isMissingRequiredField(phone)) {
            return new BotResponse("Please provide the Next of Kin phone number.", "ASK_NEXT_OF_KIN_PHONE", "ACTIVE");
        }
        session.setNextOfKinPhone(phone);
        session.setState("ASK_FAVORITE_SONG");
        chatSessionRepository.save(session);
        return new BotResponse("What is your Favorite Song?", "ASK_FAVORITE_SONG", "ACTIVE");
    }

    private BotResponse handleFavoriteSong(ChatSession session, String input) {
        String song = input == null ? "" : input.trim();
        if (RegistrationFieldValidator.isMissingRequiredField(song)) {
            return new BotResponse("Please provide your favorite song.", "ASK_FAVORITE_SONG", "ACTIVE");
        }
        session.setFavoriteSong(song);
        session.setState("ASK_FAVORITE_VERSE");
        chatSessionRepository.save(session);
        return new BotResponse("What is your Favorite Bible Verse?", "ASK_FAVORITE_VERSE", "ACTIVE");
    }

    private BotResponse handleFavoriteVerse(ChatSession session, String input) {
        String verse = input == null ? "" : input.trim();
        if (RegistrationFieldValidator.isMissingRequiredField(verse)) {
            return new BotResponse("Please provide your favorite Bible verse.", "ASK_FAVORITE_VERSE", "ACTIVE");
        }
        session.setBibleVerse(verse);
        String missingState = determineFirstMissingRequiredFieldState(session);
        if (missingState != null) {
            session.setState(missingState);
            chatSessionRepository.save(session);
            return new BotResponse(promptForState(missingState), missingState, "ACTIVE");
        }
        createOrUpdatePendingRecordFromSession(session);
        session.setState("COMPLETE");
        session.setStatus("COMPLETE");
        chatSessionRepository.save(session);

        Optional<SoldierRecord> recordOpt = findSessionRecord(session);
        if (recordOpt.isPresent() && recordOpt.get().getStatus() == RecordStatus.VERIFIED) {
            return new BotResponse("Your profile is complete. Thank you.", "COMPLETE", "COMPLETE");
        }
        return new BotResponse(
                "✅ Thank you! All details have been submitted. Verification in progress. Please wait for Admin approval.",
                "COMPLETE",
                "COMPLETE"
        );
    }

    private BotResponse handleId(ChatSession session, String input) {
        if ("skip".equalsIgnoreCase(input.trim())) {
            return new BotResponse("National ID number is required for ages 18 and above. Please provide your ID number.", 
                                 "ASK_ID", "ACTIVE");
        }

        if (input.trim().isEmpty()) {
            return new BotResponse("Please provide your National ID number.", 
                                 "ASK_ID", "ACTIVE");
        }

        session.setIdNumber(input.trim());
        session.setState("ASK_SONG");
        chatSessionRepository.save(session);
        return new BotResponse("Thank you! What is your favorite song? (Type 'skip' if you don't have one)", 
                              "ASK_SONG", "ACTIVE");
    }

    private BotResponse handleSong(ChatSession session, String input) {
        if ("skip".equalsIgnoreCase(input.trim())) {
            session.setFavoriteSong(null);
        } else {
            session.setFavoriteSong(input.trim());
        }

        session.setState("ASK_VERSE");
        chatSessionRepository.save(session);
        return new BotResponse("What is your favorite Bible verse? (Type 'skip' if you don't have one)", 
                              "ASK_VERSE", "ACTIVE");
    }

    private BotResponse handleVerse(ChatSession session, String input) {
        if ("skip".equalsIgnoreCase(input.trim())) {
            session.setBibleVerse(null);
        } else {
            session.setBibleVerse(input.trim());
        }

        session.setState("CONFIRM");
        chatSessionRepository.save(session);

        // Build summary
        String summary = buildSummary(session);
        return new BotResponse(summary, "CONFIRM", "ACTIVE");
    }

    private BotResponse handleConfirm(ChatSession session, String input) {
        if ("1".equals(input.trim()) || "confirm".equalsIgnoreCase(input.trim()) || "yes".equalsIgnoreCase(input.trim())) {
            String missingState = determineFirstMissingRequiredFieldState(session);
            if (missingState != null) {
                session.setState(missingState);
                chatSessionRepository.save(session);
                return new BotResponse(promptForState(missingState), missingState, "ACTIVE");
            }

            // Save to soldier_registration
            SoldierRegistration registration = SoldierRegistration.builder()
                    .corpsId(session.getCorpsId() != null ? String.valueOf(session.getCorpsId()) : null)
                    .firstName(session.getFirstName())
                    .lastName(session.getLastName())
                    .dob(session.getDob())
                    .idNumber(session.getIdNumber())
                    .favoriteSong(session.getFavoriteSong())
                    .bibleVerse(session.getBibleVerse())
                    .build();

            soldierRegistrationRepository.save(registration);

            // Also create record in soldier_records for admin dashboard
            try {
                SoldierRecord record = new SoldierRecord();
                // Use sessionId as waId for chat registrations (prefix with "CHAT-")
                record.setWaId("CHAT-" + session.getSessionId());
                
                // Generate record code with year: HTE-2026-005
                Long seqValue = soldierRecordRepository.getNextRecordSequence();
                int currentYear = Year.now().getValue();
                record.setRecordCode("HTE-" + currentYear + "-" + String.format("%03d", seqValue));
                
                // Set corps name (use fixed corps or default)
                String corpsName = getCorpsName(session.getCorpsId());
                record.setCorpsName(corpsName);
                record.setEnrolledCorpsName(corpsName);
                
                record.setFirstName(session.getFirstName());
                record.setFamilyName(session.getLastName());
                record.setDob(session.getDob());
                record.setAge(session.getAge());
                record.setIdNumber(session.getIdNumber());
                applyRequiredRegistrationFields(record, session);
                record.setStatus(RecordStatus.PENDING); // Candidate pending admin verification
                record.setTemplateType("STANDARD");
                record.setChatSessionId(session.getSessionId()); // Link to chat session
                
                SoldierRecord savedRecord = soldierRecordRepository.save(record);
                session.setRecordId(savedRecord.getId()); // Store record ID for later reference
                log.info("Created soldier record {} for chat session: {}", savedRecord.getId(), session.getSessionId());
            } catch (Exception e) {
                log.error("Failed to create soldier record for chat session: {}", session.getSessionId(), e);
                // Continue even if record creation fails - registration is still saved
            }

            // Update session
            session.setState("COMPLETE");
            session.setStatus("COMPLETE");
            chatSessionRepository.save(session);

            return new BotResponse("✅ Registration received! You are currently a Candidate. An admin must verify your profile before you can register others.", 
                                  "COMPLETE", "COMPLETE");
        } else if ("3".equals(input.trim()) || "cancel".equalsIgnoreCase(input.trim()) || "no".equalsIgnoreCase(input.trim())) {
            return handleCancel(session.getSessionId());
        } else {
            return new BotResponse("Please reply with:\n1 to Confirm and save\n3 to Cancel", 
                                 "CONFIRM", "ACTIVE");
        }
    }

    private BotResponse handleReset(String sessionId) {
        Optional<ChatSession> sessionOpt = chatSessionRepository.findBySessionId(sessionId);
        if (sessionOpt.isPresent()) {
            ChatSession session = sessionOpt.get();
            session.setState("START");
            session.setStatus("ACTIVE");
            session.setCorpsName(DEFAULT_CORPS_NAME);
            session.setFirstName(null);
            session.setLastName(null);
            session.setGender(null);
            session.setDob(null);
            session.setAge(null);
            session.setMaritalStatus(null);
            session.setChildrenCount(null);
            session.setAddress(null);
            session.setPhoneNumber(null);
            session.setIdNumber(null);
            session.setNextOfKinName(null);
            session.setNextOfKinPhone(null);
            session.setFavoriteSong(null);
            session.setBibleVerse(null);
            session.setWard(null);
            session.setBrigade(null);
            session.setDepartment(null);
            session.setRecordId(null);
            session.setPersonImageUploaded(false);
            session.setCertImageUploaded(false);
            session.setFamilyMode(false);
            session.setOriginalRegistrantId(null);
            session.setFamilyRelationType(null);
            session.setAskMaritalAfterAddress(false);
            session.setLastNotifiedStep(null);
            chatSessionRepository.save(session);
        }

        saveMessage(sessionId, "BOT", "Registration reset. Type 'start' to begin again.");
        return new BotResponse("Registration reset. Type 'start' to begin again.", "START", "ACTIVE");
    }

    private BotResponse handleCancel(String sessionId) {
        Optional<ChatSession> sessionOpt = chatSessionRepository.findBySessionId(sessionId);
        if (sessionOpt.isPresent()) {
            ChatSession session = sessionOpt.get();
            session.setState("CANCELLED");
            session.setStatus("CANCELLED");
            chatSessionRepository.save(session);
        }

        saveMessage(sessionId, "BOT", "Cancelled. Type 'start' to begin again.");
        return new BotResponse("Cancelled. Type 'start' to begin again.", "CANCELLED", "CANCELLED");
    }

    private void saveMessage(String sessionId, String sender, String messageText) {
        ChatMessage message = new ChatMessage();
        message.setSessionId(sessionId);
        message.setSender(sender);
        message.setMessageText(messageText);
        chatMessageRepository.save(message);
    }

    private boolean isValidName(String name) {
        if (name == null || name.trim().isEmpty()) {
            return false;
        }
        String trimmed = name.trim();
        // Must be at least 2 characters, only letters/spaces/hyphens/apostrophes
        // Reject if it looks like an ID (contains 6+ digits)
        if (trimmed.length() < 2) {
            return false;
        }
        if (ID_PATTERN.matcher(trimmed).matches()) {
            return false;
        }
        return NAME_PATTERN.matcher(trimmed).matches();
    }

    private LocalDate parseDate(String input) {
        if (input == null || input.trim().isEmpty()) {
            return null;
        }

        String trimmed = input.trim();
        DateTimeFormatter[] formatters = {
            DateTimeFormatter.ofPattern("dd/MM/yyyy"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd"),
            DateTimeFormatter.ofPattern("d/M/yyyy"),
            DateTimeFormatter.ofPattern("yyyy-M-d")
        };

        for (DateTimeFormatter formatter : formatters) {
            try {
                return LocalDate.parse(trimmed, formatter);
            } catch (DateTimeParseException e) {
                // Try next format
            }
        }

        return null;
    }

    private int calculateAge(LocalDate dob) {
        return Period.between(dob, LocalDate.now()).getYears();
    }

    private Integer parseChildrenCount(String input) {
        if (input == null) {
            return null;
        }
        String trimmed = input.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        try {
            int value = Integer.parseInt(trimmed);
            return value < 0 ? null : value;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String buildSummary(ChatSession session) {
        StringBuilder sb = new StringBuilder();
        sb.append("Please review your registration:\n\n");
        sb.append("First Name: ").append(session.getFirstName()).append("\n");
        sb.append("Last Name: ").append(session.getLastName()).append("\n");
        sb.append("Date of Birth: ").append(session.getDob()).append("\n");
        sb.append("Age: ").append(session.getAge()).append("\n");
        if (session.getIdNumber() != null) {
            sb.append("ID Number: ").append(session.getIdNumber()).append("\n");
        }
        if (session.getFavoriteSong() != null) {
            sb.append("Favorite Song: ").append(session.getFavoriteSong()).append("\n");
        } else {
            sb.append("Favorite Song: (not provided)\n");
        }
        if (session.getBibleVerse() != null) {
            sb.append("Favorite Bible Verse: ").append(session.getBibleVerse()).append("\n");
        } else {
            sb.append("Favorite Bible Verse: (not provided)\n");
        }
        sb.append("\nReply:\n1 to Confirm and save\n3 to Cancel");
        return sb.toString();
    }

    private String getCorpsName(Integer corpsId) {
        return DEFAULT_CORPS_NAME;
    }

    private Optional<SoldierRecord> findSessionRecord(ChatSession session) {
        if (session.getRecordId() != null) {
            Optional<SoldierRecord> byId = soldierRecordRepository.findById(session.getRecordId());
            if (byId.isPresent()) {
                return byId;
            }
        }
        if (session.getSessionId() != null) {
            return soldierRecordRepository.findByChatSessionId(session.getSessionId());
        }
        return Optional.empty();
    }

    private String determineNextMissingState(SoldierRecord r) {
        if (r.getStatus() == RecordStatus.VERIFIED) {
            return determinePostApprovalMissingState(r);
        }
        return determinePreApprovalMissingState(r);
    }

    private String determinePreApprovalMissingState(SoldierRecord r) {
        if (isBlank(r.getFirstName())) return "ASK_FIRST_NAME";
        if (isBlank(r.getFamilyName())) return "ASK_LAST_NAME";
        if (isBlank(r.getGender())) return "ASK_GENDER";
        if (r.getDob() == null) return "ASK_DOB";
        if (r.getKidsCount() == null) return "ASK_CHILDREN_COUNT";
        if (isBlank(r.getAddress())) return "ASK_ADDRESS";
        if (isBlank(r.getCorpsName())) {
            r.setCorpsName(DEFAULT_CORPS_NAME);
            r.setEnrolledCorpsName(DEFAULT_CORPS_NAME);
            soldierRecordRepository.save(r);
        }
        if (isBlank(r.getWard())) return "ASK_WARD";
        if (isBlank(r.getBrigade())) return "ASK_BRIGADE";
        if (isBlank(r.getDepartment())) return "ASK_DEPARTMENT";
        if (isBlank(r.getPersonImagePath())) return "ASK_PERSON_IMAGE";
        if (isBlank(r.getCertImagePath())) return "ASK_CERT_IMAGE";
        if (isBlank(r.getPhoneNumber())) return "ASK_PHONE";
        if (isBlank(r.getNextOfKinName())) return "ASK_NEXT_OF_KIN_NAME";
        if (isBlank(r.getNextOfKinPhone())) return "ASK_NEXT_OF_KIN_PHONE";
        if (isBlank(r.getFavoriteSong())) return "ASK_FAVORITE_SONG";
        if (isBlank(r.getFavoriteBibleVerse())) return "ASK_FAVORITE_VERSE";
        return "COMPLETE";
    }

    private String determinePostApprovalMissingState(SoldierRecord r) {
        if (r.getAge() != null && r.getAge() >= 18 && isBlank(r.getIdNumber())) return "ASK_ID_NUMBER";
        if (isBlank(r.getPhoneNumber())) return "ASK_PHONE";
        if (isBlank(r.getNextOfKinName())) return "ASK_NEXT_OF_KIN_NAME";
        if (isBlank(r.getNextOfKinPhone())) return "ASK_NEXT_OF_KIN_PHONE";
        if (isBlank(r.getFavoriteSong())) return "ASK_FAVORITE_SONG";
        if (isBlank(r.getFavoriteBibleVerse())) return "ASK_FAVORITE_VERSE";
        return "COMPLETE";
    }

    private String promptForState(String state) {
        return switch (state) {
            case "ASK_FIRST_NAME" -> "What is your First Name?";
            case "ASK_LAST_NAME" -> "And your Surname?";
            case "ASK_CORPS" -> "What is your Ward?";
            case "ASK_GENDER" -> "Are you Male or Female?";
            case "ASK_DOB" -> "What is your Date of Birth? (Please use DD/MM/YYYY format)";
            case "ASK_CHILDREN_COUNT" -> "How many children do you have? (If none, type 0)";
            case "ASK_ADDRESS" -> "What is your Home Address?";
            case "ASK_PHONE" -> "What is your best Contact/Mobile Number?";
            case "ASK_ID_NUMBER" -> "Please enter your National ID Number.";
            case "ASK_NEXT_OF_KIN_NAME" -> "Who is your Next of Kin?";
            case "ASK_NEXT_OF_KIN_PHONE" -> "What is their Phone Number?";
            case "ASK_FAVORITE_SONG" -> "What is your Favorite Song?";
            case "ASK_FAVORITE_VERSE" -> "What is your Favorite Bible Verse?";
            case "ASK_WARD" -> "What is your Ward?";
            case "ASK_BRIGADE" -> "What is your Brigade?";
            case "ASK_DEPARTMENT" -> "Which fellowship/department are you in? (Youth, Men, Home League)";
            case "ASK_PERSON_IMAGE" -> "Perfect! Last step: Please upload your personal photo.";
            case "ASK_CERT_IMAGE" -> "Please upload your Enrollment Certificate or ID Card image for verification.";
            case "ASK_FAMILY_RELATION_TYPE" -> "Are you registering a Family Member or a Friend?";
            default -> "Your registration is already complete.";
        };
    }

    private boolean isBlank(String value) {
        return RegistrationFieldValidator.isMissingRequiredField(value);
    }

    private String determineFirstMissingRequiredFieldState(ChatSession session) {
        if (RegistrationFieldValidator.isMissingRequiredField(session.getPhoneNumber())) return "ASK_PHONE";
        if (RegistrationFieldValidator.isMissingRequiredField(session.getNextOfKinName())) return "ASK_NEXT_OF_KIN_NAME";
        if (RegistrationFieldValidator.isMissingRequiredField(session.getNextOfKinPhone())) return "ASK_NEXT_OF_KIN_PHONE";
        if (RegistrationFieldValidator.isMissingRequiredField(session.getFavoriteSong())) return "ASK_FAVORITE_SONG";
        if (RegistrationFieldValidator.isMissingRequiredField(session.getBibleVerse())) return "ASK_FAVORITE_VERSE";
        return null;
    }

    private void applyRequiredRegistrationFields(SoldierRecord record, ChatSession session) {
        record.setPhoneNumber(RegistrationFieldValidator.sanitizeRequiredField(session.getPhoneNumber()));
        record.setNextOfKinName(RegistrationFieldValidator.sanitizeRequiredField(session.getNextOfKinName()));
        record.setNextOfKinPhone(RegistrationFieldValidator.sanitizeRequiredField(session.getNextOfKinPhone()));
        record.setFavoriteSong(RegistrationFieldValidator.sanitizeRequiredField(session.getFavoriteSong()));
        record.setFavoriteBibleVerse(RegistrationFieldValidator.sanitizeRequiredField(session.getBibleVerse()));
    }

    private void createOrUpdatePendingRecordFromSession(ChatSession session) {
        String corpsName = DEFAULT_CORPS_NAME;

        String genderNorm = MembershipClassifier.normalizeGender(session.getGender());
        String maritalNorm = MembershipClassifier.normalizeMarital(session.getMaritalStatus());
        int age = session.getAge() == null ? 0 : session.getAge();
        int kids = session.getChildrenCount() == null ? 0 : session.getChildrenCount();
        String department = !isBlank(session.getDepartment())
                ? session.getDepartment().trim()
                : MembershipClassifier.classifyDepartment(age, genderNorm, maritalNorm, kids);
        String brigadeEligibility = MembershipClassifier.classifyBrigadeEligibility(age);

        SoldierRecord record;
        if (session.getRecordId() != null) {
            record = soldierRecordRepository.findById(session.getRecordId()).orElse(new SoldierRecord());
        } else {
            record = new SoldierRecord();
            record.setWaId("CHAT-" + session.getSessionId());
            Long seqValue = soldierRecordRepository.getNextRecordSequence();
            int currentYear = Year.now().getValue();
            record.setRecordCode("HTE-" + currentYear + "-" + String.format("%03d", seqValue));
            record.setTemplateType("STANDARD");
            record.setChatSessionId(session.getSessionId());
            record.setStatus(RecordStatus.PENDING);
        }

        record.setCorpsName(corpsName);
        record.setEnrolledCorpsName(corpsName);
        record.setFirstName(session.getFirstName());
        record.setFamilyName(session.getLastName());
        record.setGender(genderNorm);
        record.setDob(session.getDob());
        record.setAge(session.getAge());
        record.setMaritalStatus(maritalNorm);
        record.setKidsCount(kids);
        applyRequiredRegistrationFields(record, session);
        record.setAddress(session.getAddress());
        record.setIdNumber(session.getIdNumber());
        record.setWard(session.getWard());
        record.setBrigade(session.getBrigade());
        record.setDepartment(department);
        record.setBrigadeEligibility(brigadeEligibility);
        if (Boolean.TRUE.equals(session.getFamilyMode()) && session.getOriginalRegistrantId() != null) {
            record.setPrimaryRegistrantId(session.getOriginalRegistrantId());
            record.setRegistrationRelation(session.getFamilyRelationType() != null ? session.getFamilyRelationType() : "Family");
        }
        if (record.getStatus() != RecordStatus.VERIFIED) {
            record.setStatus(RecordStatus.PENDING);
        }

        SoldierRecord savedRecord = soldierRecordRepository.save(record);
        session.setRecordId(savedRecord.getId());

        SoldierRegistration registration = SoldierRegistration.builder()
                .corpsId(session.getCorpsId() != null ? String.valueOf(session.getCorpsId()) : null)
                .corpsName(corpsName)
                .firstName(session.getFirstName())
                .lastName(session.getLastName())
                .gender(genderNorm)
                .dob(session.getDob())
                .maritalStatus(maritalNorm)
                .numberOfChildren(kids)
                .address(session.getAddress())
                .idNumber(session.getIdNumber())
                .favoriteSong(RegistrationFieldValidator.sanitizeRequiredField(session.getFavoriteSong()))
                .bibleVerse(RegistrationFieldValidator.sanitizeRequiredField(session.getBibleVerse()))
                .build();
        soldierRegistrationRepository.save(registration);
    }

    // Post-verification handlers
    private BotResponse checkForVerification(ChatSession session) {
        Optional<SoldierRecord> recordOpt = Optional.empty();

        // Check if record was verified
        if (session.getRecordId() != null) {
            recordOpt = soldierRecordRepository.findById(session.getRecordId());
        }

        // Fallback: recover record linkage by chatSessionId
        if (recordOpt.isEmpty() && session.getSessionId() != null) {
            recordOpt = soldierRecordRepository.findByChatSessionId(session.getSessionId());
            recordOpt.ifPresent(record -> {
                session.setRecordId(record.getId());
                chatSessionRepository.save(session);
            });
        }

        if (recordOpt.isPresent()) {
            SoldierRecord record = recordOpt.get();
            if (record.getStatus() == RecordStatus.REUPLOAD_REQUIRED || Boolean.TRUE.equals(record.getNeedsReupload())) {
                session.setState("ASK_CERT_IMAGE");
                session.setStatus("ACTIVE");
                session.setLastNotifiedStep("ASK_CERT_IMAGE");
                chatSessionRepository.save(session);
                String displayName = Arrays.asList(record.getFirstName(), record.getFamilyName())
                        .stream()
                        .filter(s -> s != null && !s.isBlank())
                        .collect(Collectors.joining(" "));
                String reason = (record.getPhotoReviewNotes() != null && !record.getPhotoReviewNotes().isBlank())
                        ? record.getPhotoReviewNotes().trim()
                        : (record.getHouseholdAdminNotes() != null && !record.getHouseholdAdminNotes().isBlank()
                            ? record.getHouseholdAdminNotes().trim()
                            : "The uploaded image was unclear.");
                String prompt = String.format(
                        "Hi %s, Admin requested a new photo. Reason: %s Please upload your certificate again below.",
                        (displayName == null || displayName.isBlank()) ? "member" : displayName,
                        reason
                );
                return new BotResponse(prompt, "ASK_CERT_IMAGE", "ACTIVE");
            }
            if (record.getStatus() == RecordStatus.PENDING || record.getStatus() == RecordStatus.IN_PROGRESS) {
                String preApprovalNext = determinePreApprovalMissingState(record);
                if (!"COMPLETE".equals(preApprovalNext)) {
                    session.setState(preApprovalNext);
                    session.setStatus("ACTIVE");
                    chatSessionRepository.save(session);
                    return new BotResponse(
                            "Your profile is incomplete. " + promptForState(preApprovalNext),
                            preApprovalNext,
                            "ACTIVE");
                }
            }
            if (record.getStatus() == RecordStatus.VERIFIED) {
                String postApprovalNext = determinePostApprovalMissingState(record);
                if (!"COMPLETE".equals(postApprovalNext)) {
                    session.setState(postApprovalNext);
                    session.setStatus("ACTIVE");
                    chatSessionRepository.save(session);
                    return new BotResponse(
                            "Your profile is incomplete. " + promptForState(postApprovalNext),
                            postApprovalNext,
                            "ACTIVE");
                }
                if (!"VERIFIED_NOTIFICATION".equals(session.getState())) {
                    return notifyChatOfVerification(session, record);
                }
                return new BotResponse("Your profile is fully complete. Thank you.", "COMPLETE", "COMPLETE");
            }
            if (record.getStatus() == RecordStatus.DECLINED) {
                String reason = (record.getHouseholdAdminNotes() != null && !record.getHouseholdAdminNotes().trim().isEmpty())
                        ? record.getHouseholdAdminNotes().trim()
                        : "No specific reason provided. Please contact the Corps Secretary.";
                String message = "Your registration was not approved at this time.\nReason: " + reason
                        + "\nWould you like to re-submit your photo or edit your details?";
                return new BotResponse(message, "COMPLETE", "DECLINED", reason);
            }
        }
        return new BotResponse("Verification in progress. Please wait for Admin approval.", 
                              "COMPLETE", "COMPLETE");
    }

    private BotResponse notifyChatOfVerification(ChatSession session, SoldierRecord record) {
        session.setState("VERIFIED_NOTIFICATION");
        session.setLastNotifiedStep("VERIFIED_NOTIFICATION_SENT");
        chatSessionRepository.save(session);
        
        String message = String.format(
            "🎉 Congratulations! Your registration has been verified by the Admin. Let's finish the final part of your profile.\n\n" +
            "Your Record ID: %s",
            record.getRecordCode()
        );
        
        saveMessage(session.getSessionId(), "BOT", message);
        return new BotResponse(message, "VERIFIED_NOTIFICATION", "ACTIVE");
    }

    private BotResponse handleVerifiedNotification(ChatSession session, String input) {
        Optional<SoldierRecord> recordOpt = findSessionRecord(session);
        if (recordOpt.isEmpty()) {
            session.setState("ASK_ID_NUMBER");
            session.setLastNotifiedStep("ASK_ID_NUMBER");
            chatSessionRepository.save(session);
            return new BotResponse("To complete your official file, what is your National ID Number?", "ASK_ID_NUMBER", "ACTIVE");
        }

        String nextState = determineNextMissingState(recordOpt.get());
        if ("COMPLETE".equals(nextState)) {
            session.setState("COMPLETE");
            session.setLastNotifiedStep("COMPLETE");
            chatSessionRepository.save(session);
            return new BotResponse(
                    "Your profile is complete. Would you like to register another person (Family Member/Child)?",
                    "COMPLETE",
                    "COMPLETE",
                    null,
                    Arrays.asList("Register Someone Else", "Finish")
            );
        }

        session.setState(nextState);
        session.setLastNotifiedStep(nextState);
        chatSessionRepository.save(session);
        return new BotResponse(promptForState(nextState), nextState, "ACTIVE");
    }

    private BotResponse handleWard(ChatSession session, String input) {
        if (input == null || input.trim().isEmpty()) {
            return new BotResponse("Please provide your Ward.", "ASK_WARD", "ACTIVE");
        }
        
        session.setWard(input.trim());
        session.setState("ASK_BRIGADE");
        chatSessionRepository.save(session);
        
        // Update record
        if (session.getRecordId() != null) {
            Optional<SoldierRecord> recordOpt = soldierRecordRepository.findById(session.getRecordId());
            if (recordOpt.isPresent()) {
                SoldierRecord record = recordOpt.get();
                record.setWard(input.trim());
                soldierRecordRepository.save(record);
            }
        }
        
        return new BotResponse("What is your Brigade?", "ASK_BRIGADE", "ACTIVE");
    }

    private BotResponse handleBrigade(ChatSession session, String input) {
        if (input == null || input.trim().isEmpty()) {
            return new BotResponse("Please provide your Brigade.", "ASK_BRIGADE", "ACTIVE");
        }
        
        session.setBrigade(input.trim());
        session.setState("ASK_DEPARTMENT");
        chatSessionRepository.save(session);

        return new BotResponse("Which fellowship/department are you in? (Youth, Men, Home League)",
                              "ASK_DEPARTMENT", "ACTIVE");
    }

    private BotResponse handleDepartment(ChatSession session, String input) {
        if (input == null || input.trim().isEmpty()) {
            return new BotResponse("Please provide your fellowship/department (Youth, Men, Home League).", "ASK_DEPARTMENT", "ACTIVE");
        }
        session.setDepartment(input.trim());
        session.setState("ASK_PERSON_IMAGE");
        createOrUpdatePendingRecordFromSession(session);
        chatSessionRepository.save(session);

        return new BotResponse("Perfect! Last step: Please upload your personal photo. Click the paperclip icon below to attach a photo.",
                "ASK_PERSON_IMAGE", "ACTIVE");
    }

    private BotResponse handleFamilySameSurname(ChatSession session, String input) {
        String normalized = input == null ? "" : input.trim().toLowerCase();
        if (normalized.startsWith("y")) {
            session.setState("ASK_FAMILY_FIRST_NAME");
            chatSessionRepository.save(session);
            return new BotResponse("What is their First Name?", "ASK_FAMILY_FIRST_NAME", "ACTIVE");
        }
        if (normalized.startsWith("n")) {
            session.setState("ASK_LAST_NAME");
            chatSessionRepository.save(session);
            return new BotResponse("Please enter their Surname.", "ASK_LAST_NAME", "ACTIVE");
        }
        return new BotResponse("Please choose Yes or No.", "ASK_FAMILY_SAME_SURNAME", "ACTIVE", null, Arrays.asList("Yes", "No"));
    }

    private BotResponse handleFamilyFirstName(ChatSession session, String input) {
        if (!isValidName(input)) {
            return new BotResponse("Please enter a valid first name.", "ASK_FAMILY_FIRST_NAME", "ACTIVE");
        }
        session.setFirstName(input.trim());
        session.setState("ASK_GENDER");
        chatSessionRepository.save(session);
        return new BotResponse("Are they Male or Female?", "ASK_GENDER", "ACTIVE");
    }

    private BotResponse handleFamilySameAddress(ChatSession session, String input) {
        String normalized = input == null ? "" : input.trim().toLowerCase();
        if (normalized.startsWith("y")) {
            session.setAskMaritalAfterAddress(false);
            session.setCorpsName(DEFAULT_CORPS_NAME);
            session.setState("ASK_WARD");
            chatSessionRepository.save(session);
            return new BotResponse("What is their Ward?", "ASK_WARD", "ACTIVE");
        }
        if (normalized.startsWith("n")) {
            session.setAskMaritalAfterAddress(true);
            session.setState("ASK_ADDRESS");
            chatSessionRepository.save(session);
            return new BotResponse("Please enter the new Home Address.", "ASK_ADDRESS", "ACTIVE");
        }
        return new BotResponse("Please choose Yes or No.", "ASK_FAMILY_SAME_ADDRESS", "ACTIVE", null, Arrays.asList("Yes", "No"));
    }

    private BotResponse handleFamilySameCorps(ChatSession session, String input) {
        session.setCorpsName(DEFAULT_CORPS_NAME);
        session.setState("ASK_WARD");
        chatSessionRepository.save(session);
        return new BotResponse("What is their Ward?", "ASK_WARD", "ACTIVE");
    }

    private BotResponse handlePersonImage(ChatSession session, String input) {
        // Accept text acknowledgment, but image should be uploaded via /api/bot/upload-image
        if (input != null && !input.trim().isEmpty() && !input.toLowerCase().contains("skip")) {
            // If user sends text, remind them to upload image
            return new BotResponse("Please upload your personal photo using the image upload button. Click the 📷 icon or drag and drop an image.", 
                              "ASK_PERSON_IMAGE", "ACTIVE");
        }
        return new BotResponse("Please upload your personal photo. Click the image icon or send an image.", 
                              "ASK_PERSON_IMAGE", "ACTIVE");
    }

    private BotResponse handleCertImage(ChatSession session, String input) {
        // Accept text acknowledgment, but image should be uploaded via /api/bot/upload-image
        if (input != null && !input.trim().isEmpty() && !input.toLowerCase().contains("skip")) {
            // If user sends text, remind them to upload image
            return new BotResponse("Please upload your certificate image using the image upload button. Click the 📷 icon or drag and drop an image.", 
                              "ASK_CERT_IMAGE", "ACTIVE");
        }
        return new BotResponse("Please upload your certificate image. Click the image icon or send an image.", 
                              "ASK_CERT_IMAGE", "ACTIVE");
    }

    /**
     * Handle image upload from chat
     */
    @Transactional
    public BotResponse handleImageUpload(String sessionId, MultipartFile file, String imageType) throws IOException {
        Optional<ChatSession> sessionOpt = chatSessionRepository.findBySessionId(sessionId);
        if (sessionOpt.isEmpty()) {
            return new BotResponse("Session not found. Please start a new registration.", "ERROR", "ERROR");
        }

        ChatSession session = sessionOpt.get();
        
        // Validate state
        if (!"ASK_PERSON_IMAGE".equals(session.getState()) && !"ASK_CERT_IMAGE".equals(session.getState())) {
            return new BotResponse("Image upload is not expected at this stage. Please follow the conversation flow.", 
                                  session.getState(), session.getStatus());
        }

        // Save file
        String filename = fileService.saveFile(file);
        
        // Update record
        if (session.getRecordId() != null) {
            Optional<SoldierRecord> recordOpt = soldierRecordRepository.findById(session.getRecordId());
            if (recordOpt.isPresent()) {
                SoldierRecord record = recordOpt.get();
                if ("person".equals(imageType) && "ASK_PERSON_IMAGE".equals(session.getState())) {
                    record.setPersonImagePath(filename);
                    record.setPhotoStatus(SoldierRecord.PhotoStatus.UPLOADED);
                    session.setPersonImageUploaded(true);
                    session.setState("ASK_CERT_IMAGE");
                    chatSessionRepository.save(session);
                    soldierRecordRepository.save(record);
                    
                    saveMessage(sessionId, "USER", "[Uploaded personal photo]");
                    saveMessage(sessionId, "BOT", "Thank you! Personal photo uploaded successfully. Please now upload your Enrollment Certificate or ID Card image.");

                    BotResponse response = new BotResponse(
                            "✅ Photo uploaded successfully.\n\nPlease upload your Enrollment Certificate or ID Card image to continue.",
                            "ASK_CERT_IMAGE",
                            "ACTIVE"
                    );
                    response.setPhotoPath("/api/images/" + filename);
                    return response;
                }
                if ("cert".equals(imageType) && "ASK_CERT_IMAGE".equals(session.getState())) {
                    record.setCertImagePath(filename);
                    record.setNeedsReupload(false);
                    if (record.getStatus() == RecordStatus.REUPLOAD_REQUIRED) {
                        record.setStatus(RecordStatus.PENDING);
                    }
                    session.setCertImageUploaded(true);
                    session.setState("ASK_PHONE");
                    session.setStatus("ACTIVE");
                    chatSessionRepository.save(session);
                    soldierRecordRepository.save(record);

                    saveMessage(sessionId, "USER", "[Uploaded certificate image]");
                    saveMessage(sessionId, "BOT", "Thank you! Certificate uploaded. What is your best Contact/Mobile Number?");
                    BotResponse response = new BotResponse(
                            "✅ Certificate uploaded successfully!\n\nWhat is your best Contact/Mobile Number?",
                            "ASK_PHONE",
                            "ACTIVE"
                    );
                    response.setPhotoPath("/api/images/" + filename);
                    return response;
                }
            }
        }

        return new BotResponse("Image uploaded, but could not update record. Please contact support.", 
                              session.getState(), session.getStatus());
    }

    /**
     * Public method to be called from RecordService when status changes to VERIFIED
     */
    @Transactional
    public void notifyUserOfVerification(UUID recordId) {
        Optional<SoldierRecord> recordOpt = soldierRecordRepository.findById(recordId);
        if (recordOpt.isEmpty()) {
            log.warn("Record not found for verification notification: {}", recordId);
            return;
        }
        
        SoldierRecord record = recordOpt.get();
        if (record.getChatSessionId() == null) {
            log.debug("Record {} has no chat session, skipping notification", recordId);
            return;
        }
        
        Optional<ChatSession> sessionOpt = chatSessionRepository.findBySessionId(record.getChatSessionId());
        if (sessionOpt.isEmpty()) {
            log.warn("Chat session not found for record: {}", recordId);
            return;
        }
        
        ChatSession session = sessionOpt.get();
        session.setRecordId(recordId);
        
        // Only notify if not already notified
        if (!"VERIFIED_NOTIFICATION".equals(session.getState()) && 
            !"ASK_WARD".equals(session.getState()) && 
            !"ASK_BRIGADE".equals(session.getState()) &&
            !"ASK_PERSON_IMAGE".equals(session.getState()) &&
            !"ASK_CERT_IMAGE".equals(session.getState())) {
            
            notifyChatOfVerification(session, record);
            log.info("Notified chat session {} of verification for record {}", session.getSessionId(), recordId);
        }
    }

    @Transactional
    public boolean requestCertificateUpload(UUID recordId) {
        Optional<SoldierRecord> recordOpt = soldierRecordRepository.findById(recordId);
        if (recordOpt.isEmpty()) {
            return false;
        }
        SoldierRecord record = recordOpt.get();
        if (record.getChatSessionId() == null || record.getChatSessionId().trim().isEmpty()) {
            return false;
        }
        Optional<ChatSession> sessionOpt = chatSessionRepository.findBySessionId(record.getChatSessionId());
        if (sessionOpt.isEmpty()) {
            return false;
        }
        ChatSession session = sessionOpt.get();
        session.setRecordId(record.getId());
        session.setState("ASK_CERT_IMAGE");
        session.setStatus("ACTIVE");
        session.setLastNotifiedStep("ASK_CERT_IMAGE");
        chatSessionRepository.save(session);
        saveMessage(session.getSessionId(), "BOT", "Please upload your Enrollment Certificate or ID Card image for verification.");
        return true;
    }

    // Response DTO
    public static class BotResponse {
        private String replyText;
        private String state;
        private String status;
        private String declineReason;
        private List<String> choices;
        private String photoPath;
        private String memberStatus;
        private String memberFirstName;
        private String memberLastName;
        private String memberRecordCode;
        private String memberDepartment;
        private String personImagePath;

        public BotResponse(String replyText, String state, String status) {
            this.replyText = replyText;
            this.state = state;
            this.status = status;
            this.declineReason = null;
            this.choices = null;
            this.photoPath = null;
        }

        public BotResponse(String replyText, String state, String status, String declineReason) {
            this.replyText = replyText;
            this.state = state;
            this.status = status;
            this.declineReason = declineReason;
            this.choices = null;
            this.photoPath = null;
        }

        public BotResponse(String replyText, String state, String status, String declineReason, List<String> choices) {
            this.replyText = replyText;
            this.state = state;
            this.status = status;
            this.declineReason = declineReason;
            this.choices = choices;
            this.photoPath = null;
        }

        public String getReplyText() {
            return replyText;
        }

        public String getState() {
            return state;
        }

        public String getStatus() {
            return status;
        }

        public String getDeclineReason() {
            return declineReason;
        }

        public List<String> getChoices() {
            return choices;
        }

        public String getPhotoPath() {
            return photoPath;
        }

        public void setPhotoPath(String photoPath) {
            this.photoPath = photoPath;
        }

        public String getMemberStatus() {
            return memberStatus;
        }

        public void setMemberStatus(String memberStatus) {
            this.memberStatus = memberStatus;
        }

        public String getMemberFirstName() {
            return memberFirstName;
        }

        public void setMemberFirstName(String memberFirstName) {
            this.memberFirstName = memberFirstName;
        }

        public String getMemberLastName() {
            return memberLastName;
        }

        public void setMemberLastName(String memberLastName) {
            this.memberLastName = memberLastName;
        }

        public String getMemberRecordCode() {
            return memberRecordCode;
        }

        public void setMemberRecordCode(String memberRecordCode) {
            this.memberRecordCode = memberRecordCode;
        }

        public String getMemberDepartment() {
            return memberDepartment;
        }

        public void setMemberDepartment(String memberDepartment) {
            this.memberDepartment = memberDepartment;
        }

        public String getPersonImagePath() {
            return personImagePath;
        }

        public void setPersonImagePath(String personImagePath) {
            this.personImagePath = personImagePath;
        }
    }
}
