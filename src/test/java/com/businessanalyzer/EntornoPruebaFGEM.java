package com.businessanalyzer;

import com.businessanalyzer.config.DatabaseFGEM;
import com.businessanalyzer.config.MigradorFGEM;
import com.businessanalyzer.model.DatosEmpresaFGEM;
import com.businessanalyzer.model.DatosUsuarioFGEM;
import com.businessanalyzer.model.EmpresaFGEM;
import com.businessanalyzer.model.MonedaFGEM;
import com.businessanalyzer.model.SectorFGEM;
import com.businessanalyzer.model.TamanoEmpresaFGEM;
import com.businessanalyzer.repository.CategoriaRepositoryFGEM;
import com.businessanalyzer.repository.ProductoRepositoryFGEM;
import com.businessanalyzer.service.CatalogoServiceFGEM;
import com.businessanalyzer.service.OperacionServiceFGEM;
import com.businessanalyzer.repository.OperacionRepositoryFGEM;
import com.businessanalyzer.repository.MovimientoRepositoryFGEM;
import com.businessanalyzer.service.FinanzasServiceFGEM;
import com.businessanalyzer.service.PatrimonioServiceFGEM;
import com.businessanalyzer.service.GanaderiaServiceFGEM;
import com.businessanalyzer.service.AgriculturaServiceFGEM;
import com.businessanalyzer.service.ValoracionServiceFGEM;
import com.businessanalyzer.service.TesoreriaServiceFGEM;
import com.businessanalyzer.service.PreferenciasServiceFGEM;
import com.businessanalyzer.repository.TesoreriaRepositoryFGEM;
import com.businessanalyzer.service.VisitaServiceFGEM;
import com.businessanalyzer.service.InformeTecnicoServiceFGEM;
import com.businessanalyzer.repository.VisitaRepositoryFGEM;
import com.businessanalyzer.repository.InformeRepositoryFGEM;
import com.businessanalyzer.repository.AgriculturaRepositoryFGEM;
import com.businessanalyzer.repository.GanaderiaRepositoryFGEM;
import com.businessanalyzer.repository.ActivoRepositoryFGEM;
import com.businessanalyzer.repository.DeudaRepositoryFGEM;
import com.businessanalyzer.model.RolFGEM;
import com.businessanalyzer.model.UsuarioFGEM;
import com.businessanalyzer.repository.ConfiguracionRepositoryFGEM;
import com.businessanalyzer.repository.EmpresaRepositoryFGEM;
import com.businessanalyzer.repository.RegistroAccesoRepositoryFGEM;
import com.businessanalyzer.repository.RolRepositoryFGEM;
import com.businessanalyzer.repository.UsuarioRepositoryFGEM;
import com.businessanalyzer.seguridad.PasswordHasherFGEM;
import com.businessanalyzer.seguridad.SesionFGEM;
import com.businessanalyzer.service.AutenticacionServiceFGEM;
import com.businessanalyzer.service.ConfiguracionServiceFGEM;
import com.businessanalyzer.service.EmpresaServiceFGEM;
import com.businessanalyzer.service.UsuarioServiceFGEM;

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
public final class EntornoPruebaFGEM {

    public static final String PASSWORD_ADMIN_FGEM = "Admin2026";

    public final DatabaseFGEM databaseFGEM;
    public final RelojAjustableFGEM relojFGEM = new RelojAjustableFGEM(LocalDateTime.of(2026, 1, 15, 9, 0));
    public final SesionFGEM sesionFGEM = new SesionFGEM();
    public final RolRepositoryFGEM rolesFGEM = new RolRepositoryFGEM();
    public final UsuarioRepositoryFGEM usuariosFGEM = new UsuarioRepositoryFGEM(rolesFGEM);
    public final RegistroAccesoRepositoryFGEM registroFGEM = new RegistroAccesoRepositoryFGEM();
    public final PasswordHasherFGEM hasherFGEM;
    public final AutenticacionServiceFGEM autenticacionFGEM;
    public final UsuarioServiceFGEM usuarioServiceFGEM;
    public final ConfiguracionServiceFGEM configuracionFGEM;
    public final EmpresaServiceFGEM empresaServiceFGEM;
    public final CatalogoServiceFGEM catalogoFGEM;
    public final OperacionServiceFGEM operacionesFGEM;
    public final FinanzasServiceFGEM finanzasFGEM;
    public final PatrimonioServiceFGEM patrimonioFGEM;
    public final GanaderiaServiceFGEM ganaderiaFGEM;
    public final AgriculturaServiceFGEM agriculturaFGEM;
    public final ValoracionServiceFGEM valoracionFGEM;
    public final VisitaServiceFGEM visitasFGEM;
    public final InformeTecnicoServiceFGEM informeTecnicoFGEM;
    public final TesoreriaServiceFGEM tesoreriaFGEM;
    public final PreferenciasServiceFGEM preferenciasFGEM;

    public EntornoPruebaFGEM(Path carpetaFGEM) {
        this(carpetaFGEM, new PasswordHasherFGEM(1_000));
    }

    public EntornoPruebaFGEM(Path carpetaFGEM, PasswordHasherFGEM hasherFGEM) {
        this.databaseFGEM = new DatabaseFGEM(carpetaFGEM.resolve("prueba.db"));
        new MigradorFGEM(databaseFGEM).migrarFGEM();
        this.hasherFGEM = hasherFGEM;
        this.autenticacionFGEM = new AutenticacionServiceFGEM(databaseFGEM, usuariosFGEM, rolesFGEM, registroFGEM, hasherFGEM, sesionFGEM, relojFGEM);
        this.usuarioServiceFGEM = new UsuarioServiceFGEM(databaseFGEM, usuariosFGEM, rolesFGEM, registroFGEM, hasherFGEM, sesionFGEM, relojFGEM);
        this.configuracionFGEM = new ConfiguracionServiceFGEM(new ConfiguracionRepositoryFGEM(databaseFGEM));
        this.empresaServiceFGEM = new EmpresaServiceFGEM(databaseFGEM, new EmpresaRepositoryFGEM(), usuariosFGEM, configuracionFGEM,
                sesionFGEM, relojFGEM);
        this.catalogoFGEM = new CatalogoServiceFGEM(databaseFGEM, new ProductoRepositoryFGEM(), new CategoriaRepositoryFGEM(),
                empresaServiceFGEM, sesionFGEM, relojFGEM);
        this.operacionesFGEM = new OperacionServiceFGEM(databaseFGEM, new OperacionRepositoryFGEM(), new ProductoRepositoryFGEM(),
                empresaServiceFGEM, configuracionFGEM, sesionFGEM, relojFGEM);
        this.finanzasFGEM = new FinanzasServiceFGEM(databaseFGEM, new MovimientoRepositoryFGEM(), empresaServiceFGEM, sesionFGEM, relojFGEM);
        this.ganaderiaFGEM = new GanaderiaServiceFGEM(databaseFGEM, new GanaderiaRepositoryFGEM(), empresaServiceFGEM, sesionFGEM, relojFGEM);
        this.agriculturaFGEM = new AgriculturaServiceFGEM(databaseFGEM, new AgriculturaRepositoryFGEM(), empresaServiceFGEM, sesionFGEM, relojFGEM);
        this.visitasFGEM = new VisitaServiceFGEM(databaseFGEM, new VisitaRepositoryFGEM(), empresaServiceFGEM, sesionFGEM, relojFGEM, carpetaFGEM);
        this.patrimonioFGEM = new PatrimonioServiceFGEM(databaseFGEM, new ActivoRepositoryFGEM(), new DeudaRepositoryFGEM(),
                new ProductoRepositoryFGEM(), new OperacionRepositoryFGEM(), new MovimientoRepositoryFGEM(), empresaServiceFGEM,
                sesionFGEM, relojFGEM);
        this.valoracionFGEM = new ValoracionServiceFGEM(databaseFGEM, new OperacionRepositoryFGEM(), new MovimientoRepositoryFGEM(),
                new InformeRepositoryFGEM(), empresaServiceFGEM, configuracionFGEM, sesionFGEM, relojFGEM);
        this.preferenciasFGEM = new PreferenciasServiceFGEM(databaseFGEM, usuariosFGEM, new EmpresaRepositoryFGEM(), configuracionFGEM, sesionFGEM);
        this.tesoreriaFGEM = new TesoreriaServiceFGEM(databaseFGEM, new TesoreriaRepositoryFGEM(), new OperacionRepositoryFGEM(),
                new DeudaRepositoryFGEM(), empresaServiceFGEM, sesionFGEM, relojFGEM);
        this.informeTecnicoFGEM = new InformeTecnicoServiceFGEM(databaseFGEM, new InformeRepositoryFGEM(), visitasFGEM, ganaderiaFGEM,
                agriculturaFGEM, patrimonioFGEM, empresaServiceFGEM, sesionFGEM, relojFGEM);
    }

    /** Crea una empresa con datos mínimos (requiere sesión con permiso) y la deja activa. */
    public EmpresaFGEM crearEmpresaActivaFGEM(String razonSocialFGEM) {
        EmpresaFGEM empresaFGEM = empresaServiceFGEM.crearFGEM(new DatosEmpresaFGEM(razonSocialFGEM, null, null, SectorFGEM.COMERCIO,
                TamanoEmpresaFGEM.MICRO, MonedaFGEM.PEN, 1, null, null, null, null));
        return empresaServiceFGEM.seleccionarEmpresaActivaFGEM(empresaFGEM.idFGEM());
    }

    /** Crea un usuario con el rol indicado (requiere sesión de administrador). Contraseña: Temporal123. */
    public UsuarioFGEM crearUsuarioFGEM(String nombreUsuarioFGEM, String codigoRolFGEM) {
        return usuarioServiceFGEM.crearFGEM(new DatosUsuarioFGEM(nombreUsuarioFGEM, "Usuario " + nombreUsuarioFGEM, null,
                idRolFGEM(codigoRolFGEM)), "Temporal123", "Temporal123");
    }

    /** Cierra la sesión actual y entra con otro usuario. */
    public UsuarioFGEM entrarComoFGEM(String nombreUsuarioFGEM, String passwordFGEM) {
        autenticacionFGEM.cerrarSesionFGEM();
        return autenticacionFGEM.iniciarSesionFGEM(nombreUsuarioFGEM, passwordFGEM);
    }

    /** Crea el administrador inicial "admin" e inicia sesión con él. */
    public UsuarioFGEM administradorConectadoFGEM() {
        autenticacionFGEM.crearAdministradorInicialFGEM(
                new DatosUsuarioFGEM("admin", "Administrador General", "admin@empresa.com", 0),
                PASSWORD_ADMIN_FGEM, PASSWORD_ADMIN_FGEM);
        return autenticacionFGEM.iniciarSesionFGEM("admin", PASSWORD_ADMIN_FGEM);
    }

    public long idRolFGEM(String codigoFGEM) {
        return databaseFGEM.conConexionFGEM(cFGEM -> rolesFGEM.buscarPorCodigoFGEM(cFGEM, codigoFGEM)).map(RolFGEM::idFGEM).orElseThrow();
    }

    /** Reloj fijo que solo avanza cuando la prueba lo pide. */
    public static final class RelojAjustableFGEM extends Clock {
        private Instant instanteFGEM;

        public RelojAjustableFGEM(LocalDateTime inicioFGEM) {
            this.instanteFGEM = inicioFGEM.toInstant(ZoneOffset.UTC);
        }

        public void avanzarFGEM(Duration duracionFGEM) {
            instanteFGEM = instanteFGEM.plus(duracionFGEM);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zonaFGEM) {
            return this;
        }

        @Override
        public Instant instant() {
            return instanteFGEM;
        }
    }
}
