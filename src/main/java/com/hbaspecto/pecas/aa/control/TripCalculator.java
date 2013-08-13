package com.hbaspecto.pecas.aa.control;

import java.util.Iterator;
import java.util.TreeSet;


import org.apache.log4j.Logger;

import com.hbaspecto.functions.InverseCumulativeNormal;
import com.hbaspecto.matrix.SparseMatrix;
import com.hbaspecto.pecas.aa.commodity.Commodity;
import com.pb.common.datafile.TableDataSet;
import com.pb.common.matrix.AlphaToBeta;
import com.pb.common.matrix.AlphaToBetaInterface;
import com.pb.common.matrix.HashtableAlphaToBeta;
import com.pb.common.matrix.Matrix;
import com.pb.common.matrix.StringIndexedNDimensionalMatrix;

public class TripCalculator {

	static Logger logger = Logger.getLogger(TripCalculator.class);
	String[] tripTypes;
	private Matrix[] luzMatrices;
	final double useNormalAt;
	final double useRoundedAt;
	
	TripCalculator(double useNormalAt, double useRoundedAt) {
		this.useNormalAt = useNormalAt;
		this.useRoundedAt = useRoundedAt;
	}

	public Matrix[] calculateLuzTrips(TableDataSet spec) {
		tripTypes = uniqueEntries(spec.getColumnAsString("TripType"));
		luzMatrices = new Matrix[tripTypes.length];
		for (int r=1;r<=spec.getRowCount();r++) {
			logger.info("  Adding luz flows for "+
					spec.getStringValueAt(r, "CommodityName")+" to " +
					spec.getStringValueAt(r,"TripType")+" trips");
			Commodity c = Commodity.retrieveCommodity(spec.getStringValueAt(r, "CommodityName"));
			if (c==null) {
				logger.error("Invalid commodity name "+spec.getStringValueAt(r, "CommodityName")+" in TripCalculation.csv");
			}
			Matrix flows = null;
			if (spec.getStringValueAt(r,"CommodityDirection").equalsIgnoreCase("b")) {
				flows = c.getBuyingFlowMatrix();
			} else if (spec.getStringValueAt(r,"CommodityDirection").equalsIgnoreCase("s")) {
				flows = c.getSellingFlowMatrix();
			} else {
				logger.error("Invalid CommodityDirection "+spec.getStringValueAt(r,"CommodityDirection")+ "in TripCalculation.csv");
			}
			flows = flows.multiply(spec.getValueAt(r, "Coefficient"));
			Matrix trips = luzMatrices[findStringIndex(spec.getStringValueAt(r,"TripType"))];
			Matrix newTrips = null;
			if (trips ==null) {
				// trip table isn't created yet; create it now
				newTrips = flows;
			} else {
			    newTrips = trips.add(flows);
			}
			luzMatrices[findStringIndex(spec.getStringValueAt(r,"TripType"))]=newTrips;
		}
		return luzMatrices;
	}
	
	/**
	 * @param a2b correspondence between TAZ (alpha zones) and LUZ (beta zones)
	 * @param spec describing how certain trips are to be generated from flow rates
	 * @param alphaZonalMake 2 dimensional matrix of make amounts, first index is commodity second index is TAZ zone 
	 * @param alphaZonalUse 2 dimensional matrix of use amounts, first index is commodity second index is TAZ zone
	 * @param maxTaz maximum TAZ number
	 * @return
	 */
	public com.hbaspecto.matrix.SparseMatrix[] sampleTazTrips(Matrix[] luzTrips, AlphaToBetaInterface a2b, TableDataSet spec, StringIndexedNDimensionalMatrix alphaZonalMake, StringIndexedNDimensionalMatrix alphaZonalUse, int maxTaz) {
		if (tripTypes ==null) {
			String msg = TripCalculator.class.getName()+".sampleTazTrips called before "+TripCalculator.class.getName()+".calculateLuzTrips";
			logger.fatal(msg);
			throw new RuntimeException(msg);
		}
		logger.info("Sampling TAZ Trips");
		double[][] originSamples = new double[tripTypes.length][maxTaz+1];
		double[][] destinationSamples = new double[tripTypes.length][maxTaz+1];
		// go through trip spec calculating trip production rates and trip attraction rates
		int[] alphaZones = a2b.getAlphaExternals0Based();
		int[] makeLocationSpecifier = new int[2];
		int[] useLocationSpecifier = new int[2];
		for (int r=1;r<=spec.getRowCount();r++) {
			Commodity c = Commodity.retrieveCommodity(spec.getStringValueAt(r, "CommodityName"));
			if (c==null) {
				logger.error("Invalid commodity name "+spec.getStringValueAt(r, "CommodityName")+" in TripCalculation.csv");
			}
			logger.info("Calculating origin and destination TAZ trips for "+c);
			makeLocationSpecifier[0] = alphaZonalMake.getIntLocationForDimension(0,c.getName());
			useLocationSpecifier[0] = alphaZonalUse.getIntLocationForDimension(0, c.getName());
			int tripIndex = findStringIndex(spec.getStringValueAt(r,"TripType"));
			double coefficient = spec.getValueAt(r, "Coefficient");
				// loop through TAZ's
			for (int tazIdx = 0;tazIdx<alphaZones.length;tazIdx++) {
				int alphaZone = alphaZones[tazIdx];
				String alphaString = String.valueOf(alphaZone);
				int beta = a2b.getBetaZone(alphaZone);
				makeLocationSpecifier[1] = alphaZonalMake.getIntLocationForDimension(1, alphaString);
				double alphaMake = alphaZonalMake.getValue(makeLocationSpecifier);
				useLocationSpecifier[1] = alphaZonalUse.getIntLocationForDimension(1,alphaString);
				double alphaUse = alphaZonalUse.getValue(useLocationSpecifier);
				originSamples[tripIndex][alphaZone] += alphaMake*coefficient;
				destinationSamples[tripIndex][alphaZone] += alphaUse*coefficient;
				// ENHANCEMENT could also use import/export quantities separately, so that if a commodity disappears or appears through exchange-zone (old style) imports/exports that portion is split equally across TAZs
			}
		}
		// now sample out the trips
		SparseMatrix[] tazTrips = new SparseMatrix[tripTypes.length];
		for (int i=0;i<tripTypes.length;i++) {
			tazTrips[i] = new SparseMatrix(maxTaz+1,maxTaz+1);
		}
		for (int tripType = 0;tripType < tripTypes.length;tripType++) {
			Matrix luzMatrix = luzTrips[tripType];
			SparseMatrix tazMatrix = tazTrips[tripType];
			logger.info("Sampling OD trips for "+tripTypes[tripType]);
			for (Iterator origIt = luzMatrix.getExternalNumberIterator(); origIt.hasNext();) {
				int origLuz = ((Integer) origIt.next()).intValue();
				System.out.print(origLuz+" ");
				for (Iterator destIt = luzMatrix.getExternalNumberIterator(); destIt.hasNext();) {
					int destLuz = ((Integer) destIt.next()).intValue();
					float trips = luzMatrix.getValueAt(origLuz, destLuz);
					int[] origTazs = a2b.getAlphasForBetas(origLuz);
					int[] destTazs = a2b.getAlphasForBetas(destLuz);
					double totalAllocationFactor = 0;
					for (int tazOrig : origTazs) {
						for (int tazDest : destTazs) {
							totalAllocationFactor += originSamples[tripType][tazOrig]*destinationSamples[tripType][tazDest];
						}
					}
					boolean allocationFactorIsZoneCount = false;
					if (totalAllocationFactor == 0 ) {
						allocationFactorIsZoneCount = true;
						totalAllocationFactor = origTazs.length*destTazs.length;
					}
					for (int tazOrig : origTazs) {
						for (int tazDest : destTazs) {
							double expectedTrips = trips * originSamples[tripType][tazOrig]*destinationSamples[tripType][tazDest]/totalAllocationFactor;
							if (allocationFactorIsZoneCount) {
								expectedTrips = trips / totalAllocationFactor;
							}
							int tazTripCount = poisson(expectedTrips, useNormalAt, useRoundedAt);
							tazMatrix.set(tazOrig,tazDest,tazTripCount);
						}
					}
				}
			}
			System.out.println();
		}
		return tazTrips;
	}

	private void poissonSample(Matrix[] matrices) {
		int matrixNumber=0;
		for (Matrix m : matrices) {
			double origTotal = 0;
			double newTotal = 0;
			for (int r : m.getExternalRowNumbers()) {
				for (int c: m.getExternalColumnNumbers()) {
					float value = m.getValueAt(r, c);
					origTotal+= value;
					int sample = poisson((double) value, useNormalAt, useRoundedAt);
					newTotal += sample;
					m.setValueAt(r, c, (float) sample);
				}
			}
			logger.info("Poisson sampling of trip matrix "+tripTypes[matrixNumber++]+", origTrips:"+origTotal+", newTrips:"+newTotal);
			
		}
	}

	private static int poisson(double lambda, double useNormalAt, double useRoundedAt) {
		if (lambda < 0) {
			logger.error("Negative trip rate "+lambda);
			return 0;
		}
		if (lambda == 0) return 0;
		if (lambda > useRoundedAt) {
			return (int) (lambda+0.5);
		}
		double random = Math.random();
		if (lambda >useNormalAt) {
			//use normal approximation
			double invCumNormal = InverseCumulativeNormal.getInvCDF(random,false);
			// normal approximation to poisson has variance=lambda and mean=lambda
			double value = (invCumNormal*Math.sqrt(lambda))+lambda;
			int intValue = (int) (value+0.5);
			return Math.max(0,intValue);
		}
		// else calculate cumulative probability function directly from sum
		int i = 0;
		double cumProb =0;
		double lambdaPowK = 1;
		double kFactorial = 1;
		double expNegLambda = Math.exp(-lambda);
		do {
			cumProb += lambdaPowK*expNegLambda/kFactorial;
			if (cumProb >= random) return i;
			i++;
			lambdaPowK*=lambda;
			kFactorial *=i;
		} while (i<lambda*10000);
		logger.info("poisson sampling count is 10000 times expected value of "+lambda+" , random sample is "+random+" cumProbability was "+cumProb+", returning the next value "+i);
		return i;
	}

	private int findStringIndex(String tripType) {
		int i = 0;
		do {
			if (tripTypes[i].equals(tripType)) {
				return i;
			}
			i++;
		} while (i<tripTypes.length);
		return -1;
		
	}

	private String[] uniqueEntries(String[] strings) {
		TreeSet<String> entries = new TreeSet<String>();
		for (String string : strings) {
			entries.add(string);
		}
		return entries.toArray(new String[entries.size()]);
	}

	public String[] getTripArrayNames() {
		return tripTypes;
	}
	
	public Matrix[] applyPoissonDistribution() {
		poissonSample(luzMatrices);
		return luzMatrices;
	}
	
}
