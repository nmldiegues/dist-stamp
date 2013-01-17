package eu.cloudtm.jstamp.vacation;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class List_t<E> implements Iterable<E>, Serializable{

    @SuppressWarnings("unchecked")
    protected Cons<E> elements = (Cons<E>) Cons.empty();
    protected final String cacheKey;

    public List_t(String cacheKey) {
	this.cacheKey = cacheKey;
    }

    public void add(E element) {
	elements = elements.cons(element);
	Vacation.cache.put(cacheKey, this);
    }

    public E find(int type, int id) {
	for (E iter : elements) {
	    if (iter instanceof Reservation_Info) {
		Reservation_Info resIter = (Reservation_Info) iter;
		if (resIter.type == type && resIter.id == id) {
		    return iter;
		}
	    } else {
		assert (false);
	    }
	}
	return null;
    }

    public boolean remove(E element) {
	Cons<E> oldElems = elements;
	Cons<E> newElems = oldElems.removeFirst(element);

	if (oldElems == newElems) {
	    Vacation.cache.put(cacheKey, this);
	    return false;
	} else {
	    elements = newElems;
	    Vacation.cache.put(cacheKey, this);
	    return true;
	}
    }

    // Iterable<E> methods

    @Override
    public Iterator<E> iterator() {
	List<E> snapshot = new ArrayList<E>();
	for (E element : elements)
	    snapshot.add(element);
	Collections.reverse(snapshot);
	return snapshot.iterator();
    }
}
