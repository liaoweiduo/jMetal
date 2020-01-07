package org.uma.jmetal.problem.multiobjective.FeatureSelection;

import libsvm.SelfOptimizingLinearLibSVM;
import net.sf.javaml.classification.Classifier;
import net.sf.javaml.classification.KNearestNeighbors;
import net.sf.javaml.classification.evaluation.EvaluateDataset;
import net.sf.javaml.classification.evaluation.PerformanceMeasure;
import net.sf.javaml.classification.tree.RandomForest;
import net.sf.javaml.core.Dataset;
import net.sf.javaml.core.DefaultDataset;
import net.sf.javaml.core.DenseInstance;
import net.sf.javaml.core.Instance;
import org.uma.jmetal.problem.impl.AbstractDoubleProblem;
import org.uma.jmetal.solution.DoubleSolution;
import org.uma.jmetal.util.JMetalException;

import java.util.*;

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
    void fullfillData(Dataset dataTrain, Dataset dataTest, double[] accuracyList) {
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

        double errorRate;
        if (numberOfSelectedFeatures == 0)      // if no feature is selected, the error rate is 1
            errorRate = 1;
        else {
            Dataset newDataTrain = getSelectedFeatureData(solution, dataTrain);
            Dataset newDataTest = getSelectedFeatureData(solution, dataTest);

//            RandomForest rf = new RandomForest(5);
//            rf.buildClassifier(dataTrain);

            KNearestNeighbors knn = new KNearestNeighbors(5);
            knn.buildClassifier(newDataTrain);

            Map<Object, PerformanceMeasure> pm = EvaluateDataset.testDataset(knn, newDataTest);
            double balancedAccuracy = 0;
            for (Object o : pm.keySet())
                balancedAccuracy += pm.get(o).getAccuracy();
            errorRate = 1 - balancedAccuracy / pm.size();
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
    private Dataset getSelectedFeatureData(DoubleSolution solution, Dataset data){
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

    /**
     * reduce the selected features size based on the accuracy of each feature
     * first remove the feature with less accuracy
     * until the number of selected features match refRate * D (variable number)
     *
     * @param solution the target solution
     * @param refRate the reference point of this subproblem
     */
    public void reduceSize (DoubleSolution solution, double refRate){
        int numberOfVariables = getNumberOfVariables();
        int numberOfSelectedFeatures = getSelectedFeatureNumber(solution);
        int nref = (int) (refRate * numberOfVariables);

        if (numberOfSelectedFeatures > nref) {
            ArrayList<Double> selectedFeaturesAccuracy = new ArrayList<Double>();

            for (int index = 0; index < numberOfVariables; index++) {
                if (solution.getVariableValue(index) > threshold) {
                    selectedFeaturesAccuracy.add(accuracyList[index]);
                }
            }
            selectedFeaturesAccuracy.sort(new Comparator<Double>() {
                @Override
                public int compare(Double o1, Double o2) {
                    if (o1 > o2)
                        return 1;
                    else if (o1 < o2)
                        return -1;
                    else
                        return 0;
//                    return (o1 > o2)?1:-1;
                }
            });

            for (int index = 0; index < numberOfSelectedFeatures - nref; index++) {
                double selectedAccuracy = selectedFeaturesAccuracy.get(index);
                int featurePosition = -1;
                for (int i = 0; i < numberOfVariables ;i++){
                    if (Math.abs(accuracyList[i] - selectedAccuracy) < 0.0001){
                        featurePosition = i;
                        break;
                    }
                }
                if (featurePosition == -1)
                    throw new JMetalException("reduceSize: do not find selected accuracy in accuracy list.");
                solution.setVariableValue(featurePosition, Math.random() * threshold);  // the reduced variable value is set to a random number between 0 to threshold(0.6)
            }
        }
    }

    /**
     * add the unselected features randomly
     * first remove the feature with less accuracy
     * until the number of selected features match refRate * D (variable number)
     *
     * @param solution the target solution
     * @param refRate the reference point of this subproblem
     */
    public void increaseSize (DoubleSolution solution, double refRate){
        int numberOfVariables = getNumberOfVariables();
        int numberOfSelectedFeatures = getSelectedFeatureNumber(solution);
        int numberOfUnSelectedFeatures = numberOfVariables - numberOfSelectedFeatures;
        int nref = (int) (refRate * numberOfVariables);

        ArrayList<Integer> unselectedFeaturesIndexList = new ArrayList<Integer>();
        for (int index = 0; index < numberOfVariables; index++) {
            if (solution.getVariableValue(index) < threshold) {
                unselectedFeaturesIndexList.add(index);
            }
        }

        Collections.shuffle(unselectedFeaturesIndexList);       //乱序排列index

        for (int index = 0; index < nref - numberOfSelectedFeatures; index++) {
            solution.setVariableValue(unselectedFeaturesIndexList.get(index), Math.random() * (1 - threshold) + threshold); // the added variable value is set to a random number between threshold(0.6) to 1
        }

    }
}
