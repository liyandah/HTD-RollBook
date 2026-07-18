package org.salvationarmy.whatsapp.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.auth.oauth2.GoogleCredentials;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

/**
 * Service to interact with Dialogflow ES DetectIntent API using REST.
 * 
 * This service sends user messages to Dialogflow and retrieves bot responses.
 * It uses the Dialogflow REST API and requires:
 * - GOOGLE_APPLICATION_CREDENTIALS environment variable pointing to service account JSON
 * - dialogflow.project-id configured in application.properties
 */
@Service
@Slf4j
public class DialogflowClientService {

    @Value("${dialogflow.project-id:}")
    private String projectId;

    @Value("${dialogflow.language-code:en}")
    private String languageCode;

    private static final String DIALOGFLOW_API_URL = "https://dialogflow.googleapis.com/v2/projects/%s/agent/sessions/%s:detectIntent";
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Sends a text message to Dialogflow and returns the fulfillment text.
     * 
     * @param sessionId Unique session identifier (e.g., UUID)
     * @param messageText User's message text
     * @return Bot's response text, or fallback message if empty
     */
    public String detectIntent(String sessionId, String messageText) {
        // Validate project ID
        if (projectId == null || projectId.trim().isEmpty()) {
            log.error("Dialogflow project-id is not configured");
            return "Sorry, the chat service is not properly configured. Please contact support.";
        }

        try {
            // Get access token
            String accessToken = getAccessToken();
            if (accessToken == null) {
                log.error("Failed to obtain Google Cloud access token");
                return "Sorry, authentication failed. Please check configuration.";
            }

            // Build request URL
            String urlString = String.format(DIALOGFLOW_API_URL, projectId, sessionId);
            URL url = new URL(urlString);

            // Build request body
            String requestBody = buildRequestBody(messageText);

            // Make HTTP POST request
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Authorization", "Bearer " + accessToken);
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setDoOutput(true);

            // Send request
            connection.getOutputStream().write(requestBody.getBytes(StandardCharsets.UTF_8));

            // Read response
            int responseCode = connection.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                String errorResponse = readErrorResponse(connection);
                log.error("Dialogflow API error ({}): {}", responseCode, errorResponse);
                return "Sorry, I'm having trouble connecting. Please try again.";
            }

            String responseBody = readResponse(connection);
            JsonNode responseJson = objectMapper.readTree(responseBody);
            
            // Extract fulfillment text
            JsonNode queryResult = responseJson.path("queryResult");
            String fulfillmentText = queryResult.path("fulfillmentText").asText();
            
            // Log intent if it's REG_CONFIRM (without personal data)
            JsonNode intent = queryResult.path("intent");
            if (!intent.isMissingNode()) {
                String intentName = intent.path("displayName").asText();
                if ("REG_CONFIRM".equals(intentName) || "REG_SUBMIT".equals(intentName)) {
                    log.info("Dialogflow matched intent: {} for session {}", intentName, sessionId);
                }
            }

            // Return fulfillment text or fallback
            if (fulfillmentText != null && !fulfillmentText.trim().isEmpty()) {
                return fulfillmentText;
            } else {
                log.warn("Dialogflow returned empty fulfillment text for session: {}", sessionId);
                return "Sorry, I didn't understand. Please try again.";
            }

        } catch (IOException e) {
            log.error("Error calling Dialogflow DetectIntent API: ", e);
            return "Sorry, I'm having trouble connecting. Please try again.";
        } catch (Exception e) {
            log.error("Unexpected error in Dialogflow service: ", e);
            return "Sorry, an error occurred. Please try again.";
        }
    }

    private String getAccessToken() throws IOException {
        String credentialsPath = System.getenv("GOOGLE_APPLICATION_CREDENTIALS");
        if (credentialsPath == null || credentialsPath.trim().isEmpty()) {
            log.error("GOOGLE_APPLICATION_CREDENTIALS environment variable is not set");
            return null;
        }

        try (FileInputStream serviceAccountStream = new FileInputStream(credentialsPath)) {
            GoogleCredentials credentials = GoogleCredentials.fromStream(serviceAccountStream)
                    .createScoped("https://www.googleapis.com/auth/cloud-platform");
            credentials.refreshIfExpired();
            return credentials.getAccessToken().getTokenValue();
        }
    }

    private String buildRequestBody(String messageText) {
        return String.format(
            "{\"queryInput\":{\"text\":{\"text\":\"%s\",\"languageCode\":\"%s\"}}}",
            escapeJson(messageText),
            languageCode
        );
    }

    private String escapeJson(String text) {
        return text.replace("\\", "\\\\")
                   .replace("\"", "\\\"")
                   .replace("\n", "\\n")
                   .replace("\r", "\\r")
                   .replace("\t", "\\t");
    }

    private String readResponse(HttpURLConnection connection) throws IOException {
        try (Scanner scanner = new Scanner(connection.getInputStream(), StandardCharsets.UTF_8.name())) {
            scanner.useDelimiter("\\A");
            return scanner.hasNext() ? scanner.next() : "";
        }
    }

    private String readErrorResponse(HttpURLConnection connection) {
        try {
            if (connection.getErrorStream() != null) {
                try (Scanner scanner = new Scanner(connection.getErrorStream(), StandardCharsets.UTF_8.name())) {
                    scanner.useDelimiter("\\A");
                    return scanner.hasNext() ? scanner.next() : "";
                }
            }
        } catch (Exception e) {
            log.warn("Failed to read error response: ", e);
        }
        return "Unknown error";
    }
}
