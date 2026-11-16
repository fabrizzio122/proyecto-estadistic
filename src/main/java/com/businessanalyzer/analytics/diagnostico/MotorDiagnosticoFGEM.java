package com.businessanalyzer.analytics.diagnostico;

import com.businessanalyzer.analytics.EstadoResultadosFGEM;
import com.businessanalyzer.analytics.PuntoEquilibrioFGEM;
import com.businessanalyzer.analytics.RentabilidadProductoFGEM;
import com.businessanalyzer.analytics.agricultura.ResultadoCampaniaFGEM;
import com.businessanalyzer.analytics.estadistica.EstadisticaDescriptivaFGEM;
import com.businessanalyzer.analytics.ganaderia.IndicadoresEspecieFGEM;
import com.businessanalyzer.analytics.patrimonio.BalanceGeneralFGEM;
import com.businessanalyzer.analytics.patrimonio.IndicadoresPatrimonioFGEM;
import com.businessanalyzer.analytics.patrimonio.SituacionPatrimonialFGEM;
import com.businessanalyzer.analytics.estadistica.TendenciaFGEM;
import com.businessanalyzer.analytics.prediccion.PronosticadorFGEM;
import com.businessanalyzer.analytics.prediccion.PronosticoFGEM;
import com.businessanalyzer.analytics.simulacion.BaseSimulacionFGEM;
import com.businessanalyzer.analytics.simulacion.SimuladorFGEM;
import com.businessanalyzer.analytics.simulacion.SupuestosFGEM;
import com.businessanalyzer.model.MonedaFGEM;
import com.businessanalyzer.model.SectorFGEM;
import com.businessanalyzer.analytics.tesoreria.ProyeccionCajaFGEM;
import com.businessanalyzer.util.FechasFGEM;
import com.businessanalyzer.util.MontosFGEM;

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
public final class MotorDiagnosticoFGEM {

    /** Meses que se revisan para contar meses con pérdida. */
    static final int MESES_REVISION_PERDIDAS_FGEM = 6;
    /** Meses de historia necesarios para evaluar la tendencia y la regularidad. */
    static final int MINIMO_MESES_SERIE_FGEM = 6;
    /** Margen de seguridad con el que se considera que el negocio tiene un colchón suficiente. */
    static final BigDecimal SEGURIDAD_OBJETIVO_FGEM = new BigDecimal("0.20");
    private static final int HORIZONTE_PROYECCION_FGEM = 3;

    /** Umbrales que dependen del sector (un supermercado vive con márgenes menores que un servicio). */
    record UmbralesFGEM(BigDecimal netoVerdeFGEM, BigDecimal brutoVerdeFGEM, BigDecimal brutoAmarilloFGEM) {

        static UmbralesFGEM deFGEM(SectorFGEM sectorFGEM) {
            return switch (sectorFGEM == null ? SectorFGEM.OTRO : sectorFGEM) {
                case COMERCIO, TIENDA -> new UmbralesFGEM(bdFGEM("0.05"), bdFGEM("0.25"), bdFGEM("0.15"));
                case RESTAURANTE -> new UmbralesFGEM(bdFGEM("0.08"), bdFGEM("0.55"), bdFGEM("0.40"));
                case SERVICIOS -> new UmbralesFGEM(bdFGEM("0.10"), bdFGEM("0.45"), bdFGEM("0.30"));
                case PRODUCCION -> new UmbralesFGEM(bdFGEM("0.07"), bdFGEM("0.30"), bdFGEM("0.20"));
                case AGROPECUARIO -> new UmbralesFGEM(bdFGEM("0.08"), bdFGEM("0.35"), bdFGEM("0.20"));
                default -> new UmbralesFGEM(bdFGEM("0.06"), bdFGEM("0.30"), bdFGEM("0.20"));
            };
        }
    }

    private final DatosDiagnosticoFGEM datosFGEM;
    private final MonedaFGEM monedaFGEM;
    private final EstadoResultadosFGEM estadoFGEM;
    private final PuntoEquilibrioFGEM equilibrioFGEM;
    private final BaseSimulacionFGEM baseFGEM;
    private final SupuestosFGEM neutrosFGEM;
    private final UmbralesFGEM umbralesFGEM;

    private MotorDiagnosticoFGEM(DatosDiagnosticoFGEM datosFGEM) {
        this.datosFGEM = datosFGEM;
        this.monedaFGEM = datosFGEM.monedaFGEM();
        this.estadoFGEM = EstadoResultadosFGEM.deFGEM(datosFGEM.operacionesFGEM(), datosFGEM.finanzasFGEM());
        this.equilibrioFGEM = PuntoEquilibrioFGEM.deFGEM(datosFGEM.operacionesFGEM(), datosFGEM.finanzasFGEM());
        this.baseFGEM = BaseSimulacionFGEM.deFGEM(datosFGEM.periodoFGEM(), datosFGEM.mesesPeriodoFGEM(), datosFGEM.operacionesFGEM(), datosFGEM.finanzasFGEM());
        this.neutrosFGEM = SupuestosFGEM.neutrosFGEM(baseFGEM.tasaImpuestoEfectivaFGEM());
        this.umbralesFGEM = UmbralesFGEM.deFGEM(datosFGEM.sectorFGEM());
    }

    public static DiagnosticoFGEM evaluarFGEM(DatosDiagnosticoFGEM datosFGEM) {
        if (datosFGEM.operacionesFGEM().ventasNetasFGEM().signum() <= 0) {
            return DiagnosticoFGEM.sinDatosFGEM(datosFGEM.periodoFGEM(), "No hay ventas registradas en " + datosFGEM.periodoFGEM().descripcionFGEM()
                    + ". Registra las ventas, compras y gastos del negocio para obtener el diagnóstico.");
        }
        return new MotorDiagnosticoFGEM(datosFGEM).diagnosticarFGEM();
    }

    private DiagnosticoFGEM diagnosticarFGEM() {
        List<HallazgoFGEM> hallazgosFGEM = new ArrayList<>(List.of(
                margenNetoFGEM(), margenBrutoFGEM(), mesesConPerdidaFGEM(),
                margenSeguridadFGEM(), pesoCostosFijosFGEM(), cargaFinancieraFGEM(), liquidezFGEM(), cajaProyectadaFGEM(), endeudamientoFGEM(),
                variacionVentasFGEM(), tendenciaVentasFGEM(), proyeccionVentasFGEM(),
                regularidadVentasFGEM(), concentracionFGEM(), productosConPerdidaFGEM()));
        if (datosFGEM.agroFGEM() != null) {
            hallazgosFGEM.addAll(List.of(natalidadFGEM(datosFGEM.agroFGEM()), mortalidadCriasFGEM(datosFGEM.agroFGEM()),
                    rendimientoCultivosFGEM(datosFGEM.agroFGEM()), compromisosFGEM(datosFGEM.agroFGEM())));
        }

        int puntajeFGEM = puntajeFGEM(hallazgosFGEM);
        NivelSaludFGEM nivelFGEM = NivelSaludFGEM.deFGEM(puntajeFGEM);
        Optional<HallazgoFGEM> claveEnRojoFGEM = hallazgosFGEM.stream()
                .filter(hFGEM -> hFGEM.semaforoFGEM() == SemaforoFGEM.ROJO && hFGEM.tipoFGEM().pesoFGEM() == 3)
                .findFirst();
        boolean limitadoFGEM = claveEnRojoFGEM.isPresent() && nivelFGEM.ordinal() < NivelSaludFGEM.EN_RIESGO.ordinal();
        if (limitadoFGEM) {
            nivelFGEM = NivelSaludFGEM.EN_RIESGO;
        }

        Map<AreaFGEM, Integer> porAreaFGEM = new EnumMap<>(AreaFGEM.class);
        for (AreaFGEM areaFGEM : AreaFGEM.values()) {
            List<HallazgoFGEM> deAreaFGEM = hallazgosFGEM.stream().filter(hFGEM -> hFGEM.tipoFGEM().areaFGEM() == areaFGEM).toList();
            int pFGEM = puntajeFGEM(deAreaFGEM);
            if (pFGEM >= 0) {
                porAreaFGEM.put(areaFGEM, pFGEM);
            }
        }

        DiagnosticoFGEM parcialFGEM = new DiagnosticoFGEM(datosFGEM.periodoFGEM(), nivelFGEM, puntajeFGEM, hallazgosFGEM, porAreaFGEM, List.of());
        List<String> resumenFGEM = new ArrayList<>();
        resumenFGEM.add(nivelFGEM.descripcionFGEM() + ": " + puntajeFGEM + " de 100 puntos, con " + parcialFGEM.contarFGEM(SemaforoFGEM.VERDE)
                + " indicadores en verde, " + parcialFGEM.contarFGEM(SemaforoFGEM.AMARILLO) + " en amarillo y "
                + parcialFGEM.contarFGEM(SemaforoFGEM.ROJO) + " en rojo (" + datosFGEM.periodoFGEM().descripcionFGEM() + ").");
        if (limitadoFGEM) {
            resumenFGEM.add("La calificación se limita a «En riesgo» mientras el " + claveEnRojoFGEM.get().tipoFGEM().nombreFGEM().toLowerCase()
                    + " esté en rojo, aunque otros indicadores estén bien.");
        }
        if (porAreaFGEM.size() >= 2) {
            AreaFGEM fuerteFGEM = porAreaFGEM.entrySet().stream().max(Map.Entry.comparingByValue()).orElseThrow().getKey();
            AreaFGEM debilFGEM = porAreaFGEM.entrySet().stream().min(Map.Entry.comparingByValue()).orElseThrow().getKey();
            if (porAreaFGEM.get(fuerteFGEM) > porAreaFGEM.get(debilFGEM)) {
                resumenFGEM.add("Su punto más fuerte es " + fuerteFGEM.conArticuloFGEM() + " y el más débil, " + debilFGEM.conArticuloFGEM() + ".");
            }
        }
        parcialFGEM.recomendacionesFGEM().stream().findFirst().ifPresentOrElse(
                hFGEM -> resumenFGEM.add("Lo más urgente: " + hFGEM.tipoFGEM().nombreFGEM().toLowerCase() + " (" + hFGEM.valorFGEM() + ")."),
                () -> resumenFGEM.add("No se detectaron alertas: mantén el seguimiento mensual."));
        return new DiagnosticoFGEM(datosFGEM.periodoFGEM(), nivelFGEM, puntajeFGEM, hallazgosFGEM, porAreaFGEM, resumenFGEM);
    }

    /** Promedio ponderado de los indicadores evaluados; −1 si ninguno pudo evaluarse. */
    static int puntajeFGEM(List<HallazgoFGEM> hallazgosFGEM) {
        int pesosFGEM = 0;
        int sumaFGEM = 0;
        for (HallazgoFGEM hFGEM : hallazgosFGEM) {
            if (hFGEM.semaforoFGEM().evaluadoFGEM()) {
                pesosFGEM += hFGEM.tipoFGEM().pesoFGEM();
                sumaFGEM += hFGEM.tipoFGEM().pesoFGEM() * hFGEM.semaforoFGEM().puntajeFGEM();
            }
        }
        return pesosFGEM == 0 ? -1 : Math.round((float) sumaFGEM / pesosFGEM);
    }

    // ------------------------------------------------------------------ rentabilidad

    private HallazgoFGEM margenNetoFGEM() {
        TipoIndicadorFGEM tipoFGEM = TipoIndicadorFGEM.MARGEN_NETO;
        BigDecimal margenFGEM = estadoFGEM.margenNetoFGEM();
        String criterioFGEM = "Bien desde " + pctFGEM(umbralesFGEM.netoVerdeFGEM()) + " para " + sectorTextoFGEM() + "; riesgo si es negativo.";
        String por100FGEM = margenFGEM.movePointRight(2).setScale(1, RoundingMode.HALF_UP).toPlainString();
        if (margenFGEM.compareTo(umbralesFGEM.netoVerdeFGEM()) >= 0) {
            return new HallazgoFGEM(tipoFGEM, SemaforoFGEM.VERDE, pctFGEM(margenFGEM), criterioFGEM,
                    "De cada 100 vendidos quedan " + por100FGEM + " de ganancia neta, un buen nivel para el sector.", null);
        }
        if (margenFGEM.signum() >= 0) {
            BigDecimal metaFGEM = baseFGEM.ventasFGEM().multiply(umbralesFGEM.netoVerdeFGEM());
            return new HallazgoFGEM(tipoFGEM, SemaforoFGEM.AMARILLO, pctFGEM(margenFGEM), criterioFGEM,
                    "El negocio gana, pero poco: de cada 100 vendidos quedan " + por100FGEM + ".",
                    "Para llegar a un margen de " + pctFGEM(umbralesFGEM.netoVerdeFGEM()) + precioParaMetaFGEM(metaFGEM)
                            + " Prueba combinaciones en Simulación de escenarios.");
        }
        BigDecimal perdidaMensualFGEM = baseFGEM.utilidadAntesImpuestosFGEM().negate().max(BigDecimal.ZERO);
        return new HallazgoFGEM(tipoFGEM, SemaforoFGEM.ROJO, pctFGEM(margenFGEM), criterioFGEM,
                "El negocio pierde dinero: en promedio " + dineroFGEM(estadoFGEM.utilidadNetaFGEM().negate().divide(
                        BigDecimal.valueOf(datosFGEM.mesesPeriodoFGEM()), 2, RoundingMode.HALF_UP)) + " al mes.",
                "Para dejar de perder" + precioParaMetaFGEM(BigDecimal.ZERO)
                        + " Otra vía es recortar costos y gastos en unos " + dineroFGEM(perdidaMensualFGEM) + " al mes.");
    }

    /** "habría que subir los precios alrededor de +4.0 % (con el mismo volumen)." o una alternativa. */
    private String precioParaMetaFGEM(BigDecimal metaFGEM) {
        return SimuladorFGEM.cambioPrecioParaMetaFGEM(baseFGEM, neutrosFGEM, metaFGEM)
                .filter(cFGEM -> cFGEM.signum() > 0)
                .map(cFGEM -> ", habría que subir los precios alrededor de +" + pctFGEM(cFGEM) + " (vendiendo lo mismo).")
                .orElse(", habría que revisar a fondo precios y costos.");
    }

    private HallazgoFGEM margenBrutoFGEM() {
        TipoIndicadorFGEM tipoFGEM = TipoIndicadorFGEM.MARGEN_BRUTO;
        BigDecimal margenFGEM = estadoFGEM.margenBrutoFGEM();
        String criterioFGEM = "Bien desde " + pctFGEM(umbralesFGEM.brutoVerdeFGEM()) + "; riesgo por debajo de "
                + pctFGEM(umbralesFGEM.brutoAmarilloFGEM()) + " (" + sectorTextoFGEM() + ").";
        String lecturaFGEM = "De cada 100 vendidos, " + margenFGEM.movePointRight(2).setScale(1, RoundingMode.HALF_UP).toPlainString()
                + " quedan para cubrir los gastos del negocio después de pagar lo vendido.";
        if (margenFGEM.compareTo(umbralesFGEM.brutoVerdeFGEM()) >= 0) {
            return new HallazgoFGEM(tipoFGEM, SemaforoFGEM.VERDE, pctFGEM(margenFGEM), criterioFGEM, lecturaFGEM, null);
        }
        SemaforoFGEM semaforoFGEM = margenFGEM.compareTo(umbralesFGEM.brutoAmarilloFGEM()) >= 0 ? SemaforoFGEM.AMARILLO : SemaforoFGEM.ROJO;
        String peorFGEM = datosFGEM.productosFGEM().stream()
                .filter(pFGEM -> pFGEM.margenSobreVentasFGEM() != null)
                .min(Comparator.comparing(RentabilidadProductoFGEM::margenSobreVentasFGEM))
                .map(pFGEM -> " El producto con menor margen es \"" + pFGEM.nombreFGEM() + "\" (" + pctFGEM(pFGEM.margenSobreVentasFGEM()) + ").")
                .orElse("");
        return new HallazgoFGEM(tipoFGEM, semaforoFGEM, pctFGEM(margenFGEM), criterioFGEM, lecturaFGEM,
                "Revisa los precios de venta y negocia mejores costos con tus proveedores." + peorFGEM);
    }

    private HallazgoFGEM mesesConPerdidaFGEM() {
        TipoIndicadorFGEM tipoFGEM = TipoIndicadorFGEM.MESES_CON_PERDIDA;
        int nFGEM = datosFGEM.utilidadMensualFGEM().length;
        int revisadosFGEM = Math.min(MESES_REVISION_PERDIDAS_FGEM, nFGEM);
        if (revisadosFGEM < 3) {
            return HallazgoFGEM.sinDatosFGEM(tipoFGEM, "Se necesitan al menos 3 meses con ventas.");
        }
        List<String> conPerdidaFGEM = new ArrayList<>();
        for (int iFGEM = nFGEM - revisadosFGEM; iFGEM < nFGEM; iFGEM++) {
            if (datosFGEM.utilidadMensualFGEM()[iFGEM] < 0) {
                conPerdidaFGEM.add(mesTextoFGEM(datosFGEM.mesesFGEM().get(iFGEM)));
            }
        }
        String valorFGEM = conPerdidaFGEM.size() + " de " + revisadosFGEM;
        String criterioFGEM = "Bien si no hay ninguno; riesgo con 3 o más.";
        if (conPerdidaFGEM.isEmpty()) {
            return new HallazgoFGEM(tipoFGEM, SemaforoFGEM.VERDE, valorFGEM, criterioFGEM,
                    "El negocio ganó dinero en cada uno de los últimos " + revisadosFGEM + " meses.", null);
        }
        SemaforoFGEM semaforoFGEM = conPerdidaFGEM.size() >= 3 ? SemaforoFGEM.ROJO : SemaforoFGEM.AMARILLO;
        return new HallazgoFGEM(tipoFGEM, semaforoFGEM, valorFGEM, criterioFGEM,
                "Hubo pérdida en " + String.join(", ", conPerdidaFGEM) + ".",
                "Revisa qué pasó en esos meses (gastos extraordinarios, temporada baja) y separa una reserva "
                        + "de los meses buenos para cubrir los malos.");
    }

    // ------------------------------------------------------------------ solidez

    private HallazgoFGEM margenSeguridadFGEM() {
        TipoIndicadorFGEM tipoFGEM = TipoIndicadorFGEM.MARGEN_SEGURIDAD;
        String criterioFGEM = "Bien desde " + pctFGEM(SEGURIDAD_OBJETIVO_FGEM) + "; riesgo por debajo de 5 %.";
        if (equilibrioFGEM.ventasEquilibrioFGEM() == null) {
            return new HallazgoFGEM(tipoFGEM, SemaforoFGEM.ROJO, "No alcanzable", criterioFGEM,
                    "Lo que se cobra por cada venta no cubre ni su costo variable: vender más no resuelve el problema.",
                    "Sube los precios o reduce el costo de lo que vendes antes de buscar más ventas.");
        }
        BigDecimal margenFGEM = equilibrioFGEM.margenSeguridadFGEM();
        BigDecimal mesesFGEM = BigDecimal.valueOf(datosFGEM.mesesPeriodoFGEM());
        String equilibrioMensualFGEM = dineroFGEM(equilibrioFGEM.ventasEquilibrioFGEM().divide(mesesFGEM, 2, RoundingMode.HALF_UP));
        if (margenFGEM.compareTo(SEGURIDAD_OBJETIVO_FGEM) >= 0) {
            return new HallazgoFGEM(tipoFGEM, SemaforoFGEM.VERDE, pctFGEM(margenFGEM), criterioFGEM,
                    "Las ventas podrían caer hasta " + pctFGEM(margenFGEM) + " antes de entrar en pérdida (equilibrio: "
                            + equilibrioMensualFGEM + " al mes).", null);
        }
        BigDecimal reduccionFGEM = reduccionFijosParaFGEM(SEGURIDAD_OBJETIVO_FGEM).divide(mesesFGEM, 2, RoundingMode.HALF_UP);
        String alternativaFGEM = reduccionFGEM.signum() > 0
                ? " o reducir los costos fijos en unos " + dineroFGEM(reduccionFGEM) + " al mes para tener un colchón de "
                + pctFGEM(SEGURIDAD_OBJETIVO_FGEM) + "."
                : ".";
        if (margenFGEM.signum() < 0) {
            BigDecimal faltanFGEM = equilibrioFGEM.ventasFaltantesFGEM().divide(mesesFGEM, 2, RoundingMode.HALF_UP);
            return new HallazgoFGEM(tipoFGEM, SemaforoFGEM.ROJO, pctFGEM(margenFGEM), criterioFGEM,
                    "Las ventas están por debajo del punto de equilibrio (" + equilibrioMensualFGEM + " al mes).",
                    "Hay que vender unos " + dineroFGEM(faltanFGEM) + " más al mes" + alternativaFGEM);
        }
        SemaforoFGEM semaforoFGEM = margenFGEM.compareTo(new BigDecimal("0.05")) >= 0 ? SemaforoFGEM.AMARILLO : SemaforoFGEM.ROJO;
        return new HallazgoFGEM(tipoFGEM, semaforoFGEM, pctFGEM(margenFGEM), criterioFGEM,
                "Hay poco colchón: con una caída de ventas de solo " + pctFGEM(margenFGEM) + " el negocio entraría en pérdida.",
                "Aumenta las ventas" + alternativaFGEM);
    }

    /** Reducción total de costos fijos para que el margen de seguridad llegue al objetivo. */
    private BigDecimal reduccionFijosParaFGEM(BigDecimal objetivoFGEM) {
        BigDecimal fijosPermitidosFGEM = equilibrioFGEM.ventasFGEM().multiply(BigDecimal.ONE.subtract(objetivoFGEM))
                .multiply(equilibrioFGEM.razonContribucionFGEM());
        return equilibrioFGEM.costosFijosFGEM().subtract(fijosPermitidosFGEM).max(BigDecimal.ZERO);
    }

    private HallazgoFGEM pesoCostosFijosFGEM() {
        TipoIndicadorFGEM tipoFGEM = TipoIndicadorFGEM.PESO_COSTOS_FIJOS;
        BigDecimal pesoFGEM = estadoFGEM.sobreVentasFGEM(datosFGEM.finanzasFGEM().costosFijosEquilibrioFGEM());
        String criterioFGEM = "Bien hasta 30 %; riesgo por encima de 45 %.";
        String lecturaFGEM = "Los costos fijos (alquiler, sueldos, servicios…) equivalen al " + pctFGEM(pesoFGEM) + " de las ventas.";
        if (pesoFGEM.compareTo(new BigDecimal("0.30")) <= 0) {
            return new HallazgoFGEM(tipoFGEM, SemaforoFGEM.VERDE, pctFGEM(pesoFGEM), criterioFGEM, lecturaFGEM, null);
        }
        SemaforoFGEM semaforoFGEM = pesoFGEM.compareTo(new BigDecimal("0.45")) <= 0 ? SemaforoFGEM.AMARILLO : SemaforoFGEM.ROJO;
        return new HallazgoFGEM(tipoFGEM, semaforoFGEM, pctFGEM(pesoFGEM), criterioFGEM, lecturaFGEM,
                "Revisa alquiler, planilla y servicios: cada gasto fijo que reduzcas mejora la utilidad en el mismo monto. "
                        + "También puedes aprovechar mejor la estructura actual vendiendo más con los mismos recursos.");
    }

    private HallazgoFGEM cargaFinancieraFGEM() {
        TipoIndicadorFGEM tipoFGEM = TipoIndicadorFGEM.CARGA_FINANCIERA;
        BigDecimal interesesFGEM = estadoFGEM.gastosFinancierosFGEM();
        String criterioFGEM = "Bien hasta 20 %; riesgo por encima de 50 % o si la operación no cubre los intereses.";
        if (interesesFGEM.signum() == 0) {
            return new HallazgoFGEM(tipoFGEM, SemaforoFGEM.VERDE, "0 %", criterioFGEM, "No hay intereses por deudas registrados.", null);
        }
        String recomendacionFGEM = "Evalúa refinanciar a una tasa menor o adelantar pagos de la deuda más cara, "
                + "y evita nuevos préstamos hasta mejorar la utilidad operativa.";
        if (estadoFGEM.utilidadOperativaFGEM().signum() <= 0) {
            return new HallazgoFGEM(tipoFGEM, SemaforoFGEM.ROJO, "No cubre", criterioFGEM,
                    "La operación no genera utilidad suficiente para pagar los intereses (" + dineroFGEM(interesesFGEM)
                            + " en el período).", recomendacionFGEM);
        }
        BigDecimal cargaFGEM = interesesFGEM.divide(estadoFGEM.utilidadOperativaFGEM(), 6, RoundingMode.HALF_UP);
        String lecturaFGEM = "Los intereses se llevan el " + pctFGEM(cargaFGEM) + " de la utilidad operativa.";
        if (cargaFGEM.compareTo(new BigDecimal("0.20")) <= 0) {
            return new HallazgoFGEM(tipoFGEM, SemaforoFGEM.VERDE, pctFGEM(cargaFGEM), criterioFGEM, lecturaFGEM, null);
        }
        SemaforoFGEM semaforoFGEM = cargaFGEM.compareTo(new BigDecimal("0.50")) <= 0 ? SemaforoFGEM.AMARILLO : SemaforoFGEM.ROJO;
        return new HallazgoFGEM(tipoFGEM, semaforoFGEM, pctFGEM(cargaFGEM), criterioFGEM, lecturaFGEM, recomendacionFGEM);
    }

    /** Balance general disponible con al menos un activo registrado; si no, liquidez y endeudamiento no se evalúan. */
    private Optional<SituacionPatrimonialFGEM> patrimonioFGEM() {
        return Optional.ofNullable(datosFGEM.patrimonioFGEM()).filter(sFGEM -> sFGEM.balanceFGEM().tieneActivosFGEM());
    }

    private HallazgoFGEM liquidezFGEM() {
        TipoIndicadorFGEM tipoFGEM = TipoIndicadorFGEM.LIQUIDEZ;
        Optional<SituacionPatrimonialFGEM> situacionFGEM = patrimonioFGEM();
        if (situacionFGEM.isEmpty()) {
            return HallazgoFGEM.sinDatosFGEM(tipoFGEM, "Registra las cuentas en «Tesorería» y los activos y deudas en «Activos y deudas» para evaluarla.");
        }
        IndicadoresPatrimonioFGEM iFGEM = situacionFGEM.get().indicadoresFGEM();
        String criterioFGEM = "Bien desde " + IndicadoresPatrimonioFGEM.vecesFGEM(IndicadoresPatrimonioFGEM.LIQUIDEZ_VERDE_FGEM)
                + "; riesgo por debajo de " + IndicadoresPatrimonioFGEM.vecesFGEM(IndicadoresPatrimonioFGEM.LIQUIDEZ_AMARILLO_FGEM) + ".";
        BigDecimal liquidezFGEM = iFGEM.liquidezCorrienteFGEM();
        if (liquidezFGEM == null) {
            return new HallazgoFGEM(tipoFGEM, SemaforoFGEM.VERDE, "Sin deudas", criterioFGEM,
                    "No hay deudas que venzan en los próximos 12 meses.", null);
        }
        String valorFGEM = IndicadoresPatrimonioFGEM.vecesFGEM(liquidezFGEM);
        String lecturaFGEM = "Por cada " + monedaFGEM.simboloFGEM() + " 1 que vence en el año hay " + monedaFGEM.simboloFGEM() + " " + valorFGEM
                + " en caja, cobros pendientes e inventario.";
        if (liquidezFGEM.compareTo(IndicadoresPatrimonioFGEM.LIQUIDEZ_VERDE_FGEM) >= 0) {
            return new HallazgoFGEM(tipoFGEM, SemaforoFGEM.VERDE, valorFGEM, criterioFGEM, lecturaFGEM, null);
        }
        if (liquidezFGEM.compareTo(IndicadoresPatrimonioFGEM.LIQUIDEZ_AMARILLO_FGEM) >= 0) {
            return new HallazgoFGEM(tipoFGEM, SemaforoFGEM.AMARILLO, valorFGEM, criterioFGEM, lecturaFGEM,
                    "Cobra a tiempo a los clientes, mantén una reserva de caja de al menos un mes de gastos fijos "
                            + "y no pagues bienes de largo plazo con dinero que necesitas en el año.");
        }
        return new HallazgoFGEM(tipoFGEM, SemaforoFGEM.ROJO, valorFGEM, criterioFGEM, lecturaFGEM,
                "Faltan unos " + dineroFGEM(iFGEM.capitalTrabajoFGEM().negate()) + " para cubrir lo que vence en el año. Negocia "
                        + "más plazo con proveedores, refinancia los préstamos a un plazo mayor y acelera los cobros pendientes.");
    }

    /**
     * Rojo si con el pronóstico base el dinero se acaba en los próximos seis meses; amarillo si solo se
     * acaba en el escenario pesimista; verde si alcanza en ambos.
     */
    private HallazgoFGEM cajaProyectadaFGEM() {
        TipoIndicadorFGEM tipoFGEM = TipoIndicadorFGEM.CAJA_PROYECTADA;
        ProyeccionCajaFGEM cajaFGEM = datosFGEM.cajaFGEM();
        if (cajaFGEM == null || !cajaFGEM.suficienteFGEM()) {
            return HallazgoFGEM.sinDatosFGEM(tipoFGEM, "Se necesitan al menos " + ProyeccionCajaFGEM.MINIMO_MESES_FGEM
                    + " meses completos de movimientos en las cuentas de «Tesorería».");
        }
        String criterioFGEM = "Bien si el dinero alcanza los próximos " + ProyeccionCajaFGEM.HORIZONTE_FGEM
                + " meses incluso en un escenario pesimista.";
        List<ProyeccionCajaFGEM.MesProyectadoFGEM> mesesFGEM = cajaFGEM.mesesFGEM();
        BigDecimal minimoFGEM = mesesFGEM.stream().map(ProyeccionCajaFGEM.MesProyectadoFGEM::saldoFGEM)
                .min(BigDecimal::compareTo).orElse(cajaFGEM.saldoActualFGEM());
        BigDecimal minimoPesimistaFGEM = mesesFGEM.stream().map(ProyeccionCajaFGEM.MesProyectadoFGEM::saldoPesimistaFGEM)
                .min(BigDecimal::compareTo).orElse(cajaFGEM.saldoActualFGEM());
        BigDecimal alFinalFGEM = mesesFGEM.get(mesesFGEM.size() - 1).saldoFGEM();
        if (cajaFGEM.primerNegativoFGEM() != null) {
            return new HallazgoFGEM(tipoFGEM, SemaforoFGEM.ROJO, "Falta en " + mesTextoFGEM(cajaFGEM.primerNegativoFGEM()), criterioFGEM,
                    "Con el ritmo actual de cobros, pagos y cuotas, los " + dineroFGEM(cajaFGEM.saldoActualFGEM())
                            + " disponibles hoy no alcanzan: en " + mesTextoFGEM(cajaFGEM.primerNegativoFGEM())
                            + " faltaría dinero para pagar.",
                    "Necesitarías unos " + dineroFGEM(minimoFGEM.negate()) + " adicionales. Acelera los cobros pendientes, "
                            + "posterga las compras que no sean urgentes, recorta gastos que no generan ventas y "
                            + "negocia con tiempo un plazo mayor para las cuotas.");
        }
        if (cajaFGEM.primerNegativoPesimistaFGEM() != null) {
            return new HallazgoFGEM(tipoFGEM, SemaforoFGEM.AMARILLO, dineroFGEM(alFinalFGEM), criterioFGEM,
                    "El dinero alcanza con el pronóstico base, pero si los próximos meses salen tan mal como el "
                            + "peor caso esperado faltaría en " + mesTextoFGEM(cajaFGEM.primerNegativoPesimistaFGEM()) + ".",
                    "Arma una reserva de unos " + dineroFGEM(minimoPesimistaFGEM.negate())
                            + " (por ejemplo, separando una parte de la venta de cada semana) y evita comprometer "
                            + "nuevas cuotas hasta tenerla.");
        }
        return new HallazgoFGEM(tipoFGEM, SemaforoFGEM.VERDE, dineroFGEM(alFinalFGEM), criterioFGEM,
                "El dinero alcanza los próximos " + ProyeccionCajaFGEM.HORIZONTE_FGEM + " meses incluso en el escenario "
                        + "pesimista; al final del período habría unos " + dineroFGEM(alFinalFGEM) + " disponibles.", null);
    }

    private HallazgoFGEM endeudamientoFGEM() {
        TipoIndicadorFGEM tipoFGEM = TipoIndicadorFGEM.ENDEUDAMIENTO;
        Optional<SituacionPatrimonialFGEM> situacionFGEM = patrimonioFGEM();
        if (situacionFGEM.isEmpty()) {
            return HallazgoFGEM.sinDatosFGEM(tipoFGEM, "Registra los activos y las deudas en «Activos y deudas» para evaluarlo.");
        }
        BalanceGeneralFGEM bFGEM = situacionFGEM.get().balanceFGEM();
        BigDecimal ratioFGEM = situacionFGEM.get().indicadoresFGEM().endeudamientoFGEM();
        String criterioFGEM = "Bien hasta " + pctFGEM(IndicadoresPatrimonioFGEM.ENDEUDAMIENTO_VERDE_FGEM) + "; riesgo por encima de "
                + pctFGEM(IndicadoresPatrimonioFGEM.ENDEUDAMIENTO_AMARILLO_FGEM) + ".";
        if (bFGEM.totalPasivoFGEM().signum() == 0) {
            return new HallazgoFGEM(tipoFGEM, SemaforoFGEM.VERDE, "0 %", criterioFGEM, "El negocio no tiene deudas pendientes.", null);
        }
        if (ratioFGEM == null || bFGEM.patrimonioFGEM().signum() <= 0) {
            return new HallazgoFGEM(tipoFGEM, SemaforoFGEM.ROJO, ratioFGEM == null ? "Sin activos" : pctFGEM(ratioFGEM), criterioFGEM,
                    "Las deudas (" + dineroFGEM(bFGEM.totalPasivoFGEM()) + ") superan a los bienes del negocio ("
                            + dineroFGEM(bFGEM.totalActivoFGEM()) + "): el patrimonio es negativo.",
                    "No tomes nuevos préstamos, destina las utilidades a pagar primero las deudas más caras "
                            + "y evalúa un aporte de capital de los dueños.");
        }
        String lecturaFGEM = "Las deudas financian el " + pctFGEM(ratioFGEM) + " de los bienes del negocio; el resto es patrimonio "
                + "de los dueños (" + dineroFGEM(bFGEM.patrimonioFGEM()) + ").";
        if (ratioFGEM.compareTo(IndicadoresPatrimonioFGEM.ENDEUDAMIENTO_VERDE_FGEM) <= 0) {
            return new HallazgoFGEM(tipoFGEM, SemaforoFGEM.VERDE, pctFGEM(ratioFGEM), criterioFGEM, lecturaFGEM, null);
        }
        SemaforoFGEM semaforoFGEM = ratioFGEM.compareTo(IndicadoresPatrimonioFGEM.ENDEUDAMIENTO_AMARILLO_FGEM) <= 0
                ? SemaforoFGEM.AMARILLO : SemaforoFGEM.ROJO;
        BigDecimal excesoFGEM = bFGEM.totalPasivoFGEM().subtract(bFGEM.totalActivoFGEM().multiply(IndicadoresPatrimonioFGEM.ENDEUDAMIENTO_VERDE_FGEM));
        return new HallazgoFGEM(tipoFGEM, semaforoFGEM, pctFGEM(ratioFGEM), criterioFGEM, lecturaFGEM,
                "Para bajar al " + pctFGEM(IndicadoresPatrimonioFGEM.ENDEUDAMIENTO_VERDE_FGEM) + " habría que reducir las deudas en unos "
                        + dineroFGEM(excesoFGEM.max(BigDecimal.ZERO)) + ". Evita nuevos préstamos y amortiza primero los de mayor tasa.");
    }

    // ------------------------------------------------------------------ producción agropecuaria

    private HallazgoFGEM natalidadFGEM(DatosAgroFGEM agroFGEM) {
        TipoIndicadorFGEM tipoFGEM = TipoIndicadorFGEM.NATALIDAD;
        List<IndicadoresEspecieFGEM> conDatosFGEM = agroFGEM.especiesFGEM().stream().filter(eFGEM -> eFGEM.natalidadFGEM() != null).toList();
        if (conDatosFGEM.isEmpty()) {
            return HallazgoFGEM.sinDatosFGEM(tipoFGEM, "No hay hembras reproductoras registradas para calcularla.");
        }
        SemaforoFGEM peorFGEM = SemaforoFGEM.VERDE;
        IndicadoresEspecieFGEM criticaFGEM = conDatosFGEM.get(0);
        List<String> valoresFGEM = new ArrayList<>();
        for (IndicadoresEspecieFGEM eFGEM : conDatosFGEM) {
            SemaforoFGEM sFGEM = mayorEsMejorFGEM(eFGEM.natalidadFGEM(), eFGEM.especieFGEM().natalidadVerdeFGEM(), eFGEM.especieFGEM().natalidadAmarilloFGEM());
            valoresFGEM.add(eFGEM.especieFGEM().descripcionFGEM() + " " + pctFGEM(eFGEM.natalidadFGEM()));
            if (sFGEM.puntajeFGEM() < peorFGEM.puntajeFGEM()) {
                peorFGEM = sFGEM;
                criticaFGEM = eFGEM;
            }
        }
        String criterioFGEM = conDatosFGEM.stream().map(eFGEM -> eFGEM.especieFGEM().descripcionFGEM() + ": bien desde "
                + pctFGEM(eFGEM.especieFGEM().natalidadVerdeFGEM())).reduce((aFGEM, bFGEM) -> aFGEM + "; " + bFGEM).orElse("") + ".";
        String lecturaFGEM = conDatosFGEM.stream().map(eFGEM -> eFGEM.nacimientosFGEM() + " crías de " + eFGEM.hembrasInicioFGEM() + " "
                + eFGEM.especieFGEM().hembrasFGEM()).reduce((aFGEM, bFGEM) -> aFGEM + "; " + bFGEM).orElse("") + ".";
        return new HallazgoFGEM(tipoFGEM, peorFGEM, String.join(" · ", valoresFGEM), criterioFGEM, lecturaFGEM, peorFGEM == SemaforoFGEM.VERDE ? null
                : "En " + criticaFGEM.especieFGEM().descripcionFGEM().toLowerCase() + ", revisa la condición corporal de las "
                + criticaFGEM.especieFGEM().hembrasFGEM() + ", la época de empadre y la proporción de machos; pide una evaluación "
                + "veterinaria de las que no parieron.");
    }

    private HallazgoFGEM mortalidadCriasFGEM(DatosAgroFGEM agroFGEM) {
        TipoIndicadorFGEM tipoFGEM = TipoIndicadorFGEM.MORTALIDAD_CRIAS;
        List<IndicadoresEspecieFGEM> conDatosFGEM = agroFGEM.especiesFGEM().stream().filter(eFGEM -> eFGEM.mortalidadCriasFGEM() != null).toList();
        if (conDatosFGEM.isEmpty()) {
            return HallazgoFGEM.sinDatosFGEM(tipoFGEM, "No hubo nacimientos en los últimos 12 meses.");
        }
        SemaforoFGEM peorFGEM = SemaforoFGEM.VERDE;
        IndicadoresEspecieFGEM criticaFGEM = conDatosFGEM.get(0);
        List<String> valoresFGEM = new ArrayList<>();
        for (IndicadoresEspecieFGEM eFGEM : conDatosFGEM) {
            SemaforoFGEM sFGEM = menorEsMejorFGEM(eFGEM.mortalidadCriasFGEM(), eFGEM.especieFGEM().mortalidadCriasVerdeFGEM(), eFGEM.especieFGEM().mortalidadCriasAmarilloFGEM());
            valoresFGEM.add(eFGEM.especieFGEM().descripcionFGEM() + " " + pctFGEM(eFGEM.mortalidadCriasFGEM()));
            if (sFGEM.puntajeFGEM() < peorFGEM.puntajeFGEM()) {
                peorFGEM = sFGEM;
                criticaFGEM = eFGEM;
            }
        }
        String criterioFGEM = conDatosFGEM.stream().map(eFGEM -> eFGEM.especieFGEM().descripcionFGEM() + ": bien hasta "
                + pctFGEM(eFGEM.especieFGEM().mortalidadCriasVerdeFGEM())).reduce((aFGEM, bFGEM) -> aFGEM + "; " + bFGEM).orElse("") + ".";
        String lecturaFGEM = conDatosFGEM.stream().map(eFGEM -> eFGEM.muertesCriasFGEM() + " de " + eFGEM.nacimientosFGEM() + " crías de "
                + eFGEM.especieFGEM().descripcionFGEM().toLowerCase()).reduce((aFGEM, bFGEM) -> aFGEM + "; " + bFGEM).orElse("") + " murieron.";
        return new HallazgoFGEM(tipoFGEM, peorFGEM, String.join(" · ", valoresFGEM), criterioFGEM, lecturaFGEM, peorFGEM == SemaforoFGEM.VERDE ? null
                : "En " + criticaFGEM.especieFGEM().descripcionFGEM().toLowerCase() + ", protege a las crías del frío y la lluvia "
                + "(cobertizos), asegura que tomen calostro en las primeras horas y dosifica contra parásitos.");
    }

    private HallazgoFGEM rendimientoCultivosFGEM(DatosAgroFGEM agroFGEM) {
        TipoIndicadorFGEM tipoFGEM = TipoIndicadorFGEM.RENDIMIENTO_CULTIVOS;
        List<ResultadoCampaniaFGEM> conReferenciaFGEM = agroFGEM.campaniasFGEM().stream()
                .filter(rFGEM -> rFGEM.campaniaFGEM().rendimientoReferenciaFGEM() != null).toList();
        if (conReferenciaFGEM.isEmpty()) {
            return HallazgoFGEM.sinDatosFGEM(tipoFGEM, "No hay campañas cerradas con rendimiento de referencia en los últimos 12 meses.");
        }
        BigDecimal ponderadoFGEM = BigDecimal.ZERO;
        BigDecimal areaFGEM = BigDecimal.ZERO;
        ResultadoCampaniaFGEM peorFGEM = conReferenciaFGEM.get(0);
        for (ResultadoCampaniaFGEM rFGEM : conReferenciaFGEM) {
            BigDecimal avanceFGEM = rFGEM.avanceReferenciaFGEM() == null ? BigDecimal.ZERO : rFGEM.avanceReferenciaFGEM();
            ponderadoFGEM = ponderadoFGEM.add(avanceFGEM.multiply(rFGEM.campaniaFGEM().areaFGEM()));
            areaFGEM = areaFGEM.add(rFGEM.campaniaFGEM().areaFGEM());
            BigDecimal avancePeorFGEM = peorFGEM.avanceReferenciaFGEM() == null ? BigDecimal.ZERO : peorFGEM.avanceReferenciaFGEM();
            if (avanceFGEM.compareTo(avancePeorFGEM) < 0) {
                peorFGEM = rFGEM;
            }
        }
        BigDecimal promedioFGEM = ponderadoFGEM.divide(areaFGEM, 6, RoundingMode.HALF_UP);
        SemaforoFGEM semaforoFGEM = mayorEsMejorFGEM(promedioFGEM, ResultadoCampaniaFGEM.RENDIMIENTO_VERDE_FGEM, ResultadoCampaniaFGEM.RENDIMIENTO_AMARILLO_FGEM);
        String criterioFGEM = "Bien si alcanza la referencia; bajo por debajo del 70 % de ella.";
        String lecturaFGEM = conReferenciaFGEM.size() + (conReferenciaFGEM.size() == 1 ? " campaña cerrada" : " campañas cerradas")
                + " en los últimos 12 meses; en promedio rindieron el " + pctFGEM(promedioFGEM) + " de su referencia.";
        String nombrePeorFGEM = peorFGEM.campaniaFGEM().cultivoFGEM().descripcionFGEM() + " (" + peorFGEM.campaniaFGEM().parcelaNombreFGEM() + ")";
        return new HallazgoFGEM(tipoFGEM, semaforoFGEM, pctFGEM(promedioFGEM) + " de la referencia", criterioFGEM, lecturaFGEM,
                semaforoFGEM == SemaforoFGEM.VERDE ? null
                        : "El rendimiento más bajo fue el de " + nombrePeorFGEM
                        + (peorFGEM.campaniaFGEM().motivoFGEM() == null ? "" : ", perdido por " + peorFGEM.campaniaFGEM().motivoFGEM().toLowerCase())
                        + ". Revisa la semilla, el abonamiento y el control de plagas, y compara costos y rendimientos "
                        + "en Agricultura → Comparación.");
    }

    private HallazgoFGEM compromisosFGEM(DatosAgroFGEM agroFGEM) {
        TipoIndicadorFGEM tipoFGEM = TipoIndicadorFGEM.COMPROMISOS_INSPECCION;
        int evaluablesFGEM = agroFGEM.compromisosCumplidosFGEM() + agroFGEM.compromisosFallidosFGEM();
        if (evaluablesFGEM == 0) {
            return HallazgoFGEM.sinDatosFGEM(tipoFGEM, "No hay compromisos de inspección cuyo plazo ya se haya cumplido.");
        }
        BigDecimal cumplimientoFGEM = BigDecimal.valueOf(agroFGEM.compromisosCumplidosFGEM())
                .divide(BigDecimal.valueOf(evaluablesFGEM), 6, RoundingMode.HALF_UP);
        SemaforoFGEM semaforoFGEM = mayorEsMejorFGEM(cumplimientoFGEM, new BigDecimal("0.80"), new BigDecimal("0.50"));
        return new HallazgoFGEM(tipoFGEM, semaforoFGEM, pctFGEM(cumplimientoFGEM), "Bien desde 80 %; riesgo por debajo de 50 %.",
                agroFGEM.compromisosCumplidosFGEM() + " de " + evaluablesFGEM + " compromisos cumplidos.",
                semaforoFGEM == SemaforoFGEM.VERDE ? null
                        : "Atiende los " + agroFGEM.compromisosFallidosFGEM() + " compromisos no cumplidos o vencidos antes de la "
                        + "próxima visita (Visitas de inspección → Compromisos).");
    }

    private static SemaforoFGEM mayorEsMejorFGEM(BigDecimal valorFGEM, BigDecimal verdeFGEM, BigDecimal amarilloFGEM) {
        if (valorFGEM.compareTo(verdeFGEM) >= 0) {
            return SemaforoFGEM.VERDE;
        }
        return valorFGEM.compareTo(amarilloFGEM) >= 0 ? SemaforoFGEM.AMARILLO : SemaforoFGEM.ROJO;
    }

    private static SemaforoFGEM menorEsMejorFGEM(BigDecimal valorFGEM, BigDecimal verdeFGEM, BigDecimal amarilloFGEM) {
        if (valorFGEM.compareTo(verdeFGEM) <= 0) {
            return SemaforoFGEM.VERDE;
        }
        return valorFGEM.compareTo(amarilloFGEM) <= 0 ? SemaforoFGEM.AMARILLO : SemaforoFGEM.ROJO;
    }

    // ------------------------------------------------------------------ crecimiento

    private HallazgoFGEM variacionVentasFGEM() {
        TipoIndicadorFGEM tipoFGEM = TipoIndicadorFGEM.VARIACION_VENTAS;
        BigDecimal anteriorFGEM = datosFGEM.operacionesAnterioresFGEM().ventasNetasFGEM();
        if (anteriorFGEM.signum() <= 0) {
            return HallazgoFGEM.sinDatosFGEM(tipoFGEM, "No hay ventas en " + datosFGEM.periodoComparadoFGEM().descripcionFGEM() + " para comparar.");
        }
        BigDecimal cambioFGEM = estadoFGEM.ventasNetasFGEM().subtract(anteriorFGEM).divide(anteriorFGEM, 6, RoundingMode.HALF_UP);
        String valorFGEM = signoFGEM(cambioFGEM) + pctFGEM(cambioFGEM);
        String criterioFGEM = "Bien si no caen más de 2 %; riesgo si caen más de 10 %.";
        String lecturaFGEM = "Las ventas " + (cambioFGEM.signum() >= 0 ? "subieron " : "bajaron ") + pctFGEM(cambioFGEM.abs())
                + " frente a " + datosFGEM.periodoComparadoFGEM().descripcionFGEM() + ".";
        if (cambioFGEM.compareTo(new BigDecimal("-0.02")) >= 0) {
            return new HallazgoFGEM(tipoFGEM, SemaforoFGEM.VERDE, valorFGEM, criterioFGEM, lecturaFGEM, null);
        }
        SemaforoFGEM semaforoFGEM = cambioFGEM.compareTo(new BigDecimal("-0.10")) >= 0 ? SemaforoFGEM.AMARILLO : SemaforoFGEM.ROJO;
        return new HallazgoFGEM(tipoFGEM, semaforoFGEM, valorFGEM, criterioFGEM, lecturaFGEM,
                "Averigua qué productos bajaron (Análisis estadístico → ABC) y si la caída es de temporada; "
                        + "refuerza la promoción de los productos que más venden.");
    }

    private HallazgoFGEM tendenciaVentasFGEM() {
        TipoIndicadorFGEM tipoFGEM = TipoIndicadorFGEM.TENDENCIA_VENTAS;
        double[] ventasFGEM = ultimosFGEM(datosFGEM.ventasMensualesFGEM(), 12);
        if (ventasFGEM.length < MINIMO_MESES_SERIE_FGEM) {
            return HallazgoFGEM.sinDatosFGEM(tipoFGEM, "Se necesitan al menos " + MINIMO_MESES_SERIE_FGEM + " meses con ventas.");
        }
        TendenciaFGEM tendenciaFGEM = TendenciaFGEM.deFGEM(ventasFGEM);
        double crecimientoFGEM = tendenciaFGEM.crecimientoMensualFGEM();
        String valorFGEM = (crecimientoFGEM >= 0 ? "+" : "") + pctFGEM(crecimientoFGEM) + " al mes";
        String criterioFGEM = "Riesgo si caen de forma sostenida (tendencia significativa con p < 0.05).";
        String recomendacionFGEM = "Las ventas vienen bajando: revisa precios frente a la competencia, la atención y la "
                + "publicidad, y apóyate en Predicciones para anticipar los próximos meses.";
        return switch (tendenciaFGEM.direccionFGEM()) {
            case CRECIENTE -> new HallazgoFGEM(tipoFGEM, SemaforoFGEM.VERDE, valorFGEM, criterioFGEM,
                    "Las ventas crecen de forma sostenida, alrededor de " + pctFGEM(crecimientoFGEM) + " al mes.", null);
            case DECRECIENTE -> new HallazgoFGEM(tipoFGEM, SemaforoFGEM.ROJO, valorFGEM, criterioFGEM,
                    "Las ventas caen de forma sostenida, alrededor de " + pctFGEM(Math.abs(crecimientoFGEM)) + " al mes.",
                    recomendacionFGEM);
            default -> crecimientoFGEM < -0.01
                    ? new HallazgoFGEM(tipoFGEM, SemaforoFGEM.AMARILLO, valorFGEM, criterioFGEM,
                    "Las ventas tienden a bajar, aunque la tendencia todavía no es clara.", recomendacionFGEM)
                    : new HallazgoFGEM(tipoFGEM, SemaforoFGEM.VERDE, valorFGEM, criterioFGEM,
                    "Las ventas se mantienen estables, sin una tendencia clara al alza ni a la baja.", null);
        };
    }

    private HallazgoFGEM proyeccionVentasFGEM() {
        TipoIndicadorFGEM tipoFGEM = TipoIndicadorFGEM.PROYECCION_VENTAS;
        double[] ventasFGEM = datosFGEM.ventasMensualesFGEM();
        PronosticoFGEM pronosticoFGEM = PronosticadorFGEM.pronosticarFGEM("Ventas netas", ventasFGEM, HORIZONTE_PROYECCION_FGEM, true);
        if (!pronosticoFGEM.suficienteFGEM() || ventasFGEM.length < HORIZONTE_PROYECCION_FGEM) {
            return HallazgoFGEM.sinDatosFGEM(tipoFGEM, "Se necesitan al menos " + PronosticadorFGEM.MINIMO_DATOS_FGEM + " meses con ventas.");
        }
        double recientesFGEM = Arrays.stream(ultimosFGEM(ventasFGEM, HORIZONTE_PROYECCION_FGEM)).sum();
        if (recientesFGEM <= 0) {
            return HallazgoFGEM.sinDatosFGEM(tipoFGEM, "No hay ventas en los últimos meses para comparar.");
        }
        double cambioFGEM = pronosticoFGEM.totalFGEM() / recientesFGEM - 1;
        String valorFGEM = (cambioFGEM >= 0 ? "+" : "") + pctFGEM(cambioFGEM);
        String criterioFGEM = "Bien si no se proyecta una caída mayor a 2 %; riesgo si es mayor a 10 %.";
        String lecturaFGEM = "Para los próximos " + HORIZONTE_PROYECCION_FGEM + " meses se proyectan ventas de "
                + dineroFGEM(BigDecimal.valueOf(pronosticoFGEM.totalFGEM())) + " (confiabilidad "
                + pronosticoFGEM.confiabilidadFGEM().descripcionFGEM().toLowerCase() + ").";
        if (cambioFGEM >= -0.02) {
            return new HallazgoFGEM(tipoFGEM, SemaforoFGEM.VERDE, valorFGEM, criterioFGEM, lecturaFGEM, null);
        }
        return new HallazgoFGEM(tipoFGEM, cambioFGEM >= -0.10 ? SemaforoFGEM.AMARILLO : SemaforoFGEM.ROJO, valorFGEM, criterioFGEM, lecturaFGEM,
                "Prepárate para meses más flojos: ajusta compras e inventario a la venta esperada y planifica "
                        + "promociones con anticipación.");
    }

    // ------------------------------------------------------------------ riesgo comercial

    private HallazgoFGEM regularidadVentasFGEM() {
        TipoIndicadorFGEM tipoFGEM = TipoIndicadorFGEM.REGULARIDAD_VENTAS;
        double[] ventasFGEM = ultimosFGEM(datosFGEM.ventasMensualesFGEM(), 12);
        if (ventasFGEM.length < MINIMO_MESES_SERIE_FGEM) {
            return HallazgoFGEM.sinDatosFGEM(tipoFGEM, "Se necesitan al menos " + MINIMO_MESES_SERIE_FGEM + " meses con ventas.");
        }
        double cvFGEM = EstadisticaDescriptivaFGEM.deFGEM(ventasFGEM).coeficienteVariacionFGEM();
        if (Double.isNaN(cvFGEM)) {
            return HallazgoFGEM.sinDatosFGEM(tipoFGEM, "No se pudo calcular la variación de las ventas.");
        }
        String criterioFGEM = "Bien hasta 15 %; riesgo por encima de 30 %.";
        String lecturaFGEM = "Las ventas mensuales se alejan en promedio un " + pctFGEM(cvFGEM) + " de su valor medio.";
        if (cvFGEM <= 0.15) {
            return new HallazgoFGEM(tipoFGEM, SemaforoFGEM.VERDE, pctFGEM(cvFGEM), criterioFGEM, lecturaFGEM + " Son bastante predecibles.", null);
        }
        return new HallazgoFGEM(tipoFGEM, cvFGEM <= 0.30 ? SemaforoFGEM.AMARILLO : SemaforoFGEM.ROJO, pctFGEM(cvFGEM), criterioFGEM, lecturaFGEM,
                "Planifica compras y caja según los meses fuertes y débiles, y usa promociones para sostener los meses bajos.");
    }

    private HallazgoFGEM concentracionFGEM() {
        TipoIndicadorFGEM tipoFGEM = TipoIndicadorFGEM.CONCENTRACION;
        BigDecimal totalFGEM = datosFGEM.productosFGEM().stream().map(RentabilidadProductoFGEM::ventasFGEM).reduce(BigDecimal.ZERO, BigDecimal::add);
        if (totalFGEM.signum() <= 0) {
            return HallazgoFGEM.sinDatosFGEM(tipoFGEM, "No hay ventas por producto en el período.");
        }
        RentabilidadProductoFGEM principalFGEM = datosFGEM.productosFGEM().stream()
                .max(Comparator.comparing(RentabilidadProductoFGEM::ventasFGEM)).orElseThrow();
        BigDecimal participacionFGEM = principalFGEM.ventasFGEM().divide(totalFGEM, 6, RoundingMode.HALF_UP);
        String criterioFGEM = "Bien hasta 40 %; riesgo por encima de 60 %.";
        String lecturaFGEM = "\"" + principalFGEM.nombreFGEM() + "\" representa el " + pctFGEM(participacionFGEM) + " de las ventas.";
        if (participacionFGEM.compareTo(new BigDecimal("0.40")) <= 0) {
            return new HallazgoFGEM(tipoFGEM, SemaforoFGEM.VERDE, pctFGEM(participacionFGEM), criterioFGEM, lecturaFGEM, null);
        }
        SemaforoFGEM semaforoFGEM = participacionFGEM.compareTo(new BigDecimal("0.60")) <= 0 ? SemaforoFGEM.AMARILLO : SemaforoFGEM.ROJO;
        return new HallazgoFGEM(tipoFGEM, semaforoFGEM, pctFGEM(participacionFGEM), criterioFGEM, lecturaFGEM,
                "Impulsa otros productos o servicios para no depender tanto de \"" + principalFGEM.nombreFGEM()
                        + "\": un problema con él (proveedor, precio, competencia) afectaría a todo el negocio.");
    }

    private HallazgoFGEM productosConPerdidaFGEM() {
        TipoIndicadorFGEM tipoFGEM = TipoIndicadorFGEM.PRODUCTOS_CON_PERDIDA;
        BigDecimal totalFGEM = datosFGEM.productosFGEM().stream().map(RentabilidadProductoFGEM::ventasFGEM).reduce(BigDecimal.ZERO, BigDecimal::add);
        if (totalFGEM.signum() <= 0) {
            return HallazgoFGEM.sinDatosFGEM(tipoFGEM, "No hay ventas por producto en el período.");
        }
        List<RentabilidadProductoFGEM> conPerdidaFGEM = datosFGEM.productosFGEM().stream()
                .filter(pFGEM -> pFGEM.margenFGEM().signum() < 0)
                .sorted(Comparator.comparing(RentabilidadProductoFGEM::margenFGEM))
                .toList();
        String criterioFGEM = "Bien si no hay ninguno; riesgo si superan el 5 % de las ventas.";
        if (conPerdidaFGEM.isEmpty()) {
            return new HallazgoFGEM(tipoFGEM, SemaforoFGEM.VERDE, "0 %", criterioFGEM,
                    "Todos los productos se venden por encima de su costo.", null);
        }
        BigDecimal ventasFGEM = conPerdidaFGEM.stream().map(RentabilidadProductoFGEM::ventasFGEM).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal perdidaFGEM = conPerdidaFGEM.stream().map(RentabilidadProductoFGEM::margenFGEM).reduce(BigDecimal.ZERO, BigDecimal::add).negate();
        BigDecimal participacionFGEM = ventasFGEM.divide(totalFGEM, 6, RoundingMode.HALF_UP);
        List<String> nombresFGEM = conPerdidaFGEM.stream().limit(3).map(pFGEM -> "\"" + pFGEM.nombreFGEM() + "\"").toList();
        String listaFGEM = String.join(", ", nombresFGEM) + (conPerdidaFGEM.size() > 3 ? " y " + (conPerdidaFGEM.size() - 3) + " más" : "");
        SemaforoFGEM semaforoFGEM = participacionFGEM.compareTo(new BigDecimal("0.05")) <= 0 ? SemaforoFGEM.AMARILLO : SemaforoFGEM.ROJO;
        return new HallazgoFGEM(tipoFGEM, semaforoFGEM, pctFGEM(participacionFGEM), criterioFGEM,
                (conPerdidaFGEM.size() == 1 ? "1 producto se vende" : conPerdidaFGEM.size() + " productos se venden")
                        + " por debajo de su costo: " + listaFGEM + " (pérdida de " + dineroFGEM(perdidaFGEM) + " en el período).",
                "Sube el precio de " + (conPerdidaFGEM.size() == 1 ? "ese producto" : "esos productos")
                        + " por encima de su costo o renegocia su compra; si los usas como gancho, verifica que "
                        + "realmente atraigan otras ventas.");
    }

    // ------------------------------------------------------------------ utilidades

    private static double[] ultimosFGEM(double[] valoresFGEM, int cantidadFGEM) {
        return Arrays.copyOfRange(valoresFGEM, Math.max(0, valoresFGEM.length - cantidadFGEM), valoresFGEM.length);
    }

    private String sectorTextoFGEM() {
        SectorFGEM sectorFGEM = datosFGEM.sectorFGEM() == null ? SectorFGEM.OTRO : datosFGEM.sectorFGEM();
        return sectorFGEM == SectorFGEM.OTRO ? "un negocio en general" : "el sector " + sectorFGEM.descripcionFGEM().toLowerCase();
    }

    private String dineroFGEM(BigDecimal valorFGEM) {
        return MontosFGEM.formatearFGEM(valorFGEM.setScale(2, RoundingMode.HALF_UP), monedaFGEM);
    }

    private static String pctFGEM(BigDecimal fraccionFGEM) {
        return MontosFGEM.porcentajeFGEM(fraccionFGEM);
    }

    private static String pctFGEM(double fraccionFGEM) {
        return MontosFGEM.porcentajeFGEM(BigDecimal.valueOf(fraccionFGEM));
    }

    private static String signoFGEM(BigDecimal valorFGEM) {
        return valorFGEM.signum() > 0 ? "+" : "";
    }

    private static String mesTextoFGEM(YearMonth mesFGEM) {
        return FechasFGEM.nombreMesFGEM(mesFGEM.getMonthValue()).toLowerCase() + " " + mesFGEM.getYear();
    }

    private static BigDecimal bdFGEM(String valorFGEM) {
        return new BigDecimal(valorFGEM);
    }
}
