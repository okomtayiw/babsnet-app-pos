package com.babsnet.posapp.util;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.Base64;

public class BarcodeUtil {
    public static String generateUniqueBarcode(String base) {
        String timestamp = String.valueOf(System.currentTimeMillis());
        int random4 = new java.util.Random().nextInt(9000) + 1000;
        return base + timestamp + random4;
    }

    // Generate barcode image, return as Base64 string
    public static String generateBarcodeBase64(String text, int width, int height) throws Exception {
        BitMatrix bitMatrix = new MultiFormatWriter().encode(
                text, BarcodeFormat.CODE_128, width, height);
        BufferedImage bufferedImage = MatrixToImageWriter.toBufferedImage(bitMatrix);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(bufferedImage, "png", baos);
        byte[] imageBytes = baos.toByteArray();
        String base64 = Base64.getEncoder().encodeToString(imageBytes);

        return base64;
    }

    public static BufferedImage generateBarcodeBufferedImage(String text, int width, int height) throws Exception {
        BitMatrix bitMatrix = new MultiFormatWriter().encode(
                text, BarcodeFormat.CODE_128, width, height
        );
        return MatrixToImageWriter.toBufferedImage(bitMatrix);
    }

    public static BufferedImage generateBarcodeWithText(String text, int width, int height) throws Exception {
        int barcodeHeight = (int) (height * 0.7);  // 70% barcode, 30% text
        int textHeight = height - barcodeHeight;

        // 1. Barcode dari ZXing
        BitMatrix bitMatrix = new com.google.zxing.MultiFormatWriter().encode(
                text, com.google.zxing.BarcodeFormat.CODE_128, width, barcodeHeight
        );
        BufferedImage barcodeImage = com.google.zxing.client.j2se.MatrixToImageWriter.toBufferedImage(bitMatrix);

        // 2. Canvas gabungan
        BufferedImage combined = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = combined.createGraphics();
        g.setColor(Color.WHITE); // background
        g.fillRect(0, 0, width, height);

        // Draw barcode di atas
        g.drawImage(barcodeImage, 0, 0, null);

        // Atur font & size lebih kecil, jelas
        int fontSize = Math.max(12, textHeight - 4); // minimal 12px
        Font font = new Font("Consolas", Font.PLAIN, fontSize);

        // Supaya lebih tajam
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setFont(font);
        g.setColor(Color.BLACK);

        FontMetrics fm = g.getFontMetrics();
        int textWidth = fm.stringWidth(text);
        int textX = (width - textWidth) / 2;
        int textY = barcodeHeight + fm.getAscent() + 2;

        g.drawString(text, textX, textY);
        g.dispose();
        return combined;
    }



}
