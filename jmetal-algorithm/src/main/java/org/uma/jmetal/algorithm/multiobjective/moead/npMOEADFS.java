package org.uma.jmetal.algorithm.multiobjective.moead;

import org.apache.commons.math3.geometry.euclidean.oned.SubOrientedPoint;
import org.uma.jmetal.algorithm.multiobjective.moead.util.MOEADUtils;
import org.uma.jmetal.operator.CrossoverOperator;
import org.uma.jmetal.operator.MutationOperator;
import org.uma.jmetal.problem.Problem;
import org.uma.jmetal.problem.multiobjective.FeatureSelection.FeatureSelection;
import org.uma.jmetal.solution.DoubleSolution;
import org.uma.jmetal.util.JMetalLogger;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class npMOEADFS extends MOEADSTAT {
	protected int processorNumber;
	protected ExecutorService executorService;

	protected int[] permutation;

	public npMOEADFS(Problem<DoubleSolution> problem,
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

	protected void submitToSlaves (DoubleSolution[] childList) {	// submit processorNumber tasks until finished
		Future[] subProcessState = new Future[processorNumber];

		int idleProcessorNumber = processorNumber;
		for (int i = 0; i < populationSize; i++){
			if (idleProcessorNumber > 0){	// submit to slave processors
				idleProcessorNumber --;

				int subProblemId = permutation[i];
				NeighborType neighborType = chooseNeighborType();
				DoubleSolution child = childList[subProblemId];

				evaluateRunable evaluateChildProcess = new evaluateRunable(child, subProblemId, neighborType);
				subProcessState[idleProcessorNumber] = executorService.submit(evaluateChildProcess);

			} else {
				// check sub process state.
				checkSubProcessState(subProcessState);
				idleProcessorNumber = processorNumber;
			}
		}
	}

	protected void submitToSlaves (List<Integer> indexToAdd) {	// submit processorNumber tasks until finished
		Future[] subProcessState = new Future[processorNumber];

		int idleProcessorNumber = processorNumber;
		for (int i = 0; i < indexToAdd.size(); i++){
			if (idleProcessorNumber > 0) {
				idleProcessorNumber --;

				int index = indexToAdd.get(i);
				fixRunable fixProcess = new fixRunable(index);
				subProcessState[idleProcessorNumber] = executorService.submit(fixProcess);

			} else {
				// check sub process state.
				checkSubProcessState(subProcessState);
				idleProcessorNumber = processorNumber;
			}
		}
	}

	protected void checkSubProcessState (Future[] subProcessState) {
		for (int subProcessIndex = 0; subProcessIndex < subProcessState.length; subProcessIndex ++){
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
			permutation = new int[populationSize];
			MOEADUtils.randomPermutation(permutation, populationSize);

			if((iterations%this.stepIteration)==0){
				List<DoubleSolution> list = new ArrayList<DoubleSolution>();
				for(DoubleSolution sol: population){
					list.add((DoubleSolution) sol.copy());
				}
				recordSolutions.add(list);
			}

			DoubleSolution[] childList = new DoubleSolution[populationSize];
			for (int i = 0; i < populationSize; i++) {
				int subProblemId = permutation[i];

				NeighborType neighborType = chooseNeighborType() ;
				List<DoubleSolution> parents = parentSelection(subProblemId, neighborType) ;

				differentialEvolutionCrossover.setCurrentSolution(population.get(subProblemId));
				List<DoubleSolution> children = crossoverOperator.execute(parents);
				DoubleSolution child = children.get(0) ;
				mutationOperator.execute(child);

				((FeatureSelection)problem).reduceSize(child, refPoints[subProblemId]);

				childList[subProblemId] = child;	// child 按照weight vector次序排列
			}

			submitToSlaves(childList);

			//check for the same one in the population before evaluating
			//the solution with large nref add random feature to nref
			List<Integer> indexToAdd = checkForDuplicate();
			//System.out.println("Re-initializing "+indexToRandom.size());
			submitToSlaves(indexToAdd);

//			updateExternalPopulation();
			iterations++;
		} while (iterations < maxEvaluations);

		executorService.shutdown();
	}

	@Override
	public String getName() {
		// TODO Auto-generated method stub
		return "npMOEAD-FS-" + processorNumber;
	}
	@Override
	public String getDescription() {
		// TODO Auto-generated method stub
		return "naive master-slave mode parallel implementation of MOEAD with multiple reference points";
	}

	protected class evaluateRunable implements Runnable {
		private DoubleSolution child;
		private int subProblemId;
		private NeighborType neighborType;

		public evaluateRunable(DoubleSolution child, int subProblemId, NeighborType neighborType) {
			this.child = child;
			this.subProblemId = subProblemId;
			this.neighborType = neighborType;
		}

		@Override
		public void run() {
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
