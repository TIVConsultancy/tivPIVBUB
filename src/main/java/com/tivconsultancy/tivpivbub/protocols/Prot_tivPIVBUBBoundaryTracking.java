/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.tivconsultancy.tivpivbub.protocols;

import com.tivconsultancy.opentiv.edgedetector.OpenTIV_Edges;
import com.tivconsultancy.opentiv.edgedetector.OpenTIV_Edges.ReturnCotnainer_EllipseFit;
import com.tivconsultancy.opentiv.helpfunctions.colorspaces.ColorSpaceCIEELab;
import com.tivconsultancy.opentiv.helpfunctions.colorspaces.Colorbar;
import com.tivconsultancy.opentiv.helpfunctions.settings.SettingObject;
import com.tivconsultancy.opentiv.helpfunctions.settings.SettingsCluster;
import com.tivconsultancy.opentiv.highlevel.protocols.NameSpaceProtocolResults1D;
import com.tivconsultancy.opentiv.highlevel.protocols.Protocol;
import com.tivconsultancy.opentiv.highlevel.protocols.UnableToRunException;
import com.tivconsultancy.opentiv.imageproc.primitives.ImageGrid;
import com.tivconsultancy.opentiv.imageproc.primitives.ImageInt;
import com.tivconsultancy.opentiv.math.algorithms.Sorting;
import com.tivconsultancy.opentiv.math.exceptions.EmptySetException;
import com.tivconsultancy.opentiv.math.specials.EnumObject;
import com.tivconsultancy.opentiv.physics.vectors.VelocityVec;
import com.tivconsultancy.opentiv.postproc.vector.PaintVectors;
import com.tivconsultancy.opentiv.velocimetry.boundarytracking.BoundTrackZiegenhein_2018;
import com.tivconsultancy.opentiv.velocimetry.boundarytracking.ReturnContainerBoundaryTracking;
import com.tivconsultancy.tivGUI.StaticReferences;
import com.tivconsultancy.tivpivbub.PIVBUBController;
import com.tivconsultancy.tivpivbub.data.DataBUB;
import java.awt.Color;
import java.awt.image.BufferedImage;
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
public class Prot_tivPIVBUBBoundaryTracking extends Protocol {

    private static final long serialVersionUID = 7641291376662282578L;

    String name = "Boundary Tracking";

    BufferedImage VectorDisplay;
    ImageInt edgesB1;
    ImageInt edgesB2;
    ImageInt contours1;
    ImageInt contours2;

    public Prot_tivPIVBUBBoundaryTracking() {
        contours1 = new ImageInt(50, 50, 0);
        contours2 = new ImageInt(50, 50, 0);
        edgesB1 = new ImageInt(50, 50, 0);
        edgesB2 = new ImageInt(50, 50, 0);
        buildLookUp();
        initSettins();
        buildClusters();
    }

    private void buildLookUp() {
        ((PIVBUBController) StaticReferences.controller).getDataBUB().setImage(outNames.Edges.toString(), edgesB1.getBuffImage());
        ((PIVBUBController) StaticReferences.controller).getDataBUB().setImage(outNames.Contours1.toString(), contours1.getBuffImage());
        ((PIVBUBController) StaticReferences.controller).getDataBUB().setImage(outNames.Contours2.toString(), contours2.getBuffImage());
        ((PIVBUBController) StaticReferences.controller).getDataBUB().setImage(outNames.BoundaryTracking.toString(), VectorDisplay);
    }

    @Override
    public NameSpaceProtocolResults1D[] get1DResultsNames() {
        return new NameSpaceProtocolResults1D[0];
    }

    @Override
    public List<String> getIdentForViews() {
        return Arrays.asList(new String[]{outNames.BoundaryTracking.toString(), outNames.Contours1.toString(), outNames.Contours2.toString()});
    }

    @Override
    public void setImage(BufferedImage bi) {
        VectorDisplay = bi;
        buildLookUp();
    }

    @Override
    public Double getOverTimesResult(NameSpaceProtocolResults1D ident) {
        return null;
    }

    @Override
    public void run(Object... input) throws UnableToRunException {

        PIVBUBController control = ((PIVBUBController) StaticReferences.controller);
        
        edgesB1 = OpenTIV_Edges.performEdgeDetecting(this, new ImageInt(control.getDataPIV().iaReadInFirst));
        edgesB1 = OpenTIV_Edges.performEdgeOperations(this, edgesB1, new ImageInt(control.getDataPIV().iaReadInFirst));
        int iFirstThreshold = Integer.valueOf(this.getSettingsValue("OuterEdgesThreshold").toString());
        int iSecondThreshold = Integer.valueOf(this.getSettingsValue("OuterEdgesThresholdSecond").toString());
        this.setSettingsValue("OuterEdgesThreshold", iSecondThreshold);
        edgesB2 = OpenTIV_Edges.performEdgeDetecting(this, new ImageInt(control.getDataPIV().iaReadInSecond)); 
        this.setSettingsValue("OuterEdgesThreshold", iFirstThreshold);
        
        try {
            control.getDataBUB().results_BT = BoundTrackZiegenhein_2018.runBoundTrack(this,  new ImageGrid(edgesB1.iaPixels), new ImageGrid(edgesB2.iaPixels));
            this.contours1 = control.getDataBUB().results_BT.contours1;
            this.contours2 = control.getDataBUB().results_BT.contours2;
            List<VelocityVec> vecs = new ArrayList<>(control.getDataBUB().results_BT.velocityVectors.values());
            Colorbar oColBar = new Colorbar.StartEndLinearColorBar(0.0, getMaxVecLength(vecs).dEnum * 1.1, Colorbar.StartEndLinearColorBar.getColdToWarmRainbow2(), new ColorSpaceCIEELab(), (Colorbar.StartEndLinearColorBar.ColorOperation<Double>) (Double pParameter) -> pParameter);
            VectorDisplay = PaintVectors.paintOnImage(vecs, oColBar, control.getDataBUB().results_BT.contours1.getBuffImage(), null, getAutoStretchFactor(vecs, control.getDataBUB().results_BT.contours1.iaPixels.length / 10.0, 1.0));
        } catch (EmptySetException ex) {
            StaticReferences.getlog().log(Level.SEVERE, "Unable to track boundaries", ex);
        } catch (IOException ex) {
            Logger.getLogger(Prot_tivPIVBUBBoundaryTracking.class.getName()).log(Level.SEVERE, null, ex);
        }

        buildLookUp();

    }

    public Double getAutoStretchFactor(List<VelocityVec> oVeloVecs, double pictureScale, double autoStretchFactor) {
        try {
            EnumObject o = getMaxVecLength(oVeloVecs);

            Double dStretch = (pictureScale / o.dEnum * autoStretchFactor);
            return dStretch;

        } catch (EmptySetException ex) {
            StaticReferences.getlog().log(Level.SEVERE, "Cannot auto stretch vectors for boundary tracking, 1.0 assumed", ex);
            return 1.0;
        }
    }

    public EnumObject getMaxVecLength(List<VelocityVec> oVeloVecs) throws EmptySetException {
        EnumObject o = Sorting.getMaxCharacteristic(oVeloVecs, new Sorting.Characteristic() {

                                                @Override
                                                public Double getCharacteristicValue(Object pParameter) {
                                                    return ((VelocityVec) pParameter).opUnitTangent.dValue;
                                                }
                                            });
        return o;
    }

    public List<Color> getColorbar() {
        String colbar = getSettingsValue("tivBUBColBar").toString();
        if (colbar.equals("Brown")) {
            return Colorbar.StartEndLinearColorBar.getBrown();
        }
        if (colbar.equals("ColdCutRainbow")) {
            return Colorbar.StartEndLinearColorBar.getColdCutRainbow();
        }
        if (colbar.equals("ColdRainbow")) {
            return Colorbar.StartEndLinearColorBar.getColdRainbow();
        }
        if (colbar.equals("ColdToWarm")) {
            return Colorbar.StartEndLinearColorBar.getColdToWarm();
        }
        if (colbar.equals("ColdToWarmRainbow")) {
            return Colorbar.StartEndLinearColorBar.getColdToWarmRainbow();
        }
        if (colbar.equals("ColdToWarmRainbow2")) {
            return Colorbar.StartEndLinearColorBar.getColdToWarmRainbow2();
        }
        if (colbar.equals("Grey")) {
            return Colorbar.StartEndLinearColorBar.getGrey();
        }
        if (colbar.equals("Jet")) {
            return Colorbar.StartEndLinearColorBar.getJet();
        }
        if (colbar.equals("LightBlue")) {
            return Colorbar.StartEndLinearColorBar.getLightBlue();
        }
        if (colbar.equals("LightBrown")) {
            return Colorbar.StartEndLinearColorBar.getLightBrown();
        }
        if (colbar.equals("Pink")) {
            return Colorbar.StartEndLinearColorBar.getPink();
        }
        if (colbar.equals("WarmToColdRainbow")) {
            return Colorbar.StartEndLinearColorBar.getWarmToColdRainbow();
        }
        if (colbar.equals("darkGreen")) {
            return Colorbar.StartEndLinearColorBar.getdarkGreen();
        }
        if (colbar.equals("veryLightBrown")) {
            return Colorbar.StartEndLinearColorBar.getveryLightBrown();
        }
        int iGreyValueVec = 255;
        return Colorbar.StartEndLinearColorBar.getCustom(iGreyValueVec, iGreyValueVec, iGreyValueVec, iGreyValueVec, iGreyValueVec, iGreyValueVec);

    }

    public List<SettingObject> getHints() {
        List<SettingObject> ls = super.getHints();
        ls.add(new SettingObject("Colorbar", "tivBUBColBar", "None", SettingObject.SettingsType.String));
        ls.add(new SettingObject("Colorbar", "tivBUBColBar", "Jet", SettingObject.SettingsType.String));
        ls.add(new SettingObject("Colorbar", "tivBUBColBar", "ColdCutRainbow", SettingObject.SettingsType.String));
        ls.add(new SettingObject("Colorbar", "tivBUBColBar", "ColdRainbow", SettingObject.SettingsType.String));
        ls.add(new SettingObject("Colorbar", "tivBUBColBar", "ColdToWarm", SettingObject.SettingsType.String));
        ls.add(new SettingObject("Colorbar", "tivBUBColBar", "ColdToWarmRainbow", SettingObject.SettingsType.String));
        ls.add(new SettingObject("Colorbar", "tivBUBColBar", "Grey", SettingObject.SettingsType.String));
        ls.add(new SettingObject("Colorbar", "tivBUBColBar", "LightBlue", SettingObject.SettingsType.String));
        ls.add(new SettingObject("Colorbar", "tivBUBColBar", "Brown", SettingObject.SettingsType.String));
        ls.add(new SettingObject("Colorbar", "tivBUBColBar", "LightBrown", SettingObject.SettingsType.String));
        ls.add(new SettingObject("Colorbar", "tivBUBColBar", "veryLightBrown", SettingObject.SettingsType.String));
        ls.add(new SettingObject("Colorbar", "tivBUBColBar", "Pink", SettingObject.SettingsType.String));
        ls.add(new SettingObject("Colorbar", "tivBUBColBar", "WarmToColdRainbow", SettingObject.SettingsType.String));
        ls.add(new SettingObject("Colorbar", "tivBUBColBar", "darkGreen", SettingObject.SettingsType.String));
        return ls;
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

        //Edge Detector
        this.loSettings.add(new SettingObject("Edge Detector", "OuterEdges", true, SettingObject.SettingsType.Boolean));
        this.loSettings.add(new SettingObject("Threshold First Pic", "OuterEdgesThreshold", 127, SettingObject.SettingsType.Integer));
        this.loSettings.add(new SettingObject("Threshold Second Pic", "OuterEdgesThresholdSecond", 127, SettingObject.SettingsType.Integer));
        this.loSettings.add(new SettingObject("Filter Small Edges", "SortOutSmallEdges", false, SettingObject.SettingsType.Boolean));
        this.loSettings.add(new SettingObject("MinSize", "MinSize", 30, SettingObject.SettingsType.Integer));
        this.loSettings.add(new SettingObject("Filter Large Edges", "SortOutLargeEdges", false, SettingObject.SettingsType.Boolean));
        this.loSettings.add(new SettingObject("MaxSize", "MaxSize", 1000, SettingObject.SettingsType.Integer));
        
        //Curv processing
        this.loSettings.add(new SettingObject("Curvature Order", "iCurvOrder", 5, SettingObject.SettingsType.Integer));
        this.loSettings.add(new SettingObject("Tang Order", "iTangOrder", 10, SettingObject.SettingsType.Integer));
        this.loSettings.add(new SettingObject("Curvature Threshold", "dCurvThresh", 0.075, SettingObject.SettingsType.Double));
        this.loSettings.add(new SettingObject("Colorbar", "tivBUBColBar", "ColdToWarmRainbow2", SettingObject.SettingsType.String));
        
        //Tracking
        this.loSettings.add(new SettingObject("Search Radius Y Max", "BUBSRadiusYPlus", 20, SettingObject.SettingsType.Integer));
        this.loSettings.add(new SettingObject("Search Radius Y Min", "BUBSRadiusYMinus", 5, SettingObject.SettingsType.Integer));
        this.loSettings.add(new SettingObject("Search Radius X Max", "BUBSRadiusXPlus", 20, SettingObject.SettingsType.Integer));
        this.loSettings.add(new SettingObject("Search Radius X Min", "BUBSRadiusXMinus", -20, SettingObject.SettingsType.Integer));
        
        
    }

    @Override
    public void buildClusters() {
        
        SettingsCluster edgeDetect = new SettingsCluster("BT: Edges",
                                                         new String[]{"OuterEdgesThreshold", "OuterEdgesThresholdSecond", "SortOutSmallEdges", "MinSize"}, this);
        edgeDetect.setDescription("Boundary Tracking");
        lsClusters.add(edgeDetect);
        
        SettingsCluster boundSplit = new SettingsCluster("Contour Splitting",
                                                         new String[]{"iCurvOrder", "iTangOrder", "dCurvThresh"}, this);
        boundSplit.setDescription("Contour Splitting");
        lsClusters.add(boundSplit);
        
        SettingsCluster boundTrack = new SettingsCluster("Boundary Tracking",
                                                         new String[]{"BUBSRadiusYPlus", "BUBSRadiusYMinus", "BUBSRadiusXPlus", "BUBSRadiusXMinus", "tivBUBColBar"}, this);
        boundTrack.setDescription("Boundary Tracking");
        lsClusters.add(boundTrack);

    }

    @Override
    public BufferedImage getView(String identFromViewer) {
        BufferedImage img = ((PIVBUBController) StaticReferences.controller).getDataBUB().getImage(identFromViewer);
        if (img == null) {
            img = (new ImageInt(50, 50, 0)).getBuffImage();
        }
        return img;
    }

    private enum outNames {
        BoundaryTracking, Contours1, Contours2, Edges
    }

}
