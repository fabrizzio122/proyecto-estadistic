package com.businessanalyzer.service;

import com.businessanalyzer.config.Database;
import com.businessanalyzer.exception.ValidacionException;
import com.businessanalyzer.model.Empresa;
import com.businessanalyzer.model.Usuario;
import com.businessanalyzer.repository.EmpresaRepository;
import com.businessanalyzer.repository.UsuarioRepository;
import com.businessanalyzer.seguridad.Sesion;

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
public class PreferenciasService {

    /** Colores válidos; deben coincidir con {@code ui.ColorAcento} y con la restricción de la migración V14. */
    public static final Set<String> COLORES = Set.of("AMBAR", "CORAL", "LIMA", "CELESTE");
    /** Cuántos usuarios se recuerdan como máximo en un equipo. */
    public static final int MAXIMO_RECORDADOS = 4;

    static final String CLAVE_ULTIMO_COLOR = "ui.acento";
    static final String CLAVE_RECORDADOS = "acceso.recordados";
    static final String CLAVE_ANIMACIONES = "ui.animaciones.reducidas";
    private static final String CLAVE_EMPRESA_ACTIVA = "empresa.activa.";

    /** Lo que muestra la tarjeta de un usuario recordado en el inicio de sesión. */
    public record PerfilRecordado(String nombreUsuario, String nombreCompleto, String empresa, String color) {

        /** Iniciales para el avatar: «Fabrizzio Espinoza» → «FE». */
        public String iniciales() {
            String[] partes = nombreCompleto.strip().split("\\s+");
            String primera = partes[0].substring(0, 1);
            String segunda = partes.length > 1 ? partes[partes.length - 1].substring(0, 1) : "";
            return (primera + segunda).toUpperCase(Locale.ROOT);
        }
    }

    private final Database database;
    private final UsuarioRepository usuarios;
    private final EmpresaRepository empresas;
    private final ConfiguracionService configuracion;
    private final Sesion sesion;

    public PreferenciasService(Database database, UsuarioRepository usuarios, EmpresaRepository empresas,
                               ConfiguracionService configuracion, Sesion sesion) {
        this.database = database;
        this.usuarios = usuarios;
        this.empresas = empresas;
        this.configuracion = configuracion;
        this.sesion = sesion;
    }

    // ------------------------------------------------------------------ color de acento

    /** Último color usado en este equipo; vacío si nunca se eligió. */
    public Optional<String> ultimoColor() {
        String valor = configuracion.obtener(CLAVE_ULTIMO_COLOR, "");
        return COLORES.contains(valor) ? Optional.of(valor) : Optional.empty();
    }

    public void guardarUltimoColor(String color) {
        configuracion.guardar(CLAVE_ULTIMO_COLOR, validar(color));
    }

    /** Color del usuario conectado; vacío si usa el predeterminado. */
    public Optional<String> colorDelUsuario() {
        Usuario usuario = sesion.usuario();
        return database.conConexion(c -> usuarios.colorAcento(c, usuario.id())).filter(COLORES::contains);
    }

    /** Guarda el color del usuario conectado y lo deja como último color del equipo. */
    public void guardarColorDelUsuario(String color) {
        String limpio = validar(color);
        Usuario usuario = sesion.usuario();
        database.enTransaccion(c -> {
            usuarios.guardarColorAcento(c, usuario.id(), limpio);
            return null;
        });
        guardarUltimoColor(limpio);
    }

    private static String validar(String color) {
        String limpio = color == null ? "" : color.strip().toUpperCase(Locale.ROOT);
        if (!COLORES.contains(limpio)) {
            throw new ValidacionException("Color no válido. Elige ámbar, coral, lima o celeste.");
        }
        return limpio;
    }

    // ------------------------------------------------------------------ usuarios recordados

    /** Usuarios recordados en el equipo, del último que entró al primero (solo cuentas activas). */
    public List<PerfilRecordado> recordados() {
        List<String> nombres = nombresRecordados();
        if (nombres.isEmpty()) {
            return List.of();
        }
        return database.conConexion(c -> {
            List<PerfilRecordado> perfiles = new ArrayList<>();
            for (String nombre : nombres) {
                Optional<UsuarioRepository.Credenciales> credenciales = usuarios.buscarCredenciales(c, nombre);
                if (credenciales.isEmpty() || !credenciales.get().usuario().activo()) {
                    continue;
                }
                Usuario u = credenciales.get().usuario();
                String empresa = null;
                String activa = configuracion.obtener(CLAVE_EMPRESA_ACTIVA + u.id(), "");
                if (activa.matches("\\d{1,18}")) {
                    empresa = empresas.buscarPorId(c, Long.parseLong(activa)).map(Empresa::nombreVisible).orElse(null);
                }
                perfiles.add(new PerfilRecordado(u.nombreUsuario(), u.nombreCompleto(), empresa,
                        usuarios.colorAcento(c, u.id()).filter(COLORES::contains).orElse(null)));
            }
            return perfiles;
        });
    }

    /** Recuerda al usuario conectado en este equipo (queda primero en la lista). */
    public void recordarUsuarioActual() {
        String nombre = sesion.usuario().nombreUsuario();
        Set<String> nombres = new LinkedHashSet<>();
        nombres.add(nombre);
        nombresRecordados().stream().filter(n -> !n.equalsIgnoreCase(nombre)).forEach(nombres::add);
        configuracion.guardar(CLAVE_RECORDADOS, String.join(",", nombres.stream().limit(MAXIMO_RECORDADOS).toList()));
    }

    /** Deja de recordar a un usuario en este equipo. */
    public void olvidar(String nombreUsuario) {
        List<String> restantes = nombresRecordados().stream()
                .filter(n -> !n.equalsIgnoreCase(nombreUsuario == null ? "" : nombreUsuario.strip()))
                .toList();
        configuracion.guardar(CLAVE_RECORDADOS, String.join(",", restantes));
    }

    private List<String> nombresRecordados() {
        String valor = configuracion.obtener(CLAVE_RECORDADOS, "");
        return Arrays.stream(valor.split(",")).map(String::strip).filter(s -> !s.isEmpty()).toList();
    }

    // ------------------------------------------------------------------ animaciones

    public boolean animacionesReducidas() {
        return Boolean.parseBoolean(configuracion.obtener(CLAVE_ANIMACIONES, "false"));
    }

    public void reducirAnimaciones(boolean reducir) {
        configuracion.guardar(CLAVE_ANIMACIONES, Boolean.toString(reducir));
    }
}
