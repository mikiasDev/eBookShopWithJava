package com.mycompany.ebookshop;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class MainApp extends Application {
    
    private static Stage primaryStage;
    
    @Override
    public void start(Stage stage) throws Exception {
        primaryStage = stage;
        Parent root = FXMLLoader.load(getClass().getResource("/com/mycompany/ebookshop/login.fxml"));
        Scene scene = new Scene(root, 1000, 700);
        primaryStage.setTitle("eBookShop - Login");
        primaryStage.setScene(scene);
        primaryStage.setMinWidth(900);
        primaryStage.setMinHeight(600);
        primaryStage.show();
    }
    
    public static void changeScene(String fxml, String title) {
        try {
            Parent root = FXMLLoader.load(MainApp.class.getResource("/com/mycompany/ebookshop/" + fxml));
            Scene scene = new Scene(root, 1000, 700);
            primaryStage.setTitle(title);
            primaryStage.setScene(scene);
        } catch (Exception e) {
            System.err.println("Error loading scene: " + fxml);
            e.printStackTrace();
        }
    }
    
    public static void main(String[] args) {
        launch(args);
    }
}