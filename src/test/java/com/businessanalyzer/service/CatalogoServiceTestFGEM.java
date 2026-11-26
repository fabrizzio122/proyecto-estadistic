package com.businessanalyzer.service;

import com.businessanalyzer.EntornoPruebaFGEM;
import com.businessanalyzer.exception.AccesoDenegadoExceptionFGEM;
import com.businessanalyzer.exception.ValidacionExceptionFGEM;
import com.businessanalyzer.model.CategoriaFGEM;
import com.businessanalyzer.model.DatosCategoriaFGEM;
import com.businessanalyzer.model.DatosProductoFGEM;
import com.businessanalyzer.model.EmpresaFGEM;
import com.businessanalyzer.model.ProductoFGEM;
import com.businessanalyzer.model.RegistroPrecioFGEM;
import com.businessanalyzer.model.TipoItemFGEM;
import com.businessanalyzer.model.UnidadMedidaFGEM;
import com.businessanalyzer.model.UsuarioFGEM;
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

class CatalogoServiceTestFGEM {

    @TempDir
    Path carpetaFGEM;

    private EntornoPruebaFGEM entornoFGEM;
    private CatalogoServiceFGEM catalogoFGEM;
    private EmpresaFGEM empresaFGEM;

    @BeforeEach
    void prepararFGEM() {
        entornoFGEM = new EntornoPruebaFGEM(carpetaFGEM);
        catalogoFGEM = entornoFGEM.catalogoFGEM;
        entornoFGEM.administradorConectadoFGEM();
        empresaFGEM = entornoFGEM.crearEmpresaActivaFGEM("Comercial Andina");
    }

    @Test
    void registraUnProductoConCodigoAutomaticoEHistorialInicialFGEM() {
        ProductoFGEM p1FGEM = catalogoFGEM.crearProductoFGEM(productoFGEM(null, "Arroz 5 kg", "18.50", "24.90"));
        ProductoFGEM p2FGEM = catalogoFGEM.crearProductoFGEM(productoFGEM(null, "Azúcar 1 kg", "3.20", "4.50"));
        ProductoFGEM s1FGEM = catalogoFGEM.crearProductoFGEM(new DatosProductoFGEM(TipoItemFGEM.SERVICIO, "", "Delivery", null, null,
                UnidadMedidaFGEM.SERVICIO, BigDecimal.ZERO, new BigDecimal("5")));

        assertEquals("P-0001", p1FGEM.codigoFGEM());
        assertEquals("P-0002", p2FGEM.codigoFGEM());
        assertEquals("S-0001", s1FGEM.codigoFGEM());
        assertEquals(0, new BigDecimal("24.90").compareTo(p1FGEM.precioVentaFGEM()));
        assertEquals(empresaFGEM.idFGEM(), p1FGEM.empresaIdFGEM());
        assertEquals(1, catalogoFGEM.historialPreciosFGEM(p1FGEM.idFGEM()).size());
    }

    @Test
    void elCodigoEsUnicoPorEmpresaFGEM() {
        catalogoFGEM.crearProductoFGEM(productoFGEM("arr-01", "Arroz", "1", "2"));
        assertThrows(ValidacionExceptionFGEM.class, () -> catalogoFGEM.crearProductoFGEM(productoFGEM("ARR-01", "Otro", "1", "2")));

        entornoFGEM.crearEmpresaActivaFGEM("Otra empresa");
        ProductoFGEM enOtraFGEM = catalogoFGEM.crearProductoFGEM(productoFGEM("ARR-01", "Arroz", "1", "2"));
        assertEquals("ARR-01", enOtraFGEM.codigoFGEM(), "En otra empresa el mismo código es válido");
    }

    @Test
    void elCodigoAutomaticoContinuaDespuesDelMayorFGEM() {
        catalogoFGEM.crearProductoFGEM(productoFGEM("P-0007", "Manual", "1", "2"));
        assertEquals("P-0008", catalogoFGEM.crearProductoFGEM(productoFGEM(null, "Automático", "1", "2")).codigoFGEM());
    }

    @Test
    void validaLosDatosFGEM() {
        assertInvalidoFGEM(productoFGEM(null, "", "1", "2"));
        assertInvalidoFGEM(productoFGEM("con espacio", "Nombre", "1", "2"));
        assertInvalidoFGEM(productoFGEM(null, "Nombre", "-1", "2"));
        assertInvalidoFGEM(productoFGEM(null, "Nombre", "1", "2000000000"));
        assertInvalidoFGEM(productoFGEM(null, "Nombre", "1.123456", "2"));
        assertInvalidoFGEM(new DatosProductoFGEM(null, null, "Nombre", null, null, UnidadMedidaFGEM.UNIDAD,
                BigDecimal.ONE, BigDecimal.TEN));
        assertInvalidoFGEM(new DatosProductoFGEM(TipoItemFGEM.PRODUCTO, null, "Nombre", null, null, null,
                BigDecimal.ONE, BigDecimal.TEN));
    }

    @Test
    void permiteVenderBajoElCostoPeroLoMarcaComoNegativoFGEM() {
        ProductoFGEM pFGEM = catalogoFGEM.crearProductoFGEM(productoFGEM(null, "Oferta", "10", "8"));
        assertTrue(pFGEM.margenFGEM().negativoFGEM());
    }

    @Test
    void soloLosCambiosDeCostoOPrecioQuedanEnElHistorialFGEM() {
        ProductoFGEM pFGEM = catalogoFGEM.crearProductoFGEM(productoFGEM(null, "Arroz", "18.50", "24.90"));

        catalogoFGEM.actualizarProductoFGEM(pFGEM.idFGEM(), productoFGEM(null, "Arroz extra", "18.50", "24.90"));
        assertEquals(1, catalogoFGEM.historialPreciosFGEM(pFGEM.idFGEM()).size(), "Cambiar el nombre no cambia el precio");

        entornoFGEM.relojFGEM.avanzarFGEM(Duration.ofDays(30));
        ProductoFGEM actualizadoFGEM = catalogoFGEM.actualizarProductoFGEM(pFGEM.idFGEM(), productoFGEM(null, "Arroz extra", "19.00", "26.50"));
        List<RegistroPrecioFGEM> historialFGEM = catalogoFGEM.historialPreciosFGEM(pFGEM.idFGEM());

        assertEquals(pFGEM.codigoFGEM(), actualizadoFGEM.codigoFGEM(), "Si se deja el código vacío se conserva el anterior");
        assertEquals(2, historialFGEM.size());
        assertEquals(0, new BigDecimal("26.50").compareTo(historialFGEM.get(0).precioVentaFGEM()), "El más reciente primero");
        assertEquals("admin", historialFGEM.get(0).usuarioFGEM());
    }

    @Test
    void losProductosArchivadosSeOcultanPeroNoSePierdenFGEM() {
        ProductoFGEM pFGEM = catalogoFGEM.crearProductoFGEM(productoFGEM(null, "Arroz", "1", "2"));
        catalogoFGEM.cambiarEstadoProductoFGEM(pFGEM.idFGEM(), false);

        assertTrue(catalogoFGEM.listarProductosFGEM(false).isEmpty());
        assertEquals(1, catalogoFGEM.listarProductosFGEM(true).size());
        assertFalse(catalogoFGEM.listarProductosFGEM(true).get(0).activoFGEM());
    }

    @Test
    void lasCategoriasPertenecenALaEmpresaFGEM() {
        CategoriaFGEM abarrotesFGEM = catalogoFGEM.crearCategoriaFGEM(new DatosCategoriaFGEM("Abarrotes", "Productos secos"));
        ProductoFGEM pFGEM = catalogoFGEM.crearProductoFGEM(productoFGEM(null, "Arroz", "1", "2").conCategoriaFGEM(abarrotesFGEM.idFGEM()));
        assertEquals("Abarrotes", pFGEM.categoriaNombreFGEM());
        assertThrows(ValidacionExceptionFGEM.class, () -> catalogoFGEM.crearCategoriaFGEM(new DatosCategoriaFGEM("ABARROTES", null)));

        entornoFGEM.crearEmpresaActivaFGEM("Otra empresa");
        assertTrue(catalogoFGEM.listarCategoriasFGEM().isEmpty());
        assertThrows(ValidacionExceptionFGEM.class,
                () -> catalogoFGEM.crearProductoFGEM(productoFGEM(null, "Fideos", "1", "2").conCategoriaFGEM(abarrotesFGEM.idFGEM())),
                "No se puede usar una categoría de otra empresa");
        assertThrows(ValidacionExceptionFGEM.class, () -> catalogoFGEM.actualizarProductoFGEM(pFGEM.idFGEM(), productoFGEM(null, "X", "1", "2")),
                "No se puede editar un producto de otra empresa");
    }

    @Test
    void unaCategoriaEnUsoNoSePuedeEliminarFGEM() {
        CategoriaFGEM bebidasFGEM = catalogoFGEM.crearCategoriaFGEM(new DatosCategoriaFGEM("Bebidas", null));
        ProductoFGEM pFGEM = catalogoFGEM.crearProductoFGEM(productoFGEM(null, "Agua", "1", "2").conCategoriaFGEM(bebidasFGEM.idFGEM()));
        catalogoFGEM.cambiarEstadoProductoFGEM(pFGEM.idFGEM(), false);

        assertThrows(ValidacionExceptionFGEM.class, () -> catalogoFGEM.eliminarCategoriaFGEM(bebidasFGEM.idFGEM()),
                "Incluso si el producto está archivado");

        catalogoFGEM.actualizarProductoFGEM(pFGEM.idFGEM(), productoFGEM(null, "Agua", "1", "2"));
        catalogoFGEM.eliminarCategoriaFGEM(bebidasFGEM.idFGEM());
        assertTrue(catalogoFGEM.listarCategoriasFGEM().isEmpty());
        assertNull(catalogoFGEM.listarProductosFGEM(true).get(0).categoriaIdFGEM());
    }

    @Test
    void respetaLosPermisosYLaEmpresaActivaFGEM() {
        catalogoFGEM.crearProductoFGEM(productoFGEM(null, "Arroz", "1", "2"));
        UsuarioFGEM analistaFGEM = entornoFGEM.crearUsuarioFGEM("analista", "ANALISTA");
        UsuarioFGEM operadorFGEM = entornoFGEM.crearUsuarioFGEM("operador", "OPERADOR");
        entornoFGEM.empresaServiceFGEM.asignarAccesosFGEM(empresaFGEM.idFGEM(), Set.of(analistaFGEM.idFGEM(), operadorFGEM.idFGEM()));

        entornoFGEM.entrarComoFGEM("analista", "Temporal123");
        assertThrows(ValidacionExceptionFGEM.class, () -> catalogoFGEM.listarProductosFGEM(false),
                "Sin empresa activa no hay catálogo");
        entornoFGEM.empresaServiceFGEM.restaurarEmpresaActivaFGEM();
        assertEquals(1, catalogoFGEM.listarProductosFGEM(false).size(), "El analista puede consultar");
        assertThrows(AccesoDenegadoExceptionFGEM.class, () -> catalogoFGEM.crearProductoFGEM(productoFGEM(null, "X", "1", "2")));

        entornoFGEM.entrarComoFGEM("operador", "Temporal123");
        entornoFGEM.empresaServiceFGEM.restaurarEmpresaActivaFGEM();
        assertEquals("P-0002", catalogoFGEM.crearProductoFGEM(productoFGEM(null, "Fideos", "1", "2")).codigoFGEM(),
                "El operador sí registra productos");
    }

    @Test
    void siPierdeElAccesoALaEmpresaNoPuedeSeguirRegistrandoFGEM() {
        UsuarioFGEM gerenteFGEM = entornoFGEM.crearUsuarioFGEM("gerente", "GERENTE");
        entornoFGEM.empresaServiceFGEM.asignarAccesosFGEM(empresaFGEM.idFGEM(), Set.of(gerenteFGEM.idFGEM()));
        entornoFGEM.entrarComoFGEM("gerente", "Temporal123");
        entornoFGEM.empresaServiceFGEM.restaurarEmpresaActivaFGEM();

        entornoFGEM.databaseFGEM.conConexionFGEM(cFGEM -> {
            try (var psFGEM = cFGEM.prepareStatement("DELETE FROM usuario_empresa")) {
                return psFGEM.executeUpdate();
            }
        });
        assertThrows(AccesoDenegadoExceptionFGEM.class, () -> catalogoFGEM.crearProductoFGEM(productoFGEM(null, "X", "1", "2")));
    }

    private void assertInvalidoFGEM(DatosProductoFGEM datosFGEM) {
        assertThrows(ValidacionExceptionFGEM.class, () -> catalogoFGEM.crearProductoFGEM(datosFGEM));
    }

    private static DatosProductoFGEM productoFGEM(String codigoFGEM, String nombreFGEM, String costoFGEM, String precioFGEM) {
        return new DatosProductoFGEM(TipoItemFGEM.PRODUCTO, codigoFGEM, nombreFGEM, null, null, UnidadMedidaFGEM.UNIDAD,
                new BigDecimal(costoFGEM), new BigDecimal(precioFGEM));
    }
}
