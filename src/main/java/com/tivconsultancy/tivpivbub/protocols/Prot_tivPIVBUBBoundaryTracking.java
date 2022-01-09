/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.tivconsultancy.tivpivbub.protocols;

import com.tivconsultancy.opentiv.edgedetector.OpenTIV_Edges;
import com.tivconsultancy.opentiv.helpfunctions.colorspaces.ColorSpaceCIEELab;
import com.tivconsultancy.opentiv.helpfunctions.colorspaces.Colorbar;
import com.tivconsultancy.opentiv.helpfunctions.matrix.MatrixEntry;
import com.tivconsultancy.opentiv.helpfunctions.settings.SettingObject;
import com.tivconsultancy.opentiv.helpfunctions.settings.SettingsCluster;
import com.tivconsultancy.opentiv.highlevel.protocols.NameSpaceProtocolResults1D;
import com.tivconsultancy.opentiv.highlevel.protocols.Protocol;
import com.tivconsultancy.opentiv.highlevel.protocols.UnableToRunException;
import com.tivconsultancy.opentiv.imageproc.algorithms.algorithms.BasicIMGOper;
import static com.tivconsultancy.opentiv.imageproc.algorithms.algorithms.EdgeDetections.getThinEdge;
import com.tivconsultancy.opentiv.imageproc.algorithms.algorithms.EllipseDetection;
import com.tivconsultancy.opentiv.imageproc.algorithms.algorithms.Morphology;
import com.tivconsultancy.opentiv.imageproc.contours.CPX;
import com.tivconsultancy.opentiv.imageproc.img_io.IMG_Reader;
import com.tivconsultancy.opentiv.imageproc.img_io.IMG_Writer;
import com.tivconsultancy.opentiv.imageproc.primitives.ImageGrid;
import com.tivconsultancy.opentiv.imageproc.primitives.ImageInt;
import com.tivconsultancy.opentiv.imageproc.primitives.ImagePoint;
import com.tivconsultancy.opentiv.imageproc.shapes.ArbStructure2;
import com.tivconsultancy.opentiv.imageproc.shapes.Circle;
import com.tivconsultancy.opentiv.math.algorithms.Sorting;
import com.tivconsultancy.opentiv.math.exceptions.EmptySetException;
import com.tivconsultancy.opentiv.math.primitives.OrderedPair;
import com.tivconsultancy.opentiv.math.specials.EnumObject;
import com.tivconsultancy.opentiv.physics.vectors.VelocityVec;
import com.tivconsultancy.opentiv.postproc.vector.PaintVectors;
import com.tivconsultancy.opentiv.velocimetry.boundarytracking.BoundTrackZiegenhein_2018;
import com.tivconsultancy.opentiv.velocimetry.boundarytracking.CPXTr;
import com.tivconsultancy.opentiv.velocimetry.boundarytracking.ReturnContainerBoundaryTracking;
import com.tivconsultancy.tivGUI.StaticReferences;
import com.tivconsultancy.tivpivbub.PIVBUBController;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
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
        try {
            if ((boolean) control.getCurrentMethod().getProtocol("boundtrack").getSettingsValue("SimpleTracking") && (boolean) control.getCurrentMethod().getProtocol("boundtrack").getSettingsValue("BoundTrack")) {
                throw new java.lang.RuntimeException("SimpleTracking and BoundTrack not support. Please choose one.");
            }

            ImageInt blackboard = (ImageInt) control.getCurrentMethod().getProtocol("preproc").getResults()[0];
            ImageGrid oGrid = new ImageGrid(blackboard.iaPixels);
            ImageInt grad = new ImageInt(blackboard.iaPixels.length, blackboard.iaPixels[0].length);
            grad.iaPixels = getThinEdge(blackboard.iaPixels, Boolean.FALSE, null, null, 0);

            ImageInt iMask1 = (ImageInt) control.getCurrentMethod().getProtocol("mask").getResults()[1];
            ImageInt iMask2 = (ImageInt) control.getCurrentMethod().getProtocol("mask").getResults()[2];

            List<CPXTr> lCPXTr1 = getBoundaries(iMask1);
            List<CPXTr> lCPXTr2 = getBoundaries(iMask2);

            HashMap<CPXTr, Circle> map = new HashMap<CPXTr, Circle>();
            List<Circle> lc = new ArrayList<>();
            for (CPXTr CPX : lCPXTr1) {
                List<MatrixEntry> lme = ImagePoint.getMEList(CPX.lo);
                Circle cc = EllipseDetection.EllipseFit(lme);
                if (cc != null) {
                    double dGreyDerivative = 0.0;
                    for (MatrixEntry me : lme) {
                        dGreyDerivative += getMaxDeriNeighbor(me, grad);
                    }
                    cc.dAvergeGreyDerivative = dGreyDerivative / (double) lme.size();
                    map.put(CPX, cc);
                    lc.add(cc);
                }
            }

            System.out.println(lCPXTr1.size() + " to " + lCPXTr2.size());

            if ((boolean) control.getCurrentMethod().getProtocol("boundtrack").getSettingsValue("SimpleTracking")) {
                //******Works
                /*List<Circle> lc2 = new ArrayList<>();
                for (CPXTr CPX : lCPXTr2) {
                    List<MatrixEntry> lme = ImagePoint.getMEList(CPX.lo);
                    Circle cc = EllipseDetection.EllipseFit(lme);
                    if (cc != null) {
                        lc2.add(cc);
                    }
                }
                Map<CPXTr, VelocityVec> oVelocityVectors = simpleTracking(lc2, control, map);
                control.getDataBUB().results_BT = new ReturnContainerBoundaryTracking(oVelocityVectors, lCPXTr1, lCPXTr2);
                 */
                //****
                lCPXTr1 = getStrucs(iMask1);
                lCPXTr2 = getStrucs(iMask2);
                Map<CPXTr, VelocityVec> oVelocityVectors = simpleTrackingStructs(lCPXTr1, lCPXTr2, control);
                control.getDataBUB().results_BT = new ReturnContainerBoundaryTracking(oVelocityVectors, lCPXTr1, lCPXTr2);
            }

            if ((boolean) control.getCurrentMethod().getProtocol("boundtrack").getSettingsValue("BoundTrack")) {
                control.getDataBUB().results_BT = BoundTrackZiegenhein_2018.runBoundTrack(this, lCPXTr1, lCPXTr2, oGrid, map);
            }

            control.getDataBUB().results_EFit = new OpenTIV_Edges.ReturnCotnainer_EllipseFit(lc, blackboard);
            control.getDataBUB().iaEdgesFirst = blackboard.iaPixels;
            if ((boolean) control.getCurrentMethod().getProtocol("boundtrack").getSettingsValue("SimpleTracking") || (boolean) control.getCurrentMethod().getProtocol("boundtrack").getSettingsValue("BoundTrack")) {
                List<VelocityVec> vecs = new ArrayList<>(control.getDataBUB().results_BT.velocityVectors.values());
                Colorbar oColBar = new Colorbar.StartEndLinearColorBar(0.0, getMaxVecLength(vecs).dEnum * 1.1, Colorbar.StartEndLinearColorBar.getColdToWarmRainbow2(), new ColorSpaceCIEELab(), (Colorbar.StartEndLinearColorBar.ColorOperation<Double>) (Double pParameter) -> pParameter);
                VectorDisplay = PaintVectors.paintOnImage(vecs, oColBar, blackboard.getBuffImage(), null, getAutoStretchFactor(vecs, blackboard.iaPixels.length / 10.0, 1.0));
            }

        } catch (EmptySetException ex) {
            StaticReferences.getlog().log(Level.SEVERE, "Unable to fit ellipses", ex);
        } catch (IOException ex) {
            Logger.getLogger(Prot_tivPIVBUBBoundaryTracking.class.getName()).log(Level.SEVERE, null, ex);
        }

        buildLookUp();

    }

    public static double getMaxDeriNeighbor(MatrixEntry meRef, ImageInt iGrad) {
        List<MatrixEntry> loRef = iGrad.getNeighborsN8(meRef.i, meRef.j);
        loRef.add(meRef);
        double dMax = 0.0;
        for (MatrixEntry me : loRef) {
            if (me != null) {
                if (iGrad.iaPixels[me.i][me.j] > dMax) {
                    dMax = iGrad.iaPixels[me.i][me.j];
                }
            }
        }
        return dMax;

    }

    public static List<CPXTr> getStrucs(ImageInt iMask) {
        ImageGrid oGrid = new ImageGrid(iMask.iaPixels.length, iMask.iaPixels[0].length);
        List<CPXTr> lCPXTr = new ArrayList<>();
        ImageInt Input = iMask.clone();
        for (int i = 0; i < iMask.iaPixels.length; i++) {
            for (int j = 0; j < iMask.iaPixels[0].length; j++) {
                if (Input.iaPixels[i][j] > 0 && Input.iaPixels[i][j] <= 255) {
                    final int iValue = iMask.iaPixels[i][j];
                    ArbStructure2 Structure = new ArbStructure2((new Morphology()).markFillN4(Input, i, j, (pParameter) -> {
                        if (Input.iaPixels[pParameter.i][pParameter.j] == iValue) {
                            return true;
                        } else {
                            return false;
                        }
                    }));
                    if (Structure.loPoints.size() > 50) {
                        CPXTr cpx = new CPXTr(new CPX(Structure.loPoints, oGrid));
                        cpx.oStart = cpx.lo.get(0);
                        lCPXTr.add(cpx);
                    }
                }
            }
        }
        return lCPXTr;
    }

    public static List<CPXTr> getBoundaries(ImageInt iMask) throws IOException {
        ImageGrid oGrid = new ImageGrid(iMask.iaPixels.length, iMask.iaPixels[0].length);
        ImageInt Input = iMask.clone();
        ImageInt oOuterEdges = BasicIMGOper.threshold(iMask.clone(), 1);
        ImageInt oOuterEdges2 = BasicIMGOper.threshold(iMask.clone(), 254);
        Morphology.markEdgesBinarizeImage(oOuterEdges);
        Morphology.markEdgesBinarizeImage(oOuterEdges2);

        List<CPXTr> lCPXTr = new ArrayList<>();

        for (int i = 0; i < iMask.iaPixels.length; i++) {
            for (int j = 0; j < iMask.iaPixels[0].length; j++) {
                if (Input.iaPixels[i][j] > 0 && Input.iaPixels[i][j] <= 255) {
                    final int iValue = iMask.iaPixels[i][j];
                    ArbStructure2 Structure = new ArbStructure2((new Morphology()).markFillN4(Input, i, j, (pParameter) -> {
                        if (Input.iaPixels[pParameter.i][pParameter.j] == iValue) {
                            return true;
                        } else {
                            return false;
                        }
                    }));

                    if (Structure.loPoints.size() > 10) {
                        List<MatrixEntry> meBound = new ArrayList<>();
                        for (MatrixEntry loPoint : Structure.loPoints) {
                            if (oOuterEdges.baMarker[loPoint.i][loPoint.j]) {
                                meBound.add(loPoint);
                            }
                        }

                        double iArea = 0.0;
                        if (meBound.size() > 0) {
                            Circle cc = EllipseDetection.EllipseFit(meBound);
                            if (cc != null) {
                                iArea = cc.dDiameterI / 2.0 * cc.dDiameterJ / 2.0 * Math.PI;
                            }
                        }
                        if ((int) iArea < Structure.loPoints.size()) {

                            for (MatrixEntry me : Structure.loPoints) {
                                List<MatrixEntry> loN8 = Input.getNeighborsN8(me.i, me.j);
                                for (MatrixEntry oN8 : loN8) {
                                    if (oN8 != null && oOuterEdges2.baMarker[oN8.i][oN8.j]) {
                                        meBound.add(me);
                                        break;
                                    }
                                }
                            }

                        }
                        if (meBound.size() > 5) {
                            meBound.get(0).dValue = iValue;
                            CPXTr cpx = new CPXTr(new CPX(meBound, oGrid));
                            cpx.oStart = cpx.lo.get(0);
                            lCPXTr.add(cpx);
                        }
                    }
                    Input.setPoints(Structure.loPoints, 0);

                }
            }
        }

        return lCPXTr;
    }

    public static Map<CPXTr, VelocityVec> simpleTracking(List<Circle> lCenter2, PIVBUBController controller, HashMap<CPXTr, Circle> map) throws EmptySetException {
        int YPlus = -1 * (Integer) controller.getCurrentMethod().getProtocol("boundtrack").getSettingsValue("BUBSRadiusYPlus");
        int YMinus = -1 * (Integer) controller.getCurrentMethod().getProtocol("boundtrack").getSettingsValue("BUBSRadiusYMinus");
        int XPlus = (Integer) controller.getCurrentMethod().getProtocol("boundtrack").getSettingsValue("BUBSRadiusXPlus");
        int XMinus = (Integer) controller.getCurrentMethod().getProtocol("boundtrack").getSettingsValue("BUBSRadiusXMinus");
        Map<CPXTr, VelocityVec> oVelocityVectors = new HashMap<>();
        Iterator it = map.entrySet().iterator();
        while (it.hasNext()) {
//        for (Circle c : lCenter1) {
            Map.Entry pair = (Map.Entry) it.next();
            Circle c = (Circle) pair.getValue();
            OrderedPair me = c.opSubPixelCenter;
            List<OrderedPair> loo = new ArrayList<>();
            for (Circle c2 : lCenter2) {
                OrderedPair me2 = c2.opSubPixelCenter;
                if (me2.y > me.y + YPlus && me2.y < me.y + YMinus && me2.x < me.x + XPlus && me2.x > me.x + XMinus) {
                    loo.add(me2);
                }
            }
            if (loo.size() == 1) {
                VelocityVec oVecSubPix = new VelocityVec(-(me.x - loo.get(0).x), -(me.y - loo.get(0).y), me);
                oVelocityVectors.put((CPXTr) pair.getKey(), oVecSubPix);
//                System.out.println("ye");
//                controller.getDataBUB().results.put(c, oVecSubPix);
            } else {
                VelocityVec oVecSubPix = new VelocityVec(0.0, 0.0, me);
                oVelocityVectors.put((CPXTr) pair.getKey(), oVecSubPix);
//                controller.getDataBUB().results.put(c, oVecSubPix);
            }
        }
        return oVelocityVectors;
    }

    public static Map<CPXTr, VelocityVec> simpleTrackingStructs(List<CPXTr> lCPXTr1, List<CPXTr> lCPXTr2, PIVBUBController controller) {
        int YPlus = -1 * (Integer) controller.getCurrentMethod().getProtocol("boundtrack").getSettingsValue("BUBSRadiusYPlus");
        int YMinus = -1 * (Integer) controller.getCurrentMethod().getProtocol("boundtrack").getSettingsValue("BUBSRadiusYMinus");
        int XPlus = (Integer) controller.getCurrentMethod().getProtocol("boundtrack").getSettingsValue("BUBSRadiusXPlus");
        int XMinus = (Integer) controller.getCurrentMethod().getProtocol("boundtrack").getSettingsValue("BUBSRadiusXMinus");
        Map<CPXTr, VelocityVec> oVelocityVectors = new HashMap<>();
        for (CPXTr cpx1 : lCPXTr1) {
            List<MatrixEntry> loo = new ArrayList<>();
            MatrixEntry me = MatrixEntry.getMeanEntry(ImagePoint.getMEList(cpx1.lo));
            for (CPXTr cpx2 : lCPXTr2) {
                MatrixEntry me2 = MatrixEntry.getMeanEntry(ImagePoint.getMEList(cpx2.lo));
                if (me2.i > me.i + YPlus && me2.i < me.i + YMinus && me2.j < me.j + XPlus && me2.j > me.j + XMinus) {
                    loo.add(me2);
                }
            }
            if (loo.size() == 1) {
                VelocityVec oVecSubPix = new VelocityVec(-(double) (me.j - loo.get(0).j), -(double) (me.i - loo.get(0).i), new OrderedPair(me.j, me.i));
                oVelocityVectors.put(cpx1, oVecSubPix);
//                System.out.println("ye");
//                controller.getDataBUB().results.put(c, oVecSubPix);
            } else {
                VelocityVec oVecSubPix = new VelocityVec(0.0, 0.0, new OrderedPair(me.j, me.i));
                oVelocityVectors.put(cpx1, oVecSubPix);
//                controller.getDataBUB().results.put(c, oVecSubPix);
            }
        }
        return oVelocityVectors;
    }

    public static List<CPXTr> setArcs(List<String[]> lsIn, ImageGrid oGrid) {
        List<CPXTr> lCPXTr1 = new ArrayList<>();
        List<MatrixEntry> lme = new ArrayList<>();
        for (int i = 0; i < lsIn.size(); i++) {
            int iValue = Integer.valueOf(lsIn.get(i)[2]);
            int iI = Integer.valueOf(lsIn.get(i)[0]);
            int iJ = Integer.valueOf(lsIn.get(i)[1]);
//            if (iI == 0 || iI == oGrid.iLength - 1 || iJ == 0 || iJ == oGrid.jLength - 1) {
//                continue;
//            }
            if (i < lsIn.size() - 1) {
                int iValuePlus = Integer.valueOf(lsIn.get(i + 1)[2]);
                if (iValue == iValuePlus) {
                    if (iI > 0 && iI < oGrid.iLength - 1 && iJ > 0 && iJ < oGrid.jLength - 1) {
                        lme.add(new MatrixEntry(iI, iJ, iValue));
                    }
                } else {
                    if (iI > 0 && iI < oGrid.iLength - 1 && iJ > 0 && iJ < oGrid.jLength - 1) {
                        lme.add(new MatrixEntry(iI, iJ, iValue));
                    }
                    if (lme.size() > 10) {
                        CPXTr cpx = new CPXTr(new CPX(lme, oGrid));
                        cpx.oStart = cpx.lo.get(0);
                        lCPXTr1.add(cpx);

                    }
                    lme.clear();
                }
            } else {
                if (iI > 0 && iI < oGrid.iLength - 1 && iJ > 0 && iJ < oGrid.jLength - 1) {
                    lme.add(new MatrixEntry(iI, iJ, iValue));
                }
                if (lme.size() > 10) {
                    CPXTr cpx = new CPXTr(new CPX(lme, oGrid));
                    cpx.oStart = cpx.lo.get(0);
                    lCPXTr1.add(cpx);
                }
            }
        }
        return lCPXTr1;
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
        this.loSettings.add(new SettingObject("Ellipse Fitting", "Ellipsefit", true, SettingObject.SettingsType.Boolean));
        this.loSettings.add(new SettingObject("Boundary Tracking", "BoundTrack", false, SettingObject.SettingsType.Boolean));
        this.loSettings.add(new SettingObject("Simple Tracking", "SimpleTracking", false, SettingObject.SettingsType.Boolean));
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
                new String[]{"BUBSRadiusYPlus", "BUBSRadiusYMinus", "BUBSRadiusXPlus", "BUBSRadiusXMinus", "tivBUBColBar", "Ellipsefit", "BoundTrack", "SimpleTracking"}, this);
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
