package com.babsnet.posapp.session;

import com.babsnet.posapp.model.Product;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public class CartSession {
    private static final ObservableList<Product> cart = FXCollections.observableArrayList();

    public static ObservableList<Product> getCart() {
        return cart;
    }

    public static void clearCart() {
        cart.clear();
    }
}

