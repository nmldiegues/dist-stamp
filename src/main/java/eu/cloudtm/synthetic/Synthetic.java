package eu.cloudtm.synthetic;

import java.io.IOException;
import java.util.List;
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

public class Synthetic {

    public Synthetic() {
    }

    int CLIENTS;
    int NUMBER;
    int TRANSACTIONS;
    int USER;
    int MODE;

    public void parseArgs(String argv[]) {

	for (int i = 1; i < argv.length; i++) {
	    String arg = argv[i];
	    if (arg.equals("-c"))
		CLIENTS = Integer.parseInt(argv[++i]);
	    else if (arg.equals("-n"))
		NUMBER = Integer.parseInt(argv[++i]);
	    else if (arg.equals("-t"))
		TRANSACTIONS = Integer.parseInt(argv[++i]);
	    else if (arg.equals("-u"))
		USER = Integer.parseInt(argv[++i]);
	    else if (arg.equals("-m"))
		MODE = Integer.parseInt(argv[++i]);
	}

    }

    public static final AtomicInteger aborts = new AtomicInteger(0);
    public static Cache<Object, Object> cache;
    public static TransactionManager txManager; 

    public static void main(String argv[]) throws InterruptedException, IOException, NotSupportedException, SystemException, SecurityException, IllegalStateException, RollbackException, HeuristicMixedException, HeuristicRollbackException {
	DefaultCacheManager defaultCacheManager = new DefaultCacheManager(argv[0]);
	cache = defaultCacheManager.getCache();
	txManager = cache.getAdvancedCache().getTransactionManager();

	Client client;
	long start;
	long stop;

	/* Initialization */
	Synthetic vac = new Synthetic();
	vac.parseArgs(argv);

	Transport transport = defaultCacheManager.getTransport();
	while (transport.getMembers().size() < vac.CLIENTS) {}

	Thread.sleep(3000);

	Address myAddr = transport.getAddress();
	int i = 0;
	int myIndex = -1;
	for (Address addr : transport.getMembers()) {
	    if (myAddr.equals(addr)) {
		myIndex = i;
	    }
	    i++;
	}
	
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
	    
	    Client.setupClashes(vac.CLIENTS, vac.NUMBER);
	    cache.put("MANAGER", "Manager");
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

	String manager = null;
	System.out.println("[Any] Grabbing manager");
	while (manager == null) {
	    txManager.begin();
	    manager = (String)cache.get("MANAGER");
	    txManager.commit();
	}
	System.out.println("[Any] Got the manager: " + manager);
	client = new Client(myIndex, vac.CLIENTS, vac.NUMBER, vac.USER, vac.TRANSACTIONS, vac.MODE);

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
	client.run();
	stop = System.currentTimeMillis();

	long diff = stop - start;
	System.out.println(diff + " " + aborts.get());


	Address coord = transport.getCoordinator();
	List<Address> members = transport.getMembers();
	if (vac.CLIENTS > 1) {
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

		System.out.println("[Coordinator] Checking tables");
		txManager.begin();
		cache.markAsWriteTransaction();
		txManager.commit();
		System.out.println("Tables are consistent!");

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
