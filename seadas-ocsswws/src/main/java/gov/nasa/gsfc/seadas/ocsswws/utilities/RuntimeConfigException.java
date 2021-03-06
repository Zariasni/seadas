package gov.nasa.gsfc.seadas.ocsswws.utilities;

/**
 * Indicates conditions that a reasonable Ceres application might want to catch.
 *
 * @author  Norman Fomferra
 * @version $Id: RuntimeConfigException.java,v 1.1 2007/03/28 11:39:11 norman Exp $
 */
public class RuntimeConfigException extends Exception {
    /**
     * Constructs a new exception with the specified detail message.  The
     * cause is not initialized, and may subsequently be initialized by
     * a call to {@link #initCause}.
     *
     * @param message the detail message. The detail message is saved for
     *                later retrieval by the {@link #getMessage()} method.
     */
    public RuntimeConfigException(String message) {
        super(message);
    }

    /**
     * Constructs a new exception with the specified detail message and
     * cause.  <p>Note that the detail message associated with
     * <code>cause</code> is <i>not</i> automatically incorporated in
     * this exception's detail message.
     *
     * @param message the detail message (which is saved for later retrieval
     *                by the {@link #getMessage()} method).
     * @param cause   the cause (which is saved for later retrieval by the
     *                {@link #getCause()} method).  (A <tt>null</tt> value is
     *                permitted, and indicates that the cause is nonexistent or
     *                unknown.)
     * @since 1.4
     */
    public RuntimeConfigException(String message, Throwable cause) {
        super(message, cause);
    }
}
