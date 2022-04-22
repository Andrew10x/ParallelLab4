package Task2;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Scanner;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveAction;

public class Main {
    static int[][] destArr;
    static double time1 = 0;
    static double time2 = 0;

    public static void main(String[] args) throws IOException {
        multMatr();
    }

    public static void multMatr() throws IOException {
        Path path = Paths.get("D:\\genMatrix\\matrix1000.txt");
        Scanner scanner = new Scanner(path);
        int length = scanner.nextInt();
        int[][] arr1 = new int[length][length], arr2 = new int[length][length];
        destArr = new int[length][length];
        int numberOfThreads = 5;

        initMatrix(scanner, arr1, arr2, numberOfThreads);
        algorithm(arr1, arr2, destArr, numberOfThreads);
        System.out.println(time1);
        System.out.println(time2);
        System.out.println("Speedup: " + (time1/time2));
    }


    public static void initMatrix(Scanner scanner, int[][] arr1, int[][] arr2, int numberOfThreads) throws IOException {


        for(int i=0; i<arr1.length; i++) {
            for(int j=0; j<arr1.length; j++) {
                int el = scanner.nextInt();
                arr1[i][j] = el;
                arr2[i][j] = el;
            }
        }
        scanner.close();
    }

    public static void algorithm(int[][] matrix1, int[][] matrix2, int[][] result, int numThreads) {

        long start, end;
        start = System.currentTimeMillis();
        int rowsPerThread = ParallelMatrixWorker.applyRowsPerThread(numThreads,result.length);
        Thread[] parallelStringMatrixThreads=new Thread[numThreads];
        for (int threadNumber = 0; threadNumber < numThreads; threadNumber++) {
            int startIndex = threadNumber * rowsPerThread;
            parallelStringMatrixThreads[threadNumber] = new Thread(new ParallelMatrixWorker(startIndex, matrix1, matrix2, result));
            parallelStringMatrixThreads[threadNumber].start();
        }
        waitMatrixThreads(parallelStringMatrixThreads);

        end = System.currentTimeMillis();
        time1 = end - start;


        start = System.currentTimeMillis();
        ForkJoinPool pool = new ForkJoinPool();
        MatrixMultTask matrixMultTask = new MatrixMultTask(0,  matrix1, matrix2, result, matrix1.length);
        pool.invoke(matrixMultTask);
        pool.shutdown();
        end = System.currentTimeMillis();
        time2 = end - start;

    }
    private static void waitMatrixThreads(Thread[] threads){
        for (Thread thread:threads) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }


}

class MatrixMultTask extends RecursiveAction {
    protected int rowsPerThread;
    protected  int threadIndex;
    protected  int[][] matrix1;
    protected int[][] matrix2;
    protected int[][] resultMatrix;

    public MatrixMultTask(int threadIndex, int[][] matrix1, int[][] matrix2, int[][] resultMatrix, int rowsPerThread) {
        this.threadIndex = threadIndex;
        this.matrix1 = matrix1;
        this.matrix2 = matrix2;
        this.resultMatrix = resultMatrix;
        this.rowsPerThread = rowsPerThread;
    }

    protected void computeDirectly() {
        for (int a = threadIndex; a < threadIndex + rowsPerThread && a < resultMatrix.length; a++) {
            for (int i = 0; i < resultMatrix.length; i++) {
                for (int j = 0; j < resultMatrix[0].length; j++) {
                    resultMatrix[a][i] += matrix1[a][j] * matrix2[j][i];
                }
            }
        }
    }

    @Override
    protected void compute() {
        if(rowsPerThread <= 20)
            computeDirectly();
        else {
            int m2;
            if(rowsPerThread % 2 == 1) {
                rowsPerThread /= 2;
                m2 = rowsPerThread + 1;
            }
            else {
                rowsPerThread /= 2;
                m2 = rowsPerThread;
            }
            invokeAll(new MatrixMultTask(threadIndex,  matrix1, matrix2, resultMatrix, rowsPerThread),
                    new MatrixMultTask(threadIndex + rowsPerThread,  matrix1, matrix2, resultMatrix, m2));
        }
    }
}

class ParallelMatrixWorker extends ParallelMatrixMultyplication implements Runnable {

    public ParallelMatrixWorker(int threadIndex, int[][] matrix1, int[][] matrix2, int[][] resultMatrix) {
        this.threadIndex = threadIndex;
        this.matrix1 = matrix1;
        this.matrix2 = matrix2;
        this.resultMatrix = resultMatrix;
    }

    @Override
    public void run() {
        multiply();
    }

    @Override
    protected void multiply() {
        for (int a = threadIndex; a < threadIndex + rowsPerThread && a < resultMatrix.length; a++) {
            for (int i = 0; i < resultMatrix.length; i++) {
                for (int j = 0; j < resultMatrix[0].length; j++) {
                    resultMatrix[a][i] += matrix1[a][j] * matrix2[j][i];
                }
            }
        }
    }
}

abstract class ParallelMatrixMultyplication {
    protected static int rowsPerThread;
    protected  int threadIndex;
    protected  int[][] matrix1;
    protected int[][] matrix2;
    protected int[][] resultMatrix;

    public static int applyRowsPerThread(int numThreads,int resultLength) {
        rowsPerThread = resultLength > numThreads ? (int) Math.round((double) resultLength / numThreads) : 1;
        while (rowsPerThread*numThreads<resultLength){
            rowsPerThread++;
        }
        return rowsPerThread;
    }

    protected abstract void multiply();
}
