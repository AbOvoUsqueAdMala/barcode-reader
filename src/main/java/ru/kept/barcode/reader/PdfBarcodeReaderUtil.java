package ru.kept.barcode.reader;

import com.google.zxing.*;
import lombok.experimental.UtilityClass;
import lombok.extern.log4j.Log4j2;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.io.RandomAccessReadBufferedFile;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.rendering.PDFRenderer;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;
import java.awt.image.ConvolveOp;
import java.awt.image.Kernel;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.nio.file.Files;

@Log4j2
@UtilityClass
class PdfBarcodeReaderUtil {

    FileService fileService = new FileService();

    public static List<BarcodeInformation> readBarcodeFromPdf(MultipartFile multipartFile) throws IOException, RuntimeException {

        List<File> filesForDeleting = new ArrayList<>();

        File pdfTmpFile = File.createTempFile("recognizablePDF", ".pdf");

        filesForDeleting.add(pdfTmpFile);
        multipartFile.transferTo(pdfTmpFile);

        List<BarcodeInformation> listOfBarcodes = new ArrayList<>();

        try (PDDocument document = Loader.loadPDF(new RandomAccessReadBufferedFile(pdfTmpFile)))
        {
            PDFRenderer pdfRenderer = new PDFRenderer(document);

            for (int page = 0; page < document.getNumberOfPages(); ++page) {
                BufferedImage image = pdfRenderer.renderImageWithDPI(page, 300);
                double contrastFactor = 1.5;
                image = enhanceContrast(image, contrastFactor);
                image = reduceNoise(image);
                toGrayscale(image);

                File pdfTempFile = File.createTempFile("PDF_page_" + (page + 1) + "_", ".pdf");
                PDPage pdPage = document.getPage(page);
                try (PDDocument newDocument = new PDDocument()) {
                    newDocument.addPage(pdPage);
                    newDocument.save(pdfTempFile);
                }

                fileService.addFile(pdfTempFile);//files will be deleted after they are received and when the application is finished

                File imageTempFile = File.createTempFile("_page" + page,".png");
                filesForDeleting.add(imageTempFile); //Image  should be deleted after recognition of the barcode
                ImageIO.write(image, "png", imageTempFile);
                String barcodeData = readBarcodeFromFile(imageTempFile.getAbsolutePath());

                listOfBarcodes.add(new BarcodeInformation(page + 1, barcodeData, pdfTempFile.getName()));
                log.info("Barcode on page " + (page + 1) + ": " + barcodeData);
            }

        } catch (Exception e) {
            throw new RuntimeException(e);
        }finally {
            for (File file: filesForDeleting) {
                Files.deleteIfExists(file.toPath());
            }
        }

        return listOfBarcodes;
    }

    public static String readBarcodeFromFile(String filePath) {

        File barcodeFile = new File(filePath);
        try {

            BufferedImage bufferedImage = ImageIO.read(barcodeFile);
            LuminanceSource source = new BufferedImageLuminanceSource(bufferedImage);
            BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));
            final EnumMap<DecodeHintType, Object> hints = new EnumMap<>(DecodeHintType.class);
            hints.put(DecodeHintType.TRY_HARDER, true);
            return new MultiFormatReader().decode(bitmap, hints).getText();

        }catch (IOException | NotFoundException e) {
            return "notFound";
        }

    }

    public static BufferedImage enhanceContrast(BufferedImage originalImage, double contrastFactor) {

        int width = originalImage.getWidth();
        int height = originalImage.getHeight();

        BufferedImage resultImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int rgb = originalImage.getRGB(x, y);

                int alpha = (rgb >> 24) & 0xFF;
                int red = (rgb >> 16) & 0xFF;
                int green = (rgb >> 8) & 0xFF;
                int blue = rgb & 0xFF;

                // Увеличение контрастности
                red = (int) ((red - 128) * contrastFactor + 128);
                green = (int) ((green - 128) * contrastFactor + 128);
                blue = (int) ((blue - 128) * contrastFactor + 128);

                // Ограничение значений до диапазона [0, 255]
                red = Math.min(Math.max(red, 0), 255);
                green = Math.min(Math.max(green, 0), 255);
                blue = Math.min(Math.max(blue, 0), 255);

                int newRGB = (alpha << 24) | (red << 16) | (green << 8) | blue;
                resultImage.setRGB(x, y, newRGB);
            }
        }

        return resultImage;
    }

    public static BufferedImage reduceNoise(BufferedImage originalImage) {
        float[] matrix = {
                1.0f / 9.0f, 1.0f / 9.0f, 1.0f / 9.0f,
                1.0f / 9.0f, 1.0f / 9.0f, 1.0f / 9.0f,
                1.0f / 9.0f, 1.0f / 9.0f, 1.0f / 9.0f
        };
        BufferedImageOp op = new ConvolveOp(new Kernel(3, 3, matrix), ConvolveOp.EDGE_NO_OP, null);
        return op.filter(originalImage, null);
    }

    private static void toGrayscale(BufferedImage image) {

        int width = image.getWidth();
        int height = image.getHeight();

        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {

                int rgb = image.getRGB(i, j);

                int red = (rgb >> 16) & 0xFF;
                int green = (rgb >> 8) & 0xFF;
                int blue = (rgb) & 0xFF;

                int gray = (red + green + blue) / 3;

                image.setRGB(i, j, (gray << 16) | (gray << 8) | gray);
            }
        }

    }

}
