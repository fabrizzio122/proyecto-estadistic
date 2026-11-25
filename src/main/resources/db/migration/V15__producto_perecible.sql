-- Fase 28: productos perecibles (pan, lácteos frescos, verduras...).
-- Las sugerencias los tratan aparte: se reponen para menos días y, si no se venden, la recomendación es urgente.
ALTER TABLE producto ADD COLUMN perecible INTEGER NOT NULL DEFAULT 0 CHECK (perecible IN (0, 1));
