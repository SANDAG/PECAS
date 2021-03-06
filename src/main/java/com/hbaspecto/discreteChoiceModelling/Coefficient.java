package com.hbaspecto.discreteChoiceModelling;

/**
 * Represents an arbitrary coefficient in a choice model. The coefficient has
 * two values, an "internal" value and a "transformed" value, related by a
 * transformation, and setting either value changes the other according to the
 * transformation. The transformed value should be normally distributed.
 * Coefficients should not override equals() or hashCode().
 * 
 */
public interface Coefficient
{

    /**
     * Retrieves the coefficient's value.
     * 
     * @return The value.
     */
    double getValue();

    /**
     * Changes the coefficient's value.
     * 
     * @param v
     *            The new value.
     */
    void setValue(double v);

    /**
     * Retrieves the coefficient's transformed value.
     * 
     * @return The transformed value.
     */
    double getTransformedValue();

    /**
     * Sets the coefficient's transformed value.
     * 
     * @param v
     *            The new transformed value.
     */
    void setTransformedValue(double v);

    /**
     * Finds the derivative of the transformed value of the coefficient with
     * respect to its internal value.
     * 
     * @return The derivative at the current value.
     */
    double getTransformationDerivative();

    /**
     * Finds the derivative of the internal value of the coefficient with
     * respect to its transformed value.
     * 
     * @return The derivative at the current value.
     */
    double getInverseTransformationDerivative();

    /**
     * Returns the coefficient's name.
     */
    String getName();
}
