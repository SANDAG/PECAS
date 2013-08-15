package com.hbaspecto.pecas.aa.control;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import org.apache.log4j.Logger;

import com.hbaspecto.pecas.NoAlternativeAvailable;
import com.hbaspecto.pecas.OverflowException;
import com.hbaspecto.pecas.aa.AAStatusLogger;
import com.hbaspecto.pecas.aa.activities.ProductionActivity;
import com.hbaspecto.pecas.aa.commodity.AbstractCommodity;
import com.hbaspecto.pecas.aa.commodity.Commodity;
import com.hbaspecto.pecas.aa.control.AAPProcessor.ZoneQuantityStorage;
import com.hbaspecto.pecas.aa.technologyChoice.ConsumptionFunction;
import com.hbaspecto.pecas.aa.technologyChoice.ProductionFunction;
import com.hbaspecto.pecas.zones.PECASZone;
import com.pb.common.matrix.AlphaToBeta;
import com.pb.common.matrix.AlphaToBetaInterface;
import com.pb.common.matrix.StringIndexedNDimensionalMatrix;

public class TAZSplitter {

	static final Logger logger = Logger.getLogger(TAZSplitter.class);
	private final Hashtable floorspaceInventory;

	private final AlphaToBetaInterface floorspaceZoneCrossref;
	private final int maxAlphaZone;
	private final String outputPath;
	private final PECASZone[] zones;
	
	
	/**
	 * An N-dimensional array of make and use coefficients.  ZonalMakeUse.csv stored in an array
	 * Dimension 0 is production activities
	 * Dimension 1 is beta zone
	 * Dimension 2 is commodity name
	 * Dimension 3 is "M" or "U" for make or use
	 */
	private final StringIndexedNDimensionalMatrix zonalMakeUseCoefficients;
	StringIndexedNDimensionalMatrix alphaZonalMake;
	StringIndexedNDimensionalMatrix alphaZonalUse;

	//	 private final AAPProcessor pp;

	TAZSplitter(AlphaToBeta a2b, Hashtable floorspaceInventory, int maxTAZ, String outputPath, PECASZone[] zones, StringIndexedNDimensionalMatrix zonalMakeUseCoefficients) {
		this.floorspaceZoneCrossref = a2b;
		this.floorspaceInventory = floorspaceInventory;
		maxAlphaZone = maxTAZ;
		this.outputPath=outputPath;
		this.zones = zones;
		this.zonalMakeUseCoefficients = zonalMakeUseCoefficients;
		//this.pp = pp;
	}

	void writeFloorspaceZoneTables(ArrayList<String> CommodityNamesForDetailedOutputs) {
		String[] commodityNames = setupZonalMakeAndUseObjects();

		BufferedWriter locationsFile;
		BufferedWriter detailedMakeFile = null;
		BufferedWriter detailedUseFile = null;
		try {
			locationsFile = new BufferedWriter(new FileWriter(outputPath + "ActivityLocations2.csv"));
			locationsFile.write("Activity,ZoneNumber,Quantity\n");
			if (CommodityNamesForDetailedOutputs.size()>0) {
				detailedMakeFile = new BufferedWriter(new FileWriter(outputPath + "TAZDetailedMake.csv"));
				detailedMakeFile.write("Activity,TAZ,Commodity,Amount\n");
				detailedUseFile = new BufferedWriter(new FileWriter(outputPath + "TAZDetailedUse.csv"));
				detailedUseFile.write("Activity,TAZ,Commodity,Amount\n");
			}
			Iterator productionActivityIterator = ProductionActivity.getAllProductionActivities().iterator();
			logger.info("Splitting production activities into FloorspaceZones");
			AAStatusLogger.logText("Splitting production activities into floorspace zones");
			while (productionActivityIterator.hasNext()) {
				ProductionActivity p = (ProductionActivity)productionActivityIterator.next();
				//if(logger.isDebugEnabled()) 
				logger.info("\t splitting "+p+" into FloorspaceZones");
				try {
					ConsumptionFunction cf = p.getConsumptionFunction();
					ProductionFunction pf = p.getProductionFunction();
					double[] activityLocationsSplit = new double[maxAlphaZone+1];
					for (int betaZoneIndexNumber = 0; betaZoneIndexNumber < p.getMyDistribution().length; betaZoneIndexNumber++) {
						double pecasZoneTotal = p.getMyDistribution()[betaZoneIndexNumber].getQuantity();
	                    double[] buyingZUtilities = new double[cf.size()];
	                    for (int c = 0; c < cf.size(); c++) {
	                        AbstractCommodity com = cf.commodityAt(c);
	                        if (com == null) {
	                            buyingZUtilities[c] = 0;
	                        } else {
	                            buyingZUtilities[c] = com.calcZUtility(zones[betaZoneIndexNumber], false);
	                        }
	                    } //CUSellc,z and CUBuyc,z have now been calculated for the commodites made or used by the activity
	                    double[] sellingZUtilities = new double[pf.size()];
	                    for (int c = 0; c < pf.size(); c++) {
	                        AbstractCommodity com = pf.commodityAt(c);
	                        if (com == null) {
	                            sellingZUtilities[c] = 0;
	                        } else {
	                            sellingZUtilities[c] = com.calcZUtility(zones[betaZoneIndexNumber], true);
	                        }
	                    } //CUSellc,z and CUBuyc,z have now been calculated for the commodites made or used by the activity
	
	                    double totalFloorspaceConsumption = 0;
						double[] amounts = null;;
						try {
							amounts = cf.calcAmounts(buyingZUtilities, sellingZUtilities, betaZoneIndexNumber);
							totalFloorspaceConsumption = calculateTotalFloorspaceConsumption(
									cf, betaZoneIndexNumber, buyingZUtilities,
									amounts);
						} catch (NoAlternativeAvailable e) {
							
							// total floorspace consumption is zero.
						}
						if (totalFloorspaceConsumption == 0) {
							splitActivityTotalEquallyAcrossTAZs(
									activityLocationsSplit,
									betaZoneIndexNumber, pecasZoneTotal);
						} else {
							splitActivityTotalBasedOnSpaceConsumption(cf,
									activityLocationsSplit,
									betaZoneIndexNumber, pecasZoneTotal,
									amounts, totalFloorspaceConsumption);
						}
					}
					//end betazone loop
					
					// write out split activity locations
				    ArrayList<Integer> alphaZones = new ArrayList<Integer>();
				    int[] alphaZonesArray = floorspaceZoneCrossref.getAlphaExternals0Based();
				    for (int alphaZoneNumber : alphaZonesArray) {
				    	alphaZones.add(alphaZoneNumber);
				    }
					for(int azone = 0; azone < activityLocationsSplit.length;azone++) {
						if (alphaZones.contains(azone)) {
							locationsFile.write(p.name+",");
							locationsFile.write(azone+",");
							locationsFile.write(activityLocationsSplit[azone]+"\n");
						}
					}
					// activity have been split into zones, now need to split make and use of commodities
					// results stored in zonalMake and zonalUse
					splitMakeAndUseAmount(commodityNames, p, activityLocationsSplit, CommodityNamesForDetailedOutputs, detailedMakeFile, detailedUseFile);
					// end production activity loop
				} catch ( OverflowException e) {
					String msg = "Can't split Activity p";
					logger.error(msg,e);
				}
			}
			logger.info("\tActivityLocations2.csv has been written");
			locationsFile.close();
			if (detailedMakeFile !=null) detailedMakeFile.close();
			if (detailedUseFile != null) detailedUseFile.close();
		} catch (IOException e) {
			logger.fatal("Can't create location output file");
			e.printStackTrace();
		}
		writeBinaryAlphaZoneTotalMakeUse();
		writeAsciiAlphaZoneTotalMakeUse(commodityNames);
	}//end writeFloorspaceZoneTables

	private void writeAsciiAlphaZoneTotalMakeUse(String[] commodityNames) {
		try {
			BufferedWriter out = new BufferedWriter(new FileWriter(outputPath + "FloorspaceZoneTotalMakeUse.csv"));
			out.write("Commodity,ZoneNumber,Made,Used\n");
			String[] index = new String[2];
			for (int comNum=0;comNum<commodityNames.length;comNum++) {
				index[0] = commodityNames[comNum];
				for (int zoneNum=0;zoneNum<=maxAlphaZone;zoneNum++) {
					Integer betaZone = floorspaceZoneCrossref.getBetaZone(zoneNum);
					if (betaZone!=-1) {
						// valid TAZ;
						index[1] = String.valueOf(zoneNum);
						out.write(commodityNames[comNum]+","+zoneNum+","+alphaZonalMake.getValue(index)+","+alphaZonalUse.getValue(index)+"\n");
					}
				}
			}
			out.flush();
			out.close();
		} catch (java.io.IOException e) {
			logger.fatal("Can't write out FloorspaceZoneTotalMakeUse use ascii csv file "+e);
		}
	}
	


	private void writeBinaryAlphaZoneTotalMakeUse() {
		String filename = outputPath + "FloorspaceZoneTotalMakeUse.bin";
		if (filename != null) {
			try {
				java.io.FileOutputStream fos = new java.io.FileOutputStream(filename);
				java.io.ObjectOutputStream out = new java.io.ObjectOutputStream(fos);
				out.writeObject(alphaZonalMake);
				out.writeObject(alphaZonalUse);
				out.flush();
				out.close();
			} catch (java.io.IOException e) {
				logger.fatal("Can't write out FloorspaceZoneTotalMakeUse use binary file "+e);
			}
		}
	}

	private void splitMakeAndUseAmount(String[] commodityNames,
			ProductionActivity productionActivity,
			double[] activityLocationsSplit, ArrayList<String> commodityNamesForDetailedOutput,
			BufferedWriter detailedMakeFile, BufferedWriter detailedUseFile) throws IOException {
		int[] zonalMakeUseIndices = new int[4];
		String[] aZoneTotalMUIndices = new String[2];
		zonalMakeUseIndices[0] = zonalMakeUseCoefficients.getIntLocationForDimension(0,productionActivity.name);
			
		int makeIndex = zonalMakeUseCoefficients.getIntLocationForDimension(3,"M");
		int useIndex = zonalMakeUseCoefficients.getIntLocationForDimension(3,"U");
		for(int azone = 0; azone < activityLocationsSplit.length;azone++) {
			Integer integerAZone = new Integer(azone);
			String stringAZone = integerAZone.toString();
			int betaZone =  floorspaceZoneCrossref.getBetaZone(azone);
			if (betaZone !=-1) {
				// some alphazones don't have a betazone, effectively we ignore those
				zonalMakeUseIndices[1] = zonalMakeUseCoefficients.getIntLocationForDimension(1,String.valueOf(betaZone));
				for (int commodity=0;commodity<commodityNames.length;commodity++) {
					boolean detailedOutputCommodity=false;
					if (commodityNamesForDetailedOutput!=null) {
						if (commodityNamesForDetailedOutput.contains(commodityNames[commodity])) {
							detailedOutputCommodity = true;
						}
					}
					zonalMakeUseIndices[2] = zonalMakeUseCoefficients.getIntLocationForDimension(2,commodityNames[commodity]);
					aZoneTotalMUIndices[0] = commodityNames[commodity];
					aZoneTotalMUIndices[1] = stringAZone;
					
					// first split make
					zonalMakeUseIndices[3] = makeIndex;
					int[] zonalMakeLocation = null;
					try {
						zonalMakeLocation = alphaZonalMake.getIntLocation(aZoneTotalMUIndices);
					} catch (RuntimeException e) {
						// location doesnt exist yet
						alphaZonalMake.setValue(0,aZoneTotalMUIndices);
						zonalMakeLocation = alphaZonalMake.getIntLocation(aZoneTotalMUIndices);
					}
					float makeCoefficient = zonalMakeUseCoefficients.getValue(zonalMakeUseIndices);
					double zonalMakeValue = activityLocationsSplit[azone]*makeCoefficient;
					
					if (detailedOutputCommodity && zonalMakeValue != 0) {
						detailedMakeFile.write(productionActivity.name+","+azone+","+commodityNames[commodity]+","+zonalMakeValue+"\n");
					}
					
					// now increment make by getting, adding and setting
					zonalMakeValue += alphaZonalMake.getValue(zonalMakeLocation);
					alphaZonalMake.setValue((float) zonalMakeValue,zonalMakeLocation);
					
					// now split use
					zonalMakeUseIndices[3] = useIndex;
					int[] zonalUseLocation = null;
					try {
						zonalUseLocation = alphaZonalUse.getIntLocation(aZoneTotalMUIndices);
					} catch (RuntimeException e) {
						// location doesnt exist yet
						alphaZonalUse.setValue(0,aZoneTotalMUIndices);
						zonalUseLocation = alphaZonalUse.getIntLocation(aZoneTotalMUIndices);
					}
					float useCoefficient = zonalMakeUseCoefficients.getValue(zonalMakeUseIndices);
					double zonalUseValue = activityLocationsSplit[azone]*useCoefficient;
					
					if (detailedOutputCommodity && zonalUseValue != 0) {
						detailedUseFile.write(productionActivity.name+","+azone+","+commodityNames[commodity]+","+zonalUseValue+"\n");
					}

					// now increment use, by getting, adding and setting.
					zonalUseValue += alphaZonalUse.getValue(zonalUseLocation);
					alphaZonalUse.setValue((float) zonalUseValue,zonalUseLocation);
				}
			}
		}
	}

	private double calculateTotalFloorspaceConsumption(ConsumptionFunction cf,
			int betaZoneIndexNumber, double[] buyingZUtilities, double[] amounts)
	{
		try {
			for (int c = 0; c < cf.size(); c++) {
				AbstractCommodity com = cf.commodityAt(c);
				if (com == null) {
					buyingZUtilities[c] = 0;
				} else {
					buyingZUtilities[c] = cf.commodityAt(c).calcZUtility(zones[betaZoneIndexNumber], false);
				}
			} //CUSellc,z and CUBuyc,z have now been calculated for the commodites made or used by the activity
		} catch (Exception e) {
			logger.fatal("Error adding activity quantity to ActivityLocations2 table");
			e.printStackTrace();
		}
		double totalFloorspaceConsumption = 0;
		for (int c =0; c<amounts.length;c++) {
			Commodity commodity = (Commodity) cf.commodityAt(c);
			if (commodity!=null) {
				if (commodity.isFloorspaceCommodity()) {
					totalFloorspaceConsumption += amounts[c]; 
				}
			}
		}
		return totalFloorspaceConsumption;
	}

	private String[] setupZonalMakeAndUseObjects() {
		int[] makeUseArraySize = new int[2];
		makeUseArraySize[0] = AbstractCommodity.getAllCommodities().size();
		makeUseArraySize[1] = maxAlphaZone+1;
		String[] columnNames = new String[2];
		columnNames[0] = "Commodity";
		columnNames[1] = "FloorspaceZone";
		alphaZonalMake = new StringIndexedNDimensionalMatrix("zonalMake",2,makeUseArraySize,columnNames);
		alphaZonalUse = new StringIndexedNDimensionalMatrix("zonalUse",2,makeUseArraySize,columnNames);

		// set up indices for zonalMake and zonalUse
		Commodity[] commodities = new Commodity[AbstractCommodity.getAllCommodities().size()];
		commodities = AbstractCommodity.getAllCommodities().toArray(commodities);
		String[] commodityNames = new String[commodities.length];
		for (int c=0;c<commodityNames.length;c++) {
			commodityNames[c] = commodities[c].name;
		}
		String[] alphaZoneNumberArray = new String[maxAlphaZone+1];
		for (int i=0;i<alphaZoneNumberArray.length;i++) {
			alphaZoneNumberArray[i] = (new Integer(i)).toString();
		}
		String[][] zonalMakeIndicesKeys = new String[2][];
		zonalMakeIndicesKeys[0] = commodityNames;
		zonalMakeIndicesKeys[1] = alphaZoneNumberArray;
		alphaZonalMake.setStringKeys(zonalMakeIndicesKeys);
		alphaZonalUse.setStringKeys(zonalMakeIndicesKeys);
		return commodityNames;
	}

	private void splitActivityTotalBasedOnSpaceConsumption(
			ConsumptionFunction cf, double[] activityLocationsSplit,
			int betaZoneIndexNumber, double pecasZoneTotalActivityAmount,
			double[] consumptionAmounts, double totalFloorspaceConsumption) {
		for (int c =0; c<consumptionAmounts.length;c++) {
			Commodity commodity = (Commodity) cf.commodityAt(c);
			if (commodity!=null) {
				if (commodity.isFloorspaceCommodity()) {
					double floorspaceTotal = 0;
					double activityAmountForCommodity=pecasZoneTotalActivityAmount*consumptionAmounts[c]/totalFloorspaceConsumption;
					ZoneQuantityStorage fsi = (ZoneQuantityStorage) floorspaceInventory.get(cf.commodityAt(c).getName());
					int[] alphaZones = floorspaceZoneCrossref.getAlphaExternals0Based();
					int alphaCount = 0;
					for (int alphaZoneNumber : alphaZones) {
						int betaZone = floorspaceZoneCrossref.getBetaZone(alphaZoneNumber);
						if (zones[betaZoneIndexNumber].zoneUserNumber== betaZone) {
							alphaCount++;
							floorspaceTotal += fsi.getQuantityForZone(alphaZoneNumber);
						}
					}
					if (floorspaceTotal==0) {
						// this means that there is no space *of*this*type* in this zone, but
						// with import and export functions specified the activity could still be consuming
						// minimal amounts of this space anyways.  So divide up the amount equaly across TAZ's
						for (int alpha2 : alphaZones) {
							int betaZoneNumber = floorspaceZoneCrossref.getBetaZone(alpha2);
							if (zones[betaZoneIndexNumber].zoneUserNumber== betaZoneNumber) {
								activityLocationsSplit[alpha2]+=activityAmountForCommodity/alphaCount;
							}
						}

					} else {
						for (int alpha2: alphaZones) {
							int betaZoneNumber = floorspaceZoneCrossref.getBetaZone(alpha2);
							if (zones[betaZoneIndexNumber].zoneUserNumber== betaZoneNumber) {
								double inventoryTemp = fsi.getQuantityForZone(alpha2);
								activityLocationsSplit[alpha2]+=activityAmountForCommodity*inventoryTemp/floorspaceTotal;
							}
						}
					} //end for for writing TAZs

				} // endif for checking floorspace commodity type
			} //endif for checking to see if commodity is null
		}//endfor for iterating through commodities
	}

	private void splitActivityTotalEquallyAcrossTAZs(
			double[] activityLocationsSplit, int betaZoneIndexNumber,
			double pecasZoneTotal) {
		int alphaCount = 0;
		int[] alphaZones = floorspaceZoneCrossref.getAlphaExternals0Based();
		for (int alphaZone : alphaZones) {
			int betaZoneNumber = floorspaceZoneCrossref.getBetaZone(alphaZone);
			if (zones[betaZoneIndexNumber].zoneUserNumber== betaZoneNumber) {
				alphaCount++;
			}
		}
		for (int alphaZone : alphaZones) {
			int betaZoneNumber = floorspaceZoneCrossref.getBetaZone(alphaZone);
			if (zones[betaZoneIndexNumber].zoneUserNumber== betaZoneNumber) {
				activityLocationsSplit[alphaZone]+=pecasZoneTotal/alphaCount;
			}
		}
	}

}
