package courseproject.factoryfeud.generation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Indicates that every configured raw candidate failed validation. */
public final class FactoryFeudGenerationException extends Exception {
    private final List<FactoryFeudGenerationAttempt> attempts;

    FactoryFeudGenerationException(String message,
            List<FactoryFeudGenerationAttempt> attempts) {
        super(message);
        this.attempts = Collections.unmodifiableList(new ArrayList<>(attempts));
    }

    public List<FactoryFeudGenerationAttempt> getAttempts() { return attempts; }
}
