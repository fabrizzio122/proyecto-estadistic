package com.businessanalyzer.model;

import java.util.Arrays;
import java.util.List;

/**
 * Tipos de bienes y derechos que puede registrar una empresa.
 * <p>
 * Los corrientes se convierten en dinero en menos de un año (caja, lo que deben los clientes);
 * los no corrientes son los bienes que se usan para operar (local, máquinas, vehículos).
 * El inventario de mercadería no figura aquí: se calcula solo con el stock y el costo del catálogo.
 */
public enum CategoriaActivoFGEM {

    CAJA_BANCOS("Caja y bancos", true, null,
            "Dinero en efectivo y en cuentas bancarias. Actualiza el saldo cada cierto tiempo."),
    CUENTAS_POR_COBRAR("Cuentas por cobrar", true, null,
            "Lo que los clientes le deben al negocio por ventas al crédito."),
    OTROS_CORRIENTES("Otros activos corrientes", true, null,
            "Adelantos a proveedores, garantías u otros montos que se recuperan en menos de un año."),
    TERRENOS("Terrenos", false, null,
            "Los terrenos no se deprecian."),
    EDIFICIOS("Edificios y construcciones", false, 20,
            "Local propio, almacén o ampliaciones. Vida útil habitual: 20 años."),
    MAQUINARIA("Maquinaria y equipos", false, 10,
            "Máquinas, herramientas y equipos de producción. Vida útil habitual: 10 años."),
    VEHICULOS("Vehículos", false, 5,
            "Autos, motos, camiones y otros vehículos. Vida útil habitual: 5 años."),
    MOBILIARIO("Muebles y enseres", false, 10,
            "Anaqueles, vitrinas, mesas, sillas y refrigeradoras. Vida útil habitual: 10 años."),
    EQUIPOS_COMPUTO("Equipos de cómputo", false, 4,
            "Computadoras, impresoras y cajas registradoras. Vida útil habitual: 4 años."),
    OTROS_NO_CORRIENTES("Otros activos fijos", false, 10,
            "Cualquier otro bien que se use varios años. Deja la vida útil vacía si no se deprecia.");

    private final String descripcionFGEM;
    private final boolean corrienteFGEM;
    private final Integer vidaUtilSugeridaFGEM;
    private final String ayudaFGEM;

    CategoriaActivoFGEM(String descripcionFGEM, boolean corrienteFGEM, Integer vidaUtilSugeridaFGEM, String ayudaFGEM) {
        this.descripcionFGEM = descripcionFGEM;
        this.corrienteFGEM = corrienteFGEM;
        this.vidaUtilSugeridaFGEM = vidaUtilSugeridaFGEM;
        this.ayudaFGEM = ayudaFGEM;
    }

    public String descripcionFGEM() {
        return descripcionFGEM;
    }

    /** Se convierte en dinero en menos de un año. */
    public boolean corrienteFGEM() {
        return corrienteFGEM;
    }

    /** Puede tener vida útil y depreciarse (activos fijos, salvo terrenos). */
    public boolean depreciableFGEM() {
        return !corrienteFGEM && this != TERRENOS;
    }

    /** Años de vida útil propuestos al registrar; {@code null} si no se deprecia. */
    public Integer vidaUtilSugeridaFGEM() {
        return vidaUtilSugeridaFGEM;
    }

    public String ayudaFGEM() {
        return ayudaFGEM;
    }

    public static List<CategoriaActivoFGEM> deFGEM(boolean corrientesFGEM) {
        return Arrays.stream(values()).filter(cFGEM -> cFGEM.corrienteFGEM == corrientesFGEM).toList();
    }

    @Override
    public String toString() {
        return descripcionFGEM;
    }
}
