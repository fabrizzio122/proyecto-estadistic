package com.businessanalyzer.controller;

import com.businessanalyzer.analytics.Margenes;
import com.businessanalyzer.analytics.ResumenCatalogo;
import com.businessanalyzer.config.AppContext;
import com.businessanalyzer.model.Categoria;
import com.businessanalyzer.model.DatosCategoria;
import com.businessanalyzer.model.DatosProducto;
import com.businessanalyzer.model.Empresa;
import com.businessanalyzer.model.Moneda;
import com.businessanalyzer.model.Permiso;
import com.businessanalyzer.model.Producto;
import com.businessanalyzer.model.RegistroPrecio;
import com.businessanalyzer.model.TipoItem;
import com.businessanalyzer.model.UnidadMedida;
import com.businessanalyzer.service.CatalogoService;
import com.businessanalyzer.ui.Dialogos;
import com.businessanalyzer.ui.Errores;
import com.businessanalyzer.ui.Formularios;
import com.businessanalyzer.ui.Notificador;
import com.businessanalyzer.ui.TarjetaIndicador;
import com.businessanalyzer.ui.VistaModulo;
import com.businessanalyzer.util.Montos;
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
public class ProductosController implements VistaModulo {

    private static final String TODOS_LOS_TIPOS = "Todos los tipos";
    private static final Categoria TODAS = new Categoria(-1, -1, "Todas las categorías", null, 0);
    private static final Categoria SIN_CATEGORIA = new Categoria(0, 0, "Sin categoría", null, 0);
    /** Por debajo de este margen sobre el precio se resalta en ámbar. */
    private static final BigDecimal MARGEN_BAJO = new BigDecimal("0.20");

    private final AppContext contexto;
    private final ObservableList<Producto> productos = FXCollections.observableArrayList();
    private final FilteredList<Producto> filtrados = new FilteredList<>(productos);
    private final ObservableList<Categoria> categorias = FXCollections.observableArrayList();
    private Moneda moneda = Moneda.PEN;

    private final TarjetaIndicador kpiItems = new TarjetaIndicador("Ítems activos", "fth-package");
    private final TarjetaIndicador kpiMargen = new TarjetaIndicador("Margen promedio", "fth-percent");
    private final TarjetaIndicador kpiNegativos = new TarjetaIndicador("Con margen negativo", "fth-alert-triangle");
    private final TarjetaIndicador kpiCategorias = new TarjetaIndicador("Categorías", "fth-folder");

    @FXML private HBox indicadores;
    @FXML private TextField txtBuscar;
    @FXML private ComboBox<String> cmbFiltroTipo;
    @FXML private ComboBox<Categoria> cmbFiltroCategoria;
    @FXML private CheckBox chkArchivados;
    @FXML private TableView<Producto> tabla;
    @FXML private TableColumn<Producto, String> colCodigo;
    @FXML private TableColumn<Producto, String> colNombre;
    @FXML private TableColumn<Producto, String> colTipo;
    @FXML private TableColumn<Producto, String> colCategoria;
    @FXML private TableColumn<Producto, String> colUnidad;
    @FXML private TableColumn<Producto, BigDecimal> colCosto;
    @FXML private TableColumn<Producto, BigDecimal> colPrecio;
    @FXML private TableColumn<Producto, BigDecimal> colMargen;
    @FXML private TableColumn<Producto, Producto> colMargenPct;
    @FXML private Button btnNuevo;
    @FXML private Button btnEditar;
    @FXML private MenuButton menuAcciones;
    @FXML private MenuItem itemHistorial;
    @FXML private MenuItem itemEstado;

    @FXML private TableView<Categoria> tablaCategorias;
    @FXML private TableColumn<Categoria, String> colCatNombre;
    @FXML private TableColumn<Categoria, String> colCatDescripcion;
    @FXML private TableColumn<Categoria, Number> colCatCantidad;
    @FXML private Button btnNuevaCategoria;
    @FXML private Button btnEditarCategoria;
    @FXML private Button btnEliminarCategoria;

    public ProductosController(AppContext contexto) {
        this.contexto = contexto;
    }

    @FXML
    private void initialize() {
        indicadores.getChildren().addAll(kpiItems, kpiMargen, kpiNegativos, kpiCategorias);
        configurarFiltros();
        configurarTablaProductos();
        configurarTablaCategorias();

        boolean gestiona = contexto.sesion().tienePermiso(Permiso.PRODUCTOS_GESTIONAR);
        for (Node nodo : List.of(btnNuevo, btnEditar, btnNuevaCategoria, btnEditarCategoria, btnEliminarCategoria)) {
            nodo.setVisible(gestiona);
            nodo.setManaged(gestiona);
        }
        itemEstado.setVisible(gestiona);
    }

    @Override
    public void alMostrar() {
        cargar();
    }

    // ------------------------------------------------------------------ carga

    @FXML
    private void cargar() {
        Empresa empresa = contexto.sesion().empresaActiva().orElse(null);
        boolean hayEmpresa = empresa != null;
        btnNuevo.setDisable(!hayEmpresa);
        btnNuevaCategoria.setDisable(!hayEmpresa);
        if (!hayEmpresa) {
            productos.clear();
            categorias.clear();
            String mensaje = "Selecciona una empresa en la barra superior para ver su catálogo.";
            tabla.setPlaceholder(new Label(mensaje));
            tablaCategorias.setPlaceholder(new Label(mensaje));
            actualizarIndicadores();
            return;
        }
        moneda = empresa.moneda();
        tabla.setPlaceholder(new Label("No hay productos ni servicios. Usa \"Nuevo\" para registrar el primero."));
        tablaCategorias.setPlaceholder(new Label("No hay categorías registradas."));

        Long seleccion = seleccionado() == null ? null : seleccionado().id();
        try {
            categorias.setAll(servicio().listarCategorias());
            productos.setAll(servicio().listarProductos(chkArchivados.isSelected()));
        } catch (RuntimeException e) {
            Notificador.error(Errores.mensaje(e));
            return;
        }
        refrescarFiltroCategorias();
        if (seleccion != null) {
            tabla.getItems().stream().filter(p -> p.id() == seleccion).findFirst()
                    .ifPresent(p -> tabla.getSelectionModel().select(p));
        }
        tabla.refresh();
        actualizarIndicadores();
        actualizarAcciones();
    }

    private void actualizarIndicadores() {
        ResumenCatalogo resumen = ResumenCatalogo.de(productos);
        kpiItems.actualizar(String.valueOf(resumen.total()),
                plural(resumen.productos(), "producto", "productos") + " · "
                        + plural(resumen.servicios(), "servicio", "servicios"));
        kpiMargen.actualizar(Montos.porcentaje(resumen.margenPromedio()), "Promedio simple sobre el precio de venta");
        kpiNegativos.actualizar(String.valueOf(resumen.conMargenNegativo()),
                resumen.conMargenNegativo() == 0 ? "Ningún ítem se vende bajo su costo" : "Se venden por debajo del costo");
        kpiCategorias.actualizar(String.valueOf(categorias.size()), "Para agrupar el análisis");
    }

    // ------------------------------------------------------------------ productos

    @FXML
    private void nuevo() {
        FormularioProducto formulario = new FormularioProducto(null);
        Producto[] creado = new Producto[1];
        boolean ok = Dialogos.formulario(ventana(), "Nuevo producto o servicio",
                "Deja el código vacío para que el sistema asigne uno. Los precios se registran sin impuestos.",
                formulario.contenido(), "Registrar", () -> creado[0] = servicio().crearProducto(formulario.datos()));
        if (ok) {
            cargar();
            seleccionar(creado[0].id());
            Notificador.exito(creado[0].tipo().descripcion() + " \"" + creado[0].nombre() + "\" registrado con el código "
                    + creado[0].codigo() + ".");
        }
    }

    @FXML
    private void editar() {
        Producto producto = seleccionado();
        if (producto == null || !contexto.sesion().tienePermiso(Permiso.PRODUCTOS_GESTIONAR)) {
            return;
        }
        FormularioProducto formulario = new FormularioProducto(producto);
        boolean ok = Dialogos.formulario(ventana(), "Editar " + producto.tipo().descripcion().toLowerCase(),
                null, formulario.contenido(), "Guardar cambios",
                () -> servicio().actualizarProducto(producto.id(), formulario.datos()));
        if (ok) {
            cargar();
            Notificador.exito("\"" + producto.nombre() + "\" actualizado.");
        }
    }

    @FXML
    private void cambiarEstado() {
        Producto producto = seleccionado();
        if (producto == null) {
            return;
        }
        boolean reactivar = !producto.activo();
        String mensaje = reactivar
                ? "¿Reactivar \"" + producto.nombre() + "\"?"
                : "¿Archivar \"" + producto.nombre() + "\"? Dejará de aparecer en el catálogo, pero se "
                + "conservarán su historial y sus operaciones.";
        if (!Dialogos.confirmar(ventana(), reactivar ? "Reactivar" : "Archivar", mensaje)) {
            return;
        }
        try {
            servicio().cambiarEstadoProducto(producto.id(), reactivar);
            cargar();
            Notificador.exito(reactivar ? "Reactivado." : "Archivado.");
        } catch (RuntimeException e) {
            Notificador.error(Errores.mensaje(e));
        }
    }

    @FXML
    private void verHistorial() {
        Producto producto = seleccionado();
        if (producto == null) {
            return;
        }
        List<RegistroPrecio> historial;
        try {
            historial = servicio().historialPrecios(producto.id());
        } catch (RuntimeException e) {
            Notificador.error(Errores.mensaje(e));
            return;
        }
        TableView<RegistroPrecio> tablaHistorial = new TableView<>(FXCollections.observableArrayList(historial));
        tablaHistorial.getColumns().add(columna("Vigente desde", r ->
                com.businessanalyzer.util.Fechas.paraMostrar(r.fecha()), 130, false));
        tablaHistorial.getColumns().add(columna("Costo", r -> Montos.formatear(r.costoUnitario(), moneda), 100, true));
        tablaHistorial.getColumns().add(columna("Precio", r -> Montos.formatear(r.precioVenta(), moneda), 100, true));
        tablaHistorial.getColumns().add(columna("Margen %", r -> Montos.porcentaje(r.margen().sobrePrecio()), 90, true));
        tablaHistorial.getColumns().add(columna("Usuario", r -> Objects.requireNonNullElse(r.usuario(), ""), 110, false));
        tablaHistorial.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        tablaHistorial.setPrefHeight(280);
        Dialogos.informacion(ventana(), "Historial de precios",
                producto.codigo() + " — " + producto.nombre() + ". Del cambio más reciente al más antiguo.",
                tablaHistorial);
    }

    // ------------------------------------------------------------------ categorías

    @FXML
    private void nuevaCategoria() {
        TextField nombre = new TextField();
        TextField descripcion = new TextField();
        GridPane grilla = Formularios.grilla();
        Formularios.fila(grilla, "Nombre *", nombre);
        Formularios.fila(grilla, "Descripción", descripcion);
        boolean ok = Dialogos.formulario(ventana(), "Nueva categoría", null, grilla, "Crear categoría",
                () -> servicio().crearCategoria(new DatosCategoria(nombre.getText(), descripcion.getText())));
        if (ok) {
            cargar();
            Notificador.exito("Categoría creada.");
        }
    }

    @FXML
    private void editarCategoria() {
        Categoria categoria = tablaCategorias.getSelectionModel().getSelectedItem();
        if (categoria == null) {
            return;
        }
        TextField nombre = new TextField(categoria.nombre());
        TextField descripcion = new TextField(Objects.requireNonNullElse(categoria.descripcion(), ""));
        GridPane grilla = Formularios.grilla();
        Formularios.fila(grilla, "Nombre *", nombre);
        Formularios.fila(grilla, "Descripción", descripcion);
        boolean ok = Dialogos.formulario(ventana(), "Editar categoría", null, grilla, "Guardar cambios",
                () -> servicio().actualizarCategoria(categoria.id(),
                        new DatosCategoria(nombre.getText(), descripcion.getText())));
        if (ok) {
            cargar();
            Notificador.exito("Categoría actualizada.");
        }
    }

    @FXML
    private void eliminarCategoria() {
        Categoria categoria = tablaCategorias.getSelectionModel().getSelectedItem();
        if (categoria == null || !Dialogos.confirmar(ventana(), "Eliminar categoría",
                "¿Eliminar la categoría \"" + categoria.nombre() + "\"? Esta acción no se puede deshacer.")) {
            return;
        }
        try {
            servicio().eliminarCategoria(categoria.id());
            cargar();
            Notificador.exito("Categoría eliminada.");
        } catch (RuntimeException e) {
            Notificador.error(Errores.mensaje(e));
        }
    }

    // ------------------------------------------------------------------ configuración de tablas

    private void configurarFiltros() {
        cmbFiltroTipo.getItems().setAll(TODOS_LOS_TIPOS, TipoItem.PRODUCTO.descripcion() + "s",
                TipoItem.SERVICIO.descripcion() + "s");
        cmbFiltroTipo.setValue(TODOS_LOS_TIPOS);
        cmbFiltroCategoria.getItems().setAll(TODAS);
        cmbFiltroCategoria.setValue(TODAS);
        txtBuscar.textProperty().addListener((o, a, n) -> aplicarFiltro());
        cmbFiltroTipo.valueProperty().addListener((o, a, n) -> aplicarFiltro());
        cmbFiltroCategoria.valueProperty().addListener((o, a, n) -> aplicarFiltro());
    }

    private void refrescarFiltroCategorias() {
        Categoria elegida = cmbFiltroCategoria.getValue();
        List<Categoria> opciones = new ArrayList<>();
        opciones.add(TODAS);
        opciones.add(SIN_CATEGORIA);
        opciones.addAll(categorias);
        cmbFiltroCategoria.getItems().setAll(opciones);
        cmbFiltroCategoria.setValue(opciones.stream()
                .filter(c -> elegida != null && c.id() == elegida.id()).findFirst().orElse(TODAS));
    }

    private void aplicarFiltro() {
        String texto = txtBuscar.getText() == null ? "" : txtBuscar.getText().strip().toLowerCase(Locale.ROOT);
        String tipo = cmbFiltroTipo.getValue();
        Categoria categoria = cmbFiltroCategoria.getValue();
        filtrados.setPredicate(p -> {
            boolean coincideTexto = texto.isEmpty()
                    || p.codigo().toLowerCase(Locale.ROOT).contains(texto)
                    || p.nombre().toLowerCase(Locale.ROOT).contains(texto);
            boolean coincideTipo = tipo == null || TODOS_LOS_TIPOS.equals(tipo)
                    || tipo.startsWith(p.tipo().descripcion());
            boolean coincideCategoria = categoria == null || categoria.id() == TODAS.id()
                    || (categoria.id() == SIN_CATEGORIA.id() ? p.categoriaId() == null
                    : Objects.equals(p.categoriaId(), categoria.id()));
            return coincideTexto && coincideTipo && coincideCategoria;
        });
    }

    private void configurarTablaProductos() {
        colCodigo.setCellValueFactory(c -> texto(c.getValue().codigo()));
        colNombre.setCellValueFactory(c -> texto(c.getValue().nombre()));
        colTipo.setCellValueFactory(c -> texto(c.getValue().tipo().descripcion()));
        colCategoria.setCellValueFactory(c -> texto(Objects.requireNonNullElse(c.getValue().categoriaNombre(), "")));
        colUnidad.setCellValueFactory(c -> texto(c.getValue().unidad().abreviatura()));

        colCosto.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().costoUnitario()));
        colPrecio.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().precioVenta()));
        colMargen.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().margen().unitario()));
        colCosto.setCellFactory(col -> new CeldaMonto());
        colPrecio.setCellFactory(col -> new CeldaMonto());
        colMargen.setCellFactory(col -> new CeldaMonto());

        colMargenPct.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue()));
        colMargenPct.setComparator(Comparator.comparing(p -> p.margen().sobrePrecio(),
                Comparator.nullsFirst(Comparator.naturalOrder())));
        colMargenPct.setCellFactory(col -> new CeldaMargen());

        SortedList<Producto> ordenados = new SortedList<>(filtrados);
        ordenados.comparatorProperty().bind(tabla.comparatorProperty());
        tabla.setItems(ordenados);
        tabla.getSelectionModel().selectedItemProperty().addListener((o, a, n) -> actualizarAcciones());
        tabla.setRowFactory(t -> {
            TableRow<Producto> fila = new TableRow<>() {
                @Override
                protected void updateItem(Producto producto, boolean vacio) {
                    super.updateItem(producto, vacio);
                    pseudoClassStateChanged(javafx.css.PseudoClass.getPseudoClass("archivado"),
                            !vacio && producto != null && !producto.activo());
                }
            };
            fila.setOnMouseClicked(e -> {
                if (e.getButton() == MouseButton.PRIMARY && e.getClickCount() == 2 && !fila.isEmpty()) {
                    editar();
                }
            });
            return fila;
        });
    }

    private void configurarTablaCategorias() {
        colCatNombre.setCellValueFactory(c -> texto(c.getValue().nombre()));
        colCatDescripcion.setCellValueFactory(c -> texto(Objects.requireNonNullElse(c.getValue().descripcion(), "")));
        colCatCantidad.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().cantidadItems()));
        tablaCategorias.setItems(categorias);
        tablaCategorias.getSelectionModel().selectedItemProperty().addListener((o, a, n) -> {
            btnEditarCategoria.setDisable(n == null);
            btnEliminarCategoria.setDisable(n == null);
        });
        btnEditarCategoria.setDisable(true);
        btnEliminarCategoria.setDisable(true);
    }

    private void actualizarAcciones() {
        Producto producto = seleccionado();
        btnEditar.setDisable(producto == null);
        menuAcciones.setDisable(producto == null);
        if (producto != null) {
            itemEstado.setText(producto.activo() ? "Archivar" : "Reactivar");
        }
    }

    // ------------------------------------------------------------------ utilidades

    private void seleccionar(long id) {
        tabla.getItems().stream().filter(p -> p.id() == id).findFirst()
                .ifPresent(p -> {
                    tabla.getSelectionModel().select(p);
                    tabla.scrollTo(p);
                });
    }

    private Producto seleccionado() {
        return tabla.getSelectionModel().getSelectedItem();
    }

    private CatalogoService servicio() {
        return contexto.catalogo();
    }

    private Window ventana() {
        return tabla.getScene().getWindow();
    }

    private static String plural(int cantidad, String singular, String plural) {
        return cantidad + " " + (cantidad == 1 ? singular : plural);
    }

    private static ReadOnlyStringWrapper texto(String valor) {
        return new ReadOnlyStringWrapper(valor);
    }

    private static <T> TableColumn<T, String> columna(String titulo, Function<T, String> valor, double ancho,
                                                      boolean numero) {
        TableColumn<T, String> columna = new TableColumn<>(titulo);
        columna.setCellValueFactory(c -> new ReadOnlyStringWrapper(valor.apply(c.getValue())));
        columna.setPrefWidth(ancho);
        if (numero) {
            columna.getStyleClass().add("col-numero");
        }
        return columna;
    }

    /** Clase de color según el margen: negativo, bajo (menor al 20 %) o saludable. */
    private static String claseMargen(Margenes.Margen margen) {
        if (margen.negativo()) {
            return "margen-negativo";
        }
        if (margen.sobrePrecio() != null && margen.sobrePrecio().compareTo(MARGEN_BAJO) < 0) {
            return "margen-bajo";
        }
        return "margen-ok";
    }

    private final class CeldaMonto extends TableCell<Producto, BigDecimal> {
        @Override
        protected void updateItem(BigDecimal valor, boolean vacio) {
            super.updateItem(valor, vacio);
            getStyleClass().remove("margen-negativo");
            if (vacio || valor == null) {
                setText(null);
                return;
            }
            setText(Montos.formatear(valor, moneda));
            if (valor.signum() < 0) {
                getStyleClass().add("margen-negativo");
            }
        }
    }

    private static final class CeldaMargen extends TableCell<Producto, Producto> {
        @Override
        protected void updateItem(Producto producto, boolean vacio) {
            super.updateItem(producto, vacio);
            getStyleClass().removeAll("margen-negativo", "margen-bajo", "margen-ok");
            if (vacio || producto == null) {
                setText(null);
                return;
            }
            Margenes.Margen margen = producto.margen();
            setText(Montos.porcentaje(margen.sobrePrecio()));
            getStyleClass().add(claseMargen(margen));
        }
    }

    /** Campos del formulario de producto o servicio, con vista previa del margen. */
    private final class FormularioProducto {
        private final ComboBox<TipoItem> cmbTipo = new ComboBox<>(FXCollections.observableArrayList(TipoItem.values()));
        private final TextField txtCodigo = new TextField();
        private final TextField txtNombre = new TextField();
        private final ComboBox<Categoria> cmbCategoria = new ComboBox<>();
        private final ComboBox<UnidadMedida> cmbUnidad =
                new ComboBox<>(FXCollections.observableArrayList(UnidadMedida.values()));
        private final TextField txtCosto = new TextField();
        private final TextField txtPrecio = new TextField();
        private final Label lblMargen = new Label();
        private final TextArea txtDescripcion = new TextArea();

        FormularioProducto(Producto p) {
            List<Categoria> opciones = new ArrayList<>();
            opciones.add(SIN_CATEGORIA);
            opciones.addAll(categorias);
            cmbCategoria.getItems().setAll(opciones);
            txtCodigo.setPromptText("Automático");
            txtCosto.setPromptText("0.00");
            txtPrecio.setPromptText("0.00");
            txtDescripcion.setPrefRowCount(2);
            txtDescripcion.setWrapText(true);
            lblMargen.getStyleClass().add("hint");
            lblMargen.setWrapText(true);

            if (p == null) {
                cmbTipo.setValue(TipoItem.PRODUCTO);
                cmbUnidad.setValue(UnidadMedida.UNIDAD);
                cmbCategoria.setValue(SIN_CATEGORIA);
            } else {
                cmbTipo.setValue(p.tipo());
                txtCodigo.setText(p.codigo());
                txtNombre.setText(p.nombre());
                cmbCategoria.setValue(opciones.stream()
                        .filter(c -> p.categoriaId() != null && c.id() == p.categoriaId())
                        .findFirst().orElse(SIN_CATEGORIA));
                cmbUnidad.setValue(p.unidad());
                txtCosto.setText(Montos.paraEditar(p.costoUnitario()));
                txtPrecio.setText(Montos.paraEditar(p.precioVenta()));
                txtDescripcion.setText(Objects.requireNonNullElse(p.descripcion(), ""));
            }
            // Un servicio nuevo se mide normalmente por servicio u hora.
            cmbTipo.valueProperty().addListener((o, anterior, tipo) -> {
                if (p == null && tipo == TipoItem.SERVICIO && cmbUnidad.getValue() == UnidadMedida.UNIDAD) {
                    cmbUnidad.setValue(UnidadMedida.SERVICIO);
                } else if (p == null && tipo == TipoItem.PRODUCTO && cmbUnidad.getValue() == UnidadMedida.SERVICIO) {
                    cmbUnidad.setValue(UnidadMedida.UNIDAD);
                }
            });
            txtCosto.textProperty().addListener((o, a, n) -> actualizarMargen());
            txtPrecio.textProperty().addListener((o, a, n) -> actualizarMargen());
            actualizarMargen();
        }

        GridPane contenido() {
            GridPane grilla = Formularios.grilla();
            Formularios.fila(grilla, "Tipo *", cmbTipo);
            Formularios.fila(grilla, "Código", txtCodigo);
            Formularios.fila(grilla, "Nombre *", txtNombre);
            Formularios.fila(grilla, "Categoría", cmbCategoria);
            Formularios.fila(grilla, "Unidad de medida *", cmbUnidad);
            Formularios.fila(grilla, "Costo unitario (" + moneda.simbolo() + ") *", txtCosto);
            Formularios.fila(grilla, "Precio de venta (" + moneda.simbolo() + ") *", txtPrecio);
            Formularios.fila(grilla, "", lblMargen);
            Formularios.fila(grilla, "Descripción", txtDescripcion);
            return grilla;
        }

        private void actualizarMargen() {
            lblMargen.getStyleClass().removeAll("margen-negativo", "margen-bajo", "margen-ok");
            try {
                BigDecimal costo = Montos.leer(txtCosto.getText(), "Costo");
                BigDecimal precio = Montos.leer(txtPrecio.getText(), "Precio");
                Margenes.Margen margen = Margenes.calcular(costo, precio);
                lblMargen.setText("Margen: " + Montos.formatear(margen.unitario(), moneda) + " por unidad · "
                        + Montos.porcentaje(margen.sobrePrecio()) + " del precio · "
                        + Montos.porcentaje(margen.sobreCosto()) + " sobre el costo");
                lblMargen.getStyleClass().add(claseMargen(margen));
            } catch (RuntimeException sinDatosCompletos) {
                lblMargen.setText("Escribe el costo y el precio para ver el margen.");
            }
        }

        DatosProducto datos() {
            Categoria categoria = cmbCategoria.getValue();
            return new DatosProducto(cmbTipo.getValue(), txtCodigo.getText(), txtNombre.getText(),
                    txtDescripcion.getText(),
                    categoria == null || categoria.id() == SIN_CATEGORIA.id() ? null : categoria.id(),
                    cmbUnidad.getValue(),
                    Montos.leer(txtCosto.getText(), "Costo unitario"),
                    Montos.leer(txtPrecio.getText(), "Precio de venta"));
        }
    }
}
