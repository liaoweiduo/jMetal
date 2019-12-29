package org.uma.jmetal.problem.multiobjective.FeatureSelection;

import be.abeel.util.Pair;
import net.sf.javaml.classification.KNearestNeighbors;
import net.sf.javaml.classification.evaluation.EvaluateDataset;
import net.sf.javaml.classification.evaluation.PerformanceMeasure;
import net.sf.javaml.core.Dataset;
import net.sf.javaml.core.DefaultDataset;
import net.sf.javaml.core.DenseInstance;
import net.sf.javaml.core.Instance;
import net.sf.javaml.sampling.Sampling;
import net.sf.javaml.tools.data.FileHandler;

import java.io.*;
import java.util.Map;

public class buildData {
    public static void main(String[] args) throws IOException{
        buildFromRaw(args);
    }

    public static void buildFromRaw(String[] args) throws IOException {
        String basePath = "jmetal-problem/src/main/resources/classificationData/";
        int featuresNumber = 649;
        int instanceNumber = 2000;
        String dataName = "MultipleFeatures";
        Dataset data = FileHandler.loadDataset(new File(basePath + dataName + "/data.dat"),0,",");
//        data.addAll(FileHandler.loadDataset(new File(basePath + dataName + "/madelon_valid.data")));
//        data.addAll(FileHandler.loadDataset(new File(basePath + dataName + "/xac.dat"),18," "));
//        data.addAll(FileHandler.loadDataset(new File(basePath + dataName + "/xad.dat"),18," "));
//        data.addAll(FileHandler.loadDataset(new File(basePath + dataName + "/xae.dat"),18," "));
//        data.addAll(FileHandler.loadDataset(new File(basePath + dataName + "/xaf.dat"),18," "));
//        data.addAll(FileHandler.loadDataset(new File(basePath + dataName + "/xag.dat"),18," "));
//        data.addAll(FileHandler.loadDataset(new File(basePath + dataName + "/xah.dat"),18," "));
//        data.addAll(FileHandler.loadDataset(new File(basePath + dataName + "/xai.dat"),18," "));

        // pre process data
//        for (Instance ins : data){
//            ins.removeAttribute(0);
//            ins.removeAttribute(0);
//        }
        for (Instance ins : data) {
            for (int i = 0; i < featuresNumber; i++){
                if (ins.get(i).isNaN()){
                    ins.put(i, 0.0);
                }
            }
        }



        // 7 3 sampling
        Sampling s = Sampling.SubSampling;
        Pair<Dataset, Dataset> datas = s.sample(data, (int)(data.size()*0.7));
        Dataset dataTrain = datas.x();
        Dataset dataTest = datas.y();
        FileHandler.exportDataset(data,new File(basePath + dataName + "/" + dataName +".dat"));
        FileHandler.exportDataset(dataTrain,new File(basePath + dataName + "/train.dat"));
        FileHandler.exportDataset(dataTest,new File(basePath + dataName + "/test.dat"));

        // calculate the total error rate
        KNearestNeighbors knn = new KNearestNeighbors(5);
        knn.buildClassifier(dataTrain);
        int wrong = 0;
        /* Classify all instances and check with the correct class values */
        for (Instance inst : dataTest) {
            Object predictedClassValue = knn.classify(inst);
            Object realClassValue = inst.classValue();
            if (!predictedClassValue.equals(realClassValue))
                wrong++;
        }
        double errorRate = (double) wrong / dataTest.size();
        System.out.println("error rate:" + errorRate);

        System.out.println("balanced accuracy:");
        Map<Object, PerformanceMeasure> pm = EvaluateDataset.testDataset(knn, dataTest);
        double balancedAccuracy = 0;
        for(Object o:pm.keySet()) {
            System.out.println(o + ": " + pm.get(o).getAccuracy());
            balancedAccuracy += pm.get(o).getAccuracy();
        }
        balancedAccuracy /= pm.size();
        System.out.println("balanced accuracy:" + balancedAccuracy);


        // calculate accuracy of each single feature
        double[] accuracyList = new double[featuresNumber];
        for (int featureIndex = 0; featureIndex < featuresNumber; featureIndex++){
//            if (featureIndex == 19){
//                accuracyList[featureIndex] = 0;
//                System.out.println("#"+featureIndex+" accuracy:" + 0);
//                continue;
//            }
            Dataset newDataTrain = new DefaultDataset();
            Dataset newDataTest = new DefaultDataset();
            for (Instance ins : dataTrain)
                newDataTrain.add(new DenseInstance(new double[]{ins.value(featureIndex)},ins.classValue()));
            for (Instance ins : dataTest)
                newDataTest.add(new DenseInstance(new double[]{ins.value(featureIndex)},ins.classValue()));
            KNearestNeighbors knn1 = new KNearestNeighbors(5);
            knn1.buildClassifier(newDataTrain);
            double balancedAccuracy1 = 0;
            try {
                Map<Object, PerformanceMeasure> pm1 = EvaluateDataset.testDataset(knn1, newDataTest);

                for (Object o: pm1.keySet()){
                    balancedAccuracy1 += pm1.get(o).getAccuracy();
                }
                balancedAccuracy1 /= pm1.size();
            } catch (NullPointerException e){
                System.out.println("#"+featureIndex+" null pointer exception.");
            }
            accuracyList[featureIndex] = balancedAccuracy1;
            System.out.println("#"+featureIndex+" accuracy:" + balancedAccuracy1);
        }
        BufferedWriter bw = new BufferedWriter(new FileWriter(new File(basePath + dataName + "/accuracy.dat"), true));
        for (double a : accuracyList)
            bw.write(Double.toString(a) + '\t');
        bw.write('\n');
        bw.flush();
        bw.close();
    }

}