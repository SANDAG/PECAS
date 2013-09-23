package com.hbaspecto.pecas.sd.estimation;

import java.util.HashMap;
import com.hbaspecto.discreteChoiceModelling.Coefficient;
import com.hbaspecto.pecas.sd.SpaceTypesI;

/**
 * A coefficient that has a single spacetype it applies to. This class provides static methods to retrieve each coefficient type. They all guarantee
 * that they will return the same object every time they are called with the same parameters, meaning that they can be compared by object identity.
 */
public abstract class SpaceTypeCoefficient
        implements Coefficient
{
    private String                                                               name;
    private int                                                                  spacetype;

    // Transition constants.
    public static final String                                                   NO_CHANGE_CONST           = "ncconst";
    public static final String                                                   DEMOLISH_TRANSITION_CONST = "democonst";
    public static final String                                                   DERELICT_TRANSITION_CONST = "drltconst";
    public static final String                                                   RENOVATE_TRANSITION_CONST = "renoconst";
    public static final String                                                   RENOVATE_DERELICT_CONST   = "rendconst";
    public static final String                                                   ADD_TRANSITION_CONST      = "addconst";
    public static final String                                                   NEW_FROM_TRANSITION_CONST = "newfromconst";
    public static final String                                                   NEW_TO_TRANSITION_CONST   = "newtoconst";

    // Dispersion parameters.
    public static final String                                                   NO_CHANGE_DISP            = "topdisp";
    public static final String                                                   CHANGE_OPTIONS_DISP       = "chdisp";
    public static final String                                                   DEMOLISH_DERELICT_DISP    = "dddisp";
    public static final String                                                   RENOVATE_ADD_NEW_DISP     = "randisp";
    public static final String                                                   ADD_NEW_DISP              = "andisp";
    public static final String                                                   NEW_TYPE_DISP             = "typdisp";
    public static final String                                                   INTENSITY_DISP            = "intdisp";

    // Step point.
    public static final String                                                   STEP_POINT                = "step";
    public static final String                                                   BELOW_STEP_POINT_ADJ      = "below";
    public static final String                                                   ABOVE_STEP_POINT_ADJ      = "above";
    public static final String                                                   STEP_POINT_AMOUNT         = "stepamt";

    private static final HashMap<String, HashMap<Integer, SpaceTypeCoefficient>> theCoeffs                 = new HashMap<String, HashMap<Integer, SpaceTypeCoefficient>>();

    protected SpaceTypeCoefficient(String name, int spacetype)
    {
        this.name = name;
        this.spacetype = spacetype;
    }

    @Override
    public String getName()
    {
        return name + "-" + spacetype;
    }

    @Override
    public double getTransformedValue()
    {
        return getValue();
    }

    @Override
    public void setTransformedValue(double v)
    {
        setValue(v);
    }

    @Override
    public double getTransformationDerivative()
    {
        return 1;
    }

    @Override
    public double getInverseTransformationDerivative()
    {
        return 1;
    }

    public int getSpacetype()
    {
        return spacetype;
    }

    @Override
    public String toString()
    {
        return getName();
    }

    // Returns null if the coefficient doesn't exist.
    protected static SpaceTypeCoefficient getExistingCoefficient(String name, int spacetype)
    {
        if (theCoeffs.containsKey(name))
        {
            HashMap<Integer, SpaceTypeCoefficient> spaceTypeCoeffs = theCoeffs.get(name);
            if (spaceTypeCoeffs.containsKey(spacetype)) return spaceTypeCoeffs.get(spacetype);
            else return null;
        } else return null;
    }

    protected static void insertNewCoefficient(String name, int spacetype,
            SpaceTypeCoefficient newCoeff)
    {
        if (theCoeffs.containsKey(name)) theCoeffs.get(name).put(spacetype, newCoeff);
        else
        {
            HashMap<Integer, SpaceTypeCoefficient> spaceTypeCoeffs = new HashMap<Integer, SpaceTypeCoefficient>();
            theCoeffs.put(name, spaceTypeCoeffs);
            spaceTypeCoeffs.put(spacetype, newCoeff);
        }
    }

    /**
     * Returns the no-change transition constant for the given spacetype.
     */
    public static SpaceTypeCoefficient getNoChangeConst(int spacetype)
    {
        SpaceTypeCoefficient coeff = getExistingCoefficient(NO_CHANGE_CONST, spacetype);
        if (coeff == null)
        {
            coeff = new NoChangeConstant(spacetype);
            insertNewCoefficient(NO_CHANGE_CONST, spacetype, coeff);
        }
        return coeff;
    }

    /**
     * Returns the demolish transition constant for the given spacetype.
     */
    public static SpaceTypeCoefficient getDemolishTransitionConst(int spacetype)
    {
        SpaceTypeCoefficient coeff = getExistingCoefficient(DEMOLISH_TRANSITION_CONST, spacetype);
        if (coeff == null)
        {
            coeff = new DemolishTransitionConstant(spacetype);
            insertNewCoefficient(DEMOLISH_TRANSITION_CONST, spacetype, coeff);
        }
        return coeff;
    }

    /**
     * Returns the derelict transition constant for the given spacetype.
     */
    public static SpaceTypeCoefficient getDerelictTransitionConst(int spacetype)
    {
        SpaceTypeCoefficient coeff = getExistingCoefficient(DERELICT_TRANSITION_CONST, spacetype);
        if (coeff == null)
        {
            coeff = new DerelictTransitionConstant(spacetype);
            insertNewCoefficient(DERELICT_TRANSITION_CONST, spacetype, coeff);
        }
        return coeff;
    }

    /**
     * Returns the renovate transition constant for non-derelict space of the given spacetype.
     */
    public static SpaceTypeCoefficient getRenovateTransitionConst(int spacetype)
    {
        SpaceTypeCoefficient coeff = getExistingCoefficient(RENOVATE_TRANSITION_CONST, spacetype);
        if (coeff == null)
        {
            coeff = new RenovateTransitionConstant(spacetype);
            insertNewCoefficient(RENOVATE_TRANSITION_CONST, spacetype, coeff);
        }
        return coeff;
    }

    /**
     * Returns the renovate transition constant for derelict space of the given spacetype.
     */
    public static SpaceTypeCoefficient getRenovateDerelictConst(int spacetype)
    {
        SpaceTypeCoefficient coeff = getExistingCoefficient(RENOVATE_DERELICT_CONST, spacetype);
        if (coeff == null)
        {
            coeff = new RenovateDerelictConstant(spacetype);
            insertNewCoefficient(RENOVATE_DERELICT_CONST, spacetype, coeff);
        }
        return coeff;
    }

    /**
     * Returns the add space transition constant for the given spacetype.
     */
    public static SpaceTypeCoefficient getAddTransitionConst(int spacetype)
    {
        SpaceTypeCoefficient coeff = getExistingCoefficient(ADD_TRANSITION_CONST, spacetype);
        if (coeff == null)
        {
            coeff = new AddTransitionConstant(spacetype);
            insertNewCoefficient(ADD_TRANSITION_CONST, spacetype, coeff);
        }
        return coeff;
    }

    /**
     * Returns the build new from transition constant for the given spacetype, constant for building anything new on a parcel of this type
     */
    public static SpaceTypeCoefficient getNewFromTransitionConst(int spacetype)
    {
        SpaceTypeCoefficient coeff = getExistingCoefficient(NEW_FROM_TRANSITION_CONST, spacetype);
        if (coeff == null)
        {
            coeff = new NewFromTransitionConstant(spacetype);
            insertNewCoefficient(NEW_FROM_TRANSITION_CONST, spacetype, coeff);
        }
        return coeff;
    }

    /**
     * Returns the build new to transition constant for the given spacetype, constant for building this type new on any existing.
     */
    public static SpaceTypeCoefficient getNewToTransitionConst(int spacetype)
    {
        SpaceTypeCoefficient coeff = getExistingCoefficient(NEW_TO_TRANSITION_CONST, spacetype);
        if (coeff == null)
        {
            coeff = new NewToTransitionConstant(spacetype);
            insertNewCoefficient(NEW_TO_TRANSITION_CONST, spacetype, coeff);
        }
        return coeff;
    }

    /**
     * Returns the top-level dispersion parameter for the given spacetype.
     */
    public static SpaceTypeCoefficient getNoChangeDisp(int spacetype)
    {
        return DispersionParameter.getNoChangeDisp(spacetype);
    }

    /**
     * Returns the change options dispersion parameter for the given spacetype.
     */
    public static SpaceTypeCoefficient getChangeOptionsDisp(int spacetype)
    {
        return DispersionParameter.getChangeOptionsDisp(spacetype);
    }

    /**
     * Returns the demolish/derelict dispersion parameter for the given spacetype.
     */
    public static SpaceTypeCoefficient getDemolishDerelictDisp(int spacetype)
    {
        return DispersionParameter.getDemolishDerelictDisp(spacetype);
    }

    /**
     * Returns the renovate/add space/build new dispersion parameter for the given spacetype.
     */
    public static SpaceTypeCoefficient getRenovateAddNewDisp(int spacetype)
    {
        return DispersionParameter.getRenovateAddNewDisp(spacetype);
    }

    /**
     * Returns the add space/build new dispersion parameter for the given spacetype.
     */
    public static SpaceTypeCoefficient getAddNewDisp(int spacetype)
    {
        return DispersionParameter.getAddNewDisp(spacetype);
    }

    /**
     * Returns the new spacetype dispersion parameter for the given spacetype.
     */
    public static SpaceTypeCoefficient getNewTypeDisp(int spacetype)
    {
        return DispersionParameter.getNewTypeDisp(spacetype);
    }

    /**
     * Returns the building intensity dispersion parameter for the given spacetype.
     */
    public static SpaceTypeCoefficient getIntensityDisp(int spacetype)
    {
        return DispersionParameter.getIntensityDisp(spacetype);
    }

    /**
     * Returns the density shaping function's step point for the given spacetype.
     */
    public static SpaceTypeCoefficient getStepPoint(int spacetype)
    {
        SpaceTypeCoefficient coeff = getExistingCoefficient(STEP_POINT, spacetype);
        if (coeff == null)
        {
            coeff = new StepPoint(spacetype);
            insertNewCoefficient(STEP_POINT, spacetype, coeff);
        }
        return coeff;
    }

    /**
     * Returns the density shaping function's adjustment below the step point for the given spacetype.
     */
    public static SpaceTypeCoefficient getBelowStepPointAdj(int spacetype)
    {
        SpaceTypeCoefficient coeff = getExistingCoefficient(BELOW_STEP_POINT_ADJ, spacetype);
        if (coeff == null)
        {
            coeff = new BelowStepPointAdjustment(spacetype);
            insertNewCoefficient(BELOW_STEP_POINT_ADJ, spacetype, coeff);
        }
        return coeff;
    }

    /**
     * Returns the density shaping function's adjustment above the step point for the given spacetype.
     */
    public static SpaceTypeCoefficient getAboveStepPointAdj(int spacetype)
    {
        SpaceTypeCoefficient coeff = getExistingCoefficient(ABOVE_STEP_POINT_ADJ, spacetype);
        if (coeff == null)
        {
            coeff = new AboveStepPointAdjustment(spacetype);
            insertNewCoefficient(ABOVE_STEP_POINT_ADJ, spacetype, coeff);
        }
        return coeff;
    }

    /**
     * Returns the density shaping function's step point size for the given spacetype.
     */
    public static SpaceTypeCoefficient getStepPointAmount(int spacetype)
    {
        SpaceTypeCoefficient coeff = getExistingCoefficient(STEP_POINT_AMOUNT, spacetype);
        if (coeff == null)
        {
            coeff = new StepPointAmount(spacetype);
            insertNewCoefficient(STEP_POINT_AMOUNT, spacetype, coeff);
        }
        return coeff;
    }

    // A subclass for each type of coefficient.

    private static class NoChangeConstant
            extends SpaceTypeCoefficient
    {
        private NoChangeConstant(int spacetype)
        {
            super(NO_CHANGE_CONST, spacetype);
        }

        @Override
        public double getValue()
        {
            return SpaceTypesI.getAlreadyCreatedSpaceTypeBySpaceTypeID(getSpacetype())
                    .get_NoChangeTransitionConst();
        }

        @Override
        public void setValue(double v)
        {
            SpaceTypesI.getAlreadyCreatedSpaceTypeBySpaceTypeID(getSpacetype())
                    .set_NoChangeTransitionConst(v);
        }
    }

    private static class DemolishTransitionConstant
            extends SpaceTypeCoefficient
    {
        private DemolishTransitionConstant(int spacetype)
        {
            super(DEMOLISH_TRANSITION_CONST, spacetype);
        }

        @Override
        public double getValue()
        {
            return SpaceTypesI.getAlreadyCreatedSpaceTypeBySpaceTypeID(getSpacetype())
                    .get_DemolishTransitionConst();
        }

        @Override
        public void setValue(double v)
        {
            SpaceTypesI.getAlreadyCreatedSpaceTypeBySpaceTypeID(getSpacetype())
                    .set_DemolishTransitionConst(v);
        }
    }

    private static class DerelictTransitionConstant
            extends SpaceTypeCoefficient
    {
        private DerelictTransitionConstant(int spacetype)
        {
            super(DERELICT_TRANSITION_CONST, spacetype);
        }

        @Override
        public double getValue()
        {
            return SpaceTypesI.getAlreadyCreatedSpaceTypeBySpaceTypeID(getSpacetype())
                    .get_DerelictTransitionConst();
        }

        @Override
        public void setValue(double v)
        {
            SpaceTypesI.getAlreadyCreatedSpaceTypeBySpaceTypeID(getSpacetype())
                    .set_DerelictTransitionConst(v);
        }
    }

    private static class RenovateTransitionConstant
            extends SpaceTypeCoefficient
    {
        private RenovateTransitionConstant(int spacetype)
        {
            super(RENOVATE_TRANSITION_CONST, spacetype);
        }

        @Override
        public double getValue()
        {
            return SpaceTypesI.getAlreadyCreatedSpaceTypeBySpaceTypeID(getSpacetype())
                    .get_RenovateTransitionConst();
        }

        @Override
        public void setValue(double v)
        {
            SpaceTypesI.getAlreadyCreatedSpaceTypeBySpaceTypeID(getSpacetype())
                    .set_RenovateTransitionConst(v);
        }
    }

    private static class RenovateDerelictConstant
            extends SpaceTypeCoefficient
    {
        private RenovateDerelictConstant(int spacetype)
        {
            super(RENOVATE_DERELICT_CONST, spacetype);
        }

        @Override
        public double getValue()
        {
            return SpaceTypesI.getAlreadyCreatedSpaceTypeBySpaceTypeID(getSpacetype())
                    .get_RenovateDerelictTransitionConst();
        }

        @Override
        public void setValue(double v)
        {
            SpaceTypesI.getAlreadyCreatedSpaceTypeBySpaceTypeID(getSpacetype())
                    .set_RenovateDerelictTransitionConst(v);
        }
    }

    private static class AddTransitionConstant
            extends SpaceTypeCoefficient
    {
        private AddTransitionConstant(int spacetype)
        {
            super(ADD_TRANSITION_CONST, spacetype);
        }

        @Override
        public double getValue()
        {
            return SpaceTypesI.getAlreadyCreatedSpaceTypeBySpaceTypeID(getSpacetype())
                    .get_AddTransitionConst();
        }

        @Override
        public void setValue(double v)
        {
            SpaceTypesI.getAlreadyCreatedSpaceTypeBySpaceTypeID(getSpacetype())
                    .set_AddTransitionConst(v);
        }
    }

    private static class NewFromTransitionConstant
            extends SpaceTypeCoefficient
    {
        private NewFromTransitionConstant(int spacetype)
        {
            super(NEW_FROM_TRANSITION_CONST, spacetype);
        }

        @Override
        public double getValue()
        {
            return SpaceTypesI.getAlreadyCreatedSpaceTypeBySpaceTypeID(getSpacetype())
                    .get_NewFromTransitionConst();
        }

        @Override
        public void setValue(double v)
        {
            SpaceTypesI.getAlreadyCreatedSpaceTypeBySpaceTypeID(getSpacetype())
                    .set_NewFromTransitionConst(v);
        }
    }

    private static class NewToTransitionConstant
            extends SpaceTypeCoefficient
    {
        private NewToTransitionConstant(int spacetype)
        {
            super(NEW_TO_TRANSITION_CONST, spacetype);
        }

        @Override
        public double getValue()
        {
            return SpaceTypesI.getAlreadyCreatedSpaceTypeBySpaceTypeID(getSpacetype())
                    .get_NewToTransitionConst();
        }

        @Override
        public void setValue(double v)
        {
            SpaceTypesI.getAlreadyCreatedSpaceTypeBySpaceTypeID(getSpacetype())
                    .set_NewToTransitionConst(v);
        }
    }

    private static class StepPoint
            extends SpaceTypeCoefficient
    {
        private StepPoint(int spacetype)
        {
            super(STEP_POINT, spacetype);
        }

        @Override
        public double getValue()
        {
            return SpaceTypesI.getAlreadyCreatedSpaceTypeBySpaceTypeID(getSpacetype())
                    .get_StepPoint();
        }

        @Override
        public void setValue(double v)
        {
            SpaceTypesI.getAlreadyCreatedSpaceTypeBySpaceTypeID(getSpacetype()).set_StepPoint(v);
        }
    }

    private static class BelowStepPointAdjustment
            extends SpaceTypeCoefficient
    {
        private BelowStepPointAdjustment(int spacetype)
        {
            super(BELOW_STEP_POINT_ADJ, spacetype);
        }

        @Override
        public double getValue()
        {
            return SpaceTypesI.getAlreadyCreatedSpaceTypeBySpaceTypeID(getSpacetype())
                    .get_BelowStepPointAdjustment();
        }

        @Override
        public void setValue(double v)
        {
            SpaceTypesI.getAlreadyCreatedSpaceTypeBySpaceTypeID(getSpacetype())
                    .set_BelowStepPointAdjustment(v);
        }
    }

    private static class AboveStepPointAdjustment
            extends SpaceTypeCoefficient
    {
        private AboveStepPointAdjustment(int spacetype)
        {
            super(ABOVE_STEP_POINT_ADJ, spacetype);
        }

        @Override
        public double getValue()
        {
            return SpaceTypesI.getAlreadyCreatedSpaceTypeBySpaceTypeID(getSpacetype())
                    .get_AboveStepPointAdjustment();
        }

        @Override
        public void setValue(double v)
        {
            SpaceTypesI.getAlreadyCreatedSpaceTypeBySpaceTypeID(getSpacetype())
                    .set_AboveStepPointAdjustment(v);
        }
    }

    private static class StepPointAmount
            extends SpaceTypeCoefficient
    {
        private StepPointAmount(int spacetype)
        {
            super(STEP_POINT_AMOUNT, spacetype);
        }

        @Override
        public double getValue()
        {
            return SpaceTypesI.getAlreadyCreatedSpaceTypeBySpaceTypeID(getSpacetype())
                    .get_StepPointAdjustment();
        }

        @Override
        public void setValue(double v)
        {
            SpaceTypesI.getAlreadyCreatedSpaceTypeBySpaceTypeID(getSpacetype())
                    .set_StepPointAdjustment(v);
        }
    }
}
