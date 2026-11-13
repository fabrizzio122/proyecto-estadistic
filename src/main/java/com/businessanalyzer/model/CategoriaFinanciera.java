package com.businessanalyzer.model;

import java.util.Arrays;
import java.util.List;

/**
 * Categorías de ingresos y gastos. Cada una indica su grupo en el estado de resultados y,
 * en los gastos, el comportamiento sugerido (el usuario puede cambiarlo en cada movimiento).
 * <p>
 * Los préstamos recibidos o pagados y los aportes de capital no son ingresos ni gastos:
 * se tratarán en el flujo de caja.
 */
public enum CategoriaFinanciera {

    // ------------------------------------------------------------------ gastos operativos
    SUELDOS("Sueldos y salarios", TipoMovimiento.GASTO, GrupoFinanciero.GASTO_OPERATIVO, Comportamiento.FIJO),
    ALQUILER("Alquiler de local", TipoMovimiento.GASTO, GrupoFinanciero.GASTO_OPERATIVO, Comportamiento.FIJO),
    SERVICIOS_BASICOS("Luz, agua, internet y teléfono", TipoMovimiento.GASTO, GrupoFinanciero.GASTO_OPERATIVO, Comportamiento.FIJO),
    MANTENIMIENTO("Mantenimiento y reparaciones", TipoMovimiento.GASTO, GrupoFinanciero.GASTO_OPERATIVO, Comportamiento.FIJO),
    SEGUROS("Seguros", TipoMovimiento.GASTO, GrupoFinanciero.GASTO_OPERATIVO, Comportamiento.FIJO),
    SERVICIOS_PROFESIONALES("Contabilidad y servicios profesionales", TipoMovimiento.GASTO, GrupoFinanciero.GASTO_OPERATIVO, Comportamiento.FIJO),
    TRIBUTOS("Tributos, licencias y arbitrios", TipoMovimiento.GASTO, GrupoFinanciero.GASTO_OPERATIVO, Comportamiento.FIJO),
    DEPRECIACION("Depreciación de equipos", TipoMovimiento.GASTO, GrupoFinanciero.GASTO_OPERATIVO, Comportamiento.FIJO),
    MARKETING("Publicidad y marketing", TipoMovimiento.GASTO, GrupoFinanciero.GASTO_OPERATIVO, Comportamiento.VARIABLE),
    TRANSPORTE("Transporte y fletes", TipoMovimiento.GASTO, GrupoFinanciero.GASTO_OPERATIVO, Comportamiento.VARIABLE),
    COMISIONES_VENTA("Comisiones de venta", TipoMovimiento.GASTO, GrupoFinanciero.GASTO_OPERATIVO, Comportamiento.VARIABLE),
    EMPAQUES("Empaques y suministros", TipoMovimiento.GASTO, GrupoFinanciero.GASTO_OPERATIVO, Comportamiento.VARIABLE),
    OTROS_GASTOS("Otros gastos operativos", TipoMovimiento.GASTO, GrupoFinanciero.GASTO_OPERATIVO, Comportamiento.FIJO),

    // ------------------------------------------------------------------ gastos financieros e impuestos
    INTERESES_PRESTAMOS("Intereses de préstamos", TipoMovimiento.GASTO, GrupoFinanciero.GASTO_FINANCIERO, Comportamiento.FIJO),
    COMISIONES_BANCARIAS("Comisiones bancarias", TipoMovimiento.GASTO, GrupoFinanciero.GASTO_FINANCIERO, Comportamiento.VARIABLE),
    IMPUESTO_RENTA("Impuesto a la renta", TipoMovimiento.GASTO, GrupoFinanciero.IMPUESTO_RENTA, Comportamiento.VARIABLE),

    // ------------------------------------------------------------------ ingresos no comerciales
    INTERESES_GANADOS("Intereses ganados", TipoMovimiento.INGRESO, GrupoFinanciero.INGRESO_FINANCIERO, null),
    ALQUILERES_COBRADOS("Alquileres cobrados", TipoMovimiento.INGRESO, GrupoFinanciero.OTRO_INGRESO, null),
    SUBSIDIOS("Subsidios y donaciones", TipoMovimiento.INGRESO, GrupoFinanciero.OTRO_INGRESO, null),
    VENTA_ACTIVOS("Venta de activos", TipoMovimiento.INGRESO, GrupoFinanciero.OTRO_INGRESO, null),
    OTROS_INGRESOS("Otros ingresos", TipoMovimiento.INGRESO, GrupoFinanciero.OTRO_INGRESO, null);

    private final String descripcion;
    private final TipoMovimiento tipo;
    private final GrupoFinanciero grupo;
    private final Comportamiento comportamientoSugerido;

    CategoriaFinanciera(String descripcion, TipoMovimiento tipo, GrupoFinanciero grupo,
                        Comportamiento comportamientoSugerido) {
        this.descripcion = descripcion;
        this.tipo = tipo;
        this.grupo = grupo;
        this.comportamientoSugerido = comportamientoSugerido;
    }

    public String descripcion() {
        return descripcion;
    }

    public TipoMovimiento tipo() {
        return tipo;
    }

    public GrupoFinanciero grupo() {
        return grupo;
    }

    /** {@code null} en las categorías de ingreso. */
    public Comportamiento comportamientoSugerido() {
        return comportamientoSugerido;
    }

    public static List<CategoriaFinanciera> de(TipoMovimiento tipo) {
        return Arrays.stream(values()).filter(c -> c.tipo == tipo).toList();
    }

    @Override
    public String toString() {
        return descripcion;
    }
}
