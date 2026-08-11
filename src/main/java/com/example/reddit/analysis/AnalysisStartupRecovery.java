package com.example.reddit.analysis;

import com.example.reddit.persistence.AnalysisPersistenceService;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import org.jboss.logging.Logger;

@ApplicationScoped
public class AnalysisStartupRecovery {
    private static final Logger LOG = Logger.getLogger(AnalysisStartupRecovery.class);

    private final AnalysisPersistenceService persistence;

    public AnalysisStartupRecovery(AnalysisPersistenceService persistence) {
        this.persistence = persistence;
    }

    void onStart(@Observes StartupEvent ignored) {
        int recovered = persistence.failInterruptedRuns();
        if (recovered > 0) {
            LOG.warnf("Marked interrupted Phase 2 analyses failed count=%d", recovered);
        }
    }
}
