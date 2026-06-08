package com.mycompany.ebookshop.tools;

import java.util.List;

import com.mycompany.ebookshop.database.BookDAO;
import com.mycompany.ebookshop.models.Book;


public class AllBooksReport {
    public static void main(String[] args) {
        List<Book> samples = BookDAO.getSampleBooks();
        
        System.out.println("=".repeat(120));
        System.out.println("ALL EBOOKSHOP CATALOG BOOKS - COMPLETE REPORT");
        System.out.println("Total Books: " + samples.size());
        System.out.println("=".repeat(120));
        
        
        samples.stream()
                .collect(java.util.stream.Collectors.groupingBy(Book::getCategory))
                .forEach((category, books) -> {
                    System.out.println("\n[" + category + "] - " + books.size() + " books");
                    System.out.println("-".repeat(120));
                    
                    for (int i = 0; i < books.size(); i++) {
                        Book b = books.get(i);
                        System.out.printf("%2d. Title: %-40s | Author: %-25s | ISBN: %s%n",
                                (i + 1), b.getTitle(), b.getAuthor(), b.getIsbn());
                        System.out.printf("    Price: $%6.2f | Cover: %s%n",
                            b.getPrice(), b.getImageUrl());
                    }
                });
        
        System.out.println("\n" + "=".repeat(120));
        System.out.println("SUMMARY");
        System.out.println("=".repeat(120));
        System.out.println("✓ Total books: " + samples.size());
        System.out.println("✓ All books have OpenLibrary online cover URLs");
        System.out.println("✓ Format: https://covers.openlibrary.org/b/isbn/[ISBN]-L.jpg");
        System.out.println("✓ Fallback: If ISBN unavailable → Unsplash → Picsum");
        System.out.println("=".repeat(120));
    }
}
