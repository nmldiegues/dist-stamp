package eu.cloudtm.microbenchmark;

import java.io.Serializable;


public class IntSetLinkedList implements IntSet, Serializable {

    public class Node implements Serializable {
	/* final */ private int m_value;

	public Node(int value, Node next) {
	    m_value = value;
	    setNext(next);
	}

	public Node(int value) {
	    this(value, null);
	}

	public int getValue() {
	    return m_value;
	}

	public void setNext(Node next) {
	    if (next != null) {
		Micro.cache.put(m_value + ":next", next);
	    }
	}

	public Node getNext() {
	    return (Node) Micro.cache.get(m_value + ":next");
	}
    }

    /* final */ private Node m_first;

    public IntSetLinkedList() { }
    
    public IntSetLinkedList(boolean dummy) {
	Node min = new Node(Integer.MIN_VALUE);
	Node max = new Node(Integer.MAX_VALUE);
	min.setNext(max);
	m_first = min;
    }

    public boolean add(int value) {
	boolean result;

	Node previous = m_first;
	Node next = previous.getNext();
	int v;
	while ((v = next.getValue()) < value) {
	    previous = next;
	    next = previous.getNext();
	}
	result = v != value;
	if (result) {
	    previous.setNext(new Node(value, next));
	}

	return result;
    }
    
    public boolean add(final int value, final Client c) {
	CommandCollectAborts<Boolean> cmd = new CommandCollectAborts<Boolean>() {
	    @Override
	    public Boolean runTx() {
		boolean result;

		Node previous = m_first;
		Node next = previous.getNext();
		int v;
		while ((v = next.getValue()) < value) {
		    previous = next;
		    next = previous.getNext();
		}
		result = v != value;
		if (result) {
		    previous.setNext(new Node(value, next));
		}

		return result;
	    }

	};
	boolean r = cmd.doIt();
	c.ab_add += cmd.getAborts(); 
	return r;
    }

    public boolean remove(final int value, final Client c) {
	CommandCollectAborts<Boolean> cmd = new CommandCollectAborts<Boolean>() {
	    @Override
	    public Boolean runTx() {
		boolean result;

		Node previous = m_first;
		Node next = previous.getNext();
		int v;
		while ((v = next.getValue()) < value) {
		    previous = next;
		    next = previous.getNext();
		}
		result = v == value;
		if (result) {
		    previous.setNext(next.getNext());
		}

		return result;
	    }

	};
	boolean r = cmd.doIt();
	c.ab_rm += cmd.getAborts(); 
	return r;
    }

    public boolean contains(final int value, final Client c) {
	CommandCollectAborts<Boolean> cmd = new CommandCollectAborts<Boolean>(true) {
	    @Override
	    public Boolean runTx() {
		boolean result;

		Node previous = m_first;
		Node next = previous.getNext();
		int v;
		while ((v = next.getValue()) < value) {
		    previous = next;
		    next = previous.getNext();
		}
		result = (v == value);

		return result;
	    }

	};
	boolean r = cmd.doIt();
	c.ab_cnt += cmd.getAborts(); 
	return r;
    }
}
