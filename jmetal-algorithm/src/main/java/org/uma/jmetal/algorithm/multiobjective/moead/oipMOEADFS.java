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

public class oipMOEADFS extends AbstractMOEAD<DoubleSolution> {

	private DifferentialEvolutionCrossover differentialEvolutionCrossover ;
	private int migrationRatio;

	// for parallelism
	private int subPopulationNum = 2;
	private int subPopulationSize;
	private int overlappingSize;
	private ExecutorService executorService;
	private volatile Messages message;

	private List<AbstractMOEAD<DoubleSolution>> algorithmList = new ArrayList<>();

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
				FunctionType.TCHE, "", neighborhoodSelectionProbability,
				maximumNumberOfReplacedSolutions, neighborSize);

		differentialEvolutionCrossover = (DifferentialEvolutionCrossover)crossoverOperator ;
		this.subPopulationNum = subPopulationNum;
		checkNumberOfThreads(population, subPopulationNum);
		this.executorService = Executors.newFixedThreadPool(subPopulationNum);
		this.subPopulationSize = populationSize / subPopulationNum;
		this.overlappingSize = overlappingSize; // neighborSize / 2;
		this.message = new Messages(subPopulationNum);
		this.migrationRatio = migrationRatio;

		for (int subPopulationIndex = 0; subPopulationIndex < subPopulationNum; subPopulationIndex ++){
			int totalSubPopulationSize = subPopulationSize +
					((subPopulationIndex == subPopulationNum - 1 || subPopulationIndex == 0)?1:2) * overlappingSize;
			SubProcess subProcess = new SubProcess(problem, totalSubPopulationSize, subPopulationSize, overlappingSize,
					subPopulationSize, maxEvaluations, crossoverOperator, mutationOperator, functionType, dataDirectory,
					neighborhoodSelectionProbability, maximumNumberOfReplacedSolutions, neighborSize, subPopulationNum,
					subPopulationIndex);
			algorithmList.add(subProcess);
		}
	}

	@Override
	public void run() {
		for (int subPopulationIndex = 0; subPopulationIndex < subPopulationNum; subPopulationIndex ++){
			SubProcess subProcess = (SubProcess) algorithmList.get(subPopulationIndex);
			executorService.submit(subProcess);
		}
		executorService.shutdown();
		while(!executorService.isTerminated());

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
	}

	private class SubProcess extends AbstractMOEAD<DoubleSolution>{
		private int subPopulationId;
		private int overlappingSize;
		private int subPopulationNum;
		private int truePopulationSize;
		private boolean[] changeFlag;

		private double[] refPoints;
		private double weight = 0.01;

		private SubProcess(Problem<DoubleSolution> problem,
						   int populationSize,
						   int truePopulationSize,
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
			this.changeFlag = new boolean[populationSize];
		}

		@Override
		public void run() {
			initializePopulation();
			initializeUniformRef();
			initializeNeighborhood();
			idealPoint.update(population);

			evaluations = truePopulationSize ;
			iterations = 0 ;
			do {
				//	updateNeighborhoodTopology();
				int[] permutation = new int[truePopulationSize];
				int shift = (subPopulationId == 0)?0:overlappingSize;
				MOEADUtils.randomPermutation(
						permutation, truePopulationSize, shift);

				if((iterations%this.stepIteration)==0){
					List<DoubleSolution> list = new ArrayList<DoubleSolution>();
					for(DoubleSolution sol: population){
						list.add((DoubleSolution) sol.copy());
					}
					recordSolutions.add(list);
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
		}

		private void migration(int iterations) {
			if (iterations % migrationRatio == 0){
				// put changed(flag true) overlapping individual into corresponding neighborhood area.
				if (subPopulationId != 0)	// left overlapping
					for (int populationIndex = 0; populationIndex < overlappingSize; populationIndex ++)
						if (changeFlag[populationIndex]) {
							message.subPopulation[subPopulationId - 1].offer((DoubleSolution) population.get(populationIndex).copy());
						}
				if (subPopulationId != subPopulationNum - 1)	// right overlapping
					for (int populationIndex = populationSize - overlappingSize; populationIndex < populationSize; populationIndex ++)
						if (changeFlag[populationIndex])
							message.subPopulation[subPopulationId+1].offer((DoubleSolution) population.get(populationIndex).copy());
				changeFlag = new boolean[populationSize];

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
		public List<Integer> checkForDuplicate(){
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

		private void initializeUniformRef() {
			refPoints = new double[this.populationSize];
			double step = 1.0 / (this.truePopulationSize * this.subPopulationNum);	// total
			double startPoint =  (double) (this.truePopulationSize * this.subPopulationId
					- ((this.subPopulationId == 0)?0:1) * this.overlappingSize)
					/ (this.truePopulationSize * this.subPopulationNum);
			for(int i=0;i<this.populationSize;i++){
				refPoints[i] = startPoint + (i+1) * step;
			}
		}

		protected void initializePopulation() {
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
				System.arraycopy(idx, 0, neighborhood[i], 0, neighborSize);
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
			return "oipMOEAD-FS SubProcess:" + subPopulationId;
		}
		@Override
		public String getDescription() {
			// TODO Auto-generated method stub
			return "overlapping island mode parallel MOEAD with multiple reference points";
		}

		public void setWeight(double weight){
			this.weight = weight;
		}
	}

	@Override
	public String getName() {
		// TODO Auto-generated method stub
		return "oipMOEAD-FS-" + subPopulationNum;
	}
	@Override
	public String getDescription() {
		// TODO Auto-generated method stub
		return "overlapping island mode parallel MOEAD with multiple reference points";
	}

	// for parallelism
	private void checkNumberOfThreads(List<DoubleSolution> population, int numberOfThreadsForSubPopulation) {
		if ((population.size() % numberOfThreadsForSubPopulation) != 0) {
			throw new JMetalException("Wrong number of threads: the remainder if the " +
					"population size (" + population.size() + ") is not divisible by " +
					numberOfThreadsForSubPopulation) ;
		}
	}

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
