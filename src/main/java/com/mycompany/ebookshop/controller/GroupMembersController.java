package com.mycompany.ebookshop.controller;

import com.mycompany.ebookshop.MainApp;
import javafx.fxml.FXML;

public class GroupMembersController {
    
    @FXML
    public void handleBackToMain() {
        MainApp.changeScene("main.fxml", "eBookShop - Main Store");
    }
}