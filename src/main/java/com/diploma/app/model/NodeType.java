package com.diploma.app.model;

public enum NodeType {

    LOCAL("Локальный"),
    REGIONAL("Национальный"),
    NATIONAL("Региональный"),
    SUPPLIER("Поставщик");

    private final String displayName;

    NodeType(String displayName) {
        this.displayName = displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }

    // Shouldn't be like this but oh well
    public String getId() {
        return this.name();
    }
}
