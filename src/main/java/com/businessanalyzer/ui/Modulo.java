package com.businessanalyzer.ui;

import com.businessanalyzer.model.Permiso;

import java.util.Arrays;
import java.util.Optional;

/**
 * Modulos del sistema tal como aparecen en el menu lateral (en este orden).
 * <p>
 * Un modulo sin vista propia muestra una pantalla "en construccion" que indica la fase en
 * la que se implementa. Al construir un modulo basta con asignarle su archivo FXML aqui.
 * Cada modulo solo aparece en el menu si el usuario tiene el permiso indicado.
 */
public enum Modulo {

    DASHBOARD(Seccion.GENERAL, "Dashboard",
            "Resumen de los indicadores clave del negocio", "fth-grid", 1,
            Permiso.DASHBOARD_VER, "/fxml/dashboard-view.fxml"),

    EMPRESAS(Seccion.GESTION, "Empresas",
            "Registro de las empresas y negocios a analizar", "fth-briefcase", 3,
            Permiso.EMPRESAS_VER, "/fxml/empresas-view.fxml"),
    PRODUCTOS(Seccion.GESTION, "Productos y servicios",
            "Catálogo con costos, precios y márgenes", "fth-package", 4,
            Permiso.PRODUCTOS_VER, "/fxml/productos-view.fxml"),
    VENTAS_COMPRAS(Seccion.GESTION, "Ventas y compras",
            "Registro de las operaciones comerciales", "fth-shopping-cart", 5,
            Permiso.OPERACIONES_VER, "/fxml/operaciones-view.fxml"),
    FINANZAS(Seccion.GESTION, "Gestión financiera",
            "Ingresos, gastos y costos fijos y variables", "fth-dollar-sign", 6,
            Permiso.FINANZAS_VER, "/fxml/finanzas-view.fxml"),
    PATRIMONIO(Seccion.GESTION, "Activos y deudas",
            "Bienes, deudas, balance general y patrimonio", "fth-layers", 14,
            Permiso.FINANZAS_VER, "/fxml/patrimonio-view.fxml"),

    GANADERIA(Seccion.AGRO, "Ganadería",
            "Hato, nacimientos, mortalidad, producción y valor del ganado", "fth-feather", 15,
            Permiso.AGRO_VER, "/fxml/ganaderia-view.fxml"),
    AGRICULTURA(Seccion.AGRO, "Agricultura",
            "Parcelas, campañas, costos, cosechas y rendimiento", "fth-sun", 16,
            Permiso.AGRO_VER, "/fxml/agricultura-view.fxml"),
    VISITAS(Seccion.AGRO, "Visitas de inspección",
            "Verificación en campo, compromisos, fotos e informe técnico", "fth-clipboard", 17,
            Permiso.AGRO_VER, "/fxml/visitas-view.fxml"),

    ANALISIS_FINANCIERO(Seccion.ANALISIS, "Análisis financiero",
            "Estado de resultados, márgenes, ratios y punto de equilibrio", "fth-pie-chart", 7,
            Permiso.ANALISIS_VER, "/fxml/analisis-financiero-view.fxml"),
    ESTADISTICA(Seccion.ANALISIS, "Análisis estadístico",
            "Tendencias, estacionalidad, dispersión y análisis ABC", "fth-bar-chart-2", 8,
            Permiso.ANALISIS_VER, "/fxml/estadistica-view.fxml"),
    PREDICCIONES(Seccion.ANALISIS, "Predicciones",
            "Proyección de ventas y resultados de los próximos meses", "fth-trending-up", 9,
            Permiso.ANALISIS_VER, "/fxml/predicciones-view.fxml"),
    ESCENARIOS(Seccion.ANALISIS, "Simulación de escenarios",
            "Qué pasaría si cambian los precios, costos o volúmenes", "fth-sliders", 10,
            Permiso.ANALISIS_VER, "/fxml/simulacion-view.fxml"),
    DIAGNOSTICO(Seccion.ANALISIS, "Diagnóstico empresarial",
            "Semáforo de riesgos y recomendaciones", "fth-activity", 11,
            Permiso.ANALISIS_VER, "/fxml/diagnostico-view.fxml"),
    VALORACION(Seccion.ANALISIS, "Valoración del negocio",
            "Cuánto vale el negocio por lo que tiene y por lo que gana", "fth-star", 19,
            Permiso.ANALISIS_VER, "/fxml/valoracion-view.fxml"),
    INFORMES(Seccion.ANALISIS, "Informes y reportes",
            "Reportes en PDF y exportación a Excel", "fth-file-text", 12,
            Permiso.INFORMES_GENERAR, "/fxml/informes-view.fxml"),

    USUARIOS(Seccion.SISTEMA, "Usuarios y permisos",
            "Cuentas, roles y control de acceso", "fth-users", 2,
            Permiso.USUARIOS_GESTIONAR, "/fxml/usuarios-view.fxml"),
    CONFIGURACION(Seccion.SISTEMA, "Configuración",
            "Preferencias y copias de seguridad", "fth-settings", 13,
            Permiso.CONFIGURACION_GESTIONAR, "/fxml/configuracion-view.fxml");

    /** Grupos del menu lateral. */
    public enum Seccion {
        GENERAL("General"),
        GESTION("Gestión"),
        AGRO("Agropecuario"),
        ANALISIS("Análisis"),
        SISTEMA("Sistema");

        private final String titulo;

        Seccion(String titulo) {
            this.titulo = titulo;
        }

        public String titulo() {
            return titulo;
        }
    }

    public static final String VISTA_PENDIENTE = "/fxml/modulo-pendiente-view.fxml";

    private final Seccion seccion;
    private final String titulo;
    private final String descripcion;
    private final String icono;
    private final int fase;
    private final Permiso permiso;
    private final String fxml;

    Modulo(Seccion seccion, String titulo, String descripcion, String icono, int fase,
           Permiso permiso, String fxml) {
        this.seccion = seccion;
        this.titulo = titulo;
        this.descripcion = descripcion;
        this.icono = icono;
        this.fase = fase;
        this.permiso = permiso;
        this.fxml = fxml;
    }

    public Seccion seccion() {
        return seccion;
    }

    public String titulo() {
        return titulo;
    }

    public String descripcion() {
        return descripcion;
    }

    /** Codigo del icono Feather (Ikonli), por ejemplo {@code fth-grid}. */
    public String icono() {
        return icono;
    }

    /** Fase del proyecto en la que se implementa el modulo. */
    public int fase() {
        return fase;
    }

    /** Permiso necesario para ver el modulo. */
    public Permiso permiso() {
        return permiso;
    }

    /** Solo se muestra cuando la empresa activa es del sector agropecuario. */
    public boolean soloAgropecuario() {
        return seccion == Seccion.AGRO;
    }

    public boolean disponible() {
        return fxml != null;
    }

    /** Ruta FXML a mostrar: la propia o la de "en construccion". */
    public String vista() {
        return disponible() ? fxml : VISTA_PENDIENTE;
    }

    public static Optional<Modulo> desdeNombre(String nombre) {
        return Arrays.stream(values())
                .filter(m -> m.name().equals(nombre))
                .findFirst();
    }
}
