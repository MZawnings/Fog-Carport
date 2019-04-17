/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package data.models;

import java.util.ArrayList;

/**
 *
 * @author Camilla
 */
public class PartslistModel {
    ArrayList<MaterialModel> billOfMaterials;
    private int totalprice;

    public PartslistModel() {
        billOfMaterials = new ArrayList();
    }

    public ArrayList<MaterialModel> getBillOfMaterials() {
        return billOfMaterials;
    }
    
    public void addMaterial(MaterialModel material){
        this.billOfMaterials.add(material);
        this.totalprice += material.getPrice();
    }

    public int getTotalprice()
    {
        return totalprice;
    }

    public void setTotalprice(int totalprice)
    {
        this.totalprice = totalprice;
    }
    
    
    
}
