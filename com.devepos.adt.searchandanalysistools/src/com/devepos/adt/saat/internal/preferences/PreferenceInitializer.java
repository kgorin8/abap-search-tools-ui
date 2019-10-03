package com.devepos.adt.saat.internal.preferences;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.jface.preference.IPreferenceStore;

import com.devepos.adt.saat.SearchAndAnalysisPlugin;

/**
 * Class used to initialize default preference values.
 */
public class PreferenceInitializer extends AbstractPreferenceInitializer {

	/*
	 * (non-Javadoc)
	 *
	 * @see org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer#
	 * initializeDefaultPreferences()
	 */
	@Override
	public void initializeDefaultPreferences() {
		final IPreferenceStore store = SearchAndAnalysisPlugin.getDefault().getPreferenceStore();
		store.setDefault(IPreferences.REMEMBER_LAST_PROJECT_IN_OBJ_EXPLORER, true);
		store.setDefault(IPreferences.CURSOR_AT_END_OF_SEARCH_INPUT, true);
		store.setDefault(IPreferences.MAX_SEARCH_RESULTS, 50);
		store.setDefault(IPreferences.SHOW_FULL_ASSOCIATION_NAME, true);

	}

}