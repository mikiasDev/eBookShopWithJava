package com.mycompany.ebookshop.tools;

import java.util.List;

import com.mycompany.ebookshop.database.BookDAO;
import com.mycompany.ebookshop.models.Book;


public class ResetCatalog {
    public static void main(String[] args) {
        if (args.length == 0) {
            printUsage();
            return;
        }

        String command = args[0].toLowerCase();
        BookDAO dao = new BookDAO();

        switch (command) {
            case "clear" -> doClear(dao);
            case "reseed" -> doReseed(dao);
            case "info" -> doInfo(dao);
            case "clean-duplicates" -> doCleanDuplicates(dao);
            default -> {
                System.err.println("Unknown command: " + command);
                printUsage();
            }
        }
    }

    private static void doClear(BookDAO dao) {
        System.out.println("Clearing catalog...");
        int deleted = dao.deleteAllBooks();
        System.out.println("✓ Deleted " + deleted + " books from catalog.");
    }

    private static void doReseed(BookDAO dao) {
        System.out.println("Re-seeding catalog...");
        
        
        int deleted = dao.deleteAllBooks();
        System.out.println("  Cleared " + deleted + " books.");

        
        List<Book> samples = BookDAO.getSampleBooks();
        System.out.println("  Inserting " + samples.size() + " sample books...");

        
        int inserted = 0;
        for (Book b : samples) {
            if (dao.addBook(b)) {
                inserted++;
            }
        }

        System.out.println("✓ Re-seeded " + inserted + " sample books into catalog.");
    }

    private static void doInfo(BookDAO dao) {
        System.out.println("Catalog Info:");
        System.out.println("  Sample books available: " + BookDAO.getSampleBooks().size());
        List<Book> all = dao.getAllBooks();
        System.out.println("  Books in database: " + (all != null ? all.size() : 0));
        
        if (all != null && !all.isEmpty()) {
            System.out.println("\n  First 5 books:");
            for (int i = 0; i < Math.min(5, all.size()); i++) {
                Book b = all.get(i);
                System.out.println("    " + (i + 1) + ". " + b.getTitle() + " (" + b.getIsbn() + ")");
            }
        }
    }

    private static void doCleanDuplicates(BookDAO dao) {
        System.out.println("Cleaning duplicate books...");
        int removed = dao.removeDuplicateBooks();
        System.out.println("✓ Removed " + removed + " duplicate book entries.");
    }

    private static void printUsage() {
        System.out.println("Usage: java ResetCatalog <command>");
        System.out.println("Commands:");
        System.out.println("  clear             - Delete all books from the database");
        System.out.println("  reseed            - Clear all books and re-insert the 80 sample books");
        System.out.println("  info              - Show catalog info and sample counts");
        System.out.println("  clean-duplicates  - Remove duplicate ISBN entries from the database");
    }
}
