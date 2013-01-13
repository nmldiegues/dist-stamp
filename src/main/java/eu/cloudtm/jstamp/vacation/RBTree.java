package eu.cloudtm.jstamp.vacation;

import java.util.Iterator;
import java.util.NoSuchElementException;

import jvstm.VBox;
import jvstm.util.RedBlackTree;

public class RBTree<K extends Comparable<K>, V> {

    protected final VBox<RedBlackTree<Entry<K, V>>> index;

    @SuppressWarnings("unchecked")
    public RBTree() {
	index = new VBox<RedBlackTree<Entry<K, V>>>(RedBlackTree.EMPTY);
    }

    public void put(K key, V value) {
	if (value == null) {
	    throw new RuntimeException("RBTree does not support null values!");
	}
	index.put(index.get().put(new Entry<K, V>(key, value)));
    }

    public V putIfAbsent(K key, V value) {
	if (value == null) {
	    throw new RuntimeException("RBTree does not support null values!");
	}

	Entry<K, V> newEntry = new Entry<K, V>(key, value);
	Entry<K, V> oldVal = index.get().get(newEntry);
	if (oldVal != null) {
	    return oldVal.value;
	}

	index.put(index.get().put(newEntry));
	return null;
    }

    public V get(K key) {
	Entry<K, V> entry = new Entry<K, V>(key, null);
	Entry<K, V> oldVal = index.get().get(entry);
	if (oldVal != null) {
	    return oldVal.value;
	} else {
	    return null;
	}
    }

    public Iterable<V> getRange(K minKey, K maxKey) {
	final Entry<K, V> entryMin = new Entry<K, V>(minKey, null);
	final Entry<K, V> entryMax = new Entry<K, V>(maxKey, null);

	return new Iterable<V>() {
	    @Override
	    public Iterator<V> iterator() {
		return new IndexIterator(index.get().iterator(entryMin, entryMax));
	    }
	};
    }

    public boolean remove(K key) {
	Entry<K, V> entry = new Entry<K, V>(key, null);
	Entry<K, V> existing = index.get().get(entry);
	index.put(index.get().put(entry));
	return (existing.value != null);
    }

    public Iterator<V> iterator() {
	return new IndexIterator(index.get().iterator());
    }

    public Iterable<K> getKeys() {
	return new Iterable<K>() {
	    @Override
	    public Iterator<K> iterator() {
		return new KeyIterator(index.get().iterator());
	    }
	};
    }

    class KeyIterator implements Iterator<K> {
	private final Iterator<Entry<K, V>> iter;
	private Entry<K, V> next;

	KeyIterator(Iterator<Entry<K, V>> iter) {
	    this.iter = iter;
	    updateNext();
	}

	private void updateNext() {
	    while (iter.hasNext()) {
		Entry<K, V> nextEntry = iter.next();
		if (nextEntry.value != null) {
		    next = nextEntry;
		    return;
		}
	    }
	    next = null;
	}

	@Override
	public boolean hasNext() {
	    return next != null;
	}

	@Override
	public K next() {
	    if (next == null) {
		throw new NoSuchElementException();
	    } else {
		K key = next.key;
		updateNext();
		return key;
	    }
	}

	@Override
	public void remove() {
	    throw new Error("Cannot remove keys");
	}
    }

    class IndexIterator implements Iterator<V> {
	private final Iterator<Entry<K, V>> iter;
	private Entry<K, V> next;

	IndexIterator(Iterator<Entry<K, V>> iter) {
	    this.iter = iter;
	    updateNext();
	}

	private void updateNext() {
	    while (iter.hasNext()) {
		Entry<K, V> nextEntry = iter.next();
		if (nextEntry.value != null) {
		    next = nextEntry;
		    return;
		}
	    }
	    next = null;
	}

	@Override
	public boolean hasNext() {
	    return next != null;
	}

	@Override
	public V next() {
	    if (next == null) {
		throw new NoSuchElementException();
	    } else {
		V result = next.value;
		updateNext();
		return result;
	    }
	}

	@Override
	public void remove() {
	    throw new Error("Cannot remove indexes");
	}
    }

    static class Entry<K extends Comparable<K>, V> implements Comparable<Entry<K, V>> {
	private final K key;
	private final V value;

	Entry(K key, V value) {
	    this.key = key;
	    this.value = value;
	}

	@Override
	public int compareTo(Entry<K, V> other) {
	    return this.key.compareTo(other.key);
	}
    }

    @Override
    public Object clone() {
	throw new Error(this.getClass().getCanonicalName() + ".clone() not implemented");
    }

}
