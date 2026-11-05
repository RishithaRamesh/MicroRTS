package courseproject.factoryfeud.generation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import rts.PhysicalGameState;

/** Successful generated map together with configuration, metrics, and attempt audit. */
public final class FactoryFeudGeneratedMap {
    private final PhysicalGameState physicalGameState;
    private final FactoryFeudGeneratorConfig config;
    private final FactoryFeudMapMetrics metrics;
    private final List<FactoryFeudGenerationAttempt> attempts;

    FactoryFeudGeneratedMap(PhysicalGameState physicalGameState,
            FactoryFeudGeneratorConfig config, FactoryFeudMapMetrics metrics,
            List<FactoryFeudGenerationAttempt> attempts) {
        this.physicalGameState = physicalGameState;
        this.config = config;
        this.metrics = metrics;
        this.attempts = Collections.unmodifiableList(new ArrayList<>(attempts));
    }

    public PhysicalGameState getPhysicalGameState() { return physicalGameState; }
    public FactoryFeudGeneratorConfig getConfig() { return config; }
    public FactoryFeudMapMetrics getMetrics() { return metrics; }
    public List<FactoryFeudGenerationAttempt> getAttempts() { return attempts; }
    public int getSuccessfulAttemptNumber() { return attempts.size(); }
}
