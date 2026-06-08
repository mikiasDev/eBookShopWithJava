package com.mycompany.ebookshop.controller;

import java.awt.Desktop;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import com.mycompany.ebookshop.MainApp;
import com.mycompany.ebookshop.database.BookDAO;
import com.mycompany.ebookshop.database.OrderDAO;
import com.mycompany.ebookshop.models.Book;
import com.mycompany.ebookshop.models.Order;
import com.mycompany.ebookshop.models.OrderItem;
import com.mycompany.ebookshop.models.User;
import com.mycompany.ebookshop.utils.AlertUtils;
import com.mycompany.ebookshop.utils.DownloadUtils;
import com.mycompany.ebookshop.utils.SessionManager;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.control.TableCell;
import javafx.util.Callback;

@SuppressWarnings("all")
public class OrdersController {

    @FXML private TableView<Order> ordersTable;
    @FXML private TableColumn<Order, Integer> colOrderId;
    @FXML private TableColumn<Order, String> colOrderDate;
    @FXML private TableColumn<Order, Double> colOrderTotal;
    @FXML private TableColumn<Order, String> colOrderStatus;
    @FXML private TableColumn<Order, Void> colOrderAction;

    private final BookDAO bookDAO = new BookDAO();
    private final OrderDAO orderDAO = new OrderDAO();
    private static final DateTimeFormatter DF = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @FXML
    public void initialize() {
        colOrderId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colOrderDate.setCellValueFactory(cell -> {
            var order = cell.getValue();
            var dt = order.getOrderDate();
            return new javafx.beans.property.SimpleStringProperty(dt != null ? dt.format(DF) : "");
        });
        colOrderTotal.setCellValueFactory(new PropertyValueFactory<>("totalAmount"));
        colOrderStatus.setCellValueFactory(new PropertyValueFactory<>("status"));

        colOrderAction.setCellFactory(createActionCellFactory());

        loadOrders();
    }

    private Callback<TableColumn<Order, Void>, TableCell<Order, Void>> createActionCellFactory() {
        return param -> new TableCell<>() {
            private final Button btnDetails = new Button("Details");
            private final Button btnReceipt = new Button("Receipt");
            private final Button btnDownload = new Button("View");
            {
                btnDetails.setStyle("-fx-padding: 6 12; -fx-font-size: 11px; -fx-background-color: #007bff; -fx-text-fill: white; -fx-border-radius: 4; -fx-cursor: hand;");
                btnReceipt.setStyle("-fx-padding: 6 12; -fx-font-size: 11px; -fx-background-color: #28a745; -fx-text-fill: white; -fx-border-radius: 4; -fx-cursor: hand;");
                btnDownload.setStyle("-fx-padding: 6 12; -fx-font-size: 11px; -fx-background-color: #ffc107; -fx-text-fill: black; -fx-border-radius: 4; -fx-cursor: hand;");
                
                btnDetails.setOnAction(evt -> {
                    Order o = getTableView().getItems().get(getIndex());
                    showOrderDetails(o);
                });
                btnReceipt.setOnAction(evt -> {
                    Order o = getTableView().getItems().get(getIndex());
                    handleReceiptAction(o);
                });
                btnDownload.setOnAction(evt -> {
                    Order o = getTableView().getItems().get(getIndex());
                    viewBooksFromOrder(o);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    HBox box = new HBox(8, btnDetails, btnReceipt, btnDownload);
                    box.setStyle("-fx-alignment: center-left;");
                    setGraphic(box);
                }
            }
        };
    }

    private void showOrderDetails(Order order) {
        if (order == null) return;
        var items = order.getItems();
        StringBuilder sb = new StringBuilder();
        sb.append("Order #").append(order.getId()).append("\n");
        sb.append("Date: ").append(order.getOrderDate() != null ? order.getOrderDate().format(DF) : "").append("\n");
        sb.append("Status: ").append(order.getStatus()).append("\n\n");
        sb.append("Items:\n");
        if (items == null || items.isEmpty()) {
            sb.append("(no items)");
        } else {
            items.forEach(it -> sb.append(it.getBookTitle()).append(" x").append(it.getQuantity()).append(" — $").append(String.format("%.2f", it.getPrice())).append("\n"));
        }
        javafx.scene.control.TextArea ta = new javafx.scene.control.TextArea(sb.toString());
        ta.setEditable(false);
        ta.setWrapText(true);
        ta.setPrefRowCount(12);
        javafx.scene.control.Alert a = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.INFORMATION);
        a.setTitle("Order Details");
        a.getDialogPane().setContent(ta);
        a.showAndWait();
    }

    private void handleReceiptAction(Order order) {
        if (order == null) return;
        String receiptHtml = buildReceiptHtml(order);
        try {
            var receiptsDir = java.nio.file.Path.of(System.getProperty("user.home", "."), "eBookShopReceipts");
            java.nio.file.Files.createDirectories(receiptsDir);
            var target = receiptsDir.resolve("eBookShop-Receipt-" + order.getId() + ".html");
            java.nio.file.Files.writeString(target, receiptHtml, java.nio.charset.StandardCharsets.UTF_8);
            orderDAO.updateReceiptPath(order.getId(), target.toString());
            order.setReceiptPath(target.toString());
            Desktop.getDesktop().browse(target.toUri());
        } catch (java.io.IOException | RuntimeException e) {
            System.err.println("OrdersController receipt action error: " + e.getMessage());
            com.mycompany.ebookshop.utils.AlertUtils.showError("Unable to open or generate receipt.");
        }
    }

    private void loadOrders() {
        var user = SessionManager.getInstance().getCurrentUser();
        if (user == null) return;
        List<Order> orders = orderDAO.getOrdersByUser(user.getId());
        
        for (Order o : orders) {
            var items = orderDAO.getOrderItemsByOrder(o.getId());
            o.setItems(items);
        }
        ordersTable.getItems().setAll(orders);
    }



    @FXML
    public void handleBack() {
        MainApp.changeScene("main.fxml", "eBookShop - Book Store");
    }

        private String buildReceiptHtml(Order order) {
        List<OrderItem> items = order.getItems();
        if (items == null || items.isEmpty()) {
            items = orderDAO.getOrderItemsByOrder(order.getId());
            order.setItems(items);
        }

        double subtotal = 0.0;
        List<String> rowHtml = new ArrayList<>();
        for (OrderItem item : items) {
            Book book = bookDAO.getBookById(item.getBookId());
            String title = book != null ? book.getTitle() : safeText(item.getBookTitle());
            String author = book != null ? book.getAuthor() : "N/A";
            double lineTotal = item.getPrice() * item.getQuantity();
            subtotal += lineTotal;
            rowHtml.add("<tr>"
                + "<td>" + escapeHtml(title) + "</td>"
                + "<td>" + escapeHtml(author) + "</td>"
                + "<td class='num'>" + item.getQuantity() + "</td>"
                + "<td class='num'>$" + String.format("%.2f", item.getPrice()) + "</td>"
                + "<td class='num'>$" + String.format("%.2f", lineTotal) + "</td>"
                + "</tr>");
        }

        double tax = subtotal * 0.08;
        double total = subtotal + tax;
        String rows = String.join("", rowHtml);
        String dateStr = order.getOrderDate() != null ? order.getOrderDate().format(DF) : "";
        User user = SessionManager.getInstance().getCurrentUser();
        String customerName = user != null ? safeText(user.getFullName()) : "N/A";
        String customerEmail = user != null ? safeText(user.getEmail()) : "N/A";
        String customerPhone = user != null ? safeText(user.getPhone()) : "N/A";
        String customerAddress = user != null ? safeText(user.getAddress()) : "N/A";

        return String.format("""
            <!DOCTYPE html>
            <html lang='en'>
            <head>
            <meta charset='utf-8'>
            <meta name='viewport' content='width=device-width, initial-scale=1.0'>
            <title>eBookShop Receipt</title>
            <style>
            body{font-family:Segoe UI,Arial,sans-serif;background:#f4f8fc;color:#183b56;margin:0;padding:32px;}
            .receipt{max-width:900px;margin:0 auto;background:#fff;border:1px solid #dfe9f5;border-radius:18px;box-shadow:0 12px 30px rgba(24,59,86,.08);overflow:hidden;}
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
            <div><strong>Phone:</strong> %s</div>
            <div><strong>Status:</strong> %s</div>
            <div style='grid-column:1 / -1;'><strong>Address:</strong> %s</div>
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
            order.getId(),
            escapeHtml(dateStr),
            escapeHtml(customerName),
            escapeHtml(customerEmail),
            escapeHtml(customerPhone),
            escapeHtml(safeText(order.getStatus())),
            escapeHtml(customerAddress),
            rows,
            String.format("%.2f", subtotal),
            String.format("%.2f", tax),
            String.format("%.2f", total));
    }

    private void downloadBooksFromOrder(Order order) {
        if (order == null) return;
        
        List<OrderItem> items = order.getItems();
        if (items == null || items.isEmpty()) {
            items = orderDAO.getOrderItemsByOrder(order.getId());
        }
        
        List<String> bookTitles = new ArrayList<>();
        List<String> filePaths = new ArrayList<>();
        
        for (OrderItem item : items) {
            Book book = bookDAO.getBookById(item.getBookId());
            if (book != null && book.getFilePath() != null && !book.getFilePath().trim().isEmpty()) {
                bookTitles.add(book.getTitle());
                filePaths.add(book.getFilePath());
            }
        }
        
        if (bookTitles.isEmpty()) {
            AlertUtils.showWarning("No downloadable books in this order.");
        } else {
            DownloadUtils.downloadBooks(bookTitles, filePaths);
        }
    }

    private void viewBooksFromOrder(Order order) {
        if (order == null) return;

        List<OrderItem> items = order.getItems();
        if (items == null || items.isEmpty()) {
            items = orderDAO.getOrderItemsByOrder(order.getId());
        }

        List<String> bookTitles = new ArrayList<>();
        List<String> filePaths = new ArrayList<>();

        for (OrderItem item : items) {
            Book book = bookDAO.getBookById(item.getBookId());
            if (book != null && book.getFilePath() != null && !book.getFilePath().trim().isEmpty()) {
                bookTitles.add(book.getTitle());
                filePaths.add(book.getFilePath());
            }
        }

        if (bookTitles.isEmpty()) {
            AlertUtils.showWarning("No viewable books in this order.");
        } else {
            DownloadUtils.viewBooks(bookTitles, filePaths);
        }
    }

    private String safeText(String value) {
        return value == null || value.isBlank() ? "N/A" : value.trim();
    }

    private String escapeHtml(String value) {
        if (value == null) return "";
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&#39;");
    }
}
