package co.monitorsiae.dominio;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ValidadorConfiguracionTest {

    private final ValidadorConfiguracion validador = new ValidadorConfiguracion();

    @Test
    void configuracionCompletaNoTieneProblemas() {
        Configuracion configuracion = Configuracion.porDefecto()
                .conClientes(List.of(ClientesDePrueba.valido("Ana")));

        assertTrue(validador.validar(configuracion).isEmpty());
    }

    @Test
    void descripcionDebeTenerEntre150Y500Caracteres() {
        Configuracion corta = conCliente(ClientesDePrueba.conDescripcion("Ana", "x".repeat(149)));
        Configuracion larga = conCliente(ClientesDePrueba.conDescripcion("Ana", "x".repeat(501)));
        Configuracion limiteInferior = conCliente(ClientesDePrueba.conDescripcion("Ana", "x".repeat(150)));
        Configuracion limiteSuperior = conCliente(ClientesDePrueba.conDescripcion("Ana", "x".repeat(500)));

        assertEquals(1, validador.validar(corta).size());
        assertEquals(1, validador.validar(larga).size());
        assertTrue(validador.validar(limiteInferior).isEmpty());
        assertTrue(validador.validar(limiteSuperior).isEmpty());
    }

    @Test
    void usarAlternativasExigeAlMenosUnaAlternativa() {
        ReglasSeleccion reglas = new ReglasSeleccion(Modalidad.PORTE, List.of(CriterioArma.de("glock")),
                PoliticaSinObjetivo.USAR_ALTERNATIVAS, List.of(), "", List.of(), 1);

        assertEquals(1, validador.validar(conCliente(ClientesDePrueba.conReglas("Ana", reglas))).size());
    }

    @Test
    void seguirEsperandoExigeAlMenosUnObjetivo() {
        ReglasSeleccion reglas = new ReglasSeleccion(Modalidad.PORTE, List.of(),
                PoliticaSinObjetivo.SEGUIR_ESPERANDO, List.of(), "", List.of(), 1);

        assertEquals(1, validador.validar(conCliente(ClientesDePrueba.conReglas("Ana", reglas))).size());
    }

    @Test
    void cualquieraDelTipoPermiteNoTenerObjetivosPeroExigeElTipo() {
        ReglasSeleccion conTipo = new ReglasSeleccion(Modalidad.PORTE, List.of(),
                PoliticaSinObjetivo.CUALQUIERA_DEL_TIPO, List.of(), "Pistola", List.of(), 1);
        ReglasSeleccion sinTipo = new ReglasSeleccion(Modalidad.PORTE, List.of(),
                PoliticaSinObjetivo.CUALQUIERA_DEL_TIPO, List.of(), " ", List.of(), 1);

        assertTrue(validador.validar(conCliente(ClientesDePrueba.conReglas("Ana", conTipo))).isEmpty());
        assertEquals(1, validador.validar(conCliente(ClientesDePrueba.conReglas("Ana", sinTipo))).size());
    }

    @Test
    void criterioSinPalabrasClaveEsInvalido() {
        ReglasSeleccion reglas = new ReglasSeleccion(Modalidad.PORTE, List.of(CriterioArma.de(" ", "")),
                PoliticaSinObjetivo.SEGUIR_ESPERANDO, List.of(), "", List.of(), 1);

        assertEquals(1, validador.validar(conCliente(ClientesDePrueba.conReglas("Ana", reglas))).size());
    }

    @Test
    void identificadoresRepetidosSonInvalidos() {
        Cliente original = ClientesDePrueba.valido("Ana");
        Cliente copia = new Cliente(original.id(), "Otra", true, original.tramite(), original.modo(),
                original.seleccion());

        List<ProblemaConfiguracion> problemas = validador.validar(
                Configuracion.porDefecto().conClientes(List.of(original, copia)));

        assertEquals(1, problemas.size());
    }

    @Test
    void horarioRestringidoExigeRangoValido() {
        HorarioOperacion invertido = new HorarioOperacion(true, List.of(java.time.DayOfWeek.MONDAY),
                java.time.LocalTime.of(16, 0), java.time.LocalTime.of(8, 0), HorarioOperacion.ZONA_COLOMBIA);
        Configuracion configuracion = new Configuracion(1, null, null, 5, invertido, List.of());

        assertEquals(1, validador.validar(configuracion).size());
    }

    private static Configuracion conCliente(Cliente cliente) {
        return Configuracion.porDefecto().conClientes(List.of(cliente));
    }
}
