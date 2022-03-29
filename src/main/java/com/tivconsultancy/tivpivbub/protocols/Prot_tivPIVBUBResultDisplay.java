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
package com.tivconsultancy.tivpivbub.protocols;

import com.tivconsultancy.opentiv.edgedetector.OpenTIV_Edges;
import com.tivconsultancy.opentiv.helpfunctions.colorspaces.ColorSpaceCIEELab;
import com.tivconsultancy.opentiv.helpfunctions.colorspaces.Colorbar;
import com.tivconsultancy.opentiv.helpfunctions.matrix.MatrixEntry;
import com.tivconsultancy.opentiv.highlevel.protocols.NameSpaceProtocolResults1D;
import com.tivconsultancy.opentiv.highlevel.protocols.Protocol;
import com.tivconsultancy.opentiv.highlevel.protocols.UnableToRunException;
import com.tivconsultancy.opentiv.imageproc.primitives.ImageInt;
import com.tivconsultancy.opentiv.imageproc.shapes.Shape;
import com.tivconsultancy.opentiv.math.specials.LookUp;
import com.tivconsultancy.opentiv.math.specials.NameObject;
import com.tivconsultancy.opentiv.physics.vectors.VelocityVec;
import com.tivconsultancy.opentiv.postproc.vector.PaintVectors;
import com.tivconsultancy.tivGUI.StaticReferences;
import com.tivconsultancy.tivpiv.PIVController;
import com.tivconsultancy.tivpiv.data.DataPIV;
import com.tivconsultancy.tivpivbub.PIVBUBController;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;

/**
 *
 * @author TZ ThomasZiegenhein@TIVConsultancy.com +1 480 494 7254
 */
public class Prot_tivPIVBUBResultDisplay extends Protocol implements Serializable {

    private static final long serialVersionUID = -1142269276593182501L;
    transient BufferedImage imgResult;
    private String name = "Result PIV_BT";
    transient protected LookUp<BufferedImage> outPutImages;

    public Prot_tivPIVBUBResultDisplay(String name) {
        this();
        this.name = name;
    }

    public Prot_tivPIVBUBResultDisplay() {
        super();
        imgResult = (new ImageInt(1, 1, 200)).getBuffImage();
        buildLookUp();
        initSettings();
        buildClusters();
    }

    private void buildLookUp() {
        outPutImages = new LookUp<>();
        outPutImages.add(new NameObject<>(name, imgResult));
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
    public BufferedImage getView(String identFromViewer) {
        return outPutImages.get(identFromViewer);
    }

    @Override
    public Double getOverTimesResult(NameSpaceProtocolResults1D ident) {
        return null;
    }

    @Override
    public void run(Object... input) throws UnableToRunException {
        PIVBUBController controller = ((PIVBUBController) StaticReferences.controller);

        List<VelocityVec> vecs = new ArrayList<>(controller.getDataBUB().results.values());
        Colorbar oColBar;
//        ImageInt res = (ImageInt) controller.getCurrentMethod().getProtocol("preproc").getResults()[0];
        DataPIV data = ((PIVController) StaticReferences.controller).getDataPIV();
        ImageInt res = new ImageInt(data.iaReadInFirst);
        imgResult = res.getBuffImage();
        try {
//            int YPlus = Math.abs((Integer) controller.getCurrentMethod().getProtocol("bubtrack").getSettingsValue("BUBSRadiusYPlus"));
//            int YMlinus = Math.abs((Integer) controller.getCurrentMethod().getProtocol("bubtrack").getSettingsValue("BUBSRadiusYMinus"));
            double maxVecLength = (Double) controller.getCurrentMethod().getProtocol("display").getSettingsValue("MaxDisp");
//            double maxVecLength = Math.abs((double) (Integer) controller.getCurrentMethod().getProtocol("bubtrack").getSettingsValue("BUBSRadiusYPlus"));

            double StretchFactor = data.dStretch;
            StretchFactor = StretchFactor / 4.0;
                maxVecLength = maxVecLength * 4.0;
            oColBar = new Colorbar.StartEndLinearColorBar(0.0, maxVecLength, Colorbar.StartEndLinearColorBar.getColdToWarmRainbow2(), new ColorSpaceCIEELab(), (Colorbar.StartEndLinearColorBar.ColorOperation<Double>) (Double pParameter) -> pParameter);
            imgResult = PaintVectors.paintOnImage(vecs, oColBar, imgResult, null, StretchFactor);

            if ((boolean) controller.getCurrentMethod().getProtocol("inter areas").getSettingsValue("PIV_Interrogation")) {
                StretchFactor = StretchFactor * 4.0;
                maxVecLength = maxVecLength / 4.0;
                List<VelocityVec> vecsPIV = data.oGrid.getVectors(true);
                Colorbar oColBar2 = new Colorbar.StartEndLinearColorBar(0.0, maxVecLength, Colorbar.StartEndLinearColorBar.getColdToWarmRainbow2(), new ColorSpaceCIEELab(), (Colorbar.StartEndLinearColorBar.ColorOperation<Double>) (Double pParameter) -> pParameter);
                imgResult = PaintVectors.paintOnImage(vecsPIV, oColBar2, imgResult, null, StretchFactor);

            }
        } catch (IOException ex) {
            Logger.getLogger(Prot_tivPIVBUBResultDisplay.class.getName()).log(Level.SEVERE, null, ex);
        }
        OpenTIV_Edges.ReturnContainer_Shape lsC = ((PIVBUBController) StaticReferences.controller).getDataBUB().results_Shape;
        Graphics2D g2Result = imgResult.createGraphics();
        g2Result.drawImage(imgResult, 0, 0, null);
        for (Shape loCircle : lsC.loShapes) {
            g2Result.setColor(Color.BLUE);
            for (MatrixEntry me : loCircle.getlmeList()) {
                if (res.isInside(me.i, me.j)) {
                    g2Result.drawLine(me.j, me.i, me.j, me.i);
                }
            }
        }
        g2Result.dispose();
        String sFileName = controller.getCurrentFileSelected().getName().substring(0, controller.getCurrentFileSelected().getName().indexOf("."));
        File oPath = new File(controller.getCurrentFileSelected().getParent() + System.getProperty("file.separator") + "ResultImages");
        if (!oPath.exists()) {
            oPath.mkdir();
        }
        try {
            ImageIO.write(imgResult, "png", new File(oPath.getPath() + System.getProperty("file.separator") + sFileName + "PIV_BT.jpg"));
        } catch (IOException ex) {
            Logger.getLogger(Prot_tivPIVBUBResultDisplay.class.getName()).log(Level.SEVERE, null, ex);
        }

        buildLookUp();
    }

    @Override
    public String getType() {
        return name;
    }

    @Override
    public void buildClusters() {
//        SettingsCluster edgeDetector = new SettingsCluster("Edge Detector",
//                new String[]{"OuterEdges", "OuterEdgesThreshold"}, this);
//        edgeDetector.setDescription("Canny Edge Detector");
//        lsClusters.add(edgeDetector);
//
//        SettingsCluster curveSplit = new SettingsCluster("Split Curves",
//                new String[]{"SplitByCurv", "OrderCurvature", "ThresCurvSplitting"}, this);
//        curveSplit.setDescription("Splits the contours from the Canny Edge Detector");
//        lsClusters.add(curveSplit);
//
//        SettingsCluster filterEdges = new SettingsCluster("Filter",
//                new String[]{"SortOutSmallEdges", "MinSize", "SortOutLargeEdges", "MaxSize"}, this);
//        filterEdges.setDescription("Filters the contours from the Canny Edge Detector");
//        lsClusters.add(filterEdges);
//
//        SettingsCluster shapeFit = new SettingsCluster("Shape Fit",
//                new String[]{"EllipseFit_Ziegenhein2019", "EllipseFit_Ziegenhein2019_Distance", "EllipseFit_Ziegenhein2019_LeadingSize"}, this);
//        shapeFit.setDescription("Fits ellipses");
//        lsClusters.add(shapeFit);
//
//        SettingsCluster boundSplit = new SettingsCluster("Contour Splitting",
//                new String[]{"iCurvOrder", "iTangOrder", "dCurvThresh"}, this);
//        boundSplit.setDescription("Contour Splitting");
//        lsClusters.add(boundSplit);
//
//        SettingsCluster boundTrack = new SettingsCluster("Boundary Tracking",
//                new String[]{"BUBSRadiusYPlus", "BUBSRadiusYMinus", "BUBSRadiusXPlus", "BUBSRadiusXMinus", "tivBUBColBar"}, this);
//        boundTrack.setDescription("Boundary Tracking");
//        lsClusters.add(boundTrack);

    }

    /**
     * Cuts the image
     *
     * @param oInput Input image in the openTIV ImageInt format
     * @param oSettings Settings object containing the settings information
     * @return
     */
    private void initSettings() {
//          this.loSettings.add(new SettingObject("Execution Order" ,"ExecutionOrder", new ArrayList<>(), SettingObject.SettingsType.Object));

        //Edge Detectors
//        this.loSettings.add(new SettingObject("Edge Detector", "OuterEdges", false, SettingObject.SettingsType.Boolean));
//        this.loSettings.add(new SettingObject("Threshold", "OuterEdgesThreshold", 127, SettingObject.SettingsType.Integer));
//
//        //Simple Edge Detection
//        this.loSettings.add(new SettingObject("SimpleEdges", "SimpleEdges", false, SettingObject.SettingsType.Boolean));
//        this.loSettings.add(new SettingObject("SimpleEdgesThreshold", "SimpleEdgesThreshold", 127, SettingObject.SettingsType.Integer));
//
//        //Edge Operations
//        this.loSettings.add(new SettingObject("Filter Small Edges", "SortOutSmallEdges", false, SettingObject.SettingsType.Boolean));
//        this.loSettings.add(new SettingObject("MinSize", "MinSize", 30, SettingObject.SettingsType.Integer));
//        this.loSettings.add(new SettingObject("Filter Large Edges", "SortOutLargeEdges", false, SettingObject.SettingsType.Boolean));
//        this.loSettings.add(new SettingObject("MaxSize", "MaxSize", 1000, SettingObject.SettingsType.Integer));
//        this.loSettings.add(new SettingObject("RemoveOpenContours", "RemoveOpenContours", false, SettingObject.SettingsType.Boolean));
//        this.loSettings.add(new SettingObject("RemoveClosedContours", "RemoveClosedContours", false, SettingObject.SettingsType.Boolean));
//        this.loSettings.add(new SettingObject("CloseOpenContours", "CloseOpenContours", false, SettingObject.SettingsType.Boolean));
//        this.loSettings.add(new SettingObject("DistanceCloseContours", "DistanceCloseContours", 10, SettingObject.SettingsType.Integer));
//        this.loSettings.add(new SettingObject("ConnectOpenContours", "ConnectOpenContours", false, SettingObject.SettingsType.Boolean));
//        this.loSettings.add(new SettingObject("DistanceConnectContours", "DistanceConnectContours", 10, SettingObject.SettingsType.Integer));
//        this.loSettings.add(new SettingObject("SplitByCurv", "SplitByCurv", false, SettingObject.SettingsType.Boolean));
//        this.loSettings.add(new SettingObject("OrderCurvature", "OrderCurvature", 10, SettingObject.SettingsType.Integer));
//        this.loSettings.add(new SettingObject("ThresCurvSplitting", "ThresCurvSplitting", 0.9, SettingObject.SettingsType.Double));
//        this.loSettings.add(new SettingObject("RemoveWeakEdges", "RemoveWeakEdges", false, SettingObject.SettingsType.Boolean));
//        this.loSettings.add(new SettingObject("ThresWeakEdges", "ThresWeakEdges", 180, SettingObject.SettingsType.Integer));
//
//        //Shape Fitting
//        this.loSettings.add(new SettingObject("Ellipse Fit", "EllipseFit_Ziegenhein2019", false, SettingObject.SettingsType.Boolean));
//        this.loSettings.add(new SettingObject("Distance", "EllipseFit_Ziegenhein2019_Distance", 50, SettingObject.SettingsType.Double));
//        this.loSettings.add(new SettingObject("Leading Size", "EllipseFit_Ziegenhein2019_LeadingSize", 30, SettingObject.SettingsType.Double));
//        //Shape Filter
//        this.loSettings.add(new SettingObject("RatioFilter_Max", "RatioFilter_Max", false, SettingObject.SettingsType.Boolean));
//        this.loSettings.add(new SettingObject("RatioFilter_Max_Value", "RatioFilter_Max_Value", 1, SettingObject.SettingsType.Double));
//        this.loSettings.add(new SettingObject("RatioFilter_Min", "RatioFilter_Min", false, SettingObject.SettingsType.Boolean));
//        this.loSettings.add(new SettingObject("RatioFilter_Min_Value", "RatioFilter_Min_Value", 0, SettingObject.SettingsType.Integer));
//        this.loSettings.add(new SettingObject("Size_Max", "Size_Max", false, SettingObject.SettingsType.Boolean));
//        this.loSettings.add(new SettingObject("Size_Max_Value", "Size_Max_Value", 10000, SettingObject.SettingsType.Integer));
//        this.loSettings.add(new SettingObject("Size_Min", "Size_Min", false, SettingObject.SettingsType.Boolean));
//        this.loSettings.add(new SettingObject("Size_Min_Value", "Size_Min_Value", 1, SettingObject.SettingsType.Integer));
//        this.loSettings.add(new SettingObject("Major_Max", "Major_Max", false, SettingObject.SettingsType.Boolean));
//        this.loSettings.add(new SettingObject("Major_Max_Value", "Major_Max_Value", 10000, SettingObject.SettingsType.Integer));
//        this.loSettings.add(new SettingObject("Major_Min", "Major_Min", false, SettingObject.SettingsType.Boolean));
//        this.loSettings.add(new SettingObject("Major_Min_Value", "Major_Min_Value", 0, SettingObject.SettingsType.Integer));
//        this.loSettings.add(new SettingObject("Minor_Max", "Minor_Max", false, SettingObject.SettingsType.Boolean));
//        this.loSettings.add(new SettingObject("Minor_Max_Value", "Minor_Max_Value", 10000, SettingObject.SettingsType.Integer));
//        this.loSettings.add(new SettingObject("Minor_Min", "Minor_Min", false, SettingObject.SettingsType.Boolean));
//        this.loSettings.add(new SettingObject("Minor_Min_Value", "Minor_Min_Value", 1, SettingObject.SettingsType.Integer));
//
//        this.loSettings.add(new SettingObject("Curvature Order", "iCurvOrder", 5, SettingObject.SettingsType.Integer));
//        this.loSettings.add(new SettingObject("Tang Order", "iTangOrder", 10, SettingObject.SettingsType.Integer));
//        this.loSettings.add(new SettingObject("Curvature Threshold", "dCurvThresh", 0.075, SettingObject.SettingsType.Double));
//
//        //Tracking
//        this.loSettings.add(new SettingObject("Search Radius Y Max", "BUBSRadiusYPlus", 20, SettingObject.SettingsType.Integer));
//        this.loSettings.add(new SettingObject("Search Radius Y Min", "BUBSRadiusYMinus", 5, SettingObject.SettingsType.Integer));
//        this.loSettings.add(new SettingObject("Search Radius X Max", "BUBSRadiusXPlus", 20, SettingObject.SettingsType.Integer));
//        this.loSettings.add(new SettingObject("Search Radius X Min", "BUBSRadiusXMinus", -20, SettingObject.SettingsType.Integer));
    }

    @Override
    public Object[] getResults() {
        return new Object[]{imgResult};
    }

    @Override
    public void setImage(BufferedImage bi) {
        for (String s : getIdentForViews()) {
            outPutImages.set(s, bi);
        }
    }

}
