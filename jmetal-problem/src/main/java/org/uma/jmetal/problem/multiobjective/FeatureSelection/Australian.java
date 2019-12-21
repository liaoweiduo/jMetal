package org.uma.jmetal.problem.multiobjective.FeatureSelection;

import net.sf.javaml.core.Dataset;
import net.sf.javaml.tools.data.FileHandler;
import org.uma.jmetal.util.JMetalException;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

public class Australian extends FeatureSelection {

    private int instanceNumber;
    private int classNumber;
    private int featureNumber;

    public Australian() {
        this.instanceNumber = 690;
        this.classNumber = 2;
        this.featureNumber = 14;
        String dataName = "Australian";

        setNumberOfVariables(featureNumber);
        setNumberOfObjectives(2);
        setName(dataName);

        try {
            String basePath = "jmetal-problem/src/main/resources/classificationData/";
            Dataset dataTrain = FileHandler.loadDataset(new File(basePath + dataName + "/train.dat"),0,"\t");
            Dataset dataTest = FileHandler.loadDataset(new File(basePath + dataName + "/test.dat"),0,"\t");
            double[] accuracyList = new double[featureNumber];
            BufferedReader br = new BufferedReader(new FileReader(basePath + dataName + "/accuracy.dat"));
            String record = br.readLine();
            String[] splitedRecord = record.split("\t");
            for (int index = 0;index < featureNumber;index++){
                accuracyList[index] = Double.parseDouble(splitedRecord[index]);
            }
            fullfillData(dataTrain, dataTest, accuracyList);
        }catch (Exception e){
            throw new JMetalException("Error reading data ", e) ;
        }
    }

}
