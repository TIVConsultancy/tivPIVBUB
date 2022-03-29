/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.tivconsultancy.tivpivbub.protocols;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.tivconsultancy.opentiv.edgedetector.OpenTIV_Edges;
import com.tivconsultancy.opentiv.helpfunctions.matrix.MatrixEntry;
import com.tivconsultancy.opentiv.helpfunctions.settings.SettingObject;
import com.tivconsultancy.opentiv.helpfunctions.settings.Settings;
import com.tivconsultancy.opentiv.helpfunctions.settings.SettingsCluster;
import com.tivconsultancy.opentiv.highlevel.protocols.NameSpaceProtocolResults1D;
import com.tivconsultancy.opentiv.highlevel.protocols.Protocol;
import com.tivconsultancy.opentiv.highlevel.protocols.UnableToRunException;
import com.tivconsultancy.opentiv.imageproc.algorithms.algorithms.BasicIMGOper;
import static com.tivconsultancy.opentiv.imageproc.algorithms.algorithms.EdgeDetections.getThinEdge;
import com.tivconsultancy.opentiv.imageproc.algorithms.algorithms.EllipseDetection;
import com.tivconsultancy.opentiv.imageproc.algorithms.algorithms.Morphology;
import com.tivconsultancy.opentiv.imageproc.algorithms.algorithms.N8;
import com.tivconsultancy.opentiv.imageproc.img_io.IMG_Writer;
import com.tivconsultancy.opentiv.imageproc.primitives.ImageInt;
import com.tivconsultancy.opentiv.imageproc.shapes.ArbStructure2;
import com.tivconsultancy.opentiv.imageproc.shapes.Circle;
import com.tivconsultancy.opentiv.imageproc.shapes.Line;
import com.tivconsultancy.opentiv.imageproc.shapes.Shape;
import com.tivconsultancy.opentiv.math.primitives.OrderedPair;
import com.tivconsultancy.opentiv.physics.vectors.VelocityVec;
import com.tivconsultancy.opentiv.preprocessor.OpenTIV_PreProc;
import com.tivconsultancy.tivGUI.StaticReferences;
import com.tivconsultancy.tivpivbub.PIVBUBController;
import com.tivconsultancy.tivpivbub.data.DataBUB.BubbleJSON;
import delete.com.tivconsultancy.opentiv.devgui.main.ImagePath;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
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
public class Prot_tivPIVBUBBubbleFinder extends Protocol {

    private static final long serialVersionUID = 7641291376662282578L;

    String name = "Bubble Detector";

    ImageInt edges1st;
    ImageInt edges2nd;

    ImageInt shapeFit;

    public Prot_tivPIVBUBBubbleFinder() {
        edges1st = new ImageInt(50, 50, 0);
        shapeFit = new ImageInt(50, 50, 0);
        buildLookUp();
        initSettins();
        buildClusters();
    }

    private void buildLookUp() {
        if (edges1st != null) {
            ((PIVBUBController) StaticReferences.controller).getDataBUB().setImage(outNames.EdgesShapeFit.toString(), edges1st.getBuffImage());
        }
        if (edges2nd != null || shapeFit != null) {
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
    public void setImage(BufferedImage bi) {
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
        List<ImagePath> sNames = controller.getCurrentMethod().getInputImages();
        ImageInt readFirst = new ImageInt(controller.getDataPIV().iaReadInFirst);
        ImageInt grad = new ImageInt(readFirst.iaPixels.length, readFirst.iaPixels[0].length);
        String sPath = controller.getCurrentFileSelected().getParent();
        String sFolder = (String) controller.getCurrentMethod().getProtocol("mask").getSettingsValue("mask_Path");
        boolean bTracking = controller.getCurrentMethod().getProtocol("bubtrack").getSettingsValue("Tracking").toString().contains("Disable_Tracking") ? false : true;
        //******Method 1*******
        if (this.getSettingsValue("Reco").toString().contains("Edge_Detector_and_Ellipse_fit")) {

            if (controller.getDataBUB().results_Shape_2nd == null) {
                System.out.println("Bubble Identification using Edge_Detector_and_Ellipse_fit");
                edges1st = OpenTIV_Edges.performEdgeDetecting(this, readFirst);
                controller.getDataBUB().iaEdgesRAWFirst = edges1st.clone().iaPixels;
                edges1st = OpenTIV_Edges.performEdgeOperations(this, edges1st, readFirst);
                controller.getDataBUB().results_Shape = OpenTIV_Edges.performShapeFitting(this, edges1st);
            } else {
                controller.getDataBUB().results_Shape = controller.getDataBUB().results_Shape_2nd;
            }

            if (bTracking) {
                ImageInt readSecond = new ImageInt(controller.getDataPIV().iaReadInSecond);
                edges2nd = OpenTIV_Edges.performEdgeDetecting(this, readSecond);
                controller.getDataBUB().iaEdgesRAWSecond = edges2nd.clone().iaPixels;
                edges2nd = OpenTIV_Edges.performEdgeOperations(this, edges2nd, readSecond);
                controller.getDataBUB().results_Shape_2nd = OpenTIV_Edges.performShapeFitting(this, edges2nd);

            }

            //******Method 2*******
        } else if (this.getSettingsValue("Reco").toString().contains("Read_Mask_and_Ellipse_fit")) {

            if (controller.getDataBUB().results_Shape_2nd == null) {
                System.out.println("Bubble Identification using given Mask and Ellipse fit");
                
                grad.iaPixels = getThinEdge(readFirst.iaPixels, Boolean.FALSE, null, null, 1);
                
                ImageInt iMask1 = (ImageInt) controller.getCurrentMethod().getProtocol("mask").getResults()[1];
                try {
                    List<Shape> lsC = getBoundEllipses(iMask1, grad);
                    controller.getDataBUB().results_Shape = new OpenTIV_Edges.ReturnContainer_Shape(lsC, readFirst);

                } catch (IOException ex) {
                    Logger.getLogger(Prot_tivPIVBUBBubbleFinder.class.getName()).log(Level.SEVERE, null, ex);
                }
            } else {
                controller.getDataBUB().results_Shape = controller.getDataBUB().results_Shape_2nd;
            }

            if (bTracking) {
                ImageInt readSecond = new ImageInt(controller.getDataPIV().iaReadInSecond);
                //ImageInt grad = new ImageInt(readSecond.iaPixels.length, readSecond.iaPixels[0].length);
                grad.iaPixels = getThinEdge(readSecond.iaPixels, Boolean.FALSE, null, null, 1);
                ImageInt iMask1 = (ImageInt) controller.getCurrentMethod().getProtocol("mask").getResults()[2];
                try {
                    List<Shape> lsC = getBoundEllipses(iMask1, grad);
                    controller.getDataBUB().results_Shape_2nd = new OpenTIV_Edges.ReturnContainer_Shape(lsC, readFirst);

                } catch (IOException ex) {
                    Logger.getLogger(Prot_tivPIVBUBBubbleFinder.class.getName()).log(Level.SEVERE, null, ex);
                }

            }

            //******Method 3*******
        } else if (this.getSettingsValue("Reco").toString().contains("ReadMaskandPoints")) {

            if (controller.getDataBUB().results_Shape_2nd == null) {
                System.out.println("Bubble Identification using given Mask and Points");
                //ImageInt grad = new ImageInt(readFirst.iaPixels.length, readFirst.iaPixels[0].length);
                grad.iaPixels = getThinEdge(readFirst.iaPixels, Boolean.FALSE, null, null, 1);
                ImageInt iMask1 = (ImageInt) controller.getCurrentMethod().getProtocol("mask").getResults()[1];
                String sName = sNames.get(0).toString().substring(0, sNames.get(0).toString().indexOf("."));
                controller.getDataBUB().results_Shape = new OpenTIV_Edges.ReturnContainer_Shape(getArbStrucs(iMask1, grad, sName, sPath, sFolder, controller), readFirst);
            } else {
                controller.getDataBUB().results_Shape = controller.getDataBUB().results_Shape_2nd;
            }

            if (bTracking) {
                ImageInt readSecond = new ImageInt(controller.getDataPIV().iaReadInSecond);
                ImageInt iMask1 = (ImageInt) controller.getCurrentMethod().getProtocol("mask").getResults()[2];
                //ImageInt grad = new ImageInt(readSecond.iaPixels.length, readSecond.iaPixels[0].length);
                grad.iaPixels = getThinEdge(readSecond.iaPixels, Boolean.FALSE, null, null, 1);
                String sName = sNames.get(1).toString().substring(0, sNames.get(1).toString().indexOf("."));
                controller.getDataBUB().results_Shape_2nd = new OpenTIV_Edges.ReturnContainer_Shape(getArbStrucs(iMask1, grad, sName, sPath, sFolder, controller), readSecond);

            }
        }
        if (controller.getDataBUB().results_Shape != null) {
            for (Shape o : controller.getDataBUB().results_Shape.loShapes) {
                readFirst.setPoints(o.getlmeList(), 255);
                if (!bTracking) {
                    controller.getDataBUB().results.put(o, new VelocityVec(0.0, 0.0, o.getSubPixelCenter()));
                }
            }

        }

        shapeFit = readFirst;
        buildLookUp();

    }

    public static List<Shape> getArbStrucs(ImageInt iMask1, ImageInt grad, String sName, String sPath, String sFolder, PIVBUBController controller) {
        List<SettingObject> lsc1 = controller.getCurrentMethod().getProtocol("preproc").getAllSettings();
        Settings oSet = new Settings() {
            @Override
            public String getType() {
                throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
            }

            @Override
            public void buildClusters() {
                throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
            }
        };
        oSet.loSettings.addAll(lsc1);
        int[] iCuts = getCut(oSet);
        List<Shape> lsArbstruc = new ArrayList<>();
        Gson gson = new Gson();
        try {
            JsonReader reader = new JsonReader(new FileReader(sPath + System.getProperty("file.separator") + sFolder + System.getProperty("file.separator") + sName + ".json"));
            List<BubbleJSON> in = gson.fromJson(reader, new TypeToken<List<BubbleJSON>>() {
            }.getType());
            reader.close();
            for (BubbleJSON bubbleJSON : in) {
                bubbleJSON.createLme(iCuts);
            }
            //Ascending bubbleJSON.ID order    
            for (int i = 1; i <= iMask1.getMax(); i++) {

                MatrixEntry meStart = iMask1.getSeed(i);

                if (meStart != null) {
                    int iValue = i;
                    ArbStructure2 Structure = new ArbStructure2((new Morphology()).markFillN8(iMask1, meStart.i, meStart.j, (pParameter) -> {
                        if (iMask1.iaPixels[pParameter.i][pParameter.j] == iValue) {
                            return true;
                        } else {
                            return false;
                        }
                    }));
                    
                    double dAvGrad = 0.0;
                    double dCounter = 0.0;
                    for (MatrixEntry me : Structure.loPoints) {
                        N8 oN8 = new N8(iMask1, me.i, me.j);
                        if (oN8.isBorder()) {
                            dAvGrad += getMaxDeriNeighbor(me, grad);
                            dCounter += 1.0;
                        }
                    }
                    dAvGrad = dAvGrad / dCounter;
                    if (in.size() > 0) {
                        if (in.get(0).ID == i) {
                            ImageInt iOneBubCorrected = new ImageInt(iMask1.iaPixels.length, iMask1.iaPixels[0].length);
                            BubbleJSON bub = in.get(0);
                            for (int j = 0; j < bub.lme.size() - 1; j++) {
                                Line oL = new Line(bub.lme.get(j), bub.lme.get(j + 1));
                                oL.setLine(iOneBubCorrected, 255);
                            }
                            Line oL = new Line(bub.lme.get(0), bub.lme.get(bub.lme.size() - 1));
                            oL.setLine(iOneBubCorrected, 255);
                            MatrixEntry meCenter = MatrixEntry.getMeanEntry(bub.lme);
                            iOneBubCorrected.setPoints(new Morphology().markFillN4(iOneBubCorrected, meCenter.i, meCenter.j), 255);
                            iOneBubCorrected.setPoints(Structure.loPoints, 255);
                            iOneBubCorrected.resetMarkers();
                            in.remove(0);
                            Structure = new ArbStructure2((new Morphology()).markFillN8(iOneBubCorrected, meCenter.i, meCenter.j, (pParameter) -> {
                                if (iOneBubCorrected.iaPixels[pParameter.i][pParameter.j] == 255) {
                                    return true;
                                } else {
                                    return false;
                                }
                            }));
                        }
                    }
                    ImageInt iOneBub = new ImageInt(iMask1.iaPixels.length, iMask1.iaPixels[0].length);
                    iOneBub.setPoints(Structure.loPoints, 255);
                    List<MatrixEntry> lmeBorder = Structure.getBorderPoints(iOneBub);
                    if (Structure.loPoints.size() < 2) {
                        continue;
                    }
                    if (lmeBorder.size() > 2) {
                        Structure.loPoints = lmeBorder;
                        Structure.getMajorMinor(lmeBorder);
                    } else {
                        Structure.getMajorMinor(Structure.loPoints);
                    }
                    Structure.dAvergeGreyDerivative = dAvGrad;
                    lsArbstruc.add(Structure);
                }
            }

        } catch (FileNotFoundException ex) {
            Logger.getLogger(Prot_tivPIVBUBBubbleFinder.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(Prot_tivPIVBUBBubbleFinder.class.getName()).log(Level.SEVERE, null, ex);
        }
        return lsArbstruc;
    }

    public static List<Shape> getBoundEllipses(ImageInt iMask, ImageInt iGrad) throws IOException {
        ImageInt Input = iMask.clone();
        ImageInt oOuterEdges = BasicIMGOper.threshold(iMask.clone(), 1);
        ImageInt oOuterEdges2 = BasicIMGOper.threshold(iMask.clone(), 254);
        Morphology.markEdgesBinarizeImage(oOuterEdges);
        Morphology.markEdgesBinarizeImage(oOuterEdges2);

        List<Shape> lCircles = new ArrayList<>();

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
                        Circle cc = null;
                        if (meBound.size() > 0) {
                            cc = EllipseDetection.EllipseFit(meBound);
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
                            cc = EllipseDetection.EllipseFit(meBound);

                        }
                        if (meBound.size() > 5 && cc != null) {
                            double dAvGrad = 0.0;
                            double dCounter = 0.0;
                            for (MatrixEntry me : meBound) {
                                dAvGrad += iGrad.iaPixels[me.i][me.j];
                                dCounter += 1.0;
                            }
                            cc.dAvergeGreyDerivative = dAvGrad / dCounter;
                            lCircles.add(cc);
                        }
                    }
                    Input.setPoints(Structure.loPoints, 0);

                }
            }
        }

        return lCircles;
    }
    
        public static double getMaxDeriNeighbor(MatrixEntry meRef, ImageInt iGrad) {
        List<MatrixEntry> loRef = iGrad.getNeighborsN8(meRef.i, meRef.j);
        loRef.add(meRef);
        double dMax = 0.0;
       // MatrixEntry meChosen = new MatrixEntry();
        for (MatrixEntry me : loRef) {
            if (me != null) {
                if (iGrad.iaPixels[me.i][me.j] > dMax) {
                    dMax = iGrad.iaPixels[me.i][me.j];
                    //meChosen=me;
                }
            }
        }
        //iGrad.setPoint(meChosen, 0);
        return dMax;
        }

    public static int[] getCut(Settings oSettings) {
        boolean bCutTop = ((boolean) oSettings.getSettingsValue("BcutyTop"));
        boolean bCutBottom = ((boolean) oSettings.getSettingsValue("BcutyBottom"));
        boolean bCutLeft = ((boolean) oSettings.getSettingsValue("BcutxLeft"));
        boolean bCutRight = ((boolean) oSettings.getSettingsValue("BcutxRight"));
        int[] iCuts = new int[]{0, 0, 0, 0};
        if (bCutTop) {
            iCuts[0] = (int) oSettings.getSettingsValue("cutyTop");
        }
        if (bCutBottom) {
            iCuts[1] = (int) oSettings.getSettingsValue("cutyBottom");

        }
        if (bCutLeft) {
            iCuts[2] = (int) oSettings.getSettingsValue("cutxLeft");
        }
        if (bCutRight) {
            iCuts[3] = (int) oSettings.getSettingsValue("cutxRight");

        }
        return iCuts;
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
        this.loSettings.add(new SettingObject("Execution Order", "ExecutionOrder", new ArrayList<>(), SettingObject.SettingsType.Object));

        //PreProc for Bubble Finder
        this.loSettings.add(new SettingObject("LinNormalization", "LinNormalization", false, SettingObject.SettingsType.Boolean));
        this.loSettings.add(new SettingObject("NonLinNormalization", "NonLinNormalization", false, SettingObject.SettingsType.Boolean));
        //Edge Detectors
        this.loSettings.add(new SettingObject("Edge Detector", "OuterEdges", true, SettingObject.SettingsType.Boolean));
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
        this.loSettings.add(new SettingObject("Method", "Reco", "Default(ReadMaskandPoints)", SettingObject.SettingsType.String));
        //this.loSettings.add(new SettingObject("Ellipse Fit", "EllipseFit_Ziegenhein2019", false, SettingObject.SettingsType.Boolean));
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
    }

    @Override
    public void buildClusters() {
        SettingsCluster bubbleIdent = new SettingsCluster("Bubble Identification",
                new String[]{"Reco"}, this);
        bubbleIdent.setDescription("Method to find Bubbles");
        lsClusters.add(bubbleIdent);

        SettingsCluster bubImgPreproc = new SettingsCluster("Bubble Preproc",
                new String[]{"LinNormalization", "NonLinNormalization"}, this);
        bubImgPreproc.setDescription("Bubble Image Preprocessing");
        lsClusters.add(bubImgPreproc);

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

        SettingsCluster ellipseFit = new SettingsCluster("Ellipse fit",
                new String[]{"EllipseFit_Ziegenhein2019_Distance", "EllipseFit_Ziegenhein2019_LeadingSize"}, this);
        ellipseFit.setDescription("Parameters for ellipse fitting");
        lsClusters.add(ellipseFit);

    }

    @Override
    public List<SettingObject> getHints() {
        List<SettingObject> ls = super.getHints();
        ls.add(new SettingObject("Method", "Reco", "ReadMaskandPoints", SettingObject.SettingsType.String));
        ls.add(new SettingObject("Method", "Reco", "Read_Mask_and_Ellipse_fit", SettingObject.SettingsType.String));
        ls.add(new SettingObject("Method", "Reco", "Edge_Detector_and_Ellipse_fit", SettingObject.SettingsType.String));

        return ls;
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
        EdgesShapeFit, ShapeFit
    }

}
