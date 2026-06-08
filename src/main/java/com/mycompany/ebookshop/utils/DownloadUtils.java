package com.mycompany.ebookshop.utils;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Locale;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;


@SuppressWarnings("all")
public class DownloadUtils {

    private static final URI PURCHASED_BOOK_VIEW_URI = URI.create(
            "file:///C:/Users/Admin/Downloads/eBookShop/eBookShop/src/main/resources/books/Sample.pdf");

    
    public static boolean downloadBook(String bookTitle, String filePath) {
        if (filePath == null || filePath.trim().isEmpty()) {
            AlertUtils.showError("This book does not have a file available for download.");
            return false;
        }

        try {
            Path sourceFile = resolveSourcePath(bookTitle, filePath);
            if (sourceFile == null || !Files.exists(sourceFile)) {
                AlertUtils.showError("Book file not found: " + filePath);
                return false;
            }

            Path downloadsDir = getDownloadsDirectory();
            Files.createDirectories(downloadsDir);

            String safeFileName = sanitizeFileName(bookTitle);
            String extension = getFileExtension(filePath);
            Path destinationFile = downloadsDir.resolve(safeFileName + extension);

            
            int counter = 1;
            while (Files.exists(destinationFile)) {
                destinationFile = downloadsDir.resolve(safeFileName + " (" + counter + ")" + extension);
                counter++;
            }

            Files.copy(sourceFile, destinationFile, StandardCopyOption.REPLACE_EXISTING);
            AlertUtils.showSuccess("Book downloaded successfully!\n\nLocation: " + destinationFile.toString());
            return true;
        } catch (IOException ex) {
            AlertUtils.showError("Failed to download book: " + ex.getMessage());
            return false;
        }
    }

    
    public static boolean downloadBooks(List<String> bookTitles, List<String> filePaths) {
        if (bookTitles == null || filePaths == null || bookTitles.size() != filePaths.size()) {
            AlertUtils.showError("Invalid book list for download.");
            return false;
        }

        if (bookTitles.isEmpty()) {
            AlertUtils.showWarning("No books to download.");
            return false;
        }

        try {
            Path downloadsDir = getDownloadsDirectory();
            Files.createDirectories(downloadsDir);

            int successCount = 0;
            StringBuilder failedBooks = new StringBuilder();

            for (int i = 0; i < bookTitles.size(); i++) {
                String bookTitle = bookTitles.get(i);
                String filePath = filePaths.get(i);

                if (filePath == null || filePath.trim().isEmpty()) {
                    failedBooks.append("\n• ").append(bookTitle).append(" (no file)");
                    continue;
                }

                try {
                    Path sourceFile = resolveSourcePath(bookTitle, filePath);
                    if (sourceFile == null || !Files.exists(sourceFile)) {
                        failedBooks.append("\n• ").append(bookTitle).append(" (file not found)");
                        continue;
                    }

                    String safeFileName = sanitizeFileName(bookTitle);
                    String extension = getFileExtension(filePath);
                    Path destinationFile = downloadsDir.resolve(safeFileName + extension);

                    int counter = 1;
                    while (Files.exists(destinationFile)) {
                        destinationFile = downloadsDir.resolve(safeFileName + " (" + counter + ")" + extension);
                        counter++;
                    }

                    Files.copy(sourceFile, destinationFile, StandardCopyOption.REPLACE_EXISTING);
                    successCount++;
                } catch (IOException ex) {
                    failedBooks.append("\n• ").append(bookTitle).append(" (").append(ex.getMessage()).append(")");
                }
            }

            StringBuilder message = new StringBuilder();
            message.append("Downloaded ").append(successCount).append(" of ").append(bookTitles.size()).append(" books");
            if (failedBooks.length() > 0) {
                message.append("\n\nFailed downloads:").append(failedBooks);
            }
            message.append("\n\nLocation: ").append(getDownloadsDirectory().toString());

            if (successCount > 0) {
                AlertUtils.showSuccess(message.toString());
                return true;
            } else {
                AlertUtils.showError(message.toString());
                return false;
            }
        } catch (IOException ex) {
            AlertUtils.showError("Failed to download books: " + ex.getMessage());
            return false;
        }
    }

    
    public static boolean openDownloadsFolder() {
        try {
            Path downloadsDir = getDownloadsDirectory();
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().open(downloadsDir.toFile());
                return true;
            }
        } catch (IOException ex) {
            AlertUtils.showError("Could not open Downloads folder: " + ex.getMessage());
        }
        return false;
    }

    
    public static Path getDownloadsDirectory() {
        String userHome = System.getProperty("user.home", ".");
        return Path.of(userHome).resolve("Downloads").resolve("eBookShop");
    }

    
    private static String sanitizeFileName(String fileName) {
        
        return fileName.toLowerCase().replaceAll("[<>:\"/\\\\|?*]", "").replaceAll("\\s+", "-").trim();
    }
    private static String getFileExtension(String filePath) {
        int lastDot = filePath.lastIndexOf('.');
        if (lastDot > 0) return filePath.substring(lastDot);
        return ".pdf";
    }

    private static Path resolveSourcePath(String bookTitle, String storedPath) {
        try {
            Path classpathStored = resolveClasspathResource(storedPath);
            if (classpathStored != null) {
                return classpathStored;
            }

            if (storedPath != null && !storedPath.isEmpty()) {
                Path p = Path.of(storedPath);
                if (Files.exists(p)) return p;
                
                String dir = p.getParent() != null ? p.getParent().toString() : "";
                String baseName = p.getFileName() != null ? p.getFileName().toString() : "";
                if (baseName.contains(".")) {
                    baseName = baseName.substring(0, baseName.lastIndexOf('.'));
                }
                
                String[] exts = new String[]{".pdf", ".epub", ".mobi", ".txt"};
                for (String ext : exts) {
                    Path alt = Path.of(dir, baseName + ext);
                    if (Files.exists(alt)) return alt;
                }
            }

            Path sharedSample = findExistingPath(
                Path.of("C:/Users/Admin/Desktop/eBookShop/eBookShop/src/main/resources/books/Sample.pdf"),
                Path.of("C:/Users/Admin/Desktop/eBookShop/eBookShop/src/main/resources/books/sample.pdf"),
                Path.of("target/classes/books/Sample.pdf"),
                Path.of("ebooks/sample.pdf"),
                Path.of("ebooks/Sample.pdf"),
                Path.of("target/classes/books/sample.pdf")
            );
            if (sharedSample != null) {
                return sharedSample;
            }

            String safe = sanitizeFileName(bookTitle);
            String[] exts = new String[]{".pdf", ".epub", ".mobi", ".txt"};
            
            Path resourceBooksPath = Path.of("target/classes/books");
            for (String ext : exts) {
                Path cand = findIgnoreCase(resourceBooksPath, safe + ext);
                if (cand != null) return cand;
            }

            Path srcResourceBooksPath = Path.of("C:/Users/Admin/Desktop/eBookShop/eBookShop/src/main/resources/books");
            for (String ext : exts) {
                Path cand = findIgnoreCase(srcResourceBooksPath, safe + ext);
                if (cand != null) return cand;
            }

            Path prod = Path.of("C:/data/ebookshop/ebooks");
            for (String ext : exts) {
                Path cand = prod.resolve(safe + ext);
                if (Files.exists(cand)) return cand;
            }

            Path local = Path.of("C:/Users/Admin/Desktop/eBookShop/eBookShop/ebooks");
            for (String ext : exts) {
                Path cand = findIgnoreCase(local, safe + ext);
                if (cand != null) return cand;
            }
        } catch (Exception ex) {
        }
        return null;
    }

    private static Path resolveClasspathResource(String storedPath) {
        if (storedPath == null || storedPath.isBlank()) {
            return null;
        }

        String resourcePath = storedPath.trim();
        if (resourcePath.startsWith("classpath:")) {
            resourcePath = resourcePath.substring("classpath:".length());
        }
        if (!resourcePath.startsWith("/")) {
            resourcePath = "/" + resourcePath;
        }

        try (var inputStream = DownloadUtils.class.getResourceAsStream(resourcePath)) {
            if (inputStream == null) {
                return null;
            }

            String fileName = Path.of(resourcePath).getFileName().toString();
            String suffix = fileName.contains(".") ? fileName.substring(fileName.lastIndexOf('.')) : ".pdf";
            Path tempFile = Files.createTempFile("ebookshop-", suffix);
            Files.copy(inputStream, tempFile, StandardCopyOption.REPLACE_EXISTING);
            tempFile.toFile().deleteOnExit();
            return tempFile;
        } catch (IOException ex) {
            return null;
        }
    }

    private static Path findExistingPath(Path... candidates) {
        for (Path candidate : candidates) {
            if (candidate != null && Files.exists(candidate)) {
                return candidate;
            }
        }
        return null;
    }

    private static Path findIgnoreCase(Path dir, String fileName) {
        try {
            if (dir == null || !Files.exists(dir) || !Files.isDirectory(dir)) return null;
            String want = fileName.toLowerCase(Locale.ROOT);
            try (DirectoryStream<Path> ds = Files.newDirectoryStream(dir)) {
                for (Path p : ds) {
                    if (p.getFileName() == null) continue;
                    if (p.getFileName().toString().toLowerCase(Locale.ROOT).equals(want)) {
                        return p;
                    }
                }
            }
        } catch (Exception e) {
        }
        return null;
    }

    public static boolean viewBook(String bookTitle, String filePath) {
        try {
            if (Desktop.isDesktopSupported()) {
                try {
                    Desktop.getDesktop().browse(PURCHASED_BOOK_VIEW_URI);
                    return true;
                } catch (Exception e) {
                }
            }

            Path sourceFile = resolveSourcePath(bookTitle, filePath);
            if (sourceFile != null && Files.exists(sourceFile)) {
                if (Desktop.isDesktopSupported()) {
                    try {
                        if (isPdfFile(sourceFile, filePath)) {
                            Desktop.getDesktop().browse(sourceFile.toUri());
                            return true;
                        }

                        Desktop.getDesktop().open(sourceFile.toFile());
                        return true;
                    } catch (Exception e) {
                    }
                }
            }

            String safe = sanitizeFileName(bookTitle);
            String[] exts = new String[]{".pdf", ".epub", ".mobi", ".txt"};
            for (String ext : exts) {
                String resourcePath = "/books/" + safe + ext;
                try (var is = DownloadUtils.class.getResourceAsStream(resourcePath)) {
                    if (is != null) {
                        Path tmp = Files.createTempFile(safe + "-", ext);
                        Files.copy(is, tmp, StandardCopyOption.REPLACE_EXISTING);
                        tmp.toFile().deleteOnExit();
                        if (Desktop.isDesktopSupported()) {
                            try {
                                String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
                                if (os.contains("win")) {
                                    try {
                                        Runtime.getRuntime().exec(new String[] {"cmd", "/c", "start", "\"\"", tmp.toAbsolutePath().toString()});
                                        return true;
                                    } catch (IOException e) {
                                        Desktop.getDesktop().browse(tmp.toUri());
                                        return true;
                                    }
                                } else {
                                    Desktop.getDesktop().browse(tmp.toUri());
                                    return true;
                                }
                            } catch (Exception e) {
                            }
                        }
                    }
                } catch (IOException e) {
                }
            }
        } catch (Exception ex) {
        }
        AlertUtils.showError("Unable to open book for viewing.");
        return false;
    }

    private static boolean isPdfFile(Path sourceFile, String storedPath) {
        String name = sourceFile.getFileName() != null ? sourceFile.getFileName().toString() : "";
        String path = storedPath == null ? "" : storedPath;
        return name.toLowerCase(Locale.ROOT).endsWith(".pdf") || path.toLowerCase(Locale.ROOT).contains(".pdf");
    }

    public static boolean viewBooks(List<String> bookTitles, List<String> filePaths) {
        if (bookTitles == null || filePaths == null || bookTitles.size() != filePaths.size()) {
            AlertUtils.showError("Invalid book list for viewing.");
            return false;
        }

        if (bookTitles.isEmpty()) {
            AlertUtils.showWarning("No books could be opened for viewing.");
            return false;
        }

        if (bookTitles.size() == 1) {
            return viewBook(bookTitles.get(0), filePaths.get(0));
        }

        VBox content = new VBox(10);
        content.setPadding(new Insets(12));

        int validBooks = 0;
        for (int i = 0; i < bookTitles.size(); i++) {
            String bookTitle = bookTitles.get(i) != null ? bookTitles.get(i) : "Untitled Book";
            String filePath = filePaths.get(i);
            if (filePath == null || filePath.trim().isEmpty()) {
                continue;
            }

            validBooks++;

            Label titleLabel = new Label(bookTitle);
            titleLabel.setWrapText(true);
            titleLabel.setMaxWidth(360);

            Button viewButton = new Button("View");
            viewButton.setOnAction(evt -> viewBook(bookTitle, filePath));

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            HBox row = new HBox(12, titleLabel, spacer, viewButton);
            row.setAlignment(Pos.CENTER_LEFT);
            row.setStyle("-fx-padding: 8 0; -fx-border-color: transparent transparent #eef3f8 transparent; -fx-border-width: 0 0 1 0;");
            content.getChildren().add(row);
        }

        if (validBooks == 0) {
            AlertUtils.showWarning("No books could be opened for viewing.");
            return false;
        }

        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setFitToWidth(true);
        scrollPane.setPrefViewportWidth(520);
        scrollPane.setPrefViewportHeight(320);

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Purchased Books");
        alert.setHeaderText("Choose a book to view");
        alert.getDialogPane().setContent(scrollPane);
        alert.getDialogPane().getButtonTypes().setAll(ButtonType.CLOSE);
        alert.showAndWait();
        return true;
    }
}