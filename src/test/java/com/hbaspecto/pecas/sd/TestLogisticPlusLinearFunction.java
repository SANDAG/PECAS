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

import static org.junit.Assert.assertTrue;
import org.junit.BeforeClass;
import org.junit.Test;
import com.hbaspecto.functions.LogisticPlusLinearFunction;

public class TestLogisticPlusLinearFunction
{

    @BeforeClass
    public static void setUpBeforeClass() throws Exception
    {
        // nothing
    }

    @Test
    public final void testEvaluate()
    {
        final LogisticPlusLinearFunction lplf = new LogisticPlusLinearFunction(1200000, 1, 2,
                391507.6, 15225.29);
        final double midpoint = lplf.evaluate(1);
        assertTrue(midpoint > 1199999.9999 && midpoint < 1200000.00001);
        double x = -30;
        double oldValue = lplf.evaluate(x);
        while (x < 50)
        {
            x += .01;
            final double newValue = lplf.evaluate(x);
            assertTrue("Should increase at least at rate of slope at" + x,
                    (newValue - oldValue) / .01 >= 15225.28);
            oldValue = newValue;
        }
    }

    @Test
    public final void testDerivative()
    {
        final LogisticPlusLinearFunction lplf = new LogisticPlusLinearFunction(1200000, 1, 2,
                391507.6, 15225.29);
        double x = -30;
        double oldValue = lplf.evaluate(x);
        while (x < 50)
        {
            x += .01;
            final double newValue = lplf.evaluate(x);
            final double derivative = lplf.derivative(x - .01 / 2);
            final double numericalDerivative = (newValue - oldValue) / .01;
            assertTrue("Numerical derivative " + numericalDerivative + " should equal derivative "
                    + derivative + " at " + x, Math.abs(numericalDerivative - derivative) < 5);
            oldValue = newValue;
        }
    }

}
