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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
@Log4j2
public class FileService {

    private final List<File> filesForDeleting = Collections.synchronizedList(new ArrayList<>());

    public ResponseEntity<Resource> getPdf(String fileName) throws IOException {

        Path path = Paths.get(System.getProperty("java.io.tmpdir"), fileName);
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

    public synchronized void addFile(File file) {
        filesForDeleting.add(file);
    }

    public void deleteFile(String absolutePath) {
        File file = getFileByName(absolutePath);
        if (file.delete()) {
            filesForDeleting.remove(file);
        }
    }

    public File getFileByName(String absolutePath) {
        return filesForDeleting.stream()
                .filter(file -> file.getAbsolutePath().equals(absolutePath))
                .findFirst()
                .orElse(null);
    }

    public synchronized void deleteAllFiles() {
        filesForDeleting.forEach(file -> log.info("file " + file.getAbsolutePath() + " deleted"));
    }

    @Scheduled(fixedRate=10000)
    public void deleteFiles() {
        deleteAllFiles();
    }

}