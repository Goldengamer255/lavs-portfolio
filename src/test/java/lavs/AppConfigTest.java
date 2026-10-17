package lavs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockStatic;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

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

    @Test
    void loadsValuesFromLocalEnvironmentFile() throws IOException {
        byte[] contents = ("LAVS_BASE_URL=https://local.test/\n"
                + "LAVS_ORGANISATION_ID=17\n").getBytes(StandardCharsets.UTF_8);
        try (MockedStatic<Files> files = mockStatic(Files.class)) {
            files.when(() -> Files.isRegularFile(any(Path.class))).thenReturn(true);
            files.when(() -> Files.newInputStream(any(Path.class)))
                    .thenReturn(new ByteArrayInputStream(contents));

            AppConfig config = AppConfig.load();

            assertEquals("https://local.test/", config.baseUrl());
            assertEquals(17, config.organisationId());
        }
    }

    @Test
    void reportsUnreadableLocalEnvironmentFile() {
        try (MockedStatic<Files> files = mockStatic(Files.class)) {
            files.when(() -> Files.isRegularFile(any(Path.class))).thenReturn(true);
            files.when(() -> Files.newInputStream(any(Path.class)))
                    .thenThrow(new IOException("Permission denied"));

            IllegalStateException error = assertThrows(IllegalStateException.class, AppConfig::load);
            assertEquals("Permission denied", error.getCause().getMessage());
        }
    }
}
