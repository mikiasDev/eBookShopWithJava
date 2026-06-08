package com.mycompany.ebookshop.controller;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import com.mycompany.ebookshop.MainApp;
import com.mycompany.ebookshop.database.BookDAO;
import com.mycompany.ebookshop.models.Book;
import com.mycompany.ebookshop.utils.AlertUtils;
import com.mycompany.ebookshop.utils.SessionManager;

import javafx.animation.Interpolator;
import javafx.animation.ScaleTransition;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;

public class MainController {

        private static final String DEFAULT_PLACEHOLDER_URL = "https://covers.openlibrary.org/b/isbn/9780134685991-L.jpg";
    private static final List<String> SAMPLE_IMAGE_URLS = List.of(
            "https://covers.openlibrary.org/b/isbn/9780134685991-L.jpg",
            "https://covers.openlibrary.org/b/isbn/9780132350884-L.jpg",
            "https://covers.openlibrary.org/b/isbn/9780201616224-L.jpg",
            "https://covers.openlibrary.org/b/isbn/9780596009205-L.jpg",
            "https://covers.openlibrary.org/b/isbn/9780743273565-L.jpg",
            "https://covers.openlibrary.org/b/isbn/9780061120084-L.jpg"
    );
    private static final double BASE_IMAGE_WIDTH = 120.0;
    private static final double BASE_IMAGE_HEIGHT = 170.0;
    private static final double BASE_CARD_WIDTH = 180.0;

    @FXML private Label welcomeLabel;
    @FXML private Button cartButton;
    @FXML private Label sectionTitleLabel;
    @FXML private ScrollPane booksScrollPane;
    @FXML private GridPane booksGrid;
    @FXML private ComboBox<String> categoryComboBox;
    @FXML private TextField searchField;
    @FXML private Button clearFilterBtn;
    @FXML private Slider zoomSlider;

    private final BookDAO bookDAO = new BookDAO();
    private final ObservableList<Book> allBooks = FXCollections.observableArrayList();
    private final List<BookCard> visibleCards = new ArrayList<>();
    private String activeCategory = "All Categories";

    @FXML
    public void initialize() {
        if (SessionManager.getInstance().getCurrentUser() != null && welcomeLabel != null) {
            welcomeLabel.setText("Welcome, " + SessionManager.getInstance().getCurrentUser().getFullName() + "!");
        }

        updateCartCount();
        configureZoomSlider();
        configureResponsiveGrid();
        loadBooks();
    }

    private void configureZoomSlider() {
        if (zoomSlider == null) {
            return;
        }
        zoomSlider.valueProperty().addListener((obs, oldValue, newValue) -> {
            updateGridLayout();
            updateVisibleCardZoom();
        });
    }

    private void configureResponsiveGrid() {
        if (booksScrollPane != null) {
            booksScrollPane.viewportBoundsProperty().addListener((obs, oldBounds, newBounds) -> updateGridLayout());
        }
        if (booksGrid != null) {
            booksGrid.setHgap(12.0);
            booksGrid.setVgap(12.0);
            booksGrid.setPadding(new Insets(4));
        }
    }

    private void loadBooks() {
        List<Book> books = bookDAO.getAllBooks();
        allBooks.setAll(books != null ? books : List.of());
        setupCategoryComboBox();
        renderBooks(applyFilters());
    }

    private void setupCategoryComboBox() {
        if (categoryComboBox == null) {
            return;
        }

        List<String> categories = allBooks.stream()
                .map(Book::getCategory)
                .filter(category -> category != null && !category.isBlank())
                .distinct()
                .sorted(Comparator.naturalOrder())
                .toList();

        List<String> comboItems = new ArrayList<>();
        comboItems.add("All Categories");
        comboItems.addAll(categories);
        categoryComboBox.setItems(FXCollections.observableArrayList(comboItems));
        categoryComboBox.setValue(activeCategory);
    }

    @FXML
    public void handleCategorySelect() {
        activeCategory = categoryComboBox != null && categoryComboBox.getValue() != null
                ? categoryComboBox.getValue()
                : "All Categories";
        if (clearFilterBtn != null) {
            clearFilterBtn.setVisible(!"All Categories".equals(activeCategory) || hasSearchText());
        }
        updateSectionTitle();
        renderBooks(applyFilters());
    }

    @FXML
    public void handleSearch() {
        if (clearFilterBtn != null) {
            clearFilterBtn.setVisible(hasSearchText() || (activeCategory != null && !"All Categories".equals(activeCategory)));
        }
        updateSectionTitle();
        renderBooks(applyFilters());
    }

    @FXML
    public void handleDashboard() {
        if (searchField != null) {
            searchField.clear();
        }
        activeCategory = "All Categories";
        if (categoryComboBox != null) {
            categoryComboBox.setValue("All Categories");
        }
        if (clearFilterBtn != null) {
            clearFilterBtn.setVisible(false);
        }
        updateSectionTitle();
        renderBooks(allBooks);
    }

    @FXML
    public void handleViewCart() {
        if (SessionManager.getInstance().getCart().isEmpty()) {
            AlertUtils.showWarning("Your cart is empty!");
        } else {
            MainApp.changeScene("cart.fxml", "eBookShop - Shopping Cart");
        }
    }

    @FXML
    public void handleViewOrders() {
        MainApp.changeScene("orders.fxml", "eBookShop - My Orders");
    }

    @FXML
    public void handleProfile() {
        MainApp.changeScene("profile.fxml", "eBookShop - Profile");
    }

    @FXML
    public void handleLogout() {
        SessionManager.getInstance().clearSession();
        MainApp.changeScene("login.fxml", "eBookShop - Login");
    }

    @FXML
    public void handleGroupMembers() {
        MainApp.changeScene("group_members.fxml", "eBookShop - Group Members");
    }

    private List<Book> applyFilters() {
        String query = searchField != null && searchField.getText() != null
                ? searchField.getText().trim().toLowerCase(Locale.ROOT)
                : "";

        return allBooks.stream()
                .filter(book -> "All Categories".equals(activeCategory)
                        || (book.getCategory() != null && activeCategory.equals(book.getCategory())))
                .filter(book -> query.isBlank()
                        || containsIgnoreCase(book.getTitle(), query)
                        || containsIgnoreCase(book.getAuthor(), query))
                .toList();
    }

    private boolean containsIgnoreCase(String value, String query) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(query);
    }

    private boolean hasSearchText() {
        return searchField != null && searchField.getText() != null && !searchField.getText().trim().isBlank();
    }

    private void renderBooks(List<Book> books) {
        if (booksGrid == null) {
            return;
        }

        booksGrid.getChildren().clear();
        visibleCards.clear();

        if (books == null || books.isEmpty()) {
            if (sectionTitleLabel != null) {
                sectionTitleLabel.setText("No books found");
            }
            Label noBooksLabel = new Label("No books found matching your criteria.");
            noBooksLabel.setStyle("-fx-text-fill: #7f8c8d; -fx-font-size: 16px; -fx-padding: 20;");
            booksGrid.add(noBooksLabel, 0, 0);
            return;
        }

        updateSectionTitle();

        int index = 0;
        for (Book book : books) {
            BookCard card = createBookCard(book, index++);
            visibleCards.add(card);
        }

        updateGridLayout();
        updateVisibleCardZoom();
    }

    private BookCard createBookCard(Book book, int index) {
        ImageView coverView = new ImageView();
        coverView.setPreserveRatio(true);
        coverView.setSmooth(true);
        coverView.setCache(true);
        coverView.setFitWidth(BASE_IMAGE_WIDTH);
        coverView.setFitHeight(BASE_IMAGE_HEIGHT);

        Rectangle clip = new Rectangle(BASE_IMAGE_WIDTH, BASE_IMAGE_HEIGHT);
        clip.setArcWidth(18);
        clip.setArcHeight(18);
        coverView.setClip(clip);

        ProgressIndicator loadingIndicator = new ProgressIndicator();
        loadingIndicator.setMaxSize(28, 28);
        loadingIndicator.setVisible(true);

        StackPane imageHolder = new StackPane(coverView, loadingIndicator);
        imageHolder.setAlignment(Pos.CENTER);
        imageHolder.setMinHeight(BASE_IMAGE_HEIGHT + 16);

        Label titleLabel = new Label(book.getTitle());
        titleLabel.getStyleClass().add("book-title");
        titleLabel.setWrapText(true);
        titleLabel.setMaxWidth(BASE_IMAGE_WIDTH + 20);
        titleLabel.setAlignment(Pos.CENTER);
        titleLabel.setFont(Font.font(titleLabel.getFont().getFamily(), FontWeight.BOLD, 15));

        Label authorLabel = new Label(book.getAuthor());
        authorLabel.getStyleClass().add("book-author");
        authorLabel.setWrapText(true);
        authorLabel.setMaxWidth(BASE_IMAGE_WIDTH + 20);
        authorLabel.setAlignment(Pos.CENTER);

        Label categoryLabel = new Label(book.getCategory() != null && !book.getCategory().isBlank() ? book.getCategory() : "Book");
        categoryLabel.getStyleClass().add("catalog-book-chip");

        Label priceLabel = new Label(String.format("$%.2f", book.getPrice()));
        priceLabel.getStyleClass().add("book-price-tag");

        Button addToCartBtn = new Button("Add to Cart");
        addToCartBtn.getStyleClass().add("button-primary");
        addToCartBtn.setMaxWidth(Double.MAX_VALUE);
        
        addToCartBtn.setOnAction(evt -> {
            SessionManager.getInstance().addToCart(book);
            updateCartCount();
            AlertUtils.showInfo("Added to cart: " + book.getTitle());
        });

        VBox buttonRow = new VBox(8, addToCartBtn);
        buttonRow.setFillWidth(true);
        buttonRow.setMaxWidth(Double.MAX_VALUE);

        VBox detailsBox = new VBox(4, titleLabel, authorLabel, priceLabel);
        detailsBox.setAlignment(Pos.CENTER);

        VBox cardRoot = new VBox(10, categoryLabel, imageHolder, detailsBox, buttonRow);
        cardRoot.getStyleClass().add("book-card");
        cardRoot.setAlignment(Pos.CENTER);
        cardRoot.setPadding(new Insets(16));
        cardRoot.setMinWidth(BASE_CARD_WIDTH + 12);
        cardRoot.setPrefWidth(BASE_CARD_WIDTH + 12);
        cardRoot.setMaxWidth(BASE_CARD_WIDTH + 120);

        VBox.setVgrow(imageHolder, Priority.NEVER);
        VBox.setVgrow(buttonRow, Priority.NEVER);

        String imageUrl = resolveDashboardImageUrl(book, index);
        loadImageAsync(imageUrl, coverView, loadingIndicator, book.getTitle());

        installHoverZoom(coverView, cardRoot);
        coverView.setOnMouseClicked(event -> {
            if (event.getClickCount() >= 1) {
                openImagePopup(book, imageUrl);
            }
        });

        return new BookCard(cardRoot, coverView, loadingIndicator, imageUrl);
    }

    private void installHoverZoom(ImageView coverView, VBox cardRoot) {
        ScaleTransition zoomIn = new ScaleTransition(Duration.millis(180), coverView);
        zoomIn.setToX(1.03);
        zoomIn.setToY(1.03);
        zoomIn.setInterpolator(Interpolator.EASE_BOTH);

        ScaleTransition zoomOut = new ScaleTransition(Duration.millis(180), coverView);
        zoomOut.setToX(1.0);
        zoomOut.setToY(1.0);
        zoomOut.setInterpolator(Interpolator.EASE_BOTH);

        cardRoot.addEventHandler(MouseEvent.MOUSE_ENTERED, event -> zoomIn.playFromStart());
        cardRoot.addEventHandler(MouseEvent.MOUSE_EXITED, event -> zoomOut.playFromStart());
    }

    public void loadImageAsync(String imageUrl, ImageView targetView) {
        loadImageAsync(imageUrl, targetView, null, null);
    }

    private void loadImageAsync(String imageUrl, ImageView targetView, ProgressIndicator indicator, String bookTitle) {
        if (targetView == null) {
            return;
        }

        if (indicator != null) {
            indicator.setVisible(true);
        }

        Task<Image> loadTask = new Task<>() {
            @Override
            protected Image call() {
                return loadImageWithFallback(imageUrl, bookTitle);
            }
        };

        loadTask.setOnSucceeded(event -> {
            targetView.setImage(loadTask.getValue());
            targetView.setCache(true);
            targetView.setSmooth(true);
            if (indicator != null) {
                indicator.setVisible(false);
            }
        });

        loadTask.setOnFailed(event -> {
            if (indicator != null) {
                indicator.setVisible(false);
            }
            targetView.setImage(getEmbeddedPlaceholderImage());
        });

        Thread loader = new Thread(loadTask, "book-image-loader");
        loader.setDaemon(true);
        loader.start();
    }

    private Image loadImageWithFallback(String imageUrl, String bookTitle) {
        String candidateUrl = normalizeToHttps(imageUrl);
        String fallbackUrl = DEFAULT_PLACEHOLDER_URL;

        if (candidateUrl == null || candidateUrl.isBlank()) {
            candidateUrl = defaultSampleUrl(bookTitle);
        }

        if (looksLikeHttpUrl(imageUrl) && !isHttps(imageUrl)) {
            candidateUrl = normalizeToHttps(imageUrl);
        }

        try {
            Image remoteImage = new Image(candidateUrl, false);
            if (!remoteImage.isError()) {
                return remoteImage;
            }
        } catch (Exception ex) {
            if (looksLikeHttpUrl(imageUrl)) {
                Platform.runLater(() -> AlertUtils.showError("Image URL could not be loaded for: " + safeTitle(bookTitle)));
            }
        }

        try {
            Image fallbackImage = new Image(fallbackUrl, false);
            if (!fallbackImage.isError()) {
                return fallbackImage;
            }
        } catch (Exception ex) {
            
        }

        return getEmbeddedPlaceholderImage();
    }

    private boolean looksLikeHttpUrl(String value) {
        return value != null && value.trim().toLowerCase(Locale.ROOT).startsWith("http");
    }

    private boolean isHttps(String value) {
        return value != null && value.trim().toLowerCase(Locale.ROOT).startsWith("https://");
    }

    private String normalizeToHttps(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.startsWith("http://")) {
            return "https://" + trimmed.substring(7);
        }
        return trimmed;
    }

    private String defaultSampleUrl(String bookTitle) {
        int index = 0;
        if (bookTitle != null && !bookTitle.isBlank()) {
            index = Math.abs(bookTitle.hashCode());
        }
        return SAMPLE_IMAGE_URLS.get(index % SAMPLE_IMAGE_URLS.size());
    }

    private String coverUrlForIsbn(String isbn) {
        if (isbn == null) {
            return DEFAULT_PLACEHOLDER_URL;
        }
        String normalizedIsbn = isbn.trim().replaceAll("[^0-9Xx]", "");
        if (normalizedIsbn.isBlank()) {
            return DEFAULT_PLACEHOLDER_URL;
        }
        return "https://covers.openlibrary.org/b/isbn/" + normalizedIsbn + "-L.jpg";
    }

    private String resolveDashboardImageUrl(Book book, int index) {
        String imageUrl = book != null ? book.getImageUrl() : null;
        String normalized = normalizeToHttps(imageUrl);

        if (normalized != null && isHttps(normalized)) {
            return normalized;
        }

        if (looksLikeHttpUrl(imageUrl)) {
            return normalized;
        }

        if (book != null && book.getIsbn() != null && !book.getIsbn().isBlank()) {
            return coverUrlForIsbn(book.getIsbn());
        }

        return defaultSampleUrl(book != null ? book.getTitle() + "-" + index : String.valueOf(index));
    }

    private Image getEmbeddedPlaceholderImage() {
        return new Image(DEFAULT_PLACEHOLDER_URL, false);
    }

    private void openImagePopup(Book book, String imageUrl) {
        Stage popupStage = new Stage();
        popupStage.initModality(Modality.APPLICATION_MODAL);
        popupStage.setTitle(book.getTitle());

        ImageView popupImage = new ImageView();
        popupImage.setPreserveRatio(true);
        popupImage.setSmooth(true);
        popupImage.setCache(true);
        popupImage.setFitWidth(700);
        popupImage.setFitHeight(1000);

        ProgressIndicator popupIndicator = new ProgressIndicator();
        popupIndicator.setMaxSize(42, 42);

        StackPane popupImageHolder = new StackPane(popupImage, popupIndicator);
        popupImageHolder.setAlignment(Pos.CENTER);
        popupImageHolder.setPadding(new Insets(12));

        Slider popupZoom = new Slider(0.5, 3.0, 1.0);
        popupZoom.setShowTickLabels(true);
        popupZoom.setShowTickMarks(true);
        popupZoom.setBlockIncrement(0.1);

        Label popupTitle = new Label(book.getTitle());
        popupTitle.getStyleClass().add("page-title");
        popupTitle.setFont(Font.font(popupTitle.getFont().getFamily(), FontWeight.BOLD, 22));

        Label popupMeta = new Label(book.getAuthor() + "  |  $" + String.format("%.2f", book.getPrice()));
        popupMeta.getStyleClass().add("page-subtitle");

        popupZoom.valueProperty().addListener((obs, oldValue, newValue) -> {
            double zoom = newValue.doubleValue();
            popupImage.setFitWidth(700 * zoom);
            popupImage.setFitHeight(1000 * zoom);
        });

        VBox popupContent = new VBox(14,
                popupTitle,
                popupMeta,
                popupZoom,
                new Separator(),
                popupImageHolder);
        popupContent.setPadding(new Insets(18));
        popupContent.getStyleClass().add("surface-card");

        ScrollPane scrollPane = new ScrollPane(popupContent);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background-color: transparent; -fx-background: transparent;");

        Scene scene = new Scene(scrollPane, 860, 980);
        popupStage.setScene(scene);

        loadImageAsync(imageUrl, popupImage, popupIndicator, book.getTitle());

        popupStage.show();
    }

    private void updateGridLayout() {
        if (booksGrid == null) {
            return;
        }

        booksGrid.getChildren().clear();

        if (visibleCards.isEmpty()) {
            return;
        }

        double zoom = zoomSlider != null ? zoomSlider.getValue() : 1.0;
        double viewportWidth = getViewportWidth();
        double horizontalGap = booksGrid.getHgap() > 0 ? booksGrid.getHgap() : 22.0;

        
        double baseTarget = Math.max(180.0, BASE_CARD_WIDTH * zoom);

        
        int columns = Math.max(1, (int) Math.floor((viewportWidth + horizontalGap) / (baseTarget + horizontalGap)));
        final int MAX_COLUMNS = 4;
        if (columns > MAX_COLUMNS) {
            columns = MAX_COLUMNS;
        }

        
        double columnTargetWidth;
        if (columns > 1) {
            double totalGaps = horizontalGap * (columns - 1);
            double distributed = (viewportWidth - totalGaps) / columns;
            
            columnTargetWidth = Math.max(baseTarget, Math.floor(distributed) - 8);
        } else {
            columnTargetWidth = Math.max(baseTarget, viewportWidth - 24);
        }

        int row = 0;
        int column = 0;
        for (BookCard card : visibleCards) {
            card.root.setPrefWidth(columnTargetWidth);
            card.root.setMaxWidth(Math.max(columnTargetWidth + 40, BASE_CARD_WIDTH * zoom + 40));
            booksGrid.add(card.root, column, row);
            GridPane.setHalignment(card.root, HPos.CENTER);
            GridPane.setValignment(card.root, VPos.TOP);
            column++;
            if (column >= columns) {
                column = 0;
                row++;
            }
        }
    }

    private double getViewportWidth() {
        if (booksScrollPane != null && booksScrollPane.getViewportBounds() != null) {
            double width = booksScrollPane.getViewportBounds().getWidth();
            if (width > 0) {
                return width;
            }
        }
        if (booksGrid != null && booksGrid.getWidth() > 0) {
            return booksGrid.getWidth();
        }
        return 980.0;
    }

    private void updateVisibleCardZoom() {
        double zoom = zoomSlider != null ? zoomSlider.getValue() : 1.0;
        for (BookCard card : visibleCards) {
            card.coverView.setFitWidth(BASE_IMAGE_WIDTH * zoom);
            card.coverView.setFitHeight(BASE_IMAGE_HEIGHT * zoom);
            card.root.setScaleX(1.0);
            card.root.setScaleY(1.0);
        }
        if (booksGrid != null) {
            booksGrid.setHgap(12.0 * Math.max(0.85, zoom));
            booksGrid.setVgap(12.0 * Math.max(0.85, zoom));
        }
    }

    private void updateCartCount() {
        if (cartButton == null) {
            return;
        }
        int itemCount = SessionManager.getInstance().getCart().stream()
                .mapToInt(cartItem -> Math.max(1, cartItem.getQuantity()))
                .sum();
        cartButton.setText(itemCount > 0 ? "🛒 Cart (" + itemCount + ")" : "🛒 Cart");
    }

    private String safeTitle(String title) {
        return title == null || title.isBlank() ? "Untitled Book" : title;
    }

    private void updateSectionTitle() {
        if (sectionTitleLabel == null) {
            return;
        }

        String query = hasSearchText() ? searchField.getText().trim() : "";
        if (!query.isBlank()) {
            sectionTitleLabel.setText("Search Results for \"" + query + "\"");
            return;
        }

        if (activeCategory != null && !"All Categories".equals(activeCategory)) {
            sectionTitleLabel.setText("Category: " + activeCategory);
            return;
        }

        sectionTitleLabel.setText("Discover Your Next Read");
    }

    private static final class BookCard {
        private final VBox root;
        private final ImageView coverView;
        @SuppressWarnings("unused")
        private final ProgressIndicator progressIndicator;
        @SuppressWarnings("unused")
        private final String imageUrl;

        private BookCard(VBox root, ImageView coverView, ProgressIndicator progressIndicator, String imageUrl) {
            this.root = root;
            this.coverView = coverView;
            this.progressIndicator = progressIndicator;
            this.imageUrl = imageUrl;
        }
    }
}