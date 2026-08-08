package com.example.reddit.reddit;

import com.example.reddit.config.CrawloraConfig;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.util.concurrent.TimeUnit;
import java.util.function.LongConsumer;
import java.util.function.LongSupplier;

@ApplicationScoped
public class CrawloraRequestPacer {
    private static final Logger LOG = Logger.getLogger(CrawloraRequestPacer.class);

    private final CrawloraConfig config;
    private final LongSupplier nanoTime;
    private final LongConsumer sleeper;
    private long nextRequestAtNanos;
    private boolean requestScheduled;

    @Inject
    public CrawloraRequestPacer(CrawloraConfig config) {
        this(config, System::nanoTime, CrawloraRequestPacer::sleep);
    }

    CrawloraRequestPacer(
            CrawloraConfig config,
            LongSupplier nanoTime,
            LongConsumer sleeper) {
        this.config = config;
        this.nanoTime = nanoTime;
        this.sleeper = sleeper;
    }

    public synchronized void awaitTurn(String operation) {
        long intervalNanos = TimeUnit.MILLISECONDS.toNanos(
                Math.max(0L, config.minRequestInterval()));
        if (intervalNanos == 0L) {
            return;
        }

        long now = nanoTime.getAsLong();
        if (!requestScheduled) {
            requestScheduled = true;
            nextRequestAtNanos = now + intervalNanos;
            return;
        }

        long waitNanos = nextRequestAtNanos - now;
        if (waitNanos > 0L) {
            LOG.infof("Crawlora request pacing operation=\"%s\" waitMs=%d",
                    operation, TimeUnit.NANOSECONDS.toMillis(waitNanos));
            sleeper.accept(waitNanos);
        }
        nextRequestAtNanos = Math.max(nextRequestAtNanos, nanoTime.getAsLong()) + intervalNanos;
    }

    private static void sleep(long nanoseconds) {
        try {
            long milliseconds = TimeUnit.NANOSECONDS.toMillis(nanoseconds);
            int remainingNanoseconds = (int) (nanoseconds
                    - TimeUnit.MILLISECONDS.toNanos(milliseconds));
            Thread.sleep(milliseconds, remainingNanoseconds);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new RedditClientException("Interrupted while pacing Crawlora requests", exception);
        }
    }
}
