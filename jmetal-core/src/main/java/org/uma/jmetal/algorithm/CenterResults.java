package org.uma.jmetal.algorithm;

import java.util.ArrayList;
import java.util.List;

public interface CenterResults {
    int stepIteration = 10;    // per stepIteration generations stores the solution
    List<List> recordSolutions = new ArrayList<>();
    List<List> getRecordSolutions() ;
}
