ALTER TABLE usuario ADD COLUMN color_acento TEXT
    CHECK (color_acento IS NULL OR color_acento IN ('AMBAR', 'CORAL', 'LIMA', 'CELESTE'));
