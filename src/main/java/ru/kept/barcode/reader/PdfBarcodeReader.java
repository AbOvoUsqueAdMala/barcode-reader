package ru.kept.barcode.reader;

import com.google.zxing.*;
import lombok.extern.log4j.Log4j2;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.io.RandomAccessReadBufferedFile;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@Log4j2
public class PdfBarcodeReader {

    public static List<BarcodeInformation> readBarcodeFromPdf(MultipartFile multipartFile) throws IOException {

        List<File> filesForDeleting = new ArrayList<>();

        String tmpdir = System.getProperty("java.io.tmpdir");
        File pdfTmpFile = new File(tmpdir + "\\test.pdf");
        filesForDeleting.add(pdfTmpFile);
        multipartFile.transferTo(pdfTmpFile);

        List<BarcodeInformation> listOfBarcodes = new ArrayList<>();

        try (PDDocument document = Loader.loadPDF(new RandomAccessReadBufferedFile(pdfTmpFile)))
        {
            PDFRenderer pdfRenderer = new PDFRenderer(document);

            for (int page = 0; page < document.getNumberOfPages(); ++page) {
                BufferedImage image = pdfRenderer.renderImageWithDPI(page, 300);
                File outputfile = new File(tmpdir + "\\_page" + page + ".png");
                filesForDeleting.add(pdfTmpFile);
                ImageIO.write(image, "png", outputfile);
                String barcodeData = readBarcodeFromFile(outputfile.getAbsolutePath());

                listOfBarcodes.add(new BarcodeInformation(page + 1, barcodeData));
                log.info("Barcode on page " + (page + 1) + ": " + barcodeData);
            }

        } catch (Exception e) {
            throw new RuntimeException(e);
        }finally {
            for (File file: filesForDeleting) {
                var isDeleted = file.delete();
                if (isDeleted)
                    log.info("File " + file.getName() + " is deleted from tmp");
            }

        }

        return listOfBarcodes;
    }

    public static String readBarcodeFromFile(String filePath) throws IOException, NotFoundException {
        File barcodeFile = new File(filePath);

        if (!barcodeFile.exists()) {
            throw new IOException("File not found: " + filePath);
        }

        BufferedImage bufferedImage = ImageIO.read(barcodeFile);

        if (bufferedImage == null) {
            throw new IOException("Unable to read image file: " + filePath);
        }

        LuminanceSource source = new BufferedImageLuminanceSource(bufferedImage);
        BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));

        final HashMap<DecodeHintType, Object> hints = new HashMap<>();
        hints.put(DecodeHintType.TRY_HARDER, true);

        return new MultiFormatReader().decode(bitmap, hints).getText();

    }

}
