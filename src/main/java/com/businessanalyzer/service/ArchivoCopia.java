package com.businessanalyzer.service;

import com.businessanalyzer.config.AppConfig;
import com.businessanalyzer.exception.ValidacionException;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * Formato de las copias de seguridad desde la versión con visitas de inspección: un archivo ZIP con
 * la base de datos ({@value #ENTRADA_BASE_DATOS}) y la carpeta de fotos ({@code fotos-visitas/...}).
 * Las copias antiguas, que son solo el archivo de la base de datos, se siguen aceptando.
 */
final class ArchivoCopia {

    static final String ENTRADA_BASE_DATOS = "business-analyzer.db";
    private static final String PREFIJO_FOTOS = AppConfig.CARPETA_FOTOS + "/";
    private static final byte[] CABECERA_ZIP = {'P', 'K', 3, 4};

    private ArchivoCopia() {
    }

    /** El archivo es un ZIP (copia nueva) y no una base de datos suelta (copia antigua). */
    static boolean esZip(Path archivo) {
        try (InputStream entrada = Files.newInputStream(archivo)) {
            return Arrays.equals(entrada.readNBytes(CABECERA_ZIP.length), CABECERA_ZIP);
        } catch (IOException e) {
            return false;
        }
    }

    /** Empaqueta la base de datos y, si existe, la carpeta de fotos. Devuelve la cantidad de fotos incluidas. */
    static int crear(Path baseDatos, Path carpetaFotos, Path destino) throws IOException {
        int fotos = 0;
        try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(destino))) {
            zip.putNextEntry(new ZipEntry(ENTRADA_BASE_DATOS));
            Files.copy(baseDatos, zip);
            zip.closeEntry();
            if (Files.isDirectory(carpetaFotos)) {
                List<Path> archivos;
                try (Stream<Path> recorrido = Files.walk(carpetaFotos)) {
                    archivos = recorrido.filter(Files::isRegularFile).sorted().toList();
                }
                for (Path foto : archivos) {
                    String relativo = carpetaFotos.relativize(foto).toString().replace('\\', '/');
                    ZipEntry entrada = new ZipEntry(PREFIJO_FOTOS + relativo);
                    // Las fotos ya vienen comprimidas: se guardan tal cual para no gastar tiempo.
                    entrada.setMethod(ZipEntry.DEFLATED);
                    zip.setLevel(java.util.zip.Deflater.NO_COMPRESSION);
                    zip.putNextEntry(entrada);
                    Files.copy(foto, zip);
                    zip.closeEntry();
                    zip.setLevel(java.util.zip.Deflater.DEFAULT_COMPRESSION);
                    fotos++;
                }
            }
        }
        return fotos;
    }

    /**
     * Extrae la base de datos a {@code destinoBaseDatos} y, si {@code carpetaFotos} no es nula, las fotos
     * dentro de ella. Devuelve la cantidad de fotos encontradas en la copia.
     */
    static int extraer(Path zipCopia, Path destinoBaseDatos, Path carpetaFotos) throws IOException {
        boolean conBaseDatos = false;
        int fotos = 0;
        try (ZipInputStream zip = new ZipInputStream(Files.newInputStream(zipCopia))) {
            for (ZipEntry entrada = zip.getNextEntry(); entrada != null; entrada = zip.getNextEntry()) {
                String nombre = entrada.getName();
                if (entrada.isDirectory()) {
                    continue;
                }
                if (ENTRADA_BASE_DATOS.equals(nombre)) {
                    if (destinoBaseDatos != null) {
                        copiar(zip, destinoBaseDatos);
                    }
                    conBaseDatos = true;
                } else if (nombre.startsWith(PREFIJO_FOTOS)) {
                    fotos++;
                    if (carpetaFotos != null) {
                        Path destino = carpetaFotos.resolve(nombre.substring(PREFIJO_FOTOS.length())).normalize();
                        if (!destino.startsWith(carpetaFotos.normalize())) {
                            throw new ValidacionException("La copia contiene rutas no válidas.");
                        }
                        Files.createDirectories(destino.getParent());
                        copiar(zip, destino);
                    }
                }
            }
        }
        if (!conBaseDatos) {
            throw new ValidacionException("El archivo no contiene la base de datos de Business Analyzer.");
        }
        return fotos;
    }

    private static void copiar(InputStream entrada, Path destino) throws IOException {
        try (OutputStream salida = Files.newOutputStream(destino)) {
            entrada.transferTo(salida);
        }
    }

    /** Mueve {@code origen} a {@code destino} reemplazándolo. */
    static void mover(Path origen, Path destino) throws IOException {
        Files.move(origen, destino, StandardCopyOption.REPLACE_EXISTING);
    }
}
