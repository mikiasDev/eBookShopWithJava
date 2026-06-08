package com.mycompany.ebookshop.models;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class Book {
    private final IntegerProperty id = new SimpleIntegerProperty();
    private final StringProperty title = new SimpleStringProperty();
    private final StringProperty author = new SimpleStringProperty();
    private final StringProperty isbn = new SimpleStringProperty();
    private final DoubleProperty price = new SimpleDoubleProperty();
    private final StringProperty category = new SimpleStringProperty();
    private final StringProperty description = new SimpleStringProperty();
    private final StringProperty imageUrl = new SimpleStringProperty();
    private final StringProperty filePath = new SimpleStringProperty();
    
    public Book() {}
    
    public Book(int id, String title, String author, String isbn, 
                double price, String category, String description, String imageUrl) {
        this.id.set(id);
        this.title.set(title);
        this.author.set(author);
        this.isbn.set(isbn);
        this.price.set(price);
        this.category.set(category);
        this.description.set(description);
        this.imageUrl.set(imageUrl);
        this.filePath.set(null);
    }
    
    public Book(int id, String title, String author, String isbn, 
                double price, String category, String description, String imageUrl, String filePath) {
        this.id.set(id);
        this.title.set(title);
        this.author.set(author);
        this.isbn.set(isbn);
        this.price.set(price);
        this.category.set(category);
        this.description.set(description);
        this.imageUrl.set(imageUrl);
        this.filePath.set(filePath);
    }
    
    
    public int getId() { return id.get(); }
    public String getTitle() { return title.get(); }
    public String getAuthor() { return author.get(); }
    public String getIsbn() { return isbn.get(); }
    public double getPrice() { return price.get(); }
    public String getCategory() { return category.get(); }
    public String getDescription() { return description.get(); }
    public String getImageUrl() { return imageUrl.get(); }
    public String getFilePath() { return filePath.get(); }
    
    
    public void setId(int id) { this.id.set(id); }
    public void setTitle(String title) { this.title.set(title); }
    public void setAuthor(String author) { this.author.set(author); }
    public void setIsbn(String isbn) { this.isbn.set(isbn); }
    public void setPrice(double price) { this.price.set(price); }
    public void setCategory(String category) { this.category.set(category); }
    public void setDescription(String description) { this.description.set(description); }
    public void setImageUrl(String imageUrl) { this.imageUrl.set(imageUrl); }
    public void setFilePath(String filePath) { this.filePath.set(filePath); }
    
    
    public IntegerProperty idProperty() { return id; }
    public StringProperty titleProperty() { return title; }
    public StringProperty authorProperty() { return author; }
    public StringProperty isbnProperty() { return isbn; }
    public DoubleProperty priceProperty() { return price; }
    public StringProperty categoryProperty() { return category; }
    public StringProperty descriptionProperty() { return description; }
    public StringProperty imageUrlProperty() { return imageUrl; }
    public StringProperty filePathProperty() { return filePath; }
}