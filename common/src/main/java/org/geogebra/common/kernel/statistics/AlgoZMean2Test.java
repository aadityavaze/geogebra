/* 
GeoGebra - Dynamic Mathematics for Everyone
http://www.geogebra.org

This file is part of GeoGebra.

This program is free software; you can redistribute it and/or modify it 
under the terms of the GNU General Public License as published by 
the Free Software Foundation.

 */

package org.geogebra.common.kernel.statistics;

import org.apache.commons.math3.distribution.NormalDistribution;
import org.geogebra.common.kernel.Construction;
import org.geogebra.common.kernel.algos.AlgoElement;
import org.geogebra.common.kernel.commands.Commands;
import org.geogebra.common.kernel.geos.GeoElement;
import org.geogebra.common.kernel.geos.GeoList;
import org.geogebra.common.kernel.geos.GeoNumeric;
import org.geogebra.common.kernel.geos.GeoText;
import org.geogebra.common.util.StringUtil;

/**
 * 
 * 
 * @author G. Sturr
 */
public class AlgoZMean2Test extends AlgoElement {

	private GeoNumeric mean, sd, n, mean_2, sd_2, n_2; // input
	private GeoText tail;
	private GeoList list, list2;
	private GeoList result; // output
	private double se;

	/**
	 * @param cons
	 * @param label
	 * @param mean
	 * @param sd
	 * @param n
	 * @param mean_2
	 * @param sd_2
	 * @param n_2
	 * @param tail
	 */
	public AlgoZMean2Test(Construction cons, String label, GeoNumeric mean,
			GeoNumeric sd, GeoNumeric n, GeoNumeric mean_2, GeoNumeric sd_2,
			GeoNumeric n_2, GeoText tail) {
		this(cons, mean, sd, n, mean_2, sd_2, n_2, tail);
		result.setLabel(label);
	}

	/**
	 * @param cons
	 * @param mean
	 * @param sd
	 * @param n
	 * @param mean_2
	 * @param sd_2
	 * @param n_2
	 * @param tail
	 */
	public AlgoZMean2Test(Construction cons, GeoNumeric mean, GeoNumeric sd,
			GeoNumeric n, GeoNumeric mean_2, GeoNumeric sd_2, GeoNumeric n_2,
			GeoText tail) {
		super(cons);
		this.mean = mean;
		this.sd = sd;
		this.n = n;
		this.mean_2 = mean_2;
		this.sd_2 = sd_2;
		this.n_2 = n_2;
		this.tail = tail;
		result = new GeoList(cons);
		setInputOutput(); // for AlgoElement

		compute();
	}

	/**
	 * @param cons
	 * @param label
	 * @param list
	 * @param list2
	 * @param sd
	 * @param sd_2
	 * @param level
	 */
	public AlgoZMean2Test(Construction cons, String label, GeoList list,
			GeoNumeric sd, GeoList list2, GeoNumeric sd_2, GeoText tail) {
		super(cons);

		this.list = list;
		this.sd = sd;
		this.list2 = list2;
		this.sd_2 = sd_2;
		this.tail = tail;
		result = new GeoList(cons);
		setInputOutput(); // for AlgoElement

		compute();
		result.setLabel(label);
	}

	@Override
	public Commands getClassName() {
		return Commands.ZMean2Test;
	}

	@Override
	protected void setInputOutput() {

		if (list == null) {
			input = new GeoElement[7];
			input[0] = mean;
			input[1] = sd;
			input[2] = n;
			input[3] = mean_2;
			input[4] = sd_2;
			input[5] = n_2;
			input[6] = tail;
		} else {
			input = new GeoElement[5];
			input[0] = list;
			input[1] = sd;
			input[2] = list2;
			input[3] = sd_2;
			input[4] = tail;
		}

		setOnlyOutput(result);
		setDependencies(); // done by AlgoElement
	}

	/**
	 * @return {lower confidence limit, upper confidence limit}.
	 */
	public GeoList getResult() {
		return result;
	}

	/**
	 * @return standard error
	 */
	public double getSE() {
		return se;
	}

	@Override
	public final void compute() {

		String testType;
		if (tail.getTextString().equals("<")) {
			testType = "left";
		} else if (tail.getTextString().equals(">")) {
			testType = "right";
		} else if (StringUtil.isNotEqual(tail.getTextString())) {
			testType = "two";
		} else {
			result.setUndefined();
			return;
		}

		double mean1, mean2;
		double n1, n2;

		double sd1 = sd.getDouble();
		double sd2 = sd_2.getDouble();

		if (list == null) {
			mean1 = mean.getDouble();
			mean2 = mean_2.getDouble();
			n1 = n.getDouble();
			n2 = n_2.getDouble();
		} else {
			mean1 = list.mean();
			n1 = list.size();
			mean2 = list2.mean();
			n2 = list2.size();
		}

		se = Math.sqrt(sd1 * sd1 / n1 + sd2 * sd2 / n2);
		double testStatistic = (mean1 - mean2) / se;

		NormalDistribution normalDist = new NormalDistribution(0, 1);
		double P = 0;
		try {
			P = normalDist.cumulativeProbability(testStatistic);
		} catch (Exception e) {
			result.setUndefined();
			return;
		}

		if ("right".equals(testType)) {
			P = 1 - P;
		} else if ("two".equals(testType)) {
			if (testStatistic < 0) {
				P = 2 * P;
			} else {
				P = 2 * (1 - P);
			}
		}

		// put these results into the output list
		result.clear();
		result.addNumber(P, null);
		result.addNumber(testStatistic, null);

	}

}
