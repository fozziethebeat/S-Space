package edu.ucla.sspace.text;

import java.util.Arrays;
import java.util.Iterator;


/**
 * @author Keith Stevens
 */
public class BaseFilter implements Filter {

    private final Filter base;

    public BaseFilter() {
        this(null);
    }

    public BaseFilter(Filter base) {
        this.base = base;
    }

    public Iterable<String> filter(String tokens) {
        return filter(tokens.split("\\s+"));
    }

    public Iterable<String> filter(String[] tokens) {
        return filter(Arrays.asList(tokens));
    }

    public Iterable<String> filter(Iterable<String> tokens) {
        return new LayeredIterable((base != null)
            ? base.filter(tokens) : tokens);
    }

    protected Iterator<String> getIterator(Iterator<String> iter) {
        return iter;
    }

    public class LayeredIterable implements Iterable<String> {
        private final Iterable<String> iterable;

        public LayeredIterable(Iterable<String> iterable) {
            this.iterable = iterable;
        }
        public Iterator<String> iterator() {
            return getIterator(iterable.iterator());
        }
    }
}
