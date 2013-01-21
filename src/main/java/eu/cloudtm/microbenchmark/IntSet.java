package eu.cloudtm.microbenchmark;

public interface IntSet {

    public boolean add(int value, Client c);

    public boolean remove(int value, Client c);

    public boolean contains(int value, Client c);
}
