package com.hbaspecto.pecas.aa.control;

import java.util.List;

import com.hbaspecto.pecas.NoAlternativeAvailable;
import com.hbaspecto.pecas.OverflowException;
import com.hbaspecto.pecas.aa.activities.ProductionActivity;
import com.hbaspecto.pecas.aa.commodity.AbstractCommodity;
import com.hbaspecto.pecas.aa.commodity.Commodity;
import com.hbaspecto.pecas.aa.technologyChoice.ConsumptionFunction;
import com.hbaspecto.pecas.aa.technologyChoice.ProductionFunction;
import com.hbaspecto.pecas.zones.AbstractZone;

public class FakeProductionActivity extends ProductionActivity {

	protected FakeProductionActivity(String name, AbstractZone[] allZones) {
		super(name, allZones);
		// TODO Auto-generated constructor stub
	}

	@Override
	public void migrationAndAllocation(double timeStep, double inMigration,
			double outMigration) throws OverflowException {
		// TODO Auto-generated method stub

	}

	@Override
	public void reMigrationAndReAllocation() throws CantRedoError {
		// TODO Auto-generated method stub

	}

	@Override
	public double getUtility() throws OverflowException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public ProductionFunction getProductionFunction() {
		return new ProductionFunction() {

			@Override
			public int size() {
				return Commodity.getAllCommodities().size();
			}

			@Override
			public void doFinalSetupAndSetCommodityOrder(List commodityList) {
				// TODO Auto-generated method stub

			}

			@Override
			public AbstractCommodity commodityAt(int i) {
				return Commodity.getAllCommodities().get(i);
			}

			@Override
			public double[] calcAmounts(double[] buyingZUtilities,
					double[] sellingZUtilities, int zoneIndex)
					throws NoAlternativeAvailable {
				double[] amounts = new double[buyingZUtilities.length];
				for (int i = 0; i < amounts.length; i++)
					amounts[i] = .5 * (double) i;

				return amounts;
			}
		};
	}

	@Override
	public ConsumptionFunction getConsumptionFunction() {
		return new ConsumptionFunction() {

			@Override
			public int size() {
				return Commodity.getAllCommodities().size();
			}

			@Override
			public void doFinalSetupAndSetCommodityOrder(List commodityList) {
				// TODO Auto-generated method stub

			}

			@Override
			public AbstractCommodity commodityAt(int i) {
				return Commodity.getAllCommodities().get(i);
			}

			@Override
			public double[] calcAmounts(double[] buyingZUtilities,
					double[] sellingZUtilities, int zoneIndex)
					throws NoAlternativeAvailable {
				double[] amounts = new double[buyingZUtilities.length];
				for (int i = 0; i < amounts.length; i++)
					amounts[i] = .5 * (double) i;

				return amounts;
			}
		};
	}

	@Override
	public void checkConstraintConsistency() {
		// TODO Auto-generated method stub

	}

}
