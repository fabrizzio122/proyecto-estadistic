package com.businessanalyzer.model;

import java.util.Arrays;
import java.util.List;

public enum CategoriaFinancieraFGEM {

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

    INTERESES_PRESTAMOS("Intereses de préstamos", TipoMovimientoFGEM.GASTO, GrupoFinancieroFGEM.GASTO_FINANCIERO, ComportamientoFGEM.FIJO),
    COMISIONES_BANCARIAS("Comisiones bancarias", TipoMovimientoFGEM.GASTO, GrupoFinancieroFGEM.GASTO_FINANCIERO, ComportamientoFGEM.VARIABLE),
    IMPUESTO_RENTA("Impuesto a la renta", TipoMovimientoFGEM.GASTO, GrupoFinancieroFGEM.IMPUESTO_RENTA, ComportamientoFGEM.VARIABLE),

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
