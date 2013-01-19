package eu.cloudtm.jstamp.vacation;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.NotSupportedException;
import javax.transaction.RollbackException;
import javax.transaction.SystemException;
import javax.transaction.TransactionManager;

import org.infinispan.Cache;
import org.infinispan.manager.DefaultCacheManager;
import org.infinispan.remoting.transport.Transport;

public class Vacation {

    public Vacation() {
    }

    public static void displayUsage(String appName) {
	System.out.println("Usage: %s [options]\n" + appName);
	System.out.println("\nOptions:                                             (defaults)\n");
	System.out.println("    c <UINT>   Number of [c]lients                   (%i)\n" + Definitions.PARAM_DEFAULT_CLIENTS);
	System.out.println("    n <UINT>   [n]umber of user queries/transaction  (%i)\n" + Definitions.PARAM_DEFAULT_NUMBER);
	System.out.println("    q <UINT>   Percentage of relations [q]ueried     (%i)\n" + Definitions.PARAM_DEFAULT_QUERIES);
	System.out.println("    r <UINT>   Number of possible [r]elations        (%i)\n" + Definitions.PARAM_DEFAULT_RELATIONS);
	System.out.println("    t <UINT>   Number of [t]ransactions              (%i)\n" + Definitions.PARAM_DEFAULT_TRANSACTIONS);
	System.out.println("    u <UINT>   Percentage of [u]ser transactions     (%i)\n" + Definitions.PARAM_DEFAULT_USER);
	System.exit(1);
    }

    int CLIENTS;
    int LOCAL_THREADS;
    int NUMBER;
    int QUERIES;
    int RELATIONS;
    int TRANSACTIONS;
    int USER;

    public void setDefaultParams() {
	CLIENTS = Definitions.PARAM_DEFAULT_CLIENTS;
	NUMBER = Definitions.PARAM_DEFAULT_NUMBER;
	QUERIES = Definitions.PARAM_DEFAULT_QUERIES;
	RELATIONS = Definitions.PARAM_DEFAULT_RELATIONS;
	TRANSACTIONS = Definitions.PARAM_DEFAULT_TRANSACTIONS;
	USER = Definitions.PARAM_DEFAULT_USER;
    }

    public void parseArgs(String argv[]) {
	int opterr = 0;

	setDefaultParams();
	for (int i = 0; i < argv.length; i++) {
	    String arg = argv[i];
	    if (arg.equals("-c"))
		CLIENTS = Integer.parseInt(argv[++i]);
	    else if (arg.equals("-l"))
		LOCAL_THREADS = Integer.parseInt(argv[++i]);
	    else if (arg.equals("-n"))
		NUMBER = Integer.parseInt(argv[++i]);
	    else if (arg.equals("-q"))
		QUERIES = Integer.parseInt(argv[++i]);
	    else if (arg.equals("-r"))
		RELATIONS = Integer.parseInt(argv[++i]);
	    else if (arg.equals("-t"))
		TRANSACTIONS = Integer.parseInt(argv[++i]);
	    else if (arg.equals("-u"))
		USER = Integer.parseInt(argv[++i]);
	    else if (arg.equals("-nest"))
		Operation.nestedParallelismOn = Boolean.parseBoolean(argv[++i]);
	    else if (arg.equals("-sib"))
		Operation.numberParallelSiblings = Integer.parseInt(argv[++i]);
	    else if (arg.equals("-updatePar"))
		Operation.parallelizeUpdateTables = Boolean.parseBoolean(argv[++i]);
	    else
		opterr++;
	}

	if (opterr > 0) {
	    displayUsage(argv[0]);
	}
    }

    public Manager initializeManager() {
	int i;
	int t;
	// System.out.println("Initializing manager... ");

	Random randomPtr = new Random();
	randomPtr.random_alloc();
	Manager managerPtr = new Manager();

	int numRelation = RELATIONS;
	int ids[] = new int[numRelation];
	for (i = 0; i < numRelation; i++) {
	    ids[i] = i + 1;
	}

	for (t = 0; t < 4; t++) {

	    /* Shuffle ids */
	    for (i = 0; i < numRelation; i++) {
		int x = randomPtr.posrandom_generate() % numRelation;
		int y = randomPtr.posrandom_generate() % numRelation;
		int tmp = ids[x];
		ids[x] = ids[y];
		ids[y] = tmp;
	    }

	    /* Populate table */
	    for (i = 0; i < numRelation; i++) {
		boolean status = false;
		int id = ids[i];
		int num = ((randomPtr.posrandom_generate() % 5) + 1) * 100;
		int price = ((randomPtr.posrandom_generate() % 5) * 10) + 50;
		if (t == 0) {
		    status = managerPtr.manager_addCar(id, num, price);
		} else if (t == 1) {
		    status = managerPtr.manager_addFlight(id, num, price);
		} else if (t == 2) {
		    status = managerPtr.manager_addRoom(id, num, price);
		} else if (t == 3) {
		    status = managerPtr.manager_addCustomer(id);
		}
		assert (status);
	    }

	} /* for t */

	// System.out.println("done.");
	return managerPtr;
    }

    /*
     * TODO: RBTrees are put in a single key,value? as they were VBox<RBTREE>?
     * Or use key,value for the actual purpose that RBTrees exist? I am afraid 
     * that changes dramatically the patterns of access though. I will start with 
     * the first and assess the overhead which may be huge because of the large 
     * collections going back and forth in JGroups
     */
    public Client[] initializeClients(Manager managerPtr) {
	Random randomPtr;
	Client clients[];
	int i;
	int numThreads = LOCAL_THREADS;
	int numTransaction = TRANSACTIONS;
	int numTransactionPerClient;
	int numQueryPerTransaction = NUMBER;
	int numRelation = RELATIONS;
	int percentQuery = QUERIES;
	int queryRange;
	int percentUser = USER;

	// System.out.println("Initializing clients... ");

	randomPtr = new Random();
	randomPtr.random_alloc();

	clients = new Client[numThreads];

	numTransactionPerClient = (int) ((double) numTransaction / (double) numThreads + 0.5);
	queryRange = (int) (percentQuery / 100.0 * numRelation + 0.5);

	for (i = 0; i < numThreads; i++) {
	    clients[i] = new Client(i, managerPtr, numTransactionPerClient, numQueryPerTransaction, queryRange, percentUser);
	}

	// System.out.println("done.");
	// System.out.println("    Transactions        = " + numTransaction);
	// System.out.println("    Clients             = " + numClient);
	// System.out.println("    Transactions/client = " +
	// numTransactionPerClient);
	// System.out.println("    Queries/transaction = " +
	// numQueryPerTransaction);
	// System.out.println("    Relations           = " + numRelation);
	// System.out.println("    Query percent       = " + percentQuery);
	// System.out.println("    Query range         = " + queryRange);
	// System.out.println("    Percent user        = " + percentUser);

	return clients;
    }

    void checkTables(Manager managerPtr) {
	int i;
	int numRelation = RELATIONS;
	RBTree<Integer, Customer> customerTablePtr = managerPtr.customerTable;
	RBTree<Integer, Reservation> tables[] = new RBTree[3];
	tables[0] = managerPtr.carTable;
	tables[1] = managerPtr.flightTable;
	tables[2] = managerPtr.roomTable;
	int numTable = 3;

	int t;

	// System.out.println("Checking tables... ");

	/* Check for unique customer IDs */
	int percentQuery = QUERIES;
	int queryRange = (int) (percentQuery / 100.0 * numRelation + 0.5);
	int maxCustomerId = queryRange + 1;
	for (i = 1; i <= maxCustomerId; i++) {
	    if (customerTablePtr.get(i) != null) {
		if (customerTablePtr.remove(i)) {
		    assert (customerTablePtr.get(i) == null);
		}
	    }
	}

	/* Check reservation tables for consistency and unique ids */
	for (t = 0; t < numTable; t++) {
	    RBTree<Integer, Reservation> tablePtr = tables[t];
	    for (i = 1; i <= numRelation; i++) {
		if (tablePtr.get(i) != null) {
		    boolean status = false;
		    if (t == 0) {
			status = managerPtr.manager_addCar(i, 0, 0);
		    } else if (t == 1) {
			status = managerPtr.manager_addFlight(i, 0, 0);
		    } else if (t == 2) {
			status = managerPtr.manager_addRoom(i, 0, 0);
		    }
		    assert (status);
		    if (tablePtr.remove(i)) {
			assert (!tablePtr.remove(i));
		    }
		}
	    }
	}

	// System.out.println("done.");
    }

    public static final AtomicInteger aborts = new AtomicInteger(0);
    public static Cache<String, Object> cache;
    public static TransactionManager txManager; 

    public static void main(String argv[]) throws InterruptedException, IOException, NotSupportedException, SystemException, SecurityException, IllegalStateException, RollbackException, HeuristicMixedException, HeuristicRollbackException {
	DefaultCacheManager defaultCacheManager = new DefaultCacheManager("/home/nmld/workspace/dist-stamp/ispn.xml");
	cache = defaultCacheManager.getCache();
	txManager = cache.getAdvancedCache().getTransactionManager();

	Manager manager = null;
	Client clients[];
	long start;
	long stop;

	/* Initialization */
	Vacation vac = new Vacation();
	vac.parseArgs(argv);

	Transport transport = defaultCacheManager.getTransport();
	while (transport.getMembers().size() < vac.CLIENTS) {}

	Thread.sleep(3000);

	if (transport.isCoordinator()) {
	    txManager.begin();
	    cache.markAsWriteTransaction();
	    cache.put("START_TOKEN", "NO");
	    txManager.commit();
	    System.out.println("Setup token NO");

	    // should be inevitable
	    txManager.begin();
	    cache.markAsWriteTransaction();
	    manager = vac.initializeManager();
	    cache.put("MANAGER", manager);
	    txManager.commit();
	}

	Thread.sleep(5000);

	txManager.begin();
	manager = (Manager)cache.get("MANAGER");
	clients = vac.initializeClients(manager);
	txManager.commit();

	Thread.sleep(1000);

	if (transport.isCoordinator()) {
	    txManager.begin();
	    cache.markAsWriteTransaction();
	    cache.put("START_TOKEN", "YES");
	    txManager.commit();
	    System.out.println("Setup token YES");
	} else {
	    while (true) {
		txManager.begin();
		String token = (String) cache.get("START_TOKEN");
		txManager.commit();
		if (token != null && token.equals("YES")) {
		    System.out.println("Slave Starting");
		    break;
		}
	    }
	}

	start = System.currentTimeMillis();
	for (int i = 1; i < vac.LOCAL_THREADS; i++) {
	    clients[i].start();
	}
	clients[0].run();
	for (int i = 1; i < vac.LOCAL_THREADS; i++) {
	    clients[i].join();
	}


	if (vac.CLIENTS > 1) {
	    if (!transport.isCoordinator()) {
		try {
		    txManager.begin();
		    cache.markAsWriteTransaction();
		    cache.put("START_TOKEN", "NO");
		    txManager.commit();
		    System.out.println("Finish token");
		} catch (Exception e) {}
	    } else {
		while (true) {
		    txManager.begin();
		    String token = (String) cache.get("START_TOKEN");
		    txManager.commit();
		    if (token != null && token.equals("NO")) {
			System.out.println("Master detected finish");
			break;
		    }
		}
	    }
	}

	stop = System.currentTimeMillis();

	long diff = stop - start;
	System.out.println(diff + " " + aborts.get());

	txManager.begin();
	cache.markAsWriteTransaction();
	vac.checkTables(manager);
	txManager.commit();
	System.out.println("Tables are consistent!");
	
	System.exit(0);
    }

}
