package org.geogebra.common.kernel.stepbystep.solution;

import java.util.ArrayList;
import java.util.List;

import org.geogebra.common.gui.view.algebra.StepGuiBuilder;
import org.geogebra.common.kernel.stepbystep.steptree.StepNode;
import org.geogebra.common.main.Localization;

public class SolutionStep {
	/**
	 * The color of the solution step is either contained in the parameters
	 * themselves, or - when there is no parameter, but there is still need for a
	 * color (to signal for example the regrouping of constants), you have to pass a
	 * color. This will be represented as a dot after the text of the step.
	 */

	private SolutionStepType type;
	private StepNode[] parameters;
	private int color;

	private List<SolutionStep> substeps;

	public SolutionStep(SolutionStepType type, StepNode... parameters) {
		this.type = type;
		this.parameters = new StepNode[parameters.length];
		for (int i = 0; i < parameters.length; i++) {
			this.parameters[i] = parameters[i].deepCopy();
		}
	}

	public SolutionStep(SolutionStepType type, int color) {
		this.type = type;
		this.color = color;
	}

	/**
	 * Get the simple (no colors) text of the step
	 * 
	 * @return default text, formatted using LaTeX
	 */
	public String getDefault(Localization loc) {
		return type.getDefaultText(loc, parameters);
	}

	/**
	 * Get the detailed (colored) text of the step
	 * 
	 * @return colored text, formatted using LaTeX
	 */
	public String getColored(Localization loc) {
		return type.getDetailedText(loc, color, parameters);
	}

	public void getListOfSteps(StepGuiBuilder builder, Localization loc) {
		getListOfSteps(builder, loc, true);
	}

	/**
	 * Builds the solution tree using a StepGuiBuilder
	 * 
	 * @param builder
	 *            StepGuiBuilder to use (different for the web and for the tests)
	 */
	public void getListOfSteps(StepGuiBuilder builder, Localization loc, boolean detailed) {
		if (substeps != null && type == SolutionStepType.WRAPPER) {
			for (int i = 0; i < substeps.size(); i++) {
				(substeps.get(i)).getListOfSteps(builder, loc, true);
			}
		} else if (substeps != null && type == SolutionStepType.SUBSTEP_WRAPPER) {
			builder.startDefault();
			for (int i = 1; i < substeps.size(); i++) {
				(substeps.get(i)).getListOfSteps(builder, loc, false);
			}
			builder.switchToDetailed();
			for (int i = 0; i < substeps.size(); i++) {
				(substeps.get(i)).getListOfSteps(builder, loc, true);
			}
			builder.endDetailed();
		} else {
			if (detailed) {
				builder.addLatexRow(getColored(loc));
			} else {
				builder.addLatexRow(getDefault(loc));
			}

			if (substeps != null) {
				builder.startGroup();
				for (int i = 0; i < substeps.size(); i++) {
					(substeps.get(i)).getListOfSteps(builder, loc, true);
				}
				builder.endGroup();
			}
		}
	}

	/**
	 * Adds a substep
	 * 
	 * @param s
	 *            substep to add
	 */
	public void addSubStep(SolutionStep s) {
		if (s != null) {
			if (substeps == null) {
				substeps = new ArrayList<SolutionStep>();
			}

			substeps.add(s);
		}
	}

	public List<SolutionStep> getSubsteps() {
		return substeps;
	}

	public SolutionStepType getType() {
		return type;
	}
}
