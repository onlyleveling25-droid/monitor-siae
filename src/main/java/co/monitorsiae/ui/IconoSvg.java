package co.monitorsiae.ui;

import java.io.InputStream;
import java.util.Objects;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;

import javafx.scene.Node;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.SVGPath;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

final class IconoSvg {

    private static final double VIEW_BOX_SIZE = 24;

    private IconoSvg() {
    }

    static Node crear(String nombre, double tamano, String color) {
        String recurso = "icons/" + nombre + ".svg";
        try (InputStream entrada = IconoSvg.class.getResourceAsStream(recurso)) {
            if (entrada == null) {
                throw new IllegalStateException("No se encontró el icono SVG " + recurso + ".");
            }
            DocumentBuilderFactory fabrica = DocumentBuilderFactory.newInstance();
            fabrica.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            fabrica.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            fabrica.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            fabrica.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
            fabrica.setXIncludeAware(false);
            fabrica.setExpandEntityReferences(false);

            NodeList caminos = fabrica.newDocumentBuilder().parse(entrada)
                    .getDocumentElement().getElementsByTagName("path");
            StringBuilder datos = new StringBuilder();
            for (int i = 0; i < caminos.getLength(); i++) {
                Element camino = (Element) caminos.item(i);
                if (datos.length() > 0) {
                    datos.append(' ');
                }
                datos.append(Objects.requireNonNull(camino.getAttribute("d")));
            }
            if (datos.length() == 0) {
                throw new IllegalStateException("El icono SVG no contiene trazados: " + recurso);
            }

            SVGPath trazado = new SVGPath();
            trazado.setContent(datos.toString());
            trazado.setFill(Color.TRANSPARENT);
            trazado.setStroke(Color.web(color));
            trazado.setStrokeWidth(1.8);
            trazado.setStrokeLineCap(javafx.scene.shape.StrokeLineCap.ROUND);
            trazado.setStrokeLineJoin(javafx.scene.shape.StrokeLineJoin.ROUND);
            double escala = tamano / VIEW_BOX_SIZE;
            trazado.setScaleX(escala);
            trazado.setScaleY(escala);

            StackPane icono = new StackPane(trazado);
            icono.setMinSize(tamano, tamano);
            icono.setPrefSize(tamano, tamano);
            icono.setMaxSize(tamano, tamano);
            icono.setMouseTransparent(true);
            return icono;
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo cargar el icono SVG " + recurso + ".", e);
        }
    }
}
