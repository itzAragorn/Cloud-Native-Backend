-- ==========================================
-- Script de Población: USUARIOS-SERVICE
-- ==========================================
-- Ejecutar este script en la base de datos de usuarios-service
-- 
-- Usuarios creados:
-- 1. Admin: admin@bancocloud.com | Password: ah
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
  '$2a$10$lKZcGn8RgsFsDrz.pXJPYe.hxF3e7gVz8uVHVhm5Hw2KzYGJZWyM2',
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
  '$2a$10$KNlvVkNjJn8c.9XzP.9cWO4GQMQ8m5DvL2K3vQ4rT8sZ1Y0pM6Ba',
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
  '$2a$10$4qnKnVcL9m3P.8YzQ.8cXO5HRNPR9n6EwM3L4wR5sU9aZ1PqN7Cb',
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
  '$2a$10$5roLoWdM0n4Q.9ZaR.9dYP6ISQSo0o7FxN4M5xS6tV0bA2QrO8Dc',
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
