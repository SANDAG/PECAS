/*
 * Created on 28-Oct-2005
 * 
 * Copyright 2006 HBA Specto Incorporated
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.hbaspecto.pecas.sd;

import junit.framework.TestCase;

public class ZoningRegulationTest
        extends TestCase
{

    public static void main(String[] args)
    {
        junit.textui.TestRunner.run(ZoningRegulationTest.class);
    }

    /*
     * Test method for
     * 'com.pb.models.pecas.ld.ZoningRegulation.setMaxFAR(double)'
     */
    /*
     * 
     * 
     * public void testSetMaxFAR() { ZoningPermissions z = new
     * ZoningPermissions(4,0,1,0); z.setMaxFAR(3); try { z.setMaxFAR(1.01);
     * throw new
     * Exception("Shoudln't be able to set a FAR this low, need a reasonable range"
     * ); } catch (Exception e) { };
     * 
     * 
     * }
     */
    /*
     * Test method for
     * 'com.pb.models.pecas.ld.ZoningRegulation.setMinMaxFAR(double, double)'
     */
    public void testSetMinMaxFAR()
    {
        /*
         * ZoningPermissions z = new ZoningPermissions(4,0,1,0);
         * z.setMinMaxFAR(1,33); try { z.setMinMaxFAR(5,4); throw new Exception(
         * "Shoudln't be able to set a FAR this low, need a reasonable range");
         * } catch (Exception e) {};
         */

    }

    /*
     * Test method for
     * 'com.pb.models.pecas.ld.ZoningRegulation.setMinFAR(double)'
     */
    public void testSetMinFAR()
    {
        /*
         * ZoningPermissions z = new ZoningPermissions(4,0,1,0);
         * z.setMinFAR(1.2); try { z.setMinFAR(3.99); throw new Exception(
         * "Shoudln't be able to set a FAR this low, need a reasonable range");
         * } catch (Exception e) { }; try { z.setMinFAR(4.05); throw new
         * Exception
         * ("Shoudln't be able to set a FAR this low, need a reasonable range");
         * } catch (Exception e) { };
         */
    }

}
