package com.hbaspecto.pecas.sd.estimation;

import com.hbaspecto.pecas.sd.SpaceTypesI;

/**
 * A coefficient representing a dispersion parameter. Dispersion parameters
 * are calibrated assuming a log-normal prior, so they apply a natural
 * logarithm transformation.
 *
 */
public abstract class DispersionParameter extends SpaceTypeCoefficient
{
    protected DispersionParameter(String name, int spacetype)
    {
        super(name, spacetype);
    }
    
    @Override
    public double getTransformedValue()
    {
        return Math.log(getValue());
    }
    
    @Override
    public void setTransformedValue(double v)
    {
        setValue(Math.exp(v));
    }
    
    @Override
    public double getTransformationDerivative()
    {
        return 1 / getValue();
    }
    
    @Override
    public double getInverseTransformationDerivative()
    {
        return getValue();
    }
    
    /**
     * Returns the top-level dispersion parameter for the given spacetype.
     */
    public static SpaceTypeCoefficient getNoChangeDisp(int spacetype)
    {
        SpaceTypeCoefficient coeff = getExistingCoefficient(NO_CHANGE_DISP, spacetype);
        if(coeff == null)
        {
            coeff = new NoChangeDispersion(spacetype);
            insertNewCoefficient(NO_CHANGE_DISP, spacetype, coeff);
        }
        return coeff;
    }
    
    /**
     * Returns the change options dispersion parameter for the given spacetype.
     */
    public static SpaceTypeCoefficient getChangeOptionsDisp(int spacetype)
    {
        SpaceTypeCoefficient coeff = getExistingCoefficient(CHANGE_OPTIONS_DISP, spacetype);
        if(coeff == null)
        {
            coeff = new ChangeOptionsDispersion(spacetype);
            insertNewCoefficient(CHANGE_OPTIONS_DISP, spacetype, coeff);
        }
        return coeff;
    }
    
    /**
     * Returns the demolish/derelict dispersion parameter for the given spacetype.
     */
    public static SpaceTypeCoefficient getDemolishDerelictDisp(int spacetype)
    {
        SpaceTypeCoefficient coeff = getExistingCoefficient(DEMOLISH_DERELICT_DISP, spacetype);
        if(coeff == null)
        {
            coeff = new DemolishDerelictDispersion(spacetype);
            insertNewCoefficient(DEMOLISH_DERELICT_DISP, spacetype, coeff);
        }
        return coeff;
    }
    
    /**
     * Returns the renovate/add space/build new dispersion parameter for the given spacetype.
     */
    public static SpaceTypeCoefficient getRenovateAddNewDisp(int spacetype)
    {
        SpaceTypeCoefficient coeff = getExistingCoefficient(RENOVATE_ADD_NEW_DISP, spacetype);
        if(coeff == null)
        {
            coeff = new RenovateAddNewDispersion(spacetype);
            insertNewCoefficient(RENOVATE_ADD_NEW_DISP, spacetype, coeff);
        }
        return coeff;
    }
    
    /**
     * Returns the add space/build new dispersion parameter for the given spacetype.
     */
    public static SpaceTypeCoefficient getAddNewDisp(int spacetype)
    {
        SpaceTypeCoefficient coeff = getExistingCoefficient(ADD_NEW_DISP, spacetype);
        if(coeff == null)
        {
            coeff = new AddNewDispersion(spacetype);
            insertNewCoefficient(ADD_NEW_DISP, spacetype, coeff);
        }
        return coeff;
    }
    
    /**
     * Returns the new spacetype dispersion parameter for the given spacetype.
     */
    public static SpaceTypeCoefficient getNewTypeDisp(int spacetype)
    {
        SpaceTypeCoefficient coeff = getExistingCoefficient(NEW_TYPE_DISP, spacetype);
        if(coeff == null)
        {
            coeff = new NewTypeDispersion(spacetype);
            insertNewCoefficient(NEW_TYPE_DISP, spacetype, coeff);
        }
        return coeff;
    }
    
    /**
     * Returns the building intensity dispersion parameter for the given spacetype.
     */
    public static SpaceTypeCoefficient getIntensityDisp(int spacetype)
    {
        SpaceTypeCoefficient coeff = getExistingCoefficient(INTENSITY_DISP, spacetype);
        if(coeff == null)
        {
            coeff = new IntensityDispersion(spacetype);
            insertNewCoefficient(INTENSITY_DISP, spacetype, coeff);
        }
        return coeff;
    }
    
    // Types of dispersion parameters.
    
    private static class NoChangeDispersion extends DispersionParameter
    {
        private NoChangeDispersion(int spacetype)
        {
            super(NO_CHANGE_DISP, spacetype);
        }

        @Override
        public double getValue()
        {
            return SpaceTypesI.getAlreadyCreatedSpaceTypeBySpaceTypeID(getSpacetype()).get_NochangeDispersionParameter();
        }

        @Override
        public void setValue(double v)
        {
            SpaceTypesI.getAlreadyCreatedSpaceTypeBySpaceTypeID(getSpacetype()).set_NochangeDispersionParameter(v);
        }
    }
    
    private static class ChangeOptionsDispersion extends DispersionParameter
    {
        private ChangeOptionsDispersion(int spacetype)
        {
            super(CHANGE_OPTIONS_DISP, spacetype);
        }

        @Override
        public double getValue()
        {
            return SpaceTypesI.getAlreadyCreatedSpaceTypeBySpaceTypeID(getSpacetype()).get_GkDispersionParameter();
        }

        @Override
        public void setValue(double v)
        {
            SpaceTypesI.getAlreadyCreatedSpaceTypeBySpaceTypeID(getSpacetype()).set_GkDispersionParameter(v);
        }
    }
    
    private static class DemolishDerelictDispersion extends DispersionParameter
    {
        private DemolishDerelictDispersion(int spacetype)
        {
            super(DEMOLISH_DERELICT_DISP, spacetype);
        }

        @Override
        public double getValue()
        {
            return SpaceTypesI.getAlreadyCreatedSpaceTypeBySpaceTypeID(getSpacetype()).get_GwDispersionParameter();
        }

        @Override
        public void setValue(double v)
        {
            SpaceTypesI.getAlreadyCreatedSpaceTypeBySpaceTypeID(getSpacetype()).set_GwDispersionParameter(v);
        }
    }
    
    private static class RenovateAddNewDispersion extends DispersionParameter
    {
        private RenovateAddNewDispersion(int spacetype)
        {
            super(RENOVATE_ADD_NEW_DISP, spacetype);
        }

        @Override
        public double getValue()
        {
            return SpaceTypesI.getAlreadyCreatedSpaceTypeBySpaceTypeID(getSpacetype()).get_GzDispersionParameter();
        }

        @Override
        public void setValue(double v)
        {
            SpaceTypesI.getAlreadyCreatedSpaceTypeBySpaceTypeID(getSpacetype()).set_GzDispersionParameter(v);
        }
    }
    
    private static class AddNewDispersion extends DispersionParameter
    {
        private AddNewDispersion(int spacetype)
        {
            super(ADD_NEW_DISP, spacetype);
        }

        @Override
        public double getValue()
        {
            return SpaceTypesI.getAlreadyCreatedSpaceTypeBySpaceTypeID(getSpacetype()).get_GyDispersionParameter();
        }

        @Override
        public void setValue(double v)
        {
            SpaceTypesI.getAlreadyCreatedSpaceTypeBySpaceTypeID(getSpacetype()).set_GyDispersionParameter(v);
        }
    }
    
    private static class NewTypeDispersion extends DispersionParameter
    {
        private NewTypeDispersion(int spacetype)
        {
            super(NEW_TYPE_DISP, spacetype);
        }

        @Override
        public double getValue()
        {
            return SpaceTypesI.getAlreadyCreatedSpaceTypeBySpaceTypeID(getSpacetype()).get_NewTypeDispersionParameter();
        }

        @Override
        public void setValue(double v)
        {
            SpaceTypesI.getAlreadyCreatedSpaceTypeBySpaceTypeID(getSpacetype()).set_NewTypeDispersionParameter(v);
        }
    }
    
    private static class IntensityDispersion extends DispersionParameter
    {
        private IntensityDispersion(int spacetype)
        {
            super(INTENSITY_DISP, spacetype);
        }

        @Override
        public double getValue()
        {
            return SpaceTypesI.getAlreadyCreatedSpaceTypeBySpaceTypeID(getSpacetype()).get_IntensityDispersionParameter();
        }

        @Override
        public void setValue(double v)
        {
            SpaceTypesI.getAlreadyCreatedSpaceTypeBySpaceTypeID(getSpacetype()).set_IntensityDispersionParameter(v);
        }
    }
}
