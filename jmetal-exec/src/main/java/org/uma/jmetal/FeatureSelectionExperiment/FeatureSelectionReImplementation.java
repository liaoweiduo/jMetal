package org.uma.jmetal.FeatureSelectionExperiment;

import org.uma.jmetal.algorithm.Algorithm;
import org.uma.jmetal.algorithm.multiobjective.moead.AbstractMOEAD;
import org.uma.jmetal.algorithm.multiobjective.moead.MOEADBuilder;
import org.uma.jmetal.algorithm.multiobjective.nsgaii.NSGAIIBuilder;
import org.uma.jmetal.algorithm.multiobjective.spea2.SPEA2Builder;
import org.uma.jmetal.operator.MutationOperator;
import org.uma.jmetal.operator.impl.crossover.DifferentialEvolutionCrossover;
import org.uma.jmetal.operator.impl.crossover.SBXCrossover;
import org.uma.jmetal.operator.impl.mutation.PolynomialMutation;
import org.uma.jmetal.problem.Problem;
import org.uma.jmetal.problem.multiobjective.FeatureSelection.Isolet;
import org.uma.jmetal.problem.multiobjective.FeatureSelection.Madelon;
import org.uma.jmetal.problem.multiobjective.FeatureSelection.Musk1;
import org.uma.jmetal.problem.multiobjective.FeatureSelection.Vehicle;
import org.uma.jmetal.qualityindicator.impl.*;
import org.uma.jmetal.qualityindicator.impl.hypervolume.PISAHypervolume;
import org.uma.jmetal.solution.DoubleSolution;
import org.uma.jmetal.util.experiment.Experiment;
import org.uma.jmetal.util.experiment.ExperimentBuilder;
import org.uma.jmetal.util.experiment.component.*;
import org.uma.jmetal.util.experiment.util.ExperimentAlgorithm;
import org.uma.jmetal.util.experiment.util.ExperimentProblem;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Example of experimental study based on solving the problems (configured with 3 objectives) with the algorithms
 * NSGAII, SPEA2, and SMPSO
 * <p>
 * This experiment assumes that the reference Pareto front are known and stored in files whose names are different
 * from the default name expected for every problem. While the default would be "problem_name.pf" (e.g. DTLZ1.pf),
 * the references are stored in files following the nomenclature "problem_name.3D.pf" (e.g. DTLZ1.3D.pf). This is
 * indicated when creating the ExperimentProblem instance of each of the evaluated poblems by using the method
 * changeReferenceFrontTo()
 * <p>
 * Six quality indicators are used for performance assessment.
 * <p>
 * The steps to carry out the experiment are: 1. Configure the experiment 2. Execute the algorithms
 * 3. Compute que quality indicators 4. Generate Latex tables reporting means and medians 5.
 * Generate R scripts to produce latex tables with the result of applying the Wilcoxon Rank Sum Test
 * 6. Generate Latex tables with the ranking obtained by applying the Friedman test 7. Generate R
 * scripts to obtain boxplots
 */

public class FeatureSelectionReImplementation {

  private static final int INDEPENDENT_RUNS = 20;
  private static final String CLASS_NAME = new Object() {
    public String getClassName() {
      String clazzName = this.getClass().getName();
      System.out.println(clazzName);
      return clazzName.substring(clazzName.lastIndexOf('.') + 1,
              clazzName.lastIndexOf('$'));
    }
  }.getClassName();

  public static void main(String[] args) throws IOException {
    String experimentBaseDirectory = (args.length == 1)? args[0]:"Experiments";

    List<ExperimentProblem<DoubleSolution>> problemList = new ArrayList<>();
    problemList.add(new ExperimentProblem<>(new Vehicle()).changeReferenceFrontTo("DTLZ1.2D.pf"));
    problemList.add(new ExperimentProblem<>(new Musk1()).changeReferenceFrontTo("DTLZ1.2D.pf"));
    problemList.add(new ExperimentProblem<>(new Madelon()).changeReferenceFrontTo("DTLZ1.2D.pf"));
    problemList.add(new ExperimentProblem<>(new Isolet()).changeReferenceFrontTo("DTLZ1.2D.pf"));

    List<ExperimentAlgorithm<DoubleSolution, List<DoubleSolution>>> algorithmList =
            configureAlgorithmList(problemList);

    Experiment<DoubleSolution, List<DoubleSolution>> experiment =
            new ExperimentBuilder<DoubleSolution, List<DoubleSolution>>(CLASS_NAME)
                    .setAlgorithmList(algorithmList)
                    .setProblemList(problemList)
                    .setReferenceFrontDirectory("/pareto_fronts")
                    .setExperimentBaseDirectory(experimentBaseDirectory)
                    .setOutputParetoFrontFileName("FUN")
                    .setOutputParetoSetFileName("VAR")
                    .setIndicatorList(Arrays.asList(
                            new Epsilon<DoubleSolution>(),
                            new Spread<DoubleSolution>(),
                            new GenerationalDistance<DoubleSolution>(),
                            new PISAHypervolume<DoubleSolution>(),
                            new InvertedGenerationalDistance<DoubleSolution>(),
                            new InvertedGenerationalDistancePlus<DoubleSolution>()))
                    .setIndependentRuns(INDEPENDENT_RUNS)
                    .setNumberOfCores(8)
                    .build();

    new ExecuteAlgorithms<>(experiment).run();
//    new ComputeQualityIndicators<>(experiment).run();
//    new GenerateLatexTablesWithStatistics(experiment).run();
//    new GenerateWilcoxonTestTablesWithR<>(experiment).run();
//    new GenerateFriedmanTestTables<>(experiment).run();
//    new GenerateBoxplotsWithR<>(experiment).setRows(3).setColumns(3).setDisplayNotch().run();
  }

  /**
   * The algorithm list is composed of pairs {@link Algorithm} + {@link Problem} which form part of
   * a {@link ExperimentAlgorithm}, which is a decorator for class {@link Algorithm}.
   */
  static List<ExperimentAlgorithm<DoubleSolution, List<DoubleSolution>>> configureAlgorithmList(
          List<ExperimentProblem<DoubleSolution>> problemList) {
    List<ExperimentAlgorithm<DoubleSolution, List<DoubleSolution>>> algorithms = new ArrayList<>();
    for (int run = 0; run < INDEPENDENT_RUNS; run++) {

      for (int i = 0; i < problemList.size(); i++) {
        Algorithm<List<DoubleSolution>> algorithm = new NSGAIIBuilder<DoubleSolution>(
                problemList.get(i).getProblem(),
                new SBXCrossover(1.0, 20.0),
                new PolynomialMutation(1.0 / problemList.get(i).getProblem().getNumberOfVariables(),
                        20.0),
                Math.min(problemList.get(i).getProblem().getNumberOfVariables(),200))
                .setMaxEvaluations(problemList.get(i).getProblem().getNumberOfVariables() * 200)
                .build();
        algorithms.add(new ExperimentAlgorithm<>(algorithm, problemList.get(i), run));
      }

      for (int i = 0; i < problemList.size(); i++) {
        Algorithm<List<DoubleSolution>> algorithm = new SPEA2Builder<DoubleSolution>(
                problemList.get(i).getProblem(),
                new SBXCrossover(1.0, 10.0),
                new PolynomialMutation(1.0 / problemList.get(i).getProblem().getNumberOfVariables(),
                        20.0))
                .setMaxIterations(200)
                .setPopulationSize(Math.min(problemList.get(i).getProblem().getNumberOfVariables(),200))
                .build();
        algorithms.add(new ExperimentAlgorithm<>(algorithm, problemList.get(i), run));
      }

      for (int i = 0; i < problemList.size(); i++) {
        MutationOperator<DoubleSolution> mutation;
        DifferentialEvolutionCrossover crossover;
        double cr = 0.6 ;
        double f = 0.7 ;
        crossover = new DifferentialEvolutionCrossover(cr, f, "rand/1/bin");
        double mutationProbability = 1.0 / problemList.get(i).getProblem().getNumberOfVariables();
        double mutationDistributionIndex = 20.0;
        mutation = new PolynomialMutation(mutationProbability, mutationDistributionIndex);
        Algorithm<List<DoubleSolution>> algorithm = new MOEADBuilder(
                problemList.get(i).getProblem(),
                MOEADBuilder.Variant.MOEAD)
                .setCrossover(crossover)
                .setMutation(mutation)
                .setMaxEvaluations(200 * problemList.get(i).getProblem().getNumberOfVariables())
                .setPopulationSize(Math.min(problemList.get(i).getProblem().getNumberOfVariables(),200))
                .setResultPopulationSize(Math.min(problemList.get(i).getProblem().getNumberOfVariables(),200))
                .setNeighborhoodSelectionProbability(0.85)
                .setMaximumNumberOfReplacedSolutions(1)
                .setNeighborSize(Math.max(problemList.get(i).getProblem().getNumberOfVariables() / 10, 4))
                .setFunctionType(AbstractMOEAD.FunctionType.TCHE)
                .setDataDirectory("MOEAD_Weights")
                .build() ;
        algorithms.add(new ExperimentAlgorithm<>(algorithm, problemList.get(i), run));
      }

      for (int i = 0; i < problemList.size(); i++) {
        MutationOperator<DoubleSolution> mutation;
        DifferentialEvolutionCrossover crossover;
        double cr = 0.6 ;
        double f = 0.7 ;
        crossover = new DifferentialEvolutionCrossover(cr, f, "rand/1/bin");
        double mutationProbability = 1.0 / problemList.get(i).getProblem().getNumberOfVariables();
        double mutationDistributionIndex = 20.0;
        mutation = new PolynomialMutation(mutationProbability, mutationDistributionIndex);
        Algorithm<List<DoubleSolution>> algorithm = new MOEADBuilder(
                problemList.get(i).getProblem(),
                MOEADBuilder.Variant.MOEADSTAT)
                .setCrossover(crossover)
                .setMutation(mutation)
                .setMaxEvaluations(200)
                .setPopulationSize(Math.min(problemList.get(i).getProblem().getNumberOfVariables(),200))
                .setResultPopulationSize(Math.min(problemList.get(i).getProblem().getNumberOfVariables(),200))
                .setNeighborhoodSelectionProbability(0.85)
                .setMaximumNumberOfReplacedSolutions(1)
                .setNeighborSize(Math.max(problemList.get(i).getProblem().getNumberOfVariables() / 10, 4))
                .build() ;
        algorithms.add(new ExperimentAlgorithm<>(algorithm, problemList.get(i), run));
      }
    }
    return algorithms;
  }
}
