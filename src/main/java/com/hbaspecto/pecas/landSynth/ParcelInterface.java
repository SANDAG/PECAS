package com.hbaspecto.pecas.landSynth;

public interface ParcelInterface
{

    int getTaz();

    float getSize();

    float getQuantity();

    int getCoverage();

    boolean isSameSpaceType(String type);

    boolean isVacantCoverege();

    String getValue(String string);

    void setCoverage(String myCode);

    /**
     * @param amount
     */
    void addSqFtAssigned(float amount);

    /**
     * @param f
     */
    void setQuantity(float f);

    int getRevision();

    double getInitialFAR();

    long getId();

    double getOldScore(int intCoverageType);

    void setOldScore(int intCoverageType, double score);

}