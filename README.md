# DigitalWalletLite – Console-Based Java Wallet

A Java console-based digital wallet system with MySQL backend. All logic is in a single `Main.java` file.

## Prerequisites

Before running, ensure:

- Java JDK 8+ (check with `java -version`)  
- MySQL server installed and running  
- MySQL Connector/J (`mysql-connector-j-8.0.33.jar`) in the `lib/` folder  

## How to Run

### 1. Open a terminal and navigate to your project folder
(Replace /path/to/DigitalWalletLite with your actual path)
```bash
cd /path/to/DigitalWalletLite
```

### 2. Compile the Java file
#### macOS / Linux:
```bash
mkdir -p bin
javac -d bin -cp "lib/mysql-connector-j-8.0.33.jar" Main.java
```
#### Windows (PowerShell / CMD):
```cmd
mkdir bin
javac -d bin -cp "lib\mysql-connector-j-8.0.33.jar" Main.java
```
### 3. Run the application
#### macOS / Linux:
```bash
java -cp "bin:lib/mysql-connector-j-8.0.33.jar" Main
```
#### Windows (PowerShell / CMD):
```cmd
java -cp "bin;lib\mysql-connector-j-8.0.33.jar" Main
```

### Features

- User registration and login
- Deposit and withdraw money
- Transfer money to other users
- Check account balance
- View transaction history

## MySQL Server

Ensure MySQL is running before starting the app.

#### macOS (Homebrew):

```bash
brew services start mysql
brew services stop mysql
brew services list
```

#### Linux (systemd):

```bash
sudo systemctl start mysql
sudo systemctl stop mysql
sudo systemctl status mysql
```

#### Windows:

Start MySQL using the Services panel or with CMD commands:

```cmd
net start mysql
net stop mysql
```

## Note

- JDBC (Java Database Connectivity) is used to connect Java with MySQL.  
- ``PreparedStatement`` and ``ResultSet`` are used for database operations, not strictly part of OOP.  
- User IDs (AUTO_INCREMENT) are used for efficient wallet and transaction management.
