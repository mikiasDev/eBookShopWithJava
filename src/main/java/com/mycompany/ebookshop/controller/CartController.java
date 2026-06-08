package com.mycompany.ebookshop.controller;

import com.mycompany.ebookshop.MainApp;
import com.mycompany.ebookshop.models.CartItem;
import com.mycompany.ebookshop.utils.AlertUtils;
import com.mycompany.ebookshop.utils.SessionManager;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.util.converter.IntegerStringConverter;

public class CartController {
    
    @FXML private TableView<CartItem> cartTable;
    @FXML private TableColumn<CartItem, String> colTitle;
    @FXML private TableColumn<CartItem, String> colImage;
    @FXML private TableColumn<CartItem, String> colAuthor;
    @FXML private TableColumn<CartItem, Double> colPrice;
    @FXML private TableColumn<CartItem, Integer> colQuantity;
    @FXML private TableColumn<CartItem, Double> colTotal;
    @FXML private Label subtotalLabel;
    @FXML private Label taxLabel;
    @FXML private Label totalLabel;
    @FXML private Label cartItemCountLabel;
    @FXML private Button checkoutButton;
    
    @FXML
    public void initialize() {
        setupTableColumns();
        cartTable.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        loadCart();
        updateTotals();
    }
    
    @FXML
    private void handleBack() {
        MainApp.changeScene("main.fxml", "eBookShop - Book Store");
    }
    
    private void setupTableColumns() {
        
        colImage.setCellValueFactory(cellData -> cellData.getValue().getBook().imageUrlProperty());
        colImage.setCellFactory(col -> new TableCell<CartItem, String>() {
            private final ImageView iv = new ImageView();
            {
                iv.setFitWidth(88);
                iv.setFitHeight(88);
                iv.setPreserveRatio(true);
                iv.setSmooth(true);
            }
            @Override
            protected void updateItem(String path, boolean empty) {
                super.updateItem(path, empty);
                if (empty || path == null) {
                    setGraphic(null);
                } else {
                    Image img = null;
                    try {
                        java.net.URL url = getClass().getResource("/com/mycompany/ebookshop/" + path);
                        if (url != null) img = new Image(url.toExternalForm(), true);
                        else img = new Image(path, true);
                    } catch (Exception ex) { img = null; }
                    if (img == null || img.isError()) {
                        CartItem rowItem = getTableRow() != null ? getTableRow().getItem() : null;
                        String isbn = rowItem != null && rowItem.getBook() != null ? rowItem.getBook().getIsbn() : null;
                        String normalizedIsbn = isbn == null ? "" : isbn.trim().replaceAll("[^0-9Xx]", "");
                        String fallbackUrl = normalizedIsbn.isBlank()
                            ? "https://covers.openlibrary.org/b/isbn/9780134685991-L.jpg"
                            : "https://covers.openlibrary.org/b/isbn/" + normalizedIsbn + "-L.jpg";
                        img = new Image(fallbackUrl, true);
                    }
                    iv.setImage(img);
                    setGraphic(iv);
                }
            }
        });

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
        
        
        colQuantity.setCellFactory(TextFieldTableCell.forTableColumn(new IntegerStringConverter()));
        colQuantity.setOnEditCommit(event -> {
            CartItem item = event.getRowValue();
            int newQuantity = event.getNewValue();
            if (newQuantity > 0) {
                item.setQuantity(newQuantity);
                SessionManager.getInstance().updateCart();
                updateTotals();
                cartTable.refresh();
            } else {
                AlertUtils.showWarning("Invalid quantity! Must be at least 1.");
            }
        });
        
        
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
        updateCartCountLabel();
        updateCheckoutButtonState();
    }
    
    private void updateTotals() {
        double subtotal = SessionManager.getInstance().getCartTotal();
        double tax = subtotal * 0.08;
        double total = subtotal + tax;
        
        subtotalLabel.setText(String.format("$%.2f", subtotal));
        taxLabel.setText(String.format("$%.2f", tax));
        totalLabel.setText(String.format("$%.2f", total));
        
        updateCheckoutButtonState();
        updateCartCountLabel();
    }

    private void updateCheckoutButtonState() {
        if (checkoutButton != null) {
            checkoutButton.setDisable(SessionManager.getInstance().getCart().isEmpty());
        }
    }

    private void updateCartCountLabel() {
        if (cartItemCountLabel == null) {
            return;
        }

        int itemCount = SessionManager.getInstance().getCart().stream()
                .mapToInt(cartItem -> Math.max(1, cartItem.getQuantity()))
                .sum();
        cartItemCountLabel.setText(itemCount == 1 ? "(1 item)" : "(" + itemCount + " items)");
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
        SessionManager.getInstance().setCheckoutItems(SessionManager.getInstance().getCart());
        MainApp.changeScene("checkout.fxml", "eBookShop - Checkout");
    }

    @FXML
    private void handleCheckoutSelected() {
        ObservableList<CartItem> selectedItems = cartTable.getSelectionModel().getSelectedItems();
        if (selectedItems == null || selectedItems.isEmpty()) {
            AlertUtils.showWarning("Select one or more cart items to pay for.");
            return;
        }

        SessionManager.getInstance().setCheckoutItems(selectedItems);
        MainApp.changeScene("checkout.fxml", "eBookShop - Checkout");
    }
    
    @FXML
    private void handleContinueShopping() {
        MainApp.changeScene("main.fxml", "eBookShop - Book Store");
    }
}