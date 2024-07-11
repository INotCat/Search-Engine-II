import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BuildIndex {
    List<String> bookList = new ArrayList<>();
    List<String> wordCountForEachBook = new ArrayList<>();
    public static void main(String[] args) {
        BuildIndex bi = new BuildIndex();
        Trie tr = new Trie();
        SerialzingHandler sh = new SerialzingHandler();
        Operators op = new Operators();
        Indexer idxr = new Indexer();
        tfIdfMapMaker tfidfmapmaker = new tfIdfMapMaker();

        op.readFile(args[0], bi.bookList);
        
        op.makeTrie(tr, bi.bookList, bi.wordCountForEachBook, idxr.wordTfIdfMap);

        tfidfmapmaker.makeTfIdfMap(idxr.wordTfIdfMap, tr, bi.wordCountForEachBook);
        // for(Map.Entry<String, Map<Integer, double[]>> wordEntry : idxr.wordTfIdfMap.entrySet()){
        //      for(Map.Entry<Integer, double[]> entry : idxr.wordTfIdfMap.get("to").entrySet()){
        //          System.out.println("Index: "+entry.getKey()+"||"+"tfidf: "+entry.getValue()[0]);
        //   }
        // }

        sh.serialize(idxr, args[0]);
       
    }
}

class Operators{
    public int bookCount = 0;

    public void makeTrie(Trie root, List<String> bookList, List<String> wordCountForEachBook,
    Map<String,Map<Integer,double[]>> wordMap){
        if(root == null || bookList == null || wordCountForEachBook == null){
            throw new IllegalArgumentException("Arguments cannot be null when making Trie");
        }
        //record which book we are handle
        int bookIndex = 0;
        try {
            for(String book : bookList){
                //Initialize the wordCount for each book
                int wordCount =0;

                for(String word : book.split("\\s+")){
                    root.insert(word, bookIndex);
                    wordCount++;

                    if(!wordMap.containsKey(word)){
                        Map<Integer, double[]> m = new HashMap<>();
                        wordMap.put(word,m);
                    }
                }
                wordCountForEachBook.add(String.valueOf(wordCount));
                bookIndex++;
            }
        } catch (Exception e) {
            System.err.println("Error in making Trie");
            e.printStackTrace();
        } 
    }

    public void writeFile(String content){
        File file = new File("output.txt");

        try{
            if(!file.exists()){
                if(!file.createNewFile()){
                    throw new IOException("Fail to create the file: output.txt");
                }
            }
            
            try(BufferedWriter bw = new BufferedWriter(new FileWriter(file))){
                bw.write(content);
            }            

        } catch(Exception e){
            System.err.println("Error in outputing the file");
            e.printStackTrace();
        }
    }

    public void readFile(String filePath, List<String> bookList){
        try(BufferedReader br = new BufferedReader(new FileReader(filePath))){
            StringBuilder sb = new StringBuilder();
            String line = null;
            int count = 0;
            while((line = br.readLine())!=null){
                parseDocument(line, sb);
                count++;
                if(count % 5 == 0){
                    bookList.add(sb.toString());
                    //reset the value
                    sb.setLength(0);
                    count = 0;
                    this.bookCount++;
                }
            }
            //If there are remaining lines less than 5
            if (sb.length() > 0) {
                bookList.add(sb.toString());
            }
        } catch (Exception e) {
            System.out.println("Error in reading the file");
            e.printStackTrace();
        }
    }

    public void parseDocument(String line, StringBuilder sb){
        line = line.toLowerCase();
        line = line.replaceAll("[^a-z]+", " ");
        //need trim otherwise may exsit many space in the head
        line = line.trim();
        //If we do not add a space here, next line's word would stick to the previous line
        line = line + " ";
        sb.append(line);
    }

    /**
     * 
     * @param filePath : fileName for the task that want to test
     * @param queryList : TFIDSearch would have a list to record all the
     * tasks(each input line is a task), the index 0 is the topXNumber, defining
     * the top X TFIDF should be output, and the actual query is starting from 
     * the second line. 
     * @note Since the query in each line may contain same word, 
     * and we only count once. That's why we use Set to store each word.
     * 
     */
    public void readQuery(String filePath, List<Set<String>>queryList, 
                        int[] topXnumber, List<Map<String, double[]>> wordCountMapList){
                            //wordCountInQuery is used to count each word's time

        try(BufferedReader bf = new BufferedReader(new FileReader(filePath))){
            String line = null;
            
            //Use to check the first line
            boolean isFirstLine = true;
            while((line = bf.readLine())!=null){

                if(isFirstLine){
                    topXnumber[0] = Integer.parseInt(line);
                    isFirstLine = false;
                }
                else{
                    Set<String> querySet = new HashSet<>();
                    Map<String, double[]> wordCountMap = new HashMap<>();
                    parseQuery(line, querySet, wordCountMap);
                    queryList.add(querySet);
                    wordCountMapList.add(wordCountMap);
                }
            }
        } catch(Exception e){
            System.err.println("Error in reading query");
            e.printStackTrace();
        }
    }

    public void parseQuery(String line, Set<String>querySet, Map<String, double[]> wordCountMap){
        for(String s : line.split("\\s+")){
            querySet.add(s);
            if(!wordCountMap.containsKey(s)){
                wordCountMap.put(s, new double[]{1});
            }
            else{
                wordCountMap.get(s)[0]++;
            }
        }        
    }
}

class tfIdfMapMaker{
    public void makeTfIdfMap(Map<String, Map<Integer, double[]>> wordTfIdfMap, 
            Trie root,  List<String> wordCountForEachBook){
        tfidfCaculator tc = new tfidfCaculator();
        for(Map.Entry<String, Map<Integer, double[]>> wordEntry : wordTfIdfMap.entrySet()){
            
            Map<Integer, double[]> map = root.search(wordEntry.getKey());
            for(Map.Entry<Integer, double[]> indexEntry : map.entrySet()){

                double tf = tc.tf(indexEntry.getValue()[0], 
                Double.valueOf(wordCountForEachBook.get(indexEntry.getKey())));

                double idf = tc.idf(wordCountForEachBook.size(), map.size());
                wordTfIdfMap.get(wordEntry.getKey()).put(indexEntry.getKey(), new double[]{tf*idf});
            }
        }
    }
}

class SerialzingHandler{    

    public void serialize(Indexer idx, String fileName){
        Pattern pattern = Pattern.compile("corpus\\d+.txt$");
        Matcher matcher = pattern.matcher(fileName);
        String fileSeq = "";
        // Check if there is any match
        if (matcher.find()) { 
            // Extract the matched number part
             fileSeq = matcher.group(); 
        }
        fileSeq = fileSeq.replace(".txt", "");

        //System.out.println(fileSeq);
        try {
            FileOutputStream fos = new FileOutputStream(fileSeq+".ser");
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            oos.writeObject(idx);

            oos.close();
            fos.close();
        } catch (IOException e) {
            System.err.println("IOException in serializing.");
            e.printStackTrace();
        }
    }

    public Indexer deserialize(String inputFileName){
        try(FileInputStream fis = new FileInputStream(inputFileName);
            ObjectInputStream ois = new ObjectInputStream(fis)){
            
            return (Indexer) ois.readObject();
        } catch (IOException e1) {
            System.err.println("IOEexception in deserializing");
            e1.printStackTrace();
        } catch (ClassNotFoundException e2) {
            System.err.println("ClassNotFoundException");
            e2.printStackTrace();
        }   
        return null;
    }
}

class tfidfCaculator{
    public double tf(double termFrequencyInEachBook , double wordCountForEachBook) {
        try {
            return termFrequencyInEachBook / wordCountForEachBook;  
        } catch (ArithmeticException e) {
            System.out.println("Divided by zero when counting TF");
            e.printStackTrace();
            return 0;
        }
    }

    public double idf(double docSize, double numberDocContainTerm) {
        try {
            return Math.log(docSize / numberDocContainTerm);
        } catch (ArithmeticException e) {
            System.out.println("Divided by zero in IDF");
            e.printStackTrace();
            return 0;
        }
    }
}