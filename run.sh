#! /bin/sh

cp="/home/nmld/.m2/repository/org/infinispan/infinispan-core/5.2.0-cloudtm-SNAPSHOT/infinispan-core-5.2.0-cloudtm-SNAPSHOT.jar:/home/nmld/.m2/repository/org/jgroups/jgroups/3.3.0-cloudtm-SNAPSHOT/jgroups-3.3.0-cloudtm-SNAPSHOT.jar:/home/nmld/.m2/repository/org/jboss/spec/javax/transaction/jboss-transaction-api_1.1_spec/1.0.0.Final/jboss-transaction-api_1.1_spec-1.0.0.Final.jar:/home/nmld/.m2/repository/org/jboss/marshalling/jboss-marshalling-river/1.3.6.GA/jboss-marshalling-river-1.3.6.GA.jar:/home/nmld/.m2/repository/org/jboss/marshalling/jboss-marshalling/1.3.6.GA/jboss-marshalling-1.3.6.GA.jar:/home/nmld/.m2/repository/org/jboss/logging/jboss-logging/3.1.0.GA/jboss-logging-3.1.0.GA.jar:/home/nmld/.m2/repository/org/codehaus/woodstox/woodstox-core-asl/4.1.1/woodstox-core-asl-4.1.1.jar:/home/nmld/.m2/repository/org/codehaus/woodstox/stax2-api/3.1.1/stax2-api-3.1.1.jar:/home/nmld/.m2/repository/com/clearspring/analytics/stream/2.2.0/stream-2.2.0.jar:/home/nmld/.m2/repository/com/github/egrim/java-bloomier-filter/1.0.Final/java-bloomier-filter-1.0.Final.jar:/home/nmld/.m2/repository/com/googlecode/kryo/1.04/kryo-1.04.jar:/home/nmld/.m2/repository/asm/asm/3.2/asm-3.2.jar:/home/nmld/.m2/repository/com/googlecode/reflectasm/1.01/reflectasm-1.01.jar:/home/nmld/.m2/repository/com/googlecode/minlog/1.2/minlog-1.2.jar:/home/nmld/.m2/repository/org/rhq/helpers/rhq-pluginAnnotations/3.0.4/rhq-pluginAnnotations-3.0.4.jar"

prefix="/home/nmld/workspace/dist-stamp"
configFile=$prefix/ispn.xml

minMem="-Xms512M"
maxMem="-Xmx6G"

tm[1]="gmu"
tm[2]="ssi"

combs[1]="-c 1 -l 1"
combs[2]="-c 2 -l 1"
combs[3]="-c 3 -l 1"
combs[4]="-c 4 -l 1"
combs[5]="-c 5 -l 1"
combs[6]="-c 6 -l 1"
combs[7]="-c 7 -l 1"
combs[8]="-c 8 -l 1"

nodes[1]="1"
nodes[2]="2"
nodes[3]="3"
nodes[4]="4"
nodes[5]="5"
nodes[6]="6"
nodes[7]="7"
nodes[8]="8"

for ispn in 2
do
	cp $prefix/ispn-${tm[$ispn]}.xml ispn.xml
	mvn clean compile
	cp $prefix/jgroups.xml target/classes/jgroups.xml
	cd target;
	cd classes;
	for t in 3 4 5 6 7 8
        do
        	for attempt in 1
                do
			echo "sem -j${nodes[$t]} ${combs[$t]} -n 4 -q 60 -u 98 -r 256 -t 4000 >> $prefix/results/${tm[$ispn]}-$t-$i-$attempt.out"
			for i in `seq 1 ${nodes[$t]}`
			do
				sem -j${nodes[$t]} java $minMem $maxMem -cp $cp:. eu.cloudtm.jstamp.vacation.Vacation $configFile ${combs[$t]} -n 4 -q 60 -u 98 -r 256 -t 4000 >> $prefix/results/${tm[$ispn]}-$t-$i-$attempt.out
			done
			sem --wait
                done
        done
	cd ../../;
done
