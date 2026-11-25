package com.businessanalyzer.model;

/** Informes que puede generar el sistema y su formato de archivo. */
public enum TipoInformeFGEM {

    INFORME_EJECUTIVO("Informe ejecutivo", FormatoFGEM.PDF, "fth-award",
            "Resumen para la gerencia: indicadores clave, estado de resultados, evolución, punto de "
                    + "equilibrio, productos más rentables y diagnóstico con recomendaciones."),
    ESTADO_RESULTADOS("Estado de resultados", FormatoFGEM.PDF, "fth-file-text",
            "Estado de resultados comparado con el período anterior, márgenes, punto de equilibrio "
                    + "y resultado de cada mes."),
    RENTABILIDAD_PRODUCTOS("Rentabilidad por producto", FormatoFGEM.PDF, "fth-package",
            "Ventas, costo y margen de cada producto o servicio vendido en el período, con su gráfico."),
    LIBRO_EXCEL("Libro de datos en Excel", FormatoFGEM.EXCEL, "fth-grid",
            "Hojas con el estado de resultados, la evolución mensual, los productos, las ventas, las "
                    + "compras y los ingresos y gastos, listas para filtrar o seguir analizando."),
    INFORME_TECNICO("Informe técnico de visita", FormatoFGEM.PDF, "fth-clipboard",
            "Informe de la visita de inspección a un productor agropecuario: verificación en campo, compromisos, "
                    + "indicadores del hato, campañas, situación patrimonial y fotos. Se genera desde «Visitas»."),
    VALORACION("Valoración del negocio", FormatoFGEM.PDF, "fth-star",
            "Cuánto vale el negocio por su patrimonio, por capitalización de utilidades y por flujos descontados, "
                    + "con sensibilidad. Se genera desde «Valoración del negocio»."),
    LISTA_COMPRAS("Lista de compras", FormatoFGEM.PDF, "fth-shopping-cart",
            "Productos por reponer agrupados por categoría, con la cantidad a comprar y el costo aproximado, para "
                    + "imprimir. Se genera desde «Sugerencias»."),
    LISTA_COMPRAS_EXCEL("Lista de compras", FormatoFGEM.EXCEL, "fth-shopping-cart",
            "Productos por reponer con la cantidad y el costo como fórmula, para ajustar antes de comprar. "
                    + "Se genera desde «Sugerencias».");

    /** Formato del archivo generado. */
    public enum FormatoFGEM {
        PDF("PDF", "pdf", "Documento PDF"),
        EXCEL("Excel", "xlsx", "Libro de Excel");

        private final String nombreFGEM;
        private final String extensionFGEM;
        private final String descripcionArchivoFGEM;

        FormatoFGEM(String nombreFGEM, String extensionFGEM, String descripcionArchivoFGEM) {
            this.nombreFGEM = nombreFGEM;
            this.extensionFGEM = extensionFGEM;
            this.descripcionArchivoFGEM = descripcionArchivoFGEM;
        }

        public String nombreFGEM() {
            return nombreFGEM;
        }

        public String extensionFGEM() {
            return extensionFGEM;
        }

        public String descripcionArchivoFGEM() {
            return descripcionArchivoFGEM;
        }
    }

    private final String tituloFGEM;
    private final FormatoFGEM formatoFGEM;
    private final String iconoFGEM;
    private final String descripcionFGEM;

    TipoInformeFGEM(String tituloFGEM, FormatoFGEM formatoFGEM, String iconoFGEM, String descripcionFGEM) {
        this.tituloFGEM = tituloFGEM;
        this.formatoFGEM = formatoFGEM;
        this.iconoFGEM = iconoFGEM;
        this.descripcionFGEM = descripcionFGEM;
    }

    /** Se genera desde su propio módulo (visitas, valoración, sugerencias), no desde el módulo de informes. */
    public boolean desdeOtroModuloFGEM() {
        return this == INFORME_TECNICO || this == VALORACION || this == LISTA_COMPRAS || this == LISTA_COMPRAS_EXCEL;
    }

    public String tituloFGEM() {
        return tituloFGEM;
    }

    public FormatoFGEM formatoFGEM() {
        return formatoFGEM;
    }

    /** Código del icono Feather (Ikonli). */
    public String iconoFGEM() {
        return iconoFGEM;
    }

    public String descripcionFGEM() {
        return descripcionFGEM;
    }

    @Override
    public String toString() {
        return tituloFGEM;
    }
}
