import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

/** Kotlin/Android-aware coverage thresholds over AGP's JaCoCo XML output. */
public final class CoverageGate {
    private record Counter(long missed, long covered) {
        Counter plus(Counter other) {
            return new Counter(missed + other.missed, covered + other.covered);
        }

        double percentage() {
            long total = missed + covered;
            return total == 0 ? 100.0 : covered * 100.0 / total;
        }
    }

    private record Source(String packageName, String fileName, Map<String, Counter> counters) {}

    private record Requirement(String label, Predicate<Source> sources, String counter, double minimum) {}

    public static void main(String[] args) throws Exception {
        if (args.length != 2 || !(args[0].equals("unit") || args[0].equals("e2e"))) {
            throw new IllegalArgumentException("Usage: java scripts/CoverageGate.java unit|e2e report.xml");
        }
        Path report = Path.of(args[1]);
        if (!Files.isRegularFile(report) || Files.size(report) == 0) {
            throw new IllegalStateException("Coverage report is missing or empty: " + report);
        }

        List<Source> sources = readSources(report);
        List<Requirement> requirements = args[0].equals("unit") ? unitRequirements() : e2eRequirements();
        boolean failed = false;
        for (Requirement requirement : requirements) {
            Counter total = aggregate(sources, requirement.sources(), requirement.counter());
            double actual = total.percentage();
            System.out.printf(
                "%-42s %7.2f%% (%d/%d) minimum %.2f%%%n",
                requirement.label(), actual, total.covered(), total.covered() + total.missed(), requirement.minimum()
            );
            if (actual + 0.0001 < requirement.minimum()) failed = true;
        }
        if (failed) throw new IllegalStateException("Coverage fell below an audited non-regression threshold");
    }

    private static List<Requirement> unitRequirements() {
        return List.of(
            new Requirement("Deterministic domain lines", inPackage("com/whip/app/domain"), "LINE", 78.0),
            new Requirement("Deterministic domain branches", inPackage("com/whip/app/domain"), "BRANCH", 51.0),
            new Requirement("Core settings and policy lines", inPackage("com/whip/app/core"), "LINE", 63.0)
        );
    }

    private static List<Requirement> e2eRequirements() {
        Set<String> repositories = Set.of(
            "BackupRepository.kt", "GoalRepository.kt", "GymRepository.kt", "HabitRepository.kt",
            "LinkRepository.kt", "MeasurementRepository.kt", "RoutineRepository.kt", "TaskRepository.kt",
            "TrackRepository.kt"
        );
        Set<String> firstClassScreens = Set.of(
            "WhipApp.kt", "TaskEditorDialog.kt", "TaskComponents.kt", "HabitScreens.kt", "GoalScreens.kt",
            "TrackScreens.kt", "GymScreens.kt", "SettingsScreens.kt"
        );
        Predicate<Source> productCode = source -> !source.fileName().endsWith("_Impl.kt");
        return List.of(
            new Requirement("E2E product-code lines", productCode, "LINE", 64.0),
            new Requirement("E2E product-code branches", productCode, "BRANCH", 37.0),
            new Requirement("First-class repository lines", source -> repositories.contains(source.fileName()), "LINE", 79.5),
            new Requirement("First-class screen lines", source -> firstClassScreens.contains(source.fileName()), "LINE", 59.0),
            new Requirement("Track screen lines", named("TrackScreens.kt"), "LINE", 45.0),
            new Requirement("Gym screen lines", named("GymScreens.kt"), "LINE", 57.0)
        );
    }

    private static Predicate<Source> inPackage(String packageName) {
        return source -> source.packageName().equals(packageName);
    }

    private static Predicate<Source> named(String fileName) {
        return source -> source.fileName().equals(fileName);
    }

    private static Counter aggregate(List<Source> sources, Predicate<Source> filter, String counterName) {
        Counter result = new Counter(0, 0);
        for (Source source : sources) {
            if (filter.test(source)) result = result.plus(source.counters().getOrDefault(counterName, new Counter(0, 0)));
        }
        if (result.missed() + result.covered() == 0) {
            throw new IllegalStateException("Coverage scope was empty for " + counterName);
        }
        return result;
    }

    private static List<Source> readSources(Path report) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        var builder = factory.newDocumentBuilder();
        builder.setEntityResolver((publicId, systemId) -> new InputSource(new java.io.StringReader("")));
        var document = builder.parse(report.toFile());
        List<Source> sources = new ArrayList<>();
        NodeList packages = document.getElementsByTagName("package");
        for (int packageIndex = 0; packageIndex < packages.getLength(); packageIndex++) {
            Element packageElement = (Element) packages.item(packageIndex);
            NodeList children = packageElement.getChildNodes();
            for (int childIndex = 0; childIndex < children.getLength(); childIndex++) {
                Node child = children.item(childIndex);
                if (!(child instanceof Element sourceElement) || !sourceElement.getTagName().equals("sourcefile")) continue;
                Map<String, Counter> counters = new HashMap<>();
                NodeList sourceChildren = sourceElement.getChildNodes();
                for (int counterIndex = 0; counterIndex < sourceChildren.getLength(); counterIndex++) {
                    Node counterNode = sourceChildren.item(counterIndex);
                    if (!(counterNode instanceof Element counterElement) || !counterElement.getTagName().equals("counter")) continue;
                    counters.put(
                        counterElement.getAttribute("type"),
                        new Counter(
                            Long.parseLong(counterElement.getAttribute("missed")),
                            Long.parseLong(counterElement.getAttribute("covered"))
                        )
                    );
                }
                sources.add(new Source(packageElement.getAttribute("name"), sourceElement.getAttribute("name"), counters));
            }
        }
        return sources;
    }
}
