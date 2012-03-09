package edu.ucla.sspace.svs;


/**
 * @author Keith Stevens
 */
public class RelationTuple {
    public int head;
    public String relation;

    public RelationTuple(int head, String relation) {
        this.head = head;
        this.relation = relation;
    }

    public boolean equals(Object o) {
        if (o == null || !(o instanceof RelationTuple))
            return false;
        RelationTuple r = (RelationTuple) o;
        return this.head == r.head && this.relation == r.relation;
    }

    public int hashCode() {
        return head ^ relation.hashCode();
    }
}
