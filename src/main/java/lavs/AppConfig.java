package lavs;

/**
 * Runtime configuration required to connect the desktop client to the mainframe.
 *
 * <p>The base URL can be supplied with the {@code lavs.baseUrl} system property or
 * the {@code LAVS_BASE_URL} environment variable. A system property takes precedence,
 * which makes it convenient to override the URL for an individual Maven run without
 * changing the machine-wide environment.</p>
 *
 * @param baseUrl root URL of the mainframe API
 */
public record AppConfig(String baseUrl) {

    private static final String DEFAULT_BASE_URL =
            "https://animating-eraser-arrogance.ngrok-free.dev/";

    /**
     * Loads configuration from the process environment, falling back to the course
     * mainframe URL when no override is present.
     *
     * @return resolved application configuration
     */
    public static AppConfig load() {
        String propertyValue = System.getProperty("lavs.baseUrl");
        String environmentValue = System.getenv("LAVS_BASE_URL");
        String configured = firstNonBlank(propertyValue, environmentValue, DEFAULT_BASE_URL);
        return new AppConfig(configured);
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        throw new IllegalStateException("No LAVS base URL is configured");
    }
}
