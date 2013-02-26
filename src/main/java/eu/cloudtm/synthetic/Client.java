package eu.cloudtm.synthetic;

import java.util.Random;
import java.util.UUID;

public class Client extends Thread {
    private int number;
    private int writePerc;
    private int txs;
    private String prefix;
    private int mode;

    public Client(final int number, int writePerc, int txs, int mode) {
	this.number = number;
	this.writePerc = writePerc;
	this.txs = txs;
	this.prefix = UUID.randomUUID().toString();
	this.mode = mode;

	setupDisjoint(number);
    }

    private void setupDisjoint(final int number) {
	CommandCollectAborts<Void> cmd = new CommandCollectAborts<Void>() {
	    @Override
	    public Void runTx() {
		for (int i = 0; i < number; i++) {
		    Synthetic.cache.put(prefix + "--" + i, 0);
		}
		return null;
	    }
	    @Override
	    public boolean isReadOnly() {
		return false;
	    }
	};
	cmd.doIt();
	if (cmd.getAborts() > 0) {
	    Synthetic.aborts.addAndGet(cmd.getAborts());
	}
    }

    // Only called by Coordinator
    public static void setupClashes(final int number) {
	for (int i = 0; i < number; i++) {
	    Synthetic.cache.put("clash" + i, 0);
	}
    }

    @Override
    public void run() {
	if (mode == 1) {
	    runDisjoint();
	} else if (mode == 2) {
	    runWithClashes();
	} else if (mode == 3) {
	    runWithCommonPool();
	} else if (mode == 4) {
	    runFavorableTWM();
	}
    }

    private void runDisjoint() {
	Random random = new Random();
	for (int i = 0; i < txs; i++) {
	    final int r = Math.abs(random.nextInt() % number);
	    final boolean isWrite = (r % 100) < writePerc;

	    CommandCollectAborts<Void> cmd =new CommandCollectAborts<Void>() {
		@Override
		public Void runTx() {
		    Integer val = (Integer) Synthetic.cache.get(prefix + "--" + r);
		    if (isWrite) {
			Synthetic.cache.put(prefix + "--" + r, val + 1);
		    }

		    return null;
		}

		@Override
		public boolean isReadOnly() {
		    return ! isWrite;
		}
	    };
	    cmd.doIt();
	    if (cmd.getAborts() > 0) {
		Synthetic.aborts.addAndGet(cmd.getAborts());
	    }
	}
    }

    // Writes to its own, and writes to all shared
    private void runWithClashes() {
	Random random = new Random();
	for (int i = 0; i < txs; i++) {
	    final int r = Math.abs(random.nextInt() % number);
	    final boolean isWrite = (r % 100) < writePerc;

	    CommandCollectAborts<Void> cmd =new CommandCollectAborts<Void>() {
		@Override
		public Void runTx() {
		    for (int k = 0; k < number; k++) {
			Integer val = (Integer) Synthetic.cache.get(prefix + "--" + k);
			if (isWrite) {
			    Synthetic.cache.put(prefix + "--" + k, val + 1);
			    Synthetic.cache.put("clash" + k, ((Integer) Synthetic.cache.get("clash" + k)) + 1);
			}
		    }
		    return null;
		}

		@Override
		public boolean isReadOnly() {
		    return ! isWrite;
		}
	    };
	    cmd.doIt();
	    if (cmd.getAborts() > 0) {
		Synthetic.aborts.addAndGet(cmd.getAborts());
	    }
	}
    }
    
    // Simulate YCSB with a common pool that is randomly accessed with low probability of clash and only 1 op per tx
    private void runWithCommonPool() {
	Random random = new Random();
	for (int i = 0; i < txs; i++) {
	    final int r = Math.abs(random.nextInt() % number);
	    final boolean isWrite = (r % 100) < writePerc;

	    CommandCollectAborts<Void> cmd =new CommandCollectAborts<Void>() {
		@Override
		public Void runTx() {
		    Synthetic.cache.get("clash" + r);;
		    if (isWrite) {
			Synthetic.cache.put("clash" + r, 42);
		    }
		    return null;
		}

		@Override
		public boolean isReadOnly() {
		    return ! isWrite;
		}
	    };
	    cmd.doIt();
	    if (cmd.getAborts() > 0) {
		Synthetic.aborts.addAndGet(cmd.getAborts());
	    }
	}
    }
    
    // Simulate scenario favorable to TWM
    private void runFavorableTWM() {
	Random random = new Random();
	for (int i = 0; i < txs; i++) {
	    final int r = Math.abs(random.nextInt() % number);
	    final boolean isWrite = (r % 100) < writePerc;
	    final boolean isActuallyWrite = (Math.abs(random.nextInt() % number) % 100) < writePerc;

	    CommandCollectAborts<Void> cmd =new CommandCollectAborts<Void>() {
		@Override
		public Void runTx() {
		    for (int k = 0; k < 25; k++) {
			Synthetic.cache.get("clash" + Math.abs((r + k) % number));
		    }
		    if (isWrite && isActuallyWrite) {
			Synthetic.cache.put("clash" + r, 42);
		    }
		    return null;
		}

		@Override
		public boolean isReadOnly() {
		    return ! isWrite;
		}
	    };
	    cmd.doIt();
	    if (cmd.getAborts() > 0) {
		Synthetic.aborts.addAndGet(cmd.getAborts());
	    }
	}
    }
    
}

