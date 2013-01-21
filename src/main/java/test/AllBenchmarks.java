package test;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AllBenchmarks {

    public static final int combinations = 4; 
    public static final int[] POSSIBLE_NODES = { 1, 2, 3, 4, 5, 6, 7, 8};
    public static final int[] POSSIBLE_THREADS = { 1, 1, 1, 1, 1, 1, 1, 1};
    public static final String[] TESTS = { "gmu", "ssi" };
    public static final int ATTEMPTS = 1;

    public static class Result {
	public double time = 0;
	public double aborts = 0;
    }

    public static void main(String[] args) {
	// test -> comb -> results
	Map<String, Map<Integer, Result>> allData = new HashMap<String, Map<Integer, Result>>();
	for (String test : TESTS) {
	    Map<Integer, Result> perTest = new HashMap<Integer, Result>();
	    for (int i = 1; i <= combinations; i++) {
		Result result = new Result();
		for (int a = 0; a < ATTEMPTS; a++) {
		    double maxTime = 0.0;
		    double aborts = 0.0;
		    for (int c = 1; c <= POSSIBLE_NODES[i - 1]; c++) {
			List<String> content = getFileContent(args[0] + "/" + test + "-" + i + "-" + c + "-" + (a+1) + ".out");
			int l = 0;
			while (true) {
			    try {
				String[] parts = content.get(l).split(" ");
				int time = Integer.parseInt(parts[0]);
				int ab = Integer.parseInt(parts[1]);
				if (time > maxTime) {
				    maxTime = time;
				}
				aborts += ab;
				break;
			    } catch (Exception e) {
				l++;
			    }
			}
		    }
		    result.time += maxTime;
		    result.aborts += aborts;
		}
		perTest.put(i, result);
	    }
	    allData.put(test, perTest);
	}

	String output = "- gmu ssi";
	String outputA = "- gmu ssi";
	for (int comb = 0; comb < combinations; comb++) {
	    output += "\n" + POSSIBLE_NODES[comb] + "*" + POSSIBLE_THREADS[comb];
	    outputA += "\n" + POSSIBLE_NODES[comb] + "*" + POSSIBLE_THREADS[comb];
	    for (String test : TESTS) {
		Result result = allData.get(test).get(comb + 1);
		double avg = result.time / ATTEMPTS;
		double aborts = result.aborts / ATTEMPTS;
		output += " " + ((int)roundTwoDecimals(avg));
		outputA += " " + ((int)roundTwoDecimals(aborts));
	    }
	}
	writeToFile(args[0] + "/results/t.output", output);
	writeToFile(args[0] + "/results/a.output", outputA);

    }

    private static void writeToFile(String filename, String content) {
	try {
	    FileWriter fstream = new FileWriter(filename);
	    BufferedWriter out = new BufferedWriter(fstream);
	    out.write(content);
	    out.close();
	} catch (Exception e) {
	    throw new RuntimeException(e);
	}
    }

    private static List<String> getFileContent(String filename) {
	List<String> testLines1 = new ArrayList<String>();
	try {
	    FileInputStream is = new FileInputStream(filename);
	    DataInputStream in = new DataInputStream(is);
	    BufferedReader br = new BufferedReader(new InputStreamReader(in, Charset.forName("UTF-8")));
	    String strLine;
	    while ((strLine = br.readLine()) != null) {
		if (strLine.equals("")) {
		    continue;
		}
		testLines1.add(strLine);
	    }
	    br.close();
	} catch (Exception e) {
	    e.printStackTrace();
	    System.exit(1);
	}
	return testLines1;
    }

    private static double roundTwoDecimals(double d) {
	DecimalFormat twoDForm = new DecimalFormat("#");
	return Double.valueOf(twoDForm.format(d));
    }

}
