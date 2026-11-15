package com.businessanalyzer.analytics.diagnostico;

import com.businessanalyzer.analytics.EstadoResultados;
import com.businessanalyzer.analytics.PuntoEquilibrio;
import com.businessanalyzer.analytics.RentabilidadProducto;
import com.businessanalyzer.analytics.estadistica.EstadisticaDescriptiva;
import com.businessanalyzer.analytics.patrimonio.BalanceGeneral;
import com.businessanalyzer.analytics.patrimonio.IndicadoresPatrimonio;
import com.businessanalyzer.analytics.patrimonio.SituacionPatrimonial;
import com.businessanalyzer.analytics.estadistica.Tendencia;
import com.businessanalyzer.analytics.prediccion.Pronosticador;
import com.businessanalyzer.analytics.prediccion.Pronostico;
import com.businessanalyzer.analytics.simulacion.BaseSimulacion;
import com.businessanalyzer.analytics.simulacion.Simulador;
import com.businessanalyzer.analytics.simulacion.Supuestos;
import com.businessanalyzer.model.Moneda;
import com.businessanalyzer.model.Sector;
import com.businessanalyzer.util.Fechas;
import com.businessanalyzer.util.Montos;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Motor del diagnóstico empresarial: evalúa catorce indicadores con un semáforo, calcula un puntaje
 * de salud de 0 a 100 y redacta una recomendación concreta para cada indicador en amarillo o rojo.
 * <p>
 * Reúne los cálculos de las fases anteriores (estado de resultados, punto de equilibrio,
 * rentabilidad por producto, tendencia, pronóstico y simulación) y no accede a la base de datos.
 * <p>
 * <b>Puntaje:</b> verde = 100, amarillo = 50, rojo = 0, promediados según el peso de cada
 * indicador. Si un indicador clave (peso 3) está en rojo, la calificación no puede ser mejor que
 * «En riesgo», aunque el promedio sea alto.
 */
public final class MotorDiagnostico {

    /** Meses que se revisan para contar meses con pérdida. */
    static final int MESES_REVISION_PERDIDAS = 6;
    /** Meses de historia necesarios para evaluar la tendencia y la regularidad. */
    static final int MINIMO_MESES_SERIE = 6;
    /** Margen de seguridad con el que se considera que el negocio tiene un colchón suficiente. */
    static final BigDecimal SEGURIDAD_OBJETIVO = new BigDecimal("0.20");
    private static final int HORIZONTE_PROYECCION = 3;

    /** Umbrales que dependen del sector (un supermercado vive con márgenes menores que un servicio). */
    record Umbrales(BigDecimal netoVerde, BigDecimal brutoVerde, BigDecimal brutoAmarillo) {

        static Umbrales de(Sector sector) {
            return switch (sector == null ? Sector.OTRO : sector) {
                case COMERCIO, TIENDA -> new Umbrales(bd("0.05"), bd("0.25"), bd("0.15"));
                case RESTAURANTE -> new Umbrales(bd("0.08"), bd("0.55"), bd("0.40"));
                case SERVICIOS -> new Umbrales(bd("0.10"), bd("0.45"), bd("0.30"));
                case PRODUCCION -> new Umbrales(bd("0.07"), bd("0.30"), bd("0.20"));
                case AGROPECUARIO -> new Umbrales(bd("0.08"), bd("0.35"), bd("0.20"));
                default -> new Umbrales(bd("0.06"), bd("0.30"), bd("0.20"));
            };
        }
    }

    private final DatosDiagnostico datos;
    private final Moneda moneda;
    private final EstadoResultados estado;
    private final PuntoEquilibrio equilibrio;
    private final BaseSimulacion base;
    private final Supuestos neutros;
    private final Umbrales umbrales;

    private MotorDiagnostico(DatosDiagnostico datos) {
        this.datos = datos;
        this.moneda = datos.moneda();
        this.estado = EstadoResultados.de(datos.operaciones(), datos.finanzas());
        this.equilibrio = PuntoEquilibrio.de(datos.operaciones(), datos.finanzas());
        this.base = BaseSimulacion.de(datos.periodo(), datos.mesesPeriodo(), datos.operaciones(), datos.finanzas());
        this.neutros = Supuestos.neutros(base.tasaImpuestoEfectiva());
        this.umbrales = Umbrales.de(datos.sector());
    }

    public static Diagnostico evaluar(DatosDiagnostico datos) {
        if (datos.operaciones().ventasNetas().signum() <= 0) {
            return Diagnostico.sinDatos(datos.periodo(), "No hay ventas registradas en " + datos.periodo().descripcion()
                    + ". Registra las ventas, compras y gastos del negocio para obtener el diagnóstico.");
        }
        return new MotorDiagnostico(datos).diagnosticar();
    }

    private Diagnostico diagnosticar() {
        List<Hallazgo> hallazgos = List.of(
                margenNeto(), margenBruto(), mesesConPerdida(),
                margenSeguridad(), pesoCostosFijos(), cargaFinanciera(), liquidez(), endeudamiento(),
                variacionVentas(), tendenciaVentas(), proyeccionVentas(),
                regularidadVentas(), concentracion(), productosConPerdida());

        int puntaje = puntaje(hallazgos);
        NivelSalud nivel = NivelSalud.de(puntaje);
        Optional<Hallazgo> claveEnRojo = hallazgos.stream()
                .filter(h -> h.semaforo() == Semaforo.ROJO && h.tipo().peso() == 3)
                .findFirst();
        boolean limitado = claveEnRojo.isPresent() && nivel.ordinal() < NivelSalud.EN_RIESGO.ordinal();
        if (limitado) {
            nivel = NivelSalud.EN_RIESGO;
        }

        Map<Area, Integer> porArea = new EnumMap<>(Area.class);
        for (Area area : Area.values()) {
            List<Hallazgo> deArea = hallazgos.stream().filter(h -> h.tipo().area() == area).toList();
            int p = puntaje(deArea);
            if (p >= 0) {
                porArea.put(area, p);
            }
        }

        Diagnostico parcial = new Diagnostico(datos.periodo(), nivel, puntaje, hallazgos, porArea, List.of());
        List<String> resumen = new ArrayList<>();
        resumen.add(nivel.descripcion() + ": " + puntaje + " de 100 puntos, con " + parcial.contar(Semaforo.VERDE)
                + " indicadores en verde, " + parcial.contar(Semaforo.AMARILLO) + " en amarillo y "
                + parcial.contar(Semaforo.ROJO) + " en rojo (" + datos.periodo().descripcion() + ").");
        if (limitado) {
            resumen.add("La calificación se limita a «En riesgo» mientras el " + claveEnRojo.get().tipo().nombre().toLowerCase()
                    + " esté en rojo, aunque otros indicadores estén bien.");
        }
        if (porArea.size() >= 2) {
            Area fuerte = porArea.entrySet().stream().max(Map.Entry.comparingByValue()).orElseThrow().getKey();
            Area debil = porArea.entrySet().stream().min(Map.Entry.comparingByValue()).orElseThrow().getKey();
            if (porArea.get(fuerte) > porArea.get(debil)) {
                resumen.add("Su punto más fuerte es " + fuerte.conArticulo() + " y el más débil, " + debil.conArticulo() + ".");
            }
        }
        parcial.recomendaciones().stream().findFirst().ifPresentOrElse(
                h -> resumen.add("Lo más urgente: " + h.tipo().nombre().toLowerCase() + " (" + h.valor() + ")."),
                () -> resumen.add("No se detectaron alertas: mantén el seguimiento mensual."));
        return new Diagnostico(datos.periodo(), nivel, puntaje, hallazgos, porArea, resumen);
    }

    /** Promedio ponderado de los indicadores evaluados; −1 si ninguno pudo evaluarse. */
    static int puntaje(List<Hallazgo> hallazgos) {
        int pesos = 0;
        int suma = 0;
        for (Hallazgo h : hallazgos) {
            if (h.semaforo().evaluado()) {
                pesos += h.tipo().peso();
                suma += h.tipo().peso() * h.semaforo().puntaje();
            }
        }
        return pesos == 0 ? -1 : Math.round((float) suma / pesos);
    }

    // ------------------------------------------------------------------ rentabilidad

    private Hallazgo margenNeto() {
        TipoIndicador tipo = TipoIndicador.MARGEN_NETO;
        BigDecimal margen = estado.margenNeto();
        String criterio = "Bien desde " + pct(umbrales.netoVerde()) + " para " + sectorTexto() + "; riesgo si es negativo.";
        String por100 = margen.movePointRight(2).setScale(1, RoundingMode.HALF_UP).toPlainString();
        if (margen.compareTo(umbrales.netoVerde()) >= 0) {
            return new Hallazgo(tipo, Semaforo.VERDE, pct(margen), criterio,
                    "De cada 100 vendidos quedan " + por100 + " de ganancia neta, un buen nivel para el sector.", null);
        }
        if (margen.signum() >= 0) {
            BigDecimal meta = base.ventas().multiply(umbrales.netoVerde());
            return new Hallazgo(tipo, Semaforo.AMARILLO, pct(margen), criterio,
                    "El negocio gana, pero poco: de cada 100 vendidos quedan " + por100 + ".",
                    "Para llegar a un margen de " + pct(umbrales.netoVerde()) + precioParaMeta(meta)
                            + " Prueba combinaciones en Simulación de escenarios.");
        }
        BigDecimal perdidaMensual = base.utilidadAntesImpuestos().negate().max(BigDecimal.ZERO);
        return new Hallazgo(tipo, Semaforo.ROJO, pct(margen), criterio,
                "El negocio pierde dinero: en promedio " + dinero(estado.utilidadNeta().negate().divide(
                        BigDecimal.valueOf(datos.mesesPeriodo()), 2, RoundingMode.HALF_UP)) + " al mes.",
                "Para dejar de perder" + precioParaMeta(BigDecimal.ZERO)
                        + " Otra vía es recortar costos y gastos en unos " + dinero(perdidaMensual) + " al mes.");
    }

    /** "habría que subir los precios alrededor de +4.0 % (con el mismo volumen)." o una alternativa. */
    private String precioParaMeta(BigDecimal meta) {
        return Simulador.cambioPrecioParaMeta(base, neutros, meta)
                .filter(c -> c.signum() > 0)
                .map(c -> ", habría que subir los precios alrededor de +" + pct(c) + " (vendiendo lo mismo).")
                .orElse(", habría que revisar a fondo precios y costos.");
    }

    private Hallazgo margenBruto() {
        TipoIndicador tipo = TipoIndicador.MARGEN_BRUTO;
        BigDecimal margen = estado.margenBruto();
        String criterio = "Bien desde " + pct(umbrales.brutoVerde()) + "; riesgo por debajo de "
                + pct(umbrales.brutoAmarillo()) + " (" + sectorTexto() + ").";
        String lectura = "De cada 100 vendidos, " + margen.movePointRight(2).setScale(1, RoundingMode.HALF_UP).toPlainString()
                + " quedan para cubrir los gastos del negocio después de pagar lo vendido.";
        if (margen.compareTo(umbrales.brutoVerde()) >= 0) {
            return new Hallazgo(tipo, Semaforo.VERDE, pct(margen), criterio, lectura, null);
        }
        Semaforo semaforo = margen.compareTo(umbrales.brutoAmarillo()) >= 0 ? Semaforo.AMARILLO : Semaforo.ROJO;
        String peor = datos.productos().stream()
                .filter(p -> p.margenSobreVentas() != null)
                .min(Comparator.comparing(RentabilidadProducto::margenSobreVentas))
                .map(p -> " El producto con menor margen es \"" + p.nombre() + "\" (" + pct(p.margenSobreVentas()) + ").")
                .orElse("");
        return new Hallazgo(tipo, semaforo, pct(margen), criterio, lectura,
                "Revisa los precios de venta y negocia mejores costos con tus proveedores." + peor);
    }

    private Hallazgo mesesConPerdida() {
        TipoIndicador tipo = TipoIndicador.MESES_CON_PERDIDA;
        int n = datos.utilidadMensual().length;
        int revisados = Math.min(MESES_REVISION_PERDIDAS, n);
        if (revisados < 3) {
            return Hallazgo.sinDatos(tipo, "Se necesitan al menos 3 meses con ventas.");
        }
        List<String> conPerdida = new ArrayList<>();
        for (int i = n - revisados; i < n; i++) {
            if (datos.utilidadMensual()[i] < 0) {
                conPerdida.add(mesTexto(datos.meses().get(i)));
            }
        }
        String valor = conPerdida.size() + " de " + revisados;
        String criterio = "Bien si no hay ninguno; riesgo con 3 o más.";
        if (conPerdida.isEmpty()) {
            return new Hallazgo(tipo, Semaforo.VERDE, valor, criterio,
                    "El negocio ganó dinero en cada uno de los últimos " + revisados + " meses.", null);
        }
        Semaforo semaforo = conPerdida.size() >= 3 ? Semaforo.ROJO : Semaforo.AMARILLO;
        return new Hallazgo(tipo, semaforo, valor, criterio,
                "Hubo pérdida en " + String.join(", ", conPerdida) + ".",
                "Revisa qué pasó en esos meses (gastos extraordinarios, temporada baja) y separa una reserva "
                        + "de los meses buenos para cubrir los malos.");
    }

    // ------------------------------------------------------------------ solidez

    private Hallazgo margenSeguridad() {
        TipoIndicador tipo = TipoIndicador.MARGEN_SEGURIDAD;
        String criterio = "Bien desde " + pct(SEGURIDAD_OBJETIVO) + "; riesgo por debajo de 5 %.";
        if (equilibrio.ventasEquilibrio() == null) {
            return new Hallazgo(tipo, Semaforo.ROJO, "No alcanzable", criterio,
                    "Lo que se cobra por cada venta no cubre ni su costo variable: vender más no resuelve el problema.",
                    "Sube los precios o reduce el costo de lo que vendes antes de buscar más ventas.");
        }
        BigDecimal margen = equilibrio.margenSeguridad();
        BigDecimal meses = BigDecimal.valueOf(datos.mesesPeriodo());
        String equilibrioMensual = dinero(equilibrio.ventasEquilibrio().divide(meses, 2, RoundingMode.HALF_UP));
        if (margen.compareTo(SEGURIDAD_OBJETIVO) >= 0) {
            return new Hallazgo(tipo, Semaforo.VERDE, pct(margen), criterio,
                    "Las ventas podrían caer hasta " + pct(margen) + " antes de entrar en pérdida (equilibrio: "
                            + equilibrioMensual + " al mes).", null);
        }
        BigDecimal reduccion = reduccionFijosPara(SEGURIDAD_OBJETIVO).divide(meses, 2, RoundingMode.HALF_UP);
        String alternativa = reduccion.signum() > 0
                ? " o reducir los costos fijos en unos " + dinero(reduccion) + " al mes para tener un colchón de "
                + pct(SEGURIDAD_OBJETIVO) + "."
                : ".";
        if (margen.signum() < 0) {
            BigDecimal faltan = equilibrio.ventasFaltantes().divide(meses, 2, RoundingMode.HALF_UP);
            return new Hallazgo(tipo, Semaforo.ROJO, pct(margen), criterio,
                    "Las ventas están por debajo del punto de equilibrio (" + equilibrioMensual + " al mes).",
                    "Hay que vender unos " + dinero(faltan) + " más al mes" + alternativa);
        }
        Semaforo semaforo = margen.compareTo(new BigDecimal("0.05")) >= 0 ? Semaforo.AMARILLO : Semaforo.ROJO;
        return new Hallazgo(tipo, semaforo, pct(margen), criterio,
                "Hay poco colchón: con una caída de ventas de solo " + pct(margen) + " el negocio entraría en pérdida.",
                "Aumenta las ventas" + alternativa);
    }

    /** Reducción total de costos fijos para que el margen de seguridad llegue al objetivo. */
    private BigDecimal reduccionFijosPara(BigDecimal objetivo) {
        BigDecimal fijosPermitidos = equilibrio.ventas().multiply(BigDecimal.ONE.subtract(objetivo))
                .multiply(equilibrio.razonContribucion());
        return equilibrio.costosFijos().subtract(fijosPermitidos).max(BigDecimal.ZERO);
    }

    private Hallazgo pesoCostosFijos() {
        TipoIndicador tipo = TipoIndicador.PESO_COSTOS_FIJOS;
        BigDecimal peso = estado.sobreVentas(datos.finanzas().costosFijosEquilibrio());
        String criterio = "Bien hasta 30 %; riesgo por encima de 45 %.";
        String lectura = "Los costos fijos (alquiler, sueldos, servicios…) equivalen al " + pct(peso) + " de las ventas.";
        if (peso.compareTo(new BigDecimal("0.30")) <= 0) {
            return new Hallazgo(tipo, Semaforo.VERDE, pct(peso), criterio, lectura, null);
        }
        Semaforo semaforo = peso.compareTo(new BigDecimal("0.45")) <= 0 ? Semaforo.AMARILLO : Semaforo.ROJO;
        return new Hallazgo(tipo, semaforo, pct(peso), criterio, lectura,
                "Revisa alquiler, planilla y servicios: cada gasto fijo que reduzcas mejora la utilidad en el mismo monto. "
                        + "También puedes aprovechar mejor la estructura actual vendiendo más con los mismos recursos.");
    }

    private Hallazgo cargaFinanciera() {
        TipoIndicador tipo = TipoIndicador.CARGA_FINANCIERA;
        BigDecimal intereses = estado.gastosFinancieros();
        String criterio = "Bien hasta 20 %; riesgo por encima de 50 % o si la operación no cubre los intereses.";
        if (intereses.signum() == 0) {
            return new Hallazgo(tipo, Semaforo.VERDE, "0 %", criterio, "No hay intereses por deudas registrados.", null);
        }
        String recomendacion = "Evalúa refinanciar a una tasa menor o adelantar pagos de la deuda más cara, "
                + "y evita nuevos préstamos hasta mejorar la utilidad operativa.";
        if (estado.utilidadOperativa().signum() <= 0) {
            return new Hallazgo(tipo, Semaforo.ROJO, "No cubre", criterio,
                    "La operación no genera utilidad suficiente para pagar los intereses (" + dinero(intereses)
                            + " en el período).", recomendacion);
        }
        BigDecimal carga = intereses.divide(estado.utilidadOperativa(), 6, RoundingMode.HALF_UP);
        String lectura = "Los intereses se llevan el " + pct(carga) + " de la utilidad operativa.";
        if (carga.compareTo(new BigDecimal("0.20")) <= 0) {
            return new Hallazgo(tipo, Semaforo.VERDE, pct(carga), criterio, lectura, null);
        }
        Semaforo semaforo = carga.compareTo(new BigDecimal("0.50")) <= 0 ? Semaforo.AMARILLO : Semaforo.ROJO;
        return new Hallazgo(tipo, semaforo, pct(carga), criterio, lectura, recomendacion);
    }

    /** Balance general disponible con al menos un activo registrado; si no, liquidez y endeudamiento no se evalúan. */
    private Optional<SituacionPatrimonial> patrimonio() {
        return Optional.ofNullable(datos.patrimonio()).filter(s -> s.balance().tieneActivos());
    }

    private Hallazgo liquidez() {
        TipoIndicador tipo = TipoIndicador.LIQUIDEZ;
        Optional<SituacionPatrimonial> situacion = patrimonio();
        if (situacion.isEmpty()) {
            return Hallazgo.sinDatos(tipo, "Registra la caja, los cobros pendientes y las deudas en «Activos y deudas» para evaluarla.");
        }
        IndicadoresPatrimonio i = situacion.get().indicadores();
        String criterio = "Bien desde " + IndicadoresPatrimonio.veces(IndicadoresPatrimonio.LIQUIDEZ_VERDE)
                + "; riesgo por debajo de " + IndicadoresPatrimonio.veces(IndicadoresPatrimonio.LIQUIDEZ_AMARILLO) + ".";
        BigDecimal liquidez = i.liquidezCorriente();
        if (liquidez == null) {
            return new Hallazgo(tipo, Semaforo.VERDE, "Sin deudas", criterio,
                    "No hay deudas que venzan en los próximos 12 meses.", null);
        }
        String valor = IndicadoresPatrimonio.veces(liquidez);
        String lectura = "Por cada " + moneda.simbolo() + " 1 que vence en el año hay " + moneda.simbolo() + " " + valor
                + " en caja, cobros pendientes e inventario.";
        if (liquidez.compareTo(IndicadoresPatrimonio.LIQUIDEZ_VERDE) >= 0) {
            return new Hallazgo(tipo, Semaforo.VERDE, valor, criterio, lectura, null);
        }
        if (liquidez.compareTo(IndicadoresPatrimonio.LIQUIDEZ_AMARILLO) >= 0) {
            return new Hallazgo(tipo, Semaforo.AMARILLO, valor, criterio, lectura,
                    "Cobra a tiempo a los clientes, mantén una reserva de caja de al menos un mes de gastos fijos "
                            + "y no pagues bienes de largo plazo con dinero que necesitas en el año.");
        }
        return new Hallazgo(tipo, Semaforo.ROJO, valor, criterio, lectura,
                "Faltan unos " + dinero(i.capitalTrabajo().negate()) + " para cubrir lo que vence en el año. Negocia "
                        + "más plazo con proveedores, refinancia los préstamos a un plazo mayor y acelera los cobros pendientes.");
    }

    private Hallazgo endeudamiento() {
        TipoIndicador tipo = TipoIndicador.ENDEUDAMIENTO;
        Optional<SituacionPatrimonial> situacion = patrimonio();
        if (situacion.isEmpty()) {
            return Hallazgo.sinDatos(tipo, "Registra los activos y las deudas en «Activos y deudas» para evaluarlo.");
        }
        BalanceGeneral b = situacion.get().balance();
        BigDecimal ratio = situacion.get().indicadores().endeudamiento();
        String criterio = "Bien hasta " + pct(IndicadoresPatrimonio.ENDEUDAMIENTO_VERDE) + "; riesgo por encima de "
                + pct(IndicadoresPatrimonio.ENDEUDAMIENTO_AMARILLO) + ".";
        if (b.totalPasivo().signum() == 0) {
            return new Hallazgo(tipo, Semaforo.VERDE, "0 %", criterio, "El negocio no tiene deudas pendientes.", null);
        }
        if (ratio == null || b.patrimonio().signum() <= 0) {
            return new Hallazgo(tipo, Semaforo.ROJO, ratio == null ? "Sin activos" : pct(ratio), criterio,
                    "Las deudas (" + dinero(b.totalPasivo()) + ") superan a los bienes del negocio ("
                            + dinero(b.totalActivo()) + "): el patrimonio es negativo.",
                    "No tomes nuevos préstamos, destina las utilidades a pagar primero las deudas más caras "
                            + "y evalúa un aporte de capital de los dueños.");
        }
        String lectura = "Las deudas financian el " + pct(ratio) + " de los bienes del negocio; el resto es patrimonio "
                + "de los dueños (" + dinero(b.patrimonio()) + ").";
        if (ratio.compareTo(IndicadoresPatrimonio.ENDEUDAMIENTO_VERDE) <= 0) {
            return new Hallazgo(tipo, Semaforo.VERDE, pct(ratio), criterio, lectura, null);
        }
        Semaforo semaforo = ratio.compareTo(IndicadoresPatrimonio.ENDEUDAMIENTO_AMARILLO) <= 0
                ? Semaforo.AMARILLO : Semaforo.ROJO;
        BigDecimal exceso = b.totalPasivo().subtract(b.totalActivo().multiply(IndicadoresPatrimonio.ENDEUDAMIENTO_VERDE));
        return new Hallazgo(tipo, semaforo, pct(ratio), criterio, lectura,
                "Para bajar al " + pct(IndicadoresPatrimonio.ENDEUDAMIENTO_VERDE) + " habría que reducir las deudas en unos "
                        + dinero(exceso.max(BigDecimal.ZERO)) + ". Evita nuevos préstamos y amortiza primero los de mayor tasa.");
    }

    // ------------------------------------------------------------------ crecimiento

    private Hallazgo variacionVentas() {
        TipoIndicador tipo = TipoIndicador.VARIACION_VENTAS;
        BigDecimal anterior = datos.operacionesAnteriores().ventasNetas();
        if (anterior.signum() <= 0) {
            return Hallazgo.sinDatos(tipo, "No hay ventas en " + datos.periodo().anterior().descripcion() + " para comparar.");
        }
        BigDecimal cambio = estado.ventasNetas().subtract(anterior).divide(anterior, 6, RoundingMode.HALF_UP);
        String valor = signo(cambio) + pct(cambio);
        String criterio = "Bien si no caen más de 2 %; riesgo si caen más de 10 %.";
        String lectura = "Las ventas " + (cambio.signum() >= 0 ? "subieron " : "bajaron ") + pct(cambio.abs())
                + " frente a " + datos.periodo().anterior().descripcion() + ".";
        if (cambio.compareTo(new BigDecimal("-0.02")) >= 0) {
            return new Hallazgo(tipo, Semaforo.VERDE, valor, criterio, lectura, null);
        }
        Semaforo semaforo = cambio.compareTo(new BigDecimal("-0.10")) >= 0 ? Semaforo.AMARILLO : Semaforo.ROJO;
        return new Hallazgo(tipo, semaforo, valor, criterio, lectura,
                "Averigua qué productos bajaron (Análisis estadístico → ABC) y si la caída es de temporada; "
                        + "refuerza la promoción de los productos que más venden.");
    }

    private Hallazgo tendenciaVentas() {
        TipoIndicador tipo = TipoIndicador.TENDENCIA_VENTAS;
        double[] ventas = ultimos(datos.ventasMensuales(), 12);
        if (ventas.length < MINIMO_MESES_SERIE) {
            return Hallazgo.sinDatos(tipo, "Se necesitan al menos " + MINIMO_MESES_SERIE + " meses con ventas.");
        }
        Tendencia tendencia = Tendencia.de(ventas);
        double crecimiento = tendencia.crecimientoMensual();
        String valor = (crecimiento >= 0 ? "+" : "") + pct(crecimiento) + " al mes";
        String criterio = "Riesgo si caen de forma sostenida (tendencia significativa con p < 0.05).";
        String recomendacion = "Las ventas vienen bajando: revisa precios frente a la competencia, la atención y la "
                + "publicidad, y apóyate en Predicciones para anticipar los próximos meses.";
        return switch (tendencia.direccion()) {
            case CRECIENTE -> new Hallazgo(tipo, Semaforo.VERDE, valor, criterio,
                    "Las ventas crecen de forma sostenida, alrededor de " + pct(crecimiento) + " al mes.", null);
            case DECRECIENTE -> new Hallazgo(tipo, Semaforo.ROJO, valor, criterio,
                    "Las ventas caen de forma sostenida, alrededor de " + pct(Math.abs(crecimiento)) + " al mes.",
                    recomendacion);
            default -> crecimiento < -0.01
                    ? new Hallazgo(tipo, Semaforo.AMARILLO, valor, criterio,
                    "Las ventas tienden a bajar, aunque la tendencia todavía no es clara.", recomendacion)
                    : new Hallazgo(tipo, Semaforo.VERDE, valor, criterio,
                    "Las ventas se mantienen estables, sin una tendencia clara al alza ni a la baja.", null);
        };
    }

    private Hallazgo proyeccionVentas() {
        TipoIndicador tipo = TipoIndicador.PROYECCION_VENTAS;
        double[] ventas = datos.ventasMensuales();
        Pronostico pronostico = Pronosticador.pronosticar("Ventas netas", ventas, HORIZONTE_PROYECCION, true);
        if (!pronostico.suficiente() || ventas.length < HORIZONTE_PROYECCION) {
            return Hallazgo.sinDatos(tipo, "Se necesitan al menos " + Pronosticador.MINIMO_DATOS + " meses con ventas.");
        }
        double recientes = Arrays.stream(ultimos(ventas, HORIZONTE_PROYECCION)).sum();
        if (recientes <= 0) {
            return Hallazgo.sinDatos(tipo, "No hay ventas en los últimos meses para comparar.");
        }
        double cambio = pronostico.total() / recientes - 1;
        String valor = (cambio >= 0 ? "+" : "") + pct(cambio);
        String criterio = "Bien si no se proyecta una caída mayor a 2 %; riesgo si es mayor a 10 %.";
        String lectura = "Para los próximos " + HORIZONTE_PROYECCION + " meses se proyectan ventas de "
                + dinero(BigDecimal.valueOf(pronostico.total())) + " (confiabilidad "
                + pronostico.confiabilidad().descripcion().toLowerCase() + ").";
        if (cambio >= -0.02) {
            return new Hallazgo(tipo, Semaforo.VERDE, valor, criterio, lectura, null);
        }
        return new Hallazgo(tipo, cambio >= -0.10 ? Semaforo.AMARILLO : Semaforo.ROJO, valor, criterio, lectura,
                "Prepárate para meses más flojos: ajusta compras e inventario a la venta esperada y planifica "
                        + "promociones con anticipación.");
    }

    // ------------------------------------------------------------------ riesgo comercial

    private Hallazgo regularidadVentas() {
        TipoIndicador tipo = TipoIndicador.REGULARIDAD_VENTAS;
        double[] ventas = ultimos(datos.ventasMensuales(), 12);
        if (ventas.length < MINIMO_MESES_SERIE) {
            return Hallazgo.sinDatos(tipo, "Se necesitan al menos " + MINIMO_MESES_SERIE + " meses con ventas.");
        }
        double cv = EstadisticaDescriptiva.de(ventas).coeficienteVariacion();
        if (Double.isNaN(cv)) {
            return Hallazgo.sinDatos(tipo, "No se pudo calcular la variación de las ventas.");
        }
        String criterio = "Bien hasta 15 %; riesgo por encima de 30 %.";
        String lectura = "Las ventas mensuales se alejan en promedio un " + pct(cv) + " de su valor medio.";
        if (cv <= 0.15) {
            return new Hallazgo(tipo, Semaforo.VERDE, pct(cv), criterio, lectura + " Son bastante predecibles.", null);
        }
        return new Hallazgo(tipo, cv <= 0.30 ? Semaforo.AMARILLO : Semaforo.ROJO, pct(cv), criterio, lectura,
                "Planifica compras y caja según los meses fuertes y débiles, y usa promociones para sostener los meses bajos.");
    }

    private Hallazgo concentracion() {
        TipoIndicador tipo = TipoIndicador.CONCENTRACION;
        BigDecimal total = datos.productos().stream().map(RentabilidadProducto::ventas).reduce(BigDecimal.ZERO, BigDecimal::add);
        if (total.signum() <= 0) {
            return Hallazgo.sinDatos(tipo, "No hay ventas por producto en el período.");
        }
        RentabilidadProducto principal = datos.productos().stream()
                .max(Comparator.comparing(RentabilidadProducto::ventas)).orElseThrow();
        BigDecimal participacion = principal.ventas().divide(total, 6, RoundingMode.HALF_UP);
        String criterio = "Bien hasta 40 %; riesgo por encima de 60 %.";
        String lectura = "\"" + principal.nombre() + "\" representa el " + pct(participacion) + " de las ventas.";
        if (participacion.compareTo(new BigDecimal("0.40")) <= 0) {
            return new Hallazgo(tipo, Semaforo.VERDE, pct(participacion), criterio, lectura, null);
        }
        Semaforo semaforo = participacion.compareTo(new BigDecimal("0.60")) <= 0 ? Semaforo.AMARILLO : Semaforo.ROJO;
        return new Hallazgo(tipo, semaforo, pct(participacion), criterio, lectura,
                "Impulsa otros productos o servicios para no depender tanto de \"" + principal.nombre()
                        + "\": un problema con él (proveedor, precio, competencia) afectaría a todo el negocio.");
    }

    private Hallazgo productosConPerdida() {
        TipoIndicador tipo = TipoIndicador.PRODUCTOS_CON_PERDIDA;
        BigDecimal total = datos.productos().stream().map(RentabilidadProducto::ventas).reduce(BigDecimal.ZERO, BigDecimal::add);
        if (total.signum() <= 0) {
            return Hallazgo.sinDatos(tipo, "No hay ventas por producto en el período.");
        }
        List<RentabilidadProducto> conPerdida = datos.productos().stream()
                .filter(p -> p.margen().signum() < 0)
                .sorted(Comparator.comparing(RentabilidadProducto::margen))
                .toList();
        String criterio = "Bien si no hay ninguno; riesgo si superan el 5 % de las ventas.";
        if (conPerdida.isEmpty()) {
            return new Hallazgo(tipo, Semaforo.VERDE, "0 %", criterio,
                    "Todos los productos se venden por encima de su costo.", null);
        }
        BigDecimal ventas = conPerdida.stream().map(RentabilidadProducto::ventas).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal perdida = conPerdida.stream().map(RentabilidadProducto::margen).reduce(BigDecimal.ZERO, BigDecimal::add).negate();
        BigDecimal participacion = ventas.divide(total, 6, RoundingMode.HALF_UP);
        List<String> nombres = conPerdida.stream().limit(3).map(p -> "\"" + p.nombre() + "\"").toList();
        String lista = String.join(", ", nombres) + (conPerdida.size() > 3 ? " y " + (conPerdida.size() - 3) + " más" : "");
        Semaforo semaforo = participacion.compareTo(new BigDecimal("0.05")) <= 0 ? Semaforo.AMARILLO : Semaforo.ROJO;
        return new Hallazgo(tipo, semaforo, pct(participacion), criterio,
                (conPerdida.size() == 1 ? "1 producto se vende" : conPerdida.size() + " productos se venden")
                        + " por debajo de su costo: " + lista + " (pérdida de " + dinero(perdida) + " en el período).",
                "Sube el precio de " + (conPerdida.size() == 1 ? "ese producto" : "esos productos")
                        + " por encima de su costo o renegocia su compra; si los usas como gancho, verifica que "
                        + "realmente atraigan otras ventas.");
    }

    // ------------------------------------------------------------------ utilidades

    private static double[] ultimos(double[] valores, int cantidad) {
        return Arrays.copyOfRange(valores, Math.max(0, valores.length - cantidad), valores.length);
    }

    private String sectorTexto() {
        Sector sector = datos.sector() == null ? Sector.OTRO : datos.sector();
        return sector == Sector.OTRO ? "un negocio en general" : "el sector " + sector.descripcion().toLowerCase();
    }

    private String dinero(BigDecimal valor) {
        return Montos.formatear(valor.setScale(2, RoundingMode.HALF_UP), moneda);
    }

    private static String pct(BigDecimal fraccion) {
        return Montos.porcentaje(fraccion);
    }

    private static String pct(double fraccion) {
        return Montos.porcentaje(BigDecimal.valueOf(fraccion));
    }

    private static String signo(BigDecimal valor) {
        return valor.signum() > 0 ? "+" : "";
    }

    private static String mesTexto(YearMonth mes) {
        return Fechas.nombreMes(mes.getMonthValue()).toLowerCase() + " " + mes.getYear();
    }

    private static BigDecimal bd(String valor) {
        return new BigDecimal(valor);
    }
}
