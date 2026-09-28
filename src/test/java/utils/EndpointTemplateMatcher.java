package utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class EndpointTemplateMatcher {

    private static final String INVENTORY_RESOURCE =
            "/coverage/endpoint-inventory.csv";

    private static final List<EndpointTemplate> TEMPLATES =
            loadTemplates();

    private EndpointTemplateMatcher() {
    }

    /**
     * Matches a real runtime request path to the corresponding
     * OpenAPI endpoint template.
     *
     * Example:
     *
     * GET /v1/documents/abc/versions/xyz
     *
     * becomes:
     *
     * /v1/documents/{id}/versions/{version_id}
     *
     * If no inventory endpoint matches, the original path is returned.
     */
    public static String match(String method, String runtimePath) {

        if (method == null || runtimePath == null) {
            return runtimePath;
        }

        String normalizedMethod = method.trim().toUpperCase();
        String normalizedPath = normalizeRuntimePath(runtimePath);

        return TEMPLATES.stream()
                .filter(template ->
                        template.method().equals(normalizedMethod))
                .filter(template ->
                        matches(template.path(), normalizedPath))
                .sorted(
                        Comparator.comparingInt(
                                (EndpointTemplate template) ->
                                        staticSegmentCount(template.path())
                        ).reversed()
                )
                .map(EndpointTemplate::path)
                .findFirst()
                .orElse(normalizedPath);
    }

    private static boolean matches(
            String templatePath,
            String runtimePath
    ) {

        String[] templateSegments = splitPath(templatePath);
        String[] runtimeSegments = splitPath(runtimePath);

        if (templateSegments.length != runtimeSegments.length) {
            return false;
        }

        for (int i = 0; i < templateSegments.length; i++) {

            String templateSegment = templateSegments[i];
            String runtimeSegment = runtimeSegments[i];

            if (isParameter(templateSegment)) {
                if (runtimeSegment.isBlank()) {
                    return false;
                }

                continue;
            }

            if (!templateSegment.equals(runtimeSegment)) {
                return false;
            }
        }

        return true;
    }

    private static int staticSegmentCount(String path) {

        int count = 0;

        for (String segment : splitPath(path)) {
            if (!isParameter(segment)) {
                count++;
            }
        }

        return count;
    }

    private static boolean isParameter(String segment) {
        return segment.startsWith("{")
                && segment.endsWith("}");
    }

    private static String[] splitPath(String path) {

        String normalized = normalizeRuntimePath(path);

        if (normalized.equals("/")) {
            return new String[0];
        }

        return normalized
                .substring(1)
                .split("/");
    }

    private static String normalizeRuntimePath(String path) {

        String normalized = path.trim();

        int queryIndex = normalized.indexOf('?');

        if (queryIndex >= 0) {
            normalized = normalized.substring(0, queryIndex);
        }

        if (!normalized.startsWith("/")) {
            normalized = "/" + normalized;
        }

        while (normalized.length() > 1
                && normalized.endsWith("/")) {
            normalized =
                    normalized.substring(
                            0,
                            normalized.length() - 1
                    );
        }

        return normalized;
    }

    private static List<EndpointTemplate> loadTemplates() {

        InputStream input =
                EndpointTemplateMatcher.class
                        .getResourceAsStream(INVENTORY_RESOURCE);

        if (input == null) {
            throw new IllegalStateException(
                    "Endpoint inventory resource not found: "
                            + INVENTORY_RESOURCE
            );
        }

        List<EndpointTemplate> templates =
                new ArrayList<>();

        try (
                BufferedReader reader =
                        new BufferedReader(
                                new InputStreamReader(
                                        input,
                                        StandardCharsets.UTF_8
                                )
                        )
        ) {

            String line;
            boolean header = true;

            while ((line = reader.readLine()) != null) {

                if (header) {
                    header = false;
                    continue;
                }

                if (line.isBlank()) {
                    continue;
                }

                String[] columns = parseCsvLine(line);

                if (columns.length < 3) {
                    continue;
                }

                String method = columns[1].trim().toUpperCase();
                String path = columns[2].trim();

                if (method.isBlank() || path.isBlank()) {
                    continue;
                }

                templates.add(
                        new EndpointTemplate(method, path)
                );
            }

        } catch (IOException e) {
            throw new IllegalStateException(
                    "Endpoint inventory could not be loaded",
                    e
            );
        }

        return List.copyOf(templates);
    }

    private static String[] parseCsvLine(String line) {

        List<String> values = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean quoted = false;

        for (int i = 0; i < line.length(); i++) {

            char character = line.charAt(i);

            if (character == '"') {

                if (quoted
                        && i + 1 < line.length()
                        && line.charAt(i + 1) == '"') {

                    current.append('"');
                    i++;

                } else {
                    quoted = !quoted;
                }

                continue;
            }

            if (character == ',' && !quoted) {
                values.add(current.toString());
                current.setLength(0);
                continue;
            }

            current.append(character);
        }

        values.add(current.toString());

        return values.toArray(new String[0]);
    }

    private record EndpointTemplate(
            String method,
            String path
    ) {
    }
}