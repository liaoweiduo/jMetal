package org.uma.jmetal.algorithm.multiobjective.moead;

import net.sf.javaml.core.Dataset;
import net.sf.javaml.tools.data.FileHandler;
import org.uma.jmetal.algorithm.multiobjective.moead.util.MOEADUtils;
import org.uma.jmetal.operator.CrossoverOperator;
import org.uma.jmetal.operator.MutationOperator;
import org.uma.jmetal.operator.impl.crossover.DifferentialEvolutionCrossover;
import org.uma.jmetal.problem.Problem;
import org.uma.jmetal.problem.multiobjective.FeatureSelection.FeatureSelection;
import org.uma.jmetal.solution.DoubleSolution;
import org.uma.jmetal.util.JMetalException;
import org.uma.jmetal.util.JMetalLogger;

import javax.management.JMException;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;


public class aspMOEADFS extends AbstractMOEAD<DoubleSolution> {

	private DifferentialEvolutionCrossover differentialEvolutionCrossover ;
	private int migrationRatio;

	// for parallelism
	private int subPopulationNum = 2;
	protected int[] populationAssign;
	private int overlappingSize;
	private ExecutorService executorService;
	private volatile Messages message;

	private List<SubProcess> algorithmList = new ArrayList<>();

	public aspMOEADFS(Problem<DoubleSolution> problem,
					  int populationSize,        // total populationSize
					  int resultPopulationSize,
					  int maxIteration,
					  CrossoverOperator<DoubleSolution> crossoverOperator,
					  MutationOperator<DoubleSolution> mutation,
					  double neighborhoodSelectionProbability,
					  int maximumNumberOfReplacedSolutions,
					  int neighborSize,            // T = populationSize / 10 or 4
					  int subPopulationNum,        // core number
					  int overlappingSize,        // normally T/2
					  int migrationRatio        // iterations % migrationRatio == 0
	) {
		super(problem, populationSize, resultPopulationSize, maxIteration, crossoverOperator, mutation,
				FunctionType.TCHE, "", neighborhoodSelectionProbability,
				maximumNumberOfReplacedSolutions, neighborSize);

		differentialEvolutionCrossover = (DifferentialEvolutionCrossover)crossoverOperator ;
		this.subPopulationNum = subPopulationNum;
		this.executorService = Executors.newFixedThreadPool(subPopulationNum);
		this.populationAssign = new int[populationSize];

		this.overlappingSize = overlappingSize;
		this.message = new Messages(subPopulationNum);
		this.migrationRatio = migrationRatio;
	}

	@Override
	public void run() {

		// assign sub processors.
		populationAssignTask();
		for (int subPopulationIndex = 0; subPopulationIndex < subPopulationNum; subPopulationIndex ++){

			int truePopulationSize = 0;
//			for (int individualIndex = 0; individualIndex < populationSize; individualIndex++)
//				if (this.populationAssign[individualIndex] == subPopulationIndex)
//					truePopulationSize ++ ;
//			int totalSubPopulationSize = truePopulationSize;
//			if (subPopulationIndex != 0)
//				totalSubPopulationSize += overlappingSize;
//			if (subPopulationIndex != subPopulationNum - 1)
//				totalSubPopulationSize += overlappingSize;

			int[] refAssign = new int[this.populationSize]; // 0 no relate; 1 true population; 2 overlapping population
			for (int i = 0;i<this.populationSize;i++){
				if (this.populationAssign[i] == subPopulationIndex){
					truePopulationSize ++ ;
					refAssign[i] = 1;
					for (int overlappingIndex = i-1;
						 overlappingIndex >= i-this.overlappingSize && overlappingIndex >= 0;
						 overlappingIndex--){
						if (refAssign[overlappingIndex] == 0){
							refAssign[overlappingIndex] = 2;
						}
					}
					for (int overlappingIndex = i+1;
						 overlappingIndex <= i+this.overlappingSize && overlappingIndex < this.populationSize;
						 overlappingIndex++){
						if (refAssign[overlappingIndex] == 0){
							refAssign[overlappingIndex] = 2;
						}
					}
				}
			}
			// filled true population index list
			int listIndex = 0; int listValue = 0;
			for (int i = 0;i<this.populationSize;i++){
				if (refAssign[i] == 1 || refAssign[i] == 2){
					listValue++;
				}
			}
			int totalSubPopulationSize = listValue;

			SubProcess subProcess = new SubProcess(problem, totalSubPopulationSize, truePopulationSize,
					populationAssign, overlappingSize, truePopulationSize, maxEvaluations, crossoverOperator,
					mutationOperator, functionType, dataDirectory, neighborhoodSelectionProbability,
					maximumNumberOfReplacedSolutions, neighborSize, subPopulationNum, subPopulationIndex);
			algorithmList.add(subProcess);
		}

		Future[] subProcessState = new Future[subPopulationNum];
		for (int subPopulationIndex = 0; subPopulationIndex < subPopulationNum; subPopulationIndex ++){
			SubProcess subProcess = algorithmList.get(subPopulationIndex);
			subProcessState[subPopulationIndex] = executorService.submit(subProcess);
		}
		executorService.shutdown();
//		while(!executorService.isTerminated());

		// check sub process state.
		for (int subProcessIndex = 0; subProcessIndex < subPopulationNum; subProcessIndex ++){
			try {
				subProcessState[subProcessIndex].get();
			} catch (InterruptedException e) {
				JMetalLogger.logger.info
						("sub processor throws an interrupted exception, subProcessId:" + subProcessIndex);
				e.printStackTrace();
			} catch (ExecutionException e) {
				JMetalLogger.logger.info
						("sub processor throws an execution exception, subProcessId:" + subProcessIndex);
				e.printStackTrace();
			}
		}

		// combine the results of all the sub algorithm.

		for (int algorithmindex = 0; algorithmindex < subPopulationNum; algorithmindex++){
			AbstractMOEAD<DoubleSolution> algorithm = algorithmList.get(algorithmindex);
			List<List<DoubleSolution>> recordSolutions = algorithm.getRecordSolutions();

			for (int recordIndex = 0; recordIndex < recordSolutions.size(); recordIndex ++){
				List<DoubleSolution> recordSolution = recordSolutions.get(recordIndex);
                if (algorithmindex == 0) {    // first algorithm
                    this.recordSolutions.add(recordSolution);
                }
                else {
                    List<DoubleSolution> tmp = this.recordSolutions.get(recordIndex);
                    tmp.addAll(recordSolution);
                    this.recordSolutions.set(recordIndex, tmp);
                }
			}
			List<DoubleSolution> subPopulation = algorithm.population;
			this.population.addAll(subPopulation);
            this.populationSize = this.population.size();
			this.resultPopulationSize = this.population.size();
		}

		// print computation time on each processor
		int subPopulationNum = getSubPopulationNum();
		String subPopulationTimeList = "";
		for (int subPopulationIndex = 0; subPopulationIndex < subPopulationNum; subPopulationIndex++){
			oipMOEADFS.SubProcess subAlgorithm = algorithmList.get(subPopulationIndex);
			subPopulationTimeList += subAlgorithm.getComputingTime() + "ms; ";
		}
		JMetalLogger.logger.info("Sub progress execution time: " + subPopulationTimeList);
	}

	public void populationAssignTask(){
		int sum = 0;
		int[] featureNumAssign = new int[subPopulationNum];

		String basePath = "jmetal-problem/src/main/resources/computationCostsForAspMOEADFS/";
		double[] computationTimeCosts = new double[problem.getNumberOfVariables()];
		try {
			BufferedReader br = new BufferedReader(new FileReader(basePath + problem.getName() + ".dat"));
			String record = br.readLine();
			String[] splitedRecord = record.split(",");
			for (int index = 0; index < problem.getNumberOfVariables(); index++) {
				computationTimeCosts[index] = Double.parseDouble(splitedRecord[index]);
			}
		} catch (Exception e){
			JMetalLogger.logger.info("No pre knowledge for island size assignment.\n" +
					"Island size is assigned equally.");
			for (int i = 0 ;i < problem.getNumberOfVariables(); i ++)
				computationTimeCosts[i] = 1.0;
		}
		double sumComputationTime = 0;
		for (int featureNumIndex = 0; featureNumIndex < problem.getNumberOfVariables(); featureNumIndex++){
			sumComputationTime += computationTimeCosts[featureNumIndex];
		}
		double computationTimePerIsland = sumComputationTime / subPopulationNum;
		double currentComputationTime = 0;
		int islandIndex = 0; int islandSize = 0;
		int featureNumIndex;
		for (featureNumIndex = 0; featureNumIndex < problem.getNumberOfVariables(); featureNumIndex++){
			if (islandIndex == subPopulationNum - 1)
				break;
			if (currentComputationTime + computationTimeCosts[featureNumIndex] > computationTimePerIsland){
				featureNumAssign[islandIndex++] = islandSize;
				currentComputationTime = computationTimeCosts[featureNumIndex];
				islandSize = 1;
			} else {
				currentComputationTime += computationTimeCosts[featureNumIndex];
				islandSize ++;
			}
		}
		featureNumAssign[islandIndex] = problem.getNumberOfVariables() - featureNumIndex + 1;
		for (int featureNumAssignIndex = 0; featureNumAssignIndex < subPopulationNum - 1; featureNumAssignIndex++){
			featureNumAssign[featureNumAssignIndex] = featureNumAssign[featureNumAssignIndex]
					* populationSize / problem.getNumberOfVariables() ;
			sum += featureNumAssign[featureNumAssignIndex];
		}
		featureNumAssign[subPopulationNum - 1] = populationSize - sum;

		// manually tune featureNumAssign;
		switch (problem.getName()) {
			case "Australian":
				if (subPopulationNum == 1) {
					featureNumAssign = new int[]{14};
				} else if (subPopulationNum == 2) {
					featureNumAssign = new int[]{7, 7};
				} else if (subPopulationNum == 4) {
					featureNumAssign = new int[]{3, 4, 3, 4};
				} else if (subPopulationNum == 8) {
					featureNumAssign = new int[]{1, 2, 1, 2, 2, 2, 2, 2};
				}
				break;
			case "Vehicle":
				if (subPopulationNum == 1) {
					featureNumAssign = new int[]{18};
				} else if (subPopulationNum == 2) {
					featureNumAssign = new int[]{11, 7};
				} else if (subPopulationNum == 4) {
					featureNumAssign = new int[]{8, 5, 3, 2};
				} else if (subPopulationNum == 8) {
					featureNumAssign = new int[]{3, 3, 2, 2, 2, 2, 2, 2};
				} else if (subPopulationNum == 16) {
					featureNumAssign = new int[]{2, 2, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1};
				}
				break;
			case "Sonar":
				if (subPopulationNum == 1) {
					featureNumAssign = new int[]{60};
				} else if (subPopulationNum == 2) {
					featureNumAssign = new int[]{36, 24};
				} else if (subPopulationNum == 4) {
					featureNumAssign = new int[]{23, 14, 13, 10};
				} else if (subPopulationNum == 8) {
					featureNumAssign = new int[]{9, 13, 8, 6, 5, 5, 7, 7};
				}
				break;
			case "Hillvalley":
				if (subPopulationNum == 1) {
					featureNumAssign = new int[]{100};
				} else if (subPopulationNum == 2) {
					featureNumAssign = new int[]{59, 41};
				} else if (subPopulationNum == 4) {
					featureNumAssign = new int[]{40, 24, 19, 17};
				} else if (subPopulationNum == 8) {
					featureNumAssign = new int[]{28, 16, 12, 11, 10, 9, 8, 6};
				} else if (subPopulationNum == 16) {
					featureNumAssign = new int[]{17, 11, 8, 7, 6, 6, 6, 5, 5, 5, 5, 4, 4, 4, 4, 3};
				}
				break;
			case "Musk1":
				if (subPopulationNum == 1) {
					featureNumAssign = new int[]{166};
				} else if (subPopulationNum == 2) {
					featureNumAssign = new int[]{105, 61};
				} else if (subPopulationNum == 4) {
					featureNumAssign = new int[]{71, 36, 30, 29};
				} else if (subPopulationNum == 8) {
					featureNumAssign = new int[]{40, 22, 20, 19, 18, 17, 15, 15};
				} else if (subPopulationNum == 16) {
					featureNumAssign = new int[]{32, 17, 13, 12, 11, 10, 9, 8, 7, 7, 7, 7, 7, 7, 6, 6};
				}
				break;
			case "Arrhythmia":
				if (subPopulationNum == 1) {
					featureNumAssign = new int[]{200};
				} else if (subPopulationNum == 2) {
					featureNumAssign = new int[]{130, 70};
				} else if (subPopulationNum == 4) {
					featureNumAssign = new int[]{81, 45, 38, 36};
				} else if (subPopulationNum == 8) {
					featureNumAssign = new int[]{51, 29, 23, 22, 20, 19, 18, 18};
				}
				break;
			case "Madelon":
				if (subPopulationNum == 1) {
					featureNumAssign = new int[]{200};
				} else if (subPopulationNum == 2) {
					featureNumAssign = new int[]{120, 80};
				} else if (subPopulationNum == 4) {
					featureNumAssign = new int[]{72, 45, 43, 40};
				} else if (subPopulationNum == 8) {
					featureNumAssign = new int[]{48, 29, 22, 22, 21, 20, 19, 19};
				} else if (subPopulationNum == 16) {
					featureNumAssign = new int[]{47, 20, 16, 13, 12, 10, 10, 10, 9, 9, 8, 8, 8, 7, 7, 6};
				}
				break;
			case "Isolet":
				if (subPopulationNum == 1) {
					featureNumAssign = new int[]{200};
				} else if (subPopulationNum == 2) {
					featureNumAssign = new int[]{141, 59};
				} else if (subPopulationNum == 4) {
					featureNumAssign = new int[]{100, 41, 31, 28};
				} else if (subPopulationNum == 8) {
					featureNumAssign = new int[]{70, 29, 23, 19, 17, 15, 14, 13};
				} else if (subPopulationNum == 16) {
					featureNumAssign = new int[]{48, 21, 16, 13, 11, 10, 10, 10, 9, 9, 8, 8, 7, 7, 7, 6};
				}
				break;
			case "ESR":
				if (subPopulationNum == 1) {
					featureNumAssign = new int[]{178};
				} else if (subPopulationNum == 2) {
					featureNumAssign = new int[]{123, 55};
				} else if (subPopulationNum == 4) {
					featureNumAssign = new int[]{85, 38, 29, 26};
				} else if (subPopulationNum == 8) {
					featureNumAssign = new int[]{59,28,21,17,15,14,12,12};
				}
				break;
			case "MultipleFeatures":
				if (subPopulationNum == 1) {
					featureNumAssign = new int[]{200};
				} else if (subPopulationNum == 2) {
					featureNumAssign = new int[]{139, 61};
				} else if (subPopulationNum == 4) {
					featureNumAssign = new int[]{97, 42, 32, 29};
				} else if (subPopulationNum == 8) {
					featureNumAssign = new int[]{68,31,24,19,17,15,13,13};
				}
				break;
			case "C2K":
				if (subPopulationNum == 1) {
					featureNumAssign = new int[]{96};
				} else if (subPopulationNum == 2) {
					featureNumAssign = new int[]{65, 31};
				} else if (subPopulationNum == 4) {
					featureNumAssign = new int[]{41, 24, 17, 14};
				} else if (subPopulationNum == 8) {
					featureNumAssign = new int[]{28,15,14,11,9,7,6,6};

				}
				break;
			case "MFCC":
				if (subPopulationNum == 1) {
					featureNumAssign = new int[]{22};
				} else if (subPopulationNum == 2) {
					featureNumAssign = new int[]{13, 9};
				} else if (subPopulationNum == 4) {
					featureNumAssign = new int[]{8, 5, 5, 4};
				} else if (subPopulationNum == 8) {
					featureNumAssign = new int[]{5, 4, 3, 2, 2, 2, 2, 2};
				}
				break;
			case "Bankruptcy":
				if (subPopulationNum == 1) {
					featureNumAssign = new int[]{64};
				} else if (subPopulationNum == 2) {
					featureNumAssign = new int[]{43, 21};
				} else if (subPopulationNum == 4) {
					featureNumAssign = new int[]{29, 14, 11, 10};
				} else if (subPopulationNum == 8) {
					featureNumAssign = new int[]{20, 11, 9, 6, 5, 5, 4, 4};
				}
				break;

		}

		sum = featureNumAssign[0]; int assignedIslandIndex = 0;
		for (int individualIndex = 0; individualIndex < populationSize; individualIndex ++){
			if (individualIndex == sum){
				assignedIslandIndex ++;
				sum += featureNumAssign[assignedIslandIndex];
			}
			this.populationAssign[individualIndex] = assignedIslandIndex;
		}
	}

	public class SubProcess extends AbstractMOEAD<DoubleSolution>{
		private int subPopulationId;
		private int overlappingSize;
		private int subPopulationNum;
		private int truePopulationSize;
		private int[] populationAssign;
		private boolean[] changeFlag;
		private int[] truePopulationIndexList;
		private int[] refAssign;

		private double[] refPoints;
		private double weight = 0.01;

		private long computingTime = 0;

		public SubProcess(Problem<DoubleSolution> problem,
						   int populationSize,
						   int truePopulationSize,
						   int[] populationAssign,
						   int overlappingSize,		// normally T/2
						   int resultPopulationSize,
						   int maxIteration,
						   CrossoverOperator<DoubleSolution> crossoverOperator,
						   MutationOperator<DoubleSolution> mutation,
						   FunctionType functionType,
						   String dataDirectory,
						   double neighborhoodSelectionProbability,
						   int maximumNumberOfReplacedSolutions,
						   int neighborSize,			// T = populationSize / 10 or 4
						   int subPopulationNum,		// core number
						   int subPopulationId) {

			super(problem, populationSize, resultPopulationSize, maxIteration, crossoverOperator, mutation,
					functionType, dataDirectory, neighborhoodSelectionProbability, maximumNumberOfReplacedSolutions,
					neighborSize);
			this.subPopulationId = subPopulationId;
			this.overlappingSize = overlappingSize;
			this.subPopulationNum = subPopulationNum;
			this.truePopulationSize = truePopulationSize;
			this.populationAssign = populationAssign;
			this.changeFlag = new boolean[populationSize];
			this.truePopulationIndexList = new int[this.truePopulationSize];
			this.refAssign = new int[populationAssign.length];
		}

		@Override
		public void run() {
		    computingTime = System.currentTimeMillis();
			initializeUniformRef();
			initializePopulation();
			initializeNeighborhood();
			idealPoint.update(population);

			evaluations = truePopulationSize ;
			iterations = 0 ;
			do {
				//	updateNeighborhoodTopology();
				int[] permutation = new int[truePopulationSize];
				MOEADUtils.randomPermutation(
						permutation, truePopulationSize);
				for (int permutationIndex = 0; permutationIndex < truePopulationSize; permutationIndex++){
					permutation[permutationIndex] = this.truePopulationIndexList[permutation[permutationIndex]];
				}

				if((iterations%this.stepIteration)==0){
					List<DoubleSolution> list = new ArrayList<DoubleSolution>();
					for(DoubleSolution sol: population){
						list.add((DoubleSolution) sol.copy());
					}
					recordSolutions.add(list);
//					JMetalLogger.logger.info("Record centre solutions in sub processor: " + subPopulationId +
//							", iterations = " + iterations);
				}

				for (int i = 0; i < truePopulationSize; i++) {
					int subProblemId = permutation[i];

					NeighborType neighborType = chooseNeighborType() ;
					List<DoubleSolution> parents = parentSelection(subProblemId, neighborType) ;

					differentialEvolutionCrossover.setCurrentSolution(population.get(subProblemId));
					List<DoubleSolution> children = crossoverOperator.execute(parents);
					DoubleSolution child = children.get(0) ;
					mutationOperator.execute(child);

					//repairSolutionSequentially(child, this.refPoints[i]);
					((FeatureSelection)this.problem).reduceSize(child, refPoints[subProblemId]);

					problem.evaluate(child);
					evaluations++;

					idealPoint.update(child.getObjectives());
					updateNeighborhood(child, subProblemId, neighborType);
				}

				//check for the same one in the population before evaluating
				//the solution with large nref add random feature to nref
				List<Integer> indexToAdd = checkForDuplicate();
				//System.out.println("Re-initializing "+indexToRandom.size());
				for(int index: indexToAdd){
					DoubleSolution sol = this.population.get(index);
					double refRate = this.refPoints[index];
					((FeatureSelection)this.problem).increaseSize(sol, refRate);
					this.problem.evaluate(sol);
					evaluations++;
				}

				iterations++;
				migration(iterations);
			} while (iterations < maxEvaluations);
			computingTime = System.currentTimeMillis() - computingTime;
		}

		private void migration(int iterations) {
			if (iterations % migrationRatio == 0){
				// put changed(flag true) overlapping individual into corresponding neighborhood area.

				int changeFlagIndex = -1;
				for (int populationIndex = 0; populationIndex < this.populationAssign.length; populationIndex++){
					if (this.refAssign[populationIndex] == 2 || this.refAssign[populationIndex] == 1){
						changeFlagIndex ++ ;
					}
					if (this.refAssign[populationIndex] == 2 && this.changeFlag[changeFlagIndex]){
						message.subPopulation[populationAssign[populationIndex]]
								.offer((DoubleSolution) population.get(changeFlagIndex).copy());
					}

				}
				this.changeFlag = new boolean[populationSize];

				// read individual from own area, update using updateNeighborhood.
				DoubleSolution individual;
				while((individual = message.subPopulation[subPopulationId].poll()) != null){
					updateNeighborhood(individual, 0, NeighborType.POPULATION);
				}
			}
		}

		/**
		 * check for the same one in the population before evaluating
		 * the duplicated one with higher reference points will be random
		 */
		private List<Integer> checkForDuplicate(){
			List<Integer> duplicate = new ArrayList<Integer>();

			//start from one solution
			for(int i=0; i < populationSize-1; i++){
				//if the solution is not duplicated -> perform checking
				if(!duplicate.contains(i)){
					DoubleSolution ms = this.population.get(i);
					//start checking from behind
					for(int j=i+1; j<populationSize;j++){
						if(!duplicate.contains(j)){
							DoubleSolution ss = this.population.get(j);
							//if they have the same objectives
							if(Math.abs(ss.getObjective(0) - ms.getObjective(0)) < 0.00001 &&
									Math.abs(ss.getObjective(1) - ms.getObjective(1)) < 0.00001) {
								//if the refpoint of master is smaller -> the other is duplicated
								if(refPoints[i] <= refPoints[j])
									duplicate.add(j);
									//otherwise the master is duplicated and stop checking for this master
									//add it to the duplicate list
								else{
									duplicate.add(i);
									break;
								}
							}
						}
					}
				}
			}

			return duplicate;
		}

		private void initializeUniformRef() {   // and correct this.populationSize
			int wholePopulationSize = this.populationAssign.length;
			double step = 1.0 / wholePopulationSize;	// total
			double[] wholeRefPoints = new double[wholePopulationSize];
			for (int i = 0;i< wholePopulationSize;i++)
				wholeRefPoints[i] = (i+1) * step;

			// assign reference point, true on truePop;	0 no relate; 1 true population; 2 overlapping population
			for (int i = 0;i<wholePopulationSize;i++){
				if (this.populationAssign[i] == this.subPopulationId){
					refAssign[i] = 1;
					for (int overlappingIndex = i-1;
						 overlappingIndex >= i-this.overlappingSize && overlappingIndex >= 0;
						 overlappingIndex--){
						if (refAssign[overlappingIndex] == 0){
							refAssign[overlappingIndex] = 2;
						}
					}
					for (int overlappingIndex = i+1;
						 overlappingIndex <= i+this.overlappingSize && overlappingIndex < wholePopulationSize;
						 overlappingIndex++){
						if (refAssign[overlappingIndex] == 0){
							refAssign[overlappingIndex] = 2;
						}
					}
				}
			}

			// filled true population index list
			int listIndex = 0; int listValue = 0;
			for (int i = 0;i<wholePopulationSize;i++){
				if (refAssign[i] == 1){
					this.truePopulationIndexList[listIndex++] = listValue;
					listValue++;
				} else if (refAssign[i] == 2){
					listValue++;
				}
			}

			// generate refPoints
			refPoints = new double[this.populationSize];
			int refPointsIndex = 0;
			for (int i = 0;i<wholePopulationSize;i++){
				if (refAssign[i] != 0){
					refPoints[refPointsIndex++] = wholeRefPoints[i];
				}
			}
		}

		private void initializePopulation() {
			for (int i = 0; i < populationSize; i++) {
				DoubleSolution newSolution = (DoubleSolution)problem.createSolution();

				problem.evaluate(newSolution);
				population.add(newSolution);
			}
		}

		@Override
		protected void initializeNeighborhood() {
			double[] x = new double[populationSize];
			int[] idx = new int[populationSize];
			//
			for (int i = 0; i < populationSize; i++) {
				// calculate the distances based on the ref points
				for (int j = 0; j < populationSize; j++) {
					x[j] = Math.abs(refPoints[i]-refPoints[j]);
					idx[j] = j;
				}
				//
				// find 'niche' nearest neighboring subproblems
				MOEADUtils.minFastSort(x, idx, populationSize, neighborSize);
				//
				System.arraycopy(idx, 0, neighborhood[i], 0, Math.min(neighborSize,populationSize));
			}
		}

		@Override
		protected  void updateNeighborhood(DoubleSolution individual,
										   int subProblemId,
										   NeighborType neighborType) throws JMetalException {
			int size;
			int time;

			time = 0;

			if (neighborType == NeighborType.NEIGHBOR) {
				size = neighborhood[subProblemId].length;
			} else {
				size = population.size();
			}
			int[] perm = new int[size];

			MOEADUtils.randomPermutation(perm, size);

			for (int i = 0; i < size; i++) {
				int k;
				if (neighborType == NeighborType.NEIGHBOR) {
					k = neighborhood[subProblemId][perm[i]];
				} else {
					k = perm[i];
				}
				double f1, f2;

				f1 = fitnessFunction(population.get(k),refPoints[k]);
				f2 = fitnessFunction(individual, refPoints[k]);

				if (f2 < f1) {
					population.set(k, (DoubleSolution) individual.copy());
					time++;

					// change flag k to true
					changeFlag[k] = true;
				}

				if (time >= maximumNumberOfReplacedSolutions) {
					return;
				}
			}
		}

		/**
		 * this is designed for FS
		 * @param individual the doubleSolution
		 * @return fitness value
		 * @throws JMetalException not a bi-objective problem
		 */
		private double fitnessFunction(DoubleSolution individual, double refPoint) throws JMetalException{
			double fitness = 0;
			if(problem.getNumberOfObjectives() != 2){
				System.out.println("This is designed for feature selection only!!");
				System.exit(0);
			}
			fitness += individual.getObjective(1);
			fitness += 1000000*Math.max(individual.getObjective(0)-refPoint, 0);
			fitness += weight*individual.getObjective(0);
			return fitness;
		}

		@Override
		public String getName() {
			// TODO Auto-generated method stub
			return "SubProcess:" + subPopulationId;
		}
		@Override
		public String getDescription() {
			// TODO Auto-generated method stub
			return "adaptive island size overlapping island mode parallel MOEAD with multiple reference points";
		}

		public void setWeight(double weight){
			this.weight = weight;
		}

		public long getComputingTime() {
		    return this.computingTime;
        }
	}

    public int getSubPopulationNum() {
        return subPopulationNum;
    }

    public List<SubProcess> getAlgorithmList() {
        return algorithmList;
    }

    @Override
	public String getName() {
		// TODO Auto-generated method stub
		return "aspMOEAD-FS-" + subPopulationNum;
	}
	@Override
	public String getDescription() {
		// TODO Auto-generated method stub
		return "adaptive island size overlapping island mode parallel MOEAD with multiple reference points";
	}

	// for parallelism
	private class Messages {
		Queue<DoubleSolution>[] subPopulation;

		Messages (int subPopulationNum){
			this.subPopulation = new LinkedList[subPopulationNum];
			for (int i = 0;i<subPopulationNum;i++){
				this.subPopulation[i] = new LinkedList<>();
			}
		}
	}
}
