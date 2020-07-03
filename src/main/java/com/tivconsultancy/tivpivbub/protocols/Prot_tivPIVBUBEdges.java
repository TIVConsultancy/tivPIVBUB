/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.tivconsultancy.tivpivbub.protocols;

import com.tivconsultancy.opentiv.edgedetector.OpenTIV_Edges;
import com.tivconsultancy.opentiv.helpfunctions.settings.SettingObject;
import com.tivconsultancy.opentiv.helpfunctions.settings.SettingsCluster;
import com.tivconsultancy.opentiv.highlevel.protocols.NameSpaceProtocolResults1D;
import com.tivconsultancy.opentiv.highlevel.protocols.Protocol;
import com.tivconsultancy.opentiv.highlevel.protocols.UnableToRunException;
import com.tivconsultancy.opentiv.imageproc.primitives.ImageInt;
import com.tivconsultancy.tivGUI.StaticReferences;
import com.tivconsultancy.tivpivbub.PIVBUBController;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 *
 * @author TZ ThomasZiegenhein@TIVConsultancy.com +1 480 494 7254
 */
public class Prot_tivPIVBUBEdges extends Protocol {

    private static final long serialVersionUID = 7641291376662282578L;

    String name = "Ellipse Fit";
    
    ImageInt edges1st;
    ImageInt edges2nd;
    
    ImageInt shapeFit;
    
    public Prot_tivPIVBUBEdges(){
        edges1st = new ImageInt(50, 50, 0);
        shapeFit = new ImageInt(50, 50, 0);        
        buildLookUp();
        initSettins();
        buildClusters();
    }
    
    private void buildLookUp() {
        if(edges1st != null){
            ((PIVBUBController) StaticReferences.controller).getDataBUB().setImage(outNames.EdgesShapeFit.toString(), edges1st.getBuffImage());
        }
        if(edges2nd != null){
            ((PIVBUBController) StaticReferences.controller).getDataBUB().setImage(outNames.ShapeFit.toString(), shapeFit.getBuffImage());
        }
        
        
    }
    
    @Override
    public NameSpaceProtocolResults1D[] get1DResultsNames() {
        return new NameSpaceProtocolResults1D[0];
    }

    @Override
    public List<String> getIdentForViews() {
        return Arrays.asList(new String[]{outNames.EdgesShapeFit.toString(), outNames.ShapeFit.toString()});
    }

    @Override
    public void setImage(BufferedImage bi){
        edges1st = new ImageInt(bi);
        buildLookUp();
    }

    @Override
    public Double getOverTimesResult(NameSpaceProtocolResults1D ident) {
        return null;
    }

    @Override
    public void run(Object... input) throws UnableToRunException {
        PIVBUBController controller = ((PIVBUBController) StaticReferences.controller);
        ImageInt readFirst = new ImageInt(controller.getDataPIV().iaReadInFirst);
        ImageInt readSecond = new ImageInt(controller.getDataPIV().iaReadInSecond);
        edges1st = OpenTIV_Edges.performEdgeDetecting(this, readFirst);
        edges2nd = OpenTIV_Edges.performEdgeDetecting(this, readFirst);
        controller.getDataBUB().iaEdgesRAWFirst = edges1st.clone().iaPixels;
        controller.getDataBUB().iaEdgesRAWSecond = edges2nd.clone().iaPixels;
        if(edges1st!=null){
            edges1st = OpenTIV_Edges.performEdgeOperations(this, edges1st, readFirst);
            if(Boolean.valueOf(getSettingsValue("EllipseFit_Ziegenhein2019").toString())){
                controller.getDataBUB().results_EFit = OpenTIV_Edges.performShapeFitting(this, edges1st);
                shapeFit = controller.getDataBUB().results_EFit.oImage;
            }
            controller.getDataBUB().iaEdgesFirst = edges1st.iaPixels;            
        }else{
            edges1st = null;
        }
        if(edges2nd!=null){
            edges2nd = OpenTIV_Edges.performEdgeOperations(this, edges2nd, readSecond);
            controller.getDataBUB().iaEdgesSecond= edges2nd.iaPixels;
        }else{
            edges2nd = null;
        }                                        
        
        buildLookUp();
        
    }

    @Override
    public Object[] getResults() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public String getType() {
        return name;
    }
    
    private void initSettins() {
        this.loSettings.add(new SettingObject("Execution Order" ,"ExecutionOrder", new ArrayList<>(), SettingObject.SettingsType.Object));

        //Edge Detectors
        this.loSettings.add(new SettingObject("Edge Detector", "OuterEdges", false, SettingObject.SettingsType.Boolean));
        this.loSettings.add(new SettingObject("Threshold", "OuterEdgesThreshold", 127, SettingObject.SettingsType.Integer));

        //Simple Edge Detection
        this.loSettings.add(new SettingObject("SimpleEdges", "SimpleEdges", false, SettingObject.SettingsType.Boolean));
        this.loSettings.add(new SettingObject("SimpleEdgesThreshold", "SimpleEdgesThreshold", 127, SettingObject.SettingsType.Integer));

        //Edge Operations
        this.loSettings.add(new SettingObject("Filter Small Edges", "SortOutSmallEdges", false, SettingObject.SettingsType.Boolean));
        this.loSettings.add(new SettingObject("MinSize", "MinSize", 30, SettingObject.SettingsType.Integer));
        this.loSettings.add(new SettingObject("Filter Large Edges", "SortOutLargeEdges", false, SettingObject.SettingsType.Boolean));
        this.loSettings.add(new SettingObject("MaxSize", "MaxSize", 1000, SettingObject.SettingsType.Integer));
        this.loSettings.add(new SettingObject("RemoveOpenContours", "RemoveOpenContours", false, SettingObject.SettingsType.Boolean));
        this.loSettings.add(new SettingObject("RemoveClosedContours", "RemoveClosedContours", false, SettingObject.SettingsType.Boolean));
        this.loSettings.add(new SettingObject("CloseOpenContours", "CloseOpenContours", false, SettingObject.SettingsType.Boolean));                
        this.loSettings.add(new SettingObject("DistanceCloseContours", "DistanceCloseContours", 10, SettingObject.SettingsType.Integer));
        this.loSettings.add(new SettingObject("ConnectOpenContours", "ConnectOpenContours", false, SettingObject.SettingsType.Boolean));
        this.loSettings.add(new SettingObject("DistanceConnectContours", "DistanceConnectContours", 10, SettingObject.SettingsType.Integer));
        this.loSettings.add(new SettingObject("SplitByCurv", "SplitByCurv", false, SettingObject.SettingsType.Boolean));
        this.loSettings.add(new SettingObject("OrderCurvature", "OrderCurvature", 10, SettingObject.SettingsType.Integer));
        this.loSettings.add(new SettingObject("ThresCurvSplitting", "ThresCurvSplitting", 0.9, SettingObject.SettingsType.Double));
        this.loSettings.add(new SettingObject("RemoveWeakEdges", "RemoveWeakEdges", false, SettingObject.SettingsType.Boolean));
        this.loSettings.add(new SettingObject("ThresWeakEdges", "ThresWeakEdges", 180, SettingObject.SettingsType.Integer));

        //Shape Fitting
        this.loSettings.add(new SettingObject("Ellipse Fit", "EllipseFit_Ziegenhein2019", false, SettingObject.SettingsType.Boolean));
        this.loSettings.add(new SettingObject("Distance", "EllipseFit_Ziegenhein2019_Distance", 50, SettingObject.SettingsType.Double));
        this.loSettings.add(new SettingObject("Leading Size", "EllipseFit_Ziegenhein2019_LeadingSize", 30, SettingObject.SettingsType.Double));
        //Shape Filter
        this.loSettings.add(new SettingObject("RatioFilter_Max", "RatioFilter_Max", false, SettingObject.SettingsType.Boolean));
        this.loSettings.add(new SettingObject("RatioFilter_Max_Value", "RatioFilter_Max_Value", 1, SettingObject.SettingsType.Double));
        this.loSettings.add(new SettingObject("RatioFilter_Min", "RatioFilter_Min", false, SettingObject.SettingsType.Boolean));
        this.loSettings.add(new SettingObject("RatioFilter_Min_Value", "RatioFilter_Min_Value", 0,SettingObject.SettingsType.Integer));
        this.loSettings.add(new SettingObject("Size_Max", "Size_Max", false, SettingObject.SettingsType.Boolean));
        this.loSettings.add(new SettingObject("Size_Max_Value", "Size_Max_Value", 10000, SettingObject.SettingsType.Integer));
        this.loSettings.add(new SettingObject("Size_Min", "Size_Min", false, SettingObject.SettingsType.Boolean));
        this.loSettings.add(new SettingObject("Size_Min_Value", "Size_Min_Value", 1,SettingObject.SettingsType.Integer));
        this.loSettings.add(new SettingObject("Major_Max", "Major_Max", false, SettingObject.SettingsType.Boolean));
        this.loSettings.add(new SettingObject("Major_Max_Value", "Major_Max_Value", 10000,SettingObject.SettingsType.Integer));
        this.loSettings.add(new SettingObject("Major_Min", "Major_Min", false, SettingObject.SettingsType.Boolean));
        this.loSettings.add(new SettingObject("Major_Min_Value", "Major_Min_Value", 0, SettingObject.SettingsType.Integer));
        this.loSettings.add(new SettingObject("Minor_Max", "Minor_Max", false, SettingObject.SettingsType.Boolean));
        this.loSettings.add(new SettingObject("Minor_Max_Value", "Minor_Max_Value", 10000, SettingObject.SettingsType.Integer));
        this.loSettings.add(new SettingObject("Minor_Min", "Minor_Min", false, SettingObject.SettingsType.Boolean));
        this.loSettings.add(new SettingObject("Minor_Min_Value", "Minor_Min_Value", 1, SettingObject.SettingsType.Integer));
    }

    @Override
    public void buildClusters() {
        SettingsCluster edgeDetector = new SettingsCluster("Edge Detector",
                                                        new String[]{"OuterEdges", "OuterEdgesThreshold"}, this);
        edgeDetector.setDescription("Canny Edge Detector");
        lsClusters.add(edgeDetector);
        
        SettingsCluster curveSplit = new SettingsCluster("Split Curves",
                                                        new String[]{"SplitByCurv", "OrderCurvature", "ThresCurvSplitting"}, this);
        curveSplit.setDescription("Splits the contours from the Canny Edge Detector");
        lsClusters.add(curveSplit);
        
        SettingsCluster filterEdges = new SettingsCluster("Filter",
                                                        new String[]{"SortOutSmallEdges", "MinSize", "SortOutLargeEdges", "MaxSize"}, this);
        filterEdges.setDescription("Filters the contours from the Canny Edge Detector");
        lsClusters.add(filterEdges);
        
        SettingsCluster shapeFit = new SettingsCluster("Shape Fit",
                                                        new String[]{"EllipseFit_Ziegenhein2019", "EllipseFit_Ziegenhein2019_Distance", "EllipseFit_Ziegenhein2019_LeadingSize"}, this);
        shapeFit.setDescription("Fits ellipses");
        lsClusters.add(shapeFit);
        
    }

    @Override
    public BufferedImage getView(String identFromViewer) {
        BufferedImage img = ((PIVBUBController) StaticReferences.controller).getDataBUB().getImage(identFromViewer);
        if(img == null){
            img = (new ImageInt(50,50,0)).getBuffImage();
        }
        return img;
    }
    
    private enum outNames{
        EdgesShapeFit, ShapeFit
    }

}
