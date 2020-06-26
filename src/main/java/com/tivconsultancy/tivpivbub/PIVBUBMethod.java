/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.tivconsultancy.tivpivbub;

import com.tivconsultancy.opentiv.highlevel.protocols.NameSpaceProtocolResults1D;
import com.tivconsultancy.opentiv.math.specials.NameObject;
import com.tivconsultancy.tivGUI.StaticReferences;
import com.tivconsultancy.tivpiv.PIVController;
import com.tivconsultancy.tivpiv.PIVMethod;
import com.tivconsultancy.tivpiv.PIVStaticReferences;
import com.tivconsultancy.tivpivbub.protocols.Prot_tivPIVBUBEdges;

/**
 *
 * @author TZ ThomasZiegenhein@TIVConsultancy.com +1 480 494 7254
 */
public class PIVBUBMethod extends PIVMethod {
    
    public PIVBUBMethod(){
        super();
        initProtocols();
    }
    
    private void initProtocols() {        
        methods.add(new NameObject<>("edgedetect", new Prot_tivPIVBUBEdges()), methods.getSize()-1);
        System.out.println("");
    }
    
    @Override
    public void run() throws Exception {
//        StaticReferences.controller.getViewController(null).update();

        try {
            getProtocol("read").run(new Object[]{imageFile1, imageFile2});
            getProtocol("preproc").run(getProtocol("read").getResults());
            getProtocol("mask").run(getProtocol("preproc").getResults());
            PIVStaticReferences.calcIntensityValues(((PIVController) StaticReferences.controller).getDataPIV());
            getProtocol("inter areas").run();
            getProtocol("calculate").run();
            getProtocol("display").run();
            getProtocol("edgedetect").run();
//            getProtocol("postproc").run();
            getProtocol("data").run();

            for (NameSpaceProtocolResults1D e : getProtocol("data").get1DResultsNames()) {
                StaticReferences.controller.get1DResults().setResult(e.toString(), getProtocol("data").getOverTimesResult(e));
            }
            StaticReferences.controller.getPlotAbleOverTimeResults().refreshObjects();

        } catch (Exception ex) {
            throw ex;
        }
//            for (NameSpaceProtocolResults1D e : p.get1DResultsNames()) {
//                results1D.setResult(e.toString(), p.getOverTimesResult(e));
//            }

    }
    
}
