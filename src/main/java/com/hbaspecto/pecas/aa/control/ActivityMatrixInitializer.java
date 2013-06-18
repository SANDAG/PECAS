/**
 * 
 */
package com.hbaspecto.pecas.aa.control;

import com.hbaspecto.models.FutureObject;
import com.hbaspecto.pecas.ChoiceModelOverflowException;
import com.hbaspecto.pecas.NoAlternativeAvailable;
import com.hbaspecto.pecas.aa.activities.AggregateActivity;
import com.hbaspecto.pecas.aa.activities.AggregateDistribution;
import com.hbaspecto.pecas.zones.AbstractZone;

import drasys.or.linear.algebra.Algebra;
import drasys.or.linear.algebra.AlgebraException;
import drasys.or.matrix.DenseMatrix;
import drasys.or.matrix.DenseVector;
import drasys.or.matrix.VectorI;

class ActivityMatrixInitializer implements Runnable {

	final AggregateActivity activity;

	FutureObject done = new FutureObject();
	final double[][] dStorage;

	ActivityMatrixInitializer(AggregateActivity actParam,
			double[][] tempStorageParam) {
		activity = actParam;
		dStorage = tempStorageParam;
	}

	@Override
	public void run() {
		// build up relationship between average commodity price and total
		// surplus
		DenseVector pl; // P(z|a) in new documentation
		DenseMatrix fpl;
		try {
			pl = new DenseVector(
					activity.logitModelOfZonePossibilities.getChoiceProbabilities());
			fpl = new DenseMatrix(
					activity.logitModelOfZonePossibilities.choiceProbabilityDerivatives());

		}
		catch (final ChoiceModelOverflowException e) {
			e.printStackTrace();
			done.setValue(e);
			throw new RuntimeException("Can't solve for amounts in zone", e);
		}
		catch (final NoAlternativeAvailable e) {
			e.printStackTrace();
			done.setValue(e);
			throw new RuntimeException("Can't solve for amounts in zone", e);
		}
		// dulbydprice is derivative of location utility wrt changes in average
		// prices of commodites
		// is d(LU(a,z)/d(Price(bar)(c)) in new notation
		final DenseMatrix dulbydprice = new DenseMatrix(fpl.sizeOfColumns(),
				AveragePriceSurplusDerivativeMatrix.numCommodities);
		final int[] rows = new int[1];
		final int[] columns = new int[AveragePriceSurplusDerivativeMatrix.numCommodities];
		final double[][] valuesToAdd = new double[1][];
		for (int col = 0; col < AveragePriceSurplusDerivativeMatrix.numCommodities; col++) {
			columns[col] = col;
		}
		for (int location = 0; location < pl.size(); location++) {
			rows[0] = location;
			final AggregateDistribution l = (AggregateDistribution) activity.logitModelOfZonePossibilities
					.alternativeAt(location);
			valuesToAdd[0] = l.calculateLocationUtilityWRTAveragePrices();
			// dulbydprice.set(rows,columns,valuesToAdd);
			dulbydprice.setRow(location, new DenseVector(valuesToAdd[0]));
		}
		DenseMatrix dLocationByDPrice = new DenseMatrix(
				AbstractZone.getAllZones().length,
				AveragePriceSurplusDerivativeMatrix.numCommodities);
		final Algebra a = new Algebra();
		try {
			// fpl.mult(dulbydprice,dLocationByDPrice);
			dLocationByDPrice = a.multiply(fpl, dulbydprice);
			// ENHANCEMENT remove this debug code to speed things up
			for (int r1 = 0; r1 < dLocationByDPrice.sizeOfRows(); r1++) {
				for (int c1 = 0; c1 < dLocationByDPrice.sizeOfColumns(); c1++) {
					if (Double.isNaN(dLocationByDPrice.elementAt(r1, c1))) {
						AveragePriceSurplusDerivativeMatrix.logger
								.fatal("NaN in dLocationByDPrice, writing matrices to console");
						AAModel.writeOutMatrix(dLocationByDPrice, "dLocationByDPrice");
						AAModel.writeOutMatrix(fpl, "zoneChoiceWRTUtility");
						AAModel.writeOutMatrix(dulbydprice, "utiltiyWRTPrices");
						final RuntimeException e = new RuntimeException(
								"NaN in dLocationByDPrice, printing matrices to console");
						done.setValue(e);
						throw e;

					}
				}
			}
		}
		catch (final AlgebraException e1) {
			e1.printStackTrace();
			done.setValue(e1);
			throw new RuntimeException(
					"Can't multiply matrices to figure out average price surplus", e1);
		}
		for (int location = 0; location < pl.size(); location++) {
			final VectorI dThisLocationByPrices = new DenseVector(
					AveragePriceSurplusDerivativeMatrix.numCommodities);
			for (int i = 0; i < dThisLocationByPrices.size(); i++) {
				dThisLocationByPrices.setElementAt(i,
						dLocationByDPrice.elementAt(location, i));
			}
			final AggregateDistribution l = (AggregateDistribution) activity.logitModelOfZonePossibilities
					.alternativeAt(location);
			l.addTwoComponentsOfDerivativesToAveragePriceMatrix(
					activity.getTotalAmount(), dStorage, dThisLocationByPrices);
			// System.out.println();
		}
		done.setValue(new Boolean(true));
	}

}