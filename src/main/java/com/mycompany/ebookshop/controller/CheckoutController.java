package com.mycompany.ebookshop.controller;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.io.IOException;
import java.util.stream.Collectors;

import com.mycompany.ebookshop.MainApp;
import com.mycompany.ebookshop.database.OrderDAO;
import com.mycompany.ebookshop.models.CartItem;
import com.mycompany.ebookshop.models.Order;
import com.mycompany.ebookshop.models.OrderItem;
import com.mycompany.ebookshop.utils.AlertUtils;
import com.mycompany.ebookshop.utils.DownloadUtils;
import com.mycompany.ebookshop.utils.SessionManager;

import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.control.TextArea;
import javafx.scene.layout.VBox;

@SuppressWarnings("all")
public class CheckoutController {

    private static final DateTimeFormatter RECEIPT_TIMESTAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final String DEFAULT_ADDRESS = "Gondar";
    private static final String DEFAULT_PHONE = "0918527553";
    private static final String DEFAULT_CARD_NUMBER = "6756453456677889";
    private static final String DEFAULT_EXPIRY = "12/32";
    private static final String DEFAULT_CVV = "388";

    private List<CartItem> checkoutItems;
    
    @FXML private TextField fullNameField;
    @FXML private TextField addressField;
    @FXML private TextField phoneField;
    @FXML private TextField emailField;
    @FXML private TextField cardNumberField;
    @FXML private TextField expiryField;
    @FXML private TextField cvvField;
    @FXML private Label subtotalLabel;
    @FXML private Label taxLabel;
    @FXML private Label totalLabel;
    @FXML private ComboBox<String> paymentMethod;
    
    @FXML
    public void initialize() {
        paymentMethod.getItems().addAll("Credit Card", "Debit Card", "PayPal", "Cash on Delivery");
        paymentMethod.setValue("Cash on Delivery");
        checkoutItems = SessionManager.getInstance().getCheckoutItems();
        if (checkoutItems == null || checkoutItems.isEmpty()) {
            checkoutItems = SessionManager.getInstance().getCart();
        }
        updateTotals();
        loadUserInfo();
    }
    
    private void updateTotals() {
        double subtotal = checkoutItems.stream().mapToDouble(CartItem::getTotal).sum();
        double tax = subtotal * 0.08;
        double total = subtotal + tax;
        
        subtotalLabel.setText(String.format("$%.2f", subtotal));
        taxLabel.setText(String.format("$%.2f", tax));
        totalLabel.setText(String.format("$%.2f", total));
    }
    
    private void loadUserInfo() {
        if (SessionManager.getInstance().getCurrentUser() != null) {
            var user = SessionManager.getInstance().getCurrentUser();
            fullNameField.setText(user.getFullName());
            emailField.setText(user.getEmail());
            addressField.setText(user.getAddress());
            phoneField.setText(user.getPhone());
        }

        
        setIfBlank(addressField, DEFAULT_ADDRESS);
        setIfBlank(phoneField, DEFAULT_PHONE);
        setIfBlank(cardNumberField, DEFAULT_CARD_NUMBER);
        setIfBlank(expiryField, DEFAULT_EXPIRY);
        setIfBlank(cvvField, DEFAULT_CVV);
    }

    private void setIfBlank(TextField field, String value) {
        if (field == null) {
            return;
        }
        String current = field.getText();
        if (current == null || current.trim().isBlank()) {
            field.setText(value);
        }
    }
    
    @FXML
    public void handleBack() {
        MainApp.changeScene("cart.fxml", "eBookShop - Shopping Cart");
    }

    @FXML
    public void handlePlaceOrder() {
        if (!validateFields()) {
            return;
        }

        try {
            List<CartItem> receiptItems = new ArrayList<>(checkoutItems);
            if (receiptItems.isEmpty()) {
                AlertUtils.showWarning("Your checkout is empty.");
                return;
            }

            OrderDAO orderDAO = new OrderDAO();
            int userId = SessionManager.getInstance().getCurrentUser().getId();
            List<Integer> requestedBookIds = receiptItems.stream()
                    .map(item -> item.getBook().getId())
                    .toList();
            Set<Integer> previouslyPurchasedIds = orderDAO.findPreviouslyPurchasedBookIds(userId, requestedBookIds);
            if (!previouslyPurchasedIds.isEmpty()) {
                List<String> blockedTitles = receiptItems.stream()
                        .filter(item -> previouslyPurchasedIds.contains(item.getBook().getId()))
                        .map(item -> item.getBook().getTitle())
                        .distinct()
                        .toList();

                
                receiptItems = receiptItems.stream()
                        .filter(item -> !previouslyPurchasedIds.contains(item.getBook().getId()))
                        .toList();

                AlertUtils.showWarning(
                        "Already purchased and skipped: " + String.join(", ", blockedTitles)
                                + ". You can only purchase each book once.");

                if (receiptItems.isEmpty()) {
                    return;
                }
            }

            double subtotal = receiptItems.stream().mapToDouble(CartItem::getTotal).sum();
            Order order = new Order();
            order.setUserId(userId);
            order.setOrderDate(LocalDateTime.now());
            order.setTotalAmount(subtotal * 1.08);
            order.setStatus("Paid");
            order.setShippingAddress(safeText(addressField.getText()));
            order.setPaymentMethod(safeText(paymentMethod.getValue()));

            int orderId = orderDAO.createOrder(order);
            if (orderId <= 0) {
                AlertUtils.showError("Could not place the order. Please try again.");
                return;
            }

            for (var item : receiptItems) {
                OrderItem orderItem = new OrderItem(0, orderId,
                        item.getBook().getId(),
                        item.getBook().getTitle(),
                        item.getQuantity(),
                        item.getBook().getPrice());
                if (!orderDAO.addOrderItem(orderItem)) {
                    AlertUtils.showWarning("One order item could not be saved, but the order was created.");
                }
            }

            try {
                saveReceiptFileAndPersistPath(orderDAO, orderId, order.getOrderDate(), subtotal, order.getTotalAmount() - subtotal, order.getTotalAmount(), receiptItems);
            } catch (IOException ex) {
                AlertUtils.showError("Could not save the receipt file.");
            }

            for (CartItem item : receiptItems) {
                SessionManager.getInstance().removeFromCart(item);
            }

            SessionManager.getInstance().clearCheckoutItems();

            ReceiptAction receiptAction = showReceiptPreviewAndAskDownload(
                    orderId,
                    order.getOrderDate(),
                    subtotal,
                    order.getTotalAmount() - subtotal,
                    order.getTotalAmount(),
                    receiptItems);

            if (receiptAction == ReceiptAction.MY_ORDERS) {
                MainApp.changeScene("orders.fxml", "eBookShop - My Orders");
            } else if (receiptAction == ReceiptAction.DOWNLOAD_BOOKS) {
                MainApp.changeScene("main.fxml", "eBookShop - Book Store");
            } else {
                MainApp.changeScene("main.fxml", "eBookShop - Book Store");
            }
        } catch (Exception ex) {
            AlertUtils.showError("Checkout failed: " + ex.getMessage());
        }
    }

    private ReceiptAction showReceiptPreviewAndAskDownload(int orderId, LocalDateTime orderDate, double subtotal, double tax, double total, List<CartItem> items) {
        String receiptPreview = buildReceiptText(orderId, orderDate, subtotal, tax, total, items);

        TextArea receiptArea = new TextArea(receiptPreview);
        receiptArea.setEditable(false);
        receiptArea.setWrapText(true);
        receiptArea.setPrefRowCount(26);
        receiptArea.setPrefColumnCount(72);
        receiptArea.setStyle("-fx-font-family: 'Consolas'; -fx-font-size: 12px; -fx-control-inner-background: #f8fbff;");

        ScrollPane receiptScroll = new ScrollPane(receiptArea);
        receiptScroll.setFitToWidth(true);
        receiptScroll.setPrefViewportWidth(720);
        receiptScroll.setPrefViewportHeight(360);

        VBox content = new VBox(12);
        content.setPadding(new Insets(8));
        content.getChildren().addAll(
                new Label("Receipt Preview"),
            receiptScroll
        );

        ButtonType viewReceiptButton = new ButtonType("View Receipt", ButtonBar.ButtonData.OK_DONE);
        ButtonType downloadBooksButton = new ButtonType("View Books", ButtonBar.ButtonData.YES);
        ButtonType myOrdersButton = new ButtonType("My Orders", ButtonBar.ButtonData.NEXT_FORWARD);
        ButtonType downloadReceiptButton = new ButtonType("Download Receipt", ButtonBar.ButtonData.NO);
        ButtonType closeButton = new ButtonType("Close", ButtonBar.ButtonData.CANCEL_CLOSE);

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Order Complete");
        alert.setHeaderText("✓ Your order is complete. Choose what to do next.");
        alert.setResizable(true);
        alert.getDialogPane().setPrefSize(760, 540);
        alert.getDialogPane().setContent(content);
        alert.getButtonTypes().setAll(downloadBooksButton, viewReceiptButton, myOrdersButton, downloadReceiptButton, closeButton);

        ButtonType selected = alert.showAndWait().orElse(closeButton);
        if (selected == downloadBooksButton) {
            viewPurchasedBooks(items);
            try {
                
                saveReceiptFileAndPersistPath(new OrderDAO(), orderId, orderDate, subtotal, tax, total, items);
                openReceiptFile(orderId, orderDate, subtotal, tax, total, items);
            } catch (IOException ex) {
                AlertUtils.showError("Could not open the receipt file after viewing.");
            }
            return ReceiptAction.VIEW_RECEIPT;
        }
        if (selected == viewReceiptButton) {
            try {
                openReceiptFile(orderId, orderDate, subtotal, tax, total, items);
            } catch (IOException ex) {
                AlertUtils.showError("Could not open the receipt file.");
            }
            return ReceiptAction.VIEW_RECEIPT;
        }
        if (selected == myOrdersButton) {
            return ReceiptAction.MY_ORDERS;
        }
        if (selected == downloadReceiptButton) {
            try {
                downloadReceiptAutomatically(orderId, orderDate, subtotal, tax, total, items);
            } catch (IOException ex) {
                AlertUtils.showError("Could not download the receipt file.");
            }
            return ReceiptAction.DOWNLOAD_RECEIPT;
        }
        return ReceiptAction.CLOSE;
    }

    private void saveReceiptFileAndPersistPath(OrderDAO orderDAO, int orderId, LocalDateTime orderDate, double subtotal, double tax, double total, List<CartItem> items) throws IOException {
        Path receiptsDir = getAppReceiptsDirectory();
        Files.createDirectories(receiptsDir);

        Path targetFile = receiptsDir.resolve("eBookShop-Receipt-" + orderId + ".html");
        String receiptHtml = buildReceiptHtml(orderId, orderDate, subtotal, tax, total, items);
        Files.writeString(targetFile, receiptHtml, StandardCharsets.UTF_8);
        orderDAO.updateReceiptPath(orderId, targetFile.toString());
    }

    private void downloadReceiptAutomatically(int orderId, LocalDateTime orderDate, double subtotal, double tax, double total, List<CartItem> items) throws IOException {
        saveReceiptFileAndPersistPath(new OrderDAO(), orderId, orderDate, subtotal, tax, total, items);
    }

    private void openReceiptFile(int orderId, LocalDateTime orderDate, double subtotal, double tax, double total, List<CartItem> items) throws IOException {
        saveReceiptFileAndPersistPath(new OrderDAO(), orderId, orderDate, subtotal, tax, total, items);
        Path targetFile = getAppReceiptsDirectory().resolve("eBookShop-Receipt-" + orderId + ".html");
        if (Files.exists(targetFile)) {
            java.awt.Desktop.getDesktop().browse(targetFile.toUri());
        }
    }

    private void downloadPurchasedBooks(List<CartItem> items) {
        List<String> bookTitles = new ArrayList<>();
        List<String> filePaths = new ArrayList<>();
        
        for (CartItem item : items) {
            String filePath = item.getBook().getFilePath();
            if (filePath != null && !filePath.trim().isEmpty()) {
                bookTitles.add(item.getBook().getTitle());
                filePaths.add(filePath);
            }
        }
        
        if (bookTitles.isEmpty()) {
            AlertUtils.showWarning("No downloadable books in this order.");
        } else {
            DownloadUtils.downloadBooks(bookTitles, filePaths);
        }
    }

    private void viewPurchasedBooks(List<CartItem> items) {
        List<String> bookTitles = new ArrayList<>();
        List<String> filePaths = new ArrayList<>();

        for (CartItem item : items) {
            String filePath = item.getBook().getFilePath();
            if (filePath != null && !filePath.trim().isEmpty()) {
                bookTitles.add(item.getBook().getTitle());
                filePaths.add(filePath);
            }
        }

        if (bookTitles.isEmpty()) {
            AlertUtils.showWarning("No viewable books in this order.");
        } else {
            DownloadUtils.viewBooks(bookTitles, filePaths);
        }
    }

    private enum ReceiptAction {
        VIEW_RECEIPT,
        MY_ORDERS,
        DOWNLOAD_BOOKS,
        DOWNLOAD_RECEIPT,
        CLOSE
    }

    private Path getAppReceiptsDirectory() {
        Path userHome = Path.of(System.getProperty("user.home", "."));
        Path receipts = userHome.resolve("eBookShopReceipts");
        return receipts;
    }

    private String buildReceiptText(int orderId, LocalDateTime orderDate, double subtotal, double tax, double total, List<CartItem> items) {
        String customerName = safeText(fullNameField.getText());
        String customerEmail = safeText(emailField.getText());
        String customerPhone = safeText(phoneField.getText());
        String customerAddress = safeText(addressField.getText());
        String payment = safeText(paymentMethod.getValue());

        StringBuilder builder = new StringBuilder();
        builder.append("eBookShop Receipt\n");
        builder.append("========================================\n");
        builder.append("Receipt No: #").append(orderId).append('\n');
        builder.append("Date: ").append(orderDate.format(RECEIPT_TIMESTAMP)).append('\n');
        builder.append("Customer: ").append(customerName).append('\n');
        builder.append("Email: ").append(customerEmail).append('\n');
        builder.append("Phone: ").append(customerPhone).append('\n');
        builder.append("Address: ").append(customerAddress).append('\n');
        builder.append("Payment Method: ").append(payment).append('\n');
        builder.append("----------------------------------------\n");
        for (CartItem item : items) {
            builder.append(item.getBook().getTitle())
                    .append(" | ")
                    .append(item.getBook().getAuthor())
                    .append(" x")
                    .append(item.getQuantity())
                    .append("  Unit: $")
                    .append(String.format("%.2f", item.getBook().getPrice()))
                    .append("  Line: $")
                    .append(String.format("%.2f", item.getTotal()))
                    .append('\n');
        }
        builder.append("----------------------------------------\n");
        builder.append("Subtotal: $").append(String.format("%.2f", subtotal)).append('\n');
        builder.append("Tax (8%): $").append(String.format("%.2f", tax)).append('\n');
        builder.append("Total: $").append(String.format("%.2f", total)).append('\n');
        return builder.toString();
    }

    private String buildReceiptHtml(int orderId, LocalDateTime orderDate, double subtotal, double tax, double total, List<CartItem> items) {
        String customerName = safeText(fullNameField.getText());
        String customerEmail = safeText(emailField.getText());
        String payment = safeText(paymentMethod.getValue());

        String rows = items.stream()
                .map(item -> "<tr>"
                        + "<td>" + escapeHtml(item.getBook().getTitle()) + "</td>"
                        + "<td>" + escapeHtml(item.getBook().getAuthor()) + "</td>"
                        + "<td class='num'>" + item.getQuantity() + "</td>"
                        + "<td class='num'>$" + String.format("%.2f", item.getBook().getPrice()) + "</td>"
                        + "<td class='num'>$" + String.format("%.2f", item.getTotal()) + "</td>"
                        + "</tr>")
                .collect(Collectors.joining());

        return String.format("""
            <!DOCTYPE html>
            <html lang='en'>
            <head>
            <meta charset='UTF-8'>
            <meta name='viewport' content='width=device-width, initial-scale=1.0'>
            <title>eBookShop Receipt</title>
            <style>
            body{font-family:Segoe UI,Arial,sans-serif;background:#f4f8fc;color:#183b56;margin:0;padding:32px;}
            .receipt{max-width:860px;margin:0 auto;background:#fff;border:1px solid #dfe9f5;border-radius:18px;box-shadow:0 12px 30px rgba(24,59,86,.08);overflow:hidden;}
            .head{background:linear-gradient(to right,#183b56,#2d8cff);color:#fff;padding:26px 32px;}
            .head h1{margin:0;font-size:30px;} .head p{margin:6px 0 0;opacity:.9;}
            .section{padding:26px 32px;border-top:1px solid #eef3f8;}
            .meta{display:grid;grid-template-columns:1fr 1fr;gap:10px 24px;font-size:14px;}
            .meta div{padding:8px 0;border-bottom:1px dashed #e7edf5;}
            table{width:100%%;border-collapse:collapse;margin-top:8px;} th,td{padding:12px 10px;border-bottom:1px solid #edf2f7;text-align:left;} th{background:#f7fbff;font-size:13px;color:#6b7c93;} .num{text-align:right;}
            .totals{display:flex;justify-content:flex-end;margin-top:18px;} .totals table{width:320px;} .totals td{border:none;padding:8px 0;} .grand td{font-size:20px;font-weight:700;color:#2d8cff;border-top:2px solid #d9e6f5;padding-top:14px;}
            .muted{color:#6b7c93;}
            </style>
            </head>
            <body>
            <div class='receipt'>
            <div class='head'><h1>eBookShop Receipt</h1><p>Your order has been processed successfully.</p></div>
            <div class='section'>
            <div class='meta'>
            <div><strong>Receipt No:</strong> #%d</div>
            <div><strong>Date:</strong> %s</div>
            <div><strong>Customer:</strong> %s</div>
            <div><strong>Email:</strong> %s</div>
            <div><strong>Payment Method:</strong> %s</div>
            <div><strong>Status:</strong> Paid / Pending Confirmation</div>
            </div>
            </div>
            <div class='section'>
            <h2 style='margin:0 0 10px;font-size:18px;'>Items</h2>
            <table>
            <thead><tr><th>Title</th><th>Author</th><th class='num'>Qty</th><th class='num'>Price</th><th class='num'>Line Total</th></tr></thead>
            <tbody>%s</tbody>
            </table>
            <div class='totals'>
            <table>
            <tr><td class='muted'>Subtotal</td><td class='num'>$%s</td></tr>
            <tr><td class='muted'>Tax (8%%)</td><td class='num'>$%s</td></tr>
            <tr class='grand'><td>Total</td><td class='num'>$%s</td></tr>
            </table>
            </div>
            </div>
            </div>
            </body>
            </html>
            """,
            orderId,
            escapeHtml(orderDate.format(RECEIPT_TIMESTAMP)),
            escapeHtml(customerName),
            escapeHtml(customerEmail),
            escapeHtml(payment),
            rows,
            String.format("%.2f", subtotal),
            String.format("%.2f", tax),
            String.format("%.2f", total));
    }

    private String safeText(String value) {
        return value == null || value.isBlank() ? "N/A" : value.trim();
    }

    private String escapeHtml(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
    
    private boolean validateFields() {
        if (fullNameField.getText().isEmpty()) {
            AlertUtils.showWarning("Please enter full name!");
            return false;
        }
        if (addressField.getText().isEmpty()) {
            AlertUtils.showWarning("Please enter address!");
            return false;
        }
        if (!paymentMethod.getValue().equals("Cash on Delivery")) {
            if (cardNumberField.getText().length() < 16) {
                AlertUtils.showWarning("Please enter valid card number!");
                return false;
            }
            if (expiryField.getText().isBlank()) {
                AlertUtils.showWarning("Please enter card expiry!");
                return false;
            }
            if (cvvField.getText().length() < 3) {
                AlertUtils.showWarning("Please enter a valid CVV!");
                return false;
            }
        }
        return true;
    }
    
    @FXML
    public void handleBackToCart() {
        MainApp.changeScene("cart.fxml", "eBookShop - Shopping Cart");
    }
}