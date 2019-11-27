package org.uma.jmetal.problem.multiobjective.FeatureSelection;

import be.abeel.util.Pair;
import net.sf.javaml.classification.KNearestNeighbors;
import net.sf.javaml.classification.evaluation.CrossValidation;
import net.sf.javaml.classification.evaluation.EvaluateDataset;
import net.sf.javaml.classification.evaluation.PerformanceMeasure;
import net.sf.javaml.core.Dataset;
import net.sf.javaml.core.DefaultDataset;
import net.sf.javaml.core.DenseInstance;
import net.sf.javaml.core.Instance;
import net.sf.javaml.featureselection.ranking.RecursiveFeatureEliminationSVM;
import net.sf.javaml.featureselection.scoring.GainRatio;
import net.sf.javaml.sampling.Sampling;
import net.sf.javaml.tools.InstanceTools;
import net.sf.javaml.tools.data.FileHandler;

import java.io.File;
import java.util.Map;
import java.util.Random;

public class MLTest {

    public static void main(String[] args) throws Exception{
        String dataName = "Vehicle";
        Dataset data = FileHandler.loadDataset(new File("jmetal-problem/src/main/resources/classificationData/"+ dataName + "/xaa.dat"),18," ");
        data.addAll(FileHandler.loadDataset(new File("jmetal-problem/src/main/resources/classificationData/"+ dataName + "/xab.dat"),18," "));
        data.addAll(FileHandler.loadDataset(new File("jmetal-problem/src/main/resources/classificationData/"+ dataName + "/xac.dat"),18," "));
        data.addAll(FileHandler.loadDataset(new File("jmetal-problem/src/main/resources/classificationData/"+ dataName + "/xad.dat"),18," "));
        data.addAll(FileHandler.loadDataset(new File("jmetal-problem/src/main/resources/classificationData/"+ dataName + "/xae.dat"),18," "));
        data.addAll(FileHandler.loadDataset(new File("jmetal-problem/src/main/resources/classificationData/"+ dataName + "/xaf.dat"),18," "));
        data.addAll(FileHandler.loadDataset(new File("jmetal-problem/src/main/resources/classificationData/"+ dataName + "/xag.dat"),18," "));
        data.addAll(FileHandler.loadDataset(new File("jmetal-problem/src/main/resources/classificationData/"+ dataName + "/xah.dat"),18," "));
        data.addAll(FileHandler.loadDataset(new File("jmetal-problem/src/main/resources/classificationData/"+ dataName + "/xai.dat"),18," "));

        // 7 3 sampling
        Sampling s = Sampling.SubSampling;
        Pair<Dataset, Dataset> datas = s.sample(data, (int)(data.size()*0.7));
        Dataset dataTrain = datas.x();
        Dataset dataTest = datas.y();

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
        System.out.println(errorRate);


        // cross validation
        CrossValidation cv = new CrossValidation(knn);
        Map<Object, PerformanceMeasure> p = cv.crossValidation(data, 10);

        for(Object o:p.keySet())
            System.out.println(o+": "+p.get(o).getErrorRate());

        // feature scoring
        GainRatio ga = new GainRatio();
        /* Apply the algorithm to the data set */
        ga.build(data);
        /* Print out the score of each attribute */
        System.out.println("feature scoring:");
        for (int i = 0; i < ga.noAttributes(); i++)
            System.out.println(ga.score(i));

        // feature ranking
        /* Create a feature ranking algorithm */
        RecursiveFeatureEliminationSVM svmrfe = new RecursiveFeatureEliminationSVM(0.2);
        /* Apply the algorithm to the data set */
        svmrfe.build(data);
        /* Print out the rank of each attribute */
        System.out.println("feature ranking:");
        for (int i = 0; i < svmrfe.noAttributes(); i++)
            System.out.println(svmrfe.rank(i));
    }
}
