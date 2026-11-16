package com.businessanalyzer.analytics.diagnostico;

/**
 * Indicadores que evalúa el diagnóstico. El peso indica cuánto influye cada uno en el puntaje
 * global (3 = clave, 1 = complementario).
 */
public enum TipoIndicador {

    MARGEN_NETO("Margen neto", Area.RENTABILIDAD, 3,
            "Qué parte de cada venta queda como ganancia final, después de todos los costos e impuestos."),
    MARGEN_BRUTO("Margen bruto", Area.RENTABILIDAD, 2,
            "Lo que queda de cada venta después de pagar el costo de lo vendido."),
    MESES_CON_PERDIDA("Meses con pérdida", Area.RENTABILIDAD, 1,
            "Meses con utilidad neta negativa en los últimos seis meses."),

    MARGEN_SEGURIDAD("Margen de seguridad", Area.SOLIDEZ, 3,
            "Cuánto pueden caer las ventas antes de que el negocio empiece a perder."),
    PESO_COSTOS_FIJOS("Peso de los costos fijos", Area.SOLIDEZ, 1,
            "Costos fijos ÷ ventas. Cuanto más alto, más difícil es adaptarse a un mes malo."),
    CARGA_FINANCIERA("Carga financiera", Area.SOLIDEZ, 1,
            "Intereses ÷ utilidad operativa: cuánto de lo que gana el negocio se va en pagar deudas."),
    LIQUIDEZ("Liquidez", Area.SOLIDEZ, 2,
            "Activo corriente ÷ pasivo corriente: si la caja, los cobros y el inventario alcanzan para "
                    + "pagar lo que vence en el año."),
    CAJA_PROYECTADA("Caja proyectada", Area.SOLIDEZ, 2,
            "Si el dinero de las cuentas alcanza para los próximos seis meses, según el pronóstico del flujo de "
                    + "operación y las cuotas de las deudas."),
    ENDEUDAMIENTO("Endeudamiento", Area.SOLIDEZ, 2,
            "Deudas ÷ activos: qué parte de los bienes del negocio está financiada por terceros."),

    VARIACION_VENTAS("Ventas frente al trimestre anterior", Area.CRECIMIENTO, 2,
            "Ventas de los últimos tres meses comparadas con los tres meses previos."),
    TENDENCIA_VENTAS("Tendencia de las ventas", Area.CRECIMIENTO, 2,
            "Dirección de las ventas mensuales del último año (regresión lineal)."),
    PROYECCION_VENTAS("Proyección de ventas", Area.CRECIMIENTO, 1,
            "Ventas pronosticadas para los próximos tres meses frente a los últimos tres."),

    REGULARIDAD_VENTAS("Regularidad de las ventas", Area.RIESGO_COMERCIAL, 1,
            "Coeficiente de variación de las ventas mensuales: cuánto cambian de un mes a otro."),
    CONCENTRACION("Dependencia del producto principal", Area.RIESGO_COMERCIAL, 1,
            "Parte de las ventas que viene del producto o servicio que más se vende."),
    PRODUCTOS_CON_PERDIDA("Productos vendidos con pérdida", Area.RIESGO_COMERCIAL, 2,
            "Parte de las ventas que corresponde a productos vendidos por debajo de su costo."),

    NATALIDAD("Natalidad del hato", Area.AGROPECUARIO, 2,
            "Crías nacidas por cada 100 hembras reproductoras en los últimos 12 meses, según la especie."),
    MORTALIDAD_CRIAS("Mortalidad de crías", Area.AGROPECUARIO, 2,
            "Crías muertas por cada 100 nacidas en los últimos 12 meses."),
    RENDIMIENTO_CULTIVOS("Rendimiento de los cultivos", Area.AGROPECUARIO, 2,
            "Kilos por hectárea de las campañas cerradas en los últimos 12 meses frente a su referencia."),
    COMPROMISOS_INSPECCION("Cumplimiento de compromisos", Area.AGROPECUARIO, 1,
            "Compromisos de las visitas de inspección cumplidos frente a los que ya debían cumplirse.");

    private final String nombre;
    private final Area area;
    private final int peso;
    private final String queMide;

    TipoIndicador(String nombre, Area area, int peso, String queMide) {
        this.nombre = nombre;
        this.area = area;
        this.peso = peso;
        this.queMide = queMide;
    }

    public String nombre() {
        return nombre;
    }

    public Area area() {
        return area;
    }

    public int peso() {
        return peso;
    }

    /** Solo se evalúa en empresas del sector agropecuario. */
    public boolean soloAgropecuario() {
        return area == Area.AGROPECUARIO;
    }

    public String queMide() {
        return queMide;
    }
}
