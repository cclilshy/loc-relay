package com.cclilshy.locrelay.common.log;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class BoundedLogBufferTest {
    @Test
    public void keepsOnlyNewestLinesWithinLimit() {
        String log = "";

        log = BoundedLogBuffer.append(log, "one", 2);
        log = BoundedLogBuffer.append(log, "two", 2);
        log = BoundedLogBuffer.append(log, "three", 2);

        assertEquals("two\nthree", log);
    }

    @Test
    public void ignoresBlankLines() {
        assertEquals("one", BoundedLogBuffer.append("one", "  ", 4));
    }
}
