package com.businessanalyzer.ui;

import com.businessanalyzer.model.PermisoFGEM;

import java.util.Arrays;
import java.util.Optional;

/**
 * Modulos del sistema tal como aparecen en el menu lateral (en este orden).
 * <p>
 * Un modulo sin vista propia muestra una pantalla "en construccion" que indica la fase en
 * la que se implementa. Al construir un modulo basta con asignarle su archivo FXML aqui.
 * Cada modulo solo aparece en el menu si el usuario tiene el permiso indicado.
 */
public enum ModuloFGEM {

    DASHBOARD(SeccionFGEM.GENERAL, "Dashboard",
            "Resumen de los indicadores clave del negocio", "fth-grid", 1,
            PermisoFGEM.DASHBOARD_VER, "/fxml/dashboard-view.fxml"),

    EMPRESAS(SeccionFGEM.GESTION, "Empresas",
            "Registro de las empresas y negocios a analizar", "fth-briefcase", 3,
            PermisoFGEM.EMPRESAS_VER, "/fxml/empresas-view.fxml"),
    PRODUCTOS(SeccionFGEM.GESTION, "Productos y servicios",
            "Catálogo con costos, precios y márgenes", "fth-package", 4,
            PermisoFGEM.PRODUCTOS_VER, "/fxml/productos-view.fxml"),
    VENTAS_COMPRAS(SeccionFGEM.GESTION, "Ventas y compras",
            "Registro de las operaciones comerciales", "fth-shopping-cart", 5,
            PermisoFGEM.OPERACIONES_VER, "/fxml/operaciones-view.fxml"),
    FINANZAS(SeccionFGEM.GESTION, "Gestión financiera",
            "Ingresos, gastos y costos fijos y variables", "fth-dollar-sign", 6,
            PermisoFGEM.FINANZAS_VER, "/fxml/finanzas-view.fxml"),
    TESORERIA(SeccionFGEM.GESTION, "Tesorería",
            "Cuentas de dinero, cobros y pagos, flujo de efectivo y proyección de caja", "fth-credit-card", 22,
            PermisoFGEM.FINANZAS_VER, "/fxml/tesoreria-view.fxml"),
    PATRIMONIO(SeccionFGEM.GESTION, "Activos y deudas",
            "Bienes, deudas, balance general y patrimonio", "fth-layers", 14,
            PermisoFGEM.FINANZAS_VER, "/fxml/patrimonio-view.fxml"),

    GANADERIA(SeccionFGEM.AGRO, "Ganadería",
            "Hato, nacimientos, mortalidad, producción y valor del ganado", "fth-feather", 15,
            PermisoFGEM.AGRO_VER, "/fxml/ganaderia-view.fxml"),
    AGRICULTURA(SeccionFGEM.AGRO, "Agricultura",
            "Parcelas, campañas, costos, cosechas y rendimiento", "fth-sun", 16,
            PermisoFGEM.AGRO_VER, "/fxml/agricultura-view.fxml"),
    VISITAS(SeccionFGEM.AGRO, "Visitas de inspección",
            "Verificación en campo, compromisos, fotos e informe técnico", "fth-clipboard", 17,
            PermisoFGEM.AGRO_VER, "/fxml/visitas-view.fxml"),

    ANALISIS_FINANCIERO(SeccionFGEM.ANALISIS, "Análisis financiero",
            "Estado de resultados, márgenes, ratios y punto de equilibrio", "fth-pie-chart", 7,
            PermisoFGEM.ANALISIS_VER, "/fxml/analisis-financiero-view.fxml"),
    ESTADISTICA(SeccionFGEM.ANALISIS, "Análisis estadístico",
            "Tendencias, estacionalidad, dispersión y análisis ABC", "fth-bar-chart-2", 8,
            PermisoFGEM.ANALISIS_VER, "/fxml/estadistica-view.fxml"),
    PREDICCIONES(SeccionFGEM.ANALISIS, "Predicciones",
            "Proyección de ventas y resultados de los próximos meses", "fth-trending-up", 9,
            PermisoFGEM.ANALISIS_VER, "/fxml/predicciones-view.fxml"),
    ESCENARIOS(SeccionFGEM.ANALISIS, "Simulación de escenarios",
            "Qué pasaría si cambian los precios, costos o volúmenes", "fth-sliders", 10,
            PermisoFGEM.ANALISIS_VER, "/fxml/simulacion-view.fxml"),
    DIAGNOSTICO(SeccionFGEM.ANALISIS, "Diagnóstico empresarial",
            "Semáforo de riesgos y recomendaciones", "fth-activity", 11,
            PermisoFGEM.ANALISIS_VER, "/fxml/diagnostico-view.fxml"),
    VALORACION(SeccionFGEM.ANALISIS, "Valoración del negocio",
            "Cuánto vale el negocio por lo que tiene y por lo que gana", "fth-star", 19,
            PermisoFGEM.ANALISIS_VER, "/fxml/valoracion-view.fxml"),
    INFORMES(SeccionFGEM.ANALISIS, "Informes y reportes",
            "Reportes en PDF y exportación a Excel", "fth-file-text", 12,
            PermisoFGEM.INFORMES_GENERAR, "/fxml/informes-view.fxml"),

    USUARIOS(SeccionFGEM.SISTEMA, "Usuarios y permisos",
            "Cuentas, roles y control de acceso", "fth-users", 2,
            PermisoFGEM.USUARIOS_GESTIONAR, "/fxml/usuarios-view.fxml"),
    CONFIGURACION(SeccionFGEM.SISTEMA, "Configuración",
            "Preferencias y copias de seguridad", "fth-settings", 13,
            PermisoFGEM.CONFIGURACION_GESTIONAR, "/fxml/configuracion-view.fxml");

    /** Grupos del menu lateral. */
    public enum SeccionFGEM {
        GENERAL("General"),
        GESTION("Gestión"),
        AGRO("Agropecuario"),
        ANALISIS("Análisis"),
        SISTEMA("Sistema");

        private final String tituloFGEM;

        SeccionFGEM(String tituloFGEM) {
            this.tituloFGEM = tituloFGEM;
        }

        public String tituloFGEM() {
            return tituloFGEM;
        }
    }

    public static final String VISTA_PENDIENTE_FGEM = "/fxml/modulo-pendiente-view.fxml";

    private final SeccionFGEM seccionFGEM;
    private final String tituloFGEM;
    private final String descripcionFGEM;
    private final String iconoFGEM;
    private final int faseFGEM;
    private final PermisoFGEM permisoFGEM;
    private final String fxmlFGEM;

    ModuloFGEM(SeccionFGEM seccionFGEM, String tituloFGEM, String descripcionFGEM, String iconoFGEM, int faseFGEM,
           PermisoFGEM permisoFGEM, String fxmlFGEM) {
        this.seccionFGEM = seccionFGEM;
        this.tituloFGEM = tituloFGEM;
        this.descripcionFGEM = descripcionFGEM;
        this.iconoFGEM = iconoFGEM;
        this.faseFGEM = faseFGEM;
        this.permisoFGEM = permisoFGEM;
        this.fxmlFGEM = fxmlFGEM;
    }

    public SeccionFGEM seccionFGEM() {
        return seccionFGEM;
    }

    public String tituloFGEM() {
        return tituloFGEM;
    }

    public String descripcionFGEM() {
        return descripcionFGEM;
    }

    /** Codigo del icono Feather (Ikonli), por ejemplo {@code fth-grid}. */
    public String iconoFGEM() {
        return iconoFGEM;
    }

    /** Fase del proyecto en la que se implementa el modulo. */
    public int faseFGEM() {
        return faseFGEM;
    }

    /** Permiso necesario para ver el modulo. */
    public PermisoFGEM permisoFGEM() {
        return permisoFGEM;
    }

    /** Solo se muestra cuando la empresa activa es del sector agropecuario. */
    public boolean soloAgropecuarioFGEM() {
        return seccionFGEM == SeccionFGEM.AGRO;
    }

    public boolean disponibleFGEM() {
        return fxmlFGEM != null;
    }

    /** Ruta FXML a mostrar: la propia o la de "en construccion". */
    public String vistaFGEM() {
        return disponibleFGEM() ? fxmlFGEM : VISTA_PENDIENTE_FGEM;
    }

    public static Optional<ModuloFGEM> desdeNombreFGEM(String nombreFGEM) {
        return Arrays.stream(values())
                .filter(mFGEM -> mFGEM.name().equals(nombreFGEM))
                .findFirst();
    }
}
