package com.mycompany.ebookshop.models;

import javafx.beans.property.*;

public class Book {
    private final IntegerProperty id = new SimpleIntegerProperty();
    private final StringProperty title = new SimpleStringProperty();
    private final StringProperty author = new SimpleStringProperty();
    private final StringProperty isbn = new SimpleStringProperty();
    private final DoubleProperty price = new SimpleDoubleProperty();
    private final IntegerProperty stock = new SimpleIntegerProperty();
    private final StringProperty category = new SimpleStringProperty();
    private final StringProperty description = new SimpleStringProperty();
    private final StringProperty imageUrl = new SimpleStringProperty();
    
    public Book() {}
    
    public Book(int id, String title, String author, String isbn, 
                double price, int stock, String category, String description, String imageUrl) {
        this.id.set(id);
        this.title.set(title);
        this.author.set(author);
        this.isbn.set(isbn);
        this.price.set(price);
        this.stock.set(stock);
        this.category.set(category);
        this.description.set(description);
        this.imageUrl.set(imageUrl);
    }
    
    // Getters
    public int getId() { return id.get(); }
    public String getTitle() { return title.get(); }
    public String getAuthor() { return author.get(); }
    public String getIsbn() { return isbn.get(); }
    public double getPrice() { return price.get(); }
    public int getStock() { return stock.get(); }
    public String getCategory() { return category.get(); }
    public String getDescription() { return description.get(); }
    public String getImageUrl() { return imageUrl.get(); }
    
    // Setters
    public void setId(int id) { this.id.set(id); }
    public void setTitle(String title) { this.title.set(title); }
    public void setAuthor(String author) { this.author.set(author); }
    public void setIsbn(String isbn) { this.isbn.set(isbn); }
    public void setPrice(double price) { this.price.set(price); }
    public void setStock(int stock) { this.stock.set(stock); }
    public void setCategory(String category) { this.category.set(category); }
    public void setDescription(String description) { this.description.set(description); }
    public void setImageUrl(String imageUrl) { this.imageUrl.set(imageUrl); }
    
    // Property getters for JavaFX binding
    public IntegerProperty idProperty() { return id; }
    public StringProperty titleProperty() { return title; }
    public StringProperty authorProperty() { return author; }
    public StringProperty isbnProperty() { return isbn; }
    public DoubleProperty priceProperty() { return price; }
    public IntegerProperty stockProperty() { return stock; }
    public StringProperty categoryProperty() { return category; }
    public StringProperty descriptionProperty() { return description; }
    public StringProperty imageUrlProperty() { return imageUrl; }
}