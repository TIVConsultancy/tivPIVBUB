/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.tivconsultancy.tivpivbub.data;

import com.tivconsultancy.opentiv.datamodels.Result1D;
import com.tivconsultancy.opentiv.datamodels.ResultsImageShowAble;
import com.tivconsultancy.opentiv.datamodels.overtime.DataBaseEntry;
import com.tivconsultancy.opentiv.datamodels.overtime.DatabaseRAM;
import com.tivconsultancy.opentiv.edgedetector.OpenTIV_Edges.ReturnContainer_Shape;
import com.tivconsultancy.opentiv.helpfunctions.matrix.MatrixEntry;
import com.tivconsultancy.opentiv.imageproc.shapes.Shape;
import com.tivconsultancy.opentiv.math.specials.LookUp;
import com.tivconsultancy.opentiv.math.specials.NameObject;
import com.tivconsultancy.opentiv.physics.vectors.VelocityVec;
import com.tivconsultancy.opentiv.velocimetry.boundarytracking.ReturnContainerBoundaryTracking;
import com.tivconsultancy.tivGUI.StaticReferences;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import javax.imageio.ImageIO;

/**
 *
 * @author TZ ThomasZiegenhein@TIVConsultancy.com +1 480 494 7254
 */
public class DataBUB implements DataBaseEntry, Serializable, ResultsImageShowAble {

    // Output Images
    protected LookUp<Object> outPutImages;

    // 1D Data
    public Result1D results1D;

    //Data
    public int[][] iaEdgesFirst;
    public int[][] iaEdgesSecond;

    //Data
    public int[][] iaEdgesRAWFirst;
    public int[][] iaEdgesRAWSecond;

    public ReturnContainer_Shape results_Shape = null;
    public ReturnContainer_Shape results_Shape_2nd = null;
    public ReturnContainerBoundaryTracking results_BT = null;
    public Map<Shape, VelocityVec> results;

    public DataBUB(int index) {
        results1D = new Result1D(index);
        outPutImages = new LookUp<>();
        results = new HashMap<>();
    }

    @Override
    public BufferedImage getImage(String sIdent) {
        if (StaticReferences.controller.getDataBase() instanceof DatabaseRAM) {
            return (BufferedImage) outPutImages.get(sIdent);
        } else {
            byte[] imgIByte = (byte[]) outPutImages.get(sIdent);
            if (imgIByte == null) {
                return null;
            }

            try {
                return ImageIO.read(new ByteArrayInputStream((byte[]) outPutImages.get(sIdent)));
            } catch (IOException ex) {
                StaticReferences.getlog().log(Level.WARNING, "Cannot get image from storeage: " + sIdent, ex);
                return null;
            }
        }
    }

    @Override
    public void setImage(String sIdent, BufferedImage img) {
        if (StaticReferences.controller.getDataBase() instanceof DatabaseRAM) {
            if (!outPutImages.set(sIdent, img)) {
                outPutImages.add(new NameObject<>(sIdent, img));
            }
        } else {

            try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                ImageIO.write(img, "png", baos);
                baos.flush();
                byte[] imageInByte = baos.toByteArray();

                if (!outPutImages.set(sIdent, imageInByte)) {
                    outPutImages.add(new NameObject<>(sIdent, imageInByte));
                }
            } catch (IOException ex) {
                StaticReferences.getlog().log(Level.WARNING, "Cannot store image: " + sIdent, ex);
            }
        }
    }

    public static class BubbleJSON {

        public int ID;
        public int[] YPoints;
        public int[] XPoints;
        public List<MatrixEntry> lme;

        public BubbleJSON() {

        }

        public BubbleJSON(int ID, int[] YPoints, int[] XPoints) {
            this.ID = ID;
            this.YPoints = YPoints;
            this.XPoints = XPoints;
        }

        public void createLme(int[] iCuts) {
            this.lme = new ArrayList<>();
            for (int i = 0; i < YPoints.length; i++) {
                lme.add(new MatrixEntry(YPoints[i]-iCuts[0], XPoints[i]-iCuts[2]));
            }
        }
    }

}
