package org.uma.jmetal.algorithm.multiobjective.moead;

import org.uma.jmetal.operator.CrossoverOperator;
import org.uma.jmetal.operator.MutationOperator;
import org.uma.jmetal.problem.Problem;
import org.uma.jmetal.problem.multiobjective.FeatureSelection.FeatureSelection;
import org.uma.jmetal.solution.DoubleSolution;

import java.util.*;
import java.util.concurrent.Future;

public class rMSM extends npMOEADFS {

	public rMSM(Problem<DoubleSolution> problem,
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
				dataDirectory, neighborhoodSelectionProbability, maximumNumberOfReplacedSolutions, neighborSize, processorNumber);
	}

	@Override
	protected void submitToSlaves (DoubleSolution[] childList) {	// submit processorNumber tasks until finished
		// random order

		Future[] subProcessState = new Future[populationSize];

		for (int i = 0; i < populationSize; i++) {
			int subProblemId = permutation[i];
			NeighborType neighborType = chooseNeighborType();
			DoubleSolution child = childList[subProblemId];

			evaluateRunable evaluateChildProcess = new evaluateRunable(child, subProblemId, neighborType);
			subProcessState[i] = executorService.submit(evaluateChildProcess);
		}

		// check sub process state.
		checkSubProcessState(subProcessState,0,populationSize);
	}

	@Override
	protected void submitToSlaves (List<Integer> indexToAdd) {	// submit processorNumber tasks until finished
		Future[] subProcessState = new Future[indexToAdd.size()];
		for (int i = 0; i < indexToAdd.size(); i++){
			int index = indexToAdd.get(i);
			fixRunable fixProcess = new fixRunable(index);
			subProcessState[i] = executorService.submit(fixProcess);
		}

		checkSubProcessState(subProcessState,0,indexToAdd.size());
	}

	@Override
	public String getName() {
		// TODO Auto-generated method stub
		return "rMSM-" + processorNumber;
	}
	@Override
	public String getDescription() {
		// TODO Auto-generated method stub
		return "random master-slave mode parallel implementation of MOEAD with multiple reference points";
	}
}
