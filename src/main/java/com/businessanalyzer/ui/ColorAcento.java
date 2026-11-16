package com.businessanalyzer.ui;

import java.util.Arrays;
import java.util.Optional;

/** Color de acento de la interfaz: lo elige cada usuario y tiñe botones, selección y gráficos destacados. */
public enum ColorAcento {

    AMBAR("Ámbar", "#F2B544"),
    CORAL("Coral", "#FF8F73"),
    LIMA("Lima", "#C7E05A"),
    CELESTE("Celeste", "#7CC4F5");

    /** El que se usa mientras nadie ha elegido otro. */
    public static final ColorAcento PREDETERMINADO = AMBAR;

    private final String nombre;
    private final String hex;

    ColorAcento(String nombre, String hex) {
        this.nombre = nombre;
        this.hex = hex;
    }

    public String nombre() {
        return nombre;
    }

    /** Color en formato {@code #RRGGBB}. */
    public String hex() {
        return hex;
    }

    public javafx.scene.paint.Color color() {
        return javafx.scene.paint.Color.web(hex);
    }

    public java.awt.Color awt() {
        return java.awt.Color.decode(hex);
    }

    /** Interpreta el nombre guardado (por ejemplo {@code LIMA}); vacío si no corresponde a ninguno. */
    public static Optional<ColorAcento> desde(String nombre) {
        return nombre == null ? Optional.empty()
                : Arrays.stream(values()).filter(c -> c.name().equalsIgnoreCase(nombre.strip())).findFirst();
    }

    @Override
    public String toString() {
        return nombre;
    }
}
