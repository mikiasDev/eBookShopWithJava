package com.mycompany.ebookshop.database;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.mycompany.ebookshop.models.Book;
import com.mycompany.ebookshop.models.CartItem;
import com.mycompany.ebookshop.utils.CoverService;

@SuppressWarnings("all")
public class CartDAO {

    public List<CartItem> getCartItemsByUser(int userId) {
        List<CartItem> items = new ArrayList<>();
        String query = "SELECT ci.quantity, b.id, b.title, b.author, b.isbn, b.price, b.category, b.description, b.image_url "
            + "FROM cart_items ci JOIN books b ON ci.book_id = b.id WHERE ci.user_id = ? ORDER BY ci.updated_at DESC, ci.id DESC";

        Connection conn = DBConnection.getConnection();
        if (conn == null) {
            return items;
        }

        ensureCartTable(conn);

        try (PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, userId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    String title = rs.getString("title");
                    String author = rs.getString("author");
                    String isbn = rs.getString("isbn");
                    String imageUrl = rs.getString("image_url");

                        Book book = new Book(
                            rs.getInt("id"),
                            title,
                            author,
                            isbn,
                            rs.getDouble("price"),
                            rs.getString("category"),
                            rs.getString("description"),
                            imageUrl);
                    book.setImageUrl(CoverService.getCoverUrl(book));
                    items.add(new CartItem(book, rs.getInt("quantity")));
                }
            }
        } catch (SQLException e) {
            System.err.println("CartDAO getCartItemsByUser error: " + e.getMessage());
        }

        return items;
    }

    public boolean upsertCartItem(int userId, int bookId, int quantity) {
        if (quantity <= 0) {
            return removeCartItem(userId, bookId);
        }

        String query = "INSERT INTO cart_items (user_id, book_id, quantity) VALUES (?, ?, ?) "
                + "ON DUPLICATE KEY UPDATE quantity = VALUES(quantity), updated_at = CURRENT_TIMESTAMP";

        Connection conn = DBConnection.getConnection();
        if (conn == null) {
            return false;
        }

        ensureCartTable(conn);

        try (PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, userId);
            pstmt.setInt(2, bookId);
            pstmt.setInt(3, quantity);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("CartDAO upsertCartItem error: " + e.getMessage());
            return false;
        }
    }

    public boolean removeCartItem(int userId, int bookId) {
        String query = "DELETE FROM cart_items WHERE user_id = ? AND book_id = ?";

        Connection conn = DBConnection.getConnection();
        if (conn == null) {
            return false;
        }

        ensureCartTable(conn);

        try (PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, userId);
            pstmt.setInt(2, bookId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("CartDAO removeCartItem error: " + e.getMessage());
            return false;
        }
    }

    public boolean clearCart(int userId) {
        String query = "DELETE FROM cart_items WHERE user_id = ?";

        Connection conn = DBConnection.getConnection();
        if (conn == null) {
            return false;
        }

        ensureCartTable(conn);

        try (PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, userId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("CartDAO clearCart error: " + e.getMessage());
            return false;
        }
    }

    private void ensureCartTable(Connection conn) {
        try {
            DatabaseMetaData metaData = conn.getMetaData();
            try (ResultSet tables = metaData.getTables(null, null, "cart_items", null)) {
                if (tables.next()) {
                    return;
                }
            }

            String createTable = "CREATE TABLE IF NOT EXISTS cart_items ("
                    + "id INT AUTO_INCREMENT PRIMARY KEY, "
                    + "user_id INT NOT NULL, "
                    + "book_id INT NOT NULL, "
                    + "quantity INT NOT NULL DEFAULT 1, "
                    + "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, "
                    + "updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, "
                    + "UNIQUE KEY uq_cart_user_book (user_id, book_id), "
                    + "FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE, "
                    + "FOREIGN KEY (book_id) REFERENCES books(id) ON DELETE CASCADE"
                    + ")";

            try (PreparedStatement pstmt = conn.prepareStatement(createTable)) {
                pstmt.executeUpdate();
            }
        } catch (SQLException e) {
            System.err.println("CartDAO ensureCartTable error: " + e.getMessage());
        }
    }
}