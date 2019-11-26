package org.uma.jmetal.problem.multiobjective.FeatureSelection;

import org.uma.jmetal.problem.impl.AbstractDoubleProblem;
import org.uma.jmetal.solution.DoubleSolution;

/**
 * Basic Feature Selection Problem
 */
@SuppressWarnings("serial")
public abstract class FeatureSelection extends AbstractDoubleProblem {

    private double[][] dataTrain;
    private String[] labelTrain;
    private double[][] dataTest;
    private String[] labelTest;
    private double[] accuracy;
    private double threshold;   // variable value > threshold: feature choosen

    /**
     * fill data and calculation of each single feature error rate
     *
     * @param dataTrain train data
     * @param labelTrain train data label
     * @param dataTest test data
     * @param labelTest test data label
     */
    public void fullfillData(double[][] dataTrain, String[] labelTrain, double[][] dataTest, String[] labelTest) {
        this.dataTrain = dataTrain;
        this.labelTrain = labelTrain;
        this.dataTest = dataTest;
        this.labelTest = labelTest;
        this.threshold = 0.6;
        // calculation of accuracy with only use the single feature.
        this.accuracy = new double[dataTrain[0].length];
        for (int featureIndex = 0; featureIndex < dataTrain[0].length; featureIndex++){
            double[][] newDataTrain = new double[dataTrain.length][1];
            double[][] newDataTest = new double[dataTest.length][1];
            for (int i = 0; i < dataTrain.length; i++)
                newDataTrain[i][1] = dataTrain[i][featureIndex];
            for (int i = 0; i < dataTest.length; i++)
                newDataTest[i][1] = dataTest[i][featureIndex];
            this.accuracy[featureIndex] = new KNN(newDataTrain,labelTrain,newDataTest,labelTest).getErrorRate();
        }
    }

    @Override
    public void evaluate(DoubleSolution solution) {
        int numberOfVariables = getNumberOfVariables();
        int numberOfObjectives = getNumberOfObjectives();

        double[] f = new double[numberOfObjectives];

        double[][] newDataTrain = getSelectedFeatureData(solution, dataTrain);
        double[][] newDataTest = getSelectedFeatureData(solution, dataTest);

        int numberOfSelectedFeatures = getSelectedFeatureNumber(solution);
        f[0] = numberOfSelectedFeatures / (double)numberOfVariables;

        if (numberOfSelectedFeatures == 0)
            f[1] = 1;
        else
            f[1] = new KNN(newDataTrain,labelTrain,newDataTest,labelTest).getErrorRate();

        for (int i = 0; i < numberOfObjectives; i++) {
            solution.setObjective(i, f[i]);
        }
    }

    /**
     * get the number of selected features
     *
     * @param solution the target solution
     * @return number of selected features
     */
    public int getSelectedFeatureNumber(DoubleSolution solution){
        int numberOfSelectedFeature = 0;
        int numberOfVariables = getNumberOfVariables();
        for (int index = 0; index < numberOfVariables; index++){
            double variableValue = solution.getVariableValue(index);
            if (variableValue > threshold){
                numberOfSelectedFeature++;
            }
        }
        return numberOfSelectedFeature;
    }


    /**
     * return the selected features index
     *
     * @param solution the target solution
     * @param data the target data
     * @return the bool list with the same length with solution variable 0 is not-selected; 1 is selected.
     */
    public double[][] getSelectedFeatureData(DoubleSolution solution, double[][] data){
        int numberOfVariables = getNumberOfVariables();
        int numberOfSelectedFeatures = getSelectedFeatureNumber(solution);
        double[][] newData = new double[data.length][numberOfSelectedFeatures];
        for (int line = 0; line < data.length; line++) {
            int selectedFeatureIndex = 0;
            for (int index = 0; index < numberOfVariables; index++) {
                double variableValue = solution.getVariableValue(index);
                if (variableValue > threshold)
                    newData[line][selectedFeatureIndex++] = data[line][index];
            }
        }
        return newData;
    }

    public double[][] getDataTrain() {
        return dataTrain;
    }

    public void setDataTrain(double[][] dataTrain) {
        this.dataTrain = dataTrain;
    }

    public String[] getLabelTrain() {
        return labelTrain;
    }

    public void setLabelTrain(String[] labelTrain) {
        this.labelTrain = labelTrain;
    }

    public double[][] getDataTest() {
        return dataTest;
    }

    public void setDataTest(double[][] dataTest) {
        this.dataTest = dataTest;
    }

    public String[] getLabelTest() {
        return labelTest;
    }

    public void setLabelTest(String[] labelTest) {
        this.labelTest = labelTest;
    }

    public double[] getAccuracy() {
        return accuracy;
    }

    public void setAccuracy(double[] accuracy) {
        this.accuracy = accuracy;
    }
}
