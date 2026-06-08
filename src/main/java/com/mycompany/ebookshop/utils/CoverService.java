package com.mycompany.ebookshop.utils;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.imageio.ImageIO;

import com.mycompany.ebookshop.models.Book;

public final class CoverService {

    private static final String DEFAULT_PLACEHOLDER = "https://placehold.co/300x420/png?text=eBookShop";
    private static final Path GENERATED_COVERS_DIR = Path.of(System.getProperty("user.home", "."), "eBookShop", "generated-covers");

    private CoverService() {}

    public static String getCoverUrl(Book book) {
        if (book == null) return generateCoverUri("eBookShop", "");

        String imageUrl = book.getImageUrl();
        if (imageUrl != null && !imageUrl.isBlank()) {
            String resolved = resolveLocalOrRemoteImage(imageUrl);
            if (resolved != null) {
                return resolved;
            }
        }

        String isbn = book.getIsbn();
        String title = book.getTitle();

        String openLibrary = buildOpenLibraryCoverUrl(isbn);
        if (openLibrary != null) {
            return openLibrary;
        }

        return generateCoverUri(title, isbn);
    }

    private static String resolveLocalOrRemoteImage(String imageUrl) {
        String trimmed = imageUrl.trim();

        if (trimmed.startsWith("file:")) {
            return trimmed;
        }

        if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            
            return trimmed;
        }

        try {
            Path localPath = Path.of(trimmed);
            if (Files.exists(localPath)) {
                return localPath.toUri().toString();
            }
        } catch (java.nio.file.InvalidPathException e) {
            
        }

        return null;
    }

    public static String getGeneratedCoverUrl(String title, String isbn) {
        return generateCoverUri(title, isbn);
    }

    private static String buildOpenLibraryCoverUrl(String isbn) {
        String normalizedIsbn = safeText(isbn).replaceAll("[^0-9Xx]", "");
        if (normalizedIsbn.isBlank()) {
            return null;
        }
        return "https://covers.openlibrary.org/b/isbn/" + normalizedIsbn + "-L.jpg";
    }

    private static String generateCoverUri(String title, String isbn) {
        try {
            Files.createDirectories(GENERATED_COVERS_DIR);

            String key = buildCoverKey(title, isbn);
            Path target = GENERATED_COVERS_DIR.resolve(key + ".png");
            if (!Files.exists(target)) {
                createGeneratedCoverImage(target, title, isbn);
            }
            return target.toUri().toString();
        } catch (IOException e) {
            return DEFAULT_PLACEHOLDER;
        }
    }

    private static void createGeneratedCoverImage(Path target, String title, String isbn) throws IOException {
        int width = 300;
        int height = 420;
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int hash = Math.abs((safeText(title) + "|" + safeText(isbn)).hashCode());
            Color top = new Color(24 + (hash % 90), 70 + (hash / 10 % 100), 120 + (hash / 100 % 80));
            Color bottom = new Color(14 + (hash / 7 % 80), 40 + (hash / 11 % 90), 74 + (hash / 13 % 110));

            g.setPaint(new java.awt.GradientPaint(0, 0, top, 0, height, bottom));
            g.fill(new RoundRectangle2D.Double(0, 0, width, height, 40, 40));

            g.setColor(new Color(255, 255, 255, 35));
            g.fillOval(-40, -20, 180, 180);
            g.fillOval(190, 260, 130, 130);

            g.setColor(new Color(255, 255, 255, 220));
            g.setStroke(new BasicStroke(6f));
            g.draw(new RoundRectangle2D.Double(18, 18, width - 36, height - 36, 34, 34));

            g.setFont(new Font("Segoe UI", Font.BOLD, 28));
            g.setColor(Color.WHITE);
            String initials = buildInitials(title, isbn);
            FontMetrics fm = g.getFontMetrics();
            int initialsWidth = fm.stringWidth(initials);
            g.drawString(initials, (width - initialsWidth) / 2, 140);

            g.setFont(new Font("Segoe UI", Font.BOLD, 18));
            g.setColor(new Color(255, 255, 255, 245));
            drawCenteredWrappedText(g, safeText(title), 40, 210, width - 80, 3, 24);

            g.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            g.setColor(new Color(255, 255, 255, 210));
            drawCenteredWrappedText(g, safeText(isbn).isBlank() ? "eBookShop" : safeText(isbn), 40, 370, width - 80, 1, 16);
        } finally {
            g.dispose();
        }

        ImageIO.write(image, "png", target.toFile());
    }

    private static String buildCoverKey(String title, String isbn) {
        String keySource = safeText(isbn);
        if (keySource.isBlank()) {
            keySource = safeText(title);
        }
        if (keySource.isBlank()) {
            keySource = "ebookshop";
        }
        return keySource.toLowerCase().replaceAll("[^a-z0-9]+", "-").replaceAll("^-+|-+$", "");
    }

    private static String buildInitials(String title, String isbn) {
        String source = safeText(title);
        if (source.isBlank()) {
            source = safeText(isbn);
        }
        String[] words = source.trim().split("\\s+");
        StringBuilder initials = new StringBuilder();
        for (String word : words) {
            if (!word.isBlank() && Character.isLetterOrDigit(word.charAt(0))) {
                initials.append(Character.toUpperCase(word.charAt(0)));
            }
            if (initials.length() >= 3) break;
        }
        return initials.isEmpty() ? "BK" : initials.toString();
    }

    private static void drawCenteredWrappedText(Graphics2D g, String text, int x, int y, int maxWidth, int maxLines, int lineHeight) {
        if (text == null || text.isBlank()) {
            return;
        }

        FontMetrics fm = g.getFontMetrics();
        String[] words = text.trim().split("\\s+");
        StringBuilder line = new StringBuilder();
        int linesDrawn = 0;
        int currentY = y;

        for (String word : words) {
            String candidate = line.isEmpty() ? word : line + " " + word;
            if (fm.stringWidth(candidate) > maxWidth && !line.isEmpty()) {
                int lineWidth = fm.stringWidth(line.toString());
                g.drawString(line.toString(), x + (maxWidth - lineWidth) / 2, currentY);
                currentY += lineHeight;
                linesDrawn++;
                if (linesDrawn >= maxLines) {
                    return;
                }
                line = new StringBuilder(word);
            } else {
                line = new StringBuilder(candidate);
            }
        }

        if (!line.isEmpty() && linesDrawn < maxLines) {
            int lineWidth = fm.stringWidth(line.toString());
            g.drawString(line.toString(), x + (maxWidth - lineWidth) / 2, currentY);
        }
    }

    private static String safeText(String value) {
        return value == null ? "" : value.trim();
    }

}
