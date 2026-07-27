package net.thevalorsmp.storage;

/**
 * Unchecked wrapper for storage failures. Repositories translate {@code SQLException} into this type
 * at their boundary so callers never handle checked JDBC exceptions (CODING_STANDARDS.md section 7).
 */
public final class RepositoryException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * Creates an exception for a storage failure with no underlying exception, such as an
     * unsupported backend detected from JDBC metadata.
     *
     * @param message what was being attempted
     */
    public RepositoryException(String message) {
        super(message);
    }

    /**
     * Creates an exception describing a failed storage operation.
     *
     * @param message what was being attempted
     * @param cause   the underlying storage failure
     */
    public RepositoryException(String message, Throwable cause) {
        super(message, cause);
    }
}
