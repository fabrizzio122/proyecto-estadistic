-- V14: preferencias visuales de cada usuario. El color de acento de la interfaz (ámbar, coral, lima o
-- celeste) se guarda por usuario y se aplica a toda la aplicación al iniciar sesión. Vacío = el color
-- predeterminado.
ALTER TABLE usuario ADD COLUMN color_acento TEXT
    CHECK (color_acento IS NULL OR color_acento IN ('AMBAR', 'CORAL', 'LIMA', 'CELESTE'));
