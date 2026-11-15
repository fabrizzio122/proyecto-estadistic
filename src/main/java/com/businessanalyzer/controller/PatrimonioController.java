package com.businessanalyzer.controller;

import com.businessanalyzer.analytics.patrimonio.BalanceGeneral;
import com.businessanalyzer.analytics.patrimonio.BalanceGeneral.Partida;
import com.businessanalyzer.analytics.patrimonio.Depreciacion;
import com.businessanalyzer.analytics.patrimonio.IndicadoresPatrimonio;
import com.businessanalyzer.analytics.patrimonio.LecturaPatrimonio;
import com.businessanalyzer.analytics.patrimonio.SituacionPatrimonial;
import com.businessanalyzer.config.AppContext;
import com.businessanalyzer.model.Activo;
import com.businessanalyzer.model.CategoriaActivo;
import com.businessanalyzer.model.CategoriaDeuda;
import com.businessanalyzer.model.DatosActivo;
import com.businessanalyzer.model.DatosDeuda;
import com.businessanalyzer.model.Deuda;
import com.businessanalyzer.model.Empresa;
import com.businessanalyzer.model.Moneda;
import com.businessanalyzer.model.PagoDeuda;
import com.businessanalyzer.model.Permiso;
import com.businessanalyzer.service.PatrimonioService;
import com.businessanalyzer.ui.CamposFecha;
import com.businessanalyzer.ui.Dialogos;
import com.businessanalyzer.ui.Errores;
import com.businessanalyzer.ui.Formularios;
import com.businessanalyzer.ui.Notificador;
import com.businessanalyzer.ui.TarjetaIndicador;
import com.businessanalyzer.ui.VistaModulo;
import com.businessanalyzer.util.Fechas;
import com.businessanalyzer.util.Montos;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.css.PseudoClass;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Window;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Function;

/** Módulo "Activos y deudas": registro de bienes y deudas, balance general e indicadores patrimoniales. */
public class PatrimonioController implements VistaModulo {

    private enum TipoFila { TITULO, DETALLE, SUBTOTAL, RESULTADO }

    private record FilaBalance(String concepto, BigDecimal monto, TipoFila tipo) {
    }

    private static final PseudoClass SUBTOTAL = PseudoClass.getPseudoClass("subtotal");
    private static final PseudoClass RESULTADO = PseudoClass.getPseudoClass("resultado");
    private static final PseudoClass ARCHIVADO = PseudoClass.getPseudoClass("archivado");

    private final AppContext contexto;
    private Moneda moneda = Moneda.PEN;
    private BigDecimal totalActivo = BigDecimal.ZERO;

    private final ObservableList<Activo> activos = FXCollections.observableArrayList();
    private final FilteredList<Activo> activosFiltrados = new FilteredList<>(activos);
    private final ObservableList<Deuda> deudas = FXCollections.observableArrayList();
    private final FilteredList<Deuda> deudasFiltradas = new FilteredList<>(deudas);

    private final TarjetaIndicador kpiActivos = new TarjetaIndicador("Total activos", "fth-box");
    private final TarjetaIndicador kpiDeudas = new TarjetaIndicador("Total deudas", "fth-credit-card");
    private final TarjetaIndicador kpiPatrimonio = new TarjetaIndicador("Patrimonio", "fth-shield");
    private final TarjetaIndicador kpiLiquidez = new TarjetaIndicador("Liquidez corriente", "fth-droplet");

    @FXML private HBox indicadores;
    @FXML private TableView<FilaBalance> tablaBalance;
    @FXML private TableColumn<FilaBalance, FilaBalance> colBalConcepto;
    @FXML private TableColumn<FilaBalance, FilaBalance> colBalMonto;
    @FXML private TableColumn<FilaBalance, FilaBalance> colBalPorcentaje;
    @FXML private Label lblCorte;
    @FXML private GridPane gridIndicadores;
    @FXML private VBox lectura;

    @FXML private TextField txtBuscarActivo;
    @FXML private CheckBox chkBajas;
    @FXML private Button btnEditarActivo;
    @FXML private Button btnBajaActivo;
    @FXML private Button btnNuevoActivo;
    @FXML private TableView<Activo> tablaActivos;
    @FXML private TableColumn<Activo, String> colActTipo;
    @FXML private TableColumn<Activo, String> colActDescripcion;
    @FXML private TableColumn<Activo, LocalDate> colActFecha;
    @FXML private TableColumn<Activo, BigDecimal> colActValor;
    @FXML private TableColumn<Activo, String> colActVida;
    @FXML private TableColumn<Activo, BigDecimal> colActDepreciacion;
    @FXML private TableColumn<Activo, BigDecimal> colActLibros;

    @FXML private TextField txtBuscarDeuda;
    @FXML private CheckBox chkAnuladas;
    @FXML private Button btnVerPagos;
    @FXML private Button btnEditarDeuda;
    @FXML private Button btnAnularDeuda;
    @FXML private Button btnPagar;
    @FXML private Button btnNuevaDeuda;
    @FXML private TableView<Deuda> tablaDeudas;
    @FXML private TableColumn<Deuda, String> colDeuAcreedor;
    @FXML private TableColumn<Deuda, String> colDeuTipo;
    @FXML private TableColumn<Deuda, LocalDate> colDeuFecha;
    @FXML private TableColumn<Deuda, BigDecimal> colDeuMonto;
    @FXML private TableColumn<Deuda, BigDecimal> colDeuSaldo;
    @FXML private TableColumn<Deuda, BigDecimal> colDeuCuota;
    @FXML private TableColumn<Deuda, String> colDeuTasa;
    @FXML private TableColumn<Deuda, LocalDate> colDeuVence;
    @FXML private TableColumn<Deuda, String> colDeuEstado;

    public PatrimonioController(AppContext contexto) {
        this.contexto = contexto;
    }

    @FXML
    private void initialize() {
        indicadores.getChildren().addAll(kpiActivos, kpiDeudas, kpiPatrimonio, kpiLiquidez);

        ColumnConstraints conceptos = new ColumnConstraints();
        conceptos.setMinWidth(190);
        ColumnConstraints valores = new ColumnConstraints();
        valores.setHgrow(Priority.ALWAYS);
        gridIndicadores.getColumnConstraints().setAll(conceptos, valores);

        configurarTablaBalance();
        configurarTablaActivos();
        configurarTablaDeudas();
        txtBuscarActivo.textProperty().addListener((o, a, n) -> filtrar());
        txtBuscarDeuda.textProperty().addListener((o, a, n) -> filtrar());

        boolean gestiona = contexto.sesion().tienePermiso(Permiso.FINANZAS_GESTIONAR);
        for (Node nodo : List.of(btnEditarActivo, btnBajaActivo, btnNuevoActivo, btnEditarDeuda, btnAnularDeuda,
                btnPagar, btnNuevaDeuda)) {
            nodo.setVisible(gestiona);
            nodo.setManaged(gestiona);
        }
    }

    @Override
    public void alMostrar() {
        cargar();
    }

    // ------------------------------------------------------------------ carga

    @FXML
    private void cargar() {
        Empresa empresa = contexto.sesion().empresaActiva().orElse(null);
        btnNuevoActivo.setDisable(empresa == null);
        btnNuevaDeuda.setDisable(empresa == null);
        if (empresa == null) {
            activos.clear();
            deudas.clear();
            tablaBalance.getItems().clear();
            String mensaje = "Selecciona una empresa en la barra superior para ver sus activos y deudas.";
            tablaBalance.setPlaceholder(new Label(mensaje));
            tablaActivos.setPlaceholder(new Label(mensaje));
            tablaDeudas.setPlaceholder(new Label(mensaje));
            limpiarIndicadores();
            return;
        }
        moneda = empresa.moneda();
        tablaActivos.setPlaceholder(new Label("No hay activos registrados. Empieza por la caja y los equipos del negocio."));
        tablaDeudas.setPlaceholder(new Label("No hay deudas registradas."));
        try {
            PatrimonioService servicio = servicio();
            activos.setAll(servicio.listarActivos(chkBajas.isSelected()));
            deudas.setAll(servicio.listarDeudas(chkAnuladas.isSelected()));
            mostrarSituacion(servicio.situacion());
        } catch (RuntimeException e) {
            activos.clear();
            deudas.clear();
            limpiarIndicadores();
            Notificador.error(Errores.mensaje(e));
        }
        filtrar();
        actualizarAcciones();
    }

    private void limpiarIndicadores() {
        for (TarjetaIndicador kpi : List.of(kpiActivos, kpiDeudas, kpiPatrimonio, kpiLiquidez)) {
            kpi.actualizar(TarjetaIndicador.SIN_VALOR, "");
        }
        gridIndicadores.getChildren().clear();
        lectura.getChildren().clear();
        lblCorte.setText("");
    }

    private void mostrarSituacion(SituacionPatrimonial s) {
        BalanceGeneral b = s.balance();
        IndicadoresPatrimonio i = s.indicadores();
        totalActivo = b.totalActivo();

        kpiActivos.actualizar(Montos.formatear(b.totalActivo(), moneda),
                "Corriente: " + Montos.formatear(b.totalActivoCorriente(), moneda));
        kpiDeudas.actualizar(Montos.formatear(b.totalPasivo(), moneda),
                b.deudasPendientes() + (b.deudasPendientes() == 1 ? " deuda pendiente" : " deudas pendientes"));
        kpiPatrimonio.actualizar(Montos.formatear(b.patrimonio(), moneda),
                i.endeudamiento() == null ? "Lo que pertenece a los dueños"
                        : "Deudas: " + Montos.porcentaje(i.endeudamiento()) + " de los activos");
        kpiLiquidez.actualizar(i.liquidezCorriente() == null ? TarjetaIndicador.SIN_VALOR
                        : IndicadoresPatrimonio.veces(i.liquidezCorriente()),
                i.liquidezCorriente() == null ? "Sin deudas de corto plazo" : "Activo ÷ pasivo corriente");

        tablaBalance.getItems().setAll(filasBalance(b));
        tablaBalance.setPlaceholder(new Label("Sin datos."));
        lblCorte.setText("Balance al " + Fechas.paraMostrar(b.corte()) + ". Inventario según el stock y el costo actuales; "
                + "activos fijos a su valor actual (ya descontada la depreciación).");
        mostrarIndicadores(s);

        lectura.getChildren().clear();
        for (String frase : LecturaPatrimonio.generar(s, moneda)) {
            Label punto = new Label("•  " + frase);
            punto.setWrapText(true);
            punto.getStyleClass().add("lectura");
            lectura.getChildren().add(punto);
        }
    }

    private static List<FilaBalance> filasBalance(BalanceGeneral b) {
        List<FilaBalance> filas = new ArrayList<>();
        filas.add(new FilaBalance("ACTIVO", null, TipoFila.TITULO));
        detalle(filas, b.activoCorriente());
        filas.add(new FilaBalance("Activo corriente", b.totalActivoCorriente(), TipoFila.SUBTOTAL));
        detalle(filas, b.activoNoCorriente());
        filas.add(new FilaBalance("Activo no corriente", b.totalActivoNoCorriente(), TipoFila.SUBTOTAL));
        filas.add(new FilaBalance("TOTAL ACTIVO", b.totalActivo(), TipoFila.RESULTADO));
        filas.add(new FilaBalance("PASIVO", null, TipoFila.TITULO));
        detalle(filas, b.pasivoCorriente());
        filas.add(new FilaBalance("Pasivo corriente (vence en 12 meses)", b.totalPasivoCorriente(), TipoFila.SUBTOTAL));
        detalle(filas, b.pasivoNoCorriente());
        filas.add(new FilaBalance("Pasivo no corriente", b.totalPasivoNoCorriente(), TipoFila.SUBTOTAL));
        filas.add(new FilaBalance("TOTAL PASIVO", b.totalPasivo(), TipoFila.SUBTOTAL));
        filas.add(new FilaBalance("PATRIMONIO (activo − pasivo)", b.patrimonio(), TipoFila.RESULTADO));
        filas.add(new FilaBalance("TOTAL PASIVO + PATRIMONIO", b.totalPasivo().add(b.patrimonio()), TipoFila.RESULTADO));
        return filas;
    }

    private static void detalle(List<FilaBalance> filas, List<Partida> partidas) {
        for (Partida p : partidas) {
            filas.add(new FilaBalance("    " + p.concepto(), p.monto(), TipoFila.DETALLE));
        }
    }

    private void mostrarIndicadores(SituacionPatrimonial s) {
        BalanceGeneral b = s.balance();
        IndicadoresPatrimonio i = s.indicadores();
        gridIndicadores.getChildren().clear();
        int fila = 0;
        fila = indicador(fila, "Liquidez corriente", i.liquidezCorriente() == null ? "Sin deudas de corto plazo"
                        : IndicadoresPatrimonio.veces(i.liquidezCorriente()),
                semaforoMayor(i.liquidezCorriente(), IndicadoresPatrimonio.LIQUIDEZ_VERDE, IndicadoresPatrimonio.LIQUIDEZ_AMARILLO),
                "Activo corriente ÷ pasivo corriente. Bien desde 1.50; riesgo por debajo de 1.00.");
        fila = indicador(fila, "Prueba ácida", i.pruebaAcida() == null ? "—" : IndicadoresPatrimonio.veces(i.pruebaAcida()),
                semaforoMayor(i.pruebaAcida(), BigDecimal.ONE, new BigDecimal("0.5")),
                "Igual que la liquidez, pero sin contar el inventario. Bien desde 1.00.");
        fila = indicador(fila, "Capital de trabajo", Montos.formatear(i.capitalTrabajo(), moneda),
                i.capitalTrabajo().signum() < 0 ? "margen-negativo" : null,
                "Activo corriente − pasivo corriente: el dinero que queda para operar después de pagar lo del año.");
        fila = indicador(fila, "Endeudamiento", Montos.porcentaje(i.endeudamiento()),
                semaforoMenor(i.endeudamiento(), IndicadoresPatrimonio.ENDEUDAMIENTO_VERDE, IndicadoresPatrimonio.ENDEUDAMIENTO_AMARILLO),
                "Deudas ÷ activos. Bien hasta 50 %; riesgo por encima de 70 %.");
        fila = indicador(fila, "Deuda por cada 1 de patrimonio",
                i.apalancamiento() == null ? (b.patrimonio().signum() <= 0 && b.totalPasivo().signum() > 0
                        ? "Patrimonio negativo" : "—") : IndicadoresPatrimonio.veces(i.apalancamiento()),
                i.apalancamiento() == null && b.patrimonio().signum() <= 0 && b.totalPasivo().signum() > 0
                        ? "margen-negativo" : semaforoMenor(i.apalancamiento(), BigDecimal.ONE, new BigDecimal("2.33")),
                "Deudas ÷ patrimonio (apalancamiento).");
        String meses = i.mesesResultados() == 0 ? "" : " (últimos " + i.mesesResultados() + " meses)";
        fila = indicador(fila, "Rentabilidad del patrimonio", Montos.porcentaje(i.rentabilidadPatrimonio()),
                signo(i.rentabilidadPatrimonio()),
                "Utilidad neta anual ÷ patrimonio" + meses + ". Cuánto rinde al año lo invertido por los dueños.");
        fila = indicador(fila, "Rentabilidad de los activos", Montos.porcentaje(i.rentabilidadActivos()),
                signo(i.rentabilidadActivos()), "Utilidad neta anual ÷ activos" + meses + ".");
        fila = indicador(fila, "Cuotas mensuales", b.cuotasMensuales().signum() == 0 ? "Sin cuotas"
                : Montos.formatear(b.cuotasMensuales(), moneda), null, "Suma de las cuotas de las deudas pendientes.");
        fila = indicador(fila, "Cobertura de cuotas", i.coberturaCuotas() == null ? "—"
                        : IndicadoresPatrimonio.veces(i.coberturaCuotas()) + " veces",
                semaforoMayor(i.coberturaCuotas(), IndicadoresPatrimonio.COBERTURA_VERDE, BigDecimal.ONE),
                "Utilidad operativa mensual ÷ cuotas mensuales. Bien desde 1.30: la capacidad de pago que miran los bancos.");
        indicador(fila, "Depreciación mensual", Montos.formatear(b.depreciacionMensual(), moneda), null,
                "Desgaste mensual de los activos fijos. Acumulada: " + Montos.formatear(b.depreciacionAcumulada(), moneda) + ".");
    }

    private int indicador(int fila, String nombre, String valor, String clase, String ayuda) {
        Label etiqueta = new Label(nombre);
        etiqueta.getStyleClass().add("info-key");
        etiqueta.setTooltip(new Tooltip(ayuda));
        Label dato = new Label(valor);
        dato.getStyleClass().add("info-value");
        if (clase != null) {
            dato.getStyleClass().add(clase);
        }
        dato.setTooltip(new Tooltip(ayuda));
        gridIndicadores.add(etiqueta, 0, fila);
        gridIndicadores.add(dato, 1, fila);
        return fila + 1;
    }

    /** Clase de color cuando más alto es mejor. */
    private static String semaforoMayor(BigDecimal valor, BigDecimal verde, BigDecimal amarillo) {
        if (valor == null) {
            return null;
        }
        if (valor.compareTo(verde) >= 0) {
            return "margen-ok";
        }
        return valor.compareTo(amarillo) >= 0 ? "margen-bajo" : "margen-negativo";
    }

    /** Clase de color cuando más bajo es mejor. */
    private static String semaforoMenor(BigDecimal valor, BigDecimal verde, BigDecimal amarillo) {
        if (valor == null) {
            return null;
        }
        if (valor.compareTo(verde) <= 0) {
            return "margen-ok";
        }
        return valor.compareTo(amarillo) <= 0 ? "margen-bajo" : "margen-negativo";
    }

    private static String signo(BigDecimal valor) {
        return valor == null ? null : valor.signum() < 0 ? "margen-negativo" : null;
    }

    private void filtrar() {
        String activo = normalizar(txtBuscarActivo.getText());
        activosFiltrados.setPredicate(a -> activo.isEmpty()
                || a.descripcion().toLowerCase(Locale.ROOT).contains(activo)
                || a.categoria().descripcion().toLowerCase(Locale.ROOT).contains(activo));
        String deuda = normalizar(txtBuscarDeuda.getText());
        deudasFiltradas.setPredicate(d -> deuda.isEmpty()
                || d.acreedor().toLowerCase(Locale.ROOT).contains(deuda)
                || (d.descripcion() != null && d.descripcion().toLowerCase(Locale.ROOT).contains(deuda))
                || d.categoria().descripcion().toLowerCase(Locale.ROOT).contains(deuda));
    }

    private static String normalizar(String texto) {
        return texto == null ? "" : texto.strip().toLowerCase(Locale.ROOT);
    }

    // ------------------------------------------------------------------ activos

    @FXML
    private void nuevoActivo() {
        FormularioActivo formulario = new FormularioActivo(null);
        Activo[] creado = new Activo[1];
        boolean ok = Dialogos.formulario(ventana(), "Nuevo activo", "Montos sin impuestos.",
                formulario.contenido(), "Registrar", 520, () -> creado[0] = servicio().registrarActivo(formulario.datos()));
        if (ok) {
            cargar();
            seleccionar(tablaActivos, a -> a.id() == creado[0].id());
            Notificador.exito("Activo registrado: " + creado[0].descripcion() + ".");
        }
    }

    @FXML
    private void editarActivo() {
        Activo a = tablaActivos.getSelectionModel().getSelectedItem();
        if (a == null || a.dadoDeBaja() || !contexto.sesion().tienePermiso(Permiso.FINANZAS_GESTIONAR)) {
            return;
        }
        FormularioActivo formulario = new FormularioActivo(a);
        boolean ok = Dialogos.formulario(ventana(), "Editar activo",
                a.categoria().corriente() ? "Actualiza el saldo y su fecha cada vez que cambie." : null,
                formulario.contenido(), "Guardar cambios", 520, () -> servicio().actualizarActivo(a.id(), formulario.datos()));
        if (ok) {
            cargar();
            Notificador.exito("Activo actualizado.");
        }
    }

    @FXML
    private void darDeBaja() {
        Activo a = tablaActivos.getSelectionModel().getSelectedItem();
        if (a == null || a.dadoDeBaja()) {
            return;
        }
        DatePicker fecha = CamposFecha.nuevo(LocalDate.now());
        TextField motivo = new TextField();
        motivo.setPromptText("Ej.: vendido, malogrado, registrado por error");
        GridPane grilla = Formularios.grilla();
        Formularios.fila(grilla, "Fecha de baja *", fecha);
        Formularios.fila(grilla, "Motivo *", motivo);
        Formularios.nota(grilla, "Si lo vendiste, registra el dinero recibido como ingreso en Gestión financiera "
                + "(categoría «Venta de activos»).");
        boolean ok = Dialogos.formulario(ventana(), "Dar de baja un activo",
                "\"" + a.descripcion() + "\" dejará de contar en el balance desde la fecha indicada, pero se conservará "
                        + "en el historial.", grilla, "Dar de baja",
                () -> servicio().darDeBaja(a.id(), CamposFecha.leer(fecha, "baja"), motivo.getText()));
        if (ok) {
            cargar();
            Notificador.exito("Activo dado de baja.");
        }
    }

    // ------------------------------------------------------------------ deudas

    @FXML
    private void nuevaDeuda() {
        FormularioDeuda formulario = new FormularioDeuda(null);
        Deuda[] creada = new Deuda[1];
        boolean ok = Dialogos.formulario(ventana(), "Nueva deuda",
                "Registra el capital adeudado. Si ya pagaste una parte, indica el monto original y registra los pagos, "
                        + "o registra directamente el saldo actual con la fecha de hoy.",
                formulario.contenido(), "Registrar", 520, () -> creada[0] = servicio().registrarDeuda(formulario.datos()));
        if (ok) {
            cargar();
            seleccionar(tablaDeudas, d -> d.id() == creada[0].id());
            Notificador.exito("Deuda registrada: " + Montos.formatear(creada[0].monto(), moneda) + ".");
        }
    }

    @FXML
    private void editarDeuda() {
        Deuda d = tablaDeudas.getSelectionModel().getSelectedItem();
        if (d == null || d.anulada() || !contexto.sesion().tienePermiso(Permiso.FINANZAS_GESTIONAR)) {
            return;
        }
        FormularioDeuda formulario = new FormularioDeuda(d);
        boolean ok = Dialogos.formulario(ventana(), "Editar deuda", null, formulario.contenido(), "Guardar cambios",
                520, () -> servicio().actualizarDeuda(d.id(), formulario.datos()));
        if (ok) {
            cargar();
            Notificador.exito("Deuda actualizada.");
        }
    }

    @FXML
    private void anularDeuda() {
        Deuda d = tablaDeudas.getSelectionModel().getSelectedItem();
        if (d == null || d.anulada()) {
            return;
        }
        TextField motivo = new TextField();
        motivo.setPromptText("Ej.: registrada dos veces");
        GridPane grilla = Formularios.grilla();
        Formularios.fila(grilla, "Motivo *", motivo);
        boolean ok = Dialogos.formulario(ventana(), "Anular deuda",
                "La deuda con " + d.acreedor() + " (" + Montos.formatear(d.monto(), moneda) + ") dejará de contar en el "
                        + "balance, pero se conservará como anulada. Para una deuda ya pagada no hace falta anularla.",
                grilla, "Anular", () -> servicio().anularDeuda(d.id(), motivo.getText()));
        if (ok) {
            cargar();
            Notificador.exito("Deuda anulada.");
        }
    }

    @FXML
    private void registrarPago() {
        Deuda d = tablaDeudas.getSelectionModel().getSelectedItem();
        if (d == null || !d.pendiente()) {
            return;
        }
        DatePicker fecha = CamposFecha.nuevo(LocalDate.now());
        TextField monto = new TextField();
        monto.setPromptText("0.00");
        TextField nota = new TextField();
        nota.setPromptText("Opcional. Ej.: cuota 3 de 12");
        GridPane grilla = Formularios.grilla();
        Formularios.fila(grilla, "Fecha *", fecha);
        Formularios.fila(grilla, "Capital pagado (" + moneda.simbolo() + ") *", monto);
        Formularios.nota(grilla, "Saldo pendiente: " + Montos.formatear(d.saldo(), moneda)
                + ". Registra solo el capital; el interés va como gasto en Gestión financiera.");
        Formularios.fila(grilla, "Nota", nota);
        Deuda[] actualizada = new Deuda[1];
        boolean ok = Dialogos.formulario(ventana(), "Registrar pago a " + d.acreedor(), null, grilla, "Registrar pago",
                () -> actualizada[0] = servicio().registrarPago(d.id(), CamposFecha.leer(fecha, "pago"),
                        Montos.leer(monto.getText(), "Capital pagado"), nota.getText()));
        if (ok) {
            cargar();
            seleccionar(tablaDeudas, x -> x.id() == d.id());
            Notificador.exito(actualizada[0].cancelada() ? "Pago registrado: la deuda quedó cancelada."
                    : "Pago registrado. Saldo: " + Montos.formatear(actualizada[0].saldo(), moneda) + ".");
        }
    }

    @FXML
    private void verPagos() {
        Deuda d = tablaDeudas.getSelectionModel().getSelectedItem();
        if (d == null) {
            return;
        }
        TableView<PagoDeuda> tabla = new TableView<>();
        tabla.setPrefHeight(280);
        tabla.setPlaceholder(new Label("Aún no hay pagos registrados."));
        tabla.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        tabla.getColumns().add(columna("Fecha", 95, p -> Fechas.paraMostrar(p.fecha()), false));
        tabla.getColumns().add(columna("Capital", 110, p -> Montos.formatear(p.monto(), moneda), true));
        tabla.getColumns().add(columna("Nota", 170, p -> Objects.requireNonNullElse(p.nota(), ""), false));
        tabla.getColumns().add(columna("Registró", 120, p -> p.anulado()
                ? "Anulado por " + p.anuladoPor() : p.registradoPor(), false));
        tabla.setRowFactory(t -> new TableRow<>() {
            @Override
            protected void updateItem(PagoDeuda p, boolean vacio) {
                super.updateItem(p, vacio);
                pseudoClassStateChanged(ARCHIVADO, !vacio && p != null && p.anulado());
            }
        });
        Label resumen = new Label();
        resumen.getStyleClass().add("info-value");
        Runnable recargar = () -> {
            Deuda actual = servicio().listarDeudas(true).stream().filter(x -> x.id() == d.id()).findFirst().orElse(d);
            tabla.getItems().setAll(servicio().listarPagos(d.id()));
            resumen.setText("Monto: " + Montos.formatear(actual.monto(), moneda) + "   ·   Pagado: "
                    + Montos.formatear(actual.pagado(), moneda) + "   ·   Saldo: " + Montos.formatear(actual.saldo(), moneda));
        };
        boolean[] cambios = {false};
        VBox contenido = new VBox(10, resumen, tabla);
        if (contexto.sesion().tienePermiso(Permiso.FINANZAS_GESTIONAR) && !d.anulada()) {
            Button anular = new Button("Anular pago seleccionado");
            anular.getStyleClass().add("button-secondary");
            anular.disableProperty().bind(tabla.getSelectionModel().selectedItemProperty().isNull());
            anular.setOnAction(e -> {
                PagoDeuda p = tabla.getSelectionModel().getSelectedItem();
                if (p == null || p.anulado()) {
                    return;
                }
                if (Dialogos.confirmar(tabla.getScene().getWindow(), "Anular pago",
                        "El pago de " + Montos.formatear(p.monto(), moneda) + " del " + Fechas.paraMostrar(p.fecha())
                                + " se anulará y el saldo de la deuda volverá a subir.")) {
                    try {
                        servicio().anularPago(p.id());
                        cambios[0] = true;
                        recargar.run();
                    } catch (RuntimeException ex) {
                        Notificador.error(Errores.mensaje(ex));
                    }
                }
            });
            contenido.getChildren().add(anular);
        }
        try {
            recargar.run();
        } catch (RuntimeException e) {
            Notificador.error(Errores.mensaje(e));
            return;
        }
        Dialogos.informacion(ventana(), "Pagos a " + d.acreedor(), d.categoria().descripcion()
                + (d.descripcion() == null ? "" : " · " + d.descripcion()), contenido);
        if (cambios[0]) {
            cargar();
        }
    }

    private static TableColumn<PagoDeuda, String> columna(String titulo, double ancho,
                                                          Function<PagoDeuda, String> valor, boolean numero) {
        TableColumn<PagoDeuda, String> columna = new TableColumn<>(titulo);
        columna.setPrefWidth(ancho);
        columna.setSortable(false);
        columna.setCellValueFactory(c -> new ReadOnlyStringWrapper(valor.apply(c.getValue())));
        if (numero) {
            columna.getStyleClass().add("col-numero");
        }
        return columna;
    }

    // ------------------------------------------------------------------ tablas

    private void configurarTablaBalance() {
        colBalConcepto.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue()));
        colBalConcepto.setCellFactory(col -> celdaBalance(f -> f.concepto(), f -> null));
        colBalMonto.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue()));
        colBalMonto.setCellFactory(col -> celdaBalance(f -> f.monto() == null ? "" : Montos.formatear(f.monto(), moneda),
                f -> f.monto() != null && f.monto().signum() < 0 ? "margen-negativo" : null));
        colBalPorcentaje.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue()));
        colBalPorcentaje.setCellFactory(col -> celdaBalance(f -> f.monto() == null || totalActivo.signum() == 0 ? ""
                : Montos.porcentaje(f.monto().divide(totalActivo, 6, RoundingMode.HALF_UP)), f -> null));
        tablaBalance.setRowFactory(t -> new TableRow<>() {
            @Override
            protected void updateItem(FilaBalance fila, boolean vacio) {
                super.updateItem(fila, vacio);
                boolean subtotal = !vacio && fila != null
                        && (fila.tipo() == TipoFila.SUBTOTAL || fila.tipo() == TipoFila.TITULO);
                pseudoClassStateChanged(SUBTOTAL, subtotal);
                pseudoClassStateChanged(RESULTADO, !vacio && fila != null && fila.tipo() == TipoFila.RESULTADO);
            }
        });
    }

    private static TableCell<FilaBalance, FilaBalance> celdaBalance(Function<FilaBalance, String> texto,
                                                                    Function<FilaBalance, String> clase) {
        return new TableCell<>() {
            @Override
            protected void updateItem(FilaBalance fila, boolean vacio) {
                super.updateItem(fila, vacio);
                getStyleClass().removeAll("margen-negativo");
                if (vacio || fila == null) {
                    setText(null);
                    return;
                }
                setText(texto.apply(fila));
                String estilo = clase.apply(fila);
                if (estilo != null) {
                    getStyleClass().add(estilo);
                }
            }
        };
    }

    private void configurarTablaActivos() {
        LocalDate hoy = LocalDate.now();
        colActTipo.setCellValueFactory(c -> texto(c.getValue().categoria().descripcion()));
        colActDescripcion.setCellValueFactory(c -> texto(c.getValue().descripcion()
                + (c.getValue().dadoDeBaja() ? "  (baja)" : "")));
        colActFecha.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().fecha()));
        colActFecha.setCellFactory(col -> celdaFecha());
        colActValor.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().valor()));
        colActValor.setCellFactory(col -> celdaMonto());
        colActVida.setCellValueFactory(c -> texto(c.getValue().seDeprecia()
                ? c.getValue().vidaUtilAnios() + (c.getValue().vidaUtilAnios() == 1 ? " año" : " años") : "—"));
        colActDepreciacion.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(
                Depreciacion.de(c.getValue(), hoy).acumulada()));
        colActDepreciacion.setCellFactory(col -> celdaMonto());
        colActLibros.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(
                Depreciacion.de(c.getValue(), hoy).valorEnLibros()));
        colActLibros.setCellFactory(col -> celdaMonto());

        SortedList<Activo> ordenados = new SortedList<>(activosFiltrados);
        ordenados.comparatorProperty().bind(tablaActivos.comparatorProperty());
        tablaActivos.setItems(ordenados);
        tablaActivos.getSelectionModel().selectedItemProperty().addListener((o, a, n) -> actualizarAcciones());
        tablaActivos.setRowFactory(t -> {
            TableRow<Activo> fila = new TableRow<>() {
                @Override
                protected void updateItem(Activo a, boolean vacio) {
                    super.updateItem(a, vacio);
                    boolean baja = !vacio && a != null && a.dadoDeBaja();
                    pseudoClassStateChanged(ARCHIVADO, baja);
                    setTooltip(baja ? new Tooltip("Dado de baja el " + Fechas.paraMostrar(a.fechaBaja()) + ": "
                            + a.motivoBaja()) : null);
                }
            };
            fila.setOnMouseClicked(e -> {
                if (e.getButton() == MouseButton.PRIMARY && e.getClickCount() == 2 && !fila.isEmpty()) {
                    editarActivo();
                }
            });
            return fila;
        });
    }

    private void configurarTablaDeudas() {
        colDeuAcreedor.setCellValueFactory(c -> texto(c.getValue().acreedor()));
        colDeuTipo.setCellValueFactory(c -> texto(c.getValue().categoria().descripcion()));
        colDeuFecha.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().fecha()));
        colDeuFecha.setCellFactory(col -> celdaFecha());
        colDeuMonto.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().monto()));
        colDeuMonto.setCellFactory(col -> celdaMonto());
        colDeuSaldo.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().saldo()));
        colDeuSaldo.setCellFactory(col -> celdaMonto());
        colDeuCuota.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().cuotaMensual()));
        colDeuCuota.setCellFactory(col -> celdaMonto());
        colDeuTasa.setCellValueFactory(c -> texto(c.getValue().tasaAnual() == null ? "—"
                : Montos.porcentaje(c.getValue().tasaAnual().movePointLeft(2))));
        colDeuVence.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().vencimiento()));
        colDeuVence.setCellFactory(col -> celdaFecha());
        colDeuEstado.setCellValueFactory(c -> texto(c.getValue().estadoTexto()));

        SortedList<Deuda> ordenadas = new SortedList<>(deudasFiltradas);
        ordenadas.comparatorProperty().bind(tablaDeudas.comparatorProperty());
        tablaDeudas.setItems(ordenadas);
        tablaDeudas.getSelectionModel().selectedItemProperty().addListener((o, a, n) -> actualizarAcciones());
        tablaDeudas.setRowFactory(t -> {
            TableRow<Deuda> fila = new TableRow<>() {
                @Override
                protected void updateItem(Deuda d, boolean vacio) {
                    super.updateItem(d, vacio);
                    boolean apagada = !vacio && d != null && !d.pendiente();
                    pseudoClassStateChanged(ARCHIVADO, apagada);
                    setTooltip(!vacio && d != null && d.anulada()
                            ? new Tooltip("Anulada: " + d.motivoAnulacion()) : null);
                }
            };
            fila.setOnMouseClicked(e -> {
                if (e.getButton() == MouseButton.PRIMARY && e.getClickCount() == 2 && !fila.isEmpty()) {
                    verPagos();
                }
            });
            return fila;
        });
    }

    private void actualizarAcciones() {
        Activo a = tablaActivos.getSelectionModel().getSelectedItem();
        boolean sinActivo = a == null || a.dadoDeBaja();
        btnEditarActivo.setDisable(sinActivo);
        btnBajaActivo.setDisable(sinActivo);

        Deuda d = tablaDeudas.getSelectionModel().getSelectedItem();
        btnVerPagos.setDisable(d == null);
        btnEditarDeuda.setDisable(d == null || d.anulada());
        btnAnularDeuda.setDisable(d == null || d.anulada());
        btnPagar.setDisable(d == null || !d.pendiente());
    }

    private <T> TableCell<T, BigDecimal> celdaMonto() {
        return new TableCell<>() {
            @Override
            protected void updateItem(BigDecimal monto, boolean vacio) {
                super.updateItem(monto, vacio);
                setText(vacio || monto == null ? (vacio ? null : "—") : Montos.formatear(monto, moneda));
            }
        };
    }

    private static <T> TableCell<T, LocalDate> celdaFecha() {
        return new TableCell<>() {
            @Override
            protected void updateItem(LocalDate fecha, boolean vacio) {
                super.updateItem(fecha, vacio);
                setText(vacio ? null : Fechas.paraMostrar(fecha));
            }
        };
    }

    // ------------------------------------------------------------------ utilidades

    private static <T> void seleccionar(TableView<T> tabla, java.util.function.Predicate<T> criterio) {
        tabla.getItems().stream().filter(criterio).findFirst().ifPresent(x -> {
            tabla.getSelectionModel().select(x);
            tabla.scrollTo(x);
        });
    }

    private PatrimonioService servicio() {
        return contexto.patrimonio();
    }

    private Window ventana() {
        return tablaActivos.getScene().getWindow();
    }

    private static ReadOnlyStringWrapper texto(String valor) {
        return new ReadOnlyStringWrapper(valor);
    }

    private static BigDecimal montoOpcional(String texto, String campo) {
        return texto == null || texto.isBlank() ? null : Montos.leer(texto, campo);
    }

    /** Formulario de activo. La vida útil y el valor residual solo se piden en bienes que se deprecian. */
    private final class FormularioActivo {
        private final ComboBox<CategoriaActivo> cmbCategoria =
                new ComboBox<>(FXCollections.observableArrayList(CategoriaActivo.values()));
        private final Label lblAyuda = new Label();
        private final TextField txtDescripcion = new TextField();
        private final DatePicker dpFecha = CamposFecha.nuevo(LocalDate.now());
        private final TextField txtValor = new TextField();
        private final TextField txtVida = new TextField();
        private final TextField txtResidual = new TextField();
        private final Label lblFecha = new Label();
        private final Label lblValor = new Label();

        FormularioActivo(Activo a) {
            cmbCategoria.setPromptText("Selecciona el tipo de activo");
            cmbCategoria.setVisibleRowCount(10);
            txtDescripcion.setPromptText("Ej.: Refrigeradora exhibidora, Cuenta BCP");
            txtValor.setPromptText("0.00");
            txtVida.setPromptText("Años");
            txtResidual.setPromptText("0.00 (opcional)");
            lblAyuda.getStyleClass().add("hint");
            lblAyuda.setWrapText(true);
            lblAyuda.setMinHeight(Region.USE_PREF_SIZE);

            cmbCategoria.valueProperty().addListener((o, anterior, c) -> {
                actualizarCampos(c);
                if (c != null && a == null) {
                    txtVida.setText(c.vidaUtilSugerida() == null ? "" : String.valueOf(c.vidaUtilSugerida()));
                }
            });
            if (a != null) {
                cmbCategoria.setValue(a.categoria());
                txtDescripcion.setText(a.descripcion());
                dpFecha.setValue(a.fecha());
                txtValor.setText(Montos.paraEditar(a.valor()));
                txtVida.setText(a.vidaUtilAnios() == null ? "" : String.valueOf(a.vidaUtilAnios()));
                txtResidual.setText(a.valorResidual().signum() == 0 ? "" : Montos.paraEditar(a.valorResidual()));
            } else {
                actualizarCampos(null);
            }
        }

        private void actualizarCampos(CategoriaActivo c) {
            boolean corriente = c != null && c.corriente();
            boolean deprecia = c != null && c.depreciable();
            lblAyuda.setText(c == null ? "" : c.ayuda());
            lblFecha.setText(corriente ? "Fecha del saldo *" : "Fecha de adquisición *");
            lblValor.setText((corriente ? "Saldo (" : "Valor de adquisición (") + moneda.simbolo() + ") *");
            txtVida.setDisable(!deprecia);
            txtResidual.setDisable(!deprecia);
            if (!deprecia) {
                txtVida.clear();
                txtResidual.clear();
            }
        }

        GridPane contenido() {
            GridPane grilla = Formularios.grilla();
            Formularios.fila(grilla, "Tipo *", cmbCategoria);
            Formularios.fila(grilla, "", lblAyuda);
            Formularios.fila(grilla, "Descripción *", txtDescripcion);
            lblFecha.getStyleClass().add("form-label");
            lblValor.getStyleClass().add("form-label");
            int fila = grilla.getRowCount();
            grilla.add(lblFecha, 0, fila);
            grilla.add(dpFecha, 1, fila);
            dpFecha.setMaxWidth(Double.MAX_VALUE);
            grilla.add(lblValor, 0, fila + 1);
            grilla.add(txtValor, 1, fila + 1);
            Formularios.fila(grilla, "Vida útil (años)", txtVida);
            Formularios.fila(grilla, "Valor residual (" + moneda.simbolo() + ")", txtResidual);
            Formularios.nota(grilla, "Vida útil vacía = no se deprecia. Valor residual: lo que valdría al final de su vida útil.");
            return grilla;
        }

        DatosActivo datos() {
            LocalDate fecha = CamposFecha.leer(dpFecha, "el activo");
            Integer vida = null;
            String textoVida = txtVida.getText() == null ? "" : txtVida.getText().strip();
            if (!textoVida.isEmpty()) {
                try {
                    vida = Integer.parseInt(textoVida);
                } catch (NumberFormatException e) {
                    throw new com.businessanalyzer.exception.ValidacionException(
                            "La vida útil debe ser un número entero de años.");
                }
            }
            return new DatosActivo(cmbCategoria.getValue(), txtDescripcion.getText(), fecha,
                    Montos.leer(txtValor.getText(), "Valor"), vida, montoOpcional(txtResidual.getText(), "Valor residual"));
        }
    }

    /** Formulario de deuda. Tasa, cuota y vencimiento son opcionales, pero mejoran el análisis. */
    private final class FormularioDeuda {
        private final ComboBox<CategoriaDeuda> cmbCategoria =
                new ComboBox<>(FXCollections.observableArrayList(CategoriaDeuda.values()));
        private final TextField txtAcreedor = new TextField();
        private final TextField txtDescripcion = new TextField();
        private final DatePicker dpFecha = CamposFecha.nuevo(LocalDate.now());
        private final TextField txtMonto = new TextField();
        private final TextField txtTasa = new TextField();
        private final TextField txtCuota = new TextField();
        private final DatePicker dpVencimiento = CamposFecha.nuevo(null);

        FormularioDeuda(Deuda d) {
            cmbCategoria.setPromptText("Selecciona el tipo de deuda");
            txtAcreedor.setPromptText("Ej.: Caja Arequipa, Distribuidora Lima");
            txtDescripcion.setPromptText("Opcional. Ej.: préstamo para la refrigeradora");
            txtMonto.setPromptText("0.00");
            txtTasa.setPromptText("Ej.: 24.5 (opcional)");
            txtCuota.setPromptText("Opcional");
            if (d != null) {
                cmbCategoria.setValue(d.categoria());
                txtAcreedor.setText(d.acreedor());
                txtDescripcion.setText(Objects.requireNonNullElse(d.descripcion(), ""));
                dpFecha.setValue(d.fecha());
                txtMonto.setText(Montos.paraEditar(d.monto()));
                txtTasa.setText(d.tasaAnual() == null ? "" : d.tasaAnual().stripTrailingZeros().toPlainString());
                txtCuota.setText(d.cuotaMensual() == null ? "" : Montos.paraEditar(d.cuotaMensual()));
                dpVencimiento.setValue(d.vencimiento());
            }
        }

        GridPane contenido() {
            GridPane grilla = Formularios.grilla();
            Formularios.fila(grilla, "Tipo *", cmbCategoria);
            Formularios.fila(grilla, "Acreedor *", txtAcreedor);
            Formularios.fila(grilla, "Descripción", txtDescripcion);
            Formularios.fila(grilla, "Fecha de inicio *", dpFecha);
            Formularios.fila(grilla, "Monto (" + moneda.simbolo() + ") *", txtMonto);
            Formularios.fila(grilla, "Tasa anual (%)", txtTasa);
            Formularios.fila(grilla, "Cuota mensual (" + moneda.simbolo() + ")", txtCuota);
            Formularios.fila(grilla, "Vencimiento", dpVencimiento);
            Formularios.nota(grilla, "Con la cuota se calcula la capacidad de pago; con el vencimiento, qué parte "
                    + "de la deuda es de corto plazo (sin vencimiento se considera toda de corto plazo).");
            return grilla;
        }

        DatosDeuda datos() {
            LocalDate fecha = CamposFecha.leer(dpFecha, "inicio");
            LocalDate vencimiento = CamposFecha.leer(dpVencimiento, "vencimiento");
            return new DatosDeuda(cmbCategoria.getValue(), txtAcreedor.getText(), txtDescripcion.getText(), fecha,
                    Montos.leer(txtMonto.getText(), "Monto"), montoOpcional(txtTasa.getText(), "Tasa anual"),
                    montoOpcional(txtCuota.getText(), "Cuota mensual"), vencimiento);
        }
    }
}
