package com.geekbrains.geek.cloud.server;

import java.sql.*;
import java.util.ArrayList;
import java.util.Date;

class DataBase {
    private static Connection connection;
    private static Statement stmt;

    static void connect(){
        try {
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection("jdbc:sqlite:users.db");
            stmt = connection.createStatement();
        } catch (ClassNotFoundException|SQLException e) {
            e.printStackTrace();
        }
    }



    static void disconnect(){
        try {
            connection.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

}
