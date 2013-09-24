package com.hbaspecto.pecas.land;

public interface ParcelInterface
{

    String get_ParcelId();

    void set_ParcelId(String value);

    long get_PecasParcelNum();

    void set_PecasParcelNum(long value);

    int get_SpaceTypeId();

    void set_SpaceTypeId(int value);

    int get_AvailableServicesCode();

    void set_AvailableServicesCode(int value);

    int get_YearBuilt();

    void set_YearBuilt(int value);

    double get_SpaceQuantity();

    void set_SpaceQuantity(double value);

    double get_LandArea();

    void set_LandArea(double value);

    boolean get_IsDerelict();

    void set_IsDerelict(boolean isDerelict);

    boolean get_IsBrownfield();

    void set_IsBrownfield(boolean isBrownfield);

}