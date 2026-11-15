package com.businessanalyzer.controller;

import com.businessanalyzer.analytics.valoracion.SupuestosValoracion;
import com.businessanalyzer.analytics.valoracion.ValoracionNegocio;
import com.businessanalyzer.config.AppContext;
import com.businessanalyzer.exception.ValidacionException;
import com.businessanalyzer.model.Empresa;
import com.businessanalyzer.model.Moneda;
import com.businessanalyzer.model.Permiso;
import com.businessanalyzer.service.ValoracionService;
import com.businessanalyzer.ui.Dialogos;
import com.businessanalyzer.ui.Errores;
import com.businessanalyzer.ui.Graficos;
import com.businessanalyzer.ui.Notificador;
import com.businessanalyzer.ui.Tareas;
import com.businessanalyzer.ui.TarjetaIndicador;
import com.businessanalyzer.ui.VistaModulo;
import com.businessanalyzer.util.Montos;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextField;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

import java.awt.Desktop;
import java.io.File;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Módulo "Valoración del negocio": cuánto vale por patrimonio, por capitalización y por flujos descontados. */
public class ValoracionController implements VistaModulo {

    private static final String CLAVE_CARPETA = "informes.carpeta";

    private final AppContext contexto;
    private Moneda moneda = Moneda.PEN;
    private SupuestosValoracion supuestosActuales;
    private BigDecimal crecimientoSugerido = BigDecimal.ZERO;
    private long solicitud;

    private final TarjetaIndicador kpiReferencia = new TarjetaIndicador("Valor de referencia", "fth-star");
    private final TarjetaIndicador kpiPatrimonial = new TarjetaIndicador("Valor patrimonial", "fth-layers");
    private final TarjetaIndicador kpiRentabilidad = new TarjetaIndicador("Valor por rentabilidad", "fth-trending-up");
    private final TarjetaIndicador kpiFondo = new TarjetaIndicador("Fondo de comercio", "fth-award");

    @FXML private TextField txtTasa;
    @FXML private TextField txtCrecimiento;
    @FXML private ProgressIndicator progreso;
    @FXML private Button btnExportar;
    @FXML private Label lblSupuestos;
    @FXML private HBox indicadores;
    @FXML private GridPane gridMetodos;
    @FXML private StackPane graficoMetodos;
    @FXML private GridPane gridProyeccion;
    @FXML private GridPane gridSensibilidad;
    @FXML private VBox lectura;

    public ValoracionController(AppContext contexto) {
        this.contexto = contexto;
    }

    @FXML
    private void initialize() {
        indicadores.getChildren().addAll(kpiReferencia, kpiPatrimonial, kpiRentabilidad, kpiFondo);
        ColumnConstraints c1 = new ColumnConstraints();
        c1.setMinWidth(230);
        ColumnConstraints c2 = new ColumnConstraints();
        c2.setHgrow(Priority.ALWAYS);
        gridMetodos.getColumnConstraints().setAll(c1, c2);
        boolean informes = contexto.sesion().tienePermiso(Permiso.INFORMES_GENERAR);
        btnExportar.setVisible(informes);
        btnExportar.setManaged(informes);
        txtTasa.setOnAction(e -> calcular());
        txtCrecimiento.setOnAction(e -> calcular());
    }

    @Override
    public void alMostrar() {
        Empresa empresa = contexto.sesion().empresaActiva().orElse(null);
        if (empresa == null) {
            limpiar("Selecciona una empresa en la barra superior.");
            return;
        }
        moneda = empresa.moneda();
        try {
            SupuestosValoracion s = contexto.valoracion().supuestos();
            mostrarSupuestos(s);
            ejecutar(s);
        } catch (RuntimeException e) {
            limpiar(Errores.mensaje(e));
        }
    }

    // ------------------------------------------------------------------ acciones

    @FXML
    private void calcular() {
        SupuestosValoracion s;
        try {
            BigDecimal tasa = porcentaje(txtTasa.getText(), "Tasa de descuento");
            BigDecimal crecimiento = porcentaje(txtCrecimiento.getText(), "Crecimiento anual");
            s = SupuestosValoracion.con(tasa, crecimiento);
            contexto.valoracion().guardarSupuestos(tasa, crecimiento);
        } catch (RuntimeException e) {
            Notificador.error(Errores.mensaje(e));
            return;
        }
        ejecutar(s);
    }

    @FXML
    private void usarSugeridos() {
        try {
            contexto.valoracion().guardarSupuestos(SupuestosValoracion.TASA_PREDETERMINADA, null);
        } catch (RuntimeException e) {
            Notificador.error(Errores.mensaje(e));
            return;
        }
        alMostrar();
    }

    private void ejecutar(SupuestosValoracion s) {
        progreso.setVisible(true);
        long numero = ++solicitud;
        Tareas.ejecutar(() -> contexto.valoracion().valorar(s),
                r -> {
                    if (numero == solicitud) {
                        progreso.setVisible(false);
                        supuestosActuales = s;
                        crecimientoSugerido = r.crecimientoSugerido();
                        mostrar(r);
                    }
                },
                error -> {
                    if (numero == solicitud) {
                        progreso.setVisible(false);
                        Notificador.error(Errores.mensaje(error));
                    }
                });
    }

    @FXML
    private void exportar() {
        Empresa empresa = contexto.sesion().empresaActiva().orElse(null);
        if (empresa == null || supuestosActuales == null) {
            return;
        }
        FileChooser selector = new FileChooser();
        selector.setTitle("Guardar informe de valoración");
        selector.getExtensionFilters().add(new FileChooser.ExtensionFilter("Documento PDF (*.pdf)", "*.pdf"));
        String limpio = java.text.Normalizer.normalize(empresa.nombreVisible(), java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "").replaceAll("[^A-Za-z0-9]+", "-").replaceAll("(^-|-$)", "");
        selector.setInitialFileName("Valoracion-" + limpio + "-" + LocalDate.now() + ".pdf");
        String guardada = contexto.configuracion().obtener(CLAVE_CARPETA, null);
        for (Path carpeta : new Path[]{guardada == null ? null : Path.of(guardada),
                Path.of(System.getProperty("user.home"), "Documents"), Path.of(System.getProperty("user.home"))}) {
            if (carpeta != null && Files.isDirectory(carpeta)) {
                selector.setInitialDirectory(carpeta.toFile());
                break;
            }
        }
        File archivo = selector.showSaveDialog(btnExportar.getScene().getWindow());
        if (archivo == null) {
            return;
        }
        SupuestosValoracion s = supuestosActuales;
        progreso.setVisible(true);
        btnExportar.setDisable(true);
        Tareas.ejecutar(() -> contexto.valoracion().exportar(s, archivo.toPath()),
                generado -> {
                    progreso.setVisible(false);
                    btnExportar.setDisable(false);
                    try {
                        contexto.configuracion().guardar(CLAVE_CARPETA, generado.getParent().toString());
                    } catch (RuntimeException e) {
                        // No poder recordar la carpeta no impide seguir.
                    }
                    if (Dialogos.confirmar(btnExportar.getScene().getWindow(), "Informe generado",
                            "Se guardó «" + generado.getFileName() + "».\n\n¿Quieres abrirlo ahora?")) {
                        abrir(generado);
                    }
                },
                error -> {
                    progreso.setVisible(false);
                    btnExportar.setDisable(false);
                    Notificador.error(Errores.mensaje(error));
                });
    }

    private void abrir(Path ruta) {
        if (!Desktop.isDesktopSupported() || !Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
            Notificador.advertencia("El sistema no permite abrir archivos desde la aplicación. Ruta: " + ruta);
            return;
        }
        Tareas.ejecutar(() -> {
            Desktop.getDesktop().open(ruta.toFile());
            return null;
        }, ok -> {
        }, error -> Notificador.error("No se pudo abrir el archivo: " + Errores.mensaje(error)));
    }

    // ------------------------------------------------------------------ presentación

    private void mostrarSupuestos(SupuestosValoracion s) {
        txtTasa.setText(numero(s.tasaDescuento()));
        txtCrecimiento.setText(numero(s.crecimiento()));
    }

    private void mostrar(ValoracionService.Resultado r) {
        ValoracionNegocio v = r.valoracion();
        lblSupuestos.setText("Crecimiento sugerido por la tendencia de las ventas del último año: "
                + Montos.porcentaje(crecimientoSugerido) + ". Después de " + v.supuestos().anios()
                + " años se supone un crecimiento de " + Montos.porcentaje(v.supuestos().crecimientoLargoPlazo())
                + " anual." + (r.periodoResultados() == null ? " Aún no hay ventas: solo se calcula el valor patrimonial."
                : " Utilidad de " + r.periodoResultados().descripcion() + "."));

        kpiReferencia.actualizar(Montos.formatear(v.valorReferencia(), moneda),
                v.base() == ValoracionNegocio.Base.RENTABILIDAD ? "Por lo que gana" : "Por lo que tiene");
        kpiPatrimonial.actualizar(Montos.formatear(v.patrimonial(), moneda), "Activos − deudas");
        kpiRentabilidad.actualizar(v.valorRentabilidad() == null ? TarjetaIndicador.SIN_VALOR
                : Montos.formatear(v.valorRentabilidad(), moneda), "Utilidad anual " + Montos.formatear(v.utilidadAnual(), moneda));
        kpiFondo.actualizar(v.fondoComercio() == null ? TarjetaIndicador.SIN_VALOR : Montos.formatear(v.fondoComercio(), moneda),
                "Rentabilidad − patrimonio");

        gridMetodos.getChildren().clear();
        int fila = 0;
        fila = fila(gridMetodos, fila, "Patrimonial (activos − deudas)", Montos.formatear(v.patrimonial(), moneda), false);
        fila = fila(gridMetodos, fila, "Capitalización de utilidades", v.capitalizacion() == null ? "No aplica (sin utilidad)"
                : Montos.formatear(v.capitalizacion(), moneda), false);
        fila = fila(gridMetodos, fila, "Flujos descontados (" + v.supuestos().anios() + " años)",
                v.flujosDescontados() == null ? "No aplica (sin flujo positivo)" : Montos.formatear(v.flujosDescontados(), moneda), false);
        fila = fila(gridMetodos, fila, "Rango", Montos.formatear(v.minimo(), moneda) + "  a  " + Montos.formatear(v.maximo(), moneda), false);
        fila(gridMetodos, fila, "Valor de referencia", Montos.formatear(v.valorReferencia(), moneda), true);

        Map<String, double[]> serie = new LinkedHashMap<>();
        serie.put("Valor", new double[]{v.patrimonial().doubleValue(),
                v.capitalizacion() == null ? 0 : v.capitalizacion().doubleValue(),
                v.flujosDescontados() == null ? 0 : v.flujosDescontados().doubleValue()});
        graficoMetodos.getChildren().setAll(Graficos.visor(Graficos.barrasCategorias(
                List.of("Patrimonio", "Capitalización", "Flujos"), serie,
                "'" + moneda.simbolo() + "' #,##0")));

        mostrarProyeccion(v);
        mostrarSensibilidad(v);

        lectura.getChildren().clear();
        for (String frase : v.lectura(moneda)) {
            Label punto = new Label("•  " + frase);
            punto.setWrapText(true);
            punto.getStyleClass().add("lectura");
            lectura.getChildren().add(punto);
        }
    }

    private void mostrarProyeccion(ValoracionNegocio v) {
        gridProyeccion.getChildren().clear();
        if (v.proyeccion().isEmpty()) {
            gridProyeccion.add(texto("Sin flujo positivo que proyectar.", "muted"), 0, 0);
            return;
        }
        int fila = 0;
        gridProyeccion.addRow(fila++, texto("Año", "info-key"), texto("Flujo", "info-key"),
                texto("Factor", "info-key"), texto("Valor hoy", "info-key"));
        for (ValoracionNegocio.Proyeccion p : v.proyeccion()) {
            gridProyeccion.addRow(fila++, texto(String.valueOf(p.anio()), "info-value"),
                    texto(Montos.formatear(p.flujo(), moneda), "info-value"),
                    texto(p.factor().toPlainString(), "muted"), texto(Montos.formatear(p.valorPresente(), moneda), "info-value"));
        }
        gridProyeccion.addRow(fila++, texto("Terminal", "info-value"), texto(Montos.formatear(v.valorTerminal(), moneda), "info-value"),
                texto("", "muted"), texto(Montos.formatear(v.valorTerminalPresente(), moneda), "info-value"));
        gridProyeccion.addRow(fila, texto("Total", "step-title"), texto("", "muted"), texto("", "muted"),
                texto(Montos.formatear(v.flujosDescontados(), moneda), "step-title"));
        Label nota = texto("Flujo del año 1 = (utilidad " + Montos.formatear(v.utilidadAnual(), moneda) + " + depreciación "
                + Montos.formatear(v.depreciacionAnual(), moneda) + ") × (1 + crecimiento).", "hint");
        nota.setWrapText(true);
        gridProyeccion.add(nota, 0, fila + 1, 4, 1);
    }

    private void mostrarSensibilidad(ValoracionNegocio v) {
        gridSensibilidad.getChildren().clear();
        if (v.flujosDescontados() == null) {
            gridSensibilidad.add(texto("No aplica sin flujo positivo.", "muted"), 0, 0);
            return;
        }
        ValoracionNegocio.Sensibilidad s = v.sensibilidad();
        gridSensibilidad.add(texto("Tasa / crec.", "info-key"), 0, 0);
        for (int j = 0; j < s.crecimientos().size(); j++) {
            gridSensibilidad.add(texto(Montos.porcentaje(s.crecimientos().get(j)), "info-key"), j + 1, 0);
        }
        for (int i = 0; i < s.tasas().size(); i++) {
            gridSensibilidad.add(texto(Montos.porcentaje(s.tasas().get(i)), "info-key"), 0, i + 1);
            for (int j = 0; j < s.crecimientos().size(); j++) {
                BigDecimal valor = s.valores()[i][j];
                Label celda = texto(valor == null ? "—" : moneda.simbolo() + " " + Montos.cantidad(valor.setScale(0, RoundingMode.HALF_UP)),
                        i == 1 && j == 1 ? "step-title" : "info-value");
                gridSensibilidad.add(celda, j + 1, i + 1);
            }
        }
    }

    private void limpiar(String mensaje) {
        for (TarjetaIndicador kpi : List.of(kpiReferencia, kpiPatrimonial, kpiRentabilidad, kpiFondo)) {
            kpi.actualizar(TarjetaIndicador.SIN_VALOR, "");
        }
        gridMetodos.getChildren().clear();
        gridProyeccion.getChildren().clear();
        gridSensibilidad.getChildren().clear();
        graficoMetodos.getChildren().clear();
        lectura.getChildren().setAll(texto(mensaje, "muted"));
        lblSupuestos.setText("");
    }

    private static int fila(GridPane grilla, int fila, String concepto, String valor, boolean destacado) {
        grilla.addRow(fila, texto(concepto, destacado ? "step-title" : "info-key"),
                texto(valor, destacado ? "total-valor-destacado" : "info-value"));
        return fila + 1;
    }

    private static Label texto(String texto, String clase) {
        Label l = new Label(texto);
        l.getStyleClass().add(clase);
        return l;
    }

    /** "20" → 0.20; acepta coma decimal. */
    private static BigDecimal porcentaje(String texto, String campo) {
        if (texto == null || texto.isBlank()) {
            throw new ValidacionException("Indica el campo \"" + campo + "\".");
        }
        try {
            return new BigDecimal(texto.strip().replace(',', '.').replace("%", "")).movePointLeft(2);
        } catch (NumberFormatException e) {
            throw new ValidacionException("El campo \"" + campo + "\" debe ser un número, por ejemplo 20.");
        }
    }

    private static String numero(BigDecimal fraccion) {
        return fraccion.movePointRight(2).setScale(1, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString();
    }
}
