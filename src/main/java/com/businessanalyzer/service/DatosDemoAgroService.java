package com.businessanalyzer.service;

import com.businessanalyzer.exception.ValidacionException;
import com.businessanalyzer.model.CategoriaActivo;
import com.businessanalyzer.model.CategoriaDeuda;
import com.businessanalyzer.model.CategoriaGanado;
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
    private final PatrimonioService patrimonio;
    private final Sesion sesion;
    private final Clock reloj;

    public DatosDemoAgroService(EmpresaService empresas, GanaderiaService ganaderia, PatrimonioService patrimonio,
                                Sesion sesion, Clock reloj) {
        this.empresas = empresas;
        this.ganaderia = ganaderia;
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
        return empresa;
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
