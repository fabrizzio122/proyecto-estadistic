package com.businessanalyzer.service;

import com.businessanalyzer.config.Database;
import com.businessanalyzer.exception.AccesoDenegadoException;
import com.businessanalyzer.exception.ValidacionException;
import com.businessanalyzer.model.DatosCuenta;
import com.businessanalyzer.model.DatosEmpresa;
import com.businessanalyzer.model.TipoCuenta;
import com.businessanalyzer.repository.TesoreriaRepository;
import com.businessanalyzer.model.Empresa;
import com.businessanalyzer.model.Permiso;
import com.businessanalyzer.model.Usuario;
import com.businessanalyzer.repository.EmpresaRepository;
import com.businessanalyzer.repository.UsuarioRepository;
import com.businessanalyzer.seguridad.Sesion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Registro de empresas, control de acceso por empresa y empresa activa de la sesión.
 * <p>
 * Reglas de acceso:
 * <ul>
 *     <li>Los administradores trabajan con todas las empresas.</li>
 *     <li>El resto de usuarios solo ve las empresas que tiene asignadas. Quien registra una
 *         empresa queda asignado a ella automáticamente.</li>
 *     <li>Las empresas no se eliminan: se archivan, para conservar su información histórica.</li>
 * </ul>
 */
public class EmpresaService {

    private static final Logger log = LoggerFactory.getLogger(EmpresaService.class);
    private static final String CLAVE_EMPRESA_ACTIVA = "empresa.activa";
    /** Nombre de la cuenta de efectivo que se crea con cada empresa. */
    public static final String CUENTA_INICIAL = "Caja";

    private final TesoreriaRepository tesoreria = new TesoreriaRepository();

    private final Database database;
    private final EmpresaRepository empresas;
    private final UsuarioRepository usuarios;
    private final ConfiguracionService configuracion;
    private final Sesion sesion;
    private final Clock reloj;

    public EmpresaService(Database database, EmpresaRepository empresas, UsuarioRepository usuarios,
                          ConfiguracionService configuracion, Sesion sesion, Clock reloj) {
        this.database = database;
        this.empresas = empresas;
        this.usuarios = usuarios;
        this.configuracion = configuracion;
        this.sesion = sesion;
        this.reloj = reloj;
    }

    /** Empresas que el usuario puede ver, opcionalmente incluyendo las archivadas. */
    public List<Empresa> listar(boolean incluirArchivadas) {
        sesion.requerir(Permiso.EMPRESAS_VER);
        Usuario usuario = sesion.usuario();
        return database.conConexion(c -> usuario.rol().esAdministrador()
                ? empresas.listar(c, incluirArchivadas)
                : empresas.listarAsignadas(c, usuario.id(), incluirArchivadas));
    }

    /** Empresas activas disponibles para elegir como empresa de trabajo. */
    public List<Empresa> listarParaSelector() {
        return listar(false);
    }

    public Empresa crear(DatosEmpresa datos) {
        sesion.requerir(Permiso.EMPRESAS_GESTIONAR);
        DatosEmpresa limpios = ValidadorEmpresa.normalizar(datos, hoy());
        Usuario usuario = sesion.usuario();
        LocalDateTime ahora = ahora();

        Empresa creada = database.enTransaccion(c -> {
            exigirIdentificacionDisponible(c, limpios.identificacionFiscal(), 0);
            long id = empresas.insertar(c, limpios, ahora);
            if (!usuario.rol().esAdministrador()) {
                empresas.asignar(c, usuario.id(), id);
            }
            // Toda empresa empieza con una caja para su efectivo; las demás cuentas se crean en «Tesorería».
            tesoreria.insertarCuenta(c, id, new DatosCuenta(CUENTA_INICIAL, TipoCuenta.CAJA, null, BigDecimal.ZERO),
                    usuario.nombreUsuario(), ahora);
            return empresas.buscarPorId(c, id).orElseThrow();
        });
        log.info("Empresa {} registrada por {}", creada.nombreVisible(), usuario.nombreUsuario());
        return creada;
    }

    public Empresa actualizar(long id, DatosEmpresa datos) {
        sesion.requerir(Permiso.EMPRESAS_GESTIONAR);
        DatosEmpresa limpios = ValidadorEmpresa.normalizar(datos, hoy());
        LocalDateTime ahora = ahora();

        Empresa actualizada = database.enTransaccion(c -> {
            exigirAcceso(c, id);
            exigirIdentificacionDisponible(c, limpios.identificacionFiscal(), id);
            empresas.actualizar(c, id, limpios, ahora);
            return empresas.buscarPorId(c, id).orElseThrow();
        });
        sesion.actualizarEmpresa(actualizada);
        return actualizada;
    }

    /** Archiva ({@code false}) o reactiva ({@code true}) una empresa. */
    public Empresa cambiarEstado(long id, boolean activa) {
        sesion.requerir(Permiso.EMPRESAS_GESTIONAR);
        LocalDateTime ahora = ahora();

        Empresa resultado = database.enTransaccion(c -> {
            exigirAcceso(c, id);
            empresas.cambiarActiva(c, id, activa, ahora);
            return empresas.buscarPorId(c, id).orElseThrow();
        });
        sesion.actualizarEmpresa(resultado);
        return resultado;
    }

    // ------------------------------------------------------------------ acceso de usuarios

    /** Usuarios (no administradores) asignados a la empresa. Solo para administradores. */
    public Set<Long> usuariosConAcceso(long empresaId) {
        sesion.requerir(Permiso.USUARIOS_GESTIONAR);
        return database.conConexion(c -> {
            exigirEmpresa(c, empresaId);
            return empresas.usuariosAsignados(c, empresaId);
        });
    }

    public void asignarAccesos(long empresaId, Set<Long> usuarioIds) {
        sesion.requerir(Permiso.USUARIOS_GESTIONAR);
        database.enTransaccion(c -> {
            exigirEmpresa(c, empresaId);
            for (long usuarioId : usuarioIds) {
                if (usuarios.buscarPorId(c, usuarioId).isEmpty()) {
                    throw new ValidacionException("Uno de los usuarios seleccionados ya no existe. Actualiza la lista.");
                }
            }
            empresas.reemplazarAsignaciones(c, empresaId, usuarioIds);
            return null;
        });
    }

    // ------------------------------------------------------------------ empresa activa

    public Empresa seleccionarEmpresaActiva(long id) {
        Usuario usuario = sesion.usuario();
        Empresa empresa = database.conConexion(c -> exigirAcceso(c, id));
        if (!empresa.activa()) {
            throw new ValidacionException("La empresa \"" + empresa.nombreVisible()
                    + "\" está archivada. Reactívala para trabajar con ella.");
        }
        sesion.seleccionarEmpresa(empresa);
        recordar(usuario, empresa.id());
        return empresa;
    }

    /**
     * Empresa sobre la que se registran los datos de negocio. Vuelve a comprobar en la base de
     * datos que el usuario sigue teniendo acceso y que la empresa no fue archivada.
     */
    public Empresa empresaDeTrabajo() {
        Empresa activa = sesion.empresaActivaRequerida();
        Empresa actual = database.conConexion(c -> exigirAcceso(c, activa.id()));
        if (!actual.activa()) {
            throw new ValidacionException("La empresa \"" + actual.nombreVisible()
                    + "\" fue archivada. Elige otra en la barra superior.");
        }
        return actual;
    }

    /**
     * Al iniciar sesión: vuelve a abrir la última empresa usada. Si ya no está disponible,
     * elige la primera empresa activa a la que el usuario tiene acceso.
     */
    public Optional<Empresa> restaurarEmpresaActiva() {
        Usuario usuario = sesion.usuario();
        if (!usuario.tiene(Permiso.EMPRESAS_VER)) {
            return Optional.empty();
        }
        List<Empresa> disponibles = listarParaSelector();
        String guardada = configuracion.obtener(claveEmpresaActiva(usuario), "");
        Optional<Empresa> elegida = disponibles.stream()
                .filter(e -> String.valueOf(e.id()).equals(guardada))
                .findFirst()
                .or(() -> disponibles.stream().findFirst());
        sesion.seleccionarEmpresa(elegida.orElse(null));
        return elegida;
    }

    // ------------------------------------------------------------------ utilidades

    private Empresa exigirEmpresa(Connection c, long id) throws SQLException {
        return empresas.buscarPorId(c, id)
                .orElseThrow(() -> new ValidacionException("La empresa ya no existe. Actualiza la lista."));
    }

    /** Devuelve la empresa si el usuario de la sesión puede trabajar con ella. */
    private Empresa exigirAcceso(Connection c, long id) throws SQLException {
        Empresa empresa = exigirEmpresa(c, id);
        Usuario usuario = sesion.usuario();
        if (!usuario.rol().esAdministrador() && !empresas.estaAsignada(c, usuario.id(), id)) {
            throw new AccesoDenegadoException("No tienes acceso a la empresa \"" + empresa.nombreVisible() + "\".");
        }
        return empresa;
    }

    private void exigirIdentificacionDisponible(Connection c, String identificacion, long excluirId)
            throws SQLException {
        if (identificacion != null && empresas.existeIdentificacion(c, identificacion, excluirId)) {
            throw new ValidacionException("Ya existe una empresa con el RUC o identificación \"" + identificacion + "\".");
        }
    }

    private void recordar(Usuario usuario, long empresaId) {
        try {
            configuracion.guardar(claveEmpresaActiva(usuario), String.valueOf(empresaId));
        } catch (RuntimeException e) {
            log.warn("No se pudo recordar la empresa activa de {}", usuario.nombreUsuario(), e);
        }
    }

    private static String claveEmpresaActiva(Usuario usuario) {
        return CLAVE_EMPRESA_ACTIVA + "." + usuario.id();
    }

    private LocalDateTime ahora() {
        return LocalDateTime.now(reloj);
    }

    private LocalDate hoy() {
        return LocalDate.now(reloj);
    }
}
