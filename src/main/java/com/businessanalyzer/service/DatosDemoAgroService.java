package com.businessanalyzer.service;

import com.businessanalyzer.exception.ValidacionException;
import com.businessanalyzer.model.CategoriaActivo;
import com.businessanalyzer.model.CategoriaDeuda;
import com.businessanalyzer.model.AspectoInspeccion;
import com.businessanalyzer.model.Campania;
import com.businessanalyzer.model.Compromiso;
import com.businessanalyzer.model.ConclusionVisita;
import com.businessanalyzer.model.DatosVisita;
import com.businessanalyzer.model.EstadoCompromiso;
import com.businessanalyzer.model.ResultadoAspecto;
import com.businessanalyzer.model.TipoVisita;
import com.businessanalyzer.model.Visita;
import com.businessanalyzer.model.CategoriaFinanciera;
import com.businessanalyzer.model.CategoriaGanado;
import com.businessanalyzer.model.DatosMovimiento;
import com.businessanalyzer.model.TipoMovimiento;
import com.businessanalyzer.model.Cultivo;
import com.businessanalyzer.model.DatosCampania;
import com.businessanalyzer.model.DatosParcela;
import com.businessanalyzer.model.DestinoCosecha;
import com.businessanalyzer.model.EstadoCampania;
import com.businessanalyzer.model.Parcela;
import com.businessanalyzer.model.RubroCosto;
import com.businessanalyzer.model.Tenencia;
import com.businessanalyzer.model.DatosActivo;
import com.businessanalyzer.model.DatosDeuda;
import com.businessanalyzer.model.DatosEmpresa;
import com.businessanalyzer.model.DatosMovimientoGanado;
import com.businessanalyzer.model.DatosProduccion;
import com.businessanalyzer.model.Empresa;
import com.businessanalyzer.model.Especie;
import com.businessanalyzer.model.Moneda;
import com.businessanalyzer.model.Permiso;
import com.businessanalyzer.model.ProductoGanadero;
import com.businessanalyzer.model.Sector;
import com.businessanalyzer.model.TamanoEmpresa;
import com.businessanalyzer.model.TipoMovimientoGanado;
import com.businessanalyzer.seguridad.Sesion;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import static com.businessanalyzer.model.CategoriaGanado.*;
import static com.businessanalyzer.model.TipoMovimientoGanado.*;

/**
 * Crea un productor agropecuario de demostración («Fundo Demo») con vacunos, alpacas y ovinos:
 * inventario inicial de hace un año, nacimientos, muertes, ventas, producción de leche y fibra,
 * precios por cabeza, un tractor y un préstamo. Todo se registra con los servicios normales.
 */
public class DatosDemoAgroService {

    public static final String NOMBRE_FUNDO = "Fundo Demo";

    private final EmpresaService empresas;
    private final GanaderiaService ganaderia;
    private final AgriculturaService agricultura;
    private final VisitaService visitas;
    private final FinanzasService finanzas;
    private final PatrimonioService patrimonio;
    private final Sesion sesion;
    private final Clock reloj;

    public DatosDemoAgroService(EmpresaService empresas, GanaderiaService ganaderia, AgriculturaService agricultura,
                                PatrimonioService patrimonio, Sesion sesion, Clock reloj, VisitaService visitas,
                                FinanzasService finanzas) {
        this.finanzas = finanzas;
        this.visitas = visitas;
        this.empresas = empresas;
        this.ganaderia = ganaderia;
        this.agricultura = agricultura;
        this.patrimonio = patrimonio;
        this.sesion = sesion;
        this.reloj = reloj;
    }

    public Empresa crearFundoDemo() {
        sesion.requerir(Permiso.EMPRESAS_GESTIONAR);
        sesion.requerir(Permiso.AGRO_GESTIONAR);
        sesion.requerir(Permiso.FINANZAS_GESTIONAR);
        boolean existe = empresas.listar(true).stream().anyMatch(e -> NOMBRE_FUNDO.equalsIgnoreCase(e.nombreVisible()));
        if (existe) {
            throw new ValidacionException("Ya existe la empresa \"" + NOMBRE_FUNDO + "\". Selecciónala en la barra superior.");
        }
        Empresa empresa = empresas.crear(new DatosEmpresa("Fundo Demo (productor ganadero)", NOMBRE_FUNDO, null,
                Sector.AGROPECUARIO, TamanoEmpresa.MICRO, Moneda.PEN, 1, null, "Datos ficticios de demostración",
                null, null));
        empresas.seleccionarEmpresaActiva(empresa.id());

        LocalDate hoy = LocalDate.now(reloj);
        LocalDate inicio = hoy.minusMonths(12).withDayOfMonth(1);

        // Inventario inicial (hace un año).
        Map<CategoriaGanado, Integer> inicial = new EnumMap<>(CategoriaGanado.class);
        inicial.putAll(Map.of(VACA, 12, VAQUILLONA, 4, TERNERA, 3, TERNERO, 3, TORETE, 2, TORO, 1));
        inicial.putAll(Map.of(ALPACA_MADRE, 60, ALPACA_TUI_HEMBRA, 15, ALPACA_TUI_MACHO, 12, ALPACA_PADRE, 3,
                ALPACA_CAPON, 10));
        inicial.putAll(Map.of(OVEJA, 25, BORREGA, 6, CORDERO, 4, CARNERO, 1));
        inicial.forEach((c, n) -> mov(inicio, INVENTARIO_INICIAL, c, n, null, null, null));

        // Vacunos: partos repartidos en el año, una muerte de ternero y venta de toretes.
        mov(mes(inicio, 1, 8), NACIMIENTO, TERNERA, 2, null, null, null);
        mov(mes(inicio, 3, 15), NACIMIENTO, TERNERO, 2, null, null, null);
        mov(mes(inicio, 5, 4), NACIMIENTO, TERNERA, 2, null, null, null);
        mov(mes(inicio, 8, 20), NACIMIENTO, TERNERO, 2, null, null, null);
        mov(mes(inicio, 9, 2), MUERTE, TERNERO, 1, null, "Diarrea neonatal", null);
        mov(mes(inicio, 4, 10), CAMBIO_CATEGORIA, TERNERA, 3, VAQUILLONA, null, null);
        mov(mes(inicio, 6, 12), VENTA, TORETE, 2, null, null, "4800");

        // Alpacas: parición de diciembre a marzo, mortalidad de crías por neumonía y heladas.
        mov(mes(inicio, 1, 10), NACIMIENTO, ALPACA_CRIA, 12, null, null, null);
        mov(mes(inicio, 2, 10), NACIMIENTO, ALPACA_CRIA, 16, null, null, null);
        mov(mes(inicio, 3, 10), NACIMIENTO, ALPACA_CRIA, 10, null, null, null);
        mov(mes(inicio, 2, 25), MUERTE, ALPACA_CRIA, 3, null, "Neumonía", null);
        mov(mes(inicio, 3, 18), MUERTE, ALPACA_CRIA, 2, null, "Helada", null);
        mov(mes(inicio, 7, 5), MUERTE, ALPACA_MADRE, 2, null, "Parasitosis", null);
        mov(mes(inicio, 5, 1), CAMBIO_CATEGORIA, ALPACA_TUI_HEMBRA, 15, ALPACA_MADRE, null, null);
        mov(mes(inicio, 8, 14), VENTA, ALPACA_CAPON, 6, null, null, "1680");
        mov(mes(inicio, 10, 3), CONSUMO, ALPACA_CAPON, 2, null, null, null);

        // Ovinos.
        mov(mes(inicio, 2, 5), NACIMIENTO, CORDERO, 19, null, null, null);
        mov(mes(inicio, 3, 1), MUERTE, CORDERO, 2, null, "Frío", null);
        mov(mes(inicio, 7, 22), VENTA, CORDERO, 8, null, null, "1120");
        mov(mes(inicio, 9, 9), CONSUMO, OVEJA, 1, null, null, null);
        mov(mes(inicio, 4, 28), PERDIDA, BORREGA, 1, null, "Abigeato", null);

        // Producción: leche de cada mes y una esquila de alpacas y ovinos.
        for (int i = 1; i <= 12; i++) {
            LocalDate fin = mes(inicio, i, 1).minusDays(1);
            if (fin.isAfter(hoy) || fin.isBefore(inicio)) {
                continue;
            }
            int vacasOrdeno = 8 + (i % 3 == 0 ? 1 : 0);
            BigDecimal litros = BigDecimal.valueOf(vacasOrdeno * 30L * (55 + i % 4 * 3)).movePointLeft(1);
            ganaderia.registrarProduccion(new DatosProduccion(Especie.VACUNO, ProductoGanadero.LECHE, fin, 30,
                    litros, vacasOrdeno, new BigDecimal("1.30"), "Total del mes"));
        }
        ganaderia.registrarProduccion(new DatosProduccion(Especie.ALPACA, ProductoGanadero.FIBRA, mes(inicio, 1, 20),
                1, new BigDecimal("158.5"), 70, new BigDecimal("14.00"), "Esquila anual"));
        ganaderia.registrarProduccion(new DatosProduccion(Especie.OVINO, ProductoGanadero.LANA, mes(inicio, 1, 22),
                1, new BigDecimal("62"), 30, new BigDecimal("3.50"), "Esquila anual"));

        Map<CategoriaGanado, BigDecimal> precios = new EnumMap<>(CategoriaGanado.class);
        precios.putAll(Map.of(VACA, bd("2800"), VAQUILLONA, bd("2000"), TERNERA, bd("900"), TERNERO, bd("1000"),
                TORETE, bd("1800"), TORO, bd("4000")));
        precios.putAll(Map.of(ALPACA_MADRE, bd("450"), ALPACA_TUI_HEMBRA, bd("300"), ALPACA_CRIA, bd("150"),
                ALPACA_TUI_MACHO, bd("250"), ALPACA_PADRE, bd("1200"), ALPACA_CAPON, bd("280")));
        precios.putAll(Map.of(OVEJA, bd("300"), BORREGA, bd("220"), CORDERO, bd("120"), CARNERO, bd("500")));
        ganaderia.guardarPrecios(precios);

        patrimonio.registrarActivo(new DatosActivo(CategoriaActivo.CAJA_BANCOS, "Efectivo y cuenta de ahorros", hoy,
                bd("3500"), null, null));
        patrimonio.registrarActivo(new DatosActivo(CategoriaActivo.MAQUINARIA, "Picadora de forraje y ordeñadora",
                hoy.minusYears(3), bd("9000"), 10, bd("500")));
        patrimonio.registrarActivo(new DatosActivo(CategoriaActivo.EDIFICIOS, "Establo y cobertizo para alpacas",
                hoy.minusYears(6), bd("18000"), 20, null));
        patrimonio.registrarDeuda(new DatosDeuda(CategoriaDeuda.PRESTAMO_BANCARIO, "Agrobanco",
                "Crédito para compra de vaquillonas", inicio, bd("15000"), bd("15"), bd("720"), inicio.plusMonths(24)));
        registrarAgricultura(inicio);
        registrarGastos(inicio, hoy);
        registrarVisitas(hoy);
        return empresa;
    }

    /** Una supervisión hace 3 meses con compromisos y una de seguimiento reciente que los verifica. */
    private void registrarVisitas(LocalDate hoy) {
        String inspector = "Ing. Rosa Quispe";
        Map<AspectoInspeccion, ResultadoAspecto> primera = new EnumMap<>(AspectoInspeccion.class);
        primera.putAll(Map.of(AspectoInspeccion.SANIDAD_ANIMAL, ResultadoAspecto.PARCIAL,
                AspectoInspeccion.VACUNACION, ResultadoAspecto.NO_CUMPLE,
                AspectoInspeccion.ALIMENTACION, ResultadoAspecto.CUMPLE,
                AspectoInspeccion.INSTALACIONES, ResultadoAspecto.PARCIAL,
                AspectoInspeccion.ESTADO_CULTIVOS, ResultadoAspecto.CUMPLE,
                AspectoInspeccion.REGISTROS, ResultadoAspecto.PARCIAL,
                AspectoInspeccion.USO_CREDITO, ResultadoAspecto.CUMPLE));
        Map<AspectoInspeccion, String> notasPrimera = new EnumMap<>(AspectoInspeccion.class);
        notasPrimera.put(AspectoInspeccion.VACUNACION, "Falta la dosificación contra parásitos de las alpacas.");
        notasPrimera.put(AspectoInspeccion.INSTALACIONES, "El cobertizo de crías no protege de las heladas.");
        Visita anterior = visitas.registrar(new DatosVisita(hoy.minusMonths(3), TipoVisita.SUPERVISION, inspector,
                "Anexo Chacapampa", -12.1352, -75.2311, "Juan Huamán (productor)",
                "Hato en buen estado general. Se observaron crías de alpaca con signos de neumonía.",
                ConclusionVisita.CON_OBSERVACIONES, hoy.minusDays(10), primera, notasPrimera, List.of(
                new DatosVisita.NuevoCompromiso("Dosificar a todas las alpacas contra parásitos", hoy.minusMonths(2)),
                new DatosVisita.NuevoCompromiso("Mejorar el cobertizo de crías antes de las heladas", hoy.minusMonths(1)),
                new DatosVisita.NuevoCompromiso("Llevar al día el registro de nacimientos y muertes", hoy.plusMonths(1)))));

        Map<AspectoInspeccion, ResultadoAspecto> segunda = new EnumMap<>(AspectoInspeccion.class);
        segunda.putAll(Map.of(AspectoInspeccion.SANIDAD_ANIMAL, ResultadoAspecto.CUMPLE,
                AspectoInspeccion.VACUNACION, ResultadoAspecto.CUMPLE,
                AspectoInspeccion.ALIMENTACION, ResultadoAspecto.CUMPLE,
                AspectoInspeccion.INSTALACIONES, ResultadoAspecto.PARCIAL,
                AspectoInspeccion.ESTADO_CULTIVOS, ResultadoAspecto.CUMPLE,
                AspectoInspeccion.PLAGAS, ResultadoAspecto.CUMPLE,
                AspectoInspeccion.REGISTROS, ResultadoAspecto.CUMPLE,
                AspectoInspeccion.USO_CREDITO, ResultadoAspecto.CUMPLE,
                AspectoInspeccion.COMPROMISOS, ResultadoAspecto.PARCIAL,
                AspectoInspeccion.RIEGO, ResultadoAspecto.NO_APLICA));
        Visita reciente = visitas.registrar(new DatosVisita(hoy.minusDays(10), TipoVisita.SEGUIMIENTO_CREDITO, inspector,
                "Anexo Chacapampa", -12.1352, -75.2311, "Juan Huamán (productor)",
                "Se dosificó a las alpacas y bajó la mortalidad de crías. El cobertizo sigue a medio construir.",
                ConclusionVisita.FAVORABLE, hoy.plusMonths(3), segunda, Map.of(), List.of(
                new DatosVisita.NuevoCompromiso("Terminar el cobertizo de crías", hoy.plusMonths(1)))));

        List<Compromiso> pendientes = visitas.compromisos().stream().filter(k -> k.visitaId() == anterior.id()).toList();
        for (Compromiso k : pendientes) {
            EstadoCompromiso estado = k.descripcion().startsWith("Dosificar") ? EstadoCompromiso.CUMPLIDO
                    : k.descripcion().startsWith("Mejorar") ? EstadoCompromiso.INCUMPLIDO : null;
            if (estado != null) {
                visitas.verificarCompromiso(k.id(), estado, reciente.fecha(), reciente.id(), null);
            }
        }
    }

    /** Gastos de cada mes del productor (pastor, sanidad animal, fletes, intereses del crédito...). */
    private void registrarGastos(LocalDate inicio, LocalDate hoy) {
        for (int i = 1; i <= 12; i++) {
            gasto(CategoriaFinanciera.SUELDOS, mes(inicio, i, 28), hoy, "Pastor (medio tiempo)", 600);
            gasto(CategoriaFinanciera.OTROS_GASTOS, mes(inicio, i, 8), hoy, "Vacunas, dosificaciones y medicinas",
                    150 + (i % 4 == 1 ? 120 : 0));
            gasto(CategoriaFinanciera.SERVICIOS_BASICOS, mes(inicio, i, 15), hoy, "Luz y agua", 60);
            gasto(CategoriaFinanciera.TRANSPORTE, mes(inicio, i, 20), hoy, "Flete a la feria", 90 + (i % 3) * 20);
            gasto(CategoriaFinanciera.MANTENIMIENTO, mes(inicio, i, 12), hoy, "Reparación de cercos y canales", 70);
            gasto(CategoriaFinanciera.INTERESES_PRESTAMOS, mes(inicio, i, 10), hoy, "Interés del crédito Agrobanco", 180);
        }
    }

    private void gasto(CategoriaFinanciera categoria, LocalDate fecha, LocalDate hoy, String descripcion, int monto) {
        if (!fecha.isAfter(hoy)) {
            finanzas.registrar(new DatosMovimiento(TipoMovimiento.GASTO, categoria, null, fecha, descripcion,
                    BigDecimal.valueOf(monto), null));
        }
    }

    /** Tres parcelas y cinco campañas: papa, quinua y avena cosechadas, un maíz perdido por granizada y papa en curso. */
    private void registrarAgricultura(LocalDate inicio) {
        Parcela alta = agricultura.registrarParcela(new DatosParcela("Parcela Alta", bd("1.5"), Tenencia.PROPIA, false,
                "Ladera, 3 800 m s. n. m."));
        Parcela rio = agricultura.registrarParcela(new DatosParcela("Chacra del río", bd("0.8"), Tenencia.PROPIA, true,
                "Junto al canal de riego"));
        Parcela comunal = agricultura.registrarParcela(new DatosParcela("Terreno comunal", bd("2"), Tenencia.COMUNAL,
                false, null));

        // Papa: rendimiento algo bajo la referencia, pero rentable.
        Campania papa = campania(alta, Cultivo.PAPA, "Yungay", mes(inicio, 0, 15), "1", "0.80");
        costos(papa, mes(inicio, 0, 5), new Object[][]{
                {RubroCosto.PREPARACION, "Yunta y surcado", "600"}, {RubroCosto.SEMILLA, "Semilla 1 200 kg", "2400"},
                {RubroCosto.FERTILIZANTE, "Urea y fosfato", "1500"}, {RubroCosto.ABONO_ORGANICO, "Guano de corral", "400"},
                {RubroCosto.FITOSANITARIOS, "Control de rancha", "650"}, {RubroCosto.MANO_OBRA, "Jornales de aporque", "2200"}});
        costo(papa, mes(inicio, 6, 2), RubroCosto.COSECHA, "Jornales de cosecha y sacos", "700");
        cosecha(papa, mes(inicio, 6, 5), "9500", DestinoCosecha.VENTA, "0.85");
        cosecha(papa, mes(inicio, 6, 5), "1500", DestinoCosecha.AUTOCONSUMO, null);
        cosecha(papa, mes(inicio, 6, 5), "1200", DestinoCosecha.SEMILLA, null);
        cosecha(papa, mes(inicio, 6, 5), "300", DestinoCosecha.MERMA, null);
        agricultura.cerrarCampania(papa.id(), EstadoCampania.COSECHADA, mes(inicio, 6, 6), null);

        // Quinua: por encima de la referencia.
        Campania quinua = campania(comunal, Cultivo.QUINUA, "Blanca de Juli", mes(inicio, 0, 20), "1.2", null);
        costos(quinua, mes(inicio, 0, 10), new Object[][]{
                {RubroCosto.PREPARACION, "Preparación del terreno", "500"}, {RubroCosto.SEMILLA, "Semilla 15 kg", "150"},
                {RubroCosto.FERTILIZANTE, "Fertilización", "600"}, {RubroCosto.MANO_OBRA, "Deshierbo y raleo", "1800"},
                {RubroCosto.FITOSANITARIOS, "Control de mildiu", "300"}});
        costo(quinua, mes(inicio, 6, 8), RubroCosto.COSECHA, "Siega, trilla y venteo", "600");
        cosecha(quinua, mes(inicio, 6, 12), "1300", DestinoCosecha.VENTA, "5.50");
        cosecha(quinua, mes(inicio, 6, 12), "260", DestinoCosecha.AUTOCONSUMO, null);
        agricultura.cerrarCampania(quinua.id(), EstadoCampania.COSECHADA, mes(inicio, 6, 12), null);

        // Avena forrajera para el ganado.
        Campania avena = campania(rio, Cultivo.AVENA_FORRAJERA, null, mes(inicio, 1, 5), "0.8", "0.15");
        costos(avena, mes(inicio, 1, 1), new Object[][]{
                {RubroCosto.PREPARACION, "Arado con tractor", "450"}, {RubroCosto.SEMILLA, "Semilla 80 kg", "320"},
                {RubroCosto.RIEGO, "Tarifa de agua", "130"}, {RubroCosto.MANO_OBRA, "Siembra y corte", "400"}});
        cosecha(avena, mes(inicio, 5, 10), "14000", DestinoCosecha.ANIMALES, null);
        agricultura.cerrarCampania(avena.id(), EstadoCampania.COSECHADA, mes(inicio, 5, 10), null);

        // Maíz perdido por granizada.
        Campania maiz = campania(alta, Cultivo.MAIZ_AMILACEO, "Blanco Urubamba", mes(inicio, 1, 10), "0.5", "2.50");
        costos(maiz, mes(inicio, 1, 2), new Object[][]{
                {RubroCosto.PREPARACION, "Yunta", "250"}, {RubroCosto.SEMILLA, "Semilla 20 kg", "160"},
                {RubroCosto.MANO_OBRA, "Siembra y deshierbo", "490"}});
        agricultura.cerrarCampania(maiz.id(), EstadoCampania.PERDIDA, mes(inicio, 4, 18), "Granizada");

        // Nueva campaña de papa, recién sembrada.
        Campania nueva = campania(alta, Cultivo.PAPA, "Canchán", mes(inicio, 11, 10), "1", "0.80");
        costos(nueva, mes(inicio, 11, 1), new Object[][]{
                {RubroCosto.PREPARACION, "Yunta y surcado", "650"}, {RubroCosto.SEMILLA, "Semilla 1 200 kg", "2500"}});
    }

    private Campania campania(Parcela parcela, Cultivo cultivo, String variedad, LocalDate siembra, String area,
                              String precioChacra) {
        return agricultura.registrarCampania(new DatosCampania(parcela.id(), cultivo, variedad, siembra, bd(area), null,
                precioChacra == null ? null : bd(precioChacra), null));
    }

    private void costos(Campania campania, LocalDate fecha, Object[][] filas) {
        for (Object[] f : filas) {
            costo(campania, fecha, (RubroCosto) f[0], (String) f[1], (String) f[2]);
        }
    }

    private void costo(Campania campania, LocalDate fecha, RubroCosto rubro, String descripcion, String monto) {
        agricultura.registrarCosto(campania.id(), fecha, rubro, descripcion, bd(monto));
    }

    private void cosecha(Campania campania, LocalDate fecha, String kilos, DestinoCosecha destino, String precio) {
        agricultura.registrarCosecha(campania.id(), fecha, bd(kilos), destino, precio == null ? null : bd(precio), null);
    }

    /** Día {@code dia} del mes que está {@code meses} después de {@code inicio}. */
    private static LocalDate mes(LocalDate inicio, int meses, int dia) {
        LocalDate base = inicio.plusMonths(meses);
        return base.withDayOfMonth(Math.min(dia, base.lengthOfMonth()));
    }

    private void mov(LocalDate fecha, TipoMovimientoGanado tipo, CategoriaGanado categoria, int cantidad,
                     CategoriaGanado destino, String causa, String monto) {
        if (fecha.isAfter(LocalDate.now(reloj))) {
            return;
        }
        ganaderia.registrarMovimiento(new DatosMovimientoGanado(fecha, tipo, categoria, cantidad, destino,
                monto == null ? null : bd(monto), causa, null));
    }

    private static BigDecimal bd(String valor) {
        return new BigDecimal(valor);
    }
}
