package org.uma.jmetal.FeatureSelectionExperiment;

import org.uma.jmetal.algorithm.Algorithm;
import org.uma.jmetal.algorithm.multiobjective.moead.AbstractMOEAD;
import org.uma.jmetal.algorithm.multiobjective.moead.MOEADBuilder;
import org.uma.jmetal.operator.MutationOperator;
import org.uma.jmetal.operator.impl.crossover.DifferentialEvolutionCrossover;
import org.uma.jmetal.operator.impl.mutation.PolynomialMutation;
import org.uma.jmetal.problem.Problem;
import org.uma.jmetal.problem.multiobjective.FeatureSelection.Australian;
import org.uma.jmetal.problem.multiobjective.FeatureSelection.Madelon;
import org.uma.jmetal.problem.multiobjective.FeatureSelection.Sonar;
import org.uma.jmetal.problem.multiobjective.FeatureSelection.Vehicle;
import org.uma.jmetal.qualityindicator.impl.GenerationalDistance;
import org.uma.jmetal.qualityindicator.impl.InvertedGenerationalDistance;
import org.uma.jmetal.qualityindicator.impl.InvertedGenerationalDistancePlus;
import org.uma.jmetal.qualityindicator.impl.hypervolume.PISAHypervolume;
import org.uma.jmetal.solution.DoubleSolution;
import org.uma.jmetal.util.experiment.Experiment;
import org.uma.jmetal.util.experiment.ExperimentBuilder;
import org.uma.jmetal.util.experiment.component.ExecuteAlgorithms;
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

public class pMOEADFS {

  private static int RUN_FROM;
  private static int RUN_TO;
  private static int NUM_SUBPOPULATION = 2;
  private static String ALGORITHM = "STAT";
  private static final String CLASS_NAME = new Object() {
    public String getClassName() {
      String clazzName = this.getClass().getName();
      System.out.println(clazzName);
      return clazzName.substring(clazzName.lastIndexOf('.') + 1,
              clazzName.lastIndexOf('$'));
    }
  }.getClassName();

  public static void main(String[] args) throws IOException {
    if (args.length == 0){
      RUN_FROM = 0;
      RUN_TO = 1;
    } else {
        RUN_FROM = Integer.parseInt(args[0]);
        RUN_TO = Integer.parseInt(args[1]);

        if (args.length > 2) {
            NUM_SUBPOPULATION = Integer.parseInt(args[2]);
            if (args.length == 4) {
                ALGORITHM = args[3];
            }
        }
    }

    String experimentBaseDirectory = "Experiments";

    List<ExperimentProblem<DoubleSolution>> problemList = new ArrayList<>();
//      problemList.add(new ExperimentProblem<>(new Australian()));
//    problemList.add(new ExperimentProblem<>(new Vehicle()));  //.changeReferenceFrontTo("DTLZ1.2D.pf")
      problemList.add(new ExperimentProblem<>(new Sonar()));
//      problemList.add(new ExperimentProblem<>(new Hillvalley()));
//      problemList.add(new ExperimentProblem<>(new Musk1()));
//      problemList.add(new ExperimentProblem<>(new Arrhythmia()));
//      problemList.add(new ExperimentProblem<>(new Madelon()));
//      problemList.add(new ExperimentProblem<>(new Isolet()));

    List<ExperimentAlgorithm<DoubleSolution, List<DoubleSolution>>> algorithmList =
            configureAlgorithmListForData(problemList);
//      configureAlgorithmListForIndicator(problemList);

    Experiment<DoubleSolution, List<DoubleSolution>> experiment =
            new ExperimentBuilder<DoubleSolution, List<DoubleSolution>>(CLASS_NAME)
                    .setAlgorithmList(algorithmList)
                    .setProblemList(problemList)
                    .setReferenceFrontDirectory("/pareto_fronts")
                    .setExperimentBaseDirectory(experimentBaseDirectory)
                    .setOutputParetoFrontFileName("FUN")
                    .setOutputParetoSetFileName("VAR")
                    .setIndicatorList(Arrays.asList(
//                            new Epsilon<DoubleSolution>(),
//                            new Spread<DoubleSolution>(),
                            new GenerationalDistance<DoubleSolution>(),
                            new PISAHypervolume<DoubleSolution>(),
                            new InvertedGenerationalDistance<DoubleSolution>(),
                            new InvertedGenerationalDistancePlus<DoubleSolution>()))
                    .setIndependentRuns(RUN_TO-RUN_FROM+1)
                    .setNumberOfCores(21)
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
  private static List<ExperimentAlgorithm<DoubleSolution, List<DoubleSolution>>> configureAlgorithmListForData(
          List<ExperimentProblem<DoubleSolution>> problemList) {
    List<ExperimentAlgorithm<DoubleSolution, List<DoubleSolution>>> algorithms = new ArrayList<>();

    MOEADBuilder.Variant algo = MOEADBuilder.Variant.MOEADSTAT;
    if (ALGORITHM.equals("oip"))
        algo = MOEADBuilder.Variant.oipMOEADFS;
    else if (ALGORITHM.equals("asp"))
        algo = MOEADBuilder.Variant.aspMOEADFS;
    else if (ALGORITHM.equals("rdp"))
        algo = MOEADBuilder.Variant.rdpMOEADFS;

    for (int run = RUN_FROM; run <= RUN_TO; run++) {

        // for get data
        for (int i = 0; i < problemList.size(); i++) {
            MutationOperator<DoubleSolution> mutation;
            DifferentialEvolutionCrossover crossover;
            double cr = 0.6 ;
            double f = 0.7 ;
            crossover = new DifferentialEvolutionCrossover(cr, f, "rand/1/bin");
            double mutationProbability = 1.0 / problemList.get(i).getProblem().getNumberOfVariables();
            double mutationDistributionIndex = 20.0;
            mutation = new PolynomialMutation(mutationProbability, mutationDistributionIndex);
            int populationSize = Math.min(problemList.get(i).getProblem().getNumberOfVariables(),200);
            AbstractMOEAD<DoubleSolution> algorithm = new MOEADBuilder(
                    problemList.get(i).getProblem(),
                    algo)
                    .setCrossover(crossover)
                    .setMutation(mutation)
                    .setMaxEvaluations(200)
                    .setPopulationSize(populationSize)
                    .setResultPopulationSize(populationSize)
                    .setNeighborhoodSelectionProbability(0.85)
                    .setMaximumNumberOfReplacedSolutions(1)
                    .setNeighborSize(Math.max(populationSize / 10, 4))
                    .setNumberOfThreads(NUM_SUBPOPULATION) // number of core
                    .setOverlappingSize(Math.max(populationSize / 10, 4) / 2)
                    .setMigrationRatio(10)
                    .build() ;
            algorithms.add(new ExperimentAlgorithm<>(algorithm, problemList.get(i), run).setRS(algorithm)); // set RS
        }
    }
      return algorithms;
  }
      private static List<ExperimentAlgorithm<DoubleSolution, List<DoubleSolution>>> configureAlgorithmListForIndicator(
              List<ExperimentProblem<DoubleSolution>> problemList) {
          List<ExperimentAlgorithm<DoubleSolution, List<DoubleSolution>>> algorithms = new ArrayList<>();
          for (int run = RUN_FROM; run <= RUN_TO; run++) {
        // for get indicator value
//        for (int i = 0; i < problemList.size(); i++) {
//          Algorithm<List<DoubleSolution>> algorithm = new NSGAIIBuilder<DoubleSolution>(
//                  problemList.get(i).getProblem(),
//                  new SBXCrossover(1.0, 20.0),
//                  new PolynomialMutation(1.0 / problemList.get(i).getProblem().getNumberOfVariables(),
//                          20.0),
//                  Math.min(problemList.get(i).getProblem().getNumberOfVariables(), 200))
//                  .setMaxEvaluations(problemList.get(i).getProblem().getNumberOfVariables() * 200)
//                  .build();
//          algorithms.add(new ExperimentAlgorithm<>(algorithm, problemList.get(i), run));
//        }
//
//        for (int i = 0; i < problemList.size(); i++) {
//          Algorithm<List<DoubleSolution>> algorithm = new SPEA2Builder<DoubleSolution>(
//                  problemList.get(i).getProblem(),
//                  new SBXCrossover(1.0, 10.0),
//                  new PolynomialMutation(1.0 / problemList.get(i).getProblem().getNumberOfVariables(),
//                          20.0))
//                  .setMaxIterations(200)
//                  .setPopulationSize(Math.min(problemList.get(i).getProblem().getNumberOfVariables(), 200))
//                  .build();
//          algorithms.add(new ExperimentAlgorithm<>(algorithm, problemList.get(i), run));
//        }
//
//        for (int i = 0; i < problemList.size(); i++) {
//          MutationOperator<DoubleSolution> mutation;
//          DifferentialEvolutionCrossover crossover;
//          double cr = 0.6 ;
//          double f = 0.7 ;
//          crossover = new DifferentialEvolutionCrossover(cr, f, "rand/1/bin");
//          double mutationProbability = 1.0 / problemList.get(i).getProblem().getNumberOfVariables();
//          double mutationDistributionIndex = 20.0;
//          mutation = new PolynomialMutation(mutationProbability, mutationDistributionIndex);
//          int populationSize = Math.min(problemList.get(i).getProblem().getNumberOfVariables(),200);
//          AbstractMOEAD<DoubleSolution> algorithm = new MOEADBuilder(
//                  problemList.get(i).getProblem(),
//                  MOEADBuilder.Variant.MOEAD)
//                  .setCrossover(crossover)
//                  .setMutation(mutation)
//                  .setMaxEvaluations(200 * populationSize)
//                  .setPopulationSize(populationSize)
//                  .setResultPopulationSize(populationSize)
//                  .setNeighborhoodSelectionProbability(0.85)
//                  .setMaximumNumberOfReplacedSolutions(1)
//                  .setNeighborSize(Math.max(populationSize / 10, 4))
//                  .setFunctionType(AbstractMOEAD.FunctionType.TCHE)
////                  .setDataDirectory("MOEAD_Weights")
//                  .build() ;
//          algorithms.add(new ExperimentAlgorithm<>(algorithm, problemList.get(i), run).setRS(algorithm));
//        }

//        for (int i = 0; i < problemList.size(); i++) {
//            MutationOperator<DoubleSolution> mutation;
//            DifferentialEvolutionCrossover crossover;
//            double cr = 0.6 ;
//            double f = 0.7 ;
//            crossover = new DifferentialEvolutionCrossover(cr, f, "rand/1/bin");
//            double mutationProbability = 1.0 / problemList.get(i).getProblem().getNumberOfVariables();
//            double mutationDistributionIndex = 20.0;
//            mutation = new PolynomialMutation(mutationProbability, mutationDistributionIndex);
//            int populationSize = Math.min(problemList.get(i).getProblem().getNumberOfVariables(),200);
//            AbstractMOEAD<DoubleSolution> algorithm = new MOEADBuilder(
//                    problemList.get(i).getProblem(),
//                    MOEADBuilder.Variant.oipMOEADFS)
//                    .setCrossover(crossover)
//                    .setMutation(mutation)
//                    .setMaxEvaluations(200)
//                    .setPopulationSize(populationSize)
//                    .setResultPopulationSize(populationSize)
//                    .setNeighborhoodSelectionProbability(0.85)
//                    .setMaximumNumberOfReplacedSolutions(1)
//                    .setNeighborSize(Math.max(populationSize / 10, 4))
//                    .setNumberOfThreads(1) // number of core
//                    .setOverlappingSize(Math.max(populationSize / 10, 4) / 2)
//                    .setMigrationRatio(10)
//                    .build() ;
//            algorithms.add(new ExperimentAlgorithm<>(algorithm, problemList.get(i), run).setRS(algorithm)); // set RS
//        }
//
        for (int i = 0; i < problemList.size(); i++) {
            MutationOperator<DoubleSolution> mutation;
            DifferentialEvolutionCrossover crossover;
            double cr = 0.6 ;
            double f = 0.7 ;
            crossover = new DifferentialEvolutionCrossover(cr, f, "rand/1/bin");
            double mutationProbability = 1.0 / problemList.get(i).getProblem().getNumberOfVariables();
            double mutationDistributionIndex = 20.0;
            mutation = new PolynomialMutation(mutationProbability, mutationDistributionIndex);
            int populationSize = Math.min(problemList.get(i).getProblem().getNumberOfVariables(),200);
            AbstractMOEAD<DoubleSolution> algorithm = new MOEADBuilder(
                    problemList.get(i).getProblem(),
                    MOEADBuilder.Variant.oipMOEADFS)
                    .setCrossover(crossover)
                    .setMutation(mutation)
                    .setMaxEvaluations(200)
                    .setPopulationSize(populationSize)
                    .setResultPopulationSize(populationSize)
                    .setNeighborhoodSelectionProbability(0.85)
                    .setMaximumNumberOfReplacedSolutions(1)
                    .setNeighborSize(Math.max(populationSize / 10, 4))
                    .setNumberOfThreads(2) // number of core
                    .setOverlappingSize(Math.max(populationSize / 10, 4) / 2)
                    .setMigrationRatio(10)
                    .build() ;
            algorithms.add(new ExperimentAlgorithm<>(algorithm, problemList.get(i), run).setRS(algorithm)); // set RS
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
            int populationSize = Math.min(problemList.get(i).getProblem().getNumberOfVariables(),200);
            AbstractMOEAD<DoubleSolution> algorithm = new MOEADBuilder(
                    problemList.get(i).getProblem(),
                    MOEADBuilder.Variant.oipMOEADFS)
                    .setCrossover(crossover)
                    .setMutation(mutation)
                    .setMaxEvaluations(200)
                    .setPopulationSize(populationSize)
                    .setResultPopulationSize(populationSize)
                    .setNeighborhoodSelectionProbability(0.85)
                    .setMaximumNumberOfReplacedSolutions(1)
                    .setNeighborSize(Math.max(populationSize / 10, 4))
                    .setNumberOfThreads(4) // number of core
                    .setOverlappingSize(Math.max(populationSize / 10, 4) / 2)
                    .setMigrationRatio(10)
                    .build() ;
            algorithms.add(new ExperimentAlgorithm<>(algorithm, problemList.get(i), run).setRS(algorithm)); // set RS
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
            int populationSize = Math.min(problemList.get(i).getProblem().getNumberOfVariables(),200);
            AbstractMOEAD<DoubleSolution> algorithm = new MOEADBuilder(
                    problemList.get(i).getProblem(),
                    MOEADBuilder.Variant.oipMOEADFS)
                    .setCrossover(crossover)
                    .setMutation(mutation)
                    .setMaxEvaluations(200)
                    .setPopulationSize(populationSize)
                    .setResultPopulationSize(populationSize)
                    .setNeighborhoodSelectionProbability(0.85)
                    .setMaximumNumberOfReplacedSolutions(1)
                    .setNeighborSize(Math.max(populationSize / 10, 4))
                    .setNumberOfThreads(8) // number of core
                    .setOverlappingSize(Math.max(populationSize / 10, 4) / 2)
                    .setMigrationRatio(10)
                    .build() ;
            algorithms.add(new ExperimentAlgorithm<>(algorithm, problemList.get(i), run).setRS(algorithm));
        }
//
//        for (int i = 0; i < problemList.size(); i++) {
//            MutationOperator<DoubleSolution> mutation;
//            DifferentialEvolutionCrossover crossover;
//            double cr = 0.6 ;
//            double f = 0.7 ;
//            crossover = new DifferentialEvolutionCrossover(cr, f, "rand/1/bin");
//            double mutationProbability = 1.0 / problemList.get(i).getProblem().getNumberOfVariables();
//            double mutationDistributionIndex = 20.0;
//            mutation = new PolynomialMutation(mutationProbability, mutationDistributionIndex);
//            int populationSize = Math.min(problemList.get(i).getProblem().getNumberOfVariables(),200);
//            AbstractMOEAD<DoubleSolution> algorithm = new MOEADBuilder(
//                    problemList.get(i).getProblem(),
//                    MOEADBuilder.Variant.aspMOEADFS)
//                    .setCrossover(crossover)
//                    .setMutation(mutation)
//                    .setMaxEvaluations(200)
//                    .setPopulationSize(populationSize)
//                    .setResultPopulationSize(populationSize)
//                    .setNeighborhoodSelectionProbability(0.85)
//                    .setMaximumNumberOfReplacedSolutions(1)
//                    .setNeighborSize(Math.max(populationSize / 10, 4))
//                    .setNumberOfThreads(1) // number of core
//                    .setOverlappingSize(Math.max(populationSize / 10, 4) / 2)
//                    .setMigrationRatio(10)
//                    .build() ;
//            algorithms.add(new ExperimentAlgorithm<>(algorithm, problemList.get(i), run).setRS(algorithm)); // set RS
//        }

        for (int i = 0; i < problemList.size(); i++) {
            MutationOperator<DoubleSolution> mutation;
            DifferentialEvolutionCrossover crossover;
            double cr = 0.6 ;
            double f = 0.7 ;
            crossover = new DifferentialEvolutionCrossover(cr, f, "rand/1/bin");
            double mutationProbability = 1.0 / problemList.get(i).getProblem().getNumberOfVariables();
            double mutationDistributionIndex = 20.0;
            mutation = new PolynomialMutation(mutationProbability, mutationDistributionIndex);
            int populationSize = Math.min(problemList.get(i).getProblem().getNumberOfVariables(),200);
            AbstractMOEAD<DoubleSolution> algorithm = new MOEADBuilder(
                    problemList.get(i).getProblem(),
                    MOEADBuilder.Variant.aspMOEADFS)
                    .setCrossover(crossover)
                    .setMutation(mutation)
                    .setMaxEvaluations(200)
                    .setPopulationSize(populationSize)
                    .setResultPopulationSize(populationSize)
                    .setNeighborhoodSelectionProbability(0.85)
                    .setMaximumNumberOfReplacedSolutions(1)
                    .setNeighborSize(Math.max(populationSize / 10, 4))
                    .setNumberOfThreads(2) // number of core
                    .setOverlappingSize(Math.max(populationSize / 10, 4) / 2)
                    .setMigrationRatio(10)
                    .build() ;
            algorithms.add(new ExperimentAlgorithm<>(algorithm, problemList.get(i), run).setRS(algorithm)); // set RS
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
            int populationSize = Math.min(problemList.get(i).getProblem().getNumberOfVariables(),200);
            AbstractMOEAD<DoubleSolution> algorithm = new MOEADBuilder(
                    problemList.get(i).getProblem(),
                    MOEADBuilder.Variant.aspMOEADFS)
                    .setCrossover(crossover)
                    .setMutation(mutation)
                    .setMaxEvaluations(200)
                    .setPopulationSize(populationSize)
                    .setResultPopulationSize(populationSize)
                    .setNeighborhoodSelectionProbability(0.85)
                    .setMaximumNumberOfReplacedSolutions(1)
                    .setNeighborSize(Math.max(populationSize / 10, 4))
                    .setNumberOfThreads(4) // number of core
                    .setOverlappingSize(Math.max(populationSize / 10, 4) / 2)
                    .setMigrationRatio(10)
                    .build() ;
            algorithms.add(new ExperimentAlgorithm<>(algorithm, problemList.get(i), run).setRS(algorithm)); // set RS
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
            int populationSize = Math.min(problemList.get(i).getProblem().getNumberOfVariables(),200);
            AbstractMOEAD<DoubleSolution> algorithm = new MOEADBuilder(
                    problemList.get(i).getProblem(),
                    MOEADBuilder.Variant.aspMOEADFS)
                    .setCrossover(crossover)
                    .setMutation(mutation)
                    .setMaxEvaluations(200)
                    .setPopulationSize(populationSize)
                    .setResultPopulationSize(populationSize)
                    .setNeighborhoodSelectionProbability(0.85)
                    .setMaximumNumberOfReplacedSolutions(1)
                    .setNeighborSize(Math.max(populationSize / 10, 4))
                    .setNumberOfThreads(8) // number of core
                    .setOverlappingSize(Math.max(populationSize / 10, 4) / 2)
                    .setMigrationRatio(10)
                    .build() ;
            algorithms.add(new ExperimentAlgorithm<>(algorithm, problemList.get(i), run).setRS(algorithm));
        }

//        for (int i = 0; i < problemList.size(); i++) {
//            MutationOperator<DoubleSolution> mutation;
//            DifferentialEvolutionCrossover crossover;
//            double cr = 0.6 ;
//            double f = 0.7 ;
//            crossover = new DifferentialEvolutionCrossover(cr, f, "rand/1/bin");
//            double mutationProbability = 1.0 / problemList.get(i).getProblem().getNumberOfVariables();
//            double mutationDistributionIndex = 20.0;
//            mutation = new PolynomialMutation(mutationProbability, mutationDistributionIndex);
//            int populationSize = Math.min(problemList.get(i).getProblem().getNumberOfVariables(),200);
//            AbstractMOEAD<DoubleSolution> algorithm = new MOEADBuilder(
//                    problemList.get(i).getProblem(),
//                    MOEADBuilder.Variant.rdpMOEADFS)
//                    .setCrossover(crossover)
//                    .setMutation(mutation)
//                    .setMaxEvaluations(200)
//                    .setPopulationSize(populationSize)
//                    .setResultPopulationSize(populationSize)
//                    .setNeighborhoodSelectionProbability(0.85)
//                    .setMaximumNumberOfReplacedSolutions(1)
//                    .setNeighborSize(Math.max(populationSize / 10, 4))
//                    .setNumberOfThreads(1) // number of core
//                    .setOverlappingSize(Math.max(populationSize / 10, 4) / 2)
//                    .setMigrationRatio(10)
//                    .build() ;
//            algorithms.add(new ExperimentAlgorithm<>(algorithm, problemList.get(i), run).setRS(algorithm)); // set RS
//        }

        for (int i = 0; i < problemList.size(); i++) {
            MutationOperator<DoubleSolution> mutation;
            DifferentialEvolutionCrossover crossover;
            double cr = 0.6 ;
            double f = 0.7 ;
            crossover = new DifferentialEvolutionCrossover(cr, f, "rand/1/bin");
            double mutationProbability = 1.0 / problemList.get(i).getProblem().getNumberOfVariables();
            double mutationDistributionIndex = 20.0;
            mutation = new PolynomialMutation(mutationProbability, mutationDistributionIndex);
            int populationSize = Math.min(problemList.get(i).getProblem().getNumberOfVariables(),200);
            AbstractMOEAD<DoubleSolution> algorithm = new MOEADBuilder(
                    problemList.get(i).getProblem(),
                    MOEADBuilder.Variant.rdpMOEADFS)
                    .setCrossover(crossover)
                    .setMutation(mutation)
                    .setMaxEvaluations(200)
                    .setPopulationSize(populationSize)
                    .setResultPopulationSize(populationSize)
                    .setNeighborhoodSelectionProbability(0.85)
                    .setMaximumNumberOfReplacedSolutions(1)
                    .setNeighborSize(Math.max(populationSize / 10, 4))
                    .setNumberOfThreads(2) // number of core
                    .setOverlappingSize(Math.max(populationSize / 10, 4) / 2)
                    .setMigrationRatio(10)
                    .build() ;
            algorithms.add(new ExperimentAlgorithm<>(algorithm, problemList.get(i), run).setRS(algorithm)); // set RS
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
            int populationSize = Math.min(problemList.get(i).getProblem().getNumberOfVariables(),200);
            AbstractMOEAD<DoubleSolution> algorithm = new MOEADBuilder(
                    problemList.get(i).getProblem(),
                    MOEADBuilder.Variant.rdpMOEADFS)
                    .setCrossover(crossover)
                    .setMutation(mutation)
                    .setMaxEvaluations(200)
                    .setPopulationSize(populationSize)
                    .setResultPopulationSize(populationSize)
                    .setNeighborhoodSelectionProbability(0.85)
                    .setMaximumNumberOfReplacedSolutions(1)
                    .setNeighborSize(Math.max(populationSize / 10, 4))
                    .setNumberOfThreads(4) // number of core
                    .setOverlappingSize(Math.max(populationSize / 10, 4) / 2)
                    .setMigrationRatio(10)
                    .build() ;
            algorithms.add(new ExperimentAlgorithm<>(algorithm, problemList.get(i), run).setRS(algorithm)); // set RS
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
            int populationSize = Math.min(problemList.get(i).getProblem().getNumberOfVariables(),200);
            AbstractMOEAD<DoubleSolution> algorithm = new MOEADBuilder(
                    problemList.get(i).getProblem(),
                    MOEADBuilder.Variant.rdpMOEADFS)
                    .setCrossover(crossover)
                    .setMutation(mutation)
                    .setMaxEvaluations(200)
                    .setPopulationSize(populationSize)
                    .setResultPopulationSize(populationSize)
                    .setNeighborhoodSelectionProbability(0.85)
                    .setMaximumNumberOfReplacedSolutions(1)
                    .setNeighborSize(Math.max(populationSize / 10, 4))
                    .setNumberOfThreads(8) // number of core
                    .setOverlappingSize(Math.max(populationSize / 10, 4) / 2)
                    .setMigrationRatio(10)
                    .build() ;
            algorithms.add(new ExperimentAlgorithm<>(algorithm, problemList.get(i), run).setRS(algorithm));
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
          int populationSize = Math.min(problemList.get(i).getProblem().getNumberOfVariables(),200);
          AbstractMOEAD<DoubleSolution> algorithm = new MOEADBuilder(
                  problemList.get(i).getProblem(),
                  MOEADBuilder.Variant.MOEADSTAT)
                  .setCrossover(crossover)
                  .setMutation(mutation)
                  .setMaxEvaluations(200)
                  .setPopulationSize(populationSize)
                  .setResultPopulationSize(populationSize)
                  .setNeighborhoodSelectionProbability(0.85)
                  .setMaximumNumberOfReplacedSolutions(1)
                  .setNeighborSize(Math.max(populationSize / 10, 4))
                  .build() ;
          algorithms.add(new ExperimentAlgorithm<>(algorithm, problemList.get(i), run).setRS(algorithm));
        }
    }
    return algorithms;
  }
}
