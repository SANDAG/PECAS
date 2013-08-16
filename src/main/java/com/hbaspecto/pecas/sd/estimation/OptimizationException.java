package com.hbaspecto.pecas.sd.estimation;

/**
 * Thrown to indicate that a problem has occurred during an optimization.
 * 
 * @author Graham
 * 
 */
public class OptimizationException extends Exception {
	private static final long serialVersionUID = 1L;

	public OptimizationException() {
	}

	public OptimizationException(String arg0) {
		super(arg0);
	}

	public OptimizationException(Throwable arg0) {
		super(arg0);
	}

	public OptimizationException(String arg0, Throwable arg1) {
		super(arg0, arg1);
	}

}
