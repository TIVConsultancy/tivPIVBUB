/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.tivconsultancy.tivpivbub.protocols;

import com.tivconsultancy.opentiv.helpfunctions.io.Writer;
import com.tivconsultancy.opentiv.helpfunctions.settings.SettingObject;
import com.tivconsultancy.opentiv.helpfunctions.settings.SettingsCluster;
import com.tivconsultancy.opentiv.highlevel.protocols.NameSpaceProtocolResults1D;
import com.tivconsultancy.opentiv.highlevel.protocols.UnableToRunException;
import com.tivconsultancy.opentiv.imageproc.primitives.ImageInt;
import com.tivconsultancy.opentiv.imageproc.shapes.Circle;
import com.tivconsultancy.opentiv.math.algorithms.Averaging;
import com.tivconsultancy.opentiv.math.interfaces.Value;
import com.tivconsultancy.opentiv.math.primitives.Vector;
import com.tivconsultancy.opentiv.math.specials.LookUp;
import com.tivconsultancy.opentiv.math.specials.NameObject;
import com.tivconsultancy.opentiv.physics.vectors.VelocityVec;
import com.tivconsultancy.opentiv.velocimetry.helpfunctions.VelocityGrid;
import com.tivconsultancy.tivGUI.StaticReferences;
import com.tivconsultancy.tivpiv.PIVController;
import com.tivconsultancy.tivpiv.PIVMethod;
import com.tivconsultancy.tivpiv.data.DataPIV;
import com.tivconsultancy.tivpiv.protocols.PIVProtocol;
import com.tivconsultancy.tivpiv.tivPIVSubControllerSQL;
import com.tivconsultancy.tivpiv.tivPIVSubControllerSQL.sqlEntryPIV;
import com.tivconsultancy.tivpivbub.PIVBUBController;
import com.tivconsultancy.tivpivbub.data.DataBUB;
import com.tivconsultancy.tivpivbub.tivPIVBUBSubControllerSQL;
import com.tivconsultancy.tivpivbub.tivPIVBUBSubControllerSQL.sqlEntryBUB;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

/**
 *
 * @author TZ ThomasZiegenhein@TIVConsultancy.com +1 480 494 7254
 */
public class Prot_tivPIVBUBDataHandling extends PIVProtocol {

    private static final long serialVersionUID = -2277348339833199755L;

    private String name = "DataExport";
    LookUp<Double> results1D = new LookUp<>();

    public Prot_tivPIVBUBDataHandling() {
        super();
        buildLookUp();
        initSettins();
        buildClusters();
    }

    private void buildLookUp() {
    }

    @Override
    public NameSpaceProtocolResults1D[] get1DResultsNames() {
        return NameSpaceProtocol1DResults.class.getEnumConstants();
    }

    @Override
    public List<String> getIdentForViews() {
        return Arrays.asList(new String[]{});
    }

    @Override
    public void setImage(BufferedImage bi) {
        buildLookUp();
    }

    @Override
    public Double getOverTimesResult(NameSpaceProtocolResults1D ident) {
        return results1D.get(ident.toString());
    }

    @Override
    public void run(Object... input) throws UnableToRunException {

        DataBUB dataBUB = ((PIVBUBController) StaticReferences.controller).getDataBUB();
        String expName = ((PIVMethod) StaticReferences.controller.getCurrentMethod()).experimentSQL;
        if (expName == null) {
            expName = getSettingsValue("sql_experimentident").toString();
        }

        String settingsPIVNamePIV = getSettingsValue("sql_evalsettingspiv").toString();
        String settingsPIVNameBUB = getSettingsValue("sql_evalsettingsbub").toString();
        PIVBUBController controller = ((PIVBUBController) StaticReferences.controller);

        int refPX_X = Integer.valueOf(getSettingsValue("data_refposPXX").toString());
        if ((boolean) controller.getCurrentMethod().getProtocol("preproc").getSettingsValue("BcutxLeft")) {
            int iCutLeft = Integer.valueOf(controller.getCurrentMethod().getProtocol("preproc").getSettingsValue("cutxLeft").toString());
            refPX_X = refPX_X - iCutLeft;
        }
        double refM_X = Double.valueOf(getSettingsValue("data_refposMX").toString());
        int refPX_Y = Integer.valueOf(getSettingsValue("data_refposPXY").toString());
        if ((boolean) controller.getCurrentMethod().getProtocol("preproc").getSettingsValue("BcutyTop")) {
            int iCutTop = Integer.valueOf(controller.getCurrentMethod().getProtocol("preproc").getSettingsValue("cutyTop").toString());
            refPX_Y = refPX_Y - iCutTop;
        }
        double refM_Y = Double.valueOf(getSettingsValue("data_refposMY").toString());
        double refM_Z = Double.valueOf(getSettingsValue("data_refposMZ").toString());
        double fps = Integer.valueOf(getSettingsValue("data_FPS").toString());
        boolean bUPSERT = Boolean.valueOf(getSettingsValue("sql_upsert").toString());
        tivPIVBUBSubControllerSQL sql_Control = (tivPIVBUBSubControllerSQL) StaticReferences.controller.getSQLControler(null);
        sql_Control.setTimeStamp(getTimeStamp());
//        int iBurstNumber = getBurstNumber();
        double dResolution = Double.valueOf(getSettingsValue("data_Resolution").toString()) / 1000000.0;
        String sExportPath = getSettingsValue("data_csvExportPath").toString();

        try {
            if (Boolean.valueOf(this.getSettingsValue("sql_activation").toString())) {
                System.out.println("Uploading Data to " + expName);
//                dResolution = sql_Control.getResolution(expName) / 1000000.0;
//                double dResolution = sql_Control.getResolution(expName) / 100000.0;
                if ((boolean) controller.getCurrentMethod().getProtocol("inter areas").getSettingsValue("PIV_Interrogation") == true) {
                    DataPIV dataPIV = ((PIVBUBController) StaticReferences.controller).getDataPIV();
                    List<sqlEntryPIV> entriesPIV = new ArrayList<>();
                    for (Vector v : dataPIV.oGrid.getVectors()) {
                        double dPosX = (v.getPosX() - refPX_X) * dResolution + refM_X;
                        double dPosY = refM_Y - (v.getPosY() - refPX_Y) * dResolution;
                        double dPosZ = refM_Z;
                        double dVX = v.getX() * dResolution * fps;
                        double dVY = -1.0 * v.getY() * dResolution * fps;
                        entriesPIV.add(new sqlEntryPIV(expName, settingsPIVNamePIV, dPosX, dPosY, dPosZ, dVX, dVY));
                    }
                    if (bUPSERT) {
                        sql_Control.upsertEntry(entriesPIV);
                    } else {
                        sql_Control.insertEntry(entriesPIV);
                    }
                }
                List<sqlEntryBUB> entriesBUB = new ArrayList<>();
                if ((boolean) controller.getCurrentMethod().getProtocol("boundtrack").getSettingsValue("BoundTrack")||(boolean) controller.getCurrentMethod().getProtocol("boundtrack").getSettingsValue("SimpleTracking")) {
                    for (Map.Entry<Circle, VelocityVec> m : dataBUB.results.entrySet()) {
                        double dPosX = (m.getValue().getPosX() - refPX_X) * dResolution + refM_X;
                        double dPosY = refM_Y - (m.getValue().getPosY() - refPX_Y) * dResolution;
                        double dPosZ = refM_Z;
                        double dVX = m.getValue().getX() * dResolution * fps;
                        double dVY = -1.0 * m.getValue().getY() * dResolution * fps;
                        double majorAxis = Math.max(m.getKey().dDiameterI, m.getKey().dDiameterJ) * dResolution;
                        double minorAxis = Math.min(m.getKey().dDiameterI, m.getKey().dDiameterJ) * dResolution;
                        double orientaton = m.getKey().dAngle;
                        int AverageGreyDeri = (int) (double) m.getKey().dAvergeGreyDerivative;
                        entriesBUB.add(new sqlEntryBUB(expName, settingsPIVNameBUB, dPosX, dPosY, dPosZ, dVX, dVY, majorAxis, minorAxis, orientaton, AverageGreyDeri));
                    }
                } else {
                    for (Circle m : dataBUB.results_EFit.loCircles) {
                        double dPosX = (m.meCenter.j - refPX_X) * dResolution + refM_X;
                        double dPosY = refM_Y - (m.meCenter.i - refPX_Y) * dResolution;
                        double dPosZ = refM_Z;
                        double dVX = 0.0;
                        double dVY = 0.0;
                        double majorAxis = Math.max(m.dDiameterI, m.dDiameterJ) * dResolution;
                        double minorAxis = Math.min(m.dDiameterI, m.dDiameterJ) * dResolution;
                        double orientaton = m.dAngle;
                        int AverageGreyDeri = (int) (double) m.dAvergeGreyDerivative;
                        entriesBUB.add(new sqlEntryBUB(expName, settingsPIVNameBUB, dPosX, dPosY, dPosZ, dVX, dVY, majorAxis, minorAxis, orientaton, AverageGreyDeri));

                    }
                }
                if (bUPSERT) {
                    sql_Control.upsertEntryBUB(entriesBUB);
                } else {
                    sql_Control.insertEntryBUB(entriesBUB);
                }

//            for(Vector v : data.oGrid.getVectors()){
//                double dPosX = (v.getPosX() - refPX_X)*dResolution + refM_X;
//                double dPosY = (v.getPosY() - refPX_Y)*dResolution + refM_Y;
//                double dPosZ = refM_Z;
//                double dVX = v.getX() * dResolution;
//                double dVY = v.getY() * dResolution;
//                if(bUPSERT){
//                    sql_Control.upsertEntry(new sqlEntryPIV(expName, settingsPIVName, dPosX, dPosY, dPosZ, dVX, dVY));
//                }else{
//                    sql_Control.insertEntry(new sqlEntryPIV(expName, settingsPIVName, dPosX, dPosY, dPosZ, dVX, dVY));
//                }
//            }
            }
        } catch (Exception e) {
            StaticReferences.getlog().log(Level.SEVERE, "Error writing to SQL database", e);
        }

        try {
            if (Boolean.valueOf(this.getSettingsValue("data_csvExport").toString())) {
//                mache export csv
                List<String[]> lsOut = new ArrayList<>();
                int time = (int) getTimeStamp();
                if ((boolean) controller.getCurrentMethod().getProtocol("inter areas").getSettingsValue("PIV_Interrogation") == true) {
                    DataPIV dataPIV = ((PIVBUBController) StaticReferences.controller).getDataPIV();
                    for (Vector v : dataPIV.oGrid.getVectors()) {
                        double dPosX = (v.getPosX() - refPX_X) * dResolution + refM_X;
                        double dPosY = (v.getPosY() - refPX_Y) * dResolution + refM_Y;
                        double dPosYPx = v.getPosY();
                        double dPosXPx = v.getPosX();
                        double dPosZ = refM_Z;
                        double dVX = v.getX() * dResolution * fps;
                        double dVY = -1.0 * v.getY() * dResolution * fps;
                        String[] sOut = new String[7];
                        sOut[0] = String.valueOf(dPosX);
                        sOut[1] = String.valueOf(dPosY);
                        sOut[2] = String.valueOf(dPosZ);
                        sOut[3] = String.valueOf(dVX);
                        sOut[4] = String.valueOf(dVY);
                        sOut[5] = String.valueOf(dPosXPx);
                        sOut[6] = String.valueOf(dPosYPx);
                        lsOut.add(sOut);
                    }
                    lsOut.add(0, new String[]{"posx", "posy", "velox", "veloy", "posxPx", "posyPx"});
                    Writer oWrite = new Writer(sExportPath + System.getProperty("file.separator") + "LiqVelo" + time + ".csv");
                    oWrite.writels(lsOut, ",");
                    lsOut.clear();
                }
                if ((boolean) controller.getCurrentMethod().getProtocol("boundtrack").getSettingsValue("BoundTrack")) {
                    for (Map.Entry<Circle, VelocityVec> m : dataBUB.results.entrySet()) {
                        double dPosX = (m.getValue().getPosX() - refPX_X) * dResolution + refM_X;
                        double dPosY = (m.getValue().getPosY() - refPX_Y) * dResolution + refM_Y;
                        double dPosZ = refM_Z;
                        double dVX = m.getValue().getX() * dResolution * fps;
                        double dVY = -1.0 * m.getValue().getY() * dResolution * fps;
                        double majorAxis = Math.max(m.getKey().dDiameterI, m.getKey().dDiameterJ) * dResolution;
                        double minorAxis = Math.min(m.getKey().dDiameterI, m.getKey().dDiameterJ) * dResolution;
                        double orientaton = m.getKey().dAngle;
                        String[] sOut = new String[9];
                        sOut[0] = String.valueOf(dPosX);
                        sOut[1] = String.valueOf(dPosY);
                        sOut[2] = String.valueOf(dPosZ);
                        sOut[3] = String.valueOf(dVX);
                        sOut[4] = String.valueOf(dVY);
                        sOut[5] = String.valueOf(majorAxis);
                        sOut[6] = String.valueOf(minorAxis);
                        sOut[7] = String.valueOf(orientaton);
                        sOut[8] = String.valueOf(Math.pow(8 * (majorAxis / 2.0) * (majorAxis / 2.0) * (minorAxis / 2.0), 1.0 / 3.0));
                        lsOut.add(sOut);
                    }
                } else {
                    for (Circle m : dataBUB.results_EFit.loCircles) {
                        double dPosX = (m.meCenter.j - refPX_X) * dResolution + refM_X;
                        double dPosY = refM_Y - (m.meCenter.i - refPX_Y) * dResolution;
                        double dPosZ = refM_Z;
                        double dVX = 0.0;
                        double dVY = 0.0;
                        double majorAxis = Math.max(m.dDiameterI, m.dDiameterJ) * dResolution;
                        double minorAxis = Math.min(m.dDiameterI, m.dDiameterJ) * dResolution;
                        double orientaton = m.dAngle;
                        String[] sOut = new String[9];
                        sOut[0] = String.valueOf(dPosX);
                        sOut[1] = String.valueOf(dPosY);
                        sOut[2] = String.valueOf(dPosZ);
                        sOut[3] = String.valueOf(dVX);
                        sOut[4] = String.valueOf(dVY);
                        sOut[5] = String.valueOf(majorAxis);
                        sOut[6] = String.valueOf(minorAxis);
                        sOut[7] = String.valueOf(orientaton);
                        sOut[8] = String.valueOf(Math.pow(8 * (majorAxis / 2.0) * (majorAxis / 2.0) * (minorAxis / 2.0), 1.0 / 3.0));
                        lsOut.add(sOut);

                    }
                }
                lsOut.add(0, new String[]{"posx", "posy", "posz", "velox", "veloy", "major", "minor", "orientation", "Diameter"});
                Writer oWrite2 = new Writer(sExportPath + System.getProperty("file.separator") + "BubVelo" + time + ".csv");
                oWrite2.writels(lsOut, ",");

            }
        } catch (Exception e) {
        }
//        PIVBUBController controller = ((PIVBUBController) StaticReferences.controller);
        if ((boolean) controller.getCurrentMethod().getProtocol("inter areas").getSettingsValue("PIV_Interrogation") == true) {
            run1DResults(dResolution, fps);
        }
    }

    public void run1DResults(double resolution, double fps) {
        results1D = new LookUp<>();

        DataPIV data = ((PIVController) StaticReferences.controller).getDataPIV();

        List<VelocityVec> loVec = data.oGrid.getVectors();

        double avgx = Averaging.getMeanAverage(loVec, new Value<Object>() {
            @Override
            public Double getValue(Object pParameter) {
                return ((VelocityVec) pParameter).getVelocityX();
            }
        });

        results1D.addDuplFree(new NameObject<>(NameSpaceProtocol1DResults.avgx.toString(), avgx * resolution * fps));

        double avgy = Averaging.getMeanAverage(loVec, new Value<Object>() {
            @Override
            public Double getValue(Object pParameter) {
                return ((VelocityVec) pParameter).getVelocityY();
            }
        });

        results1D.addDuplFree(new NameObject<>(NameSpaceProtocol1DResults.avgy.toString(), avgy * resolution * fps));

        List<Double> lvarX = new ArrayList<>();
        List<Double> lvarY = new ArrayList<>();
        for (VelocityVec v : loVec) {
            lvarX.add(Math.pow((v.getVelocityX() - avgx), 2));
            lvarY.add(Math.pow((v.getVelocityY() - avgy), 2));
        }

//        VelocityGrid ovelo = getVeloGrid();
//        List<Double> lvarX = new ArrayList<>();
//        for (OrderedPair[] lop : ovelo.GridVeloX.calcVariance()) {
//            for (OrderedPair op : lop) {
//                lvarX.add(op.dValue);
//            }
//        }
//        List<Double> lvarY = new ArrayList<>();
//        for (OrderedPair[] lop : ovelo.GridVeloY.calcVariance()) {
//            for (OrderedPair op : lop) {
//                lvarY.add(op.dValue);
//            }
//        }
        double varX = Averaging.getMeanAverage(lvarX, null);
        results1D.addDuplFree(new NameObject<>(NameSpaceProtocol1DResults.tkeX.toString(), varX * resolution * resolution * fps * fps));

        double varY = Averaging.getMeanAverage(lvarY, null);
        results1D.addDuplFree(new NameObject<>(NameSpaceProtocol1DResults.tkey.toString(), varY * resolution * resolution * fps * fps));

    }

    public VelocityGrid getVeloGrid() {
//        DataPIV data = ((PIVController) StaticReferences.controller).getDataPIV();
//        return data.oGrid.getVeloGrid();
        DataPIV data = ((PIVController) StaticReferences.controller).getDataPIV();
        ImageInt oSourceImage = new ImageInt(data.iaReadInFirst);
        double dSize = data.oGrid.getCellSize();
        int iOffSet = 0;
        if ("50Overlap".equals(data.sGridType)) {
            dSize = dSize / 2.0;
            iOffSet = (int) (dSize / 2.0);
        }
        VelocityGrid oOutputGrid = new VelocityGrid(iOffSet, oSourceImage.iaPixels[0].length, oSourceImage.iaPixels.length, iOffSet, (int) (oSourceImage.iaPixels[0].length / dSize), (int) (oSourceImage.iaPixels.length / dSize));

        oOutputGrid = data.oGrid.getVeloGrid(oOutputGrid, data);
        return oOutputGrid;

    }

    private int getBurstNumber() {
        int index = ((PIVController) StaticReferences.controller).getSelecedIndex();
//        DataPIV data = ((PIVController) StaticReferences.controller).getDataPIV();
        int iBurstLength = (Integer) ((PIVController) StaticReferences.controller).getCurrentMethod().getProtocol("calculate").getSettingsValue("tivPIVBurstLength");
        if (iBurstLength > 1) {
            return (index / iBurstLength);
        } else {
            return 0;
        }
    }

    private double getTimeStamp() {
        int index = ((PIVController) StaticReferences.controller).getSelecedIndex();
        DataPIV data = ((PIVController) StaticReferences.controller).getDataPIV();
        int fps = Integer.valueOf(getSettingsValue("data_FPS").toString());
        if (data.iBurstLength > 1) {
            int burstFreq = Integer.valueOf(getSettingsValue("data_BurstFreq").toString());
            double dBurstTime = ((int) (index / data.iBurstLength)) * (1.0 / burstFreq);
            double dRestTime = (index - (((int) (index / data.iBurstLength)) * data.iBurstLength)) * (1.0 / fps);
            System.out.println("Burst Time" + dBurstTime);
            System.out.println("Rest Time" + dRestTime);
//            System.out.println("Burst Number " + getBurstNumber());
            return dBurstTime + dRestTime;
        } else {
            return index * (1.0 / fps);
        }
    }

    @Override
    public Object[] getResults() {
        return new Object[0];
    }

    @Override
    public String getType() {
        return name;
    }

    private void initSettins() {
        this.loSettings.add(new SettingObject("Export->SQL", "sql_activation", false, SettingObject.SettingsType.Boolean));
        this.loSettings.add(new SettingObject("Experiment", "sql_experimentident", "NabilColumnTergitol1p0", SettingObject.SettingsType.String));
        this.loSettings.add(new SettingObject("UPSERT", "sql_upsert", false, SettingObject.SettingsType.Boolean));
        this.loSettings.add(new SettingObject("Settings PIV", "sql_evalsettingspiv", "bestpractice", SettingObject.SettingsType.String));
        this.loSettings.add(new SettingObject("Settings BUB", "sql_evalsettingsbub", "bestpractice", SettingObject.SettingsType.String));
        this.loSettings.add(new SettingObject("Reference Pos X [Px]", "data_refposPXX", 0, SettingObject.SettingsType.String));
        this.loSettings.add(new SettingObject("Reference Pos X [m]", "data_refposMX", 0.0, SettingObject.SettingsType.Double));
        this.loSettings.add(new SettingObject("Reference Pos Y [Px]", "data_refposPXY", 0, SettingObject.SettingsType.Integer));
        this.loSettings.add(new SettingObject("Reference Pos Y [m]", "data_refposMY", 0.0, SettingObject.SettingsType.Double));
//        this.loSettings.add(new SettingObject("Reference Pos Z [Px]", "data_refposPXZ", 0, SettingObject.SettingsType.Integer));
        this.loSettings.add(new SettingObject("Reference Pos Z [m]", "data_refposMZ", 0.0, SettingObject.SettingsType.Double));
        this.loSettings.add(new SettingObject("Resolution [micron/Px]", "data_Resolution", 10.0, SettingObject.SettingsType.Double));
        this.loSettings.add(new SettingObject("FPS", "data_FPS", 500, SettingObject.SettingsType.Integer));
        this.loSettings.add(new SettingObject("Burst Frequency [Hz]", "data_BurstFreq", 5, SettingObject.SettingsType.Integer));
        this.loSettings.add(new SettingObject("CSV", "data_csvExport", false, SettingObject.SettingsType.Boolean));
        this.loSettings.add(new SettingObject("Export Path", "data_csvExportPath", "Diretory", SettingObject.SettingsType.String));

    }

    @Override
    public void buildClusters() {
        SettingsCluster sqlCluster = new SettingsCluster("SQL",
                new String[]{"sql_activation", "sql_experimentident", "sql_upsert", "sql_evalsettingspiv", "sql_evalsettingsbub"}, this);
        sqlCluster.setDescription("Handles the export to the SQL database");
        lsClusters.add(sqlCluster);

        SettingsCluster csvExport = new SettingsCluster("CSV",
                new String[]{"data_csvExport", "data_csvExportPath"}, this);
        csvExport.setDescription("CSV export");
        lsClusters.add(csvExport);

        SettingsCluster refPos = new SettingsCluster("Referennce Position",
                new String[]{"data_refposPXX", "data_refposMX",
                    "data_refposPXY", "data_refposMY", "data_refposMZ", "data_Resolution"}, this);
        refPos.setDescription("Specifies the reference position in the image");
        lsClusters.add(refPos);

        SettingsCluster timeSettings = new SettingsCluster("Time",
                new String[]{"data_FPS", "data_BurstFreq"}, this);
        timeSettings.setDescription("Time settings for FPS and Burst Frequency");
        lsClusters.add(timeSettings);

    }

    private enum NameSpaceProtocol1DResults implements NameSpaceProtocolResults1D {
        avgx, avgy, tkeX, tkey
    }

}
