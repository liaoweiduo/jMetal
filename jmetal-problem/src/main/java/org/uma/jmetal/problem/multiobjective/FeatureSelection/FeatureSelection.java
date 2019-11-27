package org.uma.jmetal.problem.multiobjective.FeatureSelection;

import net.sf.javaml.classification.KNearestNeighbors;
import net.sf.javaml.core.Dataset;
import net.sf.javaml.core.DefaultDataset;
import net.sf.javaml.core.DenseInstance;
import net.sf.javaml.core.Instance;
import org.uma.jmetal.problem.impl.AbstractDoubleProblem;
import org.uma.jmetal.solution.DoubleSolution;
import org.uma.jmetal.util.JMetalException;

import java.util.ArrayList;
import java.util.List;

/**
 * Basic Feature Selection Problem
 */
@SuppressWarnings("serial")
public abstract class FeatureSelection extends AbstractDoubleProblem {

    private Dataset dataTrain;
    private Dataset dataTest;
    private double[] accuracyList;
    private double threshold;   // variable value > threshold: feature choosen

    /**
     * fill data and calculation of each single feature error rate
     *
     * @param dataTrain train data
     * @param dataTest test data
     * @param accuracyList accuracy of each feature
     */
    public void fullfillData(Dataset dataTrain, Dataset dataTest, double[] accuracyList) {
        this.dataTrain = dataTrain;
        this.dataTest = dataTest;
        this.accuracyList = accuracyList;
        this.threshold = 0.6;

        List<Double> lowerLimit = new ArrayList<>(getNumberOfVariables()) ;
        List<Double> upperLimit = new ArrayList<>(getNumberOfVariables()) ;

        for (int i = 0; i < getNumberOfVariables(); i++) {
            lowerLimit.add(0.0);
            upperLimit.add(1.0);
        }

        setLowerLimit(lowerLimit);
        setUpperLimit(upperLimit);

    }

    @Override
    public void evaluate(DoubleSolution solution) {
        int numberOfVariables = getNumberOfVariables();
        int numberOfObjectives = getNumberOfObjectives();

        double[] f = new double[numberOfObjectives];

        int numberOfSelectedFeatures = getSelectedFeatureNumber(solution);
        f[0] = numberOfSelectedFeatures / (double)numberOfVariables;

        double errorRate = 0;
        if (numberOfSelectedFeatures == 0)      // if no feature is selected, the error rate is 1
            errorRate = 1;
        else{
            Dataset newDataTrain = getSelectedFeatureData(solution, dataTrain);
            Dataset newDataTest = getSelectedFeatureData(solution, dataTest);

            KNearestNeighbors knn = new KNearestNeighbors(5);
            knn.buildClassifier(newDataTrain);

            int wrong = 0;
            /* Classify all instances and check with the correct class values */
            for (Instance inst : newDataTest) {
                Object predictedClassValue = knn.classify(inst);
                Object realClassValue = inst.classValue();
                if (!predictedClassValue.equals(realClassValue))
                    wrong++;
            }
            errorRate = (double) wrong / newDataTest.size();
//            System.out.println(errorRate);
        }
            f[1] = errorRate;

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
     * return the data with selected features
     *
     * @param solution the target solution
     * @param data the target data
     * @return the dataset with selected features
     */
    public Dataset getSelectedFeatureData(DoubleSolution solution, Dataset data){
        int numberOfVariables = getNumberOfVariables();
        int numberOfSelectedFeatures = getSelectedFeatureNumber(solution);
        Dataset newData = new DefaultDataset();
        if (numberOfSelectedFeatures == 0)
            throw (new JMetalException("number of selected features: 0"));

        for (Instance ins : data){
            double[] selectedFeatureData = new double[numberOfSelectedFeatures];
            int selectedFeatureIndex = 0;
            for (int index = 0; index < numberOfVariables; index++) {
                if (solution.getVariableValue(index) > threshold)
                    selectedFeatureData[selectedFeatureIndex++] = ins.get(index);
            }
            newData.add(new DenseInstance(selectedFeatureData, ins.classValue()));
        }

        return newData;
    }

}
