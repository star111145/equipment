package com.swpu.equipment.common.util;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.InitializingBean;
/**
 * 二维码工具类
 */
@Component
public class QrCodeUtil implements InitializingBean {

    private static final int QR_CODE_SIZE = 300;
    private static final String IMAGE_FORMAT = "png";
    private static String qrCodeDir;
    
    @Value("${upload.base-dir:src/main/resources/static/uploads}")
    private String uploadBaseDir;
    
    @Override
    public void afterPropertiesSet() throws Exception {
        String baseDir = uploadBaseDir;
        if (!new File(baseDir).isAbsolute()) {
            baseDir = System.getProperty("user.dir") + "/" + baseDir;
        }
        qrCodeDir = baseDir + "/qrcode/";
        File dir = new File(qrCodeDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        System.out.println("二维码存储目录: " + qrCodeDir);
    }

    public static String generateQrCode(String content, String fileName) throws Exception {
        Path filePath = Paths.get(qrCodeDir + fileName + "." + IMAGE_FORMAT);
        
        if (Files.exists(filePath)) {
            Files.delete(filePath);
        }

        Map<EncodeHintType, Object> hints = new HashMap<>();
        hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
        hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M);
        hints.put(EncodeHintType.MARGIN, 1);

        BitMatrix bitMatrix = new MultiFormatWriter().encode(content, BarcodeFormat.QR_CODE, QR_CODE_SIZE, QR_CODE_SIZE, hints);

        BufferedImage bufferedImage = new BufferedImage(QR_CODE_SIZE, QR_CODE_SIZE, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics2D = bufferedImage.createGraphics();

        for (int x = 0; x < QR_CODE_SIZE; x++) {
            for (int y = 0; y < QR_CODE_SIZE; y++) {
                bufferedImage.setRGB(x, y, bitMatrix.get(x, y) ? Color.BLACK.getRGB() : Color.WHITE.getRGB());
            }
        }

        graphics2D.dispose();

        ImageIO.write(bufferedImage, IMAGE_FORMAT, new File(qrCodeDir + fileName + "." + IMAGE_FORMAT));

        return "/uploads/qrcode/" + fileName + "." + IMAGE_FORMAT;
    }

    public static String getQrCodeDir() {
        return qrCodeDir;
    }

    public static boolean deleteQrCode(String fileName) {
        File file = new File(qrCodeDir + fileName + "." + IMAGE_FORMAT);
        return file.exists() && file.delete();
    }
}
