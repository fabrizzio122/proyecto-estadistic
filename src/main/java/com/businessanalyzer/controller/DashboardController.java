package com.businessanalyzer.controller;

import com.businessanalyzer.BusinessAnalyzerApp;
import com.businessanalyzer.config.AppContext;
import com.businessanalyzer.model.Empresa;
import com.businessanalyzer.model.Permiso;
import com.businessanalyzer.util.Fechas;
import com.businessanalyzer.util.Montos;
import com.businessanalyzer.analytics.ResumenOperaciones;
import com.businessanalyzer.ui.Errores;
import java.time.YearMonth;
import java.util.List;
import com.businessanalyzer.analytics.EstadoResultados;
import com.businessanalyzer.analytics.PuntoEquilibrio;
import com.businessanalyzer.service.AnalisisService;
import org.kordamp.ikonli.javafx.FontIcon;
import com.businessanalyzer.ui.Dialogos;
import com.businessanalyzer.ui.Modulo;
import com.businessanalyzer.ui.Notificador;
import com.businessanalyzer.ui.TarjetaIndicador;
import com.businessanalyzer.ui.VistaModulo;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.util.Comparator;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Panel principal. En la Fase 1 muestra la estructura de indicadores (sin datos todavia),
 * los primeros pasos recomendados y el estado del sistema.
 * Los indicadores reales se conectan en la Fase 7 (analisis financiero).
 */
public class DashboardController implements VistaModulo {

    private final AppContext contexto;

    @FXML private HBox bannerEmpresa;
    @FXML private FontIcon iconoBanner;
    @FXML private Label lblBannerTitulo;
    @FXML private Label lblBannerDetalle;
    @FXML private HBox indicadores;
    @FXML private VBox pasos;
    @FXML private Label lblVersion;
    @FXML private Label lblEsquema;
    @FXML private Label lblBaseDatos;
    @FXML private Label lblJava;
    @FXML private Label lblJavaFx;

    private final TarjetaIndicador kpiVentasMes = new TarjetaIndicador("Ventas del mes", "fth-shopping-bag");
    private final TarjetaIndicador kpiUtilidad = new TarjetaIndicador("Utilidad neta del mes", "fth-dollar-sign");
    private final TarjetaIndicador kpiMargenNeto = new TarjetaIndicador("Margen neto", "fth-percent");
    private final TarjetaIndicador kpiEquilibrio = new TarjetaIndicador("Punto de equilibrio", "fth-target");

    public DashboardController(AppContext contexto) {
        this.contexto = contexto;
    }

    @FXML
    private void initialize() {
        indicadores.getChildren().addAll(kpiVentasMes, kpiUtilidad, kpiMargenNeto, kpiEquilibrio);

        construirPrimerosPasos();

        lblVersion.setText(BusinessAnalyzerApp.VERSION);
        lblEsquema.setText("v" + contexto.versionEsquema());
        lblBaseDatos.setText(contexto.config().archivoBaseDatos().toString());
        lblJava.setText(System.getProperty("java.version"));
        lblJavaFx.setText(System.getProperty("javafx.runtime.version", "desconocida"));
    }

    @Override
    public void alMostrar() {
        Optional<Empresa> empresa = contexto.sesion().empresaActiva();
        bannerEmpresa.getStyleClass().remove("banner-aviso");
        if (empresa.isPresent()) {
            Empresa e = empresa.get();
            iconoBanner.setIconLiteral("fth-briefcase");
            lblBannerTitulo.setText(e.nombreVisible());
            lblBannerDetalle.setText(e.sector().descripcion() + "   ·   " + e.tamano().descripcion()
                    + "   ·   " + e.moneda().nombre() + " (" + e.moneda().simbolo() + ")"
                    + "   ·   Ejercicio fiscal desde " + Fechas.nombreMes(e.mesInicioEjercicio()));
        } else {
            bannerEmpresa.getStyleClass().add("banner-aviso");
            iconoBanner.setIconLiteral("fth-alert-circle");
            lblBannerTitulo.setText("No hay una empresa activa");
            lblBannerDetalle.setText(contexto.sesion().tienePermiso(Permiso.EMPRESAS_GESTIONAR)
                    ? "Registra tu primera empresa en el módulo Empresas o elige una en el selector de la barra superior."
                    : "Pide a un administrador que te asigne una empresa para empezar a trabajar.");
        }
        actualizarVentasDelMes(empresa.orElse(null));
        actualizarResultadoDelMes(empresa.orElse(null));
    }

    /** Utilidad neta, margen neto y punto de equilibrio del mes en curso (Fase 7). */
    private void actualizarResultadoDelMes(Empresa empresa) {
        List<TarjetaIndicador> tarjetas = List.of(kpiUtilidad, kpiMargenNeto, kpiEquilibrio);
        if (empresa == null) {
            tarjetas.forEach(t -> t.actualizar(TarjetaIndicador.SIN_VALOR, "Requiere una empresa activa"));
            return;
        }
        if (!contexto.sesion().tienePermiso(Permiso.ANALISIS_VER)) {
            tarjetas.forEach(t -> t.actualizar(TarjetaIndicador.SIN_VALOR, "Requiere permiso de análisis"));
            return;
        }
        try {
            YearMonth mes = YearMonth.now();
            AnalisisService.ResumenPeriodo r = contexto.analisis().resumen(mes.atDay(1), mes.atEndOfMonth());
            EstadoResultados e = r.estado();
            PuntoEquilibrio pe = r.equilibrio();
            kpiUtilidad.actualizar(Montos.formatear(e.utilidadNeta(), empresa.moneda()),
                    e.utilidadNeta().signum() >= 0 ? "Ganancia después de todos los gastos" : "El mes va con pérdida");
            kpiMargenNeto.actualizar(Montos.porcentaje(e.margenNeto()),
                    "Margen bruto " + Montos.porcentaje(e.margenBruto()));
            if (pe.ventasEquilibrio() == null) {
                kpiEquilibrio.actualizar("No alcanzable", e.ventasNetas().signum() == 0
                        ? "Sin ventas este mes" : "Las ventas no cubren sus costos variables");
            } else {
                kpiEquilibrio.actualizar(Montos.formatear(pe.ventasEquilibrio(), empresa.moneda()), pe.alcanzado()
                        ? "Alcanzado ✓"
                        : "Faltan " + Montos.formatear(pe.ventasFaltantes(), empresa.moneda()) + " en ventas");
            }
        } catch (RuntimeException ex) {
            tarjetas.forEach(t -> t.actualizar(TarjetaIndicador.SIN_VALOR, Errores.mensaje(ex)));
        }
    }

    /** Ventas netas (sin impuestos ni anuladas) del mes en curso. */
    private void actualizarVentasDelMes(Empresa empresa) {
        if (empresa == null || !contexto.sesion().tienePermiso(Permiso.OPERACIONES_VER)) {
            kpiVentasMes.actualizar(TarjetaIndicador.SIN_VALOR, "Requiere una empresa activa");
            return;
        }
        try {
            YearMonth mes = YearMonth.now();
            ResumenOperaciones r = contexto.operaciones().resumenConAgro(mes.atDay(1), mes.atEndOfMonth());
            kpiVentasMes.actualizar(Montos.formatear(r.ventasNetas(), empresa.moneda()),
                    r.cantidadVentas() + (r.cantidadVentas() == 1 ? " venta" : " ventas")
                            + " · margen bruto " + Montos.porcentaje(r.margenBrutoSobreVentas()));
        } catch (RuntimeException e) {
            kpiVentasMes.actualizar(TarjetaIndicador.SIN_VALOR, Errores.mensaje(e));
        }
    }

    private void construirPrimerosPasos() {
        int numero = 1;
        for (Modulo modulo : Stream.of(Modulo.USUARIOS, Modulo.EMPRESAS, Modulo.PRODUCTOS,
                        Modulo.VENTAS_COMPRAS, Modulo.FINANZAS)
                .sorted(Comparator.comparingInt(Modulo::fase)).toList()) {
            pasos.getChildren().add(filaPaso(numero++, modulo));
        }
    }

    private static Node filaPaso(int numero, Modulo modulo) {
        Label circulo = new Label(String.valueOf(numero));
        circulo.getStyleClass().add("step-number");
        Label titulo = new Label(modulo.titulo());
        titulo.getStyleClass().add("step-title");
        Label descripcion = new Label(modulo.descripcion());
        descripcion.getStyleClass().add("muted");
        Label fase = new Label(modulo.disponible() ? "Disponible" : "Fase " + modulo.fase());
        fase.getStyleClass().add("chip");
        if (modulo.disponible()) {
            fase.getStyleClass().add("chip-ok");
        }

        VBox textos = new VBox(2, titulo, descripcion);
        HBox.setHgrow(textos, javafx.scene.layout.Priority.ALWAYS);
        HBox fila = new HBox(12, circulo, textos, fase);
        fila.setAlignment(Pos.CENTER_LEFT);
        return fila;
    }

    @FXML
    private void probarNotificaciones() {
        Notificador.exito("Operación completada correctamente.");
        Notificador.info("Esta es una notificación informativa.");
        Notificador.advertencia("Revisa los datos antes de continuar.");
        Notificador.error("Ejemplo de mensaje de error.");
    }

    @FXML
    private void probarConfirmacion(ActionEvent evento) {
        boolean aceptado = Dialogos.confirmar(
                ((Node) evento.getSource()).getScene().getWindow(),
                "Confirmar acción",
                "Esta es una prueba del cuadro de confirmación. ¿Deseas continuar?");
        Notificador.info(aceptado ? "Confirmaste la acción." : "Cancelaste la acción.");
    }
}
