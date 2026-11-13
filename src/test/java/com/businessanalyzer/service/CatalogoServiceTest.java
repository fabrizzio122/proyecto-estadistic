package com.businessanalyzer.service;

import com.businessanalyzer.EntornoPrueba;
import com.businessanalyzer.exception.AccesoDenegadoException;
import com.businessanalyzer.exception.ValidacionException;
import com.businessanalyzer.model.Categoria;
import com.businessanalyzer.model.DatosCategoria;
import com.businessanalyzer.model.DatosProducto;
import com.businessanalyzer.model.Empresa;
import com.businessanalyzer.model.Producto;
import com.businessanalyzer.model.RegistroPrecio;
import com.businessanalyzer.model.TipoItem;
import com.businessanalyzer.model.UnidadMedida;
import com.businessanalyzer.model.Usuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CatalogoServiceTest {

    @TempDir
    Path carpeta;

    private EntornoPrueba entorno;
    private CatalogoService catalogo;
    private Empresa empresa;

    @BeforeEach
    void preparar() {
        entorno = new EntornoPrueba(carpeta);
        catalogo = entorno.catalogo;
        entorno.administradorConectado();
        empresa = entorno.crearEmpresaActiva("Comercial Andina");
    }

    @Test
    void registraUnProductoConCodigoAutomaticoEHistorialInicial() {
        Producto p1 = catalogo.crearProducto(producto(null, "Arroz 5 kg", "18.50", "24.90"));
        Producto p2 = catalogo.crearProducto(producto(null, "Azúcar 1 kg", "3.20", "4.50"));
        Producto s1 = catalogo.crearProducto(new DatosProducto(TipoItem.SERVICIO, "", "Delivery", null, null,
                UnidadMedida.SERVICIO, BigDecimal.ZERO, new BigDecimal("5")));

        assertEquals("P-0001", p1.codigo());
        assertEquals("P-0002", p2.codigo());
        assertEquals("S-0001", s1.codigo());
        assertEquals(0, new BigDecimal("24.90").compareTo(p1.precioVenta()));
        assertEquals(empresa.id(), p1.empresaId());
        assertEquals(1, catalogo.historialPrecios(p1.id()).size());
    }

    @Test
    void elCodigoEsUnicoPorEmpresa() {
        catalogo.crearProducto(producto("arr-01", "Arroz", "1", "2"));
        assertThrows(ValidacionException.class, () -> catalogo.crearProducto(producto("ARR-01", "Otro", "1", "2")));

        entorno.crearEmpresaActiva("Otra empresa");
        Producto enOtra = catalogo.crearProducto(producto("ARR-01", "Arroz", "1", "2"));
        assertEquals("ARR-01", enOtra.codigo(), "En otra empresa el mismo código es válido");
    }

    @Test
    void elCodigoAutomaticoContinuaDespuesDelMayor() {
        catalogo.crearProducto(producto("P-0007", "Manual", "1", "2"));
        assertEquals("P-0008", catalogo.crearProducto(producto(null, "Automático", "1", "2")).codigo());
    }

    @Test
    void validaLosDatos() {
        assertInvalido(producto(null, "", "1", "2"));
        assertInvalido(producto("con espacio", "Nombre", "1", "2"));
        assertInvalido(producto(null, "Nombre", "-1", "2"));
        assertInvalido(producto(null, "Nombre", "1", "2000000000"));
        assertInvalido(producto(null, "Nombre", "1.123456", "2"));
        assertInvalido(new DatosProducto(null, null, "Nombre", null, null, UnidadMedida.UNIDAD,
                BigDecimal.ONE, BigDecimal.TEN));
        assertInvalido(new DatosProducto(TipoItem.PRODUCTO, null, "Nombre", null, null, null,
                BigDecimal.ONE, BigDecimal.TEN));
    }

    @Test
    void permiteVenderBajoElCostoPeroLoMarcaComoNegativo() {
        Producto p = catalogo.crearProducto(producto(null, "Oferta", "10", "8"));
        assertTrue(p.margen().negativo());
    }

    @Test
    void soloLosCambiosDeCostoOPrecioQuedanEnElHistorial() {
        Producto p = catalogo.crearProducto(producto(null, "Arroz", "18.50", "24.90"));

        catalogo.actualizarProducto(p.id(), producto(null, "Arroz extra", "18.50", "24.90"));
        assertEquals(1, catalogo.historialPrecios(p.id()).size(), "Cambiar el nombre no cambia el precio");

        entorno.reloj.avanzar(Duration.ofDays(30));
        Producto actualizado = catalogo.actualizarProducto(p.id(), producto(null, "Arroz extra", "19.00", "26.50"));
        List<RegistroPrecio> historial = catalogo.historialPrecios(p.id());

        assertEquals(p.codigo(), actualizado.codigo(), "Si se deja el código vacío se conserva el anterior");
        assertEquals(2, historial.size());
        assertEquals(0, new BigDecimal("26.50").compareTo(historial.get(0).precioVenta()), "El más reciente primero");
        assertEquals("admin", historial.get(0).usuario());
    }

    @Test
    void losProductosArchivadosSeOcultanPeroNoSePierden() {
        Producto p = catalogo.crearProducto(producto(null, "Arroz", "1", "2"));
        catalogo.cambiarEstadoProducto(p.id(), false);

        assertTrue(catalogo.listarProductos(false).isEmpty());
        assertEquals(1, catalogo.listarProductos(true).size());
        assertFalse(catalogo.listarProductos(true).get(0).activo());
    }

    @Test
    void lasCategoriasPertenecenALaEmpresa() {
        Categoria abarrotes = catalogo.crearCategoria(new DatosCategoria("Abarrotes", "Productos secos"));
        Producto p = catalogo.crearProducto(producto(null, "Arroz", "1", "2").conCategoria(abarrotes.id()));
        assertEquals("Abarrotes", p.categoriaNombre());
        assertThrows(ValidacionException.class, () -> catalogo.crearCategoria(new DatosCategoria("ABARROTES", null)));

        entorno.crearEmpresaActiva("Otra empresa");
        assertTrue(catalogo.listarCategorias().isEmpty());
        assertThrows(ValidacionException.class,
                () -> catalogo.crearProducto(producto(null, "Fideos", "1", "2").conCategoria(abarrotes.id())),
                "No se puede usar una categoría de otra empresa");
        assertThrows(ValidacionException.class, () -> catalogo.actualizarProducto(p.id(), producto(null, "X", "1", "2")),
                "No se puede editar un producto de otra empresa");
    }

    @Test
    void unaCategoriaEnUsoNoSePuedeEliminar() {
        Categoria bebidas = catalogo.crearCategoria(new DatosCategoria("Bebidas", null));
        Producto p = catalogo.crearProducto(producto(null, "Agua", "1", "2").conCategoria(bebidas.id()));
        catalogo.cambiarEstadoProducto(p.id(), false);

        assertThrows(ValidacionException.class, () -> catalogo.eliminarCategoria(bebidas.id()),
                "Incluso si el producto está archivado");

        catalogo.actualizarProducto(p.id(), producto(null, "Agua", "1", "2"));
        catalogo.eliminarCategoria(bebidas.id());
        assertTrue(catalogo.listarCategorias().isEmpty());
        assertNull(catalogo.listarProductos(true).get(0).categoriaId());
    }

    @Test
    void respetaLosPermisosYLaEmpresaActiva() {
        catalogo.crearProducto(producto(null, "Arroz", "1", "2"));
        Usuario analista = entorno.crearUsuario("analista", "ANALISTA");
        Usuario operador = entorno.crearUsuario("operador", "OPERADOR");
        entorno.empresaService.asignarAccesos(empresa.id(), Set.of(analista.id(), operador.id()));

        entorno.entrarComo("analista", "Temporal123");
        assertThrows(ValidacionException.class, () -> catalogo.listarProductos(false),
                "Sin empresa activa no hay catálogo");
        entorno.empresaService.restaurarEmpresaActiva();
        assertEquals(1, catalogo.listarProductos(false).size(), "El analista puede consultar");
        assertThrows(AccesoDenegadoException.class, () -> catalogo.crearProducto(producto(null, "X", "1", "2")));

        entorno.entrarComo("operador", "Temporal123");
        entorno.empresaService.restaurarEmpresaActiva();
        assertEquals("P-0002", catalogo.crearProducto(producto(null, "Fideos", "1", "2")).codigo(),
                "El operador sí registra productos");
    }

    @Test
    void siPierdeElAccesoALaEmpresaNoPuedeSeguirRegistrando() {
        Usuario gerente = entorno.crearUsuario("gerente", "GERENTE");
        entorno.empresaService.asignarAccesos(empresa.id(), Set.of(gerente.id()));
        entorno.entrarComo("gerente", "Temporal123");
        entorno.empresaService.restaurarEmpresaActiva();

        // Otro administrador quita el acceso mientras la sesión sigue abierta
        entorno.database.conConexion(c -> {
            try (var ps = c.prepareStatement("DELETE FROM usuario_empresa")) {
                return ps.executeUpdate();
            }
        });
        assertThrows(AccesoDenegadoException.class, () -> catalogo.crearProducto(producto(null, "X", "1", "2")));
    }

    private void assertInvalido(DatosProducto datos) {
        assertThrows(ValidacionException.class, () -> catalogo.crearProducto(datos));
    }

    private static DatosProducto producto(String codigo, String nombre, String costo, String precio) {
        return new DatosProducto(TipoItem.PRODUCTO, codigo, nombre, null, null, UnidadMedida.UNIDAD,
                new BigDecimal(costo), new BigDecimal(precio));
    }
}
