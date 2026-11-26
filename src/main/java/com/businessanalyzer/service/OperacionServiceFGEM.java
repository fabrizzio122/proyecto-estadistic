package com.businessanalyzer.service;

import com.businessanalyzer.analytics.CalculadoraOperacionFGEM;
import com.businessanalyzer.analytics.ResumenOperacionesFGEM;
import com.businessanalyzer.config.DatabaseFGEM;
import com.businessanalyzer.exception.StockInsuficienteExceptionFGEM;
import com.businessanalyzer.exception.ValidacionExceptionFGEM;
import com.businessanalyzer.model.CondicionPagoFGEM;
import com.businessanalyzer.model.DatosOperacionFGEM;
import com.businessanalyzer.model.DatosOperacionFGEM.DatosLineaFGEM;
import com.businessanalyzer.model.EmpresaFGEM;
import com.businessanalyzer.model.OperacionFGEM;
import com.businessanalyzer.model.PermisoFGEM;
import com.businessanalyzer.model.ProductoFGEM;
import com.businessanalyzer.model.TipoItemFGEM;
import com.businessanalyzer.model.TipoOperacionFGEM;
import com.businessanalyzer.repository.OperacionRepositoryFGEM;
import com.businessanalyzer.repository.OperacionRepositoryFGEM.NuevaOperacionFGEM;
import com.businessanalyzer.repository.ProductoRepositoryFGEM;
import com.businessanalyzer.repository.TesoreriaRepositoryFGEM;
import com.businessanalyzer.seguridad.SesionFGEM;
import com.businessanalyzer.util.MontosFGEM;
import com.businessanalyzer.util.ValidacionesFGEM;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class OperacionServiceFGEM {

    public static final BigDecimal TASA_IMPUESTO_INICIAL_FGEM = new BigDecimal("18");

    private static final Logger logFGEM = LoggerFactory.getLogger(OperacionServiceFGEM.class);
    private static final String CLAVE_TASA_FGEM = "impuesto.tasa";

    private final DatabaseFGEM databaseFGEM;
    private final OperacionRepositoryFGEM operacionesFGEM;
    private final ProductoRepositoryFGEM productosFGEM;
    private final EmpresaServiceFGEM empresasFGEM;
    private final ConfiguracionServiceFGEM configuracionFGEM;
    private final SesionFGEM sesionFGEM;
    private final Clock relojFGEM;
    private final TesoreriaRepositoryFGEM tesoreriaFGEM = new TesoreriaRepositoryFGEM();
    private final CuentasEmpresaFGEM cuentasFGEM = new CuentasEmpresaFGEM(tesoreriaFGEM);

    public OperacionServiceFGEM(DatabaseFGEM databaseFGEM, OperacionRepositoryFGEM operacionesFGEM, ProductoRepositoryFGEM productosFGEM,
                            EmpresaServiceFGEM empresasFGEM, ConfiguracionServiceFGEM configuracionFGEM, SesionFGEM sesionFGEM, Clock relojFGEM) {
        this.databaseFGEM = databaseFGEM;
        this.operacionesFGEM = operacionesFGEM;
        this.productosFGEM = productosFGEM;
        this.empresasFGEM = empresasFGEM;
        this.configuracionFGEM = configuracionFGEM;
        this.sesionFGEM = sesionFGEM;
        this.relojFGEM = relojFGEM;
    }

    public List<OperacionFGEM> listarFGEM(TipoOperacionFGEM tipoFGEM, LocalDate desdeFGEM, LocalDate hastaFGEM, boolean incluirAnuladasFGEM) {
        sesionFGEM.requerirFGEM(PermisoFGEM.OPERACIONES_VER);
        EmpresaFGEM empresaFGEM = empresasFGEM.empresaDeTrabajoFGEM();
        validarPeriodoFGEM(desdeFGEM, hastaFGEM);
        return databaseFGEM.conConexionFGEM(cFGEM -> operacionesFGEM.listarFGEM(cFGEM, empresaFGEM.idFGEM(), tipoFGEM, desdeFGEM, hastaFGEM, incluirAnuladasFGEM));
    }

    public OperacionFGEM obtenerFGEM(long idFGEM) {
        sesionFGEM.requerirFGEM(PermisoFGEM.OPERACIONES_VER);
        EmpresaFGEM empresaFGEM = empresasFGEM.empresaDeTrabajoFGEM();
        return databaseFGEM.conConexionFGEM(cFGEM -> exigirOperacionFGEM(cFGEM, idFGEM, empresaFGEM.idFGEM()));
    }

    public ResumenOperacionesFGEM resumenConAgroFGEM(LocalDate desdeFGEM, LocalDate hastaFGEM) {
        sesionFGEM.requerirFGEM(PermisoFGEM.OPERACIONES_VER);
        EmpresaFGEM empresaFGEM = empresasFGEM.empresaDeTrabajoFGEM();
        validarPeriodoFGEM(desdeFGEM, hastaFGEM);
        return databaseFGEM.conConexionFGEM(cFGEM -> operacionesFGEM.resumenFGEM(cFGEM, empresaFGEM.idFGEM(), desdeFGEM, hastaFGEM));
    }

    public ResumenOperacionesFGEM resumenFGEM(LocalDate desdeFGEM, LocalDate hastaFGEM) {
        sesionFGEM.requerirFGEM(PermisoFGEM.OPERACIONES_VER);
        EmpresaFGEM empresaFGEM = empresasFGEM.empresaDeTrabajoFGEM();
        validarPeriodoFGEM(desdeFGEM, hastaFGEM);
        return databaseFGEM.conConexionFGEM(cFGEM -> operacionesFGEM.resumenOperacionesFGEM(cFGEM, empresaFGEM.idFGEM(), desdeFGEM, hastaFGEM));
    }

    public List<ProductoFGEM> productosDisponiblesFGEM() {
        sesionFGEM.requerirFGEM(PermisoFGEM.OPERACIONES_VER);
        EmpresaFGEM empresaFGEM = empresasFGEM.empresaDeTrabajoFGEM();
        return databaseFGEM.conConexionFGEM(cFGEM -> productosFGEM.listarFGEM(cFGEM, empresaFGEM.idFGEM(), false));
    }

    public BigDecimal tasaImpuestoSugeridaFGEM() {
        EmpresaFGEM empresaFGEM = empresasFGEM.empresaDeTrabajoFGEM();
        String guardadaFGEM = configuracionFGEM.obtenerFGEM(CLAVE_TASA_FGEM + "." + empresaFGEM.idFGEM(), "");
        try {
            return guardadaFGEM.isBlank() ? TASA_IMPUESTO_INICIAL_FGEM : new BigDecimal(guardadaFGEM);
        } catch (NumberFormatException eFGEM) {
            return TASA_IMPUESTO_INICIAL_FGEM;
        }
    }

    public OperacionFGEM registrarFGEM(DatosOperacionFGEM datosFGEM, boolean permitirStockNegativoFGEM) {
        sesionFGEM.requerirFGEM(PermisoFGEM.OPERACIONES_GESTIONAR);
        EmpresaFGEM empresaFGEM = empresasFGEM.empresaDeTrabajoFGEM();
        DatosOperacionFGEM dFGEM = ValidadorOperacionFGEM.normalizarFGEM(datosFGEM, LocalDate.now(relojFGEM));
        String usuarioFGEM = sesionFGEM.usuarioFGEM().nombreUsuarioFGEM();
        LocalDateTime ahoraFGEM = LocalDateTime.now(relojFGEM);

        OperacionFGEM registradaFGEM = databaseFGEM.enTransaccionFGEM(
                cFGEM -> registrarEnFGEM(cFGEM, empresaFGEM, dFGEM, permitirStockNegativoFGEM, usuarioFGEM, ahoraFGEM));

        recordarTasaFGEM(empresaFGEM, dFGEM.tasaImpuestoFGEM());
        logFGEM.info("{} {} registrada por {} ({} líneas)", dFGEM.tipoFGEM().descripcionFGEM(), registradaFGEM.numeroFGEM(), usuarioFGEM,
                registradaFGEM.cantidadItemsFGEM());
        return registradaFGEM;
    }

    public OperacionFGEM corregirFGEM(long idFGEM, DatosOperacionFGEM datosFGEM, boolean permitirStockNegativoFGEM) {
        sesionFGEM.requerirFGEM(PermisoFGEM.OPERACIONES_GESTIONAR);
        EmpresaFGEM empresaFGEM = empresasFGEM.empresaDeTrabajoFGEM();
        DatosOperacionFGEM dFGEM = ValidadorOperacionFGEM.normalizarFGEM(datosFGEM, LocalDate.now(relojFGEM));
        String usuarioFGEM = sesionFGEM.usuarioFGEM().nombreUsuarioFGEM();
        LocalDateTime ahoraFGEM = LocalDateTime.now(relojFGEM);

        OperacionFGEM nuevaFGEM = databaseFGEM.enTransaccionFGEM(cFGEM -> {
            OperacionFGEM originalFGEM = exigirOperacionFGEM(cFGEM, idFGEM, empresaFGEM.idFGEM());
            if (originalFGEM.anuladaFGEM()) {
                throw new ValidacionExceptionFGEM("La operación " + originalFGEM.numeroFGEM() + " está anulada y no se puede corregir.");
            }
            if (originalFGEM.tipoFGEM() != dFGEM.tipoFGEM()) {
                throw new ValidacionExceptionFGEM("Una " + originalFGEM.tipoFGEM().descripcionFGEM().toLowerCase()
                        + " no puede convertirse en " + dFGEM.tipoFGEM().descripcionFGEM().toLowerCase() + ".");
            }
            exigirSinPagosFGEM(cFGEM, originalFGEM);
            String numeroNuevoFGEM = operacionesFGEM.siguienteNumeroFGEM(cFGEM, empresaFGEM.idFGEM(), dFGEM.tipoFGEM());
            operacionesFGEM.anularFGEM(cFGEM, idFGEM, "Corregida: reemplazada por " + numeroNuevoFGEM, usuarioFGEM, ahoraFGEM);
            String notaFGEM = "Corrige " + originalFGEM.numeroFGEM();
            String observacionesFGEM = dFGEM.observacionesFGEM() == null || dFGEM.observacionesFGEM().isBlank() ? notaFGEM
                    : dFGEM.observacionesFGEM().startsWith("Corrige ") ? dFGEM.observacionesFGEM() : notaFGEM + ". " + dFGEM.observacionesFGEM();
            DatosOperacionFGEM conNotaFGEM = new DatosOperacionFGEM(dFGEM.tipoFGEM(), dFGEM.fechaFGEM(), dFGEM.contraparteFGEM(), dFGEM.documentoContraparteFGEM(),
                    dFGEM.comprobanteFGEM(), dFGEM.tasaImpuestoFGEM(), observacionesFGEM.length() > 500 ? observacionesFGEM.substring(0, 500)
                    : observacionesFGEM, dFGEM.lineasFGEM(), dFGEM.actualizarCostosFGEM(), dFGEM.condicionFGEM(), dFGEM.cuentaIdFGEM(), dFGEM.vencimientoFGEM());
            return registrarEnFGEM(cFGEM, empresaFGEM, conNotaFGEM, permitirStockNegativoFGEM, usuarioFGEM, ahoraFGEM);
        });

        recordarTasaFGEM(empresaFGEM, dFGEM.tasaImpuestoFGEM());
        logFGEM.info("Operación {} corregida por {}", nuevaFGEM.numeroFGEM(), usuarioFGEM);
        return nuevaFGEM;
    }

    private OperacionFGEM registrarEnFGEM(Connection cFGEM, EmpresaFGEM empresaFGEM, DatosOperacionFGEM dFGEM, boolean permitirStockNegativoFGEM,
                                  String usuarioFGEM, LocalDateTime ahoraFGEM) throws SQLException {
        boolean esVentaFGEM = dFGEM.tipoFGEM() == TipoOperacionFGEM.VENTA;
        List<ProductoFGEM> itemsFGEM = new ArrayList<>();
        List<CalculadoraOperacionFGEM.LineaFGEM> lineasCalculoFGEM = new ArrayList<>();
        List<String> faltantesFGEM = new ArrayList<>();

        for (DatosLineaFGEM lineaFGEM : dFGEM.lineasFGEM()) {
            ProductoFGEM productoFGEM = productosFGEM.buscarPorIdFGEM(cFGEM, lineaFGEM.productoIdFGEM())
                    .filter(pFGEM -> pFGEM.empresaIdFGEM() == empresaFGEM.idFGEM())
                    .orElseThrow(() -> new ValidacionExceptionFGEM(
                            "Uno de los productos ya no existe en la empresa activa. Vuelve a elegirlo."));
            if (!productoFGEM.activoFGEM()) {
                throw new ValidacionExceptionFGEM("\"" + productoFGEM.nombreFGEM() + "\" está archivado. Reactívalo para usarlo.");
            }
            BigDecimal costoFGEM = esVentaFGEM ? productoFGEM.costoUnitarioFGEM() : lineaFGEM.precioUnitarioFGEM();
            if (esVentaFGEM && productoFGEM.tipoFGEM() == TipoItemFGEM.PRODUCTO
                    && productoFGEM.stockFGEM().compareTo(lineaFGEM.cantidadFGEM()) < 0) {
                faltantesFGEM.add(productoFGEM.nombreFGEM() + " (disponible " + MontosFGEM.cantidadFGEM(productoFGEM.stockFGEM())
                        + ", se venden " + MontosFGEM.cantidadFGEM(lineaFGEM.cantidadFGEM()) + ")");
            }
            itemsFGEM.add(productoFGEM);
            lineasCalculoFGEM.add(new CalculadoraOperacionFGEM.LineaFGEM(lineaFGEM.cantidadFGEM(), lineaFGEM.precioUnitarioFGEM(), costoFGEM));
        }
        if (!faltantesFGEM.isEmpty() && !permitirStockNegativoFGEM) {
            throw new StockInsuficienteExceptionFGEM(faltantesFGEM);
        }

        CalculadoraOperacionFGEM.TotalesFGEM totalesFGEM = CalculadoraOperacionFGEM.calcularFGEM(lineasCalculoFGEM, dFGEM.tasaImpuestoFGEM());
        Long cuentaIdFGEM = dFGEM.condicionFGEM() == CondicionPagoFGEM.CONTADO
                ? cuentasFGEM.resolverFGEM(cFGEM, empresaFGEM.idFGEM(), dFGEM.cuentaIdFGEM(), null) : null;
        String numeroFGEM = operacionesFGEM.siguienteNumeroFGEM(cFGEM, empresaFGEM.idFGEM(), dFGEM.tipoFGEM());
        long idFGEM = operacionesFGEM.insertarFGEM(cFGEM, empresaFGEM.idFGEM(), new NuevaOperacionFGEM(dFGEM.tipoFGEM(), numeroFGEM, dFGEM.fechaFGEM(),
                dFGEM.contraparteFGEM(), dFGEM.documentoContraparteFGEM(), dFGEM.comprobanteFGEM(), dFGEM.tasaImpuestoFGEM(), totalesFGEM,
                dFGEM.observacionesFGEM(), usuarioFGEM, ahoraFGEM, dFGEM.condicionFGEM(), cuentaIdFGEM, dFGEM.vencimientoFGEM()));

        for (int iFGEM = 0; iFGEM < itemsFGEM.size(); iFGEM++) {
            CalculadoraOperacionFGEM.LineaFGEM lineaFGEM = lineasCalculoFGEM.get(iFGEM);
            operacionesFGEM.insertarLineaFGEM(cFGEM, idFGEM, itemsFGEM.get(iFGEM).idFGEM(), lineaFGEM.cantidadFGEM(), lineaFGEM.precioUnitarioFGEM(),
                    lineaFGEM.costoUnitarioFGEM(), CalculadoraOperacionFGEM.subtotalLineaFGEM(lineaFGEM.cantidadFGEM(), lineaFGEM.precioUnitarioFGEM()));
        }

        if (!esVentaFGEM && dFGEM.actualizarCostosFGEM()) {
            actualizarCostosDelCatalogoFGEM(cFGEM, itemsFGEM, dFGEM.lineasFGEM(), ahoraFGEM, usuarioFGEM);
        }
        return operacionesFGEM.buscarPorIdFGEM(cFGEM, idFGEM).orElseThrow();
    }

    public OperacionFGEM anularFGEM(long idFGEM, String motivoFGEM) {
        sesionFGEM.requerirFGEM(PermisoFGEM.OPERACIONES_GESTIONAR);
        EmpresaFGEM empresaFGEM = empresasFGEM.empresaDeTrabajoFGEM();
        String motivoLimpioFGEM = ValidacionesFGEM.requeridoFGEM(motivoFGEM, "Motivo de la anulación", 5, 200);
        String usuarioFGEM = sesionFGEM.usuarioFGEM().nombreUsuarioFGEM();
        LocalDateTime ahoraFGEM = LocalDateTime.now(relojFGEM);

        return databaseFGEM.enTransaccionFGEM(cFGEM -> {
            OperacionFGEM operacionFGEM = exigirOperacionFGEM(cFGEM, idFGEM, empresaFGEM.idFGEM());
            if (operacionFGEM.anuladaFGEM()) {
                throw new ValidacionExceptionFGEM("La operación " + operacionFGEM.numeroFGEM() + " ya está anulada.");
            }
            exigirSinPagosFGEM(cFGEM, operacionFGEM);
            operacionesFGEM.anularFGEM(cFGEM, idFGEM, motivoLimpioFGEM, usuarioFGEM, ahoraFGEM);
            return operacionesFGEM.buscarPorIdFGEM(cFGEM, idFGEM).orElseThrow();
        });
    }

    private void actualizarCostosDelCatalogoFGEM(Connection cFGEM, List<ProductoFGEM> itemsFGEM, List<DatosLineaFGEM> lineasFGEM,
                                             LocalDateTime ahoraFGEM, String usuarioFGEM) throws SQLException {
        for (int iFGEM = 0; iFGEM < itemsFGEM.size(); iFGEM++) {
            ProductoFGEM productoFGEM = itemsFGEM.get(iFGEM);
            BigDecimal nuevoCostoFGEM = lineasFGEM.get(iFGEM).precioUnitarioFGEM();
            if (productoFGEM.costoUnitarioFGEM().compareTo(nuevoCostoFGEM) != 0) {
                productosFGEM.actualizarCostoFGEM(cFGEM, productoFGEM.idFGEM(), nuevoCostoFGEM, ahoraFGEM);
                productosFGEM.registrarPrecioFGEM(cFGEM, productoFGEM.idFGEM(), nuevoCostoFGEM, productoFGEM.precioVentaFGEM(), ahoraFGEM, usuarioFGEM);
            }
        }
    }

    private void exigirSinPagosFGEM(Connection cFGEM, OperacionFGEM operacionFGEM) throws SQLException {
        if (tesoreriaFGEM.pagosVigentesFGEM(cFGEM, operacionFGEM.idFGEM()) > 0) {
            String queFGEM = operacionFGEM.tipoFGEM() == TipoOperacionFGEM.VENTA ? "cobros" : "pagos";
            throw new ValidacionExceptionFGEM("La operación " + operacionFGEM.numeroFGEM() + " tiene " + queFGEM
                    + " registrados en «Tesorería». Anúlalos primero.");
        }
    }

    private OperacionFGEM exigirOperacionFGEM(Connection cFGEM, long idFGEM, long empresaIdFGEM) throws SQLException {
        return operacionesFGEM.buscarPorIdFGEM(cFGEM, idFGEM)
                .filter(oFGEM -> oFGEM.empresaIdFGEM() == empresaIdFGEM)
                .orElseThrow(() -> new ValidacionExceptionFGEM("La operación no existe en la empresa activa."));
    }

    private static void validarPeriodoFGEM(LocalDate desdeFGEM, LocalDate hastaFGEM) {
        if (desdeFGEM == null || hastaFGEM == null) {
            throw new ValidacionExceptionFGEM("Indica las fechas del período.");
        }
        if (desdeFGEM.isAfter(hastaFGEM)) {
            throw new ValidacionExceptionFGEM("La fecha inicial no puede ser posterior a la final.");
        }
    }

    private void recordarTasaFGEM(EmpresaFGEM empresaFGEM, BigDecimal tasaFGEM) {
        try {
            configuracionFGEM.guardarFGEM(CLAVE_TASA_FGEM + "." + empresaFGEM.idFGEM(), tasaFGEM.stripTrailingZeros().toPlainString());
        } catch (RuntimeException eFGEM) {
            logFGEM.warn("No se pudo recordar la tasa de impuesto", eFGEM);
        }
    }
}
