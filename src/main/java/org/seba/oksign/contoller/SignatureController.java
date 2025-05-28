package org.seba.oksign.contoller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.seba.oksign.dto.SignatureRequest;
import org.seba.oksign.dto.SignatureStatusUpdate;
import org.seba.oksign.service.OksignService;
import org.seba.oksign.service.PdfGeneratorService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.Base64;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/signatures")
@RequiredArgsConstructor
public class SignatureController {

    private final OksignService oksignService;
    private final PdfGeneratorService pdfGeneratorService;

    @PostMapping
    public ResponseEntity<String> sendSignature(@RequestBody SignatureRequest request) {
        return oksignService.sendDocumentForSignature(request);
    }

    @PostMapping("/generate-pdf")
    public ResponseEntity<byte[]> generatePdf(@RequestBody SignatureRequest request) {
        try {
            byte[] pdfBytes = pdfGeneratorService.generateSignaturePdf(request);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", "document-signature.pdf");
            
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(pdfBytes);
        } catch (IOException e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}

@Slf4j
@Controller
@RequestMapping("/web")
@RequiredArgsConstructor
class WebSignatureController {
    private final OksignService oksignService;
    private final PdfGeneratorService pdfGeneratorService;

    @GetMapping("/form")
    public String showForm() {
        return "signature-form";
    }

    @PostMapping("/submit")
    public String submitForm(@ModelAttribute SignatureRequest request, Model model) {
        try {
            // Générer le PDF avec les informations du formulaire
            byte[] pdfBytes = pdfGeneratorService.generateSignaturePdf(request);
            String base64Pdf = Base64.getEncoder().encodeToString(pdfBytes);
            
            // Mettre à jour la requête avec le PDF généré
            request.setBase64Content(base64Pdf);
            request.setFileName("document-" + System.currentTimeMillis() + ".pdf");
            
            // Envoyer à OKSign pour signature avec itsme
            ResponseEntity<String> response = oksignService.sendDocumentForSignature(request);
            
            model.addAttribute("response", response.getBody());
            model.addAttribute("statusCode", response.getStatusCode().value());
            
            // Extraire l'URL de signature si disponible
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                // Parser la réponse pour extraire l'URL de signature depuis la réponse FormDescriptor
                String responseBody = response.getBody();
                log.info("Response body pour extraction URL: {}", responseBody);
                
                // La réponse contient un tableau "reason" avec l'URL de signature
                if (responseBody.contains("\"url\":") || responseBody.contains("\"url\": ")) {
                    // Chercher avec ou sans espace après les deux-points
                    int urlStart = responseBody.indexOf("\"url\":");
                    if (urlStart != -1) {
                        // Avancer jusqu'au début de la valeur de l'URL
                        urlStart = responseBody.indexOf("\"", urlStart + 6) + 1;
                        int urlEnd = responseBody.indexOf("\"", urlStart);
                        
                        if (urlStart > 0 && urlEnd > urlStart) {
                            String signingUrl = responseBody.substring(urlStart, urlEnd);
                            // Remplacer les échappements JSON
                            signingUrl = signingUrl.replace("\\/", "/");
                            log.info("URL de signature extraite: {}", signingUrl);
                            
                            // Rediriger directement vers l'URL de signature
                            return "redirect:" + signingUrl;
                        }
                    }
                }
            }
            
            // En cas d'erreur, afficher la page de résultat
            model.addAttribute("error", "Impossible d'extraire l'URL de signature");
            return "signature-result";
        } catch (IOException e) {
            model.addAttribute("error", "Erreur lors de la génération du PDF: " + e.getMessage());
            return "signature-result";
        }
    }
    
    @PostMapping("/sign-document")
    public String signDocument(@ModelAttribute SignatureRequest request, 
                             @RequestParam("signatureImage") String signatureImage,
                             Model model) {
        try {
            // Générer le PDF final avec la signature
            byte[] pdfBytes = pdfGeneratorService.generateSignaturePdf(request, signatureImage);
            String base64Pdf = Base64.getEncoder().encodeToString(pdfBytes);
            
            model.addAttribute("pdfBase64", base64Pdf);
            model.addAttribute("fileName", "document-signe.pdf");
            return "signature-result";
        } catch (IOException e) {
            model.addAttribute("error", "Erreur lors de la signature du document: " + e.getMessage());
            return "signature-result";
        }
    }

    @GetMapping("/signature-complete")
    public String signatureComplete(@RequestParam(required = false) String status,
                                  @RequestParam(required = false) String docid,
                                  Model model) {
        model.addAttribute("status", status);
        model.addAttribute("docid", docid);
        return "signature-complete";
    }
    
    @PostMapping("/webhook")
    public ResponseEntity<String> handleWebhook(@RequestBody SignatureStatusUpdate update, HttpServletRequest req) {
        // À sécuriser avec secret partagé ou signature
        System.out.println("Webhook reçu : " + update);
        return ResponseEntity.ok("received");
    }
}