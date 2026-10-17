package lavs;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/**
 * Runtime configuration required to connect the desktop client to the mainframe.
 *
 * <p>The base URL and organisation ID can be supplied with system properties,
 * environment variables, or a local {@code .env} file. System properties take
 * precedence, followed by environment variables and then {@code .env} values.</p>
 *
 * @param baseUrl root URL of the mainframe API
 * @param organisationId organisation requested for newly created customers;
 *        zero lets the server use the logged-in user's organisation
 */
public record AppConfig(String baseUrl, long organisationId) {

    private static final String DEFAULT_BASE_URL =
            "https://animating-eraser-arrogance.ngrok-free.dev/";
    private static final String DEFAULT_ORGANISATION_ID = "0";

    /**
     * Loads configuration from system properties, the process environment, and the
     * local {@code .env} file. The base URL falls back to the course mainframe URL
     * when no override is present.
     *
     * @return resolved application configuration
     */
    public static AppConfig load() {
        Properties localEnv = loadLocalEnv();
        String configuredUrl = firstNonBlank(
                System.getProperty("lavs.baseUrl"),
                System.getenv("LAVS_BASE_URL"),
                localEnv.getProperty("LAVS_BASE_URL"),
                DEFAULT_BASE_URL);

        String configuredOrganisation = firstNonBlank(
                System.getProperty("lavs.organisationId"),
                System.getenv("LAVS_ORGANISATION_ID"),
                localEnv.getProperty("LAVS_ORGANISATION_ID"),
                DEFAULT_ORGANISATION_ID);
        long organisationId = parseNonNegativeLong(configuredOrganisation, "organisation ID");
        return new AppConfig(configuredUrl, organisationId);
    }

    /**
     * Reads optional local configuration from the project-level {@code .env} file.
     */
    private static Properties loadLocalEnv() {
        Properties properties = new Properties();
        Path path = Path.of(".env");

        if (Files.isRegularFile(path)) {
            try (var input = Files.newInputStream(path)) {
                properties.load(input);
            } catch (IOException exception) {
                throw new IllegalStateException(
                        "Could not read " + path.toAbsolutePath(), exception);
            }
        }
        return properties;
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        throw new IllegalStateException("No configuration value is available.");
    }

    private static long parseNonNegativeLong(String value, String label) {
        try {
            long parsed = Long.parseLong(value);
            if (parsed < 0) {
                throw new NumberFormatException();
            }
            return parsed;
        } catch (NumberFormatException exception) {
            throw new IllegalStateException(
                    "The configured " + label + " must be a non-negative whole number.", exception);
        }
    }
}
