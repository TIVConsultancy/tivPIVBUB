/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.tivconsultancy.tivpivbub.protocols;

import com.tivconsultancy.opentiv.helpfunctions.colorspaces.ColorSpaceCIEELab;
import com.tivconsultancy.opentiv.helpfunctions.colorspaces.Colorbar;
import com.tivconsultancy.opentiv.highlevel.protocols.NameSpaceProtocolResults1D;
import com.tivconsultancy.opentiv.highlevel.protocols.Protocol;
import com.tivconsultancy.opentiv.highlevel.protocols.UnableToRunException;
import com.tivconsultancy.opentiv.imageproc.primitives.ImageInt;
import com.tivconsultancy.opentiv.imageproc.primitives.ImagePoint;
import com.tivconsultancy.opentiv.imageproc.shapes.Circle;
import com.tivconsultancy.opentiv.math.algorithms.Sorting;
import com.tivconsultancy.opentiv.math.exceptions.EmptySetException;
import com.tivconsultancy.opentiv.math.primitives.OrderedPair;
import com.tivconsultancy.opentiv.math.specials.EnumObject;
import com.tivconsultancy.opentiv.physics.vectors.VelocityVec;
import com.tivconsultancy.opentiv.postproc.vector.PaintVectors;
import com.tivconsultancy.opentiv.velocimetry.boundarytracking.CPXTr;
import com.tivconsultancy.tivGUI.StaticReferences;
import com.tivconsultancy.tivpivbub.PIVBUBController;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author TZ ThomasZiegenhein@TIVConsultancy.com +1 480 494 7254
 */
public class Prot_tivPIVBUBMergeShapeBoundTrack extends Protocol {

    private static final long serialVersionUID = 7641291376662282578L;

    String name = "Result";

    BufferedImage VectorDisplay;

    public Prot_tivPIVBUBMergeShapeBoundTrack() {
        buildLookUp();
        initSettins();
        buildClusters();
    }

    private void buildLookUp() {

        ((PIVBUBController) StaticReferences.controller).getDataBUB().setImage(outNames.Result.toString(), VectorDisplay);

    }

    @Override
    public NameSpaceProtocolResults1D[] get1DResultsNames() {
        return new NameSpaceProtocolResults1D[0];
    }

    @Override
    public List<String> getIdentForViews() {
        return Arrays.asList(new String[]{outNames.Result.toString()});
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

        PIVBUBController controller = ((PIVBUBController) StaticReferences.controller);
        ImageInt contourtsShapeFit = new ImageInt(controller.getDataBUB().iaEdgesFirst);
        List<Circle> circles = controller.getDataBUB().results_EFit.loCircles;
        Map<CPXTr, VelocityVec> interfaceVelo = controller.getDataBUB().results_BT.velocityVectors;

        Map<CPXTr, Circle> connectionMap = new HashMap<>();

        int iCounter = 256;
        for (Circle c : circles) {
            contourtsShapeFit.setPoints(c.lmeCircle, iCounter);
            iCounter++;
        }

        for (CPXTr cpx : interfaceVelo.keySet()) {
            List<Integer> votes = getEmptyVoteList(circles.size() + 1);
            for (ImagePoint p : cpx.lo) {
                int circIndex = contourtsShapeFit.getPointIMGP(p);
                if (circIndex > 255) {
                    int currentVote = votes.get(circIndex - 256);
                    currentVote++;
                    votes.set(circIndex - 256, currentVote);
                }
            }
            EnumObject o = Sorting.getMaxCharacteristic2(votes, new Sorting.Characteristic<Integer>() {
                                                     @Override
                                                     public Double getCharacteristicValue(Integer pParameter) {
                                                         return pParameter * 1.0;
                                                     }
                                                 });
            if (o.dEnum > 1) {
                int circMostVotes = votes.indexOf(Integer.valueOf(o.dEnum.intValue()));
                connectionMap.put(cpx, circles.get(circMostVotes));
            }
        }

        Map<Circle, List<CPXTr>> reverseConnectionMap = new HashMap<>();

        for (Circle c : circles) {
            List<CPXTr> contoursConnectedToCircle = new ArrayList<>();
            for (Map.Entry<CPXTr, Circle> entry : connectionMap.entrySet()) {
                if (entry.getValue().equals(c)) {
                    contoursConnectedToCircle.add(entry.getKey());
                }
            }
            if (!contoursConnectedToCircle.isEmpty()) {
                reverseConnectionMap.put(c, contoursConnectedToCircle);
            }
        }

        ImageInt connected = new ImageInt(contourtsShapeFit.iaPixels.length, contourtsShapeFit.iaPixels[0].length, 0);

        for (Map.Entry<Circle, List<CPXTr>> entry : reverseConnectionMap.entrySet()) {
//            int greyValue = (int) (Math.random()*255.0);
            double dvx = 0.0;
            double dvy = 0.0;
            double dvz = 0.0;
            double x = 0.0;
            double y = 0.0;
            int iCount = 0;
            for (CPXTr cpx : entry.getValue()) {
                if (cpx == null) {
                    continue;
                }
                VelocityVec vec = interfaceVelo.get(cpx);
                dvx = dvx + vec.getVelocityX();
                dvy = dvy + vec.getVelocityY();
//                dvz = dvx + vec.getVelocityZ();
                x = x + vec.getPosX();
                y = y + vec.getPosY();
                iCount++;
//                connected.setPointsIMGP(cpx.getPoints(), greyValue);
            }
            if (iCount > 0) {
                connected.setPoints(entry.getKey().lmeCircle, 255);                
                controller.getDataBUB().results.put(entry.getKey(), new VelocityVec(dvx / iCount, dvy / iCount, new OrderedPair(x / iCount, y / iCount)));
            }
        }

        try {
//            control.getDataBUB().results_BT = BoundTrackZiegenhein_2018.runBoundTrack(this, new ImageGrid(edgesB1.iaPixels), new ImageGrid(edgesB2.iaPixels));
//            this.contours1 = control.getDataBUB().results_BT.contours1;
//            this.contours2 = control.getDataBUB().results_BT.contours2;
            List<VelocityVec> vecs = new ArrayList<>(controller.getDataBUB().results.values());
            Colorbar oColBar = new Colorbar.StartEndLinearColorBar(0.0, getMaxVecLength(vecs).dEnum * 1.1, Colorbar.StartEndLinearColorBar.getColdToWarmRainbow2(), new ColorSpaceCIEELab(), (Colorbar.StartEndLinearColorBar.ColorOperation<Double>) (Double pParameter) -> pParameter);
            VectorDisplay = PaintVectors.paintOnImage(vecs, oColBar, connected.getBuffImage(), null, getAutoStretchFactor(vecs, connected.iaPixels.length / 10.0, 1.0));
        } catch (EmptySetException ex) {
            StaticReferences.getlog().log(Level.SEVERE, "Unable to track boundaries", ex);
            VectorDisplay = connected.getBuffImage();
        } catch (IOException ex) {
            Logger.getLogger(Prot_tivPIVBUBBoundaryTracking.class.getName()).log(Level.SEVERE, null, ex);
            VectorDisplay = connected.getBuffImage();
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

    public List<Integer> getEmptyVoteList(int iSize) {
        List<Integer> votes = new ArrayList<>();
        for (int i = 0; i < iSize; i++) {
            votes.add(0);
        }
        return votes;
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
//        this.loSettings.add(new SettingObject("Execution Order", "ExecutionOrder", new ArrayList<>(), SettingObject.SettingsType.Object));
    }

    @Override
    public void buildClusters() {
//        SettingsCluster edgeDetector = new SettingsCluster("Edge Detector",
//                                                           new String[]{"OuterEdges", "OuterEdgesThreshold"}, this);
//        edgeDetector.setDescription("Canny Edge Detector");
//        lsClusters.add(edgeDetector);
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
        Result
    }

}
