package com.businessanalyzer.service;

import com.businessanalyzer.exception.ValidacionException;
import com.businessanalyzer.model.CuentaDinero;
import com.businessanalyzer.repository.TesoreriaRepository;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Objects;

/**
 * Resuelve y verifica la cuenta de dinero que indica un registro (venta, gasto, pago…). La comparten
 * los servicios que mueven dinero.
 */
final class CuentasEmpresa {

    private final TesoreriaRepository tesoreria;

    CuentasEmpresa(TesoreriaRepository tesoreria) {
        this.tesoreria = tesoreria;
    }

    /** La cuenta indicada o, si no se indicó, la cuenta principal de la empresa. */
    long resolver(Connection c, long empresaId, Long cuentaId, Long actual) throws SQLException {
        if (cuentaId != null) {
            return exigir(c, empresaId, cuentaId, actual).id();
        }
        return tesoreria.cuentaPrincipal(c, empresaId)
                .orElseThrow(() -> new ValidacionException(
                        "La empresa no tiene cuentas activas. Registra una en «Tesorería»."))
                .id();
    }

    /** La cuenta indicada (verificada) o {@code null} si no se indicó. */
    Long opcional(Connection c, long empresaId, Long cuentaId, Long actual) throws SQLException {
        return cuentaId == null ? null : exigir(c, empresaId, cuentaId, actual).id();
    }

    /**
     * Verifica que la cuenta sea de la empresa y esté activa. Una cuenta desactivada se acepta solo si el
     * registro ya la usaba ({@code actual}), para poder corregir otros datos sin cambiarla.
     */
    CuentaDinero exigir(Connection c, long empresaId, long cuentaId, Long actual) throws SQLException {
        CuentaDinero cuenta = tesoreria.buscarCuenta(c, cuentaId)
                .filter(k -> k.empresaId() == empresaId)
                .orElseThrow(() -> new ValidacionException("La cuenta no existe en la empresa activa."));
        if (!cuenta.activa() && !Objects.equals(actual, cuentaId)) {
            throw new ValidacionException("La cuenta «" + cuenta.nombre() + "» está desactivada.");
        }
        return cuenta;
    }
}
