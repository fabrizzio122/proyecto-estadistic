package com.businessanalyzer.repository;

import com.businessanalyzer.model.CampaniaFGEM;
import com.businessanalyzer.model.CosechaFGEM;
import com.businessanalyzer.model.CostoCampaniaFGEM;
import com.businessanalyzer.model.DestinoCosechaFGEM;
import com.businessanalyzer.model.MovimientoGanadoFGEM;
import com.businessanalyzer.model.ProduccionGanaderaFGEM;
import com.businessanalyzer.model.TipoMovimientoGanadoFGEM;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Ventas y costos de la producción agropecuaria, para que el estado de resultados y todos los
 * análisis los incluyan (el productor no registra estas ventas en «Ventas y compras»):
 * <ul>
 *   <li><b>Venta de animales:</b> ventas del hato con su valor total, en la fecha de la venta.</li>
 *   <li><b>Leche, fibra y lana:</b> los registros de producción con precio se consideran vendidos.</li>
 *   <li><b>Cosechas:</b> las cosechas vendidas con precio. Su costo es el costo por kilo de la campaña.</li>
 *   <li><b>Resto del costo de las campañas:</b> al cerrarlas, lo que no se asignó a ventas (semilla,
 *       forraje para los animales, merma, campañas perdidas) se reconoce como costo. Lo que consume la
 *       familia no es un costo del negocio y no se incluye.</li>
 * </ul>
 */
public class VentasAgroRepositoryFGEM {

    /**
     * Partida agropecuaria del estado de resultados.
     *
     * @param claveFGEM    identificador del «producto» para la rentabilidad por producto (negativo)
     * @param cantidadFGEM cabezas, litros o kilos vendidos (0 en los costos de cierre)
     * @param esVentaFGEM  {@code true} si es una venta; {@code false} si es solo costo
     */
    public record PartidaFGEM(LocalDate fechaFGEM, long claveFGEM, String nombreFGEM, BigDecimal cantidadFGEM, BigDecimal ventaFGEM,
                          BigDecimal costoFGEM, boolean esVentaFGEM) {
    }

    private final GanaderiaRepositoryFGEM ganaderiaFGEM = new GanaderiaRepositoryFGEM();
    private final AgriculturaRepositoryFGEM agriculturaFGEM = new AgriculturaRepositoryFGEM();

    public List<PartidaFGEM> partidasFGEM(Connection cFGEM, long empresaIdFGEM, LocalDate desdeFGEM, LocalDate hastaFGEM) throws SQLException {
        List<PartidaFGEM> partidasFGEM = new ArrayList<>();

        for (MovimientoGanadoFGEM mFGEM : ganaderiaFGEM.listarMovimientosFGEM(cFGEM, empresaIdFGEM, hastaFGEM, false)) {
            if (mFGEM.tipoFGEM() == TipoMovimientoGanadoFGEM.VENTA && mFGEM.montoFGEM() != null && !mFGEM.fechaFGEM().isBefore(desdeFGEM)) {
                partidasFGEM.add(new PartidaFGEM(mFGEM.fechaFGEM(), -(1000L + mFGEM.especieFGEM().ordinal()),
                        "Venta de " + mFGEM.especieFGEM().descripcionFGEM().toLowerCase(), BigDecimal.valueOf(mFGEM.cantidadFGEM()),
                        mFGEM.montoFGEM(), BigDecimal.ZERO, true));
            }
        }

        for (ProduccionGanaderaFGEM pFGEM : ganaderiaFGEM.listarProduccionFGEM(cFGEM, empresaIdFGEM, desdeFGEM, hastaFGEM, false)) {
            if (pFGEM.precioUnitarioFGEM() != null && pFGEM.precioUnitarioFGEM().signum() > 0) {
                partidasFGEM.add(new PartidaFGEM(pFGEM.fechaFGEM(), -(2000L + pFGEM.productoFGEM().ordinal() * 10L + pFGEM.especieFGEM().ordinal()),
                        pFGEM.productoFGEM().descripcionFGEM() + " (" + pFGEM.especieFGEM().descripcionFGEM().toLowerCase() + ")",
                        pFGEM.cantidadFGEM(), pFGEM.valorFGEM(), BigDecimal.ZERO, true));
            }
        }

        List<CampaniaFGEM> campaniasFGEM = agriculturaFGEM.listarCampaniasFGEM(cFGEM, empresaIdFGEM, false);
        if (campaniasFGEM.isEmpty()) {
            return partidasFGEM;
        }
        List<CostoCampaniaFGEM> costosFGEM = agriculturaFGEM.listarCostosFGEM(cFGEM, empresaIdFGEM, null, false);
        List<CosechaFGEM> cosechasFGEM = agriculturaFGEM.listarCosechasFGEM(cFGEM, empresaIdFGEM, null, false);
        for (CampaniaFGEM kFGEM : campaniasFGEM) {
            BigDecimal costoTotalFGEM = costosFGEM.stream().filter(xFGEM -> xFGEM.campaniaIdFGEM() == kFGEM.idFGEM())
                    .map(CostoCampaniaFGEM::montoFGEM).reduce(BigDecimal.ZERO, BigDecimal::add);
            List<CosechaFGEM> deCampaniaFGEM = cosechasFGEM.stream().filter(hFGEM -> hFGEM.campaniaIdFGEM() == kFGEM.idFGEM()).toList();
            BigDecimal produccionFGEM = sumaFGEM(deCampaniaFGEM, null);
            BigDecimal familiaFGEM = sumaFGEM(deCampaniaFGEM, DestinoCosechaFGEM.AUTOCONSUMO);
            BigDecimal kilosNegocioFGEM = produccionFGEM.subtract(familiaFGEM);
            BigDecimal costoPorKiloFGEM = kilosNegocioFGEM.signum() > 0
                    ? costoTotalFGEM.divide(kilosNegocioFGEM, 6, RoundingMode.HALF_UP) : BigDecimal.ZERO;
            long claveFGEM = -(3000L + kFGEM.cultivoFGEM().ordinal());
            BigDecimal vendidosConPrecioFGEM = BigDecimal.ZERO;
            for (CosechaFGEM hFGEM : deCampaniaFGEM) {
                if (hFGEM.destinoFGEM() != DestinoCosechaFGEM.VENTA || hFGEM.precioFGEM() == null) {
                    continue;
                }
                vendidosConPrecioFGEM = vendidosConPrecioFGEM.add(hFGEM.cantidadFGEM());
                if (!hFGEM.fechaFGEM().isBefore(desdeFGEM) && !hFGEM.fechaFGEM().isAfter(hastaFGEM)) {
                    partidasFGEM.add(new PartidaFGEM(hFGEM.fechaFGEM(), claveFGEM, kFGEM.cultivoFGEM().descripcionFGEM(), hFGEM.cantidadFGEM(),
                            dineroFGEM(hFGEM.cantidadFGEM().multiply(hFGEM.precioFGEM())), dineroFGEM(hFGEM.cantidadFGEM().multiply(costoPorKiloFGEM)), true));
                }
            }
            if (kFGEM.estadoFGEM().cerradaFGEM() && kFGEM.fechaCierreFGEM() != null
                    && !kFGEM.fechaCierreFGEM().isBefore(desdeFGEM) && !kFGEM.fechaCierreFGEM().isAfter(hastaFGEM)) {
                BigDecimal costoNegocioFGEM = produccionFGEM.signum() > 0
                        ? costoTotalFGEM.multiply(kilosNegocioFGEM).divide(produccionFGEM, 6, RoundingMode.HALF_UP) : costoTotalFGEM;
                BigDecimal restanteFGEM = dineroFGEM(costoNegocioFGEM.subtract(vendidosConPrecioFGEM.multiply(costoPorKiloFGEM)));
                if (restanteFGEM.signum() > 0) {
                    partidasFGEM.add(new PartidaFGEM(kFGEM.fechaCierreFGEM(), claveFGEM, kFGEM.cultivoFGEM().descripcionFGEM(), BigDecimal.ZERO,
                            BigDecimal.ZERO, restanteFGEM, false));
                }
            }
        }
        return partidasFGEM;
    }

    private static BigDecimal sumaFGEM(List<CosechaFGEM> cosechasFGEM, DestinoCosechaFGEM destinoFGEM) {
        return cosechasFGEM.stream().filter(hFGEM -> destinoFGEM == null || hFGEM.destinoFGEM() == destinoFGEM)
                .map(CosechaFGEM::cantidadFGEM).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static BigDecimal dineroFGEM(BigDecimal valorFGEM) {
        return valorFGEM.setScale(2, RoundingMode.HALF_UP);
    }
}
