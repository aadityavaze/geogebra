package org.geogebra.web.web.gui.toolbarpanel;

import java.util.ArrayList;

import org.geogebra.common.gui.toolcategorization.ToolCategorization;
import org.geogebra.common.gui.toolcategorization.ToolCategorization.Category;
import org.geogebra.web.html5.gui.util.NoDragImage;
import org.geogebra.web.html5.main.AppW;
import org.geogebra.web.web.gui.app.GGWToolBar;
import org.geogebra.web.web.gui.util.StandardButton;

import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Label;

public class Tools extends FlowPanel {

	ToolCategorization mToolCategorization;
	AppW app;

	public Tools(AppW appl) {
		app = appl;
		mToolCategorization = new ToolCategorization(app,
				ToolCategorization.Type.GRAPHING_CALCULATOR, false);
		mToolCategorization.resetTools();
		ArrayList<ToolCategorization.Category> categories = mToolCategorization
				.getCategories();

		for (int i = 0; i < categories.size(); i++) {
			add(new CategoryPanel(categories.get(i)));
		}

	}

	private class CategoryPanel extends FlowPanel {
		private Category category;

		public CategoryPanel(ToolCategorization.Category cat) {
			super();
			category = cat;
			initGui();
		}

		private void initGui() {
			add(new Label(mToolCategorization.getLocalizedHeader(category)));

			FlowPanel toolsPanel = new FlowPanel();
			ArrayList<Integer> tools = mToolCategorization.getTools(
					mToolCategorization.getCategories().indexOf(category));

			for (int i = 0; i < tools.size(); i++) {
				toolsPanel.add(getButton(tools.get(i)));
			}

			add(toolsPanel);

		}

		private StandardButton getButton(int mode) {
			NoDragImage im = new NoDragImage(GGWToolBar
					.getImageURL(mode, app));
			StandardButton btn = new StandardButton(null, "", 32);
			btn.getUpFace().setImage(im);
			return btn;
		}
	}

}