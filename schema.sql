-- Tables and database are created only once, existing tables and data are not overwritten
CREATE DATABASE IF NOT EXISTS wallet_db_single;

-- Select the database to use
USE wallet_db_single;

-- Users table stores all registered users 
CREATE TABLE IF NOT EXISTS users (
    -- id is unique, represents user id
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(100) NOT NULL UNIQUE, -- unique email for login
    password VARCHAR(255) NOT NULL -- password (store hashed in real apps)
);

-- Wallets table stores the balance for each user
CREATE TABLE IF NOT EXISTS wallets (
    user_id INT PRIMARY KEY,
    balance DOUBLE DEFAULT 0, -- current wallet balance
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
    -- If a row in users is deleted, all rows in this table that reference that user_id are automatically deleted.
);

-- Transactions table logs all operations for each user
CREATE TABLE IF NOT EXISTS transactions (
    id INT AUTO_INCREMENT PRIMARY KEY, -- unique transaction ID
    user_id INT NOT NULL, -- references which user did this transaction
    amount DOUBLE NOT NULL, -- transaction amount
    type VARCHAR(50) NOT NULL, -- deposit, withdraw, transfer
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE -- for example, DELETE FROM users WHERE id = 1;
    -- If a user is deleted, all their transactions are automatically deleted
);