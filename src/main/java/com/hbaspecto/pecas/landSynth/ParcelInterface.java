package com.hbaspecto.pecas.landSynth;

import java.sql.SQLException;

public interface ParcelInterface {

	public int getTaz();

    public float getSize();

    public float getQuantity();

    public int getCoverage();
    
    public boolean isSameSpaceType( String type);
    
    public boolean isVacantCoverege();
   
    public String getValue(String string);

    public void setCoverage(String myCode);

    /**
     * @param amount
     */
    public void addSqFtAssigned(float amount);

    /**
     * @param f
     */
    public void setQuantity(float f);

    public int getRevision();

    public double getInitialFAR();
    
    public long getId();

	public double getOldScore(int intCoverageType);

	public void setOldScore(int intCoverageType, double score);

}