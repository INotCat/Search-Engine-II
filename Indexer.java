import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class Indexer implements Serializable {
    Map<String, Map<Integer, double[]>> wordTfIdfMap = new HashMap<>();
}

class TrieNode {
    TrieNode[] children = new TrieNode[26];
    boolean isEndOfWord = false;
    double totalFrequencyOfEachWord = 0;
    public Map<Integer, double[]> bookInfoMap = new HashMap<>();
}

class Trie{
    public TrieNode root = new TrieNode();
    public double sumOfWordInTree = 0;

    public void insert(String word, int bookIndex){
        Integer index = Integer.valueOf(bookIndex);
        TrieNode node = root;
        //save the total frequency of words in Trie
        this.sumOfWordInTree++;
        
        //iterate to find the endnode of the word and record the info on this node
        for (char c : word.toCharArray()) {
            if (node.children[c - 'a'] == null) {
            node.children[c - 'a'] = new TrieNode();
            }
            node = node.children[c - 'a'];
            }
            node.isEndOfWord = true;
            //A map is in the last character of a word to store 
            //Key: bookIndex -> keyValue: frequency 
            if(node.bookInfoMap.containsKey(index)){
                node.bookInfoMap.get(index)[0]++;
            }
            else{
                node.bookInfoMap.put(index, new double[]{1});
            }
            node.totalFrequencyOfEachWord++;
    }

    public Map<Integer, double[]> search(String word) {
        TrieNode node = root;
        for (char c : word.toCharArray()) {
            node = node.children[c - 'a'];
            if (node == null) {
                return null;
            }
        }
        return node.bookInfoMap;
    }
}