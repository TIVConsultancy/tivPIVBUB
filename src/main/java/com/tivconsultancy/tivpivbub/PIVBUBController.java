/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.tivconsultancy.tivpivbub;

import com.tivconsultancy.tivpiv.PIVController;
import com.tivconsultancy.tivpivbub.data.DataBUB;

/**
 *
 * @author TZ ThomasZiegenhein@TIVConsultancy.com +1 480 494 7254
 */
public class PIVBUBController extends PIVController {

    protected DataBUB databaseBUB1Step;
    public int[][] iaEdgesFirst;
    public int[][] iaEdgesSecond;

    public PIVBUBController() {
        super();
        initDatabase();
    }

    @Override
    protected void initSubControllers() {
        super.initSubControllers();
        subSQL = new tivPIVBUBSubControllerSQL();
    }

    public DataBUB getDataBUB() {
        return databaseBUB1Step;
    }

    private void initDatabase() {
        databaseBUB1Step = new DataBUB(getSelecedIndex());
    }

    @Override
    public void startNewIndexStep() {
        DataBUB databaseBUBPrevStep = new DataBUB(0);
        boolean bTrack = this.getCurrentMethod().getProtocol("bubtrack").getSettingsValue("Tracking") == "Disable Tracking" ? false : true;
        if (this.neglect_prevStep) {
            this.databaseBUB1Step.results_Shape_2nd = null;
            this.neglect_prevStep=false;
        }
        if (this.databaseBUB1Step.results_Shape_2nd != null && bTrack ) {
            databaseBUBPrevStep.results_Shape_2nd = this.databaseBUB1Step.results_Shape_2nd;
        }
        super.startNewIndexStep();
        databaseBUB1Step = new DataBUB(getSelecedIndex());
        if (databaseBUBPrevStep.results_Shape_2nd != null && bTrack) {
            databaseBUB1Step.results_Shape_2nd = databaseBUBPrevStep.results_Shape_2nd;
        }

    }

}
