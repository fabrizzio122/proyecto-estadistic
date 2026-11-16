package com.businessanalyzer.service;

import com.businessanalyzer.config.AppConfigFGEM;
import com.businessanalyzer.exception.ValidacionExceptionFGEM;

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
final class ArchivoCopiaFGEM {

    static final String ENTRADA_BASE_DATOS_FGEM = "business-analyzer.db";
    private static final String PREFIJO_FOTOS_FGEM = AppConfigFGEM.CARPETA_FOTOS_FGEM + "/";
    private static final byte[] CABECERA_ZIP_FGEM = {'P', 'K', 3, 4};

    private ArchivoCopiaFGEM() {
    }

    /** El archivo es un ZIP (copia nueva) y no una base de datos suelta (copia antigua). */
    static boolean esZipFGEM(Path archivoFGEM) {
        try (InputStream entradaFGEM = Files.newInputStream(archivoFGEM)) {
            return Arrays.equals(entradaFGEM.readNBytes(CABECERA_ZIP_FGEM.length), CABECERA_ZIP_FGEM);
        } catch (IOException eFGEM) {
            return false;
        }
    }

    /** Empaqueta la base de datos y, si existe, la carpeta de fotos. Devuelve la cantidad de fotos incluidas. */
    static int crearFGEM(Path baseDatosFGEM, Path carpetaFotosFGEM, Path destinoFGEM) throws IOException {
        int fotosFGEM = 0;
        try (ZipOutputStream zipFGEM = new ZipOutputStream(Files.newOutputStream(destinoFGEM))) {
            zipFGEM.putNextEntry(new ZipEntry(ENTRADA_BASE_DATOS_FGEM));
            Files.copy(baseDatosFGEM, zipFGEM);
            zipFGEM.closeEntry();
            if (Files.isDirectory(carpetaFotosFGEM)) {
                List<Path> archivosFGEM;
                try (Stream<Path> recorridoFGEM = Files.walk(carpetaFotosFGEM)) {
                    archivosFGEM = recorridoFGEM.filter(Files::isRegularFile).sorted().toList();
                }
                for (Path fotoFGEM : archivosFGEM) {
                    String relativoFGEM = carpetaFotosFGEM.relativize(fotoFGEM).toString().replace('\\', '/');
                    ZipEntry entradaFGEM = new ZipEntry(PREFIJO_FOTOS_FGEM + relativoFGEM);
                    // Las fotos ya vienen comprimidas: se guardan tal cual para no gastar tiempo.
                    entradaFGEM.setMethod(ZipEntry.DEFLATED);
                    zipFGEM.setLevel(java.util.zip.Deflater.NO_COMPRESSION);
                    zipFGEM.putNextEntry(entradaFGEM);
                    Files.copy(fotoFGEM, zipFGEM);
                    zipFGEM.closeEntry();
                    zipFGEM.setLevel(java.util.zip.Deflater.DEFAULT_COMPRESSION);
                    fotosFGEM++;
                }
            }
        }
        return fotosFGEM;
    }

    /**
     * Extrae la base de datos a {@code destinoBaseDatos} y, si {@code carpetaFotos} no es nula, las fotos
     * dentro de ella. Devuelve la cantidad de fotos encontradas en la copia.
     */
    static int extraerFGEM(Path zipCopiaFGEM, Path destinoBaseDatosFGEM, Path carpetaFotosFGEM) throws IOException {
        boolean conBaseDatosFGEM = false;
        int fotosFGEM = 0;
        try (ZipInputStream zipFGEM = new ZipInputStream(Files.newInputStream(zipCopiaFGEM))) {
            for (ZipEntry entradaFGEM = zipFGEM.getNextEntry(); entradaFGEM != null; entradaFGEM = zipFGEM.getNextEntry()) {
                String nombreFGEM = entradaFGEM.getName();
                if (entradaFGEM.isDirectory()) {
                    continue;
                }
                if (ENTRADA_BASE_DATOS_FGEM.equals(nombreFGEM)) {
                    if (destinoBaseDatosFGEM != null) {
                        copiarFGEM(zipFGEM, destinoBaseDatosFGEM);
                    }
                    conBaseDatosFGEM = true;
                } else if (nombreFGEM.startsWith(PREFIJO_FOTOS_FGEM)) {
                    fotosFGEM++;
                    if (carpetaFotosFGEM != null) {
                        Path destinoFGEM = carpetaFotosFGEM.resolve(nombreFGEM.substring(PREFIJO_FOTOS_FGEM.length())).normalize();
                        if (!destinoFGEM.startsWith(carpetaFotosFGEM.normalize())) {
                            throw new ValidacionExceptionFGEM("La copia contiene rutas no válidas.");
                        }
                        Files.createDirectories(destinoFGEM.getParent());
                        copiarFGEM(zipFGEM, destinoFGEM);
                    }
                }
            }
        }
        if (!conBaseDatosFGEM) {
            throw new ValidacionExceptionFGEM("El archivo no contiene la base de datos de Business Analyzer.");
        }
        return fotosFGEM;
    }

    private static void copiarFGEM(InputStream entradaFGEM, Path destinoFGEM) throws IOException {
        try (OutputStream salidaFGEM = Files.newOutputStream(destinoFGEM)) {
            entradaFGEM.transferTo(salidaFGEM);
        }
    }

    /** Mueve {@code origen} a {@code destino} reemplazándolo. */
    static void moverFGEM(Path origenFGEM, Path destinoFGEM) throws IOException {
        Files.move(origenFGEM, destinoFGEM, StandardCopyOption.REPLACE_EXISTING);
    }
}
