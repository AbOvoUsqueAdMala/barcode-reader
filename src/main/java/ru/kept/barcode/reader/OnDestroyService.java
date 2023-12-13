package ru.kept.barcode.reader;

import jakarta.annotation.PreDestroy;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

@Service
@Log4j2
public class OnDestroyService {

    FileService fileService = new FileService();
    @PreDestroy
    public void onDestroy() {
        fileService.deleteAllFiles();
        log.info("All temp files are deleted");
    }

}
