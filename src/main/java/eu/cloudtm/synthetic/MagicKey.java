package eu.cloudtm.synthetic;

import java.io.Serializable;

public class MagicKey implements Serializable {

    private static final long serialVersionUID = -1072474466685642719L;
    protected static int CLIENTS;
    protected static int NUMBER;
    
    protected final int key;
    protected final int node;
    
    public MagicKey(int key) {
	this.key = key;
	this.node = (int) Math.floor((double)((key * CLIENTS) / NUMBER));
    }
    
    public MagicKey(int node, int key) {
	this.node = node; 
	this.key = key;
    }

    @Override
    public boolean equals (Object o) {
       if (this == o) return true;
       if (o == null || getClass() != o.getClass()) return false;

       MagicKey other = (MagicKey) o;

       if (this.hashCode() != other.hashCode()) return false;
       return this.key == other.key && this.node == other.node;
    }
    
    public int hashCode() {
	return key;
    }
    
    @Override
    public String toString() {
        return this.node + " owns " + this.key; 
    }
}
