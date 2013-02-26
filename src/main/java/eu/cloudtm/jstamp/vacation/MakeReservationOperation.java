package eu.cloudtm.jstamp.vacation;


public class MakeReservationOperation extends Operation {

    final private Manager manager;
    final private int[] types;
    final private int[] ids;
    final private int[] maxPrices;
    final private int[] maxIds;
    final private int customerId;
    final private int numQuery;
    final private boolean readOnly;

    public MakeReservationOperation(Manager manager, Random random, int numQueryPerTx, int queryRange) {
	this.manager = manager;
	this.types = new int[numQueryPerTx];
	this.ids = new int[numQueryPerTx];

	this.maxPrices = new int[Definitions.NUM_RESERVATION_TYPE];
	this.maxIds = new int[Definitions.NUM_RESERVATION_TYPE];
	this.maxPrices[0] = -1;
	this.maxPrices[1] = -1;
	this.maxPrices[2] = -1;
	this.maxIds[0] = -1;
	this.maxIds[1] = -1;
	this.maxIds[2] = -1;
	int n;
	this.numQuery = numQueryPerTx;
	this.customerId = random.posrandom_generate() % queryRange + 1;
	
	int[] baseIds = new int[20];
	for (int i = 0; i < 20; i++) {
	    baseIds[i] = (random.random_generate() % queryRange) + 1;
	}
	
	for (n = 0; n < numQuery; n++) {
	    types[n] = random.random_generate() % Definitions.NUM_RESERVATION_TYPE;
	    ids[n] = baseIds[n % 20];
	}
	
	this.readOnly = (random.random_generate() % 100) <= queryRange;
    }

    @Override
    public void doOperation() {
	CommandCollectAborts<Void> cmd = new CommandCollectAborts<Void>() {
	    public Void runTx() {
		makeReservationNotNested();
		return null;
	    }

	    @Override
	    public boolean isReadOnly() {
		return MakeReservationOperation.this.readOnly;
	    }
	};
	cmd.doIt();
	if (cmd.getAborts() > 0) {
	    Vacation.aborts.addAndGet(cmd.getAborts());
	}
    }

    private void makeReservationNotNested() {
	boolean isFound = false;
	int n;
	for (n = 0; n < numQuery; n++) {
	    int t = types[n];
	    int id = ids[n];
	    int price = -1;
	    if (t == Definitions.RESERVATION_CAR) {
		if (manager.manager_queryCar(id) >= 0) {
		    price = manager.manager_queryCarPrice(id);
		}
	    } else if (t == Definitions.RESERVATION_FLIGHT) {
		if (manager.manager_queryFlight(id) >= 0) {
		    price = manager.manager_queryFlightPrice(id);
		}
	    } else if (t == Definitions.RESERVATION_ROOM) {
		if (manager.manager_queryRoom(id) >= 0) {
		    price = manager.manager_queryRoomPrice(id);
		}
	    } else {
		assert (false);
	    }
	    if (price > maxPrices[t]) {
		maxPrices[t] = price;
		maxIds[t] = id;
		isFound = true;
	    }
	}

	if (!readOnly) {
	    if (isFound) {
		manager.manager_addCustomer(customerId);
		manager.manager_doCustomer();
	    } 
	    if (maxIds[Definitions.RESERVATION_CAR] > 0) {
		manager.manager_reserveCar(customerId, maxIds[Definitions.RESERVATION_CAR]);
	    }
	    if (maxIds[Definitions.RESERVATION_FLIGHT] > 0) {
		manager.manager_reserveFlight(customerId, maxIds[Definitions.RESERVATION_FLIGHT]);
	    }
	    if (maxIds[Definitions.RESERVATION_ROOM] > 0) {
		manager.manager_reserveRoom(customerId, maxIds[Definitions.RESERVATION_ROOM]);
	    }
	}
    }

}
