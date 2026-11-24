package com.businessanalyzer.analytics.patrimonio;

import com.businessanalyzer.model.ActivoFGEM;
import com.businessanalyzer.model.CategoriaActivoFGEM;
import com.businessanalyzer.model.CategoriaDeudaFGEM;
import com.businessanalyzer.model.DeudaFGEM;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Balance general simplificado a una fecha de corte:
 * <pre>
 *   Activo corriente      (caja y bancos, cuentas por cobrar, inventario, otros)
 * + Activo no corriente   (bienes a su valor en libros, ya descontada la depreciación)
 * = Total activo
 *   Pasivo corriente      (lo que vence en los próximos 12 meses)
 * + Pasivo no corriente   (el resto de las deudas)
 * = Total pasivo
 *   Patrimonio = Total activo − Total pasivo
 * </pre>
 * El inventario se valoriza con el stock actual de los productos por su costo unitario. El efectivo es el
 * saldo calculado de las cuentas de tesorería; las ventas y compras al crédito pendientes se suman como
 * cuentas por cobrar y por pagar.
 *
 * @param efectivoFGEM              saldo de las cuentas de tesorería (Fase 22)
 * @param porCobrarFGEM             ventas al crédito pendientes de cobro (Fase 22)
 * @param porPagarFGEM              compras al crédito pendientes de pago (Fase 22)
 * @param depreciacionAcumuladaFGEM depreciación de todos los activos fijos hasta la fecha de corte
 * @param depreciacionMensualFGEM   depreciación que generan los activos fijos cada mes
 * @param cuotasMensualesFGEM       suma de las cuotas mensuales de las deudas pendientes
 * @param ganadoFGEM                valor de mercado del hato (Fase 15); cero si no hay ganado
 * @param cultivosFGEM              costos acumulados de las campañas agrícolas en curso (Fase 16)
 * @param activosRegistradosFGEM    cantidad de activos registrados (sin contar el inventario)
 * @param deudasPendientesFGEM      cantidad de deudas con saldo
 */
public record BalanceGeneralFGEM(LocalDate corteFGEM,
                             List<PartidaFGEM> activoCorrienteFGEM,
                             List<PartidaFGEM> activoNoCorrienteFGEM,
                             List<PartidaFGEM> pasivoCorrienteFGEM,
                             List<PartidaFGEM> pasivoNoCorrienteFGEM,
                             BigDecimal inventarioFGEM,
                             BigDecimal ganadoFGEM,
                             BigDecimal cultivosFGEM,
                             BigDecimal depreciacionAcumuladaFGEM,
                             BigDecimal depreciacionMensualFGEM,
                             BigDecimal cuotasMensualesFGEM,
                             int activosRegistradosFGEM,
                             int deudasPendientesFGEM,
                             BigDecimal efectivoFGEM,
                             BigDecimal porCobrarFGEM,
                             BigDecimal porPagarFGEM) {

    public static final String INVENTARIO_FGEM = "Inventario de mercadería";
    public static final String EFECTIVO_FGEM = "Efectivo (cuentas de tesorería)";
    public static final String POR_COBRAR_FGEM = "Ventas al crédito por cobrar";
    public static final String POR_PAGAR_FGEM = "Compras al crédito por pagar";
    public static final String GANADO_FGEM = "Ganado (valor de mercado)";
    public static final String CULTIVOS_FGEM = "Cultivos en proceso (al costo)";

    /** Rubro del balance con su monto. */
    public record PartidaFGEM(String conceptoFGEM, BigDecimal montoFGEM) {
    }

    public BalanceGeneralFGEM {
        activoCorrienteFGEM = List.copyOf(activoCorrienteFGEM);
        activoNoCorrienteFGEM = List.copyOf(activoNoCorrienteFGEM);
        pasivoCorrienteFGEM = List.copyOf(pasivoCorrienteFGEM);
        pasivoNoCorrienteFGEM = List.copyOf(pasivoNoCorrienteFGEM);
    }

    /**
     * @param activosFGEM    activos de la empresa (se consideran los vigentes a la fecha de corte)
     * @param inventarioFGEM valor del stock de mercadería
     * @param deudasFGEM     deudas de la empresa (se consideran las pendientes iniciadas hasta la fecha de corte)
     */
    public static BalanceGeneralFGEM deFGEM(LocalDate corteFGEM, Collection<ActivoFGEM> activosFGEM, BigDecimal inventarioFGEM,
                                    Collection<DeudaFGEM> deudasFGEM) {
        return deFGEM(corteFGEM, activosFGEM, inventarioFGEM, BigDecimal.ZERO, deudasFGEM);
    }

    /** Igual que el anterior, sumando el valor del ganado al activo no corriente. */
    public static BalanceGeneralFGEM deFGEM(LocalDate corteFGEM, Collection<ActivoFGEM> activosFGEM, BigDecimal inventarioFGEM,
                                    BigDecimal ganadoFGEM, Collection<DeudaFGEM> deudasFGEM) {
        return deFGEM(corteFGEM, activosFGEM, inventarioFGEM, ganadoFGEM, BigDecimal.ZERO, deudasFGEM);
    }

    /** Igual que el anterior, sumando al activo corriente lo invertido en las campañas agrícolas en curso. */
    public static BalanceGeneralFGEM deFGEM(LocalDate corteFGEM, Collection<ActivoFGEM> activosFGEM, BigDecimal inventarioFGEM,
                                    BigDecimal ganadoFGEM, BigDecimal cultivosFGEM, Collection<DeudaFGEM> deudasFGEM) {
        return deFGEM(corteFGEM, activosFGEM, inventarioFGEM, ganadoFGEM, cultivosFGEM, deudasFGEM, BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO);
    }

    /**
     * Balance completo, con el dinero de las cuentas de tesorería y las ventas y compras al crédito pendientes.
     *
     * @param efectivoFGEM  saldo de las cuentas (puede ser negativo si una cuenta quedó sobregirada)
     * @param porCobrarFGEM ventas al crédito pendientes de cobro
     * @param porPagarFGEM  compras al crédito pendientes de pago (se consideran de corto plazo)
     */
    public static BalanceGeneralFGEM deFGEM(LocalDate corteFGEM, Collection<ActivoFGEM> activosFGEM, BigDecimal inventarioFGEM,
                                    BigDecimal ganadoFGEM, BigDecimal cultivosFGEM, Collection<DeudaFGEM> deudasFGEM,
                                    BigDecimal efectivoFGEM, BigDecimal porCobrarFGEM, BigDecimal porPagarFGEM) {
        BigDecimal dineroFGEM = efectivoFGEM == null ? BigDecimal.ZERO : efectivoFGEM;
        BigDecimal cobrarFGEM = porCobrarFGEM == null ? BigDecimal.ZERO : porCobrarFGEM.max(BigDecimal.ZERO);
        BigDecimal pagarFGEM = porPagarFGEM == null ? BigDecimal.ZERO : porPagarFGEM.max(BigDecimal.ZERO);
        Map<CategoriaActivoFGEM, BigDecimal> porCategoriaFGEM = new EnumMap<>(CategoriaActivoFGEM.class);
        BigDecimal depAcumuladaFGEM = BigDecimal.ZERO;
        BigDecimal depMensualFGEM = BigDecimal.ZERO;
        int registradosFGEM = 0;
        for (ActivoFGEM aFGEM : activosFGEM) {
            if (!aFGEM.vigenteEnFGEM(corteFGEM)) {
                continue;
            }
            registradosFGEM++;
            DepreciacionFGEM dFGEM = DepreciacionFGEM.deFGEM(aFGEM, corteFGEM);
            depAcumuladaFGEM = depAcumuladaFGEM.add(dFGEM.acumuladaFGEM());
            depMensualFGEM = depMensualFGEM.add(dFGEM.mensualFGEM());
            porCategoriaFGEM.merge(aFGEM.categoriaFGEM(), dFGEM.valorEnLibrosFGEM(), BigDecimal::add);
        }

        List<PartidaFGEM> corrienteFGEM = new ArrayList<>();
        List<PartidaFGEM> noCorrienteFGEM = new ArrayList<>();
        BigDecimal stockFGEM = inventarioFGEM == null ? BigDecimal.ZERO : inventarioFGEM.max(BigDecimal.ZERO);
        BigDecimal enProcesoFGEM = cultivosFGEM == null ? BigDecimal.ZERO : cultivosFGEM.max(BigDecimal.ZERO);
        if (dineroFGEM.signum() != 0) {
            corrienteFGEM.add(new PartidaFGEM(EFECTIVO_FGEM, dineroFGEM));
        }
        for (CategoriaActivoFGEM categoriaFGEM : CategoriaActivoFGEM.values()) {
            if (categoriaFGEM == CategoriaActivoFGEM.CUENTAS_POR_COBRAR && cobrarFGEM.signum() > 0) {
                corrienteFGEM.add(new PartidaFGEM(POR_COBRAR_FGEM, cobrarFGEM));
            }
            if (categoriaFGEM == CategoriaActivoFGEM.OTROS_CORRIENTES && stockFGEM.signum() > 0) {
                corrienteFGEM.add(new PartidaFGEM(INVENTARIO_FGEM, stockFGEM));
            }
            if (categoriaFGEM == CategoriaActivoFGEM.OTROS_CORRIENTES && enProcesoFGEM.signum() > 0) {
                corrienteFGEM.add(new PartidaFGEM(CULTIVOS_FGEM, enProcesoFGEM));
            }
            BigDecimal montoFGEM = porCategoriaFGEM.get(categoriaFGEM);
            if (montoFGEM != null) {
                (categoriaFGEM.corrienteFGEM() ? corrienteFGEM : noCorrienteFGEM).add(new PartidaFGEM(categoriaFGEM.descripcionFGEM(), montoFGEM));
            }
        }
        BigDecimal hatoFGEM = ganadoFGEM == null ? BigDecimal.ZERO : ganadoFGEM.max(BigDecimal.ZERO);
        if (hatoFGEM.signum() > 0) {
            noCorrienteFGEM.add(new PartidaFGEM(GANADO_FGEM, hatoFGEM));
        }

        Map<CategoriaDeudaFGEM, BigDecimal> cortoPlazoFGEM = new EnumMap<>(CategoriaDeudaFGEM.class);
        Map<CategoriaDeudaFGEM, BigDecimal> largoPlazoFGEM = new EnumMap<>(CategoriaDeudaFGEM.class);
        BigDecimal cuotasFGEM = BigDecimal.ZERO;
        int pendientesFGEM = 0;
        for (DeudaFGEM dFGEM : deudasFGEM) {
            if (!dFGEM.pendienteFGEM() || dFGEM.fechaFGEM().isAfter(corteFGEM)) {
                continue;
            }
            pendientesFGEM++;
            BigDecimal cortoFGEM = porcionCorrienteFGEM(dFGEM, corteFGEM);
            cortoPlazoFGEM.merge(dFGEM.categoriaFGEM(), cortoFGEM, BigDecimal::add);
            BigDecimal largoFGEM = dFGEM.saldoFGEM().subtract(cortoFGEM);
            if (largoFGEM.signum() > 0) {
                largoPlazoFGEM.merge(dFGEM.categoriaFGEM(), largoFGEM, BigDecimal::add);
            }
            if (dFGEM.cuotaMensualFGEM() != null) {
                cuotasFGEM = cuotasFGEM.add(dFGEM.cuotaMensualFGEM());
            }
        }
        List<PartidaFGEM> pasivoCorrienteFGEM = new ArrayList<>();
        if (pagarFGEM.signum() > 0) {
            pasivoCorrienteFGEM.add(new PartidaFGEM(POR_PAGAR_FGEM, pagarFGEM));
        }
        pasivoCorrienteFGEM.addAll(partidasFGEM(cortoPlazoFGEM));
        return new BalanceGeneralFGEM(corteFGEM, corrienteFGEM, noCorrienteFGEM, pasivoCorrienteFGEM, partidasFGEM(largoPlazoFGEM),
                stockFGEM, hatoFGEM, enProcesoFGEM, depAcumuladaFGEM, depMensualFGEM, cuotasFGEM, registradosFGEM, pendientesFGEM, dineroFGEM, cobrarFGEM,
                pagarFGEM);
    }

    /**
     * Parte del saldo que se pagará en los próximos 12 meses. Sin vencimiento, o si vence dentro
     * de un año, es todo el saldo; si vence después, se supone que el capital se paga en partes
     * iguales cada mes hasta el vencimiento.
     */
    public static BigDecimal porcionCorrienteFGEM(DeudaFGEM dFGEM, LocalDate corteFGEM) {
        BigDecimal saldoFGEM = dFGEM.saldoFGEM();
        if (dFGEM.vencimientoFGEM() == null) {
            return saldoFGEM;
        }
        long mesesFGEM = mesesHastaFGEM(corteFGEM, dFGEM.vencimientoFGEM());
        if (mesesFGEM <= 12) {
            return saldoFGEM;
        }
        return saldoFGEM.multiply(BigDecimal.valueOf(12)).divide(BigDecimal.valueOf(mesesFGEM), 2, RoundingMode.HALF_UP);
    }

    /** Meses que faltan hasta la fecha (redondeado hacia arriba); 0 si ya pasó. */
    static long mesesHastaFGEM(LocalDate desdeFGEM, LocalDate hastaFGEM) {
        if (!hastaFGEM.isAfter(desdeFGEM)) {
            return 0;
        }
        long mesesFGEM = ChronoUnit.MONTHS.between(desdeFGEM, hastaFGEM);
        return desdeFGEM.plusMonths(mesesFGEM).isBefore(hastaFGEM) ? mesesFGEM + 1 : mesesFGEM;
    }

    private static List<PartidaFGEM> partidasFGEM(Map<CategoriaDeudaFGEM, BigDecimal> montosFGEM) {
        return montosFGEM.entrySet().stream()
                .filter(eFGEM -> eFGEM.getValue().signum() > 0)
                .map(eFGEM -> new PartidaFGEM(eFGEM.getKey().descripcionFGEM(), eFGEM.getValue()))
                .toList();
    }

    public BigDecimal totalActivoCorrienteFGEM() {
        return sumaFGEM(activoCorrienteFGEM);
    }

    public BigDecimal totalActivoNoCorrienteFGEM() {
        return sumaFGEM(activoNoCorrienteFGEM);
    }

    public BigDecimal totalActivoFGEM() {
        return totalActivoCorrienteFGEM().add(totalActivoNoCorrienteFGEM());
    }

    public BigDecimal totalPasivoCorrienteFGEM() {
        return sumaFGEM(pasivoCorrienteFGEM);
    }

    public BigDecimal totalPasivoNoCorrienteFGEM() {
        return sumaFGEM(pasivoNoCorrienteFGEM);
    }

    public BigDecimal totalPasivoFGEM() {
        return totalPasivoCorrienteFGEM().add(totalPasivoNoCorrienteFGEM());
    }

    /** Lo que realmente pertenece a los dueños: activos − deudas. Puede ser negativo. */
    public BigDecimal patrimonioFGEM() {
        return totalActivoFGEM().subtract(totalPasivoFGEM());
    }

    /** No hay activos ni deudas registrados: no se puede evaluar la situación patrimonial. */
    public boolean sinRegistrosFGEM() {
        return !tieneActivosFGEM() && deudasPendientesFGEM == 0;
    }

    /**
     * Hay activos registrados, ganado valorizado, dinero en las cuentas de tesorería (aunque estén en rojo) o
     * ventas por cobrar. El inventario solo no cuenta: se calcula del catálogo aunque no se haya registrado nada más.
     */
    public boolean tieneActivosFGEM() {
        return activosRegistradosFGEM > 0 || ganadoFGEM.signum() > 0 || cultivosFGEM.signum() > 0
                || (efectivoFGEM != null && efectivoFGEM.signum() != 0) || (porCobrarFGEM != null && porCobrarFGEM.signum() > 0);
    }

    private static BigDecimal sumaFGEM(List<PartidaFGEM> partidasFGEM) {
        return partidasFGEM.stream().map(PartidaFGEM::montoFGEM).reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
