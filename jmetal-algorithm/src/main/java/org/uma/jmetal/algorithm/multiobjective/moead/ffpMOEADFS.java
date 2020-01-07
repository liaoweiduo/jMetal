package org.uma.jmetal.algorithm.multiobjective.moead;

import org.uma.jmetal.algorithm.multiobjective.moead.util.MOEADUtils;
import org.uma.jmetal.operator.CrossoverOperator;
import org.uma.jmetal.operator.MutationOperator;
import org.uma.jmetal.problem.Problem;
import org.uma.jmetal.problem.multiobjective.FeatureSelection.FeatureSelection;
import org.uma.jmetal.solution.DoubleSolution;
import org.uma.jmetal.util.JMetalLogger;

import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class ffpMOEADFS extends npMOEADFS {

	public ffpMOEADFS(Problem<DoubleSolution> problem,
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

		// sort childList in ascending order of time
		List<DoubleSolution> childListArray = Arrays.asList(childList);
		childListArray.sort(new Comparator<DoubleSolution>() {
			@Override
			public int compare(DoubleSolution o1, DoubleSolution o2) {	// 升序
				FeatureSelection fs = (FeatureSelection) problem;
				int nF1 = fs.getSelectedFeatureNumber(o1);
				int nF2 = fs.getSelectedFeatureNumber(o2);
				if (nF1 < nF2)
					return -1;
				else if (nF1 == nF2)
					return 0;
				else
					return 1;
			}
		});

		Future[] subProcessState = new Future[populationSize];

		for (int i = 0; i < populationSize; i++) {
			NeighborType neighborType = chooseNeighborType();
			DoubleSolution child = childListArray.get(i);

			evaluateRunable evaluateChildProcess = new evaluateRunable(child, i, neighborType);
			subProcessState[i] = executorService.submit(evaluateChildProcess);
		}

		// check sub process state.
		checkSubProcessState(subProcessState);
	}

	@Override
	protected void submitToSlaves (List<Integer> indexToAdd) {	// submit processorNumber tasks until finished
		Future[] subProcessState = new Future[indexToAdd.size()];
		for (int i = 0; i < indexToAdd.size(); i++){
			int index = indexToAdd.get(i);
			fixRunable fixProcess = new fixRunable(index);
			subProcessState[i] = executorService.submit(fixProcess);
		}

		checkSubProcessState(subProcessState);
	}

	@Override
	public String getName() {
		// TODO Auto-generated method stub
		return "ffpMOEAD-FS-" + processorNumber;
	}
	@Override
	public String getDescription() {
		// TODO Auto-generated method stub
		return "fast-one-first-serve master-slave mode parallel implementation of MOEAD with multiple reference points";
	}
}
