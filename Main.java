/*
========================= JDBC Helpers (Not Core OOP) =========================
JDBC              --> Java API that lets Java objects interact with databases like any other class, through methods.
PreparedStatement --> an object to send parameterized SQL queries to the DB, like calling a method with arguments.
Statement         --> an object to send static SQL queries without parameters.
ResultSet         --> stores the data returned from a query; acts like an object we iterate over.
res.next()         --> moves to the next row of the ResultSet; returns false if no more rows.
getGeneratedKeys()--> built-in JDBC method that returns DB-generated AUTO_INCREMENT keys.
setString, setInt --> methods to assign values to SQL parameters, like passing arguments to a method.
executeUpdate()   --> executes insert/update/delete queries; returns affected row count.
executeQuery()    --> executes select queries; returns ResultSet object with query results.
*/
import java.sql.*; 
import java.util.Scanner;

public class Main { // Abstraction (class hides implementation details, exposes only necessary methods)

    // Database connection using JDBC (will use the variables inside the main function)
    static final String URL = "jdbc:mysql://localhost:3306/wallet_db_single";
    static final String USER = "wallet_user";
    static final String PASS = "1010";

    static Scanner sc = new Scanner(System.in); 
    static Connection conn; // will use it to check database connection, find it's use under the main function

    static class User // Encapsulation (data grouped inside class, hides internal details)
    {
        int id;
        String name;
        String email;
        User(int id, String name, String email) // constructor
        {
            this.id = id; // this keyword
            this.name = name;
            this.email = email;
        }
    }

    // Print outputs with a border
    static void displayMessage(String title, String msg) // Polymorphism
    {
        System.out.println();
        System.out.println("=== " + title + " ===");
        System.out.println(msg);
        System.out.println("===========================");
    }

    // Register 
    static void registerUser() throws SQLException {
        System.out.print("Enter name: ");
        String name = sc.nextLine(); 
        System.out.print("Enter email: ");
        String email = sc.nextLine(); 
        System.out.print("Enter password: ");
        String password = sc.nextLine(); 

        String sql = "INSERT INTO users (name, email, password) VALUES (?, ?, ?)"; // just a string :)
        /*
        * Statement & PreparedStatement |**|send SQL queries|**| to the database.
        Statement         --> executes static SQL queries directly (no parameters)
        PreparedStatement --> executes parameterized queries (with ?)

        Statement.RETURN_GENERATED_KEYS --> 
        During loginUser, you must fetch the ID from the database, because all wallet 
        operations rely on user_id as the key.

        * We need user_id for fast query processing, rather than email
        */
        try (PreparedStatement stmnt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            // though they are id column, i am accessing name, email, password, here id is auto generated 
            // and who is making id? it's MySQL, automatically assigns a new, unique integer to id
            stmnt.setString(1, name); 
            stmnt.setString(2, email);
            stmnt.setString(3, password);
            stmnt.executeUpdate(); // executes insert/update/delete queries; returns affected row count.
            ResultSet res = stmnt.getGeneratedKeys();
            /*
            * ResultSet --> **stores the result** returned by the database after executing a query.
            * res.next() --> moves to the first row in this query result; false if no match found.
            "First row" = **first matching row** returned by the query, not necessarily row = 1.
            * getGeneratedKeys() is a built-in method in JDBC)
            * JDBC --> Java Database Connectivity, **a Java API ** that allows Java programs to connect 
            to a database, send SQL queries, and process results.
            A bridge between Java code and a database.
            */
            if (res.next()) displayMessage("Registered", "Your ID: " + res.getInt(1)); // first col
        } catch (SQLIntegrityConstraintViolationException e) { // catch duplicate email through res.next()
            displayMessage("Error", "Email already exists. Try logging in.");
        }
    }

    // Login 
    static User loginUser() throws SQLException {
        System.out.print("Enter email: ");
        String email = sc.nextLine(); 
        System.out.print("Enter password: ");
        String password = sc.nextLine(); 

        String sql = "SELECT * FROM users WHERE email = ? AND password = ?";
        try (PreparedStatement stmnt = conn.prepareStatement(sql)) {
            stmnt.setString(1, email);
            stmnt.setString(2, password);
            ResultSet res = stmnt.executeQuery();
            if (res.next()) { 
                displayMessage("Welcome", res.getString("name"));
                return new User(res.getInt("id"), res.getString("name"), res.getString("email"));
            } else {
                displayMessage("Error", "Invalid email or password.");
                return null;
            }
        }
    }

    // Get wallet balance
    static double getUserBalance(int userId) throws SQLException {
        String sql = "SELECT balance FROM wallets WHERE user_id = ?";
        try (PreparedStatement stmnt = conn.prepareStatement(sql)) {
            stmnt.setInt(1, userId);
            ResultSet res = stmnt.executeQuery();
            
            if(res.next()){
                return res.getDouble("balance");
            } else {
                return 0;
            }
        }
    }

    // Deposit money
    static void depositMoney(User user) throws SQLException {
        System.out.print("Enter amount to depositMoney: ");
        double amount = Double.parseDouble(sc.nextLine());

        if (amount <= 0) {
            displayMessage("Error", "Amount must be positive.");
            return;
        }

        double balance = getUserBalance(user.id);
        String sql = "INSERT INTO wallets (user_id, balance) VALUES (?, ?) ON DUPLICATE KEY UPDATE balance = ?";
        try (PreparedStatement stmnt = conn.prepareStatement(sql)) {
            stmnt.setInt(1, user.id);
            stmnt.setDouble(2, balance + amount);
            stmnt.setDouble(3, balance + amount);
            stmnt.executeUpdate();
        }
        recordTransaction(user.id, amount, "Deposit");
        displayMessage("Deposit", "New balance: " + (balance + amount));
    }

    // Withdraw money
    static void withdrawMoney(User user) throws SQLException {
        System.out.print("Enter amount to withdrawMoney: ");
        double amount = Double.parseDouble(sc.nextLine());
        if (amount <= 0) {
            displayMessage("Error", "Amount must be positive.");
            return;
        }
        double balance = getUserBalance(user.id);
        if (amount > balance) {
            displayMessage("Error", "Insufficient funds.");
            return;
        }
        String sql = "UPDATE wallets SET balance = ? WHERE user_id = ?";
        try (PreparedStatement stmnt = conn.prepareStatement(sql)) {
            stmnt.setDouble(1, balance - amount); 
            stmnt.setInt(2, user.id);
            stmnt.executeUpdate();
        }
        recordTransaction(user.id, amount, "Withdrawal");
        displayMessage("Withdrawal", "New balance: " + (balance - amount));
    }

    // Transfer money
    static void transferMoney(User user) throws SQLException {
        System.out.print("Enter recipient email: ");
        String recipientEmail = sc.nextLine();
        String sql = "SELECT id FROM users WHERE email = ?";
        int recipientId;
        try (PreparedStatement stmnt = conn.prepareStatement(sql)) {
            stmnt.setString(1, recipientEmail);
            ResultSet res = stmnt.executeQuery();
            if (res.next()) recipientId = res.getInt("id");
            else {
                displayMessage("Error", "Recipient not found.");
                return;
            }
        }

        System.out.print("Enter amount to send: ");
        double amount = Double.parseDouble(sc.nextLine());
        if (amount <= 0) {
            displayMessage("Error", "Amount must be positive.");
            return;
        }

        double senderBalance = getUserBalance(user.id);
        if (amount > senderBalance) {
            displayMessage("Error", "Insufficient funds.");
            return;
        }
        double receiverBalance = getUserBalance(recipientId);

        String sqlSender = "UPDATE wallets SET balance = ? WHERE user_id = ?";
        String sqlReceiver = "INSERT INTO wallets (user_id, balance) VALUES (?, ?) ON DUPLICATE KEY UPDATE balance = ?";
        try (PreparedStatement stmnt1 = conn.prepareStatement(sqlSender);
             PreparedStatement stmnt2 = conn.prepareStatement(sqlReceiver)) {
            stmnt1.setDouble(1, senderBalance - amount);
            stmnt1.setInt(2, user.id);
            stmnt1.executeUpdate();

            stmnt2.setInt(1, recipientId);
            stmnt2.setDouble(2, receiverBalance + amount);
            stmnt2.setDouble(3, receiverBalance + amount);
            stmnt2.executeUpdate();
        }
        recordTransaction(user.id, amount, "Sent to " + recipientEmail);
        recordTransaction(recipientId, amount, "Received from " + user.email);
        displayMessage("Transfer", "New balance: " + (senderBalance - amount));
    }

    // Record transaction
    static void recordTransaction(int userId, double amount, String type) throws SQLException {
        String sql = "INSERT INTO transactions (user_id, amount, type) VALUES (?, ?, ?)";
        try (PreparedStatement stmnt = conn.prepareStatement(sql)) {
            stmnt.setInt(1, userId);
            stmnt.setDouble(2, amount);
            stmnt.setString(3, type);
            stmnt.executeUpdate();
        }
    }

    // Show transaction history
    static void showTransactionHistory(User user) throws SQLException {
        String sql = "SELECT * FROM transactions WHERE user_id = ?";
        try (PreparedStatement stmnt = conn.prepareStatement(sql)) {
            stmnt.setInt(1, user.id);
            ResultSet res = stmnt.executeQuery();
            StringBuilder sb = new StringBuilder();
            while (res.next()) {
                sb.append(res.getInt("id")).append(": ")
                  .append(res.getString("type")).append(" ")
                  .append(String.format("%.2f", res.getDouble("amount")))
                  .append("\n");
            }
            displayMessage("Transaction History", sb.toString().trim());
        }
    }

    // Check balance
    static void checkUserBalance(User user) throws SQLException {
        double balance = getUserBalance(user.id);
        displayMessage("Balance", String.valueOf(balance));
    }

    // Main Function
    public static void main(String[] args) // main function
    {
        try {
            // Inheritance (JDBC driver classes extend and implement interfaces inside java.sql)
            Class.forName("com.mysql.cj.jdbc.Driver"); // sub class/children
            // com.mysql.cj.jdbc.NonRegisteringDriver (an internal MySQL base class that actually implements most of the logic)
            conn = DriverManager.getConnection(URL, USER, PASS); // The basic service for managing a set of JDBC drivers.

            while (true) // infinite loop will continue to do operations until we exit
            {
                System.out.println("\n=== Digital Wallet System ===");
                System.out.println("1. Login");
                System.out.println("2. Register");
                System.out.println("3. Exit");
                System.out.print("Choose option: ");
                int choice = Integer.parseInt(sc.nextLine());

                if (choice == 1) {
                    User user = loginUser();
                    if (user != null) {
                        while (true) {
                            System.out.println("1. Deposit");
                            System.out.println("2. Withdraw");
                            System.out.println("3. Transfer");
                            System.out.println("4. Transaction History");
                            System.out.println("5. Check Balance");
                            System.out.println("6. Logout");
                            int op = Integer.parseInt(sc.nextLine());
                            if (op == 1) depositMoney(user);
                            else if (op == 2) withdrawMoney(user);
                            else if (op == 3) transferMoney(user);
                            else if (op == 4) showTransactionHistory(user);
                            else if (op == 5) checkUserBalance(user);
                            else break;
                        }
                    }
                } else if (choice == 2) registerUser();
                else break; 
            }

        } catch (ClassNotFoundException e) {
            displayMessage("Error", "MySQL JDBC Driver not found.");
            e.printStackTrace();
        } catch (SQLException e) { // if the mysql server is stopped
            displayMessage("Database error", e.getMessage());
            e.printStackTrace();
        } finally { 
            try { if (conn != null) conn.close(); } catch (SQLException e) {}
        }
        // 'finally' keyword ---> disconnecting my program from MySQL.
        // 'finally' keyword ---> Does NOT stop MySQL server; server keeps running.
    }
}