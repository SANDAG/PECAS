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
/*
 * Created on Jan 4, 2005
 *
 */
package com.hbaspecto.pecas.aa.control;

import drasys.or.matrix.DenseVector;

import java.util.Iterator;

import com.hbaspecto.pecas.aa.commodity.Commodity;
import com.hbaspecto.pecas.aa.commodity.Exchange;

/**
 * @author jabraham
 *
 * To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public class TotalSurplusVector extends DenseVector {

    public TotalSurplusVector() {
        super(Commodity.getAllCommodities().size());
        int commodityIndex =0;
        Iterator comIt = Commodity.getAllCommodities().iterator();
        while (comIt.hasNext()) {
            double surplus = 0;
            Commodity c = (Commodity) comIt.next();
            Iterator exIt = c.getAllExchanges().iterator();
            while (exIt.hasNext()) {
                surplus += ((Exchange) exIt.next()).exchangeSurplus();
            }
            setElementAt(commodityIndex,surplus);
            commodityIndex++;
        }
    }


}
