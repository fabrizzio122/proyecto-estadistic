package com.businessanalyzer.service;

import com.businessanalyzer.analytics.Periodo;
import com.businessanalyzer.analytics.ganaderia.ExistenciaValorizada;
import com.businessanalyzer.analytics.ganaderia.Hato;
import com.businessanalyzer.analytics.ganaderia.IndicadoresEspecie;
import com.businessanalyzer.config.Database;
import com.businessanalyzer.exception.ValidacionException;
import com.businessanalyzer.model.CategoriaGanado;
import com.businessanalyzer.model.DatosMovimientoGanado;
import com.businessanalyzer.model.DatosProduccion;
import com.businessanalyzer.model.Empresa;
import com.businessanalyzer.model.Especie;
import com.businessanalyzer.model.MovimientoGanado;
import com.businessanalyzer.model.Permiso;
import com.businessanalyzer.model.ProduccionGanadera;
import com.businessanalyzer.model.Sector;
import com.businessanalyzer.repository.GanaderiaRepository;
import com.businessanalyzer.seguridad.Sesion;
import com.businessanalyzer.util.Validaciones;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Ganadería de la empresa activa (solo empresas del sector agropecuario): existencias del hato,
 * movimientos, producción de leche, fibra y lana, precios por cabeza e indicadores técnicos.
 * <p>
 * Las existencias nunca pueden quedar negativas: un movimiento que deje sin animales a una
 * categoría en alguna fecha se rechaza. Nada se elimina; los registros se anulan.
 */
public class GanaderiaService {

    /** Fecha lejana para leer todos los movimientos, incluidos los posteriores a una fecha dada. */
    private static final LocalDate SIN_LIMITE = LocalDate.of(9999, 12, 31);

    private final Database database;
    private final GanaderiaRepository repositorio;
    private final EmpresaService empresas;
    private final Sesion sesion;
    private final Clock reloj;

    public GanaderiaService(Database database, GanaderiaRepository repositorio, EmpresaService empresas,
                            Sesion sesion, Clock reloj) {
        this.database = database;
        this.repositorio = repositorio;
        this.empresas = empresas;
        this.sesion = sesion;
        this.reloj = reloj;
    }

    // ------------------------------------------------------------------ consultas

    /** Existencias de hoy por categoría, con su precio y valor. */
    public List<ExistenciaValorizada> hato() {
        sesion.requerir(Permiso.AGRO_VER);
        Empresa empresa = empresaAgropecuaria();
        LocalDate hoy = LocalDate.now(reloj);
        return database.conConexion(c -> ExistenciaValorizada.de(
                Hato.existencias(repositorio.listarMovimientos(c, empresa.id(), hoy, false), hoy),
                repositorio.precios(c, empresa.id())));
    }

    /** Movimientos del período, del más reciente al más antiguo. */
    public List<MovimientoGanado> listarMovimientos(LocalDate desde, LocalDate hasta, boolean incluirAnulados) {
        sesion.requerir(Permiso.AGRO_VER);
        Empresa empresa = empresaAgropecuaria();
        Periodo periodo = new Periodo(desde, hasta);
        return database.conConexion(c -> repositorio.listarMovimientos(c, empresa.id(), periodo.hasta(), incluirAnulados))
                .stream()
                .filter(m -> !m.fecha().isBefore(periodo.desde()))
                .sorted(Comparator.comparing(MovimientoGanado::fecha).thenComparingLong(MovimientoGanado::id).reversed())
                .toList();
    }

    public List<ProduccionGanadera> listarProduccion(LocalDate desde, LocalDate hasta, boolean incluirAnulados) {
        sesion.requerir(Permiso.AGRO_VER);
        Empresa empresa = empresaAgropecuaria();
        Periodo periodo = new Periodo(desde, hasta);
        return database.conConexion(c -> repositorio.listarProduccion(c, empresa.id(), periodo.desde(),
                periodo.hasta(), incluirAnulados));
    }

    public Map<CategoriaGanado, BigDecimal> precios() {
        sesion.requerir(Permiso.AGRO_VER);
        Empresa empresa = empresaAgropecuaria();
        return database.conConexion(c -> repositorio.precios(c, empresa.id()));
    }

    /** Indicadores del período para cada especie con animales o movimientos. */
    public List<IndicadoresEspecie> indicadores(Periodo periodo) {
        sesion.requerir(Permiso.AGRO_VER);
        Empresa empresa = empresaAgropecuaria();
        return database.conConexion(c -> {
            List<MovimientoGanado> movimientos = repositorio.listarMovimientos(c, empresa.id(), periodo.hasta(), false);
            List<ProduccionGanadera> produccion = repositorio.listarProduccion(c, empresa.id(), periodo.desde(),
                    periodo.hasta(), false);
            List<IndicadoresEspecie> resultado = new ArrayList<>();
            for (Especie especie : Especie.values()) {
                IndicadoresEspecie i = IndicadoresEspecie.calcular(especie, periodo, movimientos, produccion);
                if (i.tieneDatos()) {
                    resultado.add(i);
                }
            }
            return resultado;
        });
    }

    // ------------------------------------------------------------------ registro

    public MovimientoGanado registrarMovimiento(DatosMovimientoGanado datos) {
        sesion.requerir(Permiso.AGRO_GESTIONAR);
        Empresa empresa = empresaAgropecuaria();
        DatosMovimientoGanado d = ValidadorGanaderia.movimiento(datos, LocalDate.now(reloj));
        String usuario = sesion.usuario().nombreUsuario();
        LocalDateTime ahora = LocalDateTime.now(reloj);
        return database.enTransaccion(c -> {
            long id = repositorio.insertarMovimiento(c, empresa.id(), d, usuario, ahora);
            exigirExistencias(c, empresa.id());
            return repositorio.buscarMovimiento(c, id).orElseThrow();
        });
    }

    /** Anula un movimiento registrado por error. Se rechaza si dejaría categorías con existencias negativas. */
    public MovimientoGanado anularMovimiento(long id, String motivo) {
        sesion.requerir(Permiso.AGRO_GESTIONAR);
        Empresa empresa = empresaAgropecuaria();
        String motivoLimpio = Validaciones.requerido(motivo, "Motivo de la anulación", 5, 200);
        String usuario = sesion.usuario().nombreUsuario();
        LocalDateTime ahora = LocalDateTime.now(reloj);
        return database.enTransaccion(c -> {
            MovimientoGanado actual = repositorio.buscarMovimiento(c, id)
                    .filter(m -> m.empresaId() == empresa.id())
                    .orElseThrow(() -> new ValidacionException("El movimiento no existe en la empresa activa."));
            if (actual.anulado()) {
                throw new ValidacionException("El movimiento ya está anulado.");
            }
            repositorio.anularMovimiento(c, id, motivoLimpio, usuario, ahora);
            exigirExistencias(c, empresa.id());
            return repositorio.buscarMovimiento(c, id).orElseThrow();
        });
    }

    public ProduccionGanadera registrarProduccion(DatosProduccion datos) {
        sesion.requerir(Permiso.AGRO_GESTIONAR);
        Empresa empresa = empresaAgropecuaria();
        DatosProduccion d = ValidadorGanaderia.produccion(datos, LocalDate.now(reloj));
        String usuario = sesion.usuario().nombreUsuario();
        LocalDateTime ahora = LocalDateTime.now(reloj);
        return database.enTransaccion(c -> {
            long id = repositorio.insertarProduccion(c, empresa.id(), d, usuario, ahora);
            return repositorio.buscarProduccion(c, id).orElseThrow();
        });
    }

    public ProduccionGanadera anularProduccion(long id) {
        sesion.requerir(Permiso.AGRO_GESTIONAR);
        Empresa empresa = empresaAgropecuaria();
        String usuario = sesion.usuario().nombreUsuario();
        LocalDateTime ahora = LocalDateTime.now(reloj);
        return database.enTransaccion(c -> {
            ProduccionGanadera actual = repositorio.buscarProduccion(c, id)
                    .filter(p -> p.empresaId() == empresa.id())
                    .orElseThrow(() -> new ValidacionException("El registro no existe en la empresa activa."));
            if (actual.anulado()) {
                throw new ValidacionException("El registro ya está anulado.");
            }
            repositorio.anularProduccion(c, id, usuario, ahora);
            return repositorio.buscarProduccion(c, id).orElseThrow();
        });
    }

    /** Guarda los precios por cabeza; un precio {@code null} borra el de esa categoría. */
    public void guardarPrecios(Map<CategoriaGanado, BigDecimal> precios) {
        sesion.requerir(Permiso.AGRO_GESTIONAR);
        Empresa empresa = empresaAgropecuaria();
        String usuario = sesion.usuario().nombreUsuario();
        LocalDateTime ahora = LocalDateTime.now(reloj);
        precios.values().forEach(ValidadorGanaderia::precio);
        database.enTransaccion(c -> {
            for (Map.Entry<CategoriaGanado, BigDecimal> e : precios.entrySet()) {
                repositorio.guardarPrecio(c, empresa.id(), e.getKey(), e.getValue(), usuario, ahora);
            }
            return null;
        });
    }

    // ------------------------------------------------------------------ utilidades

    /** La empresa activa, siempre que sea del sector agropecuario. */
    private Empresa empresaAgropecuaria() {
        Empresa empresa = empresas.empresaDeTrabajo();
        if (empresa.sector() != Sector.AGROPECUARIO) {
            throw new ValidacionException("La ganadería solo está disponible para empresas del sector agropecuario. "
                    + "Cambia el sector en Empresas → Editar.");
        }
        return empresa;
    }

    /** Rechaza la operación (y deshace la transacción) si alguna categoría queda con existencias negativas. */
    private void exigirExistencias(Connection c, long empresaId) throws SQLException {
        Hato.primerFaltante(repositorio.listarMovimientos(c, empresaId, SIN_LIMITE, false)).ifPresent(f -> {
            throw new ValidacionException("No alcanzan los animales: el " + com.businessanalyzer.util.Fechas.paraMostrar(f.fecha())
                    + " faltarían " + f.cantidad() + " en «" + f.categoria().nombreCompleto() + "». Registra primero "
                    + "el inventario inicial, los nacimientos o las compras de esa categoría.");
        });
    }
}
