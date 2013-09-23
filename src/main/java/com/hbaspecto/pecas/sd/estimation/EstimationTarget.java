package com.hbaspecto.pecas.sd.estimation;

import java.util.List;
import com.hbaspecto.pecas.land.LandInventory;

public abstract class EstimationTarget
{

    private double        targetValue;
    private LandInventory land;

    protected void setLandInventory(LandInventory l)
    {
        land = l;
    }

    protected LandInventory getLandInventory()
    {
        return land;
    }

    public void setTargetValue(double targetValue)
    {
        this.targetValue = targetValue;
    }

    public double getTargetValue()
    {
        return targetValue;
    }

    /**
     * Returns the modelled value. This should be called after <code>setModelledValue</code> has been invoked on every associated
     * <code>ExpectedValue</code>.
     * 
     * @return The modelled value.
     */
    public abstract double getModelledValue();

    /**
     * Returns the derivative of the modelled value with respect to each coefficient. This should be called after <code>setModelledValue</code> and
     * <code>setDerivative</code> have been called on every associated <code>ExpectedValue</code>.
     * 
     * @return The derivatives.
     */
    public abstract double[] getDerivatives();

    /**
     * Returns the list of expected value calculators whose values this target depends on. Normally, if an <code>EstimationTarget</code> object also
     * implements <ExpectedValue>, its <code>getComponentTargets()</code> method should return a collection containing only the object itself.
     * 
     * @return The list of components.
     */
    public abstract List<ExpectedValue> getAssociatedExpectedValues();

    public abstract String getName();
}
