import javax.imageio.ImageIO;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Genera el icono de Monitor SIAE (un monitor con una linea de pulso) sin depender de editores
 * externos: {@code herramientas/icono/monitor-siae.ico} para el .exe y el instalador, y
 * {@code monitor-siae.png} para la ventana de la aplicacion.
 *
 * <p>Uso, desde la raiz del proyecto: {@code java herramientas/icono/GenerarIcono.java}
 */
public final class GenerarIcono {

    private static final int[] TAMANOS = {16, 24, 32, 48, 64, 128, 256};

    public static void main(String[] args) throws IOException {
        Path carpeta = Path.of("herramientas", "icono");
        Path pngVentana = Path.of("src", "main", "resources", "co", "monitorsiae", "ui", "icono-aplicacion.png");

        List<byte[]> imagenes = new ArrayList<>();
        for (int tamano : TAMANOS) {
            imagenes.add(png(dibujar(tamano)));
        }
        Files.write(carpeta.resolve("monitor-siae.ico"), ico(imagenes));
        Files.write(pngVentana, png(dibujar(256)));
        System.out.println("Generado " + carpeta.resolve("monitor-siae.ico") + " y " + pngVentana);
    }

    private static BufferedImage dibujar(int t) {
        BufferedImage imagen = new BufferedImage(t, t, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = imagen.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

        double margen = t * 0.03;
        double lado = t - 2 * margen;
        g.setPaint(new GradientPaint(0, 0, new Color(0x2563EB), 0, t, new Color(0x1E3A8A)));
        g.fill(new RoundRectangle2D.Double(margen, margen, lado, lado, t * 0.22, t * 0.22));

        // El dibujo vive en un lienzo de 24x24 y se centra con un margen interior.
        double escala = t * 0.64 / 24.0;
        double origen = t * 0.18;
        g.translate(origen, origen + t * 0.015);
        g.scale(escala, escala);
        // En tamanos pequenos el trazo es relativamente mas grueso para que no desaparezca.
        float grosor = t <= 32 ? 2.6f : 1.9f;
        g.setStroke(new BasicStroke(grosor, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(Color.WHITE);

        Path2D pantalla = new Path2D.Double();
        pantalla.moveTo(4, 5.5);
        pantalla.lineTo(20, 5.5);
        pantalla.lineTo(20, 16.5);
        pantalla.lineTo(4, 16.5);
        pantalla.closePath();
        g.draw(pantalla);

        Path2D soporte = new Path2D.Double();
        soporte.moveTo(8, 20);
        soporte.lineTo(16, 20);
        soporte.moveTo(12, 16.5);
        soporte.lineTo(12, 20);
        g.draw(soporte);

        Path2D pulso = new Path2D.Double();
        pulso.moveTo(6.8, 11);
        pulso.lineTo(9, 11);
        pulso.lineTo(10.4, 8);
        pulso.lineTo(13, 13.8);
        pulso.lineTo(14.4, 11);
        pulso.lineTo(17.2, 11);
        g.setColor(new Color(0xFCD34D));
        g.draw(pulso);

        g.dispose();
        return imagen;
    }

    private static byte[] png(BufferedImage imagen) throws IOException {
        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        ImageIO.write(imagen, "png", salida);
        return salida.toByteArray();
    }

    /** Contenedor .ico con cada tamano guardado como PNG (admitido desde Windows Vista). */
    private static byte[] ico(List<byte[]> pngs) {
        int n = pngs.size();
        int desplazamiento = 6 + 16 * n;
        int total = desplazamiento;
        for (byte[] p : pngs) {
            total += p.length;
        }
        ByteBuffer b = ByteBuffer.allocate(total).order(ByteOrder.LITTLE_ENDIAN);
        b.putShort((short) 0).putShort((short) 1).putShort((short) n);
        int cursor = desplazamiento;
        for (int i = 0; i < n; i++) {
            int lado = TAMANOS[i];
            b.put((byte) (lado >= 256 ? 0 : lado)).put((byte) (lado >= 256 ? 0 : lado));
            b.put((byte) 0).put((byte) 0);
            b.putShort((short) 1).putShort((short) 32);
            b.putInt(pngs.get(i).length).putInt(cursor);
            cursor += pngs.get(i).length;
        }
        for (byte[] p : pngs) {
            b.put(p);
        }
        return b.array();
    }
}
