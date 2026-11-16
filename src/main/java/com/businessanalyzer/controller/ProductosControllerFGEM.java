package com.businessanalyzer.controller;

import com.businessanalyzer.analytics.MargenesFGEM;
import com.businessanalyzer.analytics.ResumenCatalogoFGEM;
import com.businessanalyzer.config.AppContextFGEM;
import com.businessanalyzer.model.CategoriaFGEM;
import com.businessanalyzer.model.DatosCategoriaFGEM;
import com.businessanalyzer.model.DatosProductoFGEM;
import com.businessanalyzer.model.EmpresaFGEM;
import com.businessanalyzer.model.MonedaFGEM;
import com.businessanalyzer.model.PermisoFGEM;
import com.businessanalyzer.model.ProductoFGEM;
import com.businessanalyzer.model.RegistroPrecioFGEM;
import com.businessanalyzer.model.TipoItemFGEM;
import com.businessanalyzer.model.UnidadMedidaFGEM;
import com.businessanalyzer.service.CatalogoServiceFGEM;
import com.businessanalyzer.ui.DialogosFGEM;
import com.businessanalyzer.ui.ErroresFGEM;
import com.businessanalyzer.ui.FormulariosFGEM;
import com.businessanalyzer.ui.NotificadorFGEM;
import com.businessanalyzer.ui.TarjetaIndicadorFGEM;
import com.businessanalyzer.ui.VistaModuloFGEM;
import com.businessanalyzer.util.MontosFGEM;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.stage.Window;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Function;

/** Módulo "Productos y servicios": catálogo con costos, precios y márgenes, y categorías. */
public class ProductosControllerFGEM implements VistaModuloFGEM {

    private static final String TODOS_LOS_TIPOS_FGEM = "Todos los tipos";
    private static final CategoriaFGEM TODAS_FGEM = new CategoriaFGEM(-1, -1, "Todas las categorías", null, 0);
    private static final CategoriaFGEM SIN_CATEGORIA_FGEM = new CategoriaFGEM(0, 0, "Sin categoría", null, 0);
    /** Por debajo de este margen sobre el precio se resalta en ámbar. */
    private static final BigDecimal MARGEN_BAJO_FGEM = new BigDecimal("0.20");

    private final AppContextFGEM contextoFGEM;
    private final ObservableList<ProductoFGEM> productosFGEM = FXCollections.observableArrayList();
    private final FilteredList<ProductoFGEM> filtradosFGEM = new FilteredList<>(productosFGEM);
    private final ObservableList<CategoriaFGEM> categoriasFGEM = FXCollections.observableArrayList();
    private MonedaFGEM monedaFGEM = MonedaFGEM.PEN;

    private final TarjetaIndicadorFGEM kpiItemsFGEM = new TarjetaIndicadorFGEM("Ítems activos", "fth-package");
    private final TarjetaIndicadorFGEM kpiMargenFGEM = new TarjetaIndicadorFGEM("Margen promedio", "fth-percent");
    private final TarjetaIndicadorFGEM kpiNegativosFGEM = new TarjetaIndicadorFGEM("Con margen negativo", "fth-alert-triangle");
    private final TarjetaIndicadorFGEM kpiCategoriasFGEM = new TarjetaIndicadorFGEM("Categorías", "fth-folder");

    @FXML private HBox indicadoresFGEM;
    @FXML private TextField txtBuscarFGEM;
    @FXML private ComboBox<String> cmbFiltroTipoFGEM;
    @FXML private ComboBox<CategoriaFGEM> cmbFiltroCategoriaFGEM;
    @FXML private CheckBox chkArchivadosFGEM;
    @FXML private TableView<ProductoFGEM> tablaFGEM;
    @FXML private TableColumn<ProductoFGEM, String> colCodigoFGEM;
    @FXML private TableColumn<ProductoFGEM, String> colNombreFGEM;
    @FXML private TableColumn<ProductoFGEM, String> colTipoFGEM;
    @FXML private TableColumn<ProductoFGEM, String> colCategoriaFGEM;
    @FXML private TableColumn<ProductoFGEM, String> colUnidadFGEM;
    @FXML private TableColumn<ProductoFGEM, BigDecimal> colStockFGEM;
    @FXML private TableColumn<ProductoFGEM, BigDecimal> colCostoFGEM;
    @FXML private TableColumn<ProductoFGEM, BigDecimal> colPrecioFGEM;
    @FXML private TableColumn<ProductoFGEM, BigDecimal> colMargenFGEM;
    @FXML private TableColumn<ProductoFGEM, ProductoFGEM> colMargenPctFGEM;
    @FXML private Button btnNuevoFGEM;
    @FXML private Button btnEditarFGEM;
    @FXML private MenuButton menuAccionesFGEM;
    @FXML private MenuItem itemHistorialFGEM;
    @FXML private MenuItem itemEstadoFGEM;

    @FXML private TableView<CategoriaFGEM> tablaCategoriasFGEM;
    @FXML private TableColumn<CategoriaFGEM, String> colCatNombreFGEM;
    @FXML private TableColumn<CategoriaFGEM, String> colCatDescripcionFGEM;
    @FXML private TableColumn<CategoriaFGEM, Number> colCatCantidadFGEM;
    @FXML private Button btnNuevaCategoriaFGEM;
    @FXML private Button btnEditarCategoriaFGEM;
    @FXML private Button btnEliminarCategoriaFGEM;

    public ProductosControllerFGEM(AppContextFGEM contextoFGEM) {
        this.contextoFGEM = contextoFGEM;
    }

    @FXML
    private void initialize() {
        indicadoresFGEM.getChildren().addAll(kpiItemsFGEM, kpiMargenFGEM, kpiNegativosFGEM, kpiCategoriasFGEM);
        configurarFiltrosFGEM();
        configurarTablaProductosFGEM();
        configurarTablaCategoriasFGEM();

        boolean gestionaFGEM = contextoFGEM.sesionFGEM().tienePermisoFGEM(PermisoFGEM.PRODUCTOS_GESTIONAR);
        for (Node nodoFGEM : List.of(btnNuevoFGEM, btnEditarFGEM, btnNuevaCategoriaFGEM, btnEditarCategoriaFGEM, btnEliminarCategoriaFGEM)) {
            nodoFGEM.setVisible(gestionaFGEM);
            nodoFGEM.setManaged(gestionaFGEM);
        }
        itemEstadoFGEM.setVisible(gestionaFGEM);
    }

    @Override
    public void alMostrarFGEM() {
        cargarFGEM();
    }

    // ------------------------------------------------------------------ carga

    @FXML
    private void cargarFGEM() {
        EmpresaFGEM empresaFGEM = contextoFGEM.sesionFGEM().empresaActivaFGEM().orElse(null);
        boolean hayEmpresaFGEM = empresaFGEM != null;
        btnNuevoFGEM.setDisable(!hayEmpresaFGEM);
        btnNuevaCategoriaFGEM.setDisable(!hayEmpresaFGEM);
        if (!hayEmpresaFGEM) {
            productosFGEM.clear();
            categoriasFGEM.clear();
            String mensajeFGEM = "Selecciona una empresa en la barra superior para ver su catálogo.";
            tablaFGEM.setPlaceholder(new Label(mensajeFGEM));
            tablaCategoriasFGEM.setPlaceholder(new Label(mensajeFGEM));
            actualizarIndicadoresFGEM();
            return;
        }
        monedaFGEM = empresaFGEM.monedaFGEM();
        tablaFGEM.setPlaceholder(new Label("No hay productos ni servicios. Usa \"Nuevo\" para registrar el primero."));
        tablaCategoriasFGEM.setPlaceholder(new Label("No hay categorías registradas."));

        Long seleccionFGEM = seleccionadoFGEM() == null ? null : seleccionadoFGEM().idFGEM();
        try {
            categoriasFGEM.setAll(servicioFGEM().listarCategoriasFGEM());
            productosFGEM.setAll(servicioFGEM().listarProductosFGEM(chkArchivadosFGEM.isSelected()));
        } catch (RuntimeException eFGEM) {
            NotificadorFGEM.errorFGEM(ErroresFGEM.mensajeFGEM(eFGEM));
            return;
        }
        refrescarFiltroCategoriasFGEM();
        if (seleccionFGEM != null) {
            tablaFGEM.getItems().stream().filter(pFGEM -> pFGEM.idFGEM() == seleccionFGEM).findFirst()
                    .ifPresent(pFGEM -> tablaFGEM.getSelectionModel().select(pFGEM));
        }
        tablaFGEM.refresh();
        actualizarIndicadoresFGEM();
        actualizarAccionesFGEM();
    }

    private void actualizarIndicadoresFGEM() {
        ResumenCatalogoFGEM resumenFGEM = ResumenCatalogoFGEM.deFGEM(productosFGEM);
        kpiItemsFGEM.actualizarFGEM(String.valueOf(resumenFGEM.totalFGEM()),
                pluralFGEM(resumenFGEM.productosFGEM(), "producto", "productos") + " · "
                        + pluralFGEM(resumenFGEM.serviciosFGEM(), "servicio", "servicios"));
        kpiMargenFGEM.actualizarFGEM(MontosFGEM.porcentajeFGEM(resumenFGEM.margenPromedioFGEM()), "Promedio simple sobre el precio de venta");
        kpiNegativosFGEM.actualizarFGEM(String.valueOf(resumenFGEM.conMargenNegativoFGEM()),
                resumenFGEM.conMargenNegativoFGEM() == 0 ? "Ningún ítem se vende bajo su costo" : "Se venden por debajo del costo");
        kpiCategoriasFGEM.actualizarFGEM(String.valueOf(categoriasFGEM.size()), "Para agrupar el análisis");
    }

    // ------------------------------------------------------------------ productos

    @FXML
    private void nuevoFGEM() {
        FormularioProductoFGEM formularioFGEM = new FormularioProductoFGEM(null);
        ProductoFGEM[] creadoFGEM = new ProductoFGEM[1];
        boolean okFGEM = DialogosFGEM.formularioFGEM(ventanaFGEM(), "Nuevo producto o servicio",
                "Deja el código vacío para que el sistema asigne uno. Los precios se registran sin impuestos.",
                formularioFGEM.contenidoFGEM(), "Registrar", () -> creadoFGEM[0] = servicioFGEM().crearProductoFGEM(formularioFGEM.datosFGEM()));
        if (okFGEM) {
            cargarFGEM();
            seleccionarFGEM(creadoFGEM[0].idFGEM());
            NotificadorFGEM.exitoFGEM(creadoFGEM[0].tipoFGEM().descripcionFGEM() + " \"" + creadoFGEM[0].nombreFGEM() + "\" registrado con el código "
                    + creadoFGEM[0].codigoFGEM() + ".");
        }
    }

    @FXML
    private void editarFGEM() {
        ProductoFGEM productoFGEM = seleccionadoFGEM();
        if (productoFGEM == null || !contextoFGEM.sesionFGEM().tienePermisoFGEM(PermisoFGEM.PRODUCTOS_GESTIONAR)) {
            return;
        }
        FormularioProductoFGEM formularioFGEM = new FormularioProductoFGEM(productoFGEM);
        boolean okFGEM = DialogosFGEM.formularioFGEM(ventanaFGEM(), "Editar " + productoFGEM.tipoFGEM().descripcionFGEM().toLowerCase(),
                null, formularioFGEM.contenidoFGEM(), "Guardar cambios",
                () -> servicioFGEM().actualizarProductoFGEM(productoFGEM.idFGEM(), formularioFGEM.datosFGEM()));
        if (okFGEM) {
            cargarFGEM();
            NotificadorFGEM.exitoFGEM("\"" + productoFGEM.nombreFGEM() + "\" actualizado.");
        }
    }

    @FXML
    private void cambiarEstadoFGEM() {
        ProductoFGEM productoFGEM = seleccionadoFGEM();
        if (productoFGEM == null) {
            return;
        }
        boolean reactivarFGEM = !productoFGEM.activoFGEM();
        String mensajeFGEM = reactivarFGEM
                ? "¿Reactivar \"" + productoFGEM.nombreFGEM() + "\"?"
                : "¿Archivar \"" + productoFGEM.nombreFGEM() + "\"? Dejará de aparecer en el catálogo, pero se "
                + "conservarán su historial y sus operaciones.";
        if (!DialogosFGEM.confirmarFGEM(ventanaFGEM(), reactivarFGEM ? "Reactivar" : "Archivar", mensajeFGEM)) {
            return;
        }
        try {
            servicioFGEM().cambiarEstadoProductoFGEM(productoFGEM.idFGEM(), reactivarFGEM);
            cargarFGEM();
            NotificadorFGEM.exitoFGEM(reactivarFGEM ? "Reactivado." : "Archivado.");
        } catch (RuntimeException eFGEM) {
            NotificadorFGEM.errorFGEM(ErroresFGEM.mensajeFGEM(eFGEM));
        }
    }

    @FXML
    private void verHistorialFGEM() {
        ProductoFGEM productoFGEM = seleccionadoFGEM();
        if (productoFGEM == null) {
            return;
        }
        List<RegistroPrecioFGEM> historialFGEM;
        try {
            historialFGEM = servicioFGEM().historialPreciosFGEM(productoFGEM.idFGEM());
        } catch (RuntimeException eFGEM) {
            NotificadorFGEM.errorFGEM(ErroresFGEM.mensajeFGEM(eFGEM));
            return;
        }
        TableView<RegistroPrecioFGEM> tablaHistorialFGEM = new TableView<>(FXCollections.observableArrayList(historialFGEM));
        tablaHistorialFGEM.getColumns().add(columnaFGEM("Vigente desde", rFGEM ->
                com.businessanalyzer.util.FechasFGEM.paraMostrarFGEM(rFGEM.fechaFGEM()), 130, false));
        tablaHistorialFGEM.getColumns().add(columnaFGEM("Costo", rFGEM -> MontosFGEM.formatearFGEM(rFGEM.costoUnitarioFGEM(), monedaFGEM), 100, true));
        tablaHistorialFGEM.getColumns().add(columnaFGEM("Precio", rFGEM -> MontosFGEM.formatearFGEM(rFGEM.precioVentaFGEM(), monedaFGEM), 100, true));
        tablaHistorialFGEM.getColumns().add(columnaFGEM("Margen %", rFGEM -> MontosFGEM.porcentajeFGEM(rFGEM.margenFGEM().sobrePrecioFGEM()), 90, true));
        tablaHistorialFGEM.getColumns().add(columnaFGEM("Usuario", rFGEM -> Objects.requireNonNullElse(rFGEM.usuarioFGEM(), ""), 110, false));
        tablaHistorialFGEM.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        tablaHistorialFGEM.setPrefHeight(280);
        DialogosFGEM.informacionFGEM(ventanaFGEM(), "Historial de precios",
                productoFGEM.codigoFGEM() + " — " + productoFGEM.nombreFGEM() + ". Del cambio más reciente al más antiguo.",
                tablaHistorialFGEM);
    }

    // ------------------------------------------------------------------ categorías

    @FXML
    private void nuevaCategoriaFGEM() {
        TextField nombreFGEM = new TextField();
        TextField descripcionFGEM = new TextField();
        GridPane grillaFGEM = FormulariosFGEM.grillaFGEM();
        FormulariosFGEM.filaFGEM(grillaFGEM, "Nombre *", nombreFGEM);
        FormulariosFGEM.filaFGEM(grillaFGEM, "Descripción", descripcionFGEM);
        boolean okFGEM = DialogosFGEM.formularioFGEM(ventanaFGEM(), "Nueva categoría", null, grillaFGEM, "Crear categoría",
                () -> servicioFGEM().crearCategoriaFGEM(new DatosCategoriaFGEM(nombreFGEM.getText(), descripcionFGEM.getText())));
        if (okFGEM) {
            cargarFGEM();
            NotificadorFGEM.exitoFGEM("Categoría creada.");
        }
    }

    @FXML
    private void editarCategoriaFGEM() {
        CategoriaFGEM categoriaFGEM = tablaCategoriasFGEM.getSelectionModel().getSelectedItem();
        if (categoriaFGEM == null) {
            return;
        }
        TextField nombreFGEM = new TextField(categoriaFGEM.nombreFGEM());
        TextField descripcionFGEM = new TextField(Objects.requireNonNullElse(categoriaFGEM.descripcionFGEM(), ""));
        GridPane grillaFGEM = FormulariosFGEM.grillaFGEM();
        FormulariosFGEM.filaFGEM(grillaFGEM, "Nombre *", nombreFGEM);
        FormulariosFGEM.filaFGEM(grillaFGEM, "Descripción", descripcionFGEM);
        boolean okFGEM = DialogosFGEM.formularioFGEM(ventanaFGEM(), "Editar categoría", null, grillaFGEM, "Guardar cambios",
                () -> servicioFGEM().actualizarCategoriaFGEM(categoriaFGEM.idFGEM(),
                        new DatosCategoriaFGEM(nombreFGEM.getText(), descripcionFGEM.getText())));
        if (okFGEM) {
            cargarFGEM();
            NotificadorFGEM.exitoFGEM("Categoría actualizada.");
        }
    }

    @FXML
    private void eliminarCategoriaFGEM() {
        CategoriaFGEM categoriaFGEM = tablaCategoriasFGEM.getSelectionModel().getSelectedItem();
        if (categoriaFGEM == null || !DialogosFGEM.confirmarFGEM(ventanaFGEM(), "Eliminar categoría",
                "¿Eliminar la categoría \"" + categoriaFGEM.nombreFGEM() + "\"? Esta acción no se puede deshacer.")) {
            return;
        }
        try {
            servicioFGEM().eliminarCategoriaFGEM(categoriaFGEM.idFGEM());
            cargarFGEM();
            NotificadorFGEM.exitoFGEM("Categoría eliminada.");
        } catch (RuntimeException eFGEM) {
            NotificadorFGEM.errorFGEM(ErroresFGEM.mensajeFGEM(eFGEM));
        }
    }

    // ------------------------------------------------------------------ configuración de tablas

    private void configurarFiltrosFGEM() {
        cmbFiltroTipoFGEM.getItems().setAll(TODOS_LOS_TIPOS_FGEM, TipoItemFGEM.PRODUCTO.descripcionFGEM() + "s",
                TipoItemFGEM.SERVICIO.descripcionFGEM() + "s");
        cmbFiltroTipoFGEM.setValue(TODOS_LOS_TIPOS_FGEM);
        cmbFiltroCategoriaFGEM.getItems().setAll(TODAS_FGEM);
        cmbFiltroCategoriaFGEM.setValue(TODAS_FGEM);
        txtBuscarFGEM.textProperty().addListener((oFGEM, aFGEM, nFGEM) -> aplicarFiltroFGEM());
        cmbFiltroTipoFGEM.valueProperty().addListener((oFGEM, aFGEM, nFGEM) -> aplicarFiltroFGEM());
        cmbFiltroCategoriaFGEM.valueProperty().addListener((oFGEM, aFGEM, nFGEM) -> aplicarFiltroFGEM());
    }

    private void refrescarFiltroCategoriasFGEM() {
        CategoriaFGEM elegidaFGEM = cmbFiltroCategoriaFGEM.getValue();
        List<CategoriaFGEM> opcionesFGEM = new ArrayList<>();
        opcionesFGEM.add(TODAS_FGEM);
        opcionesFGEM.add(SIN_CATEGORIA_FGEM);
        opcionesFGEM.addAll(categoriasFGEM);
        cmbFiltroCategoriaFGEM.getItems().setAll(opcionesFGEM);
        cmbFiltroCategoriaFGEM.setValue(opcionesFGEM.stream()
                .filter(cFGEM -> elegidaFGEM != null && cFGEM.idFGEM() == elegidaFGEM.idFGEM()).findFirst().orElse(TODAS_FGEM));
    }

    private void aplicarFiltroFGEM() {
        String textoFGEM = txtBuscarFGEM.getText() == null ? "" : txtBuscarFGEM.getText().strip().toLowerCase(Locale.ROOT);
        String tipoFGEM = cmbFiltroTipoFGEM.getValue();
        CategoriaFGEM categoriaFGEM = cmbFiltroCategoriaFGEM.getValue();
        filtradosFGEM.setPredicate(pFGEM -> {
            boolean coincideTextoFGEM = textoFGEM.isEmpty()
                    || pFGEM.codigoFGEM().toLowerCase(Locale.ROOT).contains(textoFGEM)
                    || pFGEM.nombreFGEM().toLowerCase(Locale.ROOT).contains(textoFGEM);
            boolean coincideTipoFGEM = tipoFGEM == null || TODOS_LOS_TIPOS_FGEM.equals(tipoFGEM)
                    || tipoFGEM.startsWith(pFGEM.tipoFGEM().descripcionFGEM());
            boolean coincideCategoriaFGEM = categoriaFGEM == null || categoriaFGEM.idFGEM() == TODAS_FGEM.idFGEM()
                    || (categoriaFGEM.idFGEM() == SIN_CATEGORIA_FGEM.idFGEM() ? pFGEM.categoriaIdFGEM() == null
                    : Objects.equals(pFGEM.categoriaIdFGEM(), categoriaFGEM.idFGEM()));
            return coincideTextoFGEM && coincideTipoFGEM && coincideCategoriaFGEM;
        });
    }

    private void configurarTablaProductosFGEM() {
        colCodigoFGEM.setCellValueFactory(cFGEM -> textoFGEM(cFGEM.getValue().codigoFGEM()));
        colNombreFGEM.setCellValueFactory(cFGEM -> textoFGEM(cFGEM.getValue().nombreFGEM()));
        colTipoFGEM.setCellValueFactory(cFGEM -> textoFGEM(cFGEM.getValue().tipoFGEM().descripcionFGEM()));
        colCategoriaFGEM.setCellValueFactory(cFGEM -> textoFGEM(Objects.requireNonNullElse(cFGEM.getValue().categoriaNombreFGEM(), "")));
        colUnidadFGEM.setCellValueFactory(cFGEM -> textoFGEM(cFGEM.getValue().unidadFGEM().abreviaturaFGEM()));
        colStockFGEM.setCellValueFactory(cFGEM -> new ReadOnlyObjectWrapper<>(cFGEM.getValue().stockFGEM()));
        colStockFGEM.setComparator(Comparator.nullsFirst(Comparator.naturalOrder()));
        colStockFGEM.setCellFactory(colFGEM -> new TableCell<>() {
            @Override
            protected void updateItem(BigDecimal stockFGEM, boolean vacioFGEM) {
                super.updateItem(stockFGEM, vacioFGEM);
                getStyleClass().remove("margen-negativo");
                if (vacioFGEM) {
                    setText(null);
                    return;
                }
                setText(stockFGEM == null ? "—" : MontosFGEM.cantidadFGEM(stockFGEM));
                if (stockFGEM != null && stockFGEM.signum() < 0) {
                    getStyleClass().add("margen-negativo");
                }
            }
        });

        colCostoFGEM.setCellValueFactory(cFGEM -> new ReadOnlyObjectWrapper<>(cFGEM.getValue().costoUnitarioFGEM()));
        colPrecioFGEM.setCellValueFactory(cFGEM -> new ReadOnlyObjectWrapper<>(cFGEM.getValue().precioVentaFGEM()));
        colMargenFGEM.setCellValueFactory(cFGEM -> new ReadOnlyObjectWrapper<>(cFGEM.getValue().margenFGEM().unitarioFGEM()));
        colCostoFGEM.setCellFactory(colFGEM -> new CeldaMontoFGEM());
        colPrecioFGEM.setCellFactory(colFGEM -> new CeldaMontoFGEM());
        colMargenFGEM.setCellFactory(colFGEM -> new CeldaMontoFGEM());

        colMargenPctFGEM.setCellValueFactory(cFGEM -> new ReadOnlyObjectWrapper<>(cFGEM.getValue()));
        colMargenPctFGEM.setComparator(Comparator.comparing(pFGEM -> pFGEM.margenFGEM().sobrePrecioFGEM(),
                Comparator.nullsFirst(Comparator.naturalOrder())));
        colMargenPctFGEM.setCellFactory(colFGEM -> new CeldaMargenFGEM());

        SortedList<ProductoFGEM> ordenadosFGEM = new SortedList<>(filtradosFGEM);
        ordenadosFGEM.comparatorProperty().bind(tablaFGEM.comparatorProperty());
        tablaFGEM.setItems(ordenadosFGEM);
        tablaFGEM.getSelectionModel().selectedItemProperty().addListener((oFGEM, aFGEM, nFGEM) -> actualizarAccionesFGEM());
        tablaFGEM.setRowFactory(tFGEM -> {
            TableRow<ProductoFGEM> filaFGEM = new TableRow<>() {
                @Override
                protected void updateItem(ProductoFGEM productoFGEM, boolean vacioFGEM) {
                    super.updateItem(productoFGEM, vacioFGEM);
                    pseudoClassStateChanged(javafx.css.PseudoClass.getPseudoClass("archivado"),
                            !vacioFGEM && productoFGEM != null && !productoFGEM.activoFGEM());
                }
            };
            filaFGEM.setOnMouseClicked(eFGEM -> {
                if (eFGEM.getButton() == MouseButton.PRIMARY && eFGEM.getClickCount() == 2 && !filaFGEM.isEmpty()) {
                    editarFGEM();
                }
            });
            return filaFGEM;
        });
    }

    private void configurarTablaCategoriasFGEM() {
        colCatNombreFGEM.setCellValueFactory(cFGEM -> textoFGEM(cFGEM.getValue().nombreFGEM()));
        colCatDescripcionFGEM.setCellValueFactory(cFGEM -> textoFGEM(Objects.requireNonNullElse(cFGEM.getValue().descripcionFGEM(), "")));
        colCatCantidadFGEM.setCellValueFactory(cFGEM -> new ReadOnlyObjectWrapper<>(cFGEM.getValue().cantidadItemsFGEM()));
        tablaCategoriasFGEM.setItems(categoriasFGEM);
        tablaCategoriasFGEM.getSelectionModel().selectedItemProperty().addListener((oFGEM, aFGEM, nFGEM) -> {
            btnEditarCategoriaFGEM.setDisable(nFGEM == null);
            btnEliminarCategoriaFGEM.setDisable(nFGEM == null);
        });
        btnEditarCategoriaFGEM.setDisable(true);
        btnEliminarCategoriaFGEM.setDisable(true);
    }

    private void actualizarAccionesFGEM() {
        ProductoFGEM productoFGEM = seleccionadoFGEM();
        btnEditarFGEM.setDisable(productoFGEM == null);
        menuAccionesFGEM.setDisable(productoFGEM == null);
        if (productoFGEM != null) {
            itemEstadoFGEM.setText(productoFGEM.activoFGEM() ? "Archivar" : "Reactivar");
        }
    }

    // ------------------------------------------------------------------ utilidades

    private void seleccionarFGEM(long idFGEM) {
        tablaFGEM.getItems().stream().filter(pFGEM -> pFGEM.idFGEM() == idFGEM).findFirst()
                .ifPresent(pFGEM -> {
                    tablaFGEM.getSelectionModel().select(pFGEM);
                    tablaFGEM.scrollTo(pFGEM);
                });
    }

    private ProductoFGEM seleccionadoFGEM() {
        return tablaFGEM.getSelectionModel().getSelectedItem();
    }

    private CatalogoServiceFGEM servicioFGEM() {
        return contextoFGEM.catalogoFGEM();
    }

    private Window ventanaFGEM() {
        return tablaFGEM.getScene().getWindow();
    }

    private static String pluralFGEM(int cantidadFGEM, String singularFGEM, String pluralFGEM) {
        return cantidadFGEM + " " + (cantidadFGEM == 1 ? singularFGEM : pluralFGEM);
    }

    private static ReadOnlyStringWrapper textoFGEM(String valorFGEM) {
        return new ReadOnlyStringWrapper(valorFGEM);
    }

    private static <T> TableColumn<T, String> columnaFGEM(String tituloFGEM, Function<T, String> valorFGEM, double anchoFGEM,
                                                      boolean numeroFGEM) {
        TableColumn<T, String> columnaFGEM = new TableColumn<>(tituloFGEM);
        columnaFGEM.setCellValueFactory(cFGEM -> new ReadOnlyStringWrapper(valorFGEM.apply(cFGEM.getValue())));
        columnaFGEM.setPrefWidth(anchoFGEM);
        if (numeroFGEM) {
            columnaFGEM.getStyleClass().add("col-numero");
        }
        return columnaFGEM;
    }

    /** Clase de color según el margen: negativo, bajo (menor al 20 %) o saludable. */
    private static String claseMargenFGEM(MargenesFGEM.MargenFGEM margenFGEM) {
        if (margenFGEM.negativoFGEM()) {
            return "margen-negativo";
        }
        if (margenFGEM.sobrePrecioFGEM() != null && margenFGEM.sobrePrecioFGEM().compareTo(MARGEN_BAJO_FGEM) < 0) {
            return "margen-bajo";
        }
        return "margen-ok";
    }

    private final class CeldaMontoFGEM extends TableCell<ProductoFGEM, BigDecimal> {
        @Override
        protected void updateItem(BigDecimal valorFGEM, boolean vacioFGEM) {
            super.updateItem(valorFGEM, vacioFGEM);
            getStyleClass().remove("margen-negativo");
            if (vacioFGEM || valorFGEM == null) {
                setText(null);
                return;
            }
            setText(MontosFGEM.formatearFGEM(valorFGEM, monedaFGEM));
            if (valorFGEM.signum() < 0) {
                getStyleClass().add("margen-negativo");
            }
        }
    }

    private static final class CeldaMargenFGEM extends TableCell<ProductoFGEM, ProductoFGEM> {
        @Override
        protected void updateItem(ProductoFGEM productoFGEM, boolean vacioFGEM) {
            super.updateItem(productoFGEM, vacioFGEM);
            getStyleClass().removeAll("margen-negativo", "margen-bajo", "margen-ok");
            if (vacioFGEM || productoFGEM == null) {
                setText(null);
                return;
            }
            MargenesFGEM.MargenFGEM margenFGEM = productoFGEM.margenFGEM();
            setText(MontosFGEM.porcentajeFGEM(margenFGEM.sobrePrecioFGEM()));
            getStyleClass().add(claseMargenFGEM(margenFGEM));
        }
    }

    /** Campos del formulario de producto o servicio, con vista previa del margen. */
    private final class FormularioProductoFGEM {
        private final ComboBox<TipoItemFGEM> cmbTipoFGEM = new ComboBox<>(FXCollections.observableArrayList(TipoItemFGEM.values()));
        private final TextField txtCodigoFGEM = new TextField();
        private final TextField txtNombreFGEM = new TextField();
        private final ComboBox<CategoriaFGEM> cmbCategoriaFGEM = new ComboBox<>();
        private final ComboBox<UnidadMedidaFGEM> cmbUnidadFGEM =
                new ComboBox<>(FXCollections.observableArrayList(UnidadMedidaFGEM.values()));
        private final TextField txtCostoFGEM = new TextField();
        private final TextField txtPrecioFGEM = new TextField();
        private final Label lblMargenFGEM = new Label();
        private final TextArea txtDescripcionFGEM = new TextArea();

        FormularioProductoFGEM(ProductoFGEM pFGEM) {
            List<CategoriaFGEM> opcionesFGEM = new ArrayList<>();
            opcionesFGEM.add(SIN_CATEGORIA_FGEM);
            opcionesFGEM.addAll(categoriasFGEM);
            cmbCategoriaFGEM.getItems().setAll(opcionesFGEM);
            txtCodigoFGEM.setPromptText("Automático");
            txtCostoFGEM.setPromptText("0.00");
            txtPrecioFGEM.setPromptText("0.00");
            txtDescripcionFGEM.setPrefRowCount(2);
            txtDescripcionFGEM.setWrapText(true);
            lblMargenFGEM.getStyleClass().add("hint");
            lblMargenFGEM.setWrapText(true);

            if (pFGEM == null) {
                cmbTipoFGEM.setValue(TipoItemFGEM.PRODUCTO);
                cmbUnidadFGEM.setValue(UnidadMedidaFGEM.UNIDAD);
                cmbCategoriaFGEM.setValue(SIN_CATEGORIA_FGEM);
            } else {
                cmbTipoFGEM.setValue(pFGEM.tipoFGEM());
                txtCodigoFGEM.setText(pFGEM.codigoFGEM());
                txtNombreFGEM.setText(pFGEM.nombreFGEM());
                cmbCategoriaFGEM.setValue(opcionesFGEM.stream()
                        .filter(cFGEM -> pFGEM.categoriaIdFGEM() != null && cFGEM.idFGEM() == pFGEM.categoriaIdFGEM())
                        .findFirst().orElse(SIN_CATEGORIA_FGEM));
                cmbUnidadFGEM.setValue(pFGEM.unidadFGEM());
                txtCostoFGEM.setText(MontosFGEM.paraEditarFGEM(pFGEM.costoUnitarioFGEM()));
                txtPrecioFGEM.setText(MontosFGEM.paraEditarFGEM(pFGEM.precioVentaFGEM()));
                txtDescripcionFGEM.setText(Objects.requireNonNullElse(pFGEM.descripcionFGEM(), ""));
            }
            // Un servicio nuevo se mide normalmente por servicio u hora.
            cmbTipoFGEM.valueProperty().addListener((oFGEM, anteriorFGEM, tipoFGEM) -> {
                if (pFGEM == null && tipoFGEM == TipoItemFGEM.SERVICIO && cmbUnidadFGEM.getValue() == UnidadMedidaFGEM.UNIDAD) {
                    cmbUnidadFGEM.setValue(UnidadMedidaFGEM.SERVICIO);
                } else if (pFGEM == null && tipoFGEM == TipoItemFGEM.PRODUCTO && cmbUnidadFGEM.getValue() == UnidadMedidaFGEM.SERVICIO) {
                    cmbUnidadFGEM.setValue(UnidadMedidaFGEM.UNIDAD);
                }
            });
            txtCostoFGEM.textProperty().addListener((oFGEM, aFGEM, nFGEM) -> actualizarMargenFGEM());
            txtPrecioFGEM.textProperty().addListener((oFGEM, aFGEM, nFGEM) -> actualizarMargenFGEM());
            actualizarMargenFGEM();
        }

        GridPane contenidoFGEM() {
            GridPane grillaFGEM = FormulariosFGEM.grillaFGEM();
            FormulariosFGEM.filaFGEM(grillaFGEM, "Tipo *", cmbTipoFGEM);
            FormulariosFGEM.filaFGEM(grillaFGEM, "Código", txtCodigoFGEM);
            FormulariosFGEM.filaFGEM(grillaFGEM, "Nombre *", txtNombreFGEM);
            FormulariosFGEM.filaFGEM(grillaFGEM, "Categoría", cmbCategoriaFGEM);
            FormulariosFGEM.filaFGEM(grillaFGEM, "Unidad de medida *", cmbUnidadFGEM);
            FormulariosFGEM.filaFGEM(grillaFGEM, "Costo unitario (" + monedaFGEM.simboloFGEM() + ") *", txtCostoFGEM);
            FormulariosFGEM.filaFGEM(grillaFGEM, "Precio de venta (" + monedaFGEM.simboloFGEM() + ") *", txtPrecioFGEM);
            FormulariosFGEM.filaFGEM(grillaFGEM, "", lblMargenFGEM);
            FormulariosFGEM.filaFGEM(grillaFGEM, "Descripción", txtDescripcionFGEM);
            return grillaFGEM;
        }

        private void actualizarMargenFGEM() {
            lblMargenFGEM.getStyleClass().removeAll("margen-negativo", "margen-bajo", "margen-ok");
            try {
                BigDecimal costoFGEM = MontosFGEM.leerFGEM(txtCostoFGEM.getText(), "Costo");
                BigDecimal precioFGEM = MontosFGEM.leerFGEM(txtPrecioFGEM.getText(), "Precio");
                MargenesFGEM.MargenFGEM margenFGEM = MargenesFGEM.calcularFGEM(costoFGEM, precioFGEM);
                lblMargenFGEM.setText("Margen: " + MontosFGEM.formatearFGEM(margenFGEM.unitarioFGEM(), monedaFGEM) + " por unidad · "
                        + MontosFGEM.porcentajeFGEM(margenFGEM.sobrePrecioFGEM()) + " del precio · "
                        + MontosFGEM.porcentajeFGEM(margenFGEM.sobreCostoFGEM()) + " sobre el costo");
                lblMargenFGEM.getStyleClass().add(claseMargenFGEM(margenFGEM));
            } catch (RuntimeException sinDatosCompletosFGEM) {
                lblMargenFGEM.setText("Escribe el costo y el precio para ver el margen.");
            }
        }

        DatosProductoFGEM datosFGEM() {
            CategoriaFGEM categoriaFGEM = cmbCategoriaFGEM.getValue();
            return new DatosProductoFGEM(cmbTipoFGEM.getValue(), txtCodigoFGEM.getText(), txtNombreFGEM.getText(),
                    txtDescripcionFGEM.getText(),
                    categoriaFGEM == null || categoriaFGEM.idFGEM() == SIN_CATEGORIA_FGEM.idFGEM() ? null : categoriaFGEM.idFGEM(),
                    cmbUnidadFGEM.getValue(),
                    MontosFGEM.leerFGEM(txtCostoFGEM.getText(), "Costo unitario"),
                    MontosFGEM.leerFGEM(txtPrecioFGEM.getText(), "Precio de venta"));
        }
    }
}
