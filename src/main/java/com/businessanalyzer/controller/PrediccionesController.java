package com.businessanalyzer.controller;

import com.businessanalyzer.analytics.prediccion.LecturaPrediccion;
import com.businessanalyzer.analytics.prediccion.MetodoPronostico;
import com.businessanalyzer.analytics.prediccion.Pronosticador;
import com.businessanalyzer.analytics.prediccion.Pronostico;
import com.businessanalyzer.analytics.prediccion.ResultadoPrediccion;
import com.businessanalyzer.config.AppContext;
import com.businessanalyzer.model.Empresa;
import com.businessanalyzer.model.Moneda;
import com.businessanalyzer.service.PrediccionService;
import com.businessanalyzer.ui.Errores;
import com.businessanalyzer.ui.Graficos;
import com.businessanalyzer.ui.Notificador;
import com.businessanalyzer.ui.Tareas;
import com.businessanalyzer.ui.TarjetaIndicador;
import com.businessanalyzer.ui.VistaModulo;
import com.businessanalyzer.util.Fechas;
import com.businessanalyzer.util.Montos;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.YearMonth;
import java.util.List;
import java.util.function.Function;

/** Módulo "Predicciones": proyección de ventas y utilidad neta con el método más preciso. */
public class PrediccionesController implements VistaModulo {

    private static final int MESES_HISTORIA_GRAFICO = 12;

    private final AppContext contexto;
    private Moneda moneda = Moneda.PEN;
    private long solicitudActual;

    private final TarjetaIndicador kpiVentas = new TarjetaIndicador("Ventas proyectadas", "fth-trending-up");
    private final TarjetaIndicador kpiCambio = new TarjetaIndicador("Cambio esperado", "fth-repeat");
    private final TarjetaIndicador kpiUtilidad = new TarjetaIndicador("Utilidad proyectada", "fth-dollar-sign");
    private final TarjetaIndicador kpiConfiabilidad = new TarjetaIndicador("Confiabilidad", "fth-check-circle");

    @FXML private ComboBox<Integer> cmbHorizonte;
    @FXML private ProgressIndicator progreso;
    @FXML private Label lblHistoria;
    @FXML private HBox indicadores;
    @FXML private Tab tabVentas;
    @FXML private Tab tabUtilidad;
    @FXML private Tab tabMetodos;

    public PrediccionesController(AppContext contexto) {
        this.contexto = contexto;
    }

    @FXML
    private void initialize() {
        indicadores.getChildren().addAll(kpiVentas, kpiCambio, kpiUtilidad, kpiConfiabilidad);
        cmbHorizonte.getItems().setAll(3, 6, 12);
        cmbHorizonte.setConverter(new StringConverter<>() {
            @Override
            public String toString(Integer meses) {
                return meses == null ? "" : "Próximos " + meses + " meses";
            }

            @Override
            public Integer fromString(String texto) {
                return null;
            }
        });
        cmbHorizonte.setValue(6);
        cmbHorizonte.valueProperty().addListener((o, a, n) -> cargar());
        tabMetodos.setContent(explicacionMetodos());
    }

    @Override
    public void alMostrar() {
        cargar();
    }

    private void cargar() {
        Empresa empresa = contexto.sesion().empresaActiva().orElse(null);
        if (empresa == null || cmbHorizonte.getValue() == null) {
            Label aviso = new Label("Selecciona una empresa en la barra superior para ver sus proyecciones.");
            aviso.getStyleClass().add("muted");
            tabVentas.setContent(new StackPane(aviso));
            tabUtilidad.setContent(new StackPane());
            return;
        }
        moneda = empresa.moneda();
        int horizonte = cmbHorizonte.getValue();
        progreso.setVisible(true);
        long numero = ++solicitudActual;
        Tareas.ejecutar(() -> contexto.predicciones().predecir(horizonte),
                resultado -> {
                    if (numero == solicitudActual) {
                        progreso.setVisible(false);
                        mostrar(resultado);
                    }
                },
                error -> {
                    if (numero == solicitudActual) {
                        progreso.setVisible(false);
                        Notificador.error(Errores.mensaje(error));
                    }
                });
    }

    private void mostrar(ResultadoPrediccion r) {
        lblHistoria.setText(r.meses().isEmpty() ? "Sin meses completos con ventas"
                : "Basado en " + r.meses().size() + (r.meses().size() == 1 ? " mes completo" : " meses completos")
                + " de historia (" + nombreMes(r.meses().get(0)) + " a " + nombreMes(r.meses().get(r.meses().size() - 1)) + ")");
        mostrarIndicadores(r);
        List<String> lectura = LecturaPrediccion.generar(r, moneda);
        contexto.mostrarFrase(com.businessanalyzer.ui.Modulo.PREDICCIONES, r.pronosticoVentas() == null || lectura.isEmpty() ? null : lectura.get(0));
        tabVentas.setContent(contenidoSerie(r, r.pronosticoVentas(), r.ventas(), lectura));
        tabUtilidad.setContent(contenidoSerie(r, r.pronosticoUtilidad(), r.utilidad(), null));
    }

    private void mostrarIndicadores(ResultadoPrediccion r) {
        Pronostico ventas = r.pronosticoVentas();
        if (!ventas.suficiente()) {
            for (TarjetaIndicador kpi : List.of(kpiVentas, kpiCambio, kpiUtilidad, kpiConfiabilidad)) {
                kpi.actualizar(TarjetaIndicador.SIN_VALOR, "Se necesitan " + Pronosticador.MINIMO_DATOS + " meses completos");
            }
            return;
        }
        YearMonth primero = r.primerMesProyectado(YearMonth.now());
        kpiVentas.actualizar(redondeado(ventas.total()), "Próximos " + r.horizonte() + " meses, desde " + nombreMes(primero));
        double anteriores = r.ventasUltimosMeses();
        if (anteriores > 0 && r.mesesComparados() == r.horizonte()) {
            double cambio = ventas.total() / anteriores - 1;
            kpiCambio.actualizar((cambio >= 0 ? "+" : "") + porcentaje(cambio),
                    "Últimos " + r.horizonte() + " meses: " + dinero(anteriores));
        } else {
            kpiCambio.actualizar(TarjetaIndicador.SIN_VALOR, "Historia más corta que el horizonte");
        }
        Pronostico utilidad = r.pronosticoUtilidad();
        kpiUtilidad.actualizar(redondeado(utilidad.total()), utilidad.total() >= 0 ? "Ganancia estimada del período"
                : "Pérdida estimada del período");
        kpiConfiabilidad.actualizar(ventas.confiabilidad().descripcion(),
                "Error típico " + porcentaje(ventas.errorRelativo()) + " · " + ventas.elegida().metodo().nombre());
    }

    // ------------------------------------------------------------------ contenido de cada serie

    private Node contenidoSerie(ResultadoPrediccion r, Pronostico p, double[] historia, List<String> lectura) {
        if (!p.suficiente()) {
            Label aviso = new Label("Se necesitan al menos " + Pronosticador.MINIMO_DATOS + " meses completos con ventas "
                    + "para proyectar (hay " + p.datos() + "). Sigue registrando tus operaciones y vuelve más adelante.");
            aviso.getStyleClass().add("muted");
            aviso.setWrapText(true);
            VBox caja = new VBox(aviso);
            caja.getStyleClass().addAll("card", "tab-contenido");
            return caja;
        }

        VBox tarjetaGrafico = tarjeta(p.serie() + ": historia y proyección");
        StackPane grafico = new StackPane(Graficos.visor(Graficos.pronostico(r.meses(), historia, p,
                MESES_HISTORIA_GRAFICO, moneda)));
        VBox.setVgrow(grafico, Priority.ALWAYS);
        grafico.setMinHeight(200);
        tarjetaGrafico.getChildren().add(grafico);
        VBox.setVgrow(tarjetaGrafico, Priority.ALWAYS);

        VBox izquierda = new VBox(12, tarjetaGrafico);
        if (lectura != null) {
            VBox tarjetaLectura = tarjeta(null);
            for (String frase : lectura) {
                Label punto = new Label("•  " + frase);
                punto.setWrapText(true);
                punto.setMinHeight(Region.USE_PREF_SIZE);
                punto.getStyleClass().add("lectura");
                tarjetaLectura.getChildren().add(punto);
            }
            ScrollPane desplazable = new ScrollPane(tarjetaLectura);
            desplazable.setFitToWidth(true);
            desplazable.getStyleClass().add("dialog-scroll");
            desplazable.setPrefHeight(150);
            desplazable.setMinHeight(110);
            izquierda.getChildren().add(desplazable);
        }
        HBox.setHgrow(izquierda, Priority.ALWAYS);

        VBox derecha = tarjeta("Método elegido");
        Label metodo = new Label(p.elegida().metodo().nombre());
        metodo.getStyleClass().add("total-valor-destacado");
        Label explicacion = new Label(p.elegida().metodo().explicacion());
        explicacion.getStyleClass().add("muted");
        explicacion.setWrapText(true);
        explicacion.setMinHeight(Region.USE_PREF_SIZE);
        derecha.getChildren().addAll(metodo, explicacion, subtitulo("Proyección mensual"), tablaPuntos(r, p),
                subtitulo("Error medio de cada método con tus datos"), tablaMetodos(p));
        // El panel derecho se desplaza si no entra completo (horizontes largos o ventanas pequeñas).
        ScrollPane panelDerecho = new ScrollPane(derecha);
        panelDerecho.setFitToWidth(true);
        panelDerecho.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        panelDerecho.getStyleClass().add("dialog-scroll");
        panelDerecho.setPrefWidth(450);
        panelDerecho.setMinWidth(400);

        HBox contenido = new HBox(16, izquierda, panelDerecho);
        contenido.getStyleClass().add("tab-contenido");
        return contenido;
    }

    private TableView<Pronostico.Punto> tablaPuntos(ResultadoPrediccion r, Pronostico p) {
        YearMonth primero = r.primerMesProyectado(YearMonth.now());
        TableView<Pronostico.Punto> tabla = new TableView<>(FXCollections.observableArrayList(p.puntos()));
        TableColumn<Pronostico.Punto, String> mes = columna("Mes", punto -> mesCorto(primero.plusMonths(punto.h() - 1L)), 80, false);
        mes.setMinWidth(70);
        tabla.getColumns().add(mes);
        tabla.getColumns().add(columna("Proyección", punto -> entero(punto.valor()), 95, true));
        tabla.getColumns().add(columna("Rango 95 %", punto -> entero(punto.inferior()) + " – "
                + entero(punto.superior()).replace(moneda.simbolo() + " ", ""), 150, true));
        tabla.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        tabla.getStyleClass().add("tabla-compacta");
        tabla.setFixedCellSize(32);
        tabla.setPrefHeight(Math.min(32 * p.puntos().size() + 42, 32 * 6 + 42));
        tabla.setMinHeight(Region.USE_PREF_SIZE);
        return tabla;
    }

    private TableView<Pronostico.Evaluacion> tablaMetodos(Pronostico p) {
        TableView<Pronostico.Evaluacion> tabla = new TableView<>(FXCollections.observableArrayList(p.evaluaciones()));
        tabla.getColumns().add(columna("Método", e -> (e == p.elegida() ? "✓ " : "   ") + e.metodo().nombre(), 230, false));
        tabla.getColumns().add(columna("Error medio", e -> dinero(e.mae()), 100, true));
        tabla.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        tabla.getStyleClass().add("tabla-compacta");
        tabla.setFixedCellSize(32);
        tabla.setPrefHeight(32 * p.evaluaciones().size() + 42);
        tabla.setMinHeight(Region.USE_PREF_SIZE);
        tabla.setTooltip(new javafx.scene.control.Tooltip("Error medio al pronosticar cada uno de los "
                + p.elegida().mesesEvaluados() + " meses validados con los datos anteriores a él"));
        return tabla;
    }

    /** Monto sin decimales: "S/ 29,518". */
    private String entero(double valor) {
        return moneda.simbolo() + " " + new java.text.DecimalFormat("#,##0",
                java.text.DecimalFormatSymbols.getInstance(java.util.Locale.US)).format(Math.round(valor));
    }

    /** "nov 26". */
    private static String mesCorto(YearMonth mes) {
        return nombreMes(mes).substring(0, 3) + " " + String.valueOf(mes.getYear()).substring(2);
    }

    /** Pestaña "Cómo se calcula": método por método y la forma de elegir. */
    private Node explicacionMetodos() {
        VBox caja = tarjeta("Cómo se proyecta");
        caja.getChildren().add(parrafo("Se usan solo meses completos (el mes en curso es el primero que se proyecta) y "
                + "hasta " + PrediccionService.MESES_HISTORIA + " meses de historia."));
        for (MetodoPronostico metodo : Pronosticador.METODOS) {
            Label nombre = new Label(metodo.nombre());
            nombre.getStyleClass().add("step-title");
            caja.getChildren().addAll(nombre, parrafo(metodo.explicacion()));
        }
        caja.getChildren().addAll(
                subtitulo("Cómo se elige el método"),
                parrafo("Cada método \"predice\" cada mes pasado usando solo los meses anteriores a él, y se compara con lo "
                        + "que realmente se vendió. Se elige el que tuvo el menor error medio. Así el sistema usa el método "
                        + "que mejor funciona con los datos de tu negocio, no uno fijo."),
                subtitulo("Rango probable"),
                parrafo("Es un intervalo aproximado al 95 %: pronóstico ± 1.96 × error histórico × √(meses de distancia). "
                        + "Se ensancha cuanto más lejos está el mes y cuanto más se equivocó el método en el pasado."),
                subtitulo("Confiabilidad"),
                parrafo("Alta: error típico menor al 10 % de una venta mensual. Media: hasta 25 %. Baja: mayor, o menos de "
                        + "3 meses para validar."),
                subtitulo("Limitaciones"),
                parrafo("El método de Holt-Winters, que capta la estacionalidad anual (por ejemplo, campañas de diciembre), "
                        + "necesita al menos 24 meses de historia y se incorporará cuando haya datos suficientes. Ningún "
                        + "pronóstico anticipa cambios bruscos del mercado."));
        ScrollPane desplazable = new ScrollPane(caja);
        desplazable.setFitToWidth(true);
        desplazable.getStyleClass().addAll("dialog-scroll", "tab-contenido");
        return desplazable;
    }

    // ------------------------------------------------------------------ utilidades

    private static VBox tarjeta(String titulo) {
        VBox caja = new VBox(8);
        caja.getStyleClass().add("card");
        if (titulo != null) {
            Label label = new Label(titulo);
            label.getStyleClass().add("card-title");
            caja.getChildren().add(label);
        }
        return caja;
    }

    private static Label subtitulo(String texto) {
        Label label = new Label(texto);
        label.getStyleClass().add("step-title");
        label.setPadding(new javafx.geometry.Insets(6, 0, 0, 0));
        return label;
    }

    private static Label parrafo(String texto) {
        Label label = new Label(texto);
        label.getStyleClass().add("muted");
        label.setWrapText(true);
        label.setMinHeight(Region.USE_PREF_SIZE);
        return label;
    }

    private static <T> TableColumn<T, String> columna(String titulo, Function<T, String> valor, double ancho,
                                                      boolean numero) {
        TableColumn<T, String> columna = new TableColumn<>(titulo);
        columna.setCellValueFactory(c -> new ReadOnlyStringWrapper(valor.apply(c.getValue())));
        columna.setPrefWidth(ancho);
        columna.setSortable(false);
        if (numero) {
            columna.getStyleClass().add("col-numero");
        }
        return columna;
    }

    /** Las proyecciones son aproximadas: en las tarjetas se muestran sin céntimos. */
    private String redondeado(double valor) {
        return Double.isNaN(valor) ? "—" : Montos.formatear(BigDecimal.valueOf(valor).setScale(0, RoundingMode.HALF_UP), moneda)
                .replaceAll("\\.00$", "");
    }

    private String dinero(double valor) {
        return Double.isNaN(valor) ? "—" : Montos.formatear(BigDecimal.valueOf(valor).setScale(2, RoundingMode.HALF_UP), moneda);
    }

    private static String porcentaje(double fraccion) {
        return Double.isNaN(fraccion) ? "—" : Montos.porcentaje(BigDecimal.valueOf(fraccion));
    }

    private static String nombreMes(YearMonth mes) {
        return Fechas.nombreMes(mes.getMonthValue()).toLowerCase() + " " + mes.getYear();
    }
}
