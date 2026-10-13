package lavs.ui.support;

import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.concurrent.RejectedExecutionException;
import javafx.beans.property.BooleanProperty;
import javafx.concurrent.Task;

// AI-assisted: class design and Task pattern suggested by Claude, written as code and implemented by me
/**
 * Runs slow work, like mainframe calls, in the background so the screen doesn't
 * freeze.
 */
public final class TaskRunner {

    private final Executor executor;
    private final Map<BooleanProperty, Integer> running = new IdentityHashMap<>();

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
     * @param busy      true if any work using it is running, screens show spinner
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
            markFinished(busy);
            onSuccess.accept(task.getValue());
        });

        task.setOnFailed(event -> {
            markFinished(busy);
            onFailure.accept(task.getException());
        });

        markStarted(busy);
        try {
            executor.execute(task);
        } catch (RejectedExecutionException e) {
            markFinished(busy);
            onFailure.accept(e);
        }
    }

    // Counts running tasks per busy property, busy turns off when last finishes.
    // Only used on the JavaFX thread, so no locking is needed.
    private void markStarted(BooleanProperty busy) {
        running.merge(busy, 1, Integer::sum);
        busy.set(true);
    }

    private void markFinished(BooleanProperty busy) {
        int stillRunning = running.merge(busy, -1, Integer::sum);
        if (stillRunning <= 0) {
            running.remove(busy);
            busy.set(false);
        }
    }
}
