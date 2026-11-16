package com.businessanalyzer.service;

import com.businessanalyzer.exception.ValidacionExceptionFGEM;
import com.businessanalyzer.model.CategoriaActivoFGEM;
import com.businessanalyzer.model.CategoriaDeudaFGEM;
import com.businessanalyzer.model.CategoriaFinancieraFGEM;
import com.businessanalyzer.model.CondicionPagoFGEM;
import com.businessanalyzer.model.CuentaDineroFGEM;
import com.businessanalyzer.model.DatosActivoFGEM;
import com.businessanalyzer.model.DatosCuentaFGEM;
import com.businessanalyzer.model.DatosDeudaFGEM;
import com.businessanalyzer.model.DatosEmpresaFGEM;
import com.businessanalyzer.model.DatosMovimientoFGEM;
import com.businessanalyzer.model.DatosMovimientoTesoreriaFGEM;
import com.businessanalyzer.model.DatosOperacionFGEM;
import com.businessanalyzer.model.DatosOperacionFGEM.DatosLineaFGEM;
import com.businessanalyzer.model.DatosProductoFGEM;
import com.businessanalyzer.model.DeudaFGEM;
import com.businessanalyzer.model.EmpresaFGEM;
import com.businessanalyzer.model.MonedaFGEM;
import com.businessanalyzer.model.OperacionFGEM;
import com.businessanalyzer.model.PermisoFGEM;
import com.businessanalyzer.model.ProductoFGEM;
import com.businessanalyzer.model.SectorFGEM;
import com.businessanalyzer.model.TamanoEmpresaFGEM;
import com.businessanalyzer.model.TipoCuentaFGEM;
import com.businessanalyzer.model.TipoItemFGEM;
import com.businessanalyzer.model.TipoMovimientoFGEM;
import com.businessanalyzer.model.TipoMovimientoTesoreriaFGEM;
import com.businessanalyzer.model.TipoOperacionFGEM;
import com.businessanalyzer.model.UnidadMedidaFGEM;
import com.businessanalyzer.seguridad.SesionFGEM;

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
public class DatosDemoServiceFGEM {

    public static final String NOMBRE_DEMO_FGEM = "Bodega Demo";
    private static final BigDecimal IGV_FGEM = new BigDecimal("18");
    /** Efectivo que se deja en caja al depositar a fin de mes. */
    private static final BigDecimal FONDO_CAJA_FGEM = new BigDecimal("1000");

    private final EmpresaServiceFGEM empresasFGEM;
    private final CatalogoServiceFGEM catalogoFGEM;
    private final OperacionServiceFGEM operacionesFGEM;
    private final FinanzasServiceFGEM finanzasFGEM;
    private final PatrimonioServiceFGEM patrimonioFGEM;
    private final TesoreriaServiceFGEM tesoreriaFGEM;
    private final SesionFGEM sesionFGEM;
    private final Clock relojFGEM;

    public DatosDemoServiceFGEM(EmpresaServiceFGEM empresasFGEM, CatalogoServiceFGEM catalogoFGEM, OperacionServiceFGEM operacionesFGEM,
                            FinanzasServiceFGEM finanzasFGEM, PatrimonioServiceFGEM patrimonioFGEM, TesoreriaServiceFGEM tesoreriaFGEM,
                            SesionFGEM sesionFGEM, Clock relojFGEM) {
        this.patrimonioFGEM = patrimonioFGEM;
        this.empresasFGEM = empresasFGEM;
        this.catalogoFGEM = catalogoFGEM;
        this.operacionesFGEM = operacionesFGEM;
        this.finanzasFGEM = finanzasFGEM;
        this.tesoreriaFGEM = tesoreriaFGEM;
        this.sesionFGEM = sesionFGEM;
        this.relojFGEM = relojFGEM;
    }

    /** Cuentas de la bodega y el efectivo acumulado en caja y Yape desde el último depósito. */
    private static final class CuentasFGEM {
        long cajaFGEM;
        long bancoFGEM;
        long yapeFGEM;
        BigDecimal enCajaFGEM = BigDecimal.ZERO;
        BigDecimal enYapeFGEM = BigDecimal.ZERO;
        /** Ventas en efectivo del día 16 en adelante del mes que se está generando. */
        BigDecimal segundaQuincenaFGEM = BigDecimal.ZERO;
    }

    public EmpresaFGEM crearEmpresaDemoFGEM() {
        sesionFGEM.requerirFGEM(PermisoFGEM.EMPRESAS_GESTIONAR);
        sesionFGEM.requerirFGEM(PermisoFGEM.PRODUCTOS_GESTIONAR);
        sesionFGEM.requerirFGEM(PermisoFGEM.OPERACIONES_GESTIONAR);
        sesionFGEM.requerirFGEM(PermisoFGEM.FINANZAS_GESTIONAR);
        boolean existeFGEM = empresasFGEM.listarFGEM(true).stream().anyMatch(eFGEM -> NOMBRE_DEMO_FGEM.equalsIgnoreCase(eFGEM.nombreVisibleFGEM()));
        if (existeFGEM) {
            throw new ValidacionExceptionFGEM("Ya existe la empresa \"" + NOMBRE_DEMO_FGEM + "\". Selecciónala en la barra superior.");
        }

        EmpresaFGEM empresaFGEM = empresasFGEM.crearFGEM(new DatosEmpresaFGEM("Bodega Demo E.I.R.L.", NOMBRE_DEMO_FGEM, null,
                SectorFGEM.COMERCIO, TamanoEmpresaFGEM.MICRO, MonedaFGEM.PEN, 1, null, "Datos ficticios de demostración",
                null, null));
        empresasFGEM.seleccionarEmpresaActivaFGEM(empresaFGEM.idFGEM());
        CuentasFGEM cuentasFGEM = crearCuentasFGEM();

        ProductoFGEM arrozFGEM = itemFGEM(TipoItemFGEM.PRODUCTO, "Arroz 5 kg", "18.50", "24.90");
        ProductoFGEM azucarFGEM = itemFGEM(TipoItemFGEM.PRODUCTO, "Azúcar rubia 1 kg", "3.20", "4.50");
        ProductoFGEM aceiteFGEM = itemFGEM(TipoItemFGEM.PRODUCTO, "Aceite vegetal 1 l", "8.90", "11.80");
        ProductoFGEM gaseosaFGEM = itemFGEM(TipoItemFGEM.PRODUCTO, "Gaseosa 3 l", "7.40", "10.50");
        ProductoFGEM detergenteFGEM = itemFGEM(TipoItemFGEM.PRODUCTO, "Detergente 2 kg", "14.20", "19.90");
        ProductoFGEM aguaFGEM = itemFGEM(TipoItemFGEM.PRODUCTO, "Agua mineral (caja x15)", "16.00", "15.00");
        ProductoFGEM deliveryFGEM = itemFGEM(TipoItemFGEM.SERVICIO, "Delivery a domicilio", "2.50", "5.00");

        LocalDate hoyFGEM = LocalDate.now(relojFGEM);
        YearMonth actualFGEM = YearMonth.from(hoyFGEM);
        registrarPrestamoYEquiposFGEM(hoyFGEM, cuentasFGEM);
        // Volumen mensual creciente, con un pico en diciembre y un febrero flojo (el mes malo del año).
        int[] estacionalFGEM = {92, 80, 97, 98, 100, 99, 103, 104, 101, 106, 112, 125};
        ProductoFGEM[] productosFGEM = {arrozFGEM, azucarFGEM, aceiteFGEM, gaseosaFGEM, detergenteFGEM, aguaFGEM, deliveryFGEM};
        String[] preciosFGEM = {"24.90", "4.50", "11.80", "10.50", "19.90", "15.00", "5.00"};
        // Semilla fija: los datos de demostración salen siempre iguales.
        java.util.Random azarFGEM = new java.util.Random(20_26L);
        for (int iFGEM = 12; iFGEM >= 0; iFGEM--) {
            YearMonth mesFGEM = actualFGEM.minusMonths(iFGEM);
            int vFGEM = (int) Math.round(3.2 * (100 + (12 - iFGEM) * 3) * estacionalFGEM[mesFGEM.getMonthValue() - 1] / 100.0);
            // Se repone lo que se vende cada mes, con un pequeño colchón de stock (se paga desde el banco).
            compraFGEM(mesFGEM.atDay(1), hoyFGEM, cuentasFGEM.bancoFGEM, lFGEM(arrozFGEM, vFGEM + 30, "18.50"), lFGEM(azucarFGEM, vFGEM * 3 + 35, "3.20"),
                    lFGEM(aceiteFGEM, vFGEM + 12, "8.90"), lFGEM(gaseosaFGEM, vFGEM + 20, "7.40"), lFGEM(detergenteFGEM, vFGEM / 2 + 8, "14.20"),
                    lFGEM(aguaFGEM, 14, "16.00"));
            ventasDelMesFGEM(mesFGEM, hoyFGEM, azarFGEM, productosFGEM, preciosFGEM,
                    new int[]{vFGEM + 10, vFGEM * 3, vFGEM, vFGEM + 8, vFGEM / 2, 12, 14 + (12 - iFGEM)}, cuentasFGEM);
            gastoFGEM(CategoriaFinancieraFGEM.ALQUILER, mesFGEM.atDay(5), hoyFGEM, "Alquiler del local", 1200, cuentasFGEM.bancoFGEM);
            gastoFGEM(CategoriaFinancieraFGEM.SUELDOS, mesFGEM.atEndOfMonth(), hoyFGEM, "Planilla (2 trabajadores)", 2400, cuentasFGEM.bancoFGEM);
            gastoFGEM(CategoriaFinancieraFGEM.SERVICIOS_BASICOS, mesFGEM.atDay(12), hoyFGEM, "Luz, agua e internet",
                    360 + (iFGEM % 3) * 15, cuentasFGEM.bancoFGEM);
            gastoFGEM(CategoriaFinancieraFGEM.SERVICIOS_PROFESIONALES, mesFGEM.atDay(20), hoyFGEM, "Honorarios del contador", 450,
                    cuentasFGEM.bancoFGEM);
            if (gastoFGEM(CategoriaFinancieraFGEM.TRANSPORTE, mesFGEM.atDay(9), hoyFGEM, "Fletes", vFGEM * 2, cuentasFGEM.cajaFGEM)) {
                cuentasFGEM.enCajaFGEM = cuentasFGEM.enCajaFGEM.subtract(BigDecimal.valueOf(vFGEM * 2L));
            }
            gastoFGEM(CategoriaFinancieraFGEM.MARKETING, mesFGEM.atDay(2), hoyFGEM, "Publicidad en redes",
                    vFGEM / 2 + (iFGEM % 2) * 25, cuentasFGEM.bancoFGEM);
            gastoFGEM(CategoriaFinancieraFGEM.IMPUESTO_RENTA, mesFGEM.atEndOfMonth(), hoyFGEM, "Pago a cuenta del impuesto a la renta",
                    380, cuentasFGEM.bancoFGEM);
            if (movimientoFGEM(TipoMovimientoFGEM.INGRESO, CategoriaFinancieraFGEM.ALQUILERES_COBRADOS, mesFGEM.atDay(4), hoyFGEM,
                    "Alquiler de cochera", 200, cuentasFGEM.cajaFGEM)) {
                cuentasFGEM.enCajaFGEM = cuentasFGEM.enCajaFGEM.add(BigDecimal.valueOf(200));
            }
            if (tesoreriaFGEM(TipoMovimientoTesoreriaFGEM.RETIRO, mesFGEM.atDay(15), hoyFGEM, cuentasFGEM.cajaFGEM, null, new BigDecimal("600"),
                    "Retiro del dueño para gastos de la casa")) {
                cuentasFGEM.enCajaFGEM = cuentasFGEM.enCajaFGEM.subtract(new BigDecimal("600"));
            }
            depositarQuincenaFGEM(mesFGEM, hoyFGEM, cuentasFGEM);
            depositarFinDeMesFGEM(mesFGEM, hoyFGEM, cuentasFGEM);
        }
        registrarCreditosFGEM(hoyFGEM, cuentasFGEM, arrozFGEM, aceiteFGEM, detergenteFGEM, gaseosaFGEM);
        patrimonioFGEM.registrarDeudaFGEM(new DatosDeudaFGEM(CategoriaDeudaFGEM.TRIBUTOS, "SUNAT", "IGV del mes por pagar",
                hoyFGEM, new BigDecimal("540"), null, null, null));
        return empresaFGEM;
    }

    /** La caja se crea con la empresa; se le pone el saldo de apertura y se agregan el banco y Yape. */
    private CuentasFGEM crearCuentasFGEM() {
        CuentasFGEM cuentasFGEM = new CuentasFGEM();
        CuentaDineroFGEM cajaFGEM = tesoreriaFGEM.cuentasActivasFGEM().get(0);
        tesoreriaFGEM.actualizarCuentaFGEM(cajaFGEM.idFGEM(), new DatosCuentaFGEM(cajaFGEM.nombreFGEM(), TipoCuentaFGEM.CAJA, null, new BigDecimal("800")));
        cuentasFGEM.cajaFGEM = cajaFGEM.idFGEM();
        cuentasFGEM.bancoFGEM = tesoreriaFGEM.registrarCuentaFGEM(new DatosCuentaFGEM("BCP Cuenta corriente", TipoCuentaFGEM.BANCO,
                "193-0000000-0-00", new BigDecimal("20000"))).idFGEM();
        cuentasFGEM.yapeFGEM = tesoreriaFGEM.registrarCuentaFGEM(new DatosCuentaFGEM("Yape", TipoCuentaFGEM.BILLETERA, "987 654 321",
                BigDecimal.ZERO)).idFGEM();
        cuentasFGEM.enCajaFGEM = new BigDecimal("800");
        return cuentasFGEM;
    }

    /**
     * Una venta por día (el cierre de caja de la bodega). Las cantidades del mes se reparten entre los
     * días según el día de la semana (sábado fuerte, domingo y lunes flojos) con algo de variación, y se
     * redondean acumulando el resto para que el total del mes se mantenga. Uno de cada cuatro días los
     * clientes pagan sobre todo con Yape.
     */
    private void ventasDelMesFGEM(YearMonth mesFGEM, LocalDate hoyFGEM, java.util.Random azarFGEM, ProductoFGEM[] productosFGEM,
                              String[] preciosFGEM, int[] totalesMesFGEM, CuentasFGEM cuentasFGEM) {
        cuentasFGEM.segundaQuincenaFGEM = BigDecimal.ZERO;
        double[] pesoDiaFGEM = {0.9, 0.95, 1.0, 1.0, 1.15, 1.35, 0.65}; // lunes … domingo
        double[] pesosFGEM = new double[mesFGEM.lengthOfMonth()];
        double sumaFGEM = 0;
        for (int dFGEM = 0; dFGEM < pesosFGEM.length; dFGEM++) {
            pesosFGEM[dFGEM] = pesoDiaFGEM[mesFGEM.atDay(dFGEM + 1).getDayOfWeek().getValue() - 1] * (0.85 + azarFGEM.nextDouble() * 0.3);
            sumaFGEM += pesosFGEM[dFGEM];
        }
        double[] acumuladoFGEM = new double[productosFGEM.length];
        for (int dFGEM = 0; dFGEM < pesosFGEM.length; dFGEM++) {
            LocalDate fechaFGEM = mesFGEM.atDay(dFGEM + 1);
            List<DatosLineaFGEM> lineasFGEM = new java.util.ArrayList<>();
            for (int pFGEM = 0; pFGEM < productosFGEM.length; pFGEM++) {
                acumuladoFGEM[pFGEM] += totalesMesFGEM[pFGEM] * pesosFGEM[dFGEM] / sumaFGEM;
                int cantidadFGEM = (int) acumuladoFGEM[pFGEM];
                acumuladoFGEM[pFGEM] -= cantidadFGEM;
                if (cantidadFGEM > 0) {
                    lineasFGEM.add(lFGEM(productosFGEM[pFGEM], cantidadFGEM, preciosFGEM[pFGEM]));
                }
            }
            if (!fechaFGEM.isAfter(hoyFGEM) && !lineasFGEM.isEmpty()) {
                boolean porYapeFGEM = fechaFGEM.getDayOfMonth() % 4 == 0;
                OperacionFGEM ventaFGEM = operacionesFGEM.registrarFGEM(new DatosOperacionFGEM(TipoOperacionFGEM.VENTA, fechaFGEM, "Clientes varios",
                        null, null, IGV_FGEM, "Cierre de caja del día", lineasFGEM, false, CondicionPagoFGEM.CONTADO,
                        porYapeFGEM ? cuentasFGEM.yapeFGEM : cuentasFGEM.cajaFGEM, null), true);
                if (porYapeFGEM) {
                    cuentasFGEM.enYapeFGEM = cuentasFGEM.enYapeFGEM.add(ventaFGEM.totalFGEM());
                } else {
                    cuentasFGEM.enCajaFGEM = cuentasFGEM.enCajaFGEM.add(ventaFGEM.totalFGEM());
                    if (fechaFGEM.getDayOfMonth() > 15) {
                        cuentasFGEM.segundaQuincenaFGEM = cuentasFGEM.segundaQuincenaFGEM.add(ventaFGEM.totalFGEM());
                    }
                }
            }
        }
    }

    /**
     * El día 15 se deposita lo vendido en efectivo en la primera quincena (menos fletes y el retiro del dueño),
     * dejando el fondo para el vuelto.
     */
    private void depositarQuincenaFGEM(YearMonth mesFGEM, LocalDate hoyFGEM, CuentasFGEM cuentasFGEM) {
        LocalDate quincenaFGEM = mesFGEM.atDay(15);
        // Efectivo en caja al cierre del día 15: lo acumulado ese día menos lo que se vendió en la segunda quincena.
        BigDecimal alQuinceFGEM = cuentasFGEM.enCajaFGEM.subtract(cuentasFGEM.segundaQuincenaFGEM);
        BigDecimal depositoFGEM = alQuinceFGEM.subtract(FONDO_CAJA_FGEM);
        if (depositoFGEM.signum() > 0 && tesoreriaFGEM(TipoMovimientoTesoreriaFGEM.TRANSFERENCIA, quincenaFGEM, hoyFGEM, cuentasFGEM.cajaFGEM,
                cuentasFGEM.bancoFGEM, depositoFGEM, "Depósito de la primera quincena")) {
            cuentasFGEM.enCajaFGEM = cuentasFGEM.enCajaFGEM.subtract(depositoFGEM);
        }
    }

    /** El último día del mes se deposita el efectivo en el banco (dejando el fondo de caja) y se pasa lo de Yape. */
    private void depositarFinDeMesFGEM(YearMonth mesFGEM, LocalDate hoyFGEM, CuentasFGEM cuentasFGEM) {
        LocalDate finFGEM = mesFGEM.atEndOfMonth();
        BigDecimal depositoFGEM = cuentasFGEM.enCajaFGEM.subtract(FONDO_CAJA_FGEM);
        if (depositoFGEM.signum() > 0 && tesoreriaFGEM(TipoMovimientoTesoreriaFGEM.TRANSFERENCIA, finFGEM, hoyFGEM, cuentasFGEM.cajaFGEM,
                cuentasFGEM.bancoFGEM, depositoFGEM, "Depósito de la venta del mes")) {
            cuentasFGEM.enCajaFGEM = FONDO_CAJA_FGEM;
        }
        if (cuentasFGEM.enYapeFGEM.signum() > 0 && tesoreriaFGEM(TipoMovimientoTesoreriaFGEM.TRANSFERENCIA, finFGEM, hoyFGEM, cuentasFGEM.yapeFGEM,
                cuentasFGEM.bancoFGEM, cuentasFGEM.enYapeFGEM, "Pase de Yape al banco")) {
            cuentasFGEM.enYapeFGEM = BigDecimal.ZERO;
        }
    }

    /**
     * Préstamo de la caja municipal: el dinero entra al banco, con él se compra la motocicleta del delivery
     * y luego se paga una cuota al mes (capital + intereses) desde el banco. Los demás equipos ya se tenían.
     */
    private void registrarPrestamoYEquiposFGEM(LocalDate hoyFGEM, CuentasFGEM cuentasFGEM) {
        activoFGEM(CategoriaActivoFGEM.MOBILIARIO, "Anaqueles y mostrador", hoyFGEM.minusYears(3), "4500", 10, null, null);
        activoFGEM(CategoriaActivoFGEM.MOBILIARIO, "Refrigeradora exhibidora", hoyFGEM.minusMonths(14), "3200", 10, "300", null);
        activoFGEM(CategoriaActivoFGEM.EQUIPOS_COMPUTO, "Computadora y caja registradora", hoyFGEM.minusYears(2), "2600", 4, null,
                null);

        LocalDate inicioFGEM = hoyFGEM.minusMonths(12).withDayOfMonth(1);
        DeudaFGEM prestamoFGEM = patrimonioFGEM.registrarDeudaFGEM(new DatosDeudaFGEM(CategoriaDeudaFGEM.PRESTAMO_BANCARIO,
                "Caja Municipal", "Préstamo para equipar la bodega", inicioFGEM, new BigDecimal("12000"),
                new BigDecimal("16"), new BigDecimal("560"), inicioFGEM.plusMonths(36), cuentasFGEM.bancoFGEM));
        activoFGEM(CategoriaActivoFGEM.VEHICULOS, "Motocicleta para delivery", inicioFGEM.plusDays(3), "5500", 5, "800",
                cuentasFGEM.bancoFGEM);
        for (int iFGEM = 1; iFGEM <= 12; iFGEM++) {
            LocalDate fechaFGEM = inicioFGEM.plusMonths(iFGEM).withDayOfMonth(10);
            if (!fechaFGEM.isAfter(hoyFGEM)) {
                patrimonioFGEM.registrarPagoFGEM(prestamoFGEM.idFGEM(), fechaFGEM, new BigDecimal("400"), new BigDecimal("160"),
                        cuentasFGEM.bancoFGEM, "Cuota " + iFGEM + " de 36");
            }
        }
    }

    /**
     * Ventas y compras al crédito del último mes y medio: un restaurante que paga en partes (con una
     * deuda vencida), una venta reciente por cobrar y mercadería del distribuidor a 30 días.
     */
    private void registrarCreditosFGEM(LocalDate hoyFGEM, CuentasFGEM cuentasFGEM, ProductoFGEM arrozFGEM, ProductoFGEM aceiteFGEM,
                                   ProductoFGEM detergenteFGEM, ProductoFGEM gaseosaFGEM) {
        OperacionFGEM vencidaFGEM = operacionesFGEM.registrarFGEM(new DatosOperacionFGEM(TipoOperacionFGEM.VENTA, hoyFGEM.minusDays(40),
                "Restaurante Doña Rosa", "10456789012", null, IGV_FGEM, "Pedido mensual del restaurante",
                List.of(lFGEM(arrozFGEM, 20, "24.90"), lFGEM(aceiteFGEM, 15, "11.80")), false, CondicionPagoFGEM.CREDITO, null,
                hoyFGEM.minusDays(10)), true);
        tesoreriaFGEM.registrarPagoFGEM(vencidaFGEM.idFGEM(), cuentasFGEM.yapeFGEM, hoyFGEM.minusDays(25), new BigDecimal("300"), "Adelanto por Yape");
        operacionesFGEM.registrarFGEM(new DatosOperacionFGEM(TipoOperacionFGEM.VENTA, hoyFGEM.minusDays(6), "Bodeguita San Martín",
                null, null, IGV_FGEM, "Venta al por mayor", List.of(lFGEM(gaseosaFGEM, 24, "10.00"), lFGEM(detergenteFGEM, 6, "19.00")),
                false, CondicionPagoFGEM.CREDITO, null, hoyFGEM.plusDays(9)), true);
        operacionesFGEM.registrarFGEM(new DatosOperacionFGEM(TipoOperacionFGEM.COMPRA, hoyFGEM.minusDays(5), "Distribuidora Lima",
                "20512345678", "F001-004512", IGV_FGEM, "Mercadería al crédito a 30 días",
                List.of(lFGEM(detergenteFGEM, 80, "14.20"), lFGEM(aceiteFGEM, 90, "8.90")), false, CondicionPagoFGEM.CREDITO, null,
                hoyFGEM.plusDays(25)), true);
    }

    private void activoFGEM(CategoriaActivoFGEM categoriaFGEM, String descripcionFGEM, LocalDate fechaFGEM, String valorFGEM, Integer vidaFGEM,
                        String residualFGEM, Long cuentaFGEM) {
        patrimonioFGEM.registrarActivoFGEM(new DatosActivoFGEM(categoriaFGEM, descripcionFGEM, fechaFGEM, new BigDecimal(valorFGEM), vidaFGEM,
                residualFGEM == null ? null : new BigDecimal(residualFGEM), cuentaFGEM));
    }

    private ProductoFGEM itemFGEM(TipoItemFGEM tipoFGEM, String nombreFGEM, String costoFGEM, String precioFGEM) {
        return catalogoFGEM.crearProductoFGEM(new DatosProductoFGEM(tipoFGEM, null, nombreFGEM, null, null,
                tipoFGEM == TipoItemFGEM.SERVICIO ? UnidadMedidaFGEM.SERVICIO : UnidadMedidaFGEM.UNIDAD,
                new BigDecimal(costoFGEM), new BigDecimal(precioFGEM)));
    }

    private static DatosLineaFGEM lFGEM(ProductoFGEM pFGEM, int cantidadFGEM, String precioFGEM) {
        return new DatosLineaFGEM(pFGEM.idFGEM(), BigDecimal.valueOf(cantidadFGEM), new BigDecimal(precioFGEM));
    }

    private void compraFGEM(LocalDate fechaFGEM, LocalDate hoyFGEM, long cuentaFGEM, DatosLineaFGEM... lineasFGEM) {
        if (!fechaFGEM.isAfter(hoyFGEM)) {
            operacionesFGEM.registrarFGEM(new DatosOperacionFGEM(TipoOperacionFGEM.COMPRA, fechaFGEM, null, null, null, IGV_FGEM, null,
                    List.of(lineasFGEM), false, CondicionPagoFGEM.CONTADO, cuentaFGEM, null), true);
        }
    }

    private boolean gastoFGEM(CategoriaFinancieraFGEM categoriaFGEM, LocalDate fechaFGEM, LocalDate hoyFGEM, String descripcionFGEM, int montoFGEM,
                          long cuentaFGEM) {
        return movimientoFGEM(TipoMovimientoFGEM.GASTO, categoriaFGEM, fechaFGEM, hoyFGEM, descripcionFGEM, montoFGEM, cuentaFGEM);
    }

    /** Registra el ingreso o gasto si la fecha ya pasó; devuelve si se registró. */
    private boolean movimientoFGEM(TipoMovimientoFGEM tipoFGEM, CategoriaFinancieraFGEM categoriaFGEM, LocalDate fechaFGEM, LocalDate hoyFGEM,
                               String descripcionFGEM, int montoFGEM, long cuentaFGEM) {
        if (fechaFGEM.isAfter(hoyFGEM)) {
            return false;
        }
        finanzasFGEM.registrarFGEM(new DatosMovimientoFGEM(tipoFGEM, categoriaFGEM, null, fechaFGEM, descripcionFGEM,
                BigDecimal.valueOf(montoFGEM), null, cuentaFGEM));
        return true;
    }

    /** Registra la transferencia, aporte o retiro si la fecha ya pasó; devuelve si se registró. */
    private boolean tesoreriaFGEM(TipoMovimientoTesoreriaFGEM tipoFGEM, LocalDate fechaFGEM, LocalDate hoyFGEM, Long origenFGEM, Long destinoFGEM,
                              BigDecimal montoFGEM, String descripcionFGEM) {
        if (fechaFGEM.isAfter(hoyFGEM)) {
            return false;
        }
        tesoreriaFGEM.registrarMovimientoFGEM(new DatosMovimientoTesoreriaFGEM(tipoFGEM, fechaFGEM, origenFGEM, destinoFGEM, montoFGEM, descripcionFGEM));
        return true;
    }
}
