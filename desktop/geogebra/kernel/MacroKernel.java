/* 
GeoGebra - Dynamic Mathematics for Everyone
http://www.geogebra.org

This file is part of GeoGebra.

This program is free software; you can redistribute it and/or modify it 
under the terms of the GNU General Public License as published by 
the Free Software Foundation.

*/
package geogebra.kernel;

import geogebra.common.util.MaxSizeHashMap;
import geogebra.io.MyXMLHandler;
import geogebra.kernel.commands.AlgebraProcessor;
import geogebra.kernel.geos.GeoGebraCASInterface;
import geogebra.common.kernel.arithmetic.ExpressionNodeEvaluator;
import geogebra.common.kernel.cas.GeoGebraCasInterfaceSlim;
import geogebra.common.kernel.geos.GeoElement;
import geogebra.common.main.MyError;
import geogebra.common.main.AbstractApplication.CasType;

import java.util.LinkedHashMap;



/**
 * Kernel with its own construction for macros.
 */
public class MacroKernel extends Kernel  {

	private Kernel parentKernel;
	private MacroConstruction macroCons;
	
	public MacroKernel(Kernel parentKernel) {
		this.parentKernel = parentKernel;
		
		app = parentKernel.app;
		setUndoActive(false);
		setAllowVisibilitySideEffects(false);
		
		macroCons = new MacroConstruction(this);
		cons = macroCons;	
		
		//does 3D as parentKernel
		setManager3D(getParentKernel().newManager3D(this));
	}
	
	public final boolean isMacroKernel() {
		return true;
	}
	
	public Kernel getParentKernel() {
		return parentKernel;
	}		
	
//	public boolean isUseTempVariablePrefix() {		
//		return super.isUseTempVariablePrefix();
//	}
//	
//	public void setUseTempVariablePrefix(boolean flag) {
//		useTempVariablePrefix = flag;
//		super.setUseTempVariablePrefix(flag);
//	}
	
	public void addReservedLabel(String label) {
		macroCons.addReservedLabel(label);
	}
	
	public void setGlobalVariableLookup(boolean flag) {
		macroCons.setGlobalVariableLookup(flag);
	}
	
	/**
	 * Sets macro construction of this kernel via XML string.	 
	 * @return success state
	 */
	public void loadXML(String xmlString) throws Exception {
		macroCons.loadXML(xmlString);
	}	

	public final double getXmax() {
		return parentKernel.getXmax();
	}
	public final double getXmin() {
		return parentKernel.getXmin();
	}
	public final double getXscale() {
		return parentKernel.getXscale();
	}
	public final double getYmax() {
		return parentKernel.getYmax();
	}
	public final double getYmin() {
		return parentKernel.getYmin();
	}
	public final double getYscale() {
		return parentKernel.getYscale();
	}
	
	/**
	 * Adds a new macro to the parent kernel.
	 */
	public void addMacro(Macro macro) {
		parentKernel.addMacro(macro);
	}
	
	/**
	 * Returns the macro object for the given macro name.
	 * Note: null may be returned.
	 */
	public Macro getMacro(String name) {
		return parentKernel.getMacro(name);	
	}			
	
	
	////////////////////////////////////////
	// METHODS USING KERNEL3D
	////////////////////////////////////////
	
	public MyXMLHandler newMyXMLHandler(Construction cons){
		return parentKernel.newMyXMLHandler(this, cons);		
	}
	
	
	protected AlgebraProcessor newAlgebraProcessor(Kernel kernel){
		return parentKernel.newAlgebraProcessor(kernel);
	}
	
	protected ExpressionNodeEvaluator newExpressionNodeEvaluator(){
		return parentKernel.newExpressionNodeEvaluator();
	}
	
	public GeoElement createGeoElement(Construction cons, String type) throws MyError {    
		return parentKernel.createGeoElement(cons, type);
	}
	
	public boolean handleCoords(GeoElement geo, LinkedHashMap<String, String> attrs) {
		return parentKernel.handleCoords(geo, attrs);
	}
	
	/**
	 * Returns the parent kernel's GeoGebraCAS object.
	 */
	public GeoGebraCasInterfaceSlim getGeoGebraCAS() {
		return parentKernel.getGeoGebraCAS();
	}
	
	/**
	 * @return Whether the GeoGebraCAS of the parent kernel has been initialized before
	 */
	public boolean isGeoGebraCASready() {
		return parentKernel.isGeoGebraCASready();
	}

	/**
	 * Resets the GeoGebraCAS of the parent kernel and clears all variables.
	 */
	public void resetGeoGebraCAS() {
		parentKernel.resetGeoGebraCAS();
	}
		
	/**
	 * Sets currently used underlying CAS, e.g. MPReduce or Maxima.
	 * @param casID CasType.MPREDUCE or CAS_MPREDUCE.CAS_Maxima
	 */
	public void setDefaultCAS(CasType casID) {
		parentKernel.setDefaultCAS(DEFAULT_CAS);
	}
	
	/**
	 * Removes the given variableName from their underlying CAS.
	 */
	public void unbindVariableInGeoGebraCAS(String variableName) {
		parentKernel.unbindVariableInGeoGebraCAS(variableName);
	}
	
	/**
	 * @return Hash map for caching CAS results from parent kernel.
	 */
	public MaxSizeHashMap<String, String> getCasCache() {
		return parentKernel.getCasCache();
	}
	
	/**
	 * @return Whether parent kernel is already using CAS caching.
	 */
	public boolean hasCasCache() {
		return parentKernel.hasCasCache();
	}
	
}
