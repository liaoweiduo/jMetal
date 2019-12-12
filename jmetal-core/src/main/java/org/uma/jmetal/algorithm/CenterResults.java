package org.uma.jmetal.algorithm;

import java.util.ArrayList;
import java.util.List;

public interface CenterResults<Result> {
    int stepIteration = 10;    // per stepIteration generations stores the solution
    List<Result> getRecordSolutions() ;
}
