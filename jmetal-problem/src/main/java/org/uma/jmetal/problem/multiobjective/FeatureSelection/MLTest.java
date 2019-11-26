package org.uma.jmetal.problem.multiobjective.FeatureSelection;

import net.sf.javaml.core.Dataset;
import net.sf.javaml.core.DefaultDataset;
import net.sf.javaml.core.DenseInstance;
import net.sf.javaml.core.Instance;
import net.sf.javaml.tools.InstanceTools;

public class MLTest {
    public static void main(String[] args) {
        Dataset data = new DefaultDataset();
        for (int i =0;i<10;i++){
            Instance tmpInstance = InstanceTools.randomInstance(25);
            data.add(tmpInstance);
        }

    }
}
