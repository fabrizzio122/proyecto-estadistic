package com.businessanalyzer.analytics.valoracion;

import com.businessanalyzer.model.MonedaFGEM;
import com.businessanalyzer.util.MontosFGEM;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

public record ValoracionNegocioFGEM(SupuestosValoracionFGEM supuestosFGEM, BigDecimal patrimonialFGEM, BigDecimal utilidadAnualFGEM,
                                BigDecimal depreciacionAnualFGEM, BigDecimal flujoAnualFGEM, BigDecimal capitalizacionFGEM,
                                BigDecimal flujosDescontadosFGEM, List<ProyeccionFGEM> proyeccionFGEM, BigDecimal valorTerminalFGEM,
                                BigDecimal valorTerminalPresenteFGEM, BigDecimal valorRentabilidadFGEM, BigDecimal fondoComercioFGEM,
                                BigDecimal valorReferenciaFGEM, BaseFGEM baseFGEM, BigDecimal minimoFGEM, BigDecimal maximoFGEM,
                                int mesesResultadosFGEM) {

    public static final int MESES_CONFIABLES_FGEM = 6;
    private static final MathContext PRECISION_FGEM = new MathContext(20, RoundingMode.HALF_UP);

    public enum BaseFGEM { RENTABILIDAD, PATRIMONIO }

    public record ProyeccionFGEM(int anioFGEM, BigDecimal flujoFGEM, BigDecimal factorFGEM, BigDecimal valorPresenteFGEM) {
    }

    public record SensibilidadFGEM(List<BigDecimal> tasasFGEM, List<BigDecimal> crecimientosFGEM, BigDecimal[][] valoresFGEM) {
    }

    public static ValoracionNegocioFGEM calcularFGEM(SupuestosValoracionFGEM sFGEM, BigDecimal patrimonialFGEM, BigDecimal utilidadAnualFGEM,
                                             BigDecimal depreciacionMensualFGEM, int mesesResultadosFGEM) {
        BigDecimal utilidadFGEM = utilidadAnualFGEM == null ? BigDecimal.ZERO : utilidadAnualFGEM;
        BigDecimal depreciacionFGEM = depreciacionMensualFGEM.multiply(BigDecimal.valueOf(12));
        BigDecimal flujoFGEM = utilidadFGEM.add(depreciacionFGEM);

        BigDecimal capitalizacionFGEM = utilidadFGEM.signum() > 0 ? dineroFGEM(utilidadFGEM.divide(sFGEM.tasaDescuentoFGEM(), PRECISION_FGEM)) : null;

        List<ProyeccionFGEM> proyeccionFGEM = new ArrayList<>();
        BigDecimal valorTerminalFGEM = null;
        BigDecimal terminalPresenteFGEM = null;
        BigDecimal descontadosFGEM = null;
        if (flujoFGEM.signum() > 0 && utilidadAnualFGEM != null) {
            BigDecimal sumaFGEM = BigDecimal.ZERO;
            BigDecimal ultimoFlujoFGEM = flujoFGEM;
            for (int tFGEM = 1; tFGEM <= sFGEM.aniosFGEM(); tFGEM++) {
                BigDecimal flujoTFGEM = flujoFGEM.multiply(BigDecimal.ONE.add(sFGEM.crecimientoFGEM()).pow(tFGEM), PRECISION_FGEM);
                BigDecimal factorFGEM = BigDecimal.ONE.divide(BigDecimal.ONE.add(sFGEM.tasaDescuentoFGEM()).pow(tFGEM), PRECISION_FGEM);
                BigDecimal presenteFGEM = flujoTFGEM.multiply(factorFGEM, PRECISION_FGEM);
                proyeccionFGEM.add(new ProyeccionFGEM(tFGEM, dineroFGEM(flujoTFGEM), factorFGEM.setScale(4, RoundingMode.HALF_UP), dineroFGEM(presenteFGEM)));
                sumaFGEM = sumaFGEM.add(presenteFGEM);
                ultimoFlujoFGEM = flujoTFGEM;
            }
            BigDecimal vtFGEM = ultimoFlujoFGEM.multiply(BigDecimal.ONE.add(sFGEM.crecimientoLargoPlazoFGEM()), PRECISION_FGEM)
                    .divide(sFGEM.tasaDescuentoFGEM().subtract(sFGEM.crecimientoLargoPlazoFGEM()), PRECISION_FGEM);
            BigDecimal vtPresenteFGEM = vtFGEM.divide(BigDecimal.ONE.add(sFGEM.tasaDescuentoFGEM()).pow(sFGEM.aniosFGEM()), PRECISION_FGEM);
            valorTerminalFGEM = dineroFGEM(vtFGEM);
            terminalPresenteFGEM = dineroFGEM(vtPresenteFGEM);
            descontadosFGEM = dineroFGEM(sumaFGEM.add(vtPresenteFGEM));
        }

        List<BigDecimal> rentabilidadFGEM = new ArrayList<>();
        if (capitalizacionFGEM != null) {
            rentabilidadFGEM.add(capitalizacionFGEM);
        }
        if (descontadosFGEM != null) {
            rentabilidadFGEM.add(descontadosFGEM);
        }
        BigDecimal valorRentabilidadFGEM = rentabilidadFGEM.isEmpty() ? null
                : dineroFGEM(rentabilidadFGEM.stream().reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(rentabilidadFGEM.size()), PRECISION_FGEM));
        BigDecimal fondoFGEM = valorRentabilidadFGEM == null ? null : valorRentabilidadFGEM.subtract(patrimonialFGEM);
        boolean porRentabilidadFGEM = valorRentabilidadFGEM != null && valorRentabilidadFGEM.compareTo(patrimonialFGEM) > 0;
        BigDecimal referenciaFGEM = porRentabilidadFGEM ? valorRentabilidadFGEM : patrimonialFGEM;

        List<BigDecimal> todosFGEM = new ArrayList<>(rentabilidadFGEM);
        todosFGEM.add(patrimonialFGEM);
        BigDecimal minimoFGEM = todosFGEM.stream().min(BigDecimal::compareTo).orElse(patrimonialFGEM);
        BigDecimal maximoFGEM = todosFGEM.stream().max(BigDecimal::compareTo).orElse(patrimonialFGEM);
        return new ValoracionNegocioFGEM(sFGEM, dineroFGEM(patrimonialFGEM), dineroFGEM(utilidadFGEM), dineroFGEM(depreciacionFGEM), dineroFGEM(flujoFGEM),
                capitalizacionFGEM, descontadosFGEM, proyeccionFGEM, valorTerminalFGEM, terminalPresenteFGEM, valorRentabilidadFGEM, fondoFGEM,
                dineroFGEM(referenciaFGEM), porRentabilidadFGEM ? BaseFGEM.RENTABILIDAD : BaseFGEM.PATRIMONIO, dineroFGEM(minimoFGEM), dineroFGEM(maximoFGEM),
                mesesResultadosFGEM);
    }

    public SensibilidadFGEM sensibilidadFGEM() {
        List<BigDecimal> tasasFGEM = List.of(supuestosFGEM.tasaDescuentoFGEM().subtract(new BigDecimal("0.05")),
                supuestosFGEM.tasaDescuentoFGEM(), supuestosFGEM.tasaDescuentoFGEM().add(new BigDecimal("0.05")));
        List<BigDecimal> crecimientosFGEM = List.of(supuestosFGEM.crecimientoFGEM().subtract(new BigDecimal("0.02")),
                supuestosFGEM.crecimientoFGEM(), supuestosFGEM.crecimientoFGEM().add(new BigDecimal("0.02")));
        BigDecimal[][] valoresFGEM = new BigDecimal[tasasFGEM.size()][crecimientosFGEM.size()];
        for (int iFGEM = 0; iFGEM < tasasFGEM.size(); iFGEM++) {
            for (int jFGEM = 0; jFGEM < crecimientosFGEM.size(); jFGEM++) {
                try {
                    SupuestosValoracionFGEM sFGEM = new SupuestosValoracionFGEM(tasasFGEM.get(iFGEM), crecimientosFGEM.get(jFGEM),
                            supuestosFGEM.crecimientoLargoPlazoFGEM().min(tasasFGEM.get(iFGEM).subtract(new BigDecimal("0.01"))),
                            supuestosFGEM.aniosFGEM());
                    valoresFGEM[iFGEM][jFGEM] = calcularFGEM(sFGEM, patrimonialFGEM, utilidadAnualFGEM, depreciacionAnualFGEM
                            .divide(BigDecimal.valueOf(12), PRECISION_FGEM), mesesResultadosFGEM).flujosDescontadosFGEM();
                } catch (RuntimeException eFGEM) {
                    valoresFGEM[iFGEM][jFGEM] = null;
                }
            }
        }
        return new SensibilidadFGEM(tasasFGEM, crecimientosFGEM, valoresFGEM);
    }

    public boolean pocosDatosFGEM() {
        return mesesResultadosFGEM < MESES_CONFIABLES_FGEM;
    }

    public List<String> lecturaFGEM(MonedaFGEM monedaFGEM) {
        List<String> frasesFGEM = new ArrayList<>();
        frasesFGEM.add("Con lo que tiene y lo que debe, el negocio vale " + MontosFGEM.formatearFGEM(patrimonialFGEM, monedaFGEM)
                + " (valor patrimonial).");
        if (valorRentabilidadFGEM == null) {
            frasesFGEM.add("Como en los últimos 12 meses no tuvo utilidad, por ahora solo puede valorarse por lo que tiene: "
                    + "un comprador pagaría como mucho su valor patrimonial.");
        } else {
            frasesFGEM.add("Por lo que gana (" + MontosFGEM.formatearFGEM(utilidadAnualFGEM, monedaFGEM) + " al año), y exigiendo una "
                    + "rentabilidad de " + MontosFGEM.porcentajeFGEM(supuestosFGEM.tasaDescuentoFGEM()) + " anual, vale alrededor de "
                    + MontosFGEM.formatearFGEM(valorRentabilidadFGEM, monedaFGEM) + ".");
            if (baseFGEM == BaseFGEM.RENTABILIDAD) {
                frasesFGEM.add("El negocio vale más por lo que gana que por lo que tiene: el fondo de comercio (clientela, "
                        + "ubicación, prestigio) se estima en " + MontosFGEM.formatearFGEM(fondoComercioFGEM, monedaFGEM) + ".");
            } else {
                frasesFGEM.add("Lo que gana no alcanza a justificar el valor de sus bienes: rinde menos de lo que exigiría "
                        + "un inversionista. Mejorar la utilidad es la forma de que el negocio valga más.");
            }
        }
        frasesFGEM.add("Valor de referencia: " + MontosFGEM.formatearFGEM(valorReferenciaFGEM, monedaFGEM) + " (rango de "
                + MontosFGEM.formatearFGEM(minimoFGEM, monedaFGEM) + " a " + MontosFGEM.formatearFGEM(maximoFGEM, monedaFGEM) + ").");
        if (pocosDatosFGEM() && valorRentabilidadFGEM != null) {
            frasesFGEM.add("Atención: la utilidad se calculó con solo " + mesesResultadosFGEM + " meses de datos; "
                    + "la valoración por rentabilidad es solo orientativa.");
        }
        return frasesFGEM;
    }

    private static BigDecimal dineroFGEM(BigDecimal valorFGEM) {
        return valorFGEM.setScale(2, RoundingMode.HALF_UP);
    }
}
