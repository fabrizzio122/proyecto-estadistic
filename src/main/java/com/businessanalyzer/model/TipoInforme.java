package com.businessanalyzer.model;

/** Informes que puede generar el sistema y su formato de archivo. */
public enum TipoInforme {

    INFORME_EJECUTIVO("Informe ejecutivo", Formato.PDF, "fth-award",
            "Resumen para la gerencia: indicadores clave, estado de resultados, evolución, punto de "
                    + "equilibrio, productos más rentables y diagnóstico con recomendaciones."),
    ESTADO_RESULTADOS("Estado de resultados", Formato.PDF, "fth-file-text",
            "Estado de resultados comparado con el período anterior, márgenes, punto de equilibrio "
                    + "y resultado de cada mes."),
    RENTABILIDAD_PRODUCTOS("Rentabilidad por producto", Formato.PDF, "fth-package",
            "Ventas, costo y margen de cada producto o servicio vendido en el período, con su gráfico."),
    LIBRO_EXCEL("Libro de datos en Excel", Formato.EXCEL, "fth-grid",
            "Hojas con el estado de resultados, la evolución mensual, los productos, las ventas, las "
                    + "compras y los ingresos y gastos, listas para filtrar o seguir analizando.");

    /** Formato del archivo generado. */
    public enum Formato {
        PDF("PDF", "pdf", "Documento PDF"),
        EXCEL("Excel", "xlsx", "Libro de Excel");

        private final String nombre;
        private final String extension;
        private final String descripcionArchivo;

        Formato(String nombre, String extension, String descripcionArchivo) {
            this.nombre = nombre;
            this.extension = extension;
            this.descripcionArchivo = descripcionArchivo;
        }

        public String nombre() {
            return nombre;
        }

        public String extension() {
            return extension;
        }

        public String descripcionArchivo() {
            return descripcionArchivo;
        }
    }

    private final String titulo;
    private final Formato formato;
    private final String icono;
    private final String descripcion;

    TipoInforme(String titulo, Formato formato, String icono, String descripcion) {
        this.titulo = titulo;
        this.formato = formato;
        this.icono = icono;
        this.descripcion = descripcion;
    }

    public String titulo() {
        return titulo;
    }

    public Formato formato() {
        return formato;
    }

    /** Código del icono Feather (Ikonli). */
    public String icono() {
        return icono;
    }

    public String descripcion() {
        return descripcion;
    }

    @Override
    public String toString() {
        return titulo;
    }
}
