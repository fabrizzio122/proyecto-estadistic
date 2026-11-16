package com.businessanalyzer.analytics.diagnostico;

/**
 * Indicadores que evalúa el diagnóstico. El peso indica cuánto influye cada uno en el puntaje
 * global (3 = clave, 1 = complementario).
 */
public enum TipoIndicadorFGEM {

    MARGEN_NETO("Margen neto", AreaFGEM.RENTABILIDAD, 3,
            "Qué parte de cada venta queda como ganancia final, después de todos los costos e impuestos."),
    MARGEN_BRUTO("Margen bruto", AreaFGEM.RENTABILIDAD, 2,
            "Lo que queda de cada venta después de pagar el costo de lo vendido."),
    MESES_CON_PERDIDA("Meses con pérdida", AreaFGEM.RENTABILIDAD, 1,
            "Meses con utilidad neta negativa en los últimos seis meses."),

    MARGEN_SEGURIDAD("Margen de seguridad", AreaFGEM.SOLIDEZ, 3,
            "Cuánto pueden caer las ventas antes de que el negocio empiece a perder."),
    PESO_COSTOS_FIJOS("Peso de los costos fijos", AreaFGEM.SOLIDEZ, 1,
            "Costos fijos ÷ ventas. Cuanto más alto, más difícil es adaptarse a un mes malo."),
    CARGA_FINANCIERA("Carga financiera", AreaFGEM.SOLIDEZ, 1,
            "Intereses ÷ utilidad operativa: cuánto de lo que gana el negocio se va en pagar deudas."),
    LIQUIDEZ("Liquidez", AreaFGEM.SOLIDEZ, 2,
            "Activo corriente ÷ pasivo corriente: si la caja, los cobros y el inventario alcanzan para "
                    + "pagar lo que vence en el año."),
    CAJA_PROYECTADA("Caja proyectada", AreaFGEM.SOLIDEZ, 2,
            "Si el dinero de las cuentas alcanza para los próximos seis meses, según el pronóstico del flujo de "
                    + "operación y las cuotas de las deudas."),
    ENDEUDAMIENTO("Endeudamiento", AreaFGEM.SOLIDEZ, 2,
            "Deudas ÷ activos: qué parte de los bienes del negocio está financiada por terceros."),

    VARIACION_VENTAS("Ventas frente al trimestre anterior", AreaFGEM.CRECIMIENTO, 2,
            "Ventas de los últimos tres meses comparadas con los tres meses previos."),
    TENDENCIA_VENTAS("Tendencia de las ventas", AreaFGEM.CRECIMIENTO, 2,
            "Dirección de las ventas mensuales del último año (regresión lineal)."),
    PROYECCION_VENTAS("Proyección de ventas", AreaFGEM.CRECIMIENTO, 1,
            "Ventas pronosticadas para los próximos tres meses frente a los últimos tres."),

    REGULARIDAD_VENTAS("Regularidad de las ventas", AreaFGEM.RIESGO_COMERCIAL, 1,
            "Coeficiente de variación de las ventas mensuales: cuánto cambian de un mes a otro."),
    CONCENTRACION("Dependencia del producto principal", AreaFGEM.RIESGO_COMERCIAL, 1,
            "Parte de las ventas que viene del producto o servicio que más se vende."),
    PRODUCTOS_CON_PERDIDA("Productos vendidos con pérdida", AreaFGEM.RIESGO_COMERCIAL, 2,
            "Parte de las ventas que corresponde a productos vendidos por debajo de su costo."),

    NATALIDAD("Natalidad del hato", AreaFGEM.AGROPECUARIO, 2,
            "Crías nacidas por cada 100 hembras reproductoras en los últimos 12 meses, según la especie."),
    MORTALIDAD_CRIAS("Mortalidad de crías", AreaFGEM.AGROPECUARIO, 2,
            "Crías muertas por cada 100 nacidas en los últimos 12 meses."),
    RENDIMIENTO_CULTIVOS("Rendimiento de los cultivos", AreaFGEM.AGROPECUARIO, 2,
            "Kilos por hectárea de las campañas cerradas en los últimos 12 meses frente a su referencia."),
    COMPROMISOS_INSPECCION("Cumplimiento de compromisos", AreaFGEM.AGROPECUARIO, 1,
            "Compromisos de las visitas de inspección cumplidos frente a los que ya debían cumplirse.");

    private final String nombreFGEM;
    private final AreaFGEM areaFGEM;
    private final int pesoFGEM;
    private final String queMideFGEM;

    TipoIndicadorFGEM(String nombreFGEM, AreaFGEM areaFGEM, int pesoFGEM, String queMideFGEM) {
        this.nombreFGEM = nombreFGEM;
        this.areaFGEM = areaFGEM;
        this.pesoFGEM = pesoFGEM;
        this.queMideFGEM = queMideFGEM;
    }

    public String nombreFGEM() {
        return nombreFGEM;
    }

    public AreaFGEM areaFGEM() {
        return areaFGEM;
    }

    public int pesoFGEM() {
        return pesoFGEM;
    }

    /** Solo se evalúa en empresas del sector agropecuario. */
    public boolean soloAgropecuarioFGEM() {
        return areaFGEM == AreaFGEM.AGROPECUARIO;
    }

    public String queMideFGEM() {
        return queMideFGEM;
    }
}
