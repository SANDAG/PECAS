package com.hbaspecto.pecas.sd.estimation;

/**
 * The interface for classes that turn a development event into an expected
 * value. Subclasses of <code>EstimationTarget</code> also usually implement
 * <code>ExpectedValue</code>, but some estimation targets depend on several
 * expected values, in which case the subclass of <code>EstimationTarget</code>
 * will have several associated implementations of <code>ExpectedValue</code>. A
 * notable example of this is the <code>SpaceTypeIntensityTarget</code>, whose
 * modelled value is calculated as the sum of the expected FARs divided by the
 * expected number of Build-new alternatives chosen -
 * <code>SpaceTypeIntensityTarget</code> does not implement
 * <code>ExpectedValue</code> itself, but has inner classes that do implement
 * it, one for adding up the total FAR and one for counting the number of
 * Build-new events.
 * 
 * @author Graham
 */
public interface ExpectedValue
{
    /**
     * Checks whether this target is applicable to the current parcel, i.e.
     * whether it is possible even in principle for the parcel to add a
     * contribution to the target value.
     * 
     * @return True if the target is applicable to the current parcel.
     */
    boolean appliesToCurrentParcel();

    /**
     * Finds the expected value of this target based on the expected quantities
     * of added space and new space and the parcel properties for the current
     * parcel.
     * 
     * @param spacetype
     *            The spacetype of the new or added development.
     * @param expectedAddedSpace
     *            The expected space to be added.
     * @param expectedNewSpace
     *            The expected space to be built new.
     * @return The expected value of this target for the current parcel.
     */
    double getModelledTotalNewValueForParcel(int spacetype,
            double expectedAddedSpace, double expectedNewSpace);

    /**
     * Returns the derivative of the expected value of this target with respect
     * to the quantity of space added.
     * 
     * @param spacetype
     *            The spacetype of the new or added development.
     * @param expectedAddedSpace
     *            The expected space to be added.
     * @param expectedNewSpace
     *            The expected space to be built new.
     * @return The derivative of the expected value for the current parcel.
     */
    double getModelledTotalNewDerivativeWRTAddedSpace(int spacetype,
            double expectedAddedSpace, double expectedNewSpace);

    /**
     * Returns the derivative of the expected value of this target with respect
     * to the quantity of new space.
     * 
     * @param spacetype
     *            The spacetype of the new or added development.
     * @param expectedAddedSpace
     *            The expected space to be added.
     * @param expectedNewSpace
     *            The expected space to be built new.
     * @return The derivative of the expected value for the current parcel.
     */
    double getModelledTotalNewDerivativeWRTNewSpace(int spacetype,
            double expectedAddedSpace, double expectedNewSpace);

    /**
     * Sets the modelled value, which should also set the value for any targets
     * that this expected value is a component of.
     * 
     * @param value
     *            The modelled value.
     */
    void setModelledValue(double value);

    /**
     * Sets the derivatives of the modelled value with respect to each
     * coefficient, which should also set the derivatives for any targets that
     * this expected value is a component of.
     * 
     * @param derivatives
     *            The derivatives.
     */
    void setDerivatives(double[] derivatives);
}
