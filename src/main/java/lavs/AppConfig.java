package lavs;

/**
 * Runtime configuration required to connect the desktop client to the mainframe.
 *
 * <p>The base URL and organisation ID can be supplied with system properties or
 * environment variables. System properties take precedence, which makes it convenient
 * to override configuration for an individual Maven run without changing the
 * machine-wide environment.</p>
 *
 * @param baseUrl root URL of the mainframe API
 * @param organisationId organisation assigned to newly created customers
 */
public record AppConfig(String baseUrl, long organisationId) {

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
        String configuredUrl = firstNonBlank(propertyValue, environmentValue, DEFAULT_BASE_URL);

        String configuredOrganisation = firstNonBlank(
                System.getProperty("lavs.organisationId"),
                System.getenv("LAVS_ORGANISATION_ID"));
        long organisationId = parsePositiveLong(configuredOrganisation, "organisation ID");
        return new AppConfig(configuredUrl, organisationId);
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        throw new IllegalStateException(
                "No organisation ID is configured. Set lavs.organisationId "
                        + "or LAVS_ORGANISATION_ID.");
    }

    private static long parsePositiveLong(String value, String label) {
        try {
            long parsed = Long.parseLong(value);
            if (parsed <= 0) {
                throw new NumberFormatException();
            }
            return parsed;
        } catch (NumberFormatException exception) {
            throw new IllegalStateException(
                    "The configured " + label + " must be a positive whole number.", exception);
        }
    }
}
