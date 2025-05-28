package org.seba.oksign.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.seba.oksign.dto.SignatureRequest;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;

@Slf4j
@Service
@RequiredArgsConstructor
public class PdfGeneratorService {

    public byte[] generateSignaturePdf(SignatureRequest request) throws IOException {
        return generateSignaturePdf(request, null);
    }
    
    public byte[] generateSignaturePdf(SignatureRequest request, String signatureImageBase64) throws IOException {
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);

            try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                // Titre
                contentStream.beginText();
                contentStream.setFont(PDType1Font.HELVETICA_BOLD, 18);
                contentStream.setLeading(20f);
                contentStream.newLineAtOffset(50, 750);
                contentStream.showText("Document de Signature");
                contentStream.endText();

                // Informations du signataire
                contentStream.beginText();
                contentStream.setFont(PDType1Font.HELVETICA, 12);
                contentStream.setLeading(15f);
                contentStream.newLineAtOffset(50, 680);
                
                contentStream.showText("Informations du signataire:");
                contentStream.newLine();
                contentStream.newLine();
                contentStream.showText("Nom: " + request.getLastName());
                contentStream.newLine();
                contentStream.showText("Prenom: " + request.getFirstName());
                contentStream.newLine();
                contentStream.showText("Email: " + request.getEmail());
                contentStream.newLine();
                contentStream.showText("Telephone: " + request.getGsm());
                contentStream.newLine();
                contentStream.newLine();
                contentStream.showText("Date: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
                contentStream.endText();

                // Zone de signature
                contentStream.beginText();
                contentStream.setFont(PDType1Font.HELVETICA, 10);
                contentStream.newLineAtOffset(50, 300);
                contentStream.showText("Signature:");
                contentStream.endText();

                // Si une signature est fournie, l'ajouter au PDF
                if (signatureImageBase64 != null && !signatureImageBase64.isEmpty()) {
                    try {
                        // Retirer le préfixe data:image/png;base64, si présent
                        String base64Image = signatureImageBase64;
                        if (base64Image.contains(",")) {
                            base64Image = base64Image.split(",")[1];
                        }
                        
                        byte[] imageBytes = Base64.getDecoder().decode(base64Image);
                        PDImageXObject signatureImage = PDImageXObject.createFromByteArray(document, imageBytes, "signature");
                        
                        // Dessiner l'image de signature
                        contentStream.drawImage(signatureImage, 50, 200, 200, 80);
                    } catch (Exception e) {
                        log.error("Erreur lors de l'ajout de la signature", e);
                        // En cas d'erreur, dessiner le rectangle vide
                        contentStream.setLineWidth(1f);
                        contentStream.addRect(50, 200, 200, 80);
                        contentStream.stroke();
                    }
                } else {
                    // Rectangle pour la zone de signature
                    contentStream.setLineWidth(1f);
                    contentStream.addRect(50, 200, 200, 80);
                    contentStream.stroke();
                    
                    // Instructions
                    contentStream.beginText();
                    contentStream.setFont(PDType1Font.HELVETICA, 10);
                    contentStream.newLineAtOffset(50, 180);
                    contentStream.showText("Veuillez signer dans la zone ci-dessus");
                    contentStream.endText();
                }
            }

            // Note: Suppression de la logique d'attachement de document base64
            // Le PDF généré contient uniquement les informations du formulaire

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            document.save(baos);
            return baos.toByteArray();
        }
    }

}