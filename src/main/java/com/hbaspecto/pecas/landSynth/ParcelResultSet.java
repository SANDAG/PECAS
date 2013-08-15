/*
 * Created on Dec 16, 2003
 *
 *
 * Copyright  2003 HBA Specto Incorporated
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
package com.hbaspecto.pecas.landSynth;

import java.sql.SQLException;
import java.sql.ResultSet;

/**
 * @author jabraham
 *
 */
public class ParcelResultSet {
    
  
    public ParcelResultSet(ResultSet set, String zoneColumnName, String areaColumnName, String initialFARColumnName){
        mySet = set;
        this.zoneColumnName = zoneColumnName;
        this.areaColumnName = areaColumnName;
        this.initialFARColumnName=initialFARColumnName;
    }
    
    final String zoneColumnName;
    final String areaColumnName;
    final String initialFARColumnName;

    private final ResultSet mySet;
    public ResultSet getSet() {
        return mySet;
    }

    private int tazColumn = -1;
    private int areaColumn = -1;
    private int sqFtColumn = -1;
    private int pecasTypeColumn = -1;
    private int initialFARColumn = -1;

    int getTazColumn() throws SQLException {
        if (tazColumn<0) {
            tazColumn = mySet.findColumn(zoneColumnName); 
        }
        return tazColumn;
    }

    int getAreaColumn() throws SQLException {
        if (areaColumn<0) {
            areaColumn = mySet.findColumn(areaColumnName); 
        }
        return areaColumn;
    }
    int getInitialFARColumn() throws SQLException {
        if (initialFARColumnName==null) return -1;
        if (initialFARColumn<0) {
            initialFARColumn = mySet.findColumn(initialFARColumnName); 
        }
        return initialFARColumn;
    }
    int getSqFtColumn() throws SQLException {
        if (sqFtColumn<0) {
            sqFtColumn = mySet.findColumn("PECASSQFT"); 
        }
        return sqFtColumn;
    }
    int getPecasTypeColumn() throws SQLException {
        if (pecasTypeColumn<0) {
            pecasTypeColumn = mySet.findColumn("PECASTYPE"); 
        }
        return pecasTypeColumn;
    }
    
    void close() throws SQLException {
        mySet.close();
    }
}
