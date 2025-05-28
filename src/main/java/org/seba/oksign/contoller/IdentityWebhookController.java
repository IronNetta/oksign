package org.seba.oksign.contoller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@Slf4j
@RequestMapping("/webhook")
@RequiredArgsConstructor
public class IdentityWebhookController {

    @PostMapping("/identity")
    public ResponseEntity<String> handleIdentityVerification(@RequestBody Map<String, Object> update) {

        log.info("Vérification d'identité itsme reçue: {}", update);

        String status = (String) update.get("status");
        String documentId = (String) update.get("documentId");
        String signerEmail = (String) update.get("signerEmail");

        switch (status) {
            case "qr_generated":
                log.info("QR Code de vérification généré pour: {}", signerEmail);
                break;

            case "qr_scanned":
                log.info("QR Code scanné par: {}", signerEmail);
                break;

            case "signed":
                log.info("Identité vérifiée avec succès pour: {}", signerEmail);
                // Ici vous pouvez marquer l'utilisateur comme vérifié dans votre système
                break;

            case "failed":
                log.warn("Échec de la vérification d'identité pour: {}", signerEmail);
                break;
        }

        return ResponseEntity.ok("identity verification processed");
    }
}