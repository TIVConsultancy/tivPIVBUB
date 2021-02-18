/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.tivconsultancy.tivpivbub.protocols;

import com.tivconsultancy.opentiv.highlevel.protocols.NameSpaceProtocolResults1D;
import com.tivconsultancy.opentiv.highlevel.protocols.Protocol;
import com.tivconsultancy.opentiv.highlevel.protocols.UnableToRunException;
import com.tivconsultancy.opentiv.imageproc.img_io.IMG_Writer;
import com.tivconsultancy.opentiv.imageproc.primitives.ImageInt;
import com.tivconsultancy.opentiv.math.algorithms.Averaging;
import com.tivconsultancy.opentiv.math.interfaces.Value;
import com.tivconsultancy.opentiv.math.specials.LookUp;
import com.tivconsultancy.opentiv.math.specials.NameObject;
import com.tivconsultancy.opentiv.physics.vectors.VelocityVec;
import com.tivconsultancy.tivGUI.StaticReferences;
import com.tivconsultancy.tivpiv.PIVController;
import com.tivconsultancy.tivpiv.PIVMethod;
import com.tivconsultancy.tivpiv.data.DataPIV;
import com.tivconsultancy.tivpiv.protocols.PIVProtocol;
import com.tivconsultancy.tivpiv.tivPIVSubControllerSQL;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author TZ ThomasZiegenhein@TIVConsultancy.com +1 480 494 7254
 */
public class Prot_tivPIVAIPredictions extends PIVProtocol {

    ImageInt mask1;
    ImageInt mask2;
    ImageInt intersec1;
    ImageInt intersec2;

    private String name = "AIPredict";

    public Prot_tivPIVAIPredictions() {
        super();
        buildLookUp();
        initSettins();
        buildClusters();
    }

    private void buildLookUp() {
    }

    @Override
    public NameSpaceProtocolResults1D[] get1DResultsNames() {
        return new NameSpaceProtocolResults1D[0];
    }

    @Override
    public List<String> getIdentForViews() {
        return Arrays.asList(new String[]{name});
    }

    @Override
    public Double getOverTimesResult(NameSpaceProtocolResults1D ident) {
        return null;
    }

    @Override
    public void run(Object... input) throws UnableToRunException {
        if (input.length >= 2) {
            ImageInt pic = (ImageInt) input[0];
            File oF1 = (File) input[1];
            File oF2 = (File) input[2];
            PIVMethod method = ((PIVMethod) StaticReferences.controller.getCurrentMethod());
            try {
                if (method.bReadFromSQL) {
                    //BufferedImage[] BI = ((tivPIVSubControllerSQL) StaticReferences.controller.getSQLControler(null)).readPredictionFromSQL(method.experimentSQL, oF1.getName());
                    mask1 = ((tivPIVSubControllerSQL) StaticReferences.controller.getSQLControler(null)).readPredictionFromSQL(method.experimentSQL, oF1.getName(), pic.iaPixels.length, pic.iaPixels[0].length)[0];
                    intersec1 = ((tivPIVSubControllerSQL) StaticReferences.controller.getSQLControler(null)).readPredictionFromSQL(method.experimentSQL, oF1.getName(), pic.iaPixels.length, pic.iaPixels[0].length)[1];

//                    IMG_Writer.PaintGreyPNG(mask1, new File("C:\\Users\\Nutzer\\Desktop\\TestNN\\mask1.jpeg"));
                    mask2 = ((tivPIVSubControllerSQL) StaticReferences.controller.getSQLControler(null)).readPredictionFromSQL(method.experimentSQL, oF2.getName(), pic.iaPixels.length, pic.iaPixels[0].length)[0];
                    intersec2 = ((tivPIVSubControllerSQL) StaticReferences.controller.getSQLControler(null)).readPredictionFromSQL(method.experimentSQL, oF2.getName(), pic.iaPixels.length, pic.iaPixels[0].length)[1];
                }
                for (int i = 0; i < mask1.iaPixels.length; i++) {
                    for (int j = 0; j < mask1.iaPixels[0].length; j++) {
                        if (mask1.iaPixels[i][j] == 255) {
                            mask1.baMarker[i][j] = true;
                        } else {
                            mask1.baMarker[i][j] = false;
                        }
                        if (mask2.iaPixels[i][j] == 255) {
                            mask2.baMarker[i][j] = true;
                        } else {
                            mask2.baMarker[i][j] = false;
                        }
                    }
                }
            } catch (Exception e) {
                StaticReferences.getlog().log(Level.SEVERE, "Cannot load SQL mask", e);
            }
        }

        buildLookUp();
    }

    @Override
    public void setImage(BufferedImage bi) {
        mask1 = new ImageInt(bi);
        mask2 = new ImageInt(bi);
        buildLookUp();
    }

    @Override
    public String getType() {
        return name;
    }

    private void initSettins() {
//        this.loSettings.add(new SettingObject("Hart1998", "tivPIVHart1998", false, SettingObject.SettingsType.Boolean));
//        this.loSettings.add(new SettingObject("Hart1998Divider", "tivPIVHart1998Divider", 2.0, SettingObject.SettingsType.Double));
//        this.loSettings.add(new SettingObject("Sub Pixel Type", "tivPIVSubPixelType", "Gaussian", SettingObject.SettingsType.String));
//        this.loSettings.add(new SettingObject("Multipass", "tivPIVMultipass", false, SettingObject.SettingsType.Boolean));
//        this.loSettings.add(new SettingObject("Multipass BiLinear", "tivPIVMultipass_BiLin", true, SettingObject.SettingsType.Boolean));
//        this.loSettings.add(new SettingObject("Multipass Count", "tivPIVMultipassCount", 3, SettingObject.SettingsType.Integer));
//        this.loSettings.add(new SettingObject("Refinement", "tivPIVInterrAreaRefine", false, SettingObject.SettingsType.Boolean));
    }

    @Override
    public void buildClusters() {
//        SettingsCluster IMGMultipass = new SettingsCluster("Multipass",
//                                                           new String[]{"tivPIVMultipass", "tivPIVMultipassCount"}, this);
//        IMGMultipass.setDescription("Multiple runs of the displacements");
//        lsClusters.add(IMGMultipass);
//
//        SettingsCluster SubPixel = new SettingsCluster("Sub Pixel Accuracy",
//                                                       new String[]{"tivPIVSubPixelType", "tivPIVMultipass_BiLin"}, this);
//        SubPixel.setDescription("Improving the accuracy with sub pixel interpolation");
//        lsClusters.add(SubPixel);
//
//        SettingsCluster Improvements = new SettingsCluster("Improvements",
//                                                           new String[]{"tivPIVHart1998", "tivPIVHart1998Divider", "tivPIVInterrAreaRefine"}, this);
//        Improvements.setDescription("Other strategies to improve the results");
//        lsClusters.add(Improvements);

    }

//    public List<SettingObject> getHints(){
//        List<SettingObject> ls = super.getHints();
//        ls.add(new SettingObject("Sub Pixel Type", "tivPIVSubPixelType", "None", SettingObject.SettingsType.String));
//        ls.add(new SettingObject("Sub Pixel Type", "tivPIVSubPixelType", "Parabolic", SettingObject.SettingsType.String));
//        ls.add(new SettingObject("Sub Pixel Type", "tivPIVSubPixelType", "Centroid", SettingObject.SettingsType.String));
//        return ls;
//    }
//    public static InterrGrid posProc(InterrGrid oGrid, DataPIV Data) {
//        /*
//         put everything that is used to improve the result after the standard FFT        
//         */
//
//        if (Data.bValidate) {
//            oGrid.validateVectors(Data.iStampSize, Data.dValidationThreshold, Data.sValidationType);
//            if (Data.bInterpolation) {
//                oGrid.reconstructInvalidVectors(5);
//            }
//        }
//
//        if (Data.bMultipass || Data.bMultipass_BiLin) {
//            for (int i = 0; i < Data.iMultipassCount; i++) {
////                oGrid.shiftAndRecalc();
//                if (Data.bMultipass_BiLin) {
//                    oGrid.shiftAndRecalcSubPix(Data);
//                } else {
//                    oGrid.shiftAndRecalc(Data);
//                }
//                if (Data.bValidate) {
//                    oGrid.validateVectors(Data.iStampSize, Data.dValidationThreshold, Data.sValidationType);
//                    if (Data.bInterpolation) {
//                        oGrid.reconstructInvalidVectors(5);
//                    }
//                }
//            }
//        }
//        if (Data.bRefine) {
//            for (int i = 0; i < oGrid.oaContent.length; i++) {
//                for (int j = 0; j < oGrid.oaContent[0].length; j++) {
//                    oGrid.oaContent[i][j].refine();
//                }
//            }
//            InterrGrid oRefine = oGrid.getRefinesGrid();
//            oRefine.checkMask(Data);
//
//            if (Data.bMultipass || Data.bMultipass_BiLin) {
//                for (int i = 0; i < Data.iMultipassCount; i++) {
//                    if (Data.bMultipass_BiLin) {
//                        oGrid.shiftAndRecalcSubPix(Data);
//                    } else {
//                        oGrid.shiftAndRecalc(Data);
//                    }
//                    oRefine.validateVectors(Data.iStampSize, Data.dValidationThreshold, Data.sValidationType);
//                    oRefine.reconstructInvalidVectors(5);
//                }
//            }
//
//            return oRefine;
//
//        }
//
//        return oGrid;
//
//    }
    @Override
    public Object[] getResults() {
        return new Object[]{mask1.clone(), mask2.clone(), intersec1.clone(), intersec2.clone()};
    }

}
