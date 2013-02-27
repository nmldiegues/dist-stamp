package eu.cloudtm.synthetic;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.infinispan.distribution.ch.DefaultConsistentHash;
import org.infinispan.remoting.transport.Address;

public class CustomHashing extends DefaultConsistentHash {

    private Address[] addresses;
    private int lastIndex = 0;

    @Override
    public void setCaches(Set<Address> newCaches) {
	super.setCaches(newCaches);
	addresses = new Address[newCaches.size()];
	int i = 0;
	for (Address addr : newCaches) {
	    addresses[i] = addr;
	    i++;
	}
    }

    @Override
    public List<Address> locate(Object key, int replCount) {
	if (replCount != 1) {
	    throw new RuntimeException("Not supported replCount: " + replCount);
	}
	
	List<Address> result = new ArrayList<Address>(1);
	if (key instanceof Integer) {
	    result.add(addresses[lastIndex]);
	    System.out.println(key + " -> " + lastIndex);
	    lastIndex = (lastIndex + 1) % addresses.length;
	    return result;
	} else {
	    return super.locate(key, replCount);
	}
    }

}
