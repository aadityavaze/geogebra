package com.himamis.retex.editor.share.controller;

import java.util.ArrayList;

import com.google.j2objc.annotations.Weak;
import com.himamis.retex.editor.share.editor.MathField;
import com.himamis.retex.editor.share.meta.MetaArray;
import com.himamis.retex.editor.share.meta.MetaCharacter;
import com.himamis.retex.editor.share.meta.MetaFunction;
import com.himamis.retex.editor.share.meta.MetaModel;
import com.himamis.retex.editor.share.meta.Tag;
import com.himamis.retex.editor.share.model.MathArray;
import com.himamis.retex.editor.share.model.MathCharacter;
import com.himamis.retex.editor.share.model.MathComponent;
import com.himamis.retex.editor.share.model.MathContainer;
import com.himamis.retex.editor.share.model.MathFunction;
import com.himamis.retex.editor.share.model.MathSequence;
import com.himamis.retex.editor.share.util.JavaKeyCodes;
import com.himamis.retex.editor.share.util.Unicode;

@SuppressWarnings("javadoc")
public class InputController {

	public static final char FUNCTION_OPEN_KEY = '('; // probably universal
	public static final char FUNCTION_CLOSE_KEY = ')';
	public static final char DELIMITER_KEY = ';';

	private MetaModel metaModel;

    @Weak
    private MathField mathField;

    private boolean createFrac = true;

	public InputController(MetaModel metaModel) {
		this.metaModel = metaModel;
	}

    public MathField getMathField() {
        return mathField;
    }

    public void setMathField(MathField mathField) {
        this.mathField = mathField;
    }

    public boolean getCreateFrac() {
        return createFrac;
    }

    public void setCreateFrac(boolean createFrac) {
        this.createFrac = createFrac;
    }

	final static private char getLetter(MathComponent component)
			throws Exception {
		if (!(component instanceof MathCharacter)) {
			throw new Exception("Math component is not a character");
		}

		MathCharacter mathCharacter = (MathCharacter) component;
		if (!mathCharacter.isCharacter()) {
			throw new Exception("Math component is not a character");
		}

		char c = mathCharacter.getUnicode();

		if (!Character.isLetter(c)) {
			throw new Exception("Math component is not a character");
		}

		return c;
	}

	/**
	 * Insert array.
	 */
	public MathArray newArray(EditorState editorState, int size,
			char arrayOpenKey) {
		MathSequence currentField = editorState.getCurrentField();
		int currentOffset = editorState.getCurrentOffset();
		MetaArray meta = metaModel.getArray(arrayOpenKey);
		MathArray array = new MathArray(meta, size);
		ArrayList<MathComponent> removed = cut(currentField, currentOffset, -1,
				editorState, array, true);

		// add sequence
		MathSequence field = new MathSequence();
		array.setArgument(0, field);
		insertReverse(field, -1, removed);
		for (int i = 1; i < size; i++) {
			// add sequence
			array.setArgument(i, new MathSequence());
		}
		editorState.resetSelection();
		// set current
		editorState.setCurrentField(field);
		editorState.setCurrentOffset(field.size());
		return array;
	}

	/**
	 * Insert matrix.
	 */
	public void newMatrix(EditorState editorState, int columns, int rows) {
		MathSequence currentField = editorState.getCurrentField();
		int currentOffset = editorState.getCurrentOffset();
		MetaArray meta = metaModel.getMatrix();
		MathArray matrix = new MathArray(meta, columns, rows);
		currentField.addArgument(currentOffset, matrix);

		// add sequence
		MathSequence field = new MathSequence();
		matrix.setArgument(0, field);

		for (int i = 1; i < matrix.size(); i++) {
			// add sequence
			matrix.setArgument(i, new MathSequence());
		}

		// set current
		editorState.setCurrentField(field);
		editorState.setCurrentOffset(0);
	}

	/**
	 * Insert braces (), [], {}, "".
	 */
	public void newBraces(EditorState editorState, char ch) {
		String casName = ArgumentHelper.readCharacters(editorState);
		if (ch == FUNCTION_OPEN_KEY && Tag.lookup(casName) != null) {
			delCharacters(editorState, casName.length());
			newFunction(editorState, casName, false);

		} else if ((ch == FUNCTION_OPEN_KEY || ch == '[')
				&& metaModel.isFunction(casName)) {
			delCharacters(editorState, casName.length());
			newFunction(editorState, casName, ch == '[');

		} else {
			String selText = editorState.getSelectedText().trim();
			if(editorState.getSelectionStart() instanceof MathCharacter){
				if (selText.startsWith("<") && selText.endsWith(">")) {

					deleteSelection(editorState);
					MetaArray meta = metaModel.getArray(ch);
					MathArray array = new MathArray(meta, 1);
					MathSequence seq = new MathSequence();
					array.setArgument(0, seq);
					editorState.getCurrentField()
							.addArgument(editorState.getCurrentOffset(), array);
					editorState.setCurrentField(seq);
					editorState.setCurrentOffset(0);
					return;

				}
			}
			// TODO brace type
			newArray(editorState, 1, ch);
		}
	}

	/**
	 * Insert function by name.
	 *
	 * @param name
	 *            function
	 */
	public void newFunction(EditorState editorState, String name,
			boolean square) {
		newFunction(editorState, name, 0, square);
	}

	/**
	 * Insert function by name.
	 *
	 * @param name
	 *            function
	 */
	public void newFunction(EditorState editorState, String name, int initial,
			boolean square) {
		MathSequence currentField = editorState.getCurrentField();
		int currentOffset = editorState.getCurrentOffset();
		// add extra braces for sqrt, nthroot and fraction
		if ("^".equals(name) && currentOffset > 0
				&& editorState.getSelectionEnd() == null) {
			if (currentField
					.getArgument(currentOffset - 1) instanceof MathFunction) {
				MathFunction function = (MathFunction) currentField
						.getArgument(currentOffset - 1);
				if ("sqrt".equals(function.getName())
						|| "nroot".equals(function.getName())
						|| "frac".equals(function.getName())) {

					currentField.delArgument(currentOffset - 1);
					// add braces
					MathArray array = new MathArray(
							metaModel.getArray(Tag.REGULAR), 1);
					currentField.addArgument(currentOffset - 1, array);
					// add sequence
					MathSequence field = new MathSequence();
					array.setArgument(0, field);
					field.addArgument(0, function);
				}
			}
		}

		// add function
		MathFunction function;
		Tag tag = Tag.lookup(name);
		int offset = 0;
		if (tag != null) {
			MetaFunction meta = metaModel.getGeneral(tag);
			function = new MathFunction(meta);

		} else {
			offset = 1;
			MetaFunction meta = metaModel.getFunction(name, square);
			MathSequence nameS = new MathSequence();
			for (int i = 0; i < name.length(); i++) {
				nameS.addArgument(new MathCharacter(
						metaModel.getCharacter(name.charAt(i) + "")));
			}
			function = new MathFunction(meta);
			function.setArgument(0, nameS);
		}

		// add sequences
		for (int i = offset; i < function.size(); i++) {
			MathSequence field = new MathSequence();
			function.setArgument(i, field);
		}

		// pass characters for fraction and factorial only
		if ("frac".equals(name)) {
			if (editorState.getSelectionEnd() != null) {
				ArrayList<MathComponent> removed = cut(currentField,
						currentOffset, -1, editorState, function, true);
				MathSequence field = new MathSequence();
				function.setArgument(0, field);
				insertReverse(field, -1, removed);
				editorState.resetSelection();
				editorState.setCurrentField(function.getArgument(1));
				editorState.setCurrentOffset(0);
				return;
			}
			ArgumentHelper.passArgument(editorState, function);
		} else if ("^".equals(name)) {
			if (editorState.getSelectionEnd() != null) {
				MathArray array = this.newArray(editorState, 1, '(');
				editorState
						.setCurrentField((MathSequence) array
								.getParent());
				editorState.resetSelection();
				editorState.setCurrentOffset(
						array.getParentIndex() + 1);
				newFunction(editorState, name, initial, square);
				return;
			}
		} else {
			if (editorState.getSelectionEnd() != null) {
				ArrayList<MathComponent> removed = cut(currentField,
						currentOffset, -1, editorState, function, true);
				MathSequence field = new MathSequence();
				function.setArgument(offset, field);
				insertReverse(field, -1, removed);
				editorState.resetSelection();
				editorState.incCurrentOffset();
				return;
			}
		}
		currentOffset = editorState.getCurrentOffset();
		currentField.addArgument(currentOffset, function);
		int select = offset > 0 ? offset : initial;
		if (function.hasChildren()) {
			// set current sequence
			CursorController.firstField(editorState,
					function.getArgument(select));
			editorState.setCurrentOffset(editorState.getCurrentField().size());
		} else {
			editorState.incCurrentOffset();
		}
	}

	public void newScript(EditorState editorState, String script) {
		MathSequence currentField = editorState.getCurrentField();
		if (currentField.size() == 0 && currentField.getParent() instanceof MathFunction
				&& Tag.SUPERSCRIPT == ((MathFunction) currentField.getParent())
						.getName()
				&& "^".equals(script)) {
			return;
		}
		int currentOffset = editorState.getCurrentOffset();

		int offset = currentOffset;
		while (offset > 0 && currentField
				.getArgument(offset - 1) instanceof MathFunction) {

			MathFunction function = (MathFunction) currentField
					.getArgument(offset - 1);
			if (script.equals(function.getName())) {
				editorState.setCurrentField(function.getArgument(0));
				editorState.setCurrentOffset(function.getArgument(0).size());
				return;
			}
			if (Tag.SUPERSCRIPT != function.getName()
					&& Tag.SUBSCRIPT != function.getName()) {
				break;
			}
			offset--;
		}
		offset = currentOffset;
		while (offset < currentField.size()
				&& currentField.getArgument(offset) instanceof MathFunction) {

			MathFunction function = (MathFunction) currentField
					.getArgument(offset);
			if (script.equals(function.getName())) {
				editorState.setCurrentField(function.getArgument(0));
				editorState.setCurrentOffset(0);
				return;
			}
			if (Tag.SUPERSCRIPT != function.getName()
					&& Tag.SUBSCRIPT != function.getName()) {
				break;
			}
			offset++;
		}
		if (currentOffset > 0 && currentField
				.getArgument(currentOffset - 1) instanceof MathFunction) {
			MathFunction function = (MathFunction) currentField
					.getArgument(currentOffset - 1);
			if (Tag.SUPERSCRIPT == function.getName() && "_".equals(script)) {
				currentOffset--;
			}
		}
		if (currentOffset < currentField.size() && currentField
				.getArgument(currentOffset) instanceof MathFunction) {
			MathFunction function = (MathFunction) currentField
					.getArgument(currentOffset);
			if (Tag.SUBSCRIPT == function.getName() && "^".equals(script)) {
				currentOffset++;
			}
		}
		editorState.setCurrentOffset(currentOffset);
		newFunction(editorState, script, false);
	}

	/**
	 * Insert operator.
	 */
	public void newOperator(EditorState editorState, char op) {
		MetaCharacter meta = metaModel.getOperator("" + op);
		newCharacter(editorState, meta);
	}

	/**
	 * Insert symbol.
	 *
	 * @param editorState
	 *            state
	 * @param sy
	 *            char
	 */
	public void newSymbol(EditorState editorState, char sy) {
		MetaCharacter meta = metaModel.getSymbol("" + sy);
		newCharacter(editorState, meta);
	}

	/**
	 * Insert character.
	 *
	 * @param editorState
	 *            state
	 * @param ch
	 *            char
	 */
	public void newCharacter(EditorState editorState, char ch) {
		MetaCharacter meta = metaModel.getCharacter("" + ch);
		newCharacter(editorState, meta);
	}

	/**
	 * Insert character.
	 *
	 * @param editorState
	 *            current state
	 * @param meta
	 *            character
	 */
	public void newCharacter(EditorState editorState,
			MetaCharacter meta) {
		MathComponent last = editorState.getCurrentField()
				.getArgument(editorState.getCurrentOffset() - 1);

		if (last instanceof MathCharacter) {
			MetaCharacter merge = metaModel
					.merge(((MathCharacter) last).toString(), meta);
			if (merge != null) {
				editorState.getCurrentField().setArgument(
						editorState.getCurrentOffset() - 1,
						new MathCharacter(merge));
				return;
			}
		}
		editorState.addArgument(new MathCharacter(meta));

	}

	/**
	 * Insert field.
	 *
	 * @param editorState
	 *            current state
	 * @param ch
	 *            bracket
	 */
	public void endField(EditorState editorState, char ch) {
		MathSequence currentField = editorState.getCurrentField();
		int currentOffset = editorState.getCurrentOffset();
		// first array specific ...
		if (currentField.getParent() instanceof MathArray) {
			MathArray parent = (MathArray) currentField.getParent();

			// if ',' typed within 1DArray or Vector ... add new field
			if (ch == parent.getFieldKey()
					&& (parent.is1DArray() || parent.isVector())) {

				int index = currentField.getParentIndex();
				MathSequence field = new MathSequence();
				parent.addArgument(index + 1, field);
				while (currentField.size() > currentOffset) {
					MathComponent component = currentField
							.getArgument(currentOffset);
					currentField.delArgument(currentOffset);
					field.addArgument(field.size(), component);
				}
				currentField = field;
				currentOffset = 0;

				// if ',' typed at the end of intermediate field of 2DArray or
				// Matrix ... move to next field
			} else if (ch == parent.getFieldKey()
					&& currentOffset == currentField.size()
					&& parent.size() > currentField.getParentIndex() + 1
					&& (currentField.getParentIndex() + 1)
							% parent.columns() != 0) {

				currentField = parent
						.getArgument(currentField.getParentIndex() + 1);
				currentOffset = 0;

				// if ';' typed at the end of last field ... add new row
			} else if (ch == parent.getRowKey()
					&& currentOffset == currentField.size()
					&& parent.size() == currentField.getParentIndex() + 1) {

				parent.addRow();
				currentField = parent
						.getArgument(parent.size() - parent.columns());
				currentOffset = 0;

				// if ';' typed at the end of (not last) row ... move to next
				// field
			} else if (ch == parent.getRowKey()
					&& currentOffset == currentField.size()
					&& (currentField.getParentIndex() + 1)
							% parent.columns() == 0) {

				currentField = parent
						.getArgument(currentField.getParentIndex() + 1);
				currentOffset = 0;

				// if ']' '}' typed at the end of last field ... move out of
				// array
			} else if (ch == parent.getCloseKey() && parent.isArray()) {

				ArrayList<MathComponent> removed = cut(currentField,
						currentOffset);
				insertReverse(parent.getParent(), parent.getParentIndex(),
						removed);

				currentOffset = parent.getParentIndex() + 1;
				currentField = (MathSequence) parent.getParent();
			} else if ((ch == parent.getCloseKey() && parent.isMatrix())
					&& parent.size() == currentField.getParentIndex() + 1
					&& currentOffset == currentField.size()) {

				currentOffset = parent.getParentIndex() + 1;
				currentField = (MathSequence) parent.getParent();
			}

			// now functions, braces, apostrophes ...
		} else if (currentField.getParent() != null) {
			MathContainer parent = currentField.getParent();

			// if ',' typed at the end of intermediate field of function ...
			// move to next field
			if (ch == ',' && currentOffset == currentField.size()
					&& parent instanceof MathFunction
					&& parent.size() > currentField.getParentIndex() + 1) {

				currentField = (MathSequence) parent
						.getArgument(currentField.getParentIndex() + 1);
				currentOffset = 0;

				// if ')' typed at the end of last field of function ... move
				// after closing character
			} else if (currentOffset == currentField.size()
					&& parent instanceof MathFunction
					&& ch == ((MathFunction) parent).getClosingBracket()
							.charAt(0)
					&& parent.size() == currentField.getParentIndex() + 1) {

				currentOffset = parent.getParentIndex() + 1;
				currentField = (MathSequence) parent.getParent();

				// if ')' typed at the end of last field of braces ... move
				// after closing character
			} else {
				if (ch == ',') {
					newCharacter(editorState, ch);
					// return so that the old current field and offset are not
					// set
					return;
				}
			}

			// topmost container last ...
		} else {
			// if ';' typed and at the top level ... insert delimiter char
			if (ch == DELIMITER_KEY || ch == ',') {
				newCharacter(editorState, ch);
				// return so that the old current field and offset are not set
				return;
				// update();
			}
		}
		editorState.setCurrentField(currentField);
		editorState.setCurrentOffset(currentOffset);
	}

	private static void insertReverse(MathContainer parent, int parentIndex,
			ArrayList<MathComponent> removed) {
		for (int j = removed.size() - 1; j >= 0; j--) {
			MathComponent o = removed.get(j);
			int idx = parentIndex + (removed.size() - j);
			parent.addArgument(idx, o);
		}

	}

	private static ArrayList<MathComponent> cut(MathSequence currentField,
			int from, int to, EditorState st, MathComponent array,
			boolean rec) {

		int end = to < 0 ? currentField.size() - 1 : to;
		int start = from;

		if (st.getCurrentField() == currentField
				&& st.getSelectionEnd() != null) {
			// the root is selected
			if (st.getSelectionEnd().getParent() == null && rec) {
				return cut((MathSequence) st.getSelectionEnd(), 0, -1, st,
						array, false);
			}
			// deep selection, e.g. a fraction
			if (st.getSelectionEnd().getParent() != currentField && rec) {
				return cut((MathSequence) st.getSelectionEnd().getParent(),
						st.getSelectionStart().getParentIndex(),
						st.getSelectionEnd().getParentIndex(), st, array,
						false);
			}
			// simple case: a part of sequence is selected
			end = currentField.indexOf(st.getSelectionEnd());
			start = currentField.indexOf(st.getSelectionStart());
			if (end < 0 || start < 0) {
				end = currentField.size() - 1;
				start = 0;

			}

		}
		ArrayList<MathComponent> removed = new ArrayList<MathComponent>();
		for (int i = end; i >= start; i--) {
			removed.add(currentField.getArgument(i));
			currentField.removeArgument(i);
		}
		currentField.addArgument(start, array);
		return removed;
	}

	private static ArrayList<MathComponent> cut(MathSequence currentField,
			int currentOffset) {
		ArrayList<MathComponent> removed = new ArrayList<MathComponent>();

		for (int i = currentField.size() - 1; i >= currentOffset; i--) {
			removed.add(currentField.getArgument(i));
			currentField.removeArgument(i);
		}

		return removed;
	}

	/**
	 * Insert symbol.
	 */
	public void escSymbol(EditorState editorState) {
		editorState.getRootComponent().clearArguments();
		editorState.setCurrentField(editorState.getRootComponent());
		editorState.setCurrentOffset(0);
		editorState.resetSelection();
		// String name = ArgumentHelper.readCharacters(editorState);
		// while (name.length() > 0) {
		// if (metaModel.isSymbol(name)) {
		// delCharacters(editorState, name.length());
		// MetaCharacter meta = metaModel.getSymbol(name);
		// newCharacter(editorState, meta);
		// break;
		//
		// } else if (metaModel.isOperator(name)) {
		// delCharacters(editorState, name.length());
		// MetaCharacter meta = metaModel.getOperator(name);
		// newCharacter(editorState, meta);
		// break;
		//
		// } else {
		// name = name.substring(1, name.length());
		// }
		// }
	}

	/**
	 * Backspace to remove container
	 *
	 * @param editorState
	 *            current state
	 */
	public void bkspContainer(EditorState editorState) {
		MathSequence currentField = editorState.getCurrentField();

		// if parent is function (cursor is at the beginning of the field)
		if (currentField.getParent() instanceof MathFunction) {
			MathFunction parent = (MathFunction) currentField.getParent();

			// fraction has operator like behavior
			if (Tag.FRAC == parent.getName()) {

				// if second operand is empty sequence
				if (currentField.getParentIndex() == 1
						&& currentField.size() == 0) {
					int size = parent.getArgument(0).size();
					delContainer(editorState, parent, parent.getArgument(0));
					// move after included characters
					editorState.addCurrentOffset(size);
					// if first operand is empty sequence
				} else if (currentField.getParentIndex() == 1
						&& parent.getArgument(0).size() == 0) {
					delContainer(editorState, parent, currentField);
				}

			} else if (metaModel.isGeneral(parent.getName())) {
				if (currentField.getParentIndex() == parent.getInsertIndex()) {
					delContainer(editorState, parent, currentField);
				}

				// not a fraction, and cursor is right after the sign
			} else {
				if (currentField.getParentIndex() == 1) {
					int len = parent.getArgument(0).size();
					delContainer(editorState, parent, parent.getArgument(0));
					editorState
							.setCurrentOffset(len);
				}
			}

			// if parent are empty array
		} else if (currentField.getParent() instanceof MathArray
				&& currentField.getParent().size() == 1) {

			MathArray parent = (MathArray) currentField.getParent();
			delContainer(editorState, parent, parent.getArgument(0));

			// if parent is 1DArray or Vector and cursor is at the beginning of
			// intermediate the field
		} else if (currentField.getParent() instanceof MathArray
				&& (((MathArray) currentField.getParent()).is1DArray()
						|| ((MathArray) currentField.getParent()).isVector())
				&& currentField.getParentIndex() > 0) {

			int index = currentField.getParentIndex();
			MathArray parent = (MathArray) currentField.getParent();
			MathSequence field = parent.getArgument(index - 1);
			int size = field.size();
			editorState.setCurrentOffset(0);
			while (currentField.size() > 0) {

				MathComponent component = currentField.getArgument(0);
				currentField.delArgument(0);
				field.addArgument(field.size(), component);
			}
			parent.delArgument(index);
			editorState.setCurrentField(field);
			editorState.setCurrentOffset(size);
		}

		// we stop here for now
	}

	public static void delContainer(EditorState editorState) {
		MathSequence currentField = editorState.getCurrentField();

		// if parent is function (cursor is at the end of the field)
		if (currentField.getParent() instanceof MathFunction) {
			MathFunction parent = (MathFunction) currentField.getParent();

			// fraction has operator like behavior
			if ("frac".equals(parent.getName())) {

				// first operand is current, second operand is empty sequence
				if (currentField.getParentIndex() == 0
						&& parent.getArgument(1).size() == 0) {
					int size = parent.getArgument(0).size();
					delContainer(editorState, parent, currentField);
					// move after included characters
					editorState.addCurrentOffset(size);

					// first operand is current, and first operand is empty
					// sequence
				} else if (currentField.getParentIndex() == 0
						&& (currentField).size() == 0) {
					delContainer(editorState, parent, parent.getArgument(1));
				}
			}

			// if parent are empty braces
		} else if (currentField.getParent() instanceof MathArray
				&& currentField.getParent().size() == 1
				&& currentField.size() == 0) {
			MathArray parent = (MathArray) currentField.getParent();
			int size = parent.getArgument(0).size();
			delContainer(editorState, parent, parent.getArgument(0));
			// move after included characters
			editorState.addCurrentOffset(size);

			// if parent is 1DArray or Vector and cursor is at the end of the
			// field
		} else if (currentField.getParent() instanceof MathArray
				&& (((MathArray) currentField.getParent()).is1DArray()
						|| ((MathArray) currentField.getParent()).isVector())
				&& currentField.getParentIndex() + 1 < currentField.getParent()
						.size()) {

			int index = currentField.getParentIndex();
			MathArray parent = (MathArray) currentField.getParent();
			MathSequence field = parent.getArgument(index + 1);
			int size = currentField.size();
			while (currentField.size() > 0) {

				MathComponent component = currentField.getArgument(0);
				currentField.delArgument(0);
				field.addArgument(field.size(), component);
			}
			parent.delArgument(index);
			editorState.setCurrentField(field);
			editorState.setCurrentOffset(size);
		}

		// we stop here for now
	}

	public void bkspCharacter(EditorState editorState) {
		int currentOffset = editorState.getCurrentOffset();
		if (currentOffset > 0) {
			if (editorState.getCurrentField()
					.getArgument(currentOffset - 1) instanceof MathArray) {

				MathArray parent = (MathArray) editorState.getCurrentField()
						.getArgument(currentOffset - 1);

				extendBrackets(parent, editorState);
			} else {
				editorState.getCurrentField().delArgument(currentOffset - 1);
				editorState.decCurrentOffset();
			}
		} else {
			bkspContainer(editorState);
		}
	}

	private static void extendBrackets(MathArray array,
			EditorState editorState) {
		int currentOffset = array.getParentIndex() + 1;
		MathContainer currentField = array.getParent();
		MathSequence lastArg = array.getArgument(array.size() - 1);
		int oldSize = lastArg.size();
		while (currentField.size() > currentOffset) {

			MathComponent component = currentField.getArgument(currentOffset);
			currentField.delArgument(currentOffset);
			lastArg.addArgument(lastArg.size(), component);
		}
		editorState.setCurrentField(lastArg);
		editorState.setCurrentOffset(oldSize);

	}

	public void delCharacter(EditorState editorState) {
		int currentOffset = editorState.getCurrentOffset();
		MathSequence currentField = editorState.getCurrentField();
		if (currentOffset < currentField.size()) {

				CursorController.nextCharacter(editorState);
				bkspCharacter(editorState);

		} else {
			if (currentField.getParent() instanceof MathArray) {
				extendBrackets((MathArray) currentField.getParent(),
						editorState);
			} else {
				delContainer(editorState);
			}
		}
	}

	private static void delContainer(EditorState editorState,
			MathContainer container, MathSequence operand) {
		if (container.getParent() instanceof MathSequence) {
			// when parent is sequence
			MathSequence parent = (MathSequence) container.getParent();
			int offset = container.getParentIndex();
			// delete container
			parent.delArgument(container.getParentIndex());
			// add content of operand
			while (operand.size() > 0) {
				MathComponent element = operand.getArgument(operand.size() - 1);
				operand.delArgument(operand.size() - 1);
				parent.addArgument(offset, element);
			}
			editorState.setCurrentField(parent);
			editorState.setCurrentOffset(offset);
		}
	}

	private static void delCharacters(EditorState editorState, int length0) {
		int currentOffset = editorState.getCurrentOffset();
		MathSequence currentField = editorState.getCurrentField();
		int length = length0;
		while (length > 0 && currentOffset > 0 && currentField
				.getArgument(currentOffset - 1) instanceof MathCharacter) {

			MathCharacter character = (MathCharacter) currentField
					.getArgument(currentOffset - 1);
			if (character.isOperator() || character.isSymbol()) {
				break;
			}
			currentField.delArgument(currentOffset - 1);
			currentOffset--;
			length--;
		}
		editorState.setCurrentOffset(currentOffset);
	}

	/**
	 * remove characters before and after cursor
	 *
	 * @param editorState
	 * @param lengthBeforeCursor
	 * @param lengthAfterCursor
	 */
	public void removeCharacters(EditorState editorState,
			int lengthBeforeCursor, int lengthAfterCursor) {
		if (lengthBeforeCursor == 0 && lengthAfterCursor == 0) {
			return; // nothing to delete
		}
		MathSequence seq = editorState.getCurrentField();
		for (int i = 0; i < lengthBeforeCursor; i++) {
			editorState.decCurrentOffset();
			if (editorState.getCurrentOffset() < 0
					|| editorState.getCurrentOffset() >= seq.size()) {
				bkspContainer(editorState);
				return;
			}
			seq.delArgument(editorState.getCurrentOffset());
		}
		for (int i = 0; i < lengthAfterCursor; i++) {
			seq.delArgument(editorState.getCurrentOffset());
		}
	}

	/**
	 * set ret to characters (no digit) around cursor
	 *
	 * @param ret
	 * @return word length before cursor
	 */
	public static int getWordAroundCursor(EditorState editorState,
			StringBuilder ret) {
		int pos = editorState.getCurrentOffset();
		MathSequence seq = editorState.getCurrentField();

		StringBuilder before = new StringBuilder();
		int i;
		for (i = pos - 1; i >= 0; i--) {
			try {
				before.append(getLetter(seq.getArgument(i)));
			} catch (Exception e) {
				break;
			}
		}
		int lengthBefore = pos - i - 1;

		StringBuilder after = new StringBuilder();
		for (i = pos; i < seq.size(); i++) {
			try {
				after.append(getLetter(seq.getArgument(i)));
			} catch (Exception e) {
				break;
			}
		}
		before.reverse();
		ret.append(before);
		ret.append(after);

		return lengthBefore;

	}

	public static boolean deleteSelection(EditorState editorState) {
		boolean nonempty = false;
		if (editorState.getSelectionStart() != null) {
			MathContainer parent = editorState.getSelectionStart().getParent();
			int end, start;
			if (parent == null) {
				// all the formula is selected
				parent = editorState.getRootComponent();
				start = 0;
				end = parent.size() - 1;
			} else {
				end = parent.indexOf(editorState.getSelectionEnd());
				start = parent.indexOf(editorState.getSelectionStart());
			}
			if (end >= 0 && start >= 0) {
				for (int i = end; i >= start; i--) {
					parent.delArgument(i);
					nonempty = true;
				}

				editorState.setCurrentOffset(start);
				// in most cases no impact; goes to parent node when whole
				// formula selected
				if (parent instanceof MathSequence) {
					editorState.setCurrentField((MathSequence) parent);
				}
			}

		}
		editorState.resetSelection();
		return nonempty;

	}

	/**
	 * @param editorState
	 *            current state
	 * @param ch
	 *            single char
	 * @return whether it was handled
	 */
	public boolean handleChar(EditorState editorState, char ch) {
		boolean handled = false;
		boolean allowFrac = createFrac && !editorState.isInsideQuotes();
		// backspace, delete and escape are handled for key down
		if (ch == JavaKeyCodes.VK_BACK_SPACE || ch == JavaKeyCodes.VK_DELETE
				|| ch == JavaKeyCodes.VK_ESCAPE) {
			return true;
		}
		if (ch != '(' && ch != '{' && ch != '[' && ch != '/' && ch != '|'
				&& ch != Unicode.LFLOOR && ch != Unicode.LCEIL) {
			deleteSelection(editorState);
		}
		MetaModel meta = editorState.getMetaModel();

		// special case: '|' to end abs() block
		MathContainer parent = editorState.getCurrentField().getParent();
		if (parent instanceof MathArray
				&& editorState.getSelectionStart() == null) {

			if (ch == '|' && ((MathArray) parent).getCloseKey() == '|') {

				MathSequence currentField = editorState.getCurrentField();

				int offset = editorState.getCurrentOffset();

				MathComponent nextArg = currentField.getArgument(offset);
				MathComponent prevArg = currentField.getArgument(offset - 1);
				
				// check for eg * + -
				boolean isOperation = mathField.getMetaModel()
						.isOperator(prevArg + "");

				// make sure | acts as closing | only at end of block
				// but not after eg plus eg |x+|
				if (nextArg == null && !isOperation) {
					endField(editorState, ch);
					handled = true;
				}
			}
		}

		if (!handled) {
			if (meta.isArrayCloseKey(ch)) {
				endField(editorState, ch);
				handled = true;
			} else if (meta.isFunctionOpenKey(ch)) {
				newBraces(editorState, ch);
				handled = true;
			} else if (allowFrac && ch == '^') {
				newScript(editorState, "^");
				handled = true;
			} else if (allowFrac && ch == '_') {
				newScript(editorState, "_");
				handled = true;
			} else if (allowFrac && ch == '/') {
				newFunction(editorState, "frac", 1, false);
				handled = true;
			} else if (ch == Unicode.SQUARE_ROOT) {
				newFunction(editorState, "sqrt", 0, false);
				handled = true;
			} else if (meta.isArrayOpenKey(ch)) {
				newArray(editorState, 1, ch);
				handled = true;
			} else if (ch == Unicode.MULTIPLY || ch == Unicode.CENTER_DOT
					|| ch == Unicode.BULLET) {
				newOperator(editorState, '*');
				handled = true;
			} else if (ch == ',' && allowFrac) {
				comma(editorState);
				handled = true;
			} else if (meta.isOperator("" + ch)) {
				newOperator(editorState, ch);
				handled = true;
			} else if (ch == 3 || ch == 22) {
				// invisible characters on MacOS
				handled = true;
			} else if (meta.isSymbol("" + ch)) {
				newSymbol(editorState, ch);
				handled = true;
			} else if (meta.isCharacter("" + ch)) {
				newCharacter(editorState, ch);
				handled = true;
			}
		}
		return handled;
	}

	private void comma(EditorState editorState) {
		if (trySelectNext(editorState)) {
			return;
		}

		newOperator(editorState, ',');

	}

	public static boolean trySelectNext(EditorState editorState) {
		int idx = editorState.getCurrentOffset();
		if (editorState.getSelectionEnd() != null) {
			idx = editorState.getSelectionEnd().getParentIndex() + 1;
		}
		MathSequence field = editorState.getCurrentField();
		if (field.getArgument(idx) instanceof MathCharacter
				&& ",".equals(field.getArgument(idx).toString())
				&& doSelectNext(field, editorState, idx + 1)) {
			return true;
		}
		return false;
	}

	public static boolean trySelectFirst(EditorState editorState) {
		int idx = editorState.getCurrentOffset();
		if (editorState.getSelectionEnd() != null) {
			idx = editorState.getSelectionEnd().getParentIndex() + 1;
		}

		MathSequence field = editorState.getCurrentField();
		if (idx == field.size() - 1 && doSelectNext(field, editorState, 0)) {
			return true;
		}
		return false;
	}

	/**
	 * @param args
	 *            text of the form &lt;arg1&gt;&lt;arg2&gt;
	 * @param state
	 *            current state
	 * @param offset
	 *            where to start looking
	 * @return whether successfully selected
	 */
	public static boolean doSelectNext(MathSequence args, EditorState state,
			int offset) {
		int endchar = -1;
		for (int i = offset + 1; i < args.size(); i++) {
			if (args.getArgument(i) instanceof MathCharacter
					&& ((MathCharacter) args.getArgument(i))
							.getUnicode() == '>') {
				endchar = i;
				if (i < args.size() - 1
						&& args.getArgument(i + 1) instanceof MathCharacter
						&& " ".equals(args.getArgument(i + 1).toString())) {
					endchar++;
				}
				break;
			}
		}
		if (endchar > 0) {
			state.setCurrentField(args);
			state.setSelectionStart(args.getArgument(offset));
			state.setSelectionEnd(args.getArgument(endchar));
			state.setCurrentOffset(endchar);
			return true;
		}
		return false;
	}

	public void paste() {
		if (mathField != null) {
			mathField.paste();
		}
	}

	public void copy() {
		if (mathField != null) {
			mathField.copy();
		}
	}

	public void handleTab() {
		if (mathField != null) {
			mathField.tab();
		}
	}
	
	public static MathSequence getSelectionText(EditorState editorState) {
		if (editorState.getSelectionStart() != null) {
			MathContainer parent = editorState.getSelectionStart().getParent();
			int end, start;
			if (parent == null) {
				// all the formula is selected
				return editorState.getRootComponent();

			}
			MathSequence seq = new MathSequence();
			end = parent.indexOf(editorState.getSelectionEnd());
			start = parent.indexOf(editorState.getSelectionStart());
			if (end >= 0 && start >= 0) {
				for (int i = start; i <= end; i++) {
					seq.addArgument(parent.getArgument(i).copy());
				}

				// editorState.setCurrentOffset(start);
			}
			return seq;
		}
		// editorState.resetSelection();
		return null;
	}


}
