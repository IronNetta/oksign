package org.seba.oksign.dto;

import lombok.Data;

@Data
public class SignatureRequest {
    private String firstName;
    private String lastName;
    private String email;
    private String gsm;
    private String fileName = "document-signature.pdf"; // Valeur par défaut
    private String base64Content; // Optionnel
}
