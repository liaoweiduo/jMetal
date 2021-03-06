package org.uma.jmetal.algorithm.multiobjective.moead.util;

import org.uma.jmetal.algorithm.multiobjective.moead.MOEADBuilder;
import org.uma.jmetal.algorithm.multiobjective.moead.oipMOEADFS;
import org.uma.jmetal.operator.MutationOperator;
import org.uma.jmetal.operator.impl.crossover.DifferentialEvolutionCrossover;
import org.uma.jmetal.operator.impl.mutation.PolynomialMutation;
import org.uma.jmetal.problem.DoubleProblem;
import org.uma.jmetal.solution.DoubleSolution;
import org.uma.jmetal.solution.Solution;
import org.uma.jmetal.solution.impl.DefaultDoubleSolution;
import org.uma.jmetal.util.*;
import org.uma.jmetal.util.fileoutput.impl.DefaultFileOutputContext;
import org.uma.jmetal.util.solutionattribute.impl.SolutionTextRepresentation;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ParallelismTest {

    public static void main(String[] args) throws Exception {
//        parallelismTest();
        ClassificationMethodTimeCostTestForDifferentFeatureNumber(args);
//        evaluateOne(args);
    }
    public static void evaluateOne (String[] args) throws Exception{
        DoubleProblem problem;
        oipMOEADFS algorithm;
        MutationOperator<DoubleSolution> mutation;
        DifferentialEvolutionCrossover crossover;
        String problemName = "org.uma.jmetal.problem.multiobjective.FeatureSelection.";
        String referenceParetoFront = "jmetal-core/src/main/resources/pareto_fronts/";
        String timeDataOutputFileName = "Data/evaluationTimeTest/";
        if (args.length == 1) {
            problemName += args[0];
            referenceParetoFront += args[0] + ".pf";
            timeDataOutputFileName += args[0] + ".dat";
        } else {
            problemName += "Vehicle";
            referenceParetoFront += "Vehicle.pf";
            timeDataOutputFileName += "Vehicle.dat";
        }
        problem = (DoubleProblem) ProblemUtils.<DoubleSolution> loadProblem(problemName);
        DoubleSolution solution = new DefaultDoubleSolution(problem);
        for (int variableIndex = 0; variableIndex < problem.getNumberOfVariables(); variableIndex++)
            solution.setVariableValue(variableIndex, 1.0);
        long computationTime = System.currentTimeMillis();
        problem.evaluate(solution);
        computationTime = System.currentTimeMillis() - computationTime;
        JMetalLogger.logger.info(problemName + " all feature computation time:" +
                computationTime);
    }

    public static void ClassificationMethodTimeCostTestForDifferentFeatureNumber (String[] args) throws Exception {
        DoubleProblem problem;
        oipMOEADFS algorithm;
        MutationOperator<DoubleSolution> mutation;
        DifferentialEvolutionCrossover crossover;
        String problemName = "org.uma.jmetal.problem.multiobjective.FeatureSelection.";
        String referenceParetoFront = "jmetal-core/src/main/resources/pareto_fronts/";
        String timeDataOutputFileName = "Data/evaluationTimeTest/";
        if (args.length == 1) {
            problemName += args[0];
            referenceParetoFront += args[0] + ".pf";
            timeDataOutputFileName += args[0] + ".dat";
        } else {
            problemName += "Vehicle";
            referenceParetoFront += "Vehicle.pf";
            timeDataOutputFileName += "Vehicle.dat";
        }
        problem = (DoubleProblem) ProblemUtils.<DoubleSolution> loadProblem(problemName);

        // generate different solutions
//        long[][] computationTimeList = new long[problem.getNumberOfVariables()][10];
//        for (int featureNum = 1; featureNum <= problem.getNumberOfVariables(); featureNum++){
//            JMetalLogger.logger.info(problemName + " start calculation featureNum:" + featureNum);
//            List<DoubleSolution> solutionList = getSolutionList(problem, featureNum,10);
//            int solutionIndex = 0;
//            for (DoubleSolution solution : solutionList){
//                long computationTime = System.currentTimeMillis();
//                problem.evaluate(solution);
//                computationTime = System.currentTimeMillis() - computationTime;
//                computationTimeList[featureNum-1][solutionIndex++] = computationTime;
//            }
//        }
        long[][] computationTimeList = new long[problem.getNumberOfVariables() / 10 + 1][3];
        for (int featureNum = 1; featureNum <= problem.getNumberOfVariables(); featureNum+=10){
            int fn = featureNum / 10;
            JMetalLogger.logger.info(problemName + " start calculation featureNum:" + featureNum);
            List<DoubleSolution> solutionList = getSolutionList(problem, featureNum,3);
            int solutionIndex = 0;
            for (DoubleSolution solution : solutionList){
                long computationTime = System.currentTimeMillis();
                problem.evaluate(solution);
                computationTime = System.currentTimeMillis() - computationTime;
                computationTimeList[fn][solutionIndex++] = computationTime;
            }
        }

        String outputStr = "";
        for (int featureNum = 0; featureNum < computationTimeList.length; featureNum++){
            long[] computationTimes = computationTimeList[featureNum];
            long totalTime = 0;
            double averageTime = 0;
            long maxTime = Long.MIN_VALUE;
            long minTime = Long.MAX_VALUE;
            for (long computationTime : computationTimes){
                totalTime += computationTime;
                maxTime = (maxTime < computationTime)?computationTime:maxTime;
                minTime = (minTime > computationTime)?computationTime:minTime;
            }
            averageTime = totalTime / 3.0;
            outputStr += "\n" + averageTime + ", " + maxTime + ", " + minTime +";";
        }
        JMetalLogger.logger.info(problemName + " ClassificationMethodTimeCostTestForDifferentFeatureNumber:" +
                outputStr);
//        DefaultFileOutputContext context = new DefaultFileOutputContext(timeDataOutputFileName);
//        BufferedWriter bufferedWriter = context.getFileWriter();
//        for ()
//        bufferedWriter.write(computationTimeList);
//        bufferedWriter.close();
    }

    public static void parallelismTest() {
        Process pt = new Process();
        pt.run();
    }

    public static List<DoubleSolution> getSolutionList (DoubleProblem problem, int featureNumber, int numberOfGeneratedSolutions){
        List<DoubleSolution> solutionList = new ArrayList<>();

        int numberOfVariables = problem.getNumberOfVariables();

        for (int i = 0; i < numberOfGeneratedSolutions; i++) {
            int[] permutation = new int[numberOfVariables];
            MOEADUtils.randomPermutation(permutation, numberOfVariables);

            DoubleSolution solution = new DefaultDoubleSolution(problem);
            for (int variableIndex = 0; variableIndex < problem.getNumberOfVariables(); variableIndex++)
                solution.setVariableValue(variableIndex, 0.0);
            for (int permutationIndex = 0; permutationIndex < featureNumber; permutationIndex++){
                solution.setVariableValue(permutation[permutationIndex], 1.0);
            }
            solutionList.add(solution);
        }
        return solutionList;
    }
}

class Process implements Runnable{
    protected ExecutorService executorService;
    protected volatile Messages message = new Messages();
    protected int it = 10;

    @Override
    public void run() {
        executorService = Executors.newFixedThreadPool(2);
        List<SubProcess> subProcessList = new ArrayList<>();
        message.subPopulation= new ArrayList[2];
        message.subPopulation[0] = new ArrayList<>();
        message.subPopulation[1] = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            SubProcess subProcess = new SubProcess(this, i);
            subProcessList.add(subProcess);
            executorService.submit(subProcess);
        }
        executorService.shutdown();
        while(!executorService.isShutdown());
        System.out.println("is shut down");
        while(!executorService.isTerminated());
        System.out.println("is terminated");
        System.out.println("i test:" + subProcessList.get(1).i);
        System.out.println("message test:" + message.subPopulation[0]);
    }


    // for parallelism
    protected class Messages {
        List<Double>[] subPopulation;
    }

    class SubProcess implements Runnable{

        private int i ;

        SubProcess(Process p,int i) {
            this.i = i;
            message.subPopulation[0].add(i / 1.0);
            System.out.println("it:" + p.it);
        }

        @Override
        public void run() {
            System.out.println("sub process:" + i);
            try {
                Thread.sleep((i==0)?1000:100);
                this.i = i+10;
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
