package com.example.reddit.reddit;

import com.example.reddit.config.CrawloraConfig;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CrawloraRequestPacerTest {

    @Test
    void spacesEveryRequestByTheConfiguredMinimumInterval() {
        CrawloraConfig config = mock(CrawloraConfig.class);
        when(config.minRequestInterval()).thenReturn(12_000L);
        AtomicLong clock = new AtomicLong(-5_000L);
        List<Long> sleeps = new ArrayList<>();
        CrawloraRequestPacer pacer = new CrawloraRequestPacer(
                config,
                clock::get,
                nanoseconds -> {
                    sleeps.add(nanoseconds);
                    clock.addAndGet(nanoseconds);
                });

        pacer.awaitTurn("first");
        pacer.awaitTurn("second");
        pacer.awaitTurn("third");

        assertEquals(List.of(
                        TimeUnit.MILLISECONDS.toNanos(12_000L),
                        TimeUnit.MILLISECONDS.toNanos(12_000L)),
                sleeps);
    }

    @Test
    void disablesPacingWhenTheIntervalIsZero() {
        CrawloraConfig config = mock(CrawloraConfig.class);
        when(config.minRequestInterval()).thenReturn(0L);
        List<Long> sleeps = new ArrayList<>();
        CrawloraRequestPacer pacer = new CrawloraRequestPacer(config, () -> 0L, sleeps::add);

        pacer.awaitTurn("first");
        pacer.awaitTurn("second");

        assertEquals(List.of(), sleeps);
    }
}
