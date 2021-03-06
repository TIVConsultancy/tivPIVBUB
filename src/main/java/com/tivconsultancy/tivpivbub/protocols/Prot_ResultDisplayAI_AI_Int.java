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
import static com.tivconsultancy.opentiv.edgedetector.OpenTIV_Edges.closeContours;
import static com.tivconsultancy.opentiv.edgedetector.OpenTIV_Edges.connectContours;
import com.tivconsultancy.opentiv.helpfunctions.colorspaces.ColorSpaceCIEELab;
import com.tivconsultancy.opentiv.helpfunctions.colorspaces.Colorbar;
import com.tivconsultancy.opentiv.helpfunctions.matrix.Matrix;
import com.tivconsultancy.opentiv.helpfunctions.matrix.MatrixEntry;
import com.tivconsultancy.opentiv.helpfunctions.settings.SettingObject;
import com.tivconsultancy.opentiv.helpfunctions.settings.Settings;
import com.tivconsultancy.opentiv.helpfunctions.settings.SettingsCluster;
import com.tivconsultancy.opentiv.highlevel.protocols.NameSpaceProtocolResults1D;
import com.tivconsultancy.opentiv.highlevel.protocols.Protocol;
import com.tivconsultancy.opentiv.highlevel.protocols.UnableToRunException;
import com.tivconsultancy.opentiv.imageproc.algorithms.algorithms.BasicIMGOper;
import com.tivconsultancy.opentiv.imageproc.algorithms.algorithms.EllipseDetection;
import static com.tivconsultancy.opentiv.imageproc.algorithms.algorithms.EllipseDetection.EllipseFit_Contours;
import static com.tivconsultancy.opentiv.imageproc.algorithms.algorithms.EllipseDetection.RayTracingCheck3;
import static com.tivconsultancy.opentiv.imageproc.algorithms.algorithms.EllipseDetection.checkIfInside;
import static com.tivconsultancy.opentiv.imageproc.algorithms.algorithms.EllipseDetection.convert;
import static com.tivconsultancy.opentiv.imageproc.algorithms.algorithms.EllipseDetection.convert_CPX;
import static com.tivconsultancy.opentiv.imageproc.algorithms.algorithms.EllipseDetection.filterEllipse;
import static com.tivconsultancy.opentiv.imageproc.algorithms.algorithms.EllipseDetection.getPixelsOnCircle;
import com.tivconsultancy.opentiv.imageproc.algorithms.algorithms.Morphology;
import com.tivconsultancy.opentiv.imageproc.algorithms.algorithms.N8;
import com.tivconsultancy.opentiv.imageproc.algorithms.algorithms.Ziegenhein_2018;
import com.tivconsultancy.opentiv.imageproc.algorithms.areaextraction.PreMarked;
import com.tivconsultancy.opentiv.imageproc.contours.BasicOperations;
import static com.tivconsultancy.opentiv.imageproc.contours.BasicOperations.getAllContours;
import com.tivconsultancy.opentiv.imageproc.contours.CPX;
import com.tivconsultancy.opentiv.imageproc.img_io.IMG_Writer;
import com.tivconsultancy.opentiv.imageproc.primitives.ImageGrid;
import com.tivconsultancy.opentiv.imageproc.primitives.ImageInt;
import com.tivconsultancy.opentiv.imageproc.primitives.ImagePoint;
import com.tivconsultancy.opentiv.imageproc.shapes.ArbStructure;
import com.tivconsultancy.opentiv.imageproc.shapes.ArbStructure2;
import com.tivconsultancy.opentiv.imageproc.shapes.Circle;
import com.tivconsultancy.opentiv.imageproc.shapes.Line;
import com.tivconsultancy.opentiv.imageproc.shapes.Line2;
import com.tivconsultancy.opentiv.math.algorithms.Averaging;
import com.tivconsultancy.opentiv.math.algorithms.Sorting;
import com.tivconsultancy.opentiv.math.exceptions.EmptySetException;
import com.tivconsultancy.opentiv.math.functions.PLF;
import com.tivconsultancy.opentiv.math.interfaces.SideCondition2;
import com.tivconsultancy.opentiv.math.primitives.OrderedPair;
import com.tivconsultancy.opentiv.math.sets.Set1D;
import com.tivconsultancy.opentiv.math.sets.Set2D;
import com.tivconsultancy.opentiv.math.specials.EnumObject;
import com.tivconsultancy.opentiv.math.specials.LookUp;
import com.tivconsultancy.opentiv.math.specials.NameObject;
import com.tivconsultancy.opentiv.physics.vectors.VelocityVec;
import com.tivconsultancy.opentiv.postproc.vector.PaintVectors;
import com.tivconsultancy.opentiv.velocimetry.boundarytracking.BoundTrackZiegenhein_2018;
import static com.tivconsultancy.opentiv.velocimetry.boundarytracking.BoundTrackZiegenhein_2018.getNearestForCPXTr;
import static com.tivconsultancy.opentiv.velocimetry.boundarytracking.BoundTrackZiegenhein_2018.getSubPixelDist;
import static com.tivconsultancy.opentiv.velocimetry.boundarytracking.BoundTrackZiegenhein_2018.getvalidCPXListFirst;
import static com.tivconsultancy.opentiv.velocimetry.boundarytracking.BoundTrackZiegenhein_2018.getvalidCPXListSecond;
import static com.tivconsultancy.opentiv.velocimetry.boundarytracking.BoundTrackZiegenhein_2018.oHelp;
import static com.tivconsultancy.opentiv.velocimetry.boundarytracking.BoundTrackZiegenhein_2018.setHelpFunction;
import com.tivconsultancy.opentiv.velocimetry.boundarytracking.CPXTr;
import com.tivconsultancy.opentiv.velocimetry.boundarytracking.ReturnContainerBoundaryTracking;
import com.tivconsultancy.opentiv.velocimetry.helpfunctions.VelocityGrid;
import com.tivconsultancy.tivGUI.StaticReferences;
import com.tivconsultancy.tivpiv.PIVController;
import com.tivconsultancy.tivpiv.data.DataPIV;
import com.tivconsultancy.tivpivbub.PIVBUBController;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;

/**
 *
 * @author TZ ThomasZiegenhein@TIVConsultancy.com +1 480 494 7254
 */
public class Prot_ResultDisplayAI_AI_Int extends Protocol implements Serializable {

    private static final long serialVersionUID = -1142269276593182501L;
    List<CPXTr> lCPXTr1;
    List<CPXTr> lCPXTr2;
    ImageGrid oEdges1;
    ImageGrid oEdges2;
    transient BufferedImage imgResult;
    private String name = "Result AI";
    private String nameMask = "Mask AI";
    transient protected LookUp<BufferedImage> outPutImages;

    public Prot_ResultDisplayAI_AI_Int(String name) {
        this();
        this.name = name;
    }

    public Prot_ResultDisplayAI_AI_Int() {
        super();
        imgResult = (new ImageInt(1, 1, 200)).getBuffImage();
        lCPXTr1 = new ArrayList<>();
        lCPXTr2 = new ArrayList<>();
        oEdges1 = new ImageGrid(imgResult);
        oEdges2 = new ImageGrid(imgResult);
        buildLookUp();
        initSettings();
        buildClusters();
    }

    private void buildLookUp() {
        outPutImages = new LookUp<>();
        outPutImages.add(new NameObject<>(name, imgResult));
        outPutImages.add(new NameObject<>(nameMask, oEdges1.getBuffImage()));
//        ((PIVBUBController) StaticReferences.controller).getDataBUB().setImage(name, imgResult);
//        ((PIVBUBController) StaticReferences.controller).getDataBUB().setImage(nameMask, oEdges1.getBuffImage());
    }

    @Override
    public NameSpaceProtocolResults1D[] get1DResultsNames() {
        return new NameSpaceProtocolResults1D[0];
    }

    @Override
    public List<String> getIdentForViews() {
        return Arrays.asList(new String[]{name, nameMask});
    }

    @Override
    public BufferedImage getView(String identFromViewer) {
        return outPutImages.get(identFromViewer);
//        BufferedImage img = ((PIVBUBController) StaticReferences.controller).getDataBUB().getImage(identFromViewer);
//        if (img == null) {
//            img = (new ImageInt(50, 50, 0)).getBuffImage();
//        }
//        return img;
    }

    @Override
    public Double getOverTimesResult(NameSpaceProtocolResults1D ident) {
        return null;
    }

    @Override
    public void run(Object... input) throws UnableToRunException {
        lCPXTr1.clear();
        lCPXTr2.clear();
        PIVBUBController controller = ((PIVBUBController) StaticReferences.controller);
        long dStartTime = System.currentTimeMillis();
        if (input != null && input.length != 0 && input[0] != null && input[1] != null
                && input[2] != null && input[0] instanceof ImageInt && input[1] instanceof ImageInt
                && input[2] instanceof ImageInt) {

            ImageInt blackboard = ((ImageInt) input[0]).clone();
            ImageInt mask = (ImageInt) input[1];
            ImageInt oInnerEdges = (ImageInt) input[2];
            ImageInt grad = (ImageInt) input[3];
            ImageInt oInnerClone = oInnerEdges.clone();

//        try {
//            IMG_Writer.PaintGreyPNG(mask, new File("C:\\Users\\Nutzer\\Desktop\\TestNN\\mask.jpeg"));
//            IMG_Writer.PaintGreyPNG(oInnerEdges, new File("C:\\Users\\Nutzer\\Desktop\\TestNN\\oInnerEdges.jpeg"));
//            IMG_Writer.PaintGreyPNG(blackboard, new File("C:\\Users\\Nutzer\\Desktop\\TestNN\\blackboard.jpeg"));
//        } catch (IOException ex) {
//            Logger.getLogger(Prot_ResultDisplayAI_AI_Int.class.getName()).log(Level.SEVERE, null, ex);
//        }
            ImageInt OuterBounds = mask.clone();
            ImageGrid oEdges = new ImageGrid(blackboard.iaPixels.length, blackboard.iaPixels[0].length);

            List<CPX> lCPX1 = PredictSingle(blackboard, mask, oInnerEdges, grad, OuterBounds, true);

//            for (CPX cpx : lCPX1) {
//
//                oEdges.addPoint(cpx.lo, 255);
//
//            }
            oEdges.setData(new ImageGrid(oInnerEdges.iaPixels).getData());
            oEdges1 = oEdges.clone();
            controller.getDataBUB().iaEdgesFirst = oEdges.getMatrix();
            HashSet<ImagePoint> loStarts = Ziegenhein_2018.getStart(oEdges);
            for (CPX cpx : lCPX1) {
                outer:
                for (ImagePoint ip : cpx.lo) {
                    for (ImagePoint loStart : loStarts) {
                        if (ip.equals(loStart)) {
                            cpx.oStart = ip;
                            break outer;
                        }
                    }
                }
                if (cpx.oStart == null) {
                    cpx.oStart = cpx.lo.get(0);
                }
                lCPXTr1.add(new CPXTr(cpx));
            }
            System.out.println("Intersections First image " + ((System.currentTimeMillis() - dStartTime) / 1000.0));

            if (Boolean.valueOf(String.valueOf(controller.getCurrentMethod().getSystemSetting(null).getSettingsValue("tivGUI_dataStore")))) {
                OpenTIV_Edges.ReturnCotnainer_EllipseFit lsC = ((PIVBUBController) StaticReferences.controller).getDataBUB().results_EFit;
                BufferedImage EllipsFitARGB = (new BufferedImage(blackboard.getBuffImage().getWidth(), blackboard.getBuffImage().getHeight(), BufferedImage.TYPE_INT_ARGB));
                WritableRaster EllipsFitRast = EllipsFitARGB.getRaster();
                Color blue = Color.BLUE;
                Color red = Color.RED;
                Color yellow = Color.YELLOW;
                for (int i = 0; i < blackboard.iaPixels.length; i++) {
                    for (int j = 0; j < blackboard.iaPixels[0].length; j++) {
                        int val = blackboard.iaPixels[i][j];
                        EllipsFitRast.setPixel(j, i, new int[]{val, val, val, 255});
//                        if (oInnerClone.iaPixels[i][j] > 0) {
                        if (oInnerEdges.iaPixels[i][j] > 0) {
                            EllipsFitRast.setPixel(j, i, new int[]{red.getRed(), red.getGreen(), red.getBlue(), 127});
                        }
                        if (oInnerEdges.iaPixels[i][j] > 0 && oInnerClone.iaPixels[i][j] == 0) {
                            EllipsFitRast.setPixel(j, i, new int[]{yellow.getRed(), yellow.getGreen(), yellow.getBlue(), 127});
                        }
                    }
                }

                for (Circle loCircle : lsC.loCircles) {
                    for (MatrixEntry me : loCircle.lmeCircle) {
                        if (blackboard.isInside(me.i, me.j)) {
                            EllipsFitRast.setPixel(me.j, me.i, new int[]{blue.getRed(), blue.getGreen(), blue.getBlue(), 127});
                        }
                    }
                }
                System.out.println("Circs " + lsC.loCircles.size());
                BufferedImage newResult = (new BufferedImage(blackboard.getBuffImage().getWidth(), blackboard.getBuffImage().getHeight(), BufferedImage.TYPE_INT_ARGB));
                Graphics2D g2Result = newResult.createGraphics();
                g2Result.drawImage(EllipsFitARGB, 0, 0, null);
                g2Result.dispose();

                imgResult = newResult;
            }
        }

        if ((boolean) controller.getCurrentMethod().getProtocol("boundtrack").getSettingsValue("BoundTrack")) {
            if (input != null && input.length > 4 && input[4] != null && input[5] != null
                    && input[6] != null && input[4] instanceof ImageInt && input[5] instanceof ImageInt
                    && input[6] instanceof ImageInt) {
                ImageInt blackboard = ((ImageInt) input[4]).clone();
                ImageInt mask = (ImageInt) input[5];
                ImageInt oInnerEdges = (ImageInt) input[6];
                ImageInt grad = (ImageInt) input[7];
                ImageInt OuterBounds = mask.clone();

                List<CPX> lCPX2 = PredictSingle(blackboard, mask, oInnerEdges, grad, OuterBounds, false);
                ImageGrid oEdges = new ImageGrid(blackboard.iaPixels.length, blackboard.iaPixels[0].length);
                for (CPX cpx : lCPX2) {
                    oEdges.addPoint(cpx.lo, 255);
                }
                oEdges2 = oEdges.clone();
                HashSet<ImagePoint> loStarts = Ziegenhein_2018.getStart(oEdges);

                for (CPX cpx : lCPX2) {
                    outer:
                    for (ImagePoint ip : cpx.lo) {
                        for (ImagePoint loStart : loStarts) {
                            if (ip.equals(loStart)) {
                                cpx.oStart = ip;
                                break outer;
                            }
                        }
                    }
                    if (cpx.oStart == null) {
                        cpx.oStart = cpx.lo.get(0);
                    }
                    lCPXTr2.add(new CPXTr(cpx));
                }

                System.out.println("CPXTr1 " + lCPXTr1.size() + " CPXTr2 " + lCPXTr2.size());
                System.out.println("Intersections Second image " + ((System.currentTimeMillis() - dStartTime) / 1000.0));

                PIVBUBController control = ((PIVBUBController) StaticReferences.controller);
                if (!lCPXTr1.isEmpty() && !lCPXTr2.isEmpty()) {
                    try {
                        int searchYPlus = -1 * Integer.valueOf(this.getSettingsValue("BUBSRadiusYPlus").toString());
                        int searchYMinus = -1 * Integer.valueOf(this.getSettingsValue("BUBSRadiusYMinus").toString());

                        int searchXPlus = Integer.valueOf(this.getSettingsValue("BUBSRadiusXPlus").toString());
                        int searchXMinus = Integer.valueOf(this.getSettingsValue("BUBSRadiusXMinus").toString());
//                lCPXTr1 = getvalidCPXListFirst(lCPXTr1);
//                lCPXTr2 = getvalidCPXListSecond(lCPXTr2);
                        Map<CPXTr, VelocityVec> oVelocityVectors = new HashMap<>();
                        for (CPXTr oContoursToTrack : lCPXTr1) {

                            Collection<CPXTr> lo = Sorting.getEntriesWithSameCharacteristic(oContoursToTrack, lCPXTr2, 1.0, (Sorting.Characteristic2<CPXTr>) (CPXTr pParameter, CPXTr pParameter2) -> {
                                if (pParameter.getNorm(pParameter2) < 40) {
                                    return 1.0;
                                }
                                return 0.0;
                            });

                            List<CPXTr> loSort = new ArrayList<>();
                            loSort.addAll(lo);

                            setHelpFunction(oEdges1.iLength, oEdges1.jLength);
                            VelocityVec oVec = getNearestForCPXTr(oContoursToTrack, loSort, 1, new Set2D(new Set1D(searchXMinus, searchXPlus), new Set1D(searchYPlus, searchYMinus)), oEdges1.iLength, oEdges1.jLength, (SideCondition2) (Object pParameter1, Object pParameter2) -> ((CPXTr) pParameter1).getDistance((CPXTr) pParameter2) < 40);
                            if (oVec == null) {
                                continue;
                            }
                            OrderedPair opSubPixDist = getSubPixelDist((CPXTr) oVec.VelocityObject1, (CPXTr) oVec.VelocityObject2);
                            VelocityVec oVecSubPix = (VelocityVec) oVec.add(opSubPixDist);
                            oVecSubPix.VelocityObject1 = oVec.VelocityObject1;
                            oVecSubPix.VelocityObject2 = oVec.VelocityObject2;
                            oVelocityVectors.put(oContoursToTrack, oVecSubPix);
                        }

                        setHelpFunction(oEdges1.iLength, oEdges1.jLength);
                        for (CPXTr o : lCPXTr1) {
                            oHelp.setPoint(o.lo, 255);
                        }

                        ImageInt secContours = new ImageInt(oEdges2.iLength, oEdges2.jLength, 0);
                        for (CPXTr c : lCPXTr2) {
                            secContours.setPointsIMGP(c.lo, 255);
                        }

                        control.getDataBUB().results_BT = new ReturnContainerBoundaryTracking(oVelocityVectors, new ImageInt(oHelp.getMatrix()), secContours);
                        List<VelocityVec> vecs = new ArrayList<>(control.getDataBUB().results_BT.velocityVectors.values());
                        if (Boolean.valueOf(String.valueOf(controller.getCurrentMethod().getSystemSetting(null).getSettingsValue("tivGUI_dataStore")))) {

                            Colorbar oColBar = new Colorbar.StartEndLinearColorBar(0.0, new Prot_tivPIVBUBBoundaryTracking().getMaxVecLength(vecs).dEnum * 1.1, Colorbar.StartEndLinearColorBar.getColdToWarmRainbow2(), new ColorSpaceCIEELab(), (Colorbar.StartEndLinearColorBar.ColorOperation<Double>) (Double pParameter) -> pParameter);
                            imgResult = PaintVectors.paintOnImage(vecs, oColBar, imgResult, null, new Prot_tivPIVBUBBoundaryTracking().getAutoStretchFactor(vecs, control.getDataBUB().results_BT.contours1.iaPixels.length / 10.0, 1.0));
                            DataPIV data = ((PIVController) StaticReferences.controller).getDataPIV();
                            if ((boolean) controller.getCurrentMethod().getProtocol("inter areas").getSettingsValue("PIV_Interrogation") == true) {
                                List<VelocityVec> vecsPIV = data.oGrid.getVectors();
                                Colorbar oColBar2 = new Colorbar.StartEndLinearColorBar(0.0, new Prot_tivPIVBUBBoundaryTracking().getMaxVecLength(vecsPIV).dEnum * 1.1, Colorbar.StartEndLinearColorBar.getColdToWarmRainbow2(), new ColorSpaceCIEELab(), (Colorbar.StartEndLinearColorBar.ColorOperation<Double>) (Double pParameter) -> pParameter);
                                imgResult = PaintVectors.paintOnImage(vecsPIV, oColBar2, imgResult, null, new Prot_tivPIVBUBBoundaryTracking().getAutoStretchFactor(vecs, control.getDataBUB().results_BT.contours1.iaPixels.length / 10.0, 5.0));
                            }
                        }
                        System.out.println("Tracks " + vecs.size());
                        System.out.println("Tracking finished " + ((System.currentTimeMillis() - dStartTime) / 1000.0));

                    } catch (EmptySetException ex) {
                        Logger.getLogger(Prot_ResultDisplayAI_AI_Int.class.getName()).log(Level.SEVERE, null, ex);
                    } catch (IOException ex) {
                        Logger.getLogger(Prot_ResultDisplayAI_AI_Int.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            } else {
                throw new UnableToRunException("Input cannot be processed", new IOException());
            }
        }
        buildLookUp();
    }

    public static List<CPX> PredictSingle(ImageInt blackboard, ImageInt mask, ImageInt oInnerEdges, ImageInt grad, ImageInt OuterBounds, boolean bSetToRes) {
        double dMeanGrey = 0.0;
        int iCounter = 0;
        ImageInt maskEro = mask.clone();
        Morphology.erosion(maskEro);
        Morphology.erosion(maskEro);
        Morphology.erosion(maskEro);
        ImageInt intersecDila = oInnerEdges.clone();
        Morphology.dilatation(intersecDila);
        for (int i = 0; i < mask.iaPixels.length; i++) {
            for (int j = 0; j < mask.iaPixels[0].length; j++) {
                if (mask.iaPixels[i][j] > 0) {
                    dMeanGrey += blackboard.iaPixels[i][j];
                    iCounter++;
                }
            }
        }
        ImageInt internal = BasicIMGOper.threshold(blackboard.clone(), dMeanGrey / (double) iCounter);

        for (int i = 0; i < mask.iaPixels.length; i++) {
            for (int j = 0; j < mask.iaPixels[0].length; j++) {
                if (maskEro.iaPixels[i][j] == 0 || intersecDila.iaPixels[i][j] > 0) {
                    internal.setPoint(new MatrixEntry(i, j), 0);
                }
            }
        }
        Morphology.erosion(internal);
        Morphology.erosion(internal);
//        try {
//            IMG_Writer.PaintGreyPNG(internal, new File("C:\\Users\\Nutzer\\Desktop\\owncloudHZDR\\Work\\TianBIT\\BubbleDetec\\tests\\EroMaskInternal.jpeg"));
//            IMG_Writer.PaintGreyPNG(intersecDila, new File("C:\\Users\\Nutzer\\Desktop\\owncloudHZDR\\Work\\TianBIT\\BubbleDetec\\tests\\intersecDila.jpeg"));
//        } catch (IOException ex) {
//            Logger.getLogger(Prot_ResultDisplayAI_AI_Int.class.getName()).log(Level.SEVERE, null, ex);
//        }
        ImageGrid oGrid = new ImageGrid(OuterBounds.iaPixels);

        List<ArbStructure2> ls = PreMarked.getAreasBlackOnWhite(OuterBounds);
        for (ArbStructure2 l : ls) {
            if (l.loPoints.size() < 300) {
                for (int i = 0; i < l.loPoints.size(); i++) {
                    OuterBounds.iaPixels[l.loPoints.get(i).i][l.loPoints.get(i).j] = 255;
                    OuterBounds.baMarker[l.loPoints.get(i).i][l.loPoints.get(i).j] = false;
                }
            }

        }
        ImageInt inverse = BasicIMGOper.invert(OuterBounds.clone());
        List<ArbStructure2> lsAreas = PreMarked.getAreasBlackOnWhite(inverse);
        Morphology.markEdgesBinarizeImage(OuterBounds);

        for (int i = 0; i < OuterBounds.iaPixels.length; i++) {
            for (int j = 0; j < OuterBounds.iaPixels[0].length; j++) {
                if (!OuterBounds.baMarker[i][j]) {
                    OuterBounds.iaPixels[i][j] = 0;
                }
                if (OuterBounds.iaPixels[i][j] > 0) {
                    oGrid.oa[oGrid.getIndex(i, j)].bMarker = true;
                    OuterBounds.iaPixels[i][j] = 255;
                }
            }
        }
        ImageInt oIntThin = oInnerEdges.clone();
        ImageInt Curv = new ImageInt(oInnerEdges.iaPixels.length, oInnerEdges.iaPixels[0].length, 0);
        List<List<MatrixEntry>> lCFirst = new ArrayList<>();
        System.out.println("lsAreas " + lsAreas.size());
        for (ArbStructure2 sArea : lsAreas) {
            if (sArea.loPoints.size() > 2) {
                List<List<MatrixEntry>> lC = new ArrayList<>();
//                try {
                lC = ConnectEdges(sArea, OuterBounds, oInnerEdges, grad, oIntThin, mask, Curv, blackboard, internal);
                lCFirst.addAll(lC);
            }
        }

        List<CPX> lCPX = new ArrayList<>();
        List<Circle> lsF = new ArrayList<>();
        for (List<MatrixEntry> list : lCFirst) {
            lCPX.add(new CPX(list, oGrid));
            Circle cc = EllipseDetection.EllipseFit(list);
            if (cc != null) {
                double dGreyDerivative = 0.0;
                for (MatrixEntry me : list) {
                    dGreyDerivative += getMaxDeriNeighbor(me, grad);
                }
                cc.dAvergeGreyDerivative = dGreyDerivative / (double) list.size();
                lsF.add(cc);
            }
        }

        ////*******Ziegenhein EllipseFit *******************
//            List<Circle> lCSecond = new ArrayList<>();
//            try {
//                lCSecond = EllipseDetection.Ziegenhein_2019_GUI(new ImageGrid(oIntThin2.iaPixels), this);
//                for (Circle o : lCSecond) {
//                    oIntThin2.setPoints(o.lmeCircle, 127);
//                }
//
//            } catch (EmptySetException ex) {
//                Logger.getLogger(Prot_ResultDisplayAI_AI_Int.class.getName()).log(Level.SEVERE, null, ex);
//            } catch (IOException ex) {
//                Logger.getLogger(Prot_ResultDisplayAI_AI_Int.class.getName()).log(Level.SEVERE, null, ex);
//            }
//            //****Write out****
//            Circle.writeOut(lCSecond, sDir + System.getProperty("file.separator") + sName + "Second.csv");
//        System.out.println("Fin after " + ((System.currentTimeMillis() - dStartTime) / 1000.0));
        //******Set ellipses to results******
        if (bSetToRes) {
            PIVBUBController controller = ((PIVBUBController) StaticReferences.controller);
            controller.getDataBUB().results_EFit = new OpenTIV_Edges.ReturnCotnainer_EllipseFit(lsF, blackboard);
        }
        return lCPX;

    }

    public static void getCPXinAS(ArbStructure2 ASArea, ImageInt oEdges) {
        ImageInt oGrid = new ImageInt(oEdges.iaPixels.length, oEdges.iaPixels[0].length);
        ImageInt inverse2 = BasicIMGOper.invert(oEdges.clone());
        List<ArbStructure2> innerEdges = new ArrayList<>();
        List<CPX> allContours = new ArrayList<>();
        for (MatrixEntry me : ASArea.loPoints) {
            if (inverse2.iaPixels[me.i][me.j] == 0) {
                ArbStructure2 as = new ArbStructure2((ArrayList) new Morphology().markFillN8(inverse2, me.i, me.j));
                if (as.loPoints.size() > 3) {
                    innerEdges.add(as);
                }
                inverse2.setPoints(as.loPoints, 255);
                oGrid.setPoints(as.loPoints, 255);
                List<CPX> allContoursFirst = getAllContours(oGrid);
                for (CPX cpx : allContoursFirst) {
                    if (cpx.lo.size() > 3) {
                        allContours.add(cpx);
                    }
                }
                oGrid.setPoints(as.loPoints, 0);
            }
        }
    }

    public static List<List<MatrixEntry>> ConnectEdges(ArbStructure2 ASArea, ImageInt oOuterEdges, ImageInt oInnerEdges, ImageInt iGrad, ImageInt oIntThin, ImageInt iMask, ImageInt Curv, ImageInt blackboard, ImageInt internalBrightSpots) {
        List<ArbStructure2> innerEdges = new ArrayList<>();
        List<ArbStructure2> lsHelp = new ArrayList<>();
        List<MatrixEntry> lmeOuter = new ArrayList<>();
        List<MatrixEntry> lmeHelp = new ArrayList<>();
        List<List<MatrixEntry>> lCReturn = new ArrayList<>();
        for (MatrixEntry me : ASArea.loPoints) {
            if (oOuterEdges.iaPixels[me.i][me.j] == 255) {
                lmeOuter.add(me);
                N8 oN8Outer = new N8(oInnerEdges, me.i, me.j);
                boolean bNeighOuter = false;
                for (int i = 0; i < 7; i++) {
                    if (oN8Outer.isNeigh(i)) {
                        bNeighOuter = true;
                    }
                }
                if (bNeighOuter) {
                    lmeHelp.add(me);
                }
            }
        }
        oInnerEdges.setPoints(lmeHelp, 255);
        ArbStructure2 outerEdge = new ArbStructure2(lmeOuter);
        getConcavePoints(outerEdge.loPoints, Curv, iMask, 30, 2, 0);
        ImageInt inverse2 = BasicIMGOper.invert(Curv.clone());

        for (MatrixEntry me : ASArea.loPoints) {
            if (inverse2.iaPixels[me.i][me.j] == 0) {
                ArbStructure2 as = new ArbStructure2((ArrayList) new Morphology().markFillN8(inverse2, me.i, me.j));
                if (as.loPoints.size() > 3) {
//                    innerEdges.add(as);
                    // ****
                    MatrixEntry mme = getCenterOfBorder(as.loPoints);
                    oInnerEdges.setPoint(mme, 165);
                    oInnerEdges.setPoints(as.loPoints, 200);
                }
                inverse2.setPoints(as.loPoints, 255);
            }
        }
        ImageInt inverse = BasicIMGOper.invert(oInnerEdges.clone());
        for (MatrixEntry me : ASArea.loPoints) {
            if (inverse.iaPixels[me.i][me.j] < 255) {
                ArbStructure2 as = new ArbStructure2((ArrayList) new Morphology().markFillN8(inverse, me.i, me.j));
                if (as.loPoints.size() > 1) {
                    innerEdges.add(as);
                    lsHelp.add(as.clone());
                }
                inverse.setPoints(as.loPoints, 255);
            }
        }
        if (innerEdges.size() > 0) {
            //***Mark outer ends
            for (int i = 0; i < lsHelp.size(); i++) {
                Ziegenhein_2018.thinoutEdges(oIntThin, lsHelp.get(i));
                setEnds(oIntThin, oOuterEdges, lsHelp.get(i), 175);
                for (MatrixEntry me : lsHelp.get(i).loPoints) {
                    if (oIntThin.iaPixels[me.i][me.j] == 175) {
                        OrderedPair op = getDerivationOutwardsEndPoints(me, lsHelp.get(i).loPoints.size() / 2, lsHelp.get(i).loPoints);
                        MatrixEntry mme = getMostDistantEntryInDirection(me, op, innerEdges.get(i).loPoints);
                        oInnerEdges.setPoint(mme, 175);
                    }
                }

            }
            //***Connect IE ends to border
            for (ArbStructure2 as : innerEdges) {
                connectEdgesCloseToBorder(outerEdge, as, oInnerEdges, 6, 175, 200, internalBrightSpots);
            }

            //****Connect close IE ends
            for (ArbStructure2 as : innerEdges) {
                connectInsideEdges(innerEdges, as, oInnerEdges, 36, 175, 230, outerEdge, iGrad, internalBrightSpots);
            }
            ImageInt Outer2 = new ImageInt(oOuterEdges.iaPixels.length, oOuterEdges.iaPixels[0].length, 0);
            for (MatrixEntry me : ASArea.loPoints) {
                if (oOuterEdges.iaPixels[me.i][me.j] == 255 && (Curv.iaPixels[me.i][me.j] == 255 || oInnerEdges.iaPixels[me.i][me.j] > 0)) {
                    Outer2.setPoint(me, 255);
                }
            }
            inverse = BasicIMGOper.invert(Outer2.clone());

            for (MatrixEntry me : ASArea.loPoints) {

                if (inverse.iaPixels[me.i][me.j] == 0) {
                    ArbStructure2 as = new ArbStructure2((ArrayList) new Morphology().markFillN8(inverse, me.i, me.j));
                    if (as.loPoints.size() > 3) {
                        // ****MarkerPoint
                        MatrixEntry mme = getCenterOfBorder(as.loPoints);
                        Curv.setPoint(mme, 155);

                        oInnerEdges.setPoint(mme, 155);
                    }
                    inverse.setPoints(as.loPoints, 255);
                }
            }

            //***Update InnerEdge-List
            inverse = BasicIMGOper.invert(oInnerEdges.clone());
            innerEdges.clear();
            for (MatrixEntry me : ASArea.loPoints) {
                if (inverse.iaPixels[me.i][me.j] < 255) {
                    ArbStructure2 as = new ArbStructure2((ArrayList) new Morphology().markFillN8(inverse, me.i, me.j));
                    if (as.loPoints.size() > 1) {
                        innerEdges.add(as);
                    }
                    inverse.setPoints(as.loPoints, 255);
                }
            }

            //***Check Number of MarkerPoints
            //***************************************************
            boolean ConnectSingle = false;
            for (ArbStructure2 innerEdge : innerEdges) {
                List<MatrixEntry> lmeCurv = new ArrayList<>();

                for (MatrixEntry me : innerEdge.loPoints) {
                    if (oInnerEdges.iaPixels[me.i][me.j] == 155) {
                        lmeCurv.add(me);
                    }
                }
                //Connect to next IE 
                if (lmeCurv.size() == 1 && innerEdges.size() > 1) {
                    ConnectSingle = connectSingleMarkertoIE(innerEdges, lmeCurv.get(0), oInnerEdges, 32, 200);
                    oInnerEdges.setPoint(lmeCurv.get(0), 155);
                }
            }

            if (ConnectSingle) {
                inverse = BasicIMGOper.invert(oInnerEdges.clone());
                innerEdges.clear();
                for (MatrixEntry me : ASArea.loPoints) {
                    if (inverse.iaPixels[me.i][me.j] < 255) {
                        ArbStructure2 as = new ArbStructure2((ArrayList) new Morphology().markFillN8(inverse, me.i, me.j));
                        if (as.loPoints.size() > 1) {
                            innerEdges.add(as);
                        }
                        inverse.setPoints(as.loPoints, 255);
                    }
                }
            }

            //**********************************
            ImageInt OverlappCircs = new ImageInt(oInnerEdges.iaPixels.length, oInnerEdges.iaPixels[0].length, 255);
            boolean bDesired = false;
            for (MatrixEntry me : ASArea.loPoints) {
                if (me.equals(new MatrixEntry(320, 614))) {
                    bDesired = true;
                }
                if (oInnerEdges.iaPixels[me.i][me.j] == 0) {
                    OverlappCircs.setPoint(me, 0);
                }
            }

            ImageInt OverlappCircs2 = new ImageInt(oInnerEdges.iaPixels.length, oInnerEdges.iaPixels[0].length, 0);
            ImageInt Org = new ImageInt(oInnerEdges.iaPixels.length, oInnerEdges.iaPixels[0].length, 0);
            List<List<MatrixEntry>> llmeBounds = new ArrayList<>();
            List<List<MatrixEntry>> llmeArea = new ArrayList<>();
            int iCounter = 254;
            for (MatrixEntry me : ASArea.loPoints) {
                Org.setPoint(me, blackboard.iaPixels[me.i][me.j]);
                if (OverlappCircs.iaPixels[me.i][me.j] == 0) {
                    ArbStructure2 as = new ArbStructure2((ArrayList) new Morphology().markFillN4(OverlappCircs, me.i, me.j));
                    if (as.loPoints.size() > 5) {
                        List<MatrixEntry> meBound = new ArrayList<>();
                        for (MatrixEntry loPoint : as.loPoints) {
                            if (oOuterEdges.iaPixels[loPoint.i][loPoint.j] == 255) {
                                loPoint.dValue = iCounter;
                                meBound.add(loPoint);
                            }
                        }
                        if (meBound.size() > 0) {
                                Circle cc = EllipseDetection.EllipseFit(meBound);
                                 if (cc != null) {
                                double iArea = cc.dDiameterI / 2.0 * cc.dDiameterJ / 2.0 * Math.PI;
                                OverlappCircs2.setPoints(as.loPoints, iCounter);
                                if ((int) iArea < as.loPoints.size()) {
                                    for (ArbStructure2 innerEdge : innerEdges) {
                                        meBound.addAll(getBorderEntries(innerEdge.loPoints, OverlappCircs2, iCounter));
                                    }
                                }
                            }

                            llmeBounds.add(meBound);
                        }
                        OverlappCircs.setPoints(as.loPoints, 255);
//                        OverlappCircs2.setPoints(as.loPoints, iCounter);
                        iCounter -= 50;
                    }
                }
            }
            //*********Find Curv direction of innerbound and add all connected to region from OverlappCircs2
            if (bDesired) {
                for (int i = 0; i < llmeBounds.size(); i++) {
                    Circle cc = EllipseDetection.EllipseFit(llmeBounds.get(i));
                    Org.setPoints(cc.lmeCircle, 255);
                }
                try {
                    IMG_Writer.PaintGreyPNG(Org, new File("C:\\Users\\Nutzer\\Desktop\\owncloudHZDR\\Work\\TianBIT\\BubbleDetec\\tests\\Org.jpeg"));
                } catch (IOException ex) {
                    Logger.getLogger(Prot_ResultDisplayAI_AI_Int.class.getName()).log(Level.SEVERE, null, ex);
                }
//                for (int j = 5; j < 36; j += 5) {
//                    ImageInt oBorderPixels = new ImageInt(oInnerEdges.iaPixels.length, oInnerEdges.iaPixels[0].length, 0);
//                    ImageInt oInnerClone = oInnerEdges.clone();
//                    for (ArbStructure2 innerEdge : innerEdges) {
//                        ArbStructure2 innerEdge2 = innerEdge.clone();
////                        Ziegenhein_2018.thinoutEdges(oInnerClone, innerEdge2);
//                        getCurv(innerEdge2.loPoints, oBorderPixels, 5, 0);
//                    }
//                    try {
//                        IMG_Writer.PaintGreyPNG(oBorderPixels, new File("C:\\Users\\Nutzer\\Desktop\\owncloudHZDR\\Work\\TianBIT\\BubbleDetec\\tests\\Bord_" + 5 + ".jpeg"));
//                    } catch (IOException ex) {
//                        Logger.getLogger(Prot_ResultDisplayAI_AI_Int.class.getName()).log(Level.SEVERE, null, ex);
//                    }
////                }
//
//                for (int i = 0; i < llmeBounds.size(); i++) {
//                    for (int j = 5; j < 36; j += 5) {
//
//                        int iValue = (int) llmeBounds.get(i).get(0).dValue;
//                        List<MatrixEntry> lmeBord = new ArrayList<>();
//                        for (ArbStructure2 innerEdge : innerEdges) {
////                    lmeBord.addAll(getBorderEntries(innerEdge.loPoints, OverlappCircs2, iValue));
//
////                            getCurv(getBorderEntries(innerEdge.loPoints, OverlappCircs2, iValue), oBorderPixels, j, getBorderEntries(innerEdge.loPoints, OverlappCircs2, iValue).size());
////                    getConcavePoints(getBorderEntries(innerEdge.loPoints, OverlappCircs2, iValue), oBorderPixels, OverlappCircs2, j, 3, iValue);
////                    }
//                        }
//
//                    }
//                }
//                
//                oBorderPixels.setPoints(lmeBord, iValue);
            }

//            for (ArbStructure2 innerEdge : innerEdges) {
//                MatrixEntry me = getMeanEntry(innerEdge.loPoints);
//                int iValue = OverlappCircs2.getValue(me);
//                if (iValue != 0) {
//                    for (int i = 0; i < llmeBounds.size(); i++) {
//                        if (llmeBounds.get(i).get(0).dValue == (double) iValue) {
//                            Circle cc = EllipseDetection.EllipseFit(llmeBounds.get(i));
//                            List<MatrixEntry> lmeCirc = cc.getAreaCircle();
//                            
////                            llmeBounds.get(i).addAll(getBorderEntries(innerEdge.loPoints, OverlappCircs2, iValue));
//
//                        }
//                    }
//                }
//            }
            if (llmeBounds.size() > 1) {
                for (List<MatrixEntry> meBound : llmeBounds) {
                    if (meBound.size() > 3) {
                        lCReturn.add(meBound);
                    }

                }

            } else {
                if (lmeOuter.size() > 3) {
                    lCReturn.add(lmeOuter);
                }
            }
        } else {
            if (lmeOuter.size() > 3) {
                lCReturn.add(lmeOuter);
            }
        }
        return lCReturn;
    }

    public static List<MatrixEntry> getBorderEntries(List<MatrixEntry> loPoints, ImageInt OverlappCircs2, int iValue) {
        List<MatrixEntry> lmeReturn = new ArrayList<>();
        for (MatrixEntry me : loPoints) {
            boolean bAdd = false;
            for (MatrixEntry mme : OverlappCircs2.getNeighborsN8(me.i, me.j)) {
                if (mme != null) {
                    if (OverlappCircs2.iaPixels[mme.i][mme.j] == iValue) {
                        bAdd = true;
                    }
                }
            }
            if (bAdd) {
                lmeReturn.add(me);
            }
        }
        return lmeReturn;
    }

    public static List<Circle> Ziegenhein_2019_GUI(List<CPX> allContours, Settings oSettings) throws EmptySetException, IOException {
        Double dDistance = Double.valueOf(oSettings.getSettingsValue("EllipseFit_Ziegenhein2019_Distance").toString());
        double dLeadingDistance = Double.valueOf(oSettings.getSettingsValue("EllipseFit_Ziegenhein2019_LeadingSize").toString());

        //for Debug: ImageInt oBlackBoard
        List<Circle> loReturn = new ArrayList<>();
//        List<CPX> allContours = getAllContours(oEdges);
//        oEdges.resetMarkers();

        for (CPX o : allContours) {
            double dTimer1 = 0.0;
            double dTimer2 = 0.0;
            double dTimer3 = 0.0;
            double dTimer4 = 0.0;
            double dTimer5 = 0.0;
            dTimer1 = System.currentTimeMillis();
            List<CPX> loCPXToConsider = new ArrayList<>();
            List<EllipseDetection.Circle_Fit> loPotentialEllipses = new ArrayList<>();
            if (o.lo.size() > dLeadingDistance && !o.bMarker) {
                ImagePoint opFocal = o.getFocalPoint();

                dTimer2 = System.currentTimeMillis();
                // Find the potential contours that would fit
                for (CPX osub : allContours) {
                    if (osub == null || osub.lo.size() < 5 || osub.bMarker || o.equals(osub) || o.oStart.getNorm(osub.oStart) > 3 * dDistance) {
                        continue;
                    }
                    List<MatrixEntry> lmeAllEdges = new ArrayList<>();
                    for (CPX oExclude : allContours) {
//                        if (oExclude.equals(osub)) {
//                            continue;
//                        }
                        lmeAllEdges.addAll(convert(oExclude.getPoints()));
                    }
                    if (opFocal.getNorm(osub.getFocalPoint()) < dDistance && RayTracingCheck3(convert(o.getFocalPoint()), convert(osub.getFocalPoint()), lmeAllEdges)) {
                        loCPXToConsider.add(osub);
                    }
                }
                dTimer2 = dTimer2 - System.currentTimeMillis();
//                IMG_Writer.PaintGreyPNG(oBlackBoard, new File("E:\\Sync\\openTIV\\Tests\\ImageProc\\EllipseDetect\\_3\\" + "Debug.png"));
                dTimer3 = System.currentTimeMillis();
                // Find the potential ellipses that would fit
                if (loCPXToConsider.isEmpty()) {
                    List<CPX> loContoursToFit = new ArrayList<>();
                    if (o.isClosedContour() && false) {
                        double dAVG = 0.0;
                        for (int i = 0; i < o.lo.size(); i++) {
                            dAVG = dAVG + o.getCurv(i, 5, 5);
                        }
                        dAVG = dAVG / o.lo.size();
                        CPX onew = new CPX();
                        onew.oStart = o.oStart;
                        for (int isub = 1; isub < o.lo.size(); isub++) {
//                            Random rand = new Random();
//                            onew.lo.add(o.lo.get(rand.nextInt(o.lo.size())));                            
                            if (o.getCurv(isub, 5, 5) > dAVG) {
                                onew.lo.add(o.lo.get(isub));
                            }
                        }
//                        onew.oEnd = o.lo.get(iEnd - 1);
                        loContoursToFit.add(onew);
                    } else {
                        loContoursToFit.add(o);
                    }
                    EllipseDetection.Circle_Fit oCosnider_0 = EllipseFit_Contours(loContoursToFit);
                    if (filterEllipse(oSettings, oCosnider_0)) {
                        loPotentialEllipses.add(oCosnider_0);
                    }
                }
                for (CPX oPot : loCPXToConsider) {
                    List<CPX> loContoursToFit = new ArrayList<>();
                    loContoursToFit.add(o);
                    loContoursToFit.add(oPot);
                    EllipseDetection.Circle_Fit oCosnider_1 = EllipseFit_Contours(loContoursToFit);
                    if (filterEllipse(oSettings, oCosnider_1)) {
                        loPotentialEllipses.add(oCosnider_1);
                    }

                    for (CPX oPot_sub : loCPXToConsider) {
                        if (oPot.equals(oPot_sub)) {
                            continue;
                        }
                        loContoursToFit.add(oPot_sub);
                        EllipseDetection.Circle_Fit oCosnider_2 = EllipseFit_Contours(loContoursToFit);
                        if (filterEllipse(oSettings, oCosnider_2)) {
                            loPotentialEllipses.add(oCosnider_2);
                        }
                    }
                }

                if (loPotentialEllipses.isEmpty()) {
                    continue;
                }

                dTimer3 = dTimer3 - System.currentTimeMillis();

                List<EllipseDetection.Circle_Fit> Ellipse_PassedTest = new ArrayList<>();
                for (EllipseDetection.Circle_Fit ofit : loPotentialEllipses) {
                    if (ofit == null) {
                        continue;
                    }
//                    if ((double) ofit.lmeEntriesToFit.size() / (double) ofit.lmeCircle.size() > dPercantagePoints) {
//                        if (CheckDistance(ofit, ofit.lmeEntriesToFit, (int) (0.01 * Math.max(ofit.dDiameterI, ofit.dDiameterJ)), (int) (0.011 * Math.max(ofit.dDiameterI, ofit.dDiameterJ)))) {
                    Ellipse_PassedTest.add(ofit);
//                        }
//                    }
                }

                dTimer4 = System.currentTimeMillis();
                //Find the rigth ellipse of all fitted ellipses
                List<EnumObject> lo = new ArrayList<>();
                for (EllipseDetection.Circle_Fit oPassedFit : Ellipse_PassedTest) {
                    if (oPassedFit == null || oPassedFit.loContours.isEmpty() || oPassedFit.lmeCircle.isEmpty()) {
                        continue;
                    }
                    lo.add(new EnumObject(1.0 * getPixelsOnCircle(oPassedFit, convert_CPX(oPassedFit.loContours), 1), oPassedFit));
                }
                if (lo.isEmpty()) {
                    continue;
                }

                EnumObject oCircWithMaxCirc = Sorting.getMaxCharacteristic(lo, new Sorting.Characteristic<EnumObject>() {

                    @Override
                    public Double getCharacteristicValue(EnumObject pParameter) {
                        return pParameter.dEnum;
                    }
                });
                dTimer4 = dTimer4 - System.currentTimeMillis();
                dTimer5 = System.currentTimeMillis();
                //Check Contours that are close to the ellipse
                EllipseDetection.Circle_Fit oBestFit = (EllipseDetection.Circle_Fit) ((EnumObject) oCircWithMaxCirc.o).o;
                loReturn.add(oBestFit);
                for (CPX oInEllipse : oBestFit.loContours) {
                    oInEllipse.bMarker = true;
                }
                for (CPX oCheckInEllipse : allContours) {
                    if (oCheckInEllipse == null || oCheckInEllipse.lo.size() < 5 || oCheckInEllipse.bMarker || o.oStart.getNorm(oCheckInEllipse.oStart) > 3 * dDistance) {
                        continue;
                    }
                    if (checkIfInside(oBestFit, oCheckInEllipse, 2, oCheckInEllipse.lo.size() / 3)) {
                        oCheckInEllipse.bMarker = true;
                    }
                }

                dTimer5 = dTimer5 - System.currentTimeMillis();
            }
            dTimer1 = dTimer1 - System.currentTimeMillis();
            if (dTimer1 < -10) {
                System.out.println("Overall: " + dTimer1);
                System.out.println("Amount of CPX considered: " + loCPXToConsider.size());
                System.out.println("Amount of Ellipses considered: " + loPotentialEllipses.size());
                System.out.println("Find Contours: " + (int) (dTimer2 / dTimer1 * 100) + " %");
                System.out.println("Find Ellipses: " + (int) (dTimer3 / dTimer1 * 100) + " %");
                System.out.println("Find Best Fit: " + (int) (dTimer4 / dTimer1 * 100) + " %");
                System.out.println("Find close CPX: " + (int) (dTimer5 / dTimer1 * 100) + " %");
                System.out.println("Ratio: " + dTimer1 / dTimer2);
                System.out.println("--------------------------");
            }
        }
        return loReturn;
    }

    public static MatrixEntry getClosest(List<MatrixEntry> lme, MatrixEntry ME) {
        double dMinDist = Double.MAX_VALUE;
        MatrixEntry toAdd = new MatrixEntry();
        for (MatrixEntry me : lme) {
            double dDist = me.getNorm(ME);
            if (dDist < dMinDist && !me.equals(ME)) {
                toAdd = me;
                dMinDist = dDist;
            }
        }
        return toAdd;
    }

    public static void connectAlongShortestPath(ArbStructure2 innerEdge, MatrixEntry meFirst, MatrixEntry meSecond, ImageInt oInnerEdges) {
        List<MatrixEntry> lmePath = getShortPathAlongCloud(innerEdge, meFirst, meSecond);
        if (lmePath.isEmpty()) {
            Line oLine = new Line(meFirst, meSecond);
            oLine.setLine(oInnerEdges, 70);
        } else {
            oInnerEdges.setPoints(lmePath, 80);
        }
    }

    public static List<MatrixEntry> getShortPathAlongCloud(ArbStructure2 innerEdge, MatrixEntry meStart, MatrixEntry meEnd) {
        List<MatrixEntry> lmePath = new ArrayList<>();
        lmePath.add(meStart);
        int iCounter = 0;

        while (!lmePath.get(lmePath.size() - 1).equals(meEnd) && iCounter < innerEdge.loPoints.size()) {
            getConnectedCloseToEnd(innerEdge.loPoints, lmePath.get(lmePath.size() - 1), meEnd, lmePath);
            iCounter++;
        }
        if (iCounter == innerEdge.loPoints.size()) {
            lmePath.clear();
        }
        return lmePath;
    }

    public static void getConnectedCloseToEnd(List<MatrixEntry> lme, MatrixEntry meBefore, MatrixEntry meEnd, List<MatrixEntry> lmeList) {
        List<MatrixEntry> lmeConnected = new ArrayList<>();
        for (MatrixEntry me : lme) {
            if (!me.equals(meBefore) && MatrixEntry.isConnected(me, meBefore)) {
                lmeConnected.add(me);
            }
        }
        if (lmeConnected.size() > 1) {
            double dMinDist = Double.MAX_VALUE;
            MatrixEntry toAdd = new MatrixEntry();
            for (MatrixEntry me : lmeConnected) {
                if (!containsME(lmeList, me)) {
                    double dDist = me.getNorm(meEnd);
                    if (dDist < dMinDist) {
                        toAdd = me;
                        dMinDist = dDist;
                    }
                }
            }
            lmeList.add(toAdd);
        } else if (lmeConnected.size() == 1) {
            lmeList.add(lmeConnected.get(0));
        }
    }

    public static boolean containsME(List<MatrixEntry> lme, MatrixEntry me) {
        for (MatrixEntry matrixEntry : lme) {
            if (matrixEntry.equals(me)) {
                return true;
            }
        }
        return false;
    }

    public static List<MatrixEntry> getBorder(List<MatrixEntry> lme) {
        List<MatrixEntry> lmeSorted = new ArrayList<>();
        int iLength = lme.size();
        MatrixEntry meCurrent = lme.get(0);
        lmeSorted.add(meCurrent);
        lme.remove(meCurrent);
        while (lmeSorted.size() < iLength || lme.size() > 0) {
            meCurrent = getClosestEntry(meCurrent, lme);
            lmeSorted.add(meCurrent);
            lme.remove(meCurrent);
        }
        return lmeSorted;
    }

    public static void getCurv(List<MatrixEntry> loPoints, ImageInt oOuter, int iJump, int iLength) {
        double dMax = 0.0;
//        List<MatrixEntry> lme = new ArrayList<>();
//        for (MatrixEntry me : loPoints) {
//            List<MatrixEntry> lmeHelp = new ArrayList<>();
//            List<MatrixEntry> lmeMean = new ArrayList<>();
//            for (MatrixEntry asme : loPoints) {
//                lmeHelp.add(new MatrixEntry(asme));
//            }
//            List<OrderedPair> lop = new ArrayList<>();
//            MatrixEntry meCurrent = me;
//            lop.add(me.toOrderedPair());
//            lmeMean.add(me);
//            for (int i = 0; i < iLength; i++) {
//                MatrixEntry mme = getClosestEntry(meCurrent, lmeHelp);
//                lop.add(mme.toOrderedPair());
//                lmeMean.add(mme);
//                meCurrent = mme;
//                lmeHelp.remove(meCurrent);
//            }
//            meCurrent = me;
//            for (int i = 0; i < iLength; i++) {
//                MatrixEntry mme = getClosestEntry(meCurrent, lmeHelp);
//                lop.add(0, mme.toOrderedPair());
//                lmeMean.add(0, mme);
//                meCurrent = mme;
//                lmeHelp.remove(meCurrent);
//            }
//            PLF oplf = new PLF(lop);
//            OrderedPair opcurve = oplf.getCurv(iJump, iLength, iJump);
////            meCurrent = getMeanEntry(lmeMean);
////            if (iMask.iaPixels[meCurrent.i][meCurrent.j] == 0) {
////                opcurve.dValue = opcurve.dValue * 2.0;
//////                System.out.println("Posi " + me);
//////                System.out.println("Mean " + meCurrent);
//////                oOuter.setPoint(me, 255);
////            }
////            System.out.println(lop.get(10));
////            System.out.println(opcurve.dValue);
//
////            oOuter.setPoint(me, (int) (opcurve.dValue * 254));
//            me.dValue = opcurve.dValue * 254;
//            lme.add(me);
//        }

        List<OrderedPair> lop = new ArrayList<>();
        for (MatrixEntry me : loPoints) {
            lop.add(new OrderedPair(me.j, me.i));
        }
        List<MatrixEntry> lmeHelp = new ArrayList<>();
        PLF oplf = new PLF(lop);
        for (int i = 0; i < oplf.getPoints().size(); i++) {
            OrderedPair opcurve = oplf.getCurv(iJump, i, iJump);
            if (opcurve.dValue > dMax) {
                dMax = opcurve.dValue;
            }
            lmeHelp.add(new MatrixEntry((int) oplf.getPoints().get(i).y, (int) oplf.getPoints().get(i).x, opcurve.dValue * 254));
        }

        for (MatrixEntry me : lmeHelp) {
            me.dValue = me.dValue / dMax;
            oOuter.setPoint(me, (int) me.dValue);
        }

    }

    public static void getConcavePoints(List<MatrixEntry> loPoints, ImageInt oOuter, ImageInt iMask, int iLength, double PixelDist, int iMaskValue) {
        for (MatrixEntry me : loPoints) {
//            System.out.println("New "+me);
            List<MatrixEntry> lmeHelp = new ArrayList<>();
            List<MatrixEntry> lmeMean = new ArrayList<>();
            for (MatrixEntry asme : loPoints) {
                lmeHelp.add(new MatrixEntry(asme));
            }
            MatrixEntry meCurrent = me;
            lmeMean.add(me);
            for (int i = 0; i < iLength; i++) {
                MatrixEntry mme = getClosestEntry(meCurrent, lmeHelp);
                lmeMean.add(mme);
                meCurrent = mme;
                lmeHelp.remove(meCurrent);
            }
            meCurrent = me;
            for (int i = 0; i < iLength; i++) {
                MatrixEntry mme = getClosestEntry(meCurrent, lmeHelp);
                lmeMean.add(0, mme);
                meCurrent = mme;
                lmeHelp.remove(meCurrent);
            }
            meCurrent = getMeanEntry(lmeMean);
            if (iMask.iaPixels[meCurrent.i][meCurrent.j] == iMaskValue) {
                boolean bOut = true;
                for (MatrixEntry meOuter : lmeMean) {
                    if (meCurrent.getNorm(meOuter) < PixelDist) {
                        bOut = false;

                    }
                }
                if (bOut) {
                    oOuter.setPoint(me, 255);
                }
            }
        }

    }

    public static MatrixEntry getCenterOfBorder(List<MatrixEntry> lme) {
        MatrixEntry meCurrent = new MatrixEntry(lme.get(0));
        MatrixEntry meStrat = new MatrixEntry(0, 0);
        List<MatrixEntry> lmeHelp = new ArrayList<>();
        List<MatrixEntry> lmeHelp2 = new ArrayList<>();
        for (MatrixEntry asme : lme) {
            lmeHelp.add(new MatrixEntry(asme));
        }
        for (int i = 0; i < lme.size(); i++) {
            lmeHelp.remove(meCurrent);
            lmeHelp2.add(meCurrent);
            meCurrent = getClosestEntry(meCurrent, lmeHelp);
            double dDist = lmeHelp2.get(i).getNorm(meCurrent);
            if (dDist >= 2.0) {
                meStrat = lmeHelp2.get(i);
                break;
            }

        }
        lmeHelp.clear();
        for (MatrixEntry asme : lme) {
            lmeHelp.add(new MatrixEntry(asme));
        }
        List<MatrixEntry> lmeSorted = new ArrayList<>();
        for (int i = 0; i < lme.size(); i++) {
            lmeHelp.remove(meStrat);
            lmeSorted.add(meStrat);
            meStrat = getClosestEntry(meStrat, lmeHelp);
        }
        return lmeSorted.get(lmeSorted.size() / 2);
    }

    public static MatrixEntry getMostDistantEntryInDirection(MatrixEntry me, OrderedPair op, List<MatrixEntry> lme) {
        double dMaxDist = 0.0;
        MatrixEntry meReturn = me;
        for (MatrixEntry mme : lme) {
            if (checkCorrectDirection(op, me, mme) && me.getNorm(mme) > dMaxDist) {
                dMaxDist = me.getNorm(mme);
                meReturn = mme;
            }
        }
        return meReturn;
    }

    public static MatrixEntry getClosestEntry(MatrixEntry me, List<MatrixEntry> lme) {
        double dMinDist = Double.MAX_VALUE;
        MatrixEntry meReturn = me;
        for (MatrixEntry mme : lme) {
            if (me.getNorm(mme) < dMinDist && !mme.equals(me)) {
                dMinDist = me.getNorm(mme);
                meReturn = mme;
            }
        }
        return meReturn;
    }

    public static MatrixEntry getClosestEntryConnected(MatrixEntry me, List<MatrixEntry> lme) {
        double dMinDist = Double.MAX_VALUE;
        MatrixEntry meReturn = me;
        for (MatrixEntry mme : lme) {
            if (me.getNorm(mme) < dMinDist && !mme.equals(me) && me.getNorm(mme) <= Math.sqrt(2.0)) {
                dMinDist = me.getNorm(mme);
                meReturn = mme;
            }
        }
        if (!meReturn.equals(me)) {
            return meReturn;
        } else {
            return null;
        }
    }

    public static MatrixEntry getMeanEntry(List<MatrixEntry> lme) {
        MatrixEntry me = new MatrixEntry(0, 0);
        for (MatrixEntry mme : lme) {
            me.i += mme.i;
            me.j += mme.j;
        }
        me.i = me.i / lme.size();
        me.j = me.j / lme.size();
        return me;
    }

    public static void setEnds(ImageInt oInnerEdges, ImageInt oOuterEdges, ArbStructure2 as, int iValue) {
        List<MatrixEntry> lme = new ArrayList<>();
        for (MatrixEntry me : as.loPoints) {
            //****Check if border around N8
            N8 oN8Outer = new N8(oOuterEdges, me.i, me.j);
            boolean bNeighOuter = false;
            for (int i = 0; i < 7; i++) {
                if (oN8Outer.isNeigh(i)) {
                    bNeighOuter = true;
                }
            }

            if (oInnerEdges.iaPixels[me.i][me.j] > 0 && oOuterEdges.iaPixels[me.i][me.j] != 255 && !bNeighOuter) {
                N8 oN8 = new N8(oInnerEdges, me.i, me.j);
                if (oN8.getBP() == 1) {
                    oInnerEdges.setPoint(me, iValue);
                }
            }
        }

        if (lme.size()
                > 0) {
            for (MatrixEntry me : lme) {
                as.loPoints.remove(me);
            }
        }
    }

    public static void setEnds2(ImageInt oInnerEdges, ImageInt oOuterEdges, ArbStructure2 as, int iValue) {
        List<MatrixEntry> lme = new ArrayList<>();
        for (MatrixEntry me : as.loPoints) {
            //****Check if border around N8
            N8 oN8Outer = new N8(oOuterEdges, me.i, me.j);
            boolean bNeighOuter = false;
            for (int i = 0; i < 7; i++) {
                if (oN8Outer.isNeigh(i)) {
                    bNeighOuter = true;
                }
            }

            if (oInnerEdges.iaPixels[me.i][me.j] > 0 && oOuterEdges.iaPixels[me.i][me.j] != 255 && !bNeighOuter) {
                N8 oN8 = new N8(oInnerEdges, me.i, me.j);
                if (oN8.getBP() == 1) {
                    oInnerEdges.setPoint(me, iValue);
                }
            }
        }

        if (lme.size()
                > 0) {
            for (MatrixEntry me : lme) {
                as.loPoints.remove(me);
            }
        }
    }

    public static void markOuterEnds(ArbStructure2 AS, ImageInt oInnerEdges, boolean b3P, int iValue) {

        if (b3P) {
            for (MatrixEntry me : AS.loPoints) {
                N8 oN8 = new N8(oInnerEdges, me.i, me.j);
                if (oN8.getBP() == 1
                        || (oN8.getBP() == 2 && oN8.getC2P() == 1)
                        || (oN8.getBP() == 3 && oN8.getC3P() == 1)) {
                    oInnerEdges.setPoint(me, iValue);
                }
            }
        } else {
            for (MatrixEntry me : AS.loPoints) {
                N8 oN8 = new N8(oInnerEdges, me.i, me.j);
                if (oN8.getBP() == 1
                        || (oN8.getBP() == 2 && oN8.getC2P() == 1)) {
                    oInnerEdges.setPoint(me, iValue);
                }
            }
        }

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

    public static void connectEdgesCloseToBorder(ArbStructure2 OuterEdge, ArbStructure2 InnerEdge, ImageInt oInnerEdges, double PixelDist, int iValueCheck, int iValue, ImageInt internalWhite) {
        for (MatrixEntry meInner : InnerEdge.loPoints) {
            if (oInnerEdges.iaPixels[meInner.i][meInner.j] == iValueCheck) {
                connectPointToBorder(OuterEdge.loPoints, meInner, PixelDist, iValue, oInnerEdges, null, null, internalWhite);
            }
        }
    }

    public static boolean connectSingleMarkertoIE(List<ArbStructure2> lInnerEdges, MatrixEntry meInner, ImageInt oInnerEdges, double PixelDist, int iValue) {
        List<MatrixEntry> lmeCandidates = new ArrayList<>();
        boolean connect = false;
        MatrixEntry toConnect = new MatrixEntry();
        double dMinDist = Double.MAX_VALUE;
        for (ArbStructure2 InnerEdge2 : lInnerEdges) {
            if (!checkifonborder(meInner, InnerEdge2)) {
                lmeCandidates.addAll(InnerEdge2.loPoints);
            }
        }
        for (MatrixEntry meOuter : lmeCandidates) {
            if (meInner.getNorm(meOuter) < PixelDist && meInner.getNorm(meOuter) < dMinDist) {
                dMinDist = meInner.getNorm(meOuter);
                toConnect = meOuter;
                connect = true;
            }
        }
        if (connect) {
            Line oLine = new Line(meInner, toConnect);
            oLine.setLine(oInnerEdges, iValue);
            for (MatrixEntry me : oLine.lmeLine) {
                oInnerEdges.setPoint(new MatrixEntry(me.i + 1, me.j), iValue);
                oInnerEdges.setPoint(new MatrixEntry(me.i + 1, me.j + 1), iValue);
                oInnerEdges.setPoint(new MatrixEntry(me.i + 1, me.j - 1), iValue);
                oInnerEdges.setPoint(new MatrixEntry(me.i, me.j + 1), iValue);
                oInnerEdges.setPoint(new MatrixEntry(me.i, me.j - 1), iValue);
                oInnerEdges.setPoint(new MatrixEntry(me.i - 1, me.j + 1), iValue);
                oInnerEdges.setPoint(new MatrixEntry(me.i - 1, me.j - 1), iValue);
                oInnerEdges.setPoint(new MatrixEntry(me.i - 1, me.j), iValue);
            }
        }
        return connect;
    }

    public static boolean connectPointToBorder(List<MatrixEntry> OuterEdge, MatrixEntry meInner, double PixelDist, int iValue, ImageInt oInnerEdges, OrderedPair op, List<ArbStructure2> lAS, ImageInt internalWhite) {
        boolean connect = false;
        double dMinDist = Double.MAX_VALUE;
        MatrixEntry toConnect = new MatrixEntry();
//        OrderedPair opOuter = new OrderedPair();
        if (op == null && lAS == null) {
            for (MatrixEntry meOuter : OuterEdge) {
                if (meInner.getNorm(meOuter) < PixelDist) {
                    Line oLine = new Line(meInner, meOuter);
                    if (!oLine.checkIfCrossValue(internalWhite, 255)) {
                        oLine.setLine(oInnerEdges, iValue);
                        for (MatrixEntry me : oLine.lmeLine) {
                            oInnerEdges.setPoint(new MatrixEntry(me.i + 1, me.j), iValue);
                            oInnerEdges.setPoint(new MatrixEntry(me.i + 1, me.j + 1), iValue);
                            oInnerEdges.setPoint(new MatrixEntry(me.i + 1, me.j - 1), iValue);
                            oInnerEdges.setPoint(new MatrixEntry(me.i, me.j + 1), iValue);
                            oInnerEdges.setPoint(new MatrixEntry(me.i, me.j - 1), iValue);
                            oInnerEdges.setPoint(new MatrixEntry(me.i - 1, me.j + 1), iValue);
                            oInnerEdges.setPoint(new MatrixEntry(me.i - 1, me.j - 1), iValue);
                            oInnerEdges.setPoint(new MatrixEntry(me.i - 1, me.j), iValue);
                        }
                        connect = true;
                    }
                }
            }

        } else if (op != null && lAS == null) {
            for (MatrixEntry meOuter : OuterEdge) {
                if (meInner.getNorm(meOuter) < PixelDist && meInner.getNorm(meOuter) < dMinDist && checkCorrectDirection(op, meInner, meOuter)) {
                    dMinDist = meInner.getNorm(meOuter);
                    toConnect = meOuter;
                    connect = true;
                }
            }
            if (connect) {

                Line oLine = new Line(meInner, toConnect);
                if (!oLine.checkIfCrossValue(internalWhite, 255)) {
                    oLine.setLine(oInnerEdges, iValue);
                    for (MatrixEntry me : oLine.lmeLine) {
                        oInnerEdges.setPoint(new MatrixEntry(me.i + 1, me.j), iValue);
                        oInnerEdges.setPoint(new MatrixEntry(me.i + 1, me.j + 1), iValue);
                        oInnerEdges.setPoint(new MatrixEntry(me.i + 1, me.j - 1), iValue);
                        oInnerEdges.setPoint(new MatrixEntry(me.i, me.j + 1), iValue);
                        oInnerEdges.setPoint(new MatrixEntry(me.i, me.j - 1), iValue);
                        oInnerEdges.setPoint(new MatrixEntry(me.i - 1, me.j + 1), iValue);
                        oInnerEdges.setPoint(new MatrixEntry(me.i - 1, me.j - 1), iValue);
                        oInnerEdges.setPoint(new MatrixEntry(me.i - 1, me.j), iValue);
                    }
                    for (MatrixEntry me : OuterEdge) {
                        if (me.getNorm(toConnect) <= 2.0) {
                            oInnerEdges.setPoint(me, iValue);
                        }
                    }
                }
            }

        } else {
            for (MatrixEntry meOuter : OuterEdge) {
                ArbStructure2 as = lAS.get(OuterEdge.indexOf(meOuter));
                OrderedPair opp = getDerivationOutwardsEndPoints(meOuter, as.loPoints.size() / 2, as.loPoints);
                if (meInner.getNorm(meOuter) < PixelDist && checkCorrectDirection(op, meInner, meOuter) && checkCorrectDirection(opp, meOuter, meInner) && meInner.getNorm(meOuter) < dMinDist && !meInner.equalsMatrixEntry(meOuter)) {
//                    opOuter = opp;
                    dMinDist = meInner.getNorm(meOuter);
                    toConnect = meOuter;
                    connect = true;
                }
            }
            if (connect) {
                Line oLine = new Line(meInner, toConnect);
                if (!oLine.checkIfCrossValue(internalWhite, 255)) {
                    oLine.setLine(oInnerEdges, iValue);
                    for (MatrixEntry me : oLine.lmeLine) {
                        oInnerEdges.setPoint(new MatrixEntry(me.i + 1, me.j), iValue);
                        oInnerEdges.setPoint(new MatrixEntry(me.i + 1, me.j + 1), iValue);
                        oInnerEdges.setPoint(new MatrixEntry(me.i + 1, me.j - 1), iValue);
                        oInnerEdges.setPoint(new MatrixEntry(me.i, me.j + 1), iValue);
                        oInnerEdges.setPoint(new MatrixEntry(me.i, me.j - 1), iValue);
                        oInnerEdges.setPoint(new MatrixEntry(me.i - 1, me.j + 1), iValue);
                        oInnerEdges.setPoint(new MatrixEntry(me.i - 1, me.j - 1), iValue);
                        oInnerEdges.setPoint(new MatrixEntry(me.i - 1, me.j), iValue);
                    }
                }
            }
        }

        return connect;
    }

    public static void connectInsideEdges(List<ArbStructure2> InnerEdges, ArbStructure2 InnerEdge, ImageInt oInnerEdges, double PixelDist, int iValueCheck, int iValue, ArbStructure2 OuterEdge, ImageInt iGrad, ImageInt internalWhite) {
        List<MatrixEntry> lIEOther = new ArrayList<>();
        List<MatrixEntry> lIECurrent = new ArrayList<>();
        List<MatrixEntry> lmeRest = new ArrayList<>();
//        List<ArbStructure2> asHelp = new ArrayList<>();
        for (ArbStructure2 InnerEdge1 : InnerEdges) {
            for (MatrixEntry meInner : InnerEdge1.loPoints) {
                if (!InnerEdge.containsPoint(meInner)) {
                    lmeRest.add(meInner);
                    if (oInnerEdges.iaPixels[meInner.i][meInner.j] == iValueCheck) {
                        lIEOther.add(meInner);
                    }
                } else {
                    if (oInnerEdges.iaPixels[meInner.i][meInner.j] == iValueCheck) {
                        lIECurrent.add(meInner);
                    }
                }
            }

        }
        MatrixEntry meCenter = new MatrixEntry(InnerEdge.getPosition());
        for (MatrixEntry meInner : lIECurrent) {
            oInnerEdges.setPoint(meInner, 255);
            List<MatrixEntry> lmeClose = new ArrayList<>();
            lmeClose.add(meInner);
            for (MatrixEntry meRest : lIECurrent) {
                if (!meInner.equals(meRest) && meInner.getNorm(meRest) < PixelDist) {
                    lmeClose.add(meRest);
                }
            }
            MatrixEntry mme = meInner;
            if (lmeClose.size() > 1) {
                double dMaxDist = 0.0;
                for (MatrixEntry me : lmeClose) {
                    oInnerEdges.setPoint(me, 255);
                    if (me.getNorm(meCenter) > dMaxDist) {
                        dMaxDist = me.getNorm(meCenter);
                        mme = me;
                    }
                }
            }
            oInnerEdges.setPoint(mme, iValueCheck);
        }

        for (MatrixEntry meInner : InnerEdge.loPoints) {
            if (oInnerEdges.iaPixels[meInner.i][meInner.j] == iValueCheck && !checkifonborder(meInner, OuterEdge)) {
                List<MatrixEntry> lmeHelp = new ArrayList<>();
                lmeHelp.addAll(InnerEdge.loPoints);
                OrderedPair opDirection = getDerivationOutwardsEndPoints(meInner, lmeHelp.size() / 2, lmeHelp);
                boolean check = false;
                if (opDirection.x != 0 || opDirection.y != 0) {
                    if (lIEOther.size() > 0) {
                        check = connectPointToBorder(lIEOther, meInner, PixelDist, iValue, oInnerEdges, opDirection, null, internalWhite);
                        if (check) {
//                            System.out.println("Connected to other Marker");
//                                break;
                        }
                    }

                    if (!check) {
                        check = connectPointToBorder(lmeRest, meInner, PixelDist, iValue, oInnerEdges, opDirection, null, internalWhite);
                        if (check) {
//                            System.out.println("Connected to other IE");
//                                break;
                        }
                    }
                    if (!check) {
                        check = connectPointToBorder(OuterEdge.loPoints, meInner, PixelDist / 2, iValue, oInnerEdges, opDirection, null, internalWhite);
                        if (check) {
//                            System.out.println("Connected to Border");
//                                break;
                        }
                    }
                }
            }
        }
    }

    public static boolean checkifonStruc(MatrixEntry me, List<ArbStructure2> lsAsS) {
        boolean bTrue = false;
        for (ArbStructure2 as : lsAsS) {
            bTrue = checkifonborder(me, as);
        }
        return bTrue;
    }

    public static boolean checkifonborder(MatrixEntry me, ArbStructure2 oEdge) {
        for (MatrixEntry meIn : oEdge.loPoints) {
            if (meIn.equalsMatrixEntry(me)) {
                return true;
            }
        }
        return false;
    }

    public static boolean checkCorrectDirection(OrderedPair op, MatrixEntry me1, MatrixEntry me2) {
        if (op.x >= 0 && op.y >= 0 && me2.j >= me1.j && me2.i >= me1.i) {
            return true;
        }
        if (op.x >= 0 && op.y <= 0 && me2.j >= me1.j && me2.i <= me1.i) {
            return true;
        }
        if (op.x <= 0 && op.y <= 0 && me2.j <= me1.j && me2.i <= me1.i) {
            return true;
        }
        if (op.x <= 0 && op.y >= 0 && me2.j <= me1.j && me2.i >= me1.i) {
            return true;
        }
        return false;
    }

    public static OrderedPair getDerivationOutwardsEndPoints(MatrixEntry oRef, int iLength, List<MatrixEntry> lo) {
//            if (oRef.equals(this.oStart)) {
        //Directional of Start
        double dX1 = 0;
        double dY1 = 0;
        int iCount = 0;
        for (int i = Math.min(lo.size() - iLength, iLength); i >= 1; i--) {
            dX1 = dX1 + (lo.get(i - 1).getPosX() - lo.get(i).getPosX()); // / (lo.get(i - 1).getPos().getNorm( lo.get(i).getPos())) ;
            dY1 = dY1 + (lo.get(i - 1).getPosY() - lo.get(i).getPosY()); // / (lo.get(i - 1).getPos().getNorm( lo.get(i).getPos()));
            iCount++;
        }
        dX1 = dX1 + (oRef.getPosX() - lo.get(0).getPosX());
        dY1 = dY1 + (oRef.getPosY() - lo.get(0).getPosY());
        iCount++;
        dX1 = dX1 / (1.0 * (iCount));
        dY1 = dY1 / (1.0 * (iCount));

        return new OrderedPair(dX1, dY1);
    }

    @Override
    public String getType() {
        return name;
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

        SettingsCluster boundSplit = new SettingsCluster("Contour Splitting",
                new String[]{"iCurvOrder", "iTangOrder", "dCurvThresh"}, this);
        boundSplit.setDescription("Contour Splitting");
        lsClusters.add(boundSplit);

        SettingsCluster boundTrack = new SettingsCluster("Boundary Tracking",
                new String[]{"BUBSRadiusYPlus", "BUBSRadiusYMinus", "BUBSRadiusXPlus", "BUBSRadiusXMinus", "tivBUBColBar"}, this);
        boundTrack.setDescription("Boundary Tracking");
        lsClusters.add(boundTrack);

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
        this.loSettings.add(new SettingObject("RatioFilter_Min_Value", "RatioFilter_Min_Value", 0, SettingObject.SettingsType.Integer));
        this.loSettings.add(new SettingObject("Size_Max", "Size_Max", false, SettingObject.SettingsType.Boolean));
        this.loSettings.add(new SettingObject("Size_Max_Value", "Size_Max_Value", 10000, SettingObject.SettingsType.Integer));
        this.loSettings.add(new SettingObject("Size_Min", "Size_Min", false, SettingObject.SettingsType.Boolean));
        this.loSettings.add(new SettingObject("Size_Min_Value", "Size_Min_Value", 1, SettingObject.SettingsType.Integer));
        this.loSettings.add(new SettingObject("Major_Max", "Major_Max", false, SettingObject.SettingsType.Boolean));
        this.loSettings.add(new SettingObject("Major_Max_Value", "Major_Max_Value", 10000, SettingObject.SettingsType.Integer));
        this.loSettings.add(new SettingObject("Major_Min", "Major_Min", false, SettingObject.SettingsType.Boolean));
        this.loSettings.add(new SettingObject("Major_Min_Value", "Major_Min_Value", 0, SettingObject.SettingsType.Integer));
        this.loSettings.add(new SettingObject("Minor_Max", "Minor_Max", false, SettingObject.SettingsType.Boolean));
        this.loSettings.add(new SettingObject("Minor_Max_Value", "Minor_Max_Value", 10000, SettingObject.SettingsType.Integer));
        this.loSettings.add(new SettingObject("Minor_Min", "Minor_Min", false, SettingObject.SettingsType.Boolean));
        this.loSettings.add(new SettingObject("Minor_Min_Value", "Minor_Min_Value", 1, SettingObject.SettingsType.Integer));

        this.loSettings.add(new SettingObject("Curvature Order", "iCurvOrder", 5, SettingObject.SettingsType.Integer));
        this.loSettings.add(new SettingObject("Tang Order", "iTangOrder", 10, SettingObject.SettingsType.Integer));
        this.loSettings.add(new SettingObject("Curvature Threshold", "dCurvThresh", 0.075, SettingObject.SettingsType.Double));

        //Tracking
        this.loSettings.add(new SettingObject("Search Radius Y Max", "BUBSRadiusYPlus", 20, SettingObject.SettingsType.Integer));
        this.loSettings.add(new SettingObject("Search Radius Y Min", "BUBSRadiusYMinus", 5, SettingObject.SettingsType.Integer));
        this.loSettings.add(new SettingObject("Search Radius X Max", "BUBSRadiusXPlus", 20, SettingObject.SettingsType.Integer));
        this.loSettings.add(new SettingObject("Search Radius X Min", "BUBSRadiusXMinus", -20, SettingObject.SettingsType.Integer));
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
