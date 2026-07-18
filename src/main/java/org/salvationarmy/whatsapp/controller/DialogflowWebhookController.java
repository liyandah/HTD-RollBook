package org.salvationarmy.whatsapp.controller;

import lombok.extern.slf4j.Slf4j;
import org.salvationarmy.whatsapp.service.SoldierRegistrationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Controller to handle Dialogflow webhook requests.
 *
 * How to configure Dialogflow Fulfillment:
 * 1. Go to Dialogflow Console -> Fulfillment.
 * 2. Enable Webhook.
 * 3. Set URL to: <YOUR_DOMAIN>/api/dialogflow/webhook
 * (e.g., https://your-ngrok-url.ngrok-free.app/api/dialogflow/webhook)
 * 4. Save.
 *
 * Example Request (Dialogflow ES v2):
 * {
 * "responseId": "...",
 * "session": "projects/.../sessions/sessionId",
 * "queryResult": {
 * "intent": { "displayName": "REG_CONFIRM" },
 * "parameters": {
 * "firstName": "John",
 * "lastName": "Doe",
 * "dob": "1990-01-01",
 * "idNumber": "12345"
 * }
 * }
 * }
 */
@RestController
@RequestMapping("/api/dialogflow")
@Slf4j
public class DialogflowWebhookController {

    @Autowired
    private SoldierRegistrationService registrationService;

    @PostMapping("/webhook")
    @SuppressWarnings("unchecked")
    public ResponseEntity<Map<String, Object>> handleWebhook(@RequestBody Map<String, Object> request) {
        try {
            Map<String, Object> queryResult = (Map<String, Object>) request.get("queryResult");
            if (queryResult == null) {
                log.warn("Received webhook request without queryResult");
                return ResponseEntity.ok(createResponse("OK"));
            }

            Map<String, Object> intent = (Map<String, Object>) queryResult.get("intent");
            if (intent == null) {
                return ResponseEntity.ok(createResponse("OK"));
            }

            String intentName = (String) intent.get("displayName");
            String sessionId = (String) request.get("session");

            // Extract session ID from full path "projects/.../sessions/..."
            if (sessionId != null && sessionId.contains("/sessions/")) {
                sessionId = sessionId.substring(sessionId.lastIndexOf("/sessions/") + 10);
            }

            if ("REG_CONFIRM".equals(intentName) || "REG_SUBMIT".equals(intentName)) {
                log.info("Received final registration intent: {} for session: {}", intentName, sessionId);

                Map<String, Object> parameters = (Map<String, Object>) queryResult.get("parameters");
                if (parameters == null) {
                    return ResponseEntity.ok(createResponse("Missing registration parameters, please restart."));
                }

                // Validate basic fields
                Object firstName = parameters.get("firstName");
                Object lastName = parameters.get("lastName");

                if (firstName == null || "".equals(firstName.toString().trim()) || 
                    lastName == null || "".equals(lastName.toString().trim())) {
                    return ResponseEntity.ok(createResponse("Missing name details, please restart."));
                }

                registrationService.saveFromDialogflow(parameters, sessionId);
                return ResponseEntity.ok(createResponse("Registration saved successfully. God bless you."));
            }

            return ResponseEntity.ok(createResponse("OK"));

        } catch (Exception e) {
            log.error("Error processing Dialogflow webhook: ", e);
            // Return 200 to avoid Dialogflow showing error
            return ResponseEntity.ok(createResponse("An error occurred but we received your request."));
        }
    }

    private Map<String, Object> createResponse(String text) {
        Map<String, Object> response = new HashMap<>();
        response.put("fulfillmentText", text);
        return response;
    }
}
