-- Banco Cloud - crea las tres bases de datos en la instancia RDS.
-- Las tablas las crea Hibernate (spring.jpa.hibernate.ddl-auto=update)
-- la primera vez que arranca cada microservicio.
--
--   mysql -h <rds-endpoint> -u <usuario> -p < 00-init-databases.sql

CREATE DATABASE IF NOT EXISTS usuarios_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS fondos_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS inversiones_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
