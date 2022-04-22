package com.company;

import org.w3c.dom.ls.LSOutput;

import java.io.*;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.sql.SQLOutput;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class Main {

    public static void main(String[] args) throws IOException {
        long start, end;
        start = System.currentTimeMillis();
        WordCounter wordCounter = new WordCounter();
        Folder folder = Folder.fromDirectory(new File("D:\\test2"));
        //System.out.println(wordCounter.countOccurrencesOnSingleThread(folder, "void"));
        System.out.println(wordCounter.countOccurrencesInParallel(folder, "класс"));
        end = System.currentTimeMillis();
        System.out.println("Time " + (end - start));
        wordCounter.showAllInf();
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
        for (File entry : dir.listFiles()) {
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
    int[] infArr = new int[28];
    public  AtomicInteger allNumb = new AtomicInteger(0);
    private final ForkJoinPool forkJoinPool = new ForkJoinPool();

    public WordCounter() {
        inArr();
    }


    Long countOccurrencesInParallel(Folder folder, String searchedWord) {
        return forkJoinPool.invoke(new FolderSearchTask(folder, searchedWord));
    }
    String[] wordsIn(String line) {
        return line.trim().split("(\\s|\\p{Punct})+");
    }

    void inArr() {
        Arrays.fill(infArr, 0);
    }
    public void showInFArr() {
        for(int i=0; i<infArr.length; i++){
            System.out.println(i+1 + " - " + infArr[i]);
        }
    }

    public void showAllInf() {
        System.out.println("Words: " + allNumb);
        System.out.println("M = " + calcM());
        System.out.println("D = " + calcDisp());
        System.out.println("Q = " + Math.sqrt(calcDisp()));
        showInFArr();
    }

    public double calcM() {
        double m = 0;
        double sum = 0;
        for(int i=0; i<infArr.length; i++){
            sum += (i+1)*infArr[i];
        }

        m = sum/allNumb.get();
        return m;
    }
    public double calc2M() {
        double m = 0;
        double sum = 0;
        for(int i=0; i<infArr.length; i++){
            sum += Math.pow((i+1), 2)*infArr[i];
        }

        m = sum/allNumb.get();
        return m;
    }

    public double calcDisp() {
        double disp =  calc2M() - Math.pow(calcM(), 2);
        return disp;
     }


    Long occurrencesCount(Document document, String searchedWord) {
        long count = 0;
        for (String line : document.getLines()) {
            for (String word : wordsIn(line)) {

                if(word.length() -1 >= 0) {
                    infArr[word.length() -1]++;
                    allNumb.incrementAndGet();
                }

                if (searchedWord.equals(word)) {
                    count = count + 1;
                }
            }
        }
        return count;
    }

    class DocumentSearchTask extends RecursiveTask<Long> {
        private final Document document;
        private final String searchedWord;

        DocumentSearchTask(Document document, String searchedWord) {
            super();
            this.document = document;
            this.searchedWord = searchedWord;
        }

        @Override
        protected Long compute() {
            return occurrencesCount(document, searchedWord);
        }
    }

    class FolderSearchTask extends RecursiveTask<Long> {
        private final Folder folder;
        private final String searchedWord;

        FolderSearchTask(Folder folder, String searchedWord) {
            super();
            this.folder = folder;
            this.searchedWord = searchedWord;
        }

        @Override
        protected Long compute() {
            long count = 0L;
            List<RecursiveTask<Long>> forks = new LinkedList<>();
            for (Folder subFolder : folder.getSubFolders()) {
                FolderSearchTask task = new FolderSearchTask(subFolder, searchedWord);
                forks.add(task);
                task.fork();
            }
            for (Document document : folder.getDocuments()) {
                DocumentSearchTask task = new DocumentSearchTask(document, searchedWord);
                forks.add(task);
                task.fork();
            }
            for (RecursiveTask<Long> task : forks) {
                count = count + task.join();
            }
            return count;
        }
    }

    Long countOccurrencesOnSingleThread(Folder folder, String searchedWord) {
        long count = 0;
        for (Folder subFolder : folder.getSubFolders()) {
            count = count + countOccurrencesOnSingleThread(subFolder, searchedWord);
        }
        for (Document document : folder.getDocuments()) {
            count = count + occurrencesCount(document, searchedWord);
        }
        return count;
    }
}




