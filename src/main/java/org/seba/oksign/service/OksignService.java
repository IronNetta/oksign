package org.seba.oksign.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.seba.oksign.config.OksignProperties;
import org.seba.oksign.dto.SignatureRequest;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class OksignService {

    private final OksignProperties oksignProperties;
    private final RestTemplate restTemplate = new RestTemplate();

    public ResponseEntity<String> sendDocumentForSignature(SignatureRequest request) {
        // 1. ÉTAPE 1: Upload du document d'abord
        String docId = uploadDocument(request);
        if (docId == null) {
            return ResponseEntity.badRequest().body("Erreur lors de l'upload du document");
        }

        // 2. ÉTAPE 2: Upload du FormDescriptor avec les champs de signature
        return uploadFormDescriptor(docId, request);
    }

    private String uploadDocument(SignatureRequest request) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.set("x-oksign-authorization", oksignProperties.getApiKey());
            headers.set("x-oksign-filename", request.getFileName());
            headers.set("accept", "application/json; charset=utf-8");

            // Nettoyer et valider le Base64
            String cleanBase64 = cleanBase64String(request.getBase64Content());
            log.info("Base64 nettoyé, longueur: {}", cleanBase64.length());

            // Decoder le Base64 pour envoyer le PDF binaire
            byte[] pdfBytes = Base64.getDecoder().decode(cleanBase64);

            HttpEntity<byte[]> entity = new HttpEntity<>(pdfBytes, headers);

            String url = oksignProperties.getBaseUrl() + "document/upload";
            log.info("Upload document vers URL: {}", url);

            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);
            log.info("Response upload: {}", response.getBody());

            if (response.getStatusCode().is2xxSuccessful() && response.getBody().contains("\"status\": \"OK\"")) {
                // Extraire le docId de la réponse
                String responseBody = response.getBody();
                int startIndex = responseBody.indexOf("\"reason\": \"") + 11;
                int endIndex = responseBody.indexOf("\"", startIndex);
                return responseBody.substring(startIndex, endIndex);
            }

            return null;
        } catch (Exception e) {
            log.error("Erreur lors de l'upload du document: ", e);
            return null;
        }
    }

    private ResponseEntity<String> uploadFormDescriptor(String docId, SignatureRequest request) {
        try {
            // Créer le FormDescriptor selon la documentation OKSign
            Map<String, Object> signerInfo = new HashMap<>();
            signerInfo.put("name", request.getFirstName() + " " + request.getLastName());
            signerInfo.put("email", request.getEmail());
            signerInfo.put("mobile", request.getGsm());
            signerInfo.put("actingas", "");
            signerInfo.put("id", "bt_00000000-0000-0000-0000-000000000001");

            Map<String, Object> signatureField = new HashMap<>();
            signatureField.put("inputtype", "CanvasSIG");
            signatureField.put("name", "SIG_FIELD_1");
            signatureField.put("required", true);
            signatureField.put("pagenbr", 0);
            signatureField.put("posX", 100);
            signatureField.put("posY", 100);
            signatureField.put("width", 175);
            signatureField.put("height", 70);
            signatureField.put("signerid", "bt_00000000-0000-0000-0000-000000000001");

            Map<String, Object> signingOptions = new HashMap<>();
            signingOptions.put("itsme", new HashMap<>());
            signatureField.put("signingoptions", signingOptions);

            Map<String, Object> formDescriptor = new HashMap<>();
            formDescriptor.put("reusable", false);
            formDescriptor.put("fields", List.of(signatureField));
            formDescriptor.put("signersinfo", List.of(signerInfo));

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("x-oksign-authorization", oksignProperties.getApiKey());
            headers.set("x-oksign-docid", docId);
            headers.set("accept", "application/json; charset=utf-8");

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(formDescriptor, headers);

            String url = oksignProperties.getBaseUrl() + "formdesc/upload";
            log.info("Upload FormDescriptor vers URL: {}", url);
            log.info("FormDescriptor: {}", formDescriptor);

            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);
            log.info("Response FormDescriptor: {}", response.getBody());

            return response;

        } catch (Exception e) {
            log.error("Erreur lors de l'upload du FormDescriptor: ", e);
            return ResponseEntity.status(500).body("Erreur lors de l'upload du FormDescriptor");
        }
    }

    private String cleanBase64String(String base64Input) {
        if (base64Input == null || base64Input.trim().isEmpty()) {
            throw new IllegalArgumentException("Le contenu Base64 est vide");
        }

        // Supprimer les espaces, retours à la ligne et autres caractères invisibles
        String cleaned = base64Input.replaceAll("\\s", "");

        // Supprimer le préfixe data:application/pdf;base64, s'il existe
        if (cleaned.startsWith("data:")) {
            int commaIndex = cleaned.indexOf(",");
            if (commaIndex != -1) {
                cleaned = cleaned.substring(commaIndex + 1);
            }
        }

        // Vérifier que la chaîne ne contient que des caractères Base64 valides
        if (!cleaned.matches("^[A-Za-z0-9+/]*={0,2}$")) {
            throw new IllegalArgumentException("La chaîne Base64 contient des caractères invalides");
        }

        // Ajouter le padding si nécessaire
        while (cleaned.length() % 4 != 0) {
            cleaned += "=";
        }

        return cleaned;
    }
}