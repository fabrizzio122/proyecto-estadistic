package com.businessanalyzer.informes;

import com.businessanalyzer.analytics.diagnostico.Semaforo;

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
public final class ContenidoInforme {

    /** Tipos de bloque que reconoce la plantilla. */
    enum Tipo { SECCION, ENCABEZADO, FILA, DATO, TOTAL, TEXTO, NOTA, INDICADORES, SEMAFORO, RECOMENDACION, GRAFICO, FIRMAS, ESPACIO }

    static final int COLUMNAS = 4;

    private final List<Map<String, ?>> bloques = new ArrayList<>();

    public ContenidoInforme seccion(String titulo) {
        return agregar(Tipo.SECCION, titulo);
    }

    /** Fila de títulos de una tabla. */
    public ContenidoInforme encabezado(String concepto, String... columnas) {
        return agregar(Tipo.ENCABEZADO, concepto, columnas);
    }

    public ContenidoInforme fila(String concepto, String... valores) {
        return agregar(Tipo.FILA, concepto, valores);
    }

    /** Etiqueta con un valor largo que ocupa el resto del ancho y se ajusta en varias líneas. */
    public ContenidoInforme dato(String etiqueta, String valor) {
        return agregar(Tipo.DATO, etiqueta, valor);
    }

    /** Dos espacios para firmar, con el nombre bajo cada línea. */
    public ContenidoInforme firmas(String izquierda, String derecha) {
        return agregar(Tipo.FIRMAS, null, izquierda, derecha);
    }

    /** Fila resaltada (subtotales y totales). */
    public ContenidoInforme total(String concepto, String... valores) {
        return agregar(Tipo.TOTAL, concepto, valores);
    }

    /** Párrafo de texto normal. */
    public ContenidoInforme texto(String texto) {
        return agregar(Tipo.TEXTO, texto);
    }

    /** Texto pequeño y gris (aclaraciones, fuentes de los datos). */
    public ContenidoInforme nota(String texto) {
        return agregar(Tipo.NOTA, texto);
    }

    /**
     * Fila de hasta cuatro tarjetas de indicadores.
     *
     * @param etiquetasYValores pares etiqueta, valor (por ejemplo "Ventas netas", "S/ 1,200.00")
     */
    public ContenidoInforme indicadores(String... etiquetasYValores) {
        if (etiquetasYValores.length % 2 != 0 || etiquetasYValores.length > COLUMNAS * 2) {
            throw new IllegalArgumentException("Se esperan hasta cuatro pares etiqueta-valor.");
        }
        Map<String, Object> bloque = base(Tipo.INDICADORES, null);
        for (int i = 0; i < etiquetasYValores.length / 2; i++) {
            bloque.put("k" + (i + 1), etiquetasYValores[2 * i]);
            bloque.put("c" + (i + 1), etiquetasYValores[2 * i + 1]);
        }
        bloques.add(bloque);
        return this;
    }

    /** Indicador con su color de semáforo, valor, estado y explicación. */
    public ContenidoInforme semaforo(String nombre, Semaforo semaforo, String valor, String explicacion) {
        Map<String, Object> bloque = base(Tipo.SEMAFORO, nombre);
        bloque.put("estado", semaforo.name());
        bloque.put("c1", valor);
        bloque.put("c2", semaforo.descripcion());
        bloque.put("detalle", explicacion);
        bloques.add(bloque);
        return this;
    }

    /** Recomendación con su etiqueta de prioridad coloreada. */
    public ContenidoInforme recomendacion(String prioridad, Semaforo semaforo, String titulo, String texto) {
        Map<String, Object> bloque = base(Tipo.RECOMENDACION, titulo);
        bloque.put("estado", semaforo.name());
        bloque.put("c1", prioridad);
        bloque.put("detalle", texto);
        bloques.add(bloque);
        return this;
    }

    public ContenidoInforme grafico(BufferedImage imagen) {
        Map<String, Object> bloque = base(Tipo.GRAFICO, null);
        bloque.put("imagen", imagen);
        bloques.add(bloque);
        return this;
    }

    public ContenidoInforme espacio() {
        return agregar(Tipo.ESPACIO, null);
    }

    public boolean vacio() {
        return bloques.isEmpty();
    }

    /** Bloques en el formato que espera la plantilla (un mapa por bloque). */
    List<Map<String, ?>> bloques() {
        return Collections.unmodifiableList(bloques);
    }

    /** Texto plano de todo el contenido (para pruebas y depuración). */
    public String textoPlano() {
        StringBuilder sb = new StringBuilder();
        for (Map<String, ?> b : bloques) {
            for (String clave : List.of("concepto", "k1", "c1", "k2", "c2", "k3", "c3", "k4", "c4", "detalle")) {
                Object valor = b.get(clave);
                if (valor != null) {
                    sb.append(valor).append(' ');
                }
            }
            sb.append('\n');
        }
        return sb.toString();
    }

    private ContenidoInforme agregar(Tipo tipo, String concepto, String... valores) {
        if (valores.length > COLUMNAS) {
            throw new IllegalArgumentException("Una tabla admite hasta " + COLUMNAS + " columnas de valores.");
        }
        Map<String, Object> bloque = base(tipo, concepto);
        for (int i = 0; i < valores.length; i++) {
            bloque.put("c" + (i + 1), valores[i]);
        }
        bloques.add(bloque);
        return this;
    }

    private static Map<String, Object> base(Tipo tipo, String concepto) {
        Map<String, Object> bloque = new HashMap<>();
        bloque.put("tipo", tipo.name());
        bloque.put("concepto", concepto);
        return bloque;
    }
}
