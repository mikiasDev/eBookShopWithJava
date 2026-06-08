package com.mycompany.ebookshop.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.mycompany.ebookshop.models.Book;
import com.mycompany.ebookshop.utils.CoverService;

@SuppressWarnings("all")
public class BookDAO {

    private static final Object[][] DEFAULT_BOOKS = new Object[][] {
        
        book("Effective Java", "Joshua Bloch", "9780134685991", 54.99, 18, "Technology", "A practical guide to writing robust Java code.", "https://m.media-amazon.com/images/P/0134685997.01._SX303_BO1,204,203,200_.jpg"),
        book("Clean Code", "Robert C. Martin", "9780132350884", 49.99, 22, "Technology", "Principles and practices for readable code.", "https://m.media-amazon.com/images/P/0132350882.01._SX303_BO1,204,203,200_.jpg"),
        book("Design Patterns", "Erich Gamma", "9780201633610", 62.00, 9, "Technology", "The classic catalog of object-oriented patterns.", "https://m.media-amazon.com/images/P/0201633612.01._SX303_BO1,204,203,200_.jpg"),
        book("The Pragmatic Programmer", "Andrew Hunt", "9780201616224", 52.25, 14, "Technology", "Timeless advice for software craftsmanship.", "https://m.media-amazon.com/images/P/0201616224.01._SX303_BO1,204,203,200_.jpg"),
        book("Refactoring", "Martin Fowler", "9780134757599", 57.00, 11, "Technology", "Improving the design of existing code.", "https://m.media-amazon.com/images/P/0134757599.01._SX303_BO1,204,203,200_.jpg"),
        book("Code Complete", "Steve McConnell", "9780735619678", 59.95, 13, "Technology", "A practical handbook of software construction.", "https://m.media-amazon.com/images/P/0735619670.01._SX303_BO1,204,203,200_.jpg"),
        book("The Mythical Man-Month", "Frederick P. Brooks Jr.", "9780201835953", 43.20, 17, "Technology", "Essays on software engineering and project management.", "https://m.media-amazon.com/images/P/0201835959.01._SX303_BO1,204,203,200_.jpg"),
        book("Learning Python", "Mark Lutz", "9780596158064", 67.80, 8, "Technology", "A comprehensive guide to Python programming.", "https://m.media-amazon.com/images/P/0596158068.01._SX303_BO1,204,203,200_.jpg"),
        book("Java Concurrency in Practice", "Brian Goetz", "9780321349606", 51.40, 12, "Technology", "Principles and patterns for multithreading.", "https://m.media-amazon.com/images/P/0321349601.01._SX303_BO1,204,203,200_.jpg"),
        book("Spring in Action", "Craig Walls", "9781617299025", 48.75, 10, "Technology", "Build modern web applications with Spring.", "https://m.media-amazon.com/images/P/1617299022.01._SX303_BO1,204,203,200_.jpg"),

        
        book("Good to Great", "Jim Collins", "9780066620992", 36.99, 20, "Business", "Why some companies make the leap and others don't.", "https://m.media-amazon.com/images/P/0066620996.01._SX303_BO1,204,203,200_.jpg"),
        book("The Lean Startup", "Eric Ries", "9780307887894", 34.50, 19, "Business", "How modern entrepreneurs build successful businesses.", "https://m.media-amazon.com/images/P/0307887898.01._SX303_BO1,204,203,200_.jpg"),
        book("Thinking, Fast and Slow", "Daniel Kahneman", "9780374533557", 39.95, 15, "Business", "Insights into the two systems of human thought.", "https://m.media-amazon.com/images/P/0374533555.01._SX303_BO1,204,203,200_.jpg"),
        book("Influence", "Robert B. Cialdini", "9780061241895", 38.25, 18, "Business", "The psychology of persuasion and compliance.", "https://m.media-amazon.com/images/P/0061241896.01._SX303_BO1,204,203,200_.jpg"),
        book("Start with Why", "Simon Sinek", "9781591846444", 29.95, 21, "Business", "How great leaders inspire everyone to take action.", "https://m.media-amazon.com/images/P/1591846447.01._SX303_BO1,204,203,200_.jpg"),
        book("Blue Ocean Strategy", "W. Chan Kim", "9781591396192", 42.75, 12, "Business", "Create uncontested market space and the competition.", "https://m.media-amazon.com/images/P/1591396190.01._SX303_BO1,204,203,200_.jpg"),
        book("Zero to One", "Peter Thiel", "9780804139298", 35.40, 13, "Business", "Notes on startups, or how to build the future.", "https://m.media-amazon.com/images/P/0804139296.01._SX303_BO1,204,203,200_.jpg"),
        book("The E-Myth Revisited", "Michael E. Gerber", "9780887307287", 31.80, 14, "Business", "Why most small businesses fail and what to do.", "https://m.media-amazon.com/images/P/0887307280.01._SX303_BO1,204,203,200_.jpg"),
        book("Never Split the Difference", "Chris Voss", "9780062407801", 33.60, 16, "Business", "Negotiation secrets from FBI hostage negotiator.", "https://m.media-amazon.com/images/P/0062407805.01._SX303_BO1,204,203,200_.jpg"),
        book("The Hard Thing About Hard Things", "Ben Horowitz", "9780062273208", 41.10, 10, "Business", "Building a business when there are no easy answers.", "https://m.media-amazon.com/images/P/0062273201.01._SX303_BO1,204,203,200_.jpg"),

        
        book("Sapiens", "Yuval Noah Harari", "9780062316097", 37.95, 19, "Science", "A brief history of humankind and civilization.", "https://m.media-amazon.com/images/P/0062316095.01._SX303_BO1,204,203,200_.jpg"),
        book("Cosmos", "Carl Sagan", "9780345539434", 30.50, 20, "Science", "A journey through space, time, and civilization.", "https://m.media-amazon.com/images/P/0345539435.01._SX303_BO1,204,203,200_.jpg"),
        book("A Brief History of Time", "Stephen Hawking", "9780553380163", 28.99, 18, "Science", "From the Big Bang to Black Holes.", "https://m.media-amazon.com/images/P/0553380168.01._SX303_BO1,204,203,200_.jpg"),
        book("The Selfish Gene", "Richard Dawkins", "9780199291151", 27.75, 12, "Science", "A gene-centered view of natural selection.", "https://m.media-amazon.com/images/P/0199291155.01._SX303_BO1,204,203,200_.jpg"),
        book("Why We Sleep", "Matthew Walker", "9781501144325", 31.25, 15, "Science", "Unlocking the power of sleep and dreams.", "https://m.media-amazon.com/images/P/1501144324.01._SX303_BO1,204,203,200_.jpg"),
        book("The Elegant Universe", "Brian Greene", "9780393338102", 32.10, 14, "Science", "String theory and the nature of reality.", "https://m.media-amazon.com/images/P/0393338105.01._SX303_BO1,204,203,200_.jpg"),
        book("Astrophysics for People in a Hurry", "Neil deGrasse Tyson", "9780393609394", 24.60, 17, "Science", "A quick tour of our place in the universe.", "https://m.media-amazon.com/images/P/0393609391.01._SX303_BO1,204,203,200_.jpg"),
        book("The Gene", "Siddhartha Mukherjee", "9781476733524", 40.00, 11, "Science", "An intimate history of heredity and humanity.", "https://m.media-amazon.com/images/P/1476733520.01._SX303_BO1,204,203,200_.jpg"),
        book("The Body", "Bill Bryson", "9780385539302", 29.40, 9, "Science", "A guide to what makes us human.", "https://m.media-amazon.com/images/P/0385539304.01._SX303_BO1,204,203,200_.jpg"),
        book("The Martian", "Andy Weir", "9780553418026", 26.90, 10, "Science", "Survival on Mars through science and ingenuity.", "https://m.media-amazon.com/images/P/0553418025.01._SX303_BO1,204,203,200_.jpg"),

        
        book("A People's History of the United States", "Howard Zinn", "9780062397348", 36.15, 14, "History", "History from the perspective of ordinary people.", "https://m.media-amazon.com/images/P/0062397346.01._SX303_BO1,204,203,200_.jpg"),
        book("The Rise and Fall of the Third Reich", "William L. Shirer", "9781451651683", 45.00, 12, "History", "A history of Nazi Germany and World War II.", "https://m.media-amazon.com/images/P/1451651686.01._SX303_BO1,204,203,200_.jpg"),
        book("Guns, Germs, and Steel", "Jared Diamond", "9780393317558", 34.80, 18, "History", "Why some civilizations conquered the world.", "https://m.media-amazon.com/images/P/0393317552.01._SX303_BO1,204,203,200_.jpg"),
        book("The Silk Roads", "Peter Frankopan", "9781101912379", 38.20, 13, "History", "A new history of the world through trade routes.", "https://m.media-amazon.com/images/P/1101912370.01._SX303_BO1,204,203,200_.jpg"),
        book("1776", "David McCullough", "9780743226721", 31.20, 11, "History", "The story of America's fight for independence.", "https://m.media-amazon.com/images/P/0743226720.01._SX303_BO1,204,203,200_.jpg"),
        book("The Wright Brothers", "David McCullough", "9781476728759", 29.85, 20, "History", "How Wilbur and Orville Wright invented the airplane.", "https://m.media-amazon.com/images/P/1476728755.01._SX303_BO1,204,203,200_.jpg"),
        book("SPQR", "Mary Beard", "9780871404237", 33.95, 15, "History", "A history of ancient Rome and Roman civilization.", "https://m.media-amazon.com/images/P/0871404230.01._SX303_BO1,204,203,200_.jpg"),
        book("The Diary of a Young Girl", "Anne Frank", "9780553296983", 22.50, 16, "History", "The diary of Anne Frank during Nazi occupation.", "https://m.media-amazon.com/images/P/0553296981.01._SX303_BO1,204,203,200_.jpg"),
        book("The Crusades", "Thomas Asbridge", "9781442606234", 37.60, 9, "History", "The war for the Holy Land and the clash of faiths.", "https://m.media-amazon.com/images/P/1442606230.01._SX303_BO1,204,203,200_.jpg"),
        book("The Cold War", "John Lewis Gaddis", "9780143119953", 28.70, 10, "History", "A new history of the Cold War and its legacy.", "https://m.media-amazon.com/images/P/0143119958.01._SX303_BO1,204,203,200_.jpg"),

        
        book("The Great Gatsby", "F. Scott Fitzgerald", "9780743273565", 18.99, 25, "Fiction", "A classic tale of wealth, love, and the American Dream.", "https://m.media-amazon.com/images/P/0743273567.01._SX303_BO1,204,203,200_.jpg"),
        book("To Kill a Mockingbird", "Harper Lee", "9780061120084", 19.75, 24, "Fiction", "A powerful novel about justice and human dignity.", "https://m.media-amazon.com/images/P/0061120081.01._SX303_BO1,204,203,200_.jpg"),
        book("1984", "George Orwell", "9780451524935", 17.50, 23, "Fiction", "A dystopian masterpiece of surveillance and control.", "https://m.media-amazon.com/images/P/0451524934.01._SX303_BO1,204,203,200_.jpg"),
        book("Brave New World", "Aldous Huxley", "9780060850524", 16.95, 20, "Fiction", "A future society engineered for happiness.", "https://m.media-amazon.com/images/P/0060850523.01._SX303_BO1,204,203,200_.jpg"),
        book("The Catcher in the Rye", "J.D. Salinger", "9780316769488", 21.10, 14, "Fiction", "The journey of a troubled teenage boy in New York.", "https://m.media-amazon.com/images/P/0316769487.01._SX303_BO1,204,203,200_.jpg"),
        book("The Kite Runner", "Khaled Hosseini", "9781594631931", 24.25, 18, "Fiction", "Redemption and friendship in Afghanistan.", "https://m.media-amazon.com/images/P/1594631930.01._SX303_BO1,204,203,200_.jpg"),
        book("The Book Thief", "Markus Zusak", "9780375842207", 22.40, 17, "Fiction", "A girl who steals books in Nazi Germany.", "https://m.media-amazon.com/images/P/0375842209.01._SX303_BO1,204,203,200_.jpg"),
        book("The Alchemist", "Paulo Coelho", "9780061122415", 20.00, 21, "Fiction", "A philosophical fable about following your dreams.", "https://m.media-amazon.com/images/P/0061122416.01._SX303_BO1,204,203,200_.jpg"),
        book("Life of Pi", "Yann Martel", "9780156027328", 23.15, 13, "Fiction", "An epic journey of survival and spirituality.", "https://m.media-amazon.com/images/P/0156027321.01._SX303_BO1,204,203,200_.jpg"),
        book("Little Women", "Louisa May Alcott", "9780142408766", 19.20, 15, "Fiction", "The classic story of the March sisters.", "https://m.media-amazon.com/images/P/0142408762.01._SX303_BO1,204,203,200_.jpg"),

        
        book("Charlotte's Web", "E. B. White", "9780064400558", 12.99, 30, "Children", "A pig's friendship with a spider named Charlotte.", "https://m.media-amazon.com/images/P/0064400557.01._SX303_BO1,204,203,200_.jpg"),
        book("The Very Hungry Caterpillar", "Eric Carle", "9780399226908", 11.25, 35, "Children", "A tiny caterpillar transforms into a butterfly.", "https://m.media-amazon.com/images/P/0399226907.01._SX303_BO1,204,203,200_.jpg"),
        book("Where the Wild Things Are", "Maurice Sendak", "9780060254926", 12.10, 32, "Children", "A boy's imaginative adventure with wild creatures.", "https://m.media-amazon.com/images/P/0060254920.01._SX303_BO1,204,203,200_.jpg"),
        book("Goodnight Moon", "Margaret Wise Brown", "9780064430173", 10.95, 34, "Children", "A soothing bedtime story for young children.", "https://m.media-amazon.com/images/P/0064430170.01._SX303_BO1,204,203,200_.jpg"),
        book("The Cat in the Hat", "Dr. Seuss", "9780394800011", 11.80, 31, "Children", "A mischievous cat's wild day with children.", "https://m.media-amazon.com/images/P/0394800028.01._SX303_BO1,204,203,200_.jpg"),
        book("Harry Potter and the Sorcerer's Stone", "J. K. Rowling", "9780590353427", 24.99, 26, "Children", "The first magical adventure at Hogwarts School.", "https://m.media-amazon.com/images/P/0590353427.01._SX303_BO1,204,203,200_.jpg"),
        book("Charlie and the Chocolate Factory", "Roald Dahl", "9780142410318", 13.95, 27, "Children", "A magical tour through Wonka's fantastic factory.", "https://m.media-amazon.com/images/P/0142410314.01._SX303_BO1,204,203,200_.jpg"),
        book("Matilda", "Roald Dahl", "9780142410370", 13.50, 28, "Children", "A brilliant girl discovers her extraordinary powers.", "https://m.media-amazon.com/images/P/0142410373.01._SX303_BO1,204,203,200_.jpg"),
        book("The Gruffalo", "Julia Donaldson", "9780142403877", 11.40, 33, "Children", "A clever mouse outwits a scary creature.", "https://m.media-amazon.com/images/P/0142403873.01._SX303_BO1,204,203,200_.jpg"),
        book("Brown Bear, Brown Bear, What Do You See?", "Bill Martin Jr.", "9780805017441", 10.50, 36, "Children", "A colorful and rhythmic picture book.", "https://m.media-amazon.com/images/P/0805017445.01._SX303_BO1,204,203,200_.jpg"),

        
        book("Things Fall Apart", "Chinua Achebe", "9780385474542", 21.99, 19, "African Literature", "A powerful story of colonial encounter in Nigeria.", "https://m.media-amazon.com/images/P/0385474547.01._SX303_BO1,204,203,200_.jpg"),
        book("Half of a Yellow Sun", "Chimamanda Ngozi Adichie", "9780307271075", 23.50, 18, "African Literature", "Love and war during the Biafran conflict.", "https://m.media-amazon.com/images/P/0307271072.01._SX303_BO1,204,203,200_.jpg"),
        book("Americanah", "Chimamanda Ngozi Adichie", "9780307455925", 25.25, 13, "African Literature", "Race, identity, and love across continents.", "https://m.media-amazon.com/images/P/0307455920.01._SX303_BO1,204,203,200_.jpg"),
        book("Homegoing", "Yaa Gyasi", "9781101971062", 24.95, 17, "African Literature", "Two sisters' lives separated by circumstance.", "https://m.media-amazon.com/images/P/1101971061.01._SX303_BO1,204,203,200_.jpg"),
        book("Weep Not, Child", "Ngugi wa Thiong'o", "9780141187027", 18.75, 16, "African Literature", "A boy's coming of age in colonial Kenya.", "https://m.media-amazon.com/images/P/0141187025.01._SX303_BO1,204,203,200_.jpg"),
        book("The Joys of Motherhood", "Buchi Emecheta", "9780435905705", 19.35, 15, "African Literature", "A woman's struggle in changing Nigeria.", "https://m.media-amazon.com/images/P/0435905708.01._SX303_BO1,204,203,200_.jpg"),
        book("Season of Migration to the North", "Tayeb Salih", "9780946390830", 20.40, 14, "African Literature", "A mysterious stranger in a Sudanese village.", "https://m.media-amazon.com/images/P/0946390835.01._SX303_BO1,204,203,200_.jpg"),
        book("Nervous Conditions", "Tsitsi Dangarembga", "9780935312994", 22.10, 12, "African Literature", "A girl's struggle in colonial Zimbabwe.", "https://m.media-amazon.com/images/P/0935312996.01._SX303_BO1,204,203,200_.jpg"),
        book("Black Skin, White Masks", "Frantz Fanon", "9780802196101", 24.80, 11, "African Literature", "Colonialism and the black psyche analyzed.", "https://m.media-amazon.com/images/P/0802196101.01._SX303_BO1,204,203,200_.jpg"),
        book("So Long a Letter", "Mariama Ba", "9780435905514", 18.20, 14, "African Literature", "A letter to a friend about marriage and change.", "https://m.media-amazon.com/images/P/0435905511.01._SX303_BO1,204,203,200_.jpg"),

        
        book("War and Peace", "Leo Tolstoy", "9780199232765", 29.99, 16, "World Literature", "Epic tale of love, war, and Russian history.", "https://m.media-amazon.com/images/P/0199232768.01._SX303_BO1,204,203,200_.jpg"),
        book("The Brothers Karamazov", "Fyodor Dostoevsky", "9780374528379", 28.45, 14, "World Literature", "Faith, doubt, and passion in Czarist Russia.", "https://m.media-amazon.com/images/P/0374528373.01._SX303_BO1,204,203,200_.jpg"),
        book("Crime and Punishment", "Fyodor Dostoevsky", "9780140449136", 27.30, 15, "World Literature", "A psychological exploration of guilt and redemption.", "https://m.media-amazon.com/images/P/0140449132.01._SX303_BO1,204,203,200_.jpg"),
        book("One Hundred Years of Solitude", "Gabriel Garcia Marquez", "9780060883287", 26.80, 18, "World Literature", "Magical realism in a cursed village.", "https://m.media-amazon.com/images/P/0060883286.01._SX303_BO1,204,203,200_.jpg"),
        book("The Trial", "Franz Kafka", "9780805209990", 20.90, 20, "World Literature", "A man's mysterious arrest and surreal ordeal.", "https://m.media-amazon.com/images/P/0805209999.01._SX303_BO1,204,203,200_.jpg"),
        book("The Stranger", "Albert Camus", "9780679720201", 19.65, 21, "World Literature", "An existential tale of alienation.", "https://m.media-amazon.com/images/P/0679720200.01._SX303_BO1,204,203,200_.jpg"),
        book("The Name of the Rose", "Umberto Eco", "9780156001311", 24.40, 12, "World Literature", "A medieval mystery and philosophical adventure.", "https://m.media-amazon.com/images/P/0156001314.01._SX303_BO1,204,203,200_.jpg"),
        book("The Shadow of the Wind", "Carlos Ruiz Zafon", "9780143126395", 23.75, 17, "World Literature", "Literary mystery in postwar Barcelona.", "https://m.media-amazon.com/images/P/0143126393.01._SX303_BO1,204,203,200_.jpg"),
        book("The Old Man and the Sea", "Ernest Hemingway", "9780684801223", 17.95, 22, "World Literature", "A man's struggle with nature and fate.", "https://m.media-amazon.com/images/P/0684801221.01._SX303_BO1,204,203,200_.jpg"),
        book("The Master and Margarita", "Mikhail Bulgakov", "9780140455466", 25.60, 13, "World Literature", "A satirical masterpiece blending reality and fantasy.", "https://m.media-amazon.com/images/P/0140455469.01._SX303_BO1,204,203,200_.jpg")
    };

    private static Object[] book(String title, String author, String isbn, double price, int stock, String category, String description, String imageUrl) {
        
        return new Object[] { title, author, isbn, price, stock, category, description, imageUrl, "ebooks/sample.pdf" };
    }

    
    public static java.util.List<com.mycompany.ebookshop.models.Book> getSampleBooks() {
        java.util.List<com.mycompany.ebookshop.models.Book> samples = new java.util.ArrayList<>();
        for (Object[] bookData : DEFAULT_BOOKS) {
            String title = (String) bookData[0];
            String author = (String) bookData[1];
            String isbn = (String) bookData[2];
            Double price = (Double) bookData[3];
            
            
            String category = (String) bookData[5];
            String description = (String) bookData[6];
            String imageUrl = (String) bookData[7];
            String filePath = (String) bookData[8];
            samples.add(new com.mycompany.ebookshop.models.Book(0, title, author, isbn, price, category, description, imageUrl, filePath));
        }
        return samples;
    }

    public List<Book> getAllBooks() {
        List<Book> books = new ArrayList<>();
        String query = "SELECT * FROM books ORDER BY title";

        Connection conn = DBConnection.getConnection();
        if (conn == null) {
            return deduplicateBooks(new ArrayList<>(getSampleBooks()));
        }

        ensureSampleBooks(conn);

        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            while (rs.next()) {
                books.add(extractBookFromResultSet(rs));
            }
        } catch (SQLException e) {
            System.err.println("BookDAO getAllBooks error: " + e.getMessage());
        }

        if (books.isEmpty()) {
            return deduplicateBooks(new ArrayList<>(getSampleBooks()));
        }
        return deduplicateBooks(books);
    }

    public Book getBookById(int id) {
        String query = "SELECT * FROM books WHERE id = ?";

        Connection conn = DBConnection.getConnection();
        if (conn == null) return null;

        try (PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return extractBookFromResultSet(rs);
                }
            }
        } catch (SQLException e) {
            System.err.println("BookDAO getBookById error: " + e.getMessage());
        }
        return null;
    }

    public List<Book> searchBooks(String keyword) {
        List<Book> books = new ArrayList<>();
        String query = "SELECT * FROM books WHERE title LIKE ? OR author LIKE ?";

        Connection conn = DBConnection.getConnection();
        if (conn == null) return new ArrayList<>(getSampleBooks());

        ensureSampleBooks(conn);

        try (PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, "%" + keyword + "%");
            pstmt.setString(2, "%" + keyword + "%");
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    books.add(extractBookFromResultSet(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("BookDAO searchBooks error: " + e.getMessage());
        }

        if (books.isEmpty()) {
            return deduplicateBooks(new ArrayList<>(getSampleBooks()));
        }
        return deduplicateBooks(books);
    }

    private List<Book> deduplicateBooks(List<Book> books) {
        Map<String, Book> unique = new LinkedHashMap<>();
        for (Book book : books) {
            if (book == null) {
                continue;
            }
            unique.putIfAbsent(buildDedupKey(book), book);
        }
        return new ArrayList<>(unique.values());
    }

    private String buildDedupKey(Book book) {
        return "title:" + normalizeToken(book.getTitle());
    }

    private String normalizeToken(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().toLowerCase().replace("-", "").replaceAll("\\s+", " ");
    }

    
    public boolean addBook(Book book) {
        String query = "INSERT INTO books (title, author, isbn, price, category, description, image_url, file_path) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        Connection conn = DBConnection.getConnection();
        if (conn == null) return false;

        try (PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, book.getTitle());
            pstmt.setString(2, book.getAuthor());
            pstmt.setString(3, book.getIsbn());
            pstmt.setDouble(4, book.getPrice());
            pstmt.setString(5, book.getCategory());
            pstmt.setString(6, book.getDescription());
            pstmt.setString(7, book.getImageUrl());
            pstmt.setString(8, book.getFilePath());
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("BookDAO addBook error: " + e.getMessage());
            return false;
        }
    }

    public boolean updateBook(Book book) {
        String query = "UPDATE books SET title = ?, author = ?, isbn = ?, price = ?, category = ?, description = ?, image_url = ?, file_path = ? WHERE id = ?";

        Connection conn = DBConnection.getConnection();
        if (conn == null) return false;

        try (PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, book.getTitle());
            pstmt.setString(2, book.getAuthor());
            pstmt.setString(3, book.getIsbn());
            pstmt.setDouble(4, book.getPrice());
            pstmt.setString(5, book.getCategory());
            pstmt.setString(6, book.getDescription());
            pstmt.setString(7, book.getImageUrl());
            pstmt.setString(8, book.getFilePath());
            pstmt.setInt(9, book.getId());
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("BookDAO updateBook error: " + e.getMessage());
            return false;
        }
    }

    public boolean deleteBook(int id) {
        String query = "DELETE FROM books WHERE id = ?";

        Connection conn = DBConnection.getConnection();
        if (conn == null) return false;

        try (PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, id);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("BookDAO deleteBook error: " + e.getMessage());
            return false;
        }
    }

    

    private Book extractBookFromResultSet(ResultSet rs) throws SQLException {
        String title = rs.getString("title");
        String author = rs.getString("author");
        String isbn = rs.getString("isbn");
        double price = rs.getDouble("price");
        String category = rs.getString("category");
        String description = rs.getString("description");

        String filePath = null;
        try {
            filePath = rs.getString("file_path");
        } catch (SQLException ignore) {
            
        }

        String imageUrl = null;
        try {
            imageUrl = rs.getString("image_url");
        } catch (SQLException ignored) {
        }

        Book rawBook = new Book(
            rs.getInt("id"),
            title,
            author,
            isbn,
            price,
            category,
            description,
            imageUrl,
            filePath
        );

        rawBook.setImageUrl(CoverService.getCoverUrl(rawBook));
        return rawBook;
    }

    public int deleteAllBooks() {
        String query = "DELETE FROM books";

        Connection conn = DBConnection.getConnection();
        if (conn == null) return 0;

        try (Statement stmt = conn.createStatement()) {
            return stmt.executeUpdate(query);
        } catch (SQLException e) {
            System.err.println("BookDAO deleteAllBooks error: " + e.getMessage());
            return 0;
        }
    }

    public int removeDuplicateBooks() {
        Connection conn = DBConnection.getConnection();
        if (conn == null) return 0;
        return removeDuplicateBooks(conn);
    }

    private int removeDuplicateBooks(Connection conn) {
        String query = "DELETE b1 FROM books b1 "
                + "JOIN books b2 ON b1.id > b2.id "
                + "AND ((b1.isbn IS NOT NULL AND TRIM(b1.isbn) <> '' AND b2.isbn IS NOT NULL AND TRIM(b2.isbn) <> '' "
                + "AND REPLACE(TRIM(b1.isbn), '-', '') = REPLACE(TRIM(b2.isbn), '-', '')) "
                + "OR ((b1.isbn IS NULL OR TRIM(b1.isbn) = '') AND (b2.isbn IS NULL OR TRIM(b2.isbn) = '') "
                + "AND LOWER(TRIM(b1.title)) = LOWER(TRIM(b2.title)) "
                + "AND LOWER(TRIM(b1.author)) = LOWER(TRIM(b2.author))))";

        try (Statement stmt = conn.createStatement()) {
            return stmt.executeUpdate(query);
        } catch (SQLException e) {
            System.err.println("BookDAO removeDuplicateBooks error: " + e.getMessage());
            return 0;
        }
    }

    private void ensureSampleBooks(Connection conn) {
        
        try {
            int removed = removeDuplicateBooks(conn);
            if (removed > 0) {
                System.out.println("BookDAO: Removed " + removed + " duplicate book row(s).");
            }
        } catch (Exception e) {
            System.err.println("BookDAO ensureSampleBooks: failed to remove duplicates: " + e.getMessage());
        }

        
        String countQuery = "SELECT COUNT(*) as cnt FROM books";
        int bookCount = 0;
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(countQuery)) {
            if (rs.next()) {
                bookCount = rs.getInt("cnt");
            }
        } catch (SQLException e) {
            System.err.println("BookDAO ensureSampleBooks count error: " + e.getMessage());
        }

        
        if (bookCount == 0) {
            String insertQuery = "INSERT INTO books (title, author, isbn, price, category, description, image_url, file_path) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
            try (PreparedStatement pstmt = conn.prepareStatement(insertQuery)) {
                for (Object[] bookData : DEFAULT_BOOKS) {
                    pstmt.setString(1, (String) bookData[0]);
                    pstmt.setString(2, (String) bookData[1]);
                    pstmt.setString(3, (String) bookData[2]);
                    pstmt.setDouble(4, (Double) bookData[3]);
                    
                    pstmt.setString(5, (String) bookData[5]);
                    pstmt.setString(6, (String) bookData[6]);
                    pstmt.setString(7, (String) bookData[7]);
                    pstmt.setString(8, (String) bookData[8]);
                    pstmt.addBatch();
                }
                pstmt.executeBatch();
                System.out.println("BookDAO: Seeded database with " + DEFAULT_BOOKS.length + " sample books.");
                try (Statement ustmt = conn.createStatement()) {
                    
                    String updateFiles = "UPDATE books SET file_path = 'ebooks/sample.pdf' "
                            + "WHERE file_path IS NULL OR file_path = '' OR file_path = 'ebooks/books.txt' "
                            + "OR file_path = 'src/main/resources/books/Sample.pdf' OR file_path = 'src/main/resources/books/sample.pdf' "
                            + "OR file_path = 'classpath:/books/Sample.pdf' OR file_path = 'classpath:/books/sample.pdf'";
                    int updated = ustmt.executeUpdate(updateFiles);
                    if (updated > 0) {
                        System.out.println("BookDAO: Updated " + updated + " seeded book file_path(s) to ebooks/sample.pdf");
                    }
                } catch (SQLException ex) {
                    System.err.println("BookDAO: failed to update seeded file_path: " + ex.getMessage());
                }
            } catch (SQLException e) {
                System.err.println("BookDAO ensureSampleBooks insert error: " + e.getMessage());
            }
        }
        
        try {
            fixPlaceholderImages(conn);
        } catch (Exception e) {
            System.err.println("BookDAO ensureSampleBooks: failed to fix placeholder images: " + e.getMessage());
        }
    }

    
    private void fixPlaceholderImages(Connection conn) {
        if (conn == null) return;
        String updateSql = "UPDATE books SET image_url = CONCAT('https://covers.openlibrary.org/b/isbn/', REPLACE(TRIM(isbn), '-', ''), '-L.jpg') "
                + "WHERE (image_url IS NULL OR image_url = '' OR image_url LIKE 'https://placehold.co/%' OR image_url LIKE 'https://placehold.co%') "
                + "AND isbn IS NOT NULL AND TRIM(isbn) <> ''";
        try (Statement stmt = conn.createStatement()) {
            int updated = stmt.executeUpdate(updateSql);
            if (updated > 0) {
                System.out.println("BookDAO: Updated " + updated + " placeholder image_url(s) to OpenLibrary covers.");
            }
        } catch (SQLException e) {
            System.err.println("BookDAO fixPlaceholderImages error: " + e.getMessage());
        }
    }
}