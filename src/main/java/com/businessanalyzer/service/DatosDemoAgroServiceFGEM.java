package com.businessanalyzer.service;

import com.businessanalyzer.exception.ValidacionExceptionFGEM;
import com.businessanalyzer.model.CategoriaActivoFGEM;
import com.businessanalyzer.model.CategoriaDeudaFGEM;
import com.businessanalyzer.model.AspectoInspeccionFGEM;
import com.businessanalyzer.model.CampaniaFGEM;
import com.businessanalyzer.model.CompromisoFGEM;
import com.businessanalyzer.model.ConclusionVisitaFGEM;
import com.businessanalyzer.model.DatosVisitaFGEM;
import com.businessanalyzer.model.EstadoCompromisoFGEM;
import com.businessanalyzer.model.ResultadoAspectoFGEM;
import com.businessanalyzer.model.TipoVisitaFGEM;
import com.businessanalyzer.model.VisitaFGEM;
import com.businessanalyzer.model.CategoriaFinancieraFGEM;
import com.businessanalyzer.model.CategoriaGanadoFGEM;
import com.businessanalyzer.model.DatosMovimientoFGEM;
import com.businessanalyzer.model.TipoMovimientoFGEM;
import com.businessanalyzer.model.CultivoFGEM;
import com.businessanalyzer.model.DatosCampaniaFGEM;
import com.businessanalyzer.model.DatosParcelaFGEM;
import com.businessanalyzer.model.DestinoCosechaFGEM;
import com.businessanalyzer.model.EstadoCampaniaFGEM;
import com.businessanalyzer.model.ParcelaFGEM;
import com.businessanalyzer.model.RubroCostoFGEM;
import com.businessanalyzer.model.TenenciaFGEM;
import com.businessanalyzer.analytics.tesoreria.SaldoCuentaFGEM;
import com.businessanalyzer.model.DatosActivoFGEM;
import com.businessanalyzer.model.DatosCuentaFGEM;
import com.businessanalyzer.model.TipoCuentaFGEM;
import com.businessanalyzer.model.DatosDeudaFGEM;
import com.businessanalyzer.model.DatosEmpresaFGEM;
import com.businessanalyzer.model.DatosMovimientoGanadoFGEM;
import com.businessanalyzer.model.DatosProduccionFGEM;
import com.businessanalyzer.model.EmpresaFGEM;
import com.businessanalyzer.model.EspecieFGEM;
import com.businessanalyzer.model.MonedaFGEM;
import com.businessanalyzer.model.PermisoFGEM;
import com.businessanalyzer.model.ProductoGanaderoFGEM;
import com.businessanalyzer.model.SectorFGEM;
import com.businessanalyzer.model.TamanoEmpresaFGEM;
import com.businessanalyzer.model.TipoMovimientoGanadoFGEM;
import com.businessanalyzer.seguridad.SesionFGEM;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import static com.businessanalyzer.model.CategoriaGanadoFGEM.*;
import static com.businessanalyzer.model.TipoMovimientoGanadoFGEM.*;

public class DatosDemoAgroServiceFGEM {

    public static final String NOMBRE_FUNDO_FGEM = "Fundo Demo";

    private final EmpresaServiceFGEM empresasFGEM;
    private final GanaderiaServiceFGEM ganaderiaFGEM;
    private final AgriculturaServiceFGEM agriculturaFGEM;
    private final VisitaServiceFGEM visitasFGEM;
    private final FinanzasServiceFGEM finanzasFGEM;
    private final PatrimonioServiceFGEM patrimonioFGEM;
    private final TesoreriaServiceFGEM tesoreriaFGEM;
    private final SesionFGEM sesionFGEM;
    private final Clock relojFGEM;

    public DatosDemoAgroServiceFGEM(EmpresaServiceFGEM empresasFGEM, GanaderiaServiceFGEM ganaderiaFGEM, AgriculturaServiceFGEM agriculturaFGEM,
                                PatrimonioServiceFGEM patrimonioFGEM, SesionFGEM sesionFGEM, Clock relojFGEM, VisitaServiceFGEM visitasFGEM,
                                FinanzasServiceFGEM finanzasFGEM, TesoreriaServiceFGEM tesoreriaFGEM) {
        this.tesoreriaFGEM = tesoreriaFGEM;
        this.finanzasFGEM = finanzasFGEM;
        this.visitasFGEM = visitasFGEM;
        this.empresasFGEM = empresasFGEM;
        this.ganaderiaFGEM = ganaderiaFGEM;
        this.agriculturaFGEM = agriculturaFGEM;
        this.patrimonioFGEM = patrimonioFGEM;
        this.sesionFGEM = sesionFGEM;
        this.relojFGEM = relojFGEM;
    }

    public EmpresaFGEM crearFundoDemoFGEM() {
        sesionFGEM.requerirFGEM(PermisoFGEM.EMPRESAS_GESTIONAR);
        sesionFGEM.requerirFGEM(PermisoFGEM.AGRO_GESTIONAR);
        sesionFGEM.requerirFGEM(PermisoFGEM.FINANZAS_GESTIONAR);
        boolean existeFGEM = empresasFGEM.listarFGEM(true).stream().anyMatch(eFGEM -> NOMBRE_FUNDO_FGEM.equalsIgnoreCase(eFGEM.nombreVisibleFGEM()));
        if (existeFGEM) {
            throw new ValidacionExceptionFGEM("Ya existe la empresa \"" + NOMBRE_FUNDO_FGEM + "\". Selecciónala en la barra superior.");
        }
        EmpresaFGEM empresaFGEM = empresasFGEM.crearFGEM(new DatosEmpresaFGEM("Fundo Demo (productor ganadero)", NOMBRE_FUNDO_FGEM, null,
                SectorFGEM.AGROPECUARIO, TamanoEmpresaFGEM.MICRO, MonedaFGEM.PEN, 1, null, "Datos ficticios de demostración",
                null, null));
        empresasFGEM.seleccionarEmpresaActivaFGEM(empresaFGEM.idFGEM());

        LocalDate hoyFGEM = LocalDate.now(relojFGEM);
        LocalDate inicioFGEM = hoyFGEM.minusMonths(12).withDayOfMonth(1);

        Map<CategoriaGanadoFGEM, Integer> inicialFGEM = new EnumMap<>(CategoriaGanadoFGEM.class);
        inicialFGEM.putAll(Map.of(VACA, 12, VAQUILLONA, 4, TERNERA, 3, TERNERO, 3, TORETE, 2, TORO, 1));
        inicialFGEM.putAll(Map.of(ALPACA_MADRE, 60, ALPACA_TUI_HEMBRA, 15, ALPACA_TUI_MACHO, 12, ALPACA_PADRE, 3,
                ALPACA_CAPON, 10));
        inicialFGEM.putAll(Map.of(OVEJA, 25, BORREGA, 6, CORDERO, 4, CARNERO, 1));
        inicialFGEM.forEach((cFGEM, nFGEM) -> movFGEM(inicioFGEM, INVENTARIO_INICIAL, cFGEM, nFGEM, null, null, null));

        movFGEM(mesFGEM(inicioFGEM, 1, 8), NACIMIENTO, TERNERA, 2, null, null, null);
        movFGEM(mesFGEM(inicioFGEM, 3, 15), NACIMIENTO, TERNERO, 2, null, null, null);
        movFGEM(mesFGEM(inicioFGEM, 5, 4), NACIMIENTO, TERNERA, 2, null, null, null);
        movFGEM(mesFGEM(inicioFGEM, 8, 20), NACIMIENTO, TERNERO, 2, null, null, null);
        movFGEM(mesFGEM(inicioFGEM, 9, 2), MUERTE, TERNERO, 1, null, "Diarrea neonatal", null);
        movFGEM(mesFGEM(inicioFGEM, 4, 10), CAMBIO_CATEGORIA, TERNERA, 3, VAQUILLONA, null, null);
        movFGEM(mesFGEM(inicioFGEM, 6, 12), VENTA, TORETE, 2, null, null, "4800");

        movFGEM(mesFGEM(inicioFGEM, 1, 10), NACIMIENTO, ALPACA_CRIA, 12, null, null, null);
        movFGEM(mesFGEM(inicioFGEM, 2, 10), NACIMIENTO, ALPACA_CRIA, 16, null, null, null);
        movFGEM(mesFGEM(inicioFGEM, 3, 10), NACIMIENTO, ALPACA_CRIA, 10, null, null, null);
        movFGEM(mesFGEM(inicioFGEM, 2, 25), MUERTE, ALPACA_CRIA, 3, null, "Neumonía", null);
        movFGEM(mesFGEM(inicioFGEM, 3, 18), MUERTE, ALPACA_CRIA, 2, null, "Helada", null);
        movFGEM(mesFGEM(inicioFGEM, 7, 5), MUERTE, ALPACA_MADRE, 2, null, "Parasitosis", null);
        movFGEM(mesFGEM(inicioFGEM, 5, 1), CAMBIO_CATEGORIA, ALPACA_TUI_HEMBRA, 15, ALPACA_MADRE, null, null);
        movFGEM(mesFGEM(inicioFGEM, 8, 14), VENTA, ALPACA_CAPON, 6, null, null, "1680");
        movFGEM(mesFGEM(inicioFGEM, 10, 3), CONSUMO, ALPACA_CAPON, 2, null, null, null);

        movFGEM(mesFGEM(inicioFGEM, 2, 5), NACIMIENTO, CORDERO, 19, null, null, null);
        movFGEM(mesFGEM(inicioFGEM, 3, 1), MUERTE, CORDERO, 2, null, "Frío", null);
        movFGEM(mesFGEM(inicioFGEM, 7, 22), VENTA, CORDERO, 8, null, null, "1120");
        movFGEM(mesFGEM(inicioFGEM, 9, 9), CONSUMO, OVEJA, 1, null, null, null);
        movFGEM(mesFGEM(inicioFGEM, 4, 28), PERDIDA, BORREGA, 1, null, "Abigeato", null);

        for (int iFGEM = 1; iFGEM <= 12; iFGEM++) {
            LocalDate finFGEM = mesFGEM(inicioFGEM, iFGEM, 1).minusDays(1);
            if (finFGEM.isAfter(hoyFGEM) || finFGEM.isBefore(inicioFGEM)) {
                continue;
            }
            int vacasOrdenoFGEM = 8 + (iFGEM % 3 == 0 ? 1 : 0);
            BigDecimal litrosFGEM = BigDecimal.valueOf(vacasOrdenoFGEM * 30L * (55 + iFGEM % 4 * 3)).movePointLeft(1);
            ganaderiaFGEM.registrarProduccionFGEM(new DatosProduccionFGEM(EspecieFGEM.VACUNO, ProductoGanaderoFGEM.LECHE, finFGEM, 30,
                    litrosFGEM, vacasOrdenoFGEM, new BigDecimal("1.30"), "Total del mes"));
        }
        ganaderiaFGEM.registrarProduccionFGEM(new DatosProduccionFGEM(EspecieFGEM.ALPACA, ProductoGanaderoFGEM.FIBRA, mesFGEM(inicioFGEM, 1, 20),
                1, new BigDecimal("158.5"), 70, new BigDecimal("14.00"), "Esquila anual"));
        ganaderiaFGEM.registrarProduccionFGEM(new DatosProduccionFGEM(EspecieFGEM.OVINO, ProductoGanaderoFGEM.LANA, mesFGEM(inicioFGEM, 1, 22),
                1, new BigDecimal("62"), 30, new BigDecimal("3.50"), "Esquila anual"));

        Map<CategoriaGanadoFGEM, BigDecimal> preciosFGEM = new EnumMap<>(CategoriaGanadoFGEM.class);
        preciosFGEM.putAll(Map.of(VACA, bdFGEM("2800"), VAQUILLONA, bdFGEM("2000"), TERNERA, bdFGEM("900"), TERNERO, bdFGEM("1000"),
                TORETE, bdFGEM("1800"), TORO, bdFGEM("4000")));
        preciosFGEM.putAll(Map.of(ALPACA_MADRE, bdFGEM("450"), ALPACA_TUI_HEMBRA, bdFGEM("300"), ALPACA_CRIA, bdFGEM("150"),
                ALPACA_TUI_MACHO, bdFGEM("250"), ALPACA_PADRE, bdFGEM("1200"), ALPACA_CAPON, bdFGEM("280")));
        preciosFGEM.putAll(Map.of(OVEJA, bdFGEM("300"), BORREGA, bdFGEM("220"), CORDERO, bdFGEM("120"), CARNERO, bdFGEM("500")));
        ganaderiaFGEM.guardarPreciosFGEM(preciosFGEM);

        patrimonioFGEM.registrarActivoFGEM(new DatosActivoFGEM(CategoriaActivoFGEM.MAQUINARIA, "Picadora de forraje y ordeñadora",
                hoyFGEM.minusYears(3), bdFGEM("9000"), 10, bdFGEM("500")));
        patrimonioFGEM.registrarActivoFGEM(new DatosActivoFGEM(CategoriaActivoFGEM.EDIFICIOS, "Establo y cobertizo para alpacas",
                hoyFGEM.minusYears(6), bdFGEM("18000"), 20, null));
        patrimonioFGEM.registrarDeudaFGEM(new DatosDeudaFGEM(CategoriaDeudaFGEM.PRESTAMO_BANCARIO, "Agrobanco",
                "Crédito para compra de vaquillonas", inicioFGEM, bdFGEM("15000"), bdFGEM("15"), bdFGEM("720"), inicioFGEM.plusMonths(24)));
        registrarAgriculturaFGEM(inicioFGEM);
        registrarGastosFGEM(inicioFGEM, hoyFGEM);
        registrarVisitasFGEM(hoyFGEM);
        ajustarCajaFGEM();
        return empresaFGEM;
    }

    private void ajustarCajaFGEM() {
        SaldoCuentaFGEM cajaFGEM = tesoreriaFGEM.listarCuentasFGEM(false).get(0);
        BigDecimal movidoFGEM = cajaFGEM.saldoFGEM().subtract(cajaFGEM.cuentaFGEM().saldoInicialFGEM());
        tesoreriaFGEM.actualizarCuentaFGEM(cajaFGEM.cuentaFGEM().idFGEM(), new DatosCuentaFGEM(cajaFGEM.cuentaFGEM().nombreFGEM(), TipoCuentaFGEM.CAJA, null,
                bdFGEM("3500").subtract(movidoFGEM)));
    }

    private void registrarVisitasFGEM(LocalDate hoyFGEM) {
        String inspectorFGEM = "Ing. Rosa Quispe";
        Map<AspectoInspeccionFGEM, ResultadoAspectoFGEM> primeraFGEM = new EnumMap<>(AspectoInspeccionFGEM.class);
        primeraFGEM.putAll(Map.of(AspectoInspeccionFGEM.SANIDAD_ANIMAL, ResultadoAspectoFGEM.PARCIAL,
                AspectoInspeccionFGEM.VACUNACION, ResultadoAspectoFGEM.NO_CUMPLE,
                AspectoInspeccionFGEM.ALIMENTACION, ResultadoAspectoFGEM.CUMPLE,
                AspectoInspeccionFGEM.INSTALACIONES, ResultadoAspectoFGEM.PARCIAL,
                AspectoInspeccionFGEM.ESTADO_CULTIVOS, ResultadoAspectoFGEM.CUMPLE,
                AspectoInspeccionFGEM.REGISTROS, ResultadoAspectoFGEM.PARCIAL,
                AspectoInspeccionFGEM.USO_CREDITO, ResultadoAspectoFGEM.CUMPLE));
        Map<AspectoInspeccionFGEM, String> notasPrimeraFGEM = new EnumMap<>(AspectoInspeccionFGEM.class);
        notasPrimeraFGEM.put(AspectoInspeccionFGEM.VACUNACION, "Falta la dosificación contra parásitos de las alpacas.");
        notasPrimeraFGEM.put(AspectoInspeccionFGEM.INSTALACIONES, "El cobertizo de crías no protege de las heladas.");
        VisitaFGEM anteriorFGEM = visitasFGEM.registrarFGEM(new DatosVisitaFGEM(hoyFGEM.minusMonths(3), TipoVisitaFGEM.SUPERVISION, inspectorFGEM,
                "Anexo Chacapampa", -12.1352, -75.2311, "Juan Huamán (productor)",
                "Hato en buen estado general. Se observaron crías de alpaca con signos de neumonía.",
                ConclusionVisitaFGEM.CON_OBSERVACIONES, hoyFGEM.minusDays(10), primeraFGEM, notasPrimeraFGEM, List.of(
                new DatosVisitaFGEM.NuevoCompromisoFGEM("Dosificar a todas las alpacas contra parásitos", hoyFGEM.minusMonths(2)),
                new DatosVisitaFGEM.NuevoCompromisoFGEM("Mejorar el cobertizo de crías antes de las heladas", hoyFGEM.minusMonths(1)),
                new DatosVisitaFGEM.NuevoCompromisoFGEM("Llevar al día el registro de nacimientos y muertes", hoyFGEM.plusMonths(1)))));

        Map<AspectoInspeccionFGEM, ResultadoAspectoFGEM> segundaFGEM = new EnumMap<>(AspectoInspeccionFGEM.class);
        segundaFGEM.putAll(Map.of(AspectoInspeccionFGEM.SANIDAD_ANIMAL, ResultadoAspectoFGEM.CUMPLE,
                AspectoInspeccionFGEM.VACUNACION, ResultadoAspectoFGEM.CUMPLE,
                AspectoInspeccionFGEM.ALIMENTACION, ResultadoAspectoFGEM.CUMPLE,
                AspectoInspeccionFGEM.INSTALACIONES, ResultadoAspectoFGEM.PARCIAL,
                AspectoInspeccionFGEM.ESTADO_CULTIVOS, ResultadoAspectoFGEM.CUMPLE,
                AspectoInspeccionFGEM.PLAGAS, ResultadoAspectoFGEM.CUMPLE,
                AspectoInspeccionFGEM.REGISTROS, ResultadoAspectoFGEM.CUMPLE,
                AspectoInspeccionFGEM.USO_CREDITO, ResultadoAspectoFGEM.CUMPLE,
                AspectoInspeccionFGEM.COMPROMISOS, ResultadoAspectoFGEM.PARCIAL,
                AspectoInspeccionFGEM.RIEGO, ResultadoAspectoFGEM.NO_APLICA));
        VisitaFGEM recienteFGEM = visitasFGEM.registrarFGEM(new DatosVisitaFGEM(hoyFGEM.minusDays(10), TipoVisitaFGEM.SEGUIMIENTO_CREDITO, inspectorFGEM,
                "Anexo Chacapampa", -12.1352, -75.2311, "Juan Huamán (productor)",
                "Se dosificó a las alpacas y bajó la mortalidad de crías. El cobertizo sigue a medio construir.",
                ConclusionVisitaFGEM.FAVORABLE, hoyFGEM.plusMonths(3), segundaFGEM, Map.of(), List.of(
                new DatosVisitaFGEM.NuevoCompromisoFGEM("Terminar el cobertizo de crías", hoyFGEM.plusMonths(1)))));

        List<CompromisoFGEM> pendientesFGEM = visitasFGEM.compromisosFGEM().stream().filter(kFGEM -> kFGEM.visitaIdFGEM() == anteriorFGEM.idFGEM()).toList();
        for (CompromisoFGEM kFGEM : pendientesFGEM) {
            EstadoCompromisoFGEM estadoFGEM = kFGEM.descripcionFGEM().startsWith("Dosificar") ? EstadoCompromisoFGEM.CUMPLIDO
                    : kFGEM.descripcionFGEM().startsWith("Mejorar") ? EstadoCompromisoFGEM.INCUMPLIDO : null;
            if (estadoFGEM != null) {
                visitasFGEM.verificarCompromisoFGEM(kFGEM.idFGEM(), estadoFGEM, recienteFGEM.fechaFGEM(), recienteFGEM.idFGEM(), null);
            }
        }
    }

    private void registrarGastosFGEM(LocalDate inicioFGEM, LocalDate hoyFGEM) {
        for (int iFGEM = 1; iFGEM <= 12; iFGEM++) {
            gastoFGEM(CategoriaFinancieraFGEM.SUELDOS, mesFGEM(inicioFGEM, iFGEM, 28), hoyFGEM, "Pastor (medio tiempo)", 600);
            gastoFGEM(CategoriaFinancieraFGEM.OTROS_GASTOS, mesFGEM(inicioFGEM, iFGEM, 8), hoyFGEM, "Vacunas, dosificaciones y medicinas",
                    150 + (iFGEM % 4 == 1 ? 120 : 0));
            gastoFGEM(CategoriaFinancieraFGEM.SERVICIOS_BASICOS, mesFGEM(inicioFGEM, iFGEM, 15), hoyFGEM, "Luz y agua", 60);
            gastoFGEM(CategoriaFinancieraFGEM.TRANSPORTE, mesFGEM(inicioFGEM, iFGEM, 20), hoyFGEM, "Flete a la feria", 90 + (iFGEM % 3) * 20);
            gastoFGEM(CategoriaFinancieraFGEM.MANTENIMIENTO, mesFGEM(inicioFGEM, iFGEM, 12), hoyFGEM, "Reparación de cercos y canales", 70);
            gastoFGEM(CategoriaFinancieraFGEM.INTERESES_PRESTAMOS, mesFGEM(inicioFGEM, iFGEM, 10), hoyFGEM, "Interés del crédito Agrobanco", 180);
        }
    }

    private void gastoFGEM(CategoriaFinancieraFGEM categoriaFGEM, LocalDate fechaFGEM, LocalDate hoyFGEM, String descripcionFGEM, int montoFGEM) {
        if (!fechaFGEM.isAfter(hoyFGEM)) {
            finanzasFGEM.registrarFGEM(new DatosMovimientoFGEM(TipoMovimientoFGEM.GASTO, categoriaFGEM, null, fechaFGEM, descripcionFGEM,
                    BigDecimal.valueOf(montoFGEM), null));
        }
    }

    private void registrarAgriculturaFGEM(LocalDate inicioFGEM) {
        ParcelaFGEM altaFGEM = agriculturaFGEM.registrarParcelaFGEM(new DatosParcelaFGEM("Parcela Alta", bdFGEM("1.5"), TenenciaFGEM.PROPIA, false,
                "Ladera, 3 800 m s. n. m."));
        ParcelaFGEM rioFGEM = agriculturaFGEM.registrarParcelaFGEM(new DatosParcelaFGEM("Chacra del río", bdFGEM("0.8"), TenenciaFGEM.PROPIA, true,
                "Junto al canal de riego"));
        ParcelaFGEM comunalFGEM = agriculturaFGEM.registrarParcelaFGEM(new DatosParcelaFGEM("Terreno comunal", bdFGEM("2"), TenenciaFGEM.COMUNAL,
                false, null));

        CampaniaFGEM papaFGEM = campaniaFGEM(altaFGEM, CultivoFGEM.PAPA, "Yungay", mesFGEM(inicioFGEM, 0, 15), "1", "0.80");
        costosFGEM(papaFGEM, mesFGEM(inicioFGEM, 0, 5), new Object[][]{
                {RubroCostoFGEM.PREPARACION, "Yunta y surcado", "600"}, {RubroCostoFGEM.SEMILLA, "Semilla 1 200 kg", "2400"},
                {RubroCostoFGEM.FERTILIZANTE, "Urea y fosfato", "1500"}, {RubroCostoFGEM.ABONO_ORGANICO, "Guano de corral", "400"},
                {RubroCostoFGEM.FITOSANITARIOS, "Control de rancha", "650"}, {RubroCostoFGEM.MANO_OBRA, "Jornales de aporque", "2200"}});
        costoFGEM(papaFGEM, mesFGEM(inicioFGEM, 6, 2), RubroCostoFGEM.COSECHA, "Jornales de cosecha y sacos", "700");
        cosechaFGEM(papaFGEM, mesFGEM(inicioFGEM, 6, 5), "9500", DestinoCosechaFGEM.VENTA, "0.85");
        cosechaFGEM(papaFGEM, mesFGEM(inicioFGEM, 6, 5), "1500", DestinoCosechaFGEM.AUTOCONSUMO, null);
        cosechaFGEM(papaFGEM, mesFGEM(inicioFGEM, 6, 5), "1200", DestinoCosechaFGEM.SEMILLA, null);
        cosechaFGEM(papaFGEM, mesFGEM(inicioFGEM, 6, 5), "300", DestinoCosechaFGEM.MERMA, null);
        agriculturaFGEM.cerrarCampaniaFGEM(papaFGEM.idFGEM(), EstadoCampaniaFGEM.COSECHADA, mesFGEM(inicioFGEM, 6, 6), null);

        CampaniaFGEM quinuaFGEM = campaniaFGEM(comunalFGEM, CultivoFGEM.QUINUA, "Blanca de Juli", mesFGEM(inicioFGEM, 0, 20), "1.2", null);
        costosFGEM(quinuaFGEM, mesFGEM(inicioFGEM, 0, 10), new Object[][]{
                {RubroCostoFGEM.PREPARACION, "Preparación del terreno", "500"}, {RubroCostoFGEM.SEMILLA, "Semilla 15 kg", "150"},
                {RubroCostoFGEM.FERTILIZANTE, "Fertilización", "600"}, {RubroCostoFGEM.MANO_OBRA, "Deshierbo y raleo", "1800"},
                {RubroCostoFGEM.FITOSANITARIOS, "Control de mildiu", "300"}});
        costoFGEM(quinuaFGEM, mesFGEM(inicioFGEM, 6, 8), RubroCostoFGEM.COSECHA, "Siega, trilla y venteo", "600");
        cosechaFGEM(quinuaFGEM, mesFGEM(inicioFGEM, 6, 12), "1300", DestinoCosechaFGEM.VENTA, "5.50");
        cosechaFGEM(quinuaFGEM, mesFGEM(inicioFGEM, 6, 12), "260", DestinoCosechaFGEM.AUTOCONSUMO, null);
        agriculturaFGEM.cerrarCampaniaFGEM(quinuaFGEM.idFGEM(), EstadoCampaniaFGEM.COSECHADA, mesFGEM(inicioFGEM, 6, 12), null);

        CampaniaFGEM avenaFGEM = campaniaFGEM(rioFGEM, CultivoFGEM.AVENA_FORRAJERA, null, mesFGEM(inicioFGEM, 1, 5), "0.8", "0.15");
        costosFGEM(avenaFGEM, mesFGEM(inicioFGEM, 1, 1), new Object[][]{
                {RubroCostoFGEM.PREPARACION, "Arado con tractor", "450"}, {RubroCostoFGEM.SEMILLA, "Semilla 80 kg", "320"},
                {RubroCostoFGEM.RIEGO, "Tarifa de agua", "130"}, {RubroCostoFGEM.MANO_OBRA, "Siembra y corte", "400"}});
        cosechaFGEM(avenaFGEM, mesFGEM(inicioFGEM, 5, 10), "14000", DestinoCosechaFGEM.ANIMALES, null);
        agriculturaFGEM.cerrarCampaniaFGEM(avenaFGEM.idFGEM(), EstadoCampaniaFGEM.COSECHADA, mesFGEM(inicioFGEM, 5, 10), null);

        CampaniaFGEM maizFGEM = campaniaFGEM(altaFGEM, CultivoFGEM.MAIZ_AMILACEO, "Blanco Urubamba", mesFGEM(inicioFGEM, 1, 10), "0.5", "2.50");
        costosFGEM(maizFGEM, mesFGEM(inicioFGEM, 1, 2), new Object[][]{
                {RubroCostoFGEM.PREPARACION, "Yunta", "250"}, {RubroCostoFGEM.SEMILLA, "Semilla 20 kg", "160"},
                {RubroCostoFGEM.MANO_OBRA, "Siembra y deshierbo", "490"}});
        agriculturaFGEM.cerrarCampaniaFGEM(maizFGEM.idFGEM(), EstadoCampaniaFGEM.PERDIDA, mesFGEM(inicioFGEM, 4, 18), "Granizada");

        CampaniaFGEM nuevaFGEM = campaniaFGEM(altaFGEM, CultivoFGEM.PAPA, "Canchán", mesFGEM(inicioFGEM, 11, 10), "1", "0.80");
        costosFGEM(nuevaFGEM, mesFGEM(inicioFGEM, 11, 1), new Object[][]{
                {RubroCostoFGEM.PREPARACION, "Yunta y surcado", "650"}, {RubroCostoFGEM.SEMILLA, "Semilla 1 200 kg", "2500"}});
    }

    private CampaniaFGEM campaniaFGEM(ParcelaFGEM parcelaFGEM, CultivoFGEM cultivoFGEM, String variedadFGEM, LocalDate siembraFGEM, String areaFGEM,
                              String precioChacraFGEM) {
        return agriculturaFGEM.registrarCampaniaFGEM(new DatosCampaniaFGEM(parcelaFGEM.idFGEM(), cultivoFGEM, variedadFGEM, siembraFGEM, bdFGEM(areaFGEM), null,
                precioChacraFGEM == null ? null : bdFGEM(precioChacraFGEM), null));
    }

    private void costosFGEM(CampaniaFGEM campaniaFGEM, LocalDate fechaFGEM, Object[][] filasFGEM) {
        for (Object[] fFGEM : filasFGEM) {
            costoFGEM(campaniaFGEM, fechaFGEM, (RubroCostoFGEM) fFGEM[0], (String) fFGEM[1], (String) fFGEM[2]);
        }
    }

    private void costoFGEM(CampaniaFGEM campaniaFGEM, LocalDate fechaFGEM, RubroCostoFGEM rubroFGEM, String descripcionFGEM, String montoFGEM) {
        agriculturaFGEM.registrarCostoFGEM(campaniaFGEM.idFGEM(), fechaFGEM, rubroFGEM, descripcionFGEM, bdFGEM(montoFGEM));
    }

    private void cosechaFGEM(CampaniaFGEM campaniaFGEM, LocalDate fechaFGEM, String kilosFGEM, DestinoCosechaFGEM destinoFGEM, String precioFGEM) {
        agriculturaFGEM.registrarCosechaFGEM(campaniaFGEM.idFGEM(), fechaFGEM, bdFGEM(kilosFGEM), destinoFGEM, precioFGEM == null ? null : bdFGEM(precioFGEM), null);
    }

    private static LocalDate mesFGEM(LocalDate inicioFGEM, int mesesFGEM, int diaFGEM) {
        LocalDate baseFGEM = inicioFGEM.plusMonths(mesesFGEM);
        return baseFGEM.withDayOfMonth(Math.min(diaFGEM, baseFGEM.lengthOfMonth()));
    }

    private void movFGEM(LocalDate fechaFGEM, TipoMovimientoGanadoFGEM tipoFGEM, CategoriaGanadoFGEM categoriaFGEM, int cantidadFGEM,
                     CategoriaGanadoFGEM destinoFGEM, String causaFGEM, String montoFGEM) {
        if (fechaFGEM.isAfter(LocalDate.now(relojFGEM))) {
            return;
        }
        ganaderiaFGEM.registrarMovimientoFGEM(new DatosMovimientoGanadoFGEM(fechaFGEM, tipoFGEM, categoriaFGEM, cantidadFGEM, destinoFGEM,
                montoFGEM == null ? null : bdFGEM(montoFGEM), causaFGEM, null));
    }

    private static BigDecimal bdFGEM(String valorFGEM) {
        return new BigDecimal(valorFGEM);
    }
}
