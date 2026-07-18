package org.salvationarmy.whatsapp.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.salvationarmy.whatsapp.entity.Conversation;
import org.salvationarmy.whatsapp.entity.RecordStatus;
import org.salvationarmy.whatsapp.entity.SoldierRecord;
import org.salvationarmy.whatsapp.repository.ConversationRepository;
import org.salvationarmy.whatsapp.repository.SoldierRecordRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import jakarta.annotation.PostConstruct;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.Year;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class WhatsAppService {

    private static final Logger logger = LoggerFactory.getLogger(WhatsAppService.class);

    @Autowired
    private ConversationRepository conversationRepository;

    @Autowired
    private SoldierRecordRepository soldierRecordRepository;

    @Value("${whatsapp.accessToken}")
    private String accessToken;

    @Value("${whatsapp.phoneNumberId}")
    private String phoneNumberId;

    @Value("${whatsapp.apiUrl}")
    private String apiUrl;

    @PostConstruct
    public void init() {
        if (accessToken == null || accessToken.trim().isEmpty()) {
            logger.warn("⚠️ WHATSAPP_ACCESS_TOKEN is not configured. Outbound messages will fail.");
        } else {
            logger.info("✅ WHATSAPP_ACCESS_TOKEN is configured (length: {})", accessToken.length());
        }
    }

    @Value("${app.upload.dir}")
    private String uploadDir;

    private static final String DEFAULT_CORPS_NAME = "Highfield Temple";

    // Conversation states
    private static final String STATE_START = "START";
    private static final String STATE_ASK_CORPS_NAME = "ASK_CORPS_NAME";
    private static final String STATE_ASK_ENROLLED_CORPS = "ASK_ENROLLED_CORPS";
    private static final String STATE_ASK_WARD = "ASK_WARD";
    private static final String STATE_ASK_BRIGADE = "ASK_BRIGADE";
    private static final String STATE_ASK_FIRST_NAME = "ASK_FIRST_NAME";
    private static final String STATE_ASK_FAMILY_NAME = "ASK_FAMILY_NAME";
    private static final String STATE_ASK_DOB = "ASK_DOB";
    private static final String STATE_ASK_ID_NUMBER = "ASK_ID_NUMBER";
    private static final String STATE_ASK_PERSON_IMAGE = "ASK_PERSON_IMAGE";
    private static final String STATE_ASK_CERT_IMAGE = "ASK_CERT_IMAGE";
    private static final String STATE_ASK_SONG = "ASK_SONG";
    private static final String STATE_ASK_BIBLE_VERSE = "ASK_BIBLE_VERSE";
    private static final String STATE_COMPLETE = "COMPLETE";

    @Transactional
    public void processMessage(String waId, String messageType, String textBody, String imageId) {
        logger.info("Processing message from {}: type={}, text={}, imageId={}",
                waId, messageType, textBody, imageId);

        // 1. Send immediate response based on type (as requested)
        if ("text".equals(messageType) && textBody != null) {
            sendMessage(waId, "🛡️ *HT-E ROLL BOOK* is live ✅\nType *REGISTER* to begin.");
        } else if ("image".equals(messageType)) {
            sendMessage(waId, "Image received ✅");
        }

        // 2. Handle special commands
        if (textBody != null) {
            String command = textBody.trim().toLowerCase();
            if (command.equals("restart")) {
                handleRestart(waId);
                return;
            } else if (command.equals("help")) {
                sendHelpMessage(waId);
                return;
            }
        }

        // 3. Existing state machine logic...
        // Get or create conversation
        Conversation conversation = conversationRepository.findByWaId(waId)
                .orElseGet(() -> createNewConversation(waId));

        // Get or create soldier record
        SoldierRecord record = soldierRecordRepository.findByWaIdAndStatus(waId, RecordStatus.IN_PROGRESS)
                .orElseGet(() -> createNewRecord(waId));

        // Process based on current state
        processStateTransition(conversation, record, messageType, textBody, imageId);

        // Update conversation
        conversation.setLastMessageAt(LocalDateTime.now());
        conversationRepository.save(conversation);
    }

    private Conversation createNewConversation(String waId) {
        Conversation conversation = new Conversation();
        conversation.setWaId(waId);
        conversation.setState(STATE_START);
        conversation.setLastMessageAt(LocalDateTime.now());
        return conversationRepository.save(conversation);
    }

    private SoldierRecord createNewRecord(String waId) {
        SoldierRecord record = new SoldierRecord();
        record.setWaId(waId);
        record.setStatus(RecordStatus.IN_PROGRESS);

        // Generate record_code using sequence with year: HTE-2026-005
        Long seqValue = soldierRecordRepository.getNextRecordSequence();
        int currentYear = Year.now().getValue();
        record.setRecordCode("HTE-" + currentYear + "-" + String.format("%03d", seqValue));

        // Set template_type: HIGH_FIELD_TEMPLE if any record exists for this w-id, else
        // STANDARD
        List<SoldierRecord> existingRecords = soldierRecordRepository.findByWaId(waId);
        if (!existingRecords.isEmpty()) {
            record.setTemplateType("HIGH_FIELD_TEMPLE");
        } else {
            record.setTemplateType("STANDARD");
        }

        return soldierRecordRepository.save(record);
    }

    private void processStateTransition(Conversation conv, SoldierRecord record,
            String messageType, String text, String imageId) {
        String currentState = conv.getState();

        switch (currentState) {
            case STATE_START:
                String welcomeMessage = getWelcomeMessage();
                if ("HIGH_FIELD_TEMPLE".equals(record.getTemplateType())) {
                    welcomeMessage += "Highfield Temple mode\n\n";
                }
                record.setCorpsName(DEFAULT_CORPS_NAME);
                record.setEnrolledCorpsName(DEFAULT_CORPS_NAME);
                soldierRecordRepository.save(record);
                welcomeMessage += "\nI'll help place you in the right department (Home League, Men's Fellowship, Youth, Cradle Roll, etc.).\n\n"
                        + "Type 'help' anytime for assistance or 'restart' to begin again.\n\n"
                        + "Let's begin! What is your Ward?";
                sendMessage(conv.getWaId(), welcomeMessage);
                conv.setState(STATE_ASK_WARD);
                break;

            case STATE_ASK_CORPS_NAME:
            case STATE_ASK_ENROLLED_CORPS:
                record.setCorpsName(DEFAULT_CORPS_NAME);
                record.setEnrolledCorpsName(DEFAULT_CORPS_NAME);
                soldierRecordRepository.save(record);
                sendMessage(conv.getWaId(), "What is your Ward?");
                conv.setState(STATE_ASK_WARD);
                break;

            case STATE_ASK_WARD:
                if (!"text".equals(messageType) || text == null || text.trim().isEmpty()) {
                    sendMessage(conv.getWaId(), "Please provide your Ward as text.");
                    return;
                }
                record.setWard(text.trim());
                soldierRecordRepository.save(record);
                sendMessage(conv.getWaId(), "Thank you! What is your Brigade?");
                conv.setState(STATE_ASK_BRIGADE);
                break;

            case STATE_ASK_BRIGADE:
                if (!"text".equals(messageType) || text == null || text.trim().isEmpty()) {
                    sendMessage(conv.getWaId(), "Please provide your Brigade as text.");
                    return;
                }
                record.setBrigade(text.trim());
                soldierRecordRepository.save(record);
                sendMessage(conv.getWaId(), "Excellent! What is your first name?");
                conv.setState(STATE_ASK_FIRST_NAME);
                break;

            case STATE_ASK_FIRST_NAME:
                if (!"text".equals(messageType) || text == null || text.trim().isEmpty()) {
                    sendMessage(conv.getWaId(), "Please provide your first name as text.");
                    return;
                }
                record.setFirstName(text.trim());
                soldierRecordRepository.save(record);
                sendMessage(conv.getWaId(), "Thank you! What is your family name (surname)?");
                conv.setState(STATE_ASK_FAMILY_NAME);
                break;

            case STATE_ASK_FAMILY_NAME:
                if (!"text".equals(messageType) || text == null || text.trim().isEmpty()) {
                    sendMessage(conv.getWaId(), "Please provide your family name as text.");
                    return;
                }
                record.setFamilyName(text.trim());
                soldierRecordRepository.save(record);
                sendMessage(conv.getWaId(),
                        "Got it! What is your date of birth? Please use the format YYYY-MM-DD (e.g., 1990-05-15)");
                conv.setState(STATE_ASK_DOB);
                break;

            case STATE_ASK_DOB:
                if (!"text".equals(messageType) || text == null || text.trim().isEmpty()) {
                    sendMessage(conv.getWaId(), "Please provide your date of birth in YYYY-MM-DD format.");
                    return;
                }
                LocalDate dob = validateAndParseDOB(text.trim());
                if (dob == null) {
                    sendMessage(conv.getWaId(),
                            "Invalid date format or future date. Please provide your date of birth in YYYY-MM-DD format (e.g., 1990-05-15).");
                    return;
                }
                int age = Period.between(dob, LocalDate.now()).getYears();
                record.setDob(dob);
                record.setAge(age);
                soldierRecordRepository.save(record);

                if (age >= 18) {
                    sendMessage(conv.getWaId(), "Since you are 18 or older, please provide your ID number.");
                    conv.setState(STATE_ASK_ID_NUMBER);
                } else {
                    sendMessage(conv.getWaId(), "Thank you! Now, please send a photo of yourself.");
                    conv.setState(STATE_ASK_PERSON_IMAGE);
                }
                break;

            case STATE_ASK_ID_NUMBER:
                if (!"text".equals(messageType) || text == null || text.trim().isEmpty()) {
                    sendMessage(conv.getWaId(), "Please provide your ID number as text.");
                    return;
                }
                record.setIdNumber(text.trim());
                soldierRecordRepository.save(record);
                sendMessage(conv.getWaId(), "Thank you! Now, please send a photo of yourself.");
                conv.setState(STATE_ASK_PERSON_IMAGE);
                break;

            case STATE_ASK_PERSON_IMAGE:
                if (!"image".equals(messageType) || imageId == null) {
                    sendMessage(conv.getWaId(), "Please send an image of yourself.");
                    return;
                }
                String personImagePath = downloadAndSaveImage(imageId, "person_" + waIdToFilename(conv.getWaId()));
                if (personImagePath == null) {
                    sendMessage(conv.getWaId(), "Failed to download image. Please try again.");
                    return;
                }
                record.setPersonImagePath(personImagePath);
                soldierRecordRepository.save(record);
                sendMessage(conv.getWaId(), "Great! Now please send a photo of your enrollment certificate.");
                conv.setState(STATE_ASK_CERT_IMAGE);
                break;

            case STATE_ASK_CERT_IMAGE:
                if (!"image".equals(messageType) || imageId == null) {
                    sendMessage(conv.getWaId(), "Please send an image of your certificate.");
                    return;
                }
                String certImagePath = downloadAndSaveImage(imageId, "cert_" + waIdToFilename(conv.getWaId()));
                if (certImagePath == null) {
                    sendMessage(conv.getWaId(), "Failed to download image. Please try again.");
                    return;
                }
                record.setCertImagePath(certImagePath);
                soldierRecordRepository.save(record);
                sendMessage(conv.getWaId(), "Excellent! What is your favorite song?");
                conv.setState(STATE_ASK_SONG);
                break;

            case STATE_ASK_SONG:
                if (!"text".equals(messageType) || text == null || text.trim().isEmpty()) {
                    sendMessage(conv.getWaId(), "Please provide your favorite song as text.");
                    return;
                }
                record.setFavoriteSong(text.trim());
                soldierRecordRepository.save(record);
                sendMessage(conv.getWaId(), "Wonderful! Finally, what is your favorite Bible verse?");
                conv.setState(STATE_ASK_BIBLE_VERSE);
                break;

            case STATE_ASK_BIBLE_VERSE:
                if (!"text".equals(messageType) || text == null || text.trim().isEmpty()) {
                    sendMessage(conv.getWaId(), "Please provide your favorite Bible verse as text.");
                    return;
                }
                record.setFavoriteBibleVerse(text.trim());
                record.setStatus(RecordStatus.VERIFIED);
                soldierRecordRepository.save(record);
                conv.setState(STATE_COMPLETE);
                sendMessage(conv.getWaId(),
                        "🎉 Thank you! Your enrollment information has been successfully collected.\n\n" +
                                "Your data will be reviewed by our team. God bless you!");
                break;

            case STATE_COMPLETE:
                sendMessage(conv.getWaId(),
                        "You have already completed the enrollment process. Type 'restart' to start a new enrollment.");
                break;

            default:
                sendMessage(conv.getWaId(), "Something went wrong. Type 'restart' to begin again.");
                break;
        }
    }

    private LocalDate validateAndParseDOB(String dateStr) {
        try {
            LocalDate dob = LocalDate.parse(dateStr, DateTimeFormatter.ISO_LOCAL_DATE);
            if (dob.isAfter(LocalDate.now())) {
                return null;
            }
            return dob;
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    private void handleRestart(String waId) {
        // Delete existing in-progress conversation and record
        conversationRepository.findByWaId(waId).ifPresent(conversationRepository::delete);
        soldierRecordRepository.findByWaIdAndStatus(waId, RecordStatus.IN_PROGRESS)
                .ifPresent(soldierRecordRepository::delete);

        // Create new conversation
        Conversation newConv = createNewConversation(waId);
        SoldierRecord newRecord = createNewRecord(waId);

        sendMessage(waId, "Restarting enrollment process...");
        processStateTransition(newConv, newRecord, "text", null, null);
        conversationRepository.save(newConv);
    }

    private void sendHelpMessage(String waId) {
        sendMessage(waId, "📋 *HT-E ROLL BOOK Help*\n\n" +
                "This chatbot collects your enrollment information.\n\n" +
                "*Commands:*\n" +
                "• Type 'restart' to start over\n" +
                "• Type 'help' to see this message\n\n" +
                "*Privacy Note:*\n" +
                "Your information is securely stored and used only for HT-E Roll Book purposes.\n\n"
                +
                "If you encounter issues, please contact your local office.");
    }

    public String getWelcomeMessage() {
        return "🛡️ *HT-E ROLL BOOK* 🛡️\n"
                + "--------------------------\n"
                + "Welcome to the Salvation Army Data Collection System.\n\n"
                + "Please type *REGISTER* to begin.";
    }

    public void sendMessage(String waId, String message) {
        if (accessToken == null || accessToken.trim().isEmpty()) {
            logger.warn("Skipping sendMessage to {}: WHATSAPP_ACCESS_TOKEN is not configured", waId);
            return;
        }

        try {
            String endpoint = String.format("%s/%s/messages", apiUrl, phoneNumberId);
            URL url = new URL(endpoint);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Authorization", "Bearer " + accessToken);
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);

            Map<String, Object> payload = new HashMap<>();
            payload.put("messaging_product", "whatsapp");
            payload.put("recipient_type", "individual");
            payload.put("to", waId);
            payload.put("type", "text");
            payload.put("text", Map.of("body", message));

            ObjectMapper mapper = new ObjectMapper();
            String jsonPayload = mapper.writeValueAsString(payload);

            conn.getOutputStream().write(jsonPayload.getBytes());

            int responseCode = conn.getResponseCode();
            if (responseCode >= 200 && responseCode < 300) {
                ObjectMapper mapperResponse = new ObjectMapper();
                @SuppressWarnings("unchecked")
                Map<String, Object> responseMap = (Map<String, Object>) mapperResponse.readValue(conn.getInputStream(),
                        Map.class);
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> messages = (List<Map<String, Object>>) responseMap.get("messages");
                String wamid = (messages != null && !messages.isEmpty()) ? (String) messages.get(0).get("id")
                        : "unknown";
                logger.info("✅ Successfully sent message to {}: status={}, wamid={}", waId, responseCode, wamid);
            } else {
                logger.error("❌ Failed to send message to {}: status={}", waId, responseCode);
            }

            conn.disconnect();
        } catch (Exception e) {
            logger.error("Error sending message to {}: {}", waId, e.getMessage(), e);
        }
    }

    private String downloadAndSaveImage(String mediaId, String prefix) {
        try {
            // Step 1: Get media URL from WhatsApp
            String mediaUrl = getMediaUrl(mediaId);
            if (mediaUrl == null) {
                return null;
            }

            // Step 2: Download image
            URL url = new URL(mediaUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Authorization", "Bearer " + accessToken);

            if (conn.getResponseCode() != 200) {
                logger.error("Failed to download image: response code {}", conn.getResponseCode());
                return null;
            }

            // Step 3: Save to disk
            File uploadDirectory = new File(uploadDir);
            if (!uploadDirectory.exists()) {
                uploadDirectory.mkdirs();
            }

            String filename = prefix + "_" + UUID.randomUUID().toString() + ".jpg";
            File outputFile = new File(uploadDirectory, filename);

            try (InputStream in = conn.getInputStream();
                    FileOutputStream out = new FileOutputStream(outputFile)) {
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = in.read(buffer)) != -1) {
                    out.write(buffer, 0, bytesRead);
                }
            }

            conn.disconnect();
            logger.info("Saved image: {}", outputFile.getAbsolutePath());
            return filename;

        } catch (Exception e) {
            logger.error("Error downloading and saving image: {}", e.getMessage(), e);
            return null;
        }
    }

    private String getMediaUrl(String mediaId) {
        try {
            String endpoint = String.format("%s/%s", apiUrl, mediaId);
            URL url = new URL(endpoint);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Authorization", "Bearer " + accessToken);

            if (conn.getResponseCode() != 200) {
                logger.error("Failed to get media URL: response code {}", conn.getResponseCode());
                return null;
            }

            InputStream in = conn.getInputStream();
            ObjectMapper mapper = new ObjectMapper();
            Map<String, Object> response = mapper.readValue(in, Map.class);
            conn.disconnect();

            return (String) response.get("url");
        } catch (Exception e) {
            logger.error("Error getting media URL: {}", e.getMessage(), e);
            return null;
        }
    }

    private String waIdToFilename(String waId) {
        return waId.replaceAll("[^a-zA-Z0-9]", "_");
    }
}