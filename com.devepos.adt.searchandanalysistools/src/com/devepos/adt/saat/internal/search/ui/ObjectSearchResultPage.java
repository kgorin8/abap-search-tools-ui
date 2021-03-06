package com.devepos.adt.saat.internal.search.ui;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.JFacePreferences;
import org.eclipse.jface.viewers.DelegatingStyledCellLabelProvider;
import org.eclipse.jface.viewers.DelegatingStyledCellLabelProvider.IStyledLabelProvider;
import org.eclipse.jface.viewers.IDecoration;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.jface.viewers.TreePath;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.search.internal.ui.SearchPlugin;
import org.eclipse.search.ui.IContextMenuConstants;
import org.eclipse.search.ui.ISearchResult;
import org.eclipse.search.ui.ISearchResultListener;
import org.eclipse.search.ui.ISearchResultPage;
import org.eclipse.search.ui.ISearchResultViewPart;
import org.eclipse.search.ui.NewSearchUI;
import org.eclipse.search.ui.SearchResultEvent;
import org.eclipse.search2.internal.ui.basic.views.ExpandAllAction;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.IWorkbenchCommandConstants;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.part.Page;

import com.devepos.adt.saat.internal.ICommandConstants;
import com.devepos.adt.saat.internal.SearchAndAnalysisPlugin;
import com.devepos.adt.saat.internal.cdsanalysis.CdsAnalysisUriDiscovery;
import com.devepos.adt.saat.internal.menu.MenuItemFactory;
import com.devepos.adt.saat.internal.messages.Messages;
import com.devepos.adt.saat.internal.search.IExtendedAdtObjectInfo;
import com.devepos.adt.saat.internal.util.FeatureTester;
import com.devepos.adt.saat.internal.util.IImages;
import com.devepos.adt.tools.base.ObjectType;
import com.devepos.adt.tools.base.project.IAbapProjectProvider;
import com.devepos.adt.tools.base.ui.IGeneralContextMenuConstants;
import com.devepos.adt.tools.base.ui.StylerFactory;
import com.devepos.adt.tools.base.ui.action.CollapseAllTreeNodesAction;
import com.devepos.adt.tools.base.ui.action.CollapseTreeNodesAction;
import com.devepos.adt.tools.base.ui.action.CopyToClipboardAction;
import com.devepos.adt.tools.base.ui.action.ExecuteAdtObjectAction;
import com.devepos.adt.tools.base.ui.action.OpenAdtObjectAction;
import com.devepos.adt.tools.base.ui.tree.ActionTreeNode;
import com.devepos.adt.tools.base.ui.tree.IAdtObjectReferenceNode;
import com.devepos.adt.tools.base.ui.tree.ICollectionTreeNode;
import com.devepos.adt.tools.base.ui.tree.IStyledTreeNode;
import com.devepos.adt.tools.base.ui.tree.ITreeNode;
import com.devepos.adt.tools.base.ui.tree.LazyLoadingTreeContentProvider;
import com.devepos.adt.tools.base.ui.tree.LoadingTreeItemsNode;
import com.devepos.adt.tools.base.ui.tree.PackageNode;
import com.devepos.adt.tools.base.util.AdtTypeUtil;
import com.sap.adt.tools.core.model.adtcore.IAdtObjectReference;

/**
 * The result page for an executed Object Search
 *
 * @author stockbal
 */
public class ObjectSearchResultPage extends Page implements ISearchResultPage, ISearchResultListener {
	public static final String GROUPED_BY_PACKAGE_PREF = "com.devepos.adt.saat.objectsearch.groupByPackage"; //$NON-NLS-1$
	public static final String DIALOG_ID = "com.devepos.adt.saat.ObjectSearchPage"; //$NON-NLS-1$ ;
	private String id;
	private UIState state;
	private ObjectSearchResult result;
	private ISearchResultViewPart searchViewPart;
	private TreeViewer searchResultTree;
	private Composite mainComposite;
	private ObjectSearchQuery searchQuery;

	private CollapseAllTreeNodesAction collapseAllNodesAction;
	private ExpandAllAction expandAllAction;
	private CollapseTreeNodesAction collapseNodesAction;
	private ExpandSelectedPackageNodesAction expandPackageNodesAction;
	private CopyToClipboardAction copyToClipBoardAction;
	private OpenObjectSearchPreferences openPreferencesAction;
	private GroupByPackageAction groupByPackageAction;
	private SearchFavoritesAction favoritesAction;

	private IAbapProjectProvider projectProvider;
	private boolean isDbBrowserIntegrationAvailable;
	private boolean isCdsTopDownAnalysisAvailable;
	private boolean isCdsUsedEntitiesAnalysisAvailable;
	private final IPreferenceStore prefStore;

	public ObjectSearchResultPage() {
		this.prefStore = SearchAndAnalysisPlugin.getDefault().getPreferenceStore();
		this.prefStore.setDefault(GROUPED_BY_PACKAGE_PREF, false);
	}

	/**
	 * @return the {@link ObjectSearchQuery} of this the result page
	 */
	public ObjectSearchQuery getSearchQuery() {
		return this.searchQuery != null ? this.searchQuery : null;
	}

	@Override
	public void createControl(final Composite parent) {
		this.mainComposite = createTreeViewerComposite(parent);
		createResultTree(this.mainComposite);
		initializeActions();
		hookContextMenu();
		getSite().setSelectionProvider(this.searchResultTree);
	}

	@Override
	public void setActionBars(final IActionBars actionBars) {
		final IToolBarManager tbm = actionBars.getToolBarManager();
		MenuItemFactory.addCommandItem(tbm, IContextMenuConstants.GROUP_NEW,
			ICommandConstants.OBJECT_SEARCH_OPEN_IN_DIALOG, IImages.SEARCH,
			Messages.ObjectSearchResultPage_OpenInSearchDialog_xtol, false, null);
		tbm.appendToGroup(IContextMenuConstants.GROUP_NEW, this.favoritesAction);
		tbm.appendToGroup(IContextMenuConstants.GROUP_EDIT, this.collapseAllNodesAction);
		tbm.appendToGroup(IContextMenuConstants.GROUP_EDIT, this.expandAllAction);
		tbm.appendToGroup(IContextMenuConstants.GROUP_VIEWER_SETUP, this.groupByPackageAction);
		this.copyToClipBoardAction.setActionDefinitionId(IWorkbenchCommandConstants.EDIT_COPY);
		actionBars.setGlobalActionHandler(ActionFactory.COPY.getId(), this.copyToClipBoardAction);
		actionBars.updateActionBars();

		actionBars.getMenuManager().add(this.openPreferencesAction);
	}

	@Override
	public void dispose() {
		super.dispose();
	}

	@Override
	public Control getControl() {
		return this.mainComposite;
	}

	@Override
	public void setFocus() {
		if (this.searchResultTree != null && !this.searchResultTree.getControl().isDisposed()) {
			this.searchResultTree.getControl().setFocus();
		}
	}

	@Override
	public Object getUIState() {
		if (this.searchResultTree != null && !this.searchResultTree.getControl().isDisposed()) {
			final UIState uiState = new UIState();
			uiState.setExpandedPaths(this.searchResultTree.getExpandedTreePaths());
			uiState.setSelection(this.searchResultTree.getSelection());
			return uiState;
		}
		return null;
	}

	@Override
	public void setInput(final ISearchResult search, final Object uiState) {
		if (this.result != null) {
			// clean up old search
			this.result.removeListener(this);
			this.searchResultTree.setInput(null);
		}
		this.result = (ObjectSearchResult) search;
		if (this.result != null) {
			this.result.addListener(this);
			this.searchResultTree.setInput(this.result);
			this.state = uiState instanceof UIState ? (UIState) uiState : null;
			this.searchQuery = (ObjectSearchQuery) this.result.getQuery();
			this.projectProvider = this.searchQuery.getProjectProvider();
			checkFeatureAvailibility();
			if (!NewSearchUI.isQueryRunning(this.searchQuery)) {
				updateUiState();
			}
		} else {
			this.searchViewPart.updateLabel();
		}
	}

	@Override
	public void setViewPart(final ISearchResultViewPart part) {
		this.searchViewPart = part;
	}

	@Override
	public void restoreState(final IMemento memento) {

	}

	@Override
	public void saveState(final IMemento memento) {

	}

	@Override
	public void setID(final String id) {
		this.id = id;
	}

	@Override
	public String getID() {
		return this.id;
	}

	@Override
	public String getLabel() {
		if (this.result != null) {
			return this.result.getLabel();
		}
		return ""; //$NON-NLS-1$
	}

	@Override
	public void searchResultChanged(final SearchResultEvent e) {
		if (e instanceof ObjectSearchResultEvent && ((ObjectSearchResultEvent) e).isCleanup()) {
			return;
		}
		this.state = null;
		Display.getDefault().asyncExec(() -> {
			/*
			 * If there is no active page in the workbench window the search view will not
			 * be brought to the front so it has to be done manually
			 */
			final IWorkbenchPage activeSearchPage = SearchPlugin.getActivePage();
			if (activeSearchPage != null && this.searchViewPart != null
				&& activeSearchPage.isPartVisible(this.searchViewPart)) {
				activeSearchPage.bringToTop(this.searchViewPart);
			}
			this.searchViewPart.updateLabel();
			final IAbapProjectProvider projectProvider = this.searchQuery.getProjectProvider();
			if (projectProvider != this.projectProvider) {
				this.projectProvider = projectProvider;
				checkFeatureAvailibility();
			}
			this.searchResultTree.setInput(e.getSearchResult());
			if (this.groupByPackageAction.isChecked()) {
				expandAllPackages();
			}
			updateUiState();
		});

	}

	/**
	 * @return the ID of corresponding Search Dialog Page of this result page
	 */
	public String getSearchDialogId() {
		return DIALOG_ID;
	}

	/**
	 * Creates the composite which will hold the tree viewer of the page
	 * <p>
	 * Subclasses may override to create a more complex layout <br>
	 * </p>
	 *
	 * @param  parent the parent composite
	 * @return
	 */
	protected Composite createTreeViewerComposite(final Composite parent) {
		final Composite composite = new Composite(parent, SWT.NONE);
		composite.setLayoutData(new GridData(GridData.FILL_BOTH));
		composite.setSize(100, 100);
		composite.setLayout(new FillLayout());
		return composite;
	}

	private void checkFeatureAvailibility() {
		this.isDbBrowserIntegrationAvailable = false;
		this.isCdsTopDownAnalysisAvailable = false;
		this.isCdsUsedEntitiesAnalysisAvailable = false;
		if (this.projectProvider != null && this.projectProvider.ensureLoggedOn()) {
			this.isDbBrowserIntegrationAvailable = FeatureTester
				.isSapGuiDbBrowserAvailable(this.projectProvider.getProject());
			this.isCdsTopDownAnalysisAvailable = FeatureTester
				.isCdsTopDownAnalysisAvailable(this.projectProvider.getProject());
			this.isCdsUsedEntitiesAnalysisAvailable = FeatureTester
				.isCdsUsedEntitiesAnalysisAvailable(this.projectProvider.getProject());
		}

	}

	private void initializeActions() {
		this.favoritesAction = new SearchFavoritesAction();
		this.collapseAllNodesAction = new CollapseAllTreeNodesAction(this.searchResultTree);
		this.collapseNodesAction = new CollapseTreeNodesAction(this.searchResultTree);
		this.copyToClipBoardAction = new CopyToClipboardAction();
		this.copyToClipBoardAction.registerViewer(this.searchResultTree);
		this.groupByPackageAction = new GroupByPackageAction();
		this.groupByPackageAction.setChecked(this.prefStore.getBoolean(GROUPED_BY_PACKAGE_PREF));
		this.expandAllAction = new ExpandAllPackageNodesAction();
		this.expandAllAction.setViewer(this.searchResultTree);
		this.expandAllAction.setEnabled(this.groupByPackageAction.isChecked());
		this.expandPackageNodesAction = new ExpandSelectedPackageNodesAction(this.searchResultTree);
		this.openPreferencesAction = new OpenObjectSearchPreferences();
	}

	/*
	 * Creates the result tree of the object search
	 */
	private void createResultTree(final Composite parent) {

		this.searchResultTree = new TreeViewer(parent, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL);
		this.searchResultTree.setContentProvider(new TreeContentProvider());
		this.searchResultTree.setLabelProvider(new DelegatingStyledCellLabelProvider(new ViewLabelProvider()));
		this.searchResultTree.addOpenListener(event -> {
			final ITreeSelection sel = (ITreeSelection) event.getSelection();
			final Iterator<?> selIter = sel.iterator();
			while (selIter.hasNext()) {
				handleOpenOnTreeNode(selIter.next());
			}
		});
	}

	private void handleOpenOnTreeNode(final Object node) {
		if (node == null) {
			return;
		}
		if (node instanceof IAdtObjectReferenceNode) {
			final IAdtObjectReferenceNode selectedAdtObject = (IAdtObjectReferenceNode) node;

			if (selectedAdtObject != null) {
				this.searchQuery.getProjectProvider().openObjectReference(selectedAdtObject.getObjectReference());
			}
		} else if (node instanceof ICollectionTreeNode) {
			final boolean isExpanded = this.searchResultTree.getExpandedState(node);
			if (isExpanded) {
				this.searchResultTree.collapseToLevel(node, 1);
			} else {
				this.searchResultTree.expandToLevel(node, 1);
			}
		} else if (node instanceof ActionTreeNode) {
			((ActionTreeNode) node).getAction().execute();
		}
	}

	private void hookContextMenu() {
		final MenuManager menuMgr = new MenuManager();
		menuMgr.setRemoveAllWhenShown(true);

		menuMgr.addMenuListener(menu -> {
			fillContextMenu(menu);
		});
		final Control viewerControl = this.searchResultTree.getControl();
		final Menu menu = menuMgr.createContextMenu(viewerControl);
		viewerControl.setMenu(menu);
		getSite().registerContextMenu(this.searchViewPart.getViewSite().getId(), menuMgr, this.searchResultTree);
	}

	private void fillContextMenu(final IMenuManager menu) {
		menu.add(new Separator(IContextMenuConstants.GROUP_NEW));
		menu.add(new Separator(IContextMenuConstants.GROUP_OPEN));

		final IStructuredSelection selection = this.searchResultTree.getStructuredSelection();
		if (selection == null || selection.isEmpty()) {
			return;
		}
		boolean selectionHasExpandedNodes = false;
		final List<IAdtObjectReference> adtObjRefs = new ArrayList<>();
		final List<IAdtObjectReference> previewAdtObjRefs = new ArrayList<>();
		final int selectionSize = selection.size();
		boolean singleDataPreviewObjectSelected = false;
		boolean singleCdsViewSelected = false;
		boolean hasCollapsedPackages = false;

		for (final Object selectedObject : selection.toList()) {
			if (selectedObject instanceof IAdtObjectReferenceNode) {
				final IAdtObjectReferenceNode objRefNode = (IAdtObjectReferenceNode) selectedObject;
				final IAdtObjectReference adtObjectRef = objRefNode.getObjectReference();
				if (objRefNode.supportsDataPreview()) {
					previewAdtObjRefs.add(adtObjectRef);
				}
				adtObjRefs.add(adtObjectRef);

				if (selectionSize == 1) {
					singleDataPreviewObjectSelected = true;
					singleCdsViewSelected = objRefNode.getObjectType() == ObjectType.CDS_VIEW;
				}
			}

			if (!selectionHasExpandedNodes && selectedObject instanceof ICollectionTreeNode
				&& this.searchResultTree.getExpandedState(selectedObject)) {
				selectionHasExpandedNodes = true;
			}
			if (!hasCollapsedPackages && selectedObject instanceof PackageNode
				&& !this.searchResultTree.getExpandedState(selectedObject)) {
				hasCollapsedPackages = true;
			}
		}

		if (!adtObjRefs.isEmpty()) {
			menu.appendToGroup(IContextMenuConstants.GROUP_OPEN,
				new OpenAdtObjectAction(this.projectProvider.getProject(), adtObjRefs));
		}
		if (!previewAdtObjRefs.isEmpty()) {
			menu.appendToGroup(IContextMenuConstants.GROUP_OPEN,
				new ExecuteAdtObjectAction(this.projectProvider.getProject(), previewAdtObjRefs, true));

			if (this.isDbBrowserIntegrationAvailable) {
				menu.add(new Separator(com.devepos.adt.saat.internal.IContextMenuConstants.GROUP_DB_BROWSER));
				MenuItemFactory.addOpenInDbBrowserCommand(menu, false);
				MenuItemFactory.addOpenInDbBrowserCommand(menu, true);
			}
		}

		if (!adtObjRefs.isEmpty()) {
			menu.add(new Separator(IContextMenuConstants.GROUP_ADDITIONS));
			MenuItemFactory.addCommandItem(menu, IContextMenuConstants.GROUP_ADDITIONS,
				"com.sap.adt.ris.whereused.ui.callWhereUsed", //$NON-NLS-1$
				IImages.WHERE_USED_LIST, Messages.ObjectSearch_WhereUsedListAction_xmit, null);
		}

		if (singleDataPreviewObjectSelected) {
			// check if action is supported in the current project
			if (new CdsAnalysisUriDiscovery(this.projectProvider.getDestinationId()).getCdsAnalysisUri() != null) {
				menu.add(new Separator(com.devepos.adt.saat.internal.IContextMenuConstants.GROUP_CDS_ANALYSIS));
				if (singleCdsViewSelected && this.isCdsTopDownAnalysisAvailable) {
					MenuItemFactory.addCdsAnalyzerCommandItem(menu,
						com.devepos.adt.saat.internal.IContextMenuConstants.GROUP_CDS_ANALYSIS,
						ICommandConstants.CDS_TOP_DOWN_ANALYSIS);
				}
				if (!previewAdtObjRefs.isEmpty()) {
					MenuItemFactory.addCdsAnalyzerCommandItem(menu,
						com.devepos.adt.saat.internal.IContextMenuConstants.GROUP_CDS_ANALYSIS,
						ICommandConstants.WHERE_USED_IN_CDS_ANALYSIS);
				}
				if (singleCdsViewSelected && this.isCdsUsedEntitiesAnalysisAvailable) {
					MenuItemFactory.addCdsAnalyzerCommandItem(menu,
						com.devepos.adt.saat.internal.IContextMenuConstants.GROUP_CDS_ANALYSIS,
						ICommandConstants.USED_ENTITIES_ANALYSIS);
				}
				if (!previewAdtObjRefs.isEmpty()) {
					MenuItemFactory.addCdsAnalyzerCommandItem(menu,
						com.devepos.adt.saat.internal.IContextMenuConstants.GROUP_CDS_ANALYSIS,
						ICommandConstants.FIELD_ANALYSIS);
				}

			}
		}
		if (selectionHasExpandedNodes || hasCollapsedPackages) {
			menu.add(new Separator(IGeneralContextMenuConstants.GROUP_NODE_ACTIONS));
			if (hasCollapsedPackages) {
				menu.add(this.expandPackageNodesAction);
			}
			if (selectionHasExpandedNodes) {
				menu.add(this.collapseNodesAction);
			}
		}

		menu.add(new Separator(IContextMenuConstants.GROUP_EDIT));
		menu.appendToGroup(IContextMenuConstants.GROUP_EDIT, this.copyToClipBoardAction);
	}

	private void updateUiState() {
		Display.getDefault().asyncExec(() -> {
			if (this.searchResultTree == null || this.searchResultTree.getControl().isDisposed()) {
				return;
			}
			if (this.state != null) {
				this.searchResultTree.getControl().setRedraw(false);
				try {
					this.searchResultTree.setExpandedTreePaths(this.state.getExpandedPaths());
				} finally {
					this.searchResultTree.getControl().setRedraw(true);
				}
			}
			this.searchResultTree.getControl().setFocus();
			final IAdtObjectReferenceNode[] result = this.result
				.getResultForTree(this.groupByPackageAction.isChecked());
			if (result != null && result.length > 0) {
				if (this.state != null && this.state.hasSelection()) {
					this.searchResultTree.setSelection(this.state.getSelection());
				} else {
					this.searchResultTree.setSelection(new StructuredSelection(result[0]));
				}
			}
			this.searchResultTree.refresh();
		});
	}

	private void updateGrouping() {
		BusyIndicator.showWhile(getSite().getShell().getDisplay(), () -> {
			this.searchResultTree.refresh();
		});
		this.expandAllAction.setEnabled(this.groupByPackageAction.isChecked());
		this.prefStore.putValue(GROUPED_BY_PACKAGE_PREF, Boolean.toString(this.groupByPackageAction.isChecked()));
	}

	/*
	 * Expands all package nodes
	 */
	private void expandAllPackages() {
		final Object[] packages = this.result.getPackages();
		if (packages != null) {
			BusyIndicator.showWhile(getSite().getShell().getDisplay(), () -> {
				try {
					this.searchResultTree.getControl().setRedraw(false);
					this.searchResultTree.setExpandedElements(packages);
				} finally {
					this.searchResultTree.getControl().setRedraw(true);
				}
			});
		}
	}

	/**
	 * Custom view label provider for the Result Tree
	 *
	 * @author stockbal
	 */
	class ViewLabelProvider extends LabelProvider implements ILabelProvider, IStyledLabelProvider {

		@Override
		public String getText(final Object element) {
			final ITreeNode searchResult = (ITreeNode) element;

			return searchResult.getName();
		}

		@Override
		public Image getImage(final Object element) {
			Image image = null;
			final ITreeNode searchResult = (ITreeNode) element;
			image = searchResult.getImage();
			if (image == null) {

				if (element instanceof IAdtObjectReferenceNode) {
					final IAdtObjectReferenceNode adtObjRefNode = (IAdtObjectReferenceNode) element;
					if (adtObjRefNode.getObjectType() == ObjectType.CDS_VIEW) {
						image = SearchAndAnalysisPlugin.getDefault().getImage(IImages.CDS_VIEW);
					} else {
						image = AdtTypeUtil.getInstance().getTypeImage(adtObjRefNode.getAdtObjectType());
					}
					final IExtendedAdtObjectInfo extendedResult = adtObjRefNode
						.getAdapter(IExtendedAdtObjectInfo.class);
					if (extendedResult != null) {
						final String[] overlayImages = new String[4];
						if (extendedResult.getSourceType() != null) {
							overlayImages[IDecoration.BOTTOM_RIGHT] = extendedResult.getSourceType().getImageId();
						}
						if (extendedResult.isReleased()) {
							overlayImages[IDecoration.TOP_RIGHT] = IImages.RELEASED_API_OVR;
						}
						image = SearchAndAnalysisPlugin.getDefault().overlayImage(image, overlayImages);
					}

				}
			}
			return image;
		}

		@Override
		public StyledString getStyledText(final Object element) {
			StyledString text = new StyledString();
			final ITreeNode searchResult = (ITreeNode) element;

			boolean isAdtObjRefNode = false;
			if (element instanceof IAdtObjectReferenceNode) {
				isAdtObjRefNode = true;
			}

			if (element instanceof IStyledTreeNode) {
				text = ((IStyledTreeNode) element).getStyledText();
				if (text == null) {
					text = new StyledString();
				}
			} else {
				if (element instanceof LoadingTreeItemsNode) {
					text.append(searchResult.getDisplayName(), StylerFactory.ITALIC_STYLER);
					return text;
				} else {
					text.append(searchResult.getDisplayName());
				}

				if (element instanceof ICollectionTreeNode && !isAdtObjRefNode) {
					final ICollectionTreeNode collectionNode = (ICollectionTreeNode) element;
					if (collectionNode.hasChildren()) {
						final String size = ((ICollectionTreeNode) element).getSizeAsString();
						if (size != null) {
							text.append(" (" + size + ")", StyledString.COUNTER_STYLER); //$NON-NLS-1$ //$NON-NLS-2$
						}
					}
				}

				final String description = searchResult.getDescription();
				if (description != null && !description.isEmpty()) {
					text.append("  " + description + "  ", //$NON-NLS-1$ //$NON-NLS-2$
						StylerFactory.createCustomStyler(SWT.ITALIC, JFacePreferences.DECORATIONS_COLOR, null));
				}
			}

			return text;
		}
	}

	private class TreeContentProvider extends LazyLoadingTreeContentProvider {
		@Override
		public Object[] getElements(final Object inputElement) {
			if (ObjectSearchResultPage.this.result != null) {
				return ObjectSearchResultPage.this.result
					.getResultForTree(ObjectSearchResultPage.this.groupByPackageAction.isChecked());
			}
			return new Object[0];
		}
	}

	private class GroupByPackageAction extends Action {
		public GroupByPackageAction() {
			super(Messages.ObjectSearch_GroupByPackageAction_xtol, AS_CHECK_BOX);
			setImageDescriptor(SearchAndAnalysisPlugin.getDefault().getImageDescriptor(IImages.PACKAGE));
		}

		@Override
		public void run() {
			updateGrouping();
		}
	}

	private class ExpandAllPackageNodesAction extends ExpandAllAction {
		@Override
		public void run() {
			expandAllPackages();
		}
	}

	private class ExpandSelectedPackageNodesAction extends Action {
		private final TreeViewer viewer;

		public ExpandSelectedPackageNodesAction(final TreeViewer viewer) {
			super(Messages.ObjectSearch_ExpandNodeAction_xmsg,
				SearchAndAnalysisPlugin.getDefault().getImageDescriptor(IImages.EXPAND_ALL));
			this.viewer = viewer;
		}

		@Override
		public void run() {
			final IStructuredSelection selection = this.viewer.getStructuredSelection();
			if (selection == null) {
				return;
			}
			BusyIndicator.showWhile(getSite().getShell().getDisplay(), () -> {
				this.viewer.getControl().setRedraw(false);
				try {
					for (final Object selectedObject : selection.toList()) {
						final PackageNode node = (PackageNode) selectedObject;
						this.viewer.setExpandedState(node, true);
						for (final PackageNode subNode : node.getSubPackages()) {
							this.viewer.setExpandedState(subNode, true);
						}
					}
				} finally {
					this.viewer.getControl().setRedraw(true);
				}
			});
		}
	}

	/*
	 * Represents the current state of the object
	 */
	private class UIState {
		private ISelection selection;

		/**
		 * @return the stored selection
		 */
		public ISelection getSelection() {
			return this.selection;
		}

		/**
		 * @return <code>true</code> if the stored state has a selection
		 */
		public boolean hasSelection() {
			return this.selection != null && !this.selection.isEmpty();
		}

		/**
		 * @param selection the selectedObject to set
		 */
		public void setSelection(final ISelection selection) {
			this.selection = selection;
		}

		private TreePath[] expandedPaths;

		/**
		 * @return the expandedPaths
		 */
		public TreePath[] getExpandedPaths() {
			return this.expandedPaths;
		}

		/**
		 * @param expandedPaths the expandedPaths to set
		 */
		public void setExpandedPaths(final TreePath[] expandedPaths) {
			this.expandedPaths = expandedPaths;
		}

	}

}
