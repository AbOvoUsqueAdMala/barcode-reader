package ru.kept.barcode.reader;

import lombok.extern.log4j.Log4j2;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

@Service
@Log4j2
public class FileService {

    private static final String TMP = System.getProperty("java.io.tmpdir");
    private static final Set<File> receivedFiles = Collections.synchronizedSet(new HashSet<>());
    private static final Set<File> allTempFiles = Collections.synchronizedSet(new HashSet<>());

    public static ResponseEntity<Resource> getPdf(String fileName) throws IOException {

        Path path = Paths.get(TMP, fileName);

        if (!Files.exists(path)) {
            return ResponseEntity
                    .notFound()
                    .build();
        }

        Resource resource = new ByteArrayResource(Files.readAllBytes(path));

        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + path.getFileName());
        headers.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_PDF_VALUE);

        addFile(new File(path.toString()));

        return ResponseEntity
                .ok()
                .headers(headers)
                .contentLength(Files.size(path))
                .body(resource);

    }

    public static synchronized void addFile(File file) {
        receivedFiles.add(file);
    }

    public static synchronized void deleteReceivedFiles() {

        receivedFiles.forEach(file -> {
            file.delete();
            log.info("file " + file.getAbsolutePath() + " deleted");
            });

        receivedFiles.clear();

    }

    public static synchronized void deleteAllFiles() {

        allTempFiles.forEach(file -> {
            file.delete();
            log.info("file " + file.getAbsolutePath() + " deleted");
        });

        allTempFiles.clear();

    }

    @Scheduled(fixedRate=10000)
    public void deleteFiles() {
        deleteReceivedFiles();
    }

}