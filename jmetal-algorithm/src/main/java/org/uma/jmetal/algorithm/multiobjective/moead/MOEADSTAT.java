package org.uma.jmetal.algorithm.multiobjective.moead;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.uma.jmetal.algorithm.multiobjective.moead.util.MOEADUtils;
import org.uma.jmetal.operator.CrossoverOperator;
import org.uma.jmetal.operator.MutationOperator;
import org.uma.jmetal.operator.impl.crossover.DifferentialEvolutionCrossover;
import org.uma.jmetal.problem.Problem;
import org.uma.jmetal.problem.multiobjective.FeatureSelection.FeatureSelection;
import org.uma.jmetal.solution.DoubleSolution;
import org.uma.jmetal.solution.impl.DefaultDoubleSolution;
import org.uma.jmetal.util.JMetalException;

public class MOEADSTAT extends AbstractMOEAD<DoubleSolution> {

	private DifferentialEvolutionCrossover differentialEvolutionCrossover ;
	private double[] refPoints;
	private double weight = 0.01;

	public MOEADSTAT(Problem<DoubleSolution> problem,
			int populationSize,
			int resultPopulationSize,
			int maxEvaluations,
			CrossoverOperator<DoubleSolution> crossoverOperator,
			MutationOperator<DoubleSolution> mutation,
			FunctionType functionType,
			String dataDirectory,
			double neighborhoodSelectionProbability,
			int maximumNumberOfReplacedSolutions,
			int neighborSize) {
		super(problem, populationSize, resultPopulationSize, maxEvaluations, crossoverOperator, mutation, functionType,
				dataDirectory, neighborhoodSelectionProbability, maximumNumberOfReplacedSolutions, neighborSize);

		differentialEvolutionCrossover = (DifferentialEvolutionCrossover)crossoverOperator ;
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

			for (int i = 0; i < populationSize; i++) {
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

//			updateExternalPopulation();
			iterations++;
		} while (evaluations < maxEvaluations);
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

//	private DoubleSolution randomSolution(double refRate){
//		DoubleSolution newSolution = ((FeatureSelection)this.problem).createSolutionRandom(refRate);
//		this.problem.evaluate(newSolution);
//		return newSolution;
//	}
//
//	private void repairSolutionSequentially(DoubleSolution child, double maxFRate){
//		remove(child,maxFRate);
//		swap(child);
//	}
//
//	private void swap(DoubleSolution child) {
//		FeatureSelection fs = (FeatureSelection)this.problem;
//		List<Integer> selected = new ArrayList<Integer>();
//		List<Integer> unselected = new ArrayList<Integer>();
//		for(int i=0;i<child.getNumberOfVariables();i++){
//			if(child.getVariableValue(i) > fs.getThreshold()){
//				selected.add(i);
//			}
//			else
//				unselected.add(i);
//		}
//
//		//sort selected according to the single performance
//		//worst first
//		int[] idxs = new int[selected.size()];
//		int index=0;
//		for(int i=0;i<fs.orderFeatures.length;i++){
//			int fIndex = fs.orderFeatures[i];
//			if(selected.contains(fIndex)){
//				idxs[index] = fIndex;
//				index++;
//			}
//		}
//
//		//build unselected features, array
//		int[] idxu = new int[unselected.size()];
//		index=0;
//		for(int i=0;i<fs.orderFeatures.length;i++){
//			int fIndex = fs.orderFeatures[i];
//			if(unselected.contains(fIndex)){
//				idxu[index] = fIndex;
//				index++;
//			}
//		}
//
//		//start swapping, start from the worse one
//		for(int i=0;i<idxs.length;i++){
//			//consider the best unselected first
//			int fSwapped = idxs[i];
//			for(int j=idxu.length-1;j>=0;j--){
//				int fSwap = idxu[j];
//				DoubleSolution temp = new DefaultDoubleSolution((DefaultDoubleSolution)child);
//				temp.setVariableValue(fSwapped, 0.0);
//				temp.setVariableValue(fSwap, 1.0);
//				fs.evaluate(temp);
//				if(temp.getObjective(1) < child.getObjective(1)){
//					child.setVariableValue(fSwapped, 0.0);
//					child.setVariableValue(fSwap, 1.0);
//					child.setObjective(1, temp.getObjective(1));
//
//					//now update idxu
//					idxu[j] = fSwapped;
//
//					break;
//				}
//			}
//		}
//	}
//
//	private void remove(DoubleSolution child, double maxFRate) {
//		double currentSize = 0;
//		FeatureSelection fs = (FeatureSelection)this.problem;
//		List<Integer> selected = new ArrayList<Integer>();
//		for(int i=0;i<child.getNumberOfVariables();i++){
//			if(child.getVariableValue(i) > fs.getThreshold()){
//				selected.add(i);
//				currentSize++;
//			}
//		}
//		if(maxFRate < currentSize/child.getNumberOfVariables()){
//
//			//start removing features
//			while(currentSize/fs.getNumberOfVariables() > maxFRate){
//				int bestRemove = -1;
//				double bestError = Double.MAX_VALUE;
//				for (int index =0;index<selected.size();index++){
//					int fToRemove = selected.get(index);
//					DoubleSolution temp = new DefaultDoubleSolution((DefaultDoubleSolution)child);
//					temp.setVariableValue(fToRemove, 0.0);
//					fs.evaluate(temp);
//					if(temp.getObjective(1) < bestError){
//						bestRemove = fToRemove;
//						bestError = temp.getObjective(1);
//					}
//				}
//				if(bestRemove!=-1){
//					child.setVariableValue(bestRemove, 0.0);
//					currentSize--;
//					child.setObjective(0, currentSize/child.getNumberOfVariables());
//					child.setObjective(1, bestError);
//					selected.remove(new Integer(bestRemove));
//				}
//				else{
//					break;
//				}
//			}
//		}
//	}

	private void initializeUniformRef() {
		refPoints = new double[this.populationSize];
		double step = 1.0 / this.populationSize;
		for(int i=0;i<this.populationSize;i++){
			refPoints[i] = (i+1) * step;
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
			}

			if (time >= maximumNumberOfReplacedSolutions) {
				return;
			}
		}
	}

	/**
	 * this is designed for FS
	 * @param individual
	 * @return
	 * @throws JMetalException
	 */
	public double fitnessFunction(DoubleSolution individual, double refPoint) throws JMetalException{
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
		return "MOEAD-STAT";
	}
	@Override
	public String getDescription() {
		// TODO Auto-generated method stub
		return "MOEAD with multiple reference points";
	}

	public void setWeight(double weight){
		this.weight = weight;
	}

}
