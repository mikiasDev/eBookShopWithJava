package com.mycompany.ebookshop.controller;

import com.mycompany.ebookshop.MainApp;
import com.mycompany.ebookshop.models.Book;
import com.mycompany.ebookshop.database.BookDAO;
import com.mycompany.ebookshop.utils.AlertUtils;
import com.mycompany.ebookshop.utils.SessionManager;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

public class MainController {
    
    @FXML private TableView<Book> bookTable;
    @FXML private TableColumn<Book, String> colTitle;
    @FXML private TableColumn<Book, String> colAuthor;
    @FXML private TableColumn<Book, Double> colPrice;
    @FXML private TableColumn<Book, Integer> colStock;
    @FXML private TableColumn<Book, String> colCategory;
    @FXML private TextField searchField;
    @FXML private ComboBox<String> categoryFilter;
    @FXML private Label welcomeLabel;
    @FXML private Label cartCountLabel;
    
    private ObservableList<Book> bookList;
    private FilteredList<Book> filteredBooks;
    private BookDAO bookDAO;
    
    @FXML
    public void initialize() {
        bookDAO = new BookDAO();
        loadBooks();
        setupTableColumns();
        setupFilters();
        updateWelcomeMessage();
        updateCartCount();
    }
    @FXML
private void handleGroupMembers() {
    MainApp.changeScene("group_members.fxml", "eBookShop - Group Members");
}
    private void loadBooks() {
        bookList = FXCollections.observableArrayList(bookDAO.getAllBooks());
        filteredBooks = new FilteredList<>(bookList, b -> true);
        bookTable.setItems(filteredBooks);
    }
    
    @FXML
private void handleExit() {
    if (AlertUtils.showConfirmation("Exit application?")) {
        Platform.exit();
        System.exit(0);
    }
}

@FXML
private void handleProfile() {
    AlertUtils.showInfo("Profile: " + SessionManager.getInstance().getCurrentUser().getFullName() + 
                        "\nEmail: " + SessionManager.getInstance().getCurrentUser().getEmail() +
                        "\nRole: " + SessionManager.getInstance().getCurrentUser().getRole());
}

@FXML
private void handleOrderHistory() {
    AlertUtils.showInfo("Order history feature - Coming soon!");
}

@FXML
private void handleChangePassword() {
    AlertUtils.showInfo("Change password feature - Coming soon!");
}

@FXML
private void handleAbout() {
    AlertUtils.showInfo("eBookShop v1.0\nOnline Book Store System\nUniversity of Gondar");
}

@FXML
private void handleCheckout() {
    if (SessionManager.getInstance().getCart().isEmpty()) {
        AlertUtils.showWarning("Cart is empty!");
    } else {
        MainApp.changeScene("checkout.fxml", "eBookShop - Checkout");
    }
}
    private void setupTableColumns() {
        colTitle.setCellValueFactory(new PropertyValueFactory<>("title"));
        colAuthor.setCellValueFactory(new PropertyValueFactory<>("author"));
        colPrice.setCellValueFactory(new PropertyValueFactory<>("price"));
        colStock.setCellValueFactory(new PropertyValueFactory<>("stock"));
        colCategory.setCellValueFactory(new PropertyValueFactory<>("category"));
        
        // Price formatting
        colPrice.setCellFactory(col -> new TableCell<Book, Double>() {
            @Override
            protected void updateItem(Double price, boolean empty) {
                super.updateItem(price, empty);
                setText(empty ? null : String.format("$%.2f", price));
            }
        });
        
        // Stock color formatting
        colStock.setCellFactory(col -> new TableCell<Book, Integer>() {
            @Override
            protected void updateItem(Integer stock, boolean empty) {
                super.updateItem(stock, empty);
                if (empty || stock == null) {
                    setText(null);
                } else {
                    setText(String.valueOf(stock));
                    if (stock <= 0) {
                        setStyle("-fx-text-fill: red; -fx-font-weight: bold;");
                    } else if (stock <= 5) {
                        setStyle("-fx-text-fill: orange;");
                    } else {
                        setStyle("-fx-text-fill: green;");
                    }
                }
            }
        });
    }
    
    private void setupFilters() {
        // Category filter
        categoryFilter.getItems().addAll("All", "Fiction", "Non-Fiction", "Science", "History", "Technology", "Biography", "Children");
        categoryFilter.setValue("All");
        categoryFilter.setOnAction(e -> applyFilters());
        
        // Search filter
        searchField.textProperty().addListener((obs, old, newVal) -> applyFilters());
    }
    
    private void applyFilters() {
        String searchText = searchField.getText().toLowerCase();
        String category = categoryFilter.getValue();
        
        filteredBooks.setPredicate(book -> {
            boolean matchesSearch = searchText.isEmpty() || 
                book.getTitle().toLowerCase().contains(searchText) ||
                book.getAuthor().toLowerCase().contains(searchText);
            
            boolean matchesCategory = category.equals("All") || book.getCategory().equals(category);
            
            return matchesSearch && matchesCategory;
        });
    }
    
    private void updateWelcomeMessage() {
        if (SessionManager.getInstance().getCurrentUser() != null) {
            welcomeLabel.setText("Welcome, " + SessionManager.getInstance().getCurrentUser().getFullName() + "!");
        }
    }
    
    private void updateCartCount() {
        int count = SessionManager.getInstance().getCart().size();
        cartCountLabel.setText(String.valueOf(count));
        cartCountLabel.setVisible(count > 0);
    }
    
    @FXML
    private void handleAddToCart() {
        Book selected = bookTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            AlertUtils.showWarning("Please select a book first!");
            return;
        }
        
        if (selected.getStock() <= 0) {
            AlertUtils.showError("Book is out of stock!");
            return;
        }
        
        SessionManager.getInstance().addToCart(selected);
        updateCartCount();
        AlertUtils.showInfo("Added to cart: " + selected.getTitle());
    }
    
    @FXML
    private void handleViewCart() {
        if (SessionManager.getInstance().getCart().isEmpty()) {
            AlertUtils.showWarning("Your cart is empty!");
            return;
        }
        MainApp.changeScene("cart.fxml", "eBookShop - Shopping Cart");
    }
    
    @FXML
    private void handleLogout() {
        SessionManager.getInstance().clearSession();
        MainApp.changeScene("login.fxml", "eBookShop - Login");
    }
    
    @FXML
    private void handleRefresh() {
        loadBooks();
    }
}