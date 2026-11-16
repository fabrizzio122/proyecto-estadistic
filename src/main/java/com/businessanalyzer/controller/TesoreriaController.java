package com.businessanalyzer.controller;

import com.businessanalyzer.analytics.Periodo;
import com.businessanalyzer.analytics.tesoreria.Actividad;
import com.businessanalyzer.analytics.tesoreria.FlujoEfectivo;
import com.businessanalyzer.analytics.tesoreria.FlujoMensual;
import com.businessanalyzer.analytics.tesoreria.ProyeccionCaja;
import com.businessanalyzer.analytics.tesoreria.RubroFlujo;
import com.businessanalyzer.analytics.tesoreria.SaldoCuenta;
import com.businessanalyzer.config.AppContext;
import com.businessanalyzer.model.CuentaDinero;
import com.businessanalyzer.model.DatosCuenta;
import com.businessanalyzer.model.DatosMovimientoTesoreria;
import com.businessanalyzer.model.Empresa;
import com.businessanalyzer.model.Moneda;
import com.businessanalyzer.model.MovimientoTesoreria;
import com.businessanalyzer.model.Operacion;
import com.businessanalyzer.model.PagoOperacion;
import com.businessanalyzer.model.Permiso;
import com.businessanalyzer.model.TipoCuenta;
import com.businessanalyzer.model.TipoMovimientoTesoreria;
import com.businessanalyzer.model.TipoOperacion;
import com.businessanalyzer.service.TesoreriaService;
import com.businessanalyzer.ui.CamposFecha;
import com.businessanalyzer.ui.Dialogos;
import com.businessanalyzer.ui.Errores;
import com.businessanalyzer.ui.Formularios;
import com.businessanalyzer.ui.Graficos;
import com.businessanalyzer.ui.Notificador;
import com.businessanalyzer.ui.SelectorCuenta;
import com.businessanalyzer.ui.TarjetaIndicador;
import com.businessanalyzer.ui.VistaModulo;
import com.businessanalyzer.util.Fechas;
import com.businessanalyzer.util.Montos;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
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
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Tooltip;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Window;
import org.kordamp.ikonli.javafx.FontIcon;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

/**
 * Módulo "Tesorería": cuentas de dinero con su saldo, transferencias, aportes y retiros, cuentas por
 * cobrar y por pagar, estado de flujo de efectivo y proyección de caja.
 */
public class TesoreriaController implements VistaModulo {

    private enum TipoFila { TITULO, DETALLE, SUBTOTAL, RESULTADO }

    private record FilaFlujo(String concepto, BigDecimal monto, TipoFila tipo) {
    }

    private static final PseudoClass SUBTOTAL = PseudoClass.getPseudoClass("subtotal");
    private static final PseudoClass RESULTADO = PseudoClass.getPseudoClass("resultado");
    private static final PseudoClass ARCHIVADO = PseudoClass.getPseudoClass("archivado");
    private static final int MESES_EVOLUCION = 12;

    private final AppContext contexto;
    private final ToggleGroup grupoPendientes = new ToggleGroup();
    private Moneda moneda = Moneda.PEN;

    private final TarjetaIndicador kpiDisponible = new TarjetaIndicador("Dinero disponible", "fth-dollar-sign");
    private final TarjetaIndicador kpiCobrar = new TarjetaIndicador("Por cobrar", "fth-arrow-down-circle");
    private final TarjetaIndicador kpiPagar = new TarjetaIndicador("Por pagar", "fth-arrow-up-circle");
    private final TarjetaIndicador kpiProyeccion = new TarjetaIndicador("Dinero en 6 meses", "fth-trending-up");

    @FXML private HBox indicadores;

    @FXML private CheckBox chkInactivas;
    @FXML private Button btnEditarCuenta;
    @FXML private Button btnActivarCuenta;
    @FXML private Button btnNuevaCuenta;
    @FXML private TableView<SaldoCuenta> tablaCuentas;
    @FXML private TableColumn<SaldoCuenta, String> colCtaNombre;
    @FXML private TableColumn<SaldoCuenta, String> colCtaTipo;
    @FXML private TableColumn<SaldoCuenta, String> colCtaNumero;
    @FXML private TableColumn<SaldoCuenta, BigDecimal> colCtaInicial;
    @FXML private TableColumn<SaldoCuenta, BigDecimal> colCtaEntradas;
    @FXML private TableColumn<SaldoCuenta, BigDecimal> colCtaSalidas;
    @FXML private TableColumn<SaldoCuenta, BigDecimal> colCtaSaldo;
    @FXML private TableColumn<SaldoCuenta, LocalDate> colCtaUltimo;

    @FXML private DatePicker dpMovDesde;
    @FXML private DatePicker dpMovHasta;
    @FXML private CheckBox chkMovAnulados;
    @FXML private Button btnAnularMovimiento;
    @FXML private Button btnNuevoMovimiento;
    @FXML private TableView<MovimientoTesoreria> tablaMovimientos;
    @FXML private TableColumn<MovimientoTesoreria, LocalDate> colMovFecha;
    @FXML private TableColumn<MovimientoTesoreria, String> colMovTipo;
    @FXML private TableColumn<MovimientoTesoreria, String> colMovOrigen;
    @FXML private TableColumn<MovimientoTesoreria, String> colMovDestino;
    @FXML private TableColumn<MovimientoTesoreria, BigDecimal> colMovMonto;
    @FXML private TableColumn<MovimientoTesoreria, String> colMovDescripcion;

    @FXML private ToggleButton tglCobrar;
    @FXML private ToggleButton tglPagar;
    @FXML private Button btnVerPagos;
    @FXML private Button btnRegistrarPago;
    @FXML private TableView<Operacion> tablaPendientes;
    @FXML private TableColumn<Operacion, String> colPenNumero;
    @FXML private TableColumn<Operacion, LocalDate> colPenFecha;
    @FXML private TableColumn<Operacion, String> colPenContraparte;
    @FXML private TableColumn<Operacion, BigDecimal> colPenTotal;
    @FXML private TableColumn<Operacion, BigDecimal> colPenPagado;
    @FXML private TableColumn<Operacion, BigDecimal> colPenPendiente;
    @FXML private TableColumn<Operacion, LocalDate> colPenVence;
    @FXML private TableColumn<Operacion, Operacion> colPenEstado;
    @FXML private Label lblPendientes;

    @FXML private DatePicker dpFlujoDesde;
    @FXML private DatePicker dpFlujoHasta;
    @FXML private TableView<FilaFlujo> tablaFlujo;
    @FXML private TableColumn<FilaFlujo, FilaFlujo> colFluConcepto;
    @FXML private TableColumn<FilaFlujo, FilaFlujo> colFluMonto;
    @FXML private Label lblFlujo;
    @FXML private StackPane graficoEvolucion;

    @FXML private HBox avisoProyeccion;
    @FXML private FontIcon iconoProyeccion;
    @FXML private Label lblProyeccion;
    @FXML private StackPane graficoProyeccion;
    @FXML private TableView<ProyeccionCaja.MesProyectado> tablaProyeccion;
    @FXML private TableColumn<ProyeccionCaja.MesProyectado, String> colProMes;
    @FXML private TableColumn<ProyeccionCaja.MesProyectado, BigDecimal> colProOperacion;
    @FXML private TableColumn<ProyeccionCaja.MesProyectado, BigDecimal> colProDeudas;
    @FXML private TableColumn<ProyeccionCaja.MesProyectado, BigDecimal> colProSaldo;
    @FXML private TableColumn<ProyeccionCaja.MesProyectado, BigDecimal> colProPesimista;
    @FXML private Label lblMetodo;

    public TesoreriaController(AppContext contexto) {
        this.contexto = contexto;
    }

    @FXML
    private void initialize() {
        indicadores.getChildren().addAll(kpiDisponible, kpiCobrar, kpiPagar, kpiProyeccion);
        tglCobrar.setToggleGroup(grupoPendientes);
        tglPagar.setToggleGroup(grupoPendientes);
        grupoPendientes.selectedToggleProperty().addListener((o, anterior, nuevo) -> {
            if (nuevo == null) {
                anterior.setSelected(true);
            } else {
                cargarPendientes();
            }
        });

        LocalDate hoy = LocalDate.now();
        for (DatePicker selector : List.of(dpMovDesde, dpMovHasta, dpFlujoDesde, dpFlujoHasta)) {
            CamposFecha.configurar(selector);
        }
        dpMovDesde.setValue(hoy.withDayOfMonth(1).minusMonths(2));
        dpMovHasta.setValue(hoy);
        dpFlujoDesde.setValue(hoy.withDayOfMonth(1).minusMonths(3));
        dpFlujoHasta.setValue(hoy.withDayOfMonth(1).minusDays(1));
        dpMovDesde.valueProperty().addListener((o, a, n) -> cargarMovimientos());
        dpMovHasta.valueProperty().addListener((o, a, n) -> cargarMovimientos());
        dpFlujoDesde.valueProperty().addListener((o, a, n) -> cargarFlujo());
        dpFlujoHasta.valueProperty().addListener((o, a, n) -> cargarFlujo());

        configurarTablaCuentas();
        configurarTablaMovimientos();
        configurarTablaPendientes();
        configurarTablaFlujo();
        configurarTablaProyeccion();

        boolean gestiona = contexto.sesion().tienePermiso(Permiso.FINANZAS_GESTIONAR);
        for (Node nodo : List.of(btnEditarCuenta, btnActivarCuenta, btnNuevaCuenta, btnAnularMovimiento,
                btnNuevoMovimiento)) {
            nodo.setVisible(gestiona);
            nodo.setManaged(gestiona);
        }
        boolean cobra = contexto.sesion().tienePermiso(Permiso.OPERACIONES_GESTIONAR);
        btnRegistrarPago.setVisible(cobra);
        btnRegistrarPago.setManaged(cobra);
    }

    @Override
    public void alMostrar() {
        cargar();
    }

    // ------------------------------------------------------------------ carga

    @FXML
    private void cargar() {
        Empresa empresa = contexto.sesion().empresaActiva().orElse(null);
        btnNuevaCuenta.setDisable(empresa == null);
        btnNuevoMovimiento.setDisable(empresa == null);
        if (empresa == null) {
            String mensaje = "Selecciona una empresa en la barra superior para ver su tesorería.";
            for (TableView<?> tabla : List.of(tablaCuentas, tablaMovimientos, tablaPendientes, tablaFlujo, tablaProyeccion)) {
                tabla.getItems().clear();
                tabla.setPlaceholder(new Label(mensaje));
            }
            for (TarjetaIndicador kpi : List.of(kpiDisponible, kpiCobrar, kpiPagar, kpiProyeccion)) {
                kpi.actualizar(TarjetaIndicador.SIN_VALOR, "");
            }
            graficoEvolucion.getChildren().clear();
            graficoProyeccion.getChildren().clear();
            lblProyeccion.setText(mensaje);
            return;
        }
        moneda = empresa.moneda();
        try {
            tablaCuentas.getItems().setAll(servicio().listarCuentas(chkInactivas.isSelected()));
            mostrarResumen(servicio().resumen());
        } catch (RuntimeException e) {
            tablaCuentas.getItems().clear();
            Notificador.error(Errores.mensaje(e));
        }
        cargarMovimientos();
        cargarPendientes();
        cargarFlujo();
        cargarProyeccion();
        actualizarAcciones();
    }

    private void mostrarResumen(TesoreriaService.Resumen r) {
        int cuentas = (int) tablaCuentas.getItems().stream().filter(s -> s.cuenta().activa()).count();
        kpiDisponible.actualizar(Montos.formatear(r.disponible(), moneda), r.cuentasNegativas() > 0
                ? r.cuentasNegativas() + (r.cuentasNegativas() == 1 ? " cuenta en negativo" : " cuentas en negativo")
                : "En " + cuentas + (cuentas == 1 ? " cuenta" : " cuentas"));
        kpiCobrar.actualizar(Montos.formatear(r.porCobrar(), moneda), r.porCobrarVencido().signum() > 0
                ? "Vencido: " + Montos.formatear(r.porCobrarVencido(), moneda) : "Nada vencido");
        kpiPagar.actualizar(Montos.formatear(r.porPagar(), moneda), r.porPagarVencido().signum() > 0
                ? "Vencido: " + Montos.formatear(r.porPagarVencido(), moneda) : "Nada vencido");
    }

    @FXML
    private void cargarMovimientos() {
        if (contexto.sesion().empresaActiva().isEmpty() || dpMovDesde.getValue() == null || dpMovHasta.getValue() == null) {
            return;
        }
        tablaMovimientos.setPlaceholder(new Label("No hay transferencias, aportes ni retiros en el período."));
        try {
            tablaMovimientos.getItems().setAll(servicio().listarMovimientos(dpMovDesde.getValue(), dpMovHasta.getValue(),
                    chkMovAnulados.isSelected()));
        } catch (RuntimeException e) {
            tablaMovimientos.getItems().clear();
            Notificador.error(Errores.mensaje(e));
        }
        actualizarAcciones();
    }

    private TipoOperacion tipoPendiente() {
        return tglPagar.isSelected() ? TipoOperacion.COMPRA : TipoOperacion.VENTA;
    }

    private void cargarPendientes() {
        TipoOperacion tipo = tipoPendiente();
        boolean venta = tipo == TipoOperacion.VENTA;
        colPenContraparte.setText(tipo.contraparte());
        colPenPagado.setText(venta ? "Cobrado" : "Pagado");
        btnRegistrarPago.setText(venta ? "Registrar cobro" : "Registrar pago");
        btnVerPagos.setText(venta ? "Ver cobros" : "Ver pagos");
        if (contexto.sesion().empresaActiva().isEmpty()) {
            return;
        }
        tablaPendientes.setPlaceholder(new Label(venta ? "No hay ventas al crédito pendientes de cobro."
                : "No hay compras al crédito pendientes de pago."));
        try {
            List<Operacion> pendientes = servicio().pendientes(tipo);
            tablaPendientes.getItems().setAll(pendientes);
            LocalDate hoy = LocalDate.now();
            BigDecimal total = pendientes.stream().map(Operacion::pendiente).reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal vencido = pendientes.stream().filter(o -> o.vencidaEn(hoy)).map(Operacion::pendiente)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            lblPendientes.setText(pendientes.size() + (pendientes.size() == 1 ? " operación" : " operaciones")
                    + " · pendiente " + Montos.formatear(total, moneda) + " · vencido " + Montos.formatear(vencido, moneda));
        } catch (RuntimeException e) {
            tablaPendientes.getItems().clear();
            lblPendientes.setText("");
            Notificador.error(Errores.mensaje(e));
        }
        actualizarAcciones();
    }

    private void cargarFlujo() {
        if (contexto.sesion().empresaActiva().isEmpty() || dpFlujoDesde.getValue() == null || dpFlujoHasta.getValue() == null) {
            return;
        }
        try {
            FlujoEfectivo flujo = servicio().flujoEfectivo(dpFlujoDesde.getValue(), dpFlujoHasta.getValue());
            tablaFlujo.getItems().setAll(filasFlujo(flujo));
            tablaFlujo.setPlaceholder(new Label("Sin datos."));
            lblFlujo.setText(flujo.sinMovimientos()
                    ? "No hubo movimientos de dinero registrados en las cuentas durante el período."
                    : "Método directo: lo que realmente entró y salió de las cuentas (con impuestos), aunque la venta o "
                    + "el gasto sean de otro mes. Las transferencias entre cuentas no cambian el total y no se muestran.");
            List<FlujoMensual> evolucion = servicio().evolucion(MESES_EVOLUCION);
            List<YearMonth> meses = evolucion.stream().map(FlujoMensual::mes).toList();
            graficoEvolucion.getChildren().setAll(Graficos.visor(Graficos.flujoCaja(meses,
                    serie(evolucion, FlujoMensual::entradas), serie(evolucion, FlujoMensual::salidas),
                    serie(evolucion, FlujoMensual::saldoCierre), moneda)));
        } catch (RuntimeException e) {
            tablaFlujo.getItems().clear();
            graficoEvolucion.getChildren().clear();
            lblFlujo.setText(Errores.mensaje(e));
        }
    }

    private void cargarProyeccion() {
        avisoProyeccion.getStyleClass().remove("banner-aviso");
        try {
            ProyeccionCaja p = servicio().proyeccion();
            tablaProyeccion.getItems().setAll(p.meses());
            if (!p.suficiente()) {
                kpiProyeccion.actualizar(TarjetaIndicador.SIN_VALOR, "Faltan meses de historia");
                iconoProyeccion.setIconLiteral("fth-info");
                lblProyeccion.setText("Para proyectar el dinero disponible se necesitan al menos "
                        + ProyeccionCaja.MINIMO_MESES + " meses completos con movimientos en las cuentas (hay "
                        + p.mesesHistoria() + "). Registra tus ventas, compras y gastos indicando su cuenta.");
                tablaProyeccion.setPlaceholder(new Label("Sin proyección todavía."));
                graficoProyeccion.getChildren().clear();
                lblMetodo.setText("");
                return;
            }
            BigDecimal alFinal = p.meses().get(p.meses().size() - 1).saldo();
            kpiProyeccion.actualizar(Montos.formatear(alFinal, moneda), p.primerNegativo() != null
                    ? "Falta dinero en " + mesTexto(p.primerNegativo())
                    : p.primerNegativoPesimista() != null ? "Riesgo en " + mesTexto(p.primerNegativoPesimista())
                    : "Alcanza incluso en el peor caso");
            if (p.primerNegativo() != null) {
                avisoProyeccion.getStyleClass().add("banner-aviso");
                iconoProyeccion.setIconLiteral("fth-alert-triangle");
                lblProyeccion.setText("Con el ritmo actual, el dinero no alcanza: en " + mesTexto(p.primerNegativo())
                        + " las cuentas quedarían en negativo. Acelera los cobros, posterga compras no urgentes "
                        + "o negocia las cuotas antes de llegar a ese mes.");
            } else if (p.primerNegativoPesimista() != null) {
                avisoProyeccion.getStyleClass().add("banner-aviso");
                iconoProyeccion.setIconLiteral("fth-alert-circle");
                lblProyeccion.setText("El dinero alcanza con el pronóstico base, pero en un escenario pesimista "
                        + "faltaría en " + mesTexto(p.primerNegativoPesimista()) + ". Conviene armar una reserva.");
            } else {
                iconoProyeccion.setIconLiteral("fth-check-circle");
                lblProyeccion.setText("El dinero alcanza los próximos " + ProyeccionCaja.HORIZONTE + " meses incluso "
                        + "en el escenario pesimista. Al final habría unos " + Montos.formatear(alFinal, moneda) + ".");
            }
            List<YearMonth> meses = p.meses().stream().map(ProyeccionCaja.MesProyectado::mes).toList();
            double[] base = p.meses().stream().mapToDouble(m -> m.saldo().doubleValue()).toArray();
            double[] pesimista = p.meses().stream().mapToDouble(m -> m.saldoPesimista().doubleValue()).toArray();
            graficoProyeccion.getChildren().setAll(Graficos.visor(Graficos.proyeccionCaja(meses, base, pesimista, moneda)));
            lblMetodo.setText("Hoy hay " + Montos.formatear(p.saldoActual(), moneda) + ". Flujo de operación "
                    + "pronosticado con «" + p.pronostico().elegida().metodo().nombre() + "» (el de menor error en "
                    + p.mesesHistoria() + " meses de historia; confiabilidad " + p.pronostico().confiabilidad()
                    .descripcion().toLowerCase() + "). Del mes en curso solo cuenta lo que falta del mes; a las deudas "
                    + "se les descuenta el capital de cada cuota (los intereses ya están en el flujo de operación).");
        } catch (RuntimeException e) {
            tablaProyeccion.getItems().clear();
            graficoProyeccion.getChildren().clear();
            lblProyeccion.setText(Errores.mensaje(e));
        }
    }

    private static double[] serie(List<FlujoMensual> meses, Function<FlujoMensual, BigDecimal> valor) {
        return meses.stream().mapToDouble(m -> valor.apply(m).doubleValue()).toArray();
    }

    private static List<FilaFlujo> filasFlujo(FlujoEfectivo f) {
        List<FilaFlujo> filas = new ArrayList<>();
        filas.add(new FilaFlujo("Dinero al inicio (" + Fechas.paraMostrar(f.periodo().desde()) + ")", f.saldoInicial(),
                TipoFila.RESULTADO));
        for (Actividad actividad : List.of(Actividad.OPERACION, Actividad.INVERSION, Actividad.FINANCIAMIENTO)) {
            filas.add(new FilaFlujo(actividad.descripcion().toUpperCase(), null, TipoFila.TITULO));
            boolean alguno = false;
            for (RubroFlujo rubro : RubroFlujo.values()) {
                if (rubro.actividad() == actividad && f.porRubro().containsKey(rubro)) {
                    filas.add(new FilaFlujo("    " + rubro.descripcion(), f.rubro(rubro), TipoFila.DETALLE));
                    alguno = true;
                }
            }
            if (!alguno) {
                filas.add(new FilaFlujo("    Sin movimientos", BigDecimal.ZERO, TipoFila.DETALLE));
            }
            filas.add(new FilaFlujo("Flujo neto de " + nombreCorto(actividad), f.neto(actividad), TipoFila.SUBTOTAL));
        }
        filas.add(new FilaFlujo(f.variacion().signum() >= 0 ? "AUMENTO DEL DINERO" : "DISMINUCIÓN DEL DINERO",
                f.variacion(), TipoFila.SUBTOTAL));
        filas.add(new FilaFlujo("Dinero al final (" + Fechas.paraMostrar(f.periodo().hasta()) + ")", f.saldoFinal(),
                TipoFila.RESULTADO));
        return filas;
    }

    private static String nombreCorto(Actividad actividad) {
        return switch (actividad) {
            case OPERACION -> "operación";
            case INVERSION -> "inversión";
            case FINANCIAMIENTO -> "financiamiento";
            case INTERNA -> "transferencias";
        };
    }

    @FXML
    private void ultimosTresMeses() {
        // Meses completos: con el mes en curso a medias, las compras del mes ya pagadas pesan más que las ventas.
        LocalDate hoy = LocalDate.now();
        dpFlujoHasta.setValue(hoy.withDayOfMonth(1).minusDays(1));
        dpFlujoDesde.setValue(hoy.withDayOfMonth(1).minusMonths(3));
    }

    @FXML
    private void esteAnio() {
        LocalDate hoy = LocalDate.now();
        dpFlujoHasta.setValue(hoy);
        dpFlujoDesde.setValue(hoy.withDayOfYear(1));
    }

    // ------------------------------------------------------------------ cuentas

    @FXML
    private void nuevaCuenta() {
        FormularioCuenta formulario = new FormularioCuenta(null);
        CuentaDinero[] creada = new CuentaDinero[1];
        boolean ok = Dialogos.formulario(ventana(), "Nueva cuenta",
                "Una cuenta es un lugar donde la empresa guarda dinero: la caja, una cuenta bancaria o una billetera "
                        + "digital.", formulario.contenido(), "Registrar",
                () -> creada[0] = servicio().registrarCuenta(formulario.datos()));
        if (ok) {
            cargar();
            Notificador.exito("Cuenta registrada: " + creada[0].nombre() + ".");
        }
    }

    @FXML
    private void editarCuenta() {
        SaldoCuenta s = tablaCuentas.getSelectionModel().getSelectedItem();
        if (s == null || !contexto.sesion().tienePermiso(Permiso.FINANZAS_GESTIONAR)) {
            return;
        }
        FormularioCuenta formulario = new FormularioCuenta(s.cuenta());
        boolean ok = Dialogos.formulario(ventana(), "Editar cuenta", null, formulario.contenido(), "Guardar cambios",
                () -> servicio().actualizarCuenta(s.cuenta().id(), formulario.datos()));
        if (ok) {
            cargar();
            Notificador.exito("Cuenta actualizada.");
        }
    }

    @FXML
    private void cambiarActiva() {
        SaldoCuenta s = tablaCuentas.getSelectionModel().getSelectedItem();
        if (s == null) {
            return;
        }
        boolean activar = !s.cuenta().activa();
        if (!Dialogos.confirmar(ventana(), activar ? "Activar cuenta" : "Desactivar cuenta", activar
                ? "«" + s.cuenta().nombre() + "» volverá a aparecer al registrar ventas, gastos y pagos."
                : "«" + s.cuenta().nombre() + "» dejará de aparecer al registrar. Su historial se conserva. "
                + "Solo se puede desactivar una cuenta con saldo cero.")) {
            return;
        }
        try {
            servicio().cambiarCuentaActiva(s.cuenta().id(), activar);
            cargar();
            Notificador.exito(activar ? "Cuenta activada." : "Cuenta desactivada.");
        } catch (RuntimeException e) {
            Notificador.error(Errores.mensaje(e));
        }
    }

    // ------------------------------------------------------------------ transferencias, aportes y retiros

    @FXML
    private void nuevoMovimiento() {
        List<CuentaDinero> cuentas;
        try {
            cuentas = servicio().cuentasActivas();
        } catch (RuntimeException e) {
            Notificador.error(Errores.mensaje(e));
            return;
        }
        ComboBox<TipoMovimientoTesoreria> tipo = new ComboBox<>(
                FXCollections.observableArrayList(TipoMovimientoTesoreria.values()));
        tipo.setMaxWidth(Double.MAX_VALUE);
        DatePicker fecha = CamposFecha.nuevo(LocalDate.now());
        SelectorCuenta origen = new SelectorCuenta(cuentas, null, null);
        SelectorCuenta destino = new SelectorCuenta(cuentas, null, cuentas.size() > 1 ? cuentas.get(1).id() : null);
        TextField monto = new TextField();
        monto.setPromptText("0.00");
        TextField descripcion = new TextField();
        descripcion.setPromptText("Opcional. Ej.: depósito de la venta de la semana");
        Label ayuda = new Label();
        ayuda.getStyleClass().add("hint");
        ayuda.setWrapText(true);
        tipo.valueProperty().addListener((o, a, t) -> {
            origen.setDisable(t == null || !t.usaOrigen());
            destino.setDisable(t == null || !t.usaDestino());
            ayuda.setText(t == null ? "" : switch (t) {
                case TRANSFERENCIA -> "El dinero cambia de lugar (por ejemplo, depositar el efectivo de la caja en el "
                        + "banco). El total no cambia.";
                case APORTE -> "El dueño pone dinero en el negocio. Aumenta el dinero, pero no es una venta ni un "
                        + "ingreso: no cambia la utilidad.";
                case RETIRO -> "El dueño saca dinero para uso personal. Disminuye el dinero, pero no es un gasto del "
                        + "negocio: no cambia la utilidad.";
            });
        });
        tipo.setValue(TipoMovimientoTesoreria.TRANSFERENCIA);
        GridPane grilla = Formularios.grilla();
        Formularios.fila(grilla, "Tipo *", tipo);
        Formularios.fila(grilla, "", ayuda);
        Formularios.fila(grilla, "Fecha *", fecha);
        Formularios.fila(grilla, "Sale de", origen);
        Formularios.fila(grilla, "Entra a", destino);
        Formularios.fila(grilla, "Monto (" + moneda.simbolo() + ") *", monto);
        Formularios.fila(grilla, "Descripción", descripcion);
        boolean ok = Dialogos.formulario(ventana(), "Mover dinero", null, grilla, "Registrar",
                () -> servicio().registrarMovimiento(new DatosMovimientoTesoreria(tipo.getValue(),
                        CamposFecha.leer(fecha, "el movimiento"), origen.cuentaId(), destino.cuentaId(),
                        Montos.leer(monto.getText(), "Monto"), descripcion.getText())));
        if (ok) {
            cargar();
            Notificador.exito(tipo.getValue().descripcion() + " registrado.");
        }
    }

    @FXML
    private void anularMovimiento() {
        MovimientoTesoreria m = tablaMovimientos.getSelectionModel().getSelectedItem();
        if (m == null || m.anulado()) {
            return;
        }
        TextField motivo = new TextField();
        motivo.setPromptText("Ej.: registrado dos veces");
        GridPane grilla = Formularios.grilla();
        Formularios.fila(grilla, "Motivo *", motivo);
        boolean ok = Dialogos.formulario(ventana(), "Anular movimiento",
                m.tipo().descripcion() + " de " + Montos.formatear(m.monto(), moneda) + " del "
                        + Fechas.paraMostrar(m.fecha()) + ": dejará de mover las cuentas, pero se conservará como anulado.",
                grilla, "Anular", () -> servicio().anularMovimiento(m.id(), motivo.getText()));
        if (ok) {
            cargar();
            Notificador.exito("Movimiento anulado.");
        }
    }

    // ------------------------------------------------------------------ cobros y pagos

    @FXML
    private void registrarPago() {
        Operacion o = tablaPendientes.getSelectionModel().getSelectedItem();
        if (o == null || o.pendiente().signum() == 0) {
            return;
        }
        boolean venta = o.tipo() == TipoOperacion.VENTA;
        DatePicker fecha = CamposFecha.nuevo(LocalDate.now());
        TextField monto = new TextField(Montos.paraEditar(o.pendiente()));
        SelectorCuenta cuenta = new SelectorCuenta(servicio().cuentasActivas(), null, null);
        TextField nota = new TextField();
        nota.setPromptText("Opcional. Ej.: adelanto, transferencia");
        GridPane grilla = Formularios.grilla();
        Formularios.fila(grilla, "Fecha *", fecha);
        Formularios.fila(grilla, "Monto (" + moneda.simbolo() + ") *", monto);
        Formularios.fila(grilla, venta ? "Entra a *" : "Sale de *", cuenta);
        Formularios.fila(grilla, "Nota", nota);
        Formularios.nota(grilla, "Pendiente: " + Montos.formatear(o.pendiente(), moneda) + " de "
                + Montos.formatear(o.total(), moneda) + ". Puedes registrar pagos parciales.");
        Operacion[] actualizada = new Operacion[1];
        boolean ok = Dialogos.formulario(ventana(), (venta ? "Cobrar " : "Pagar ") + o.numero() + " · "
                        + Objects.requireNonNullElse(o.contraparte(), ""), null, grilla,
                venta ? "Registrar cobro" : "Registrar pago",
                () -> {
                    if (cuenta.cuentaId() == null) {
                        throw new com.businessanalyzer.exception.ValidacionException("Elige la cuenta.");
                    }
                    actualizada[0] = servicio().registrarPago(o.id(), cuenta.cuentaId(),
                            CamposFecha.leer(fecha, venta ? "el cobro" : "el pago"), Montos.leer(monto.getText(), "Monto"),
                            nota.getText());
                });
        if (ok) {
            cargar();
            Notificador.exito(actualizada[0].pendiente().signum() == 0
                    ? o.numero() + (venta ? " quedó cobrada por completo." : " quedó pagada por completo.")
                    : "Registrado. Falta " + Montos.formatear(actualizada[0].pendiente(), moneda) + ".");
        }
    }

    @FXML
    private void verPagos() {
        Operacion o = tablaPendientes.getSelectionModel().getSelectedItem();
        if (o == null) {
            return;
        }
        boolean venta = o.tipo() == TipoOperacion.VENTA;
        TableView<PagoOperacion> tabla = new TableView<>();
        tabla.setPrefHeight(260);
        tabla.setPlaceholder(new Label(venta ? "Aún no hay cobros." : "Aún no hay pagos."));
        tabla.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        tabla.getColumns().add(columna("Fecha", 95, p -> Fechas.paraMostrar(p.fecha()), false));
        tabla.getColumns().add(columna("Monto", 110, p -> Montos.formatear(p.monto(), moneda), true));
        tabla.getColumns().add(columna("Cuenta", 120, PagoOperacion::cuenta, false));
        tabla.getColumns().add(columna("Nota", 150, p -> Objects.requireNonNullElse(p.nota(), ""), false));
        tabla.getColumns().add(columna("Registró", 120, p -> p.anulado() ? "Anulado por " + p.anuladoPor()
                : p.registradoPor(), false));
        tabla.setRowFactory(t -> new TableRow<>() {
            @Override
            protected void updateItem(PagoOperacion p, boolean vacio) {
                super.updateItem(p, vacio);
                pseudoClassStateChanged(ARCHIVADO, !vacio && p != null && p.anulado());
            }
        });
        Label resumen = new Label();
        resumen.getStyleClass().add("info-value");
        Runnable recargar = () -> {
            List<PagoOperacion> pagos = servicio().listarPagos(o.id());
            tabla.getItems().setAll(pagos);
            BigDecimal pagado = pagos.stream().filter(p -> !p.anulado()).map(PagoOperacion::monto)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            resumen.setText("Total: " + Montos.formatear(o.total(), moneda) + "   ·   " + (venta ? "Cobrado: " : "Pagado: ")
                    + Montos.formatear(pagado, moneda) + "   ·   Pendiente: "
                    + Montos.formatear(o.total().subtract(pagado).max(BigDecimal.ZERO), moneda));
        };
        boolean[] cambios = {false};
        VBox contenido = new VBox(10, resumen, tabla);
        if (contexto.sesion().tienePermiso(Permiso.OPERACIONES_GESTIONAR)) {
            Button anular = new Button(venta ? "Anular cobro seleccionado" : "Anular pago seleccionado");
            anular.getStyleClass().add("button-secondary");
            anular.disableProperty().bind(tabla.getSelectionModel().selectedItemProperty().isNull());
            anular.setOnAction(e -> {
                PagoOperacion p = tabla.getSelectionModel().getSelectedItem();
                if (p == null || p.anulado()) {
                    return;
                }
                if (Dialogos.confirmar(tabla.getScene().getWindow(), venta ? "Anular cobro" : "Anular pago",
                        "El registro de " + Montos.formatear(p.monto(), moneda) + " del " + Fechas.paraMostrar(p.fecha())
                                + " se anulará: el dinero sale de «" + p.cuenta() + "» y lo pendiente vuelve a subir.")) {
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
        Dialogos.informacion(ventana(), (venta ? "Cobros de " : "Pagos de ") + o.numero(),
                Objects.requireNonNullElse(o.contraparte(), ""), contenido);
        if (cambios[0]) {
            cargar();
        }
    }

    // ------------------------------------------------------------------ tablas

    private void configurarTablaCuentas() {
        colCtaNombre.setCellValueFactory(c -> texto(c.getValue().cuenta().nombre()
                + (c.getValue().cuenta().activa() ? "" : "  (desactivada)")));
        colCtaTipo.setCellValueFactory(c -> texto(c.getValue().cuenta().tipo().descripcion()));
        colCtaNumero.setCellValueFactory(c -> texto(Objects.requireNonNullElse(c.getValue().cuenta().numero(), "—")));
        colCtaInicial.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().cuenta().saldoInicial()));
        colCtaInicial.setCellFactory(col -> celdaMonto(false));
        colCtaEntradas.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().entradas()));
        colCtaEntradas.setCellFactory(col -> celdaMonto(false));
        colCtaSalidas.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().salidas()));
        colCtaSalidas.setCellFactory(col -> celdaMonto(false));
        colCtaSaldo.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().saldo()));
        colCtaSaldo.setCellFactory(col -> celdaMonto(true));
        colCtaUltimo.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().ultimoMovimiento()));
        colCtaUltimo.setCellFactory(col -> celdaFecha());
        tablaCuentas.getSelectionModel().selectedItemProperty().addListener((o, a, n) -> actualizarAcciones());
        tablaCuentas.setRowFactory(t -> {
            TableRow<SaldoCuenta> fila = new TableRow<>() {
                @Override
                protected void updateItem(SaldoCuenta s, boolean vacio) {
                    super.updateItem(s, vacio);
                    pseudoClassStateChanged(ARCHIVADO, !vacio && s != null && !s.cuenta().activa());
                }
            };
            fila.setOnMouseClicked(e -> {
                if (e.getButton() == MouseButton.PRIMARY && e.getClickCount() == 2 && !fila.isEmpty()) {
                    editarCuenta();
                }
            });
            return fila;
        });
    }

    private void configurarTablaMovimientos() {
        colMovFecha.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().fecha()));
        colMovFecha.setCellFactory(col -> celdaFecha());
        colMovTipo.setCellValueFactory(c -> texto(c.getValue().tipo().descripcion()
                + (c.getValue().anulado() ? "  (anulado)" : "")));
        colMovOrigen.setCellValueFactory(c -> texto(Objects.requireNonNullElse(c.getValue().cuentaOrigen(), "—")));
        colMovDestino.setCellValueFactory(c -> texto(Objects.requireNonNullElse(c.getValue().cuentaDestino(), "—")));
        colMovMonto.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().monto()));
        colMovMonto.setCellFactory(col -> celdaMonto(false));
        colMovDescripcion.setCellValueFactory(c -> texto(c.getValue().descripcion()));
        tablaMovimientos.getSelectionModel().selectedItemProperty().addListener((o, a, n) -> actualizarAcciones());
        tablaMovimientos.setRowFactory(t -> new TableRow<>() {
            @Override
            protected void updateItem(MovimientoTesoreria m, boolean vacio) {
                super.updateItem(m, vacio);
                boolean anulado = !vacio && m != null && m.anulado();
                pseudoClassStateChanged(ARCHIVADO, anulado);
                setTooltip(anulado ? new Tooltip("Anulado: " + m.motivoAnulacion()) : null);
            }
        });
    }

    private void configurarTablaPendientes() {
        colPenNumero.setCellValueFactory(c -> texto(c.getValue().numero()));
        colPenFecha.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().fecha()));
        colPenFecha.setCellFactory(col -> celdaFecha());
        colPenContraparte.setCellValueFactory(c -> texto(Objects.requireNonNullElse(c.getValue().contraparte(), "—")));
        colPenTotal.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().total()));
        colPenTotal.setCellFactory(col -> celdaMonto(false));
        colPenPagado.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().pagado()));
        colPenPagado.setCellFactory(col -> celdaMonto(false));
        colPenPendiente.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().pendiente()));
        colPenPendiente.setCellFactory(col -> celdaMonto(false));
        colPenVence.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().vencimiento()));
        colPenVence.setCellFactory(col -> celdaFecha());
        colPenEstado.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue()));
        colPenEstado.setSortable(false);
        colPenEstado.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Operacion o, boolean vacio) {
                super.updateItem(o, vacio);
                getStyleClass().removeAll("margen-negativo", "margen-bajo");
                if (vacio || o == null) {
                    setText(null);
                    return;
                }
                LocalDate hoy = LocalDate.now();
                if (o.vencimiento() == null) {
                    setText("Sin fecha límite");
                } else if (o.vencidaEn(hoy)) {
                    long dias = ChronoUnit.DAYS.between(o.vencimiento(), hoy);
                    setText("Vencida hace " + dias + (dias == 1 ? " día" : " días"));
                    getStyleClass().add("margen-negativo");
                } else {
                    long dias = ChronoUnit.DAYS.between(hoy, o.vencimiento());
                    setText(dias == 0 ? "Vence hoy" : "Vence en " + dias + (dias == 1 ? " día" : " días"));
                    if (dias <= 7) {
                        getStyleClass().add("margen-bajo");
                    }
                }
            }
        });
        tablaPendientes.getSelectionModel().selectedItemProperty().addListener((o, a, n) -> actualizarAcciones());
        tablaPendientes.setRowFactory(t -> {
            TableRow<Operacion> fila = new TableRow<>();
            fila.setOnMouseClicked(e -> {
                if (e.getButton() == MouseButton.PRIMARY && e.getClickCount() == 2 && !fila.isEmpty()) {
                    verPagos();
                }
            });
            return fila;
        });
    }

    private void configurarTablaFlujo() {
        colFluConcepto.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue()));
        colFluConcepto.setCellFactory(col -> celdaFlujo(FilaFlujo::concepto, false));
        colFluMonto.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue()));
        colFluMonto.setCellFactory(col -> celdaFlujo(f -> f.monto() == null ? "" : Montos.formatear(f.monto(), moneda), true));
        tablaFlujo.setRowFactory(t -> new TableRow<>() {
            @Override
            protected void updateItem(FilaFlujo fila, boolean vacio) {
                super.updateItem(fila, vacio);
                pseudoClassStateChanged(SUBTOTAL, !vacio && fila != null
                        && (fila.tipo() == TipoFila.SUBTOTAL || fila.tipo() == TipoFila.TITULO));
                pseudoClassStateChanged(RESULTADO, !vacio && fila != null && fila.tipo() == TipoFila.RESULTADO);
            }
        });
    }

    private TableCell<FilaFlujo, FilaFlujo> celdaFlujo(Function<FilaFlujo, String> texto, boolean colorear) {
        return new TableCell<>() {
            @Override
            protected void updateItem(FilaFlujo fila, boolean vacio) {
                super.updateItem(fila, vacio);
                getStyleClass().remove("margen-negativo");
                if (vacio || fila == null) {
                    setText(null);
                    return;
                }
                setText(texto.apply(fila));
                if (colorear && fila.monto() != null && fila.monto().signum() < 0) {
                    getStyleClass().add("margen-negativo");
                }
            }
        };
    }

    private void configurarTablaProyeccion() {
        colProMes.setCellValueFactory(c -> texto(mesTexto(c.getValue().mes())
                + (c.getValue().mes().equals(YearMonth.now()) ? " (resto)" : "")));
        colProOperacion.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().operacion()));
        colProOperacion.setCellFactory(col -> celdaMonto(true));
        colProDeudas.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().deudas()));
        colProDeudas.setCellFactory(col -> celdaMonto(true));
        colProSaldo.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().saldo()));
        colProSaldo.setCellFactory(col -> celdaMonto(true));
        colProPesimista.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().saldoPesimista()));
        colProPesimista.setCellFactory(col -> celdaMonto(true));
    }

    private void actualizarAcciones() {
        SaldoCuenta s = tablaCuentas.getSelectionModel().getSelectedItem();
        btnEditarCuenta.setDisable(s == null);
        btnActivarCuenta.setDisable(s == null);
        btnActivarCuenta.setText(s != null && !s.cuenta().activa() ? "Activar" : "Desactivar");
        MovimientoTesoreria m = tablaMovimientos.getSelectionModel().getSelectedItem();
        btnAnularMovimiento.setDisable(m == null || m.anulado());
        Operacion o = tablaPendientes.getSelectionModel().getSelectedItem();
        btnVerPagos.setDisable(o == null);
        btnRegistrarPago.setDisable(o == null || o.pendiente().signum() == 0);
    }

    // ------------------------------------------------------------------ utilidades

    /** Celda de monto; con {@code colorear}, los negativos en rojo. */
    private <T> TableCell<T, BigDecimal> celdaMonto(boolean colorear) {
        return new TableCell<>() {
            @Override
            protected void updateItem(BigDecimal monto, boolean vacio) {
                super.updateItem(monto, vacio);
                getStyleClass().remove("margen-negativo");
                setText(vacio ? null : monto == null ? "—" : Montos.formatear(monto, moneda));
                if (colorear && !vacio && monto != null && monto.signum() < 0) {
                    getStyleClass().add("margen-negativo");
                }
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

    private static TableColumn<PagoOperacion, String> columna(String titulo, double ancho,
                                                              Function<PagoOperacion, String> valor, boolean numero) {
        TableColumn<PagoOperacion, String> columna = new TableColumn<>(titulo);
        columna.setPrefWidth(ancho);
        columna.setSortable(false);
        columna.setCellValueFactory(c -> new ReadOnlyStringWrapper(valor.apply(c.getValue())));
        if (numero) {
            columna.getStyleClass().add("col-numero");
        }
        return columna;
    }

    private static String mesTexto(YearMonth mes) {
        return Fechas.nombreMes(mes.getMonthValue()) + " " + mes.getYear();
    }

    private static ReadOnlyStringWrapper texto(String valor) {
        return new ReadOnlyStringWrapper(valor);
    }

    private TesoreriaService servicio() {
        return contexto.tesoreria();
    }

    private Window ventana() {
        return tablaCuentas.getScene().getWindow();
    }

    /** Formulario de cuenta de dinero. */
    private final class FormularioCuenta {
        private final TextField txtNombre = new TextField();
        private final ComboBox<TipoCuenta> cmbTipo = new ComboBox<>(FXCollections.observableArrayList(TipoCuenta.values()));
        private final TextField txtNumero = new TextField();
        private final TextField txtSaldo = new TextField();

        FormularioCuenta(CuentaDinero c) {
            txtNombre.setPromptText("Ej.: BCP Cuenta corriente, Yape, Caja chica");
            txtNumero.setPromptText("Opcional: número de cuenta, CCI o celular");
            txtSaldo.setPromptText("0.00");
            cmbTipo.setMaxWidth(Double.MAX_VALUE);
            cmbTipo.setValue(TipoCuenta.BANCO);
            if (c != null) {
                txtNombre.setText(c.nombre());
                cmbTipo.setValue(c.tipo());
                txtNumero.setText(Objects.requireNonNullElse(c.numero(), ""));
                txtSaldo.setText(Montos.paraEditar(c.saldoInicial()));
            }
        }

        GridPane contenido() {
            GridPane grilla = Formularios.grilla();
            Formularios.fila(grilla, "Nombre *", txtNombre);
            Formularios.fila(grilla, "Tipo *", cmbTipo);
            Formularios.fila(grilla, "Número", txtNumero);
            Formularios.fila(grilla, "Saldo inicial (" + moneda.simbolo() + ")", txtSaldo);
            Formularios.nota(grilla, "Saldo inicial: el dinero que tenía la cuenta antes del primer movimiento que "
                    + "registres en el sistema. Desde ahí el saldo se calcula solo.");
            return grilla;
        }

        DatosCuenta datos() {
            return new DatosCuenta(txtNombre.getText(), cmbTipo.getValue(), txtNumero.getText(),
                    txtSaldo.getText() == null || txtSaldo.getText().isBlank() ? BigDecimal.ZERO
                            : Montos.leer(txtSaldo.getText(), "Saldo inicial"));
        }
    }
}
