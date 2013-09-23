/*
 * Copyright 2005 HBA Specto Incorporated
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a
 * copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations
 * under the License.
 */
package com.hbaspecto.pecas.sd;

import junit.framework.TestCase;
import com.pb.common.datafile.TableDataSet;

public class TestDevelopMoreAlternativeOld
        extends TestCase
{

    // HashTableLandInventory land;
    DevelopMoreAlternative d;
    SpaceTypesI            nonResType;

    public static void main(String[] args)
    {
        junit.textui.TestRunner.run(TestDevelopMoreAlternativeOld.class);
    }

    @Override
    protected void setUp() throws Exception
    {
        super.setUp();
        checkDevelopmentTypeSetup();
        final TableDataSet schemes = new TableDataSet();
        schemes.appendColumn(new String[] {"any", "any", "any"}, "ZoningScheme");
        schemes.appendColumn(new float[] {10, 10, 10}, "ZoningSchemeCode");
        schemes.appendColumn(new float[] {1, 2, 0}, "DevTypeCode");
        schemes.appendColumn(new String[] {"residential", "non residential", "vacant"},
                "AllowedDevelopmentType");
        schemes.appendColumn(new float[] {3, 5, 0}, "MaxIntensity");
        schemes.appendColumn(new float[] {1000000, 2000000, 3000000}, "LandFee");
        schemes.appendColumn(new float[] {1, 2, 0}, "MinIntensity");
        schemes.appendColumn(new float[] {0, 0, 0}, "SpaceFee");
        // ZoningRulesI.setUpZoningSchemes(schemes);
        ZoningRulesI.currentYear = 2000;
        setUpFakeInventory();
        // d = new
        // DevelopMoreAlternative(ZoningRulesI.getAlreadyCreatedZoningScheme("any"));
    }

    private void checkDevelopmentTypeSetup()
    {

    }

    private void setUpFakeInventory()
    {

    }

    @Override
    protected void tearDown() throws Exception
    {
        super.tearDown();
    }

    /*
     * Test method for 'com.pb.models.pecas.ld.DevelopMoreAlternative.DevelopMoreAlternative(ZoningScheme)'
     */
    public void testDevelopMoreAlternative() throws Exception
    {
        setUp();
    }

    /*
     * Test method for 'com.pb.models.pecas.ld.DevelopMoreAlternative.getUtility(double)'
     */
    public void testGetUtility() throws Exception
    {

    }

    /*
     * Test method for 'com.pb.models.pecas.ld.DevelopMoreAlternative.integrateOverIntensityRange(double, double, double, double, double)'
     */
    public void testIntegrateOverIntensityRange()
    {

    }

    /*
     * Test method for 'com.pb.models.pecas.ld.DevelopMoreAlternative.doDevelopment()'
     */
    public void testDoDevelopment()
    {

    }

}
