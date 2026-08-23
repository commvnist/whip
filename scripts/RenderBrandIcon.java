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
import java.util.List;
import java.util.Iterator;
import java.util.Locale;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;

/** Deterministically renders the repository and Play Store exports from the shipping vector geometry. */
public final class RenderBrandIcon {
    private static final int SIZE = 512;
    private static final int FEATURE_WIDTH = 1024;
    private static final int FEATURE_HEIGHT = 500;
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
        if (args.length == 1 && "--verify".equals(args[0])) {
            verifyImage(outputDirectory.resolve("whip-app-icon-play.png"), 512, 512, true);
            verifyImage(outputDirectory.resolve("whip-app-icon.jpg"), 512, 512, false);
            verifyImage(outputDirectory.resolve("whip-feature-graphic-play.png"), 1024, 500, false);
            verifyImage(outputDirectory.resolve("whip-feature-graphic.jpg"), 1024, 500, false);
            int screenshotCount = verifyPhoneScreenshots(Path.of("docs", "play-store", "screenshots"));
            System.out.println(
                    "Play Store assets verified: 512x512 icon, 1024x500 feature graphic, and "
                            + screenshotCount + " phone screenshots");
            return;
        }

        BufferedImage png = render(BufferedImage.TYPE_INT_ARGB);
        ImageIO.write(png, "png", outputDirectory.resolve("whip-app-icon-play.png").toFile());

        BufferedImage jpg = new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_RGB);
        Graphics2D jpgGraphics = jpg.createGraphics();
        jpgGraphics.drawImage(png, 0, 0, null);
        jpgGraphics.dispose();
        writeJpeg(jpg, outputDirectory.resolve("whip-app-icon.jpg"), 0.95f);

        BufferedImage featureGraphic = renderFeatureGraphic(png);
        ImageIO.write(featureGraphic, "png", outputDirectory.resolve("whip-feature-graphic-play.png").toFile());
        writeJpeg(featureGraphic, outputDirectory.resolve("whip-feature-graphic.jpg"), 0.95f);
    }

    private static void verifyImage(Path path, int width, int height, boolean alphaExpected) throws IOException {
        if (!Files.isRegularFile(path)) throw new IOException("Missing required Play Store asset: " + path);
        BufferedImage image = ImageIO.read(path.toFile());
        if (image == null) throw new IOException("Unreadable Play Store asset: " + path);
        if (image.getWidth() != width || image.getHeight() != height) {
            throw new IOException(path + " must be " + width + "x" + height + " but is " + image.getWidth() + "x" + image.getHeight());
        }
        if (image.getColorModel().hasAlpha() != alphaExpected) {
            throw new IOException(path + (alphaExpected ? " must retain an alpha channel" : " must be an opaque RGB image"));
        }
    }

    private static int verifyPhoneScreenshots(Path directory) throws IOException {
        if (!Files.isDirectory(directory)) {
            throw new IOException("Missing Play Store screenshot directory: " + directory);
        }
        List<Path> screenshots;
        try (var entries = Files.list(directory)) {
            screenshots = entries
                    .filter(Files::isRegularFile)
                    .filter(RenderBrandIcon::isPlayStoreImage)
                    .sorted()
                    .toList();
        }
        if (screenshots.size() < 2 || screenshots.size() > 8) {
            throw new IOException(
                    directory + " must contain 2–8 PNG or JPEG phone screenshots but contains " + screenshots.size());
        }
        for (Path screenshot : screenshots) {
            BufferedImage image = ImageIO.read(screenshot.toFile());
            if (image == null) throw new IOException("Unreadable Play Store screenshot: " + screenshot);
            int width = image.getWidth();
            int height = image.getHeight();
            if (width < 320 || width > 3840 || height < 320 || height > 3840) {
                throw new IOException(screenshot + " has a side outside the allowed 320–3840 px range");
            }
            boolean portrait = width * 16L == height * 9L;
            boolean landscape = width * 9L == height * 16L;
            if (!portrait && !landscape) {
                throw new IOException(screenshot + " must use a 9:16 or 16:9 aspect ratio but is " + width + "x" + height);
            }
            if (Files.size(screenshot) > 8L * 1024 * 1024) {
                throw new IOException(screenshot + " exceeds the Play Store 8 MB limit");
            }
        }
        return screenshots.size();
    }

    private static boolean isPlayStoreImage(Path path) {
        String name = path.getFileName().toString().toLowerCase(Locale.ROOT);
        return name.endsWith(".png") || name.endsWith(".jpg") || name.endsWith(".jpeg");
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

    private static BufferedImage renderFeatureGraphic(BufferedImage icon) {
        BufferedImage image = new BufferedImage(FEATURE_WIDTH, FEATURE_HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        graphics.setColor(BACKGROUND);
        graphics.fillRect(0, 0, FEATURE_WIDTH, FEATURE_HEIGHT);

        // Restrained depth that remains legible behind Play Store overlays and crops.
        graphics.setColor(new Color(0x12, 0x12, 0x13));
        graphics.fillOval(-244, -264, 560, 560);
        graphics.fillOval(744, 204, 560, 560);
        graphics.setColor(new Color(0x16, 0x16, 0x17));
        graphics.fillOval(824, -264, 430, 430);
        graphics.fillOval(-214, 324, 390, 390);
        graphics.setColor(new Color(0x2D, 0x2D, 0x2F));
        graphics.setStroke(new BasicStroke(2f));
        graphics.drawRoundRect(72, 52, 880, 396, 64, 64);
        graphics.drawLine(126, 250, 898, 250);
        graphics.fillOval(122, 246, 8, 8);
        graphics.fillOval(894, 246, 8, 8);

        // Crop only the safe central field of the exact Play icon. This enlarges the
        // shipping mark without reinterpreting its geometry or requiring system fonts.
        graphics.drawImage(icon, 302, 20, 722, 480, 76, 76, 436, 436, null);
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
