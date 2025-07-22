package com.babsnet.posapp.session;

import com.babsnet.posapp.model.User;
import com.babsnet.posapp.repository.UserRepository;
import com.babsnet.posapp.util.MessageDialogUtil;

import java.io.*;
import java.util.Properties;

public class SessionManager {
    private static SessionManager instance;
    private User currentUser;
    private final String SESSION_FILE = System.getProperty("user.dir") + "/user_session.properties";


    private SessionManager() {
        loadSession();
    }

    public static SessionManager getInstance() {
        if (instance == null) {
            instance = new SessionManager();
        }
        return instance;
    }

    public void login(User user) {
        this.currentUser = user;
        saveSession(user);
    }

    public void logout() {
        currentUser = null;
        clearSession();
    }

    public User getCurrentUser() {
        return currentUser;
    }

    public boolean isLoggedIn() {
        return currentUser != null;
    }

    public String getUserRole() {
        return currentUser != null ? currentUser.getRole() : null;
    }

    private void saveSession(User user) {
        Properties prop = new Properties();
        prop.setProperty("username", user.getUsername());
        try (FileOutputStream fos = new FileOutputStream(SESSION_FILE)) {
            prop.store(fos, "User Session");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void loadSession() {
        Properties prop = new Properties();
        try (FileInputStream fis = new FileInputStream(SESSION_FILE)) {
            prop.load(fis);
            String username = prop.getProperty("username");
            if (username != null && !username.isEmpty()) {
                currentUser = new UserRepository().findByUsername(username);
            }
        } catch (IOException e) {
            currentUser = null;
        }
    }

    private void clearSession() {
        File file = new File(SESSION_FILE);
        if (file.exists()) {
            boolean deleted = file.delete();
            MessageDialogUtil.showInfo("Session file delete status: " + deleted + " | Path: " + file.getAbsolutePath());
        } else {
            MessageDialogUtil.showError("Session file does not exist: " + file.getAbsolutePath());
        }
    }



}
