package lavs.validation;

import java.util.List;

public class LoanRules {

    private static final String NEW = "New";
    private static final String PENDING = "Pending";
    private static final String ACTIVE = "Active";
    private static final String DEFAULTED = "Defaulted";
    private static final String CANCELLED = "Cancelled";

    // AI-assisted: class and method signatures influenced by AI; bodies and TODO notes written by me.
    // The brief only specifies New → Pending (step 11); the rest of the table is a team decision.
    // Pending → New lets an MMM send an application back for changes (the mainframe allows it).
    // Defaulted and Cancelled are final. The mainframe has no Completed status,
    // so a fully repaid loan stays Active.

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
