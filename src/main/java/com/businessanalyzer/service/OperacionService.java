package com.businessanalyzer.service;

import com.businessanalyzer.analytics.CalculadoraOperacion;
import com.businessanalyzer.analytics.ResumenOperaciones;
import com.businessanalyzer.config.Database;
import com.businessanalyzer.exception.StockInsuficienteException;
import com.businessanalyzer.exception.ValidacionException;
import com.businessanalyzer.model.DatosOperacion;
import com.businessanalyzer.model.DatosOperacion.DatosLinea;
import com.businessanalyzer.model.Empresa;
import com.businessanalyzer.model.Operacion;
import com.businessanalyzer.model.Permiso;
import com.businessanalyzer.model.Producto;
import com.businessanalyzer.model.TipoItem;
import com.businessanalyzer.model.TipoOperacion;
import com.businessanalyzer.repository.OperacionRepository;
import com.businessanalyzer.repository.OperacionRepository.NuevaOperacion;
import com.businessanalyzer.repository.ProductoRepository;
import com.businessanalyzer.seguridad.Sesion;
import com.businessanalyzer.util.Montos;
import com.businessanalyzer.util.Validaciones;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Registro y consulta de ventas y compras de la empresa activa.
 * <p>
 * Reglas:
 * <ul>
 *     <li>Cada operación se guarda en una sola transacción: cabecera, líneas y, en compras,
 *         la actualización de costos del catálogo. O se guarda todo o nada.</li>
 *     <li>Las ventas guardan el costo del producto en ese momento, para que el margen
 *         histórico no cambie cuando cambie el costo.</li>
 *     <li>Si una venta supera el stock disponible se lanza {@link StockInsuficienteException};
 *         la interfaz puede confirmar con el usuario y repetir con {@code permitirStockNegativo}.</li>
 *     <li>Las operaciones no se eliminan: se anulan indicando el motivo.</li>
 * </ul>
 */
public class OperacionService {

    public static final BigDecimal TASA_IMPUESTO_INICIAL = new BigDecimal("18");

    private static final Logger log = LoggerFactory.getLogger(OperacionService.class);
    private static final String CLAVE_TASA = "impuesto.tasa";

    private final Database database;
    private final OperacionRepository operaciones;
    private final ProductoRepository productos;
    private final EmpresaService empresas;
    private final ConfiguracionService configuracion;
    private final Sesion sesion;
    private final Clock reloj;

    public OperacionService(Database database, OperacionRepository operaciones, ProductoRepository productos,
                            EmpresaService empresas, ConfiguracionService configuracion, Sesion sesion, Clock reloj) {
        this.database = database;
        this.operaciones = operaciones;
        this.productos = productos;
        this.empresas = empresas;
        this.configuracion = configuracion;
        this.sesion = sesion;
        this.reloj = reloj;
    }

    // ------------------------------------------------------------------ consultas

    public List<Operacion> listar(TipoOperacion tipo, LocalDate desde, LocalDate hasta, boolean incluirAnuladas) {
        sesion.requerir(Permiso.OPERACIONES_VER);
        Empresa empresa = empresas.empresaDeTrabajo();
        validarPeriodo(desde, hasta);
        return database.conConexion(c -> operaciones.listar(c, empresa.id(), tipo, desde, hasta, incluirAnuladas));
    }

    /** Operación completa, con sus líneas. */
    public Operacion obtener(long id) {
        sesion.requerir(Permiso.OPERACIONES_VER);
        Empresa empresa = empresas.empresaDeTrabajo();
        return database.conConexion(c -> exigirOperacion(c, id, empresa.id()));
    }

    /** Como {@link #resumen}, pero sumando las ventas y costos de la producción agropecuaria. */
    public ResumenOperaciones resumenConAgro(LocalDate desde, LocalDate hasta) {
        sesion.requerir(Permiso.OPERACIONES_VER);
        Empresa empresa = empresas.empresaDeTrabajo();
        validarPeriodo(desde, hasta);
        return database.conConexion(c -> operaciones.resumen(c, empresa.id(), desde, hasta));
    }

    public ResumenOperaciones resumen(LocalDate desde, LocalDate hasta) {
        sesion.requerir(Permiso.OPERACIONES_VER);
        Empresa empresa = empresas.empresaDeTrabajo();
        validarPeriodo(desde, hasta);
        return database.conConexion(c -> operaciones.resumenOperaciones(c, empresa.id(), desde, hasta));
    }

    /** Productos y servicios activos que se pueden usar en una operación nueva. */
    public List<Producto> productosDisponibles() {
        sesion.requerir(Permiso.OPERACIONES_VER);
        Empresa empresa = empresas.empresaDeTrabajo();
        return database.conConexion(c -> productos.listar(c, empresa.id(), false));
    }

    /** Última tasa de impuesto usada en la empresa (18 % la primera vez). */
    public BigDecimal tasaImpuestoSugerida() {
        Empresa empresa = empresas.empresaDeTrabajo();
        String guardada = configuracion.obtener(CLAVE_TASA + "." + empresa.id(), "");
        try {
            return guardada.isBlank() ? TASA_IMPUESTO_INICIAL : new BigDecimal(guardada);
        } catch (NumberFormatException e) {
            return TASA_IMPUESTO_INICIAL;
        }
    }

    // ------------------------------------------------------------------ registro y anulación

    public Operacion registrar(DatosOperacion datos, boolean permitirStockNegativo) {
        sesion.requerir(Permiso.OPERACIONES_GESTIONAR);
        Empresa empresa = empresas.empresaDeTrabajo();
        DatosOperacion d = ValidadorOperacion.normalizar(datos, LocalDate.now(reloj));
        String usuario = sesion.usuario().nombreUsuario();
        LocalDateTime ahora = LocalDateTime.now(reloj);

        Operacion registrada = database.enTransaccion(
                c -> registrarEn(c, empresa, d, permitirStockNegativo, usuario, ahora));

        recordarTasa(empresa, d.tasaImpuesto());
        log.info("{} {} registrada por {} ({} líneas)", d.tipo().descripcion(), registrada.numero(), usuario,
                registrada.cantidadItems());
        return registrada;
    }

    /**
     * Corrige una venta o compra: en una sola transacción anula la original (queda como historial, con
     * el número que la reemplaza) y registra los datos nuevos con un número nuevo. Como el stock se
     * calcula de las operaciones registradas, al corregir una venta sus cantidades vuelven a estar
     * disponibles antes de revisar el stock.
     */
    public Operacion corregir(long id, DatosOperacion datos, boolean permitirStockNegativo) {
        sesion.requerir(Permiso.OPERACIONES_GESTIONAR);
        Empresa empresa = empresas.empresaDeTrabajo();
        DatosOperacion d = ValidadorOperacion.normalizar(datos, LocalDate.now(reloj));
        String usuario = sesion.usuario().nombreUsuario();
        LocalDateTime ahora = LocalDateTime.now(reloj);

        Operacion nueva = database.enTransaccion(c -> {
            Operacion original = exigirOperacion(c, id, empresa.id());
            if (original.anulada()) {
                throw new ValidacionException("La operación " + original.numero() + " está anulada y no se puede corregir.");
            }
            if (original.tipo() != d.tipo()) {
                throw new ValidacionException("Una " + original.tipo().descripcion().toLowerCase()
                        + " no puede convertirse en " + d.tipo().descripcion().toLowerCase() + ".");
            }
            String numeroNuevo = operaciones.siguienteNumero(c, empresa.id(), d.tipo());
            operaciones.anular(c, id, "Corregida: reemplazada por " + numeroNuevo, usuario, ahora);
            String nota = "Corrige " + original.numero();
            String observaciones = d.observaciones() == null || d.observaciones().isBlank() ? nota
                    : d.observaciones().startsWith("Corrige ") ? d.observaciones() : nota + ". " + d.observaciones();
            DatosOperacion conNota = new DatosOperacion(d.tipo(), d.fecha(), d.contraparte(), d.documentoContraparte(),
                    d.comprobante(), d.tasaImpuesto(), observaciones.length() > 500 ? observaciones.substring(0, 500)
                    : observaciones, d.lineas(), d.actualizarCostos());
            return registrarEn(c, empresa, conNota, permitirStockNegativo, usuario, ahora);
        });

        recordarTasa(empresa, d.tasaImpuesto());
        log.info("Operación {} corregida por {}", nueva.numero(), usuario);
        return nueva;
    }

    private Operacion registrarEn(Connection c, Empresa empresa, DatosOperacion d, boolean permitirStockNegativo,
                                  String usuario, LocalDateTime ahora) throws SQLException {
        boolean esVenta = d.tipo() == TipoOperacion.VENTA;
        List<Producto> items = new ArrayList<>();
        List<CalculadoraOperacion.Linea> lineasCalculo = new ArrayList<>();
        List<String> faltantes = new ArrayList<>();

        for (DatosLinea linea : d.lineas()) {
            Producto producto = productos.buscarPorId(c, linea.productoId())
                    .filter(p -> p.empresaId() == empresa.id())
                    .orElseThrow(() -> new ValidacionException(
                            "Uno de los productos ya no existe en la empresa activa. Vuelve a elegirlo."));
            if (!producto.activo()) {
                throw new ValidacionException("\"" + producto.nombre() + "\" está archivado. Reactívalo para usarlo.");
            }
            BigDecimal costo = esVenta ? producto.costoUnitario() : linea.precioUnitario();
            if (esVenta && producto.tipo() == TipoItem.PRODUCTO
                    && producto.stock().compareTo(linea.cantidad()) < 0) {
                faltantes.add(producto.nombre() + " (disponible " + Montos.cantidad(producto.stock())
                        + ", se venden " + Montos.cantidad(linea.cantidad()) + ")");
            }
            items.add(producto);
            lineasCalculo.add(new CalculadoraOperacion.Linea(linea.cantidad(), linea.precioUnitario(), costo));
        }
        if (!faltantes.isEmpty() && !permitirStockNegativo) {
            throw new StockInsuficienteException(faltantes);
        }

        CalculadoraOperacion.Totales totales = CalculadoraOperacion.calcular(lineasCalculo, d.tasaImpuesto());
        String numero = operaciones.siguienteNumero(c, empresa.id(), d.tipo());
        long id = operaciones.insertar(c, empresa.id(), new NuevaOperacion(d.tipo(), numero, d.fecha(),
                d.contraparte(), d.documentoContraparte(), d.comprobante(), d.tasaImpuesto(), totales,
                d.observaciones(), usuario, ahora));

        for (int i = 0; i < items.size(); i++) {
            CalculadoraOperacion.Linea linea = lineasCalculo.get(i);
            operaciones.insertarLinea(c, id, items.get(i).id(), linea.cantidad(), linea.precioUnitario(),
                    linea.costoUnitario(), CalculadoraOperacion.subtotalLinea(linea.cantidad(), linea.precioUnitario()));
        }

        if (!esVenta && d.actualizarCostos()) {
            actualizarCostosDelCatalogo(c, items, d.lineas(), ahora, usuario);
        }
        return operaciones.buscarPorId(c, id).orElseThrow();
    }

    public Operacion anular(long id, String motivo) {
        sesion.requerir(Permiso.OPERACIONES_GESTIONAR);
        Empresa empresa = empresas.empresaDeTrabajo();
        String motivoLimpio = Validaciones.requerido(motivo, "Motivo de la anulación", 5, 200);
        String usuario = sesion.usuario().nombreUsuario();
        LocalDateTime ahora = LocalDateTime.now(reloj);

        return database.enTransaccion(c -> {
            Operacion operacion = exigirOperacion(c, id, empresa.id());
            if (operacion.anulada()) {
                throw new ValidacionException("La operación " + operacion.numero() + " ya está anulada.");
            }
            operaciones.anular(c, id, motivoLimpio, usuario, ahora);
            return operaciones.buscarPorId(c, id).orElseThrow();
        });
    }

    // ------------------------------------------------------------------ utilidades

    /** El precio pagado pasa a ser el costo del catálogo (y queda en el historial de precios). */
    private void actualizarCostosDelCatalogo(Connection c, List<Producto> items, List<DatosLinea> lineas,
                                             LocalDateTime ahora, String usuario) throws SQLException {
        for (int i = 0; i < items.size(); i++) {
            Producto producto = items.get(i);
            BigDecimal nuevoCosto = lineas.get(i).precioUnitario();
            if (producto.costoUnitario().compareTo(nuevoCosto) != 0) {
                productos.actualizarCosto(c, producto.id(), nuevoCosto, ahora);
                productos.registrarPrecio(c, producto.id(), nuevoCosto, producto.precioVenta(), ahora, usuario);
            }
        }
    }

    private Operacion exigirOperacion(Connection c, long id, long empresaId) throws SQLException {
        return operaciones.buscarPorId(c, id)
                .filter(o -> o.empresaId() == empresaId)
                .orElseThrow(() -> new ValidacionException("La operación no existe en la empresa activa."));
    }

    private static void validarPeriodo(LocalDate desde, LocalDate hasta) {
        if (desde == null || hasta == null) {
            throw new ValidacionException("Indica las fechas del período.");
        }
        if (desde.isAfter(hasta)) {
            throw new ValidacionException("La fecha inicial no puede ser posterior a la final.");
        }
    }

    private void recordarTasa(Empresa empresa, BigDecimal tasa) {
        try {
            configuracion.guardar(CLAVE_TASA + "." + empresa.id(), tasa.stripTrailingZeros().toPlainString());
        } catch (RuntimeException e) {
            log.warn("No se pudo recordar la tasa de impuesto", e);
        }
    }
}
