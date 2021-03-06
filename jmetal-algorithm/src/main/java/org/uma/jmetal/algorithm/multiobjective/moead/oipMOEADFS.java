package org.uma.jmetal.algorithm.multiobjective.moead;

import org.uma.jmetal.algorithm.multiobjective.moead.util.MOEADUtils;
import org.uma.jmetal.operator.CrossoverOperator;
import org.uma.jmetal.operator.MutationOperator;
import org.uma.jmetal.operator.impl.crossover.DifferentialEvolutionCrossover;
import org.uma.jmetal.problem.Problem;
import org.uma.jmetal.problem.multiobjective.FeatureSelection.FeatureSelection;
import org.uma.jmetal.solution.DoubleSolution;
import org.uma.jmetal.util.JMetalException;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class oipMOEADFS extends aspMOEADFS {

	public oipMOEADFS(Problem<DoubleSolution> problem,
                      int populationSize,		// total populationSize
                      int resultPopulationSize,
                      int maxIteration,
                      CrossoverOperator<DoubleSolution> crossoverOperator,
                      MutationOperator<DoubleSolution> mutation,
                      double neighborhoodSelectionProbability,
                      int maximumNumberOfReplacedSolutions,
                      int neighborSize,			// T = populationSize / 10 or 4
					  int subPopulationNum,		// core number
					  int overlappingSize,		// normally T/2
					  int migrationRatio		// iterations % migrationRatio == 0
	) {
		super(problem, populationSize, resultPopulationSize, maxIteration, crossoverOperator, mutation,
				neighborhoodSelectionProbability, maximumNumberOfReplacedSolutions, neighborSize,
				subPopulationNum, overlappingSize, migrationRatio);
	}

	@Override
	public void populationAssignTask() {
		int numOfCore = getSubPopulationNum();
		int islandSize = populationSize / numOfCore;
		int islandId = -1;
		for (int individualIndex = 0; individualIndex < populationSize; individualIndex ++){
			if (islandId < populationSize % numOfCore){
				if (individualIndex % (islandSize + 1) == 0){
					islandId ++;
				}
			}else{
				if ((individualIndex - (islandSize + 1) * (populationSize % numOfCore)) % islandSize == 0){
					islandId ++;
				}
			}
			this.populationAssign[individualIndex] = islandId;
		}
	}

    @Override
	public String getName() {
		// TODO Auto-generated method stub
		return "oipMOEAD-FS-" + getSubPopulationNum();
	}
	@Override
	public String getDescription() {
		// TODO Auto-generated method stub
		return "overlapping island mode parallel MOEAD with multiple reference points";
	}
}
