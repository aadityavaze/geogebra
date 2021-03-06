/* 
GeoGebra - Dynamic Mathematics for Everyone
http://www.geogebra.org

This file is part of GeoGebra.

This program is free software; you can redistribute it and/or modify it 
under the terms of the GNU General Public License as published by 
the Free Software Foundation.

 */

package org.geogebra.common.geogebra3D.kernel3D.algos;

import org.geogebra.common.geogebra3D.kernel3D.geos.GeoLine3D;
import org.geogebra.common.kernel.Construction;
import org.geogebra.common.kernel.Matrix.Coords;
import org.geogebra.common.kernel.kernelND.GeoLineND;

/**
 * Compute a line orthogonal to two lines
 *
 * @author matthieu
 */
public abstract class AlgoOrthoLineLine extends AlgoElement3D {

	protected GeoLineND line1; // input
	private GeoLine3D line; // output
	protected Coords origin, direction2, origin1, direction1;

	public AlgoOrthoLineLine(Construction cons, GeoLineND line1) {
		super(cons);
		this.line1 = line1;
		line = new GeoLine3D(cons);
	}

	public GeoLine3D getLine() {
		return line;
	}

	@Override
	public void compute() {

		origin1 = line1.getPointInD(3, 0).getInhomCoordsInSameDimension();
		direction1 = line1.getPointInD(3, 1).sub(origin1);
		setOriginAndDirection2();
		getLine().setCoord(origin, direction1.crossProduct(direction2));

	}

	protected abstract void setOriginAndDirection2();

}
