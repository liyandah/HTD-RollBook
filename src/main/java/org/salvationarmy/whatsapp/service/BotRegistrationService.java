package org.salvationarmy.whatsapp.service;

import lombok.extern.slf4j.Slf4j;
import org.salvationarmy.whatsapp.dto.ChatMessageResponse;
import org.salvationarmy.whatsapp.dto.QuickReplyOption;
import org.salvationarmy.whatsapp.entity.*;
import org.salvationarmy.whatsapp.repository.*;
import org.salvationarmy.whatsapp.util.PhoneNumberUtil;
import org.salvationarmy.whatsapp.util.MembershipClassifier;
import org.salvationarmy.whatsapp.util.RegistrationFieldValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.Year;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Service to handle bot registration flow with locked sequence matching admin dashboard format
 * Backend controls 100% of the flow - frontend only sends messages and renders replies
 */
@Service
@Slf4j
public class BotRegistrationService {
    
    @Autowired
    private RegistrationProfileRepository registrationProfileRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private MessageRepository messageRepository;
    
    @Autowired
    private ConversationNewRepository conversationRepository;
    
    @Autowired
    private ProcessedMessageRepository processedMessageRepository;
    
    @Autowired
    private SoldierRecordRepository soldierRecordRepository;
    
    @Autowired
    private org.salvationarmy.whatsapp.service.FileService fileService;
    
    @Autowired
    private PhotoManagementService photoManagementService;
    
    private static final String DEFAULT_CORPS_NAME = "Highfield Temple";

    @Value("${bot.fixed-corps-id:Highfield Temple}")
    private String fixedCorpsName;
    
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DATE_FORMATTER_SLASH = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    
    /**
     * Process a chat message with registration flow (locked sequence)
     */
    @Transactional
    public ChatMessageResponse processChatMessage(UUID conversationId, UUID userId, String content, UUID clientMessageId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        // Check idempotency if clientMessageId is provided
        if (clientMessageId != null) {
            Optional<ProcessedMessage> existing = processedMessageRepository
                    .findByUserIdAndClientMessageId(userId, clientMessageId);
            if (existing.isPresent()) {
                log.warn("[BotRegistration] Duplicate message detected for userId={}, clientMessageId={}, returning cached response (NOT sending new bot message)", 
                        userId, clientMessageId);
                // Return cached response WITHOUT sending a new bot message
                ChatMessageResponse response = new ChatMessageResponse();
                response.setBotReply(existing.get().getResponseContent());
                response.setRegistrationStep(getCurrentStep(userId));
                // Use a placeholder message ID - frontend should not add this as a new message
                response.setMessageId(null);
                return response;
            }
        }
        
        // Get or create registration profile
        RegistrationProfile profile = registrationProfileRepository.findByUserId(userId)
                .orElseGet(() -> {
                    RegistrationProfile newProfile = new RegistrationProfile();
                    newProfile.setUserId(userId);
                    newProfile.setStatus(RegistrationProfile.RegistrationStatus.DRAFT);
                    newProfile.setRegistrationStep("ASK_PHONE");
                    return registrationProfileRepository.save(newProfile);
                });
        
        String currentStep = profile.getRegistrationStep() != null 
                ? profile.getRegistrationStep() 
                : "ASK_PHONE";

        String message = content == null ? "" : content.trim();
        String lowerMessage = message.toLowerCase();

        Optional<SoldierRecord> primaryVerifiedOpt = primaryVerifiedSelfRecord(userId);

        if (primaryVerifiedOpt.isPresent()) {
            SoldierRecord record = primaryVerifiedOpt.get();
            SoldierRecord.PhotoStatus photoStatus = record.getPhotoStatus() != null 
                    ? record.getPhotoStatus() 
                    : SoldierRecord.PhotoStatus.MISSING;
            boolean bypassPhotoWallForIntent = "COMPLETED".equals(currentStep)
                    && photoStatus == SoldierRecord.PhotoStatus.MISSING
                    && (lowerMessage.contains("register") || lowerMessage.contains("profile"));
            if ((photoStatus == SoldierRecord.PhotoStatus.RESUBMIT_REQUESTED ||
                    photoStatus == SoldierRecord.PhotoStatus.REJECTED ||
                    (photoStatus == SoldierRecord.PhotoStatus.MISSING && "COMPLETED".equals(currentStep)))
                    && !bypassPhotoWallForIntent) {
                String photoRequestMessage = buildPhotoRequestMessage(record, photoStatus);
                Message botMessage = sendBotMessage(conversationId, photoRequestMessage);
                ChatMessageResponse chatResponse = new ChatMessageResponse();
                chatResponse.setBotReply(photoRequestMessage);
                chatResponse.setRegistrationStep("ASK_PERSON_IMAGE");
                chatResponse.setMessageId(botMessage.getId());
                profile.setRegistrationStep("ASK_PERSON_IMAGE");
                registrationProfileRepository.save(profile);
                return chatResponse;
            }
        }
        
        String response = "";
        String nextStep = currentStep;
        List<String> quickReplies = null;
        List<QuickReplyOption> quickReplyOptions = null;
        
        log.info("[BotRegistration] Processing message: userId={}, step={}, input='{}', clientMessageId={}", 
                userId, currentStep, message, clientMessageId);
        
        // Handle commands (allowed anytime)
        if (lowerMessage.equals("restart")) {
            return handleRestart(conversationId, userId, profile);
        } else if (lowerMessage.equals("status")) {
            return handleStatus(conversationId, userId, profile, user);
        } else if (lowerMessage.startsWith("edit ")) {
            return handleEdit(conversationId, userId, profile, message);
        }

        boolean menuActionHandled = false;
        String trimmedMsg = message.trim();
        if ("__CHAT_ACTION_proxy_reg__".equalsIgnoreCase(trimmedMsg)) {
            menuActionHandled = true;
            if (!Boolean.TRUE.equals(user.getIsVerified())) {
                nextStep = currentStep;
                response = "⚠️ You cannot register others yet. Your account is still being verified by admin.";
            } else {
                Optional<SoldierRecord> sponsor = primaryVerifiedSelfRecord(userId);
                if (sponsor.isPresent()) {
                    profile.setProxyParentRecordCode(sponsor.get().getRecordCode());
                    clearRegistrationDraft(profile);
                    nextStep = "ASK_PROXY_RELATIONSHIP";
                    response = "What is this person's relationship to you? (e.g. Child, Parent, Spouse, Dependent)";
                    quickReplies = List.of("Child", "Parent", "Spouse", "Dependent", "Other");
                } else {
                    nextStep = currentStep;
                    response = "We could not find a verified primary record for your account yet. "
                            + "Please wait for admin verification, or contact support.";
                }
            }
        } else if ("__CHAT_ACTION_view_payments__".equalsIgnoreCase(trimmedMsg)) {
            menuActionHandled = true;
            nextStep = currentStep;
            response = "To view your payments, open **Contributions → Payments** in the sidebar, "
                    + "or go to **Contributions → Payments** from the main menu.";
        } else if ("__CHAT_ACTION_update_profile__".equalsIgnoreCase(trimmedMsg)) {
            menuActionHandled = true;
            nextStep = currentStep;
            response = "To update your profile, open **Settings** in the sidebar. "
                    + "In this chat you can also type **edit phone**, **edit name**, **edit dob**, etc.";
        }

        if (!menuActionHandled) {
        // Handle "hi" message - only if step is ASK_PHONE or COMPLETED
        if ((lowerMessage.equals("hi") || lowerMessage.equals("hello") || lowerMessage.equals("hey")) 
                && (currentStep.equals("ASK_PHONE") || currentStep.equals("COMPLETED"))) {
            
            if (currentStep.equals("COMPLETED")) {
                String greet = greetingFirstName(userId, user);
                response = "Hi " + greet + "! 👋 You are already registered. How can I help you today?";
                nextStep = "COMPLETED";
                if (primaryVerifiedSelfRecord(userId).isPresent()) {
                    quickReplyOptions = verifiedHomeMenuQuickReplies();
                }
            } else {
                response = "Hi " + greetingFirstName(userId, user) +
                          "! 👋 Welcome to HT-E Roll Book.\n\nPlease enter your phone number to continue.";
                nextStep = "ASK_PHONE";
            }
        } else {
            // Process based on current step (LOCKED SEQUENCE matching admin dashboard format)
            switch (currentStep) {
                case "ASK_PHONE":
                    if (RegistrationFieldValidator.isMissingRequiredField(message)) {
                        response = "Please enter a valid Zimbabwe phone number (e.g. 077xxxxxxx or +26377xxxxxxx)";
                        break;
                    }
                    String normalizedPhone = PhoneNumberUtil.normalizeZimPhone(message);
                    if (normalizedPhone != null && PhoneNumberUtil.isValidNormalized(normalizedPhone)) {
                        profile.setPhoneNumber(normalizedPhone);
                        user.setPhoneNumber(normalizedPhone);
                        userRepository.save(user);
                        nextStep = "ASK_FIRST_NAME";
                        response = "Thanks 👌 I've saved your phone number as " + normalizedPhone + 
                                  ".\n\nPlease enter your first name.";
                        log.info("[BotRegistration] Phone saved: userId={}, phone={}, step: {} -> {}", 
                                userId, normalizedPhone, currentStep, nextStep);
                    } else {
                        response = "Please enter a valid Zimbabwe phone number (e.g. 077xxxxxxx or +26377xxxxxxx)";
                        log.warn("[BotRegistration] Invalid phone format: userId={}, input='{}'", userId, message);
                    }
                    break;
                    
                case "ASK_FIRST_NAME":
                    if (message.length() < 2) {
                        response = "Please enter a valid first name (at least 2 characters).";
                    } else if (message.contains(" ")) {
                        response = "Please enter only your first name on this step. We will ask for your family name (surname) next.";
                    } else {
                        profile.setNextOfKinName(message + "|FIRSTNAME");
                        nextStep = "ASK_FAMILY_NAME";
                        response = "Thanks! Please enter your family name (surname).";
                        log.info("[BotRegistration] First name saved: userId={}, name={}, step: {} -> {}",
                                userId, message, currentStep, nextStep);
                    }
                    break;
                    
                case "ASK_FAMILY_NAME":
                    if (message.length() < 2) {
                        response = "Please enter a valid family name (at least 2 characters).";
                    } else {
                        // Append family name to existing first name
                        String existingFirst = profile.getNextOfKinName();
                        if (existingFirst != null && existingFirst.contains("|FIRSTNAME")) {
                            profile.setNextOfKinName(existingFirst.replace("|FIRSTNAME", "") + "|FIRSTNAME");
                        }
                        profile.setNextOfKinPhone(message + "|FAMILYNAME"); // Store family name
                        nextStep = "ASK_DOB";
                        response = "Got it! What is your date of birth? Please use format YYYY-MM-DD (e.g., 1990-05-15) or DD/MM/YYYY.";
                        log.info("[BotRegistration] Family name saved: userId={}, step: {} -> {}", 
                                userId, currentStep, nextStep);
                    }
                    break;
                    
                case "ASK_DOB":
                    LocalDate dob = parseDate(message);
                    if (dob == null || dob.isAfter(LocalDate.now())) {
                        response = "Please enter a valid date of birth in format YYYY-MM-DD (e.g., 1990-05-15) or DD/MM/YYYY. Date cannot be in the future.";
                    } else {
                        int age = Period.between(dob, LocalDate.now()).getYears();
                        // Store DOB in address field with marker
                        profile.setAddress("DOB:" + dob.toString() + "|AGE:" + age);
                        nextStep = "ASK_GENDER";
                        response = "How old are you? I have calculated your age as " + age + ". Are you Male or Female?";
                        log.info("[BotRegistration] DOB saved: userId={}, dob={}, age={}, step: {} -> {}", 
                                userId, dob, age, currentStep, nextStep);
                    }
                    break;

                case "ASK_GENDER":
                    String normalizedGender = normalizeGender(message);
                    if (normalizedGender == null) {
                        response = "Please reply with Male or Female.";
                    } else {
                        profile.setAddress(profile.getAddress() + "|GENDER:" + normalizedGender);
                        Integer capturedAge = extractAge(profile);
                        if (capturedAge != null && capturedAge >= 16) {
                            nextStep = "ASK_MARITAL_STATUS";
                            response = "Are you married? (Yes/No)";
                        } else {
                            profile.setAddress(profile.getAddress() + "|MARITAL:SINGLE");
                            nextStep = "ASK_CHILDREN_COUNT";
                            response = "How many children do you have? (Enter a number, 0 if none)";
                        }
                    }
                    break;

                case "ASK_MARITAL_STATUS":
                    String normalizedMarital = normalizeMaritalYesNo(message);
                    if (normalizedMarital == null) {
                        response = "Please answer Yes or No. Are you married?";
                    } else {
                        profile.setAddress(profile.getAddress() + "|MARITAL:" + normalizedMarital);
                        nextStep = "ASK_CHILDREN_COUNT";
                        response = "How many children do you have? (Enter a number, 0 if none)";
                    }
                    break;

                case "ASK_CHILDREN_COUNT":
                    Integer childrenCount = parseChildrenCount(message);
                    if (childrenCount == null) {
                        response = "Please enter a valid number of children (0 or more).";
                    } else {
                        profile.setAddress(profile.getAddress() + "|CHILDREN:" + childrenCount);
                        Integer capturedAge = extractAge(profile);
                        if (capturedAge != null && capturedAge >= 18) {
                            nextStep = "ASK_ID_NUMBER";
                            response = "Thank you! Please provide your ID number.";
                        } else {
                            assignCorpsToProfile(profile);
                            nextStep = "ASK_ADDRESS";
                            response = "Excellent! Please enter your home address (suburb/town).";
                        }
                    }
                    break;
                    
                case "ASK_ID_NUMBER":
                    if (message.length() < 5) {
                        response = "Please enter a valid ID number (at least 5 characters).";
                    } else {
                        profile.setNationalId(message);
                        assignCorpsToProfile(profile);
                        nextStep = "ASK_ADDRESS";
                        response = "Excellent! Please enter your home address (suburb/town).";
                        log.info("[BotRegistration] ID number saved: userId={}, step: {} -> {}", 
                                userId, currentStep, nextStep);
                    }
                    break;
                    
                case "ASK_CORPS":
                    assignCorpsToProfile(profile);
                    nextStep = "ASK_ADDRESS";
                    response = "Excellent! Please enter your home address (suburb/town).";
                    break;
                    
                case "ASK_ADDRESS":
                    if (message.length() < 5) {
                        response = "Please enter a valid address (at least 5 characters).";
                    } else {
                        // Append address to address field
                        String existing = profile.getAddress();
                        profile.setAddress((existing != null ? existing + "|" : "") + "ADDRESS:" + message);
                        nextStep = "ASK_NOK_NAME";
                        response = "Please enter your next of kin's full name.";
                        log.info("[BotRegistration] Address saved: userId={}, step: {} -> {}", 
                                userId, currentStep, nextStep);
                    }
                    break;
                    
                case "ASK_NOK_NAME":
                    if (RegistrationFieldValidator.isMissingRequiredField(message) || message.trim().length() < 2) {
                        response = "Please enter a valid next of kin name (at least 2 characters).";
                    } else {
                        // Append NOK name to nextOfKinName field
                        String existing = profile.getNextOfKinName();
                        profile.setNextOfKinName((existing != null ? existing + "|" : "") + "NOKNAME:" + message);
                        nextStep = "ASK_NOK_PHONE";
                        response = "Please enter your next of kin's phone number.";
                        log.info("[BotRegistration] NOK name saved: userId={}, step: {} -> {}", 
                                userId, currentStep, nextStep);
                    }
                    break;
                    
                case "ASK_NOK_PHONE":
                    if (RegistrationFieldValidator.isMissingRequiredField(message)) {
                        response = "Please enter a valid Zimbabwe phone number (e.g. 077xxxxxxx or +26377xxxxxxx)";
                        break;
                    }
                    String normalizedNokPhone = PhoneNumberUtil.normalizeZimPhone(message);
                    if (normalizedNokPhone != null && PhoneNumberUtil.isValidNormalized(normalizedNokPhone)) {
                        // Append NOK phone to nextOfKinPhone field
                        String existing = profile.getNextOfKinPhone();
                        profile.setNextOfKinPhone((existing != null ? existing + "|" : "") + "NOKPHONE:" + normalizedNokPhone);
                        nextStep = "ASK_SONG";
                        response = "Great! What is your favorite song? (Type 'skip' if you prefer not to answer)";
                        log.info("[BotRegistration] NOK phone saved: userId={}, step: {} -> {}", 
                                userId, currentStep, nextStep);
                    } else {
                        response = "Please enter a valid Zimbabwe phone number (e.g. 077xxxxxxx or +26377xxxxxxx)";
                        log.warn("[BotRegistration] Invalid NOK phone format: userId={}, input='{}'", userId, message);
                    }
                    break;
                    
                case "ASK_SONG":
                    if (RegistrationFieldValidator.isMissingRequiredField(message)) {
                        response = "Please provide your favorite song.";
                        nextStep = "ASK_SONG";
                    } else {
                        profile.setAddress(profile.getAddress() + "|SONG:" + message);
                        nextStep = "ASK_VERSE";
                        response = "Thank you! What is your favorite Bible verse? (Type 'skip' if you prefer not to answer)";
                        log.info("[BotRegistration] Song saved: userId={}, step: {} -> {}",
                                userId, currentStep, nextStep);
                    }
                    break;

                case "ASK_VERSE":
                    if (RegistrationFieldValidator.isMissingRequiredField(message)) {
                        response = "Please provide your favorite Bible verse.";
                        nextStep = "ASK_VERSE";
                    } else {
                        profile.setAddress(profile.getAddress() + "|VERSE:" + message);
                        nextStep = "CONFIRM";
                        response = buildConfirmationMessage(profile, user);
                        log.info("[BotRegistration] Verse saved: userId={}, step: {} -> {}",
                                userId, currentStep, nextStep);
                    }
                    break;
                    
                case "CONFIRM":
                    if (message.toUpperCase().equals("CONFIRM")) {
                        Integer confirmAge = extractAge(profile);
                        if (confirmAge != null && confirmAge >= 18
                                && (profile.getNationalId() == null || profile.getNationalId().trim().isEmpty())) {
                            nextStep = "ASK_ID_NUMBER";
                            response = "ID Number is mandatory for individuals 18 years or older. Please provide your ID number.";
                            break;
                        }
                        if (RegistrationFieldValidator.isMissingRequiredField(profile.getPhoneNumber())) {
                            nextStep = "ASK_PHONE";
                            response = "Please enter a valid Zimbabwe phone number (e.g. 077xxxxxxx or +26377xxxxxxx).";
                            break;
                        }
                        if (RegistrationFieldValidator.isMissingRequiredField(extractNokName(profile))) {
                            nextStep = "ASK_NOK_NAME";
                            response = "Please enter your next of kin's full name.";
                            break;
                        }
                        if (RegistrationFieldValidator.isMissingRequiredField(extractNokPhone(profile))) {
                            nextStep = "ASK_NOK_PHONE";
                            response = "Please enter your next of kin's phone number.";
                            break;
                        }
                        if (!hasRequiredSong(profile)) {
                            nextStep = "ASK_SONG";
                            response = "Please provide your favorite song.";
                            break;
                        }
                        if (!hasRequiredVerse(profile)) {
                            nextStep = "ASK_VERSE";
                            response = "Please provide your favorite Bible verse.";
                            break;
                        }
                        boolean proxyFlow = profile.getProxyParentRecordCode() != null
                                && !profile.getProxyParentRecordCode().isBlank();
                        SoldierRecord record = createSoldierRecord(profile, user);
                        soldierRecordRepository.save(record);
                        
                        profile.setStatus(RegistrationProfile.RegistrationStatus.COMPLETED);
                        nextStep = "COMPLETED";
                        if (proxyFlow) {
                            clearRegistrationDraft(profile);
                            profile.setProxyParentRecordCode(null);
                            profile.setProxyRelationship(null);
                            response = "✅ Family member registered and linked to your record.\n\nTheir Record ID: "
                                    + record.getRecordCode()
                                    + "\n\nSubmission received! They are now in the queue for Admin approval.";
                        } else {
                            String dept = record.getDepartment() != null ? record.getDepartment() : "General Member";
                            response = "✅ Registration received! Your Record ID: " + record.getRecordCode()
                                      + "\n\nBased on your details, you have been registered in the *" + dept + "* section."
                                      + "\n\nYou are currently a Candidate. An admin must verify your profile before you can register others.";
                        }
                        log.info("[BotRegistration] Registration completed: userId={}, recordId={}, proxyFlow={}", 
                                userId, record.getRecordCode(), proxyFlow);
                    } else if (message.toUpperCase().equals("EDIT")) {
                        // Go back to first missing field
                        nextStep = "ASK_PHONE";
                        response = "Please enter your phone number to continue.";
                    } else {
                        response = "Please type CONFIRM to submit or EDIT to change your information.";
                    }
                    break;
                    
                case "PROXY_CHOICE":
                    if (isProxyFamilyIntent(lowerMessage)) {
                        Optional<SoldierRecord> sponsor = primaryVerifiedSelfRecord(userId);
                        if (sponsor.isEmpty()) {
                            response = "We could not find a verified primary record for your account. Please contact support.";
                            nextStep = "COMPLETED";
                        } else {
                            profile.setProxyParentRecordCode(sponsor.get().getRecordCode());
                            clearRegistrationDraft(profile);
                            nextStep = "ASK_PROXY_RELATIONSHIP";
                            response = "What is this person's relationship to you? (e.g. Child, Parent, Spouse, Dependent)";
                            quickReplies = List.of("Child", "Parent", "Spouse", "Dependent", "Other");
                        }
                    } else if (isProxyProfileIntent(lowerMessage)) {
                        Optional<SoldierRecord> primaryForProfile = primaryVerifiedSelfRecord(userId);
                        if (primaryForProfile.isPresent()) {
                            response = buildPrimaryVerifiedSummary(user, primaryForProfile.get());
                        } else {
                            response = "No verified profile found yet. Please wait for admin verification.";
                        }
                        nextStep = "COMPLETED";
                    } else if (isProxyDeclineIntent(lowerMessage)) {
                        response = "No problem. If you want to register someone later, type **register**. To see your details, type **profile**.";
                        nextStep = "COMPLETED";
                    } else {
                        response = "Please choose one: register a **family member**, **view profile**, or say **no thanks**.";
                        quickReplies = List.of("Register a family member", "View my profile", "No thanks");
                    }
                    break;

                case "ASK_PROXY_RELATIONSHIP":
                    if (message.length() < 2) {
                        response = "Please enter a short relationship (e.g. Child, Parent, Dependent).";
                    } else {
                        profile.setProxyRelationship(message.trim());
                        clearRegistrationDraft(profile);
                        nextStep = "ASK_PHONE";
                        response = "Thank you. What is this person's phone number? (Zimbabwe format, e.g. 077xxxxxxx)";
                    }
                    break;

                case "COMPLETED":
                    Optional<SoldierRecord> verifiedSelf = primaryVerifiedSelfRecord(userId);
                    if (verifiedSelf.isPresent()) {
                        if (lowerMessage.contains("register")) {
                            if (!Boolean.TRUE.equals(user.getIsVerified())) {
                                nextStep = "COMPLETED";
                                response = "⚠️ Access denied: your account is currently Candidate/Pending. "
                                        + "Please wait for admin verification before registering others.";
                            } else {
                                nextStep = "PROXY_CHOICE";
                                String greet = greetingFirstName(userId, user);
                                response = "I see you are already registered, " + greet + "! "
                                        + "Would you like to **register a family member** (child, parent, or dependent), "
                                        + "or **view your profile**?";
                                quickReplies = List.of("Register a family member", "View my profile", "No thanks");
                            }
                        } else if (lowerMessage.contains("profile") || lowerMessage.contains("view my")
                                || lowerMessage.contains("my details")) {
                            response = buildPrimaryVerifiedSummary(user, verifiedSelf.get());
                            nextStep = "COMPLETED";
                        } else {
                            response = "You are registered and verified. Type **register** to add a family member, "
                                    + "or **profile** to view your details.";
                        }
                    } else {
                        response = "You have already submitted a registration. If admin has not verified you yet, "
                                + "please wait for verification before adding family members. Type **status** to see your progress.";
                    }
                    break;
                    
                // Post-verification flow (after admin verifies)
                case "VERIFIED_NOTIFICATION":
                    // Advance step immediately so duplicate requests (e.g. double "hi", StrictMode) don't ask ward twice
                    profile.setRegistrationStep("ASK_WARD_POST_VERIFY");
                    registrationProfileRepository.save(profile);
                    nextStep = "ASK_WARD_POST_VERIFY";
                    response = "What is your Ward?";
                    log.info("[BotRegistration] Starting post-verification flow: userId={}, step: {} -> {} (saved immediately for idempotency)", 
                            userId, currentStep, nextStep);
                    break;
                    
                case "ASK_WARD_POST_VERIFY":
                    if (message.length() < 2) {
                        response = "Please enter a valid Ward name (at least 2 characters).";
                        nextStep = currentStep; // Stay on same step if invalid
                        log.warn("[BotRegistration] Invalid Ward input: userId={}, input='{}', step remains: {}", 
                                userId, message, currentStep);
                    } else {
                        // Update record with ward
                        updateRecordWard(userId, message);
                        nextStep = "ASK_BRIGADE_POST_VERIFY";
                        response = "Thank you! What is your Brigade?";
                        log.info("[BotRegistration] Ward saved (post-verify): userId={}, ward={}, step: {} -> {}", 
                                userId, message, currentStep, nextStep);
                    }
                    break;
                    
                case "ASK_BRIGADE_POST_VERIFY":
                    if (message.length() < 2) {
                        response = "Please enter a valid Brigade name (at least 2 characters).";
                    } else {
                        // Update record with brigade
                        updateRecordBrigade(userId, message);
                        nextStep = "ASK_PERSON_IMAGE";
                        response = "Thank you! Please upload your personal photo. Click the 📷 icon or use the upload button.";
                        log.info("[BotRegistration] Brigade saved (post-verify): userId={}, brigade={}, step: {} -> {}", 
                                userId, message, currentStep, nextStep);
                    }
                    break;
                    
                case "ASK_PERSON_IMAGE":
                    // User sent text instead of image - remind them
                    response = "Please upload your personal photo using the image upload button. Click the 📷 icon or drag and drop an image.";
                    log.info("[BotRegistration] Reminding user to upload person image: userId={}", userId);
                    break;
                    
                case "ASK_CERT_IMAGE":
                    // User sent text instead of image - remind them
                    response = "Please upload your certificate image using the image upload button. Click the 📷 icon or drag and drop an image.";
                    log.info("[BotRegistration] Reminding user to upload cert image: userId={}", userId);
                    break;
                    
                default:
                    // Unknown step - reset to ASK_PHONE
                    nextStep = "ASK_PHONE";
                    response = "Please enter your phone number to continue.";
                    log.warn("[BotRegistration] Unknown step '{}' for userId={}, resetting to ASK_PHONE", 
                            currentStep, userId);
            }
        }
        } // end if (!menuActionHandled)
        
        // Update profile step - CRITICAL: Save step BEFORE sending response
        if (!nextStep.equals(currentStep)) {
            profile.setRegistrationStep(nextStep);
            registrationProfileRepository.save(profile);
            log.info("[BotRegistration] Step saved to DB: userId={}, step: {} -> {}", 
                    userId, currentStep, nextStep);
        } else {
            log.debug("[BotRegistration] Step unchanged: userId={}, step: {}", userId, currentStep);
        }
        
        log.info("[BotRegistration] Response: userId={}, step: {} -> {}, response='{}', clientMessageId={}", 
                userId, currentStep, nextStep, response, clientMessageId);
        
        // Save processed message for idempotency
        if (clientMessageId != null) {
            ProcessedMessage processed = new ProcessedMessage();
            processed.setUserId(userId);
            processed.setClientMessageId(clientMessageId);
            processed.setConversationId(conversationId);
            processed.setResponseContent(response);
            processedMessageRepository.save(processed);
        }
        
        Message botMessage = sendBotMessage(conversationId, response);
        
        // Build response with profile snapshot for debugging
        ChatMessageResponse chatResponse = new ChatMessageResponse();
        chatResponse.setBotReply(response);
        chatResponse.setRegistrationStep(nextStep);
        chatResponse.setMessageId(botMessage.getId());
        chatResponse.setProfileSnapshot(buildProfileSnapshot(profile, user));
        chatResponse.setQuickReplies(quickReplies);
        chatResponse.setQuickReplyOptions(quickReplyOptions);
        
        return chatResponse;
    }
    
    /**
     * Create SoldierRecord from registration profile
     */
    private SoldierRecord createSoldierRecord(RegistrationProfile profile, User user) {
        SoldierRecord record = new SoldierRecord();
        
        // Extract data from profile (stored with pipe-separated markers)
        String phone = profile.getPhoneNumber();
        
        // Extract first name
        String firstName = null;
        if (profile.getNextOfKinName() != null && profile.getNextOfKinName().contains("|FIRSTNAME")) {
            firstName = profile.getNextOfKinName().split("\\|FIRSTNAME")[0];
        } else if (user.getFullName() != null) {
            String[] parts = user.getFullName().split(" ");
            firstName = parts.length > 0 ? parts[0] : null;
        }
        
        // Extract family name
        String familyName = null;
        if (profile.getNextOfKinPhone() != null && profile.getNextOfKinPhone().contains("|FAMILYNAME")) {
            familyName = profile.getNextOfKinPhone().split("\\|FAMILYNAME")[0];
        } else if (user.getFullName() != null) {
            String[] parts = user.getFullName().split(" ");
            familyName = parts.length > 1 ? parts[parts.length - 1] : null;
        }
        
        // Parse DOB and age from address field
        LocalDate dob = null;
        Integer age = null;
        String gender = null;
        String maritalStatus = null;
        Integer childrenCount = 0;
        if (profile.getAddress() != null) {
            if (profile.getAddress().contains("DOB:")) {
                String dobStr = profile.getAddress().split("DOB:")[1].split("\\|")[0];
                dob = LocalDate.parse(dobStr);
            }
            if (profile.getAddress().contains("AGE:")) {
                String ageStr = profile.getAddress().split("AGE:")[1].split("\\|")[0];
                try {
                    age = Integer.parseInt(ageStr);
                } catch (NumberFormatException e) {
                    // Recalculate if parsing fails
                    if (dob != null) {
                        age = Period.between(dob, LocalDate.now()).getYears();
                    }
                }
            }
            if (profile.getAddress().contains("GENDER:")) {
                gender = profile.getAddress().split("GENDER:")[1].split("\\|")[0];
            }
            if (profile.getAddress().contains("MARITAL:")) {
                maritalStatus = profile.getAddress().split("MARITAL:")[1].split("\\|")[0];
            }
            if (profile.getAddress().contains("CHILDREN:")) {
                try {
                    childrenCount = Integer.parseInt(profile.getAddress().split("CHILDREN:")[1].split("\\|")[0]);
                } catch (NumberFormatException ignored) {
                    childrenCount = 0;
                }
            }
        }
        
        // Extract corps, ward, brigade, address, song, verse
        String corpsName = DEFAULT_CORPS_NAME;
        String ward = null;
        String brigade = null;
        String address = null;
        String song = null;
        String verse = null;
        
        if (profile.getAddress() != null) {
            String[] parts = profile.getAddress().split("\\|");
            for (String part : parts) {
                if (part.startsWith("CORPS:")) {
                    corpsName = part.substring(6);
                } else if (part.startsWith("ADDRESS:")) {
                    address = part.substring(8);
                } else if (part.startsWith("SONG:")) {
                    song = part.substring(5);
                    if (song.isEmpty()) song = null;
                } else if (part.startsWith("VERSE:")) {
                    verse = part.substring(6);
                    if (verse.isEmpty()) verse = null;
                }
            }
        }
        
        if (profile.getNextOfKinName() != null && profile.getNextOfKinName().contains("WARD:")) {
            ward = profile.getNextOfKinName().split("WARD:")[1].split("\\|")[0];
        }
        if (profile.getNextOfKinPhone() != null && profile.getNextOfKinPhone().contains("BRIGADE:")) {
            brigade = profile.getNextOfKinPhone().split("BRIGADE:")[1].split("\\|")[0];
        }
        
        // Extract NOK info (stored in RegistrationProfile fields)
        // Note: NOK name and phone are stored in RegistrationProfile.nextOfKinName and nextOfKinPhone
        // which are already properly set, so we can access them directly if needed
        
        // Set record fields
        record.setWaId("CHAT-" + profile.getUserId().toString());
        record.setUserId(profile.getUserId());
        
        // Generate record code
        Long seqValue = soldierRecordRepository.getNextRecordSequence();
        int currentYear = Year.now().getValue();
        record.setRecordCode("HTE-" + currentYear + "-" + String.format("%03d", seqValue));
        
        record.setCorpsName(DEFAULT_CORPS_NAME);
        record.setEnrolledCorpsName(DEFAULT_CORPS_NAME);
        record.setWard(ward);
        record.setBrigade(brigade);
        record.setFirstName(firstName);
        record.setFamilyName(familyName);
        record.setDob(dob);
        record.setAge(age);
        record.setIdNumber(profile.getNationalId());
        record.setGender(gender);
        record.setMaritalStatus(maritalStatus);
        record.setKidsCount(childrenCount != null ? childrenCount : 0);
        applyMembershipClassification(record);
        record.setPhoneNumber(RegistrationFieldValidator.sanitizeRequiredField(phone));
        record.setAddress(address);
        record.setFavoriteSong(RegistrationFieldValidator.sanitizeRequiredField(song));
        record.setFavoriteBibleVerse(RegistrationFieldValidator.sanitizeRequiredField(verse));
        record.setNextOfKinName(RegistrationFieldValidator.sanitizeRequiredField(extractNokName(profile)));
        record.setNextOfKinPhone(RegistrationFieldValidator.sanitizeRequiredField(extractNokPhone(profile)));
        boolean proxyRegistration = profile.getProxyParentRecordCode() != null
                && !profile.getProxyParentRecordCode().isBlank();
        if (proxyRegistration) {
            record.setProxyId(profile.getProxyParentRecordCode().trim());
            String rel = profile.getProxyRelationship();
            record.setRelationship(rel != null && !rel.isBlank() ? rel.trim() : "Dependent");
            record.setStatus(RecordStatus.PENDING);
        } else {
            record.setStatus(RecordStatus.PENDING);
        }
        record.setTemplateType("STANDARD");
        
        if (!proxyRegistration && firstName != null && familyName != null) {
            user.setFullName(firstName + " " + familyName);
            user.setGender(gender);
            user.setMaritalStatus(maritalStatus);
            user.setChildrenCount(childrenCount != null ? childrenCount : 0);
            user.setCategory(record.getDepartment());
            user.setIsVerified(false);
            user.setMemberRole("CANDIDATE");
            userRepository.save(user);
        }
        
        return record;
    }
    
    /**
     * Parse date from various formats
     */
    private LocalDate parseDate(String dateStr) {
        if (dateStr == null || dateStr.trim().isEmpty()) {
            return null;
        }
        
        dateStr = dateStr.trim();
        
        try {
            // Try YYYY-MM-DD format
            if (dateStr.matches("\\d{4}-\\d{2}-\\d{2}")) {
                return LocalDate.parse(dateStr, DATE_FORMATTER);
            }
            // Try DD/MM/YYYY format
            if (dateStr.matches("\\d{2}/\\d{2}/\\d{4}")) {
                return LocalDate.parse(dateStr, DATE_FORMATTER_SLASH);
            }
        } catch (DateTimeParseException e) {
            log.warn("Failed to parse date: {}", dateStr);
        }
        
        return null;
    }

    private void assignCorpsToProfile(RegistrationProfile profile) {
        String existingAddress = profile.getAddress();
        if (existingAddress != null && existingAddress.contains("CORPS:")) {
            return;
        }
        profile.setAddress((existingAddress != null ? existingAddress + "|" : "") + "CORPS:" + DEFAULT_CORPS_NAME);
    }

    private Integer extractAge(RegistrationProfile profile) {
        if (profile.getAddress() == null || !profile.getAddress().contains("AGE:")) {
            return null;
        }
        try {
            return Integer.parseInt(profile.getAddress().split("AGE:")[1].split("\\|")[0]);
        } catch (Exception e) {
            return null;
        }
    }

    private String normalizeGender(String value) {
        if (value == null) {
            return null;
        }
        String v = value.trim().toLowerCase();
        if (v.equals("m") || v.equals("male") || v.equals("man")) {
            return "Male";
        }
        if (v.equals("f") || v.equals("female") || v.equals("woman")) {
            return "Female";
        }
        return null;
    }

    private String normalizeMaritalYesNo(String value) {
        if (value == null) {
            return null;
        }
        String v = value.trim().toLowerCase();
        if (v.equals("yes") || v.equals("y") || v.equals("married")) {
            return "Married";
        }
        if (v.equals("no") || v.equals("n") || v.equals("single") || v.equals("unmarried")) {
            return "Single";
        }
        return null;
    }

    private Integer parseChildrenCount(String value) {
        if (value == null) {
            return null;
        }
        try {
            int out = Integer.parseInt(value.trim());
            return out < 0 ? null : out;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private void applyMembershipClassification(SoldierRecord record) {
        int age = record.getAge() != null ? record.getAge() : 0;
        int kids = record.getKidsCount() != null ? record.getKidsCount() : 0;
        String g = MembershipClassifier.normalizeGender(record.getGender());
        String m = MembershipClassifier.normalizeMarital(record.getMaritalStatus());
        record.setDepartment(MembershipClassifier.classifyDepartment(age, g, m, kids));
        record.setBrigadeEligibility(MembershipClassifier.classifyBrigadeEligibility(age));
    }
    
    /**
     * Handle restart command - clears profile and resets to ASK_PHONE
     */
    private ChatMessageResponse handleRestart(UUID conversationId, UUID userId, RegistrationProfile profile) {
        profile.setPhoneNumber(null);
        profile.setAddress(null);
        profile.setNextOfKinName(null);
        profile.setNextOfKinPhone(null);
        profile.setNationalId(null);
        profile.setProxyParentRecordCode(null);
        profile.setProxyRelationship(null);
        profile.setStatus(RegistrationProfile.RegistrationStatus.DRAFT);
        profile.setRegistrationStep("ASK_PHONE");
        registrationProfileRepository.save(profile);
        
        // Clear user's full name
        User user = userRepository.findById(userId).orElse(null);
        if (user != null) {
            user.setFullName(null);
            userRepository.save(user);
        }
        
        String response = "Registration reset. Please enter your phone number to continue.";
        Message botMessage = sendBotMessage(conversationId, response);
        
        ChatMessageResponse chatResponse = new ChatMessageResponse();
        chatResponse.setBotReply(response);
        chatResponse.setRegistrationStep("ASK_PHONE");
        chatResponse.setMessageId(botMessage.getId());
        
        log.info("[BotRegistration] Registration restarted for userId={}", userId);
        return chatResponse;
    }
    
    /**
     * Handle status command - shows current step and captured data
     */
    private ChatMessageResponse handleStatus(UUID conversationId, UUID userId, RegistrationProfile profile, User user) {
        StringBuilder status = new StringBuilder("📋 Registration Status:\n\n");
        status.append("Current Step: ").append(profile.getRegistrationStep()).append("\n\n");
        status.append("Captured Data:\n");
        status.append("• Phone: ").append(profile.getPhoneNumber() != null ? profile.getPhoneNumber() : "Not provided").append("\n");
        status.append("• Name: ").append(user.getFullName() != null ? user.getFullName() : "Not provided").append("\n");
        status.append("• Address: ").append(extractAddress(profile) != null ? extractAddress(profile) : "Not provided").append("\n");
        status.append("• Next of Kin Name: ").append(extractNokName(profile) != null ? extractNokName(profile) : "Not provided").append("\n");
        status.append("• Next of Kin Phone: ").append(extractNokPhone(profile) != null ? extractNokPhone(profile) : "Not provided").append("\n");
        
        String response = status.toString();
        Message botMessage = sendBotMessage(conversationId, response);
        
        ChatMessageResponse chatResponse = new ChatMessageResponse();
        chatResponse.setBotReply(response);
        chatResponse.setRegistrationStep(profile.getRegistrationStep());
        chatResponse.setMessageId(botMessage.getId());
        
        return chatResponse;
    }
    
    /**
     * Handle edit command - sets step back to specified field
     */
    private ChatMessageResponse handleEdit(UUID conversationId, UUID userId, RegistrationProfile profile, String message) {
        String field = message.substring(5).trim().toLowerCase();
        String response;
        String nextStep;
        
        switch (field) {
            case "phone":
                nextStep = "ASK_PHONE";
                profile.setPhoneNumber(null);
                response = "Please enter your phone number to continue.";
                break;
            case "name":
            case "firstname":
                nextStep = "ASK_FIRST_NAME";
                response = "Please enter your first name.";
                break;
            case "familyname":
            case "surname":
                nextStep = "ASK_FAMILY_NAME";
                response = "Please enter your family name (surname).";
                break;
            case "dob":
            case "dateofbirth":
                nextStep = "ASK_DOB";
                response = "Please enter your date of birth (YYYY-MM-DD or DD/MM/YYYY).";
                break;
            case "id":
            case "idnumber":
                nextStep = "ASK_ID_NUMBER";
                profile.setNationalId(null);
                response = "Please enter your ID number.";
                break;
            case "corps":
                response = "All members are automatically assigned to Highfield Temple.";
                nextStep = profile.getRegistrationStep();
                break;
            case "ward":
                // Ward is only asked after verification
                response = "Ward will be asked after your registration is verified by admin.";
                nextStep = profile.getRegistrationStep();
                break;
            case "brigade":
                // Brigade is only asked after verification
                response = "Brigade will be asked after your registration is verified by admin.";
                nextStep = profile.getRegistrationStep();
                break;
            case "address":
                nextStep = "ASK_ADDRESS";
                response = "Please enter your home address (suburb/town).";
                break;
            case "nokname":
                nextStep = "ASK_NOK_NAME";
                response = "Please enter your next of kin's full name.";
                break;
            case "nokphone":
                nextStep = "ASK_NOK_PHONE";
                response = "Please enter your next of kin's phone number.";
                break;
            default:
                response = "Invalid edit command. Use: edit phone, edit name, edit dob, edit id, edit corps, edit ward, edit brigade, edit address, edit nokname, or edit nokphone";
                nextStep = profile.getRegistrationStep();
        }
        
        if (!nextStep.equals(profile.getRegistrationStep())) {
            profile.setRegistrationStep(nextStep);
            registrationProfileRepository.save(profile);
        }
        
        Message botMessage = sendBotMessage(conversationId, response);
        
        ChatMessageResponse chatResponse = new ChatMessageResponse();
        chatResponse.setBotReply(response);
        chatResponse.setRegistrationStep(nextStep);
        chatResponse.setMessageId(botMessage.getId());
        
        log.info("[BotRegistration] Edit command: userId={}, field={}, step={}", userId, field, nextStep);
        return chatResponse;
    }
    
    /**
     * Build photo request message based on photo status
     */
    private String buildPhotoRequestMessage(SoldierRecord record, SoldierRecord.PhotoStatus photoStatus) {
        StringBuilder message = new StringBuilder();
        
        if (photoStatus == SoldierRecord.PhotoStatus.RESUBMIT_REQUESTED) {
            message.append("📸 An administrator has requested that you upload or resubmit your personal photo.\n\n");
            if (record.getPhotoReviewNotes() != null && !record.getPhotoReviewNotes().isEmpty()) {
                message.append("Reason: ").append(record.getPhotoReviewNotes()).append("\n\n");
            }
        } else if (photoStatus == SoldierRecord.PhotoStatus.REJECTED) {
            message.append("❌ Your photo was rejected.\n\n");
            if (record.getPhotoReviewNotes() != null && !record.getPhotoReviewNotes().isEmpty()) {
                message.append("Reason: ").append(record.getPhotoReviewNotes()).append("\n\n");
            }
            message.append("Please upload a new photo.\n\n");
        } else {
            message.append("📸 Please upload your personal photo now.\n\n");
        }
        
        message.append("Click the 📎 icon or use the upload button to select your photo.");
        
        return message.toString();
    }
    
    /**
     * Build confirmation message with summary
     */
    private String buildConfirmationMessage(RegistrationProfile profile, User user) {
        StringBuilder summary = new StringBuilder("📋 Please review your registration:\n\n");
        
        // Extract and display all collected data
        summary.append("Phone: ").append(profile.getPhoneNumber() != null ? profile.getPhoneNumber() : "Not provided").append("\n");
        
        String firstName = extractFirstName(profile, user);
        String familyName = extractFamilyName(profile, user);
        summary.append("Name: ").append(firstName != null && familyName != null ? firstName + " " + familyName : "Not provided").append("\n");
        
        LocalDate dob = extractDOB(profile);
        if (dob != null) {
            summary.append("Date of Birth: ").append(dob.toString()).append("\n");
            summary.append("Age: ").append(Period.between(dob, LocalDate.now()).getYears()).append("\n");
        }
        String capturedGender = extractMarker(profile, "GENDER:");
        if (capturedGender != null) {
            summary.append("Gender: ").append(capturedGender).append("\n");
        }
        String capturedMarital = extractMarker(profile, "MARITAL:");
        if (capturedMarital != null) {
            summary.append("Marital Status: ").append(capturedMarital).append("\n");
        }
        String capturedChildren = extractMarker(profile, "CHILDREN:");
        if (capturedChildren != null) {
            summary.append("Children Count: ").append(capturedChildren).append("\n");
        }
        
        if (profile.getNationalId() != null && !profile.getNationalId().matches("\\d+")) {
            summary.append("ID Number: ").append(profile.getNationalId()).append("\n");
        }
        
        summary.append("Corps: ").append(DEFAULT_CORPS_NAME).append("\n");
        summary.append("Address: ").append(extractAddress(profile) != null ? extractAddress(profile) : "Not provided").append("\n");
        summary.append("Note: Ward and Brigade will be asked after verification.").append("\n");
        summary.append("Next of Kin Name: ").append(extractNokName(profile) != null ? extractNokName(profile) : "Not provided").append("\n");
        summary.append("Next of Kin Phone: ").append(extractNokPhone(profile) != null ? extractNokPhone(profile) : "Not provided").append("\n");
        
        summary.append("\nType CONFIRM to submit or EDIT to change.");
        return summary.toString();
    }
    
    // Helper methods to extract data from profile
    private String extractFirstName(RegistrationProfile profile, User user) {
        if (profile.getNextOfKinName() != null && profile.getNextOfKinName().contains("|FIRSTNAME")) {
            return profile.getNextOfKinName().split("\\|FIRSTNAME")[0];
        }
        if (user.getFullName() != null) {
            String[] parts = user.getFullName().split(" ");
            return parts.length > 0 ? parts[0] : null;
        }
        return null;
    }
    
    private String extractFamilyName(RegistrationProfile profile, User user) {
        if (profile.getNextOfKinPhone() != null && profile.getNextOfKinPhone().contains("|FAMILYNAME")) {
            return profile.getNextOfKinPhone().split("\\|FAMILYNAME")[0];
        }
        if (user.getFullName() != null) {
            String[] parts = user.getFullName().split(" ");
            return parts.length > 1 ? parts[parts.length - 1] : null;
        }
        return null;
    }
    
    private LocalDate extractDOB(RegistrationProfile profile) {
        if (profile.getAddress() != null && profile.getAddress().contains("DOB:")) {
            String dobStr = profile.getAddress().split("DOB:")[1].split("\\|")[0];
            try {
                return LocalDate.parse(dobStr);
            } catch (Exception e) {
                return null;
            }
        }
        return null;
    }
    
    private String extractWard(RegistrationProfile profile) {
        if (profile.getNextOfKinName() != null && profile.getNextOfKinName().contains("WARD:")) {
            return profile.getNextOfKinName().split("WARD:")[1].split("\\|")[0];
        }
        return null;
    }
    
    private String extractBrigade(RegistrationProfile profile) {
        if (profile.getNextOfKinPhone() != null && profile.getNextOfKinPhone().contains("BRIGADE:")) {
            return profile.getNextOfKinPhone().split("BRIGADE:")[1].split("\\|")[0];
        }
        return null;
    }
    
    private String extractAddress(RegistrationProfile profile) {
        if (profile.getAddress() != null && profile.getAddress().contains("ADDRESS:")) {
            return profile.getAddress().split("ADDRESS:")[1].split("\\|")[0];
        }
        return null;
    }

    private String extractMarker(RegistrationProfile profile, String marker) {
        if (profile.getAddress() == null || marker == null || !profile.getAddress().contains(marker)) {
            return null;
        }
        try {
            return profile.getAddress().split(java.util.regex.Pattern.quote(marker))[1].split("\\|")[0];
        } catch (Exception e) {
            return null;
        }
    }
    
    private String extractNokName(RegistrationProfile profile) {
        if (profile.getNextOfKinName() != null && profile.getNextOfKinName().contains("NOKNAME:")) {
            return profile.getNextOfKinName().split("NOKNAME:")[1].split("\\|")[0];
        }
        return null;
    }
    
    private String extractNokPhone(RegistrationProfile profile) {
        if (profile.getNextOfKinPhone() != null && profile.getNextOfKinPhone().contains("NOKPHONE:")) {
            return profile.getNextOfKinPhone().split("NOKPHONE:")[1].split("\\|")[0];
        }
        return null;
    }

    private boolean hasRequiredSong(RegistrationProfile profile) {
        return !RegistrationFieldValidator.isMissingRequiredField(extractMarkerValue(profile, "SONG:"));
    }

    private boolean hasRequiredVerse(RegistrationProfile profile) {
        return !RegistrationFieldValidator.isMissingRequiredField(extractMarkerValue(profile, "VERSE:"));
    }

    private String extractMarkerValue(RegistrationProfile profile, String marker) {
        if (profile.getAddress() == null || marker == null) {
            return null;
        }
        for (String part : profile.getAddress().split("\\|")) {
            if (part.startsWith(marker)) {
                return part.substring(marker.length());
            }
        }
        return null;
    }
    
    /**
     * Build profile snapshot for debugging
     */
    private Map<String, Object> buildProfileSnapshot(RegistrationProfile profile, User user) {
        Map<String, Object> snapshot = new HashMap<>();
        snapshot.put("phoneNumber", profile.getPhoneNumber());
        snapshot.put("fullName", user.getFullName());
        snapshot.put("address", extractAddress(profile));
        snapshot.put("nextOfKinName", extractNokName(profile));
        snapshot.put("nextOfKinPhone", extractNokPhone(profile));
        snapshot.put("status", profile.getStatus().name());
        snapshot.put("step", profile.getRegistrationStep());
        snapshot.put("proxyParentRecordCode", profile.getProxyParentRecordCode());
        snapshot.put("proxyRelationship", profile.getProxyRelationship());
        return snapshot;
    }
    
    /**
     * Get current step for user
     */
    private String getCurrentStep(UUID userId) {
        return registrationProfileRepository.findByUserId(userId)
                .map(p -> p.getRegistrationStep() != null ? p.getRegistrationStep() : "ASK_PHONE")
                .orElse("ASK_PHONE");
    }
    
    /**
     * Update record with ward (post-verification)
     */
    private void updateRecordWard(UUID userId, String ward) {
        primaryVerifiedSelfRecord(userId).ifPresent(record -> {
            record.setWard(ward);
            soldierRecordRepository.save(record);
        });
    }
    
    /**
     * Update record with brigade (post-verification)
     */
    private void updateRecordBrigade(UUID userId, String brigade) {
        primaryVerifiedSelfRecord(userId).ifPresent(record -> {
            record.setBrigade(brigade);
            soldierRecordRepository.save(record);
        });
    }
    
    /**
     * Handle image upload (post-verification).
     * Validates MIME type separately from step. Uses current step to decide where to save;
     * if frontend sends wrong label (e.g. "person" when we expect cert), we still accept and save to the expected slot.
     */
    @Transactional
    public ChatMessageResponse handleImageUpload(UUID conversationId, UUID userId, MultipartFile file, String imageType) throws IOException {
        RegistrationProfile profile = registrationProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Registration profile not found"));
        
        String currentStep = profile.getRegistrationStep();
        
        // 1) Validate MIME type (file-level) - do not reject valid PNG/JPEG as "invalid image type"
        String contentType = file.getContentType();
        if (contentType == null || (!contentType.equals("image/png") && !contentType.equals("image/jpeg"))) {
            String response = "Please upload a PNG or JPEG image. Other file types are not accepted.";
            Message botMessage = sendBotMessage(conversationId, response);
            ChatMessageResponse chatResponse = new ChatMessageResponse();
            chatResponse.setBotReply(response);
            chatResponse.setRegistrationStep(currentStep);
            chatResponse.setMessageId(botMessage.getId());
            return chatResponse;
        }
        
        // 2) Validate state: only accept uploads when we're in an image step
        if (!"ASK_PERSON_IMAGE".equals(currentStep) && !"ASK_CERT_IMAGE".equals(currentStep)) {
            String response = "Image upload is not expected at this stage. Please follow the conversation flow.";
            Message botMessage = sendBotMessage(conversationId, response);
            ChatMessageResponse chatResponse = new ChatMessageResponse();
            chatResponse.setBotReply(response);
            chatResponse.setRegistrationStep(currentStep);
            chatResponse.setMessageId(botMessage.getId());
            return chatResponse;
        }
        
        // Save file
        String filename = fileService.saveFile(file);
        
        SoldierRecord record = primaryVerifiedSelfRecord(userId)
                .orElseThrow(() -> new RuntimeException("Verified primary record not found"));
        
        String response;
        String nextStep;
        
        if ("ASK_PERSON_IMAGE".equals(currentStep)) {
            // We need personal photo. Accept "person" only; if they send "cert", guide them.
            if ("cert".equals(imageType)) {
                response = "Please upload your personal photo first. Then you can upload your certificate image.";
                nextStep = currentStep;
                log.warn("[BotRegistration] User sent cert image during ASK_PERSON_IMAGE: userId={}", userId);
            } else {
                // "person" or any other label when we expect person - save as person
                record.setPersonImagePath(filename);
                soldierRecordRepository.save(record);
                photoManagementService.onPhotoUploaded(userId, filename);
                nextStep = "ASK_CERT_IMAGE";
                response = "✅ Personal photo uploaded successfully!\n\nPlease upload your certificate image. Click the 📷 icon or use the upload button.";
                log.info("[BotRegistration] Person image uploaded: userId={}, filename={}, step: {} -> {}", 
                        userId, filename, currentStep, nextStep);
            }
        } else {
            // ASK_CERT_IMAGE: we need certificate. Accept "cert" or "person" (stale frontend) and save as cert
            record.setCertImagePath(filename);
            soldierRecordRepository.save(record);
            nextStep = "COMPLETED";
            response = "✅ Certificate image uploaded successfully!\n\n🎉 Thank you! All information has been submitted. Your registration is now complete!";
            log.info("[BotRegistration] Cert image uploaded: userId={}, filename={}, imageType={}, step: {} -> {}", 
                    userId, filename, imageType, currentStep, nextStep);
        }
        
        // Update profile step
        profile.setRegistrationStep(nextStep);
        registrationProfileRepository.save(profile);
        
        Message botMessage = sendBotMessage(conversationId, response);
        
        ChatMessageResponse chatResponse = new ChatMessageResponse();
        chatResponse.setBotReply(response);
        chatResponse.setRegistrationStep(nextStep);
        chatResponse.setMessageId(botMessage.getId());
        
        return chatResponse;
    }

    private Optional<SoldierRecord> primaryVerifiedSelfRecord(UUID userId) {
        return soldierRecordRepository.findByUserId(userId).stream()
                .filter(r -> r.getStatus() == RecordStatus.VERIFIED)
                .filter(r -> r.getProxyId() == null || r.getProxyId().trim().isEmpty())
                .findFirst();
    }

    private void clearRegistrationDraft(RegistrationProfile profile) {
        profile.setPhoneNumber(null);
        profile.setNationalId(null);
        profile.setAddress(null);
        profile.setNextOfKinName(null);
        profile.setNextOfKinPhone(null);
    }

    private List<QuickReplyOption> verifiedHomeMenuQuickReplies() {
        return List.of(
                new QuickReplyOption("Register someone else", "__CHAT_ACTION_proxy_reg__"),
                new QuickReplyOption("View my payments", "__CHAT_ACTION_view_payments__"),
                new QuickReplyOption("Update my info", "__CHAT_ACTION_update_profile__"));
    }

    /**
     * Prefer member record first name (soldier_records) over auth full name so we do not greet "Hi System".
     */
    private String greetingFirstName(UUID userId, User user) {
        Optional<SoldierRecord> rec = primaryMemberRecordForDisplay(userId);
        if (rec.isPresent()) {
            String fn = rec.get().getFirstName();
            if (fn != null && !fn.trim().isEmpty()) {
                return fn.trim();
            }
        }
        String full = user.getFullName();
        if (full != null && !full.isBlank()) {
            String[] p = full.trim().split("\\s+");
            if (p.length > 0 && !looksLikeGenericAccountLabel(full, p[0])) {
                return p[0];
            }
        }
        if (user.getEmail() != null && user.getEmail().contains("@")) {
            return user.getEmail().substring(0, user.getEmail().indexOf('@'));
        }
        return "there";
    }

    private Optional<SoldierRecord> primaryMemberRecordForDisplay(UUID userId) {
        Optional<SoldierRecord> verified = primaryVerifiedSelfRecord(userId);
        if (verified.isPresent()) {
            return verified;
        }
        return soldierRecordRepository.findByUserId(userId).stream()
                .filter(r -> r.getProxyId() == null || r.getProxyId().trim().isEmpty())
                .max(Comparator.comparing(SoldierRecord::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder())));
    }

    private static boolean looksLikeGenericAccountLabel(String fullName, String firstToken) {
        if (firstToken == null) {
            return true;
        }
        String lowerFull = fullName.toLowerCase();
        String lowerFirst = firstToken.toLowerCase();
        return "system".equals(lowerFirst)
                || lowerFull.contains("system administrator")
                || lowerFull.contains("administrator");
    }

    private String buildPrimaryVerifiedSummary(User user, SoldierRecord r) {
        StringBuilder sb = new StringBuilder("Here is your profile:\n\n");
        sb.append("• Record ID: ").append(r.getRecordCode() != null ? r.getRecordCode() : "—").append("\n");
        sb.append("• Name: ")
                .append(trimOrEmpty(r.getFirstName())).append(" ")
                .append(trimOrEmpty(r.getFamilyName())).append("\n");
        sb.append("• Corps: ").append(r.getCorpsName() != null ? r.getCorpsName() : "—").append("\n");
        sb.append("• Ward: ").append(r.getWard() != null ? r.getWard() : "—").append("\n");
        sb.append("• Brigade: ").append(r.getBrigade() != null ? r.getBrigade() : "—").append("\n");
        sb.append("• Phone: ").append(r.getPhoneNumber() != null ? r.getPhoneNumber() : "—").append("\n");
        sb.append("• Status: ").append(r.getStatus() != null ? r.getStatus().name() : "—").append("\n");
        return sb.toString();
    }

    private static String trimOrEmpty(String s) {
        return s == null ? "" : s.trim();
    }

    private static boolean isProxyFamilyIntent(String lower) {
        return lower.contains("__chat_action_proxy_reg__") || lower.contains("family")
                || lower.contains("someone else") || lower.contains("dependent")
                || lower.contains("register a family") || lower.contains("child") || lower.contains("parent")
                || lower.equals("yes") || lower.startsWith("yes ") || lower.contains("register someone")
                || lower.contains("member");
    }

    private static boolean isProxyProfileIntent(String lower) {
        return lower.contains("profile") || lower.contains("view my");
    }

    private static boolean isProxyDeclineIntent(String lower) {
        return lower.contains("no thanks") || lower.equals("no") || lower.contains("just checking")
                || lower.contains("not now");
    }
    
    /**
     * Send bot message directly (to avoid circular dependency)
     */
    @Transactional
    private Message sendBotMessage(UUID conversationId, String content) {
        Message message = new Message();
        message.setConversationId(conversationId);
        message.setSenderUserId(null); // Bot messages have no sender
        message.setMessageType(Message.MessageType.TEXT);
        message.setContent(content);
        message = messageRepository.save(message);

        // Update conversation last message time
        ConversationNew conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new RuntimeException("Conversation not found"));
        conversation.setLastMessageAt(LocalDateTime.now());
        conversationRepository.save(conversation);

        return message;
    }
}
