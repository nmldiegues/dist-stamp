dist-stamp
==========

Distributed implementation of the STAMP benchmark in Java.

Benchmark main class in eu.cloudtm.jstamp.vacation.Vacation.
Parametrization example: <path-to-ispn.xml> -c 2 -l 2 -n 4 -q 60 -u 98 -r 1024 -t 100
Description of parameters:
 * c: number of nodes
 * l: number of threads per node
 * n: number of operations per transaction
 * q: range of the database to be touched
 * r: size of the database
 * t: total number of transactions to be executed
 * u: percentage of transactions that make reservations (the rest update tables and delete users)

The benchmark yields the time that it takes to execute and the number of aborts it widthstood. This is printed on every node, meaning that benchmarking for results should collect these outputs, max the time that it took to execute and sum the number of aborts.

