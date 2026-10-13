package lavs.ui;

/** For NEW, customerNumber is null; for EXISTING it is the saved customer's number. */
public record CustomerScreenContext(ScreenMode mode, String customerNumber) {}
