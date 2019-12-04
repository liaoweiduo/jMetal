package org.uma.jmetal.problem.multiobjective.FeatureSelection;

import be.abeel.util.Pair;
import net.sf.javaml.classification.KNearestNeighbors;
import net.sf.javaml.core.Dataset;
import net.sf.javaml.core.DefaultDataset;
import net.sf.javaml.core.DenseInstance;
import net.sf.javaml.core.Instance;
import net.sf.javaml.sampling.Sampling;
import net.sf.javaml.tools.data.FileHandler;

import java.io.*;

public class buildData {

    public static void main(String[] args) throws IOException {
        String basePath = "jmetal-problem/src/main/resources/classificationData/";
        int featuresNumber = 617;
        int instanceNumber = 7797;
        String dataName = "Isolet";
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


        // calculate accuracy of each single feature
        double[] accuracyList = new double[featuresNumber];
        for (int featureIndex = 0; featureIndex < featuresNumber; featureIndex++){
            Dataset newDataTrain = new DefaultDataset();
            Dataset newDataTest = new DefaultDataset();
            for (Instance ins : dataTrain)
                newDataTrain.add(new DenseInstance(new double[]{ins.value(featureIndex)},ins.classValue()));
            for (Instance ins : dataTest)
                newDataTest.add(new DenseInstance(new double[]{ins.value(featureIndex)},ins.classValue()));
            KNearestNeighbors knn1 = new KNearestNeighbors(5);
            knn1.buildClassifier(newDataTrain);
            int correct = 0;
            /* Classify all instances and check with the correct class values */
            for (Instance inst : newDataTest) {
                Object predictedClassValue = knn1.classify(inst);
                Object realClassValue = inst.classValue();
                if (predictedClassValue.equals(realClassValue))
                    correct++;
            }
            double accuracy = (double) correct / newDataTest.size();
            accuracyList[featureIndex] = accuracy;
            System.out.println("#"+featureIndex+" accuracy:" + accuracy);
        }
        BufferedWriter bw = new BufferedWriter(new FileWriter(new File(basePath + dataName + "/accuracy.dat"), true));
        for (double a : accuracyList)
            bw.write(Double.toString(a) + '\t');
        bw.write('\n');
        bw.flush();
        bw.close();
    }

}