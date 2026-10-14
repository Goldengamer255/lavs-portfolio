package lavs.ui;

/** Customer details controllers receive context before their first onShow call. */
public interface CustomerContextAware {
    void setCustomerContext(CustomerScreenContext context);
}
