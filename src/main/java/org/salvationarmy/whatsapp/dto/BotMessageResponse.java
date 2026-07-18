package org.salvationarmy.whatsapp.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BotMessageResponse {
    private String replyText;
    private String state;
    private String status;
    private String declineReason;
    private List<String> choices;
    private String memberStatus;
    private String memberFirstName;
    private String memberLastName;
    private String memberRecordCode;
    private String memberDepartment;
    private String personImagePath;
}
