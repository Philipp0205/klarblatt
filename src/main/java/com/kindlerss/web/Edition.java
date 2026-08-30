package com.kindlerss.web;

/** View identity retained for the interceptor's request/model contract. */
public enum Edition {

    /** Legacy identity used only when controllers are isolated from the interceptor in tests. */
    STANDARD("standard"),

    /** Klarblatt's accessibility-first reader. */
    ACCESSIBLE("accessible");

    private final String value;

    Edition(String value) {
        this.value = value;
    }

    /** Stable value used by existing model code. */
    public String value() {
        return value;
    }

    public boolean isAccessible() {
        return this == ACCESSIBLE;
    }

}
