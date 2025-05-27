package org.seba.oksign.dto;

import lombok.Data;

@Data
public class SignatureRequest {
    private String firstName;
    private String lastName;
    private String email;
    private String gsm;
    private String fileName;
    private String base64Content;
}
