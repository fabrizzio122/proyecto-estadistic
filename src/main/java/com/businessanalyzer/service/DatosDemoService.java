package com.businessanalyzer.service;

import com.businessanalyzer.exception.ValidacionException;
import com.businessanalyzer.model.CategoriaActivo;
import com.businessanalyzer.model.CategoriaDeuda;
import com.businessanalyzer.model.CategoriaFinanciera;
import com.businessanalyzer.model.DatosActivo;
import com.businessanalyzer.model.DatosDeuda;
import com.businessanalyzer.model.Deuda;
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
    private final PatrimonioService patrimonio;
    private final Sesion sesion;
    private final Clock reloj;

    public DatosDemoService(EmpresaService empresas, CatalogoService catalogo, OperacionService operaciones,
                            FinanzasService finanzas, PatrimonioService patrimonio, Sesion sesion, Clock reloj) {
        this.patrimonio = patrimonio;
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
        // Volumen mensual creciente, con un pico en diciembre y un febrero flojo (el mes malo del año).
        int[] estacional = {92, 80, 97, 98, 100, 99, 103, 104, 101, 106, 112, 125};
        Producto[] productos = {arroz, azucar, aceite, gaseosa, detergente, agua, delivery};
        String[] precios = {"24.90", "4.50", "11.80", "10.50", "19.90", "15.00", "5.00"};
        // Semilla fija: los datos de demostración salen siempre iguales.
        java.util.Random azar = new java.util.Random(20_26L);
        for (int i = 12; i >= 0; i--) {
            YearMonth mes = actual.minusMonths(i);
            int v = (int) Math.round(3.2 * (100 + (12 - i) * 3) * estacional[mes.getMonthValue() - 1] / 100.0);
            // Se repone lo que se vende cada mes, con un pequeño colchón de stock.
            compra(mes.atDay(1), hoy, l(arroz, v + 30, "18.50"), l(azucar, v * 3 + 35, "3.20"), l(aceite, v + 12, "8.90"),
                    l(gaseosa, v + 20, "7.40"), l(detergente, v / 2 + 8, "14.20"), l(agua, 14, "16.00"));
            ventasDelMes(mes, hoy, azar, productos, precios,
                    new int[]{v + 10, v * 3, v, v + 8, v / 2, 12, 14 + (12 - i)});
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
        registrarPatrimonio(hoy);
        return empresa;
    }

    /**
     * Una venta por día (el cierre de caja de la bodega). Las cantidades del mes se reparten entre los
     * días según el día de la semana (sábado fuerte, domingo y lunes flojos) con algo de variación, y se
     * redondean acumulando el resto para que el total del mes se mantenga.
     */
    private void ventasDelMes(YearMonth mes, LocalDate hoy, java.util.Random azar, Producto[] productos,
                              String[] precios, int[] totalesMes) {
        double[] pesoDia = {0.9, 0.95, 1.0, 1.0, 1.15, 1.35, 0.65}; // lunes … domingo
        double[] pesos = new double[mes.lengthOfMonth()];
        double suma = 0;
        for (int d = 0; d < pesos.length; d++) {
            pesos[d] = pesoDia[mes.atDay(d + 1).getDayOfWeek().getValue() - 1] * (0.85 + azar.nextDouble() * 0.3);
            suma += pesos[d];
        }
        double[] acumulado = new double[productos.length];
        for (int d = 0; d < pesos.length; d++) {
            LocalDate fecha = mes.atDay(d + 1);
            List<DatosLinea> lineas = new java.util.ArrayList<>();
            for (int p = 0; p < productos.length; p++) {
                acumulado[p] += totalesMes[p] * pesos[d] / suma;
                int cantidad = (int) acumulado[p];
                acumulado[p] -= cantidad;
                if (cantidad > 0) {
                    lineas.add(l(productos[p], cantidad, precios[p]));
                }
            }
            if (!fecha.isAfter(hoy) && !lineas.isEmpty()) {
                operaciones.registrar(new DatosOperacion(TipoOperacion.VENTA, fecha, "Clientes varios", null, null, IGV,
                        "Cierre de caja del día", lineas, false), true);
            }
        }
    }

    /** Activos y deudas de la bodega: un préstamo de la caja municipal pagado mes a mes y deudas de corto plazo. */
    private void registrarPatrimonio(LocalDate hoy) {
        activo(CategoriaActivo.CAJA_BANCOS, "Caja y cuenta corriente", hoy, "6800", null, null);
        activo(CategoriaActivo.CUENTAS_POR_COBRAR, "Ventas al crédito a clientes del barrio", hoy, "950", null, null);
        activo(CategoriaActivo.MOBILIARIO, "Anaqueles y mostrador", hoy.minusYears(3), "4500", 10, null);
        activo(CategoriaActivo.MOBILIARIO, "Refrigeradora exhibidora", hoy.minusMonths(14), "3200", 10, "300");
        activo(CategoriaActivo.EQUIPOS_COMPUTO, "Computadora y caja registradora", hoy.minusYears(2), "2600", 4, null);
        activo(CategoriaActivo.VEHICULOS, "Motocicleta para delivery", hoy.minusYears(1), "5500", 5, "800");

        LocalDate inicio = hoy.minusMonths(12).withDayOfMonth(1);
        Deuda prestamo = patrimonio.registrarDeuda(new DatosDeuda(CategoriaDeuda.PRESTAMO_BANCARIO,
                "Caja Municipal", "Préstamo para equipar la bodega", inicio, new BigDecimal("12000"),
                new BigDecimal("28"), new BigDecimal("560"), inicio.plusMonths(36)));
        for (int i = 1; i <= 12; i++) {
            LocalDate fecha = inicio.plusMonths(i).withDayOfMonth(10);
            if (!fecha.isAfter(hoy)) {
                patrimonio.registrarPago(prestamo.id(), fecha, new BigDecimal("400"), "Cuota " + i + " de 36");
            }
        }
        patrimonio.registrarDeuda(new DatosDeuda(CategoriaDeuda.PROVEEDORES, "Distribuidora Lima",
                "Mercadería al crédito a 30 días", hoy, new BigDecimal("2300"), null, null, hoy.plusDays(30)));
        patrimonio.registrarDeuda(new DatosDeuda(CategoriaDeuda.TRIBUTOS, "SUNAT", "IGV del mes por pagar",
                hoy, new BigDecimal("540"), null, null, null));
    }

    private void activo(CategoriaActivo categoria, String descripcion, LocalDate fecha, String valor, Integer vida,
                        String residual) {
        patrimonio.registrarActivo(new DatosActivo(categoria, descripcion, fecha, new BigDecimal(valor), vida,
                residual == null ? null : new BigDecimal(residual)));
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
