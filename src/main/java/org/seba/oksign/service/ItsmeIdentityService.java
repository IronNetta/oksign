package org.seba.oksign.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.seba.oksign.config.OksignProperties;
import org.seba.oksign.dto.IdentityVerificationRequest;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class ItsmeIdentityService {

    private final OksignProperties oksignProperties;
    private final RestTemplate restTemplate = new RestTemplate();

    public ResponseEntity<String> generateItsmeQRCode(IdentityVerificationRequest request) {
        // Créer un document vide minimal juste pour déclencher itsme
        String docId = createEmptyDocument();
        if (docId == null) {
            return ResponseEntity.badRequest().body("Erreur lors de la création du document");
        }

        return createItsmeVerification(docId, request);
    }

    private String createEmptyDocument() {
        try {
            // Document PDF minimal vide (juste pour déclencher le processus itsme)
            String minimalPdf = "JVBERi0xLjQKJeLjz9MKMSAwIG9iago8PC9UeXBlIC9DYXRhbG9nL1BhZ2VzIDIgMCBSPj4KZW5kb2JqCjIgMCBvYmo8PC9UeXBlIC9QYWdlcy9LaWRzIFszIDAgUl0vQ291bnQgMT4+CmVuZG9iagozIDAgb2JqPDwvVHlwZSAvUGFnZS9QYXJlbnQgMiAwIFIvTWVkaWFCb3ggWzAgMCA2MTIgNzkyXT4+CmVuZG9iagp4cmVmCjAgNAowMDAwMDAwMDAwIDY1NTM1IGYgCjAwMDAwMDAwMTMgMDAwMDAgbiAKMDAwMDAwMDA2MyAwMDAwMCBuIAowMDAwMDAwMTIwIDAwMDAwIG4gCnRyYWlsZXIKPDwvU2l6ZSA0L1Jvb3QgMSAwIFI+PgpzdGFydHhyZWYKMTgwCiUlRU9G";

            byte[] pdfBytes = Base64.getDecoder().decode(minimalPdf);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.set("x-oksign-authorization", oksignProperties.getApiKey());
            headers.set("x-oksign-filename", "identity-verification.pdf");
            headers.set("accept", "application/json; charset=utf-8");

            HttpEntity<byte[]> entity = new HttpEntity<>(pdfBytes, headers);
            String url = oksignProperties.getBaseUrl() + "document/upload";

            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody().contains("\"status\": \"OK\"")) {
                String responseBody = response.getBody();
                int startIndex = responseBody.indexOf("\"reason\": \"") + 11;
                int endIndex = responseBody.indexOf("\"", startIndex);
                return responseBody.substring(startIndex, endIndex);
            }

            return null;
        } catch (Exception e) {
            log.error("Erreur lors de la création du document: ", e);
            return null;
        }
    }

    private ResponseEntity<String> createItsmeVerification(String docId, IdentityVerificationRequest request) {
        try {
            // Configuration du signataire
            Map<String, Object> signerInfo = new HashMap<>();
            signerInfo.put("name", request.getFirstName() + " " + request.getLastName());
            signerInfo.put("email", request.getEmail());
            signerInfo.put("mobile", request.getGsm());
            signerInfo.put("actingas", "Vérification d'identité");
            signerInfo.put("id", "bt_00000000-0000-0000-0000-000000000001");

            // Champ de signature invisible (juste pour déclencher itsme)
            Map<String, Object> signatureField = new HashMap<>();
            signatureField.put("inputtype", "CanvasSIG");
            signatureField.put("name", "IDENTITY_VERIFICATION");
            signatureField.put("required", true);
            signatureField.put("pagenbr", 0);
            signatureField.put("posX", 1);
            signatureField.put("posY", 1);
            signatureField.put("width", 1);
            signatureField.put("height", 1);
            signatureField.put("signerid", "bt_00000000-0000-0000-0000-000000000001");

            // Configuration itsme UNIQUEMENT
            Map<String, Object> signingOptions = new HashMap<>();
            Map<String, Object> itsmeConfig = new HashMap<>();
            itsmeConfig.put("enabled", true);
            signingOptions.put("itsme", itsmeConfig);
            signatureField.put("signingoptions", signingOptions);

            // FormDescriptor
            Map<String, Object> formDescriptor = new HashMap<>();
            formDescriptor.put("reusable", false);
            formDescriptor.put("fields", List.of(signatureField));
            formDescriptor.put("signersinfo", List.of(signerInfo));

            // Message personnalisé pour vérification d'identité
            Map<String, Object> notifications = new HashMap<>();
            Map<String, Object> smtpNotif = new HashMap<>();
            smtpNotif.put("sender", "Vérification d'identité");
            smtpNotif.put("to", List.of("bt_00000000-0000-0000-0000-000000000001"));
            smtpNotif.put("subject", "Vérification de votre identité via itsme");
            smtpNotif.put("body", "<h2>Vérification d'identité</h2>" +
                    "<p>Scannez le QR code avec votre application itsme pour confirmer votre identité.</p>");
            smtpNotif.put("language", "fr");
            notifications.put("smtp", smtpNotif);
            formDescriptor.put("notifications", notifications);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("x-oksign-authorization", oksignProperties.getApiKey());
            headers.set("x-oksign-docid", docId);
            headers.set("accept", "application/json; charset=utf-8");

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(formDescriptor, headers);
            String url = oksignProperties.getBaseUrl() + "formdesc/upload";

            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);
            log.info("QR Code itsme généré: {}", response.getBody());

            return response;

        } catch (Exception e) {
            log.error("Erreur lors de la génération du QR Code itsme: ", e);
            return ResponseEntity.status(500).body("Erreur lors de la génération du QR Code itsme");
        }
    }
}