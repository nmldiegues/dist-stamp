package eu.cloudtm.jstamp.vacation;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class List_t<E> implements Iterable<E>, Serializable{

    protected /* final */ String cacheKey;

    public List_t() { }
    
    public List_t(String cacheKey) {
	this.cacheKey = cacheKey;
	Vacation.cache.put(cacheKey, (Cons<E>) Cons.empty());
    }
    
    private void putElements(Cons<E> elems) {
	Vacation.cache.put(cacheKey, elems);
    }
    
    private Cons<E> getElements() {
	return ((Cons<E>) Vacation.cache.get(cacheKey));
    }

    public void add(E element) {
	putElements(getElements().cons(element));
    }

    public E find(int type, int id) {
	for (E iter : getElements()) {
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
	Cons<E> oldElems = getElements();
	Cons<E> newElems = oldElems.removeFirst(element);

	if (oldElems == newElems) {
	    return false;
	} else {
	    putElements(newElems);
	    return true;
	}
    }

    // Iterable<E> methods

    @Override
    public Iterator<E> iterator() {
	List<E> snapshot = new ArrayList<E>();
	for (E element : getElements())
	    snapshot.add(element);
	Collections.reverse(snapshot);
	return snapshot.iterator();
    }
}
