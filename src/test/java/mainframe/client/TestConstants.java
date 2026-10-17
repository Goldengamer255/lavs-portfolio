package mainframe.client;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public final class TestConstants {
  private static final Properties LOCAL_ENV = loadLocalEnv();

  public static final String BASE_URL = "https://animating-eraser-arrogance.ngrok-free.dev/";
  public static final String USERNAME = required("LAVS_MAINFRAME_USERNAME");
  public static final String PASSWORD = required("LAVS_MAINFRAME_PASSWORD");

  // AI-assisted: read local test credentials from .env, with process variables taking precedence.
  private static Properties loadLocalEnv() {
    var properties = new Properties();
    Path path = Path.of(".env");
    if (Files.isRegularFile(path)) {
      try (var input = Files.newInputStream(path)) {
        properties.load(input);
      } catch (IOException e) {
        throw new IllegalStateException("Could not read " + path.toAbsolutePath(), e);
      }
    }
    return properties;
  }

  private static String required(String name) {
    String value = System.getenv(name);
    if (value == null || value.isBlank()) {
      value = LOCAL_ENV.getProperty(name);
    }
    if (value == null || value.isBlank()) {
      throw new IllegalStateException(
          "Set " + name + " in .env or the environment before running live mainframe tests");
    }
    return value;
  }
}
