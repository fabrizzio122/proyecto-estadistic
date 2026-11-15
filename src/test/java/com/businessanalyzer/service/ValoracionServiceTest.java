package com.businessanalyzer.service;

import com.businessanalyzer.EntornoPrueba;
import com.businessanalyzer.analytics.patrimonio.SituacionPatrimonial;
import com.businessanalyzer.analytics.valoracion.SupuestosValoracion;
import com.businessanalyzer.exception.AccesoDenegadoException;
import com.businessanalyzer.model.Empresa;
import com.businessanalyzer.model.TipoInforme;
import com.businessanalyzer.model.Usuario;
import com.businessanalyzer.repository.InformeRepository;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.parser.PdfTextExtractor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ValoracionServiceTest {

    @TempDir
    Path carpeta;

    private EntornoPrueba entorno;
    private ValoracionService servicio;
    private Empresa bodega;

    @BeforeEach
    void preparar() {
        entorno = new EntornoPrueba(carpeta);
        servicio = entorno.valoracion;
        entorno.administradorConectado();
        bodega = new DatosDemoService(entorno.empresaService, entorno.catalogo, entorno.operaciones, entorno.finanzas,
                entorno.patrimonio, entorno.sesion, entorno.reloj).crearEmpresaDemo();
    }

    @Test
    void valoraConElBalanceYLaUtilidadDelNegocio() {
        SupuestosValoracion s = servicio.supuestos();
        assertEquals(0, SupuestosValoracion.TASA_PREDETERMINADA.compareTo(s.tasaDescuento()));
        assertTrue(s.crecimiento().compareTo(ValoracionService.CRECIMIENTO_MINIMO) >= 0
                && s.crecimiento().compareTo(ValoracionService.CRECIMIENTO_MAXIMO) <= 0);

        ValoracionService.Resultado r = servicio.valorar(s);
        SituacionPatrimonial situacion = entorno.patrimonio.situacion();
        assertEquals(0, situacion.balance().patrimonio().compareTo(r.valoracion().patrimonial()));
        assertEquals(0, situacion.indicadores().utilidadAnual().compareTo(r.valoracion().utilidadAnual()));
        assertNotNull(r.valoracion().flujosDescontados(), "La bodega demo es rentable");
        assertEquals(12, r.valoracion().mesesResultados());
    }

    @Test
    void guardaLosSupuestosPorEmpresa() {
        servicio.guardarSupuestos(new BigDecimal("0.25"), new BigDecimal("0.03"));
        SupuestosValoracion s = servicio.supuestos();
        assertEquals(0, new BigDecimal("0.25").compareTo(s.tasaDescuento()));
        assertEquals(0, new BigDecimal("0.03").compareTo(s.crecimiento()));

        // Otra empresa conserva los valores propuestos.
        entorno.crearEmpresaActiva("Otra");
        assertEquals(0, SupuestosValoracion.TASA_PREDETERMINADA.compareTo(servicio.supuestos().tasaDescuento()));
        assertThrows(RuntimeException.class, () -> servicio.guardarSupuestos(new BigDecimal("0.90"), BigDecimal.ZERO));
    }

    @Test
    void exportaElInformeYLoRegistra() throws Exception {
        Path pdf = servicio.exportar(servicio.supuestos(), carpeta.resolve("valoracion"));

        byte[] bytes = Files.readAllBytes(pdf);
        assertEquals("%PDF", new String(bytes, 0, 4));
        PdfReader lector = new PdfReader(bytes);
        String texto = new PdfTextExtractor(lector).getTextFromPage(1);
        lector.close();
        assertTrue(texto.contains("Valoración del negocio"), texto);
        assertTrue(texto.contains("Valor patrimonial"), texto);
        assertTrue(entorno.database.conConexion(c -> new InformeRepository().listar(c, bodega.id(), 10)).stream()
                .anyMatch(i -> i.tipo() == TipoInforme.VALORACION));
    }

    @Test
    void elOperadorNoVeLaValoracion() {
        Usuario operador = entorno.crearUsuario("operador", "OPERADOR");
        entorno.empresaService.asignarAccesos(bodega.id(), Set.of(operador.id()));
        entorno.entrarComo("operador", "Temporal123");
        entorno.empresaService.seleccionarEmpresaActiva(bodega.id());
        assertThrows(AccesoDenegadoException.class, servicio::supuestos);
    }
}
