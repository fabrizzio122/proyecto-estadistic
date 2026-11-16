package com.businessanalyzer.model;

import java.util.Arrays;
import java.util.List;

/**
 * Categorías del hato por especie. Las crías reciben los nacimientos; las hembras reproductoras
 * son la base de la natalidad.
 */
public enum CategoriaGanadoFGEM {

    VACA(EspecieFGEM.VACUNO, "Vacas", RolFGEM.HEMBRA_REPRODUCTORA),
    VAQUILLONA(EspecieFGEM.VACUNO, "Vaquillonas", RolFGEM.RECRIA),
    TERNERA(EspecieFGEM.VACUNO, "Terneras", RolFGEM.CRIA),
    TERNERO(EspecieFGEM.VACUNO, "Terneros", RolFGEM.CRIA),
    TORETE(EspecieFGEM.VACUNO, "Toretes", RolFGEM.RECRIA),
    TORO(EspecieFGEM.VACUNO, "Toros", RolFGEM.MACHO_REPRODUCTOR),

    OVEJA(EspecieFGEM.OVINO, "Ovejas", RolFGEM.HEMBRA_REPRODUCTORA),
    BORREGA(EspecieFGEM.OVINO, "Borregas", RolFGEM.RECRIA),
    CORDERO(EspecieFGEM.OVINO, "Corderos", RolFGEM.CRIA),
    BORREGO(EspecieFGEM.OVINO, "Borregos", RolFGEM.RECRIA),
    CARNERO(EspecieFGEM.OVINO, "Carneros", RolFGEM.MACHO_REPRODUCTOR),

    ALPACA_MADRE(EspecieFGEM.ALPACA, "Madres", RolFGEM.HEMBRA_REPRODUCTORA),
    ALPACA_TUI_HEMBRA(EspecieFGEM.ALPACA, "Tuis hembras", RolFGEM.RECRIA),
    ALPACA_CRIA(EspecieFGEM.ALPACA, "Crías", RolFGEM.CRIA),
    ALPACA_TUI_MACHO(EspecieFGEM.ALPACA, "Tuis machos", RolFGEM.RECRIA),
    ALPACA_PADRE(EspecieFGEM.ALPACA, "Padres", RolFGEM.MACHO_REPRODUCTOR),
    ALPACA_CAPON(EspecieFGEM.ALPACA, "Capones", RolFGEM.OTRO),

    LLAMA_MADRE(EspecieFGEM.LLAMA, "Madres", RolFGEM.HEMBRA_REPRODUCTORA),
    LLAMA_TUI_HEMBRA(EspecieFGEM.LLAMA, "Tuis hembras", RolFGEM.RECRIA),
    LLAMA_CRIA(EspecieFGEM.LLAMA, "Crías", RolFGEM.CRIA),
    LLAMA_TUI_MACHO(EspecieFGEM.LLAMA, "Tuis machos", RolFGEM.RECRIA),
    LLAMA_PADRE(EspecieFGEM.LLAMA, "Padres", RolFGEM.MACHO_REPRODUCTOR),
    LLAMA_CAPON(EspecieFGEM.LLAMA, "Capones", RolFGEM.OTRO);

    /** Función de la categoría en el hato. */
    public enum RolFGEM { HEMBRA_REPRODUCTORA, MACHO_REPRODUCTOR, CRIA, RECRIA, OTRO }

    private final EspecieFGEM especieFGEM;
    private final String descripcionFGEM;
    private final RolFGEM rolFGEM;

    CategoriaGanadoFGEM(EspecieFGEM especieFGEM, String descripcionFGEM, RolFGEM rolFGEM) {
        this.especieFGEM = especieFGEM;
        this.descripcionFGEM = descripcionFGEM;
        this.rolFGEM = rolFGEM;
    }

    public EspecieFGEM especieFGEM() {
        return especieFGEM;
    }

    public String descripcionFGEM() {
        return descripcionFGEM;
    }

    public RolFGEM rolFGEM() {
        return rolFGEM;
    }

    public boolean criaFGEM() {
        return rolFGEM == RolFGEM.CRIA;
    }

    /** "Alpacas · Madres". */
    public String nombreCompletoFGEM() {
        return especieFGEM.descripcionFGEM() + " · " + descripcionFGEM;
    }

    public static List<CategoriaGanadoFGEM> deFGEM(EspecieFGEM especieFGEM) {
        return Arrays.stream(values()).filter(cFGEM -> cFGEM.especieFGEM == especieFGEM).toList();
    }

    @Override
    public String toString() {
        return descripcionFGEM;
    }
}
