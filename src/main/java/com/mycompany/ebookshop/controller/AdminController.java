
package com.mycompany.ebookshop.controller;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.mycompany.ebookshop.MainApp;
import com.mycompany.ebookshop.database.BookDAO;
import com.mycompany.ebookshop.database.DBConnection;
import com.mycompany.ebookshop.database.UserDAO;
import com.mycompany.ebookshop.models.Book;
import com.mycompany.ebookshop.models.User;
import com.mycompany.ebookshop.utils.AlertUtils;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.chart.AreaChart;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import com.mycompany.ebookshop.utils.SessionManager;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

@SuppressWarnings("all")
public class AdminController {

    private static final String DEFAULT_COVER_URL = "https://covers.openlibrary.org/b/isbn/9780134685991-L.jpg";
    private static final int INVENTORY_CATALOG_COLUMNS = 4;
    private static final double INVENTORY_CARD_GAP = 18.0;
    
    
    @FXML private AreaChart<String, Number> incomeChart;
    @FXML private BarChart<String, Number> customerChart;
    @FXML private AnchorPane dashboard_form;
    @FXML private AnchorPane inventory_form;
    @FXML private AnchorPane menu_form;
    @FXML private AnchorPane customers_form;

    @FXML private Button dashboardBtn;
    @FXML private Button inventoryBtn;
    @FXML private Button menuBtn;
    @FXML private Button customersBtn;
    
    
    @FXML private Label dashboard_NC;
    @FXML private Label dashboard_TI;
    @FXML private Label dashboard_TotalI;
    @FXML private Label dashboard_NSP;

    
    @FXML private TableView<User> customerTable;
    @FXML private TableColumn<User, Integer> colCustId;
    @FXML private TableColumn<User, String> colCustName;
    @FXML private TableColumn<User, String> colCustEmail;

    
    @FXML private FlowPane inventoryContainer;
    @FXML private TableView<OrderItem> orderTable;
    @FXML private TableColumn<OrderItem, String> colOrderProduct;
    @FXML private TableColumn<OrderItem, Integer> colOrderQty;
    @FXML private TableColumn<OrderItem, Double> colOrderPrice;
    @FXML private Label totalLabel, changeLabel;
    @FXML private TextField amountField;

    
    @FXML private TableView<Book> bookTable;
    @FXML private TableColumn<Book, String> colIsbn, colTitle, colAuthor, colCategory;
    @FXML private TableColumn<Book, Double> colPrice;
    @FXML private TextField titleField, authorField, isbnField, priceField;
    @FXML private TextField imageUrlField;
    @FXML private TextArea batchImageUrlArea;
    @FXML private ComboBox<String> categoryBox;
    @FXML private ImageView bookImageView;
    @FXML private Label imagePathLabel;

    
    @FXML private Label inventorySectionTitle;
    @FXML private TextField inventorySearchField;
    @FXML private ComboBox<String> inventoryCategoryComboBox;
    @FXML private Button inventoryClearFilterBtn;
    @FXML private ScrollPane inventoryBooksScrollPane;
    @FXML private FlowPane inventoryBooksGrid;

    private final BookDAO bookDAO = new BookDAO();
    private final ObservableList<OrderItem> currentOrder = FXCollections.observableArrayList();
    private Book selectedBook;
    private String selectedImagePath = "";
    private final ObservableList<Book> inventoryBooks = FXCollections.observableArrayList();
    private String inventoryActiveCategory = "All Categories";
    private static String pendingSectionName;

    public static void requestSection(String sectionName) {
        pendingSectionName = sectionName;
    }

    @FXML
    public void handleProfile() {
        MainApp.changeScene("profile.fxml", "eBookShop - Profile");
    }

    private static String consumePendingSection() {
        String sectionName = pendingSectionName;
        pendingSectionName = null;
        return sectionName;
    }
    
    @FXML
    public void initialize() {
        
        if (incomeChart != null) { setupDashboardCharts(); }
        
        if (inventoryBooksGrid != null) {
            setupInventoryCatalogView();
            loadInventoryCatalogBooks();
        }
        
        if (bookTable != null) { 
            setupInventoryTable(); 
            loadBooks(); 
        }
        
        if (inventoryContainer != null) { 
            setupInventoryGrid(); 
            setupOrderTable(); 
        }
        if (categoryBox != null) { 
            categoryBox.getItems().addAll("Fiction", "Non-Fiction", "Science", "History", 
                "Technology", "Children", "World Literature", "Business", 
                "African Literature", "American Literature", "British Literature"); 
        }
        updateImagePreview();
        if (customerTable != null) {
            setupCustomerTable();
            loadCustomers();
        }

        if (hasAdminShell()) {
            String initialSection = consumePendingSection();
            showAdminSection(initialSection != null ? initialSection : "menu");
        }
    }

    
    private void setupDashboardCharts() {
        loadDashboardMetrics();
        loadDashboardTrendCharts();
    }

    private void loadDashboardMetrics() {
        if (dashboard_NC != null) {
            dashboard_NC.setText(String.valueOf(fetchCustomerCount()));
        }
        if (dashboard_TI != null) {
            dashboard_TI.setText(String.format("$%.2f", fetchTodayIncome()));
        }
        if (dashboard_TotalI != null) {
            dashboard_TotalI.setText(String.format("$%.2f", fetchTotalRevenue()));
        }
        if (dashboard_NSP != null) {
            dashboard_NSP.setText(String.valueOf(fetchBooksSold()));
        }
    }

    private void loadDashboardTrendCharts() {
        if (incomeChart != null) {
            incomeChart.getData().clear();
        }
        if (customerChart != null) {
            customerChart.getData().clear();
        }

        XYChart.Series<String, Number> incomeSeries = new XYChart.Series<>();
        incomeSeries.setName("Daily Income");
        for (Map.Entry<String, Number> entry : fetchDailyIncomeTrend().entrySet()) {
            incomeSeries.getData().add(new XYChart.Data<>(entry.getKey(), entry.getValue()));
        }
        if (incomeChart != null) {
            incomeChart.getData().add(incomeSeries);
        }

        XYChart.Series<String, Number> customerSeries = new XYChart.Series<>();
        customerSeries.setName("Daily Readers");
        for (Map.Entry<String, Number> entry : fetchDailyReaderTrend().entrySet()) {
            customerSeries.getData().add(new XYChart.Data<>(entry.getKey(), entry.getValue()));
        }
        if (customerChart != null) {
            customerChart.getData().add(customerSeries);
        }
    }

    private int fetchCustomerCount() {
        String query = "SELECT COUNT(*) FROM users WHERE LOWER(COALESCE(role, '')) = 'customer'";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn != null ? conn.prepareStatement(query) : null;
             ResultSet rs = pstmt != null ? pstmt.executeQuery() : null) {
            if (rs != null && rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            System.err.println("Dashboard customer count failed: " + e.getMessage());
        }
        return 0;
    }

    private double fetchTodayIncome() {
        String query = "SELECT COALESCE(SUM(total_amount), 0) FROM orders WHERE DATE(created_at) = CURRENT_DATE";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn != null ? conn.prepareStatement(query) : null;
             ResultSet rs = pstmt != null ? pstmt.executeQuery() : null) {
            if (rs != null && rs.next()) {
                return rs.getDouble(1);
            }
        } catch (SQLException e) {
            System.err.println("Dashboard today income failed: " + e.getMessage());
        }
        return 0.0;
    }

    private double fetchTotalRevenue() {
        String query = "SELECT COALESCE(SUM(total_amount), 0) FROM orders";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn != null ? conn.prepareStatement(query) : null;
             ResultSet rs = pstmt != null ? pstmt.executeQuery() : null) {
            if (rs != null && rs.next()) {
                return rs.getDouble(1);
            }
        } catch (SQLException e) {
            System.err.println("Dashboard total revenue failed: " + e.getMessage());
        }
        return 0.0;
    }

    private int fetchBooksSold() {
        String query = "SELECT COALESCE(SUM(quantity), 0) FROM order_items";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn != null ? conn.prepareStatement(query) : null;
             ResultSet rs = pstmt != null ? pstmt.executeQuery() : null) {
            if (rs != null && rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            System.err.println("Dashboard books sold failed: " + e.getMessage());
        }
        return 0;
    }

    private Map<String, Number> fetchDailyIncomeTrend() {
        Map<String, Number> trend = new LinkedHashMap<>();
        String query = "SELECT DATE(created_at) AS day_key, COALESCE(SUM(total_amount), 0) AS total_value " +
                "FROM orders WHERE created_at >= DATE_SUB(CURRENT_DATE, INTERVAL 6 DAY) " +
                "GROUP BY DATE(created_at) ORDER BY day_key";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn != null ? conn.prepareStatement(query) : null;
             ResultSet rs = pstmt != null ? pstmt.executeQuery() : null) {
            while (rs != null && rs.next()) {
                trend.put(rs.getString("day_key"), rs.getDouble("total_value"));
            }
        } catch (SQLException e) {
            System.err.println("Dashboard income trend failed: " + e.getMessage());
        }

        if (trend.isEmpty()) {
            trend.put("No orders", 0);
        }
        return trend;
    }

    private Map<String, Number> fetchDailyReaderTrend() {
        Map<String, Number> trend = new LinkedHashMap<>();
        String query = "SELECT DATE(created_at) AS day_key, COUNT(*) AS total_value " +
                "FROM users WHERE created_at >= DATE_SUB(CURRENT_DATE, INTERVAL 6 DAY) AND LOWER(COALESCE(role, '')) = 'customer' " +
                "GROUP BY DATE(created_at) ORDER BY day_key";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn != null ? conn.prepareStatement(query) : null;
             ResultSet rs = pstmt != null ? pstmt.executeQuery() : null) {
            while (rs != null && rs.next()) {
                trend.put(rs.getString("day_key"), rs.getInt("total_value"));
            }
        } catch (SQLException e) {
            System.err.println("Dashboard reader trend failed: " + e.getMessage());
        }

        if (trend.isEmpty()) {
            trend.put("No readers", 0);
        }
        return trend;
    }

    
    private void setupOrderTable() {
        if (orderTable == null) return;
        colOrderProduct.setCellValueFactory(new PropertyValueFactory<>("title"));
        colOrderQty.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        colOrderPrice.setCellValueFactory(new PropertyValueFactory<>("price"));
        orderTable.setItems(currentOrder);
    }

    private void setupInventoryGrid() {
        List<Book> books = bookDAO.getAllBooks();
        if (inventoryBooksGrid != null) {
            inventoryBooksGrid.getChildren().clear();
            if (books != null) {
                for (Book book : books) {
                    inventoryBooksGrid.getChildren().add(createMenuCard(book));
                }
            }
            updateInventoryCatalogLayout();
            return;
        }

        if (inventoryContainer == null) {
            return;
        }

        inventoryContainer.getChildren().clear();
        if (books != null) {
            for (Book book : books) {
                inventoryContainer.getChildren().add(createMenuCard(book));
            }
        }
    }

    private void setupInventoryCatalogView() {
        if (inventoryBooksScrollPane != null) {
            inventoryBooksScrollPane.viewportBoundsProperty().addListener((obs, oldBounds, newBounds) -> updateInventoryCatalogLayout());
        }
        if (inventoryBooksGrid != null) {
            inventoryBooksGrid.setHgap(INVENTORY_CARD_GAP);
            inventoryBooksGrid.setVgap(INVENTORY_CARD_GAP);
            inventoryBooksGrid.setStyle("-fx-padding: 8;");
            updateInventoryCatalogLayout();
        }
        if (inventoryCategoryComboBox != null) {
            inventoryCategoryComboBox.setOnAction(event -> handleInventoryCategorySelect());
        }
        if (inventoryClearFilterBtn != null) {
            inventoryClearFilterBtn.setVisible(false);
        }
        updateInventoryCatalogTitle();
    }

    private void loadInventoryCatalogBooks() {
        List<Book> books = bookDAO.getAllBooks();
        inventoryBooks.setAll(books != null ? books : List.of());
        setupInventoryCatalogCategories();
        renderInventoryCatalogBooks(applyInventoryCatalogFilters());
    }

    private void setupInventoryCatalogCategories() {
        if (inventoryCategoryComboBox == null) {
            return;
        }

        List<String> categories = inventoryBooks.stream()
                .map(Book::getCategory)
                .filter(category -> category != null && !category.isBlank())
                .distinct()
                .sorted(Comparator.naturalOrder())
                .toList();

        List<String> comboItems = new ArrayList<>();
        comboItems.add("All Categories");
        comboItems.addAll(categories);
        inventoryCategoryComboBox.setItems(FXCollections.observableArrayList(comboItems));
        inventoryCategoryComboBox.setValue(inventoryActiveCategory);
    }

    @FXML
    private void handleInventorySearch() {
        if (inventoryClearFilterBtn != null) {
            inventoryClearFilterBtn.setVisible(hasInventoryCatalogFilter());
        }
        updateInventoryCatalogTitle();
        renderInventoryCatalogBooks(applyInventoryCatalogFilters());
    }

    @FXML
    public void handleInventorySearch(KeyEvent event) {
        event.getEventType();
        handleInventorySearch();
    }

    @FXML
    private void handleInventoryCategorySelect() {
        inventoryActiveCategory = inventoryCategoryComboBox != null && inventoryCategoryComboBox.getValue() != null
                ? inventoryCategoryComboBox.getValue()
                : "All Categories";
        if (inventoryClearFilterBtn != null) {
            inventoryClearFilterBtn.setVisible(hasInventoryCatalogFilter());
        }
        updateInventoryCatalogTitle();
        renderInventoryCatalogBooks(applyInventoryCatalogFilters());
    }
    
    @FXML
    public void handleViewOrders() {
        com.mycompany.ebookshop.MainApp.changeScene("orders.fxml", "eBookShop - Orders");
    }

    @FXML
    public void handleInventoryClearFilters() {
        if (inventorySearchField != null) {
            inventorySearchField.clear();
        }
        inventoryActiveCategory = "All Categories";
        if (inventoryCategoryComboBox != null) {
            inventoryCategoryComboBox.setValue("All Categories");
        }
        if (inventoryClearFilterBtn != null) {
            inventoryClearFilterBtn.setVisible(false);
        }
        updateInventoryCatalogTitle();
        renderInventoryCatalogBooks(inventoryBooks);
    }

    private List<Book> applyInventoryCatalogFilters() {
        String query = inventorySearchField != null && inventorySearchField.getText() != null
                ? inventorySearchField.getText().trim().toLowerCase()
                : "";

        return inventoryBooks.stream()
                .filter(book -> "All Categories".equals(inventoryActiveCategory)
                        || (book.getCategory() != null && inventoryActiveCategory.equals(book.getCategory())))
                .filter(book -> query.isBlank()
                        || containsIgnoreCase(book.getTitle(), query)
                        || containsIgnoreCase(book.getAuthor(), query))
                .toList();
    }

    private boolean containsIgnoreCase(String value, String query) {
        return value != null && value.toLowerCase().contains(query);
    }

    private boolean hasInventoryCatalogFilter() {
        return (inventorySearchField != null && inventorySearchField.getText() != null && !inventorySearchField.getText().trim().isBlank())
                || (inventoryActiveCategory != null && !"All Categories".equals(inventoryActiveCategory));
    }

    private void renderInventoryCatalogBooks(List<Book> books) {
        if (inventoryBooksGrid == null) {
            setupInventoryGrid();
            return;
        }

        inventoryBooksGrid.getChildren().clear();

        if (books == null || books.isEmpty()) {
            if (inventorySectionTitle != null) {
                inventorySectionTitle.setText("No books found");
            }
            Label emptyLabel = new Label("No books found matching your criteria.");
            emptyLabel.setStyle("-fx-text-fill: #7f8c8d; -fx-font-size: 16px; -fx-padding: 20;");
            inventoryBooksGrid.getChildren().add(emptyLabel);
            updateInventoryCatalogLayout();
            return;
        }

        updateInventoryCatalogTitle();
        if (inventorySectionTitle != null) {
            inventorySectionTitle.setText("Browse the catalog (" + books.size() + ")");
        }

        for (Book book : books) {
            inventoryBooksGrid.getChildren().add(createMenuCard(book));
        }

        updateInventoryCatalogLayout();
    }

    private void updateInventoryCatalogTitle() {
        if (inventorySectionTitle == null) {
            return;
        }

        String baseTitle = "Browse the catalog";
        if (inventoryBooks != null && !inventoryBooks.isEmpty()) {
            baseTitle = "Browse the catalog (" + inventoryBooks.size() + ")";
        }
        inventorySectionTitle.setText(baseTitle);
    }

    private void updateInventoryCatalogLayout() {
        if (inventoryBooksGrid == null || inventoryBooksScrollPane == null) {
            return;
        }

        double viewportWidth = inventoryBooksScrollPane.getViewportBounds() != null
                ? inventoryBooksScrollPane.getViewportBounds().getWidth()
                : 0;
        if (viewportWidth <= 0) {
            return;
        }

        double availableWidth = Math.max(0, viewportWidth - 24);
        double cardWidth = (availableWidth - (INVENTORY_CARD_GAP * (INVENTORY_CATALOG_COLUMNS - 1))) / INVENTORY_CATALOG_COLUMNS;
        double wrapLength = (cardWidth * INVENTORY_CATALOG_COLUMNS) + (INVENTORY_CARD_GAP * (INVENTORY_CATALOG_COLUMNS - 1));

        inventoryBooksGrid.setPrefWrapLength(wrapLength);
        for (var node : inventoryBooksGrid.getChildren()) {
            if (node instanceof VBox cardRoot) {
                cardRoot.setMinWidth(cardWidth);
                cardRoot.setPrefWidth(cardWidth);
                cardRoot.setMaxWidth(cardWidth);
            }
        }
    }

    private VBox createMenuCard(Book book) {
        
        final double IMG_W = 120.0;
        final double IMG_H = 170.0;
        final double CARD_BASE_W = 180.0;

        ImageView coverView = new ImageView();
        coverView.setPreserveRatio(true);
        coverView.setSmooth(true);
        coverView.setCache(true);
        coverView.setFitWidth(IMG_W);
        coverView.setFitHeight(IMG_H);

        StackPane imageHolder = new StackPane(coverView);
        imageHolder.setAlignment(Pos.CENTER);
        imageHolder.setMinHeight(IMG_H + 16);

        Label category = new Label(book.getCategory() != null && !book.getCategory().isBlank() ? book.getCategory() : "Book");
        category.getStyleClass().add("catalog-book-chip");

        Label title = new Label(book.getTitle());
        title.getStyleClass().add("book-title");
        title.setWrapText(true);
        title.setMaxWidth(IMG_W + 20);
        title.setAlignment(Pos.CENTER);

        Label author = new Label(book.getAuthor() != null && !book.getAuthor().isBlank() ? book.getAuthor() : "eBookShop author");
        author.getStyleClass().add("book-author");
        author.setWrapText(true);
        author.setMaxWidth(IMG_W + 20);
        author.setAlignment(Pos.CENTER);

        Label price = new Label(String.format("$%.2f", book.getPrice()));
        price.getStyleClass().add("book-price-tag");


        Button viewBtn = new Button("View");
        viewBtn.getStyleClass().add("button-primary");
        viewBtn.setMaxWidth(Double.MAX_VALUE);
        viewBtn.setOnAction(evt -> showBookDetails(book));

        VBox buttonRow = new VBox(8, viewBtn);
        buttonRow.setFillWidth(true);
        buttonRow.setMaxWidth(Double.MAX_VALUE);

        VBox detailsBox = new VBox(4, title, author, price);
        detailsBox.setAlignment(Pos.CENTER);

        VBox cardRoot = new VBox(10, category, imageHolder, detailsBox, buttonRow);
        cardRoot.getStyleClass().add("book-card");
        cardRoot.setAlignment(Pos.CENTER);
        cardRoot.setPadding(new Insets(16));
        cardRoot.setMinWidth(CARD_BASE_W + 12);
        cardRoot.setPrefWidth(CARD_BASE_W + 12);
        cardRoot.setMaxWidth(Double.MAX_VALUE);

        coverView.setImage(resolveBookImage(book.getImageUrl()));

        return cardRoot;
    }

    private void showBookDetails(Book book) {
        if (book == null) {
            AlertUtils.showWarning("No book details available.");
            return;
        }

        StringBuilder details = new StringBuilder();
        details.append("Title: ").append(nullToPlaceholder(book.getTitle())).append('\n');
        details.append("Author: ").append(nullToPlaceholder(book.getAuthor())).append('\n');
        details.append("Category: ").append(nullToPlaceholder(book.getCategory())).append('\n');
        details.append("ISBN: ").append(nullToPlaceholder(book.getIsbn())).append('\n');
        details.append("Price: $").append(String.format("%.2f", book.getPrice())).append('\n');
        details.append("File: ").append(nullToPlaceholder(book.getFilePath())).append('\n');
        details.append("Image: ").append(nullToPlaceholder(book.getImageUrl())).append('\n');
        details.append('\n');
        details.append("Description:\n");
        details.append(book.getDescription() == null || book.getDescription().isBlank()
                ? "No description available."
                : book.getDescription().trim());

        TextArea detailsArea = new TextArea(details.toString());
        detailsArea.setEditable(false);
        detailsArea.setWrapText(true);
        detailsArea.setPrefRowCount(14);
        detailsArea.setFocusTraversable(false);

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Book Details");
        alert.setHeaderText(book.getTitle());
        alert.getDialogPane().setContent(detailsArea);
        alert.getDialogPane().setPrefSize(540, 420);
        alert.showAndWait();
    }

    private String nullToPlaceholder(String value) {
        return value == null || value.isBlank() ? "N/A" : value;
    }

    private void updateTotal() {
        double total = currentOrder.stream().mapToDouble(OrderItem::getPrice).sum();
        if (totalLabel != null) {
            totalLabel.setText(String.format("%.2f", total));
        }
    }

    @FXML
    public void handlePay() {
        try {
            double total = Double.parseDouble(totalLabel.getText());
            double amount = Double.parseDouble(amountField.getText());
            if (amount < total) { 
                AlertUtils.showError("Insufficient amount!"); 
                return; 
            }
            changeLabel.setText(String.format("%.2f", amount - total));
            AlertUtils.showInfo("Payment successful!");
            
            currentOrder.clear();
            updateTotal();
            amountField.clear();
        } catch (NumberFormatException e) { 
            AlertUtils.showError("Invalid amount!"); 
            AlertUtils.showError("Error processing payment!"); 
        }
    }

    
    @FXML
    public void switchForm(javafx.event.ActionEvent event) {
        Object source = event.getSource();
        if (source instanceof Button btn) {
            String id = btn.getId();
            switch (id) {
                case "dashboardBtn" -> handleDashboard();
                case "inventoryBtn" -> handleInventory();
                case "menuBtn" -> handleMenu();
                case "customersBtn" -> handleCustomers();
                default -> {
                    
                }
            }
        }
    }
    
    @FXML 
    public void handleRemoveItem() {
        OrderItem selected = orderTable.getSelectionModel().getSelectedItem();
        if (selected != null) { 
            currentOrder.remove(selected); 
            updateTotal(); 
        } else {
            AlertUtils.showError("Please select an item to remove!");
        }
    }

    @FXML 
    public void handleReceipt() { 
        if (currentOrder.isEmpty()) {
            AlertUtils.showError("No items in order!");
            return;
        }
        AlertUtils.showInfo("Receipt printed (mock)!"); 
    }

    
    @FXML
    private void handleDashboard() {
        if (!showAdminSection("dashboard")) {
            requestSection("dashboard");
            com.mycompany.ebookshop.MainApp.changeScene("admin.fxml", "Book Dashboard");
        }
    }

    @FXML
    private void handleInventory() {
        if (!showAdminSection("inventory")) {
            requestSection("inventory");
            com.mycompany.ebookshop.MainApp.changeScene("admin.fxml", "Book Inventory");
        }
    }

    @FXML
    private void handleMenu() {
        if (!showAdminSection("menu")) {
            requestSection("menu");
            com.mycompany.ebookshop.MainApp.changeScene("admin.fxml", "Catalog");
        }
    }

    @FXML
    private void handleCustomers() {
        if (!showAdminSection("customers")) {
            requestSection("customers");
            com.mycompany.ebookshop.MainApp.changeScene("admin.fxml", "Readers"); 
        }
    }
    
    @FXML 
    public void handleLogout() { 
        com.mycompany.ebookshop.MainApp.changeScene("login.fxml", "Login"); 
    }

    
    private void setupInventoryTable() {
        if (bookTable == null) return;
        colIsbn.setCellValueFactory(new PropertyValueFactory<>("isbn"));
        colTitle.setCellValueFactory(new PropertyValueFactory<>("title"));
        colAuthor.setCellValueFactory(new PropertyValueFactory<>("author"));
        colCategory.setCellValueFactory(new PropertyValueFactory<>("category"));
        colPrice.setCellValueFactory(new PropertyValueFactory<>("price"));

        bookTable.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                selectedBook = newVal;
                titleField.setText(newVal.getTitle());
                authorField.setText(newVal.getAuthor());
                isbnField.setText(newVal.getIsbn());
                priceField.setText(String.valueOf(newVal.getPrice()));
                categoryBox.setValue(newVal.getCategory());
                selectedImagePath = (newVal.getImageUrl() == null || newVal.getImageUrl().isBlank())
                    ? coverUrlForIsbn(newVal.getIsbn())
                    : newVal.getImageUrl();
                if (imageUrlField != null) {
                    imageUrlField.setText(selectedImagePath);
                }
                updateImagePreview();
            }
        });
    }

    private void loadBooks() {
        List<Book> books = bookDAO.getAllBooks();
        if (books != null) {
            bookTable.setItems(FXCollections.observableArrayList(books));
        }
    }

    private void setupCustomerTable() {
        colCustId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colCustName.setCellValueFactory(new PropertyValueFactory<>("fullName"));
        colCustEmail.setCellValueFactory(new PropertyValueFactory<>("email"));
    }

    private void loadCustomers() {
        UserDAO userDAO = new UserDAO();
        List<User> users = userDAO.getAllUsers();
        if (users != null) {
            customerTable.setItems(FXCollections.observableArrayList(users));
        }
    }

    @FXML 
    public void handleAddBook() {
        
        if (titleField.getText().isEmpty() || authorField.getText().isEmpty() || 
            isbnField.getText().isEmpty() || categoryBox.getValue() == null) {
            AlertUtils.showError("Please fill all required fields!");
            return;
        }
        
        try {
            String imagePath = getSelectedImageSource();
            Book book = new Book(0, 
                                titleField.getText(), 
                                authorField.getText(), 
                                isbnField.getText(),
                                Double.parseDouble(priceField.getText()), 
                                categoryBox.getValue(), 
                                "", 
                                imagePath);
            if (bookDAO.addBook(book)) {
                AlertUtils.showInfo("Book added successfully!");
                loadBooks();
                loadInventoryCatalogBooks();
                handleClearForm();
                setupInventoryGrid(); 
            } else {
                AlertUtils.showError("Failed to add book!");
            }
        } catch (NumberFormatException e) { 
            AlertUtils.showError("Please enter valid numbers for price!"); 
        } catch (Exception e) { 
            AlertUtils.showError("Check input fields!"); 
            System.err.println("Error adding book: " + e.getMessage());
        }
    }

    @FXML 
    public void handleUpdateBook() {
        if (selectedBook == null) { 
            AlertUtils.showError("Select a book to update!"); 
            return; 
        }
        
        try {
            selectedBook.setTitle(titleField.getText());
            selectedBook.setAuthor(authorField.getText());
            selectedBook.setIsbn(isbnField.getText());
            selectedBook.setPrice(Double.parseDouble(priceField.getText()));
            selectedBook.setCategory(categoryBox.getValue());
            selectedBook.setImageUrl(getSelectedImageSource());
            
            if (bookDAO.updateBook(selectedBook)) {
                AlertUtils.showInfo("Book updated successfully!");
                loadBooks();
                loadInventoryCatalogBooks();
                handleClearForm();
                setupInventoryGrid(); 
            } else {
                AlertUtils.showError("Failed to update book!");
            }
        } catch (NumberFormatException e) { 
            AlertUtils.showError("Please enter valid numbers for price!"); 
        } catch (Exception e) { 
            AlertUtils.showError("Check input fields!"); 
            System.err.println("Error updating book: " + e.getMessage());
        }
    }

    @FXML 
    public void handleDeleteBook() {
        if (selectedBook == null) { 
            AlertUtils.showError("Select a book to delete!"); 
            return; 
        }
        
        if (bookDAO.deleteBook(selectedBook.getId())) {
            AlertUtils.showInfo("Book deleted successfully!");
            loadBooks();
            loadInventoryCatalogBooks();
            handleClearForm();
            setupInventoryGrid(); 
        } else {
            AlertUtils.showError("Failed to delete book!");
        }
    }

    @FXML 
    private void handleClearForm() {
        titleField.clear(); 
        authorField.clear(); 
        isbnField.clear();
        priceField.clear(); 
        
        categoryBox.setValue(null);
        selectedBook = null;
        selectedImagePath = "";
        if (imageUrlField != null) {
            imageUrlField.clear();
        }
        updateImagePreview();
    }

    @FXML
    public void handleApplyImageUrl() {
        if (imageUrlField == null) {
            return;
        }
        String value = imageUrlField.getText() == null ? "" : imageUrlField.getText().trim();
        selectedImagePath = value;
        updateImagePreview();
    }

    @FXML
    public void handleApplyBatchImageUrls() {
        if (batchImageUrlArea == null) {
            return;
        }

        String rawText = batchImageUrlArea.getText();
        if (rawText == null || rawText.isBlank()) {
            AlertUtils.showError("Paste one or more lines in the format id=url");
            return;
        }

        int updatedCount = 0;
        int skippedCount = 0;

        for (String line : rawText.split("\\R")) {
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                continue;
            }

            int separatorIndex = trimmed.indexOf('=');
            if (separatorIndex <= 0 || separatorIndex >= trimmed.length() - 1) {
                skippedCount++;
                continue;
            }

            String idPart = trimmed.substring(0, separatorIndex).trim();
            String urlPart = trimmed.substring(separatorIndex + 1).trim();
            try {
                int bookId = Integer.parseInt(idPart);
                Book book = bookDAO.getBookById(bookId);
                if (book == null) {
                    skippedCount++;
                    continue;
                }

                book.setImageUrl(urlPart);
                if (bookDAO.updateBook(book)) {
                    updatedCount++;
                } else {
                    skippedCount++;
                }
            } catch (NumberFormatException ex) {
                skippedCount++;
            }
        }

        loadBooks();
        loadInventoryCatalogBooks();
        setupInventoryGrid();
        updateImagePreview();

        if (updatedCount > 0) {
            AlertUtils.showInfo("Updated " + updatedCount + " book image URL" + (updatedCount == 1 ? "" : "s") + ". Skipped: " + skippedCount);
        } else {
            AlertUtils.showWarning("No valid batch entries were applied. Skipped: " + skippedCount);
        }
    }

    @FXML
    public void handleImportImage() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Book Image");
        fileChooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.webp")
        );

        File selectedFile = fileChooser.showOpenDialog(bookImageView != null ? bookImageView.getScene().getWindow() : null);
        if (selectedFile != null) {
            selectedImagePath = selectedFile.getAbsolutePath();
            if (imageUrlField != null) {
                imageUrlField.setText(selectedImagePath);
            }
            updateImagePreview();
        }
    }

    private boolean hasAdminShell() {
        return dashboard_form != null || inventory_form != null || menu_form != null || customers_form != null;
    }

    private boolean showAdminSection(String sectionName) {
        if (!hasAdminShell()) {
            return false;
        }

        setPaneVisible(dashboard_form, "dashboard".equals(sectionName));
        setPaneVisible(inventory_form, "inventory".equals(sectionName));
        setPaneVisible(menu_form, "menu".equals(sectionName));
        setPaneVisible(customers_form, "customers".equals(sectionName));
        updateSidebarState(sectionName);
        return true;
    }

    private void setPaneVisible(AnchorPane pane, boolean visible) {
        if (pane != null) {
            pane.setVisible(visible);
            pane.setManaged(visible);
        }
    }

    private void updateSidebarState(String activeSection) {
        setSidebarButtonState(dashboardBtn, "dashboardBtn".equals(buttonIdForSection(activeSection)));
        setSidebarButtonState(inventoryBtn, "inventoryBtn".equals(buttonIdForSection(activeSection)));
        setSidebarButtonState(menuBtn, "menuBtn".equals(buttonIdForSection(activeSection)));
        setSidebarButtonState(customersBtn, "customersBtn".equals(buttonIdForSection(activeSection)));
    }

    private String buttonIdForSection(String sectionName) {
        if ("inventory".equals(sectionName)) return "inventoryBtn";
        if ("menu".equals(sectionName)) return "menuBtn";
        if ("customers".equals(sectionName)) return "customersBtn";
        return "dashboardBtn";
    }

    private void setSidebarButtonState(Button button, boolean active) {
        if (button == null) {
            return;
        }
        button.getStyleClass().removeAll("sidebar-button", "sidebar-button-active");
        button.getStyleClass().add(active ? "sidebar-button-active" : "sidebar-button");
    }

    private void updateImagePreview() {
        if (imageUrlField != null && (selectedImagePath == null || selectedImagePath.isBlank())) {
            imageUrlField.clear();
        }
        if (bookImageView != null) {
            String effectivePath = (selectedImagePath == null || selectedImagePath.isBlank())
                ? DEFAULT_COVER_URL
                : selectedImagePath;
            bookImageView.setImage(resolveBookImage(effectivePath));
        }
        if (imagePathLabel != null) {
            imagePathLabel.setText(selectedImagePath == null || selectedImagePath.isEmpty()
                ? "No image selected"
                : new File(selectedImagePath).getName());
        }
    }

    private String getSelectedImageSource() {
        String urlValue = imageUrlField == null ? "" : imageUrlField.getText();
        if (urlValue != null && !urlValue.trim().isBlank()) {
            selectedImagePath = urlValue.trim();
            return selectedImagePath;
        }
        if (selectedImagePath != null && !selectedImagePath.isBlank()) {
            return selectedImagePath;
        }
        return DEFAULT_COVER_URL;
    }

    private String coverUrlForIsbn(String isbn) {
        if (isbn == null) {
            return DEFAULT_COVER_URL;
        }
        String normalizedIsbn = isbn.trim().replaceAll("[^0-9Xx]", "");
        if (normalizedIsbn.isBlank()) {
            return DEFAULT_COVER_URL;
        }
        return "https://covers.openlibrary.org/b/isbn/" + normalizedIsbn + "-L.jpg";
    }

    private Image resolveBookImage(String imagePath) {
        if (imagePath != null && !imagePath.isBlank()) {
            try {
                java.net.URL resourceUrl = getClass().getResource("/com/mycompany/ebookshop/" + imagePath);
                if (resourceUrl != null) {
                    return new Image(resourceUrl.toExternalForm(), true);
                }
                File localFile = new File(imagePath);
                if (localFile.exists()) {
                    return new Image(localFile.toURI().toString(), true);
                }
                return new Image(imagePath, true);
            } catch (Exception e) {
                
            }
        }
            return new Image(DEFAULT_COVER_URL, true);
    }

    
    public static class OrderItem {
        private final String title; 
        private int quantity; 
        private double price;
        
        public OrderItem(String title, int quantity, double price) { 
            this.title = title; 
            this.quantity = quantity; 
            this.price = price; 
        }
        
        public String getTitle() { return title; }
        public int getQuantity() { return quantity; }
        public double getPrice() { return price; }
        
        public void setQuantity(int quantity) { this.quantity = quantity; }
        public void setPrice(double price) { this.price = price; }
    }
}