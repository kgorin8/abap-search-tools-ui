package com.devepos.adt.saat.internal.search;

import java.util.Map;

import org.eclipse.search.internal.ui.SearchDialog;
import org.eclipse.search.ui.NewSearchUI;
import org.eclipse.search2.internal.ui.SearchView;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

import com.devepos.adt.saat.internal.search.contentassist.SearchPatternProvider;
import com.devepos.adt.saat.internal.search.ui.ObjectSearchPage;
import com.devepos.adt.saat.internal.search.ui.ObjectSearchQuery;
import com.devepos.adt.saat.internal.search.ui.ObjectSearchRequest;
import com.devepos.adt.saat.internal.search.ui.ObjectSearchResultPage;
import com.devepos.adt.saat.internal.util.AbapProjectProviderAccessor;
import com.devepos.adt.saat.internal.util.IAbapProjectProvider;
import com.devepos.adt.saat.model.objectsearchfavorites.IObjectSearchFavorite;

/**
 * Engine for the extended ABAP Object search
 *
 * @author stockbal
 */
public class ObjectSearchEngine {

	/**
	 * Creates and runs a Object search query from the given favorite entry
	 *
	 * @param favorite a favorite entry for an object search
	 * @see            {@link NewSearchUI}
	 */
	public static void runSearchFromFavorite(final IObjectSearchFavorite favorite) {
		final String searchFilter = favorite.getSearchFilter();
		final ObjectSearchRequest searchRequest = createRequestFromFavorite(favorite);
		final IAbapProjectProvider projectProvider = AbapProjectProviderAccessor
			.getProviderForDestination(favorite.getDestinationId());
		searchRequest.setProjectProvider(projectProvider);

		if (searchFilter != null && !searchFilter.isEmpty()) {
			final SearchPatternProvider patternProvider = new SearchPatternProvider(projectProvider,
				searchRequest.getSearchType());

			final Map<String, Object> parameterMap = patternProvider.getSearchParameters(searchFilter);
			searchRequest.setParameters(parameterMap, searchFilter);
		}
		NewSearchUI.runQueryInBackground(new ObjectSearchQuery(searchRequest));
	}

	/**
	 * Opens the given favorite entry in the Object Search Page of the eclipse
	 * search dialog
	 *
	 * @param favorite a favorite entry for an object search
	 * @see            {@link SearchView}
	 */
	public static void openFavoriteInSearchDialog(final IObjectSearchFavorite favorite) {
		final String searchFilter = favorite.getSearchFilter();
		final ObjectSearchRequest searchRequest = createRequestFromFavorite(favorite);

		searchRequest.setParameters(null, searchFilter);
		final IWorkbenchWindow activeWindow = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
		final SearchDialog dialog = new SearchDialog(activeWindow, ObjectSearchResultPage.DIALOG_ID);
		dialog.setBlockOnOpen(false);
		dialog.open();
		if (dialog.getSelectedPage() instanceof ObjectSearchPage) {
			final ObjectSearchPage searchDialog = (ObjectSearchPage) dialog.getSelectedPage();
			searchDialog.setInputFromSearchQuery(new ObjectSearchQuery(searchRequest));
		}
		dialog.setBlockOnOpen(true);
	}

	private static ObjectSearchRequest createRequestFromFavorite(final IObjectSearchFavorite favorite) {
		final ObjectSearchRequest searchRequest = new ObjectSearchRequest();
		searchRequest.setAndSearchActive(favorite.isAndSearchActive());
		searchRequest.setSearchTerm(favorite.getObjectName());
		searchRequest.setMaxResults(favorite.getMaxResults());
		searchRequest.setSearchType(SearchType.valueOf(favorite.getSearchType()));
		searchRequest.setReadApiState(true);
		searchRequest.setReadPackageHierarchy(true);
		searchRequest.setDestinationId(favorite.getDestinationId());
		return searchRequest;
	}
}