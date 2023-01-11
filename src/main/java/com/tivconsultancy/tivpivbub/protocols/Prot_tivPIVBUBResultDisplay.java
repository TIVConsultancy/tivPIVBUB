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
import com.tivconsultancy.opentiv.helpfunctions.io.Writer;
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
        DataPIV data = ((PIVController) StaticReferences.controller).getDataPIV();
        ImageInt res = new ImageInt(data.iaReadInFirst);
        imgResult = res.getBuffImage();
        BufferedImage imgRes2= res.getBuffImage();
        imgRes2 = Writer.getType_Int_RGB(imgRes2);
        try {
            double maxVecLength = (Double) controller.getCurrentMethod().getProtocol("display").getSettingsValue("MaxDisp");

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
        Graphics2D g3Result = imgRes2.createGraphics();
        g3Result.drawImage(imgRes2, 0, 0, null);
        for (Shape loCircle : lsC.loShapes) {
            float a = (float) (Math.random() );
            float b = (float) (1.0);
            float c = (float) (1.0);
            g2Result.setColor(Color.getHSBColor(a, b, c));
            g3Result.setColor(Color.getHSBColor(a, b, c));
            for (MatrixEntry me : loCircle.getlmeList()) {
                if (res.isInside(me.i, me.j)) {
                    g2Result.drawLine(me.j, me.i, me.j, me.i);
                    g3Result.drawLine(me.j, me.i, me.j, me.i);
                }
            }
        }
        g2Result.dispose();
        g3Result.dispose();
        String sFileName = controller.getCurrentFileSelected().getName().substring(0, controller.getCurrentFileSelected().getName().indexOf("."));
        File oPath = new File(controller.getCurrentFileSelected().getParent() + System.getProperty("file.separator") + "ResultImages");
        if (!oPath.exists()) {
            oPath.mkdir();
        }
        try {
            ImageIO.write(imgResult, "png", new File(oPath.getPath() + System.getProperty("file.separator") + sFileName + "PIV_BT.jpg"));
            ImageIO.write(imgRes2, "png", new File(oPath.getPath() + System.getProperty("file.separator") + sFileName + "BT.jpg"));
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

    }


    private void initSettings() {

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
