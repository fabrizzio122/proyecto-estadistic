package com.businessanalyzer.analytics.simulacion;

import java.math.BigDecimal;

/** Escenarios de partida que el usuario puede aplicar con un clic y luego ajustar. */
public enum EscenarioPredefinidoFGEM {

    PESIMISTA("Pesimista", "Se vende 15 % menos y los productos cuestan 5 % más.") {
        @Override
        public SupuestosFGEM aplicarFGEM(BigDecimal tasaImpuestoFGEM) {
            return SupuestosFGEM.neutrosFGEM(tasaImpuestoFGEM)
                    .conFGEM(VariableFGEM.VOLUMEN, new BigDecimal("-0.15"))
                    .conFGEM(VariableFGEM.COSTO_UNITARIO, new BigDecimal("0.05"));
        }
    },
    OPTIMISTA("Optimista", "Se vende 15 % más y los precios suben 3 %.") {
        @Override
        public SupuestosFGEM aplicarFGEM(BigDecimal tasaImpuestoFGEM) {
            return SupuestosFGEM.neutrosFGEM(tasaImpuestoFGEM)
                    .conFGEM(VariableFGEM.VOLUMEN, new BigDecimal("0.15"))
                    .conFGEM(VariableFGEM.PRECIO, new BigDecimal("0.03"));
        }
    },
    SUBIR_PRECIOS("Subir precios 5 %", "Precios +5 %, perdiendo un 3 % de las ventas en unidades.") {
        @Override
        public SupuestosFGEM aplicarFGEM(BigDecimal tasaImpuestoFGEM) {
            return SupuestosFGEM.neutrosFGEM(tasaImpuestoFGEM)
                    .conFGEM(VariableFGEM.PRECIO, new BigDecimal("0.05"))
                    .conFGEM(VariableFGEM.VOLUMEN, new BigDecimal("-0.03"));
        }
    },
    RECORTAR_FIJOS("Recortar fijos 10 %", "Reducir 10 % los costos fijos (alquiler, servicios...).") {
        @Override
        public SupuestosFGEM aplicarFGEM(BigDecimal tasaImpuestoFGEM) {
            return SupuestosFGEM.neutrosFGEM(tasaImpuestoFGEM).conFGEM(VariableFGEM.COSTOS_FIJOS, new BigDecimal("-0.10"));
        }
    };

    private final String nombreFGEM;
    private final String descripcionFGEM;

    EscenarioPredefinidoFGEM(String nombreFGEM, String descripcionFGEM) {
        this.nombreFGEM = nombreFGEM;
        this.descripcionFGEM = descripcionFGEM;
    }

    public String nombreFGEM() {
        return nombreFGEM;
    }

    public String descripcionFGEM() {
        return descripcionFGEM;
    }

    public abstract SupuestosFGEM aplicarFGEM(BigDecimal tasaImpuestoFGEM);
}
