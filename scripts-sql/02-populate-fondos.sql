-- ==========================================
-- Script de Población: FONDOS-SERVICE
-- ==========================================
-- Ejecutar este script en la base de datos de fondos-service

USE fondos_db;

-- Limpiar datos previos (opcional)
DELETE FROM fondos;
ALTER TABLE fondos AUTO_INCREMENT = 1;

-- Fondo Conservador
INSERT INTO fondos (nombre, descripcion, valor_cuota, estado, fecha_creacion, fecha_actualizacion)
VALUES (
  'Fondo Conservador Plus',
  'Fondo de inversión conservador enfocado en rentabilidad segura a largo plazo con mínimo riesgo. Ideal para inversores cautelosos.',
  1500.00,
  'ACTIVO',
  NOW(),
  NOW()
);

-- Fondo Moderado
INSERT INTO fondos (nombre, descripcion, valor_cuota, estado, fecha_creacion, fecha_actualizacion)
VALUES (
  'Fondo Moderado Equilibrado',
  'Fondo con enfoque equilibrado entre renta fija y renta variable. Riesgo medio con potencial de crecimiento moderado.',
  2200.00,
  'ACTIVO',
  NOW(),
  NOW()
);

-- Fondo Agresivo
INSERT INTO fondos (nombre, descripcion, valor_cuota, estado, fecha_creacion, fecha_actualizacion)
VALUES (
  'Fondo Agresivo Crecimiento',
  'Fondo orientado al crecimiento de capital con exposición significativa a renta variable. Alto potencial, alto riesgo.',
  3500.00,
  'ACTIVO',
  NOW(),
  NOW()
);

-- Fondo Tecnología
INSERT INTO fondos (nombre, descripcion, valor_cuota, estado, fecha_creacion, fecha_actualizacion)
VALUES (
  'Fondo Tecnología Global',
  'Fondo especializado en empresas tecnológicas a nivel mundial. Acceso a innovación y empresas líderes en transformación digital.',
  4100.00,
  'ACTIVO',
  NOW(),
  NOW()
);

-- Fondo Renta Fija
INSERT INTO fondos (nombre, descripcion, valor_cuota, estado, fecha_creacion, fecha_actualizacion)
VALUES (
  'Fondo Renta Fija Corporativa',
  'Fondo que invierte en bonos corporativos de calidad. Generador de ingresos con bajo volatilidad.',
  1100.00,
  'ACTIVO',
  NOW(),
  NOW()
);

-- Fondo Sostenible (Inactivo - Suspendido)
INSERT INTO fondos (nombre, descripcion, valor_cuota, estado, fecha_creacion, fecha_actualizacion)
VALUES (
  'Fondo Sostenible ESG',
  'Fondo que invierte en empresas con criterios ambientales, sociales y de gobernanza. Actualmente suspendido por reestructuración.',
  2800.00,
  'INACTIVO',
  NOW(),
  NOW()
);

-- Verificar inserción
SELECT * FROM fondos;

/*
RESUMEN DE FONDOS CREADOS:
==========================

1. Fondo Conservador Plus         - $1,500.00/cuota - ACTIVO
2. Fondo Moderado Equilibrado     - $2,200.00/cuota - ACTIVO
3. Fondo Agresivo Crecimiento     - $3,500.00/cuota - ACTIVO
4. Fondo Tecnología Global        - $4,100.00/cuota - ACTIVO
5. Fondo Renta Fija Corporativa   - $1,100.00/cuota - ACTIVO
6. Fondo Sostenible ESG           - $2,800.00/cuota - INACTIVO

Total de fondos: 6 (5 activos, 1 inactivo)

*/
