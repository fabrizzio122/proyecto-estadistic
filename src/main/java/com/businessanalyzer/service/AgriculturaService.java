package com.businessanalyzer.service;

import com.businessanalyzer.analytics.agricultura.ResultadoCampania;
import com.businessanalyzer.config.Database;
import com.businessanalyzer.exception.ValidacionException;
import com.businessanalyzer.model.Campania;
import com.businessanalyzer.model.Cosecha;
import com.businessanalyzer.model.CostoCampania;
import com.businessanalyzer.model.DatosCampania;
import com.businessanalyzer.model.DatosParcela;
import com.businessanalyzer.model.DestinoCosecha;
import com.businessanalyzer.model.Empresa;
import com.businessanalyzer.model.EstadoCampania;
import com.businessanalyzer.model.Parcela;
import com.businessanalyzer.model.Permiso;
import com.businessanalyzer.model.RubroCosto;
import com.businessanalyzer.model.Sector;
import com.businessanalyzer.repository.AgriculturaRepository;
import com.businessanalyzer.seguridad.Sesion;
import com.businessanalyzer.util.Validaciones;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Agricultura de la empresa activa (solo empresas del sector agropecuario): parcelas, campañas
 * por cultivo, sus costos y cosechas, y el resultado de cada campaña.
 * <p>
 * Una campaña está «en curso» hasta que se cierra como cosechada o perdida (helada, granizo,
 * sequía…). Los costos se pueden registrar hasta seis meses antes de la siembra (preparación del
 * terreno). Nada se elimina: los registros se anulan.
 */
public class AgriculturaService {

    /** Los costos de preparación del terreno pueden ser anteriores a la siembra. */
    static final int MESES_COSTOS_PREVIOS = 6;

    private final Database database;
    private final AgriculturaRepository repositorio;
    private final EmpresaService empresas;
    private final Sesion sesion;
    private final Clock reloj;

    public AgriculturaService(Database database, AgriculturaRepository repositorio, EmpresaService empresas,
                              Sesion sesion, Clock reloj) {
        this.database = database;
        this.repositorio = repositorio;
        this.empresas = empresas;
        this.sesion = sesion;
        this.reloj = reloj;
    }

    // ------------------------------------------------------------------ parcelas

    public List<Parcela> listarParcelas(boolean incluirInactivas) {
        sesion.requerir(Permiso.AGRO_VER);
        Empresa empresa = empresaAgropecuaria();
        return database.conConexion(c -> repositorio.listarParcelas(c, empresa.id(), incluirInactivas));
    }

    public Parcela registrarParcela(DatosParcela datos) {
        sesion.requerir(Permiso.AGRO_GESTIONAR);
        Empresa empresa = empresaAgropecuaria();
        DatosParcela d = ValidadorAgricultura.parcela(datos);
        String usuario = sesion.usuario().nombreUsuario();
        LocalDateTime ahora = LocalDateTime.now(reloj);
        return database.enTransaccion(c -> {
            exigirNombreLibre(c, empresa.id(), d.nombre(), 0);
            long id = repositorio.insertarParcela(c, empresa.id(), d, usuario, ahora);
            return repositorio.buscarParcela(c, id).orElseThrow();
        });
    }

    public Parcela actualizarParcela(long id, DatosParcela datos) {
        sesion.requerir(Permiso.AGRO_GESTIONAR);
        Empresa empresa = empresaAgropecuaria();
        DatosParcela d = ValidadorAgricultura.parcela(datos);
        String usuario = sesion.usuario().nombreUsuario();
        LocalDateTime ahora = LocalDateTime.now(reloj);
        return database.enTransaccion(c -> {
            exigirParcela(c, id, empresa.id());
            exigirNombreLibre(c, empresa.id(), d.nombre(), id);
            BigDecimal mayorSiembra = repositorio.listarCampanias(c, empresa.id(), false).stream()
                    .filter(k -> k.parcelaId() == id).map(Campania::area).max(BigDecimal::compareTo).orElse(BigDecimal.ZERO);
            if (d.area().compareTo(mayorSiembra) < 0) {
                throw new ValidacionException("La parcela no puede ser menor que el área de sus campañas ("
                        + mayorSiembra.stripTrailingZeros().toPlainString() + " ha).");
            }
            repositorio.actualizarParcela(c, id, d, usuario, ahora);
            return repositorio.buscarParcela(c, id).orElseThrow();
        });
    }

    /** Desactiva (o reactiva) una parcela: deja de ofrecerse para nuevas campañas, pero conserva su historia. */
    public Parcela cambiarParcelaActiva(long id, boolean activa) {
        sesion.requerir(Permiso.AGRO_GESTIONAR);
        Empresa empresa = empresaAgropecuaria();
        return database.enTransaccion(c -> {
            exigirParcela(c, id, empresa.id());
            repositorio.cambiarParcelaActiva(c, id, activa);
            return repositorio.buscarParcela(c, id).orElseThrow();
        });
    }

    // ------------------------------------------------------------------ campañas

    /** Campañas con sus resultados, de la siembra más reciente a la más antigua. */
    public List<ResultadoCampania> listarCampanias(boolean incluirAnuladas) {
        sesion.requerir(Permiso.AGRO_VER);
        Empresa empresa = empresaAgropecuaria();
        return database.conConexion(c -> {
            List<CostoCampania> costos = repositorio.listarCostos(c, empresa.id(), null, false);
            List<Cosecha> cosechas = repositorio.listarCosechas(c, empresa.id(), null, false);
            return repositorio.listarCampanias(c, empresa.id(), incluirAnuladas).stream()
                    .map(k -> ResultadoCampania.de(k, costos, cosechas)).toList();
        });
    }

    public ResultadoCampania resultado(long campaniaId) {
        sesion.requerir(Permiso.AGRO_VER);
        Empresa empresa = empresaAgropecuaria();
        return database.conConexion(c -> resultado(c, exigirCampania(c, campaniaId, empresa.id()), empresa.id()));
    }

    public List<CostoCampania> listarCostos(long campaniaId) {
        sesion.requerir(Permiso.AGRO_VER);
        Empresa empresa = empresaAgropecuaria();
        return database.conConexion(c -> {
            exigirCampania(c, campaniaId, empresa.id());
            return repositorio.listarCostos(c, empresa.id(), campaniaId, true);
        });
    }

    public List<Cosecha> listarCosechas(long campaniaId) {
        sesion.requerir(Permiso.AGRO_VER);
        Empresa empresa = empresaAgropecuaria();
        return database.conConexion(c -> {
            exigirCampania(c, campaniaId, empresa.id());
            return repositorio.listarCosechas(c, empresa.id(), campaniaId, true);
        });
    }

    public Campania registrarCampania(DatosCampania datos) {
        sesion.requerir(Permiso.AGRO_GESTIONAR);
        Empresa empresa = empresaAgropecuaria();
        String usuario = sesion.usuario().nombreUsuario();
        LocalDateTime ahora = LocalDateTime.now(reloj);
        LocalDate hoy = LocalDate.now(reloj);
        return database.enTransaccion(c -> {
            Parcela parcela = parcelaDe(c, datos, empresa.id());
            if (!parcela.activa()) {
                throw new ValidacionException("La parcela \"" + parcela.nombre() + "\" está desactivada.");
            }
            DatosCampania d = ValidadorAgricultura.campania(datos, parcela, hoy);
            long id = repositorio.insertarCampania(c, empresa.id(), d, usuario, ahora);
            return repositorio.buscarCampania(c, id).orElseThrow();
        });
    }

    public Campania actualizarCampania(long id, DatosCampania datos) {
        sesion.requerir(Permiso.AGRO_GESTIONAR);
        Empresa empresa = empresaAgropecuaria();
        String usuario = sesion.usuario().nombreUsuario();
        LocalDateTime ahora = LocalDateTime.now(reloj);
        LocalDate hoy = LocalDate.now(reloj);
        return database.enTransaccion(c -> {
            Campania actual = exigirCampania(c, id, empresa.id());
            exigirNoAnulada(actual);
            DatosCampania d = ValidadorAgricultura.campania(datos, parcelaDe(c, datos, empresa.id()), hoy);
            boolean cosechaPrevia = repositorio.listarCosechas(c, empresa.id(), id, false).stream()
                    .anyMatch(h -> h.fecha().isBefore(d.fechaSiembra()));
            if (cosechaPrevia) {
                throw new ValidacionException("Hay cosechas registradas antes de esa fecha de siembra.");
            }
            repositorio.actualizarCampania(c, id, d, usuario, ahora);
            return repositorio.buscarCampania(c, id).orElseThrow();
        });
    }

    public ResultadoCampania registrarCosto(long campaniaId, LocalDate fecha, RubroCosto rubro, String descripcion,
                                            BigDecimal monto) {
        return guardarCosto(campaniaId, null, fecha, rubro, descripcion, monto);
    }

    /** Corrige un costo: el original queda anulado y se registra el corregido, en una sola operación. */
    public ResultadoCampania corregirCosto(long costoId, LocalDate fecha, RubroCosto rubro, String descripcion,
                                           BigDecimal monto) {
        return guardarCosto(null, costoId, fecha, rubro, descripcion, monto);
    }

    private ResultadoCampania guardarCosto(Long campaniaDestino, Long reemplaza, LocalDate fecha, RubroCosto rubro,
                                           String descripcion, BigDecimal monto) {
        sesion.requerir(Permiso.AGRO_GESTIONAR);
        Empresa empresa = empresaAgropecuaria();
        LocalDate hoy = LocalDate.now(reloj);
        LocalDate dia = ValidadorAgricultura.fecha(fecha, "La fecha del costo", hoy);
        if (rubro == null) {
            throw new ValidacionException("Selecciona el rubro del costo.");
        }
        String detalle = Validaciones.requerido(descripcion, "Descripción", 3, 100);
        BigDecimal importe = ValidadorAgricultura.monto(monto, "Monto");
        String usuario = sesion.usuario().nombreUsuario();
        LocalDateTime ahora = LocalDateTime.now(reloj);
        return database.enTransaccion(c -> {
            long campaniaId = reemplaza == null ? campaniaDestino : repositorio.anularRegistro(c, "costo_campania",
                    reemplaza, empresa.id(), usuario, ahora)
                    .orElseThrow(() -> new ValidacionException("El costo no existe o está anulado. Actualiza la lista."));
            Campania campania = exigirCampania(c, campaniaId, empresa.id());
            exigirNoAnulada(campania);
            if (dia.isBefore(campania.fechaSiembra().minusMonths(MESES_COSTOS_PREVIOS))) {
                throw new ValidacionException("El costo no puede ser de más de " + MESES_COSTOS_PREVIOS
                        + " meses antes de la siembra.");
            }
            repositorio.insertarCosto(c, campaniaId, dia, rubro, detalle, importe, usuario, ahora);
            return resultado(c, campania, empresa.id());
        });
    }

    public ResultadoCampania registrarCosecha(long campaniaId, LocalDate fecha, BigDecimal kilos, DestinoCosecha destino,
                                              BigDecimal precio, String observacion) {
        return guardarCosecha(campaniaId, null, fecha, kilos, destino, precio, observacion);
    }

    /**
     * Corrige una cosecha: la original queda anulada y se registra la corregida. Se permite aunque la
     * campaña ya esté cerrada como cosechada (corregir un dato no agrega una cosecha nueva).
     */
    public ResultadoCampania corregirCosecha(long cosechaId, LocalDate fecha, BigDecimal kilos, DestinoCosecha destino,
                                             BigDecimal precio, String observacion) {
        return guardarCosecha(null, cosechaId, fecha, kilos, destino, precio, observacion);
    }

    private ResultadoCampania guardarCosecha(Long campaniaDestino, Long reemplaza, LocalDate fecha, BigDecimal kilos,
                                             DestinoCosecha destino, BigDecimal precio, String observacion) {
        sesion.requerir(Permiso.AGRO_GESTIONAR);
        Empresa empresa = empresaAgropecuaria();
        LocalDate dia = ValidadorAgricultura.fecha(fecha, "La fecha de cosecha", LocalDate.now(reloj));
        BigDecimal cantidad = ValidadorAgricultura.monto(kilos, "Cantidad cosechada (kg)");
        if (destino == null) {
            throw new ValidacionException("Indica el destino de lo cosechado.");
        }
        BigDecimal precioVenta = destino == DestinoCosecha.VENTA && precio != null
                ? ValidadorCatalogo.monto(precio, "Precio por kg") : null;
        String nota = Validaciones.opcional(observacion, "Observación", 150);
        String usuario = sesion.usuario().nombreUsuario();
        LocalDateTime ahora = LocalDateTime.now(reloj);
        return database.enTransaccion(c -> {
            long campaniaId = reemplaza == null ? campaniaDestino : repositorio.anularRegistro(c, "cosecha",
                    reemplaza, empresa.id(), usuario, ahora)
                    .orElseThrow(() -> new ValidacionException("La cosecha no existe o está anulada. Actualiza la lista."));
            Campania campania = exigirCampania(c, campaniaId, empresa.id());
            exigirNoAnulada(campania);
            if (reemplaza == null && campania.estado() == EstadoCampania.COSECHADA) {
                throw new ValidacionException("La campaña ya está cerrada como cosechada. Reábrela para agregar cosechas.");
            }
            if (dia.isBefore(campania.fechaSiembra())) {
                throw new ValidacionException("La cosecha no puede ser anterior a la siembra.");
            }
            repositorio.insertarCosecha(c, campaniaId, dia, cantidad, destino, precioVenta, nota, usuario, ahora);
            return resultado(c, campania, empresa.id());
        });
    }

    public void anularCosto(long costoId) {
        anularRegistro("costo_campania", costoId, "El costo no existe o ya está anulado.");
    }

    public void anularCosecha(long cosechaId) {
        anularRegistro("cosecha", cosechaId, "La cosecha no existe o ya está anulada.");
    }

    /**
     * Cierra la campaña.
     *
     * @param estado {@link EstadoCampania#COSECHADA} (requiere al menos una cosecha) o
     *               {@link EstadoCampania#PERDIDA} (requiere el motivo: helada, granizo, sequía…)
     */
    public Campania cerrarCampania(long id, EstadoCampania estado, LocalDate fecha, String motivo) {
        sesion.requerir(Permiso.AGRO_GESTIONAR);
        Empresa empresa = empresaAgropecuaria();
        if (estado == null || !estado.cerrada()) {
            throw new ValidacionException("Indica si la campaña se cosechó o se perdió.");
        }
        LocalDate dia = ValidadorAgricultura.fecha(fecha, "La fecha de cierre", LocalDate.now(reloj));
        String causa = estado == EstadoCampania.PERDIDA
                ? Validaciones.requerido(motivo, "Causa de la pérdida", 3, 150)
                : Validaciones.opcional(motivo, "Observación", 150);
        String usuario = sesion.usuario().nombreUsuario();
        LocalDateTime ahora = LocalDateTime.now(reloj);
        return database.enTransaccion(c -> {
            Campania campania = exigirCampania(c, id, empresa.id());
            if (campania.estado() != EstadoCampania.EN_CURSO) {
                throw new ValidacionException("Solo se puede cerrar una campaña en curso.");
            }
            if (dia.isBefore(campania.fechaSiembra())) {
                throw new ValidacionException("El cierre no puede ser anterior a la siembra.");
            }
            if (estado == EstadoCampania.COSECHADA && repositorio.listarCosechas(c, empresa.id(), id, false).isEmpty()) {
                throw new ValidacionException("Registra al menos una cosecha antes de cerrar la campaña como cosechada.");
            }
            repositorio.cambiarEstado(c, id, estado, dia, causa, usuario, ahora);
            return repositorio.buscarCampania(c, id).orElseThrow();
        });
    }

    /** Vuelve a poner en curso una campaña cerrada (para corregir o agregar cosechas). */
    public Campania reabrirCampania(long id) {
        sesion.requerir(Permiso.AGRO_GESTIONAR);
        Empresa empresa = empresaAgropecuaria();
        String usuario = sesion.usuario().nombreUsuario();
        LocalDateTime ahora = LocalDateTime.now(reloj);
        return database.enTransaccion(c -> {
            Campania campania = exigirCampania(c, id, empresa.id());
            if (!campania.estado().cerrada()) {
                throw new ValidacionException("La campaña no está cerrada.");
            }
            repositorio.cambiarEstado(c, id, EstadoCampania.EN_CURSO, null, null, usuario, ahora);
            return repositorio.buscarCampania(c, id).orElseThrow();
        });
    }

    /** Anula una campaña registrada por error: deja de contar en los resultados y en el balance. */
    public Campania anularCampania(long id, String motivo) {
        sesion.requerir(Permiso.AGRO_GESTIONAR);
        Empresa empresa = empresaAgropecuaria();
        String motivoLimpio = Validaciones.requerido(motivo, "Motivo de la anulación", 5, 150);
        String usuario = sesion.usuario().nombreUsuario();
        LocalDateTime ahora = LocalDateTime.now(reloj);
        return database.enTransaccion(c -> {
            Campania campania = exigirCampania(c, id, empresa.id());
            exigirNoAnulada(campania);
            repositorio.cambiarEstado(c, id, EstadoCampania.ANULADA, LocalDate.now(reloj), motivoLimpio, usuario, ahora);
            return repositorio.buscarCampania(c, id).orElseThrow();
        });
    }

    // ------------------------------------------------------------------ utilidades

    private void anularRegistro(String tabla, long id, String mensaje) {
        sesion.requerir(Permiso.AGRO_GESTIONAR);
        Empresa empresa = empresaAgropecuaria();
        String usuario = sesion.usuario().nombreUsuario();
        LocalDateTime ahora = LocalDateTime.now(reloj);
        database.enTransaccion(c -> repositorio.anularRegistro(c, tabla, id, empresa.id(), usuario, ahora)
                .orElseThrow(() -> new ValidacionException(mensaje)));
    }

    private ResultadoCampania resultado(Connection c, Campania campania, long empresaId) throws SQLException {
        Campania actual = repositorio.buscarCampania(c, campania.id()).orElseThrow();
        return ResultadoCampania.de(actual, repositorio.listarCostos(c, empresaId, campania.id(), false),
                repositorio.listarCosechas(c, empresaId, campania.id(), false));
    }

    private Parcela parcelaDe(Connection c, DatosCampania datos, long empresaId) throws SQLException {
        if (datos == null || datos.parcelaId() == null) {
            throw new ValidacionException("Selecciona la parcela.");
        }
        return exigirParcela(c, datos.parcelaId(), empresaId);
    }

    private Parcela exigirParcela(Connection c, long id, long empresaId) throws SQLException {
        return repositorio.buscarParcela(c, id).filter(p -> p.empresaId() == empresaId)
                .orElseThrow(() -> new ValidacionException("La parcela no existe en la empresa activa."));
    }

    private Campania exigirCampania(Connection c, long id, long empresaId) throws SQLException {
        return repositorio.buscarCampania(c, id).filter(k -> k.empresaId() == empresaId)
                .orElseThrow(() -> new ValidacionException("La campaña no existe en la empresa activa."));
    }

    private static void exigirNoAnulada(Campania campania) {
        if (campania.estado() == EstadoCampania.ANULADA) {
            throw new ValidacionException("La campaña está anulada.");
        }
    }

    private void exigirNombreLibre(Connection c, long empresaId, String nombre, long excluirId) throws SQLException {
        if (repositorio.existeParcela(c, empresaId, nombre, excluirId)) {
            throw new ValidacionException("Ya existe una parcela llamada \"" + nombre + "\".");
        }
    }

    private Empresa empresaAgropecuaria() {
        Empresa empresa = empresas.empresaDeTrabajo();
        if (empresa.sector() != Sector.AGROPECUARIO) {
            throw new ValidacionException("La agricultura solo está disponible para empresas del sector agropecuario. "
                    + "Cambia el sector en Empresas → Editar.");
        }
        return empresa;
    }
}
