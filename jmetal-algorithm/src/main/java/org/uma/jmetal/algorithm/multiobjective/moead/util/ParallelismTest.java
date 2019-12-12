package org.uma.jmetal.algorithm.multiobjective.moead.util;

import org.uma.jmetal.solution.DoubleSolution;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ParallelismTest {

    public static void main(String[] args) {
        Process pt = new Process();
        pt.run();
    }
}
class Process implements Runnable{
    protected ExecutorService executorService;
    protected volatile Messages message = new Messages();
    protected int it = 10;

    @Override
    public void run() {
        executorService = Executors.newFixedThreadPool(2);
        List<SubProcess> subProcessList = new ArrayList<>();
        message.subPopulation= new ArrayList[2];
        message.subPopulation[0] = new ArrayList<>();
        message.subPopulation[1] = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            SubProcess subProcess = new SubProcess(this, i);
            subProcessList.add(subProcess);
            executorService.submit(subProcess);
        }
        executorService.shutdown();
        while(!executorService.isShutdown());
        System.out.println("is shut down");
        while(!executorService.isTerminated());
        System.out.println("is terminated");
        System.out.println("i test:" + subProcessList.get(1).i);
        System.out.println("message test:" + message.subPopulation[0]);
    }


    // for parallelism
    protected class Messages {
        List<Double>[] subPopulation;
    }

    class SubProcess implements Runnable{

        private int i ;

        SubProcess(Process p,int i) {
            this.i = i;
            message.subPopulation[0].add(i / 1.0);
            System.out.println("it:" + p.it);
        }

        @Override
        public void run() {
            System.out.println("sub process:" + i);
            try {
                Thread.sleep((i==0)?1000:100);
                this.i = i+10;
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
