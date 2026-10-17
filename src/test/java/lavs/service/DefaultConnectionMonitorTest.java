package lavs.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.Delayed;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import lavs.gateway.OperationResult;
import mainframe.client.Connection;
import mainframe.client.MessageRequest;
import mainframe.client.MessageResponse;
import mainframe.client.Status;
import org.junit.jupiter.api.Test;

/** Tests connection monitoring without timers, threads, or network access. */
class DefaultConnectionMonitorTest {

    @Test
    void checkNowPublishesOnlyStateChanges() {
        TestConnection connection = new TestConnection();
        RecordingScheduler worker = new RecordingScheduler();
        DefaultConnectionMonitor monitor =
                new DefaultConnectionMonitor(connection, worker, Duration.ofSeconds(1));
        List<Boolean> observed = new ArrayList<>();
        monitor.subscribe(observed::add);

        connection.connected = true;
        OperationResult<Boolean> onlineResult = monitor.checkNow();
        OperationResult<Boolean> repeatedResult = monitor.checkNow();
        connection.connected = false;
        OperationResult<Boolean> offlineResult = monitor.checkNow();

        assertTrue(onlineResult.isSuccess());
        assertEquals(Boolean.TRUE, onlineResult.value());
        assertEquals(Boolean.TRUE, repeatedResult.value());
        assertEquals(Boolean.FALSE, offlineResult.value());
        assertFalse(monitor.isOnline());
        assertEquals(List.of(false, true, false), observed);
    }

    @Test
    void closedSubscriptionStopsNotifications() {
        TestConnection connection = new TestConnection();
        DefaultConnectionMonitor monitor = new DefaultConnectionMonitor(
                connection, new RecordingScheduler(), Duration.ofSeconds(1));
        List<Boolean> observed = new ArrayList<>();
        ConnectionMonitor.Subscription subscription = monitor.subscribe(observed::add);

        subscription.close();
        subscription.close();
        connection.connected = true;
        monitor.checkNow();

        assertEquals(List.of(false), observed);
    }

    @Test
    void startSchedulesOnlyOnePeriodicProbe() {
        TestConnection connection = new TestConnection();
        RecordingScheduler worker = new RecordingScheduler();
        DefaultConnectionMonitor monitor =
                new DefaultConnectionMonitor(connection, worker, Duration.ofSeconds(2));

        monitor.start();
        monitor.start();
        worker.runProbe();

        assertEquals(1, worker.scheduleCount);
        assertEquals(0, worker.initialDelay);
        assertEquals(2_000, worker.delay);
        assertEquals(TimeUnit.MILLISECONDS, worker.unit);
        assertEquals(1, connection.checkCount.get());
    }

    @Test
    void closeCancelsScheduleWithoutShuttingDownSharedWorker() {
        RecordingScheduler worker = new RecordingScheduler();
        DefaultConnectionMonitor monitor = new DefaultConnectionMonitor(
                new TestConnection(), worker, Duration.ofSeconds(1));
        monitor.start();

        monitor.close();
        monitor.close();

        assertTrue(worker.future.cancelled);
        assertFalse(worker.isShutdown());
        assertTrue(monitor.checkNow().isUnavailable());
        assertThrows(IllegalStateException.class, monitor::start);
        assertThrows(IllegalStateException.class, () -> monitor.subscribe(ignored -> {}));
    }

    @Test
    void unexpectedConnectionFailureIsReportedAsUnavailable() {
        TestConnection connection = new TestConnection();
        connection.failure = new IllegalStateException("Ping failed");
        DefaultConnectionMonitor monitor = new DefaultConnectionMonitor(
                connection, new RecordingScheduler(), Duration.ofSeconds(1));

        OperationResult<Boolean> result = monitor.checkNow();

        assertTrue(result.isUnavailable());
        assertEquals("Ping failed", result.errorMessage());
        assertFalse(monitor.isOnline());
    }

    @Test
    void validatesConstructorArgumentsAndIntervals() {
        RecordingScheduler worker = new RecordingScheduler();
        TestConnection connection = new TestConnection();
        assertThrows(NullPointerException.class,
                () -> new DefaultConnectionMonitor(null, worker, Duration.ofSeconds(1)));
        assertThrows(NullPointerException.class,
                () -> new DefaultConnectionMonitor(connection, null, Duration.ofSeconds(1)));
        assertThrows(NullPointerException.class,
                () -> new DefaultConnectionMonitor(connection, worker, null));
        assertThrows(IllegalArgumentException.class,
                () -> new DefaultConnectionMonitor(connection, worker, Duration.ZERO));
        assertThrows(IllegalArgumentException.class,
                () -> new DefaultConnectionMonitor(connection, worker, Duration.ofSeconds(-1)));
        assertThrows(IllegalArgumentException.class,
                () -> new DefaultConnectionMonitor(connection, worker, Duration.ofNanos(1)));
        assertThrows(NullPointerException.class,
                () -> new DefaultConnectionMonitor(connection, worker).subscribe(null));
    }

    @Test
    void closeBeforeStartAndFailingListenersAreSafe() {
        DefaultConnectionMonitor closed = new DefaultConnectionMonitor(
                new TestConnection(), new RecordingScheduler(), Duration.ofSeconds(1));
        closed.close();
        closed.close();

        TestConnection connection = new TestConnection();
        DefaultConnectionMonitor monitor = new DefaultConnectionMonitor(
                connection, new RecordingScheduler(), Duration.ofSeconds(1));
        List<Boolean> observed = new ArrayList<>();
        monitor.subscribe(ignored -> { throw new IllegalStateException("UI closed"); });
        monitor.subscribe(observed::add);
        connection.connected = true;
        monitor.checkNow();
        assertEquals(List.of(false, true), observed);
    }

    @Test
    void failureWithoutMessageUsesDefaultText() {
        TestConnection connection = new TestConnection();
        connection.failure = new IllegalStateException(" ");
        DefaultConnectionMonitor monitor = new DefaultConnectionMonitor(
                connection, new RecordingScheduler(), Duration.ofSeconds(1));
        assertEquals("Mainframe connection check failed.", monitor.checkNow().errorMessage());
    }

    private static final class TestConnection implements Connection {
        private final AtomicInteger checkCount = new AtomicInteger();
        private boolean connected;
        private RuntimeException failure;

        @Override
        public boolean isConnected() {
            checkCount.incrementAndGet();
            if (failure != null) {
                throw failure;
            }
            return connected;
        }

        @Override
        public MessageResponse send(MessageRequest request) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Status Login(String username, String password) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void Logout() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void close() throws IOException {}
    }

    /** Manual scheduler that records the periodic task for deterministic execution. */
    private static final class RecordingScheduler extends AbstractExecutorService
            implements ScheduledExecutorService {
        private final TestScheduledFuture future = new TestScheduledFuture();
        private Runnable probe;
        private int scheduleCount;
        private long initialDelay;
        private long delay;
        private TimeUnit unit;
        private boolean shutdown;

        void runProbe() {
            probe.run();
        }

        @Override
        public ScheduledFuture<?> scheduleWithFixedDelay(Runnable command,
                long initialDelay, long delay, TimeUnit unit) {
            scheduleCount++;
            probe = command;
            this.initialDelay = initialDelay;
            this.delay = delay;
            this.unit = unit;
            return future;
        }

        @Override
        public void shutdown() {
            shutdown = true;
        }

        @Override
        public List<Runnable> shutdownNow() {
            shutdown = true;
            return List.of();
        }

        @Override
        public boolean isShutdown() {
            return shutdown;
        }

        @Override
        public boolean isTerminated() {
            return shutdown;
        }

        @Override
        public boolean awaitTermination(long timeout, TimeUnit unit) {
            return shutdown;
        }

        @Override
        public void execute(Runnable command) {
            command.run();
        }

        @Override
        public <V> ScheduledFuture<V> schedule(
                java.util.concurrent.Callable<V> callable, long delay, TimeUnit unit) {
            throw new UnsupportedOperationException();
        }

        @Override
        public ScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit) {
            throw new UnsupportedOperationException();
        }

        @Override
        public ScheduledFuture<?> scheduleAtFixedRate(
                Runnable command, long initialDelay, long period, TimeUnit unit) {
            throw new UnsupportedOperationException();
        }
    }

    private static final class TestScheduledFuture implements ScheduledFuture<Object> {
        private boolean cancelled;

        @Override
        public boolean cancel(boolean mayInterruptIfRunning) {
            cancelled = true;
            return true;
        }

        @Override
        public boolean isCancelled() {
            return cancelled;
        }

        @Override
        public boolean isDone() {
            return cancelled;
        }

        @Override
        public Object get() {
            throw new UnsupportedOperationException();
        }

        @Override
        public Object get(long timeout, TimeUnit unit) {
            throw new UnsupportedOperationException();
        }

        @Override
        public long getDelay(TimeUnit unit) {
            return 0;
        }

        @Override
        public int compareTo(Delayed other) {
            return 0;
        }
    }
}
