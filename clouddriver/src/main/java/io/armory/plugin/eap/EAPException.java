package io.armory.plugin.eap;

public class EAPException extends RuntimeException {

    public EAPException(String message) {
        super(message);
    }

    public EAPException(String message, Throwable cause) {
        super(message, cause);
    }
}
