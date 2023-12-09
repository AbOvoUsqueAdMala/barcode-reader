package ru.kept.barcode.reader;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("")
public class BarcodeReader {

    @PostMapping("/read-pdf")
    public ResponseEntity<?> handleFileUpload(@RequestParam("file") MultipartFile file) throws IOException {

        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body("Пожалуйста, выберите файл");
        }

        List<BarcodeInformation> barcodeInformation  = PdfBarcodeReader.readBarcodeFromPdf(file);

        return ResponseEntity.ok(barcodeInformation);
    }

}
