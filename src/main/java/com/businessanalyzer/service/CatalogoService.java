package com.businessanalyzer.service;

import com.businessanalyzer.config.Database;
import com.businessanalyzer.exception.ValidacionException;
import com.businessanalyzer.model.Categoria;
import com.businessanalyzer.model.DatosCategoria;
import com.businessanalyzer.model.DatosProducto;
import com.businessanalyzer.model.Empresa;
import com.businessanalyzer.model.Permiso;
import com.businessanalyzer.model.Producto;
import com.businessanalyzer.model.RegistroPrecio;
import com.businessanalyzer.repository.CategoriaRepository;
import com.businessanalyzer.repository.ProductoRepository;
import com.businessanalyzer.seguridad.Sesion;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Catálogo de productos, servicios y categorías de la empresa activa.
 * <p>
 * Reglas:
 * <ul>
 *     <li>Todo se registra sobre la empresa de trabajo y nunca se puede leer o modificar
 *         un ítem de otra empresa.</li>
 *     <li>El código es único por empresa. Si se deja vacío, se asigna P-0001 / S-0001...</li>
 *     <li>Cada cambio de costo o precio queda en el historial de precios.</li>
 *     <li>Los productos no se eliminan, se archivan (las ventas de la Fase 5 los referencian).
 *         Una categoría solo se elimina si ningún producto la usa.</li>
 * </ul>
 */
public class CatalogoService {

    private final Database database;
    private final ProductoRepository productos;
    private final CategoriaRepository categorias;
    private final EmpresaService empresas;
    private final Sesion sesion;
    private final Clock reloj;

    public CatalogoService(Database database, ProductoRepository productos, CategoriaRepository categorias,
                           EmpresaService empresas, Sesion sesion, Clock reloj) {
        this.database = database;
        this.productos = productos;
        this.categorias = categorias;
        this.empresas = empresas;
        this.sesion = sesion;
        this.reloj = reloj;
    }

    // ------------------------------------------------------------------ productos y servicios

    public List<Producto> listarProductos(boolean incluirArchivados) {
        sesion.requerir(Permiso.PRODUCTOS_VER);
        Empresa empresa = empresas.empresaDeTrabajo();
        return database.conConexion(c -> productos.listar(c, empresa.id(), incluirArchivados));
    }

    public Producto crearProducto(DatosProducto datos) {
        sesion.requerir(Permiso.PRODUCTOS_GESTIONAR);
        Empresa empresa = empresas.empresaDeTrabajo();
        DatosProducto limpios = ValidadorCatalogo.producto(datos);
        String usuario = sesion.usuario().nombreUsuario();
        LocalDateTime ahora = ahora();

        return database.enTransaccion(c -> {
            exigirCategoriaDeEmpresa(c, limpios.categoriaId(), empresa.id());
            String codigo = limpios.codigo() != null
                    ? limpios.codigo()
                    : productos.siguienteCodigo(c, empresa.id(), limpios.tipo().prefijoCodigo());
            exigirCodigoDisponible(c, empresa.id(), codigo, 0);
            long id = productos.insertar(c, empresa.id(), limpios.conCodigo(codigo), ahora);
            productos.registrarPrecio(c, id, limpios.costoUnitario(), limpios.precioVenta(), ahora, usuario);
            return productos.buscarPorId(c, id).orElseThrow();
        });
    }

    public Producto actualizarProducto(long id, DatosProducto datos) {
        sesion.requerir(Permiso.PRODUCTOS_GESTIONAR);
        Empresa empresa = empresas.empresaDeTrabajo();
        DatosProducto limpios = ValidadorCatalogo.producto(datos);
        String usuario = sesion.usuario().nombreUsuario();
        LocalDateTime ahora = ahora();

        return database.enTransaccion(c -> {
            Producto actual = exigirProducto(c, id, empresa.id());
            exigirCategoriaDeEmpresa(c, limpios.categoriaId(), empresa.id());
            String codigo = limpios.codigo() != null ? limpios.codigo() : actual.codigo();
            exigirCodigoDisponible(c, empresa.id(), codigo, id);
            productos.actualizar(c, id, limpios.conCodigo(codigo), ahora);
            boolean cambioPrecio = actual.costoUnitario().compareTo(limpios.costoUnitario()) != 0
                    || actual.precioVenta().compareTo(limpios.precioVenta()) != 0;
            if (cambioPrecio) {
                productos.registrarPrecio(c, id, limpios.costoUnitario(), limpios.precioVenta(), ahora, usuario);
            }
            return productos.buscarPorId(c, id).orElseThrow();
        });
    }

    /** Archiva ({@code false}) o reactiva ({@code true}) un producto o servicio. */
    public Producto cambiarEstadoProducto(long id, boolean activo) {
        sesion.requerir(Permiso.PRODUCTOS_GESTIONAR);
        Empresa empresa = empresas.empresaDeTrabajo();
        LocalDateTime ahora = ahora();
        return database.enTransaccion(c -> {
            exigirProducto(c, id, empresa.id());
            productos.cambiarActivo(c, id, activo, ahora);
            return productos.buscarPorId(c, id).orElseThrow();
        });
    }

    public List<RegistroPrecio> historialPrecios(long id) {
        sesion.requerir(Permiso.PRODUCTOS_VER);
        Empresa empresa = empresas.empresaDeTrabajo();
        return database.conConexion(c -> {
            exigirProducto(c, id, empresa.id());
            return productos.historial(c, id);
        });
    }

    // ------------------------------------------------------------------ categorías

    public List<Categoria> listarCategorias() {
        sesion.requerir(Permiso.PRODUCTOS_VER);
        Empresa empresa = empresas.empresaDeTrabajo();
        return database.conConexion(c -> categorias.listar(c, empresa.id()));
    }

    public Categoria crearCategoria(DatosCategoria datos) {
        sesion.requerir(Permiso.PRODUCTOS_GESTIONAR);
        Empresa empresa = empresas.empresaDeTrabajo();
        DatosCategoria limpios = ValidadorCatalogo.categoria(datos);
        LocalDateTime ahora = ahora();
        return database.enTransaccion(c -> {
            exigirNombreCategoriaDisponible(c, empresa.id(), limpios.nombre(), 0);
            long id = categorias.insertar(c, empresa.id(), limpios, ahora);
            return categorias.buscarPorId(c, id).orElseThrow();
        });
    }

    public Categoria actualizarCategoria(long id, DatosCategoria datos) {
        sesion.requerir(Permiso.PRODUCTOS_GESTIONAR);
        Empresa empresa = empresas.empresaDeTrabajo();
        DatosCategoria limpios = ValidadorCatalogo.categoria(datos);
        LocalDateTime ahora = ahora();
        return database.enTransaccion(c -> {
            exigirCategoria(c, id, empresa.id());
            exigirNombreCategoriaDisponible(c, empresa.id(), limpios.nombre(), id);
            categorias.actualizar(c, id, limpios, ahora);
            return categorias.buscarPorId(c, id).orElseThrow();
        });
    }

    public void eliminarCategoria(long id) {
        sesion.requerir(Permiso.PRODUCTOS_GESTIONAR);
        Empresa empresa = empresas.empresaDeTrabajo();
        database.enTransaccion(c -> {
            Categoria categoria = exigirCategoria(c, id, empresa.id());
            if (categoria.cantidadItems() > 0) {
                throw new ValidacionException("La categoría \"" + categoria.nombre() + "\" tiene "
                        + categoria.cantidadItems() + " producto(s) o servicio(s), incluidos los archivados. "
                        + "Muévelos a otra categoría antes de eliminarla.");
            }
            categorias.eliminar(c, id);
            return null;
        });
    }

    // ------------------------------------------------------------------ utilidades

    private Producto exigirProducto(Connection c, long id, long empresaId) throws SQLException {
        return productos.buscarPorId(c, id)
                .filter(p -> p.empresaId() == empresaId)
                .orElseThrow(() -> new ValidacionException(
                        "El producto o servicio no existe en la empresa activa. Actualiza la lista."));
    }

    private Categoria exigirCategoria(Connection c, long id, long empresaId) throws SQLException {
        return categorias.buscarPorId(c, id)
                .filter(cat -> cat.empresaId() == empresaId)
                .orElseThrow(() -> new ValidacionException(
                        "La categoría no existe en la empresa activa. Actualiza la lista."));
    }

    private void exigirCategoriaDeEmpresa(Connection c, Long categoriaId, long empresaId) throws SQLException {
        if (categoriaId != null) {
            exigirCategoria(c, categoriaId, empresaId);
        }
    }

    private void exigirCodigoDisponible(Connection c, long empresaId, String codigo, long excluirId)
            throws SQLException {
        if (productos.existeCodigo(c, empresaId, codigo, excluirId)) {
            throw new ValidacionException("Ya existe un producto o servicio con el código \"" + codigo + "\".");
        }
    }

    private void exigirNombreCategoriaDisponible(Connection c, long empresaId, String nombre, long excluirId)
            throws SQLException {
        if (categorias.existeNombre(c, empresaId, nombre, excluirId)) {
            throw new ValidacionException("Ya existe una categoría llamada \"" + nombre + "\".");
        }
    }

    private LocalDateTime ahora() {
        return LocalDateTime.now(reloj);
    }
}
