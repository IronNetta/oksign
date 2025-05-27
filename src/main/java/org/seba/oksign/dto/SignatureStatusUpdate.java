package org.seba.oksign.dto;

import lombok.Data;

@Data
public class SignatureStatusUpdate {
    private String event;
    private String documentId;
    private String signerEmail;
    private String status;
}
