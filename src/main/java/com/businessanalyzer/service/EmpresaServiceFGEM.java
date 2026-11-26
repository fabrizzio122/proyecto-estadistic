package com.businessanalyzer.service;

import com.businessanalyzer.config.DatabaseFGEM;
import com.businessanalyzer.exception.AccesoDenegadoExceptionFGEM;
import com.businessanalyzer.exception.ValidacionExceptionFGEM;
import com.businessanalyzer.model.DatosCuentaFGEM;
import com.businessanalyzer.model.DatosEmpresaFGEM;
import com.businessanalyzer.model.TipoCuentaFGEM;
import com.businessanalyzer.repository.TesoreriaRepositoryFGEM;
import com.businessanalyzer.model.EmpresaFGEM;
import com.businessanalyzer.model.PermisoFGEM;
import com.businessanalyzer.model.UsuarioFGEM;
import com.businessanalyzer.repository.EmpresaRepositoryFGEM;
import com.businessanalyzer.repository.UsuarioRepositoryFGEM;
import com.businessanalyzer.seguridad.SesionFGEM;
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

public class EmpresaServiceFGEM {

    private static final Logger logFGEM = LoggerFactory.getLogger(EmpresaServiceFGEM.class);
    private static final String CLAVE_EMPRESA_ACTIVA_FGEM = "empresa.activa";
    public static final String CUENTA_INICIAL_FGEM = "Caja";

    private final TesoreriaRepositoryFGEM tesoreriaFGEM = new TesoreriaRepositoryFGEM();

    private final DatabaseFGEM databaseFGEM;
    private final EmpresaRepositoryFGEM empresasFGEM;
    private final UsuarioRepositoryFGEM usuariosFGEM;
    private final ConfiguracionServiceFGEM configuracionFGEM;
    private final SesionFGEM sesionFGEM;
    private final Clock relojFGEM;

    public EmpresaServiceFGEM(DatabaseFGEM databaseFGEM, EmpresaRepositoryFGEM empresasFGEM, UsuarioRepositoryFGEM usuariosFGEM,
                          ConfiguracionServiceFGEM configuracionFGEM, SesionFGEM sesionFGEM, Clock relojFGEM) {
        this.databaseFGEM = databaseFGEM;
        this.empresasFGEM = empresasFGEM;
        this.usuariosFGEM = usuariosFGEM;
        this.configuracionFGEM = configuracionFGEM;
        this.sesionFGEM = sesionFGEM;
        this.relojFGEM = relojFGEM;
    }

    public List<EmpresaFGEM> listarFGEM(boolean incluirArchivadasFGEM) {
        sesionFGEM.requerirFGEM(PermisoFGEM.EMPRESAS_VER);
        UsuarioFGEM usuarioFGEM = sesionFGEM.usuarioFGEM();
        return databaseFGEM.conConexionFGEM(cFGEM -> usuarioFGEM.rolFGEM().esAdministradorFGEM()
                ? empresasFGEM.listarFGEM(cFGEM, incluirArchivadasFGEM)
                : empresasFGEM.listarAsignadasFGEM(cFGEM, usuarioFGEM.idFGEM(), incluirArchivadasFGEM));
    }

    public List<EmpresaFGEM> listarParaSelectorFGEM() {
        return listarFGEM(false);
    }

    public EmpresaFGEM crearFGEM(DatosEmpresaFGEM datosFGEM) {
        sesionFGEM.requerirFGEM(PermisoFGEM.EMPRESAS_GESTIONAR);
        DatosEmpresaFGEM limpiosFGEM = ValidadorEmpresaFGEM.normalizarFGEM(datosFGEM, hoyFGEM());
        UsuarioFGEM usuarioFGEM = sesionFGEM.usuarioFGEM();
        LocalDateTime ahoraFGEM = ahoraFGEM();

        EmpresaFGEM creadaFGEM = databaseFGEM.enTransaccionFGEM(cFGEM -> {
            exigirIdentificacionDisponibleFGEM(cFGEM, limpiosFGEM.identificacionFiscalFGEM(), 0);
            long idFGEM = empresasFGEM.insertarFGEM(cFGEM, limpiosFGEM, ahoraFGEM);
            if (!usuarioFGEM.rolFGEM().esAdministradorFGEM()) {
                empresasFGEM.asignarFGEM(cFGEM, usuarioFGEM.idFGEM(), idFGEM);
            }
            tesoreriaFGEM.insertarCuentaFGEM(cFGEM, idFGEM, new DatosCuentaFGEM(CUENTA_INICIAL_FGEM, TipoCuentaFGEM.CAJA, null, BigDecimal.ZERO),
                    usuarioFGEM.nombreUsuarioFGEM(), ahoraFGEM);
            return empresasFGEM.buscarPorIdFGEM(cFGEM, idFGEM).orElseThrow();
        });
        logFGEM.info("Empresa {} registrada por {}", creadaFGEM.nombreVisibleFGEM(), usuarioFGEM.nombreUsuarioFGEM());
        return creadaFGEM;
    }

    public EmpresaFGEM actualizarFGEM(long idFGEM, DatosEmpresaFGEM datosFGEM) {
        sesionFGEM.requerirFGEM(PermisoFGEM.EMPRESAS_GESTIONAR);
        DatosEmpresaFGEM limpiosFGEM = ValidadorEmpresaFGEM.normalizarFGEM(datosFGEM, hoyFGEM());
        LocalDateTime ahoraFGEM = ahoraFGEM();

        EmpresaFGEM actualizadaFGEM = databaseFGEM.enTransaccionFGEM(cFGEM -> {
            exigirAccesoFGEM(cFGEM, idFGEM);
            exigirIdentificacionDisponibleFGEM(cFGEM, limpiosFGEM.identificacionFiscalFGEM(), idFGEM);
            empresasFGEM.actualizarFGEM(cFGEM, idFGEM, limpiosFGEM, ahoraFGEM);
            return empresasFGEM.buscarPorIdFGEM(cFGEM, idFGEM).orElseThrow();
        });
        sesionFGEM.actualizarEmpresaFGEM(actualizadaFGEM);
        return actualizadaFGEM;
    }

    public EmpresaFGEM cambiarEstadoFGEM(long idFGEM, boolean activaFGEM) {
        sesionFGEM.requerirFGEM(PermisoFGEM.EMPRESAS_GESTIONAR);
        LocalDateTime ahoraFGEM = ahoraFGEM();

        EmpresaFGEM resultadoFGEM = databaseFGEM.enTransaccionFGEM(cFGEM -> {
            exigirAccesoFGEM(cFGEM, idFGEM);
            empresasFGEM.cambiarActivaFGEM(cFGEM, idFGEM, activaFGEM, ahoraFGEM);
            return empresasFGEM.buscarPorIdFGEM(cFGEM, idFGEM).orElseThrow();
        });
        sesionFGEM.actualizarEmpresaFGEM(resultadoFGEM);
        return resultadoFGEM;
    }

    public Set<Long> usuariosConAccesoFGEM(long empresaIdFGEM) {
        sesionFGEM.requerirFGEM(PermisoFGEM.USUARIOS_GESTIONAR);
        return databaseFGEM.conConexionFGEM(cFGEM -> {
            exigirEmpresaFGEM(cFGEM, empresaIdFGEM);
            return empresasFGEM.usuariosAsignadosFGEM(cFGEM, empresaIdFGEM);
        });
    }

    public void asignarAccesosFGEM(long empresaIdFGEM, Set<Long> usuarioIdsFGEM) {
        sesionFGEM.requerirFGEM(PermisoFGEM.USUARIOS_GESTIONAR);
        databaseFGEM.enTransaccionFGEM(cFGEM -> {
            exigirEmpresaFGEM(cFGEM, empresaIdFGEM);
            for (long usuarioIdFGEM : usuarioIdsFGEM) {
                if (usuariosFGEM.buscarPorIdFGEM(cFGEM, usuarioIdFGEM).isEmpty()) {
                    throw new ValidacionExceptionFGEM("Uno de los usuarios seleccionados ya no existe. Actualiza la lista.");
                }
            }
            empresasFGEM.reemplazarAsignacionesFGEM(cFGEM, empresaIdFGEM, usuarioIdsFGEM);
            return null;
        });
    }

    public EmpresaFGEM seleccionarEmpresaActivaFGEM(long idFGEM) {
        UsuarioFGEM usuarioFGEM = sesionFGEM.usuarioFGEM();
        EmpresaFGEM empresaFGEM = databaseFGEM.conConexionFGEM(cFGEM -> exigirAccesoFGEM(cFGEM, idFGEM));
        if (!empresaFGEM.activaFGEM()) {
            throw new ValidacionExceptionFGEM("La empresa \"" + empresaFGEM.nombreVisibleFGEM()
                    + "\" está archivada. Reactívala para trabajar con ella.");
        }
        sesionFGEM.seleccionarEmpresaFGEM(empresaFGEM);
        recordarFGEM(usuarioFGEM, empresaFGEM.idFGEM());
        return empresaFGEM;
    }

    public EmpresaFGEM empresaDeTrabajoFGEM() {
        EmpresaFGEM activaFGEM = sesionFGEM.empresaActivaRequeridaFGEM();
        EmpresaFGEM actualFGEM = databaseFGEM.conConexionFGEM(cFGEM -> exigirAccesoFGEM(cFGEM, activaFGEM.idFGEM()));
        if (!actualFGEM.activaFGEM()) {
            throw new ValidacionExceptionFGEM("La empresa \"" + actualFGEM.nombreVisibleFGEM()
                    + "\" fue archivada. Elige otra en la barra superior.");
        }
        return actualFGEM;
    }

    public Optional<EmpresaFGEM> restaurarEmpresaActivaFGEM() {
        UsuarioFGEM usuarioFGEM = sesionFGEM.usuarioFGEM();
        if (!usuarioFGEM.tieneFGEM(PermisoFGEM.EMPRESAS_VER)) {
            return Optional.empty();
        }
        List<EmpresaFGEM> disponiblesFGEM = listarParaSelectorFGEM();
        String guardadaFGEM = configuracionFGEM.obtenerFGEM(claveEmpresaActivaFGEM(usuarioFGEM), "");
        Optional<EmpresaFGEM> elegidaFGEM = disponiblesFGEM.stream()
                .filter(eFGEM -> String.valueOf(eFGEM.idFGEM()).equals(guardadaFGEM))
                .findFirst()
                .or(() -> disponiblesFGEM.stream().findFirst());
        sesionFGEM.seleccionarEmpresaFGEM(elegidaFGEM.orElse(null));
        return elegidaFGEM;
    }

    private EmpresaFGEM exigirEmpresaFGEM(Connection cFGEM, long idFGEM) throws SQLException {
        return empresasFGEM.buscarPorIdFGEM(cFGEM, idFGEM)
                .orElseThrow(() -> new ValidacionExceptionFGEM("La empresa ya no existe. Actualiza la lista."));
    }

    private EmpresaFGEM exigirAccesoFGEM(Connection cFGEM, long idFGEM) throws SQLException {
        EmpresaFGEM empresaFGEM = exigirEmpresaFGEM(cFGEM, idFGEM);
        UsuarioFGEM usuarioFGEM = sesionFGEM.usuarioFGEM();
        if (!usuarioFGEM.rolFGEM().esAdministradorFGEM() && !empresasFGEM.estaAsignadaFGEM(cFGEM, usuarioFGEM.idFGEM(), idFGEM)) {
            throw new AccesoDenegadoExceptionFGEM("No tienes acceso a la empresa \"" + empresaFGEM.nombreVisibleFGEM() + "\".");
        }
        return empresaFGEM;
    }

    private void exigirIdentificacionDisponibleFGEM(Connection cFGEM, String identificacionFGEM, long excluirIdFGEM)
            throws SQLException {
        if (identificacionFGEM != null && empresasFGEM.existeIdentificacionFGEM(cFGEM, identificacionFGEM, excluirIdFGEM)) {
            throw new ValidacionExceptionFGEM("Ya existe una empresa con el RUC o identificación \"" + identificacionFGEM + "\".");
        }
    }

    private void recordarFGEM(UsuarioFGEM usuarioFGEM, long empresaIdFGEM) {
        try {
            configuracionFGEM.guardarFGEM(claveEmpresaActivaFGEM(usuarioFGEM), String.valueOf(empresaIdFGEM));
        } catch (RuntimeException eFGEM) {
            logFGEM.warn("No se pudo recordar la empresa activa de {}", usuarioFGEM.nombreUsuarioFGEM(), eFGEM);
        }
    }

    private static String claveEmpresaActivaFGEM(UsuarioFGEM usuarioFGEM) {
        return CLAVE_EMPRESA_ACTIVA_FGEM + "." + usuarioFGEM.idFGEM();
    }

    private LocalDateTime ahoraFGEM() {
        return LocalDateTime.now(relojFGEM);
    }

    private LocalDate hoyFGEM() {
        return LocalDate.now(relojFGEM);
    }
}
