package co.monitorsiae.persistencia;

import co.monitorsiae.dominio.FranjaHoraria;
import co.monitorsiae.dominio.HorarioOperacion;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

import java.io.IOException;
import java.time.DateTimeException;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Lee el horario en el formato actual (una franja por día) y en el anterior a la versión 3 (días con
 * un único rango compartido), que se convierte a una franja idéntica por cada día marcado.
 */
final class DeserializadorHorario extends StdDeserializer<HorarioOperacion> {

    DeserializadorHorario() {
        super(HorarioOperacion.class);
    }

    @Override
    public HorarioOperacion deserialize(JsonParser parser, DeserializationContext contexto) throws IOException {
        JsonNode nodo = parser.getCodec().readTree(parser);
        try {
            boolean restringir = nodo.path("restringir").asBoolean(false);
            ZoneId zona = nodo.hasNonNull("zona") ? ZoneId.of(nodo.get("zona").asText()) : null;
            if (nodo.has("franjas")) {
                return new HorarioOperacion(restringir, franjasPorDia(nodo.get("franjas")), zona);
            }
            return new HorarioOperacion(restringir, diasAntiguos(nodo.get("dias")),
                    hora(nodo, "desde"), hora(nodo, "hasta"), zona);
        } catch (DateTimeException | IllegalArgumentException e) {
            throw JsonMappingException.from(parser, "El horario de operación no es válido: " + e.getMessage(), e);
        }
    }

    private static Map<DayOfWeek, FranjaHoraria> franjasPorDia(JsonNode franjas) {
        Map<DayOfWeek, FranjaHoraria> resultado = new EnumMap<>(DayOfWeek.class);
        franjas.fields().forEachRemaining(entrada -> resultado.put(DayOfWeek.valueOf(entrada.getKey()),
                new FranjaHoraria(hora(entrada.getValue(), "desde"), hora(entrada.getValue(), "hasta"))));
        return resultado;
    }

    private static List<DayOfWeek> diasAntiguos(JsonNode dias) {
        List<DayOfWeek> resultado = new ArrayList<>();
        if (dias != null && dias.isArray()) {
            dias.forEach(dia -> resultado.add(DayOfWeek.valueOf(dia.asText())));
        }
        return resultado;
    }

    private static LocalTime hora(JsonNode nodo, String campo) {
        return nodo.hasNonNull(campo) ? LocalTime.parse(nodo.get(campo).asText()) : null;
    }
}
