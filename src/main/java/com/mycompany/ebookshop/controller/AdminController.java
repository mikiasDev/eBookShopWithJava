package com.mycompany.ebookshop.controller;

import com.mycompany.ebookshop.MainApp;
import com.mycompany.ebookshop.models.Book;
import com.mycompany.ebookshop.database.BookDAO;
import com.mycompany.ebookshop.utils.AlertUtils;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

public class AdminController {
    
    @FXML private TableView<Book> bookTable;
    @FXML private TableColumn<Book, String> colTitle;
    @FXML private TableColumn<Book, String> colAuthor;
    @FXML private TableColumn<Book, Double> colPrice;
    @FXML private TableColumn<Book, Integer> colStock;
    @FXML private TableColumn<Book, String> colCategory;
    
    @FXML private TextField titleField;
    @FXML private TextField authorField;
    @FXML private TextField isbnField;
    @FXML private TextField priceField;
    @FXML private TextField stockField;
    @FXML private ComboBox<String> categoryBox;
    @FXML private TextArea descriptionArea;
    
    private BookDAO bookDAO;
    private Book selectedBook;
    
    @FXML
    public void initialize() {
        bookDAO = new BookDAO();
        setupTable();
        loadBooks();
        setupCategoryBox();
        clearForm();
    }
    
    private void setupTable() {
        colTitle.setCellValueFactory(new PropertyValueFactory<>("title"));
        colAuthor.setCellValueFactory(new PropertyValueFactory<>("author"));
        colPrice.setCellValueFactory(new PropertyValueFactory<>("price"));
        colStock.setCellValueFactory(new PropertyValueFactory<>("stock"));
        colCategory.setCellValueFactory(new PropertyValueFactory<>("category"));
        
        bookTable.getSelectionModel().selectedItemProperty().addListener((obs, old, selected) -> {
            if (selected != null) {
                selectedBook = selected;
                populateForm(selected);
            }
        });
    }
    
    private void setupCategoryBox() {
        categoryBox.getItems().addAll("Fiction", "Non-Fiction", "Science", "History", "Technology", "Biography", "Children");
    }
    
    private void loadBooks() {
        bookTable.setItems(FXCollections.observableArrayList(bookDAO.getAllBooks()));
    }
    
    private void populateForm(Book book) {
        titleField.setText(book.getTitle());
        authorField.setText(book.getAuthor());
        isbnField.setText(book.getIsbn());
        priceField.setText(String.valueOf(book.getPrice()));
        stockField.setText(String.valueOf(book.getStock()));
        categoryBox.setValue(book.getCategory());
        descriptionArea.setText(book.getDescription());
    }
    
    private void clearForm() {
        titleField.clear();
        authorField.clear();
        isbnField.clear();
        priceField.clear();
        stockField.clear();
        categoryBox.setValue(null);
        descriptionArea.clear();
        selectedBook = null;
        bookTable.getSelectionModel().clearSelection();
    }
    
    @FXML
    private void handleAddBook() {
        if (validateForm()) {
            Book book = new Book(0, titleField.getText(), authorField.getText(),
                isbnField.getText(), Double.parseDouble(priceField.getText()),
                Integer.parseInt(stockField.getText()), categoryBox.getValue(),
                descriptionArea.getText(), null);
            
            if (bookDAO.addBook(book)) {
                AlertUtils.showError("Failed to add book!");
            } else {
                AlertUtils.showInfo("Book added successfully!");
                loadBooks();
                clearForm();
            }
        }
    }
    
    @FXML
    private void handleUpdateBook() {
        if (selectedBook == null) {
            AlertUtils.showWarning("Please select a book to update!");
            return;
        }
        
        if (validateForm()) {
            selectedBook.setTitle(titleField.getText());
            selectedBook.setAuthor(authorField.getText());
            selectedBook.setIsbn(isbnField.getText());
            selectedBook.setPrice(Double.parseDouble(priceField.getText()));
            selectedBook.setStock(Integer.parseInt(stockField.getText()));
            selectedBook.setCategory(categoryBox.getValue());
            selectedBook.setDescription(descriptionArea.getText());
            
            if (bookDAO.updateBook(selectedBook)) {
                AlertUtils.showInfo("Book updated successfully!");
                loadBooks();
                clearForm();
            } else {
                AlertUtils.showError("Failed to update book!");
            }
        }
    }
    
    @FXML
    private void handleDeleteBook() {
        if (selectedBook == null) {
            AlertUtils.showWarning("Please select a book to delete!");
            return;
        }
        
        if (AlertUtils.showConfirmation("Delete " + selectedBook.getTitle() + "?")) {
            if (bookDAO.deleteBook(selectedBook.getId())) {
                AlertUtils.showInfo("Book deleted successfully!");
                loadBooks();
                clearForm();
            } else {
                AlertUtils.showError("Failed to delete book!");
            }
        }
    }
    
    @FXML
    private void handleClearForm() {
        clearForm();
    }
    
    @FXML
    private void handleLogout() {
        MainApp.changeScene("login.fxml", "eBookShop - Login");
    }
    
    private boolean validateForm() {
        if (titleField.getText().isEmpty()) {
            AlertUtils.showWarning("Please enter title!");
            return false;
        }
        if (authorField.getText().isEmpty()) {
            AlertUtils.showWarning("Please enter author!");
            return false;
        }
        try {
            Double.parseDouble(priceField.getText());
            Integer.parseInt(stockField.getText());
        } catch (NumberFormatException e) {
            AlertUtils.showWarning("Please enter valid price and stock!");
            return false;
        }
        if (categoryBox.getValue() == null) {
            AlertUtils.showWarning("Please select category!");
            return false;
        }
        return true;
    }
}