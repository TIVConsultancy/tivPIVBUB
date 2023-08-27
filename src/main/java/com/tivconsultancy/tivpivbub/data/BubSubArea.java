/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.tivconsultancy.tivpivbub.data;

import com.tivconsultancy.opentiv.helpfunctions.matrix.MatrixEntry;

/**
 *
 * @author Nutzer
 */
public class BubSubArea {

    public MatrixEntry meTL;
    public MatrixEntry meBR;
    public int[][] iaContent;

    public BubSubArea(MatrixEntry meTL, MatrixEntry meBR, int[][] iaContent) {
        this.meTL = meTL;
        this.meBR = meBR;
        this.iaContent = iaContent;
    }
}
