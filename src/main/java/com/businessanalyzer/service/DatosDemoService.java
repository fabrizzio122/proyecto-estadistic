package com.businessanalyzer.service;

import com.businessanalyzer.exception.ValidacionException;
import com.businessanalyzer.model.CategoriaActivo;
import com.businessanalyzer.model.CategoriaDeuda;
import com.businessanalyzer.model.CategoriaFinanciera;
import com.businessanalyzer.model.CondicionPago;
import com.businessanalyzer.model.CuentaDinero;
import com.businessanalyzer.model.DatosActivo;
import com.businessanalyzer.model.DatosCuenta;
import com.businessanalyzer.model.DatosDeuda;
import com.businessanalyzer.model.DatosEmpresa;
import com.businessanalyzer.model.DatosMovimiento;
import com.businessanalyzer.model.DatosMovimientoTesoreria;
import com.businessanalyzer.model.DatosOperacion;
import com.businessanalyzer.model.DatosOperacion.DatosLinea;
import com.businessanalyzer.model.DatosProducto;
import com.businessanalyzer.model.Deuda;
import com.businessanalyzer.model.Empresa;
import com.businessanalyzer.model.Moneda;
import com.businessanalyzer.model.Operacion;
import com.businessanalyzer.model.Permiso;
import com.businessanalyzer.model.Producto;
import com.businessanalyzer.model.Sector;
import com.businessanalyzer.model.TamanoEmpresa;
import com.businessanalyzer.model.TipoCuenta;
import com.businessanalyzer.model.TipoItem;
import com.businessanalyzer.model.TipoMovimiento;
import com.businessanalyzer.model.TipoMovimientoTesoreria;
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
 * <p>
 * El dinero recorre tres cuentas, como en una bodega real: las ventas entran a la caja (y algunas por
 * Yape), las compras y los gastos fijos se pagan desde el banco, y a fin de mes el efectivo se deposita
 * en el banco dejando un fondo para el vuelto.
 */
public class DatosDemoService {

    public static final String NOMBRE_DEMO = "Bodega Demo";
    private static final BigDecimal IGV = new BigDecimal("18");
    /** Efectivo que se deja en caja al depositar a fin de mes. */
    private static final BigDecimal FONDO_CAJA = new BigDecimal("1000");

    private final EmpresaService empresas;
    private final CatalogoService catalogo;
    private final OperacionService operaciones;
    private final FinanzasService finanzas;
    private final PatrimonioService patrimonio;
    private final TesoreriaService tesoreria;
    private final Sesion sesion;
    private final Clock reloj;

    public DatosDemoService(EmpresaService empresas, CatalogoService catalogo, OperacionService operaciones,
                            FinanzasService finanzas, PatrimonioService patrimonio, TesoreriaService tesoreria,
                            Sesion sesion, Clock reloj) {
        this.patrimonio = patrimonio;
        this.empresas = empresas;
        this.catalogo = catalogo;
        this.operaciones = operaciones;
        this.finanzas = finanzas;
        this.tesoreria = tesoreria;
        this.sesion = sesion;
        this.reloj = reloj;
    }

    /** Cuentas de la bodega y el efectivo acumulado en caja y Yape desde el último depósito. */
    private static final class Cuentas {
        long caja;
        long banco;
        long yape;
        BigDecimal enCaja = BigDecimal.ZERO;
        BigDecimal enYape = BigDecimal.ZERO;
        /** Ventas en efectivo del día 16 en adelante del mes que se está generando. */
        BigDecimal segundaQuincena = BigDecimal.ZERO;
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
        Cuentas cuentas = crearCuentas();

        Producto arroz = item(TipoItem.PRODUCTO, "Arroz 5 kg", "18.50", "24.90");
        Producto azucar = item(TipoItem.PRODUCTO, "Azúcar rubia 1 kg", "3.20", "4.50");
        Producto aceite = item(TipoItem.PRODUCTO, "Aceite vegetal 1 l", "8.90", "11.80");
        Producto gaseosa = item(TipoItem.PRODUCTO, "Gaseosa 3 l", "7.40", "10.50");
        Producto detergente = item(TipoItem.PRODUCTO, "Detergente 2 kg", "14.20", "19.90");
        Producto agua = item(TipoItem.PRODUCTO, "Agua mineral (caja x15)", "16.00", "15.00");
        Producto delivery = item(TipoItem.SERVICIO, "Delivery a domicilio", "2.50", "5.00");

        LocalDate hoy = LocalDate.now(reloj);
        YearMonth actual = YearMonth.from(hoy);
        registrarPrestamoYEquipos(hoy, cuentas);
        // Volumen mensual creciente, con un pico en diciembre y un febrero flojo (el mes malo del año).
        int[] estacional = {92, 80, 97, 98, 100, 99, 103, 104, 101, 106, 112, 125};
        Producto[] productos = {arroz, azucar, aceite, gaseosa, detergente, agua, delivery};
        String[] precios = {"24.90", "4.50", "11.80", "10.50", "19.90", "15.00", "5.00"};
        // Semilla fija: los datos de demostración salen siempre iguales.
        java.util.Random azar = new java.util.Random(20_26L);
        for (int i = 12; i >= 0; i--) {
            YearMonth mes = actual.minusMonths(i);
            int v = (int) Math.round(3.2 * (100 + (12 - i) * 3) * estacional[mes.getMonthValue() - 1] / 100.0);
            // Se repone lo que se vende cada mes, con un pequeño colchón de stock (se paga desde el banco).
            compra(mes.atDay(1), hoy, cuentas.banco, l(arroz, v + 30, "18.50"), l(azucar, v * 3 + 35, "3.20"),
                    l(aceite, v + 12, "8.90"), l(gaseosa, v + 20, "7.40"), l(detergente, v / 2 + 8, "14.20"),
                    l(agua, 14, "16.00"));
            ventasDelMes(mes, hoy, azar, productos, precios,
                    new int[]{v + 10, v * 3, v, v + 8, v / 2, 12, 14 + (12 - i)}, cuentas);
            gasto(CategoriaFinanciera.ALQUILER, mes.atDay(5), hoy, "Alquiler del local", 1200, cuentas.banco);
            gasto(CategoriaFinanciera.SUELDOS, mes.atEndOfMonth(), hoy, "Planilla (2 trabajadores)", 2400, cuentas.banco);
            gasto(CategoriaFinanciera.SERVICIOS_BASICOS, mes.atDay(12), hoy, "Luz, agua e internet",
                    360 + (i % 3) * 15, cuentas.banco);
            gasto(CategoriaFinanciera.SERVICIOS_PROFESIONALES, mes.atDay(20), hoy, "Honorarios del contador", 450,
                    cuentas.banco);
            if (gasto(CategoriaFinanciera.TRANSPORTE, mes.atDay(9), hoy, "Fletes", v * 2, cuentas.caja)) {
                cuentas.enCaja = cuentas.enCaja.subtract(BigDecimal.valueOf(v * 2L));
            }
            gasto(CategoriaFinanciera.MARKETING, mes.atDay(2), hoy, "Publicidad en redes",
                    v / 2 + (i % 2) * 25, cuentas.banco);
            gasto(CategoriaFinanciera.IMPUESTO_RENTA, mes.atEndOfMonth(), hoy, "Pago a cuenta del impuesto a la renta",
                    380, cuentas.banco);
            if (movimiento(TipoMovimiento.INGRESO, CategoriaFinanciera.ALQUILERES_COBRADOS, mes.atDay(4), hoy,
                    "Alquiler de cochera", 200, cuentas.caja)) {
                cuentas.enCaja = cuentas.enCaja.add(BigDecimal.valueOf(200));
            }
            if (tesoreria(TipoMovimientoTesoreria.RETIRO, mes.atDay(15), hoy, cuentas.caja, null, new BigDecimal("600"),
                    "Retiro del dueño para gastos de la casa")) {
                cuentas.enCaja = cuentas.enCaja.subtract(new BigDecimal("600"));
            }
            depositarQuincena(mes, hoy, cuentas);
            depositarFinDeMes(mes, hoy, cuentas);
        }
        registrarCreditos(hoy, cuentas, arroz, aceite, detergente, gaseosa);
        patrimonio.registrarDeuda(new DatosDeuda(CategoriaDeuda.TRIBUTOS, "SUNAT", "IGV del mes por pagar",
                hoy, new BigDecimal("540"), null, null, null));
        return empresa;
    }

    /** La caja se crea con la empresa; se le pone el saldo de apertura y se agregan el banco y Yape. */
    private Cuentas crearCuentas() {
        Cuentas cuentas = new Cuentas();
        CuentaDinero caja = tesoreria.cuentasActivas().get(0);
        tesoreria.actualizarCuenta(caja.id(), new DatosCuenta(caja.nombre(), TipoCuenta.CAJA, null, new BigDecimal("800")));
        cuentas.caja = caja.id();
        cuentas.banco = tesoreria.registrarCuenta(new DatosCuenta("BCP Cuenta corriente", TipoCuenta.BANCO,
                "193-0000000-0-00", new BigDecimal("20000"))).id();
        cuentas.yape = tesoreria.registrarCuenta(new DatosCuenta("Yape", TipoCuenta.BILLETERA, "987 654 321",
                BigDecimal.ZERO)).id();
        cuentas.enCaja = new BigDecimal("800");
        return cuentas;
    }

    /**
     * Una venta por día (el cierre de caja de la bodega). Las cantidades del mes se reparten entre los
     * días según el día de la semana (sábado fuerte, domingo y lunes flojos) con algo de variación, y se
     * redondean acumulando el resto para que el total del mes se mantenga. Uno de cada cuatro días los
     * clientes pagan sobre todo con Yape.
     */
    private void ventasDelMes(YearMonth mes, LocalDate hoy, java.util.Random azar, Producto[] productos,
                              String[] precios, int[] totalesMes, Cuentas cuentas) {
        cuentas.segundaQuincena = BigDecimal.ZERO;
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
                boolean porYape = fecha.getDayOfMonth() % 4 == 0;
                Operacion venta = operaciones.registrar(new DatosOperacion(TipoOperacion.VENTA, fecha, "Clientes varios",
                        null, null, IGV, "Cierre de caja del día", lineas, false, CondicionPago.CONTADO,
                        porYape ? cuentas.yape : cuentas.caja, null), true);
                if (porYape) {
                    cuentas.enYape = cuentas.enYape.add(venta.total());
                } else {
                    cuentas.enCaja = cuentas.enCaja.add(venta.total());
                    if (fecha.getDayOfMonth() > 15) {
                        cuentas.segundaQuincena = cuentas.segundaQuincena.add(venta.total());
                    }
                }
            }
        }
    }

    /**
     * El día 15 se deposita lo vendido en efectivo en la primera quincena (menos fletes y el retiro del dueño),
     * dejando el fondo para el vuelto.
     */
    private void depositarQuincena(YearMonth mes, LocalDate hoy, Cuentas cuentas) {
        LocalDate quincena = mes.atDay(15);
        // Efectivo en caja al cierre del día 15: lo acumulado ese día menos lo que se vendió en la segunda quincena.
        BigDecimal alQuince = cuentas.enCaja.subtract(cuentas.segundaQuincena);
        BigDecimal deposito = alQuince.subtract(FONDO_CAJA);
        if (deposito.signum() > 0 && tesoreria(TipoMovimientoTesoreria.TRANSFERENCIA, quincena, hoy, cuentas.caja,
                cuentas.banco, deposito, "Depósito de la primera quincena")) {
            cuentas.enCaja = cuentas.enCaja.subtract(deposito);
        }
    }

    /** El último día del mes se deposita el efectivo en el banco (dejando el fondo de caja) y se pasa lo de Yape. */
    private void depositarFinDeMes(YearMonth mes, LocalDate hoy, Cuentas cuentas) {
        LocalDate fin = mes.atEndOfMonth();
        BigDecimal deposito = cuentas.enCaja.subtract(FONDO_CAJA);
        if (deposito.signum() > 0 && tesoreria(TipoMovimientoTesoreria.TRANSFERENCIA, fin, hoy, cuentas.caja,
                cuentas.banco, deposito, "Depósito de la venta del mes")) {
            cuentas.enCaja = FONDO_CAJA;
        }
        if (cuentas.enYape.signum() > 0 && tesoreria(TipoMovimientoTesoreria.TRANSFERENCIA, fin, hoy, cuentas.yape,
                cuentas.banco, cuentas.enYape, "Pase de Yape al banco")) {
            cuentas.enYape = BigDecimal.ZERO;
        }
    }

    /**
     * Préstamo de la caja municipal: el dinero entra al banco, con él se compra la motocicleta del delivery
     * y luego se paga una cuota al mes (capital + intereses) desde el banco. Los demás equipos ya se tenían.
     */
    private void registrarPrestamoYEquipos(LocalDate hoy, Cuentas cuentas) {
        activo(CategoriaActivo.MOBILIARIO, "Anaqueles y mostrador", hoy.minusYears(3), "4500", 10, null, null);
        activo(CategoriaActivo.MOBILIARIO, "Refrigeradora exhibidora", hoy.minusMonths(14), "3200", 10, "300", null);
        activo(CategoriaActivo.EQUIPOS_COMPUTO, "Computadora y caja registradora", hoy.minusYears(2), "2600", 4, null,
                null);

        LocalDate inicio = hoy.minusMonths(12).withDayOfMonth(1);
        Deuda prestamo = patrimonio.registrarDeuda(new DatosDeuda(CategoriaDeuda.PRESTAMO_BANCARIO,
                "Caja Municipal", "Préstamo para equipar la bodega", inicio, new BigDecimal("12000"),
                new BigDecimal("16"), new BigDecimal("560"), inicio.plusMonths(36), cuentas.banco));
        activo(CategoriaActivo.VEHICULOS, "Motocicleta para delivery", inicio.plusDays(3), "5500", 5, "800",
                cuentas.banco);
        for (int i = 1; i <= 12; i++) {
            LocalDate fecha = inicio.plusMonths(i).withDayOfMonth(10);
            if (!fecha.isAfter(hoy)) {
                patrimonio.registrarPago(prestamo.id(), fecha, new BigDecimal("400"), new BigDecimal("160"),
                        cuentas.banco, "Cuota " + i + " de 36");
            }
        }
    }

    /**
     * Ventas y compras al crédito del último mes y medio: un restaurante que paga en partes (con una
     * deuda vencida), una venta reciente por cobrar y mercadería del distribuidor a 30 días.
     */
    private void registrarCreditos(LocalDate hoy, Cuentas cuentas, Producto arroz, Producto aceite,
                                   Producto detergente, Producto gaseosa) {
        Operacion vencida = operaciones.registrar(new DatosOperacion(TipoOperacion.VENTA, hoy.minusDays(40),
                "Restaurante Doña Rosa", "10456789012", null, IGV, "Pedido mensual del restaurante",
                List.of(l(arroz, 20, "24.90"), l(aceite, 15, "11.80")), false, CondicionPago.CREDITO, null,
                hoy.minusDays(10)), true);
        tesoreria.registrarPago(vencida.id(), cuentas.yape, hoy.minusDays(25), new BigDecimal("300"), "Adelanto por Yape");
        operaciones.registrar(new DatosOperacion(TipoOperacion.VENTA, hoy.minusDays(6), "Bodeguita San Martín",
                null, null, IGV, "Venta al por mayor", List.of(l(gaseosa, 24, "10.00"), l(detergente, 6, "19.00")),
                false, CondicionPago.CREDITO, null, hoy.plusDays(9)), true);
        operaciones.registrar(new DatosOperacion(TipoOperacion.COMPRA, hoy.minusDays(5), "Distribuidora Lima",
                "20512345678", "F001-004512", IGV, "Mercadería al crédito a 30 días",
                List.of(l(detergente, 80, "14.20"), l(aceite, 90, "8.90")), false, CondicionPago.CREDITO, null,
                hoy.plusDays(25)), true);
    }

    private void activo(CategoriaActivo categoria, String descripcion, LocalDate fecha, String valor, Integer vida,
                        String residual, Long cuenta) {
        patrimonio.registrarActivo(new DatosActivo(categoria, descripcion, fecha, new BigDecimal(valor), vida,
                residual == null ? null : new BigDecimal(residual), cuenta));
    }

    private Producto item(TipoItem tipo, String nombre, String costo, String precio) {
        return catalogo.crearProducto(new DatosProducto(tipo, null, nombre, null, null,
                tipo == TipoItem.SERVICIO ? UnidadMedida.SERVICIO : UnidadMedida.UNIDAD,
                new BigDecimal(costo), new BigDecimal(precio)));
    }

    private static DatosLinea l(Producto p, int cantidad, String precio) {
        return new DatosLinea(p.id(), BigDecimal.valueOf(cantidad), new BigDecimal(precio));
    }

    private void compra(LocalDate fecha, LocalDate hoy, long cuenta, DatosLinea... lineas) {
        if (!fecha.isAfter(hoy)) {
            operaciones.registrar(new DatosOperacion(TipoOperacion.COMPRA, fecha, null, null, null, IGV, null,
                    List.of(lineas), false, CondicionPago.CONTADO, cuenta, null), true);
        }
    }

    private boolean gasto(CategoriaFinanciera categoria, LocalDate fecha, LocalDate hoy, String descripcion, int monto,
                          long cuenta) {
        return movimiento(TipoMovimiento.GASTO, categoria, fecha, hoy, descripcion, monto, cuenta);
    }

    /** Registra el ingreso o gasto si la fecha ya pasó; devuelve si se registró. */
    private boolean movimiento(TipoMovimiento tipo, CategoriaFinanciera categoria, LocalDate fecha, LocalDate hoy,
                               String descripcion, int monto, long cuenta) {
        if (fecha.isAfter(hoy)) {
            return false;
        }
        finanzas.registrar(new DatosMovimiento(tipo, categoria, null, fecha, descripcion,
                BigDecimal.valueOf(monto), null, cuenta));
        return true;
    }

    /** Registra la transferencia, aporte o retiro si la fecha ya pasó; devuelve si se registró. */
    private boolean tesoreria(TipoMovimientoTesoreria tipo, LocalDate fecha, LocalDate hoy, Long origen, Long destino,
                              BigDecimal monto, String descripcion) {
        if (fecha.isAfter(hoy)) {
            return false;
        }
        tesoreria.registrarMovimiento(new DatosMovimientoTesoreria(tipo, fecha, origen, destino, monto, descripcion));
        return true;
    }
}
