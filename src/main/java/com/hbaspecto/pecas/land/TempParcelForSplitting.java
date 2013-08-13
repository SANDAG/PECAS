package com.hbaspecto.pecas.land;

public class TempParcelForSplitting implements ParcelInterface {
	private String parcelID;
	private int spaceTypeID, avaliableServiceCode, yearBuilt;
	private long pecasParcelNum;
	private double landArea, SpaceQuantity;
	private boolean isDerelict;
	private boolean isBrownfield;
	
	public TempParcelForSplitting(ParcelsTemp currentParcel, long maxPecasID){
		set_SpaceTypeId(currentParcel.get_SpaceTypeId());				
		set_ParcelId(currentParcel.get_ParcelId());
		set_AvailableServicesCode(currentParcel.get_AvailableServicesCode());
		set_YearBuilt(currentParcel.get_YearBuilt());	
		set_IsDerelict(currentParcel.get_IsDerelict());
		set_IsBrownfield(currentParcel.get_IsBrownfield());
		
		//This new parcel should get a new PECAS Parcel num, new SpaceQuantity, and new LAnd Area.
		set_PecasParcelNum(maxPecasID);				
		set_SpaceQuantity(-1);
		set_LandArea(-1);
	}

	@Override
	public String get_ParcelId() {		
		return parcelID;
	}

	@Override
	public long get_PecasParcelNum() {
		return pecasParcelNum;
	}

	@Override
	public double get_SpaceQuantity() {
		return SpaceQuantity;
	}

	@Override
	public double get_LandArea() {
		return landArea;
	}
	
	@Override
	public int get_SpaceTypeId() {
		return spaceTypeID;
	}

	@Override
	public int get_YearBuilt() {
		return yearBuilt;
	}

	@Override
	public void set_LandArea(double value) {
		landArea= value;
	}

	@Override
	public void set_ParcelId(String value) {
		parcelID = value;
	}

	@Override
	public void set_PecasParcelNum(long value) {
		pecasParcelNum = value;
	}

	@Override
	public void set_SpaceQuantity(double value) {		
		SpaceQuantity = value;
	}

	@Override
	public void set_SpaceTypeId(int value) {
		spaceTypeID= value;
	}

	@Override
	public void set_YearBuilt(int value) {
		yearBuilt = value;
	}

	@Override
	public int get_AvailableServicesCode() {
		return avaliableServiceCode;
	}

	@Override
	public void set_AvailableServicesCode(int value) {
		avaliableServiceCode = value;		
	}

	@Override
	public boolean get_IsDerelict() {
		return isDerelict;
	}

	@Override
	public void set_IsDerelict(boolean isDerelict) {
		this.isDerelict= isDerelict;
		
	}

	@Override
	public boolean get_IsBrownfield() {
		return isBrownfield;
	}

	@Override
	public void set_IsBrownfield(boolean isBrownfield) {
		this.isBrownfield = isBrownfield;
		
	}
}
