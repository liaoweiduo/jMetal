package org.uma.jmetal.algorithm.multiobjective.moead;

import org.uma.jmetal.algorithm.multiobjective.moead.util.MOEADUtils;
import org.uma.jmetal.operator.CrossoverOperator;
import org.uma.jmetal.operator.MutationOperator;
import org.uma.jmetal.operator.impl.crossover.DifferentialEvolutionCrossover;
import org.uma.jmetal.problem.Problem;
import org.uma.jmetal.problem.multiobjective.FeatureSelection.FeatureSelection;
import org.uma.jmetal.solution.DoubleSolution;
import org.uma.jmetal.util.JMetalException;
import org.uma.jmetal.util.JMetalLogger;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

public class pMOEADSTAT extends MOEADSTAT {
	protected int processorNumber;
	protected ExecutorService executorService;

	public pMOEADSTAT(Problem<DoubleSolution> problem,
                    int populationSize,
                    int resultPopulationSize,
                    int maxIteration,
                    CrossoverOperator<DoubleSolution> crossoverOperator,
                    MutationOperator<DoubleSolution> mutation,
                    FunctionType functionType,
                    String dataDirectory,
                    double neighborhoodSelectionProbability,
                    int maximumNumberOfReplacedSolutions,
                    int neighborSize,
					int processorNumber) {
		super(problem, populationSize, resultPopulationSize, maxIteration, crossoverOperator, mutation, functionType,
				dataDirectory, neighborhoodSelectionProbability, maximumNumberOfReplacedSolutions, neighborSize);

		this.processorNumber = processorNumber;
		this.executorService = Executors.newFixedThreadPool(this.processorNumber);

	}

	@Override
	public void run() {
		initializePopulation();
		initializeUniformRef();
		initializeNeighborhood();
		idealPoint.update(population); ;

		evaluations = populationSize ;
		int iterations = 0 ;
		do {
			//	updateNeighborhoodTopology();
			int[] permutation = new int[populationSize];
			MOEADUtils.randomPermutation(permutation, populationSize);

			if((iterations%this.stepIteration)==0){
				List<DoubleSolution> list = new ArrayList<DoubleSolution>();
				for(DoubleSolution sol: population){
					list.add((DoubleSolution) sol.copy());
				}
				recordSolutions.add(list);
			}

			Future[] subProcessState = new Future[populationSize];
			for (int i = 0; i < populationSize; i++) {
				evaluateRunable evaluateChildProcess = new evaluateRunable(permutation, i);
				subProcessState[i] = executorService.submit(evaluateChildProcess);
			}
			// check sub process state.
			for (int subProcessIndex = 0; subProcessIndex < populationSize; subProcessIndex ++){
				try {
					subProcessState[subProcessIndex].get();
				} catch (InterruptedException e) {
					JMetalLogger.logger.info
							("evaluate processor throws an interrupted exception, subProcessId:" + subProcessIndex);
					e.printStackTrace();
				} catch (ExecutionException e) {
					JMetalLogger.logger.info
							("evaluate processor throws an execution exception, subProcessId:" + subProcessIndex);
					e.printStackTrace();
				}
			}

			//check for the same one in the population before evaluating
			//the solution with large nref add random feature to nref
			List<Integer> indexToAdd = checkForDuplicate();
			//System.out.println("Re-initializing "+indexToRandom.size());
			subProcessState = new Future[indexToAdd.size()];
			for (int i = 0; i < indexToAdd.size(); i++){
				int index = indexToAdd.get(i);
				fixRunable fixProcess = new fixRunable(index);
				subProcessState[i] = executorService.submit(fixProcess);
			}
			// check sub process state.
			for (int subProcessIndex = 0; subProcessIndex < indexToAdd.size(); subProcessIndex ++){
				try {
					subProcessState[subProcessIndex].get();
				} catch (InterruptedException e) {
					JMetalLogger.logger.info
							("fix processor throws an interrupted exception, subProcessId:" + subProcessIndex);
					e.printStackTrace();
				} catch (ExecutionException e) {
					JMetalLogger.logger.info
							("fix processor throws an execution exception, subProcessId:" + subProcessIndex);
					e.printStackTrace();
				}
			}

//			updateExternalPopulation();
			iterations++;
		} while (iterations < maxEvaluations);

		executorService.shutdown();
	}

	@Override
	public String getName() {
		// TODO Auto-generated method stub
		return "pMOEAD-STAT-" + processorNumber;
	}
	@Override
	public String getDescription() {
		// TODO Auto-generated method stub
		return "master-slave mode parallel implementation of MOEAD with multiple reference points";
	}

	protected class evaluateRunable implements Runnable {
		private int[] permutation;
		private int populationIndex;

		public evaluateRunable(int[] permutation, int populationIndex) {
			this.permutation = permutation;
			this. populationIndex = populationIndex;
		}

		@Override
		public void run() {
			int subProblemId = permutation[populationIndex];

			NeighborType neighborType = chooseNeighborType() ;
			List<DoubleSolution> parents = parentSelection(subProblemId, neighborType) ;

			differentialEvolutionCrossover.setCurrentSolution(population.get(subProblemId));
			List<DoubleSolution> children = crossoverOperator.execute(parents);
			DoubleSolution child = children.get(0) ;
			mutationOperator.execute(child);

			//repairSolutionSequentially(child, this.refPoints[i]);
			((FeatureSelection)problem).reduceSize(child, refPoints[subProblemId]);
			problem.evaluate(child);
			evaluations++;

			idealPoint.update(child.getObjectives());
			updateNeighborhood(child, subProblemId, neighborType);
		}
	}
	protected class fixRunable implements Runnable {
		private int index;

		public fixRunable(int index) {
			this.index = index;
		}

		@Override
		public void run() {
			DoubleSolution sol = population.get(index);
			double refRate = refPoints[index];
			((FeatureSelection)problem).increaseSize(sol, refRate);
			problem.evaluate(sol);
			evaluations++;
		}
	}
}
