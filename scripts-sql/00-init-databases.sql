-- ==========================================
-- Crear bases de datos
-- ==========================================
CREATE DATABASE IF NOT EXISTS usuarios_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS fondos_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS inversiones_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- ==========================================
-- Switch a usuarios_db
-- ==========================================
USE usuarios_db;

-- Crear tabla usuarios
CREATE TABLE IF NOT EXISTS usuarios (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    nombre VARCHAR(100) NOT NULL,
    email VARCHAR(100) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    rol ENUM('ADMIN', 'CLIENTE') NOT NULL DEFAULT 'CLIENTE',
    acceso_autorizado BIT(1) NOT NULL DEFAULT 0,
    estado ENUM('ACTIVO', 'BLOQUEADO', 'INACTIVO') NOT NULL DEFAULT 'ACTIVO',
    fecha_creacion DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    fecha_actualizacion DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    INDEX idx_email (email),
    INDEX idx_rol (rol),
    INDEX idx_acceso (acceso_autorizado)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Insertar usuario ADMIN
INSERT INTO usuarios (nombre, email, password_hash, rol, acceso_autorizado, estado, fecha_creacion, fecha_actualizacion) 
VALUES (
  'Administrador Principal',
  'admin@bancocloud.com',
  '$2a$10$clXPLoo7vq1aYee3bXh04.mJxiCLxBY/8Q6p6eOF85WoskmzjVvtu',
  'ADMIN',
  true,
  'ACTIVO',
  NOW(6),
  NOW(6)
);

-- Insertar usuario CLIENTE (sin autorizar)
INSERT INTO usuarios (nombre, email, password_hash, rol, acceso_autorizado, estado, fecha_creacion, fecha_actualizacion) 
VALUES (
  'Juan Pérez García',
  'cliente1@bancocloud.com',
  '$2a$10$OjtE3lTl4ekvtdktC1mVtO/w2WMnBeFaIh6CdfeQgmWHUOgEc7v/2',
  'CLIENTE',
  false,
  'ACTIVO',
  NOW(6),
  NOW(6)
);

-- Insertar usuario CLIENTE (autorizado)
INSERT INTO usuarios (nombre, email, password_hash, rol, acceso_autorizado, estado, fecha_creacion, fecha_actualizacion) 
VALUES (
  'María López Rodríguez',
  'cliente2@bancocloud.com',
  '$2a$10$Ku4UluY1SY/XKqhygMe2o.uzZI2Q1tD4oJjQUrd676NM7hYxmuxtO',
  'CLIENTE',
  true,
  'ACTIVO',
  NOW(6),
  NOW(6)
);

-- Insertar usuario CLIENTE (bloqueado)
INSERT INTO usuarios (nombre, email, password_hash, rol, acceso_autorizado, estado, fecha_creacion, fecha_actualizacion) 
VALUES (
  'Carlos Sánchez Martín',
  'cliente3@bancocloud.com',
  '$2a$10$huJMWo1TapFNIGJ1H7pPd.BKMzYwZS6ELjDDayGes/ExMsfOSoVVu',
  'CLIENTE',
  true,
  'BLOQUEADO',
  NOW(6),
  NOW(6)
);

-- ==========================================
-- Switch a fondos_db
-- ==========================================
USE fondos_db;

-- Crear tabla fondos
CREATE TABLE IF NOT EXISTS fondos (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    nombre VARCHAR(100) NOT NULL,
    descripcion TEXT,
    valor_cuota DECIMAL(15,4) NOT NULL,
    estado ENUM('ACTIVO', 'INACTIVO', 'SUSPENDIDO') NOT NULL DEFAULT 'ACTIVO',
    fecha_creacion DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    fecha_actualizacion DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    INDEX idx_estado (estado)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ==========================================
-- Switch a inversiones_db
-- ==========================================
USE inversiones_db;

-- Crear tabla inversiones
CREATE TABLE IF NOT EXISTS inversiones (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    usuario_id BIGINT NOT NULL,
    fondo_id BIGINT NOT NULL,
    monto_invertido DECIMAL(15,2) NOT NULL,
    cuotas DECIMAL(15,4) NOT NULL,
    valor_cuota_compra DECIMAL(15,4) NOT NULL,
    estado ENUM('ACTIVA', 'RETIRADA', 'CANCELADA') NOT NULL DEFAULT 'ACTIVA',
    fecha_inversion DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    fecha_actualizacion DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    INDEX idx_usuario (usuario_id),
    INDEX idx_fondo (fondo_id),
    INDEX idx_estado (estado)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
