package lavs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class AppConfigTest {

    private static final String BASE_URL_PROPERTY = "lavs.baseUrl";
    private static final String ORGANISATION_PROPERTY = "lavs.organisationId";

    @AfterEach
    void clearProperties() {
        System.clearProperty(BASE_URL_PROPERTY);
        System.clearProperty(ORGANISATION_PROPERTY);
    }

    @Test
    void loadsSystemPropertyOverrides() {
        System.setProperty(BASE_URL_PROPERTY, " https://example.test/ ");
        System.setProperty(ORGANISATION_PROPERTY, "42");

        AppConfig config = AppConfig.load();

        assertEquals("https://example.test/", config.baseUrl());
        assertEquals(42, config.organisationId());
    }

    @Test
    void acceptsZeroForTheLoggedInUsersOrganisation() {
        System.setProperty(ORGANISATION_PROPERTY, "0");

        AppConfig config = AppConfig.load();

        assertEquals(0, config.organisationId());
    }

    @Test
    void rejectsNegativeOrganisationId() {
        System.setProperty(ORGANISATION_PROPERTY, "-1");

        assertThrows(IllegalStateException.class, AppConfig::load);
    }

    @Test
    void rejectsNonNumericOrganisationId() {
        System.setProperty(ORGANISATION_PROPERTY, "Group_4");

        assertThrows(IllegalStateException.class, AppConfig::load);
    }
}
