package com.mycompany.ebookshop.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.DatabaseMetaData;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.mycompany.ebookshop.models.Order;
import com.mycompany.ebookshop.models.OrderItem;

@SuppressWarnings("all")
public class OrderDAO {
    
    public int createOrder(Order order) {
        String query = "INSERT INTO orders (user_id, order_date, total_amount, status, shipping_address, payment_method) VALUES (?, ?, ?, ?, ?, ?)";
        
        Connection conn = DBConnection.getConnection();
        if (conn == null) return -1;

        try (PreparedStatement pstmt = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setInt(1, order.getUserId());
            pstmt.setTimestamp(2, java.sql.Timestamp.valueOf(order.getOrderDate() != null ? order.getOrderDate() : java.time.LocalDateTime.now()));
            pstmt.setDouble(3, order.getTotalAmount());
            pstmt.setString(4, order.getStatus());
            pstmt.setString(5, order.getShippingAddress());
            pstmt.setString(6, order.getPaymentMethod());
            pstmt.executeUpdate();

            try (ResultSet rs = pstmt.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            System.err.println("OrderDAO createOrder error: " + e.getMessage());
        }
        return -1;
    }
    
    public boolean addOrderItem(OrderItem item) {
        Connection conn = DBConnection.getConnection();
        if (conn == null) return false;

        boolean hasBookTitleColumn = hasColumn(conn, "order_items", "book_title");
        String query = hasBookTitleColumn
                ? "INSERT INTO order_items (order_id, book_id, book_title, quantity, price) VALUES (?, ?, ?, ?, ?)"
                : "INSERT INTO order_items (order_id, book_id, quantity, price) VALUES (?, ?, ?, ?)";

        try (PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, item.getOrderId());
            pstmt.setInt(2, item.getBookId());
            if (hasBookTitleColumn) {
                pstmt.setString(3, item.getBookTitle());
                pstmt.setInt(4, item.getQuantity());
                pstmt.setDouble(5, item.getPrice());
            } else {
                pstmt.setInt(3, item.getQuantity());
                pstmt.setDouble(4, item.getPrice());
            }
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("OrderDAO addOrderItem error: " + e.getMessage());
            return false;
        }
    }

    public Set<Integer> findPreviouslyPurchasedBookIds(int userId, List<Integer> bookIds) {
        if (bookIds == null || bookIds.isEmpty()) {
            return Collections.emptySet();
        }

        List<Integer> validIds = bookIds.stream().filter(id -> id != null && id > 0).distinct().toList();
        if (validIds.isEmpty()) {
            return Collections.emptySet();
        }

        String placeholders = String.join(",", java.util.Collections.nCopies(validIds.size(), "?"));
        String query = "SELECT DISTINCT oi.book_id "
                + "FROM order_items oi "
                + "JOIN orders o ON o.id = oi.order_id "
                + "WHERE o.user_id = ? AND oi.book_id IN (" + placeholders + ")";

        Connection conn = DBConnection.getConnection();
        if (conn == null) {
            return Collections.emptySet();
        }

        Set<Integer> purchasedIds = new LinkedHashSet<>();
        try (PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, userId);
            for (int i = 0; i < validIds.size(); i++) {
                pstmt.setInt(i + 2, validIds.get(i));
            }
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    purchasedIds.add(rs.getInt(1));
                }
            }
        } catch (SQLException e) {
            System.err.println("OrderDAO findPreviouslyPurchasedBookIds error: " + e.getMessage());
        }

        return purchasedIds;
    }
    
    public List<Order> getOrdersByUser(int userId) {
        List<Order> orders = new ArrayList<>();
        String query = "SELECT * FROM orders WHERE user_id = ? ORDER BY created_at DESC";
        
        Connection conn = DBConnection.getConnection();
        if (conn == null) return orders;

        try (PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, userId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    java.time.LocalDateTime createdAt = null;
                    try {
                        var ts = rs.getTimestamp("created_at");
                        if (ts != null) createdAt = ts.toLocalDateTime();
                    } catch (SQLException ignore) {}
                    Order order = new Order(
                        rs.getInt("id"),
                        rs.getInt("user_id"),
                        createdAt,
                        rs.getDouble("total_amount"),
                        rs.getString("status")
                    );
                    try {
                        String rpath = rs.getString("receipt_path");
                        if (rpath != null && !rpath.isBlank()) {
                            order.setReceiptPath(rpath);
                        }
                    } catch (SQLException ignore) {}
                    orders.add(order);
                }
            }
        } catch (SQLException e) {
            System.err.println("OrderDAO getOrdersByUser error: " + e.getMessage());
        }
        return orders;
    }

    public boolean updateReceiptPath(int orderId, String receiptPath) {
        String query = "UPDATE orders SET receipt_path = ? WHERE id = ?";
        Connection conn = DBConnection.getConnection();
        if (conn == null) return false;
        try (PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, receiptPath);
            pstmt.setInt(2, orderId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("OrderDAO updateReceiptPath error: " + e.getMessage());
            return false;
        }
    }

    public List<OrderItem> getOrderItemsByOrder(int orderId) {
        List<OrderItem> items = new ArrayList<>();
        Connection conn = DBConnection.getConnection();
        if (conn == null) return items;
        boolean hasBookTitleColumn = hasColumn(conn, "order_items", "book_title");
        String query = hasBookTitleColumn
                ? "SELECT id, order_id, book_id, book_title, quantity, price FROM order_items WHERE order_id = ?"
                : "SELECT id, order_id, book_id, quantity, price FROM order_items WHERE order_id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, orderId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    OrderItem it = new OrderItem(
                        rs.getInt("id"),
                        rs.getInt("order_id"),
                        rs.getInt("book_id"),
                        hasBookTitleColumn ? rs.getString("book_title") : null,
                        rs.getInt("quantity"),
                        rs.getDouble("price")
                    );
                    items.add(it);
                }
            }
        } catch (SQLException e) {
            System.err.println("OrderDAO getOrderItemsByOrder error: " + e.getMessage());
        }
        return items;
    }

    private boolean hasColumn(Connection conn, String tableName, String columnName) {
        try {
            DatabaseMetaData metaData = conn.getMetaData();
            try (ResultSet columns = metaData.getColumns(null, null, tableName, columnName)) {
                return columns.next();
            }
        } catch (SQLException e) {
            return false;
        }
    }
}