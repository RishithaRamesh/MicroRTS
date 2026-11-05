package courseproject.factoryfeud.generation;

/** Audit record for one raw candidate, including metrics computed before validation. */
public final class FactoryFeudGenerationAttempt {
    private final int attemptNumber;
    private final long attemptSeed;
    private final FactoryFeudMapMetrics metrics;
    private final boolean passedValidation;
    private final String validationFailure;

    FactoryFeudGenerationAttempt(int attemptNumber, long attemptSeed,
            FactoryFeudMapMetrics metrics, boolean passedValidation,
            String validationFailure) {
        this.attemptNumber = attemptNumber;
        this.attemptSeed = attemptSeed;
        this.metrics = metrics;
        this.passedValidation = passedValidation;
        this.validationFailure = validationFailure;
    }

    public int getAttemptNumber() { return attemptNumber; }
    public long getAttemptSeed() { return attemptSeed; }
    public FactoryFeudMapMetrics getMetrics() { return metrics; }
    public boolean passedValidation() { return passedValidation; }
    public String getValidationFailure() { return validationFailure; }
}
