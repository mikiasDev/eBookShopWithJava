package com.mycompany.ebookshop.models;

import javafx.beans.property.*;

public class CartItem {
    private final ObjectProperty<Book> book = new SimpleObjectProperty<>();
    private final IntegerProperty quantity = new SimpleIntegerProperty();
    
    public CartItem() {}
    
    public CartItem(Book book, int quantity) {
        this.book.set(book);
        this.quantity.set(quantity);
    }
    
    public Book getBook() { return book.get(); }
    public int getQuantity() { return quantity.get(); }
    public double getTotal() { return getBook().getPrice() * getQuantity(); }
    
    public void setBook(Book book) { this.book.set(book); }
    public void setQuantity(int quantity) { this.quantity.set(quantity); }
    
    public ObjectProperty<Book> bookProperty() { return book; }
    public IntegerProperty quantityProperty() { return quantity; }
}