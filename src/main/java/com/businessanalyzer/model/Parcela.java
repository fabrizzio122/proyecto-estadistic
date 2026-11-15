package com.businessanalyzer.model;

import java.math.BigDecimal;

/**
 * Terreno donde se siembra.
 *
 * @param area superficie en hectáreas
 */
public record Parcela(long id, long empresaId, String nombre, BigDecimal area, Tenencia tenencia, boolean riego,
                      String ubicacion, boolean activa) {

    @Override
    public String toString() {
        return nombre;
    }
}
