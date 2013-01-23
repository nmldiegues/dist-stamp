package eu.cloudtm.microbenchmark;

import java.io.IOException;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.NotSupportedException;
import javax.transaction.RollbackException;
import javax.transaction.SystemException;
import javax.transaction.TransactionManager;

import org.infinispan.Cache;
import org.infinispan.manager.DefaultCacheManager;
import org.infinispan.remoting.transport.Address;
import org.infinispan.remoting.transport.Transport;

public class Micro {

    public Micro() {
    }

    String SET;
    int NODES;
    int LOCAL_THREADS;
    int ITEMS;
    int RANGE;
    int DURATION;
    int WRITE_RATIO;

    public void parseArgs(String argv[]) {
	int opterr = 0;

	for (int i = 1; i < argv.length; i++) {
	    String arg = argv[i];
	    if (arg.equals("-c"))
		NODES = Integer.parseInt(argv[++i]);
	    else if (arg.equals("-l"))
		LOCAL_THREADS = Integer.parseInt(argv[++i]);
	    else if (arg.equals("-i"))
		ITEMS = Integer.parseInt(argv[++i]);
	    else if (arg.equals("-r"))
		RANGE = Integer.parseInt(argv[++i]);
	    else if (arg.equals("-d"))
		DURATION = Integer.parseInt(argv[++i]);
	    else if (arg.equals("-w"))
		WRITE_RATIO = Integer.parseInt(argv[++i]);
	    else if (arg.equals("-s"))
		SET = argv[++i];
	    else
		opterr++;
	}

	if (opterr > 0) {
	    System.err.println("Problem with the parameters");
	}
    }

    public Client[] initializeClients(IntSet set) {
	Client clients[];
	clients = new Client[LOCAL_THREADS];

	for (int i = 0; i < LOCAL_THREADS; i++) {
	    clients[i] = new Client(set, RANGE, WRITE_RATIO);
	}

	return clients;
    }


    public static final AtomicInteger aborts = new AtomicInteger(0);
    public static Cache<String, Object> cache;
    public static TransactionManager txManager; 

    public static void main(String argv[]) throws InterruptedException, IOException, NotSupportedException, SystemException, SecurityException, IllegalStateException, RollbackException, HeuristicMixedException, HeuristicRollbackException {
	DefaultCacheManager defaultCacheManager = new DefaultCacheManager(argv[0]);
	cache = defaultCacheManager.getCache();
	txManager = cache.getAdvancedCache().getTransactionManager();

	IntSet implSet;
	Client clients[];
	long start;
	long stop;

	/* Initialization */
	Micro vac = new Micro();
	vac.parseArgs(argv);

	Transport transport = defaultCacheManager.getTransport();
	while (transport.getMembers().size() < vac.NODES) {}

	Thread.sleep(3000);

	if (transport.isCoordinator()) {
	    txManager.begin();
	    cache.markAsWriteTransaction();
	    cache.put("START_TOKEN", "NO");
	    cache.put("FINISH_TOKEN_" + transport.getAddress(), "NO");
	    txManager.commit();
	    System.out.println("[Coordinator] Setup token to NO");

	    System.out.println("[Coordinator] Starting setup");
	    // should be inevitable
	    txManager.begin();
	    cache.markAsWriteTransaction();
	    
	    Random random = new Random();
	    IntSet set = null;
	    if (vac.SET.equals("ll")) {
		set = new IntSetLinkedList(true);
	    } else if (vac.SET.equals("sl")) {
		set = new IntSetSkipList(true);
	    }
	    for (int i = 0; i < vac.ITEMS; i++)
		set.add(random.nextInt(vac.RANGE));
	    cache.put("SET", set);
	    
	    txManager.commit();
	    System.out.println("[Coordinator] Finished setup");
	} else {
	    txManager.begin();
	    cache.markAsWriteTransaction();
	    cache.put("FINISH_TOKEN_" + transport.getAddress(), "NO");
	    txManager.commit();
	    System.out.println("[Slave] Setup finish token to no: FINISH_TOKEN_" + transport.getAddress());
	}

	Thread.sleep(5000);

	implSet = null;
	System.out.println("[Any] Grabbing set");
	while (implSet == null) {
	    txManager.begin();
	    implSet = (IntSet)cache.get("SET");
	    txManager.commit();
	}
	System.out.println("[Any] Got the set: " + implSet);
	clients = vac.initializeClients(implSet);

	Thread.sleep(2000);

	if (transport.isCoordinator()) {
	    System.out.println("[Coordinator] Setting token to YES");
	    txManager.begin();
	    cache.markAsWriteTransaction();
	    cache.put("START_TOKEN", "YES");
	    txManager.commit();
	    System.out.println("[Coordinator] Token is YES");
	} else {
	    System.out.println("[Slave] Grabbing TOKEN");
	    while (true) {
		txManager.begin();
		String token = (String) cache.get("START_TOKEN");
		txManager.commit();
		if (token != null && token.equals("YES")) {
		    System.out.println("[Slave] Got TOKEN YES");
		    break;
		}
	    }
	}
	
	System.out.println("[Any] Starting local threads");

	start = System.currentTimeMillis();
	for (int i = 0; i < vac.LOCAL_THREADS; i++) {
	    clients[i].start();
	}
	
	try {
	    Thread.sleep(vac.DURATION);
	} catch (InterruptedException e) {
	}
	
	for (int i = 0; i < vac.LOCAL_THREADS; i++) {
	    clients[i].m_phase = Client.SHUTDOWN_PHASE;
	}
	for (int i = 0; i < vac.LOCAL_THREADS; i++) {
	    clients[i].join();
	}
	
	stop = System.currentTimeMillis();

	long diff = stop - start;
	double steps = 0;
	int aborts = 0;
	for (int i = 0; i < clients.length; i++) {
		steps += clients[i].getSteps();
		aborts += clients[i].getAborts();
	}

	double res = ((steps * 1000) / (diff));
	System.out.println(res + " " + aborts);

	
	Address coord = transport.getCoordinator();
	List<Address> members = transport.getMembers();
	if (vac.NODES > 1) {
	    if (!transport.isCoordinator()) {
		try {
		    try {
			txManager.begin();
			cache.markAsWriteTransaction();
			cache.put("FINISH_TOKEN_" + transport.getAddress(), "YES");
			txManager.commit();
			System.out.println("[Slave] Finished and publicized token " + "FINISH_TOKEN_" + transport.getAddress());
		    } catch (Exception e) { /* silently catch aborts/rollbacks */ }
		    
		    while (true) {
			txManager.begin();
			String token = (String) cache.get("FINISH_TOKEN_" + coord);
			txManager.commit();
			if (token != null && token.equals("YES")) {
			    System.out.println("[Slave] Detected finish of Master FINISH_TOKEN_" + coord);
			    break;
			}
		    }
		    
		} catch (Exception e) {
		    e.printStackTrace();
		}
	    } else {
		for (Address addr : members) {
		    if (addr.equals(transport.getAddress())) {
			continue;
		    }
		    while (true) {
			txManager.begin();
			String token = (String) cache.get("FINISH_TOKEN_" + addr);
			txManager.commit();
			if (token != null && token.equals("YES")) {
			    System.out.println("[Coordinator] Detected finish of FINISH_TOKEN_" + addr);
			    break;
			}
		    }
		}
		
		try {
		    txManager.begin();
		    cache.markAsWriteTransaction();
		    cache.put("FINISH_TOKEN_" + transport.getAddress(), "YES");
		    txManager.commit();
		    System.out.println("[Coordinator] Finished and publicized token " + "FINISH_TOKEN_" + transport.getAddress());
		} catch (Exception e) { /* silently catch aborts/rollbacks */ }
	    }
	}
	
	Thread.sleep(5000);
	
	System.exit(0);
    }

}
