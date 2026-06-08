package com.mycompany.ebookshop.tools;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;

import com.mycompany.ebookshop.database.BookDAO;
import com.mycompany.ebookshop.database.DBConnection;
import com.mycompany.ebookshop.models.Book;

public class PrintBooks {
    public static void main(String[] args) {
        
        try {
            List<Book> samples = BookDAO.getSampleBooks();
            if (samples != null && !samples.isEmpty()) {
                System.out.println("Using in-memory sample books (up to 10):");
                int limit = Math.min(10, samples.size());
                for (int i = 0; i < limit; i++) {
                    Book b = samples.get(i);
                    System.out.println((i + 1) + " | " + b.getTitle() + " | " + (b.getImageUrl() == null ? "(null)" : b.getImageUrl()) + " | " + (b.getIsbn() == null ? "(null)" : b.getIsbn()));
                }
                return;
            }
        } catch (Throwable t) {
            
        }

        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT id, title, image_url, isbn FROM books LIMIT 10");
             ResultSet rs = ps.executeQuery()) {
            System.out.println("id | title | image_url | isbn");
            while (rs.next()) {
                int id = rs.getInt("id");
                String title = rs.getString("title");
                String url = rs.getString("image_url");
                String isbn = rs.getString("isbn");
                System.out.println(id + " | " + title + " | " + (url == null ? "(null)" : url) + " | " + (isbn == null ? "(null)" : isbn));
            }
        } catch (Exception e) {
            System.err.println("Error querying books: ");
            e.printStackTrace();
            System.exit(2);
        }
    }
}
