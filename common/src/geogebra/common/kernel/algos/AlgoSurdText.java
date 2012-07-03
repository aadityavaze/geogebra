/* 
GeoGebra - Dynamic Mathematics for Everyone
http://www.geogebra.org

This file is part of GeoGebra.

This program is free software; you can redistribute it and/or modify it 
under the terms of the GNU General Public License as published by 
the Free Software Foundation.

*/

package geogebra.common.kernel.algos;

import geogebra.common.kernel.Construction;
import geogebra.common.kernel.Kernel;
import geogebra.common.kernel.StringTemplate;
import geogebra.common.kernel.geos.GeoElement;
import geogebra.common.kernel.geos.GeoNumeric;
import geogebra.common.kernel.geos.GeoText;
import geogebra.common.main.App;
import geogebra.common.util.Unicode;

import org.apache.commons.math.util.MathUtils;

public class AlgoSurdText extends AlgoElement {

	private GeoNumeric num; //input
    private GeoText text; //output	
    
    protected StringBuilder sb = new StringBuilder();
    
    public AlgoSurdText(Construction cons, String label, GeoNumeric num) {
    	this(cons, num);
        text.setLabel(label);
    }

    AlgoSurdText(Construction cons, GeoNumeric num) {
        super(cons);
        this.num = num;
               
        text = new GeoText(cons);
		text.setLaTeX(true, false);
		text.setIsTextCommand(true); // stop editing as text
		
        setInputOutput();
        compute();
    }

    public AlgoSurdText(Construction cons) {
		super(cons);
	}

	@Override
	public Algos getClassName() {
        return Algos.AlgoSurdText;
    }

    @Override
	protected void setInputOutput(){
        input = new GeoElement[1];
        input[0] = num;

        setOutputLength(1);
        setOutput(0, text);
        setDependencies(); // done by AlgoElement
    }

    /**
     * Returns resulting text
     * @return resulting text
     */
    public GeoText getResult() {
        return text;
    }

    @Override
	public void compute() {   	
    	StringTemplate tpl = StringTemplate.get(app.getFormulaRenderingType());
		if (input[0].isDefined()) {
			
			sb.setLength(0);
			
			double decimal = num.getDouble();
			
			if ( Kernel.isEqual(decimal - Math.round(decimal) , 0.0, Kernel.MAX_PRECISION)) {
				sb.append(kernel.format(Math.round(decimal),tpl));
			} else {
				/*double[] frac = AlgoFractionText.DecimalToFraction(decimal, AbstractKernel.EPSILON);
				if (frac[1]<10000)
					Fractionappend(sb, (int)frac[0], (int)frac[1]);
				else*/
					//PSLQappendQuartic(sb, decimal, tpl);
				
				PSLQappendGeneral(sb, decimal, tpl);
			}
						
			text.setTextString(sb.toString());
			text.setLaTeX(true, false);
			
		} else {
			text.setUndefined();
		}			
	}
    
    private void Fractionappend(StringBuilder sb, int numer, int denom,StringTemplate tpl) {
    	
    	if (denom<0) {
			denom= -denom;
			numer= -numer;
		}
		
		if (denom == 1) { // integer
			sb.append(kernel.format(numer,tpl));				
		} else if (denom == 0) { // 1 / 0 or -1 / 0
			sb.append( numer < 0 ? "-"+Unicode.Infinity : ""+Unicode.Infinity);				
		} else {
	    	sb.append("{\\frac{");
	    	sb.append(kernel.format(numer,tpl));
	    	sb.append("}{");
	    	sb.append(kernel.format(denom,tpl));
	    	sb.append("}}");
	    	
		}
    }
    
    /**
     * Goal: modifies a StringBuilder object sb to be a radical up to quartic roots
     * The precision is adapted, according to setting
     * @param sb
     * @param num
     * @param tpl
     */
    protected void PSLQappendGeneral(StringBuilder sb, double num,StringTemplate tpl) {

    	StringBuilder sbToCAS = new StringBuilder();
    	
		int numOfConsts = 1;
		
		//double[] constValue = new double[] {Math.sqrt(2.0), Math.sqrt(3.0), Math.PI, Math.E};
		//String[] constName = new String[] {"sqrt(2)", "sqrt(3)", "pi", "exp(1)"}; //ℯ e
		
		double[] constValue = new double[] {Math.sqrt(2.0)};
		String[] constName = new String[] {"sqrt(2)"}; //ℯ e
		
		double[] numList = new double[(1+numOfConsts)*3]; //(numOfConsts+1)(degree+1)
		
		
		double temp;

		for (int j=0; j<numOfConsts; j++) {
			temp = constValue[j];
			for (int i=0; i<3; i++) {
				numList[j*3+i] = temp;
				temp *=num;
			}
		}
		
		temp = 1.0;
		//Note: it turns out to be better to put the 1 term at the end instead of in the front 
		for (int i=0; i<3; i++) {
			numList[numOfConsts*3+i] = temp;
			temp *=num;
		}
		
		int[] coeffs = PSLQ(numList,getKernel().getEpsilon(),10);
		
		//Suppose A+Bx+Cx^2 = 0, where A,B,C are linear combinations of 1 and values in constValue

		
		boolean isAZero = true;
		boolean isARational = true;
		int numOfTermsInA = 0;
		
		boolean isBZero = true;
		boolean isBRational = true;
		int numOfTermsInB = 0;
		
		boolean isCZero = true;
		boolean isCRational = true;
		int numOfTermsInC = 0;

		if (coeffs[numOfConsts*3]!=0)	{isAZero = false; numOfTermsInA++;}
		if (coeffs[numOfConsts*3+1]!=0)	{isBZero = false; numOfTermsInB++;}
		if (coeffs[numOfConsts*3+2]!=0)	{isCZero = false; numOfTermsInC++;}
		
		for (int j = 0; j<numOfConsts; j++) {
			if (coeffs[j*3]!=0) {
				isAZero = false;
				isARational = false;
				numOfTermsInA++;
			}
			if (coeffs[j*3+1]!=0) {
				isBZero = false;
				isBRational = false;
				numOfTermsInB++;
			}
			if (coeffs[j*3+2]!=0) {
				isCZero = false;
				isCRational = false;
				numOfTermsInC++;
			}
		}
		
		if (isARational && isBRational && isCRational) 
		{ //TODO: optimize this
			PSLQappendQuadratic(sb, num, tpl);
			return;
		}
		
		
		if (isAZero && isBZero && isCZero) {
			appendUndefined();
		} else if (isCZero) {
			if (isBZero) {
				appendUndefined();
			} else {
				
				StringBuilder AString = new StringBuilder();
				StringBuilder BString = new StringBuilder();
								
				AString.append(kernel.format(coeffs[numOfConsts*3], tpl));
				if(!isARational) {
					AString.append("+");
					appendCombination(AString,(coeffs[numOfConsts*3]==0)? numOfTermsInA : numOfTermsInA-1, constName, coeffs, 0, 3, tpl);
				}
				
				BString.append(kernel.format(coeffs[numOfConsts*3+1], tpl));
				if(!isBRational) {
				BString.append("+");
				appendCombination(BString,(coeffs[numOfConsts*3+1]==0)? numOfTermsInB : numOfTermsInB-1, constName, coeffs, 1, 3, tpl);
				}
				
				sbToCAS.append("-(");
				sbToCAS.append(AString.toString());
				sbToCAS.append(")/(");
				sbToCAS.append(BString.toString());
				sbToCAS.append(")");
			}
		} else {
			
		
			double Avalue = coeffs[numOfConsts*3] + evaluateCombination(constValue, coeffs, 0, 3);
			double Bvalue = coeffs[numOfConsts*3+1] + evaluateCombination(constValue, coeffs, 1, 3);
			double Cvalue = coeffs[numOfConsts*3+2] + evaluateCombination(constValue, coeffs, 2, 3);
			double discr = Bvalue*Bvalue - 4*Avalue*Cvalue;
			StringBuilder AString = new StringBuilder();
			StringBuilder BString = new StringBuilder();
			StringBuilder CString = new StringBuilder();
			
			AString.append(kernel.format(coeffs[numOfConsts*3], tpl));
			if(!isARational) {
				AString.append("+");
				appendCombination(AString,(coeffs[numOfConsts*3]==0)? numOfTermsInA : numOfTermsInA-1, constName, coeffs, 0, 3, tpl);
			}
			
			BString.append(kernel.format(coeffs[numOfConsts*3+1], tpl));
			if(!isBRational) {
			BString.append("+");
			appendCombination(BString,(coeffs[numOfConsts*3+1]==0)? numOfTermsInB : numOfTermsInB-1, constName, coeffs, 1, 3, tpl);
			}
			
			CString.append(kernel.format(coeffs[numOfConsts*3+2], tpl));
			if(!isCRational) {
				CString.append("+");
				appendCombination(CString,(coeffs[numOfConsts*3+2]==0)? numOfTermsInC : numOfTermsInC-1, constName, coeffs, 2, 3, tpl);
			}
			
			
			sbToCAS.append("(");
			
			sbToCAS.append("-(");
			sbToCAS.append(BString.toString());
			sbToCAS.append(")");
			
			if (!Kernel.isZero(discr)) {
				if (num * 2 * Cvalue + Bvalue >= 0 ) {
					sbToCAS.append("+");
				} else
					sbToCAS.append("-");
			
				sbToCAS.append("sqrt(");
				
				sbToCAS.append("(");
				sbToCAS.append(BString.toString());
				sbToCAS.append(")^2");
				
				sbToCAS.append("-4*(");
				sbToCAS.append(CString.toString());
				sbToCAS.append(")*(");
				sbToCAS.append(AString.toString());
				sbToCAS.append(")");
				
				sbToCAS.append(")");
		
			}

			sbToCAS.append(")/(");

			sbToCAS.append("2*(");
			sbToCAS.append(CString.toString());
			sbToCAS.append(")");
			sbToCAS.append(")");
			
		}
		
		sb.append(
		kernel.getGeoGebraCAS().evaluateGeoGebraCAS(
		
				kernel.getGeoGebraCAS().getCASparser().parseGeoGebraCASInputAndResolveDummyVars(sbToCAS.toString())
		
		, null,tpl)
		);
		
		/*
		if (isAZero && isBZero && isCZero) {
			appendUndefined();
		} else if (isCZero) {
			if (isBZero) {
				appendUndefined();
			} else if (isARational && isBRational) {
				//coeffs[1]: denominator;  -coeffs[0]: numerator
				int numer = -coeffs[0];
				int denom = coeffs[1];
				Fractionappend(sb, numer, denom,tpl);
			} else {
				
				boolean hasDenominator = !(isBRational && (coeffs[1] == 1 || coeffs[1]==-1));
				
				if (hasDenominator) sb.append("\\frac{ ");
				
				if (!hasDenominator && coeffs[1]==-1) sb.append("-(");
				
				if (numOfTermsInA==0) {
					sb.append(kernel.format(0, tpl));
				} else {
					if (coeffs[0]!=0) {
						sb.append(kernel.format(coeffs[0], tpl));
						if (numOfTermsInA>1) {
							sb.append("+");
							appendCombination(numOfTermsInA-1, constName, coeffs, 0, 3, tpl);
						}
					} else {
						appendCombination(numOfTermsInA, constName, coeffs, 0, 3, tpl);
					}
				}
				
				if (!hasDenominator && coeffs[1]==-1) sb.append(")");
				
				if (hasDenominator) {
					sb.append(" }{ ");
					
					if (coeffs[1]!=0) {
						sb.append(kernel.format(coeffs[1], tpl));
						if (numOfTermsInB>1) {
							sb.append("+");
							appendCombination(numOfTermsInB-1, constName, coeffs, 1, 3, tpl);
						}
					} else {
						appendCombination(numOfTermsInB, constName, coeffs, 1, 3, tpl);
					}

					sb.append(" }");
				}
				
			}
		} else {
			double Avalue = coeffs[0] + evaluateCombination(constValue, coeffs, 3, 3);
			double Bvalue = coeffs[1] + evaluateCombination(constValue, coeffs, 4, 3);
			double Cvalue = coeffs[2] + evaluateCombination(constValue, coeffs, 5, 3);
			double discr = Bvalue*Bvalue - 4*Avalue*Cvalue;
			
			sb.append("\\frac{");
			
			if (numOfTermsInB!=0) {
				sb.append("-");
			}
			
			if (numOfTermsInB == 1) {
				for (int j=0; j<1+numOfConsts; j++) {
					if (constValue[1+j*3]!=0) {
						sb.append(kernel.format(constValue[1+j*3], tpl));
						break;
					}
				}
			} else if (numOfTermsInB >1){
				sb.append("(");
				if (coeffs[1]!=0) {
					sb.append(kernel.format(coeffs[1], tpl));
					sb.append("+");
					appendCombination(numOfTermsInB-1, constName, coeffs, 1, 3, tpl);
				} else {
					appendCombination(numOfTermsInB, constName, coeffs, 1, 3, tpl);
				}
				sb.append(")");
			}
			
			
			if (!Kernel.isZero(discr)) {
				
				if (num >= -Bvalue / 2 / Cvalue) {
					if (!isBZero) 
						sb.append("+");
				} else
					sb.append("-");
				
				sb.append("\\sqrt{");
				
				sb.append("Todo...");
				
				sb.append("}");
			
				
			}
			
			sb.append(" }{ ");
			
			sb.append("Todo...");
			
			sb.append(" }");
		}
		*/
	}
			
	
    // returns the sum of constValue[j] * coeffs[offset+j*step] over j
    private static double evaluateCombination(double[] constValue, int[] coeffs, int offset, int step) {
    	double sum = 0;
    	
    	for (int j=0; j<constValue.length; j++) {
    		sum+= constValue[j] * coeffs[offset + j*step];
    	}
    	
    	return sum;
	}

	//append a linear combination coeffs[offset + j*step] * vars[j] to the StringBuilder sb 
    private void appendCombination(StringBuilder sbToCAS, int numOfTerms, String[] vars, int[] coeffs, int offset, int step, StringTemplate tpl) {
	
    	int numOfAllTerms = vars.length;
    	if (numOfAllTerms-1 != Math.floor((coeffs.length-1-step-offset)/step)) { //checksum
    		appendUndefined();
    		return;
    	}
    	
    	if (numOfTerms==0) {    		
    		return;
    	}
    	
    	int counter = numOfTerms-1; //number of pluses
			
			for (int j=0; j<numOfAllTerms; j++) {
				
				if (coeffs[offset+j*step]==0) {
					continue;
				} else if (coeffs[offset+j*step]!=1) {
					sbToCAS.append(kernel.format(coeffs[offset+j*step], tpl)); sbToCAS.append("*");
				}
				
				sbToCAS.append(vars[j]);
				
				
				if (counter>0) {
					sbToCAS.append(" + ");
					counter--;
				}
				
			}
		
	}

	private void appendUndefined() {
		
    	sb.append("\\text{"+app.getPlain("undefined")+"}");
	}

	/**
     * Goal: modifies a StringBuilder object sb to be a radical up to quartic roots
     * The precision is adapted, according to setting
     * @param sb
     * @param num
     * @param tpl
     */
    protected void PSLQappendQuartic(StringBuilder sb, double num,StringTemplate tpl) {
		double[] numPowers = new double[5];
		double temp = 1.0;
		
		for (int i=4; i>=0; i--) {
			numPowers[i] = temp;
			temp *=num;
		}
		
		int[] coeffs = PSLQ(numPowers,getKernel().getEpsilon(),10);
		
		if (coeffs[0] == 0 && coeffs[1] ==0) {

			if (coeffs[2] == 0 && coeffs[3] == 0 && coeffs[4] == 0 ) {
				sb.append("\\text{"+app.getPlain("undefined")+"}");
			} else if (coeffs[2] == 0) {
				//coeffs[1]: denominator;  coeffs[2]: numerator
				int denom = coeffs[3];
				int numer = -coeffs[4];
				Fractionappend(sb, numer, denom,tpl);
				
			} else {
				
				//coeffs, if found, shows the equation coeffs[2]+coeffs[1]x+coeffs[0]x^2=0"
				//We want x=\frac{a +/- b1\sqrt{b2}}{c}
				//where  c=coeffs[0], a=-coeffs[1], b=coeffs[1]^2 - 4*coeffs[0]*coeffs[2]
				int a = -coeffs[3];
				int b2 = coeffs[3]*coeffs[3] - 4*coeffs[2]*coeffs[4];
				int b1 =1;
				int c = 2*coeffs[2];

				if (b2 <= 0) { //should not happen!
					sb.append("\\text{"+app.getPlain("undefined")+"}");
					return;
				}
				
				//free the squares of b2
				while (b2 % 4==0) {
					b2 = b2 / 4;
					b1 = b1 * 2;
				}
				for (int s = 3; s<=Math.sqrt(b2); s+=2)
					while (b2 % (s*s) ==0) {
						b2 = b2 / (s*s);
						b1 = b1 * s;
					}
				
				if (c<0) {
					a=-a;
					c=-c;
				}
				
				boolean positive;
				if (num > (a+0.0)/c) {
					positive=true;
					if (b2==1) {
						a+=b1;
						b1=0;
						b2=0;
					}
				} else {
					positive=false;
					if (b2==1) {
						a-=b1;
						b1=0;
						b2=0;
					}
				}
				
				int gcd = MathUtils.gcd(MathUtils.gcd(a,b1),c);
				if (gcd!=1) {
					a=a/gcd;
					b1=b1/gcd;
					c=c/gcd;
				}
				
				//when fraction is needed
				if (c!=1) sb.append("\\frac{");
				
				if (a!=0) sb.append(kernel.format(a,tpl));
				
				//when the radical is surd
				if (b2!=0) {
					if (positive) {
						if (a!=0) sb.append("+");
					} else {
						sb.append("-");
					}
					
					if (b1!=1)
						sb.append(kernel.format(b1,tpl));
					sb.append("\\sqrt{");
					sb.append(kernel.format(b2,tpl));
					sb.append("}");
				}
				
				//when fraction is needed
				if (c!=1) {
					sb.append("}{");
					sb.append(kernel.format(c,tpl));
			    	sb.append("}");
				}
			}
		} else if (coeffs[0] ==0){
			sb.append("Root of a cubic equation: ");
			sb.append(kernel.format(coeffs[1], tpl));
			sb.append("x^3 + ");
			sb.append(kernel.format(coeffs[2], tpl));
			sb.append("x^2 + ");
			sb.append(kernel.format(coeffs[3], tpl));
			sb.append("x + ");
			sb.append(kernel.format(coeffs[4], tpl));
		} else {
			sb.append("Root of a quartic equation: ");
			sb.append(kernel.format(coeffs[0], tpl));
			sb.append("x^4 + ");
			sb.append(kernel.format(coeffs[1], tpl));
			sb.append("x^3 + ");
			sb.append(kernel.format(coeffs[2], tpl));
			sb.append("x^2 + ");
			sb.append(kernel.format(coeffs[3], tpl));
			sb.append("x + ");
			sb.append(kernel.format(coeffs[4], tpl));
		}
		

    }
    
    
    /**
     * Quadratic Case. modifies a StringBuilder object sb to be the quadratic-radical expression of num, within certain precision.
     * @param sb
     * @param num
     * @param tpl
     */
    protected void PSLQappendQuadratic(StringBuilder sb, double num,StringTemplate tpl) {
    	
    	if (Kernel.isZero(num)) {
    		sb.append('0');
    		return;
    	}
    	
		double[] numPowers = {num * num, num, 1.0};
		int[] coeffs = PSLQ(numPowers,Kernel.STANDARD_PRECISION,10);
		
		if (coeffs[0] == 0 && coeffs[1] == 0 && coeffs[2] == 0 ) {
			sb.append("\\text{"+app.getPlain("undefined")+"}");
		} else if (coeffs[0] == 0) {
			//coeffs[1]: denominator;  coeffs[2]: numerator
			int denom = coeffs[1];
			int numer = -coeffs[2];
			Fractionappend(sb, numer, denom,tpl);
			
		} else {
			
			//coeffs, if found, shows the equation coeffs[2]+coeffs[1]x+coeffs[0]x^2=0"
			//We want x=\frac{a +/- b1\sqrt{b2}}{c}
			//where  c=coeffs[0], a=-coeffs[1], b=coeffs[1]^2 - 4*coeffs[0]*coeffs[2]
			int a = -coeffs[1];
			int b2 = coeffs[1]*coeffs[1] - 4*coeffs[0]*coeffs[2];
			int b1 =1;
			int c = 2*coeffs[0];

			if (b2 <= 0) { //should not happen!
				sb.append("\\text{"+app.getPlain("undefined")+"}");
				return;
			}
			
			//free the squares of b2
			while (b2 % 4==0) {
				b2 = b2 / 4;
				b1 = b1 * 2;
			}
			for (int s = 3; s<=Math.sqrt(b2); s+=2)
				while (b2 % (s*s) ==0) {
					b2 = b2 / (s*s);
					b1 = b1 * s;
				}
			
			if (c<0) {
				a=-a;
				c=-c;
			}
			
			boolean positive;
			if (num > (a+0.0)/c) {
				positive=true;
				if (b2==1) {
					a+=b1;
					b1=0;
					b2=0;
				}
			} else {
				positive=false;
				if (b2==1) {
					a-=b1;
					b1=0;
					b2=0;
				}
			}
			
			int gcd = MathUtils.gcd(MathUtils.gcd(a,b1),c);
			if (gcd!=1) {
				a=a/gcd;
				b1=b1/gcd;
				c=c/gcd;
			}
			
			//when fraction is needed
			if (c!=1) sb.append("\\frac{");
			
			if (a!=0) sb.append(kernel.format(a,tpl));
			
			//when the radical is surd
			if (b2!=0) {
				if (positive) {
					if (a!=0) sb.append("+");
				} else {
					sb.append("-");
				}
				
				if (b1!=1)
					sb.append(kernel.format(b1,tpl));
				sb.append("\\sqrt{");
				sb.append(kernel.format(b2,tpl));
				sb.append("}");
			}
			
			//when fraction is needed
			if (c!=1) {
				sb.append("}{");
				sb.append(kernel.format(c,tpl));
		    	sb.append("}");
			}
		}

    }
  
    /*	Algorithm PSLQ
	* from Ferguson and Bailey (1992)
     */
	private static int[] PSLQ(double[] x, double AccuracyFactor, int bound) {
		
		int n = x.length;
		int[] coeffs = new int[n];

		double normX;
		double[] ss;
		double[][] H, P, newH;
		int[][] D, E, A, B, newAorB;
		double[][][] G;
		int [][][] R;
		double gamma, deltaSq;
		
		for (int i=0; i<n; i++) {
			coeffs[i]=0; 
		}
		
		if (n<=1)
			return coeffs;
		
		for (int i=0; i<n; i++) {
			if (Double.isNaN(x[i])) return coeffs; 
		}
		
		//normalize x
		normX = 0;
		for (int i=0; i<n; i++) {
			normX += x[i] * x[i];
		}
		normX = Math.sqrt(normX);
		for (int i=0; i<n; i++) {
			x[i] = x[i]/normX;
		}
		
		//partial sums of squares
		ss = new double[n];
		ss[n-1] = x[n-1] * x[n-1];
		for (int i = n-2; i>=0; i--) {
			ss[i] = ss[i+1] + x[i] * x[i];
		}
		for (int i = 0; i<n; i++) {
			ss[i] = Math.sqrt(ss[i]);
		}
		
		//pre-calculate ss[j]*ss[j+1]
		double[] Pss = new double[n-1];
		for (int i=0; i<n-1; i++) {
			Pss[i] = ss[i] * ss[i+1];
		}
		
		//initialize Matrix H (lower trapezoidal
		H = new double[n][n-1];
		for (int i = 0; i<n; i++) {
			for (int j=0; j<i; j++) {
				H[i][j] = -x[i]*x[j]/Pss[j];
			}
			
			if (i<n-1)
				H[i][i] = ss[i+1]/ss[i];
			
			for (int j=i+1; j<n-1; j++) {
				H[i][j] = 0;
			}
		}
		
		//test property of H: the n-1 columns are orthogonal
		/*
		for (int i =0 ; i<n-1; i++) {
			for (int j=0; j<n-1; j++) {
				double sum = 0;
				for (int k=0; k<n; k++) {
					sum += H[k][i]*H[k][j];
				}
				System.out.println(sum);
			}
		}*/
					
	
		//matrix P = In - x.x
		P = new double[n][n];
		for (int i=0; i<n; i++)
			for (int j=0; j<n; j++)
				P[i][j] = -x[i]*x[j];
		for (int i=0; i<n; i++)
			P[i][i]+=1;
		
		//debug: |P|^2=|H|^2 = n-1
		//AbstractApplication.debug("Frobenius Norm Squares: \n"
		//		+ "|P|^2 = " + frobNormSq(P,n,n)
		//		+ "|H|^2 = " + frobNormSq(H,n,n-1)
		//		);
		

		//initialize matrices R
		R = new int[n-1][n][n];
		for (int j=0; j<n-1; j++) {
			for (int i=0; i<n; i++)
				for (int k=0; k<n; k++)
					R[j][i][k]=0;
			for (int i=0; i<n; i++)
				R[j][i][i]=1;
			R[j][j][j]=0;
			R[j][j][j+1]=1;
			R[j][j+1][j]=1;
			R[j][j+1][j+1]=0;
		}
		
		gamma = 1.5;
		deltaSq = 3.0/4 - (1.0/gamma)/gamma;
		
		//initialize A, B = I_n
		A = new int[n][n];
		for (int i=0; i<n; i++)
			for (int j=0; j<n; j++)
				A[i][j]=0;
		for (int i=0; i<n; i++)
			A[i][i]=1;
		B = new int[n][n];
		for (int i=0; i<n; i++)
			for (int j=0; j<n; j++)
				B[i][j]=0;
		for (int i=0; i<n; i++)
			B[i][i]=1;
		
		//iteration
		int itCount = 0;
		double itBound = 2.0*gamma/deltaSq * n*n*(n+1)*Math.log(Math.sqrt(bound*bound*n)*n*n)/Math.log(2);
		//AbstractApplication.debug("itBound = " + itBound);
		while (itCount < itBound){
			
			//0. test if we have found a relation in a column of B
			double[] xB = new double[n];
			for (int i=0; i<n; i++) {
				xB[i]=0;
				for (int k=0; k<n; k++)
					xB[i]+= x[k]*B[k][i];
				if (Kernel.isEqual(xB[i],0,AccuracyFactor)) {
					for (int k=0; k<n; k++)
						coeffs[k] = B[k][i];
					return coeffs;
				}
			}
					
			//0.5. calculate D, E
			//matrix D
			D = new int[n][n];
			double[][] D0 = new double[n][n]; //testing
			for (int i=0; i<n; i++) {
				//define backwards. the 0's and 1's should be defined first.
				for (int j=n-1; j>=i+1; j--) {
					D[i][j]=0;
					D0[i][j]=0;
				}
				D[i][i]=1;
				D0[i][i]=1;
				
				for (int j=i-1; j>=0; j--) {
					double sum = 0;
					double sum0 = 0;
					for (int k=j+1; k<=i; k++) {
						sum+=D[i][k]*H[k][j];
						sum0+=D0[i][k]*H[k][j];
					}
					
					D[i][j]=(int) Math.floor(-1.0/H[j][j]*sum + 0.5);
					D0[i][j]=-1.0/H[j][j]*sum0;
				}
			
			}
			
			//matrix E = D^{-1}
			E = new int[n][n];
			for (int i=0; i<n; i++) {
				//define backwards. the 0's and 1's should be defined first.
				for (int j=n-1; j>=i+1; j--) {
					E[i][j]=0;
				}
				E[i][i]=1;
				for (int j=i-1; j>=0; j--) {
					int sum = 0;
					for (int k=j+1; k<=i; k++)
						sum+=E[i][k]*D[k][j];
					
					E[i][j]= -sum;
				}
				
			}
			
			//1. replace H by DH
			newH = new double[n][n-1];
			double[][] newH0 = new double[n][n-1];
			for (int i = 0; i<n; i++) {
				for (int j=0; j<n-1; j++) {
					newH[i][j]=0;
					newH0[i][j]=0;
					for (int k=0; k<n; k++) {
						newH[i][j]+=D[i][k]*H[k][j];
						newH0[i][j]+=D0[i][k]*H[k][j];
					}
					
				}
			}
			
			for (int i = 0; i<n; i++)
				for (int j=0; j<n-1; j++)
					H[i][j]=newH[i][j];
			
			
			
			//2. find j to maximize gamma^j |h_jj|
			double gammaPow = 1;
			double temp;
			double max=0;
			int index=0;
			
			for (int j=0; j<n-1; j++) {
				gammaPow *= gamma;
				temp = gammaPow * Math.abs(H[j][j]);
				if (max<temp) {
					max = temp;
					index = j;
				}
			}
		
			//2.5 calculate matrices G[0], G[1],... G[n-2]
			G = new double[n-1][n-1][n-1];
			for (int i=0; i<n-1; i++)
				for (int k=0; k<n-1; k++)
					G[n-2][i][k] = 0;
			for (int i=0; i<n-1; i++)
				G[n-2][i][i]=1;
				
			
			for (int j=0; j<n-2; j++) {
				double b = H[j+1][j];
				double c = H[j+1][j+1];
				double d = Math.sqrt(b*b+c*c);
				for (int i=0; i<n-2; i++)
					for (int k=0; k<n-2; k++)
						G[j][i][k]=0;
				for (int i=0; i<j; i++)
					G[j][i][i]=1;
				for (int i=j+2; i<n-1; i++)
					G[j][i][i]=1;
				G[j][j][j]=b/d;
				G[j][j][j+1]=-c/d;
				G[j][j+1][j]=-G[j][j][j+1]; // =c/d
				G[j][j+1][j+1]=G[j][j][j]; // = b/d
			}
			
			
			//3. replace H by R_jHG_j, A by R_jDA, B by BER_j
			newH = new double[n][n-1];
			for (int i = 0; i<n; i++) {
				for (int j=0; j<n-1; j++) {
					newH[i][j]=0;
					for (int k=0; k<n; k++)
						for (int l=0; l<n-1; l++)
							newH[i][j]+=R[index][i][k]*H[k][l]*G[index][l][j];
				}
			}
			for (int i = 0; i<n; i++)
				for (int j=0; j<n-1; j++)
					H[i][j]=newH[i][j];
			
			newAorB = new int[n][n];
			for (int i = 0; i<n; i++) {
				for (int j=0; j<n; j++) {
					newAorB[i][j]=0;
					for (int k=0; k<n; k++)
						for (int l=0; l<n; l++)
							newAorB[i][j]+=R[index][i][k]*D[k][l]*A[l][j];
				}
			}
			for (int i = 0; i<n; i++)
				for (int j=0; j<n; j++)
					A[i][j]=newAorB[i][j];
			
			for (int i = 0; i<n; i++) {
				for (int j=0; j<n; j++) {
					newAorB[i][j]=0;
					for (int k=0; k<n; k++)
						for (int l=0; l<n; l++)
							newAorB[i][j]+=B[i][k]*E[k][l]*R[index][l][j];
				}
			}
			for (int i = 0; i<n; i++)
				for (int j=0; j<n; j++)
					B[i][j]=newAorB[i][j];
			
			itCount++;
		}
		
		
		return coeffs;
	}

	
	
	private static double frobNormSq(double[][] matrix, int m, int n) {
		//m is number of rows; n is number of columns
		double ret = 0;
		
		if (m==0 || n==0)
			return ret;
		
		for (int i=0; i<m; i++)
			for (int j=0; j<n; j++)
				ret += matrix[i][j] * matrix[i][j];
		
		return ret;
	}

	@Override
	public boolean isLaTeXTextCommand() {
		return true;
	}
}
