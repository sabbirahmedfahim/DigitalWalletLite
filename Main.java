/*
========================= JDBC Helpers (Not Core OOP) =========================
PreparedStatement --> an object to send parameterized SQL queries to the DB, like calling a method with arguments.
Statement         --> an object to send static SQL queries without parameters.
ResultSet         --> stores the data returned from a query; acts like an object we iterate over.
rs.next()         --> moves to the next row of the ResultSet; returns false if no more rows.
getGeneratedKeys()--> built-in JDBC method that returns DB-generated AUTO_INCREMENT keys.
JDBC              --> Java API that lets Java objects interact with databases like any other class, through methods.
setString, setInt --> methods to assign values to SQL parameters, like passing arguments to a method.
executeUpdate()   --> executes insert/update/delete queries; returns affected row count.
executeQuery()    --> executes select queries; returns ResultSet object with query results.
*/
import java.sql.*;
import java.util.Scanner;

public class Main {

    static final String URL = "jdbc:mysql://localhost:3306/wallet_db_single";
    static final String USER = "wallet_user";
    static final String PASS = "1010";

    static Scanner sc = new Scanner(System.in);
    static Connection conn;

    static class User {
        int id;
        String name;
        String email;
        User(int id, String name, String email) {
            this.id = id;
            this.name = name;
            this.email = email;
        }
    }

    // Print outputs with a border
    static void printWithBorder(String header, String body) {
        System.out.println("=== " + header + " ===");
        System.out.println(body);
        System.out.println("===========================");
    }

    // Register 
    static void register() throws SQLException {
        System.out.print("Enter name: ");
        String name = sc.nextLine();
        System.out.print("Enter email: ");
        String email = sc.nextLine();
        System.out.print("Enter password: ");
        String password = sc.nextLine();

        String sql = "INSERT INTO users (name, email, password) VALUES (?, ?, ?)";
        /*
        * Statement & PreparedStatement **send SQL queries** to the database.
        Statement         --> executes static SQL queries directly (no parameters)
        PreparedStatement --> executes parameterized queries (with ?)

        Statement.RETURN_GENERATED_KEYS --> 
        During login, you must fetch the ID from the database, because all wallet 
        operations rely on user_id as the key.

        * We need user_id for fast query processing, rather than email
        */
        try (PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, name);
            stmt.setString(2, email);
            stmt.setString(3, password);
            stmt.executeUpdate();
            ResultSet rs = stmt.getGeneratedKeys();
            /*
            * ResultSet --> **stores the result** returned by the database after executing a query.
            * rs.next() --> moves to the first row in this query result; false if no match found.
            "First row" = **first matching row** returned by the query, not necessarily row = 1.
            * getGeneratedKeys() is a built-in method in JDBC)
            * JDBC --> Java Database Connectivity, **a Java API ** that allows Java programs to connect 
            to a database, send SQL queries, and process results.
            A bridge between Java code and a database.
            */
            if (rs.next()) printWithBorder("Registered", "Your ID: " + rs.getInt(1));
        } catch (SQLIntegrityConstraintViolationException e) { // catch duplicate email
            printWithBorder("Error", "Email already exists. Try logging in.");
        }
    }

    // Login 
    static User login() throws SQLException {
        System.out.print("Enter email: ");
        String email = sc.nextLine();
        System.out.print("Enter password: ");
        String password = sc.nextLine();

        String sql = "SELECT * FROM users WHERE email = ? AND password = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, email);
            stmt.setString(2, password);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) { 
                printWithBorder("Welcome", rs.getString("name"));
                return new User(rs.getInt("id"), rs.getString("name"), rs.getString("email"));
            } else {
                printWithBorder("Error", "Invalid email or password.");
                return null;
            }
        }
    }

    // Get wallet balance
    static double getBalance(int userId) throws SQLException {
        String sql = "SELECT balance FROM wallets WHERE user_id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();
            return rs.next() ? rs.getDouble("balance") : 0;
        }
    }

    // Deposit money
    static void deposit(User user) throws SQLException {
        System.out.print("Enter amount to deposit: ");
        double amount = Double.parseDouble(sc.nextLine());
        double balance = getBalance(user.id);
        String sql = "INSERT INTO wallets (user_id, balance) VALUES (?, ?) ON DUPLICATE KEY UPDATE balance = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, user.id);
            stmt.setDouble(2, balance + amount);
            stmt.setDouble(3, balance + amount);
            stmt.executeUpdate();
        }
        recordTransaction(user.id, amount, "Deposit");
        printWithBorder("Deposit", "New balance: " + (balance + amount));
    }

    // Withdraw money
    static void withdraw(User user) throws SQLException {
        System.out.print("Enter amount to withdraw: ");
        double amount = Double.parseDouble(sc.nextLine());
        double balance = getBalance(user.id);
        if (amount > balance) {
            printWithBorder("Error", "Insufficient funds.");
            return;
        }
        String sql = "UPDATE wallets SET balance = ? WHERE user_id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setDouble(1, balance - amount);
            stmt.setInt(2, user.id);
            stmt.executeUpdate();
        }
        recordTransaction(user.id, amount, "Withdrawal");
        printWithBorder("Withdrawal", "New balance: " + (balance - amount));
    }

    // Transfer money
    static void transfer(User user) throws SQLException {
        System.out.print("Enter recipient email: ");
        String recipientEmail = sc.nextLine();
        String sql = "SELECT id FROM users WHERE email = ?";
        int rid;
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, recipientEmail);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) rid = rs.getInt("id");
            else {
                printWithBorder("Error", "Recipient not found.");
                return;
            }
        }

        System.out.print("Enter amount to send: ");
        double amount = Double.parseDouble(sc.nextLine());
        double senderBalance = getBalance(user.id);
        if (amount > senderBalance) {
            printWithBorder("Error", "Insufficient funds.");
            return;
        }
        double receiverBalance = getBalance(rid);

        String sqlSender = "UPDATE wallets SET balance = ? WHERE user_id = ?";
        String sqlReceiver = "INSERT INTO wallets (user_id, balance) VALUES (?, ?) ON DUPLICATE KEY UPDATE balance = ?";
        try (PreparedStatement stmt1 = conn.prepareStatement(sqlSender);
             PreparedStatement stmt2 = conn.prepareStatement(sqlReceiver)) {
            stmt1.setDouble(1, senderBalance - amount);
            stmt1.setInt(2, user.id);
            stmt1.executeUpdate();

            stmt2.setInt(1, rid);
            stmt2.setDouble(2, receiverBalance + amount);
            stmt2.setDouble(3, receiverBalance + amount);
            stmt2.executeUpdate();
        }
        recordTransaction(user.id, amount, "Sent to " + recipientEmail);
        recordTransaction(rid, amount, "Received from " + user.email);
        printWithBorder("Transfer", "New balance: " + (senderBalance - amount));
    }

    // Record transaction
    static void recordTransaction(int userId, double amount, String type) throws SQLException {
        String sql = "INSERT INTO transactions (user_id, amount, type) VALUES (?, ?, ?)";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            stmt.setDouble(2, amount);
            stmt.setString(3, type);
            stmt.executeUpdate();
        }
    }

    // Show transaction history
    static void showHistory(User user) throws SQLException {
        String sql = "SELECT * FROM transactions WHERE user_id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, user.id);
            ResultSet rs = stmt.executeQuery();
            StringBuilder sb = new StringBuilder();
            while (rs.next()) {
                sb.append(rs.getInt("id")).append(": ")
                  .append(rs.getString("type")).append(" ")
                  .append(String.format("%.2f", rs.getDouble("amount")))
                  .append("\n");
            }
            printWithBorder("Transaction History", sb.toString().trim());
        }
    }

    // Check balance
    static void checkBalance(User user) throws SQLException {
        double balance = getBalance(user.id);
        printWithBorder("Balance", String.valueOf(balance));
    }

    public static void main(String[] args) {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            conn = DriverManager.getConnection(URL, USER, PASS);

            while (true) {
                System.out.println("\n=== Digital Wallet System ===");
                System.out.println("1. Login");
                System.out.println("2. Register");
                System.out.println("3. Exit");
                System.out.print("Choose option: ");
                int choice = Integer.parseInt(sc.nextLine());

                if (choice == 1) {
                    User user = login();
                    if (user != null) {
                        while (true) {
                            System.out.println("1. Deposit");
                            System.out.println("2. Withdraw");
                            System.out.println("3. Transfer");
                            System.out.println("4. Transaction History");
                            System.out.println("5. Check Balance");
                            System.out.println("6. Logout");
                            int opt = Integer.parseInt(sc.nextLine());
                            if (opt == 1) deposit(user);
                            else if (opt == 2) withdraw(user);
                            else if (opt == 3) transfer(user);
                            else if (opt == 4) showHistory(user);
                            else if (opt == 5) checkBalance(user);
                            else break;
                        }
                    }
                } else if (choice == 2) register();
                else break;
            }

        } catch (ClassNotFoundException e) {
            printWithBorder("Error", "MySQL JDBC Driver not found.");
            e.printStackTrace();
        } catch (SQLException e) {
            printWithBorder("Database error", e.getMessage());
            e.printStackTrace();
        } finally {
            try { if (conn != null) conn.close(); } catch (SQLException e) {}
        }
    }
}