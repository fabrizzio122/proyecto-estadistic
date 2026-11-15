package com.businessanalyzer.model;

import java.util.Arrays;
import java.util.List;

/**
 * Categorías del hato por especie. Las crías reciben los nacimientos; las hembras reproductoras
 * son la base de la natalidad.
 */
public enum CategoriaGanado {

    VACA(Especie.VACUNO, "Vacas", Rol.HEMBRA_REPRODUCTORA),
    VAQUILLONA(Especie.VACUNO, "Vaquillonas", Rol.RECRIA),
    TERNERA(Especie.VACUNO, "Terneras", Rol.CRIA),
    TERNERO(Especie.VACUNO, "Terneros", Rol.CRIA),
    TORETE(Especie.VACUNO, "Toretes", Rol.RECRIA),
    TORO(Especie.VACUNO, "Toros", Rol.MACHO_REPRODUCTOR),

    OVEJA(Especie.OVINO, "Ovejas", Rol.HEMBRA_REPRODUCTORA),
    BORREGA(Especie.OVINO, "Borregas", Rol.RECRIA),
    CORDERO(Especie.OVINO, "Corderos", Rol.CRIA),
    BORREGO(Especie.OVINO, "Borregos", Rol.RECRIA),
    CARNERO(Especie.OVINO, "Carneros", Rol.MACHO_REPRODUCTOR),

    ALPACA_MADRE(Especie.ALPACA, "Madres", Rol.HEMBRA_REPRODUCTORA),
    ALPACA_TUI_HEMBRA(Especie.ALPACA, "Tuis hembras", Rol.RECRIA),
    ALPACA_CRIA(Especie.ALPACA, "Crías", Rol.CRIA),
    ALPACA_TUI_MACHO(Especie.ALPACA, "Tuis machos", Rol.RECRIA),
    ALPACA_PADRE(Especie.ALPACA, "Padres", Rol.MACHO_REPRODUCTOR),
    ALPACA_CAPON(Especie.ALPACA, "Capones", Rol.OTRO),

    LLAMA_MADRE(Especie.LLAMA, "Madres", Rol.HEMBRA_REPRODUCTORA),
    LLAMA_TUI_HEMBRA(Especie.LLAMA, "Tuis hembras", Rol.RECRIA),
    LLAMA_CRIA(Especie.LLAMA, "Crías", Rol.CRIA),
    LLAMA_TUI_MACHO(Especie.LLAMA, "Tuis machos", Rol.RECRIA),
    LLAMA_PADRE(Especie.LLAMA, "Padres", Rol.MACHO_REPRODUCTOR),
    LLAMA_CAPON(Especie.LLAMA, "Capones", Rol.OTRO);

    /** Función de la categoría en el hato. */
    public enum Rol { HEMBRA_REPRODUCTORA, MACHO_REPRODUCTOR, CRIA, RECRIA, OTRO }

    private final Especie especie;
    private final String descripcion;
    private final Rol rol;

    CategoriaGanado(Especie especie, String descripcion, Rol rol) {
        this.especie = especie;
        this.descripcion = descripcion;
        this.rol = rol;
    }

    public Especie especie() {
        return especie;
    }

    public String descripcion() {
        return descripcion;
    }

    public Rol rol() {
        return rol;
    }

    public boolean cria() {
        return rol == Rol.CRIA;
    }

    /** "Alpacas · Madres". */
    public String nombreCompleto() {
        return especie.descripcion() + " · " + descripcion;
    }

    public static List<CategoriaGanado> de(Especie especie) {
        return Arrays.stream(values()).filter(c -> c.especie == especie).toList();
    }

    @Override
    public String toString() {
        return descripcion;
    }
}
