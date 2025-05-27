package org.seba.oksign.contoller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.seba.oksign.dto.SignatureRequest;
import org.seba.oksign.dto.SignatureStatusUpdate;
import org.seba.oksign.service.OksignService;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/signatures")
@RequiredArgsConstructor
public class SignatureController {

    private final OksignService oksignService;

    @PostMapping
    public ResponseEntity<String> sendSignature(@RequestBody SignatureRequest request) {
        return oksignService.sendDocumentForSignature(request);
    }
}

@Controller
@RequestMapping("/web")
@RequiredArgsConstructor
class WebSignatureController {
    private final OksignService oksignService;

    @GetMapping("/form")
    public String showForm() {
        return "signature-form";
    }

    @PostMapping("/submit")
    public String submitForm(@ModelAttribute SignatureRequest request, Model model) {
        ResponseEntity<String> response = oksignService.sendDocumentForSignature(request);
        model.addAttribute("response", response.getBody());
        return "signature-result";
    }

    @PostMapping("/webhook")
    public ResponseEntity<String> handleWebhook(@RequestBody SignatureStatusUpdate update, HttpServletRequest req) {
        // À sécuriser avec secret partagé ou signature
        System.out.println("Webhook reçu : " + update);
        return ResponseEntity.ok("received");
    }
}