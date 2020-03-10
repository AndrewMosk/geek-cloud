package com.geekbrains.geek.cloud.server;

import java.sql.*;

class DataBase {
    private static Connection connection;
    private static Statement stmt;

    static final String DB_URL = "jdbc:postgresql://localhost:5432/users";
    static final String USER = "postgres";
    static final String PASS = "qwerty";

    static void connect(){
        try {
            Class.forName("org.postgresql.Driver");
            connection = DriverManager.getConnection(DB_URL, USER, PASS);
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
            stmt.close();
            connection.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private static boolean tryToAuth(String login, int hash) throws SQLException {
        ResultSet rs = stmt.executeQuery(String.format("SELECT * FROM users WHERE login = '%s' AND hash = '%s'", login, hash));

        return rs.next();
    }

    public static boolean registration(String regString) throws SQLException {
        boolean regOk = true;
        connect();

        String[] authArr = regString.split(" ", 2);

        int hash = Integer.parseInt(authArr[1]);
        if (loginIsFree(authArr[0])) {
            tryToReg(authArr[0], hash);
        } else {
            regOk = false;
        }

        disconnect();
        return regOk;
    }

    private static boolean loginIsFree(String login) throws SQLException {
        ResultSet rs = stmt.executeQuery(String.format("SELECT * FROM users WHERE login = '%s'", login));

        return !rs.next();
    }

    private static void tryToReg(String login, int hash) throws SQLException {
        stmt.execute(String.format("INSERT INTO users VALUES ('%s', '%s')", login, hash));
    }
}