/*
 * Copyright  2005 HBA Specto Incorporated
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package com.hbaspecto.pecas.sd;

import junit.framework.TestCase;

import com.hbaspecto.pecas.land.SimpleORMLandInventory;
import com.pb.common.datafile.TableDataSet;

public class TestDevelopmentAlternative extends TestCase {

	DevelopmentAlternative d;
	SimpleORMLandInventory land;
	SpaceTypesI resType;
	private double valuePerSpace;
	private double valuePerLand;
	private double landSize;
	private SpaceTypesI nonResType;

	public static void main(String[] args) {
		junit.textui.TestRunner.run(TestDevelopmentAlternative.class);
	}

	public TestDevelopmentAlternative() {
		super();
	}

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		checkDevelopmentTypeSetup();
		final TableDataSet schemes = new TableDataSet();
		schemes.appendColumn(new String[] { "any", "any", "any" }, "ZoningScheme");
		schemes.appendColumn(new float[] { 10, 10, 10 }, "ZoningSchemeCode");
		schemes.appendColumn(new float[] { 1, 2, 0 }, "DevTypeCode");
		schemes.appendColumn(new String[] { "residential", "non residential",
				"vacant" }, "AllowedDevelopmentType");
		schemes.appendColumn(new float[] { 3, 5, 0 }, "MaxIntensity");
		schemes.appendColumn(new float[] { 1000000, 2000000, 3000000 }, "LandFee");
		schemes.appendColumn(new float[] { 2, 3, 0 }, "MinIntensity");
		schemes.appendColumn(new float[] { 0, 0, 0 }, "SpaceFee");
		// ZoningRulesI.setUpZoningSchemes(schemes);
		ZoningRulesI.currentYear = 2000;
		setUpFakeInventory();
		// d = new
		// DevelopmentAlternative(ZoningRulesI.getAlreadyCreatedZoningScheme("any"),
		// resType);
	}

	private void checkDevelopmentTypeSetup() {

	}

	private void setUpFakeInventory() {

	}

	@Override
	protected void tearDown() throws Exception {
		super.tearDown();
	}

	/*
	 * Test method for
	 * 'com.pb.osmp.ld.DevelopmentAlternative.DevelopmentAlternative(ZoningScheme,
	 * DevelopmentType)'
	 */
	public void testDevelopmentAlternative() throws Exception {
		setUp();
	}

	/*
	 * Test method for 'com.pb.osmp.ld.DevelopmentAlternative.getUtility(double)'
	 */
	public void testIntegrateOverIntensityRange() throws Exception {

	}

	/*
	 * Test method for 'com.pb.osmp.ld.DevelopmentAlternative.getUtility(double)'
	 */
	public void testGetUtility() throws Exception {
	}

	/*
	 * Test method for 'com.pb.osmp.ld.DevelopmentAlternative.doDevelopment()'
	 */
	public void testDoDevelopment() {
		// TODO test the DoDevelopment method

	}

}
