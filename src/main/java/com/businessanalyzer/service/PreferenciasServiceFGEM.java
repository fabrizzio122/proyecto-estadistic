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

public class PreferenciasServiceFGEM {

    public static final Set<String> COLORES_FGEM = Set.of("AMBAR", "CORAL", "LIMA", "CELESTE");
    public static final int MAXIMO_RECORDADOS_FGEM = 4;

    static final String CLAVE_ULTIMO_COLOR_FGEM = "ui.acento";
    static final String CLAVE_RECORDADOS_FGEM = "acceso.recordados";
    static final String CLAVE_ANIMACIONES_FGEM = "ui.animaciones.reducidas";
    private static final String CLAVE_EMPRESA_ACTIVA_FGEM = "empresa.activa.";

    public record PerfilRecordadoFGEM(String nombreUsuarioFGEM, String nombreCompletoFGEM, String empresaFGEM, String colorFGEM) {

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

    public Optional<String> ultimoColorFGEM() {
        String valorFGEM = configuracionFGEM.obtenerFGEM(CLAVE_ULTIMO_COLOR_FGEM, "");
        return COLORES_FGEM.contains(valorFGEM) ? Optional.of(valorFGEM) : Optional.empty();
    }

    public void guardarUltimoColorFGEM(String colorFGEM) {
        configuracionFGEM.guardarFGEM(CLAVE_ULTIMO_COLOR_FGEM, validarFGEM(colorFGEM));
    }

    public Optional<String> colorDelUsuarioFGEM() {
        UsuarioFGEM usuarioFGEM = sesionFGEM.usuarioFGEM();
        return databaseFGEM.conConexionFGEM(cFGEM -> usuariosFGEM.colorAcentoFGEM(cFGEM, usuarioFGEM.idFGEM())).filter(COLORES_FGEM::contains);
    }

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

    public void recordarUsuarioActualFGEM() {
        String nombreFGEM = sesionFGEM.usuarioFGEM().nombreUsuarioFGEM();
        Set<String> nombresFGEM = new LinkedHashSet<>();
        nombresFGEM.add(nombreFGEM);
        nombresRecordadosFGEM().stream().filter(nFGEM -> !nFGEM.equalsIgnoreCase(nombreFGEM)).forEach(nombresFGEM::add);
        configuracionFGEM.guardarFGEM(CLAVE_RECORDADOS_FGEM, String.join(",", nombresFGEM.stream().limit(MAXIMO_RECORDADOS_FGEM).toList()));
    }

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

    public boolean animacionesReducidasFGEM() {
        return Boolean.parseBoolean(configuracionFGEM.obtenerFGEM(CLAVE_ANIMACIONES_FGEM, "false"));
    }

    public void reducirAnimacionesFGEM(boolean reducirFGEM) {
        configuracionFGEM.guardarFGEM(CLAVE_ANIMACIONES_FGEM, Boolean.toString(reducirFGEM));
    }
}
