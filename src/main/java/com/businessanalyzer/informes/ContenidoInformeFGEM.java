package com.businessanalyzer.informes;

import com.businessanalyzer.analytics.diagnostico.SemaforoFGEM;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Contenido de un informe PDF como una lista de bloques (secciones, tablas, textos, gráficos...).
 * La plantilla {@code informe.jrxml} dibuja cada bloque según su tipo, así todos los informes
 * comparten el mismo diseño. Las tablas admiten un concepto y hasta cuatro columnas de valores.
 */
public final class ContenidoInformeFGEM {

    /** Tipos de bloque que reconoce la plantilla. */
    enum TipoFGEM { SECCION, ENCABEZADO, FILA, DATO, TOTAL, TEXTO, NOTA, INDICADORES, SEMAFORO, RECOMENDACION, GRAFICO, FIRMAS, ESPACIO }

    static final int COLUMNAS_FGEM = 4;

    private final List<Map<String, ?>> bloquesFGEM = new ArrayList<>();

    public ContenidoInformeFGEM seccionFGEM(String tituloFGEM) {
        return agregarFGEM(TipoFGEM.SECCION, tituloFGEM);
    }

    /** Fila de títulos de una tabla. */
    public ContenidoInformeFGEM encabezadoFGEM(String conceptoFGEM, String... columnasFGEM) {
        return agregarFGEM(TipoFGEM.ENCABEZADO, conceptoFGEM, columnasFGEM);
    }

    public ContenidoInformeFGEM filaFGEM(String conceptoFGEM, String... valoresFGEM) {
        return agregarFGEM(TipoFGEM.FILA, conceptoFGEM, valoresFGEM);
    }

    /** Etiqueta con un valor largo que ocupa el resto del ancho y se ajusta en varias líneas. */
    public ContenidoInformeFGEM datoFGEM(String etiquetaFGEM, String valorFGEM) {
        return agregarFGEM(TipoFGEM.DATO, etiquetaFGEM, valorFGEM);
    }

    /** Dos espacios para firmar, con el nombre bajo cada línea. */
    public ContenidoInformeFGEM firmasFGEM(String izquierdaFGEM, String derechaFGEM) {
        return agregarFGEM(TipoFGEM.FIRMAS, null, izquierdaFGEM, derechaFGEM);
    }

    /** Fila resaltada (subtotales y totales). */
    public ContenidoInformeFGEM totalFGEM(String conceptoFGEM, String... valoresFGEM) {
        return agregarFGEM(TipoFGEM.TOTAL, conceptoFGEM, valoresFGEM);
    }

    /** Párrafo de texto normal. */
    public ContenidoInformeFGEM textoFGEM(String textoFGEM) {
        return agregarFGEM(TipoFGEM.TEXTO, textoFGEM);
    }

    /** Texto pequeño y gris (aclaraciones, fuentes de los datos). */
    public ContenidoInformeFGEM notaFGEM(String textoFGEM) {
        return agregarFGEM(TipoFGEM.NOTA, textoFGEM);
    }

    /**
     * Fila de hasta cuatro tarjetas de indicadores.
     *
     * @param etiquetasYValoresFGEM pares etiqueta, valor (por ejemplo "Ventas netas", "S/ 1,200.00")
     */
    public ContenidoInformeFGEM indicadoresFGEM(String... etiquetasYValoresFGEM) {
        if (etiquetasYValoresFGEM.length % 2 != 0 || etiquetasYValoresFGEM.length > COLUMNAS_FGEM * 2) {
            throw new IllegalArgumentException("Se esperan hasta cuatro pares etiqueta-valor.");
        }
        Map<String, Object> bloqueFGEM = baseFGEM(TipoFGEM.INDICADORES, null);
        for (int iFGEM = 0; iFGEM < etiquetasYValoresFGEM.length / 2; iFGEM++) {
            bloqueFGEM.put("k" + (iFGEM + 1), etiquetasYValoresFGEM[2 * iFGEM]);
            bloqueFGEM.put("c" + (iFGEM + 1), etiquetasYValoresFGEM[2 * iFGEM + 1]);
        }
        bloquesFGEM.add(bloqueFGEM);
        return this;
    }

    /** Indicador con su color de semáforo, valor, estado y explicación. */
    public ContenidoInformeFGEM semaforoFGEM(String nombreFGEM, SemaforoFGEM semaforoFGEM, String valorFGEM, String explicacionFGEM) {
        Map<String, Object> bloqueFGEM = baseFGEM(TipoFGEM.SEMAFORO, nombreFGEM);
        bloqueFGEM.put("estado", semaforoFGEM.name());
        bloqueFGEM.put("c1", valorFGEM);
        bloqueFGEM.put("c2", semaforoFGEM.descripcionFGEM());
        bloqueFGEM.put("detalle", explicacionFGEM);
        bloquesFGEM.add(bloqueFGEM);
        return this;
    }

    /** Recomendación con su etiqueta de prioridad coloreada. */
    public ContenidoInformeFGEM recomendacionFGEM(String prioridadFGEM, SemaforoFGEM semaforoFGEM, String tituloFGEM, String textoFGEM) {
        Map<String, Object> bloqueFGEM = baseFGEM(TipoFGEM.RECOMENDACION, tituloFGEM);
        bloqueFGEM.put("estado", semaforoFGEM.name());
        bloqueFGEM.put("c1", prioridadFGEM);
        bloqueFGEM.put("detalle", textoFGEM);
        bloquesFGEM.add(bloqueFGEM);
        return this;
    }

    public ContenidoInformeFGEM graficoFGEM(BufferedImage imagenFGEM) {
        Map<String, Object> bloqueFGEM = baseFGEM(TipoFGEM.GRAFICO, null);
        bloqueFGEM.put("imagen", imagenFGEM);
        bloquesFGEM.add(bloqueFGEM);
        return this;
    }

    public ContenidoInformeFGEM espacioFGEM() {
        return agregarFGEM(TipoFGEM.ESPACIO, null);
    }

    public boolean vacioFGEM() {
        return bloquesFGEM.isEmpty();
    }

    /** Bloques en el formato que espera la plantilla (un mapa por bloque). */
    List<Map<String, ?>> bloquesFGEM() {
        return Collections.unmodifiableList(bloquesFGEM);
    }

    /** Texto plano de todo el contenido (para pruebas y depuración). */
    public String textoPlanoFGEM() {
        StringBuilder sbFGEM = new StringBuilder();
        for (Map<String, ?> bFGEM : bloquesFGEM) {
            for (String claveFGEM : List.of("concepto", "k1", "c1", "k2", "c2", "k3", "c3", "k4", "c4", "detalle")) {
                Object valorFGEM = bFGEM.get(claveFGEM);
                if (valorFGEM != null) {
                    sbFGEM.append(valorFGEM).append(' ');
                }
            }
            sbFGEM.append('\n');
        }
        return sbFGEM.toString();
    }

    private ContenidoInformeFGEM agregarFGEM(TipoFGEM tipoFGEM, String conceptoFGEM, String... valoresFGEM) {
        if (valoresFGEM.length > COLUMNAS_FGEM) {
            throw new IllegalArgumentException("Una tabla admite hasta " + COLUMNAS_FGEM + " columnas de valores.");
        }
        Map<String, Object> bloqueFGEM = baseFGEM(tipoFGEM, conceptoFGEM);
        for (int iFGEM = 0; iFGEM < valoresFGEM.length; iFGEM++) {
            bloqueFGEM.put("c" + (iFGEM + 1), valoresFGEM[iFGEM]);
        }
        bloquesFGEM.add(bloqueFGEM);
        return this;
    }

    private static Map<String, Object> baseFGEM(TipoFGEM tipoFGEM, String conceptoFGEM) {
        Map<String, Object> bloqueFGEM = new HashMap<>();
        bloqueFGEM.put("tipo", tipoFGEM.name());
        bloqueFGEM.put("concepto", conceptoFGEM);
        return bloqueFGEM;
    }
}
