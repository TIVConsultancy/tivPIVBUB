/*
 * Copyright 2020 TIVConsultancy.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.tivconsultancy.tivpivbub;

import com.tivconsultancy.opentiv.helpfunctions.settings.SettingObject;
import com.tivconsultancy.opentiv.helpfunctions.settings.Settings;
import com.tivconsultancy.tivGUI.StaticReferences;
import com.tivconsultancy.tivpiv.PIVController;
import com.tivconsultancy.tivpiv.tivPIVSubControllerSQL;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author TZ ThomasZiegenhein@TIVConsultancy.com +1 480 494 7254
 */
public class tivPIVBUBSubControllerSQL extends tivPIVSubControllerSQL {

    @Override
    public String connect(String user, String password, String database, String host) {
        super.connect(user, password, database, host);
        Settings hints = ((PIVBUBController) StaticReferences.controller).getHintsSettings();
        hints.removeSettings("sql_evalsettingsbub");
        List<String> availSettingsBUB = getColumnEntries("flowdata", "evalsettingsbub", "ident");
        for (String s : availSettingsBUB) {
            hints.addSettingsObject(new SettingObject("Settings BUB", "sql_evalsettingsbub", s, SettingObject.SettingsType.String));
        }
        return sqlData.getStatus();
    }

    public void setTimeStamp(double t) {
        this.t = (float) t;
    }

    public int insertEntryBUB(sqlEntryBUB ent) {
        return getDatabase(null).performStatement(getinsertEntryBUB(ent));
    }

    public void insertEntryBUB(List<sqlEntryBUB> ent) {
        List<String> entriesSQL = new ArrayList<>();
        for (sqlEntryBUB e : ent) {
            entriesSQL.add(getinsertEntryBUB(e));
        }
        getDatabase(null).performStatements(entriesSQL);
    }

    public int upsertEntryBUB(sqlEntryBUB ent) {
        return getDatabase(null).performStatement(getupserEntryBUB(ent));
    }

    public void upsertEntryBUB(List<sqlEntryBUB> ent) {
        List<String> entriesSQL = new ArrayList<>();
        for (sqlEntryBUB e : ent) {
            entriesSQL.add(getupserEntryBUB(e));
        }
        getDatabase(null).performStatements(entriesSQL);
    }

    public String getinsertEntryBUB(sqlEntryBUB e) {
        String sqlStatement = "INSERT INTO flowdata.bubvelo (experiment, settings,timestampexp, posx, posy, posz, velox, veloy, majoraxis, minoraxis, orientation,greyderivative) "
                + "VALUES('" + e.experiment + "', '" + e.settingsName + "', " + t + ", " + e.posX + ", " + e.posY + ", " + e.posZ + ", " + e.vX + ", " + e.vY+ ", "  + e.majorAxis 
                + ", " + e.minorAxis + ", " + e.orientation+ ", "+e.greyderivative+")";
        return sqlStatement;
    }

    public String getupserEntryBUB(sqlEntryBUB e) {
        if (Double.isNaN(e.majorAxis)||Double.isNaN(e.minorAxis)){
            throw new IllegalArgumentException("NaN major/minor axis");
        }
        String sqlStatement = "INSERT INTO flowdata.bubvelo (experiment, settings, timestampexp, posx, posy, posz, velox, veloy, majoraxis, minoraxis, orientation,greyderivative) "
                + "VALUES('" + e.experiment + "', '" + e.settingsName + "', " + t + ", " + e.posX + ", " + e.posY + ", " + e.posZ + ", " + e.vX + ", " + e.vY + ", " + e.majorAxis 
                + ", " + e.minorAxis + ", " + e.orientation+ ", "+e.greyderivative+ ")"
                + "ON CONFLICT (experiment, settings, timestampexp, posx, posy, posz) DO UPDATE SET "
                + "experiment = EXCLUDED.experiment, "
                + "settings = EXCLUDED.settings,"
                + "timestampexp = EXCLUDED.timestampexp, "
                + "posx = EXCLUDED.posx, "
                + "posy = EXCLUDED.posy, "
                + "posz = EXCLUDED.posz, "
                + "velox = EXCLUDED.velox, "
                + "veloy = EXCLUDED.veloy, "
                + "majoraxis = EXCLUDED.majoraxis, "
                + "minoraxis = EXCLUDED.minoraxis, "
                + "orientation = EXCLUDED.orientation, "               
                + "greyderivative = EXCLUDED.greyderivative";
        return sqlStatement;
    }

    public static class sqlEntryBUB {

        String experiment;
        String settingsName;
        double posX;
        double posY;
        double posZ;
        double vX;
        double vY;
        double majorAxis;
        double minorAxis;
        double orientation;
        int greyderivative;
//        int burstnumber;

        public sqlEntryBUB(String experiment, String settingsName, double posX, double posY, double posZ, double vX, double vY, double majorAxis, double minorAxis, 
                double orientation,int greyderivative) {
            this.experiment = experiment;
            this.settingsName = settingsName;
            this.posX = posX;
            this.posY = posY;
            this.posZ = posZ;
            this.vX = vX;
            this.vY = vY;
            this.majorAxis = majorAxis;
            this.minorAxis = minorAxis;
            this.orientation = orientation;
            this.greyderivative=greyderivative;
//            this.burstnumber = burstnumber;
        }

    }

}
