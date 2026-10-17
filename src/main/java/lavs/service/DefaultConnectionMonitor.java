package lavs.service;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import lavs.gateway.OperationResult;
import mainframe.client.Connection;

/**
 * Periodically checks the mainframe connection and publishes online-state changes.
 *
 * <p>Probes run on the application-wide scheduled executor. The same single worker
 * is also used for service calls, ensuring the shared {@link Connection} is never
 * accessed concurrently.</p>
 */
public final class DefaultConnectionMonitor implements ConnectionMonitor {

    private static final Duration DEFAULT_INTERVAL = Duration.ofSeconds(5);
    private static final int UNAVAILABLE = 10;

    private final Connection connection;
    private final ScheduledExecutorService worker;
    private final long intervalMillis;
    private final CopyOnWriteArrayList<Consumer<Boolean>> listeners =
            new CopyOnWriteArrayList<>();
    private final AtomicBoolean started = new AtomicBoolean(false);
    private final AtomicBoolean closed = new AtomicBoolean(false);

    private volatile boolean online;
    private volatile ScheduledFuture<?> scheduledProbe;

    /**
     * Creates a monitor that checks the mainframe every five seconds.
     *
     * @param connection shared mainframe connection
     * @param worker shared single-thread scheduled executor
     */
    public DefaultConnectionMonitor(Connection connection, ScheduledExecutorService worker) {
        this(connection, worker, DEFAULT_INTERVAL);
    }

    /** Package-visible constructor allowing deterministic intervals in tests. */
    DefaultConnectionMonitor(Connection connection, ScheduledExecutorService worker,
            Duration interval) {
        this.connection = Objects.requireNonNull(connection, "connection");
        this.worker = Objects.requireNonNull(worker, "worker");
        Objects.requireNonNull(interval, "interval");
        if (interval.isZero() || interval.isNegative()) {
            throw new IllegalArgumentException("interval must be positive");
        }
        intervalMillis = interval.toMillis();
        if (intervalMillis == 0) {
            throw new IllegalArgumentException("interval must be at least one millisecond");
        }
    }

    /** Starts one immediate probe followed by fixed-delay periodic probes. */
    @Override
    public void start() {
        if (closed.get()) {
            throw new IllegalStateException("Connection monitor is closed");
        }
        if (started.compareAndSet(false, true)) {
            scheduledProbe = worker.scheduleWithFixedDelay(
                    this::runScheduledProbe, 0, intervalMillis, TimeUnit.MILLISECONDS);
        }
    }

    @Override
    public boolean isOnline() {
        return online;
    }

    /**
     * Performs one synchronous probe. Callers should invoke this method through the
     * shared worker when initiating an additional check.
     */
    @Override
    public OperationResult<Boolean> checkNow() {
        if (closed.get()) {
            return OperationResult.failure(UNAVAILABLE, "Connection monitor is closed.", 0);
        }

        try {
            boolean connected = connection.isConnected();
            updateState(connected);
            return OperationResult.success(connected, 0);
        } catch (RuntimeException exception) {
            updateState(false);
            return OperationResult.failure(UNAVAILABLE, failureMessage(exception), 0);
        }
    }

    /**
     * Registers a listener and immediately supplies the last known state.
     * Closing the returned subscription is idempotent.
     */
    @Override
    public Subscription subscribe(Consumer<Boolean> listener) {
        Objects.requireNonNull(listener, "listener");
        if (closed.get()) {
            throw new IllegalStateException("Connection monitor is closed");
        }

        listeners.add(listener);
        notifyListener(listener, online);
        AtomicBoolean subscribed = new AtomicBoolean(true);
        return () -> {
            if (subscribed.compareAndSet(true, false)) {
                listeners.remove(listener);
            }
        };
    }

    /** Cancels future probes and releases listener references without closing the worker. */
    @Override
    public void close() {
        if (!closed.compareAndSet(false, true)) {
            return;
        }
        ScheduledFuture<?> probe = scheduledProbe;
        if (probe != null) {
            probe.cancel(false);
        }
        listeners.clear();
    }

    private void runScheduledProbe() {
        // checkNow converts runtime failures to OperationResult so an unexpected
        // connection implementation cannot cancel future scheduled executions.
        checkNow();
    }

    private void updateState(boolean connected) {
        boolean changed = online != connected;
        online = connected;
        if (changed) {
            for (Consumer<Boolean> listener : listeners) {
                notifyListener(listener, connected);
            }
        }
    }

    private void notifyListener(Consumer<Boolean> listener, boolean connected) {
        try {
            listener.accept(connected);
        } catch (RuntimeException ignored) {
            // One UI subscriber must not stop monitoring or prevent other subscribers.
        }
    }

    private String failureMessage(RuntimeException exception) {
        String message = exception.getMessage();
        return message == null || message.isBlank()
                ? "Mainframe connection check failed."
                : message;
    }
}
