package eu.cloudtm.microbenchmark;

import java.io.Serializable;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.locks.LockSupport;

import eu.cloudtm.microbenchmark.IntSetLinkedList.Node;

public class IntSetSkipList implements IntSet, Serializable {

    public class Node implements Serializable {
	/* final */ private int m_value;
	/* final */ private int level;

	public Node(int level, int value) {
	    this.level = level;
	    this.m_value = value;
	}

	public void setForward(int index, Node node) {
	    Micro.cache.put(m_value + ":" + index + ":next", node);
	}

	public Node getForward(int index) {
	    return (Node) Micro.cache.get(m_value + ":" + index + ":next");
	}

	public int getValue() {
	    return m_value;
	}

	public int getLevel() {
	    return level;
	}

    }

    // Probability to increase level
    /* final */ private double m_probability = 0.25;

    // Upper bound on the number of levels
    /* final */ private int m_maxLevel = 32;

    // Highest level so far: level in cache

    // First element of the list
    /* final */ private Node m_head;
    // Thread-private PRNG
    /* final */ private static ThreadLocal<Random> s_random = new ThreadLocal<Random>() {
	protected synchronized Random initialValue() {
	    return new Random();
	}
    };

    private void setLevel(int level) {
	Micro.cache.put("skipList:level", level);
    }

    private Integer getLevel() {
	return (Integer) Micro.cache.get("skipList:level");
    }

    public IntSetSkipList() { }
    
    public IntSetSkipList(boolean dummy) {
	setLevel(0);

	m_head = new Node(m_maxLevel, Integer.MIN_VALUE);
	Node tail = new Node(m_maxLevel, Integer.MAX_VALUE);
	for (int i = 0; i <= m_maxLevel; i++)
	    m_head.setForward(i, tail);
    }

    protected int randomLevel() {
	int l = 0;
	while (l < m_maxLevel && s_random.get().nextDouble() < m_probability)
	    l++;
	return l;
    }

    public boolean add(final int value, final Client c) {
	CommandCollectAborts<Boolean> cmd = new CommandCollectAborts<Boolean>() {
	    @Override
	    public Boolean runTx() {
		boolean result;

		Node[] update = new Node[m_maxLevel + 1];
		Node node = m_head;

		for (int i = getLevel(); i >= 0; i--) {
		    Node next = node.getForward(i);
		    while (next.getValue() < value) {
			node = next;
			next = node.getForward(i);
		    }
		    update[i] = node;
		}
		node = node.getForward(0);

		if (node.getValue() == value) {
		    result = false;
		} else {
		    int level = randomLevel();
		    if (level > getLevel()) {
			for (int i = getLevel() + 1; i <= level; i++)
			    update[i] = m_head;
			setLevel(level);
		    }
		    node = new Node(level, value);
		    for (int i = 0; i <= level; i++) {
			node.setForward(i, update[i].getForward(i));
			update[i].setForward(i, node);
		    }
		    result = true;
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

		Node[] update = new Node[m_maxLevel + 1];
		Node node = m_head;

		for (int i = getLevel(); i >= 0; i--) {
		    Node next = node.getForward(i);
		    while (next.getValue() < value) {
			node = next;
			next = node.getForward(i);
		    }
		    update[i] = node;
		}
		node = node.getForward(0);

		if (node.getValue() != value) {
		    result = false;
		} else {
		    int auxLimit = getLevel();
		    for (int i = 0; i <= auxLimit; i++) {
			if (update[i].getForward(i) == node)
			    update[i].setForward(i, node.getForward(i));
		    }
		    while (getLevel() > 0 && m_head.getForward(getLevel()).getForward(0) == null)
			setLevel(getLevel() - 1);
		    result = true;
		}

		return result;
	    }

	};
	boolean r = cmd.doIt();
	c.ab_rm += cmd.getAborts(); 
	return r;
    }

    public boolean contains(final int value, final Client c) {
	CommandCollectAborts<Boolean> cmd = new CommandCollectAborts<Boolean>() {
	    @Override
	    public Boolean runTx() {
		boolean result;

		Node node = m_head;
		int initialM_Level = getLevel();

		for (int i = initialM_Level; i >= 0; i--) {
		    Node next = node.getForward(i);
		    while (next.getValue() < value) {
			node = next;
			next = node.getForward(i);
		    }
		}
		node = node.getForward(0);

		result = (node.getValue() == value);

		return result;
	    }

	};
	boolean r = cmd.doIt();
	c.ab_cnt += cmd.getAborts(); 
	return r;
    }

}
