package com.businessanalyzer.controller;

import com.businessanalyzer.analytics.AnalisisFinanciero;
import com.businessanalyzer.analytics.EstadoResultados;
import com.businessanalyzer.analytics.Periodo;
import com.businessanalyzer.analytics.PuntoEquilibrio;
import com.businessanalyzer.analytics.RentabilidadProducto;
import com.businessanalyzer.analytics.Variacion;
import com.businessanalyzer.analytics.diagnostico.Diagnostico;
import com.businessanalyzer.analytics.diagnostico.Hallazgo;
import com.businessanalyzer.analytics.diagnostico.Semaforo;
import com.businessanalyzer.analytics.prediccion.Pronostico;
import com.businessanalyzer.analytics.prediccion.ResultadoPrediccion;
import com.businessanalyzer.config.AppContext;
import com.businessanalyzer.model.Empresa;
import com.businessanalyzer.model.Moneda;
import com.businessanalyzer.model.Permiso;
import com.businessanalyzer.ui.Errores;
import com.businessanalyzer.ui.Graficos;
import com.businessanalyzer.service.TesoreriaService;
import com.businessanalyzer.ui.Modulo;
import com.businessanalyzer.ui.Tareas;
import com.businessanalyzer.ui.TarjetaIndicador;
import com.businessanalyzer.ui.VistaModulo;
import com.businessanalyzer.util.Fechas;
import com.businessanalyzer.util.Montos;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import org.kordamp.ikonli.javafx.FontIcon;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

/**
 * Panel principal: lo más importante del negocio de un vistazo. Indicadores del mes en curso
 * (comparados con los mismos días del mes anterior), la evolución de los últimos 12 meses, la salud
 * del negocio según el diagnóstico, los productos que más aportan y la proyección del próximo mes.
 * Cada tarjeta lleva al módulo donde se ve el detalle.
 */
public class DashboardController implements VistaModulo {

    private static final int MAX_RECOMENDACIONES = 3;

    private final AppContext contexto;
    private long solicitud;

    @FXML private HBox bannerEmpresa;
    @FXML private FontIcon iconoBanner;
    @FXML private Label lblBannerTitulo;
    @FXML private Label lblBannerDetalle;
    @FXML private Label lblPeriodo;
    @FXML private HBox indicadores;
    @FXML private VBox contenido;
    @FXML private StackPane graficoEvolucion;
    @FXML private VBox salud;
    @FXML private Label lblTituloProductos;
    @FXML private StackPane graficoProductos;
    @FXML private VBox proximo;
    @FXML private Button btnAnalisis;
    @FXML private Button btnDiagnostico;
    @FXML private Button btnVentas;
    @FXML private Button btnPredicciones;

    private final TarjetaIndicador kpiVentas = new TarjetaIndicador("Ventas del mes", "fth-shopping-bag");
    private final TarjetaIndicador kpiUtilidad = new TarjetaIndicador("Utilidad neta del mes", "fth-dollar-sign");
    private final TarjetaIndicador kpiMargen = new TarjetaIndicador("Margen neto", "fth-percent");
    private final TarjetaIndicador kpiEquilibrio = new TarjetaIndicador("Punto de equilibrio", "fth-target");
    private final TarjetaIndicador kpiDinero = new TarjetaIndicador("Dinero disponible", "fth-credit-card");

    /** Lo que muestra el panel; cada parte puede faltar (sin permiso o sin datos). */
    private record Panel(AnalisisFinanciero analisis, Diagnostico diagnostico, ResultadoPrediccion prediccion,
                         String errorDiagnostico, String errorPrediccion, TesoreriaService.Resumen tesoreria) {
    }

    public DashboardController(AppContext contexto) {
        this.contexto = contexto;
    }

    @FXML
    private void initialize() {
        indicadores.getChildren().addAll(kpiVentas, kpiUtilidad, kpiMargen, kpiEquilibrio, kpiDinero);
        kpiDinero.setOnMouseClicked(e -> contexto.abrirModulo(Modulo.TESORERIA));
        kpiDinero.setStyle("-fx-cursor: hand;");
        Tooltip.install(kpiDinero, new Tooltip("Abrir «Tesorería»"));
        boolean analisis = contexto.sesion().tienePermiso(Permiso.ANALISIS_VER);
        for (Button b : List.of(btnAnalisis, btnDiagnostico, btnPredicciones)) {
            b.setVisible(analisis);
        }
        btnVentas.setVisible(contexto.sesion().tienePermiso(Permiso.OPERACIONES_VER));
    }

    @Override
    public void alMostrar() {
        Optional<Empresa> empresa = contexto.sesion().empresaActiva();
        mostrarBanner(empresa.orElse(null));
        if (empresa.isEmpty()) {
            limpiar("Elige o registra una empresa para ver sus indicadores.");
            return;
        }
        if (!contexto.sesion().tienePermiso(Permiso.ANALISIS_VER)) {
            limpiar("Tu rol no incluye los análisis. Registra ventas, compras y gastos desde el menú.");
            return;
        }
        Moneda moneda = empresa.get().moneda();
        LocalDate hoy = LocalDate.now();
        Periodo mes = new Periodo(hoy.withDayOfMonth(1), hoy);
        lblPeriodo.setText("Mes en curso: " + mes.descripcion());
        long numero = ++solicitud;
        Tareas.ejecutar(() -> cargar(mes), panel -> {
            if (numero == solicitud) {
                mostrar(panel, moneda);
            }
        }, error -> {
            if (numero == solicitud) {
                limpiar(Errores.mensaje(error));
            }
        });
    }

    private Panel cargar(Periodo mes) {
        AnalisisFinanciero analisis = contexto.analisis().analizar(mes.desde(), mes.hasta());
        Diagnostico diagnostico = null;
        String errorDiagnostico = null;
        try {
            diagnostico = contexto.diagnostico().diagnosticar();
        } catch (RuntimeException e) {
            errorDiagnostico = Errores.mensaje(e);
        }
        ResultadoPrediccion prediccion = null;
        String errorPrediccion = null;
        try {
            prediccion = contexto.predicciones().predecir(3);
        } catch (RuntimeException e) {
            errorPrediccion = Errores.mensaje(e);
        }
        TesoreriaService.Resumen tesoreria = null;
        try {
            tesoreria = contexto.tesoreria().resumen();
        } catch (RuntimeException e) {
            // Sin permiso de finanzas o sin cuentas: la tarjeta queda vacía.
        }
        return new Panel(analisis, diagnostico, prediccion, errorDiagnostico, errorPrediccion, tesoreria);
    }

    // ------------------------------------------------------------------ presentación

    private void mostrar(Panel p, Moneda moneda) {
        contenido.setVisible(true);
        contenido.setManaged(true);
        mostrarIndicadores(p.analisis(), moneda);
        TesoreriaService.Resumen t = p.tesoreria();
        if (t == null) {
            kpiDinero.actualizar(TarjetaIndicador.SIN_VALOR, "");
        } else {
            kpiDinero.actualizar(Montos.formatear(t.disponible(), moneda), t.cuentasNegativas() > 0
                    ? "¡Hay cuentas en negativo!"
                    : t.porCobrarVencido().signum() > 0
                    ? "Cobros vencidos: " + Montos.formatear(t.porCobrarVencido(), moneda)
                    : "Por cobrar: " + Montos.formatear(t.porCobrar(), moneda));
        }
        graficoEvolucion.getChildren().setAll(p.analisis().evolucion().isEmpty()
                ? vacio("Aún no hay meses con movimientos.")
                : Graficos.visor(Graficos.evolucionMensual(p.analisis().evolucion(), moneda)));
        mostrarProductos(p.analisis(), moneda);
        mostrarSalud(p.diagnostico(), p.errorDiagnostico());
        mostrarProximo(p.prediccion(), p.errorPrediccion(), moneda);
    }

    private void mostrarIndicadores(AnalisisFinanciero a, Moneda moneda) {
        EstadoResultados e = a.actual();
        EstadoResultados anterior = a.anterior();
        String comparado = " vs. " + a.periodoAnterior().descripcion().replace(" " + a.periodoAnterior().hasta().getYear(), "");
        kpiVentas.actualizar(Montos.formatear(e.ventasNetas(), moneda),
                variacion(e.ventasNetas(), anterior.ventasNetas()) + comparado);
        kpiUtilidad.actualizar(Montos.formatear(e.utilidadNeta(), moneda), e.utilidadNeta().signum() >= 0
                ? variacion(e.utilidadNeta(), anterior.utilidadNeta()) + comparado
                : "El mes va con pérdida");
        kpiMargen.actualizar(Montos.porcentaje(e.margenNeto()), "Margen bruto " + Montos.porcentaje(e.margenBruto()));
        PuntoEquilibrio pe = a.equilibrio();
        if (pe.ventasEquilibrio() == null) {
            kpiEquilibrio.actualizar("No alcanzable", e.ventasNetas().signum() == 0
                    ? "Sin ventas este mes" : "Las ventas no cubren sus costos variables");
        } else {
            kpiEquilibrio.actualizar(Montos.formatear(pe.ventasEquilibrio(), moneda), pe.alcanzado()
                    ? "Alcanzado ✓"
                    : "Faltan " + Montos.formatear(pe.ventasFaltantes(), moneda) + " en ventas");
        }
    }

    private static String variacion(BigDecimal actual, BigDecimal anterior) {
        Variacion v = new Variacion(actual, anterior);
        return v.relativa() == null ? "Sin comparación" : (v.relativa().signum() >= 0 ? "▲ " : "▼ ")
                + Montos.porcentaje(v.relativa().abs());
    }

    private void mostrarProductos(AnalisisFinanciero a, Moneda moneda) {
        List<RentabilidadProducto> productos = a.productos();
        if (productos.isEmpty()) {
            // Al empezar el mes puede no haber ventas: se muestran los de los últimos 12 meses.
            lblTituloProductos.setText("Productos que más aportan al margen");
            graficoProductos.getChildren().setAll(vacio("Sin ventas en el mes en curso."));
            return;
        }
        lblTituloProductos.setText("Productos que más aportan al margen este mes");
        graficoProductos.getChildren().setAll(Graficos.visor(Graficos.rentabilidadProductos(productos, 5, moneda)));
    }

    private void mostrarSalud(Diagnostico d, String error) {
        salud.getChildren().clear();
        if (d == null || !d.evaluado()) {
            salud.getChildren().add(texto(error != null ? error
                    : d == null ? "Sin datos." : String.join(" ", d.resumen()), "muted"));
            return;
        }
        Label puntaje = new Label(String.valueOf(d.puntaje()));
        puntaje.getStyleClass().addAll("puntaje-salud", clase(d.nivel().color()));
        Label sobre = texto("/100", "muted");
        sobre.setMinWidth(Label.USE_PREF_SIZE);
        Label nivel = new Label(d.nivel().descripcion());
        nivel.getStyleClass().addAll("nivel-salud", clase(d.nivel().color()));
        nivel.setMinWidth(Label.USE_PREF_SIZE);
        HBox cabecera = new HBox(8, puntaje, sobre, nivel);
        cabecera.setAlignment(Pos.BASELINE_LEFT);
        salud.getChildren().add(cabecera);
        salud.getChildren().add(texto("Evaluado: " + d.periodo().descripcion() + ".", "hint"));

        List<Hallazgo> pendientes = d.recomendaciones();
        if (pendientes.isEmpty()) {
            salud.getChildren().add(texto("Todos los indicadores están en verde. ¡Buen trabajo!", "lectura"));
            return;
        }
        salud.getChildren().add(texto("Atender primero", "info-key"));
        for (Hallazgo h : pendientes.subList(0, Math.min(MAX_RECOMENDACIONES, pendientes.size()))) {
            Circle punto = new Circle(5);
            punto.getStyleClass().addAll("punto-semaforo", clase(h.semaforo()));
            Label nombre = texto(h.tipo().nombre(), "lectura");
            nombre.setWrapText(true);
            HBox.setHgrow(nombre, javafx.scene.layout.Priority.ALWAYS);
            nombre.setMaxWidth(Double.MAX_VALUE);
            Label valor = texto(h.valor(), clase(h.semaforo()));
            valor.setMinWidth(Label.USE_PREF_SIZE);
            HBox fila = new HBox(8, punto, nombre, valor);
            fila.setAlignment(Pos.CENTER_LEFT);
            salud.getChildren().add(fila);
        }
    }

    private void mostrarProximo(ResultadoPrediccion r, String error, Moneda moneda) {
        proximo.getChildren().clear();
        Pronostico ventas = r == null ? null : r.pronosticoVentas();
        if (ventas == null || ventas.puntos().isEmpty()) {
            proximo.getChildren().add(texto(error != null ? error
                    : "Se necesitan al menos unos meses completos de ventas para proyectar.", "muted"));
            return;
        }
        YearMonth siguiente = r.primerMesProyectado(YearMonth.now());
        Pronostico.Punto p = ventas.puntos().get(0);
        proximo.getChildren().add(texto("Ventas esperadas en " + Fechas.nombreMes(siguiente.getMonthValue()).toLowerCase()
                + " " + siguiente.getYear(), "info-key"));
        proximo.getChildren().add(texto(aproximado(p.valor(), moneda), "kpi-value"));
        proximo.getChildren().add(texto("Rango probable: " + aproximado(Math.max(0, p.inferior()), moneda).substring(2)
                + " a " + aproximado(p.superior(), moneda).substring(2) + ".", "muted"));
        Pronostico utilidad = r.pronosticoUtilidad();
        if (utilidad != null && !utilidad.puntos().isEmpty()) {
            double u = utilidad.puntos().get(0).valor();
            proximo.getChildren().add(texto((u >= 0 ? "Utilidad esperada: " : "Pérdida esperada: ")
                    + aproximado(Math.abs(u), moneda) + ".", u >= 0 ? "lectura" : "margen-negativo"));
        }
        proximo.getChildren().add(texto("Confiabilidad " + ventas.confiabilidad().descripcion().toLowerCase()
                + " · método: " + ventas.elegida().metodo().nombre().toLowerCase() + ".", "hint"));
    }

    private void mostrarBanner(Empresa e) {
        bannerEmpresa.getStyleClass().remove("banner-aviso");
        if (e != null) {
            iconoBanner.setIconLiteral("fth-briefcase");
            lblBannerTitulo.setText(e.nombreVisible());
            lblBannerDetalle.setText(e.sector().descripcion() + "   ·   " + e.tamano().descripcion()
                    + "   ·   " + e.moneda().nombre() + " (" + e.moneda().simbolo() + ")");
            return;
        }
        bannerEmpresa.getStyleClass().add("banner-aviso");
        iconoBanner.setIconLiteral("fth-alert-circle");
        lblBannerTitulo.setText("No hay una empresa activa");
        lblBannerDetalle.setText(contexto.sesion().tienePermiso(Permiso.EMPRESAS_GESTIONAR)
                ? "Registra tu primera empresa en el módulo Empresas (o crea la «Bodega Demo» para probar) "
                + "o elige una en el selector de la barra superior."
                : "Pide a un administrador que te asigne una empresa para empezar a trabajar.");
    }

    private void limpiar(String mensaje) {
        lblPeriodo.setText("");
        for (TarjetaIndicador t : List.of(kpiVentas, kpiUtilidad, kpiMargen, kpiEquilibrio, kpiDinero)) {
            t.actualizar(TarjetaIndicador.SIN_VALOR, mensaje.length() > 40 ? "" : mensaje);
        }
        contenido.setVisible(false);
        contenido.setManaged(false);
        if (!mensaje.isBlank()) {
            lblBannerDetalle.setText(lblBannerDetalle.getText().isBlank() ? mensaje
                    : lblBannerDetalle.getText() + "\n" + mensaje);
        }
    }

    // ------------------------------------------------------------------ navegación

    @FXML
    private void irAnalisis() {
        contexto.abrirModulo(Modulo.ANALISIS_FINANCIERO);
    }

    @FXML
    private void irDiagnostico() {
        contexto.abrirModulo(Modulo.DIAGNOSTICO);
    }

    @FXML
    private void irVentas() {
        contexto.abrirModulo(Modulo.VENTAS_COMPRAS);
    }

    @FXML
    private void irPredicciones() {
        contexto.abrirModulo(Modulo.PREDICCIONES);
    }

    // ------------------------------------------------------------------ utilidades

    /** Las proyecciones son aproximadas: se muestran en soles enteros. */
    private static String aproximado(double valor, Moneda moneda) {
        return "≈ " + Montos.formatear(BigDecimal.valueOf(valor).setScale(0, java.math.RoundingMode.HALF_UP), moneda)
                .replaceAll("\\.00$", "");
    }

    private static Node vacio(String mensaje) {
        return texto(mensaje, "muted");
    }

    private static Label texto(String texto, String clase) {
        Label l = new Label(texto);
        l.getStyleClass().add(clase);
        l.setWrapText(true);
        return l;
    }

    private static String clase(Semaforo semaforo) {
        return switch (semaforo) {
            case VERDE -> "semaforo-verde";
            case AMARILLO -> "semaforo-amarillo";
            case ROJO -> "semaforo-rojo";
            case SIN_DATOS -> "semaforo-sin-datos";
        };
    }
}
