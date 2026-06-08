package com.mycompany.ebookshop;
import javafx.animation.FadeTransition;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.util.Duration;
@SuppressWarnings("all")
public class MainApp extends Application {
    private static Stage primaryStage;

    @Override
    public void start(Stage stage) throws Exception {
        primaryStage = stage;
        Parent root = FXMLLoader.load(getClass().getResource("/com/mycompany/ebookshop/login.fxml"));
        Scene scene = new Scene(root, 600, 700);
        primaryStage.setTitle("eBookShop - Login");
        primaryStage.setScene(scene);
        primaryStage.setMinWidth(900);
        primaryStage.setMinHeight(600);
        enableWindowDrag(scene);
        primaryStage.show();
        primaryStage.centerOnScreen();
    }

    public static void changeScene(String fxml, String title) {
        try {
            javafx.concurrent.Task<Parent> loadTask = new javafx.concurrent.Task<>() {
                @Override
                protected Parent call() throws Exception {
                    return FXMLLoader.load(MainApp.class.getResource("/com/mycompany/ebookshop/" + fxml));
                }
            };

            loadTask.setOnSucceeded(evt -> {
                Parent root = loadTask.getValue();
                primaryStage.setTitle(title);
                
                root.setOpacity(0);
                if (primaryStage.getScene() == null) {
                    Scene scene = new Scene(root, Math.max(800, primaryStage.getWidth()), Math.max(600, primaryStage.getHeight()));
                    primaryStage.setScene(scene);
                    enableWindowDrag(scene);
                } else {
                    primaryStage.getScene().setRoot(root);
                    enableWindowDrag(primaryStage.getScene());
                }

                
                try {
                    FadeTransition ft = new FadeTransition(Duration.millis(320), root);
                    ft.setFromValue(0);
                    ft.setToValue(1);
                    ft.play();
                } catch (Exception ignore) {
                }

                if (primaryStage.isIconified()) {
                    primaryStage.setIconified(false);
                }
                primaryStage.show();
                primaryStage.toFront();
                primaryStage.requestFocus();
            });

            loadTask.setOnFailed(evt -> {
                System.err.println("Error loading scene (background): " + fxml);
                Throwable ex = loadTask.getException();
                if (ex != null) {
                    System.err.println(ex.getClass().getName() + ": " + ex.getMessage());
                    ex.printStackTrace(System.err);
                }
            });

            Thread t = new Thread(loadTask);
            t.setDaemon(true);
            t.start();
        } catch (Exception e) {
            System.err.println("Unexpected error scheduling scene load: " + e.getMessage());
            System.err.println(e.getClass().getName() + ": " + e.getMessage());
        }
    }

    public static Stage getPrimaryStage() {
        return primaryStage;
    }

    
    private static void enableWindowDrag(Scene scene) {
        if (scene == null) return;
        final var root = scene.getRoot();
        final double[] dragOffset = new double[2];

        root.setOnMousePressed(e -> {
            Stage stage = primaryStage;
            if (stage != null) {
                dragOffset[0] = e.getScreenX() - stage.getX();
                dragOffset[1] = e.getScreenY() - stage.getY();
            }
        });

        root.setOnMouseDragged(e -> {
            Stage stage = primaryStage;
            if (stage != null && !e.isConsumed()) {
                stage.setX(e.getScreenX() - dragOffset[0]);
                stage.setY(e.getScreenY() - dragOffset[1]);
            }
        });
    }

    public static void main(String[] args) {
        launch(args);
    }
}
