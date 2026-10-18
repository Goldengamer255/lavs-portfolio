package lavs.validation;

import java.util.List;

public class LoanRules {

    private static final String NEW = "New";
    private static final String PENDING = "Pending";
    private static final String ACTIVE = "Active";
    private static final String DEFAULTED = "Defaulted";
    private static final String CANCELLED = "Cancelled";

    // AI-assisted: class and method signatures influenced by AI; bodies written by me.
    /**
     * Returns the statuses a loan can move to from its current status.
     *
     * <p>The brief only specifies New to Pending (step 11); the rest of the table is a team
     * decision. Pending to New lets an MMM send an application back for changes (the mainframe
     * allows it). Defaulted and Cancelled are final. The mainframe has no Completed status, so a
     * fully repaid loan stays Active.</p>
     *
     * <ul>
     *   <li>New: Pending, Cancelled</li>
     *   <li>Pending: Active, Cancelled, New</li>
     *   <li>Active: Defaulted</li>
     *   <li>Defaulted, Cancelled, null or unknown: none</li>
     * </ul>
     *
     * @param currentStatus the current status of the loan
     * @return the allowed next statuses, or an empty list if the status is null or unknown
     */
    public List<String> allowedNextStatuses(String currentStatus) {
        if (currentStatus == null) {
            return List.of();
        }

        if (NEW.equals(currentStatus)) {
            return List.of(PENDING, CANCELLED);
        }

        if (PENDING.equals(currentStatus)) {
            return List.of(ACTIVE, CANCELLED, NEW);
        }

        if (ACTIVE.equals(currentStatus)) {
            return List.of(DEFAULTED);
        }

        return List.of();
    }
}
