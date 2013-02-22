package eu.cloudtm.jstamp.vacation;

import java.io.Serializable;
import java.util.UUID;


public class RBTree<K extends Comparable<K>, V> implements Serializable {

    /* final */ protected String cacheKey;

    public RBTree() {
	
    }
    
    @SuppressWarnings("unchecked")
    public RBTree(String cacheKey) {
	this.cacheKey = cacheKey;
	Vacation.cache.put(cacheKey, new RedBlackTree<Entry<K, V>>(true));
    }
    
    private RedBlackTree<Entry<K, V>> getIndex() {
	RedBlackTree<Entry<K, V>> v = (RedBlackTree<Entry<K, V>>)Vacation.cache.get(cacheKey);
	if (v == null) {
	    System.out.println("null!");
	}
	return v;
    }
    
    private void putIndex(RedBlackTree<Entry<K, V>> index) {
	Vacation.cache.put(cacheKey, index);
    }

    public void put(K key, V value) {
	if (value == null) {
	    throw new RuntimeException("RBTree does not support null values!");
	}
	putIndex(getIndex().put(new Entry<K, V>(key, value)));
    }

    public V putIfAbsent(K key, V value) {
	if (value == null) {
	    throw new RuntimeException("RBTree does not support null values!");
	}

	Entry<K, V> newEntry = new Entry<K, V>(key, value);
	Entry<K, V> oldVal = getIndex().get(newEntry);
	if (oldVal != null) {
	    return oldVal.value;
	}

	putIndex(getIndex().put(newEntry));
	return null;
    }

    public V get(K key) {
	Entry<K, V> entry = new Entry<K, V>(key, null);
	Entry<K, V> oldVal = getIndex().get(entry);
	if (oldVal != null) {
	    return oldVal.value;
	} else {
	    return null;
	}
    }

    public boolean remove(K key) {
	Entry<K, V> entry = new Entry<K, V>(key, null);
	Entry<K, V> existing = getIndex().get(entry);
	putIndex(getIndex().put(entry));
	return (existing.value != null);
    }

    static class Entry<K extends Comparable<K>, V> implements Comparable<Entry<K, V>>, Serializable {
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

}
