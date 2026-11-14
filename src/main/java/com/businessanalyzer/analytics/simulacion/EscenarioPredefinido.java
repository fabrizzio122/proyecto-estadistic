package com.businessanalyzer.analytics.simulacion;

import java.math.BigDecimal;

/** Escenarios de partida que el usuario puede aplicar con un clic y luego ajustar. */
public enum EscenarioPredefinido {

    PESIMISTA("Pesimista", "Se vende 15 % menos y los productos cuestan 5 % más.") {
        @Override
        public Supuestos aplicar(BigDecimal tasaImpuesto) {
            return Supuestos.neutros(tasaImpuesto)
                    .con(Variable.VOLUMEN, new BigDecimal("-0.15"))
                    .con(Variable.COSTO_UNITARIO, new BigDecimal("0.05"));
        }
    },
    OPTIMISTA("Optimista", "Se vende 15 % más y los precios suben 3 %.") {
        @Override
        public Supuestos aplicar(BigDecimal tasaImpuesto) {
            return Supuestos.neutros(tasaImpuesto)
                    .con(Variable.VOLUMEN, new BigDecimal("0.15"))
                    .con(Variable.PRECIO, new BigDecimal("0.03"));
        }
    },
    SUBIR_PRECIOS("Subir precios 5 %", "Precios +5 %, perdiendo un 3 % de las ventas en unidades.") {
        @Override
        public Supuestos aplicar(BigDecimal tasaImpuesto) {
            return Supuestos.neutros(tasaImpuesto)
                    .con(Variable.PRECIO, new BigDecimal("0.05"))
                    .con(Variable.VOLUMEN, new BigDecimal("-0.03"));
        }
    },
    RECORTAR_FIJOS("Recortar fijos 10 %", "Reducir 10 % los costos fijos (alquiler, servicios...).") {
        @Override
        public Supuestos aplicar(BigDecimal tasaImpuesto) {
            return Supuestos.neutros(tasaImpuesto).con(Variable.COSTOS_FIJOS, new BigDecimal("-0.10"));
        }
    };

    private final String nombre;
    private final String descripcion;

    EscenarioPredefinido(String nombre, String descripcion) {
        this.nombre = nombre;
        this.descripcion = descripcion;
    }

    public String nombre() {
        return nombre;
    }

    public String descripcion() {
        return descripcion;
    }

    public abstract Supuestos aplicar(BigDecimal tasaImpuesto);
}
