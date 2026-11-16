package com.businessanalyzer.service;

import com.businessanalyzer.analytics.agricultura.ResultadoCampaniaFGEM;
import com.businessanalyzer.config.DatabaseFGEM;
import com.businessanalyzer.exception.ValidacionExceptionFGEM;
import com.businessanalyzer.model.CampaniaFGEM;
import com.businessanalyzer.model.CosechaFGEM;
import com.businessanalyzer.model.CostoCampaniaFGEM;
import com.businessanalyzer.model.DatosCampaniaFGEM;
import com.businessanalyzer.model.DatosParcelaFGEM;
import com.businessanalyzer.model.DestinoCosechaFGEM;
import com.businessanalyzer.model.EmpresaFGEM;
import com.businessanalyzer.model.EstadoCampaniaFGEM;
import com.businessanalyzer.model.ParcelaFGEM;
import com.businessanalyzer.model.PermisoFGEM;
import com.businessanalyzer.model.RubroCostoFGEM;
import com.businessanalyzer.model.SectorFGEM;
import com.businessanalyzer.repository.AgriculturaRepositoryFGEM;
import com.businessanalyzer.seguridad.SesionFGEM;
import com.businessanalyzer.util.ValidacionesFGEM;

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
public class AgriculturaServiceFGEM {

    /** Los costos de preparación del terreno pueden ser anteriores a la siembra. */
    static final int MESES_COSTOS_PREVIOS_FGEM = 6;

    private final DatabaseFGEM databaseFGEM;
    private final AgriculturaRepositoryFGEM repositorioFGEM;
    private final EmpresaServiceFGEM empresasFGEM;
    private final SesionFGEM sesionFGEM;
    private final Clock relojFGEM;

    public AgriculturaServiceFGEM(DatabaseFGEM databaseFGEM, AgriculturaRepositoryFGEM repositorioFGEM, EmpresaServiceFGEM empresasFGEM,
                              SesionFGEM sesionFGEM, Clock relojFGEM) {
        this.databaseFGEM = databaseFGEM;
        this.repositorioFGEM = repositorioFGEM;
        this.empresasFGEM = empresasFGEM;
        this.sesionFGEM = sesionFGEM;
        this.relojFGEM = relojFGEM;
    }

    // ------------------------------------------------------------------ parcelas

    public List<ParcelaFGEM> listarParcelasFGEM(boolean incluirInactivasFGEM) {
        sesionFGEM.requerirFGEM(PermisoFGEM.AGRO_VER);
        EmpresaFGEM empresaFGEM = empresaAgropecuariaFGEM();
        return databaseFGEM.conConexionFGEM(cFGEM -> repositorioFGEM.listarParcelasFGEM(cFGEM, empresaFGEM.idFGEM(), incluirInactivasFGEM));
    }

    public ParcelaFGEM registrarParcelaFGEM(DatosParcelaFGEM datosFGEM) {
        sesionFGEM.requerirFGEM(PermisoFGEM.AGRO_GESTIONAR);
        EmpresaFGEM empresaFGEM = empresaAgropecuariaFGEM();
        DatosParcelaFGEM dFGEM = ValidadorAgriculturaFGEM.parcelaFGEM(datosFGEM);
        String usuarioFGEM = sesionFGEM.usuarioFGEM().nombreUsuarioFGEM();
        LocalDateTime ahoraFGEM = LocalDateTime.now(relojFGEM);
        return databaseFGEM.enTransaccionFGEM(cFGEM -> {
            exigirNombreLibreFGEM(cFGEM, empresaFGEM.idFGEM(), dFGEM.nombreFGEM(), 0);
            long idFGEM = repositorioFGEM.insertarParcelaFGEM(cFGEM, empresaFGEM.idFGEM(), dFGEM, usuarioFGEM, ahoraFGEM);
            return repositorioFGEM.buscarParcelaFGEM(cFGEM, idFGEM).orElseThrow();
        });
    }

    public ParcelaFGEM actualizarParcelaFGEM(long idFGEM, DatosParcelaFGEM datosFGEM) {
        sesionFGEM.requerirFGEM(PermisoFGEM.AGRO_GESTIONAR);
        EmpresaFGEM empresaFGEM = empresaAgropecuariaFGEM();
        DatosParcelaFGEM dFGEM = ValidadorAgriculturaFGEM.parcelaFGEM(datosFGEM);
        String usuarioFGEM = sesionFGEM.usuarioFGEM().nombreUsuarioFGEM();
        LocalDateTime ahoraFGEM = LocalDateTime.now(relojFGEM);
        return databaseFGEM.enTransaccionFGEM(cFGEM -> {
            exigirParcelaFGEM(cFGEM, idFGEM, empresaFGEM.idFGEM());
            exigirNombreLibreFGEM(cFGEM, empresaFGEM.idFGEM(), dFGEM.nombreFGEM(), idFGEM);
            BigDecimal mayorSiembraFGEM = repositorioFGEM.listarCampaniasFGEM(cFGEM, empresaFGEM.idFGEM(), false).stream()
                    .filter(kFGEM -> kFGEM.parcelaIdFGEM() == idFGEM).map(CampaniaFGEM::areaFGEM).max(BigDecimal::compareTo).orElse(BigDecimal.ZERO);
            if (dFGEM.areaFGEM().compareTo(mayorSiembraFGEM) < 0) {
                throw new ValidacionExceptionFGEM("La parcela no puede ser menor que el área de sus campañas ("
                        + mayorSiembraFGEM.stripTrailingZeros().toPlainString() + " ha).");
            }
            repositorioFGEM.actualizarParcelaFGEM(cFGEM, idFGEM, dFGEM, usuarioFGEM, ahoraFGEM);
            return repositorioFGEM.buscarParcelaFGEM(cFGEM, idFGEM).orElseThrow();
        });
    }

    /** Desactiva (o reactiva) una parcela: deja de ofrecerse para nuevas campañas, pero conserva su historia. */
    public ParcelaFGEM cambiarParcelaActivaFGEM(long idFGEM, boolean activaFGEM) {
        sesionFGEM.requerirFGEM(PermisoFGEM.AGRO_GESTIONAR);
        EmpresaFGEM empresaFGEM = empresaAgropecuariaFGEM();
        return databaseFGEM.enTransaccionFGEM(cFGEM -> {
            exigirParcelaFGEM(cFGEM, idFGEM, empresaFGEM.idFGEM());
            repositorioFGEM.cambiarParcelaActivaFGEM(cFGEM, idFGEM, activaFGEM);
            return repositorioFGEM.buscarParcelaFGEM(cFGEM, idFGEM).orElseThrow();
        });
    }

    // ------------------------------------------------------------------ campañas

    /** Campañas con sus resultados, de la siembra más reciente a la más antigua. */
    public List<ResultadoCampaniaFGEM> listarCampaniasFGEM(boolean incluirAnuladasFGEM) {
        sesionFGEM.requerirFGEM(PermisoFGEM.AGRO_VER);
        EmpresaFGEM empresaFGEM = empresaAgropecuariaFGEM();
        return databaseFGEM.conConexionFGEM(cFGEM -> {
            List<CostoCampaniaFGEM> costosFGEM = repositorioFGEM.listarCostosFGEM(cFGEM, empresaFGEM.idFGEM(), null, false);
            List<CosechaFGEM> cosechasFGEM = repositorioFGEM.listarCosechasFGEM(cFGEM, empresaFGEM.idFGEM(), null, false);
            return repositorioFGEM.listarCampaniasFGEM(cFGEM, empresaFGEM.idFGEM(), incluirAnuladasFGEM).stream()
                    .map(kFGEM -> ResultadoCampaniaFGEM.deFGEM(kFGEM, costosFGEM, cosechasFGEM)).toList();
        });
    }

    public ResultadoCampaniaFGEM resultadoFGEM(long campaniaIdFGEM) {
        sesionFGEM.requerirFGEM(PermisoFGEM.AGRO_VER);
        EmpresaFGEM empresaFGEM = empresaAgropecuariaFGEM();
        return databaseFGEM.conConexionFGEM(cFGEM -> resultadoFGEM(cFGEM, exigirCampaniaFGEM(cFGEM, campaniaIdFGEM, empresaFGEM.idFGEM()), empresaFGEM.idFGEM()));
    }

    public List<CostoCampaniaFGEM> listarCostosFGEM(long campaniaIdFGEM) {
        sesionFGEM.requerirFGEM(PermisoFGEM.AGRO_VER);
        EmpresaFGEM empresaFGEM = empresaAgropecuariaFGEM();
        return databaseFGEM.conConexionFGEM(cFGEM -> {
            exigirCampaniaFGEM(cFGEM, campaniaIdFGEM, empresaFGEM.idFGEM());
            return repositorioFGEM.listarCostosFGEM(cFGEM, empresaFGEM.idFGEM(), campaniaIdFGEM, true);
        });
    }

    public List<CosechaFGEM> listarCosechasFGEM(long campaniaIdFGEM) {
        sesionFGEM.requerirFGEM(PermisoFGEM.AGRO_VER);
        EmpresaFGEM empresaFGEM = empresaAgropecuariaFGEM();
        return databaseFGEM.conConexionFGEM(cFGEM -> {
            exigirCampaniaFGEM(cFGEM, campaniaIdFGEM, empresaFGEM.idFGEM());
            return repositorioFGEM.listarCosechasFGEM(cFGEM, empresaFGEM.idFGEM(), campaniaIdFGEM, true);
        });
    }

    public CampaniaFGEM registrarCampaniaFGEM(DatosCampaniaFGEM datosFGEM) {
        sesionFGEM.requerirFGEM(PermisoFGEM.AGRO_GESTIONAR);
        EmpresaFGEM empresaFGEM = empresaAgropecuariaFGEM();
        String usuarioFGEM = sesionFGEM.usuarioFGEM().nombreUsuarioFGEM();
        LocalDateTime ahoraFGEM = LocalDateTime.now(relojFGEM);
        LocalDate hoyFGEM = LocalDate.now(relojFGEM);
        return databaseFGEM.enTransaccionFGEM(cFGEM -> {
            ParcelaFGEM parcelaFGEM = parcelaDeFGEM(cFGEM, datosFGEM, empresaFGEM.idFGEM());
            if (!parcelaFGEM.activaFGEM()) {
                throw new ValidacionExceptionFGEM("La parcela \"" + parcelaFGEM.nombreFGEM() + "\" está desactivada.");
            }
            DatosCampaniaFGEM dFGEM = ValidadorAgriculturaFGEM.campaniaFGEM(datosFGEM, parcelaFGEM, hoyFGEM);
            long idFGEM = repositorioFGEM.insertarCampaniaFGEM(cFGEM, empresaFGEM.idFGEM(), dFGEM, usuarioFGEM, ahoraFGEM);
            return repositorioFGEM.buscarCampaniaFGEM(cFGEM, idFGEM).orElseThrow();
        });
    }

    public CampaniaFGEM actualizarCampaniaFGEM(long idFGEM, DatosCampaniaFGEM datosFGEM) {
        sesionFGEM.requerirFGEM(PermisoFGEM.AGRO_GESTIONAR);
        EmpresaFGEM empresaFGEM = empresaAgropecuariaFGEM();
        String usuarioFGEM = sesionFGEM.usuarioFGEM().nombreUsuarioFGEM();
        LocalDateTime ahoraFGEM = LocalDateTime.now(relojFGEM);
        LocalDate hoyFGEM = LocalDate.now(relojFGEM);
        return databaseFGEM.enTransaccionFGEM(cFGEM -> {
            CampaniaFGEM actualFGEM = exigirCampaniaFGEM(cFGEM, idFGEM, empresaFGEM.idFGEM());
            exigirNoAnuladaFGEM(actualFGEM);
            DatosCampaniaFGEM dFGEM = ValidadorAgriculturaFGEM.campaniaFGEM(datosFGEM, parcelaDeFGEM(cFGEM, datosFGEM, empresaFGEM.idFGEM()), hoyFGEM);
            boolean cosechaPreviaFGEM = repositorioFGEM.listarCosechasFGEM(cFGEM, empresaFGEM.idFGEM(), idFGEM, false).stream()
                    .anyMatch(hFGEM -> hFGEM.fechaFGEM().isBefore(dFGEM.fechaSiembraFGEM()));
            if (cosechaPreviaFGEM) {
                throw new ValidacionExceptionFGEM("Hay cosechas registradas antes de esa fecha de siembra.");
            }
            repositorioFGEM.actualizarCampaniaFGEM(cFGEM, idFGEM, dFGEM, usuarioFGEM, ahoraFGEM);
            return repositorioFGEM.buscarCampaniaFGEM(cFGEM, idFGEM).orElseThrow();
        });
    }

    public ResultadoCampaniaFGEM registrarCostoFGEM(long campaniaIdFGEM, LocalDate fechaFGEM, RubroCostoFGEM rubroFGEM, String descripcionFGEM,
                                            BigDecimal montoFGEM) {
        return guardarCostoFGEM(campaniaIdFGEM, null, fechaFGEM, rubroFGEM, descripcionFGEM, montoFGEM);
    }

    /** Corrige un costo: el original queda anulado y se registra el corregido, en una sola operación. */
    public ResultadoCampaniaFGEM corregirCostoFGEM(long costoIdFGEM, LocalDate fechaFGEM, RubroCostoFGEM rubroFGEM, String descripcionFGEM,
                                           BigDecimal montoFGEM) {
        return guardarCostoFGEM(null, costoIdFGEM, fechaFGEM, rubroFGEM, descripcionFGEM, montoFGEM);
    }

    private ResultadoCampaniaFGEM guardarCostoFGEM(Long campaniaDestinoFGEM, Long reemplazaFGEM, LocalDate fechaFGEM, RubroCostoFGEM rubroFGEM,
                                           String descripcionFGEM, BigDecimal montoFGEM) {
        sesionFGEM.requerirFGEM(PermisoFGEM.AGRO_GESTIONAR);
        EmpresaFGEM empresaFGEM = empresaAgropecuariaFGEM();
        LocalDate hoyFGEM = LocalDate.now(relojFGEM);
        LocalDate diaFGEM = ValidadorAgriculturaFGEM.fechaFGEM(fechaFGEM, "La fecha del costo", hoyFGEM);
        if (rubroFGEM == null) {
            throw new ValidacionExceptionFGEM("Selecciona el rubro del costo.");
        }
        String detalleFGEM = ValidacionesFGEM.requeridoFGEM(descripcionFGEM, "Descripción", 3, 100);
        BigDecimal importeFGEM = ValidadorAgriculturaFGEM.montoFGEM(montoFGEM, "Monto");
        String usuarioFGEM = sesionFGEM.usuarioFGEM().nombreUsuarioFGEM();
        LocalDateTime ahoraFGEM = LocalDateTime.now(relojFGEM);
        return databaseFGEM.enTransaccionFGEM(cFGEM -> {
            long campaniaIdFGEM = reemplazaFGEM == null ? campaniaDestinoFGEM : repositorioFGEM.anularRegistroFGEM(cFGEM, "costo_campania",
                    reemplazaFGEM, empresaFGEM.idFGEM(), usuarioFGEM, ahoraFGEM)
                    .orElseThrow(() -> new ValidacionExceptionFGEM("El costo no existe o está anulado. Actualiza la lista."));
            CampaniaFGEM campaniaFGEM = exigirCampaniaFGEM(cFGEM, campaniaIdFGEM, empresaFGEM.idFGEM());
            exigirNoAnuladaFGEM(campaniaFGEM);
            if (diaFGEM.isBefore(campaniaFGEM.fechaSiembraFGEM().minusMonths(MESES_COSTOS_PREVIOS_FGEM))) {
                throw new ValidacionExceptionFGEM("El costo no puede ser de más de " + MESES_COSTOS_PREVIOS_FGEM
                        + " meses antes de la siembra.");
            }
            repositorioFGEM.insertarCostoFGEM(cFGEM, campaniaIdFGEM, diaFGEM, rubroFGEM, detalleFGEM, importeFGEM, usuarioFGEM, ahoraFGEM);
            return resultadoFGEM(cFGEM, campaniaFGEM, empresaFGEM.idFGEM());
        });
    }

    public ResultadoCampaniaFGEM registrarCosechaFGEM(long campaniaIdFGEM, LocalDate fechaFGEM, BigDecimal kilosFGEM, DestinoCosechaFGEM destinoFGEM,
                                              BigDecimal precioFGEM, String observacionFGEM) {
        return guardarCosechaFGEM(campaniaIdFGEM, null, fechaFGEM, kilosFGEM, destinoFGEM, precioFGEM, observacionFGEM);
    }

    /**
     * Corrige una cosecha: la original queda anulada y se registra la corregida. Se permite aunque la
     * campaña ya esté cerrada como cosechada (corregir un dato no agrega una cosecha nueva).
     */
    public ResultadoCampaniaFGEM corregirCosechaFGEM(long cosechaIdFGEM, LocalDate fechaFGEM, BigDecimal kilosFGEM, DestinoCosechaFGEM destinoFGEM,
                                             BigDecimal precioFGEM, String observacionFGEM) {
        return guardarCosechaFGEM(null, cosechaIdFGEM, fechaFGEM, kilosFGEM, destinoFGEM, precioFGEM, observacionFGEM);
    }

    private ResultadoCampaniaFGEM guardarCosechaFGEM(Long campaniaDestinoFGEM, Long reemplazaFGEM, LocalDate fechaFGEM, BigDecimal kilosFGEM,
                                             DestinoCosechaFGEM destinoFGEM, BigDecimal precioFGEM, String observacionFGEM) {
        sesionFGEM.requerirFGEM(PermisoFGEM.AGRO_GESTIONAR);
        EmpresaFGEM empresaFGEM = empresaAgropecuariaFGEM();
        LocalDate diaFGEM = ValidadorAgriculturaFGEM.fechaFGEM(fechaFGEM, "La fecha de cosecha", LocalDate.now(relojFGEM));
        BigDecimal cantidadFGEM = ValidadorAgriculturaFGEM.montoFGEM(kilosFGEM, "Cantidad cosechada (kg)");
        if (destinoFGEM == null) {
            throw new ValidacionExceptionFGEM("Indica el destino de lo cosechado.");
        }
        BigDecimal precioVentaFGEM = destinoFGEM == DestinoCosechaFGEM.VENTA && precioFGEM != null
                ? ValidadorCatalogoFGEM.montoFGEM(precioFGEM, "Precio por kg") : null;
        String notaFGEM = ValidacionesFGEM.opcionalFGEM(observacionFGEM, "Observación", 150);
        String usuarioFGEM = sesionFGEM.usuarioFGEM().nombreUsuarioFGEM();
        LocalDateTime ahoraFGEM = LocalDateTime.now(relojFGEM);
        return databaseFGEM.enTransaccionFGEM(cFGEM -> {
            long campaniaIdFGEM = reemplazaFGEM == null ? campaniaDestinoFGEM : repositorioFGEM.anularRegistroFGEM(cFGEM, "cosecha",
                    reemplazaFGEM, empresaFGEM.idFGEM(), usuarioFGEM, ahoraFGEM)
                    .orElseThrow(() -> new ValidacionExceptionFGEM("La cosecha no existe o está anulada. Actualiza la lista."));
            CampaniaFGEM campaniaFGEM = exigirCampaniaFGEM(cFGEM, campaniaIdFGEM, empresaFGEM.idFGEM());
            exigirNoAnuladaFGEM(campaniaFGEM);
            if (reemplazaFGEM == null && campaniaFGEM.estadoFGEM() == EstadoCampaniaFGEM.COSECHADA) {
                throw new ValidacionExceptionFGEM("La campaña ya está cerrada como cosechada. Reábrela para agregar cosechas.");
            }
            if (diaFGEM.isBefore(campaniaFGEM.fechaSiembraFGEM())) {
                throw new ValidacionExceptionFGEM("La cosecha no puede ser anterior a la siembra.");
            }
            repositorioFGEM.insertarCosechaFGEM(cFGEM, campaniaIdFGEM, diaFGEM, cantidadFGEM, destinoFGEM, precioVentaFGEM, notaFGEM, usuarioFGEM, ahoraFGEM);
            return resultadoFGEM(cFGEM, campaniaFGEM, empresaFGEM.idFGEM());
        });
    }

    public void anularCostoFGEM(long costoIdFGEM) {
        anularRegistroFGEM("costo_campania", costoIdFGEM, "El costo no existe o ya está anulado.");
    }

    public void anularCosechaFGEM(long cosechaIdFGEM) {
        anularRegistroFGEM("cosecha", cosechaIdFGEM, "La cosecha no existe o ya está anulada.");
    }

    /**
     * Cierra la campaña.
     *
     * @param estadoFGEM {@link EstadoCampaniaFGEM#COSECHADA} (requiere al menos una cosecha) o
     *               {@link EstadoCampaniaFGEM#PERDIDA} (requiere el motivo: helada, granizo, sequía…)
     */
    public CampaniaFGEM cerrarCampaniaFGEM(long idFGEM, EstadoCampaniaFGEM estadoFGEM, LocalDate fechaFGEM, String motivoFGEM) {
        sesionFGEM.requerirFGEM(PermisoFGEM.AGRO_GESTIONAR);
        EmpresaFGEM empresaFGEM = empresaAgropecuariaFGEM();
        if (estadoFGEM == null || !estadoFGEM.cerradaFGEM()) {
            throw new ValidacionExceptionFGEM("Indica si la campaña se cosechó o se perdió.");
        }
        LocalDate diaFGEM = ValidadorAgriculturaFGEM.fechaFGEM(fechaFGEM, "La fecha de cierre", LocalDate.now(relojFGEM));
        String causaFGEM = estadoFGEM == EstadoCampaniaFGEM.PERDIDA
                ? ValidacionesFGEM.requeridoFGEM(motivoFGEM, "Causa de la pérdida", 3, 150)
                : ValidacionesFGEM.opcionalFGEM(motivoFGEM, "Observación", 150);
        String usuarioFGEM = sesionFGEM.usuarioFGEM().nombreUsuarioFGEM();
        LocalDateTime ahoraFGEM = LocalDateTime.now(relojFGEM);
        return databaseFGEM.enTransaccionFGEM(cFGEM -> {
            CampaniaFGEM campaniaFGEM = exigirCampaniaFGEM(cFGEM, idFGEM, empresaFGEM.idFGEM());
            if (campaniaFGEM.estadoFGEM() != EstadoCampaniaFGEM.EN_CURSO) {
                throw new ValidacionExceptionFGEM("Solo se puede cerrar una campaña en curso.");
            }
            if (diaFGEM.isBefore(campaniaFGEM.fechaSiembraFGEM())) {
                throw new ValidacionExceptionFGEM("El cierre no puede ser anterior a la siembra.");
            }
            if (estadoFGEM == EstadoCampaniaFGEM.COSECHADA && repositorioFGEM.listarCosechasFGEM(cFGEM, empresaFGEM.idFGEM(), idFGEM, false).isEmpty()) {
                throw new ValidacionExceptionFGEM("Registra al menos una cosecha antes de cerrar la campaña como cosechada.");
            }
            repositorioFGEM.cambiarEstadoFGEM(cFGEM, idFGEM, estadoFGEM, diaFGEM, causaFGEM, usuarioFGEM, ahoraFGEM);
            return repositorioFGEM.buscarCampaniaFGEM(cFGEM, idFGEM).orElseThrow();
        });
    }

    /** Vuelve a poner en curso una campaña cerrada (para corregir o agregar cosechas). */
    public CampaniaFGEM reabrirCampaniaFGEM(long idFGEM) {
        sesionFGEM.requerirFGEM(PermisoFGEM.AGRO_GESTIONAR);
        EmpresaFGEM empresaFGEM = empresaAgropecuariaFGEM();
        String usuarioFGEM = sesionFGEM.usuarioFGEM().nombreUsuarioFGEM();
        LocalDateTime ahoraFGEM = LocalDateTime.now(relojFGEM);
        return databaseFGEM.enTransaccionFGEM(cFGEM -> {
            CampaniaFGEM campaniaFGEM = exigirCampaniaFGEM(cFGEM, idFGEM, empresaFGEM.idFGEM());
            if (!campaniaFGEM.estadoFGEM().cerradaFGEM()) {
                throw new ValidacionExceptionFGEM("La campaña no está cerrada.");
            }
            repositorioFGEM.cambiarEstadoFGEM(cFGEM, idFGEM, EstadoCampaniaFGEM.EN_CURSO, null, null, usuarioFGEM, ahoraFGEM);
            return repositorioFGEM.buscarCampaniaFGEM(cFGEM, idFGEM).orElseThrow();
        });
    }

    /** Anula una campaña registrada por error: deja de contar en los resultados y en el balance. */
    public CampaniaFGEM anularCampaniaFGEM(long idFGEM, String motivoFGEM) {
        sesionFGEM.requerirFGEM(PermisoFGEM.AGRO_GESTIONAR);
        EmpresaFGEM empresaFGEM = empresaAgropecuariaFGEM();
        String motivoLimpioFGEM = ValidacionesFGEM.requeridoFGEM(motivoFGEM, "Motivo de la anulación", 5, 150);
        String usuarioFGEM = sesionFGEM.usuarioFGEM().nombreUsuarioFGEM();
        LocalDateTime ahoraFGEM = LocalDateTime.now(relojFGEM);
        return databaseFGEM.enTransaccionFGEM(cFGEM -> {
            CampaniaFGEM campaniaFGEM = exigirCampaniaFGEM(cFGEM, idFGEM, empresaFGEM.idFGEM());
            exigirNoAnuladaFGEM(campaniaFGEM);
            repositorioFGEM.cambiarEstadoFGEM(cFGEM, idFGEM, EstadoCampaniaFGEM.ANULADA, LocalDate.now(relojFGEM), motivoLimpioFGEM, usuarioFGEM, ahoraFGEM);
            return repositorioFGEM.buscarCampaniaFGEM(cFGEM, idFGEM).orElseThrow();
        });
    }

    // ------------------------------------------------------------------ utilidades

    private void anularRegistroFGEM(String tablaFGEM, long idFGEM, String mensajeFGEM) {
        sesionFGEM.requerirFGEM(PermisoFGEM.AGRO_GESTIONAR);
        EmpresaFGEM empresaFGEM = empresaAgropecuariaFGEM();
        String usuarioFGEM = sesionFGEM.usuarioFGEM().nombreUsuarioFGEM();
        LocalDateTime ahoraFGEM = LocalDateTime.now(relojFGEM);
        databaseFGEM.enTransaccionFGEM(cFGEM -> repositorioFGEM.anularRegistroFGEM(cFGEM, tablaFGEM, idFGEM, empresaFGEM.idFGEM(), usuarioFGEM, ahoraFGEM)
                .orElseThrow(() -> new ValidacionExceptionFGEM(mensajeFGEM)));
    }

    private ResultadoCampaniaFGEM resultadoFGEM(Connection cFGEM, CampaniaFGEM campaniaFGEM, long empresaIdFGEM) throws SQLException {
        CampaniaFGEM actualFGEM = repositorioFGEM.buscarCampaniaFGEM(cFGEM, campaniaFGEM.idFGEM()).orElseThrow();
        return ResultadoCampaniaFGEM.deFGEM(actualFGEM, repositorioFGEM.listarCostosFGEM(cFGEM, empresaIdFGEM, campaniaFGEM.idFGEM(), false),
                repositorioFGEM.listarCosechasFGEM(cFGEM, empresaIdFGEM, campaniaFGEM.idFGEM(), false));
    }

    private ParcelaFGEM parcelaDeFGEM(Connection cFGEM, DatosCampaniaFGEM datosFGEM, long empresaIdFGEM) throws SQLException {
        if (datosFGEM == null || datosFGEM.parcelaIdFGEM() == null) {
            throw new ValidacionExceptionFGEM("Selecciona la parcela.");
        }
        return exigirParcelaFGEM(cFGEM, datosFGEM.parcelaIdFGEM(), empresaIdFGEM);
    }

    private ParcelaFGEM exigirParcelaFGEM(Connection cFGEM, long idFGEM, long empresaIdFGEM) throws SQLException {
        return repositorioFGEM.buscarParcelaFGEM(cFGEM, idFGEM).filter(pFGEM -> pFGEM.empresaIdFGEM() == empresaIdFGEM)
                .orElseThrow(() -> new ValidacionExceptionFGEM("La parcela no existe en la empresa activa."));
    }

    private CampaniaFGEM exigirCampaniaFGEM(Connection cFGEM, long idFGEM, long empresaIdFGEM) throws SQLException {
        return repositorioFGEM.buscarCampaniaFGEM(cFGEM, idFGEM).filter(kFGEM -> kFGEM.empresaIdFGEM() == empresaIdFGEM)
                .orElseThrow(() -> new ValidacionExceptionFGEM("La campaña no existe en la empresa activa."));
    }

    private static void exigirNoAnuladaFGEM(CampaniaFGEM campaniaFGEM) {
        if (campaniaFGEM.estadoFGEM() == EstadoCampaniaFGEM.ANULADA) {
            throw new ValidacionExceptionFGEM("La campaña está anulada.");
        }
    }

    private void exigirNombreLibreFGEM(Connection cFGEM, long empresaIdFGEM, String nombreFGEM, long excluirIdFGEM) throws SQLException {
        if (repositorioFGEM.existeParcelaFGEM(cFGEM, empresaIdFGEM, nombreFGEM, excluirIdFGEM)) {
            throw new ValidacionExceptionFGEM("Ya existe una parcela llamada \"" + nombreFGEM + "\".");
        }
    }

    private EmpresaFGEM empresaAgropecuariaFGEM() {
        EmpresaFGEM empresaFGEM = empresasFGEM.empresaDeTrabajoFGEM();
        if (empresaFGEM.sectorFGEM() != SectorFGEM.AGROPECUARIO) {
            throw new ValidacionExceptionFGEM("La agricultura solo está disponible para empresas del sector agropecuario. "
                    + "Cambia el sector en Empresas → Editar.");
        }
        return empresaFGEM;
    }
}
