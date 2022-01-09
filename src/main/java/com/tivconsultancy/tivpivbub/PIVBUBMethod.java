/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.tivconsultancy.tivpivbub;

import com.tivconsultancy.opentiv.highlevel.protocols.NameSpaceProtocolResults1D;
import com.tivconsultancy.opentiv.highlevel.protocols.UnableToRunException;
import static com.tivconsultancy.opentiv.imageproc.algorithms.algorithms.EdgeDetections.getThinEdge;
import com.tivconsultancy.opentiv.imageproc.primitives.ImageInt;
import com.tivconsultancy.opentiv.math.exceptions.EmptySetException;
import com.tivconsultancy.opentiv.math.specials.NameObject;
import com.tivconsultancy.tivGUI.StaticReferences;
import com.tivconsultancy.tivpiv.PIVController;
import com.tivconsultancy.tivpiv.PIVMethod;
import com.tivconsultancy.tivpiv.PIVStaticReferences;
import com.tivconsultancy.tivpiv.data.DataPIV;
import com.tivconsultancy.tivpivbub.protocols.Prot_ResultDisplayAI_AI_Int;
import com.tivconsultancy.tivpivbub.protocols.Prot_tivPIVBUBBoundaryTracking;
import com.tivconsultancy.tivpivbub.protocols.Prot_tivPIVBUBDataHandling;
import com.tivconsultancy.tivpivbub.protocols.Prot_tivPIVBUBEdges;
import com.tivconsultancy.tivpivbub.protocols.Prot_tivPIVBUBMergeShapeBoundTrack;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;

/**
 *
 * @author TZ ThomasZiegenhein@TIVConsultancy.com +1 480 494 7254
 */
public class PIVBUBMethod extends PIVMethod {

    public PIVBUBMethod() {
        super();
        initProtocols();
    }

    private void initProtocols() {
        methods.add(new NameObject<>("edgedetect", new Prot_tivPIVBUBEdges()), methods.getSize() - 1);
        methods.add(new NameObject<>("boundtrack", new Prot_tivPIVBUBBoundaryTracking()), methods.getSize() - 1);
        methods.add(new NameObject<>("result", new Prot_tivPIVBUBMergeShapeBoundTrack()), methods.getSize() - 1);
        methods.remove("data");
        methods.add(new NameObject<>("data", new Prot_tivPIVBUBDataHandling()), methods.getSize() - 1);
        methods.add(new NameObject<>("AIPost", new Prot_ResultDisplayAI_AI_Int()), methods.getSize() - 1);
    }

    @Override
    public void run() throws Exception {
        long dStartTime = System.currentTimeMillis();
        try {
            getProtocol("read").run(new Object[]{imageFile1, imageFile2});
            getProtocol("preproc").run(getProtocol("read").getResults());
            Object[] prepr = getProtocol("preproc").getResults();
            getProtocol("mask").run(new Object[]{prepr[0], prepr[1], imageFile1, imageFile2, prepr[2]});
//            if (!checkMask((ImageInt) getProtocol("mask").getResults()[1]) && !checkMask((ImageInt) getProtocol("mask").getResults()[2])) {
                PIVStaticReferences.calcIntensityValues(((PIVController) StaticReferences.controller).getDataPIV());

                if ((boolean) getProtocol("inter areas").getSettingsValue("PIV_Interrogation") == true) {
                    getProtocol("inter areas").run();
                    getProtocol("calculate").run();
                    getProtocol("display").run();
                }
                if ((boolean) getProtocol("boundtrack").getSettingsValue("Ellipsefit")) {
                    getProtocol("boundtrack").run();
                    if ((boolean) getProtocol("boundtrack").getSettingsValue("BoundTrack") || (boolean) getProtocol("boundtrack").getSettingsValue("SimpleTracking")) {
                        getProtocol("result").run();
                        if ((boolean) getProtocol("system").getSettingsValue("tivGUI_dataDraw")) {
                            getProtocol("AIPost").run();
                        }
                    }
                }
                getProtocol("data").run();
                for (NameSpaceProtocolResults1D e : getProtocol("data").get1DResultsNames()) {
                    StaticReferences.controller.get1DResults().setResult(e.toString(), getProtocol("data").getOverTimesResult(e));
                }
                StaticReferences.controller.getPlotAbleOverTimeResults().refreshObjects();
//            }
            System.out.println(System.currentTimeMillis() - dStartTime);
        } catch (UnableToRunException ex) {
            StaticReferences.getlog().log(Level.SEVERE, "Wrong input", ex);
        }

    }

    public static boolean checkMask(ImageInt img) {
        int iTopCounter = 0;
        int iBotCounter = 0;
        int iWidth = img.iaPixels[0].length;
        for (int i = 0; i < iWidth; i++) {
            if (img.iaPixels[0][i] > 0) {
                iTopCounter++;
            }
            if (img.iaPixels[img.iaPixels.length - 1][i] > 0) {
                iBotCounter++;
            }
        }
        boolean bTopError = iTopCounter >= 7 * iWidth / 8 ? true : false;
        boolean bBotError = iBotCounter >= 7 * iWidth / 8 ? true : false;
        if ((bTopError && !bBotError) || (!bTopError && bBotError)) {
            return true;
        } else {
            return false;
        }
    }

}
