package com.cclilshy.locrelay.gateway.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class GatewayExtension {
    public enum FieldType {
        TEXT,
        NUMBER,
        SECRET
    }

    public static final class Field {
        private final String label;
        private final String prefKey;
        private final String defaultValue;
        private final FieldType type;

        public Field(String label, String prefKey, String defaultValue, FieldType type) {
            this.label = label;
            this.prefKey = prefKey;
            this.defaultValue = defaultValue;
            this.type = type;
        }

        public String getLabel() {
            return label;
        }

        public String getPrefKey() {
            return prefKey;
        }

        public String getDefaultValue() {
            return defaultValue;
        }

        public FieldType getType() {
            return type;
        }
    }

    private final String id;
    private final String title;
    private final String description;
    private final String enabledPrefKey;
    private final boolean enabledByDefault;
    private final String badgeLabel;
    private final List<Field> fields;

    public GatewayExtension(
            String id,
            String title,
            String description,
            String enabledPrefKey,
            boolean enabledByDefault,
            String badgeLabel,
            List<Field> fields) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.enabledPrefKey = enabledPrefKey;
        this.enabledByDefault = enabledByDefault;
        this.badgeLabel = badgeLabel == null ? "" : badgeLabel;
        this.fields = Collections.unmodifiableList(new ArrayList<>(fields));
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getEnabledPrefKey() {
        return enabledPrefKey;
    }

    public boolean isEnabledByDefault() {
        return enabledByDefault;
    }

    public String getBadgeLabel() {
        return badgeLabel;
    }

    public List<Field> getFields() {
        return fields;
    }
}
