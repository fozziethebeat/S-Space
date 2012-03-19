package edu.ucla.sspace.text;

import edu.ucla.sspace.dependency.DependencyTreeNode;
import edu.ucla.sspace.dependency.SimpleDependencyTreeNode;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;


/**
 * @author Keith Stevens
 */
public class TokenizedDocument implements Document {

    private final Iterable<String> tokens;

    private final String title;

    private final long timestamp;

    private DependencyTreeNode[] tree;

    public TokenizedDocument(String text) {
        this(Arrays.asList(text.split("\\s+")));
    }

    public TokenizedDocument(Iterable<String> tokens) {
        this(tokens, "", System.currentTimeMillis());
    }

    public TokenizedDocument(Iterable<String> tokens, String title) {
        this(tokens, title, System.currentTimeMillis());
    }

    public TokenizedDocument(Iterable<String> tokens, 
                             String title,
                             long timestamp) {
        this.tokens = tokens;
        this.title = title;
        this.timestamp = timestamp;
    }

    public String title() {
        return title;
    }

    public long timeStamp() {
        return timestamp;
    }

    public Iterator<String> iterator() {
        return tokens.iterator();
    }

    public DependencyTreeNode[] parseTree() {
        if (tree == null) {
            int i = 0;
            List<DependencyTreeNode> treeList = 
                new ArrayList<DependencyTreeNode>();
            for (String token : this)
                treeList.add(new SimpleDependencyTreeNode(token, "", i++));
            tree = treeList.toArray(new DependencyTreeNode[i]);
        }
        return tree;
    }
}
