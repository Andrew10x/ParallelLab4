package Task4;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;

public class Main {

    public static void main(String[] args) throws IOException {
        WordCounter wordCounter = new WordCounter();
        Folder folder = Folder.fromDirectory(new File("D:\\test2"));
        String[] searchedWords = new String[]{"класс", "процесс", "void", "поток", "значениями"};
        List<List<String>> resList = wordCounter.countOccurrencesInParallel(folder, searchedWords);
        for(String w: searchedWords) {
            System.out.print(w + " ");
        }
        System.out.println();
        showLists(resList, searchedWords.length);

    }

    static void showLists(List<List<String>> resList, int l) {
        resList.sort((List<String> a, List<String> b) ->
             Integer.compare(b.size(), a.size())
       );
        for(List<String> list: resList) {
            if(list.size() == 1)
                continue;
            for(String s: list) {
                System.out.print(s + " ");
            }
            System.out.println((list.size()-1) + "/" + l);
        }
    }
}

class Document {
    private final List<String> lines;
    private final String name;

    Document(List<String> lines, String name) {
        this.lines = lines;
        this.name = name;
    }

    List<String> getLines() {
        return this.lines;
    }

    public String getName() {
        return name;
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
        return new Document(lines, file.getName());
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
    private final ForkJoinPool forkJoinPool = new ForkJoinPool();

    List<List<String>> countOccurrencesInParallel(Folder folder, String[] searchedWords) {
        return forkJoinPool.invoke(new FolderSearchTask(folder, searchedWords));
    }
    String[] wordsIn(String line) {
        return line.trim().split("(\\s|\\p{Punct})+");
    }


    List<String> occurrencesCount(Document document, String[] searchedWords) {
        List<String> words = new ArrayList<>();
        words.add(document.getName());
        for (String line : document.getLines()) {
            for (String word : wordsIn(line)) {

                if(word.length() -1 >= 0) {
                    for(String w: searchedWords){
                        if(word.equals(w) && !words.contains(w)) {
                            words.add(w);
                        }
                    }
                }
            }
        }
        return words;
    }

    class DocumentSearchTask extends RecursiveTask<List<String>> {
        private final Document document;
        private final String[] searchedWords;

        DocumentSearchTask(Document document, String[] searchedWords) {
            super();
            this.document = document;
            this.searchedWords = searchedWords;
        }

        @Override
        protected List<String> compute() {
            return occurrencesCount(document, searchedWords);
        }
    }

    class FolderSearchTask extends RecursiveTask<List<List<String>>> {
        private final Folder folder;
        private final String[] searchedWords;

        FolderSearchTask(Folder folder, String[] searchedWords) {
            super();
            this.folder = folder;
            this.searchedWords = searchedWords;
        }

        @Override
        protected List<List<String>> compute() {
            List<List<String>> resList = new ArrayList<>();
            List<FolderSearchTask> folderFork = new LinkedList<>();
            List<DocumentSearchTask> filesForks = new LinkedList<>();
            for (Folder subFolder : folder.getSubFolders()) {
                FolderSearchTask task = new FolderSearchTask(subFolder, searchedWords);
                folderFork.add(task);
                task.fork();
            }
            for (Document document : folder.getDocuments()) {
                DocumentSearchTask task = new DocumentSearchTask(document, searchedWords);
                filesForks.add(task);
                task.fork();
            }

            for(FolderSearchTask ftask: folderFork) {
                resList.addAll(ftask.join());
            }

            for (DocumentSearchTask task : filesForks) {
                resList.add(task.join());
            }
            return resList;
        }
    }
}



