/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.tivconsultancy.tivpivbub;

import com.tivconsultancy.opentiv.highlevel.protocols.NameSpaceProtocolResults1D;
import static com.tivconsultancy.opentiv.imageproc.algorithms.algorithms.EdgeDetections.getThinEdge;
import com.tivconsultancy.opentiv.imageproc.primitives.ImageInt;
import com.tivconsultancy.opentiv.math.specials.NameObject;
import com.tivconsultancy.tivGUI.StaticReferences;
import com.tivconsultancy.tivpiv.PIVController;
import com.tivconsultancy.tivpiv.PIVMethod;
import com.tivconsultancy.tivpiv.PIVStaticReferences;
import com.tivconsultancy.tivpivbub.protocols.Prot_ResultDisplayAI_AI_Int;
import com.tivconsultancy.tivpivbub.protocols.Prot_tivPIVBUBBoundaryTracking;
import com.tivconsultancy.tivpivbub.protocols.Prot_tivPIVBUBDataHandling;
import com.tivconsultancy.tivpivbub.protocols.Prot_tivPIVBUBEdges;
import com.tivconsultancy.tivpivbub.protocols.Prot_tivPIVBUBMergeShapeBoundTrack;
import java.awt.image.BufferedImage;
import java.io.File;
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
//        StaticReferences.controller.getViewController(null).update();

        try {
            getProtocol("read").run(new Object[]{imageFile1, imageFile2});
            getProtocol("preproc").run(getProtocol("read").getResults());
            Object[] prepr = getProtocol("preproc").getResults();
            getProtocol("mask").run(new Object[]{prepr[0], prepr[1], imageFile1, imageFile2, prepr[2]});
            PIVStaticReferences.calcIntensityValues(((PIVController) StaticReferences.controller).getDataPIV());
            if ((boolean) getProtocol("inter areas").getSettingsValue("PIV_Interrogation") == true) {
                getProtocol("inter areas").run();
                getProtocol("calculate").run();
                getProtocol("display").run();
            }
            ImageInt imgInput = ((ImageInt) getProtocol("preproc").getResults()[0]).clone();
            imgInput.iaPixels = getThinEdge(imgInput.iaPixels, Boolean.FALSE, null, null, 0);
            ImageInt imgInput2 = ((ImageInt) getProtocol("preproc").getResults()[1]).clone();
            imgInput2.iaPixels = getThinEdge(imgInput2.iaPixels, Boolean.FALSE, null, null, 0);
            getProtocol("AIPost").run(new Object[]{getProtocol("preproc").getResults()[0],
                getProtocol("mask").getResults()[1],
                getProtocol("mask").getResults()[3],
                imgInput,
                getProtocol("preproc").getResults()[1],
                getProtocol("mask").getResults()[2],
                getProtocol("mask").getResults()[4],
                imgInput2});

            if ((boolean) getProtocol("boundtrack").getSettingsValue("BoundTrack")) {
                getProtocol("result").run();
                getProtocol("data").run();
            }

            for (NameSpaceProtocolResults1D e : getProtocol("data").get1DResultsNames()) {
                StaticReferences.controller.get1DResults().setResult(e.toString(), getProtocol("data").getOverTimesResult(e));
            }
            StaticReferences.controller.getPlotAbleOverTimeResults().refreshObjects();

        } catch (Exception ex) {
            throw ex;
        }

    }

}
