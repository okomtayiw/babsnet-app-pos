package com.babsnet.posapp.util;

import org.mindrot.jbcrypt.BCrypt;


public class PasswordUtil {
    public static String hashPassword(String plainPassword) {
        return BCrypt.hashpw(plainPassword, BCrypt.gensalt(12));
    }
    public static boolean checkPassword(String plainPassword, String hash) {
         boolean match = BCrypt.checkpw(plainPassword, hash);
         System.out.println(match);
         return match;
    }
}


