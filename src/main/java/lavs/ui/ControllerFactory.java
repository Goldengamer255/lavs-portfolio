package lavs.ui;

import java.lang.reflect.InvocationTargetException;
import java.util.Objects;
import java.util.concurrent.Executor;
import javafx.util.Callback;
import lavs.service.AuthenticationService;
import lavs.service.CategoryService;
import lavs.service.CustomerService;
import lavs.ui.support.EntityListController;

/**
 * Central construction point for controllers created by {@link javafx.fxml.FXMLLoader}.
 *
 * <p>Controllers that coordinate application behavior receive their dependencies
 * explicitly. This keeps service lookup out of controllers and ensures every screen
 * uses the same navigator and background worker.</p>
 */
// AI-assisted: controller construction map reviewed and adapted for the LAVS shell.
public final class ControllerFactory implements Callback<Class<?>, Object> {

    private final AuthenticationService authentication;
    private final CustomerService customers;
    private final CategoryService categories;
    private final Navigator navigator;
    private final Executor worker;

    /**
     * Creates a factory backed by the application-scoped dependencies.
     *
     * @param authentication authentication and current-session operations
     * @param customers customer queries and commands
     * @param categories category values used by form controls
     * @param navigator shared screen navigator
     * @param worker executor used for mainframe calls away from the JavaFX thread
     */
    public ControllerFactory(AuthenticationService authentication, CustomerService customers,
            CategoryService categories, Navigator navigator, Executor worker) {
        this.authentication = Objects.requireNonNull(authentication, "authentication");
        this.customers = Objects.requireNonNull(customers, "customers");
        this.categories = Objects.requireNonNull(categories, "categories");
        this.navigator = Objects.requireNonNull(navigator, "navigator");
        this.worker = Objects.requireNonNull(worker, "worker");
    }

    /**
     * Creates the controller requested by an FXML document.
     *
     * <p>Known application controllers are wired explicitly. Reusable presentation-only
     * controllers may use a no-argument constructor.</p>
     *
     * @param controllerType controller class declared by the FXML document
     * @return initialized controller instance
     * @throws IllegalArgumentException if no supported construction rule exists
     */
    @Override
    public Object call(Class<?> controllerType) {
        Objects.requireNonNull(controllerType, "controllerType");
        if (controllerType == LoginController.class) {
            return new LoginController(authentication, navigator, worker);
        }
        if (controllerType == CustomerSearchController.class) {
            return new CustomerSearchController(customers, navigator, worker);
        }
        if (controllerType == CustomerDetailsController.class) {
            return new CustomerDetailsController(customers, categories, navigator, worker);
        }
        if (controllerType == EntityListController.class) {
            return new EntityListController<>();
        }

        return createNoArgumentController(controllerType);
    }

    /** Creates simple reusable controllers that do not require application services. */
    private Object createNoArgumentController(Class<?> controllerType) {
        try {
            return controllerType.getDeclaredConstructor().newInstance();
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException
                | NoSuchMethodException exception) {
            throw new IllegalArgumentException(
                    "No controller construction rule for " + controllerType.getName(), exception);
        }
    }
}
