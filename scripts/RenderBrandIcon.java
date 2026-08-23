import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;

/** Deterministically renders the repository and Play Store exports from the shipping vector geometry. */
public final class RenderBrandIcon {
    private static final int SIZE = 512;
    private static final double VIEWPORT = 108.0;
    private static final double GLYPH_SCALE = 0.86;
    private static final double GLYPH_TRANSLATE_X = 56.5 * (1.0 - GLYPH_SCALE) - 2.5;
    private static final double GLYPH_TRANSLATE_Y = 53.0 * (1.0 - GLYPH_SCALE) + 1.0;

    static {
        System.setProperty("java.awt.headless", "true");
    }

    private static final Color BACKGROUND = new Color(0x09, 0x09, 0x09);
    private static final Color FOREGROUND = new Color(0xF5, 0xF3, 0xEA);

    private RenderBrandIcon() {}

    public static void main(String[] args) throws IOException {
        Path outputDirectory = Path.of("docs", "brand");
        Files.createDirectories(outputDirectory);

        BufferedImage png = render(BufferedImage.TYPE_INT_ARGB);
        ImageIO.write(png, "png", outputDirectory.resolve("whip-app-icon-play.png").toFile());

        BufferedImage jpg = new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_RGB);
        Graphics2D jpgGraphics = jpg.createGraphics();
        jpgGraphics.drawImage(png, 0, 0, null);
        jpgGraphics.dispose();
        writeJpeg(jpg, outputDirectory.resolve("whip-app-icon.jpg"), 0.95f);
    }

    private static BufferedImage render(int imageType) {
        BufferedImage image = new BufferedImage(SIZE, SIZE, imageType);
        Graphics2D graphics = image.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        graphics.setColor(BACKGROUND);
        graphics.fillRect(0, 0, SIZE, SIZE);

        double pixelsPerUnit = SIZE / VIEWPORT;
        AffineTransform transform = new AffineTransform(
                pixelsPerUnit * GLYPH_SCALE,
                0,
                0,
                pixelsPerUnit * GLYPH_SCALE,
                pixelsPerUnit * GLYPH_TRANSLATE_X,
                pixelsPerUnit * GLYPH_TRANSLATE_Y);

        Path2D.Double mark = new Path2D.Double();
        mark.moveTo(32, 36);
        mark.lineTo(32, 56);
        mark.curveTo(32, 65, 36, 69, 41, 69);
        mark.curveTo(46, 69, 49, 64, 52, 59);
        mark.lineTo(56, 52);
        mark.curveTo(58, 48.5, 61.5, 48.5, 64, 52.5);
        mark.lineTo(69, 61.5);
        mark.curveTo(72, 66.5, 75, 70, 78, 68);
        mark.curveTo(81, 65.5, 81, 60.5, 81, 56);
        mark.lineTo(81, 36);
        graphics.setColor(FOREGROUND);
        graphics.setStroke(new BasicStroke(
                (float) (11 * pixelsPerUnit * GLYPH_SCALE),
                BasicStroke.CAP_ROUND,
                BasicStroke.JOIN_ROUND));
        graphics.draw(transform.createTransformedShape(mark));

        Path2D.Double separation = new Path2D.Double();
        separation.moveTo(48, 50.5);
        separation.curveTo(51, 49.5, 53.5, 50.5, 55.5, 53);
        separation.curveTo(57, 55, 58.5, 58, 60, 60.5);
        graphics.setColor(BACKGROUND);
        graphics.setStroke(new BasicStroke(
                (float) (1.5 * pixelsPerUnit * GLYPH_SCALE),
                BasicStroke.CAP_ROUND,
                BasicStroke.JOIN_ROUND));
        graphics.draw(transform.createTransformedShape(separation));
        graphics.dispose();
        return image;
    }

    private static void writeJpeg(BufferedImage image, Path path, float quality) throws IOException {
        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpg");
        if (!writers.hasNext()) throw new IOException("No JPEG writer is available");
        ImageWriter writer = writers.next();
        ImageWriteParam parameters = writer.getDefaultWriteParam();
        parameters.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
        parameters.setCompressionQuality(quality);
        try (ImageOutputStream output = ImageIO.createImageOutputStream(path.toFile())) {
            writer.setOutput(output);
            writer.write(null, new IIOImage(image, null, null), parameters);
        } finally {
            writer.dispose();
        }
    }
}
