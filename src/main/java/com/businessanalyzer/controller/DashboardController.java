package com.businessanalyzer.controller;

import com.businessanalyzer.analytics.AnalisisFinanciero;
import com.businessanalyzer.analytics.EstadoResultados;
import com.businessanalyzer.analytics.FraseMes;
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
import com.businessanalyzer.service.TesoreriaService;
import com.businessanalyzer.ui.BarraProporcion;
import com.businessanalyzer.ui.Errores;
import com.businessanalyzer.ui.GraficoMeses;
import com.businessanalyzer.ui.MedidorSalud;
import com.businessanalyzer.ui.Modulo;
import com.businessanalyzer.ui.Tareas;
import com.businessanalyzer.ui.VistaModulo;
import com.businessanalyzer.util.Fechas;
import com.businessanalyzer.util.Montos;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import org.kordamp.ikonli.javafx.FontIcon;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Panel principal: el mes en curso resumido en una frase y cuatro indicadores (comparados con los mismos
 * días del mes anterior), la salud del negocio, las ventas y la utilidad de los últimos 12 meses con el
 * cierre esperado del mes, lo que viene y los productos que más aportan. Cada tarjeta lleva al módulo
 * donde se ve el detalle. Todo sale de los datos reales de la empresa activa.
 */
public class DashboardController implements VistaModulo {

    private static final int MAX_PRODUCTOS = 6;
    private static final int MAX_ALERTAS = 2;

    private final AppContext contexto;
    private long solicitud;

    /** Lo que muestra el panel; cada parte puede faltar (sin permiso o sin datos). */
    private record Panel(AnalisisFinanciero analisis, Diagnostico diagnostico, ResultadoPrediccion prediccion,
                         String errorDiagnostico, String errorPrediccion, TesoreriaService.Resumen tesoreria) {
    }

    @FXML private HBox bannerEmpresa;
    @FXML private FontIcon iconoBanner;
    @FXML private Label lblBannerTitulo;
    @FXML private Label lblBannerDetalle;
    @FXML private VBox contenido;
    @FXML private VBox tarjetaMes;
    @FXML private VBox salud;
    @FXML private VBox evolucion;
    @FXML private VBox proximo;
    @FXML private VBox productos;
    @FXML private Label lblTituloProductos;
    @FXML private Button btnAnalisis;
    @FXML private Button btnDiagnostico;
    @FXML private Button btnVentas;
    @FXML private Button btnPredicciones;

    public DashboardController(AppContext contexto) {
        this.contexto = contexto;
    }

    @FXML
    private void initialize() {
        boolean analisis = contexto.sesion().tienePermiso(Permiso.ANALISIS_VER);
        for (Button b : List.of(btnAnalisis, btnDiagnostico, btnPredicciones)) {
            b.setVisible(analisis);
        }
        btnVentas.setVisible(contexto.sesion().tienePermiso(Permiso.OPERACIONES_VER));
    }

    @Override
    public void alMostrar() {
        Optional<Empresa> empresa = contexto.sesion().empresaActiva();
        if (empresa.isEmpty()) {
            aviso("No hay una empresa activa", contexto.sesion().tienePermiso(Permiso.EMPRESAS_GESTIONAR)
                    ? "Registra tu primera empresa en el módulo Empresas (o crea la «Bodega Demo» para probar) "
                    + "o elige una en el selector de la barra superior."
                    : "Pide a un administrador que te asigne una empresa para empezar a trabajar.");
            return;
        }
        if (!contexto.sesion().tienePermiso(Permiso.ANALISIS_VER)) {
            aviso("Tu rol no incluye los análisis", "Registra ventas, compras y gastos desde el menú.");
            return;
        }
        Moneda moneda = empresa.get().moneda();
        LocalDate hoy = LocalDate.now();
        Periodo mes = new Periodo(hoy.withDayOfMonth(1), hoy);
        long numero = ++solicitud;
        Tareas.ejecutar(() -> cargar(mes), panel -> {
            if (numero == solicitud) {
                mostrar(panel, moneda, hoy);
            }
        }, error -> {
            if (numero == solicitud) {
                aviso("No se pudo cargar el resumen", Errores.mensaje(error));
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
            // Sin permiso de finanzas o sin cuentas: ese indicador no se muestra.
        }
        return new Panel(analisis, diagnostico, prediccion, errorDiagnostico, errorPrediccion, tesoreria);
    }

    private void aviso(String titulo, String detalle) {
        bannerEmpresa.setVisible(true);
        bannerEmpresa.setManaged(true);
        lblBannerTitulo.setText(titulo);
        lblBannerDetalle.setText(detalle);
        contenido.setVisible(false);
        contenido.setManaged(false);
    }

    private void mostrar(Panel p, Moneda moneda, LocalDate hoy) {
        bannerEmpresa.setVisible(false);
        bannerEmpresa.setManaged(false);
        contenido.setVisible(true);
        contenido.setManaged(true);
        Double esperadoVentas = esperado(p.prediccion(), true, hoy);
        Double esperadoUtilidad = esperado(p.prediccion(), false, hoy);
        mostrarMes(p.analisis(), p.tesoreria(), moneda);
        mostrarSalud(p.diagnostico(), p.errorDiagnostico());
        mostrarEvolucion(p.analisis(), esperadoVentas, esperadoUtilidad, moneda, hoy);
        mostrarProximo(p.prediccion(), p.errorPrediccion(), p.analisis().actual().ventasNetas(), moneda, hoy);
        mostrarProductos(p.analisis(), moneda);
        contexto.mostrarAlertasDiagnostico(p.diagnostico() == null || !p.diagnostico().evaluado()
                ? 0 : p.diagnostico().recomendaciones().size());
    }

    // ------------------------------------------------------------------ tu mes hasta hoy

    private void mostrarMes(AnalisisFinanciero a, TesoreriaService.Resumen tesoreria, Moneda moneda) {
        EstadoResultados e = a.actual();
        EstadoResultados anterior = a.anterior();
        PuntoEquilibrio pe = a.equilibrio();
        Periodo previo = a.periodoAnterior();
        String mesAnterior = Fechas.nombreMes(previo.desde().getMonthValue()).toLowerCase();
        String comparado = " vs. " + previo.desde().getDayOfMonth() + " – " + previo.hasta().getDayOfMonth() + " "
                + mesAnterior.substring(0, 3);

        Label tu = etiqueta("TU ", "etiqueta-seccion");
        Label mes = etiqueta("MES HASTA HOY", "etiqueta-seccion-acento");
        HBox kicker = new HBox(tu, mes);

        FraseMes frase = FraseMes.de(e.ventasNetas(), anterior.ventasNetas(), mesAnterior, pe.cobertura(),
                pe.ventasFaltantes() == null ? "" : Montos.formatear(pe.ventasFaltantes(), moneda));
        TextFlow texto = new TextFlow();
        texto.getStyleClass().add("frase-mes");
        for (FraseMes.Parte parte : frase.partes()) {
            Text t = new Text(parte.texto());
            t.getStyleClass().addAll("frase-mes-texto", switch (parte.tono()) {
                case POSITIVO -> "frase-positiva";
                case NEGATIVO -> "frase-negativa";
                case NORMAL -> "frase-normal";
            });
            texto.getChildren().add(t);
        }

        GridPane grilla = new GridPane();
        grilla.setHgap(28);
        grilla.setVgap(22);
        for (int i = 0; i < 3; i++) {
            ColumnConstraints col = new ColumnConstraints();
            col.setPercentWidth(33.3);
            grilla.getColumnConstraints().add(col);
        }
        grilla.add(indicador("Ventas del mes", Montos.formatear(e.ventasNetas(), moneda),
                variacion(e.ventasNetas(), anterior.ventasNetas(), comparado)), 0, 0);
        grilla.add(indicador("Utilidad neta", Montos.formatear(e.utilidadNeta(), moneda),
                e.utilidadNeta().signum() < 0 ? detalle("El mes va con pérdida", "texto-alerta")
                        : variacion(e.utilidadNeta(), anterior.utilidadNeta(), comparado)), 1, 0);
        grilla.add(indicador("Margen neto", Montos.porcentaje(e.margenNeto()),
                detalle("Margen bruto " + Montos.porcentaje(e.margenBruto()), "texto-tenue")), 2, 0);
        grilla.add(equilibrio(pe, moneda), 0, 1);
        if (tesoreria != null) {
            grilla.add(indicador("Dinero disponible", Montos.formatear(tesoreria.disponible(), moneda),
                    tesoreria.cuentasNegativas() > 0 ? detalle("Hay cuentas en negativo", "texto-alerta")
                            : tesoreria.porCobrarVencido().signum() > 0
                            ? detalle("Cobros vencidos: " + Montos.formatear(tesoreria.porCobrarVencido(), moneda),
                            "texto-alerta")
                            : detalle("Por cobrar: " + Montos.formatear(tesoreria.porCobrar(), moneda), "texto-tenue")),
                    1, 1);
        }
        Region linea = new Region();
        linea.getStyleClass().add("separador");
        tarjetaMes.getChildren().setAll(kicker, texto, linea, grilla);
    }

    private VBox indicador(String titulo, String valor, Node detalle) {
        Label lblTitulo = etiqueta(titulo, "indicador-titulo");
        Label lblValor = etiqueta(valor, "indicador-valor");
        lblValor.setMinWidth(Region.USE_PREF_SIZE);
        return new VBox(4, lblTitulo, lblValor, detalle);
    }

    /** «215 % cubierto» con una barra que muestra las ventas y una marca en el punto de equilibrio. */
    private VBox equilibrio(PuntoEquilibrio pe, Moneda moneda) {
        if (pe.ventasEquilibrio() == null) {
            return indicador("Punto de equilibrio", "No alcanzable", detalle(pe.ventas().signum() == 0
                    ? "Sin ventas este mes" : "Las ventas no cubren sus costos variables", "texto-alerta"));
        }
        BigDecimal cobertura = pe.cobertura();
        double ventas = pe.ventas().doubleValue();
        double meta = pe.ventasEquilibrio().doubleValue();
        double escala = Math.max(ventas, meta);
        BarraProporcion barra = new BarraProporcion(6, pe.alcanzado() ? "relleno-exito" : "relleno-acento");
        barra.llenar(ventas / escala).marcar(meta / escala);
        barra.setMaxWidth(220);
        Tooltip.install(barra, new Tooltip("La marca es el punto de equilibrio: "
                + Montos.formatear(pe.ventasEquilibrio(), moneda)));
        Label titulo = etiqueta("Punto de equilibrio · " + Montos.formatear(pe.ventasEquilibrio(), moneda),
                "indicador-titulo");
        Label valor = etiqueta(cobertura.multiply(BigDecimal.valueOf(100)).setScale(0, RoundingMode.HALF_UP)
                + " % cubierto", "indicador-valor");
        valor.setMinWidth(Region.USE_PREF_SIZE);
        return new VBox(4, titulo, valor, barra);
    }

    private Node variacion(BigDecimal actual, BigDecimal anterior, String comparado) {
        Variacion v = new Variacion(actual, anterior);
        if (v.relativa() == null) {
            return detalle("Sin comparación", "texto-tenue");
        }
        boolean sube = v.relativa().signum() >= 0;
        return detalle((sube ? "▲ " : "▼ ") + Montos.porcentaje(v.relativa().abs()) + comparado,
                sube ? "texto-positivo" : "texto-alerta");
    }

    private static Label detalle(String texto, String clase) {
        Label l = etiqueta(texto, "indicador-detalle");
        l.getStyleClass().add(clase);
        return l;
    }

    // ------------------------------------------------------------------ salud del negocio

    private void mostrarSalud(Diagnostico d, String error) {
        salud.getChildren().clear();
        if (d == null || !d.evaluado()) {
            Label sinDatos = etiqueta(error != null ? error : d == null ? "Sin datos." : String.join(" ", d.resumen()),
                    "muted");
            sinDatos.setWrapText(true);
            salud.getChildren().add(sinDatos);
            return;
        }
        MedidorSalud medidor = new MedidorSalud(40);
        medidor.mostrar(d.puntaje(), clase(d.nivel().color()));
        Label nivel = etiqueta(d.nivel().descripcion(), "nivel-salud");
        nivel.getStyleClass().add(clase(d.nivel().color()));
        nivel.setMinWidth(Region.USE_PREF_SIZE);
        Label evaluado = etiqueta("Evaluado: " + d.periodo().descripcion(), "indicador-detalle");
        evaluado.getStyleClass().add("texto-tenue");
        evaluado.setWrapText(true);
        VBox lado = new VBox(8, nivel, evaluado);
        lado.setAlignment(Pos.CENTER_LEFT);
        HBox cabecera = new HBox(18, medidor, lado);
        cabecera.setAlignment(Pos.CENTER_LEFT);
        Region linea = new Region();
        linea.getStyleClass().add("separador");
        salud.getChildren().addAll(cabecera, linea);

        List<Hallazgo> pendientes = d.recomendaciones();
        if (pendientes.isEmpty()) {
            Label bien = etiqueta("Todos los indicadores están en verde. ¡Buen trabajo!", "lectura");
            bien.setWrapText(true);
            salud.getChildren().add(bien);
            return;
        }
        salud.getChildren().add(etiqueta("Atender primero", "indicador-titulo"));
        for (Hallazgo h : pendientes.subList(0, Math.min(MAX_ALERTAS, pendientes.size()))) {
            Label nombre = etiqueta(h.tipo().nombre(), "fila-alerta-texto");
            nombre.setWrapText(true);
            nombre.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(nombre, Priority.ALWAYS);
            Label valor = etiqueta(h.valor(), "fila-alerta-valor");
            valor.setMinWidth(Region.USE_PREF_SIZE);
            HBox fila = new HBox(10, nombre, valor);
            fila.getStyleClass().add("fila-alerta");
            if (h.semaforo() == Semaforo.AMARILLO) {
                fila.getStyleClass().add("fila-alerta-media");
            }
            fila.setAlignment(Pos.CENTER_LEFT);
            if (h.recomendacion() != null) {
                Tooltip.install(fila, new Tooltip(h.recomendacion()));
            }
            salud.getChildren().add(fila);
        }
        if (pendientes.size() > MAX_ALERTAS) {
            salud.getChildren().add(etiqueta("y " + (pendientes.size() - MAX_ALERTAS) + " más en el diagnóstico",
                    "hint"));
        }
    }

    // ------------------------------------------------------------------ últimos 12 meses

    private void mostrarEvolucion(AnalisisFinanciero a, Double esperadoVentas, Double esperadoUtilidad, Moneda moneda,
                                  LocalDate hoy) {
        List<AnalisisFinanciero.ResultadoMensual> meses = a.evolucion();
        if (meses.isEmpty()) {
            evolucion.getChildren().setAll(etiqueta("Aún no hay meses con movimientos.", "muted"));
            return;
        }
        double[] ventas = meses.stream().mapToDouble(m -> m.estado().ventasNetas().doubleValue()).toArray();
        double[] utilidad = meses.stream().mapToDouble(m -> m.estado().utilidadNeta().doubleValue()).toArray();
        evolucion.getChildren().setAll(new GraficoMeses(
                meses.stream().map(AnalisisFinanciero.ResultadoMensual::mes).toList(), ventas, utilidad,
                esperadoVentas, esperadoUtilidad, YearMonth.from(hoy), hoy.getDayOfMonth(), moneda));
    }

    /** Cierre esperado del mes en curso según las predicciones; {@code null} si no hay pronóstico. */
    private static Double esperado(ResultadoPrediccion r, boolean ventas, LocalDate hoy) {
        if (r == null) {
            return null;
        }
        Pronostico p = ventas ? r.pronosticoVentas() : r.pronosticoUtilidad();
        if (p == null || p.puntos().isEmpty() || !r.primerMesProyectado(YearMonth.from(hoy)).equals(YearMonth.from(hoy))) {
            return null;
        }
        return p.puntos().get(0).valor();
    }

    // ------------------------------------------------------------------ lo que viene

    private void mostrarProximo(ResultadoPrediccion r, String error, BigDecimal vendido, Moneda moneda, LocalDate hoy) {
        proximo.getChildren().clear();
        Pronostico ventas = r == null ? null : r.pronosticoVentas();
        if (ventas == null || ventas.puntos().isEmpty()) {
            Label sin = etiqueta(error != null ? error
                    : "Se necesitan al menos unos meses completos de ventas para proyectar.", "muted");
            sin.setWrapText(true);
            proximo.getChildren().add(sin);
            return;
        }
        YearMonth mes = r.primerMesProyectado(YearMonth.from(hoy));
        Pronostico.Punto p = ventas.puntos().get(0);
        double inferior = Math.max(0, p.inferior());
        double superior = Math.max(p.superior(), p.valor());
        double margen = (superior - inferior) * 0.18 + 1;
        double desde = Math.max(0, inferior - margen);
        double hasta = superior + margen;

        Label titulo = etiqueta("Ventas esperadas en " + Fechas.nombreMes(mes.getMonthValue()).toLowerCase(),
                "indicador-titulo");
        Label valor = etiqueta(aproximado(p.valor(), moneda), "cifra-proyectada");
        BarraProporcion rango = new BarraProporcion(8, "relleno-acento");
        rango.resaltar((inferior - desde) / (hasta - desde), (superior - desde) / (hasta - desde))
                .marcar((p.valor() - desde) / (hasta - desde));
        Label izquierda = etiqueta(sinAproximado(inferior, moneda), "rango-extremo");
        Label centro = etiqueta("rango probable", "rango-centro");
        Label derecha = etiqueta(sinAproximado(superior, moneda), "rango-extremo");
        centro.setMaxWidth(Double.MAX_VALUE);
        centro.setAlignment(Pos.CENTER);
        HBox.setHgrow(centro, Priority.ALWAYS);
        HBox extremos = new HBox(izquierda, centro, derecha);
        Region linea = new Region();
        linea.getStyleClass().add("separador");
        proximo.getChildren().addAll(titulo, valor, rango, extremos, linea);

        if (mes.equals(YearMonth.from(hoy)) && p.valor() > 0) {
            BigDecimal avance = vendido.divide(BigDecimal.valueOf(p.valor()), 4, RoundingMode.HALF_UP);
            proximo.getChildren().add(filaDato("Llevas vendido",
                    avance.multiply(BigDecimal.valueOf(100)).setScale(0, RoundingMode.HALF_UP) + " % de lo esperado",
                    "dato-valor"));
        }
        Pronostico utilidad = r.pronosticoUtilidad();
        if (utilidad != null && !utilidad.puntos().isEmpty()) {
            double u = utilidad.puntos().get(0).valor();
            proximo.getChildren().add(filaDato(u >= 0 ? "Utilidad esperada" : "Pérdida esperada",
                    aproximado(Math.abs(u), moneda), u >= 0 ? "texto-positivo" : "texto-alerta"));
        }
        Label confiabilidad = etiqueta("Confiabilidad " + ventas.confiabilidad().descripcion().toLowerCase() + " · "
                + ventas.elegida().metodo().nombre().toLowerCase(), "hint");
        confiabilidad.setWrapText(true);
        proximo.getChildren().add(confiabilidad);
    }

    private static HBox filaDato(String clave, String valor, String claseValor) {
        Label lblClave = etiqueta(clave, "dato-clave");
        Region espacio = new Region();
        HBox.setHgrow(espacio, Priority.ALWAYS);
        Label lblValor = etiqueta(valor, "dato-valor");
        lblValor.getStyleClass().add(claseValor);
        lblValor.setMinWidth(Region.USE_PREF_SIZE);
        HBox fila = new HBox(8, lblClave, espacio, lblValor);
        fila.setAlignment(Pos.CENTER_LEFT);
        return fila;
    }

    // ------------------------------------------------------------------ productos

    private void mostrarProductos(AnalisisFinanciero a, Moneda moneda) {
        List<RentabilidadProducto> lista = a.productos().stream()
                .sorted(Comparator.comparing(RentabilidadProducto::margen).reversed())
                .limit(MAX_PRODUCTOS)
                .toList();
        if (lista.isEmpty()) {
            productos.getChildren().setAll(etiqueta("Aún no hay ventas en el mes en curso.", "muted"));
            return;
        }
        double maximo = Math.max(1, lista.get(0).margen().doubleValue());
        GridPane grilla = new GridPane();
        grilla.setHgap(18);
        grilla.setVgap(12);
        ColumnConstraints nombres = new ColumnConstraints();
        nombres.setMinWidth(Region.USE_PREF_SIZE);
        ColumnConstraints barras = new ColumnConstraints();
        barras.setHgrow(Priority.ALWAYS);
        ColumnConstraints valores = new ColumnConstraints();
        valores.setMinWidth(Region.USE_PREF_SIZE);
        valores.setHalignment(javafx.geometry.HPos.RIGHT);
        grilla.getColumnConstraints().addAll(nombres, barras, valores);
        for (int i = 0; i < lista.size(); i++) {
            RentabilidadProducto rp = lista.get(i);
            double margen = rp.margen().doubleValue();
            Label nombre = etiqueta(rp.nombre(), "producto-nombre");
            nombre.setMinWidth(Region.USE_PREF_SIZE);
            BarraProporcion barra = new BarraProporcion(10, i == 0 ? "relleno-acento" : "relleno-neutro");
            barra.llenar(Math.max(0, margen) / maximo);
            Label valor = etiqueta(Montos.formatear(rp.margen().setScale(0, RoundingMode.HALF_UP), moneda)
                    .replaceAll("\\.00$", ""), "producto-valor");
            if (margen < 0) {
                valor.getStyleClass().add("texto-alerta");
            }
            grilla.addRow(i, nombre, barra, valor);
        }
        productos.getChildren().setAll(grilla);
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
        return "≈ " + sinAproximado(valor, moneda);
    }

    private static String sinAproximado(double valor, Moneda moneda) {
        return Montos.formatear(BigDecimal.valueOf(valor).setScale(0, RoundingMode.HALF_UP), moneda)
                .replaceAll("\\.00$", "");
    }

    private static Label etiqueta(String texto, String clase) {
        Label l = new Label(texto);
        l.getStyleClass().add(clase);
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
