package com.winterwell.schools;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.winterwell.maths.datastorage.DataTable;
import com.winterwell.maths.stats.StatsUtils;
import com.winterwell.utils.MathUtils;
import com.winterwell.utils.io.CSVReader;
import com.winterwell.utils.io.CSVWriter;
import com.winterwell.utils.web.SimpleJson;

public class SchoolStats {

	private static Map<String,String> name4code;

	public static void main(String[] args) {
		
		CSVReader r0 = new CSVReader(new File("data/schools/school-codes.csv"));		
		name4code = new HashMap();
		for (String[] row : r0) {
			name4code.put(row[0].trim(),row[1].trim());
		} 

		doNumAwards();
		
		doTariffPoints();
	}

	private static void doTariffPoints() {
		String dName = "tariff-points";
		CSVReader r = new CSVReader(new File("data/schools/"+dName+".csv"));			
		// [FeatureCode, DateCode, Measurement, Units, 
		// Value, School Comparator, Attainment Distribution, DataMarker]
		String[] h = r.next();
		// e.g. 1004620,Academic year 2019-20,Average Total Tariff Score,Tariff Points,
		// 114,Real Establishment,Lowest 20%,
		Map<String, Map<String, List<Double>>> name2slice2pts = new HashMap();
		for (String[] row : r) {
			String name = name4code.get(row[0]);			
			if (name==null) {
				continue;
			}
			String _year = row[1];
			Matcher m = Pattern.compile("20\\d\\d").matcher(_year);
			boolean ok = m.find();
			if ( ! ok) {
				continue;
			}
			int year = (int) MathUtils.toNum(m.group());
			Double score = Double.valueOf(row[4]);
			String rv = row[5];
			String slice = row[6];
			if ( ! rv.startsWith("Real")) {
				continue;
			}
			if ( ! slice.startsWith("High")) {
//				continue;
			}
			Map<String, List<Double>> slice2pts = (Map) SimpleJson.getCreate(name2slice2pts, name);
			List<Double> pts = slice2pts.get(slice);
			if (pts == null) {
				pts = new ArrayList();
				slice2pts.put(slice, pts);
			}
			pts.add(score);
		}
				
		// name, slice, mean, var, trend
		CSVWriter w = getWriter(dName);
		w.write("School", "Slice", "Mean Tariff Pts", "Std Dev");
		for (String name : name2slice2pts.keySet()) {
			Map<String, List<Double>> slice2pts = name2slice2pts.get(name);
			for (String slice : slice2pts.keySet()) {
				List<Double> pts = slice2pts.get(slice);
				double m = StatsUtils.mean(MathUtils.toArray(pts));
				double v = StatsUtils.var(MathUtils.toArray(pts));
				w.write(name, slice, m, Math.sqrt(v));		
			}
		}
		w.close();		
	}

	private static CSVWriter getWriter(String dName) {
		return new CSVWriter(new File("data/schools-out/"+dName+".munged.csv"));
	}

	private static void doNumAwards() {
		String dName = "num-awards";
//		FeatureCode,DateCode,Measurement,Units,Value,School Comparator,Number Of Awards,SCQF Level,Courses,DataMarker
//		5102138,Academic year 2016-17,Percent,Percent Of Leaver Cohort,96,Virtual Comparator,1 And Above,4 And Above,All Courses,
		DataTable<String> dTable = readCSV(dName);
		CSVWriter w = getWriter(dName);
		for (Object[] row : dTable) {
			w.write(row);
		}
		w.close();
	}

	private static DataTable<String> readCSV(String dName) {
		CSVReader r = new CSVReader(new File("data/schools/"+dName+".csv"));			
		String[] h = r.next(); // headers
		DataTable dTable = new DataTable<>();
		dTable.add("School", "Year", "Number of N5 or higher awards", "Percentage of pupils");
		for (String[] row : r) {
			String name = name4code.get(row[0]);			
			if (name==null) {
				continue;
			}
			String _year = row[1];
			Matcher m = Pattern.compile("20\\d\\d").matcher(_year);
			boolean ok = m.find();
			if ( ! ok) {
				continue;
			}
			int year = (int) MathUtils.toNum(m.group());
			
			// num-awards
			Double score = Double.valueOf(row[4]);
			String rv = row[5];
			if ( ! rv.startsWith("Real")) {
				continue;
			}
			// how many?
			String numAwards = row[6];
			Pattern pNum = Pattern.compile("\\d+");
			m = pNum.matcher(numAwards);
			ok = m.find();
			if ( ! ok) {
				continue;
			}
			int inumAwards = (int) MathUtils.toNum(m.group());
			
			String level = row[7];
			String courses = row[8];
			
			// National 5 = GCSE
			if ( ! level.startsWith("5")) {
				continue;
			}
			if ( ! courses.startsWith("SQA")) {
//				continue;
			}
			dTable.add(name, year, inumAwards, score);
		}
		return dTable;
	}
}
