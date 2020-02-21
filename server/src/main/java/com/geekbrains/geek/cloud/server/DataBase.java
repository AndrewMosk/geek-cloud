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
            connection = DriverManager.getConnection("jdbc:sqlite:server/users.db");
            stmt = connection.createStatement();
        } catch (ClassNotFoundException|SQLException e) {
            e.printStackTrace();
        }
    }

    static boolean authentification(String authString) throws SQLException {
        boolean authOk;
        connect();

        String[] authArr = authString.split(" ", 2);


        int hash = Integer.parseInt(authArr[1]);
        authOk = tryToAuth(authArr[0], hash);

        disconnect();
        return authOk;
    }


    static void disconnect(){
        try {
            connection.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private static boolean tryToAuth(String login, int hash) throws SQLException {
        ResultSet rs = stmt.executeQuery(String.format("SELECT * FROM Users WHERE login = '%s' AND passwordHash = '%s'", login, hash));

        return rs.next();
    }
}