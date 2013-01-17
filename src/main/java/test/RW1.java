package test;

import java.io.IOException;

import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.NotSupportedException;
import javax.transaction.RollbackException;
import javax.transaction.SystemException;
import javax.transaction.TransactionManager;

import org.infinispan.Cache;
import org.infinispan.manager.DefaultCacheManager;
import org.infinispan.remoting.transport.Transport;

public class RW1 {

    public static Cache<String, Object> cache;
    public static TransactionManager txManager; 

    public static void main(String[] args) throws IOException, InterruptedException, NotSupportedException, SystemException, SecurityException, IllegalStateException, RollbackException, HeuristicMixedException, HeuristicRollbackException {

	DefaultCacheManager defaultCacheManager = new DefaultCacheManager("/home/nmld/workspace/dist-stamp/ispn.xml");
	cache = defaultCacheManager.getCache();
	txManager = cache.getAdvancedCache().getTransactionManager();

	Transport transport = defaultCacheManager.getTransport();
	while (transport.getMembers().size() < 1) {}

	Thread.sleep(300);

	if (transport.isCoordinator()) {
	    txManager.begin();
	    cache.markAsWriteTransaction();
	    cache.put("COUNTER2", new Counter());
	    cache.put("COUNTER1", new Counter());
	    cache.put("COUNTER", new Counter());
	    txManager.commit();
	}

	Thread.sleep(500);

	while (true) {
	    txManager.begin();
	    Counter counter = (Counter) cache.get("COUNTER");
	    txManager.commit();
	    if (counter != null) {
		System.out.println("Slave Starting");
		break;
	    }
	}

	new Worker1().start();
	new Worker2().start();
	
	Thread.sleep(100000);

    }

    public static class Worker extends Thread {

	@Override
	public void run() {
	    while (true) {
		boolean inc = true;
		try {
		    System.out.println(Thread.currentThread().getId() + "] " + doIt(inc));
		    inc = !inc;
		} catch (NotSupportedException e) {
		    e.printStackTrace();
		} catch (SystemException e) {
		    e.printStackTrace();
		} catch (SecurityException e) {
		    e.printStackTrace();
		} catch (IllegalStateException e) {
		    e.printStackTrace();
		} catch (RollbackException e) {
		    e.printStackTrace();
		} catch (HeuristicMixedException e) {
		    e.printStackTrace();
		} catch (HeuristicRollbackException e) {
		    e.printStackTrace();
		}
	    }
	}

	public Integer doIt(final boolean inc) throws NotSupportedException, SystemException, SecurityException, IllegalStateException, RollbackException, HeuristicMixedException, HeuristicRollbackException {
	    return new CommandCollectAborts<Integer>() {
		public Integer runTx() {
		    Counter c = (Counter) cache.get("COUNTER");
		    if (inc) {
			c.incC();
		    } else {
			c.decC();
		    }
		    cache.put("COUNTER", c);
		    return c.getC();
		}
	    }.doIt();
	}

    }

    public static class Worker1 extends Thread {

	@Override
	public void run() {
	    while (true) {
		try {
		    System.out.println(Thread.currentThread().getId() + "] W1 " + doIt());
		} catch (NotSupportedException e) {
		    e.printStackTrace();
		} catch (SystemException e) {
		    e.printStackTrace();
		} catch (SecurityException e) {
		    e.printStackTrace();
		} catch (IllegalStateException e) {
		    e.printStackTrace();
		} catch (RollbackException e) {
		    e.printStackTrace();
		} catch (HeuristicMixedException e) {
		    e.printStackTrace();
		} catch (HeuristicRollbackException e) {
		    e.printStackTrace();
		}
	    }
	}

	public Integer doIt() throws NotSupportedException, SystemException, SecurityException, IllegalStateException, RollbackException, HeuristicMixedException, HeuristicRollbackException {
	    return new CommandCollectAborts<Integer>() {
		public Integer runTx() {
		    Counter c1 = (Counter) cache.get("COUNTER1");
		    Counter c2 = (Counter) cache.get("COUNTER2");
		    c2.incC();
		    cache.put("COUNTER2", c2);
		    return c2.getC();
		}
	    }.doIt();
	}

    }
    
    public static class Worker2 extends Thread {

	@Override
	public void run() {
	    while (true) {
		try {
		    System.out.println(Thread.currentThread().getId() + "] W2 " + doIt());
		} catch (NotSupportedException e) {
		    e.printStackTrace();
		} catch (SystemException e) {
		    e.printStackTrace();
		} catch (SecurityException e) {
		    e.printStackTrace();
		} catch (IllegalStateException e) {
		    e.printStackTrace();
		} catch (RollbackException e) {
		    e.printStackTrace();
		} catch (HeuristicMixedException e) {
		    e.printStackTrace();
		} catch (HeuristicRollbackException e) {
		    e.printStackTrace();
		}
	    }
	}

	public Integer doIt() throws NotSupportedException, SystemException, SecurityException, IllegalStateException, RollbackException, HeuristicMixedException, HeuristicRollbackException {
	    return new CommandCollectAborts<Integer>() {
		public Integer runTx() {
		    Counter c1 = (Counter) cache.get("COUNTER1");
		    c1.incC();
		    cache.put("COUNTER1", c1);
		    return c1.getC();
		}
	    }.doIt();
	}

    }

    public static class Counter {
	private int c = 0;

	public int getC() {
	    return this.c;
	}

	public void setC(int c) {
	    this.c = c;
	}

	public void incC() {
	    this.c = this.c + 1;
	}
	
	public void decC() {
	    this.c = this.c - 1;
	}
    }
}
