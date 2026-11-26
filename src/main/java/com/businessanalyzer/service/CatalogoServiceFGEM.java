package com.businessanalyzer.service;

import com.businessanalyzer.config.DatabaseFGEM;
import com.businessanalyzer.exception.ValidacionExceptionFGEM;
import com.businessanalyzer.model.CategoriaFGEM;
import com.businessanalyzer.model.DatosCategoriaFGEM;
import com.businessanalyzer.model.DatosProductoFGEM;
import com.businessanalyzer.model.EmpresaFGEM;
import com.businessanalyzer.model.PermisoFGEM;
import com.businessanalyzer.model.ProductoFGEM;
import com.businessanalyzer.model.RegistroPrecioFGEM;
import com.businessanalyzer.repository.CategoriaRepositoryFGEM;
import com.businessanalyzer.repository.ProductoRepositoryFGEM;
import com.businessanalyzer.seguridad.SesionFGEM;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

public class CatalogoServiceFGEM {

    private final DatabaseFGEM databaseFGEM;
    private final ProductoRepositoryFGEM productosFGEM;
    private final CategoriaRepositoryFGEM categoriasFGEM;
    private final EmpresaServiceFGEM empresasFGEM;
    private final SesionFGEM sesionFGEM;
    private final Clock relojFGEM;

    public CatalogoServiceFGEM(DatabaseFGEM databaseFGEM, ProductoRepositoryFGEM productosFGEM, CategoriaRepositoryFGEM categoriasFGEM,
                           EmpresaServiceFGEM empresasFGEM, SesionFGEM sesionFGEM, Clock relojFGEM) {
        this.databaseFGEM = databaseFGEM;
        this.productosFGEM = productosFGEM;
        this.categoriasFGEM = categoriasFGEM;
        this.empresasFGEM = empresasFGEM;
        this.sesionFGEM = sesionFGEM;
        this.relojFGEM = relojFGEM;
    }

    public List<ProductoFGEM> listarProductosFGEM(boolean incluirArchivadosFGEM) {
        sesionFGEM.requerirFGEM(PermisoFGEM.PRODUCTOS_VER);
        EmpresaFGEM empresaFGEM = empresasFGEM.empresaDeTrabajoFGEM();
        return databaseFGEM.conConexionFGEM(cFGEM -> productosFGEM.listarFGEM(cFGEM, empresaFGEM.idFGEM(), incluirArchivadosFGEM));
    }

    public ProductoFGEM crearProductoFGEM(DatosProductoFGEM datosFGEM) {
        sesionFGEM.requerirFGEM(PermisoFGEM.PRODUCTOS_GESTIONAR);
        EmpresaFGEM empresaFGEM = empresasFGEM.empresaDeTrabajoFGEM();
        DatosProductoFGEM limpiosFGEM = ValidadorCatalogoFGEM.productoFGEM(datosFGEM);
        String usuarioFGEM = sesionFGEM.usuarioFGEM().nombreUsuarioFGEM();
        LocalDateTime ahoraFGEM = ahoraFGEM();

        return databaseFGEM.enTransaccionFGEM(cFGEM -> {
            exigirCategoriaDeEmpresaFGEM(cFGEM, limpiosFGEM.categoriaIdFGEM(), empresaFGEM.idFGEM());
            String codigoFGEM = limpiosFGEM.codigoFGEM() != null
                    ? limpiosFGEM.codigoFGEM()
                    : productosFGEM.siguienteCodigoFGEM(cFGEM, empresaFGEM.idFGEM(), limpiosFGEM.tipoFGEM().prefijoCodigoFGEM());
            exigirCodigoDisponibleFGEM(cFGEM, empresaFGEM.idFGEM(), codigoFGEM, 0);
            long idFGEM = productosFGEM.insertarFGEM(cFGEM, empresaFGEM.idFGEM(), limpiosFGEM.conCodigoFGEM(codigoFGEM), ahoraFGEM);
            productosFGEM.registrarPrecioFGEM(cFGEM, idFGEM, limpiosFGEM.costoUnitarioFGEM(), limpiosFGEM.precioVentaFGEM(), ahoraFGEM, usuarioFGEM);
            return productosFGEM.buscarPorIdFGEM(cFGEM, idFGEM).orElseThrow();
        });
    }

    public ProductoFGEM actualizarProductoFGEM(long idFGEM, DatosProductoFGEM datosFGEM) {
        sesionFGEM.requerirFGEM(PermisoFGEM.PRODUCTOS_GESTIONAR);
        EmpresaFGEM empresaFGEM = empresasFGEM.empresaDeTrabajoFGEM();
        DatosProductoFGEM limpiosFGEM = ValidadorCatalogoFGEM.productoFGEM(datosFGEM);
        String usuarioFGEM = sesionFGEM.usuarioFGEM().nombreUsuarioFGEM();
        LocalDateTime ahoraFGEM = ahoraFGEM();

        return databaseFGEM.enTransaccionFGEM(cFGEM -> {
            ProductoFGEM actualFGEM = exigirProductoFGEM(cFGEM, idFGEM, empresaFGEM.idFGEM());
            exigirCategoriaDeEmpresaFGEM(cFGEM, limpiosFGEM.categoriaIdFGEM(), empresaFGEM.idFGEM());
            String codigoFGEM = limpiosFGEM.codigoFGEM() != null ? limpiosFGEM.codigoFGEM() : actualFGEM.codigoFGEM();
            exigirCodigoDisponibleFGEM(cFGEM, empresaFGEM.idFGEM(), codigoFGEM, idFGEM);
            productosFGEM.actualizarFGEM(cFGEM, idFGEM, limpiosFGEM.conCodigoFGEM(codigoFGEM), ahoraFGEM);
            boolean cambioPrecioFGEM = actualFGEM.costoUnitarioFGEM().compareTo(limpiosFGEM.costoUnitarioFGEM()) != 0
                    || actualFGEM.precioVentaFGEM().compareTo(limpiosFGEM.precioVentaFGEM()) != 0;
            if (cambioPrecioFGEM) {
                productosFGEM.registrarPrecioFGEM(cFGEM, idFGEM, limpiosFGEM.costoUnitarioFGEM(), limpiosFGEM.precioVentaFGEM(), ahoraFGEM, usuarioFGEM);
            }
            return productosFGEM.buscarPorIdFGEM(cFGEM, idFGEM).orElseThrow();
        });
    }

    public ProductoFGEM cambiarEstadoProductoFGEM(long idFGEM, boolean activoFGEM) {
        sesionFGEM.requerirFGEM(PermisoFGEM.PRODUCTOS_GESTIONAR);
        EmpresaFGEM empresaFGEM = empresasFGEM.empresaDeTrabajoFGEM();
        LocalDateTime ahoraFGEM = ahoraFGEM();
        return databaseFGEM.enTransaccionFGEM(cFGEM -> {
            exigirProductoFGEM(cFGEM, idFGEM, empresaFGEM.idFGEM());
            productosFGEM.cambiarActivoFGEM(cFGEM, idFGEM, activoFGEM, ahoraFGEM);
            return productosFGEM.buscarPorIdFGEM(cFGEM, idFGEM).orElseThrow();
        });
    }

    public List<RegistroPrecioFGEM> historialPreciosFGEM(long idFGEM) {
        sesionFGEM.requerirFGEM(PermisoFGEM.PRODUCTOS_VER);
        EmpresaFGEM empresaFGEM = empresasFGEM.empresaDeTrabajoFGEM();
        return databaseFGEM.conConexionFGEM(cFGEM -> {
            exigirProductoFGEM(cFGEM, idFGEM, empresaFGEM.idFGEM());
            return productosFGEM.historialFGEM(cFGEM, idFGEM);
        });
    }

    public List<CategoriaFGEM> listarCategoriasFGEM() {
        sesionFGEM.requerirFGEM(PermisoFGEM.PRODUCTOS_VER);
        EmpresaFGEM empresaFGEM = empresasFGEM.empresaDeTrabajoFGEM();
        return databaseFGEM.conConexionFGEM(cFGEM -> categoriasFGEM.listarFGEM(cFGEM, empresaFGEM.idFGEM()));
    }

    public CategoriaFGEM crearCategoriaFGEM(DatosCategoriaFGEM datosFGEM) {
        sesionFGEM.requerirFGEM(PermisoFGEM.PRODUCTOS_GESTIONAR);
        EmpresaFGEM empresaFGEM = empresasFGEM.empresaDeTrabajoFGEM();
        DatosCategoriaFGEM limpiosFGEM = ValidadorCatalogoFGEM.categoriaFGEM(datosFGEM);
        LocalDateTime ahoraFGEM = ahoraFGEM();
        return databaseFGEM.enTransaccionFGEM(cFGEM -> {
            exigirNombreCategoriaDisponibleFGEM(cFGEM, empresaFGEM.idFGEM(), limpiosFGEM.nombreFGEM(), 0);
            long idFGEM = categoriasFGEM.insertarFGEM(cFGEM, empresaFGEM.idFGEM(), limpiosFGEM, ahoraFGEM);
            return categoriasFGEM.buscarPorIdFGEM(cFGEM, idFGEM).orElseThrow();
        });
    }

    public CategoriaFGEM actualizarCategoriaFGEM(long idFGEM, DatosCategoriaFGEM datosFGEM) {
        sesionFGEM.requerirFGEM(PermisoFGEM.PRODUCTOS_GESTIONAR);
        EmpresaFGEM empresaFGEM = empresasFGEM.empresaDeTrabajoFGEM();
        DatosCategoriaFGEM limpiosFGEM = ValidadorCatalogoFGEM.categoriaFGEM(datosFGEM);
        LocalDateTime ahoraFGEM = ahoraFGEM();
        return databaseFGEM.enTransaccionFGEM(cFGEM -> {
            exigirCategoriaFGEM(cFGEM, idFGEM, empresaFGEM.idFGEM());
            exigirNombreCategoriaDisponibleFGEM(cFGEM, empresaFGEM.idFGEM(), limpiosFGEM.nombreFGEM(), idFGEM);
            categoriasFGEM.actualizarFGEM(cFGEM, idFGEM, limpiosFGEM, ahoraFGEM);
            return categoriasFGEM.buscarPorIdFGEM(cFGEM, idFGEM).orElseThrow();
        });
    }

    public void eliminarCategoriaFGEM(long idFGEM) {
        sesionFGEM.requerirFGEM(PermisoFGEM.PRODUCTOS_GESTIONAR);
        EmpresaFGEM empresaFGEM = empresasFGEM.empresaDeTrabajoFGEM();
        databaseFGEM.enTransaccionFGEM(cFGEM -> {
            CategoriaFGEM categoriaFGEM = exigirCategoriaFGEM(cFGEM, idFGEM, empresaFGEM.idFGEM());
            if (categoriaFGEM.cantidadItemsFGEM() > 0) {
                throw new ValidacionExceptionFGEM("La categoría \"" + categoriaFGEM.nombreFGEM() + "\" tiene "
                        + categoriaFGEM.cantidadItemsFGEM() + " producto(s) o servicio(s), incluidos los archivados. "
                        + "Muévelos a otra categoría antes de eliminarla.");
            }
            categoriasFGEM.eliminarFGEM(cFGEM, idFGEM);
            return null;
        });
    }

    private ProductoFGEM exigirProductoFGEM(Connection cFGEM, long idFGEM, long empresaIdFGEM) throws SQLException {
        return productosFGEM.buscarPorIdFGEM(cFGEM, idFGEM)
                .filter(pFGEM -> pFGEM.empresaIdFGEM() == empresaIdFGEM)
                .orElseThrow(() -> new ValidacionExceptionFGEM(
                        "El producto o servicio no existe en la empresa activa. Actualiza la lista."));
    }

    private CategoriaFGEM exigirCategoriaFGEM(Connection cFGEM, long idFGEM, long empresaIdFGEM) throws SQLException {
        return categoriasFGEM.buscarPorIdFGEM(cFGEM, idFGEM)
                .filter(catFGEM -> catFGEM.empresaIdFGEM() == empresaIdFGEM)
                .orElseThrow(() -> new ValidacionExceptionFGEM(
                        "La categoría no existe en la empresa activa. Actualiza la lista."));
    }

    private void exigirCategoriaDeEmpresaFGEM(Connection cFGEM, Long categoriaIdFGEM, long empresaIdFGEM) throws SQLException {
        if (categoriaIdFGEM != null) {
            exigirCategoriaFGEM(cFGEM, categoriaIdFGEM, empresaIdFGEM);
        }
    }

    private void exigirCodigoDisponibleFGEM(Connection cFGEM, long empresaIdFGEM, String codigoFGEM, long excluirIdFGEM)
            throws SQLException {
        if (productosFGEM.existeCodigoFGEM(cFGEM, empresaIdFGEM, codigoFGEM, excluirIdFGEM)) {
            throw new ValidacionExceptionFGEM("Ya existe un producto o servicio con el código \"" + codigoFGEM + "\".");
        }
    }

    private void exigirNombreCategoriaDisponibleFGEM(Connection cFGEM, long empresaIdFGEM, String nombreFGEM, long excluirIdFGEM)
            throws SQLException {
        if (categoriasFGEM.existeNombreFGEM(cFGEM, empresaIdFGEM, nombreFGEM, excluirIdFGEM)) {
            throw new ValidacionExceptionFGEM("Ya existe una categoría llamada \"" + nombreFGEM + "\".");
        }
    }

    private LocalDateTime ahoraFGEM() {
        return LocalDateTime.now(relojFGEM);
    }
}
