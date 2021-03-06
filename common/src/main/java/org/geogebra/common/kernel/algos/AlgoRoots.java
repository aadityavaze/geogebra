/*
GeoGebra - Dynamic Mathematics for Schools
Copyright Markus Hohenwarter and GeoGebra Inc.,  http://www.geogebra.org

This file is part of GeoGebra.

This program is free software; you can redistribute it and/or modify it 
under the terms of the GNU General Public License as published by 
the Free Software Foundation.

 */

package org.geogebra.common.kernel.algos;

//import geogebra.kernel.AlgoElement;
import java.util.ArrayList;

import org.apache.commons.math3.analysis.solvers.BrentSolver;
import org.geogebra.common.euclidian.EuclidianViewInterfaceCommon;
import org.geogebra.common.kernel.Construction;
import org.geogebra.common.kernel.Kernel;
import org.geogebra.common.kernel.arithmetic.Function;
import org.geogebra.common.kernel.commands.Commands;
import org.geogebra.common.kernel.geos.GeoElement;
import org.geogebra.common.kernel.geos.GeoFunction;
import org.geogebra.common.kernel.geos.GeoNumberValue;
import org.geogebra.common.kernel.geos.GeoPoint;
import org.geogebra.common.kernel.roots.RealRootUtil;
import org.geogebra.common.util.debug.Log;

//import org.apache.commons.math3.analysis.solvers.UnivariateRealSolverFactory;

/**
 * Command: Roots[ &lt;function&gt;, &lt;left-x&gt;, &lt;right-x&gt;] (TYPE 0)
 * and Command: Intersect[ &lt;function&gt;, &lt;function&gt;, &lt;left-x&gt;,
 * &lt;right-x&gt;] (TYPE 1) (just uses difference-function instead of one
 * function)
 *
 * Can be used elsewhere: public static final double[] findRoots(GeoFunction
 * f,double l,double r,int samples) public static final double[]
 * calcSingleRoot(GeoFunction f, double l, double r);
 *
 * Extends AlgoGeoPointsFunction (abstract), with the label methods, which again
 * extens AlgoElement.
 *
 * @author Hans-Petter Ulven
 * @version 2011-03-08
 */

public class AlgoRoots extends AlgoGeoPointsFunction {

	private static final int TYPE_ROOTS = 0;
	private static final int TYPE_INTERSECTIONS = 1;


	// Input-Output
	// private GeoFunctionable function; // input
	private GeoFunction f0, f1, f2, diff;

	// Vars
	private int type = TYPE_ROOTS;

	/**
	 * Computes "all" Roots of f in &lt;l,r&gt; TYPE_ROOTS
	 */
	public AlgoRoots(Construction cons, String[] labels, GeoFunction function,
			GeoNumberValue left, GeoNumberValue right, boolean labelEnabled) {
		// Ancestor gets first function for points!
		super(cons, labels, labelEnabled && !cons.isSuppressLabelsActive(),
				function);
		this.f0 = function;
		this.left = left;
		this.right = right;

		type = TYPE_ROOTS;

		setInputOutput();

		compute();

		showOneRootInAlgebraView(); // Show at least one root point in algebra
									// view

	}// Constructor TYPE_ROOTS

	public AlgoRoots(Construction cons, String[] labels, GeoFunction function,
			EuclidianViewInterfaceCommon view) {
		this(cons, labels, function, view.getXminObject(),
				view.getXmaxObject(), true);

		// updates the area that is visible
		cons.registerEuclidianViewCE(this);
		intervalDefinedByEV = true;
	}

	public AlgoRoots(Construction cons, GeoFunction function,
			GeoNumberValue left, GeoNumberValue right) {
		super(cons, function); // Ancestor gets first function for points!
		this.f0 = function;
		this.left = left;
		this.right = right;

		type = TYPE_ROOTS;

		setInputOutput();

		compute();
	}// Constructor TYPE_ROOTS

	/**
	 * Computes "all" Roots of f in &lt;l,r&gt; TYPE_INTERSECTIONS
	 */
	public AlgoRoots(Construction cons, String[] labels, GeoFunction function,
			GeoFunction function2, GeoNumberValue left, GeoNumberValue right) {
		super(cons, labels, !cons.isSuppressLabelsActive(), function); // Ancestor
																		// gets
																		// first
																		// function
																		// for
																		// points!
		this.f1 = function;
		this.f2 = function2;
		this.left = left;
		this.right = right;

		type = TYPE_INTERSECTIONS;

		setInputOutput();

		compute();

		showOneRootInAlgebraView(); // Show at least one root point in algebra
									// view

	}// Constructor TYPE_INTERSECTIONS

	@Override
	public GetCommand getClassName() {
		return Commands.Roots;
	}// getClassName()

	public GeoPoint[] getRootPoints() {
		return getPoints(); // Points in ancestor
	}// getRootPoints()

	@Override
	protected void setInputOutput() {
		switch (type) {
		default:
		case TYPE_ROOTS:
			input = new GeoElement[3];
			input[0] = f0.toGeoElement();
			input[1] = left.toGeoElement();
			input[2] = right.toGeoElement();
			break;
		case TYPE_INTERSECTIONS:
			input = new GeoElement[4];
			input[0] = f1.toGeoElement();
			input[1] = f2.toGeoElement();
			input[2] = left.toGeoElement();
			input[3] = right.toGeoElement();
		}// switch

		super.setOutput(getPoints()); // Points in ancestor

		noUndefinedPointsInAlgebraView(getPoints());

		setDependencies(); // done by AlgoElement
	}// setInputOutput()

	@Override
	public final void compute() {
		if (intervalDefinedByEV) {
			updateInterval();
		}

		boolean ok = false;
		switch (type) {
		default:
		case TYPE_ROOTS:
			ok = f0.toGeoElement().isDefined() && left.isDefined()
					&& right.isDefined();
			break;
		case TYPE_INTERSECTIONS:
			ok = f1.toGeoElement().isDefined() && f2.toGeoElement().isDefined()
					&& left.isDefined() && right.isDefined();
			break;
		}// switch
		if (!ok) {
			setPoints(new double[1], 0); // debug("error in args");
		} else {
			if (type == TYPE_INTERSECTIONS) {
				diff = new GeoFunction(cons);
				diff = GeoFunction.subtract(diff, f1, f2); // Make a difference
															// geofunction for
															// intersections
				compute2(diff);
			} else {
				compute2(f0);
			} // if type
		} // if ok input
	}// compute()

	@Override
	protected double yAt(double x) {
		if (type == TYPE_ROOTS) {
			return 0;
		}
		return f1.value(x);
	}

	private final void compute2(GeoFunction f) {

		double l = left.getDouble();
		double r = right.getDouble();
		double[] roots = new double[0];
		int numberofroots = 0;

		/*
		 * if ( !f.toGeoElement().isDefined() || !geoleft.isDefined() ||
		 * !georight.isDefined() // || (right.getDouble()<=left.getDouble() ) )
		 * { setPoints(new double[1],0); //0 flags no root=>undefined }else {
		 */

		if (l > r) {
			double tmp = l;
			l = r;
			r = tmp;
		} // Correct user input

		// / --- Algorithm --- ///
		int n = findNumberOfSamples(l, r);
		// make sure m is at least 1 even for invisible EV
		int m = Math.max(n, 1);
		try { // To catch eventual wrong indexes in arrays...
				// Adjust samples. Some research needed to find best factor in
				// if(numberofroots<m*factor...
			do { // debug("doing samples: "+m);
				roots = findRoots(f, l, r, m);

				if (roots == null) {
					numberofroots = 0;
				} else {
					numberofroots = roots.length;
				} // debug("found xvalues: "+roots);
				if (numberofroots < m / 2) {
					break;
				}
				m = m * 2;
			} while (m < MAX_SAMPLES);
			if (m > MAX_SAMPLES) {
				Log.debug("We have probably lost some roots...");
			}
		} catch (Exception e) {
			Log.debug("Exception in compute() " + e.toString());
		} // try-catch

		// }//if

		if (numberofroots == 0) {
			setPoints(new double[1], 0); // 0 flags no root=>undefined
		} else {
			setPoints(roots, roots.length);
		} // if
	}// compute()

	/**
	 * Main algorithm, public for eventual use by other commands Finds a
	 * samplesize n depending on screen coordinates Samples n intervals Collects
	 * roots in intervals where y(l)*y(r)&gt;0
	 */
	public static final double[] findRoots(GeoFunction f, double l, double r,
			int samples) {
		if (Kernel.isEqual(l, r)) {
			return Kernel.isZero(f.value(l)) ? new double[] { l }
					: new double[0];
		}
		double[] y = new double[samples + 1]; //
		ArrayList<Double> xlist = new ArrayList<Double>();
		double x, xval;
		double deltax = (r - l) / samples;

		for (int i = 0; i <= samples; i++) {
			x = l + i * deltax;
			y[i] = f.value(x);
			// if left endpoint is root by pure luck...
			if ((Math.abs(y[i]) < Kernel.MIN_PRECISION)
					&& (signChanged(f, x))) { // if
												// left
												// endpoint
												// is
												// root
												// by
												// pure
												// luck...
				xlist.add(x);
			} // if
			if (i > 0) {
				if (((y[i - 1] < 0.0d) && (y[i] > 0.0d)) || // or just
															// y[i-1]*y[i]<0...
						((y[i - 1] > 0.0d) && (y[i] < 0.0d))) {
					xval = calcSingleRoot(f, x - deltax, x);
					if (Math.abs(f.value(xval)) < Kernel.MIN_PRECISION) { // =1E-5:
																				// Quite
																				// large,
																				// but
																				// less
																				// doesn't
																				// work
																				// in
																				// Apache
																				// lib...
						xlist.add(xval);
					} // if check
				} // if possible root
			} // if both ends of interval
		} // for all endpoints
		if (xlist.size() > 0) {
			double[] res = new double[xlist.size()];
			for (int i = 0; i < xlist.size(); i++) {
				res[i] = xlist.get(i);
			} // for all x in xlist
			removeDuplicates(res); // new 08.03.11 to avoid (1,0.00000x) and
									// (1,-0.00000x) ...
			return res;
		}
		// if valid
		return null;
	}// findRoots(f,l,r)

	// / --- Private methods --- ///
	// Make all private after testing...

	/**
	 * Brent's algo Copied from AlgoRootInterval.java.
	 */
	public final static double calcSingleRoot(GeoFunction f, double left,
			double right) {
		BrentSolver rootFinder = new BrentSolver();

		if (!f.isDefined()) {
			return Double.NaN;
		}

		double root = Double.NaN;
		Function fun = f.getFunction();

		try {
			// Brent's method
			root = rootFinder.solve(AlgoRootNewton.MAX_ITERATIONS, fun, left,
					right);
		} catch (Exception e) {
			try {
				// Let's try again by searching for a valid domain first
				double[] borders = RealRootUtil.getDefinedInterval(fun, left,
						right);
				root = rootFinder.solve(AlgoRootNewton.MAX_ITERATIONS, fun,
						borders[0], borders[1]);
			} catch (Exception ex) {
				root = Double.NaN;
			} // try-catch
		} // try-catch

		return root;
	}// calcSingleRoot(f,l,r)

	private static final boolean signChanged(GeoFunction f, double x) {
		double delta = Kernel.MIN_PRECISION * 10; // Used in AlgoRootsPolynomial
		double left, right, lefty, righty;
		boolean signChanged;
		left = x - delta;
		right = x + delta;
		int count = 0;
		while (Math.abs(lefty = f.value(left)) < delta && count++ < 100) {
			left = left - delta;
		}
		count = 0;
		while (Math.abs(righty = f.value(right)) < delta && count++ < 100) {
			right = right + delta;
		}
		signChanged = lefty * righty < 0.0d;
		return signChanged;
	}// signChanged(f,x,deltax)

	/*
	 * @Override protected void updateDependentGeos() { // update dependent
	 * objects for (int i = 0; i < getOutputLength(); i++) {
	 * getOutput(i).updateCascade(); } }
	 */
	@Override
	public boolean euclidianViewUpdate() {
		compute();
		return true;
	}

	@Override
	protected void initPoints(int number) {
		super.initPoints(number);

		// parentAlgorithm is set to null in some cases (see below)
		for (int i = 0; i < points.length; i++) {
			points[i].setParentAlgorithm(this);
		}

		if (points.length > number) {

			// no visible points left
			if (number == 0) {
				ArrayList<GeoPoint> temp = new ArrayList<GeoPoint>();
				for (int i = 0; i < points.length; i++) {
					if (!points[i].getAlgoUpdateSet().isEmpty()) {
						// store points that have dependent objects
						temp.add(points[i]);
					}
				}

				// at least one point with dependencies was found
				if (temp.size() > 0) {
					// delete all other points
					for (int i = 0; i < points.length; i++) {
						if (!temp.contains(points[i])) {
							points[i].setParentAlgorithm(null);
							points[i].remove();
						}
					}
					// do not reset points -> position of the not removed points
					// is not changed
					return;
				}
			}

			for (int i = Math.max(number, 1); i < points.length; i++) {
				if (!points[i].getAlgoUpdateSet().isEmpty()) {
					points[i].setCoords(0, 0, 1); // init as defined
				} else {
					points[i].setParentAlgorithm(null);
					points[i].remove();
				}
			}

			super.setOutput(points);
		}
	}

	@Override
	protected void removePoint(int pos) {
		points[pos].doRemove();

		for (GeoPoint point : points) {
			if (point.isLabelSet()) {
				return;
			}
		}

		super.remove();
	}
}// class AlgoRoots