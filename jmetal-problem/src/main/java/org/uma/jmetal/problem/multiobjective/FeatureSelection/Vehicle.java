package org.uma.jmetal.problem.multiobjective.FeatureSelection;

import org.uma.jmetal.problem.multiobjective.FeatureSelection.FeatureSelection;
import org.uma.jmetal.util.JMetalException;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;

public class Vehicle extends FeatureSelection {

    private int instanceNumber;
    private int classNumber;
    private int featureNumber;

    public Vehicle() {
        this.instanceNumber = 846;
        this.classNumber = 4;
        this.featureNumber = 18;
        double[][] data = new double[instanceNumber][featureNumber];
        String[] label = new String[instanceNumber];
        int instanceIndex = 0;
        try {
            BufferedReader br = new BufferedReader(new FileReader("jmetal-problem/src/main/resources/classificationData/Vehicle/Vehicle.dat"));
            String record;
            while((record = br.readLine()) != null) {
                String[] splitedRecord = record.split("\t");
                for (int featureIndex = 0;featureIndex < featureNumber;featureIndex++){
                    data[instanceIndex][featureIndex] = Double.parseDouble(splitedRecord[featureIndex]);
                }
                label[instanceIndex++] = splitedRecord[featureNumber];
            }
        }catch (Exception e){
            throw new JMetalException("Error reading data ", e) ;
        }

        // randomly divide data into 7:3 train:test

    }

}
