package Task3;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class Main {

    public static void main(String[] args) throws IOException {
        WordCounter wordCounter = new WordCounter();
        Folder folder = Folder.fromDirectory(new File("D:\\test1"));
        List<HashSet<String>> myHashSetList = wordCounter.countOccurrencesInParallel(folder);
        System.out.println("Size " + myHashSetList.size());
        HashSet<String> commonWords = findCommonWords(myHashSetList);
        showHashSet(commonWords);
    }

    static HashSet<String> findCommonWords(List<HashSet<String>> myHashSetList) {
        HashSet<String> res = new HashSet<>(myHashSetList.get(0));

        for(int i=1; i<myHashSetList.size(); i++) {
            res.retainAll(myHashSetList.get(i));
        }
        return res;
    }

    static void showHashSet(HashSet<String> commonWords) {
        for(String word: commonWords) {
            System.out.println(word);
        }
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
    int[] infArr = new int[28];
    public  AtomicInteger allNumb = new AtomicInteger(0);
    private final ForkJoinPool forkJoinPool = new ForkJoinPool();

    List<HashSet<String>> countOccurrencesInParallel(Folder folder) {
        return forkJoinPool.invoke(new FolderSearchTask(folder));
    }
    String[] wordsIn(String line) {
        return line.trim().split("(\\s|\\p{Punct})+");
    }

    HashSet<String> occurrencesCount(Document document) {
        HashSet<String> myHashSet = new HashSet<>();
        for (String line : document.getLines()) {
            for (String word : wordsIn(line)) {

                if(word.length() -1 >= 0) {
                    infArr[word.length() -1]++;
                    allNumb.incrementAndGet();
                    myHashSet.add(word);
                }

            }
        }
        return myHashSet;
    }

    class DocumentSearchTask extends RecursiveTask<HashSet<String>> {
        private final Document document;

        DocumentSearchTask(Document document) {
            super();
            this.document = document;
        }

        @Override
        protected HashSet<String> compute() {
            return occurrencesCount(document);
        }
    }

    class FolderSearchTask extends RecursiveTask<List<HashSet<String>>> {
        private final Folder folder;

        FolderSearchTask(Folder folder) {
            super();
            this.folder = folder;
        }

        @Override
        protected List<HashSet<String>> compute() {
            List<FolderSearchTask> folderFork = new LinkedList<>();
            List<DocumentSearchTask> filesForks = new LinkedList<>();
            for (Folder subFolder : folder.getSubFolders()) {
                FolderSearchTask task = new FolderSearchTask(subFolder);
                folderFork.add(task);
                task.fork();
            }
            for (Document document : folder.getDocuments()) {
                DocumentSearchTask task = new DocumentSearchTask(document);
                filesForks.add(task);
                task.fork();
            }
            List<HashSet<String>> myHashSetList = new ArrayList<>();

            for(FolderSearchTask ftask: folderFork) {
                myHashSetList.addAll(ftask.join());
            }

            for (DocumentSearchTask task : filesForks) {
                myHashSetList.add(task.join());
            }
            return myHashSetList;
        }
    }
}



