package com.businessanalyzer;

import com.businessanalyzer.config.Database;
import com.businessanalyzer.config.Migrador;
import com.businessanalyzer.model.DatosEmpresa;
import com.businessanalyzer.model.DatosUsuario;
import com.businessanalyzer.model.Empresa;
import com.businessanalyzer.model.Moneda;
import com.businessanalyzer.model.Sector;
import com.businessanalyzer.model.TamanoEmpresa;
import com.businessanalyzer.repository.CategoriaRepository;
import com.businessanalyzer.repository.ProductoRepository;
import com.businessanalyzer.service.CatalogoService;
import com.businessanalyzer.service.OperacionService;
import com.businessanalyzer.repository.OperacionRepository;
import com.businessanalyzer.repository.MovimientoRepository;
import com.businessanalyzer.service.FinanzasService;
import com.businessanalyzer.service.PatrimonioService;
import com.businessanalyzer.service.GanaderiaService;
import com.businessanalyzer.service.AgriculturaService;
import com.businessanalyzer.service.VisitaService;
import com.businessanalyzer.service.InformeTecnicoService;
import com.businessanalyzer.repository.VisitaRepository;
import com.businessanalyzer.repository.InformeRepository;
import com.businessanalyzer.repository.AgriculturaRepository;
import com.businessanalyzer.repository.GanaderiaRepository;
import com.businessanalyzer.repository.ActivoRepository;
import com.businessanalyzer.repository.DeudaRepository;
import com.businessanalyzer.model.Rol;
import com.businessanalyzer.model.Usuario;
import com.businessanalyzer.repository.ConfiguracionRepository;
import com.businessanalyzer.repository.EmpresaRepository;
import com.businessanalyzer.repository.RegistroAccesoRepository;
import com.businessanalyzer.repository.RolRepository;
import com.businessanalyzer.repository.UsuarioRepository;
import com.businessanalyzer.seguridad.PasswordHasher;
import com.businessanalyzer.seguridad.Sesion;
import com.businessanalyzer.service.AutenticacionService;
import com.businessanalyzer.service.ConfiguracionService;
import com.businessanalyzer.service.EmpresaService;
import com.businessanalyzer.service.UsuarioService;

import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;

/**
 * Base de datos temporal ya migrada con todos los servicios de seguridad.
 * Usa un hash con pocas iteraciones (las pruebas serían muy lentas con 600.000)
 * y un reloj que se puede adelantar para probar los bloqueos.
 */
public final class EntornoPrueba {

    public static final String PASSWORD_ADMIN = "Admin2026";

    public final Database database;
    public final RelojAjustable reloj = new RelojAjustable(LocalDateTime.of(2026, 1, 15, 9, 0));
    public final Sesion sesion = new Sesion();
    public final RolRepository roles = new RolRepository();
    public final UsuarioRepository usuarios = new UsuarioRepository(roles);
    public final RegistroAccesoRepository registro = new RegistroAccesoRepository();
    public final PasswordHasher hasher;
    public final AutenticacionService autenticacion;
    public final UsuarioService usuarioService;
    public final ConfiguracionService configuracion;
    public final EmpresaService empresaService;
    public final CatalogoService catalogo;
    public final OperacionService operaciones;
    public final FinanzasService finanzas;
    public final PatrimonioService patrimonio;
    public final GanaderiaService ganaderia;
    public final AgriculturaService agricultura;
    public final VisitaService visitas;
    public final InformeTecnicoService informeTecnico;

    public EntornoPrueba(Path carpeta) {
        this(carpeta, new PasswordHasher(1_000));
    }

    public EntornoPrueba(Path carpeta, PasswordHasher hasher) {
        this.database = new Database(carpeta.resolve("prueba.db"));
        new Migrador(database).migrar();
        this.hasher = hasher;
        this.autenticacion = new AutenticacionService(database, usuarios, roles, registro, hasher, sesion, reloj);
        this.usuarioService = new UsuarioService(database, usuarios, roles, registro, hasher, sesion, reloj);
        this.configuracion = new ConfiguracionService(new ConfiguracionRepository(database));
        this.empresaService = new EmpresaService(database, new EmpresaRepository(), usuarios, configuracion,
                sesion, reloj);
        this.catalogo = new CatalogoService(database, new ProductoRepository(), new CategoriaRepository(),
                empresaService, sesion, reloj);
        this.operaciones = new OperacionService(database, new OperacionRepository(), new ProductoRepository(),
                empresaService, configuracion, sesion, reloj);
        this.finanzas = new FinanzasService(database, new MovimientoRepository(), empresaService, sesion, reloj);
        this.ganaderia = new GanaderiaService(database, new GanaderiaRepository(), empresaService, sesion, reloj);
        this.agricultura = new AgriculturaService(database, new AgriculturaRepository(), empresaService, sesion, reloj);
        this.visitas = new VisitaService(database, new VisitaRepository(), empresaService, sesion, reloj, carpeta);
        this.patrimonio = new PatrimonioService(database, new ActivoRepository(), new DeudaRepository(),
                new ProductoRepository(), new OperacionRepository(), new MovimientoRepository(), empresaService,
                sesion, reloj);
        this.informeTecnico = new InformeTecnicoService(database, new InformeRepository(), visitas, ganaderia,
                agricultura, patrimonio, empresaService, sesion, reloj);
    }

    /** Crea una empresa con datos mínimos (requiere sesión con permiso) y la deja activa. */
    public Empresa crearEmpresaActiva(String razonSocial) {
        Empresa empresa = empresaService.crear(new DatosEmpresa(razonSocial, null, null, Sector.COMERCIO,
                TamanoEmpresa.MICRO, Moneda.PEN, 1, null, null, null, null));
        return empresaService.seleccionarEmpresaActiva(empresa.id());
    }

    /** Crea un usuario con el rol indicado (requiere sesión de administrador). Contraseña: Temporal123. */
    public Usuario crearUsuario(String nombreUsuario, String codigoRol) {
        return usuarioService.crear(new DatosUsuario(nombreUsuario, "Usuario " + nombreUsuario, null,
                idRol(codigoRol)), "Temporal123", "Temporal123");
    }

    /** Cierra la sesión actual y entra con otro usuario. */
    public Usuario entrarComo(String nombreUsuario, String password) {
        autenticacion.cerrarSesion();
        return autenticacion.iniciarSesion(nombreUsuario, password);
    }

    /** Crea el administrador inicial "admin" e inicia sesión con él. */
    public Usuario administradorConectado() {
        autenticacion.crearAdministradorInicial(
                new DatosUsuario("admin", "Administrador General", "admin@empresa.com", 0),
                PASSWORD_ADMIN, PASSWORD_ADMIN);
        return autenticacion.iniciarSesion("admin", PASSWORD_ADMIN);
    }

    public long idRol(String codigo) {
        return database.conConexion(c -> roles.buscarPorCodigo(c, codigo)).map(Rol::id).orElseThrow();
    }

    /** Reloj fijo que solo avanza cuando la prueba lo pide. */
    public static final class RelojAjustable extends Clock {
        private Instant instante;

        public RelojAjustable(LocalDateTime inicio) {
            this.instante = inicio.toInstant(ZoneOffset.UTC);
        }

        public void avanzar(Duration duracion) {
            instante = instante.plus(duracion);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zona) {
            return this;
        }

        @Override
        public Instant instant() {
            return instante;
        }
    }
}
