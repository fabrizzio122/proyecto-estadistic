package com.businessanalyzer.importacion;

import com.businessanalyzer.model.CategoriaFinancieraFGEM;
import com.businessanalyzer.model.ComportamientoFGEM;
import com.businessanalyzer.model.TipoItemFGEM;
import com.businessanalyzer.model.TipoOperacionFGEM;
import com.businessanalyzer.model.UnidadMedidaFGEM;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Filas leídas de un libro de importación (ya convertidas a tipos de Java) y los resultados.
 * El número de fila es el que el usuario ve en Excel (la fila 1 es la de encabezados).
 */
public final class DatosImportacionFGEM {

    private DatosImportacionFGEM() {
    }

    /** Una fila de la hoja «Productos». */
    public record FilaProductoFGEM(int filaFGEM, String codigoFGEM, String nombreFGEM, TipoItemFGEM tipoFGEM, String categoriaFGEM,
                                   UnidadMedidaFGEM unidadFGEM, BigDecimal costoFGEM, BigDecimal precioFGEM, boolean perecibleFGEM) {
    }

    /**
     * Una línea de la hoja «Ventas» o «Compras». Las líneas con la misma fecha y el mismo documento
     * forman una sola operación.
     *
     * @param productoFGEM código o nombre del producto
     * @param precioFGEM   sin impuestos; {@code null} = el precio (o costo) del catálogo
     * @param tasaFGEM     porcentaje de impuesto (18 = 18 %); {@code null} = la tasa habitual de la empresa
     */
    public record FilaOperacionFGEM(int filaFGEM, TipoOperacionFGEM tipoFGEM, LocalDate fechaFGEM, String documentoFGEM, String contraparteFGEM,
                                    String rucFGEM, String productoFGEM, BigDecimal cantidadFGEM, BigDecimal precioFGEM, BigDecimal tasaFGEM) {
    }

    /**
     * Una fila de la hoja «Gastos» (también acepta categorías de ingreso).
     *
     * @param comportamientoFGEM {@code null} = el sugerido por la categoría
     */
    public record FilaMovimientoFGEM(int filaFGEM, LocalDate fechaFGEM, CategoriaFinancieraFGEM categoriaFGEM, String descripcionFGEM,
                                     BigDecimal montoFGEM, ComportamientoFGEM comportamientoFGEM, String comprobanteFGEM) {
    }

    /** Un problema en una fila concreta; {@code filaFGEM} = 0 si afecta a toda la hoja o al archivo. */
    public record ErrorFilaFGEM(String hojaFGEM, int filaFGEM, String mensajeFGEM) {

        public String ubicacionFGEM() {
            return filaFGEM > 0 ? hojaFGEM + ", fila " + filaFGEM : hojaFGEM;
        }

        @Override
        public String toString() {
            return ubicacionFGEM() + ": " + mensajeFGEM;
        }
    }

    /** Todo lo leído del libro, más los errores de formato encontrados al leerlo. */
    public record ContenidoFGEM(List<FilaProductoFGEM> productosFGEM, List<FilaOperacionFGEM> operacionesFGEM,
                                List<FilaMovimientoFGEM> movimientosFGEM, List<ErrorFilaFGEM> erroresFGEM) {

        public boolean vacioFGEM() {
            return productosFGEM.isEmpty() && operacionesFGEM.isEmpty() && movimientosFGEM.isEmpty();
        }

        public boolean tieneFGEM(TipoOperacionFGEM tipoFGEM) {
            return operacionesFGEM.stream().anyMatch(oFGEM -> oFGEM.tipoFGEM() == tipoFGEM);
        }
    }

    /**
     * Resultado de revisar o importar un libro.
     *
     * @param aplicadoFGEM {@code true} si los datos quedaron guardados; {@code false} si fue solo una revisión
     *                     o si hubo errores (en ese caso no se guarda nada)
     */
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
