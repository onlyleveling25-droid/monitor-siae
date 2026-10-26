package co.monitorsiae.app;

import co.monitorsiae.monitoreo.EventoOperativo;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Convierte eventos de la bitácora a CSV para abrirlos en Excel.
 *
 * <p>Usa punto y coma y la marca UTF-8 (BOM) porque así Excel en español separa las columnas y
 * conserva las tildes. Las celdas que empiezan por un signo de fórmula se neutralizan: el detalle
 * viene del portal y no debe poder ejecutarse como fórmula al abrir el archivo.
 */
public final class ExportadorBitacoraCsv {

    private static final String SEPARADOR = ";";
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final List<String> ENCABEZADO =
            List.of("Fecha y hora", "Cliente", "Tipo", "Seccional", "Arma", "Duración", "Captura", "Detalle");

    private ExportadorBitacoraCsv() {
    }

    public static String generar(List<EventoOperativo> eventos, ZoneId zona) {
        String filas = eventos.stream()
                .map(e -> fila(List.of(FECHA.format(e.momento().atZone(zona)), e.clienteNombre(),
                        e.tipo().etiqueta(), e.seccional(), e.arma(), e.duracionTexto(), e.captura(), e.detalle())))
                .collect(Collectors.joining("\r\n"));
        return fila(ENCABEZADO) + "\r\n" + filas + (filas.isEmpty() ? "" : "\r\n");
    }

    public static void escribir(Path destino, List<EventoOperativo> eventos, ZoneId zona) throws IOException {
        Files.writeString(destino, "﻿" + generar(eventos, zona), StandardCharsets.UTF_8);
    }

    private static String fila(List<String> celdas) {
        return celdas.stream().map(ExportadorBitacoraCsv::celda).collect(Collectors.joining(SEPARADOR));
    }

    private static String celda(String valor) {
        String texto = valor == null ? "" : valor;
        if (!texto.isEmpty() && "=+-@\t\r".indexOf(texto.charAt(0)) >= 0) {
            texto = "'" + texto;
        }
        boolean requiereComillas = texto.contains(SEPARADOR) || texto.contains("\"")
                || texto.contains("\n") || texto.contains("\r");
        return requiereComillas ? "\"" + texto.replace("\"", "\"\"") + "\"" : texto;
    }
}
