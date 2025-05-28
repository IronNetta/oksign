package org.seba.oksign.contoller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.seba.oksign.dto.IdentityVerificationRequest;
import org.seba.oksign.service.ItsmeIdentityService;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/identity")
@RequiredArgsConstructor
public class IdentityVerificationController {

    private final ItsmeIdentityService itsmeIdentityService;

    @GetMapping("/verify")
    public String showVerificationForm() {
        return "identity-verification";
    }

    @PostMapping("/generate-qr")
    public String generateQRCode(@Valid @ModelAttribute IdentityVerificationRequest request, Model model) {
        ResponseEntity<String> response = itsmeIdentityService.generateItsmeQRCode(request);

        if (response.getStatusCode().is2xxSuccessful()) {
            model.addAttribute("success", true);
            model.addAttribute("message", "QR Code itsme généré ! Consultez votre email pour le lien de vérification.");
        } else {
            model.addAttribute("success", false);
            model.addAttribute("message", "Erreur lors de la génération du QR Code: " + response.getBody());
        }

        return "identity-result";
    }
}

