-- ==========================================
-- Script de Población: INVERSIONES-SERVICE
-- ==========================================
-- Ejecutar este script en la base de datos de inversiones-service
-- 
-- IMPORTANTE: Este script asume que ya están pobladas las tablas:
--   - usuarios-service.usuarios (con IDs 2 y 3)
--   - fondos-service.fondos (con IDs 1 a 5)

USE inversiones_db;

-- Limpiar datos previos (opcional)
DELETE FROM inversiones;
ALTER TABLE inversiones AUTO_INCREMENT = 1;

-- Inversión 1: Cliente2 en Fondo Conservador
-- Monto: $50,000.00, Valor cuota: $1,500.00 → ~33.33 cuotas
INSERT INTO inversiones (usuario_id, fondo_id, monto_invertido, cuotas, valor_cuota_compra, estado, fecha_inversion, fecha_actualizacion)
VALUES (
  3,
  1,
  50000.00,
  33.3333,
  1500.00,
  'ACTIVA',
  DATE_SUB(NOW(), INTERVAL 90 DAY),
  NOW()
);

-- Inversión 2: Cliente2 en Fondo Moderado
-- Monto: $75,000.00, Valor cuota: $2,200.00 → ~34.09 cuotas
INSERT INTO inversiones (usuario_id, fondo_id, monto_invertido, cuotas, valor_cuota_compra, estado, fecha_inversion, fecha_actualizacion)
VALUES (
  3,
  2,
  75000.00,
  34.0909,
  2200.00,
  'ACTIVA',
  DATE_SUB(NOW(), INTERVAL 60 DAY),
  NOW()
);

-- Inversión 3: Cliente2 en Fondo Tecnología
-- Monto: $100,000.00, Valor cuota: $4,100.00 → ~24.39 cuotas
INSERT INTO inversiones (usuario_id, fondo_id, monto_invertido, cuotas, valor_cuota_compra, estado, fecha_inversion, fecha_actualizacion)
VALUES (
  3,
  4,
  100000.00,
  24.3902,
  4100.00,
  'ACTIVA',
  DATE_SUB(NOW(), INTERVAL 30 DAY),
  NOW()
);

-- Inversión 4: Cliente1 en Fondo Conservador
-- Monto: $30,000.00, Valor cuota: $1,500.00 → 20 cuotas
INSERT INTO inversiones (usuario_id, fondo_id, monto_invertido, cuotas, valor_cuota_compra, estado, fecha_inversion, fecha_actualizacion)
VALUES (
  2,
  1,
  30000.00,
  20.0000,
  1500.00,
  'ACTIVA',
  DATE_SUB(NOW(), INTERVAL 120 DAY),
  NOW()
);

-- Inversión 5: Cliente1 en Fondo Renta Fija
-- Monto: $45,000.00, Valor cuota: $1,100.00 → ~40.91 cuotas
INSERT INTO inversiones (usuario_id, fondo_id, monto_invertido, cuotas, valor_cuota_compra, estado, fecha_inversion, fecha_actualizacion)
VALUES (
  2,
  5,
  45000.00,
  40.9091,
  1100.00,
  'ACTIVA',
  DATE_SUB(NOW(), INTERVAL 45 DAY),
  NOW()
);

-- Inversión 6: Cliente1 en Fondo Agresivo (Retirada)
-- Monto: $20,000.00, Valor cuota: $3,500.00 → ~5.71 cuotas
INSERT INTO inversiones (usuario_id, fondo_id, monto_invertido, cuotas, valor_cuota_compra, estado, fecha_inversion, fecha_actualizacion)
VALUES (
  2,
  3,
  20000.00,
  5.7143,
  3500.00,
  'RETIRADA',
  DATE_SUB(NOW(), INTERVAL 180 DAY),
  DATE_SUB(NOW(), INTERVAL 10 DAY)
);

-- Inversión 7: Cliente2 en Fondo Renta Fija
-- Monto: $60,000.00, Valor cuota: $1,100.00 → ~54.55 cuotas
INSERT INTO inversiones (usuario_id, fondo_id, monto_invertido, cuotas, valor_cuota_compra, estado, fecha_inversion, fecha_actualizacion)
VALUES (
  3,
  5,
  60000.00,
  54.5455,
  1100.00,
  'ACTIVA',
  DATE_SUB(NOW(), INTERVAL 75 DAY),
  NOW()
);

-- Inversión 8: Cliente1 en Fondo Moderado (Cancelada)
-- Monto: $25,000.00, Valor cuota: $2,200.00 → ~11.36 cuotas
INSERT INTO inversiones (usuario_id, fondo_id, monto_invertido, cuotas, valor_cuota_compra, estado, fecha_inversion, fecha_actualizacion)
VALUES (
  2,
  2,
  25000.00,
  11.3636,
  2200.00,
  'CANCELADA',
  DATE_SUB(NOW(), INTERVAL 200 DAY),
  DATE_SUB(NOW(), INTERVAL 5 DAY)
);

-- Verificar inserción
SELECT * FROM inversiones;

-- Estadísticas de inversiones
SELECT 
  usuario_id,
  COUNT(*) as total_inversiones,
  SUM(monto_invertido) as monto_total,
  COUNT(CASE WHEN estado = 'ACTIVA' THEN 1 END) as inversiones_activas
FROM inversiones
GROUP BY usuario_id
ORDER BY usuario_id;

/*
RESUMEN DE INVERSIONES CREADAS:
================================

Cliente2 (usuario_id=3):
  • Inversión 1: $50,000 en Fondo Conservador (ACTIVA)
  • Inversión 2: $75,000 en Fondo Moderado (ACTIVA)
  • Inversión 3: $100,000 en Fondo Tecnología (ACTIVA)
  • Inversión 7: $60,000 en Fondo Renta Fija (ACTIVA)
  Total: $285,000

Cliente1 (usuario_id=2):
  • Inversión 4: $30,000 en Fondo Conservador (ACTIVA)
  • Inversión 5: $45,000 en Fondo Renta Fija (ACTIVA)
  • Inversión 6: $20,000 en Fondo Agresivo (RETIRADA)
  • Inversión 8: $25,000 en Fondo Moderado (CANCELADA)
  Total: $120,000

Total inversiones: 8
Total invertido: $405,000.00
Inversiones activas: 6
Inversiones retiradas/canceladas: 2

*/
