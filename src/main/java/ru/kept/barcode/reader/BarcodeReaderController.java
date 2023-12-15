package ru.kept.barcode.reader;

import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("")
public class BarcodeReaderController {

    @PostMapping("read-pdf")
    public ResponseEntity<?> handleFileUpload(@RequestParam("file") MultipartFile file) throws IOException {

        if (file.isEmpty())
            return ResponseEntity.notFound().build();

        return ResponseEntity.ok(PdfBarcodeReaderUtil.readBarcodeFromPdf(file));

    }

    @GetMapping("check")
    public ResponseEntity<String> checkAvailability(){
        return ResponseEntity.ok("ok");
    }

    @GetMapping("get-pdf")
    public ResponseEntity<Resource> getImage(@RequestParam String fileName) throws IOException {
        return FileService.getPdf(fileName);
    }

}
