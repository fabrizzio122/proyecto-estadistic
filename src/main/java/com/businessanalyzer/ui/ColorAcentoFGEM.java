package com.businessanalyzer.ui;

import java.util.Arrays;
import java.util.Optional;

/** Color de acento de la interfaz: lo elige cada usuario y tiñe botones, selección y gráficos destacados. */
public enum ColorAcentoFGEM {

    AMBAR("Ámbar", "#F2B544"),
    CORAL("Coral", "#FF8F73"),
    LIMA("Lima", "#C7E05A"),
    CELESTE("Celeste", "#7CC4F5");

    /** El que se usa mientras nadie ha elegido otro. */
    public static final ColorAcentoFGEM PREDETERMINADO_FGEM = AMBAR;

    private final String nombreFGEM;
    private final String hexFGEM;

    ColorAcentoFGEM(String nombreFGEM, String hexFGEM) {
        this.nombreFGEM = nombreFGEM;
        this.hexFGEM = hexFGEM;
    }

    public String nombreFGEM() {
        return nombreFGEM;
    }

    /** Color en formato {@code #RRGGBB}. */
    public String hexFGEM() {
        return hexFGEM;
    }

    public javafx.scene.paint.Color colorFGEM() {
        return javafx.scene.paint.Color.web(hexFGEM);
    }

    public java.awt.Color awtFGEM() {
        return java.awt.Color.decode(hexFGEM);
    }

    /** Interpreta el nombre guardado (por ejemplo {@code LIMA}); vacío si no corresponde a ninguno. */
    public static Optional<ColorAcentoFGEM> desdeFGEM(String nombreFGEM) {
        return nombreFGEM == null ? Optional.empty()
                : Arrays.stream(values()).filter(cFGEM -> cFGEM.name().equalsIgnoreCase(nombreFGEM.strip())).findFirst();
    }

    @Override
    public String toString() {
        return nombreFGEM;
    }
}
