package org.uma.jmetal.runner.multiobjective;

import org.uma.jmetal.algorithm.multiobjective.moead.MOEADBuilder;
import org.uma.jmetal.algorithm.multiobjective.moead.oipMOEADFS;
import org.uma.jmetal.algorithm.multiobjective.moead.rdpMOEADFS;
import org.uma.jmetal.operator.MutationOperator;
import org.uma.jmetal.operator.impl.crossover.DifferentialEvolutionCrossover;
import org.uma.jmetal.operator.impl.mutation.PolynomialMutation;
import org.uma.jmetal.problem.DoubleProblem;
import org.uma.jmetal.solution.DoubleSolution;
import org.uma.jmetal.util.AbstractAlgorithmRunner;
import org.uma.jmetal.util.AlgorithmRunner;
import org.uma.jmetal.util.JMetalLogger;
import org.uma.jmetal.util.ProblemUtils;

import java.io.FileNotFoundException;
import java.util.List;

/**
 * Class for configuring and running the MOEA/D algorithm
 *
 * @author Antonio J. Nebro <antonio@lcc.uma.es>
 */
public class rdpMOEADFSRunner extends AbstractAlgorithmRunner {
    /**
     * @param args Command line arguments.
     * @throws SecurityException
     * Invoking command:
    java org.uma.jmetal.runner.multiobjective.MOEADRunner problemName [referenceFront]
     */
    public static void main(String[] args) throws FileNotFoundException {
        int NUM_OF_CORE;
        DoubleProblem problem;
        rdpMOEADFS algorithm;
        MutationOperator<DoubleSolution> mutation;
        DifferentialEvolutionCrossover crossover;

        String problemName = "org.uma.jmetal.problem.multiobjective.FeatureSelection.";
        String referenceParetoFront = "jmetal-core/src/main/resources/pareto_fronts/";
        if (args.length == 1) {
            problemName += args[0] ;
            referenceParetoFront += args[0] + ".pf" ;
            NUM_OF_CORE = 2 ;
        } else if (args.length == 2) {
            problemName += args[0] ;
            referenceParetoFront += args[1] ;
            NUM_OF_CORE = 2 ;
        } else if (args.length == 3) {
            problemName += args[0] ;
            referenceParetoFront += args[1] ;
            NUM_OF_CORE = Integer.parseInt(args[2]) ;
        } else {
            problemName += "Vehicle" ;
            referenceParetoFront += "Vehicle.pf" ;
            NUM_OF_CORE = 2 ;
        }

        problem = (DoubleProblem) ProblemUtils.<DoubleSolution> loadProblem(problemName);

        double cr = 0.6 ;
        double f = 0.7 ;
        crossover = new DifferentialEvolutionCrossover(cr, f, "rand/1/bin");

        double mutationProbability = 1.0 / problem.getNumberOfVariables();
        double mutationDistributionIndex = 20.0;
        mutation = new PolynomialMutation(mutationProbability, mutationDistributionIndex);

        int populationSize = Math.min(problem.getNumberOfVariables(),200);

        algorithm = (rdpMOEADFS) new MOEADBuilder(problem, MOEADBuilder.Variant.rdpMOEADFS)
                .setCrossover(crossover)
                .setMutation(mutation)
                .setMaxEvaluations(200)
                .setPopulationSize(populationSize)
                .setResultPopulationSize(populationSize)
                .setNeighborhoodSelectionProbability(0.85)
                .setMaximumNumberOfReplacedSolutions(1)
                .setNeighborSize(Math.max(populationSize / 10, 4))
                .setNumberOfThreads(NUM_OF_CORE) // number of core
                .setOverlappingSize(Math.max(populationSize / 10, 4) / 2)
                .setMigrationRatio(10)
                .build() ;

        AlgorithmRunner algorithmRunner = new AlgorithmRunner.Executor(algorithm)
                .execute() ;

        long computingTime = algorithmRunner.getComputingTime() ;
        JMetalLogger.logger.info("Total execution time: " + computingTime + "ms");

        int subPopulationNum = algorithm.getSubPopulationNum();
        List<oipMOEADFS.SubProcess> subAlgorithmList = algorithm.getAlgorithmList();
        long[] subPopulationTimes = new long[subPopulationNum];
        String subPopulationTimeList = "";
        for (int subPopulationIndex = 0; subPopulationIndex < subPopulationNum; subPopulationIndex++){
            oipMOEADFS.SubProcess subAlgorithm = subAlgorithmList.get(subPopulationIndex);
            subPopulationTimes[subPopulationIndex] = subAlgorithm.getComputingTime();
            subPopulationTimeList += subAlgorithm.getComputingTime() + "ms; ";
        }
        JMetalLogger.logger.info("Sub progress execution time: " + subPopulationTimeList);

        List<DoubleSolution> population = algorithm.getResult() ;
        List<List<DoubleSolution>> populationList = algorithm.getRecordSolutions() ;

        printRecordSolutionSet(populationList);
        printFinalSolutionSet(population);
//        if (!referenceParetoFront.equals("")) {
//            printQualityIndicators(population, referenceParetoFront) ;
//        }
    }
}
