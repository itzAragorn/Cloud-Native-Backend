-- Banco Cloud - convierte en ADMIN a un usuario ya registrado.
-- Todo usuario se registra desde el frontend (Cognito) con rol CLIENTE;
-- el primer administrador se asigna a mano con este script.
-- Reemplaza el correo antes de ejecutarlo.
--
--   mysql -h <rds-endpoint> -u <usuario> -p < 01-promover-admin.sql

USE usuarios_db;

UPDATE usuarios
SET rol = 'ADMIN',
    acceso_autorizado = 1,
    estado = 'ACTIVO'
WHERE email = 'admin@ejemplo.com';
