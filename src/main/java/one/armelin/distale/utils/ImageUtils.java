package one.armelin.distale.utils;

import com.hypixel.hytale.server.core.asset.common.CommonAsset;
import com.hypixel.hytale.server.core.asset.common.CommonAssetRegistry;
import one.armelin.distale.DisTale;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;
import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class ImageUtils {
    public static BufferedImage applyGradientToSkin(BufferedImage gradient, BufferedImage skin) {
        int width = skin.getWidth();
        int height = skin.getHeight();

        BufferedImage result = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

        // Obter as cores do gradiente
        Color[] gradientColors = extractGradientColors(gradient);

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int pixelSkin = skin.getRGB(x, y);
                Color skinColor = new Color(pixelSkin, true);

                // Se o pixel for transparente, manter transparente
                if (skinColor.getAlpha() == 0) {
                    result.setRGB(x, y, pixelSkin);
                    continue;
                }

                // Obter o valor de luminosidade do pixel da skin (0-255)
                int gray = skinColor.getRed(); // Como é greyscale, R=G=B

                // Mapear o valor de cinza para uma cor do gradiente
                Color gradientColor = getColorFromGradient(gradientColors, gray);

                // Preservar a transparência original
                Color finalColor = new Color(
                        gradientColor.getRed(),
                        gradientColor.getGreen(),
                        gradientColor.getBlue(),
                        skinColor.getAlpha()
                );

                result.setRGB(x, y, finalColor.getRGB());
            }
        }

        return result;
    }

    private static Color[] extractGradientColors(BufferedImage gradient) {
        int width = gradient.getWidth();
        Color[] colors = new Color[width];

        // Extrair cores da linha do meio do gradiente
        int middleY = gradient.getHeight() / 2;

        for (int x = 0; x < width; x++) {
            colors[x] = new Color(gradient.getRGB(x, middleY));
        }

        return colors;
    }

    private static Color getColorFromGradient(Color[] gradientColors, int grayValue) {
        // Mapear o valor de cinza (0-255) para o índice do array do gradiente
        int gradientLength = gradientColors.length;

        // Normalizar o valor de cinza para o tamanho do gradiente
        float position = (grayValue / 255.0f) * (gradientLength - 1);

        int index1 = (int) Math.floor(position);
        int index2 = (int) Math.ceil(position);

        // Garantir que os índices estejam dentro dos limites
        index1 = Math.max(0, Math.min(gradientLength - 1, index1));
        index2 = Math.max(0, Math.min(gradientLength - 1, index2));

        // Se os índices são iguais, retornar a cor diretamente
        if (index1 == index2) {
            return gradientColors[index1];
        }

        // Interpolar entre as duas cores
        float fraction = position - index1;
        Color c1 = gradientColors[index1];
        Color c2 = gradientColors[index2];

        int red = (int) (c1.getRed() + (c2.getRed() - c1.getRed()) * fraction);
        int green = (int) (c1.getGreen() + (c2.getGreen() - c1.getGreen()) * fraction);
        int blue = (int) (c1.getBlue() + (c2.getBlue() - c1.getBlue()) * fraction);

        return new Color(red, green, blue);
    }

    @Nullable
    public static BufferedImage loadImageFromAsset(@Nonnull String assetPath) {
        try {
            CommonAsset commonAsset = CommonAssetRegistry. getByName(assetPath);

            if (commonAsset == null) {
                DisTale.LOGGER.atSevere().log("Asset not found in registry: %s", assetPath);
                return null;
            }

            CompletableFuture<byte[]> blobFuture = commonAsset.getBlob();
            byte[] assetData = blobFuture.get();

            if (assetData == null || assetData.length == 0) {
                DisTale.LOGGER.atSevere().log("Asset data is empty: %s", assetPath);
                return null;
            }

            ByteArrayInputStream bais = new ByteArrayInputStream(assetData);
            BufferedImage image = ImageIO.read(bais);

            if (image == null) {
                DisTale.LOGGER.atSevere().log("Asset image is empty: %s", assetPath);
                return null;
            }

            return image;

        } catch (IOException | InterruptedException | ExecutionException e) {
            DisTale.LOGGER.atSevere().withCause(e).log("Failed to load image from asset: %s", assetPath);
            return null;
        }
    }

    @Nullable
    public static String imageToBase64(@Nonnull BufferedImage image, @Nonnull String format) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(image, format, baos);
            byte[] imageBytes = baos. toByteArray();
            return Base64.getEncoder().encodeToString(imageBytes);

        } catch (IOException e) {
            DisTale.LOGGER.atSevere().withCause(e).log("Failed to convert image to Base64");
            return null;
        }
    }

    @Nullable
    public static String loadAssetAsBase64(@Nonnull String assetPath, @Nonnull String format) {
        BufferedImage image = loadImageFromAsset(assetPath);
        if (image == null) {
            return null;
        }
        return imageToBase64(image, format);
    }

    public static BufferedImage resizeByPercentage(BufferedImage originalImage, double percentage) {
        int newWidth = Math.toIntExact(Math.round(originalImage.getWidth() * (percentage / 100.0)));
        int newHeight = Math.toIntExact(Math.round(originalImage.getHeight() * (percentage / 100.0)));

        newWidth = Math.max(1, newWidth);
        newHeight = Math.max(1, newHeight);

        return resize(originalImage, newWidth, newHeight);
    }

    public static BufferedImage resize(BufferedImage originalImage, int targetWidth, int targetHeight) {
        BufferedImage resizedImage = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_ARGB);

        Graphics2D g2d = resizedImage.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        g2d.drawImage(originalImage, 0, 0, targetWidth, targetHeight, null);
        g2d.dispose();

        return resizedImage;
    }

    public static BufferedImage createTransparentBackground(int width, int height) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = image.createGraphics();
        g2d.setComposite(AlphaComposite.Clear);
        g2d.fillRect(0, 0, width, height);
        g2d.dispose();
        return image;
    }
    public static BufferedImage compose(BufferedImage background, BufferedImage overlay, int offsetX, int offsetY) {
        BufferedImage result = new BufferedImage(
                background.getWidth(),
                background.getHeight(),
                BufferedImage.TYPE_INT_ARGB
        );

        Graphics2D g2d = result.createGraphics();
        g2d.setComposite(AlphaComposite.Src);
        g2d.drawImage(background, 0, 0, null);
        g2d.setComposite(AlphaComposite.SrcOver);
        g2d.drawImage(overlay, offsetX, offsetY, null);
        g2d.dispose();
        return result;
    }

    public static BufferedImage flipHorizontally(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();
        BufferedImage flipped = new BufferedImage(width, height, image.getType());

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                flipped.setRGB(width - 1 - x, y, image.getRGB(x, y));
            }
        }

        return flipped;
    }
}
