package lavs.ui;

/**
 * Optional lifecycle contract for screen controllers.
 */
public interface ScreenLifecycle {

    /**
     * Called by the navigator on the JavaFX application thread each time the
     * screen is shown, including when restored by Back. Navigation parameters
     * and pick mode must be set before this call.
     * Controllers can refresh service-backed data here without discarding
     * unsaved edits, search criteria, or other local screen state.
     * Long-running work must be scheduled off the JavaFX application thread.
     */
    void onShow();
}
