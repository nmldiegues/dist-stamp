package eu.cloudtm.synthetic;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.infinispan.distribution.ch.DefaultConsistentHash;
import org.infinispan.remoting.transport.Address;

public class CustomHashing extends DefaultConsistentHash {

    private Address[] addresses;

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
	if (key instanceof MagicKey) {
	    result.add(addresses[((MagicKey)key).node]);
	    return result;
	} else {
	    result.add(addresses[0]);
	    return result;
	}
    }
    
    @Override
    public boolean isKeyLocalToAddress(Address target, Object key, int replCount) {
	if (replCount != 1) {
	    throw new RuntimeException("Not supported replCount: " + replCount);
	}
	
	if (key instanceof MagicKey) {
	    return target.equals(addresses[((MagicKey)key).node]);
	} else {
	    return target.equals(addresses[0]);
	}
    }

    @Override
    public Address primaryLocation(Object key) {
	if (key instanceof MagicKey) {
	    return addresses[((MagicKey)key).node];
	} else {
	    return addresses[0];
	}
    }

    public int getMyId(Address addr) {
	for (int i = 0; i < addresses.length; i++) {
	    if (addresses[i].equals(addr)) {
		return i;
	    }
	}
	throw new RuntimeException("Could not find addr: " + addr);
    }
}
