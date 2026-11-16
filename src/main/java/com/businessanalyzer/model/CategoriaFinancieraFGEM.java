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
public enum CategoriaFinancieraFGEM {

    // ------------------------------------------------------------------ gastos operativos
    SUELDOS("Sueldos y salarios", TipoMovimientoFGEM.GASTO, GrupoFinancieroFGEM.GASTO_OPERATIVO, ComportamientoFGEM.FIJO),
    ALQUILER("Alquiler de local", TipoMovimientoFGEM.GASTO, GrupoFinancieroFGEM.GASTO_OPERATIVO, ComportamientoFGEM.FIJO),
    SERVICIOS_BASICOS("Luz, agua, internet y teléfono", TipoMovimientoFGEM.GASTO, GrupoFinancieroFGEM.GASTO_OPERATIVO, ComportamientoFGEM.FIJO),
    MANTENIMIENTO("Mantenimiento y reparaciones", TipoMovimientoFGEM.GASTO, GrupoFinancieroFGEM.GASTO_OPERATIVO, ComportamientoFGEM.FIJO),
    SEGUROS("Seguros", TipoMovimientoFGEM.GASTO, GrupoFinancieroFGEM.GASTO_OPERATIVO, ComportamientoFGEM.FIJO),
    SERVICIOS_PROFESIONALES("Contabilidad y servicios profesionales", TipoMovimientoFGEM.GASTO, GrupoFinancieroFGEM.GASTO_OPERATIVO, ComportamientoFGEM.FIJO),
    TRIBUTOS("Tributos, licencias y arbitrios", TipoMovimientoFGEM.GASTO, GrupoFinancieroFGEM.GASTO_OPERATIVO, ComportamientoFGEM.FIJO),
    DEPRECIACION("Depreciación de equipos", TipoMovimientoFGEM.GASTO, GrupoFinancieroFGEM.GASTO_OPERATIVO, ComportamientoFGEM.FIJO),
    MARKETING("Publicidad y marketing", TipoMovimientoFGEM.GASTO, GrupoFinancieroFGEM.GASTO_OPERATIVO, ComportamientoFGEM.VARIABLE),
    TRANSPORTE("Transporte y fletes", TipoMovimientoFGEM.GASTO, GrupoFinancieroFGEM.GASTO_OPERATIVO, ComportamientoFGEM.VARIABLE),
    COMISIONES_VENTA("Comisiones de venta", TipoMovimientoFGEM.GASTO, GrupoFinancieroFGEM.GASTO_OPERATIVO, ComportamientoFGEM.VARIABLE),
    EMPAQUES("Empaques y suministros", TipoMovimientoFGEM.GASTO, GrupoFinancieroFGEM.GASTO_OPERATIVO, ComportamientoFGEM.VARIABLE),
    OTROS_GASTOS("Otros gastos operativos", TipoMovimientoFGEM.GASTO, GrupoFinancieroFGEM.GASTO_OPERATIVO, ComportamientoFGEM.FIJO),

    // ------------------------------------------------------------------ gastos financieros e impuestos
    INTERESES_PRESTAMOS("Intereses de préstamos", TipoMovimientoFGEM.GASTO, GrupoFinancieroFGEM.GASTO_FINANCIERO, ComportamientoFGEM.FIJO),
    COMISIONES_BANCARIAS("Comisiones bancarias", TipoMovimientoFGEM.GASTO, GrupoFinancieroFGEM.GASTO_FINANCIERO, ComportamientoFGEM.VARIABLE),
    IMPUESTO_RENTA("Impuesto a la renta", TipoMovimientoFGEM.GASTO, GrupoFinancieroFGEM.IMPUESTO_RENTA, ComportamientoFGEM.VARIABLE),

    // ------------------------------------------------------------------ ingresos no comerciales
    INTERESES_GANADOS("Intereses ganados", TipoMovimientoFGEM.INGRESO, GrupoFinancieroFGEM.INGRESO_FINANCIERO, null),
    ALQUILERES_COBRADOS("Alquileres cobrados", TipoMovimientoFGEM.INGRESO, GrupoFinancieroFGEM.OTRO_INGRESO, null),
    SUBSIDIOS("Subsidios y donaciones", TipoMovimientoFGEM.INGRESO, GrupoFinancieroFGEM.OTRO_INGRESO, null),
    VENTA_ACTIVOS("Venta de activos", TipoMovimientoFGEM.INGRESO, GrupoFinancieroFGEM.OTRO_INGRESO, null),
    OTROS_INGRESOS("Otros ingresos", TipoMovimientoFGEM.INGRESO, GrupoFinancieroFGEM.OTRO_INGRESO, null);

    private final String descripcionFGEM;
    private final TipoMovimientoFGEM tipoFGEM;
    private final GrupoFinancieroFGEM grupoFGEM;
    private final ComportamientoFGEM comportamientoSugeridoFGEM;

    CategoriaFinancieraFGEM(String descripcionFGEM, TipoMovimientoFGEM tipoFGEM, GrupoFinancieroFGEM grupoFGEM,
                        ComportamientoFGEM comportamientoSugeridoFGEM) {
        this.descripcionFGEM = descripcionFGEM;
        this.tipoFGEM = tipoFGEM;
        this.grupoFGEM = grupoFGEM;
        this.comportamientoSugeridoFGEM = comportamientoSugeridoFGEM;
    }

    public String descripcionFGEM() {
        return descripcionFGEM;
    }

    public TipoMovimientoFGEM tipoFGEM() {
        return tipoFGEM;
    }

    public GrupoFinancieroFGEM grupoFGEM() {
        return grupoFGEM;
    }

    /** {@code null} en las categorías de ingreso. */
    public ComportamientoFGEM comportamientoSugeridoFGEM() {
        return comportamientoSugeridoFGEM;
    }

    public static List<CategoriaFinancieraFGEM> deFGEM(TipoMovimientoFGEM tipoFGEM) {
        return Arrays.stream(values()).filter(cFGEM -> cFGEM.tipoFGEM == tipoFGEM).toList();
    }

    @Override
    public String toString() {
        return descripcionFGEM;
    }
}
