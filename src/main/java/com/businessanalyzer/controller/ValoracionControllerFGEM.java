package com.businessanalyzer.controller;

import com.businessanalyzer.analytics.valoracion.SupuestosValoracionFGEM;
import com.businessanalyzer.analytics.valoracion.ValoracionNegocioFGEM;
import com.businessanalyzer.config.AppContextFGEM;
import com.businessanalyzer.exception.ValidacionExceptionFGEM;
import com.businessanalyzer.model.EmpresaFGEM;
import com.businessanalyzer.model.MonedaFGEM;
import com.businessanalyzer.model.PermisoFGEM;
import com.businessanalyzer.service.ValoracionServiceFGEM;
import com.businessanalyzer.ui.DialogosFGEM;
import com.businessanalyzer.ui.ErroresFGEM;
import com.businessanalyzer.ui.GraficosFGEM;
import com.businessanalyzer.ui.NotificadorFGEM;
import com.businessanalyzer.ui.TareasFGEM;
import com.businessanalyzer.ui.TarjetaIndicadorFGEM;
import com.businessanalyzer.ui.VistaModuloFGEM;
import com.businessanalyzer.util.MontosFGEM;
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

public class ValoracionControllerFGEM implements VistaModuloFGEM {

    private static final String CLAVE_CARPETA_FGEM = "informes.carpeta";

    private final AppContextFGEM contextoFGEM;
    private MonedaFGEM monedaFGEM = MonedaFGEM.PEN;
    private SupuestosValoracionFGEM supuestosActualesFGEM;
    private BigDecimal crecimientoSugeridoFGEM = BigDecimal.ZERO;
    private long solicitudFGEM;

    private final TarjetaIndicadorFGEM kpiReferenciaFGEM = new TarjetaIndicadorFGEM("Valor de referencia", "fth-star");
    private final TarjetaIndicadorFGEM kpiPatrimonialFGEM = new TarjetaIndicadorFGEM("Valor patrimonial", "fth-layers");
    private final TarjetaIndicadorFGEM kpiRentabilidadFGEM = new TarjetaIndicadorFGEM("Valor por rentabilidad", "fth-trending-up");
    private final TarjetaIndicadorFGEM kpiFondoFGEM = new TarjetaIndicadorFGEM("Fondo de comercio", "fth-award");

    @FXML private TextField txtTasaFGEM;
    @FXML private TextField txtCrecimientoFGEM;
    @FXML private ProgressIndicator progresoFGEM;
    @FXML private Button btnExportarFGEM;
    @FXML private Label lblSupuestosFGEM;
    @FXML private HBox indicadoresFGEM;
    @FXML private GridPane gridMetodosFGEM;
    @FXML private StackPane graficoMetodosFGEM;
    @FXML private GridPane gridProyeccionFGEM;
    @FXML private GridPane gridSensibilidadFGEM;
    @FXML private VBox lecturaFGEM;

    public ValoracionControllerFGEM(AppContextFGEM contextoFGEM) {
        this.contextoFGEM = contextoFGEM;
    }

    @FXML
    private void initialize() {
        indicadoresFGEM.getChildren().addAll(kpiReferenciaFGEM, kpiPatrimonialFGEM, kpiRentabilidadFGEM, kpiFondoFGEM);
        ColumnConstraints c1FGEM = new ColumnConstraints();
        c1FGEM.setMinWidth(230);
        ColumnConstraints c2FGEM = new ColumnConstraints();
        c2FGEM.setHgrow(Priority.ALWAYS);
        gridMetodosFGEM.getColumnConstraints().setAll(c1FGEM, c2FGEM);
        boolean informesFGEM = contextoFGEM.sesionFGEM().tienePermisoFGEM(PermisoFGEM.INFORMES_GENERAR);
        btnExportarFGEM.setVisible(informesFGEM);
        btnExportarFGEM.setManaged(informesFGEM);
        txtTasaFGEM.setOnAction(eFGEM -> calcularFGEM());
        txtCrecimientoFGEM.setOnAction(eFGEM -> calcularFGEM());
    }

    @Override
    public void alMostrarFGEM() {
        EmpresaFGEM empresaFGEM = contextoFGEM.sesionFGEM().empresaActivaFGEM().orElse(null);
        if (empresaFGEM == null) {
            limpiarFGEM("Selecciona una empresa en la barra superior.");
            return;
        }
        monedaFGEM = empresaFGEM.monedaFGEM();
        try {
            SupuestosValoracionFGEM sFGEM = contextoFGEM.valoracionFGEM().supuestosFGEM();
            mostrarSupuestosFGEM(sFGEM);
            ejecutarFGEM(sFGEM);
        } catch (RuntimeException eFGEM) {
            limpiarFGEM(ErroresFGEM.mensajeFGEM(eFGEM));
        }
    }

    @FXML
    private void calcularFGEM() {
        SupuestosValoracionFGEM sFGEM;
        try {
            BigDecimal tasaFGEM = porcentajeFGEM(txtTasaFGEM.getText(), "Tasa de descuento");
            BigDecimal crecimientoFGEM = porcentajeFGEM(txtCrecimientoFGEM.getText(), "Crecimiento anual");
            sFGEM = SupuestosValoracionFGEM.conFGEM(tasaFGEM, crecimientoFGEM);
            contextoFGEM.valoracionFGEM().guardarSupuestosFGEM(tasaFGEM, crecimientoFGEM);
        } catch (RuntimeException eFGEM) {
            NotificadorFGEM.errorFGEM(ErroresFGEM.mensajeFGEM(eFGEM));
            return;
        }
        ejecutarFGEM(sFGEM);
    }

    @FXML
    private void usarSugeridosFGEM() {
        try {
            contextoFGEM.valoracionFGEM().guardarSupuestosFGEM(SupuestosValoracionFGEM.TASA_PREDETERMINADA_FGEM, null);
        } catch (RuntimeException eFGEM) {
            NotificadorFGEM.errorFGEM(ErroresFGEM.mensajeFGEM(eFGEM));
            return;
        }
        alMostrarFGEM();
    }

    private void ejecutarFGEM(SupuestosValoracionFGEM sFGEM) {
        progresoFGEM.setVisible(true);
        long numeroFGEM = ++solicitudFGEM;
        TareasFGEM.ejecutarFGEM(() -> contextoFGEM.valoracionFGEM().valorarFGEM(sFGEM),
                rFGEM -> {
                    if (numeroFGEM == solicitudFGEM) {
                        progresoFGEM.setVisible(false);
                        supuestosActualesFGEM = sFGEM;
                        crecimientoSugeridoFGEM = rFGEM.crecimientoSugeridoFGEM();
                        mostrarFGEM(rFGEM);
                    }
                },
                errorFGEM -> {
                    if (numeroFGEM == solicitudFGEM) {
                        progresoFGEM.setVisible(false);
                        NotificadorFGEM.errorFGEM(ErroresFGEM.mensajeFGEM(errorFGEM));
                    }
                });
    }

    @FXML
    private void exportarFGEM() {
        EmpresaFGEM empresaFGEM = contextoFGEM.sesionFGEM().empresaActivaFGEM().orElse(null);
        if (empresaFGEM == null || supuestosActualesFGEM == null) {
            return;
        }
        FileChooser selectorFGEM = new FileChooser();
        selectorFGEM.setTitle("Guardar informe de valoración");
        selectorFGEM.getExtensionFilters().add(new FileChooser.ExtensionFilter("Documento PDF (*.pdf)", "*.pdf"));
        String limpioFGEM = java.text.Normalizer.normalize(empresaFGEM.nombreVisibleFGEM(), java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "").replaceAll("[^A-Za-z0-9]+", "-").replaceAll("(^-|-$)", "");
        selectorFGEM.setInitialFileName("Valoracion-" + limpioFGEM + "-" + LocalDate.now() + ".pdf");
        String guardadaFGEM = contextoFGEM.configuracionFGEM().obtenerFGEM(CLAVE_CARPETA_FGEM, null);
        for (Path carpetaFGEM : new Path[]{guardadaFGEM == null ? null : Path.of(guardadaFGEM),
                Path.of(System.getProperty("user.home"), "Documents"), Path.of(System.getProperty("user.home"))}) {
            if (carpetaFGEM != null && Files.isDirectory(carpetaFGEM)) {
                selectorFGEM.setInitialDirectory(carpetaFGEM.toFile());
                break;
            }
        }
        File archivoFGEM = selectorFGEM.showSaveDialog(btnExportarFGEM.getScene().getWindow());
        if (archivoFGEM == null) {
            return;
        }
        SupuestosValoracionFGEM sFGEM = supuestosActualesFGEM;
        progresoFGEM.setVisible(true);
        btnExportarFGEM.setDisable(true);
        TareasFGEM.ejecutarFGEM(() -> contextoFGEM.valoracionFGEM().exportarFGEM(sFGEM, archivoFGEM.toPath()),
                generadoFGEM -> {
                    progresoFGEM.setVisible(false);
                    btnExportarFGEM.setDisable(false);
                    try {
                        contextoFGEM.configuracionFGEM().guardarFGEM(CLAVE_CARPETA_FGEM, generadoFGEM.getParent().toString());
                    } catch (RuntimeException eFGEM) {
                    }
                    if (DialogosFGEM.confirmarFGEM(btnExportarFGEM.getScene().getWindow(), "Informe generado",
                            "Se guardó «" + generadoFGEM.getFileName() + "».\n\n¿Quieres abrirlo ahora?")) {
                        abrirFGEM(generadoFGEM);
                    }
                },
                errorFGEM -> {
                    progresoFGEM.setVisible(false);
                    btnExportarFGEM.setDisable(false);
                    NotificadorFGEM.errorFGEM(ErroresFGEM.mensajeFGEM(errorFGEM));
                });
    }

    private void abrirFGEM(Path rutaFGEM) {
        if (!Desktop.isDesktopSupported() || !Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
            NotificadorFGEM.advertenciaFGEM("El sistema no permite abrir archivos desde la aplicación. Ruta: " + rutaFGEM);
            return;
        }
        TareasFGEM.ejecutarFGEM(() -> {
            Desktop.getDesktop().open(rutaFGEM.toFile());
            return null;
        }, okFGEM -> {
        }, errorFGEM -> NotificadorFGEM.errorFGEM("No se pudo abrir el archivo: " + ErroresFGEM.mensajeFGEM(errorFGEM)));
    }

    private void mostrarSupuestosFGEM(SupuestosValoracionFGEM sFGEM) {
        txtTasaFGEM.setText(numeroFGEM(sFGEM.tasaDescuentoFGEM()));
        txtCrecimientoFGEM.setText(numeroFGEM(sFGEM.crecimientoFGEM()));
    }

    private void mostrarFGEM(ValoracionServiceFGEM.ResultadoFGEM rFGEM) {
        ValoracionNegocioFGEM vFGEM = rFGEM.valoracionFGEM();
        lblSupuestosFGEM.setText("Crecimiento sugerido por la tendencia de las ventas del último año: "
                + MontosFGEM.porcentajeFGEM(crecimientoSugeridoFGEM) + ". Después de " + vFGEM.supuestosFGEM().aniosFGEM()
                + " años se supone un crecimiento de " + MontosFGEM.porcentajeFGEM(vFGEM.supuestosFGEM().crecimientoLargoPlazoFGEM())
                + " anual." + (rFGEM.periodoResultadosFGEM() == null ? " Aún no hay ventas: solo se calcula el valor patrimonial."
                : " Utilidad de " + rFGEM.periodoResultadosFGEM().descripcionFGEM() + "."));

        kpiReferenciaFGEM.actualizarFGEM(MontosFGEM.formatearFGEM(vFGEM.valorReferenciaFGEM(), monedaFGEM),
                vFGEM.baseFGEM() == ValoracionNegocioFGEM.BaseFGEM.RENTABILIDAD ? "Por lo que gana" : "Por lo que tiene");
        kpiPatrimonialFGEM.actualizarFGEM(MontosFGEM.formatearFGEM(vFGEM.patrimonialFGEM(), monedaFGEM), "Activos − deudas");
        kpiRentabilidadFGEM.actualizarFGEM(vFGEM.valorRentabilidadFGEM() == null ? TarjetaIndicadorFGEM.SIN_VALOR_FGEM
                : MontosFGEM.formatearFGEM(vFGEM.valorRentabilidadFGEM(), monedaFGEM), utilidadAnualizadaFGEM(vFGEM));
        kpiFondoFGEM.actualizarFGEM(vFGEM.fondoComercioFGEM() == null ? TarjetaIndicadorFGEM.SIN_VALOR_FGEM : MontosFGEM.formatearFGEM(vFGEM.fondoComercioFGEM(), monedaFGEM),
                "Rentabilidad − patrimonio");

        gridMetodosFGEM.getChildren().clear();
        int filaFGEM = 0;
        filaFGEM = filaFGEM(gridMetodosFGEM, filaFGEM, "Patrimonial (activos − deudas)", MontosFGEM.formatearFGEM(vFGEM.patrimonialFGEM(), monedaFGEM), false);
        filaFGEM = filaFGEM(gridMetodosFGEM, filaFGEM, "Capitalización de utilidades", vFGEM.capitalizacionFGEM() == null ? "No aplica (sin utilidad)"
                : MontosFGEM.formatearFGEM(vFGEM.capitalizacionFGEM(), monedaFGEM), false);
        filaFGEM = filaFGEM(gridMetodosFGEM, filaFGEM, "Flujos descontados (" + vFGEM.supuestosFGEM().aniosFGEM() + " años)",
                vFGEM.flujosDescontadosFGEM() == null ? "No aplica (sin flujo positivo)" : MontosFGEM.formatearFGEM(vFGEM.flujosDescontadosFGEM(), monedaFGEM), false);
        filaFGEM = filaFGEM(gridMetodosFGEM, filaFGEM, "Rango", MontosFGEM.formatearFGEM(vFGEM.minimoFGEM(), monedaFGEM) + "  a  " + MontosFGEM.formatearFGEM(vFGEM.maximoFGEM(), monedaFGEM), false);
        filaFGEM(gridMetodosFGEM, filaFGEM, "Valor de referencia", MontosFGEM.formatearFGEM(vFGEM.valorReferenciaFGEM(), monedaFGEM), true);

        Map<String, double[]> serieFGEM = new LinkedHashMap<>();
        serieFGEM.put("Valor", new double[]{vFGEM.patrimonialFGEM().doubleValue(),
                vFGEM.capitalizacionFGEM() == null ? 0 : vFGEM.capitalizacionFGEM().doubleValue(),
                vFGEM.flujosDescontadosFGEM() == null ? 0 : vFGEM.flujosDescontadosFGEM().doubleValue()});
        graficoMetodosFGEM.getChildren().setAll(GraficosFGEM.visorFGEM(GraficosFGEM.barrasCategoriasFGEM(
                List.of("Patrimonio", "Capitalización", "Flujos"), serieFGEM,
                "'" + monedaFGEM.simboloFGEM() + "' #,##0")));

        mostrarProyeccionFGEM(vFGEM);
        mostrarSensibilidadFGEM(vFGEM);

        lecturaFGEM.getChildren().clear();
        List<String> frasesFGEM = vFGEM.lecturaFGEM(monedaFGEM);
        contextoFGEM.mostrarFraseFGEM(com.businessanalyzer.ui.ModuloFGEM.VALORACION, frasesFGEM.isEmpty() ? null : frasesFGEM.get(0));
        for (String fraseFGEM : frasesFGEM) {
            Label puntoFGEM = new Label("•  " + fraseFGEM);
            puntoFGEM.setWrapText(true);
            puntoFGEM.getStyleClass().add("lectura");
            lecturaFGEM.getChildren().add(puntoFGEM);
        }
    }

    private void mostrarProyeccionFGEM(ValoracionNegocioFGEM vFGEM) {
        gridProyeccionFGEM.getChildren().clear();
        if (vFGEM.proyeccionFGEM().isEmpty()) {
            gridProyeccionFGEM.add(textoFGEM("Sin flujo positivo que proyectar.", "muted"), 0, 0);
            return;
        }
        int filaFGEM = 0;
        gridProyeccionFGEM.addRow(filaFGEM++, textoFGEM("Año", "info-key"), textoFGEM("Flujo", "info-key"),
                textoFGEM("Factor", "info-key"), textoFGEM("Valor hoy", "info-key"));
        for (ValoracionNegocioFGEM.ProyeccionFGEM pFGEM : vFGEM.proyeccionFGEM()) {
            gridProyeccionFGEM.addRow(filaFGEM++, textoFGEM(String.valueOf(pFGEM.anioFGEM()), "info-value"),
                    textoFGEM(MontosFGEM.formatearFGEM(pFGEM.flujoFGEM(), monedaFGEM), "info-value"),
                    textoFGEM(pFGEM.factorFGEM().toPlainString(), "muted"), textoFGEM(MontosFGEM.formatearFGEM(pFGEM.valorPresenteFGEM(), monedaFGEM), "info-value"));
        }
        gridProyeccionFGEM.addRow(filaFGEM++, textoFGEM("Terminal", "info-value"), textoFGEM(MontosFGEM.formatearFGEM(vFGEM.valorTerminalFGEM(), monedaFGEM), "info-value"),
                textoFGEM("", "muted"), textoFGEM(MontosFGEM.formatearFGEM(vFGEM.valorTerminalPresenteFGEM(), monedaFGEM), "info-value"));
        gridProyeccionFGEM.addRow(filaFGEM, textoFGEM("Total", "step-title"), textoFGEM("", "muted"), textoFGEM("", "muted"),
                textoFGEM(MontosFGEM.formatearFGEM(vFGEM.flujosDescontadosFGEM(), monedaFGEM), "step-title"));
        Label notaFGEM = textoFGEM("Flujo del año 1 = (utilidad " + MontosFGEM.formatearFGEM(vFGEM.utilidadAnualFGEM(), monedaFGEM) + " + depreciación "
                + MontosFGEM.formatearFGEM(vFGEM.depreciacionAnualFGEM(), monedaFGEM) + ") × (1 + crecimiento).", "hint");
        notaFGEM.setWrapText(true);
        gridProyeccionFGEM.add(notaFGEM, 0, filaFGEM + 1, 4, 1);
    }

    private void mostrarSensibilidadFGEM(ValoracionNegocioFGEM vFGEM) {
        gridSensibilidadFGEM.getChildren().clear();
        if (vFGEM.flujosDescontadosFGEM() == null) {
            gridSensibilidadFGEM.add(textoFGEM("No aplica sin flujo positivo.", "muted"), 0, 0);
            return;
        }
        ValoracionNegocioFGEM.SensibilidadFGEM sFGEM = vFGEM.sensibilidadFGEM();
        gridSensibilidadFGEM.add(textoFGEM("Tasa / crec.", "info-key"), 0, 0);
        for (int jFGEM = 0; jFGEM < sFGEM.crecimientosFGEM().size(); jFGEM++) {
            gridSensibilidadFGEM.add(textoFGEM(MontosFGEM.porcentajeFGEM(sFGEM.crecimientosFGEM().get(jFGEM)), "info-key"), jFGEM + 1, 0);
        }
        for (int iFGEM = 0; iFGEM < sFGEM.tasasFGEM().size(); iFGEM++) {
            gridSensibilidadFGEM.add(textoFGEM(MontosFGEM.porcentajeFGEM(sFGEM.tasasFGEM().get(iFGEM)), "info-key"), 0, iFGEM + 1);
            for (int jFGEM = 0; jFGEM < sFGEM.crecimientosFGEM().size(); jFGEM++) {
                BigDecimal valorFGEM = sFGEM.valoresFGEM()[iFGEM][jFGEM];
                Label celdaFGEM = textoFGEM(valorFGEM == null ? "—" : monedaFGEM.simboloFGEM() + " " + MontosFGEM.cantidadFGEM(valorFGEM.setScale(0, RoundingMode.HALF_UP)),
                        iFGEM == 1 && jFGEM == 1 ? "step-title" : "info-value");
                gridSensibilidadFGEM.add(celdaFGEM, jFGEM + 1, iFGEM + 1);
            }
        }
    }

    private void limpiarFGEM(String mensajeFGEM) {
        for (TarjetaIndicadorFGEM kpiFGEM : List.of(kpiReferenciaFGEM, kpiPatrimonialFGEM, kpiRentabilidadFGEM, kpiFondoFGEM)) {
            kpiFGEM.actualizarFGEM(TarjetaIndicadorFGEM.SIN_VALOR_FGEM, "");
        }
        gridMetodosFGEM.getChildren().clear();
        gridProyeccionFGEM.getChildren().clear();
        gridSensibilidadFGEM.getChildren().clear();
        graficoMetodosFGEM.getChildren().clear();
        lecturaFGEM.getChildren().setAll(textoFGEM(mensajeFGEM, "muted"));
        contextoFGEM.mostrarFraseFGEM(com.businessanalyzer.ui.ModuloFGEM.VALORACION, null);
        lblSupuestosFGEM.setText("");
    }

    private static int filaFGEM(GridPane grillaFGEM, int filaFGEM, String conceptoFGEM, String valorFGEM, boolean destacadoFGEM) {
        grillaFGEM.addRow(filaFGEM, textoFGEM(conceptoFGEM, destacadoFGEM ? "step-title" : "info-key"),
                textoFGEM(valorFGEM, destacadoFGEM ? "total-valor-destacado" : "info-value"));
        return filaFGEM + 1;
    }

    private static Label textoFGEM(String textoFGEM, String claseFGEM) {
        Label lFGEM = new Label(textoFGEM);
        lFGEM.getStyleClass().add(claseFGEM);
        return lFGEM;
    }

    private static BigDecimal porcentajeFGEM(String textoFGEM, String campoFGEM) {
        if (textoFGEM == null || textoFGEM.isBlank()) {
            throw new ValidacionExceptionFGEM("Indica el campo \"" + campoFGEM + "\".");
        }
        try {
            return new BigDecimal(textoFGEM.strip().replace(',', '.').replace("%", "")).movePointLeft(2);
        } catch (NumberFormatException eFGEM) {
            throw new ValidacionExceptionFGEM("El campo \"" + campoFGEM + "\" debe ser un número, por ejemplo 20.");
        }
    }

    private static String numeroFGEM(BigDecimal fraccionFGEM) {
        return fraccionFGEM.movePointRight(2).setScale(1, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString();
    }

    private String utilidadAnualizadaFGEM(ValoracionNegocioFGEM vFGEM) {
        String montoFGEM = MontosFGEM.formatearFGEM(vFGEM.utilidadAnualFGEM(), monedaFGEM);
        int mesesFGEM = vFGEM.mesesResultadosFGEM();
        return mesesFGEM > 0 && mesesFGEM != 12
                ? "Utilidad anualizada " + montoFGEM + " (" + mesesFGEM + (mesesFGEM == 1 ? " mes" : " meses") + " llevados a 12)"
                : "Utilidad anualizada " + montoFGEM;
    }
}
