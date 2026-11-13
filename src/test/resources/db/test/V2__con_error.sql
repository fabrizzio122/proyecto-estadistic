-- Script de prueba: la primera sentencia es valida y la segunda no.
-- Sirve para comprobar que la migracion completa se revierte.
CREATE TABLE tabla_parcial (id INTEGER PRIMARY KEY);
INSERT INTO tabla_que_no_existe (id) VALUES (1);
