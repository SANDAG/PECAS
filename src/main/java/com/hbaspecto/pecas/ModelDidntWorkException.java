package com.hbaspecto.pecas;

/**
 * Generic exception for a model not working.
 * @author John
 *
 */
public class ModelDidntWorkException extends Exception {

    private static final long serialVersionUID = 3418687713609622614L;

    /**
     * 
     */
    public ModelDidntWorkException() {
        // nothing
    }

    /**
     * @param arg0
     */
    public ModelDidntWorkException(String arg0) {
        super(arg0);
    }

    /**
     * @param arg0
     */
    public ModelDidntWorkException(Throwable arg0) {
        super(arg0);
    }

    /**
     * @param arg0
     * @param arg1
     */
    public ModelDidntWorkException(String arg0, Throwable arg1) {
        super(arg0, arg1);
    }

}
