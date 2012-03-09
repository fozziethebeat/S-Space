package edu.ucla.sspace.clustering;

import edu.ucla.sspace.matrix.PageRank;


/**
 * @author Keith Stevens
 */
public class PageRankClustering extends HubClustering {

    public PageRankClustering() {
        super(new PageRank());
    }
}
