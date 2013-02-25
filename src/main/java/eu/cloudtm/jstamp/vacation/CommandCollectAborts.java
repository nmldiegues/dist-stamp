package eu.cloudtm.jstamp.vacation;

import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.RollbackException;

import org.infinispan.CacheException;

public abstract class CommandCollectAborts<T> {

    private int aborts = 0;

    public int getAborts() {
	return this.aborts;
    }

    public abstract T runTx();

    public T doIt() {
	T result = null;
	boolean txFinished = false;
	aborts--;
	while (!txFinished) {
	    try {
		aborts++;
		Vacation.txManager.begin();
		Vacation.cache.markAsWriteTransaction();

		result = runTx();

		Vacation.txManager.commit();
//System.err.println(Thread.currentThread().getId() + "] committed");
		txFinished = true;
		return result;
	    } catch (CacheException ce) {
		//If the execution fails
	    } catch(RollbackException re) {
		//If the transaction was marked for rollback only, the transaction is rolled back and this exception is thrown.
	    } catch(HeuristicMixedException hme) {
		//If a heuristic decision was made and some some parts of the transaction have been committed while other parts have been rolled back.
		//Pedro -- most of the time, happens when some nodes fails...
	    } catch(HeuristicRollbackException hre) {
		//If a heuristic decision to roll back the transaction was made
	    } catch (Exception e) { // any other exception 	 out
		e.printStackTrace();
		throw new RuntimeException(e);
	    } finally {
		if (!txFinished) {
		    try {
			Vacation.txManager.rollback();
		    } catch(IllegalStateException ise) {
			// If the transaction is in a state where it cannot be rolled back.
			// Pedro -- happen when the commit fails. When commit fails, it invokes the rollback().
			//          so rollback() will be invoked again, but the transaction no longer exists
			// Pedro -- just ignore it
		    } catch (Exception ex) {
			throw new RuntimeException(ex);
		    }
		}
	    }
	    // Pedro had this wait here.  Why?
	    // waitingBeforeRetry();
//	    System.err.println(Thread.currentThread().getId() + "] \trestart");
	}
	// never reached
	throw new RuntimeException("code never reached");
    }


}
