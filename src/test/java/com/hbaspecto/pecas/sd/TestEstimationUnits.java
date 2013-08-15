package com.hbaspecto.pecas.sd;

import org.junit.Ignore;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@Ignore
// TODO: Fix test
@RunWith(Suite.class)
@SuiteClasses({ TestDevelopmentAlternativeEstimationMethods.class,
		TestDevelopNewAlternative.class, TestDevelopMoreAlternative.class,
		TestEasyAlternatives.class, TestExpectedValueFormula.class,
		//TestGaussBayesianObjective.class, TestMarquardtMinimizer.class 
		})
public class TestEstimationUnits {

}
