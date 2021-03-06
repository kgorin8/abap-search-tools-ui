package com.devepos.adt.saat.internal.cdsanalysis.ui;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.StatusDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.Text;

import com.devepos.adt.saat.internal.SearchAndAnalysisPlugin;
import com.devepos.adt.saat.internal.messages.Messages;
import com.devepos.adt.saat.internal.preferences.IPreferences;
import com.devepos.adt.saat.internal.util.IImages;

/**
 * This dialog is for managing the currently open analysis pages
 *
 * @author stockbal
 */
public class ManageCdsAnalysesDialog extends StatusDialog {

	private static final int REMOVE_ID = IDialogConstants.CLIENT_ID + 1;
	private static final int WIDTH_IN_CHARACTERS = 55;
	private static final int BUTTON_CHAR_WIDTH = 15;

	private final List<CdsAnalysis> input;
	private final List<CdsAnalysis> removedEntries;

	private TableViewer viewer;
	private Button removeButton;
	private final IPreferenceStore prefStore;
	private Text maxHistorySizeText;
	private CdsAnalysis result;

	public ManageCdsAnalysesDialog(final List<CdsAnalysis> analyses, final Shell parent) {
		super(parent);
		this.input = analyses;
		setTitle(Messages.CdsAnalysis_ManageCdsAnalysisHistoryTitle_xmsg);
		this.removedEntries = new ArrayList<>();
		this.prefStore = SearchAndAnalysisPlugin.getDefault().getPreferenceStore();
		setHelpAvailable(false);
	}

	@Override
	public void create() {
		super.create();

		if (this.input != null && !this.input.isEmpty()) {
			this.viewer.setSelection(new StructuredSelection(this.input.get(0)));
		}

		validateDialogState();
	}

	@Override
	protected boolean isResizable() {
		return true;
	}

	public List<CdsAnalysis> getDeletedAnalyses() {
		return this.removedEntries;
	}

	@Override
	protected IDialogSettings getDialogBoundsSettings() {
		return SearchAndAnalysisPlugin.getDefault().getDialogSettingsSection("DialogBounds_ManageCdsAnalysisHistoryDialog"); //$NON-NLS-1$
	}

	@Override
	protected int getDialogBoundsStrategy() {
		return DIALOG_PERSISTSIZE;
	}

	protected Label createMessageArea(final Composite composite) {
		final Composite parent = new Composite(composite, SWT.NONE);
		GridLayoutFactory.swtDefaults().numColumns(2).margins(0, 0).applyTo(parent);
		GridDataFactory.fillDefaults().span(2, 1).applyTo(parent);

		final Label label = new Label(parent, SWT.WRAP);
		label.setText(Messages.CdsAnalysis_ManageCdsAnalysisPagesTableHeader_xfld);
		GridDataFactory.fillDefaults().span(2, 1).applyTo(label);

		applyDialogFont(label);
		return label;
	}

	/**
	 * Returns the selected analysis
	 *
	 * @return the selected analysis
	 */
	public CdsAnalysis getSelectedAnalysis() {
		return this.result;
	}

	/*
	 * Overrides method from Dialog
	 */
	@Override
	protected Control createDialogArea(final Composite container) {
		final Composite ancestor = (Composite) super.createDialogArea(container);
		// image has to be set at this position as it has no effect in constructor
		setImage(SearchAndAnalysisPlugin.getDefault().getImage(IImages.HISTORY_LIST));

		createMessageArea(ancestor);

		final Composite parent = new Composite(ancestor, SWT.NONE);

		GridLayoutFactory.swtDefaults().numColumns(2).margins(0, 0).applyTo(parent);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(parent);

		this.viewer = new TableViewer(parent, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER | SWT.FULL_SELECTION);
		this.viewer.setContentProvider(new ArrayContentProvider());
		this.viewer.setLabelProvider(new CdsAnalysisLabelProvider());
		this.viewer.addSelectionChangedListener(event -> validateDialogState());

		final Table table = this.viewer.getTable();
		table.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseDoubleClick(final MouseEvent e) {
				buttonPressed(IDialogConstants.OPEN_ID);
			}
		});
		GridDataFactory.fillDefaults()
			.span(1, 2)
			.hint(convertWidthInCharsToPixels(WIDTH_IN_CHARACTERS), convertHeightInCharsToPixels(15))
			.grab(true, true)
			.applyTo(table);

		this.removeButton = new Button(parent, SWT.PUSH);
		this.removeButton.setText(Messages.SearchHistorySelectionDialog_DeleteHistoryEntry_xbut);
		this.removeButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent event) {
				buttonPressed(REMOVE_ID);
			}
		});
		GridDataFactory.fillDefaults()
			.align(SWT.BEGINNING, SWT.BEGINNING)
			.hint(convertWidthInCharsToPixels(BUTTON_CHAR_WIDTH), SWT.DEFAULT)
			.applyTo(this.removeButton);

		final Composite maxHistoryComposite = new Composite(ancestor, SWT.NONE);
		GridLayoutFactory.swtDefaults().numColumns(3).applyTo(maxHistoryComposite);
		GridDataFactory.fillDefaults().applyTo(maxHistoryComposite);

		final Label maxHistoryLabel = new Label(maxHistoryComposite, SWT.NONE);
		maxHistoryLabel.setText(Messages.CdsAnalysis_MaxHistoryNumberText_xfld + " "); //$NON-NLS-1$
		this.maxHistorySizeText = new Text(maxHistoryComposite, SWT.BORDER | SWT.LEFT);
		this.maxHistorySizeText.setTextLimit(2);
		this.maxHistorySizeText.setText(String.valueOf(this.prefStore.getInt(IPreferences.MAX_CDS_ANALYZER_HISTORY)));
		this.maxHistorySizeText.addModifyListener((e) -> {
			validateDialogState();
		});
		GridDataFactory.fillDefaults().hint(convertWidthInCharsToPixels(4), SWT.DEFAULT).applyTo(this.maxHistorySizeText);

		final Label maxHistoryLabel2 = new Label(maxHistoryComposite, SWT.NONE);
		maxHistoryLabel2.setText(" " + Messages.CdsAnalyzer_MaxHistoryNumberText2_fld); //$NON-NLS-1$

		applyDialogFont(ancestor);

		// set input & selections last, so all the widgets are created.
		this.viewer.setInput(this.input);
		this.viewer.getTable().setFocus();

		return ancestor;
	}

	private boolean validateHistorySize() {
		IStatus status = null;
		try {
			final String historySize = this.maxHistorySizeText.getText();
			final int size = Integer.parseInt(historySize);
			if (size < 1 || size > 30) {
				status = new Status(IStatus.ERROR, SearchAndAnalysisPlugin.PLUGIN_ID, IStatus.ERROR,
					Messages.CdsAnalysis_HistoryNumberInvalid_xmsg, null);
			}
		} catch (final NumberFormatException e) {
			status = new Status(IStatus.ERROR, SearchAndAnalysisPlugin.PLUGIN_ID, IStatus.ERROR,
				Messages.CdsAnalysis_HistoryNumberInvalid_xmsg, null);
		}
		if (status == null) {
			status = Status.OK_STATUS;
		}

		updateStatus(status);
		return !status.matches(IStatus.ERROR);
	}

	protected final void validateDialogState() {
		final IStructuredSelection sel = this.viewer.getStructuredSelection();
		final int elementsSelected = sel.toList().size();

		final boolean historySizeValid = validateHistorySize();

		this.removeButton.setEnabled(elementsSelected > 0);
		final Button openButton = getButton(IDialogConstants.OPEN_ID);
		if (openButton != null) {
			openButton.setEnabled(elementsSelected == 1 && historySizeValid);
		}
		final Button okButton = getButton(IDialogConstants.OK_ID);
		if (okButton != null) {
			okButton.setEnabled(historySizeValid);
		}
	}

	@Override
	protected void createButtonsForButtonBar(final Composite parent) {
		createButton(parent, IDialogConstants.OPEN_ID, IDialogConstants.OPEN_LABEL, true);
		createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, false);
		createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
	}

	@Override
	protected void buttonPressed(final int buttonId) {
		if (buttonId == REMOVE_ID) {
			final IStructuredSelection selection = this.viewer.getStructuredSelection();
			final Iterator<?> analysis = selection.iterator();
			while (analysis.hasNext()) {
				final CdsAnalysis current = (CdsAnalysis) analysis.next();
				this.removedEntries.add(current);
				this.input.remove(current);
				this.viewer.remove(current);
			}
			if (this.viewer.getSelection().isEmpty() && !this.input.isEmpty()) {
				this.viewer.setSelection(new StructuredSelection(this.input.get(0)));
			}
			return;
		} else if (buttonId == IDialogConstants.OPEN_ID || buttonId == IDialogConstants.OK_ID) {
			// Build a list of selected children.
			final ISelection selection = this.viewer.getSelection();
			if (selection != null && selection instanceof IStructuredSelection) {
				this.result = (CdsAnalysis) this.viewer.getStructuredSelection().getFirstElement();
			}
			this.prefStore.setValue(IPreferences.MAX_CDS_ANALYZER_HISTORY, this.maxHistorySizeText.getText());
			okPressed();
			return;
		}
		super.buttonPressed(buttonId);
	}

	private static final class CdsAnalysisLabelProvider extends LabelProvider {

		@Override
		public String getText(final Object element) {
			return ((CdsAnalysis) element).getLabel();
		}

		@Override
		public Image getImage(final Object element) {
			return ((CdsAnalysis) element).getImage();
		}
	}
}
