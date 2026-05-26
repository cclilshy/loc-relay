package com.cclilshy.locrelay.event.runtime;

import com.cclilshy.locrelay.event.domain.EventListenerLogFormatter;
import com.cclilshy.locrelay.event.domain.AndroidEventInfo;
import com.cclilshy.locrelay.webhook.runtime.WebhookDispatcher;
import com.cclilshy.locrelay.gateway.data.GatewayLogStore;
import com.cclilshy.locrelay.gateway.data.GatewayPrefs;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.provider.Telephony;
import android.telephony.SmsMessage;
import android.telephony.TelephonyManager;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class EventListenerReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || !isActive(context)) {
            return;
        }
        String action = intent.getAction();
        SharedPreferences prefs = GatewayPrefs.get(context);
        if (TelephonyManager.ACTION_PHONE_STATE_CHANGED.equals(action)) {
            if (!prefs.getBoolean(GatewayPrefs.KEY_EVENT_CALL_ENABLED, false)) {
                return;
            }
            String state = intent.getStringExtra(TelephonyManager.EXTRA_STATE);
            String incomingNumber = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER);
            String payload = EventListenerLogFormatter.callState(
                    state,
                    incomingNumber,
                    AndroidEventInfo.bundleToMap(intent.getExtras()));
            GatewayLogStore.append(
                    prefs,
                    GatewayLogStore.KEY_EVENT_LISTENER_LOG,
                    payload,
                    "events");
            WebhookDispatcher.dispatch(context, "call", payload);
            return;
        }
        if (Telephony.Sms.Intents.SMS_RECEIVED_ACTION.equals(action)) {
            if (!prefs.getBoolean(GatewayPrefs.KEY_EVENT_SMS_ENABLED, false)) {
                return;
            }
            SmsMessage[] messages = Telephony.Sms.Intents.getMessagesFromIntent(intent);
            String payload = EventListenerLogFormatter.smsReceived(
                    toSmsInfo(messages),
                    intent.getStringExtra("format"),
                    AndroidEventInfo.bundleToMap(intent.getExtras()));
            GatewayLogStore.append(
                    prefs,
                    GatewayLogStore.KEY_EVENT_LISTENER_LOG,
                    payload,
                    "events");
            WebhookDispatcher.dispatch(context, "sms", payload);
        }
    }

    private boolean isActive(Context context) {
        SharedPreferences prefs = GatewayPrefs.get(context);
        return prefs.getBoolean(GatewayPrefs.KEY_RUNNING, false)
                && prefs.getBoolean(GatewayPrefs.KEY_EVENT_LISTENER_ENABLED, false);
    }

    private List<EventListenerLogFormatter.SmsInfo> toSmsInfo(SmsMessage[] messages) {
        List<EventListenerLogFormatter.SmsInfo> info = new ArrayList<>();
        if (messages == null) {
            return info;
        }
        for (SmsMessage message : messages) {
            if (message == null) {
                continue;
            }
            info.add(new EventListenerLogFormatter.SmsInfo(
                    message.getOriginatingAddress(),
                    message.getTimestampMillis(),
                    message.getDisplayMessageBody(),
                    metadataFor(message)));
        }
        return info;
    }

    private Map<String, Object> metadataFor(SmsMessage message) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        putText(metadata, "displayOriginatingAddress", message.getDisplayOriginatingAddress());
        putText(metadata, "serviceCenterAddress", message.getServiceCenterAddress());
        putText(metadata, "messageBody", message.getMessageBody());
        putText(metadata, "pseudoSubject", message.getPseudoSubject());
        putText(metadata, "emailFrom", message.getEmailFrom());
        putText(metadata, "emailBody", message.getEmailBody());
        metadata.put("messageClass", String.valueOf(message.getMessageClass()));
        metadata.put("protocolIdentifier", message.getProtocolIdentifier());
        metadata.put("status", message.getStatus());
        metadata.put("indexOnIcc", message.getIndexOnIcc());
        metadata.put("statusOnIcc", message.getStatusOnIcc());
        metadata.put("email", message.isEmail());
        metadata.put("mwiClear", message.isMWIClearMessage());
        metadata.put("mwiSet", message.isMWISetMessage());
        metadata.put("mwiDontStore", message.isMwiDontStore());
        metadata.put("replyPathPresent", message.isReplyPathPresent());
        metadata.put("replace", message.isReplace());
        return metadata;
    }

    private void putText(Map<String, Object> metadata, String key, String value) {
        if (value != null && !value.trim().isEmpty()) {
            metadata.put(key, value);
        }
    }
}
