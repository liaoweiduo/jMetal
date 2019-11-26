package org.uma.jmetal.problem.multiobjective.FeatureSelection;

import java.io.*;

public class buildData {

    public static void main(String[] args) {
        int featuresNumber = 18;
        int instanceNumber = 846;
        String dataName = "Vehicle";
        readAndWriteAll(dataName,featuresNumber,instanceNumber);
        devideTrainTest(dataName,featuresNumber,instanceNumber);
    }

    static void readAndWriteAll(String dataName, int featuresNumber, int instanceNumber) {
        double[][] data = new double[instanceNumber][featuresNumber];
        String[] label = new String[instanceNumber];
        int instanceIndex = 0;
        try
        {
            BufferedReader br = new BufferedReader(new FileReader("jmetal-problem/src/main/resources/classificationData/"+ dataName + "/xaa.dat"));
            String record;
            while((record = br.readLine()) != null)
            {
                writeFile(record, "jmetal-problem/src/main/resources/classificationData/"+ dataName +"/"+ dataName +".dat");
//                String[] splitedRecord = record.split("\t");
//                for (int featureIndex = 0;featureIndex < featuresNumber;featureIndex++){
//                    data[instanceIndex][featureIndex] = Double.parseDouble(splitedRecord[featureIndex]);
//                }
//                label[instanceIndex++] = splitedRecord[featuresNumber];
            }

            br = new BufferedReader(new FileReader("jmetal-problem/src/main/resources/classificationData/"+ dataName +"/xab.dat"));
            while ((record = br.readLine())!=null){
                writeFile(record, "jmetal-problem/src/main/resources/classificationData/"+ dataName +"/"+ dataName +".dat");
            }
            br = new BufferedReader(new FileReader("jmetal-problem/src/main/resources/classificationData/"+ dataName +"/xac.dat"));
            while ((record = br.readLine())!=null){
                writeFile(record, "jmetal-problem/src/main/resources/classificationData/"+ dataName +"/"+ dataName +".dat");
            }
            br = new BufferedReader(new FileReader("jmetal-problem/src/main/resources/classificationData/"+ dataName +"/xad.dat"));
            while ((record = br.readLine())!=null){
                writeFile(record, "jmetal-problem/src/main/resources/classificationData/"+ dataName +"/"+ dataName +".dat");
            }
            br = new BufferedReader(new FileReader("jmetal-problem/src/main/resources/classificationData/"+ dataName +"/xae.dat"));
            while ((record = br.readLine())!=null){
                writeFile(record, "jmetal-problem/src/main/resources/classificationData/"+ dataName +"/"+ dataName +".dat");
            }
            br = new BufferedReader(new FileReader("jmetal-problem/src/main/resources/classificationData/"+ dataName +"/xaf.dat"));
            while ((record = br.readLine())!=null){
                writeFile(record, "jmetal-problem/src/main/resources/classificationData/"+ dataName +"/"+ dataName +".dat");
            }
            br = new BufferedReader(new FileReader("jmetal-problem/src/main/resources/classificationData/"+ dataName +"/xag.dat"));
            while ((record = br.readLine())!=null){
                writeFile(record, "jmetal-problem/src/main/resources/classificationData/"+ dataName +"/"+ dataName +".dat");
            }
            br = new BufferedReader(new FileReader("jmetal-problem/src/main/resources/classificationData/"+ dataName +"/xah.dat"));
            while ((record = br.readLine())!=null){
                writeFile(record, "jmetal-problem/src/main/resources/classificationData/"+ dataName +"/"+ dataName +".dat");
            }
            br = new BufferedReader(new FileReader("jmetal-problem/src/main/resources/classificationData/"+ dataName +"/xai.dat"));
            while ((record = br.readLine())!=null){
                writeFile(record, "jmetal-problem/src/main/resources/classificationData/"+ dataName +"/"+ dataName +".dat");
            }

        }
        catch(Exception e)
        {
            System.out.print(e.toString());
        }
    }

    static void writeFile(String content, String file)
    {
        try
        {
//            BufferedWriter bufferedWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file)));
            FileWriter fw = new FileWriter(file, true);   // append: 附加
            PrintWriter out = new PrintWriter(fw);
            out.println(content);
            out.close();
            fw.close();
        }
        catch (IOException e)
        {
            System.out.println("Uh oh, got an IOException error!");
            e.printStackTrace();
        }
    }

    static void devideTrainTest(String dataName, int featuresNumber, int instanceNumber){
        String[][] data = new String[instanceNumber][featuresNumber];
        String[] label = new String[instanceNumber];
        int instanceIndex = 0;
        try {
            BufferedReader br = new BufferedReader(new FileReader("jmetal-problem/src/main/resources/classificationData/" + dataName + "/xaa.dat"));
            String record;
            while ((record = br.readLine()) != null) {
                String[] splitedRecord = record.split("\t");
                for (int featureIndex = 0;featureIndex < featuresNumber;featureIndex++){
                    data[instanceIndex][featureIndex] = splitedRecord[featureIndex];
                }
                label[instanceIndex++] = splitedRecord[featuresNumber];
            }
        } catch (Exception e){
            System.out.print(e.toString());
        }


    }
}