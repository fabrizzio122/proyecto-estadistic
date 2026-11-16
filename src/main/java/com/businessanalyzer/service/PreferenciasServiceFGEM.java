package com.businessanalyzer.service;

import com.businessanalyzer.config.DatabaseFGEM;
import com.businessanalyzer.exception.ValidacionExceptionFGEM;
import com.businessanalyzer.model.EmpresaFGEM;
import com.businessanalyzer.model.UsuarioFGEM;
import com.businessanalyzer.repository.EmpresaRepositoryFGEM;
import com.businessanalyzer.repository.UsuarioRepositoryFGEM;
import com.businessanalyzer.seguridad.SesionFGEM;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

/**
 * Preferencias visuales y de acceso:
 * <ul>
 *   <li>Color de acento de cada usuario (en la base, se aplica al iniciar sesión).</li>
 *   <li>Último color usado en el equipo, para pintar el inicio de sesión antes de saber quién entra.</li>
 *   <li>Usuarios recordados en el equipo: solo ellos aparecen como tarjetas en el inicio de sesión,
 *       para no mostrar a cualquiera los nombres de todas las cuentas.</li>
 *   <li>Reducir animaciones.</li>
 * </ul>
 */
public class PreferenciasServiceFGEM {

    /** Colores válidos; deben coincidir con {@code ui.ColorAcento} y con la restricción de la migración V14. */
    public static final Set<String> COLORES_FGEM = Set.of("AMBAR", "CORAL", "LIMA", "CELESTE");
    /** Cuántos usuarios se recuerdan como máximo en un equipo. */
    public static final int MAXIMO_RECORDADOS_FGEM = 4;

    static final String CLAVE_ULTIMO_COLOR_FGEM = "ui.acento";
    static final String CLAVE_RECORDADOS_FGEM = "acceso.recordados";
    static final String CLAVE_ANIMACIONES_FGEM = "ui.animaciones.reducidas";
    private static final String CLAVE_EMPRESA_ACTIVA_FGEM = "empresa.activa.";

    /** Lo que muestra la tarjeta de un usuario recordado en el inicio de sesión. */
    public record PerfilRecordadoFGEM(String nombreUsuarioFGEM, String nombreCompletoFGEM, String empresaFGEM, String colorFGEM) {

        /** Iniciales para el avatar: «Fabrizzio Espinoza» → «FE». */
        public String inicialesFGEM() {
            String[] partesFGEM = nombreCompletoFGEM.strip().split("\\s+");
            String primeraFGEM = partesFGEM[0].substring(0, 1);
            String segundaFGEM = partesFGEM.length > 1 ? partesFGEM[partesFGEM.length - 1].substring(0, 1) : "";
            return (primeraFGEM + segundaFGEM).toUpperCase(Locale.ROOT);
        }
    }

    private final DatabaseFGEM databaseFGEM;
    private final UsuarioRepositoryFGEM usuariosFGEM;
    private final EmpresaRepositoryFGEM empresasFGEM;
    private final ConfiguracionServiceFGEM configuracionFGEM;
    private final SesionFGEM sesionFGEM;

    public PreferenciasServiceFGEM(DatabaseFGEM databaseFGEM, UsuarioRepositoryFGEM usuariosFGEM, EmpresaRepositoryFGEM empresasFGEM,
                               ConfiguracionServiceFGEM configuracionFGEM, SesionFGEM sesionFGEM) {
        this.databaseFGEM = databaseFGEM;
        this.usuariosFGEM = usuariosFGEM;
        this.empresasFGEM = empresasFGEM;
        this.configuracionFGEM = configuracionFGEM;
        this.sesionFGEM = sesionFGEM;
    }

    // ------------------------------------------------------------------ color de acento

    /** Último color usado en este equipo; vacío si nunca se eligió. */
    public Optional<String> ultimoColorFGEM() {
        String valorFGEM = configuracionFGEM.obtenerFGEM(CLAVE_ULTIMO_COLOR_FGEM, "");
        return COLORES_FGEM.contains(valorFGEM) ? Optional.of(valorFGEM) : Optional.empty();
    }

    public void guardarUltimoColorFGEM(String colorFGEM) {
        configuracionFGEM.guardarFGEM(CLAVE_ULTIMO_COLOR_FGEM, validarFGEM(colorFGEM));
    }

    /** Color del usuario conectado; vacío si usa el predeterminado. */
    public Optional<String> colorDelUsuarioFGEM() {
        UsuarioFGEM usuarioFGEM = sesionFGEM.usuarioFGEM();
        return databaseFGEM.conConexionFGEM(cFGEM -> usuariosFGEM.colorAcentoFGEM(cFGEM, usuarioFGEM.idFGEM())).filter(COLORES_FGEM::contains);
    }

    /** Guarda el color del usuario conectado y lo deja como último color del equipo. */
    public void guardarColorDelUsuarioFGEM(String colorFGEM) {
        String limpioFGEM = validarFGEM(colorFGEM);
        UsuarioFGEM usuarioFGEM = sesionFGEM.usuarioFGEM();
        databaseFGEM.enTransaccionFGEM(cFGEM -> {
            usuariosFGEM.guardarColorAcentoFGEM(cFGEM, usuarioFGEM.idFGEM(), limpioFGEM);
            return null;
        });
        guardarUltimoColorFGEM(limpioFGEM);
    }

    private static String validarFGEM(String colorFGEM) {
        String limpioFGEM = colorFGEM == null ? "" : colorFGEM.strip().toUpperCase(Locale.ROOT);
        if (!COLORES_FGEM.contains(limpioFGEM)) {
            throw new ValidacionExceptionFGEM("Color no válido. Elige ámbar, coral, lima o celeste.");
        }
        return limpioFGEM;
    }

    // ------------------------------------------------------------------ usuarios recordados

    /** Usuarios recordados en el equipo, del último que entró al primero (solo cuentas activas). */
    public List<PerfilRecordadoFGEM> recordadosFGEM() {
        List<String> nombresFGEM = nombresRecordadosFGEM();
        if (nombresFGEM.isEmpty()) {
            return List.of();
        }
        return databaseFGEM.conConexionFGEM(cFGEM -> {
            List<PerfilRecordadoFGEM> perfilesFGEM = new ArrayList<>();
            for (String nombreFGEM : nombresFGEM) {
                Optional<UsuarioRepositoryFGEM.CredencialesFGEM> credencialesFGEM = usuariosFGEM.buscarCredencialesFGEM(cFGEM, nombreFGEM);
                if (credencialesFGEM.isEmpty() || !credencialesFGEM.get().usuarioFGEM().activoFGEM()) {
                    continue;
                }
                UsuarioFGEM uFGEM = credencialesFGEM.get().usuarioFGEM();
                String empresaFGEM = null;
                String activaFGEM = configuracionFGEM.obtenerFGEM(CLAVE_EMPRESA_ACTIVA_FGEM + uFGEM.idFGEM(), "");
                if (activaFGEM.matches("\\d{1,18}")) {
                    empresaFGEM = empresasFGEM.buscarPorIdFGEM(cFGEM, Long.parseLong(activaFGEM)).map(EmpresaFGEM::nombreVisibleFGEM).orElse(null);
                }
                perfilesFGEM.add(new PerfilRecordadoFGEM(uFGEM.nombreUsuarioFGEM(), uFGEM.nombreCompletoFGEM(), empresaFGEM,
                        usuariosFGEM.colorAcentoFGEM(cFGEM, uFGEM.idFGEM()).filter(COLORES_FGEM::contains).orElse(null)));
            }
            return perfilesFGEM;
        });
    }

    /** Recuerda al usuario conectado en este equipo (queda primero en la lista). */
    public void recordarUsuarioActualFGEM() {
        String nombreFGEM = sesionFGEM.usuarioFGEM().nombreUsuarioFGEM();
        Set<String> nombresFGEM = new LinkedHashSet<>();
        nombresFGEM.add(nombreFGEM);
        nombresRecordadosFGEM().stream().filter(nFGEM -> !nFGEM.equalsIgnoreCase(nombreFGEM)).forEach(nombresFGEM::add);
        configuracionFGEM.guardarFGEM(CLAVE_RECORDADOS_FGEM, String.join(",", nombresFGEM.stream().limit(MAXIMO_RECORDADOS_FGEM).toList()));
    }

    /** Deja de recordar a un usuario en este equipo. */
    public void olvidarFGEM(String nombreUsuarioFGEM) {
        List<String> restantesFGEM = nombresRecordadosFGEM().stream()
                .filter(nFGEM -> !nFGEM.equalsIgnoreCase(nombreUsuarioFGEM == null ? "" : nombreUsuarioFGEM.strip()))
                .toList();
        configuracionFGEM.guardarFGEM(CLAVE_RECORDADOS_FGEM, String.join(",", restantesFGEM));
    }

    private List<String> nombresRecordadosFGEM() {
        String valorFGEM = configuracionFGEM.obtenerFGEM(CLAVE_RECORDADOS_FGEM, "");
        return Arrays.stream(valorFGEM.split(",")).map(String::strip).filter(sFGEM -> !sFGEM.isEmpty()).toList();
    }

    // ------------------------------------------------------------------ animaciones

    public boolean animacionesReducidasFGEM() {
        return Boolean.parseBoolean(configuracionFGEM.obtenerFGEM(CLAVE_ANIMACIONES_FGEM, "false"));
    }

    public void reducirAnimacionesFGEM(boolean reducirFGEM) {
        configuracionFGEM.guardarFGEM(CLAVE_ANIMACIONES_FGEM, Boolean.toString(reducirFGEM));
    }
}
