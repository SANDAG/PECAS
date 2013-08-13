/**
 * 
 */
package com.hbaspecto.pecas.sd;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * @author jabraham
 *
 */
public class DevelopNewAlternativeTest {

	/**
	 * @throws java.lang.Exception
	 */
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
	}

	/**
	 * @throws java.lang.Exception
	 */
	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
	}

	/**
	 * @throws java.lang.Exception
	 */
	@After
	public void tearDown() throws Exception {
	}

//	/**
//	 * Test method for {@link com.hbaspecto.pecas.sd.DevelopNewAlternative#doDevelopment()}.
//	 */
//	@Test
//	public final void testDoDevelopment() {
//		fail("Not yet implemented"); // FIXME
//	}
//
//	/**
//	 * Test method for {@link com.hbaspecto.pecas.sd.DevelopNewAlternative#getUtility(double)}.
//	 */
//	@Test
//	public final void testGetUtility() {
//		fail("Not yet implemented"); // FIXME
//	}
//
//	/**
//	 * Test method for {@link com.hbaspecto.pecas.sd.DevelopNewAlternative#integrateOverIntensityRange(double, double, double, double, double, double, double)}.
//	 */
//	@Test
//	public final void testIntegrateOverIntensityRangeDoubleDoubleDoubleDoubleDoubleDoubleDouble() {
//		fail("Not yet implemented"); // FIXME
//	}
//
//	/**
//	 * Test method for {@link com.hbaspecto.pecas.sd.DevelopNewAlternative#integrateOverIntensityRange(double, double, double)}.
//	 */
//	@Test
//	public final void testIntegrateOverIntensityRangeDoubleDoubleDouble() {
//		fail("Not yet implemented"); // FIXME
//	}
//
//	/**
//	 * Test method for {@link com.hbaspecto.pecas.sd.DevelopNewAlternative#sampleIntensity()}.
//	 */
//	@Test
//	public final void testSampleIntensity() {
//		fail("Not yet implemented"); // FIXME
//	}

	/**
	 * Test method for {@link com.hbaspecto.pecas.sd.DevelopNewAlternative#sampleIntensityWithinRanges(double, double, double, double, double[], double[], double[])}.
	 */
	@Test
	public final void testSampleIntensityWithinRanges() {
		
		// test case definition
		double dispersionParameter = 0.2;
		double landArea = 43560;
		double rent = 14;
		double landPrepCost = 1e6;
		double baseConstructionCost = 160;
		double belowStepPointCostAdj = 30;
		double aboveStepPointCostAdj = -100;
		double stepPoint = 1.8;
		double stepPointAdjustment = -4e6;
		double amortizationFactor = 0.065051435;
		int samples = 500000;
		
		// user units
		double perLandInitial = -landPrepCost*amortizationFactor/landArea;
		double perLandStep = stepPointAdjustment*amortizationFactor/landArea;
		double minFar = 0;
		double maxFar = 4;
		
		// program variables
		double[] intensityPoints = new double[3];
		intensityPoints[0] = minFar;
		intensityPoints[2] = maxFar;
		intensityPoints[1] = stepPoint;
		
		double[] perSpaceAdjustments = new double[2];
		perSpaceAdjustments[0] = amortizationFactor*belowStepPointCostAdj;
		perSpaceAdjustments[1] = amortizationFactor*aboveStepPointCostAdj;
		
		double[] perLandAdjustments = new double[2];
		perLandAdjustments[0] = 0;
		perLandAdjustments[1] = perLandStep;
		
		double perSpaceInitial = rent - amortizationFactor*baseConstructionCost;
		// now check distribution of bins, precalculated by excel
		double[] expectedProportions  = {
				0.012446647,
				0.013905961,
				0.015536372,
				0.017357941,
				0.019393081,
				0.021666833,
				0.024207171,
				0.027045353,
				0.030216298,
				0.033759023,
				0.037717116,
				0.042139277,
				0.047079917,
				0.052599825,
				0.058766917,
				0.065657073,
				0.073355068,
				0.081955619,
				0.025477021,
				0.024034960,
				0.022674523,
				0.021391091,
				0.020180303,
				0.019038050,
				0.017960450,
				0.016943845,
				0.015984783,
				0.015080006,
				0.014226441,
				0.013421190,
				0.012661518,
				0.011944846,
				0.011268739,
				0.010630901,
				0.010029167,
				0.009461492,
				0.008925948,
				0.008420718,
				0.007944085,
				0.007494431
				};
		
		testProbabilitiesAgainstExpectations(dispersionParameter, landArea, samples,
				perLandInitial, maxFar, intensityPoints, perSpaceAdjustments,
				perLandAdjustments, perSpaceInitial, expectedProportions);
		
		// check with non-zero minimum
		intensityPoints[0] = 0.5;
		
		expectedProportions  = new double[] {
				0,
				0,
				0,
				0,
				0,
				0.023516142,		
				0.026273304,
				0.029353730,
				0.032795322,
				0.036640426,
				0.040936350,
				0.045735953,
				0.051098287,
				0.057089330,
				0.063782796,
				0.071261041,
				0.079616077,
				0.088950702,
				0.027651538,
				0.026086394,
				0.024609841,
				0.023216865,
				0.021902735,
				0.020662987,
				0.019493412,
				0.018390038,
				0.017349118,
				0.016367116,
				0.015440698,
				0.014566717,
				0.013742206,
				0.012964364,
				0.012230549,
				0.011538271,
				0.010885177,
				0.010269050,
				0.009687797,
				0.009139444,
				0.008622130,
				0.008134096
		};
		
		testProbabilitiesAgainstExpectations(dispersionParameter, landArea, samples,
				perLandInitial, maxFar, intensityPoints, perSpaceAdjustments,
				perLandAdjustments, perSpaceInitial, expectedProportions);
		
		// Check if the minimum intensity is greater than the step point.
		intensityPoints[0] = 1;
		intensityPoints[1] = 0;
        
        expectedProportions = new double[] {
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                0.068535684,
                0.064656399,
                0.060996691,
                0.057544132,
                0.054286995,
                0.051214220,
                0.048315372,
                0.045580606,
                0.043000633,
                0.040566694,
                0.038270521,
                0.036104318,
                0.034060726,
                0.032132807,
                0.030314013,
                0.028598167,
                0.026979441,
                0.025452340,
                0.024011676,
                0.022652558,
                0.021370368,
                0.020160754,
                0.019019607,
                0.017943051,
                0.016927431,
                0.015969298,
                0.015065397,
                0.014212659,
                0.013408188,
                0.012649253
        };
        
     // Check if the maximum intensity is less than the step point.
        intensityPoints[1] = 5;
        
        expectedProportions = new double[] {
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                0.004370513,
                0.004882936,
                0.005455439,
                0.006095064,
                0.006809683,
                0.007608087,
                0.008500101,
                0.009496700,
                0.010610145,
                0.011854137,
                0.013243981,
                0.014796778,
                0.016531633,
                0.018469892,
                0.020635404,
                0.023054812,
                0.025757884,
                0.028777880,
                0.032151957,
                0.035921629,
                0.040133277,
                0.044838723,
                0.050095861,
                0.055969374,
                0.062531531,
                0.069863071,
                0.078054200,
                0.087205703,
                0.097430178,
                0.108853427
        };
        
        testProbabilitiesAgainstExpectations(dispersionParameter, landArea, samples,
                perLandInitial, maxFar, intensityPoints, perSpaceAdjustments,
                perLandAdjustments, perSpaceInitial, expectedProportions);
		
		// check the case if netutility ends up constant across an intensity range, amortization factor 0.1, price adjustments 30,0
		
		intensityPoints[0] = 0;
		intensityPoints[1] = 1.8;
		perSpaceAdjustments = new double[] {0, -0.3};
		perSpaceInitial = 0.3;
		
		expectedProportions = new double[] {
				//0.01398194,
				0.037950707,
				0.038179096,
				0.038408859,
				0.038640005,
				0.038872542,
				0.039106478,
				0.039341822,
				0.039578583,
				0.039816768,
				0.040056387,
				0.040297448,
				0.040539959,
				0.040783930,
				0.041029369,
				0.041276286,
				0.041524688,
				0.041774585,
				0.042025986,
				0.012763477,
				0.012763477,
				0.012763477,
				0.012763477,
				0.012763477,
				0.012763477,
				0.012763477,
				0.012763477,
				0.012763477,
				0.012763477,
				0.012763477,
				0.012763477,
				0.012763477,
				0.012763477,
				0.012763477,
				0.012763477,
				0.012763477,
				0.012763477,
				0.012763477,
				0.012763477,
				0.012763477,
				0.012763477
		};
		
		testProbabilitiesAgainstExpectations(dispersionParameter, landArea, samples,
				perLandInitial, maxFar, intensityPoints, perSpaceAdjustments,
				perLandAdjustments, perSpaceInitial, expectedProportions);
		
		
		
	}
	
	@Test
	public void testSampleIntensityMultistep() {
	    
	    // test case definition
        double dispersionParameter = 0.2;
        double landArea = 43560;
        int samples = 500000;
        
        double minFar = 1;
        double maxFar = 4;
        
        double[] intensityPoints = new double[5];
        intensityPoints[0] = minFar;
        intensityPoints[4] = maxFar;
        intensityPoints[1] = 1.8;
        intensityPoints[2] = 2.4;
        intensityPoints[3] = 3.2;
        
        double[] perSpaceAdjustments = new double[] {-2.5, 1.5, -4.5, -3.8};
        
        double[] perLandAdjustments = new double[] {0.5, 1, -0.6, -2.2};
        
        double perSpaceInitial = 3.59177;
        double perLandInitial = -1.49338;
        // now check distribution of bins, precalculated by excel
        double[] expectedProportions  = {
                //0.011312764,
                0.0,
                0.0,
                0.0,
                0.0,
                0.0,
                0.0,
                0.0,
                0.0,
                0.0,
                0.0,
                0.020684020,
                0.021140631,
                0.021607322,
                0.022084315,
                0.022571839,
                0.023070124,
                0.023579410,
                0.024099938,
                0.031313355,
                0.034670185,
                0.038386871,
                0.042501990,
                0.047058255,
                0.052102958,
                0.048185451,
                0.047318083,
                0.046466329,
                0.045629907,
                0.044808541,
                0.044001959,
                0.043209897,
                0.042432093,
                0.027024406,
                0.026912095,
                0.026800250,
                0.026688870,
                0.026577953,
                0.026467496,
                0.026357499,
                0.026247959
                };
        
        testProbabilitiesAgainstExpectations(dispersionParameter, landArea, samples,
                perLandInitial, maxFar, intensityPoints, perSpaceAdjustments,
                perLandAdjustments, perSpaceInitial, expectedProportions);
        
        // Try some missing ranges.
        intensityPoints[1] = 0.0;
        expectedProportions  = new double[] {
                //0.011312764,
                0.0,
                0.0,
                0.0,
                0.0,
                0.0,
                0.0,
                0.0,
                0.0,
                0.0,
                0.0,
                0.014091421,
                0.015602038,
                0.017274596,
                0.019126453,
                0.021176832,
                0.023447015,
                0.025960563,
                0.028743567,
                0.031824913,
                0.035236582,
                0.039013987,
                0.043196334,
                0.047827034,
                0.052954150,
                0.048972644,
                0.048091106,
                0.047225437,
                0.046375350,
                0.045540566,
                0.044720808,
                0.043915806,
                0.043125295,
                0.027465897,
                0.027351750,
                0.027238078,
                0.027124879,
                0.027012150,
                0.026899889,
                0.026788095,
                0.026676765
                };
        
        testProbabilitiesAgainstExpectations(dispersionParameter, landArea, samples,
                perLandInitial, maxFar, intensityPoints, perSpaceAdjustments,
                perLandAdjustments, perSpaceInitial, expectedProportions);
        
        intensityPoints[2] = 0.2;
        expectedProportions  = new double[] {
                //0.011312764,
                0.0,
                0.0,
                0.0,
                0.0,
                0.0,
                0.0,
                0.0,
                0.0,
                0.0,
                0.0,
                0.046019561,
                0.045191181,
                0.044377712,
                0.043578886,
                0.042794440,
                0.042024114,
                0.041267654,
                0.040524811,
                0.039795340,
                0.039078999,
                0.038375554,
                0.037684770,
                0.037006421,
                0.036340283,
                0.035686136,
                0.035043764,
                0.034412955,
                0.033793500,
                0.033185197,
                0.032587843,
                0.032001242,
                0.031425200,
                0.020014270,
                0.019931092,
                0.019848260,
                0.019765772,
                0.019683627,
                0.019601823,
                0.019520359,
                0.019439234
                };
        
        testProbabilitiesAgainstExpectations(dispersionParameter, landArea, samples,
                perLandInitial, maxFar, intensityPoints, perSpaceAdjustments,
                perLandAdjustments, perSpaceInitial, expectedProportions);
        
        intensityPoints[3] = 0.4;
        expectedProportions  = new double[] {
                //0.011312764,
                0.0,
                0.0,
                0.0,
                0.0,
                0.0,
                0.0,
                0.0,
                0.0,
                0.0,
                0.0,
                0.035385244,
                0.035238185,
                0.035091738,
                0.034945899,
                0.034800666,
                0.034656037,
                0.034512009,
                0.034368579,
                0.034225746,
                0.034083506,
                0.033941857,
                0.033800797,
                0.033660323,
                0.033520433,
                0.033381125,
                0.033242395,
                0.033104242,
                0.032966663,
                0.032829656,
                0.032693218,
                0.032557347,
                0.032422041,
                0.032287297,
                0.032153113,
                0.032019487,
                0.031886416,
                0.031753898,
                0.031621931,
                0.031490513,
                0.031359640
                };
        
        testProbabilitiesAgainstExpectations(dispersionParameter, landArea, samples,
                perLandInitial, maxFar, intensityPoints, perSpaceAdjustments,
                perLandAdjustments, perSpaceInitial, expectedProportions);
        
        intensityPoints[1] = 1.8;
        intensityPoints[2] = 2.4;
        intensityPoints[3] = 5.0;
        
        expectedProportions  = new double[] {
                //0.011312764,
                0.0,
                0.0,
                0.0,
                0.0,
                0.0,
                0.0,
                0.0,
                0.0,
                0.0,
                0.0,
                0.018803525,
                0.019218623,
                0.019642885,
                0.020076512,
                0.020519712,
                0.020972696,
                0.021435680,
                0.021908884,
                0.028466490,
                0.031518133,
                0.034896915,
                0.038637907,
                0.042779938,
                0.047365999,
                0.043804654,
                0.043016143,
                0.042241827,
                0.041481448,
                0.040734757,
                0.040001506,
                0.039281455,
                0.038574364,
                0.037880002,
                0.037198139,
                0.036528550,
                0.035871014,
                0.035225314,
                0.034591236,
                0.033968573,
                0.033357118
                };
        
        testProbabilitiesAgainstExpectations(dispersionParameter, landArea, samples,
                perLandInitial, maxFar, intensityPoints, perSpaceAdjustments,
                perLandAdjustments, perSpaceInitial, expectedProportions);
        
        intensityPoints[2] = 4.8;
        
        expectedProportions  = new double[] {
                //0.011312764,
                0.0,
                0.0,
                0.0,
                0.0,
                0.0,
                0.0,
                0.0,
                0.0,
                0.0,
                0.0,
                0.007859984,
                0.008033497,
                0.008210841,
                0.008392100,
                0.008577360,
                0.008766710,
                0.008960240,
                0.009158042,
                0.011899160,
                0.013174764,
                0.014587115,
                0.016150872,
                0.017882265,
                0.019799265,
                0.021921771,
                0.024271811,
                0.026873779,
                0.029754681,
                0.032944419,
                0.036476101,
                0.040386383,
                0.044715853,
                0.049509447,
                0.054816920,
                0.060693361,
                0.067199763,
                0.074403660,
                0.082379823,
                0.091211040,
                0.100988975
                };
        
        testProbabilitiesAgainstExpectations(dispersionParameter, landArea, samples,
                perLandInitial, maxFar, intensityPoints, perSpaceAdjustments,
                perLandAdjustments, perSpaceInitial, expectedProportions);
        
        intensityPoints[1] = 4.6;
        expectedProportions  = new double[] {
                //0.011312764,
                0.0,
                0.0,
                0.0,
                0.0,
                0.0,
                0.0,
                0.0,
                0.0,
                0.0,
                0.0,
                0.023858688,
                0.024385381,
                0.024923702,
                0.025473906,
                0.026036256,
                0.026611021,
                0.027198474,
                0.027798895,
                0.028412571,
                0.029039794,
                0.029680863,
                0.030336084,
                0.031005770,
                0.031690239,
                0.032389818,
                0.033104841,
                0.033835649,
                0.034582589,
                0.035346018,
                0.036126301,
                0.036923809,
                0.037738922,
                0.038572029,
                0.039423528,
                0.040293824,
                0.041183332,
                0.042092476,
                0.043021691,
                0.043971418,
                0.044942111
                };
        
        testProbabilitiesAgainstExpectations(dispersionParameter, landArea, samples,
                perLandInitial, maxFar, intensityPoints, perSpaceAdjustments,
                perLandAdjustments, perSpaceInitial, expectedProportions);
        
        intensityPoints[1] = 0;
        intensityPoints[2] = 2.4;
        expectedProportions  = new double[] {
                //0.011312764,
                0.0,
                0.0,
                0.0,
                0.0,
                0.0,
                0.0,
                0.0,
                0.0,
                0.0,
                0.0,
                0.012791296,
                0.014162538,
                0.015680780,
                0.017361778,
                0.019222982,
                0.021283709,
                0.023565349,
                0.026091583,
                0.028888633,
                0.031985530,
                0.035414417,
                0.039210886,
                0.043414341,
                0.048068411,
                0.044454253,
                0.043654049,
                0.042868250,
                0.042096595,
                0.041338831,
                0.040594706,
                0.039863977,
                0.039146401,
                0.038441742,
                0.037749767,
                0.037070248,
                0.036402961,
                0.035747686,
                0.035104205,
                0.034472308,
                0.033851786
                };
        
        testProbabilitiesAgainstExpectations(dispersionParameter, landArea, samples,
                perLandInitial, maxFar, intensityPoints, perSpaceAdjustments,
                perLandAdjustments, perSpaceInitial, expectedProportions);
        
        intensityPoints[2] = 0.2;
        expectedProportions  = new double[] {
                //0.011312764,
                0.0,
                0.0,
                0.0,
                0.0,
                0.0,
                0.0,
                0.0,
                0.0,
                0.0,
                0.0,
                0.042846137,
                0.042074881,
                0.041317507,
                0.040573767,
                0.039843414,
                0.039126208,
                0.038421913,
                0.037730295,
                0.037051126,
                0.036384184,
                0.035729246,
                0.035086098,
                0.034454527,
                0.033834324,
                0.033225286,
                0.032627210,
                0.032039901,
                0.031463163,
                0.030896807,
                0.030340645,
                0.029794495,
                0.029258176,
                0.028731511,
                0.028214326,
                0.027706451,
                0.027207718,
                0.026717963,
                0.026237023,
                0.025764741,
                0.025300959
                };
        
        testProbabilitiesAgainstExpectations(dispersionParameter, landArea, samples,
                perLandInitial, maxFar, intensityPoints, perSpaceAdjustments,
                perLandAdjustments, perSpaceInitial, expectedProportions);
        
        intensityPoints[2] = 4.8;
        expectedProportions  = new double[] {
                //0.011312764,
                0.0,
                0.0,
                0.0,
                0.0,
                0.0,
                0.0,
                0.0,
                0.0,
                0.0,
                0.0,
                0.005301085,
                0.005869368,
                0.006498571,
                0.007195226,
                0.007966563,
                0.008820588,
                0.009766166,
                0.010813110,
                0.011972289,
                0.013255733,
                0.014676764,
                0.016250131,
                0.017992165,
                0.019920947,
                0.022056497,
                0.024420980,
                0.027038939,
                0.029937546,
                0.033146887,
                0.036700274,
                0.040634588,
                0.044990666,
                0.049813720,
                0.055153811,
                0.061066367,
                0.067612756,
                0.074860926,
                0.082886109,
                0.091771601,
                0.101609629
                };
        
        testProbabilitiesAgainstExpectations(dispersionParameter, landArea, samples,
                perLandInitial, maxFar, intensityPoints, perSpaceAdjustments,
                perLandAdjustments, perSpaceInitial, expectedProportions);
	}

	private void testProbabilitiesAgainstExpectations(double dispersionParameter,
			double landArea, int samples, double perLandInitial, double maxFar,
			double[] intensityPoints, double[] perSpaceAdjustments,
			double[] perLandAdjustments, double perSpaceInitial,
			double[] expectedProportions) {
		// random samples
		int[] bins = new int[(int) Math.ceil(maxFar*10)]; // bin for each 0.1 FAR range
		for (int s=0; s<samples; s++) {
			double intensity = DevelopNewAlternative.sampleIntensityWithinRanges(dispersionParameter, landArea, perSpaceInitial, perLandInitial, intensityPoints, perSpaceAdjustments, perLandAdjustments);
			boolean found = false;
			for (int b=0; b<bins.length;b++ ) {
				if (intensity < (b+1)*.1) {
					bins[b]++;
					found = true;
					break;
				}
			}
			if (!found) fail("Intensity "+intensity+" did not fit into bin");
		}
		for (int b=0;b<bins.length;b++) {
			System.out.println(b*.1+", "+bins[b]/((float)samples) + ", expected = " + expectedProportions[b]);
			if (bins[b]/((float)samples) > expectedProportions[b]+1000.0/samples) fail ("bin "+b+" expected proportion:"+expectedProportions[b]+" but got "+bins[b]/((float)samples));
			if (bins[b]/((float)samples) < expectedProportions[b]-1000.0/samples) fail ("bin "+b+" expected proportion:"+expectedProportions[b]+" but got "+bins[b]/((float)samples));
		}
	}

}
