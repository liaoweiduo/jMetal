package org.uma.jmetal.runner.multiobjective;

import org.uma.jmetal.algorithm.multiobjective.moead.MOEADBuilder;
import org.uma.jmetal.algorithm.multiobjective.moead.ffpMOEADFS;
import org.uma.jmetal.algorithm.multiobjective.moead.sfpMOEADFS;
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
public class sfpMOEADFSRunner extends AbstractAlgorithmRunner {
    /**
     * @param args Command line arguments.
     * @throws SecurityException
     * Invoking command:
    java org.uma.jmetal.runner.multiobjective.MOEADRunner problemName [referenceFront]
     */
    public static void main(String[] args) throws FileNotFoundException {
        int NUM_OF_CORE;
        DoubleProblem problem;
        sfpMOEADFS algorithm;
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

        algorithm = (sfpMOEADFS) new MOEADBuilder(problem, MOEADBuilder.Variant.sfpMOEADFS)
                .setCrossover(crossover)
                .setMutation(mutation)
                .setMaxEvaluations(200)
                .setPopulationSize(Math.min(problem.getNumberOfVariables(),200))
                .setResultPopulationSize(Math.min(problem.getNumberOfVariables(),200))
                .setNeighborhoodSelectionProbability(0.85)
                .setMaximumNumberOfReplacedSolutions(Math.max(problem.getNumberOfVariables() / 10, 4))
                .setNeighborSize(Math.max(problem.getNumberOfVariables() / 10, 4))
                .setNumberOfThreads(NUM_OF_CORE)
                .build() ;

        AlgorithmRunner algorithmRunner = new AlgorithmRunner.Executor(algorithm)
                .execute() ;

        long computingTime = algorithmRunner.getComputingTime() ;
        JMetalLogger.logger.info("Total execution time: " + computingTime + "ms");

        List<DoubleSolution> population = algorithm.getResult() ;
        List<List<DoubleSolution>> populationList = algorithm.getRecordSolutions();

        printRecordSolutionSet(populationList);
        printFinalSolutionSet(population);
        if (!referenceParetoFront.equals("")) {
            printQualityIndicators(population, referenceParetoFront) ;
        }
    }
}
