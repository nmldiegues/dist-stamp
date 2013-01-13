package eu.cloudtm.jstamp.vacation;

import java.util.concurrent.atomic.AtomicLong;

import jstamp.jvstm.CommandCollectAborts;
import jvstm.Transaction;

public class DeleteCustomerOperation extends Operation {

    protected static final AtomicLong aborts = new AtomicLong(0L);

    final private Manager managerPtr;
    final private int customerId;

    public DeleteCustomerOperation(Manager managerPtr, Random randomPtr, int queryRange) {
	this.managerPtr = managerPtr; 
	this.customerId = randomPtr.posrandom_generate() % queryRange + 1;
    }

    @Override
    public void doOperation() {
	CommandCollectAborts cmd = new CommandCollectAborts() {
	    public void runTx() {
		int bill = managerPtr.manager_queryCustomerBill(customerId);
		if (bill >= 0) {
		    managerPtr.manager_deleteCustomer(customerId);
		}
	    }
	};
	Transaction.transactionallyDo(cmd);
	if (cmd.getAborts() > 0) {
	    Vacation.aborts.addAndGet(cmd.getAborts());
	}
    }

}
