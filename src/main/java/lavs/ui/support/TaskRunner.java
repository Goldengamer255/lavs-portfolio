package lavs.ui.support;

import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import javafx.beans.property.BooleanProperty;
import javafx.concurrent.Task;

// AI-assisted: class design and Task pattern suggested by Claude, written by me
/**
 * Runs slow work, like mainframe calls, in the background so the screen doesn't
 * freeze.
 */
public final class TaskRunner {

    private final Executor executor;

    /**
     * Creates a TaskRunner that uses the given executor for all background work.
     */
    public TaskRunner(Executor executor) {
        this.executor = executor;
    }

    /**
     * Runs the work in the background and reports the result back on the UI thread.
     * Must be called from the JavaFX thread.
     *
     * @param work      the slow job to run, e.g. a service call
     * @param busy      set to true while work runs, so screens show a spinner
     * @param onSuccess called with result if the work finished
     * @param onFailure called with error if the work threw an exception
     */
    public <T> void run(Callable<T> work, BooleanProperty busy,
            Consumer<T> onSuccess, Consumer<Throwable> onFailure) {
        Task<T> task = new Task<>() {
            @Override
            protected T call() throws Exception {
                return work.call();
            }
        };

        task.setOnSucceeded(event -> {
            busy.set(false);
            onSuccess.accept(task.getValue());
        });

        task.setOnFailed(event -> {
            busy.set(false);
            onFailure.accept(task.getException());
        });

        busy.set(true);
        executor.execute(task);
    }
}
