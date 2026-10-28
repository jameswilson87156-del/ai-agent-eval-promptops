package com.promptops.evalconsole.service;

/** A provider transport or protocol failure. Messages are intentionally free of request/response bodies. */
public final class ProviderCallException extends RuntimeException {
    private final String code;
    private final boolean retryable;

    public ProviderCallException(String code, String safeMessage, boolean retryable) {
        super(safeMessage);
        this.code = code;
        this.retryable = retryable;
    }

    public ProviderCallException(String code, String safeMessage, boolean retryable, Throwable cause) {
        super(safeMessage, cause);
        this.code = code;
        this.retryable = retryable;
    }

    public String code() { return code; }

    public boolean retryable() { return retryable; }
}
