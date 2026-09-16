-- ==========================================
-- Script de Población: USUARIOS-SERVICE
-- ==========================================
-- Ejecutar este script en la base de datos de usuarios-service
-- 
-- Usuarios creados:
-- 1. Admin: admin@bancocloud.com | Password: Admin@123456
-- 2. Cliente1: cliente1@bancocloud.com | Password: Cliente123@456
-- 3. Cliente2: cliente2@bancocloud.com | Password: Cliente456@789

USE usuarios_db;

-- Limpiar datos previos (opcional)
DELETE FROM usuarios;
ALTER TABLE usuarios AUTO_INCREMENT = 1;

-- Insertar usuario ADMIN
INSERT INTO usuarios (nombre, email, password_hash, rol, acceso_autorizado, estado, fecha_creacion, fecha_actualizacion) 
VALUES (
  'Administrador Principal',
  'admin@bancocloud.com',
  '$2b$10$aFJPR68AxgTzjznUzePgE.Pnj2jjQmw1CuainNHax4quc5dtXROtO',
  'ADMIN',
  true,
  'ACTIVO',
  NOW(),
  NOW()
);

-- Insertar usuario CLIENTE (sin autorizar)
INSERT INTO usuarios (nombre, email, password_hash, rol, acceso_autorizado, estado, fecha_creacion, fecha_actualizacion) 
VALUES (
  'Juan Pérez García',
  'cliente1@bancocloud.com',
  '$2b$10$3pdr3cwmwZvFUVjbL1W96uh6xelts9HhinpYN9h5fQlFlxlZ1ck.S',
  'CLIENTE',
  false,
  'ACTIVO',
  NOW(),
  NOW()
);

-- Insertar usuario CLIENTE (autorizado)
INSERT INTO usuarios (nombre, email, password_hash, rol, acceso_autorizado, estado, fecha_creacion, fecha_actualizacion) 
VALUES (
  'María López Rodríguez',
  'cliente2@bancocloud.com',
  '$2b$10$zXJB8mOpqIr02A5UOZclbOMZvjCypke43xJuJ6WCZOFpM4OyjYzJa',
  'CLIENTE',
  true,
  'ACTIVO',
  NOW(),
  NOW()
);

-- Insertar usuario CLIENTE (bloqueado)
INSERT INTO usuarios (nombre, email, password_hash, rol, acceso_autorizado, estado, fecha_creacion, fecha_actualizacion) 
VALUES (
  'Carlos Sánchez Martín',
  'cliente3@bancocloud.com',
  '$2b$10$3wkVC27I7etfA.0ppsTTgeq0G4578rvxKxjN2RbHdifVoBkh7gJQG',
  'CLIENTE',
  true,
  'BLOQUEADO',
  NOW(),
  NOW()
);

-- Verificar inserción
SELECT * FROM usuarios;

/*
NOTAS IMPORTANTES:
=================

Los passwords hasheados fueron generados con BCryptPasswordEncoder de Spring Security.
Para verificar los valores usados, aquí están las contraseñas sin hash:

1. admin@bancocloud.com     -> Contraseña: Admin@123456
2. cliente1@bancocloud.com  -> Contraseña: Cliente123@456
3. cliente2@bancocloud.com  -> Contraseña: Cliente456@789
4. cliente3@bancocloud.com  -> Contraseña: Carlos@123456

Todos los passwords cumplen con los requisitos:
✓ Mínimo 8 caracteres
✓ Al menos 1 mayúscula
✓ Al menos 1 número
✓ Al menos 1 símbolo especial (@)

*/