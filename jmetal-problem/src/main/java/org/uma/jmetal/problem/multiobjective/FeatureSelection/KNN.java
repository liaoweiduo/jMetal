package org.uma.jmetal.problem.multiobjective.FeatureSelection;

import java.util.*;
/**
 * To find k nearest neighbors of a new instance
 * Please watch my explanation of how KNN works: xxx
 *   - For classification it uses majority vote
 *   - For regression it finds the mean (average)
 *
 * Copyright (C) 2014
 * @author Dr Noureddin Sadawi
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it as you wish ONLY for legal and ethical purposes
 *
 *  I ask you only, as a professional courtesy, to cite my name, web page
 *  and my YouTube Channel!
 *
 */

public class KNN
{
    // the data
    private double[][] dataTrain;
    private String[] labelTrain;
    private double[][] dataTest;
    private String[] labelTest;
    private int k;


    /**
     * Creates a knn with k value 5
     *
     * @param dataTrain training data
     * @param labelTrain labels of training data
     * @param dataTest test data
     * @param labelTest labels of test data
     */
    public KNN( double[][] dataTrain, String[] labelTrain, double[][] dataTest, String[] labelTest) {
        this(dataTrain, labelTrain, dataTest, labelTest,5);
    }

    /**
     * Creates a knn
     *
     * @param dataTrain training data
     * @param labelTrain labels of training data
     * @param dataTest test data
     * @param labelTest labels of test data
     * @param k value of k
     */
    public KNN( double[][] dataTrain, String[] labelTrain, double[][] dataTest, String[] labelTest, int k) {
        this.dataTrain = dataTrain;
        this.labelTrain = labelTrain;
        this.dataTest = dataTest;
        this.labelTest = labelTest;
        this.k = k;
    }

    public double getErrorRate(){

        //list to save city data
        List<City> cityList = new ArrayList<City>();
        // add city data to cityList
        for (int i = 0; i < labelTrain.length; i++){
            cityList.add(new City(dataTrain[0],labelTrain[0]));
        }
        // start calculating error rate
        int errorNumber = 0;
        for (int i = 0; i < labelTest.length; i++){
            //data about unknown city
            double[] query = dataTest[i];
            //list to save distance result
            List<Result> resultList = new ArrayList<Result>();
            //find disnaces
            for(City city : cityList){
                double dist = 0.0;
                for(int j = 0; j < city.cityAttributes.length; j++){
                    dist += Math.pow(city.cityAttributes[j] - query[j], 2) ;
                    //System.out.print(city.cityAttributes[j]+" ");
                }
                double distance = Math.sqrt( dist );
                resultList.add(new Result(distance,city.cityName));
                //System.out.println(distance);
            }

            //System.out.println(resultList);
            Collections.sort(resultList, new DistanceComparator());
            String[] ss = new String[getK()];
            for(int x = 0; x < getK(); x++){
                System.out.println(resultList.get(x).cityName+ " .... " + resultList.get(x).distance);
                //get classes of k nearest instances (city names) from the list into an array
                ss[x] = resultList.get(x).cityName;
            }
            String majClass = findMajorityClass(ss);
            System.out.println("Class of new instance is: "+majClass);

            if (!majClass.equals(labelTest[i]))
                errorNumber++;
        }
        return (double)errorNumber / labelTest.length;
    }//end main

    /**
     * Returns the majority value in an array of strings
     * majority value is the most frequent value (the mode)
     * handles multiple majority values (ties broken at random)
     *
     * @param  array an array of strings
     * @return  the most frequent string in the array
     */
    private String findMajorityClass(String[] array)
    {
        //add the String array to a HashSet to get unique String values
        Set<String> h = new HashSet<String>(Arrays.asList(array));
        //convert the HashSet back to array
        String[] uniqueValues = h.toArray(new String[0]);
        //counts for unique strings
        int[] counts = new int[uniqueValues.length];
        // loop thru unique strings and count how many times they appear in origianl array
        for (int i = 0; i < uniqueValues.length; i++) {
            for (int j = 0; j < array.length; j++) {
                if(array[j].equals(uniqueValues[i])){
                    counts[i]++;
                }
            }
        }

        for (int i = 0; i < uniqueValues.length; i++)
            System.out.println(uniqueValues[i]);
        for (int i = 0; i < counts.length; i++)
            System.out.println(counts[i]);


        int max = counts[0];
        for (int counter = 1; counter < counts.length; counter++) {
            if (counts[counter] > max) {
                max = counts[counter];
            }
        }
        System.out.println("max # of occurences: "+max);

        // how many times max appears
        //we know that max will appear at least once in counts
        //so the value of freq will be 1 at minimum after this loop
        int freq = 0;
        for (int counter = 0; counter < counts.length; counter++) {
            if (counts[counter] == max) {
                freq++;
            }
        }

        //index of most freq value if we have only one mode
        int index = -1;
        if(freq==1){
            for (int counter = 0; counter < counts.length; counter++) {
                if (counts[counter] == max) {
                    index = counter;
                    break;
                }
            }
            //System.out.println("one majority class, index is: "+index);
            return uniqueValues[index];
        } else{//we have multiple modes
            int[] ix = new int[freq];//array of indices of modes
            System.out.println("multiple majority classes: "+freq+" classes");
            int ixi = 0;
            for (int counter = 0; counter < counts.length; counter++) {
                if (counts[counter] == max) {
                    ix[ixi] = counter;//save index of each max count value
                    ixi++; // increase index of ix array
                }
            }

            for (int counter = 0; counter < ix.length; counter++)
                System.out.println("class index: "+ix[counter]);

            //now choose one at random
            Random generator = new Random();
            //get random number 0 <= rIndex < size of ix
            int rIndex = generator.nextInt(ix.length);
            System.out.println("random index: "+rIndex);
            int nIndex = ix[rIndex];
            //return unique value at that index
            return uniqueValues[nIndex];
        }

    }


    /**
     * Returns the mean (average) of values in an array of doubless
     * sums elements and then divides the sum by num of elements
     *
     * @param  m an array of doubles
     * @return  the mean
     */
    private double meanOfArray(double[] m) {
        double sum = 0.0;
        for (int j = 0; j < m.length; j++){
            sum += m[j];
        }
        return sum/m.length;
    }

    public double[][] getDataTrain() {
        return dataTrain;
    }

    public void setDataTrain(double[][] dataTrain) {
        this.dataTrain = dataTrain;
    }

    public String[] getLabelTrain() {
        return labelTrain;
    }

    public void setLabelTrain(String[] labelTrain) {
        this.labelTrain = labelTrain;
    }

    public double[][] getDataTest() {
        return dataTest;
    }

    public void setDataTest(double[][] dataTest) {
        this.dataTest = dataTest;
    }

    public String[] getLabelTest() {
        return labelTest;
    }

    public void setLabelTest(String[] labelTest) {
        this.labelTest = labelTest;
    }

    public int getK() {
        return k;
    }

    public void setK(int k) {
        this.k = k;
    }

    //simple class to model instances (features + class)
    private class City {
        double[] cityAttributes;
        String cityName;
        public City(double[] cityAttributes, String cityName){
            this.cityName = cityName;
            this.cityAttributes = cityAttributes;
        }
    }
    //simple class to model results (distance + class)
    private class Result {
        double distance;
        String cityName;
        public Result(double distance, String cityName){
            this.cityName = cityName;
            this.distance = distance;
        }
    }
    //simple comparator class used to compare results via distances
    private class DistanceComparator implements Comparator<Result> {
        @Override
        public int compare(Result a, Result b) {
            return a.distance < b.distance ? -1 : a.distance == b.distance ? 0 : 1;
        }
    }

}