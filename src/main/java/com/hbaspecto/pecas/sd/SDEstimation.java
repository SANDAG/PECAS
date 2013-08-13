package com.hbaspecto.pecas.sd;

import java.util.ResourceBundle;

import simpleorm.sessionjdbc.SSessionJdbc;

import com.hbaspecto.pecas.land.Parcels;
import com.hbaspecto.pecas.land.PostgreSQLLandInventory;
import com.hbaspecto.pecas.land.SimpleORMLandInventory;
import com.hbaspecto.pecas.sd.ZoningRulesI;
import com.hbaspecto.pecas.sd.estimation.CSVEstimationReader;
import com.pb.common.util.ResourceUtil;

public class SDEstimation {

    /**
     * @param args
     */
    public static void main(String[] args) throws Exception {
        try {
            ZoningRulesI.baseYear = Integer.valueOf(args[0]);
            ZoningRulesI.currentYear = Integer.valueOf(args[0]) + Integer.valueOf(args[1]);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(
                    "Put base year and time interval on command line"
                    + "\n For example, 1990 1");
        }

    	ResourceBundle rb = ResourceUtil.getResourceBundle("sd");
		StandardSDModel.initOrm(rb);
        

        double epsilon = ResourceUtil.getDoubleProperty(rb, "EstimationConvergence", 1E-4);
        int maxits = ResourceUtil.getIntegerProperty(rb, "EstimationMaxIterations", 1);
        String parameters = ResourceUtil.checkAndGetProperty(rb, "EstimationParameterFile");
        String targets = ResourceUtil.checkAndGetProperty(rb, "EstimationTargetFile");
        
        boolean paramsDiag = ResourceUtil.getBooleanProperty(rb, "EstimationParameterVarianceAsDiagonal", false);
        boolean targetsDiag = ResourceUtil.getBooleanProperty(rb, "EstimationTargetVarianceAsDiagonal", false);
        
        StandardSDModel sd = new StandardSDModel();
        CSVEstimationReader csv = new CSVEstimationReader(targets, targetsDiag, parameters, paramsDiag);
        
        sd.calibrateModel(csv, ZoningRulesI.baseYear, ZoningRulesI.currentYear, epsilon, maxits);
        // There are often threads hanging open at the end, for unknown reasons.
        // TODO figure out the extra threads hanging around
        System.exit(0);
    }
}
