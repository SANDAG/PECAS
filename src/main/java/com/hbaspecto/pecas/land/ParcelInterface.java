package com.hbaspecto.pecas.land;

public interface ParcelInterface {
	   
	   public String get_ParcelId();
	   public void set_ParcelId( String value);

	   public long get_PecasParcelNum();
	   public void set_PecasParcelNum(long value);

	   public int get_SpaceTypeId();
	   public void set_SpaceTypeId(int value);
	   
	   public int get_AvailableServicesCode();
	   public void set_AvailableServicesCode(int value);
	   
	   public int get_YearBuilt();
	   public void set_YearBuilt(int value);

	   public double get_SpaceQuantity();
	   public void set_SpaceQuantity(double value);

	   public double get_LandArea();
	   public void set_LandArea(double value);
	   
	   public boolean get_IsDerelict();
	   public void set_IsDerelict(boolean isDerelict);
	   
	   public boolean get_IsBrownfield();
	   public void set_IsBrownfield(boolean isBrownfield);
	   
}