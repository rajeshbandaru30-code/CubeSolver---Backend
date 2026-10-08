-- ============================================================
-- CubeSolve Database Schema (MySQL 8.0+)
-- Database: cubesolve_db
-- ============================================================

CREATE DATABASE IF NOT EXISTS cubesolve_db
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE cubesolve_db;

-- ------------------------------------------------------------
-- Table: users
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS users (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  username VARCHAR(50) NOT NULL UNIQUE,
  password_hash VARCHAR(255) NOT NULL,
  email VARCHAR(100) NOT NULL UNIQUE,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_users_username (username),
  INDEX idx_users_email (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ------------------------------------------------------------
-- Table: solve_records
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS solve_records (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  cube_state TEXT NOT NULL,
  scramble_moves TEXT,
  solution_moves TEXT NOT NULL,
  move_count INT NOT NULL,
  solve_time_ms BIGINT NOT NULL,
  user_id BIGINT,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_solve_records_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
  INDEX idx_solve_records_user (user_id),
  INDEX idx_solve_records_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
