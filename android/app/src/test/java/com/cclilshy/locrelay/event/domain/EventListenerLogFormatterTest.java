package com.cclilshy.locrelay.event.domain;

import com.cclilshy.locrelay.event.permission.EventListenerPermissionSnapshot;

import static org.junit.Assert.assertEquals;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Collections;

import org.junit.Test;

public class EventListenerLogFormatterTest {
    @Test
    public void formatsPermissionSnapshotWithoutReadingEventContent() {
        EventListenerPermissionSnapshot snapshot = new EventListenerPermissionSnapshot(true, false, true);

        assertEquals(
                "permissions: call=granted sms=missing notifications=granted",
                EventListenerLogFormatter.permissionSnapshot(snapshot));
    }

    @Test
    public void formatsCallEventWithInfoJson() {
        assertEquals(
                "{\"type\":\"call\",\"data\":{\"state\":\"RINGING\",\"incomingNumber\":\"+12025550199\"}}",
                EventListenerLogFormatter.callState("RINGING", "+12025550199"));
    }

    @Test
    public void formatsSmsEventWithInfoJson() {
        assertEquals(
                "{\"type\":\"sms\",\"data\":{\"messageCount\":1,\"messages\":[{\"sender\":\"+12025550199\",\"timestampMillis\":1234,\"body\":\"hello \\\"relay\\\"\"}]}}",
                EventListenerLogFormatter.smsReceived(Collections.singletonList(
                        new EventListenerLogFormatter.SmsInfo("+12025550199", 1234L, "hello \"relay\""))));
    }

    @Test
    public void formatsNotificationEventWithInfoJson() {
        Map<String, Object> extras = new LinkedHashMap<>();
        extras.put("android.title", "Sync complete");
        extras.put("android.progress", 100);
        EventListenerLogFormatter.NotificationInfo info = new EventListenerLogFormatter.NotificationInfo(
                "com.example.app",
                42,
                "sync",
                "notification-key",
                5678L,
                "Sync complete",
                "2 files updated",
                extras);

        assertEquals(
                "{\"type\":\"notification\",\"data\":{\"packageName\":\"com.example.app\",\"id\":42,\"tag\":\"sync\",\"key\":\"notification-key\",\"postTimeMillis\":5678,\"title\":\"Sync complete\",\"text\":\"2 files updated\",\"extras\":{\"android.title\":\"Sync complete\",\"android.progress\":100}}}",
                EventListenerLogFormatter.notificationPosted(info));
    }

    @Test
    public void omitsUnavailableOptionalInfoFields() {
        assertEquals(
                "{\"type\":\"call\",\"data\":{\"state\":\"IDLE\"}}",
                EventListenerLogFormatter.callState("IDLE", null));
        assertEquals(
                "{\"type\":\"sms\",\"data\":{\"messageCount\":0}}",
                EventListenerLogFormatter.smsReceived(Collections.<EventListenerLogFormatter.SmsInfo>emptyList()));
    }
}
