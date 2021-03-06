package com.devepos.adt.saat.internal.cdsanalysis.ui;

import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.JFacePreferences;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.viewers.DelegatingStyledCellLabelProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.PlatformUI;

import com.devepos.adt.saat.internal.ICommandConstants;
import com.devepos.adt.saat.internal.IContextMenuConstants;
import com.devepos.adt.saat.internal.SearchAndAnalysisPlugin;
import com.devepos.adt.saat.internal.cdsanalysis.ICdsAnalysisPreferences;
import com.devepos.adt.saat.internal.menu.MenuItemFactory;
import com.devepos.adt.saat.internal.messages.Messages;
import com.devepos.adt.saat.internal.search.ObjectSearchUriDiscovery;
import com.devepos.adt.saat.internal.search.QueryParameterName;
import com.devepos.adt.saat.internal.search.SearchType;
import com.devepos.adt.saat.internal.ui.TreeViewUiState;
import com.devepos.adt.saat.internal.ui.ViewUiState;
import com.devepos.adt.saat.internal.util.CommandPossibleChecker;
import com.devepos.adt.saat.internal.util.IImages;
import com.devepos.adt.tools.base.destinations.IDestinationProvider;
import com.devepos.adt.tools.base.elementinfo.IAdtObjectReferenceElementInfo;
import com.devepos.adt.tools.base.ui.IGeneralContextMenuConstants;
import com.devepos.adt.tools.base.ui.StylerFactory;
import com.devepos.adt.tools.base.ui.action.PreferenceToggleAction;
import com.devepos.adt.tools.base.ui.tree.ICollectionTreeNode;
import com.devepos.adt.tools.base.ui.tree.ILazyLoadingListener;
import com.devepos.adt.tools.base.ui.tree.IStyledTreeNode;
import com.devepos.adt.tools.base.ui.tree.ITreeNode;
import com.devepos.adt.tools.base.ui.tree.LazyLoadingTreeContentProvider;
import com.devepos.adt.tools.base.ui.tree.LoadingTreeItemsNode;

/**
 * Where-Used page of CDS Analysis page
 *
 * @see    {@link CdsAnalyzerPage}
 * @author stockbal
 */
public class WhereUsedInCdsAnalysisView extends CdsAnalysisPage<WhereUsedInCdsAnalysis> {
	private final ILazyLoadingListener lazyLoadingListener;
	private PreferenceToggleAction showFromUses;
	private PreferenceToggleAction showAssocUses;
	private PreferenceToggleAction releasedUsagesOnly;
	private PreferenceToggleAction localAssociationsOnly;
	private boolean isLocalAssocOnlyFeatureAvailable;
	private static final String USES_IN_SELECT_PREF_KEY = "com.devepos.adt.saat.whereusedincds.showReferencesInSelectPartOfCds"; //$NON-NLS-1$
	private static final String USES_IN_ASSOC_PREF_KEY = "com.devepos.adt.saat.whereusedincds.showReferencesInAssocPartOfCds"; //$NON-NLS-1$
	private static final String LOCAL_ASSOCIATIONS_ONLY_PREF_KEY = "com.devepos.adt.saat.whereusedincds.onlyLocalDefinedAssociation"; //$NON-NLS-1$
	private final IPropertyChangeListener propertyChangeListener;

	public WhereUsedInCdsAnalysisView(final CdsAnalysisView parentView) {
		super(parentView);
		this.lazyLoadingListener = count -> {
			PlatformUI.getWorkbench().getDisplay().asyncExec(() -> {
				parentView.updateLabel();
			});
		};

		this.propertyChangeListener = event -> {
			final String propertyName = event.getProperty();
			final boolean showFromUsesChanged = USES_IN_SELECT_PREF_KEY.equals(propertyName);
			final boolean showAssocUsesChanged = USES_IN_ASSOC_PREF_KEY.equals(propertyName);
			final boolean localAssocsOnlyChanged = LOCAL_ASSOCIATIONS_ONLY_PREF_KEY.equals(propertyName);
			final boolean releasedUsagesOnlyChanged = ICdsAnalysisPreferences.WHERE_USED_ONLY_RELEASED_USAGES
				.equals(propertyName);

			if (!showFromUsesChanged && !showAssocUsesChanged && !releasedUsagesOnlyChanged
				&& !localAssocsOnlyChanged) {
				return;
			}
			// trigger refresh of where used analysis
			this.analysisResult.updateWhereUsedProvider(this.showFromUses.isChecked(), this.showAssocUses.isChecked());
			this.analysisResult.setLocalAssociationsOnly(this.localAssociationsOnly.isChecked());
			if (localAssocsOnlyChanged) {
				if (this.showAssocUses.isChecked()) {
					refreshAnalysis();
				}
			} else {
				refreshAnalysis();
			}
		};
		SearchAndAnalysisPlugin.getDefault()
			.getPreferenceStore()
			.addPropertyChangeListener(this.propertyChangeListener);
	}

	@Override
	public void dispose() {
		super.dispose();
		SearchAndAnalysisPlugin.getDefault()
			.getPreferenceStore()
			.removePropertyChangeListener(this.propertyChangeListener);
	}

	@Override
	protected void createActions() {
		super.createActions();
		final IPreferenceStore prefStore = SearchAndAnalysisPlugin.getDefault().getPreferenceStore();
		this.showFromUses = new PreferenceToggleAction(
			Messages.WhereUsedInCdsAnalysisView_ShowUsesInSelectPartAction_xmit,
			SearchAndAnalysisPlugin.getDefault().getImageDescriptor(IImages.DATA_SOURCE), USES_IN_SELECT_PREF_KEY, true,
			prefStore);
		this.showAssocUses = new PreferenceToggleAction(
			Messages.WhereUsedInCdsAnalysisView_ShowUsesInAssociationsAction_xmit,
			SearchAndAnalysisPlugin.getDefault().getImageDescriptor(IImages.ASSOCIATION), USES_IN_ASSOC_PREF_KEY, false,
			prefStore);
		this.localAssociationsOnly = new PreferenceToggleAction(
			Messages.WhereUsedInCdsAnalysisView_OnlyLocallyDefinedAssocUsages_xmit, null,
			LOCAL_ASSOCIATIONS_ONLY_PREF_KEY, false, prefStore);
		this.releasedUsagesOnly = new PreferenceToggleAction(
			Messages.WhereUsedInCdsAnalysisView_OnlyUsagesInReleasedEntities_xmit, null,
			ICdsAnalysisPreferences.WHERE_USED_ONLY_RELEASED_USAGES, false, prefStore);
		this.showAssocUses.addPropertyChangeListener(event -> {
			this.localAssociationsOnly
				.setEnabled(this.showAssocUses.isChecked() && this.isLocalAssocOnlyFeatureAvailable);
			if (!this.showAssocUses.isChecked()) {
				this.showFromUses.setChecked(true);
			}
		});
		this.showFromUses.addPropertyChangeListener(event -> {
			if (!this.showFromUses.isChecked()) {
				this.showAssocUses.setChecked(true);
			}
		});
	}

	@Override
	protected void configureTreeViewer(final TreeViewer treeViewer) {
		treeViewer.setContentProvider(new LazyLoadingTreeContentProvider());
		treeViewer.setUseHashlookup(true);
		treeViewer.setLabelProvider(new DelegatingStyledCellLabelProvider(new TreeViewerLabelProvider()));
	}

	@Override
	protected void fillContextMenu(final IMenuManager mgr, final CommandPossibleChecker commandPossibleChecker) {
		super.fillContextMenu(mgr, commandPossibleChecker);
		if (commandPossibleChecker.canCommandBeEnabled(ICommandConstants.CDS_TOP_DOWN_ANALYSIS)) {
			MenuItemFactory.addCdsAnalyzerCommandItem(mgr, IContextMenuConstants.GROUP_CDS_ANALYSIS,
				ICommandConstants.CDS_TOP_DOWN_ANALYSIS);
		}
		if (commandPossibleChecker.canCommandBeEnabled(ICommandConstants.USED_ENTITIES_ANALYSIS)) {
			MenuItemFactory.addCdsAnalyzerCommandItem(mgr, IContextMenuConstants.GROUP_CDS_ANALYSIS,
				ICommandConstants.USED_ENTITIES_ANALYSIS);
		}
		if (commandPossibleChecker.canCommandBeEnabled(ICommandConstants.FIELD_ANALYSIS)) {
			MenuItemFactory.addCdsAnalyzerCommandItem(mgr, IContextMenuConstants.GROUP_CDS_ANALYSIS,
				ICommandConstants.FIELD_ANALYSIS);
		}
	}

	@Override
	protected ViewUiState getUiState() {
		final TreeViewUiState uiState = new TreeViewUiState();
		uiState.setFromTreeViewer((TreeViewer) getViewer());
		return uiState;
	}

	@Override
	protected void loadInput(final ViewUiState uiState) {
		checkFeatureState();
		final TreeViewer viewer = (TreeViewer) getViewer();
		if (this.analysisResult.isResultLoaded()) {
			viewer.setInput(this.analysisResult.getResult());
			this.analysisResult.updateWhereUsedProvider(this.showFromUses.isChecked(), this.showAssocUses.isChecked());
			this.analysisResult.setLocalAssociationsOnly(this.localAssociationsOnly.isChecked());
			if (uiState != null && uiState instanceof TreeViewUiState) {
				((TreeViewUiState) uiState).applyToTreeViewer(viewer);
			} else {
				final Object[] input = (Object[]) viewer.getInput();
				if (input != null && input.length >= 1) {
					viewer.expandToLevel(input[0], 1);
					viewer.setSelection(new StructuredSelection(input[0]));
				}
			}
		} else {
			this.analysisResult.createResult(this.lazyLoadingListener);
			this.analysisResult.updateWhereUsedProvider(this.showFromUses.isChecked(), this.showAssocUses.isChecked());
			this.analysisResult.setLocalAssociationsOnly(this.localAssociationsOnly.isChecked());
			viewer.setInput(this.analysisResult.getResult());
			this.analysisResult.setResultLoaded(true);
			viewer.expandAll();
		}
	}

	@Override
	public void setActionBars(final IActionBars actionBars) {
		super.setActionBars(actionBars);
		final IMenuManager menu = actionBars.getMenuManager();
		menu.appendToGroup(IGeneralContextMenuConstants.GROUP_FILTERING, this.showFromUses);
		menu.appendToGroup(IGeneralContextMenuConstants.GROUP_FILTERING, this.showAssocUses);
		menu.appendToGroup(IGeneralContextMenuConstants.GROUP_ADDITIONS, this.releasedUsagesOnly);
		menu.appendToGroup(IGeneralContextMenuConstants.GROUP_ADDITIONS, this.localAssociationsOnly);
	}

	@Override
	protected void refreshAnalysis() {
		this.analysisResult.refreshAnalysis();
		getViewPart().updateLabel();
		getViewer().refresh();
	}

	@Override
	protected StyledString getTreeNodeLabel(final Object element) {
		StyledString text = null;
		final ITreeNode node = (ITreeNode) element;

		if (element instanceof IStyledTreeNode) {
			text = ((IStyledTreeNode) element).getStyledText();
		} else {
			text = new StyledString();
			if (element instanceof LoadingTreeItemsNode) {
				text.append(node.getDisplayName(), StylerFactory.ITALIC_STYLER);
			} else {
				text.append(" "); // for broader image due to overlay //$NON-NLS-1$
				text.append(node.getDisplayName());
			}

			if (element instanceof ICollectionTreeNode) {
				final String size = ((ICollectionTreeNode) element).getSizeAsString();
				if (size != null) {
					text.append(" (" + size + ")", StyledString.COUNTER_STYLER); //$NON-NLS-1$ //$NON-NLS-2$
				}
			}

			final String description = node.getDescription();
			if (description != null && !description.isEmpty()) {
				text.append("  " + description + "  ", //$NON-NLS-1$ //$NON-NLS-2$
					StylerFactory.createCustomStyler(SWT.ITALIC, JFacePreferences.DECORATIONS_COLOR, null));
			}
		}
		return text;
	}

	private void checkFeatureState() {
		final IAdtObjectReferenceElementInfo adtObjElemInfo = this.analysisResult.getAdtObjectInfo();
		final IDestinationProvider destProvider = adtObjElemInfo.getAdapter(IDestinationProvider.class);
		final ObjectSearchUriDiscovery uriDiscovery = new ObjectSearchUriDiscovery(destProvider.getDestinationId());
		this.isLocalAssocOnlyFeatureAvailable = uriDiscovery
			.isParameterSupported(QueryParameterName.LOCAL_DECLARED_ASSOC_ONLY, SearchType.CDS_VIEW);
		this.localAssociationsOnly.setEnabled(this.isLocalAssocOnlyFeatureAvailable && this.showAssocUses.isChecked());
		this.releasedUsagesOnly
			.setEnabled(uriDiscovery.isParameterSupported(QueryParameterName.RELEASE_STATE, SearchType.CDS_VIEW));
	}
}
