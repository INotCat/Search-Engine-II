import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class TFIDFSearch {
    List<Set<String>> querySetList = new ArrayList<>();
    List<Map<String, double[]>> wordCountMapList = new ArrayList<>();
    List<Pair> pairList = new ArrayList<>();
    public static void main(String[] args) {
        TFIDFSearch ts = new TFIDFSearch();
        SerialzingHandler sh = new SerialzingHandler();
        Operators op = new Operators();
        Indexer idxr = sh.deserialize(args[0]+".ser");
        DataProcessor dp = new DataProcessor();
        int[] topXNumber = new int[1];
        
        
        op.readQuery(args[1], ts.querySetList, topXNumber, ts.wordCountMapList);
         
        List<List<Pair>> pairlistList = new ArrayList<>();
        dp.processQueryList(ts.querySetList, idxr, topXNumber[0], pairlistList, ts.wordCountMapList);
        
        dp.sortPairInList(pairlistList);
        
        op.writeFile(dp.makeResultString(pairlistList, topXNumber[0]));
    }
}

class DataProcessor{

    public void processQueryList(List<Set<String>> querySetList, Indexer idxr, int topXNumber, 
                List<List<Pair>> pairlistList, List<Map<String, double[]>> wordCountMapList){
        int indexForLine = 0;
        Map<String, double[]> currentWordCountMap;
        for(Set<String> set : querySetList){
            List<Pair> eachPairListForEachLine = new ArrayList<>();
            currentWordCountMap = wordCountMapList.get(indexForLine);
            if(classifyQueryType(set)==1){
                andProcess(set ,idxr, topXNumber, eachPairListForEachLine, currentWordCountMap);
            }
            else if(classifyQueryType(set)==2){
                orProcess(set ,idxr, topXNumber, eachPairListForEachLine, currentWordCountMap);
            }
            else{
                singleProcess(set ,idxr, topXNumber, eachPairListForEachLine);
            }     
            pairlistList.add(eachPairListForEachLine);
            indexForLine++;
        }
    }

    public void andProcess(Set<String> querySet ,Indexer idxr, int topXNumber, 
        List<Pair> resultList, Map<String, double[]> currentWordCountMap){
        List<Map<Integer, double[]>> eachWordMap = new ArrayList<>();

        //index on array -> mapping to which word
        Map<Integer, String> wordIndexMap = new HashMap<>();
        int tmpIndex = 0;
        for(String s : querySet){
            if(s.equals("AND")){
                continue;
            }
            if(!idxr.wordTfIdfMap.containsKey(s)){
                return;
            }
            eachWordMap.add(idxr.wordTfIdfMap.get(s));  
            wordIndexMap.put(Integer.valueOf(tmpIndex), s);
            tmpIndex++;

            }

        //the list cannot be empty here since if a word does not exist it has already returned
        Set<Integer> intersectionKeys = new HashSet<>(eachWordMap.get(0).keySet());
        for (int i = 1; i < eachWordMap.size(); i++) {
            intersectionKeys.retainAll(eachWordMap.get(i).keySet());
        }
        
        String word;
        for(Integer keyValue : intersectionKeys){
            double tfIdf = 0;
            for(int i=0; i<eachWordMap.size(); i++){
                //we add the to handle a and a and b then the answe is 2a+b
                word = wordIndexMap.get(Integer.valueOf(i));
                double times = currentWordCountMap.get(word)[0];
                double tmp = eachWordMap.get(i).get(keyValue)[0] * times;

                tfIdf += tmp;
                //tfIdf += eachWordMap.get(i).get(keyValue)[0];

            }
            Pair p = new Pair(keyValue, tfIdf);
            resultList.add(p);
        } 
    }

    public void orProcess(Set<String> querySet, Indexer idxr, int topXNumber, 
    List<Pair> resultList, Map<String, double[]> currentWordCountMap){
        List<Map<Integer, double[]>> eachWordMap = new ArrayList<>();

        //index on array -> mapping to which word
        Map<Integer, String> wordIndexMap = new HashMap<>();
        int tmpIndex = 0;
        for(String s : querySet){
            if(s.equals("OR")){
                continue;
            }
            if(idxr.wordTfIdfMap.containsKey(s)){
                eachWordMap.add(idxr.wordTfIdfMap.get(s));  
                wordIndexMap.put(Integer.valueOf(tmpIndex), s);
                tmpIndex++;
            }
        }
        if (!eachWordMap.isEmpty()){
            Set<Integer> unionKeys = new HashSet<>();
            for(int i=0; i<eachWordMap.size(); i++){
                unionKeys.addAll(eachWordMap.get(i).keySet());
            }

            //After get the union key, we iterate the key through each map
            String word;
            for(Integer keyValue : unionKeys){
                double tfIdf = 0;
                for(int i=0; i<eachWordMap.size(); i++){
                    //But some map does not have that key, so skip
                    if(eachWordMap.get(i).containsKey(keyValue)){

                        //we add the to handle a and a and b then the answe is 2a+b
                        word = wordIndexMap.get(Integer.valueOf(i));
                        double times = currentWordCountMap.get(word)[0];
                        double tmp = eachWordMap.get(i).get(keyValue)[0] * times;

                        tfIdf += tmp;
                    }
                }
                Pair p = new Pair(keyValue, tfIdf);
                resultList.add(p);
            }
        }
        else{
            return;
        }
    }

    public void singleProcess(Set<String> querySet, Indexer idxr, int topXNumber, List<Pair> resultList){
        for(String s : querySet){
            if(idxr.wordTfIdfMap.containsKey(s)){
                Map<Integer, double[]> m = idxr.wordTfIdfMap.get(s);
                for(Map.Entry<Integer, double[]> entry : m.entrySet()){
                    Pair p = new Pair(entry.getKey(),entry.getValue()[0]);
                    resultList.add(p);
                }
            }
            else{
                return;
            } 
        }
    }

    public int classifyQueryType(Set<String> eachSet){
        if(eachSet.contains("AND")){ 
            return 1;
        }
        else if(eachSet.contains("OR")){
            return 2;
        }
        else {
            return 3;
        }
    }

    public void sortPairInList(List<List<Pair>> pairlistList){
        // Step 1: Sort by bookIndex in ascending order 12345
        for(List<Pair> Lp : pairlistList){
            Lp.sort(Comparator.comparingInt(p-> p.bookIndex));
        }

        // Step 2: Sort by tfIdf in descending order 54321
        for(List<Pair> Lp : pairlistList){
            Lp.sort((p1, p2) -> Double.compare(p2.tfIdf, p1.tfIdf));
        }
    }

    public String makeResultString(List<List<Pair>> pairlistList, int topXnumber){
        StringBuilder sb = new StringBuilder();

        try {
            for(List<Pair> Lp : pairlistList){

                //if it requires to output more object then the list has
                int diff = topXnumber - Lp.size();//
                if(diff > 0){
                    for(int i=0; i< diff; i++){
                        Pair p = new Pair(-1);
                        Lp.add(p);
                    }    
                }

                for(int i = 0; i<topXnumber; i++){
                    //count++;
                    sb.append(String.valueOf(Lp.get(i).bookIndex));
                    sb.append(" ");
                }
                sb.append("\n");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        //System.out.println(count);
        return sb.toString();
    }
}

class Pair{
    Integer bookIndex;
    double tfIdf;
    public Pair(Integer bookindex, double tfidf){
        this.bookIndex = bookindex;
        this.tfIdf = tfidf;
    }
    public Pair(Integer bookIndex){
        this.bookIndex = bookIndex;
        this.tfIdf = -1;
    }
}