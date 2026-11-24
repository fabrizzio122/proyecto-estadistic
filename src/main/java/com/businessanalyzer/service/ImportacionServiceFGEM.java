package com.businessanalyzer.service;

import com.businessanalyzer.config.DatabaseFGEM;
import com.businessanalyzer.exception.DataAccessExceptionFGEM;
import com.businessanalyzer.exception.StockInsuficienteExceptionFGEM;
import com.businessanalyzer.exception.ValidacionExceptionFGEM;
import com.businessanalyzer.importacion.DatosImportacionFGEM.ContenidoFGEM;
import com.businessanalyzer.importacion.DatosImportacionFGEM.ErrorFilaFGEM;
import com.businessanalyzer.importacion.DatosImportacionFGEM.FilaMovimientoFGEM;
import com.businessanalyzer.importacion.DatosImportacionFGEM.FilaOperacionFGEM;
import com.businessanalyzer.importacion.DatosImportacionFGEM.FilaProductoFGEM;
import com.businessanalyzer.importacion.DatosImportacionFGEM.ResultadoFGEM;
import com.businessanalyzer.importacion.LectorImportacionFGEM;
import com.businessanalyzer.importacion.PlantillaImportacionFGEM;
import com.businessanalyzer.importacion.PlantillaImportacionFGEM.HojaFGEM;
import com.businessanalyzer.model.CategoriaFGEM;
import com.businessanalyzer.model.CategoriaFinancieraFGEM;
import com.businessanalyzer.model.DatosCategoriaFGEM;
import com.businessanalyzer.model.DatosMovimientoFGEM;
import com.businessanalyzer.model.DatosOperacionFGEM;
import com.businessanalyzer.model.DatosOperacionFGEM.DatosLineaFGEM;
import com.businessanalyzer.model.DatosProductoFGEM;
import com.businessanalyzer.model.EmpresaFGEM;
import com.businessanalyzer.model.MovimientoFinancieroFGEM;
import com.businessanalyzer.model.OperacionFGEM;
import com.businessanalyzer.model.PermisoFGEM;
import com.businessanalyzer.model.ProductoFGEM;
import com.businessanalyzer.model.TipoOperacionFGEM;
import com.businessanalyzer.seguridad.SesionFGEM;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Savepoint;
import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Importación de productos, ventas, compras y gastos desde un libro de Excel a la empresa activa.
 * <p>
 * Reglas:
 * <ul>
 *     <li>Cada fila pasa por los mismos servicios que el registro manual: mismas validaciones, permisos,
 *         numeración (V-000001, C-000001…), cálculo de impuestos, historial de precios y tesorería.</li>
 *     <li>Todo ocurre en una sola transacción. Si alguna fila tiene un error no se guarda nada y se
 *         informan todos los errores, con su hoja y fila, para corregirlos de una vez.</li>
 *     <li>{@link #revisarFGEM} hace exactamente lo mismo pero siempre deshace los cambios: es la vista
 *         previa que se muestra antes de confirmar.</li>
 *     <li>Las ventas y compras se registran por orden de fecha (las compras antes que las ventas del mismo
 *         día) para que el stock y los costos evolucionen como en la realidad.</li>
 *     <li>Si una venta deja el stock en negativo se registra igual y se avisa: lo normal es que falte la
 *         compra del inventario inicial.</li>
 *     <li>Los productos que ya existen (mismo código o nombre) no se modifican: solo se usan.</li>
 * </ul>
 */
public class ImportacionServiceFGEM {

    private static final Logger logFGEM = LoggerFactory.getLogger(ImportacionServiceFGEM.class);
    private static final int MAXIMO_PRODUCTOS_EN_AVISO_FGEM = 8;
    /** Observación de las ventas y compras importadas; sirve para reconocerlas después. */
    static final String OBSERVACION_IMPORTADA_FGEM = "Importado desde Excel";
    /** Fecha mínima que aceptan las operaciones: se busca lo ya registrado desde aquí. */
    private static final LocalDate INICIO_HISTORIAL_FGEM = LocalDate.of(2000, 1, 1);
    private static final java.time.format.DateTimeFormatter FECHA_FGEM = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final DatabaseFGEM databaseFGEM;
    private final CatalogoServiceFGEM catalogoFGEM;
    private final OperacionServiceFGEM operacionesFGEM;
    private final FinanzasServiceFGEM finanzasFGEM;
    private final EmpresaServiceFGEM empresasFGEM;
    private final SesionFGEM sesionFGEM;
    private final Clock relojFGEM;

    public ImportacionServiceFGEM(DatabaseFGEM databaseFGEM, CatalogoServiceFGEM catalogoFGEM, OperacionServiceFGEM operacionesFGEM,
                                  FinanzasServiceFGEM finanzasFGEM, EmpresaServiceFGEM empresasFGEM, SesionFGEM sesionFGEM, Clock relojFGEM) {
        this.databaseFGEM = databaseFGEM;
        this.catalogoFGEM = catalogoFGEM;
        this.operacionesFGEM = operacionesFGEM;
        this.finanzasFGEM = finanzasFGEM;
        this.empresasFGEM = empresasFGEM;
        this.sesionFGEM = sesionFGEM;
        this.relojFGEM = relojFGEM;
    }

    /** Guarda la plantilla vacía o con el ejemplo de una bodega. */
    public void crearPlantillaFGEM(Path destinoFGEM, boolean conEjemploFGEM) {
        try {
            PlantillaImportacionFGEM.crearFGEM(destinoFGEM, conEjemploFGEM, LocalDate.now(relojFGEM));
        } catch (IOException eFGEM) {
            throw new ValidacionExceptionFGEM("No se pudo guardar la plantilla en " + destinoFGEM
                    + ". Si el archivo está abierto en Excel, ciérralo e inténtalo de nuevo.");
        }
    }

    /** Vista previa: procesa todo el archivo y deshace los cambios. */
    public ResultadoFGEM revisarFGEM(Path archivoFGEM) {
        return procesarFGEM(archivoFGEM, false);
    }

    /** Importa el archivo. Si hay algún error no guarda nada y devuelve los errores. */
    public ResultadoFGEM importarFGEM(Path archivoFGEM) {
        ResultadoFGEM resultadoFGEM = procesarFGEM(archivoFGEM, true);
        if (resultadoFGEM.aplicadoFGEM()) {
            logFGEM.info("Importación desde {} por {}: {} productos, {} ventas, {} compras, {} gastos", archivoFGEM.getFileName(),
                    sesionFGEM.usuarioFGEM().nombreUsuarioFGEM(), resultadoFGEM.productosFGEM(), resultadoFGEM.ventasFGEM(),
                    resultadoFGEM.comprasFGEM(), resultadoFGEM.gastosFGEM());
        }
        return resultadoFGEM;
    }

    private ResultadoFGEM procesarFGEM(Path archivoFGEM, boolean aplicarFGEM) {
        empresasFGEM.empresaDeTrabajoFGEM();
        ContenidoFGEM contenidoFGEM = LectorImportacionFGEM.leerFGEM(archivoFGEM);
        if (contenidoFGEM.vacioFGEM() && contenidoFGEM.erroresFGEM().isEmpty()) {
            throw new ValidacionExceptionFGEM("El archivo no tiene datos para importar: las hojas están vacías.");
        }
        exigirPermisosFGEM(contenidoFGEM);
        BigDecimal tasaHabitualFGEM = operacionesFGEM.tasaImpuestoSugeridaFGEM();
        try {
            return databaseFGEM.enTransaccionFGEM(cFGEM -> {
                ResultadoFGEM resultadoFGEM = new ProcesoFGEM(cFGEM, contenidoFGEM, tasaHabitualFGEM).ejecutarFGEM(aplicarFGEM);
                if (!resultadoFGEM.aplicadoFGEM()) {
                    throw new DeshacerFGEM(resultadoFGEM);
                }
                return resultadoFGEM;
            });
        } catch (DeshacerFGEM eFGEM) {
            return eFGEM.resultadoFGEM;
        }
    }

    private void exigirPermisosFGEM(ContenidoFGEM contenidoFGEM) {
        if (!contenidoFGEM.productosFGEM().isEmpty()) {
            sesionFGEM.requerirFGEM(PermisoFGEM.PRODUCTOS_GESTIONAR);
        }
        if (!contenidoFGEM.operacionesFGEM().isEmpty()) {
            sesionFGEM.requerirFGEM(PermisoFGEM.OPERACIONES_GESTIONAR);
        }
        if (!contenidoFGEM.movimientosFGEM().isEmpty()) {
            sesionFGEM.requerirFGEM(PermisoFGEM.FINANZAS_GESTIONAR);
        }
    }

    /** Sale de la transacción para revertirla, llevando el resultado de la revisión. */
    private static final class DeshacerFGEM extends RuntimeException {
        private final transient ResultadoFGEM resultadoFGEM;

        DeshacerFGEM(ResultadoFGEM resultadoFGEM) {
            super(null, null, false, false);
            this.resultadoFGEM = resultadoFGEM;
        }
    }

    /** Una venta o compra armada con las líneas que comparten fecha y documento. */
    private static final class GrupoFGEM {
        final TipoOperacionFGEM tipoFGEM;
        final LocalDate fechaFGEM;
        final String documentoFGEM;
        final FilaOperacionFGEM primeraFGEM;
        final List<FilaOperacionFGEM> filasFGEM = new ArrayList<>();

        GrupoFGEM(FilaOperacionFGEM primeraFGEM) {
            this.tipoFGEM = primeraFGEM.tipoFGEM();
            this.fechaFGEM = primeraFGEM.fechaFGEM();
            this.documentoFGEM = primeraFGEM.documentoFGEM();
            this.primeraFGEM = primeraFGEM;
        }

        String hojaFGEM() {
            return tipoFGEM == TipoOperacionFGEM.VENTA ? HojaFGEM.VENTAS.nombreFGEM() : HojaFGEM.COMPRAS.nombreFGEM();
        }
    }

    /** Un registro a crear en orden cronológico. */
    private record EventoFGEM(LocalDate fechaFGEM, int ordenFGEM, int filaFGEM, Runnable accionFGEM) {
    }

    /** Estado de una importación (o revisión) dentro de su transacción. */
    private final class ProcesoFGEM {
        private final Connection cFGEM;
        private final ContenidoFGEM contenidoFGEM;
        private final BigDecimal tasaHabitualFGEM;
        private final List<ErrorFilaFGEM> erroresFGEM;
        private final List<String> advertenciasFGEM = new ArrayList<>();
        private final Map<String, ProductoFGEM> porCodigoFGEM = new HashMap<>();
        private final Map<String, ProductoFGEM> porNombreFGEM = new HashMap<>();
        private final Map<String, Long> categoriasFGEM = new HashMap<>();
        private final Set<String> sinStockFGEM = new LinkedHashSet<>();
        /** Ventas y compras ya registradas, por tipo, fecha y documento → su número (V-000123). */
        private final Map<String, String> documentosExistentesFGEM = new HashMap<>();
        /** Gastos e ingresos ya registrados, por fecha, categoría, descripción y monto. */
        private final Set<String> movimientosExistentesFGEM = new HashSet<>();
        /** Número de la compra de inventario inicial ya registrada, si la hay. */
        private String inventarioInicialFGEM;
        private int ventasSinStockFGEM;
        private int existentesFGEM;
        private int categoriasNuevasFGEM;
        private int productosNuevosFGEM;
        private int ventasFGEM;
        private int comprasFGEM;
        private int gastosFGEM;
        private int lineasFGEM;
        private LocalDate desdeFGEM;
        private LocalDate hastaFGEM;

        ProcesoFGEM(Connection cFGEM, ContenidoFGEM contenidoFGEM, BigDecimal tasaHabitualFGEM) {
            this.cFGEM = cFGEM;
            this.contenidoFGEM = contenidoFGEM;
            this.tasaHabitualFGEM = tasaHabitualFGEM;
            this.erroresFGEM = new ArrayList<>(contenidoFGEM.erroresFGEM());
        }

        ResultadoFGEM ejecutarFGEM(boolean aplicarFGEM) throws SQLException {
            cargarCatalogoFGEM();
            cargarRegistrosExistentesFGEM();
            importarProductosFGEM();

            List<EventoFGEM> eventosFGEM = new ArrayList<>();
            for (GrupoFGEM grupoFGEM : agruparFGEM()) {
                int ordenFGEM = grupoFGEM.tipoFGEM == TipoOperacionFGEM.COMPRA ? 0 : 1;
                eventosFGEM.add(new EventoFGEM(grupoFGEM.fechaFGEM, ordenFGEM, grupoFGEM.primeraFGEM.filaFGEM(), () -> registrarGrupoFGEM(grupoFGEM)));
            }
            for (FilaMovimientoFGEM mFGEM : contenidoFGEM.movimientosFGEM()) {
                eventosFGEM.add(new EventoFGEM(mFGEM.fechaFGEM(), 2, mFGEM.filaFGEM(), () -> registrarMovimientoFGEM(mFGEM)));
            }
            eventosFGEM.sort(Comparator.comparing(EventoFGEM::fechaFGEM).thenComparingInt(EventoFGEM::ordenFGEM)
                    .thenComparingInt(EventoFGEM::filaFGEM));
            for (EventoFGEM eFGEM : eventosFGEM) {
                eFGEM.accionFGEM().run();
            }

            if (existentesFGEM > 0) {
                advertenciasFGEM.add(existentesFGEM + " producto(s) de la hoja «Productos» ya estaban registrados (mismo código o "
                        + "nombre): se usan tal como están, sin cambiar su costo ni su precio.");
            }
            if (ventasSinStockFGEM > 0) {
                List<String> nombresFGEM = new ArrayList<>(sinStockFGEM);
                String listaFGEM = String.join(", ", nombresFGEM.subList(0, Math.min(MAXIMO_PRODUCTOS_EN_AVISO_FGEM, nombresFGEM.size())))
                        + (nombresFGEM.size() > MAXIMO_PRODUCTOS_EN_AVISO_FGEM ? " y " + (nombresFGEM.size() - MAXIMO_PRODUCTOS_EN_AVISO_FGEM) + " más" : "");
                advertenciasFGEM.add(ventasSinStockFGEM + " venta(s) dejan el stock en negativo (" + listaFGEM + "). Normalmente falta "
                        + "registrar el inventario inicial como una compra; se importarán igual.");
            }
            boolean aplicadoFGEM = aplicarFGEM && erroresFGEM.isEmpty();
            List<ErrorFilaFGEM> ordenadosFGEM = erroresFGEM.stream()
                    .sorted(Comparator.comparingInt((ErrorFilaFGEM eFGEM) -> ordenHojaFGEM(eFGEM.hojaFGEM())).thenComparingInt(ErrorFilaFGEM::filaFGEM))
                    .toList();
            return new ResultadoFGEM(aplicadoFGEM, categoriasNuevasFGEM, productosNuevosFGEM, ventasFGEM, comprasFGEM, gastosFGEM, lineasFGEM,
                    desdeFGEM, hastaFGEM, ordenadosFGEM, List.copyOf(advertenciasFGEM));
        }

        // ------------------------------------------------------------------ catálogo

        private void cargarCatalogoFGEM() {
            for (ProductoFGEM pFGEM : catalogoFGEM.listarProductosFGEM(true)) {
                recordarFGEM(pFGEM);
            }
            for (CategoriaFGEM catFGEM : catalogoFGEM.listarCategoriasFGEM()) {
                categoriasFGEM.put(PlantillaImportacionFGEM.normalizarFGEM(catFGEM.nombreFGEM()), catFGEM.idFGEM());
            }
        }

        /**
         * Lo ya registrado en la empresa, para no duplicarlo si se vuelve a importar un archivo (o una versión
         * más completa del mismo).
         */
        private void cargarRegistrosExistentesFGEM() {
            LocalDate hoyFGEM = LocalDate.now(relojFGEM);
            if (!contenidoFGEM.operacionesFGEM().isEmpty()) {
                int importadasFGEM = 0;
                LocalDate primeraFGEM = null;
                LocalDate ultimaFGEM = null;
                for (TipoOperacionFGEM tipoFGEM : TipoOperacionFGEM.values()) {
                    for (OperacionFGEM oFGEM : operacionesFGEM.listarFGEM(tipoFGEM, INICIO_HISTORIAL_FGEM, hoyFGEM, false)) {
                        if (oFGEM.comprobanteFGEM() != null) {
                            documentosExistentesFGEM.putIfAbsent(claveDocumentoFGEM(tipoFGEM, oFGEM.fechaFGEM(), oFGEM.comprobanteFGEM()),
                                    oFGEM.numeroFGEM());
                        }
                        if (tipoFGEM == TipoOperacionFGEM.COMPRA && inventarioInicialFGEM == null
                                && esInventarioInicialFGEM(oFGEM.contraparteFGEM(), oFGEM.comprobanteFGEM())) {
                            inventarioInicialFGEM = oFGEM.numeroFGEM();
                        }
                        if (OBSERVACION_IMPORTADA_FGEM.equals(oFGEM.observacionesFGEM())) {
                            importadasFGEM++;
                            primeraFGEM = primeraFGEM == null || oFGEM.fechaFGEM().isBefore(primeraFGEM) ? oFGEM.fechaFGEM() : primeraFGEM;
                            ultimaFGEM = ultimaFGEM == null || oFGEM.fechaFGEM().isAfter(ultimaFGEM) ? oFGEM.fechaFGEM() : ultimaFGEM;
                        }
                    }
                }
                if (importadasFGEM > 0) {
                    advertenciasFGEM.add("Esta empresa ya tiene " + importadasFGEM + " venta(s) y compra(s) importadas desde Excel (del "
                            + FECHA_FGEM.format(primeraFGEM) + " al " + FECHA_FGEM.format(ultimaFGEM) + "). Si este archivo repite esos "
                            + "datos se duplicarían: las boletas, comprobantes, inventario inicial y gastos repetidos se marcan como "
                            + "error, pero las filas sin documento no se pueden comprobar.");
                }
            }
            if (!contenidoFGEM.movimientosFGEM().isEmpty()) {
                for (MovimientoFinancieroFGEM mFGEM : finanzasFGEM.listarFGEM(INICIO_HISTORIAL_FGEM, YearMonth.from(hoyFGEM).atEndOfMonth(), false)) {
                    movimientosExistentesFGEM.add(claveMovimientoFGEM(mFGEM.fechaFGEM(), mFGEM.categoriaFGEM(), mFGEM.descripcionFGEM(),
                            mFGEM.montoFGEM()));
                }
            }
        }

        private void recordarFGEM(ProductoFGEM pFGEM) {
            porCodigoFGEM.put(PlantillaImportacionFGEM.normalizarFGEM(pFGEM.codigoFGEM()), pFGEM);
            porNombreFGEM.putIfAbsent(PlantillaImportacionFGEM.normalizarFGEM(pFGEM.nombreFGEM()), pFGEM);
        }

        private ProductoFGEM buscarFGEM(String codigoONombreFGEM) {
            String claveFGEM = PlantillaImportacionFGEM.normalizarFGEM(codigoONombreFGEM);
            ProductoFGEM pFGEM = porCodigoFGEM.get(claveFGEM);
            return pFGEM != null ? pFGEM : porNombreFGEM.get(claveFGEM);
        }

        private void importarProductosFGEM() {
            Map<String, Integer> codigosEnArchivoFGEM = new HashMap<>();
            for (FilaProductoFGEM fFGEM : contenidoFGEM.productosFGEM()) {
                String hojaFGEM = HojaFGEM.PRODUCTOS.nombreFGEM();
                if (fFGEM.codigoFGEM() != null) {
                    Integer anteriorFGEM = codigosEnArchivoFGEM.putIfAbsent(PlantillaImportacionFGEM.normalizarFGEM(fFGEM.codigoFGEM()), fFGEM.filaFGEM());
                    if (anteriorFGEM != null) {
                        erroresFGEM.add(new ErrorFilaFGEM(hojaFGEM, fFGEM.filaFGEM(), "El código «" + fFGEM.codigoFGEM()
                                + "» ya aparece en la fila " + anteriorFGEM + "."));
                        continue;
                    }
                }
                // Ya existe si coincide el código o, aunque el código sea otro, el nombre (por ejemplo, si la primera
                // importación no traía códigos y el sistema asignó P-0001…).
                ProductoFGEM existenteFGEM = fFGEM.codigoFGEM() != null ? porCodigoFGEM.get(PlantillaImportacionFGEM.normalizarFGEM(fFGEM.codigoFGEM())) : null;
                if (existenteFGEM == null) {
                    existenteFGEM = porNombreFGEM.get(PlantillaImportacionFGEM.normalizarFGEM(fFGEM.nombreFGEM()));
                }
                if (existenteFGEM != null) {
                    existentesFGEM++;
                    porNombreFGEM.putIfAbsent(PlantillaImportacionFGEM.normalizarFGEM(fFGEM.nombreFGEM()), existenteFGEM);
                    if (fFGEM.codigoFGEM() != null) {
                        // Las ventas y compras del archivo pueden usar el código del archivo.
                        porCodigoFGEM.putIfAbsent(PlantillaImportacionFGEM.normalizarFGEM(fFGEM.codigoFGEM()), existenteFGEM);
                    }
                    continue;
                }
                // La categoría va en su propio paso: si el producto falla, la categoría ya creada se conserva.
                Long categoriaIdFGEM = null;
                if (fFGEM.categoriaFGEM() != null) {
                    categoriaIdFGEM = intentarFGEM(hojaFGEM, fFGEM.filaFGEM(), () -> categoriaFGEM(fFGEM.categoriaFGEM()));
                    if (categoriaIdFGEM == null) {
                        continue;
                    }
                }
                Long categoriaFinalFGEM = categoriaIdFGEM;
                intentarFGEM(hojaFGEM, fFGEM.filaFGEM(), () -> {
                    ProductoFGEM nuevoFGEM = catalogoFGEM.crearProductoFGEM(new DatosProductoFGEM(fFGEM.tipoFGEM(), fFGEM.codigoFGEM(),
                            fFGEM.nombreFGEM(), null, categoriaFinalFGEM, fFGEM.unidadFGEM(), fFGEM.costoFGEM(), fFGEM.precioFGEM()));
                    recordarFGEM(nuevoFGEM);
                    productosNuevosFGEM++;
                    return null;
                });
            }
        }

        private Long categoriaFGEM(String nombreFGEM) {
            String claveFGEM = PlantillaImportacionFGEM.normalizarFGEM(nombreFGEM);
            Long idFGEM = categoriasFGEM.get(claveFGEM);
            if (idFGEM == null) {
                idFGEM = catalogoFGEM.crearCategoriaFGEM(new DatosCategoriaFGEM(nombreFGEM.trim(), null)).idFGEM();
                categoriasFGEM.put(claveFGEM, idFGEM);
                categoriasNuevasFGEM++;
            }
            return idFGEM;
        }

        // ------------------------------------------------------------------ ventas y compras

        private List<GrupoFGEM> agruparFGEM() {
            Map<String, GrupoFGEM> gruposFGEM = new LinkedHashMap<>();
            for (FilaOperacionFGEM fFGEM : contenidoFGEM.operacionesFGEM()) {
                // Sin documento, cada fila es una operación propia.
                String claveFGEM = fFGEM.documentoFGEM() == null
                        ? fFGEM.tipoFGEM() + "#fila" + fFGEM.filaFGEM()
                        : fFGEM.tipoFGEM() + "|" + fFGEM.fechaFGEM() + "|" + PlantillaImportacionFGEM.normalizarFGEM(fFGEM.documentoFGEM());
                gruposFGEM.computeIfAbsent(claveFGEM, kFGEM -> new GrupoFGEM(fFGEM)).filasFGEM.add(fFGEM);
            }
            return new ArrayList<>(gruposFGEM.values());
        }

        private void registrarGrupoFGEM(GrupoFGEM grupoFGEM) {
            boolean ventaFGEM = grupoFGEM.tipoFGEM == TipoOperacionFGEM.VENTA;
            FilaOperacionFGEM primeraFGEM = grupoFGEM.primeraFGEM;
            String claveDocumentoFGEM = grupoFGEM.documentoFGEM == null ? null
                    : claveDocumentoFGEM(grupoFGEM.tipoFGEM, grupoFGEM.fechaFGEM, grupoFGEM.documentoFGEM);
            if (claveDocumentoFGEM != null && documentosExistentesFGEM.containsKey(claveDocumentoFGEM)) {
                erroresFGEM.add(new ErrorFilaFGEM(grupoFGEM.hojaFGEM(), primeraFGEM.filaFGEM(), (ventaFGEM ? "La boleta «" : "El comprobante «")
                        + grupoFGEM.documentoFGEM + "» del " + FECHA_FGEM.format(grupoFGEM.fechaFGEM) + " ya está registrado como "
                        + documentosExistentesFGEM.get(claveDocumentoFGEM) + ". Parece que estos datos ya se importaron: quita estas filas "
                        + "o anula antes la operación anterior."));
                return;
            }
            boolean inventarioFGEM = !ventaFGEM && esInventarioInicialFGEM(primeraFGEM.contraparteFGEM(), grupoFGEM.documentoFGEM);
            if (inventarioFGEM && inventarioInicialFGEM != null) {
                erroresFGEM.add(new ErrorFilaFGEM(grupoFGEM.hojaFGEM(), primeraFGEM.filaFGEM(), "La empresa ya tiene un inventario inicial ("
                        + inventarioInicialFGEM + "). Registrar otro duplicaría el stock: quita estas filas o anula antes " + inventarioInicialFGEM + "."));
                return;
            }
            Map<Long, DatosLineaFGEM> lineasGrupoFGEM = new LinkedHashMap<>();
            boolean conErroresFGEM = false;
            BigDecimal tasaFGEM = null;
            for (FilaOperacionFGEM fFGEM : grupoFGEM.filasFGEM) {
                ProductoFGEM productoFGEM = buscarFGEM(fFGEM.productoFGEM());
                if (productoFGEM == null) {
                    erroresFGEM.add(new ErrorFilaFGEM(grupoFGEM.hojaFGEM(), fFGEM.filaFGEM(), "El producto «" + fFGEM.productoFGEM()
                            + "» no está en el catálogo ni en la hoja «Productos». Revisa el código o el nombre."));
                    conErroresFGEM = true;
                    continue;
                }
                if (tasaFGEM == null) {
                    tasaFGEM = fFGEM.tasaFGEM();
                }
                BigDecimal precioFGEM = fFGEM.precioFGEM() != null ? fFGEM.precioFGEM()
                        : ventaFGEM ? productoFGEM.precioVentaFGEM() : productoFGEM.costoUnitarioFGEM();
                DatosLineaFGEM anteriorFGEM = lineasGrupoFGEM.get(productoFGEM.idFGEM());
                if (anteriorFGEM == null) {
                    lineasGrupoFGEM.put(productoFGEM.idFGEM(), new DatosLineaFGEM(productoFGEM.idFGEM(), fFGEM.cantidadFGEM(), precioFGEM));
                } else if (anteriorFGEM.precioUnitarioFGEM().compareTo(precioFGEM) == 0) {
                    // El mismo producto dos veces en la misma boleta y al mismo precio: se suman las cantidades.
                    lineasGrupoFGEM.put(productoFGEM.idFGEM(), new DatosLineaFGEM(productoFGEM.idFGEM(),
                            anteriorFGEM.cantidadFGEM().add(fFGEM.cantidadFGEM()), precioFGEM));
                } else {
                    erroresFGEM.add(new ErrorFilaFGEM(grupoFGEM.hojaFGEM(), fFGEM.filaFGEM(), "«" + productoFGEM.nombreFGEM()
                            + "» aparece dos veces en el documento «" + grupoFGEM.documentoFGEM + "» con precios distintos. "
                            + "Usa una sola fila o documentos diferentes."));
                    conErroresFGEM = true;
                }
            }
            if (conErroresFGEM || lineasGrupoFGEM.isEmpty()) {
                return;
            }
            DatosOperacionFGEM datosFGEM = new DatosOperacionFGEM(grupoFGEM.tipoFGEM, grupoFGEM.fechaFGEM, primeraFGEM.contraparteFGEM(),
                    primeraFGEM.rucFGEM(), grupoFGEM.documentoFGEM, tasaFGEM != null ? tasaFGEM : tasaHabitualFGEM,
                    OBSERVACION_IMPORTADA_FGEM, new ArrayList<>(lineasGrupoFGEM.values()), !ventaFGEM);
            int cantidadLineasFGEM = lineasGrupoFGEM.size();
            OperacionFGEM registradaFGEM = intentarFGEM(grupoFGEM.hojaFGEM(), primeraFGEM.filaFGEM(), () -> {
                try {
                    return operacionesFGEM.registrarFGEM(datosFGEM, false);
                } catch (StockInsuficienteExceptionFGEM eFGEM) {
                    OperacionFGEM conFaltanteFGEM = operacionesFGEM.registrarFGEM(datosFGEM, true);
                    ventasSinStockFGEM++;
                    eFGEM.faltantesFGEM().forEach(faltanteFGEM -> sinStockFGEM.add(nombreDeFaltanteFGEM(faltanteFGEM)));
                    return conFaltanteFGEM;
                }
            });
            if (registradaFGEM != null) {
                if (claveDocumentoFGEM != null) {
                    documentosExistentesFGEM.put(claveDocumentoFGEM, registradaFGEM.numeroFGEM());
                }
                if (inventarioFGEM) {
                    inventarioInicialFGEM = registradaFGEM.numeroFGEM();
                }
                if (ventaFGEM) {
                    ventasFGEM++;
                } else {
                    comprasFGEM++;
                }
                lineasFGEM += cantidadLineasFGEM;
                ampliarPeriodoFGEM(grupoFGEM.fechaFGEM);
            }
        }

        /** «Arroz (disponible 2, se venden 3)» → «Arroz». */
        private static String nombreDeFaltanteFGEM(String faltanteFGEM) {
            int parentesisFGEM = faltanteFGEM.lastIndexOf(" (disponible");
            return parentesisFGEM > 0 ? faltanteFGEM.substring(0, parentesisFGEM) : faltanteFGEM;
        }

        // ------------------------------------------------------------------ gastos

        private void registrarMovimientoFGEM(FilaMovimientoFGEM mFGEM) {
            String claveFGEM = claveMovimientoFGEM(mFGEM.fechaFGEM(), mFGEM.categoriaFGEM(), mFGEM.descripcionFGEM(), mFGEM.montoFGEM());
            if (movimientosExistentesFGEM.contains(claveFGEM)) {
                erroresFGEM.add(new ErrorFilaFGEM(HojaFGEM.GASTOS.nombreFGEM(), mFGEM.filaFGEM(), "Ya está registrado un movimiento igual "
                        + "(misma fecha, categoría, descripción y monto). Parece que ya se importó; si de verdad es otro, cambia la descripción."));
                return;
            }
            Boolean registradoFGEM = intentarFGEM(HojaFGEM.GASTOS.nombreFGEM(), mFGEM.filaFGEM(), () -> {
                finanzasFGEM.registrarFGEM(new DatosMovimientoFGEM(mFGEM.categoriaFGEM().tipoFGEM(), mFGEM.categoriaFGEM(),
                        mFGEM.comportamientoFGEM(), mFGEM.fechaFGEM(), mFGEM.descripcionFGEM(), mFGEM.montoFGEM(), mFGEM.comprobanteFGEM()));
                return Boolean.TRUE;
            });
            if (Boolean.TRUE.equals(registradoFGEM)) {
                // No se agrega a los existentes: dos gastos iguales en el mismo archivo pueden ser reales.
                gastosFGEM++;
                ampliarPeriodoFGEM(mFGEM.fechaFGEM());
            }
        }

        // ------------------------------------------------------------------ utilidades

        /**
         * Ejecuta un registro con un punto de retorno: si falla una validación se deshacen solo sus cambios,
         * se anota el error con la fila y se sigue con las demás para mostrar todos los errores juntos.
         *
         * @return lo que devuelva el trabajo, o {@code null} si falló
         */
        private <T> T intentarFGEM(String hojaFGEM, int filaFGEM, Supplier<T> trabajoFGEM) {
            Savepoint puntoFGEM;
            try {
                puntoFGEM = cFGEM.setSavepoint();
            } catch (SQLException eFGEM) {
                throw new DataAccessExceptionFGEM("No se pudo preparar la importación: " + eFGEM.getMessage(), eFGEM);
            }
            try {
                T resultadoFGEM = trabajoFGEM.get();
                cFGEM.releaseSavepoint(puntoFGEM);
                return resultadoFGEM;
            } catch (ValidacionExceptionFGEM | DataAccessExceptionFGEM eFGEM) {
                volverFGEM(puntoFGEM);
                erroresFGEM.add(new ErrorFilaFGEM(hojaFGEM, filaFGEM, eFGEM.getMessage()));
                return null;
            } catch (SQLException eFGEM) {
                throw new DataAccessExceptionFGEM("No se pudo completar la importación: " + eFGEM.getMessage(), eFGEM);
            }
        }

        private void volverFGEM(Savepoint puntoFGEM) {
            try {
                cFGEM.rollback(puntoFGEM);
                cFGEM.releaseSavepoint(puntoFGEM);
            } catch (SQLException eFGEM) {
                throw new DataAccessExceptionFGEM("No se pudo deshacer una fila con errores: " + eFGEM.getMessage(), eFGEM);
            }
        }

        private void ampliarPeriodoFGEM(LocalDate fechaFGEM) {
            desdeFGEM = desdeFGEM == null || fechaFGEM.isBefore(desdeFGEM) ? fechaFGEM : desdeFGEM;
            hastaFGEM = hastaFGEM == null || fechaFGEM.isAfter(hastaFGEM) ? fechaFGEM : hastaFGEM;
        }

        private static String claveDocumentoFGEM(TipoOperacionFGEM tipoFGEM, LocalDate fechaFGEM, String documentoFGEM) {
            return tipoFGEM + "|" + fechaFGEM + "|" + PlantillaImportacionFGEM.normalizarFGEM(documentoFGEM);
        }

        private static String claveMovimientoFGEM(LocalDate fechaFGEM, CategoriaFinancieraFGEM categoriaFGEM, String descripcionFGEM,
                                                  BigDecimal montoFGEM) {
            return fechaFGEM + "|" + categoriaFGEM + "|" + PlantillaImportacionFGEM.normalizarFGEM(descripcionFGEM) + "|"
                    + montoFGEM.stripTrailingZeros().toPlainString();
        }

        /** «Inventario inicial», «Stock inicial» o «Saldo inicial» como proveedor o comprobante. */
        private static boolean esInventarioInicialFGEM(String... textosFGEM) {
            for (String textoFGEM : textosFGEM) {
                String nFGEM = PlantillaImportacionFGEM.normalizarFGEM(textoFGEM);
                if (nFGEM.contains("inventario inicial") || nFGEM.contains("stock inicial") || nFGEM.contains("saldo inicial")) {
                    return true;
                }
            }
            return false;
        }

        private static int ordenHojaFGEM(String hojaFGEM) {
            for (HojaFGEM hFGEM : HojaFGEM.values()) {
                if (hFGEM.nombreFGEM().equals(hojaFGEM)) {
                    return hFGEM.ordinal();
                }
            }
            return -1;
        }
    }
}
