package com.businessanalyzer.importacion;

import com.businessanalyzer.model.CategoriaFinancieraFGEM;
import com.businessanalyzer.model.ComportamientoFGEM;
import com.businessanalyzer.model.TipoItemFGEM;
import com.businessanalyzer.model.TipoOperacionFGEM;
import com.businessanalyzer.model.UnidadMedidaFGEM;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public final class DatosImportacionFGEM {

    private DatosImportacionFGEM() {
    }

    public record FilaProductoFGEM(int filaFGEM, String codigoFGEM, String nombreFGEM, TipoItemFGEM tipoFGEM, String categoriaFGEM,
                                   UnidadMedidaFGEM unidadFGEM, BigDecimal costoFGEM, BigDecimal precioFGEM, boolean perecibleFGEM) {
    }

    public record FilaOperacionFGEM(int filaFGEM, TipoOperacionFGEM tipoFGEM, LocalDate fechaFGEM, String documentoFGEM, String contraparteFGEM,
                                    String rucFGEM, String productoFGEM, BigDecimal cantidadFGEM, BigDecimal precioFGEM, BigDecimal tasaFGEM) {
    }

    public record FilaMovimientoFGEM(int filaFGEM, LocalDate fechaFGEM, CategoriaFinancieraFGEM categoriaFGEM, String descripcionFGEM,
                                     BigDecimal montoFGEM, ComportamientoFGEM comportamientoFGEM, String comprobanteFGEM) {
    }

    public record ErrorFilaFGEM(String hojaFGEM, int filaFGEM, String mensajeFGEM) {

        public String ubicacionFGEM() {
            return filaFGEM > 0 ? hojaFGEM + ", fila " + filaFGEM : hojaFGEM;
        }

        @Override
        public String toString() {
            return ubicacionFGEM() + ": " + mensajeFGEM;
        }
    }

    public record ContenidoFGEM(List<FilaProductoFGEM> productosFGEM, List<FilaOperacionFGEM> operacionesFGEM,
                                List<FilaMovimientoFGEM> movimientosFGEM, List<ErrorFilaFGEM> erroresFGEM) {

        public boolean vacioFGEM() {
            return productosFGEM.isEmpty() && operacionesFGEM.isEmpty() && movimientosFGEM.isEmpty();
        }

        public boolean tieneFGEM(TipoOperacionFGEM tipoFGEM) {
            return operacionesFGEM.stream().anyMatch(oFGEM -> oFGEM.tipoFGEM() == tipoFGEM);
        }
    }

    public record ResultadoFGEM(boolean aplicadoFGEM, int categoriasFGEM, int productosFGEM, int ventasFGEM, int comprasFGEM,
                                int gastosFGEM, int lineasFGEM, LocalDate desdeFGEM, LocalDate hastaFGEM,
                                List<ErrorFilaFGEM> erroresFGEM, List<String> advertenciasFGEM) {

        public boolean sinErroresFGEM() {
            return erroresFGEM.isEmpty();
        }

        public int registrosFGEM() {
            return categoriasFGEM + productosFGEM + ventasFGEM + comprasFGEM + gastosFGEM;
        }
    }
}
