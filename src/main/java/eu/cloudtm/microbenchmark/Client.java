package eu.cloudtm.microbenchmark;

import java.io.Serializable;
import java.util.Random;



public class Client extends Thread implements Serializable {

    public static final int TEST_PHASE = 2;
    public static final int SHUTDOWN_PHASE = 3;

    final private IntSet m_set;
    final private int m_range;
    final private int m_rate;
    boolean m_write;
    int m_last;
    final private Random m_random;

    int ab_add = 0;
    int ab_rm = 0;
    int ab_cnt = 0;

    volatile protected int m_phase = TEST_PHASE;
    private int m_steps;

    public int getSteps() {
	return m_steps;
    }

    public int getAborts() {
	return ab_add + ab_rm + ab_cnt;
    }

    public Client(IntSet set, int m_range, int m_rate) {
	this.m_set = set;
	this.m_range = m_range;
	this.m_rate = m_rate;
	this.m_write = true;
	m_random = new Random();
    }

    public void run() {
	while (m_phase == TEST_PHASE) {
	    step(TEST_PHASE);
	    m_steps++;
	}
    }

    protected void step(int phase) {
	int i = m_random.nextInt(100);
	if (i < m_rate) {
	    if (m_write) {
		m_last = m_random.nextInt(m_range);
		if (m_set.add(m_last, this))
		    m_write = false;
	    } else {
		m_set.remove(m_last, this);
		if (phase == TEST_PHASE)
		m_write = true;
	    }
	} else {
	    m_set.contains(m_random.nextInt(m_range), this);
	}
    }

}