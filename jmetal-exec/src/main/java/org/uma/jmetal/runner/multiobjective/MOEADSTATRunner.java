package org.uma.jmetal.runner.multiobjective;

import org.uma.jmetal.algorithm.Algorithm;
import org.uma.jmetal.algorithm.multiobjective.moead.AbstractMOEAD;
import org.uma.jmetal.algorithm.multiobjective.moead.MOEADBuilder;
import org.uma.jmetal.algorithm.multiobjective.moead.MOEADSTAT;
import org.uma.jmetal.operator.MutationOperator;
import org.uma.jmetal.operator.impl.crossover.DifferentialEvolutionCrossover;
import org.uma.jmetal.operator.impl.mutation.PolynomialMutation;
import org.uma.jmetal.problem.DoubleProblem;
import org.uma.jmetal.problem.multiobjective.FeatureSelection.Vehicle;
import org.uma.jmetal.solution.DoubleSolution;
import org.uma.jmetal.solution.Solution;
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
public class MOEADSTATRunner extends AbstractAlgorithmRunner {
    /**
     * @param args Command line arguments.
     * @throws SecurityException
     * Invoking command:
    java org.uma.jmetal.runner.multiobjective.MOEADRunner problemName [referenceFront]
     */
    public static void main(String[] args) throws FileNotFoundException {
        DoubleProblem problem;
        AbstractMOEAD<DoubleSolution> algorithm;
        MutationOperator<DoubleSolution> mutation;
        DifferentialEvolutionCrossover crossover;

        String problemName ;
        String referenceParetoFront = "";
        if (args.length == 1) {
            problemName = args[0];
        } else if (args.length == 2) {
            problemName = args[0] ;
            referenceParetoFront = args[1] ;
        } else {
            problemName = "org.uma.jmetal.problem.multiobjective.FeatureSelection.Vehicle";
            referenceParetoFront = "jmetal-problem/src/test/resources/pareto_fronts/DTLZ1.2D.pf";
        }

        problem = (DoubleProblem) ProblemUtils.<DoubleSolution> loadProblem(problemName);

        double cr = 0.6 ;
        double f = 0.7 ;
        crossover = new DifferentialEvolutionCrossover(cr, f, "rand/1/bin");

        double mutationProbability = 1.0 / problem.getNumberOfVariables();
        double mutationDistributionIndex = 20.0;
        mutation = new PolynomialMutation(mutationProbability, mutationDistributionIndex);

        algorithm = new MOEADBuilder(problem, MOEADBuilder.Variant.MOEADSTAT)
                .setCrossover(crossover)
                .setMutation(mutation)
                .setMaxEvaluations(200)
                .setPopulationSize(Math.min(problem.getNumberOfVariables(),200))
                .setResultPopulationSize(problem.getNumberOfVariables())
                .setNeighborhoodSelectionProbability(0.85)
                .setMaximumNumberOfReplacedSolutions(1)
                .setNeighborSize(Math.max(problem.getNumberOfVariables() / 10, 4))
                .build() ;

        AlgorithmRunner algorithmRunner = new AlgorithmRunner.Executor(algorithm)
                .execute() ;

        long computingTime = algorithmRunner.getComputingTime() ;
        JMetalLogger.logger.info("Total execution time: " + computingTime + "ms");

        List<DoubleSolution> population = algorithm.getResult() ;
        List<List> populationList = algorithm.getRecordSolutions();

        printRecordSolutionSet(populationList);
        printFinalSolutionSet(population);
        if (!referenceParetoFront.equals("")) {
            printQualityIndicators(population, referenceParetoFront) ;
        }
    }
}
