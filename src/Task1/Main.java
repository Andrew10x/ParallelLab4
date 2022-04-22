package Task1;

import java.io.*;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class Main {

    public static void main(String[] args) throws IOException {
        long start, end;
        start = System.currentTimeMillis();
        WordCounter wordCounter = new WordCounter();
        Folder folder = Folder.fromDirectory(new File("D:\\test2"));
        int[] infArr = wordCounter.countOccurrencesInParallel(folder);
        end = System.currentTimeMillis();
        System.out.println("Time " + (end - start));
        wordCounter.showAllInf(infArr);
    }
}

class Document {
    private final List<String> lines;

    Document(List<String> lines) {
        this.lines = lines;
    }

    List<String> getLines() {
        return this.lines;
    }

    static Document fromFile(File file) throws IOException {
        List<String> lines = new LinkedList<>();
        try(BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line = reader.readLine();
            while (line != null) {
                lines.add(line);
                line = reader.readLine();
            }
        }
        return new Document(lines);
    }
}

class Folder {
    private final List<Folder> subFolders;
    private final List<Document> documents;

    Folder(List<Folder> subFolders, List<Document> documents) {
        this.subFolders = subFolders;
        this.documents = documents;
    }

    List<Folder> getSubFolders() {
        return this.subFolders;
    }

    List<Document> getDocuments() {
        return this.documents;
    }

    static Folder fromDirectory(File dir) throws IOException {
        List<Document> documents = new LinkedList<>();
        List<Folder> subFolders = new LinkedList<>();
        for (File entry : Objects.requireNonNull(dir.listFiles())) {
            if (entry.isDirectory()) {
                subFolders.add(Folder.fromDirectory(entry));
            } else {
                documents.add(Document.fromFile(entry));
            }
        }
        return new Folder(subFolders, documents);
    }
}

class WordCounter {
    private int n = 28;
    public  AtomicInteger allNumb = new AtomicInteger(0);
    private final ForkJoinPool forkJoinPool = new ForkJoinPool();


    int[] countOccurrencesInParallel(Folder folder) {
        return forkJoinPool.invoke(new FolderSearchTask(folder));
    }
    String[] wordsIn(String line) {
        return line.trim().split("(\\s|\\p{Punct})+");
    }

    public void showInFArr(int[] infArr) {
        for(int i=0; i<infArr.length; i++){
            System.out.println(i+1 + " - " + infArr[i]);
        }
    }

    public void showAllInf(int[] infArr) {
        System.out.println("Words: " + allNumb);
        System.out.println("M = " + calcM(infArr));
        System.out.println("D = " + calcDisp(infArr));
        System.out.println("Q = " + Math.sqrt(calcDisp(infArr)));
        showInFArr(infArr);
    }

    public double calcM(int[] infArr) {
        double m;
        double sum = 0;
        for(int i=0; i<infArr.length; i++){
            sum += (i+1)*infArr[i];
        }

        m = sum/allNumb.get();
        return m;
    }
    public double calc2M(int[] infArr) {
        double m = 0;
        double sum = 0;
        for(int i=0; i<infArr.length; i++){
            sum += Math.pow((i+1), 2)*infArr[i];
        }

        m = sum/allNumb.get();
        return m;
    }

    public double calcDisp(int[] infArr) {
        double disp =  calc2M(infArr) - Math.pow(calcM(infArr), 2);
        return disp;
    }


    int[] fillInfArr(Document document) {
        int[] infArr = new int[n];
        Arrays.fill(infArr, 0);

        for (String line : document.getLines()) {
            for (String word : wordsIn(line)) {

                if(word.length() -1 >= 0 && word.length() < n+1) {
                    infArr[word.length() -1]++;
                    allNumb.incrementAndGet();
                }
            }
        }
        return infArr;
    }

    class DocumentSearchTask extends RecursiveTask<int[]> {
        private final Document document;


        DocumentSearchTask(Document document) {
            super();
            this.document = document;
        }

        @Override
        protected int[] compute() {
            return fillInfArr(document);
        }
    }

    class FolderSearchTask extends RecursiveTask<int[]> {
        private final Folder folder;

        FolderSearchTask(Folder folder) {
            super();
            this.folder = folder;
        }

        @Override
        protected int[] compute() {
            int[] infArr = new int[n];
            List<RecursiveTask<int[]>> forks = new ArrayList<>();
            for (Folder subFolder : folder.getSubFolders()) {
                FolderSearchTask task = new FolderSearchTask(subFolder);
                forks.add(task);
                task.fork();
            }
            for (Document document : folder.getDocuments()) {
                DocumentSearchTask task = new DocumentSearchTask(document);
                forks.add(task);
                task.fork();
            }


            for(RecursiveTask<int[]> task: forks) {
                int[] resArr = task.join();
                for(int i=0; i<n; i++) {
                    infArr[i] += resArr[i];
                }
            }
            return infArr;
        }
    }

    Long countOccurrencesOnSingleThread(Folder folder, String searchedWord) {
        long count = 0;
        for (Folder subFolder : folder.getSubFolders()) {
            count = count + countOccurrencesOnSingleThread(subFolder, searchedWord);
        }
        for (Document document : folder.getDocuments()) {
            //count = count + occurrencesCount(document, searchedWord);
        }
        return count;
    }
}




