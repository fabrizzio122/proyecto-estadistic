package com.businessanalyzer.controller;

import com.businessanalyzer.analytics.sugerencias.SugerenciasProductoFGEM;
import com.businessanalyzer.analytics.sugerencias.SugerenciasProductoFGEM.MotivoPrecioFGEM;
import com.businessanalyzer.analytics.sugerencias.SugerenciasProductoFGEM.PrecioFGEM;
import com.businessanalyzer.analytics.sugerencias.SugerenciasProductoFGEM.ReposicionFGEM;
import com.businessanalyzer.analytics.sugerencias.SugerenciasProductoFGEM.ResultadoFGEM;
import com.businessanalyzer.analytics.sugerencias.SugerenciasProductoFGEM.SinMovimientoFGEM;
import com.businessanalyzer.analytics.sugerencias.SugerenciasProductoFGEM.SituacionFGEM;
import com.businessanalyzer.analytics.sugerencias.SugerenciasProductoFGEM.UrgenciaFGEM;
import com.businessanalyzer.config.AppContextFGEM;
import com.businessanalyzer.model.EmpresaFGEM;
import com.businessanalyzer.model.MonedaFGEM;
import com.businessanalyzer.model.PermisoFGEM;
import com.businessanalyzer.ui.DialogosFGEM;
import com.businessanalyzer.ui.ErroresFGEM;
import com.businessanalyzer.ui.ModuloFGEM;
import com.businessanalyzer.ui.NotificadorFGEM;
import com.businessanalyzer.ui.TarjetaIndicadorFGEM;
import com.businessanalyzer.ui.TareasFGEM;
import com.businessanalyzer.ui.VistaModuloFGEM;
import com.businessanalyzer.util.MontosFGEM;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.MenuButton;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.Tab;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.stage.FileChooser;

import java.awt.Desktop;
import java.io.File;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public class SugerenciasControllerFGEM implements VistaModuloFGEM {

    private static final DateTimeFormatter FECHA_FGEM = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final String TENUE_FGEM = "texto-tenue";
    private static final String CLAVE_CARPETA_FGEM = "informes.carpeta";
    private static final List<String> CLASES_FGEM = List.of("margen-ok", "margen-negativo", TENUE_FGEM);

    private final AppContextFGEM contextoFGEM;
    private MonedaFGEM monedaFGEM = MonedaFGEM.PEN;

    private final TarjetaIndicadorFGEM kpiGananciaFGEM = new TarjetaIndicadorFGEM("Ganancia extra posible", "fth-trending-up");
    private final TarjetaIndicadorFGEM kpiReponerFGEM = new TarjetaIndicadorFGEM("Por reponer", "fth-shopping-cart");
    private final TarjetaIndicadorFGEM kpiParadoFGEM = new TarjetaIndicadorFGEM("Dinero inmovilizado", "fth-archive");

    @FXML private Spinner<Integer> spnMargenFGEM;
    @FXML private ProgressIndicator progresoFGEM;
    @FXML private Label lblPeriodoFGEM;
    @FXML private HBox indicadoresFGEM;
    @FXML private Tab tabPreciosFGEM;
    @FXML private Tab tabReposicionFGEM;
    @FXML private Tab tabSinMovimientoFGEM;
    @FXML private TableView<PrecioFGEM> tablaPreciosFGEM;
    @FXML private TableView<ReposicionFGEM> tablaReposicionFGEM;
    @FXML private TableView<SinMovimientoFGEM> tablaSinMovimientoFGEM;
    @FXML private MenuButton menuListaComprasFGEM;
    @FXML private Button btnRegistrarCompraFGEM;

    public SugerenciasControllerFGEM(AppContextFGEM contextoFGEM) {
        this.contextoFGEM = contextoFGEM;
    }

    @FXML
    private void initialize() {
        for (TarjetaIndicadorFGEM kpiFGEM : new TarjetaIndicadorFGEM[]{kpiGananciaFGEM, kpiReponerFGEM, kpiParadoFGEM}) {
            HBox.setHgrow(kpiFGEM, Priority.ALWAYS);
            kpiFGEM.setMaxWidth(Double.MAX_VALUE);
            indicadoresFGEM.getChildren().add(kpiFGEM);
        }
        spnMargenFGEM.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 80, 20));
        spnMargenFGEM.getEditor().setOnAction(eFGEM -> calcularFGEM());
        configurarPreciosFGEM();
        configurarReposicionFGEM();
        configurarSinMovimientoFGEM();
        boolean informesFGEM = contextoFGEM.sesionFGEM().tienePermisoFGEM(PermisoFGEM.INFORMES_GENERAR);
        menuListaComprasFGEM.setVisible(informesFGEM);
        menuListaComprasFGEM.setManaged(informesFGEM);
        menuListaComprasFGEM.setDisable(true);
        boolean comprasFGEM = contextoFGEM.sesionFGEM().tienePermisoFGEM(PermisoFGEM.OPERACIONES_GESTIONAR);
        btnRegistrarCompraFGEM.setVisible(comprasFGEM);
        btnRegistrarCompraFGEM.setManaged(comprasFGEM);
        btnRegistrarCompraFGEM.setDisable(true);
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
            spnMargenFGEM.getValueFactory().setValue(contextoFGEM.sugerenciasFGEM().margenObjetivoFGEM().movePointRight(2).intValue());
        } catch (RuntimeException eFGEM) {
            limpiarFGEM(ErroresFGEM.mensajeFGEM(eFGEM));
            return;
        }
        calcularFGEM();
    }

    @FXML
    private void calcularFGEM() {
        spnMargenFGEM.increment(0);
        BigDecimal margenFGEM = BigDecimal.valueOf(spnMargenFGEM.getValue()).movePointLeft(2);
        progresoFGEM.setVisible(true);
        TareasFGEM.ejecutarFGEM(() -> contextoFGEM.sugerenciasFGEM().sugerenciasFGEM(margenFGEM), resultadoFGEM -> {
            progresoFGEM.setVisible(false);
            mostrarFGEM(resultadoFGEM);
        }, errorFGEM -> {
            progresoFGEM.setVisible(false);
            NotificadorFGEM.errorFGEM(ErroresFGEM.mensajeFGEM(errorFGEM));
        });
    }

    private void mostrarFGEM(ResultadoFGEM rFGEM) {
        LocalDate hoyFGEM = LocalDate.now();
        lblPeriodoFGEM.setText("Precios y reposición según las ventas de los últimos " + diasFGEM(rFGEM.diasAnalizadosFGEM()) + " (del "
                + FECHA_FGEM.format(hoyFGEM.minusDays(rFGEM.diasAnalizadosFGEM() - 1L)) + " al " + FECHA_FGEM.format(hoyFGEM) + "); la venta "
                + "lenta, con los últimos " + SugerenciasProductoFGEM.DIAS_HISTORIAL_FGEM + " días."
                + (rFGEM.pocosDatosFGEM() ? " Hay pocos días de ventas: toma las cantidades como orientativas." : ""));

        mostrarGananciaFGEM(rFGEM);
        long agotadosFGEM = rFGEM.reposicionFGEM().stream().filter(rFGEM2 -> rFGEM2.urgenciaFGEM() == UrgenciaFGEM.AGOTADO).count();
        int reponerFGEM = rFGEM.reposicionFGEM().size();
        kpiReponerFGEM.actualizarFGEM(String.valueOf(reponerFGEM),
                reponerFGEM == 0 ? "Hay stock para al menos dos semanas"
                        : (reponerFGEM == 1 ? "producto" : "productos") + " · compra sugerida " + dineroFGEM(rFGEM.costoReposicionFGEM())
                        + (agotadosFGEM > 0 ? " · " + agotadosFGEM + (agotadosFGEM == 1 ? " agotado" : " agotados") : ""));
        mostrarParadoFGEM(rFGEM);

        tablaPreciosFGEM.getItems().setAll(rFGEM.preciosFGEM());
        tablaReposicionFGEM.getItems().setAll(rFGEM.reposicionFGEM());
        menuListaComprasFGEM.setDisable(rFGEM.reposicionFGEM().isEmpty());
        btnRegistrarCompraFGEM.setDisable(rFGEM.reposicionFGEM().isEmpty());
        tablaSinMovimientoFGEM.getItems().setAll(rFGEM.sinMovimientoFGEM());
        tabPreciosFGEM.setText("Precios (" + rFGEM.preciosFGEM().size() + ")");
        tabReposicionFGEM.setText("Reposición (" + rFGEM.reposicionFGEM().size() + ")");
        tabSinMovimientoFGEM.setText("Sin movimiento (" + rFGEM.sinMovimientoFGEM().size() + ")");

        String fraseFGEM = rFGEM.preciosPrioritariosFGEM() == 0 ? null
                : "Ajustando " + rFGEM.preciosPrioritariosFGEM() + " precio(s) podrías ganar unos "
                + dineroFGEM(rFGEM.gananciaExtraMesFGEM()) + " más al mes, si vendes la misma cantidad.";
        contextoFGEM.mostrarFraseFGEM(ModuloFGEM.SUGERENCIAS, fraseFGEM);
    }

    private void mostrarGananciaFGEM(ResultadoFGEM rFGEM) {
        long cuantosFGEM = rFGEM.preciosPrioritariosFGEM();
        if (cuantosFGEM == 0) {
            kpiGananciaFGEM.actualizarFGEM(dineroFGEM(BigDecimal.ZERO), rFGEM.preciosFGEM().isEmpty()
                    ? "Todos los precios cumplen el margen objetivo"
                    : "Los precios por ajustar son de productos que casi no se venden");
            return;
        }
        BigDecimal extraFGEM = rFGEM.gananciaExtraMesFGEM();
        StringBuilder detalleFGEM = new StringBuilder("al mes si vendes la misma cantidad, ajustando ").append(cuantosFGEM)
                .append(cuantosFGEM == 1 ? " precio." : " precios.");
        BigDecimal utilidadFGEM = rFGEM.utilidadMensualFGEM();
        if (utilidadFGEM != null) {
            BigDecimal nuevaFGEM = utilidadFGEM.add(extraFGEM);
            if (utilidadFGEM.signum() < 0) {
                detalleFGEM.append(" Reduce la pérdida mensual de ").append(dineroFGEM(utilidadFGEM.negate()));
                detalleFGEM.append(nuevaFGEM.signum() < 0 ? " a unos " + dineroFGEM(nuevaFGEM.negate()) + "; para dejar de perder, revisa el Diagnóstico."
                        : " a cero o más: el negocio pasaría a ganar.");
            } else {
                detalleFGEM.append(" La utilidad mensual pasaría de ").append(dineroFGEM(utilidadFGEM)).append(" a unos ")
                        .append(dineroFGEM(nuevaFGEM)).append('.');
            }
        }
        kpiGananciaFGEM.actualizarFGEM(dineroFGEM(extraFGEM), detalleFGEM.toString());
    }

    private void mostrarParadoFGEM(ResultadoFGEM rFGEM) {
        if (rFGEM.sinMovimientoFGEM().isEmpty()) {
            kpiParadoFGEM.actualizarFGEM(dineroFGEM(BigDecimal.ZERO), "Todo el stock se está vendiendo");
            return;
        }
        List<String> partesFGEM = new ArrayList<>();
        BigDecimal porcentajeFGEM = rFGEM.porcentajeInmovilizadoFGEM();
        if (porcentajeFGEM != null) {
            partesFGEM.add(MontosFGEM.porcentajeFGEM(porcentajeFGEM) + " del inventario");
        }
        for (SituacionFGEM sFGEM : SituacionFGEM.values()) {
            long nFGEM = rFGEM.cantidadFGEM(sFGEM);
            if (nFGEM > 0) {
                partesFGEM.add(nFGEM + switch (sFGEM) {
                    case PERECIBLE_EN_RIESGO -> nFGEM == 1 ? " perecible en riesgo" : " perecibles en riesgo";
                    case SIN_VENTAS -> " sin ventas";
                    case VENTA_LENTA -> " de venta lenta";
                });
            }
        }
        kpiParadoFGEM.actualizarFGEM(dineroFGEM(rFGEM.valorInmovilizadoFGEM()), String.join(" · ", partesFGEM));
    }

    private void limpiarFGEM(String mensajeFGEM) {
        lblPeriodoFGEM.setText(mensajeFGEM);
        for (TarjetaIndicadorFGEM kpiFGEM : new TarjetaIndicadorFGEM[]{kpiGananciaFGEM, kpiReponerFGEM, kpiParadoFGEM}) {
            kpiFGEM.actualizarFGEM(TarjetaIndicadorFGEM.SIN_VALOR_FGEM, "");
        }
        tablaPreciosFGEM.getItems().clear();
        tablaReposicionFGEM.getItems().clear();
        tablaSinMovimientoFGEM.getItems().clear();
        menuListaComprasFGEM.setDisable(true);
        btnRegistrarCompraFGEM.setDisable(true);
    }

    @FXML
    private void registrarCompraFGEM() {
        List<ReposicionFGEM> reposicionFGEM = List.copyOf(tablaReposicionFGEM.getItems());
        if (reposicionFGEM.isEmpty()) {
            return;
        }
        contextoFGEM.proponerCompraFGEM(new AppContextFGEM.CompraPropuestaFGEM(SugerenciasProductoFGEM.lineasCompraFGEM(reposicionFGEM),
                "Reposición sugerida del " + FECHA_FGEM.format(LocalDate.now())));
    }

    @FXML
    private void exportarListaPdfFGEM() {
        exportarListaFGEM(false);
    }

    @FXML
    private void exportarListaExcelFGEM() {
        exportarListaFGEM(true);
    }

    private void exportarListaFGEM(boolean excelFGEM) {
        EmpresaFGEM empresaFGEM = contextoFGEM.sesionFGEM().empresaActivaFGEM().orElse(null);
        if (empresaFGEM == null) {
            return;
        }
        FileChooser selectorFGEM = new FileChooser();
        selectorFGEM.setTitle("Guardar lista de compras");
        selectorFGEM.getExtensionFilters().add(excelFGEM
                ? new FileChooser.ExtensionFilter("Libro de Excel (*.xlsx)", "*.xlsx")
                : new FileChooser.ExtensionFilter("Documento PDF (*.pdf)", "*.pdf"));
        String limpioFGEM = java.text.Normalizer.normalize(empresaFGEM.nombreVisibleFGEM(), java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "").replaceAll("[^A-Za-z0-9]+", "-").replaceAll("(^-|-$)", "");
        selectorFGEM.setInitialFileName("Lista-de-compras-" + limpioFGEM + "-" + LocalDate.now() + (excelFGEM ? ".xlsx" : ".pdf"));
        String guardadaFGEM = contextoFGEM.configuracionFGEM().obtenerFGEM(CLAVE_CARPETA_FGEM, null);
        for (Path carpetaFGEM : new Path[]{guardadaFGEM == null ? null : Path.of(guardadaFGEM),
                Path.of(System.getProperty("user.home"), "Documents"), Path.of(System.getProperty("user.home"))}) {
            if (carpetaFGEM != null && Files.isDirectory(carpetaFGEM)) {
                selectorFGEM.setInitialDirectory(carpetaFGEM.toFile());
                break;
            }
        }
        File archivoFGEM = selectorFGEM.showSaveDialog(menuListaComprasFGEM.getScene().getWindow());
        if (archivoFGEM == null) {
            return;
        }
        Path destinoFGEM = archivoFGEM.toPath();
        String nombreFGEM = destinoFGEM.getFileName().toString().toLowerCase(java.util.Locale.ROOT);
        String extensionFGEM = excelFGEM ? ".xlsx" : ".pdf";
        if (!nombreFGEM.endsWith(extensionFGEM)) {
            destinoFGEM = destinoFGEM.resolveSibling(destinoFGEM.getFileName() + extensionFGEM);
        }
        Path finalFGEM = destinoFGEM;
        BigDecimal margenFGEM = BigDecimal.valueOf(spnMargenFGEM.getValue()).movePointLeft(2);
        progresoFGEM.setVisible(true);
        menuListaComprasFGEM.setDisable(true);
        TareasFGEM.ejecutarFGEM(() -> contextoFGEM.sugerenciasFGEM().exportarListaComprasFGEM(margenFGEM, finalFGEM),
                generadoFGEM -> {
                    progresoFGEM.setVisible(false);
                    menuListaComprasFGEM.setDisable(tablaReposicionFGEM.getItems().isEmpty());
                    try {
                        contextoFGEM.configuracionFGEM().guardarFGEM(CLAVE_CARPETA_FGEM, generadoFGEM.getParent().toString());
                    } catch (RuntimeException eFGEM) {
                    }
                    if (DialogosFGEM.confirmarFGEM(menuListaComprasFGEM.getScene().getWindow(), "Lista de compras guardada",
                            "Se guardó «" + generadoFGEM.getFileName() + "».\n\n¿Quieres abrirla ahora?")) {
                        abrirFGEM(generadoFGEM);
                    }
                },
                errorFGEM -> {
                    progresoFGEM.setVisible(false);
                    menuListaComprasFGEM.setDisable(tablaReposicionFGEM.getItems().isEmpty());
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

    private void configurarPreciosFGEM() {
        Function<PrecioFGEM, String> tenueFGEM = pFGEM -> pFGEM.notaFGEM() != null ? TENUE_FGEM : null;
        tablaPreciosFGEM.getColumns().setAll(
                columnaFGEM("Producto", 175, pFGEM -> pFGEM.productoFGEM().nombreFGEM(), tenueFGEM),
                columnaFGEM("Motivo", 165, pFGEM -> pFGEM.motivoFGEM().descripcionFGEM(),
                        pFGEM -> pFGEM.notaFGEM() != null ? TENUE_FGEM : pFGEM.motivoFGEM() == MotivoPrecioFGEM.PERDIDA ? "margen-negativo" : null),
                numeroFGEM("Costo", 80, pFGEM -> dineroFGEM(pFGEM.productoFGEM().costoUnitarioFGEM()), tenueFGEM),
                numeroFGEM("Precio", 80, pFGEM -> dineroFGEM(pFGEM.productoFGEM().precioVentaFGEM()), tenueFGEM),
                numeroFGEM("Margen", 80, pFGEM -> MontosFGEM.porcentajeFGEM(pFGEM.margenActualFGEM()), tenueFGEM),
                numeroFGEM("Sugerido", 90, pFGEM -> dineroFGEM(pFGEM.precioSugeridoFGEM()),
                        pFGEM -> pFGEM.notaFGEM() != null ? TENUE_FGEM : "margen-ok"),
                numeroFGEM("Nuevo margen", 115, pFGEM -> MontosFGEM.porcentajeFGEM(pFGEM.margenSugeridoFGEM()), tenueFGEM),
                numeroFGEM("Vende al mes", 115, pFGEM -> MontosFGEM.cantidadFGEM(pFGEM.unidadesMesFGEM().setScale(1, RoundingMode.HALF_UP)), tenueFGEM),
                numeroFGEM("Extra al mes", 105, pFGEM -> pFGEM.notaFGEM() != null && pFGEM.gananciaExtraMesFGEM().signum() == 0 ? "—"
                        : dineroFGEM(pFGEM.gananciaExtraMesFGEM()), pFGEM -> pFGEM.notaFGEM() != null ? TENUE_FGEM : "margen-ok"),
                ajustadaFGEM(columnaFGEM("Nota", 90, pFGEM -> pFGEM.notaFGEM() == null ? "" : pFGEM.notaFGEM().descripcionFGEM(), tenueFGEM)));
        tablaPreciosFGEM.setPlaceholder(new Label("Todos los precios cumplen el margen objetivo."));
        tablaPreciosFGEM.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
    }

    private void configurarReposicionFGEM() {
        tablaReposicionFGEM.getColumns().setAll(
                columnaFGEM("Producto", 190, rFGEM -> rFGEM.productoFGEM().nombreFGEM() + (rFGEM.productoFGEM().perecibleFGEM() ? "  (perecible)" : ""), null),
                columnaFGEM("Estado", 150, rFGEM -> rFGEM.urgenciaFGEM().descripcionFGEM(),
                        rFGEM -> rFGEM.urgenciaFGEM() == UrgenciaFGEM.PRONTO ? null : "margen-negativo"),
                numeroFGEM("Stock", 70, rFGEM -> MontosFGEM.cantidadFGEM(rFGEM.stockFGEM()), null),
                numeroFGEM("Ritmo de venta", 170, this::ritmoFGEM, null),
                numeroFGEM("Alcanza para", 95, rFGEM -> rFGEM.urgenciaFGEM() == UrgenciaFGEM.AGOTADO ? "—"
                        : diasFGEM(rFGEM.diasCoberturaFGEM().setScale(0, RoundingMode.DOWN).intValue()), null),
                numeroFGEM("Comprar", 85, rFGEM -> MontosFGEM.cantidadFGEM(rFGEM.cantidadFGEM()) + " " + rFGEM.productoFGEM().unidadFGEM().abreviaturaFGEM(),
                        rFGEM -> "margen-ok"),
                numeroFGEM("Costo aprox.", 105, rFGEM -> dineroFGEM(rFGEM.costoFGEM()), null));
        tablaReposicionFGEM.setPlaceholder(new Label("Hay stock para al menos dos semanas de todos los productos que se venden."));
        tablaReposicionFGEM.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
    }

    private String ritmoFGEM(ReposicionFGEM rFGEM) {
        String vendidoFGEM = MontosFGEM.cantidadFGEM(rFGEM.vendidoFGEM()) + " en " + diasFGEM(rFGEM.diasBaseFGEM());
        if (rFGEM.segunHistorialFGEM()) {
            return vendidoFGEM + " (antes de agotarse)";
        }
        return rFGEM.ventaDiariaFGEM().compareTo(new BigDecimal("0.1")) < 0 ? vendidoFGEM
                : MontosFGEM.cantidadFGEM(rFGEM.ventaDiariaFGEM().setScale(1, RoundingMode.HALF_UP)) + " por día";
    }

    private void configurarSinMovimientoFGEM() {
        Function<SinMovimientoFGEM, String> urgenteFGEM = sFGEM -> sFGEM.situacionFGEM() == SituacionFGEM.PERECIBLE_EN_RIESGO ? "margen-negativo" : null;
        tablaSinMovimientoFGEM.getColumns().setAll(
                columnaFGEM("Producto", 200, sFGEM -> sFGEM.productoFGEM().nombreFGEM(), null),
                columnaFGEM("Situación", 230, sFGEM -> sFGEM.situacionFGEM().descripcionFGEM(), urgenteFGEM),
                numeroFGEM("Stock", 70, sFGEM -> MontosFGEM.cantidadFGEM(sFGEM.stockFGEM()), null),
                numeroFGEM("Dinero inmovilizado", 130, sFGEM -> dineroFGEM(sFGEM.valorFGEM()), null),
                columnaFGEM("Detalle", 210, this::detalleParadoFGEM, null),
                ajustadaFGEM(columnaFGEM("Qué hacer", 300, sFGEM -> sFGEM.situacionFGEM().sugerenciaFGEM(), urgenteFGEM)));
        tablaSinMovimientoFGEM.setPlaceholder(new Label("Todo el stock se está vendiendo a buen ritmo."));
        tablaSinMovimientoFGEM.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
    }

    private String detalleParadoFGEM(SinMovimientoFGEM sFGEM) {
        if (sFGEM.diasCoberturaFGEM() == null) {
            return sFGEM.situacionFGEM() == SituacionFGEM.PERECIBLE_EN_RIESGO
                    ? "Sin ventas en " + diasFGEM(SugerenciasProductoFGEM.DIAS_ANALISIS_FGEM)
                    : "Sin ventas en " + diasFGEM(SugerenciasProductoFGEM.DIAS_HISTORIAL_FGEM);
        }
        return "Tardaría " + diasFGEM(sFGEM.diasCoberturaFGEM().intValue()) + " en venderse";
    }

    private String dineroFGEM(BigDecimal valorFGEM) {
        return MontosFGEM.formatearFGEM(valorFGEM.setScale(2, RoundingMode.HALF_UP), monedaFGEM);
    }

    static String diasFGEM(int diasFGEM) {
        return diasFGEM + (diasFGEM == 1 ? " día" : " días");
    }

    private static <T> TableColumn<T, String> columnaFGEM(String tituloFGEM, double anchoFGEM, Function<T, String> textoFGEM,
                                                          Function<T, String> claseFGEM) {
        TableColumn<T, String> columnaFGEM = new TableColumn<>(tituloFGEM);
        columnaFGEM.setPrefWidth(anchoFGEM);
        columnaFGEM.setMinWidth(Math.min(anchoFGEM, 70));
        columnaFGEM.setCellValueFactory(cFGEM -> new ReadOnlyStringWrapper(textoFGEM.apply(cFGEM.getValue())));
        columnaFGEM.setCellFactory(cFGEM -> new TableCell<>() {
            @Override
            protected void updateItem(String itemFGEM, boolean vacioFGEM) {
                super.updateItem(itemFGEM, vacioFGEM);
                getStyleClass().removeAll(CLASES_FGEM);
                setText(vacioFGEM ? null : itemFGEM);
                setTooltip(vacioFGEM || itemFGEM == null || itemFGEM.length() < 25 ? null : new Tooltip(itemFGEM));
                int filaFGEM = getIndex();
                if (!vacioFGEM && claseFGEM != null && getTableView() != null && filaFGEM >= 0 && filaFGEM < getTableView().getItems().size()) {
                    String claseCeldaFGEM = claseFGEM.apply(getTableView().getItems().get(filaFGEM));
                    if (claseCeldaFGEM != null) {
                        getStyleClass().add(claseCeldaFGEM);
                    }
                }
            }
        });
        return columnaFGEM;
    }

    private static <T> TableColumn<T, String> ajustadaFGEM(TableColumn<T, String> columnaFGEM) {
        var fabricaFGEM = columnaFGEM.getCellFactory();
        columnaFGEM.setCellFactory(cFGEM -> {
            TableCell<T, String> celdaFGEM = fabricaFGEM.call(cFGEM);
            Label textoFGEM = new Label();
            textoFGEM.setWrapText(true);
            textoFGEM.setMinHeight(javafx.scene.layout.Region.USE_PREF_SIZE);
            textoFGEM.maxWidthProperty().bind(columnaFGEM.widthProperty().subtract(14));
            textoFGEM.prefWidthProperty().bind(columnaFGEM.widthProperty().subtract(14));
            celdaFGEM.textProperty().addListener((oFGEM, aFGEM, nFGEM) -> {
                textoFGEM.setText(nFGEM);
                celdaFGEM.setGraphic(nFGEM == null || nFGEM.isEmpty() ? null : textoFGEM);
            });
            celdaFGEM.setContentDisplay(javafx.scene.control.ContentDisplay.GRAPHIC_ONLY);
            celdaFGEM.getStyleClass().addListener((javafx.collections.ListChangeListener<String>) cambioFGEM -> {
                textoFGEM.getStyleClass().removeAll(CLASES_FGEM);
                celdaFGEM.getStyleClass().stream().filter(CLASES_FGEM::contains).forEach(textoFGEM.getStyleClass()::add);
            });
            return celdaFGEM;
        });
        return columnaFGEM;
    }

    private static <T> TableColumn<T, String> numeroFGEM(String tituloFGEM, double anchoFGEM, Function<T, String> textoFGEM,
                                                         Function<T, String> claseFGEM) {
        TableColumn<T, String> columnaFGEM = columnaFGEM(tituloFGEM, anchoFGEM, textoFGEM, claseFGEM);
        columnaFGEM.getStyleClass().add("col-numero");
        return columnaFGEM;
    }
}
