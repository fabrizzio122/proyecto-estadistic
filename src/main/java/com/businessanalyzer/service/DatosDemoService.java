package com.businessanalyzer.service;

import com.businessanalyzer.exception.ValidacionException;
import com.businessanalyzer.model.CategoriaFinanciera;
import com.businessanalyzer.model.DatosEmpresa;
import com.businessanalyzer.model.DatosMovimiento;
import com.businessanalyzer.model.DatosOperacion;
import com.businessanalyzer.model.DatosOperacion.DatosLinea;
import com.businessanalyzer.model.DatosProducto;
import com.businessanalyzer.model.Empresa;
import com.businessanalyzer.model.Moneda;
import com.businessanalyzer.model.Permiso;
import com.businessanalyzer.model.Producto;
import com.businessanalyzer.model.Sector;
import com.businessanalyzer.model.TamanoEmpresa;
import com.businessanalyzer.model.TipoItem;
import com.businessanalyzer.model.TipoMovimiento;
import com.businessanalyzer.model.TipoOperacion;
import com.businessanalyzer.model.UnidadMedida;
import com.businessanalyzer.seguridad.Sesion;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

/**
 * Crea una empresa de demostración (una bodega) con 12 meses de compras, ventas, gastos e ingresos,
 * para conocer el sistema sin tener que registrar datos reales. La empresa queda como empresa activa.
 * Todo se registra a través de los servicios normales, con sus validaciones y permisos.
 */
public class DatosDemoService {

    public static final String NOMBRE_DEMO = "Bodega Demo";
    private static final BigDecimal IGV = new BigDecimal("18");

    private final EmpresaService empresas;
    private final CatalogoService catalogo;
    private final OperacionService operaciones;
    private final FinanzasService finanzas;
    private final Sesion sesion;
    private final Clock reloj;

    public DatosDemoService(EmpresaService empresas, CatalogoService catalogo, OperacionService operaciones,
                            FinanzasService finanzas, Sesion sesion, Clock reloj) {
        this.empresas = empresas;
        this.catalogo = catalogo;
        this.operaciones = operaciones;
        this.finanzas = finanzas;
        this.sesion = sesion;
        this.reloj = reloj;
    }

    public Empresa crearEmpresaDemo() {
        sesion.requerir(Permiso.EMPRESAS_GESTIONAR);
        sesion.requerir(Permiso.PRODUCTOS_GESTIONAR);
        sesion.requerir(Permiso.OPERACIONES_GESTIONAR);
        sesion.requerir(Permiso.FINANZAS_GESTIONAR);
        boolean existe = empresas.listar(true).stream().anyMatch(e -> NOMBRE_DEMO.equalsIgnoreCase(e.nombreVisible()));
        if (existe) {
            throw new ValidacionException("Ya existe la empresa \"" + NOMBRE_DEMO + "\". Selecciónala en la barra superior.");
        }

        Empresa empresa = empresas.crear(new DatosEmpresa("Bodega Demo E.I.R.L.", NOMBRE_DEMO, null,
                Sector.COMERCIO, TamanoEmpresa.MICRO, Moneda.PEN, 1, null, "Datos ficticios de demostración",
                null, null));
        empresas.seleccionarEmpresaActiva(empresa.id());

        Producto arroz = item(TipoItem.PRODUCTO, "Arroz 5 kg", "18.50", "24.90");
        Producto azucar = item(TipoItem.PRODUCTO, "Azúcar rubia 1 kg", "3.20", "4.50");
        Producto aceite = item(TipoItem.PRODUCTO, "Aceite vegetal 1 l", "8.90", "11.80");
        Producto gaseosa = item(TipoItem.PRODUCTO, "Gaseosa 3 l", "7.40", "10.50");
        Producto detergente = item(TipoItem.PRODUCTO, "Detergente 2 kg", "14.20", "19.90");
        Producto agua = item(TipoItem.PRODUCTO, "Agua mineral (caja x15)", "16.00", "15.00");
        Producto delivery = item(TipoItem.SERVICIO, "Delivery a domicilio", "2.50", "5.00");

        LocalDate hoy = LocalDate.now(reloj);
        YearMonth actual = YearMonth.from(hoy);
        // Volumen mensual creciente, con un pico en diciembre y bajada en enero y febrero.
        int[] estacional = {92, 90, 97, 98, 100, 99, 103, 104, 101, 106, 112, 125};
        for (int i = 12; i >= 0; i--) {
            YearMonth mes = actual.minusMonths(i);
            int v = (int) Math.round(3.2 * (100 + (12 - i) * 3) * estacional[mes.getMonthValue() - 1] / 100.0);
            compra(mes.atDay(1), hoy, l(arroz, v * 2, "18.50"), l(azucar, v * 4, "3.20"), l(aceite, v * 2, "8.90"),
                    l(gaseosa, v * 2, "7.40"), l(detergente, v, "14.20"), l(agua, 20, "16.00"));
            for (int semana = 0; semana < 4; semana++) {
                LocalDate fecha = mes.atDay(3 + semana * 7);
                if (fecha.isAfter(hoy)) {
                    continue;
                }
                int f = v / 4 + (semana == 3 ? 6 : semana);
                venta(fecha, l(arroz, f + 4, "24.90"), l(azucar, f * 3, "4.50"), l(aceite, f, "11.80"),
                        l(gaseosa, f + 2, "10.50"), l(detergente, f / 2, "19.90"), l(agua, 3, "15.00"),
                        l(delivery, 2 + semana, "5.00"));
            }
            gasto(CategoriaFinanciera.ALQUILER, mes.atDay(5), hoy, "Alquiler del local", 1200);
            gasto(CategoriaFinanciera.SUELDOS, mes.atEndOfMonth(), hoy, "Planilla (2 trabajadores)", 2400);
            gasto(CategoriaFinanciera.SERVICIOS_BASICOS, mes.atDay(12), hoy, "Luz, agua e internet", 360 + (i % 3) * 15);
            gasto(CategoriaFinanciera.SERVICIOS_PROFESIONALES, mes.atDay(20), hoy, "Honorarios del contador", 450);
            gasto(CategoriaFinanciera.TRANSPORTE, mes.atDay(9), hoy, "Fletes", v * 2);
            gasto(CategoriaFinanciera.MARKETING, mes.atDay(2), hoy, "Publicidad en redes", v / 2 + (i % 2) * 25);
            gasto(CategoriaFinanciera.INTERESES_PRESTAMOS, mes.atDay(10), hoy, "Interés de préstamo", 160);
            gasto(CategoriaFinanciera.IMPUESTO_RENTA, mes.atEndOfMonth(), hoy, "Pago a cuenta del impuesto a la renta", 380);
            movimiento(TipoMovimiento.INGRESO, CategoriaFinanciera.ALQUILERES_COBRADOS, mes.atDay(4), hoy,
                    "Alquiler de cochera", 200);
        }
        return empresa;
    }

    private Producto item(TipoItem tipo, String nombre, String costo, String precio) {
        return catalogo.crearProducto(new DatosProducto(tipo, null, nombre, null, null,
                tipo == TipoItem.SERVICIO ? UnidadMedida.SERVICIO : UnidadMedida.UNIDAD,
                new BigDecimal(costo), new BigDecimal(precio)));
    }

    private static DatosLinea l(Producto p, int cantidad, String precio) {
        return new DatosLinea(p.id(), BigDecimal.valueOf(cantidad), new BigDecimal(precio));
    }

    private void compra(LocalDate fecha, LocalDate hoy, DatosLinea... lineas) {
        if (!fecha.isAfter(hoy)) {
            operaciones.registrar(new DatosOperacion(TipoOperacion.COMPRA, fecha, null, null, null, IGV, null,
                    List.of(lineas), false), true);
        }
    }

    private void venta(LocalDate fecha, DatosLinea... lineas) {
        operaciones.registrar(new DatosOperacion(TipoOperacion.VENTA, fecha, null, null, null, IGV, null,
                List.of(lineas), false), true);
    }

    private void gasto(CategoriaFinanciera categoria, LocalDate fecha, LocalDate hoy, String descripcion, int monto) {
        movimiento(TipoMovimiento.GASTO, categoria, fecha, hoy, descripcion, monto);
    }

    private void movimiento(TipoMovimiento tipo, CategoriaFinanciera categoria, LocalDate fecha, LocalDate hoy,
                            String descripcion, int monto) {
        if (!fecha.isAfter(hoy)) {
            finanzas.registrar(new DatosMovimiento(tipo, categoria, null, fecha, descripcion,
                    BigDecimal.valueOf(monto), null));
        }
    }
}
