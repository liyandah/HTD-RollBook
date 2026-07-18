package org.salvationarmy.whatsapp.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class WhatsAppWebhookRequest {

    @JsonProperty("object")
    private String object;

    @JsonProperty("entry")
    private List<Entry> entry;

    @Data
    public static class Entry {
        @JsonProperty("id")
        private String id;

        @JsonProperty("changes")
        private List<Change> changes;
    }

    @Data
    public static class Change {
        @JsonProperty("value")
        private Value value;

        @JsonProperty("field")
        private String field;
    }

    @Data
    public static class Value {
        @JsonProperty("messaging_product")
        private String messagingProduct;

        @JsonProperty("metadata")
        private Metadata metadata;

        @JsonProperty("contacts")
        private List<Contact> contacts;

        @JsonProperty("messages")
        private List<Message> messages;

        @JsonProperty("statuses")
        private List<Status> statuses;
    }

    @Data
    public static class Status {
        @JsonProperty("id")
        private String id;

        @JsonProperty("status")
        private String status;

        @JsonProperty("timestamp")
        private String timestamp;

        @JsonProperty("recipient_id")
        private String recipientId;
    }

    @Data
    public static class Metadata {
        @JsonProperty("display_phone_number")
        private String displayPhoneNumber;

        @JsonProperty("phone_number_id")
        private String phoneNumberId;
    }

    @Data
    public static class Contact {
        @JsonProperty("profile")
        private Profile profile;

        @JsonProperty("wa_id")
        private String waId;
    }

    @Data
    public static class Profile {
        @JsonProperty("name")
        private String name;
    }

    @Data
    public static class Message {
        @JsonProperty("from")
        private String from;

        @JsonProperty("id")
        private String id;

        @JsonProperty("timestamp")
        private String timestamp;

        @JsonProperty("type")
        private String type;

        @JsonProperty("text")
        private TextMessage text;

        @JsonProperty("image")
        private ImageMessage image;
    }

    @Data
    public static class TextMessage {
        @JsonProperty("body")
        private String body;
    }

    @Data
    public static class ImageMessage {
        @JsonProperty("caption")
        private String caption;

        @JsonProperty("mime_type")
        private String mimeType;

        @JsonProperty("sha256")
        private String sha256;

        @JsonProperty("id")
        private String id;
    }
}
