package com.businessanalyzer.repository;

import com.businessanalyzer.model.Campania;
import com.businessanalyzer.model.Cosecha;
import com.businessanalyzer.model.CostoCampania;
import com.businessanalyzer.model.DestinoCosecha;
import com.businessanalyzer.model.MovimientoGanado;
import com.businessanalyzer.model.ProduccionGanadera;
import com.businessanalyzer.model.TipoMovimientoGanado;

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
public class VentasAgroRepository {

    /**
     * Partida agropecuaria del estado de resultados.
     *
     * @param clave    identificador del «producto» para la rentabilidad por producto (negativo)
     * @param cantidad cabezas, litros o kilos vendidos (0 en los costos de cierre)
     * @param esVenta  {@code true} si es una venta; {@code false} si es solo costo
     */
    public record Partida(LocalDate fecha, long clave, String nombre, BigDecimal cantidad, BigDecimal venta,
                          BigDecimal costo, boolean esVenta) {
    }

    private final GanaderiaRepository ganaderia = new GanaderiaRepository();
    private final AgriculturaRepository agricultura = new AgriculturaRepository();

    public List<Partida> partidas(Connection c, long empresaId, LocalDate desde, LocalDate hasta) throws SQLException {
        List<Partida> partidas = new ArrayList<>();

        for (MovimientoGanado m : ganaderia.listarMovimientos(c, empresaId, hasta, false)) {
            if (m.tipo() == TipoMovimientoGanado.VENTA && m.monto() != null && !m.fecha().isBefore(desde)) {
                partidas.add(new Partida(m.fecha(), -(1000L + m.especie().ordinal()),
                        "Venta de " + m.especie().descripcion().toLowerCase(), BigDecimal.valueOf(m.cantidad()),
                        m.monto(), BigDecimal.ZERO, true));
            }
        }

        for (ProduccionGanadera p : ganaderia.listarProduccion(c, empresaId, desde, hasta, false)) {
            if (p.precioUnitario() != null && p.precioUnitario().signum() > 0) {
                partidas.add(new Partida(p.fecha(), -(2000L + p.producto().ordinal() * 10L + p.especie().ordinal()),
                        p.producto().descripcion() + " (" + p.especie().descripcion().toLowerCase() + ")",
                        p.cantidad(), p.valor(), BigDecimal.ZERO, true));
            }
        }

        List<Campania> campanias = agricultura.listarCampanias(c, empresaId, false);
        if (campanias.isEmpty()) {
            return partidas;
        }
        List<CostoCampania> costos = agricultura.listarCostos(c, empresaId, null, false);
        List<Cosecha> cosechas = agricultura.listarCosechas(c, empresaId, null, false);
        for (Campania k : campanias) {
            BigDecimal costoTotal = costos.stream().filter(x -> x.campaniaId() == k.id())
                    .map(CostoCampania::monto).reduce(BigDecimal.ZERO, BigDecimal::add);
            List<Cosecha> deCampania = cosechas.stream().filter(h -> h.campaniaId() == k.id()).toList();
            BigDecimal produccion = suma(deCampania, null);
            BigDecimal familia = suma(deCampania, DestinoCosecha.AUTOCONSUMO);
            BigDecimal kilosNegocio = produccion.subtract(familia);
            BigDecimal costoPorKilo = kilosNegocio.signum() > 0
                    ? costoTotal.divide(kilosNegocio, 6, RoundingMode.HALF_UP) : BigDecimal.ZERO;
            long clave = -(3000L + k.cultivo().ordinal());
            BigDecimal vendidosConPrecio = BigDecimal.ZERO;
            for (Cosecha h : deCampania) {
                if (h.destino() != DestinoCosecha.VENTA || h.precio() == null) {
                    continue;
                }
                vendidosConPrecio = vendidosConPrecio.add(h.cantidad());
                if (!h.fecha().isBefore(desde) && !h.fecha().isAfter(hasta)) {
                    partidas.add(new Partida(h.fecha(), clave, k.cultivo().descripcion(), h.cantidad(),
                            dinero(h.cantidad().multiply(h.precio())), dinero(h.cantidad().multiply(costoPorKilo)), true));
                }
            }
            if (k.estado().cerrada() && k.fechaCierre() != null
                    && !k.fechaCierre().isBefore(desde) && !k.fechaCierre().isAfter(hasta)) {
                BigDecimal costoNegocio = produccion.signum() > 0
                        ? costoTotal.multiply(kilosNegocio).divide(produccion, 6, RoundingMode.HALF_UP) : costoTotal;
                BigDecimal restante = dinero(costoNegocio.subtract(vendidosConPrecio.multiply(costoPorKilo)));
                if (restante.signum() > 0) {
                    partidas.add(new Partida(k.fechaCierre(), clave, k.cultivo().descripcion(), BigDecimal.ZERO,
                            BigDecimal.ZERO, restante, false));
                }
            }
        }
        return partidas;
    }

    private static BigDecimal suma(List<Cosecha> cosechas, DestinoCosecha destino) {
        return cosechas.stream().filter(h -> destino == null || h.destino() == destino)
                .map(Cosecha::cantidad).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static BigDecimal dinero(BigDecimal valor) {
        return valor.setScale(2, RoundingMode.HALF_UP);
    }
}
