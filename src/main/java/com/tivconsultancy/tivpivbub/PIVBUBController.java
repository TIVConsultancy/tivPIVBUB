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
    
    public PIVBUBController(){
        super();
        initDatabase();        
    }
    
    @Override
    protected void initSubControllers() {
        super.initSubControllers();
        subSQL = new tivPIVBUBSubControllerSQL();        
    }   
    
    public DataBUB getDataBUB(){
        return databaseBUB1Step;
    }

    private void initDatabase() {
        databaseBUB1Step = new DataBUB(getSelecedIndex());
    }
    
    @Override
    public void startNewIndexStep() {
//        DataBUB databaseBUBPrevStep = new DataBUB(0);
//        if (this.databaseBUB1Step.results_BT!=null){
//            databaseBUBPrevStep.results_BT=this.databaseBUB1Step.results_BT;
//            System.out.println("Yeah!");
//        } else {
//            System.out.println("NOOO");
//        }
        super.startNewIndexStep();
        databaseBUB1Step = new DataBUB(getSelecedIndex());
//        databaseBUB1Step.results_EFit2=databaseBUBPrevStep.results_EFit2;
    }
    
}
