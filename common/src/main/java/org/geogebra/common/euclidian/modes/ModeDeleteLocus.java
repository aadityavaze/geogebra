package org.geogebra.common.euclidian.modes;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;

import org.geogebra.common.awt.GPoint2D;
import org.geogebra.common.awt.GRectangle;
import org.geogebra.common.euclidian.EuclidianConstants;
import org.geogebra.common.euclidian.EuclidianController;
import org.geogebra.common.euclidian.EuclidianCursor;
import org.geogebra.common.euclidian.EuclidianView;
import org.geogebra.common.euclidian.Hits;
import org.geogebra.common.euclidian.event.AbstractEvent;
import org.geogebra.common.euclidian.event.PointerEventType;
import org.geogebra.common.factories.AwtFactory;
import org.geogebra.common.kernel.Construction;
import org.geogebra.common.kernel.MyPoint;
import org.geogebra.common.kernel.SegmentType;
import org.geogebra.common.kernel.algos.AlgoAttachCopyToView;
import org.geogebra.common.kernel.algos.AlgoElement;
import org.geogebra.common.kernel.algos.AlgoLocusStroke;
import org.geogebra.common.kernel.geos.GeoElement;
import org.geogebra.common.kernel.geos.GeoLocusStroke;
import org.geogebra.common.main.Feature;
import org.geogebra.common.util.debug.Log;

public class ModeDeleteLocus extends ModeDelete {
	private EuclidianView view;
	private EuclidianController ec;
	private boolean objDeleteMode = false, penDeleteMode = false;
	private ArrayList<GPoint2D> interPoints;
	private ArrayList<MyPoint[]> newDataAndRealPoint = new ArrayList<MyPoint[]>();

	public ModeDeleteLocus(EuclidianView view) {
		super(view);
		this.ec = view.getEuclidianController();
		this.view = view;
		this.interPoints = new ArrayList<GPoint2D>();
	}

	GRectangle rect = AwtFactory.getPrototype().newRectangle(0, 0, 100, 100);

	@Override
	public void handleMouseDraggedForDelete(AbstractEvent e, int deleteSize,
			boolean forceOnlyStrokes) {
		if (e == null) {
			return;
		}

		int eventX = e.getX();
		int eventY = e.getY();
		rect.setBounds(eventX - deleteSize / 2, eventY - deleteSize / 2,
				deleteSize, deleteSize);
		view.setDeletionRectangle(rect);
		view.setIntersectionHits(rect);
		Hits h = view.getHits();
		if (!this.objDeleteMode && !this.penDeleteMode) {
			updatePenDeleteMode(h);
		}
		boolean onlyStrokes = forceOnlyStrokes || this.penDeleteMode;

		// hide cursor, the new "cursor" is the deletion rectangle
		view.setCursor(EuclidianCursor.TRANSPARENT);

		Iterator<GeoElement> it = h.iterator();

		resetAlgoSet();
		while (it.hasNext()) {
			GeoElement geo = it.next();
			// delete tool should delete the object for dragging
			// at whiteboard
			// see MOW-97
			if (view.getApplication().has(Feature.ERASER)
					&& ec.getMode() == EuclidianConstants.MODE_DELETE) {
				geo.removeOrSetUndefinedIfHasFixedDescendent();
			} else if (geo instanceof GeoLocusStroke) {
				GeoLocusStroke gps = (GeoLocusStroke) geo;

				// we need two arrays for the case that AlgoAttachCopyToView is
				// involved
				// the original points (dataPoints) are saved, but will be
				// translated
				// and everything by the algorithm so that the
				// GeoLocusStroke-output
				// holds the points which are really drawn (and should be used
				// for
				// hit detection).

				MyPoint[] realPoints = gps.getPointsND();
				MyPoint[] dataPoints;

				if (geo.getParentAlgorithm() != null && (geo
						.getParentAlgorithm() instanceof AlgoAttachCopyToView)) {
					AlgoElement ae = geo.getParentAlgorithm();
					for (int i = 0; i < ae.getInput().length; i++) {
						if (ae.getInput()[i] instanceof GeoLocusStroke) {
							gps = (GeoLocusStroke) ae.getInput()[i];
						}
					}
				}

				if (gps.getParentAlgorithm() != null
						&& gps.getParentAlgorithm() instanceof AlgoLocusStroke) {
					dataPoints = ((AlgoLocusStroke) gps.getParentAlgorithm())
							.getPointsND();
				} else {
					dataPoints = gps.getPointsND();
				}

				boolean hasVisiblePart = false;
				if (realPoints.length == dataPoints.length) {
					for (int i = 0; i < dataPoints.length; i++) {
						MyPoint p = realPoints[i];
						if (p.isDefined() && Math.max(
								Math.abs(eventX
										- view.toScreenCoordXd(p.getX())),
								Math.abs(eventY - view.toScreenCoordYd(
										p.getY()))) <= deleteSize / 2.0) {
							// end point of segment is in rectangle
							if ((i - 1 >= 0 && dataPoints[i - 1].isDefined())) {
								// get intersection point
								interPoints.clear();
								interPoints = getAllIntersectionPoint(
										dataPoints[i - 1], dataPoints[i],
											rect);
								if (!interPoints.isEmpty()
										&& interPoints.size() == 1) {
									i = handleEraserAtJoinPointOrEndOfSegments(
											dataPoints, realPoints, i);
									if (newDataAndRealPoint != null
											&& !newDataAndRealPoint.isEmpty()) {
										dataPoints = newDataAndRealPoint.get(0);
										realPoints = newDataAndRealPoint.get(1);
									}
								}
								// no intersection point
								else {
									i = handleEraserAtPoint(dataPoints,
											realPoints, i);
									if (newDataAndRealPoint != null
											&& !newDataAndRealPoint.isEmpty()) {
										dataPoints = newDataAndRealPoint.get(0);
										realPoints = newDataAndRealPoint.get(1);
									}
								}
							}
							// start point of segment is in rectangle
							else if (i - 1 >= 0
									&& !dataPoints[i - 1].isDefined()
									&& i + 1 < dataPoints.length
									&& dataPoints[i + 1].isDefined()) {
								handleEraserAtStartPointOfSegment(dataPoints,
										realPoints, i);
								dataPoints = newDataAndRealPoint.get(0);
								realPoints = newDataAndRealPoint.get(1);
							}
							// handle first/last/single remained point
							else {
								handleLastFirstOrSinglePoints(dataPoints, i);
								dataPoints = newDataAndRealPoint.get(0);
							}
						}
						// eraser is between the endpoints of segment
						else {
							if (i < dataPoints.length - 1
									&& dataPoints[i].isDefined()
									&& dataPoints[i + 1].isDefined()) {
								i = handleEraserBetweenPointsOfSegment(
										dataPoints, realPoints, i);
								if (newDataAndRealPoint != null
										&& !newDataAndRealPoint.isEmpty()) {
									dataPoints = newDataAndRealPoint.get(0);
									realPoints = newDataAndRealPoint.get(1);
									i = i + 2;
								}
							}
						}

						populateAlgoUpdateSet(dataPoints[i]);

						if (!hasVisiblePart && dataPoints[i].isDefined()) {
							hasVisiblePart = true;
						}
					}

					deleteUnnecessaryUndefPoints(dataPoints, realPoints);
					if (newDataAndRealPoint != null
							&& !newDataAndRealPoint.isEmpty()) {
						dataPoints = newDataAndRealPoint.get(0);
						realPoints = newDataAndRealPoint.get(1);
					}

					updatePolyLineDataPoints(dataPoints, gps);

				} else {
					Log.debug(
							"Can't delete points on stroke. Different number of in and output points.");
				}
				if (hasVisiblePart) { // still something visible, don't delete
					it.remove(); // remove this Stroke from hits
				}
			} else {
				if (!this.penDeleteMode) {
					this.objDeleteMode = true;
				}
				if (onlyStrokes) {
					it.remove();
				}
			}
		}

		ec.deleteAll(h);
		updateAlgoSet();
	}

	private void updateAlgoSet() {
		// TODO Auto-generated method stub

	}

	private void resetAlgoSet() {
		// TODO Auto-generated method stub

	}

	private void deleteUnnecessaryUndefPoints(MyPoint[] dataPoints,
			MyPoint[] realPoints) {
		newDataAndRealPoint.clear();
		ArrayList<MyPoint> dataPointList = new ArrayList<MyPoint>(
				dataPoints.length);
		ArrayList<MyPoint> realPointList = new ArrayList<MyPoint>(
				realPoints.length);
		int i = 1;
		while (i < dataPoints.length) {
			if (!dataPoints[i].isDefined() && !dataPoints[i - 1].isDefined()) {
				i++;
			} else {
				dataPointList.add(dataPoints[i - 1]);
				realPointList.add(realPoints[i - 1]);
				i++;
			}
		}
		dataPointList.add(dataPoints[i - 1]);
		realPointList.add(realPoints[i - 1]);
		MyPoint[] newDataPoints = new MyPoint[dataPointList.size()];
		MyPoint[] newRealPoints = new MyPoint[dataPointList.size()];
		dataPointList.toArray(newDataPoints);
		realPointList.toArray(newRealPoints);
		if (newDataPoints.length != dataPoints.length) {
			newDataAndRealPoint.add(newDataPoints);
			newDataAndRealPoint.add(newRealPoints);
		}
	}

	// add new undefined points and update old points coordinates
	private ArrayList<MyPoint[]> getNewPolyLinePoints(
			MyPoint[] dataPoints, MyPoint[] realPoints, int newSize,
			int i, int indexInter1, int indexUndef, int indexInter2,
			double[] realCoords) {
		ArrayList<MyPoint[]> dataAndRealPoint = new ArrayList<MyPoint[]>();
		MyPoint[] newDataPoints = Arrays.copyOf(dataPoints,
				dataPoints.length + newSize);
		MyPoint[] newRealPoints = Arrays.copyOf(realPoints,
				realPoints.length + newSize);

		if (newSize == 1) {
			for (int j = dataPoints.length; j > i + 1; j--) {
				newDataPoints[j + newSize - 1] = dataPoints[j - 1];
				newRealPoints[j + newSize - 1] = realPoints[j - 1];
			}
		} else if (newSize == -1) {
			for (int j = dataPoints.length; j > i; j--) {
				newDataPoints[j] = dataPoints[j - 1];
				newRealPoints[j] = realPoints[j - 1];
			}
		} else {
			for (int j = dataPoints.length; j > i - newSize + 3; j--) {
				newDataPoints[j + newSize - 1] = dataPoints[j - 1];
				newRealPoints[j + newSize - 1] = realPoints[j - 1];
			}
		}
		newDataPoints[indexInter1] = ngp(
				view.getKernel().getConstruction(), realCoords[0],
				realCoords[1], 1);
		newDataPoints[indexUndef] = ngp(
				view.getKernel().getConstruction());
		newRealPoints[indexUndef] = ngp(
				view.getKernel().getConstruction());
		newDataPoints[indexInter2] = ngp(
				view.getKernel().getConstruction(), realCoords[2],
				realCoords[3], 1);

		dataAndRealPoint.add(newDataPoints);
		dataAndRealPoint.add(newRealPoints);

		return dataAndRealPoint;
	}

	private MyPoint ngp(Construction construction) {
		return new MyPoint(Double.NaN, Double.NaN, SegmentType.LINE_TO);
	}

	private MyPoint ngp(Construction construction, double d, double e, int i) {
		// TODO Auto-generated method stub
		return new MyPoint(d, e, SegmentType.LINE_TO);
	}

	private void populateAlgoUpdateSet(MyPoint point) {
		// TODO ignore?
		// if (as == null) {
		// as = point.getAlgoUpdateSet();
		// } else {
		// as.addAll(point.getAlgoUpdateSet());
		// }
	}

	private void updatePenDeleteMode(Hits h) {
		// if we switched to pen deletion just now, some geos may still need
		// removing
		Iterator<GeoElement> it2 = h.iterator();
		while (it2.hasNext()) {
			GeoElement geo2 = it2.next();
			if (geo2 instanceof GeoLocusStroke) {
				this.penDeleteMode = true;
			}
		}
	}

	/**
	 * @param point1
	 *            start point of segment
	 * @param point2
	 *            end point of segment
	 * @param rectangle
	 *            eraser
	 * @return intersection point with top of rectangle (if there is any)
	 */
	public GPoint2D getTopIntersectionPoint(MyPoint point1, MyPoint point2,
			GRectangle rectangle) {
		// Top line
		return getIntersectionPoint(point1, point2,
				rectangle.getX(), rectangle.getY(),
						rectangle.getX() + rectangle.getWidth(),
				rectangle.getY());
	}

	/**
	 * @param point1
	 *            start point of segment
	 * @param point2
	 *            end point of segment
	 * @param rectangle
	 *            eraser
	 * @return intersection point with bottom of rectangle (if there is any)
	 */
	public GPoint2D getBottomIntersectionPoint(MyPoint point1, MyPoint point2,
			GRectangle rectangle) {
		// Bottom line
		return getIntersectionPoint(point1, point2,
				rectangle.getX(),
						rectangle.getY() + rectangle.getHeight(),
						rectangle.getX() + rectangle.getWidth(),
				rectangle.getY() + rectangle.getHeight());
	}

	/**
	 * @param point1
	 *            start point of segment
	 * @param point2
	 *            end point of segment
	 * @param rectangle
	 *            eraser
	 * @return intersection point with left side of rectangle (if there is any)
	 */
	public GPoint2D getLeftIntersectionPoint(MyPoint point1, MyPoint point2,
			GRectangle rectangle) {
		// Left side
		return getIntersectionPoint(point1, point2,
				rectangle.getX(), rectangle.getY(),
						rectangle.getX(),
				rectangle.getY() + rectangle.getHeight());
	}

	/**
	 * @param point1
	 *            start point of segment
	 * @param point2
	 *            end point of segment
	 * @param rectangle
	 *            eraser
	 * @return intersection point with right side of rectangle (if there is any)
	 */
	public GPoint2D getRightIntersectionPoint(MyPoint point1, MyPoint point2,
			GRectangle rectangle) {
		// Right side
		return getIntersectionPoint(point1, point2,
				rectangle.getX() + rectangle.getWidth(),
						rectangle.getY(),
						rectangle.getX() + rectangle.getWidth(),
				rectangle.getY() + rectangle.getHeight());
	}

	/**
	 * method to get all intersection points of a segment with the eraser (with
	 * each side of rectangle)
	 * 
	 * @param point1
	 *            start point of segment
	 * @param point2
	 *            end point of segment
	 * @param rectangle
	 *            eraser
	 * @return list of intersection points
	 */
	public ArrayList<GPoint2D> getAllIntersectionPoint(MyPoint point1,
			MyPoint point2,
			GRectangle rectangle) {
		ArrayList<GPoint2D> interPointList = new ArrayList<GPoint2D>();
		// intersection points
		GPoint2D topInter = getTopIntersectionPoint(point1, point2, rectangle);
		if (topInter != null) {
			interPointList.add(topInter);
		}
		GPoint2D bottomInter = getBottomIntersectionPoint(point1, point2,
				rectangle);
		if (bottomInter != null) {
			interPointList.add(bottomInter);
		}
		GPoint2D leftInter = getLeftIntersectionPoint(point1, point2,
				rectangle);
		if (leftInter != null) {
			interPointList.add(leftInter);
		}
		GPoint2D rightInter = getRightIntersectionPoint(point1, point2,
				rectangle);
		if (rightInter != null) {
			interPointList.add(rightInter);
		}
		return interPointList;
	}

	/**
	 * method to get the intersection point of two segment (not line)
	 * 
	 * @param point1
	 *            start point of first segment
	 * @param point2
	 *            end point of first segment
	 * @param startPointX
	 *            start coord of start point of second segment
	 * @param startPointY
	 *            end coord of start point of second segment
	 * @param endPointX
	 *            start coord of end point of second segment
	 * @param endPointY
	 *            end coord of end point of second segment
	 * @return intersection point
	 */
	public GPoint2D getIntersectionPoint(MyPoint point1, MyPoint point2,
			double startPointX, double startPointY, double endPointX,
			double endPointY) {
		double x1 = view.toScreenCoordXd(point1.getX());
		double y1 = view.toScreenCoordYd(point1.getY());
		double x2 = view.toScreenCoordXd(point2.getX());
		double y2 = view.toScreenCoordYd(point2.getY());

		double x3 = startPointX;
		double y3 = startPointY;
		double x4 = endPointX;
		double y4 = endPointY;

		GPoint2D p = null;

		double d = (x1 - x2) * (y3 - y4) - (y1 - y2) * (x3 - x4);
		// are not parallel
		if (d != 0.0) {
			// coords of intersection point with line
			double xi = ((x3 - x4) * (x1 * y2 - y1 * x2)
					- (x1 - x2) * (x3 * y4 - y3 * x4)) / d;
			double yi = ((y3 - y4) * (x1 * y2 - y1 * x2)
					- (y1 - y2) * (x3 * y4 - y3 * x4)) / d;
			// needed to get only the intersection points with segment
			// and not with line
			if (onSegment(Math.round(x1), Math.round(y1), xi, yi,
					Math.round(x2), Math.round(y2))
					&& onSegment(Math.round(x3), Math.round(y3), xi, yi,
							Math.round(x4), Math.round(y4))) {
				p = new GPoint2D.Double(xi, yi);
			}
		}
		return p;
	}

	// check if intersection point is on segment
	private static boolean onSegment(double segStartX, double segStartY,
			double interPointX, double interPointY, double segEndX,
			double segEndY) {
		if (interPointX <= Math.max(segStartX, segEndX)
				&& interPointX >= Math.min(segStartX, segEndX)
				&& interPointY <= Math.max(segStartY, segEndY)
				&& interPointY >= Math.min(segStartY, segEndY)) {
			return true;
		}

		return false;
	}

	// check if the two intersection point is close enough
	private static boolean areClose(GPoint2D point1, GPoint2D point2) {
		double distance = Math.hypot(point1.getX() - point2.getX(),
				point1.getY() - point2.getY());
		return distance < 20;
	}

	private double[] getInterRealCoords(MyPoint point) {
		double[] coords = new double[4];

		double realX1 = view.toRealWorldCoordX(interPoints.get(0).getX());
		double realY1 = view.toRealWorldCoordY(interPoints.get(0).getY());
		double realX2 = view.toRealWorldCoordX(interPoints.get(1).getX());
		double realY2 = view.toRealWorldCoordY(interPoints.get(1).getY());

		double distance1 = Math.hypot(point.getX() - realX1,
				point.getY() - realY1);

		double distance2 = Math.hypot(point.getX() - realX2,
				point.getY() - realY2);

		// we need to decide the order of intersection points
		// in order to set correct the intersection points
		if (distance1 < distance2) {
			coords[0] = realX1;
			coords[1] = realY1;
			coords[2] = realX2;
			coords[3] = realY2;
		} else {
			coords[0] = realX2;
			coords[1] = realY2;
			coords[2] = realX1;
			coords[3] = realY1;
		}

		return coords;
	}

	@Override
	public void mousePressed(PointerEventType type) {
		this.objDeleteMode = false;
		this.penDeleteMode = false;
	}

	private void updatePolyLineDataPoints(MyPoint[] dataPoints,
			GeoLocusStroke gps) {
		if (dataPoints.length != gps.getPoints().size()) {
			if (gps.getParentAlgorithm() != null
					&& gps.getParentAlgorithm() instanceof AlgoLocusStroke) {
				MyPoint[] data = new MyPoint[dataPoints.length];
				for (int k = 0; k < dataPoints.length; k++) {
					data[k] = dataPoints[k];
				}

				((AlgoLocusStroke) gps.getParentAlgorithm())
						.updateFrom(data);

				gps.notifyUpdate();
			}
		}
	}

	@Override
	public boolean process(Hits hits, boolean control,
			boolean selPreview) {
		if (hits.isEmpty() || this.penDeleteMode) {
			return false;
		}
		ec.addSelectedGeo(hits, 1, false, selPreview);
		if (ec.selGeos() == 1) {
			GeoElement[] geos = ec.getSelectedGeos();
			resetAlgoSet();
			// delete only parts of GeoLocusStroke, not the whole object
			// when eraser tool is used
			if (geos[0] instanceof GeoLocusStroke
					&& ec.getMode() == EuclidianConstants.MODE_ERASER) {
				updatePenDeleteMode(hits);
				int eventX = 0;
				int eventY = 0;
				if (ec.getMouseLoc() != null) {
					eventX = ec.getMouseLoc().getX();
					eventY = ec.getMouseLoc().getY();
					rect.setBounds(eventX - ec.getDeleteToolSize() / 2,
							eventY - ec.getDeleteToolSize() / 2,
							ec.getDeleteToolSize(), ec.getDeleteToolSize());
				} else {
					return false;
				}
				GeoLocusStroke gps = (GeoLocusStroke) geos[0];
				MyPoint[] realPoints = gps.getPointsND();
				MyPoint[] dataPoints;

				if (geos[0].getParentAlgorithm() != null && (geos[0]
						.getParentAlgorithm() instanceof AlgoAttachCopyToView)) {
					AlgoElement ae = geos[0].getParentAlgorithm();
					for (int i = 0; i < ae.getInput().length; i++) {
						if (ae.getInput()[i] instanceof GeoLocusStroke) {
							gps = (GeoLocusStroke) ae.getInput()[i];
						}
					}
				}
				if (gps.getParentAlgorithm() != null
						&& gps.getParentAlgorithm() instanceof AlgoLocusStroke) {
					dataPoints = ((AlgoLocusStroke) gps
							.getParentAlgorithm())
									.getPointsND();
				} else {
					dataPoints = gps.getPointsND();
				}

				boolean hasVisiblePart = false;
				if (realPoints.length == dataPoints.length) {
					for (int i = 0; i < dataPoints.length; i++) {
						MyPoint p = realPoints[i];
						if (p.isDefined() && Math.max(
								Math.abs(eventX
										- view.toScreenCoordXd(p.getX())),
								Math.abs(eventY
										- view.toScreenCoordYd(p.getX()))) <= ec
												.getDeleteToolSize() / 2.0) {
							// end point of segment is in rectangle
							if ((i - 1 >= 0 && dataPoints[i - 1].isDefined())) {
								// get intersection point
								interPoints.clear();
								interPoints = getAllIntersectionPoint(
										dataPoints[i - 1], dataPoints[i], rect);
								// one intersection point
								if (!interPoints.isEmpty()
										&& interPoints.size() == 1) {
									i = handleEraserAtJoinPointOrEndOfSegments(
											dataPoints, realPoints, i);
									if (newDataAndRealPoint != null
											&& !newDataAndRealPoint.isEmpty()) {
										dataPoints = newDataAndRealPoint.get(0);
										realPoints = newDataAndRealPoint.get(1);
									}
								}
								// no intersection point
								else {
									i = handleEraserAtPoint(dataPoints,
											realPoints, i);
									if (newDataAndRealPoint != null
											&& !newDataAndRealPoint.isEmpty()) {
										dataPoints = newDataAndRealPoint.get(0);
										realPoints = newDataAndRealPoint.get(1);
									}
								}
							}
							// start point of segment is in rectangle
							else if (i - 1 >= 0
									&& !dataPoints[i - 1].isDefined()
									&& i + 1 < dataPoints.length
									&& dataPoints[i + 1].isDefined()) {
								handleEraserAtStartPointOfSegment(dataPoints,
										realPoints, i);
								dataPoints = newDataAndRealPoint.get(0);
								realPoints = newDataAndRealPoint.get(1);
							}
							// handle first/last/single remained point
							else {
								handleLastFirstOrSinglePoints(dataPoints, i);
								dataPoints = newDataAndRealPoint.get(0);
							}
						}
						// eraser is between the points of segment
						else {
							if (i < dataPoints.length - 1 &&
									dataPoints[i].isDefined()
									&& dataPoints[i + 1].isDefined()) {
								i = handleEraserBetweenPointsOfSegment(
										dataPoints, realPoints, i);
								if (newDataAndRealPoint != null
										&& !newDataAndRealPoint.isEmpty()) {
									dataPoints = newDataAndRealPoint.get(0);
									realPoints = newDataAndRealPoint.get(1);
									i = i + 2;
								}
							}
						}

						populateAlgoUpdateSet(dataPoints[i]);

						if (!hasVisiblePart && dataPoints[i].isDefined()) {
							hasVisiblePart = true;
						}
					}

					deleteUnnecessaryUndefPoints(dataPoints, realPoints);
					if (newDataAndRealPoint != null
							&& !newDataAndRealPoint.isEmpty()) {
						dataPoints = newDataAndRealPoint.get(0);
						realPoints = newDataAndRealPoint.get(1);
					}

					updatePolyLineDataPoints(dataPoints, gps);

				} else {
					Log.debug(
							"Can't delete points on stroke. Different number of in and output points.");
				}
				if (!hasVisiblePart) { // still something visible, don't delete
					// remove this Stroke
					geos[0].removeOrSetUndefinedIfHasFixedDescendent();
				}
				updateAlgoSet();
			}
			// delete this object
			else {
				geos[0].removeOrSetUndefinedIfHasFixedDescendent();
			}
			return true;
		}
		return false;
	}

	private void handleLastFirstOrSinglePoints(MyPoint[] dataPoints, int i) {
		newDataAndRealPoint.clear();
		if ((i == 0 && ((i + 1 < dataPoints.length
				&& !dataPoints[i + 1].isDefined())
				|| (i + 1 == dataPoints.length)))
				|| (i - 1 >= 0 && !dataPoints[i - 1].isDefined()
						&& i + 1 == dataPoints.length)) {
			dataPoints[i].setUndefined();
		}
		// handle single remained point
		else if (i - 1 >= 0 && !dataPoints[i - 1].isDefined()
				&& i + 1 < dataPoints.length
				&& !dataPoints[i + 1].isDefined()) {
			dataPoints[i].setUndefined();
		}
		populateAlgoUpdateSet(dataPoints[i]);
		newDataAndRealPoint.add(dataPoints);

	}

	private void handleEraserAtStartPointOfSegment(MyPoint[] dataPoints,
			MyPoint[] realPoints, int i) {
		newDataAndRealPoint.clear();
		// get intersection points
		interPoints.clear();
		interPoints = getAllIntersectionPoint(dataPoints[i], dataPoints[i + 1],
				rect);
		if (!interPoints.isEmpty() && interPoints.size() == 1) {
			double realX = view.toRealWorldCoordX(interPoints.get(0).getX());
			double realY = view.toRealWorldCoordY(interPoints.get(0).getY());
			// switch old point with intersection point
			dataPoints[i].setCoords(realX, realY, 1);
		}
		// no intersection
		else if (interPoints.isEmpty()) {
			double pointX = view.toScreenCoordXd(dataPoints[i + 1].getX());
			double pointY = view.toScreenCoordYd(dataPoints[i + 1].getY());
			GPoint2D point = AwtFactory.getPrototype().newPoint2D(pointX,
					pointY);
			// if end point is also inside of the
			// rectangle
			if (rect.contains(point)) {
				// we can set point the start point at
				// undefined
				dataPoints[i].setUndefined();
			}
		}
		// 2 intersection points
		else {
			if (areClose(interPoints.get(0), interPoints.get(1))) {
				double realX = view
						.toRealWorldCoordX(interPoints.get(0).getX());
				double realY = view
						.toRealWorldCoordY(interPoints.get(0).getY());
				// switch old point with intersection
				// point
				dataPoints[i].setCoords(realX, realY, 1);
			} else {
				dataPoints[i].setUndefined();
			}
		}
		newDataAndRealPoint.add(dataPoints);
		newDataAndRealPoint.add(realPoints);
	}

	private int handleEraserAtPoint(MyPoint[] dataPoints, MyPoint[] realPoints,
			int i) {
		int index = i;
		newDataAndRealPoint.clear();
		// no intersection points
		if (interPoints.isEmpty()) {
			double pointX = view.toScreenCoordXd(dataPoints[i - 1].getX());
			double pointY = view.toScreenCoordYd(dataPoints[i - 1].getY());
			GPoint2D point = AwtFactory.getPrototype().newPoint2D(pointX,
					pointY);
			// if the first point is also inside of
			// rectangle
			if (rect.contains(point)) {
				// we can set the end point to undefined
				dataPoints[i].setUndefined();
			}
		}
		// two intersection points
		else {
			if (areClose(interPoints.get(0), interPoints.get(1))) {
				double realX = view
						.toRealWorldCoordX(interPoints.get(0).getX());
				double realY = view
						.toRealWorldCoordY(interPoints.get(0).getY());
				// switch old point with intersection
				// point
				dataPoints[i].setCoords(realX, realY, 1);
			} else {
				double[] realCoords = getInterRealCoords(
						dataPoints[i - 1]);
				newDataAndRealPoint = getNewPolyLinePoints(dataPoints,
						realPoints, 1, i, i - 1, i, i + 1, realCoords);
				populateAlgoUpdateSet(newDataAndRealPoint.get(0)[i]);
				index = i + 2;
			}
		}
		if (newDataAndRealPoint != null && newDataAndRealPoint.isEmpty()) {
			newDataAndRealPoint.add(dataPoints);
			newDataAndRealPoint.add(realPoints);
		}
		return index;
	}

	private int handleEraserAtJoinPointOrEndOfSegments(MyPoint[] dataPoints,
			MyPoint[] realPoints, int i) {
		int index = i;
		newDataAndRealPoint.clear();
		ArrayList<GPoint2D> secondInterPoints;
		if (i + 1 < dataPoints.length && dataPoints[i + 1].isDefined()) {
			// see if there is intersection point with next segment
			secondInterPoints = getAllIntersectionPoint(dataPoints[i],
					dataPoints[i + 1], rect);
			// case point is the join point of 2 segments
			if (!secondInterPoints.isEmpty() && secondInterPoints.size() == 1) {
				interPoints.add(secondInterPoints.get(0));
				double[] realCoords = getInterRealCoords(
						dataPoints[i - 1]);
				if (i + 2 < dataPoints.length && dataPoints[i + 2].isDefined()
						&& i - 2 > 0 && dataPoints[i - 2].isDefined()) {
					// switch old point with
					// intersection point
					dataPoints[i - 1].setCoords(realCoords[0], realCoords[1],
							1);
					dataPoints[i].setUndefined();
					// switch old point with
					// intersection point
					dataPoints[i + 1].setCoords(realCoords[2], realCoords[3],
							1);
					newDataAndRealPoint.add(dataPoints);
					newDataAndRealPoint.add(realPoints);
					index = i + 2;
				} else if (i + 2 < dataPoints.length
						&& !dataPoints[i + 2].isDefined() && i - 2 > 0
						&& dataPoints[i - 2].isDefined()) {
					newDataAndRealPoint = getNewPolyLinePoints(dataPoints,
							realPoints, 1, i, i - 1, i, i + 1, realCoords);
					populateAlgoUpdateSet(
							newDataAndRealPoint.get(0)[i - 1]);
					index = i + 2;
				} else if (i - 2 > 0 && !dataPoints[i - 2].isDefined()
						&& i + 2 < dataPoints.length
						&& dataPoints[i + 2].isDefined()) {
					newDataAndRealPoint = getNewPolyLinePoints(dataPoints,
							realPoints, 1, i, i, i + 1, i + 2, realCoords);
					populateAlgoUpdateSet(
							newDataAndRealPoint.get(0)[i - 1]);
					index = i + 2;
				} else if (i - 2 > 0 && !dataPoints[i - 2].isDefined()
						&& i + 2 < dataPoints.length
						&& !dataPoints[i + 2].isDefined()) {
					newDataAndRealPoint = getNewPolyLinePoints(dataPoints,
							realPoints, 2, i, i, i + 1, i + 2, realCoords);
					populateAlgoUpdateSet(
							newDataAndRealPoint.get(0)[i - 1]);
					index = i + 3;
				} else {
					newDataAndRealPoint = getNewPolyLinePoints(dataPoints,
							realPoints, 1, i, i, i + 1, i + 2, realCoords);
					populateAlgoUpdateSet(
							newDataAndRealPoint.get(0)[i + 1]);
					index = i + 2;
				}
			}
		}
		// point is endpoint of segment
		else {
			double realX = view.toRealWorldCoordX(interPoints.get(0).getX());
			double realY = view.toRealWorldCoordY(interPoints.get(0).getY());
			// switch old point with
			// intersection point
			dataPoints[i].setCoords(realX, realY, 1);
			newDataAndRealPoint.add(dataPoints);
			newDataAndRealPoint.add(realPoints);
		}
		return index;
	}

	private int handleEraserBetweenPointsOfSegment(
			MyPoint[] dataPoints, MyPoint[] realPoints, int i) {
		int index = i;
		interPoints.clear();
		interPoints = getAllIntersectionPoint(dataPoints[i], dataPoints[i + 1],
				rect);
		newDataAndRealPoint.clear();
		if (!interPoints.isEmpty() && interPoints.size() >= 2) {
			double[] realCoords = getInterRealCoords(dataPoints[i]);
			// case ?,(A),(B),? or ?,(A),(B)
			if (i - 1 > 0 && !dataPoints[i - 1].isDefined()
					&& ((i + 2 < dataPoints.length
							&& !dataPoints[i + 2].isDefined())
							|| i + 1 == dataPoints.length - 1)) {
				newDataAndRealPoint = getNewPolyLinePoints(dataPoints,
						realPoints, 3, i, i + 1, i + 2, i + 3, realCoords);
				index = i + 2;
			}
			// case ?,(A),(B),...
			else if (i - 1 > 0 && !dataPoints[i - 1].isDefined()
					&& i + 1 != dataPoints.length - 1) {
				newDataAndRealPoint = getNewPolyLinePoints(dataPoints,
						realPoints, 2, i, i + 1, i + 2, i + 3, realCoords);
				index++;
			}
			// case ...,(A),(B),?,... or ...,(A),(B)
			else if (i + 1 == dataPoints.length - 1
					|| (i + 2 < dataPoints.length
							&& !dataPoints[i + 2].isDefined())) {
				newDataAndRealPoint = getNewPolyLinePoints(dataPoints,
						realPoints, 2, i, i, i + 1, i + 2, realCoords);
				index++;
			}
			// otherwise
			else {
				newDataAndRealPoint = getNewPolyLinePoints(dataPoints,
						realPoints, 1, i, i, i + 1, i + 2, realCoords);
			}
		}
		return index;
	}
}