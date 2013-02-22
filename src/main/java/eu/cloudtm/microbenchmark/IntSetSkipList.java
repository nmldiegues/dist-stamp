package eu.cloudtm.microbenchmark;

import java.io.Serializable;
import java.util.Random;
import java.util.UUID;

public class IntSetSkipList implements IntSet, Serializable {

    public class Node implements Serializable {
	/* final */ private int m_value;
	/* final */ private int level;
	/* final */ private String uuid; 

	public Node(int level, int value) {
	    this.level = level;
	    this.m_value = value;
	    this.uuid = UUID.randomUUID().toString();
	}

	public void setForward(int index, Node node) {
//	    if (node == this || node.getValue() == this.m_value) {
//		System.err.println("here!");
//	    }
	    Micro.cache.put(uuid + ":" + m_value + ":" + index + ":next", node);
	}

	public Node getForward(int index) {
	    return (Node) Micro.cache.get(uuid + ":" + m_value + ":" + index + ":next");
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

    public boolean add(int value) {
	boolean result;

	Node[] update = new Node[m_maxLevel + 1];
	Node node = m_head;

	for (int i = getLevel(); i >= 0; i--) {
	    Node next = node.getForward(i);
	    while (next.getValue() < value) {
//		if (node == next || node.getValue() == next.getValue()) {
//		    System.err.println("bla");
//		}
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
    
    public boolean add(final int value, final Client c) {
	CommandCollectAborts<Boolean> cmd = new CommandCollectAborts<Boolean>() {
	    @Override
	    public Boolean runTx() {
		boolean result;

		Node[] update = new Node[m_maxLevel + 1];
		Node node = m_head;
		int level = getLevel();
		
		for (int i = level; i >= 0; i--) {
		    Node next = node.getForward(i);
//		    System.err.println(Thread.currentThread().getId() + "] out");
		    while (next.getValue() < value) {
//			System.err.println(Thread.currentThread().getId() + "] " + next.getValue() + " < " + value + " " + i);
			node = next;
			next = node.getForward(i);
		    }
//		    System.err.println(Thread.currentThread().getId() + "] after");
		    update[i] = node;
		}
		node = node.getForward(0);

		if (node.getValue() == value) {
		    result = false;
		} else {
		    int newLevel = randomLevel();
		    if (newLevel > level) {
			for (int i = level + 1; i <= level; i++)
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

		int level = getLevel();
		
		for (int i = level; i >= 0; i--) {
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
		    for (int i = 0; i <= level; i++) {
			if (update[i].getForward(i).getValue() == node.getValue())
			    update[i].setForward(i, node.getForward(i));
		    }
		    
		    while (level > 0 && m_head.getForward(level).getForward(0) == null) {
			level--;
			setLevel(level);
		    }			
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
