package com.mycompany.ebookshop.controller;

import com.mycompany.ebookshop.MainApp;
import com.mycompany.ebookshop.models.Book;
import com.mycompany.ebookshop.models.CartItem;
import com.mycompany.ebookshop.utils.AlertUtils;
import com.mycompany.ebookshop.utils.SessionManager;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.util.converter.IntegerStringConverter;

public class CartController {
    
    @FXML private TableView<CartItem> cartTable;
    @FXML private TableColumn<CartItem, String> colTitle;
    @FXML private TableColumn<CartItem, String> colAuthor;
    @FXML private TableColumn<CartItem, Double> colPrice;
    @FXML private TableColumn<CartItem, Integer> colQuantity;
    @FXML private TableColumn<CartItem, Double> colTotal;
    @FXML private Label subtotalLabel;
    @FXML private Label taxLabel;
    @FXML private Label totalLabel;
    @FXML private Button checkoutButton;
    
    @FXML
    public void initialize() {
        setupTableColumns();
        loadCart();
        updateTotals();
    }
    
    private void setupTableColumns() {
        colTitle.setCellValueFactory(cellData -> cellData.getValue().getBook().titleProperty());
        colAuthor.setCellValueFactory(cellData -> cellData.getValue().getBook().authorProperty());
        colPrice.setCellValueFactory(cellData -> cellData.getValue().getBook().priceProperty().asObject());
        colQuantity.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        colTotal.setCellValueFactory(cellData -> 
            javafx.beans.binding.Bindings.createObjectBinding(
                () -> cellData.getValue().getTotal(),
                cellData.getValue().quantityProperty()
            )
        );
        
        // Make quantity editable - FIXED VERSION
        colQuantity.setCellFactory(TextFieldTableCell.forTableColumn(new IntegerStringConverter()));
        colQuantity.setOnEditCommit(event -> {
            CartItem item = event.getRowValue();
            int newQuantity = event.getNewValue();
            if (newQuantity > 0 && newQuantity <= item.getBook().getStock()) {
                item.setQuantity(newQuantity);
                SessionManager.getInstance().updateCart();
                updateTotals();
                cartTable.refresh();
            } else {
                AlertUtils.showWarning("Invalid quantity! Max: " + item.getBook().getStock());
            }
        });
        
        // Price formatting
        colPrice.setCellFactory(col -> new TableCell<CartItem, Double>() {
            @Override
            protected void updateItem(Double price, boolean empty) {
                super.updateItem(price, empty);
                setText(empty ? null : String.format("$%.2f", price));
            }
        });
        
        colTotal.setCellFactory(col -> new TableCell<CartItem, Double>() {
            @Override
            protected void updateItem(Double total, boolean empty) {
                super.updateItem(total, empty);
                setText(empty ? null : String.format("$%.2f", total));
            }
        });
    }
    
    private void loadCart() {
        cartTable.setItems(FXCollections.observableArrayList(SessionManager.getInstance().getCart()));
        cartTable.setEditable(true);
    }
    
    private void updateTotals() {
        double subtotal = SessionManager.getInstance().getCartTotal();
        double tax = subtotal * 0.08;
        double total = subtotal + tax;
        
        subtotalLabel.setText(String.format("$%.2f", subtotal));
        taxLabel.setText(String.format("$%.2f", tax));
        totalLabel.setText(String.format("$%.2f", total));
        
        checkoutButton.setDisable(SessionManager.getInstance().getCart().isEmpty());
    }
    
    @FXML
    private void handleRemoveItem() {
        CartItem selected = cartTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            SessionManager.getInstance().removeFromCart(selected);
            loadCart();
            updateTotals();
            AlertUtils.showInfo("Item removed from cart!");
        } else {
            AlertUtils.showWarning("Please select an item to remove!");
        }
    }
    
    @FXML
    private void handleClearCart() {
        if (AlertUtils.showConfirmation("Clear cart?")) {
            SessionManager.getInstance().clearCart();
            loadCart();
            updateTotals();
            AlertUtils.showInfo("Cart cleared!");
        }
    }
    
    @FXML
    private void handleCheckout() {
        if (SessionManager.getInstance().getCart().isEmpty()) {
            AlertUtils.showWarning("Your cart is empty!");
            return;
        }
        MainApp.changeScene("checkout.fxml", "eBookShop - Checkout");
    }
    
    @FXML
    private void handleContinueShopping() {
        MainApp.changeScene("main.fxml", "eBookShop - Book Store");
    }
}