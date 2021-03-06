package com.devepos.adt.saat.internal.search;

import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.fieldassist.IContentProposal;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.graphics.Image;

import com.devepos.adt.saat.internal.SearchAndAnalysisPlugin;
import com.devepos.adt.saat.internal.messages.Messages;
import com.devepos.adt.saat.internal.search.contentassist.SearchParameterProposal;
import com.devepos.adt.saat.internal.util.IImages;
import com.devepos.adt.tools.base.project.IAbapProjectProvider;

/**
 * Search parameter to restrict query results to entities which have a certain
 * API state
 *
 * @author stockbal
 */
public class ReleaseStateSearchParameter extends NamedItemProposalProvider implements ISearchParameter, ISearchProposalProvider {
	private final Image image;

	public ReleaseStateSearchParameter(final IAbapProjectProvider projectProvider) {
		super(projectProvider, QueryParameterName.RELEASE_STATE, NamedItemType.API_STATE, true);
		this.image = SearchAndAnalysisPlugin.getDefault().getImage(IImages.API_PARAM);
	}

	@Override
	public List<IContentProposal> getProposalList(final String query) throws CoreException {
		return getProposals("*", query);
	}

	@Override
	public QueryParameterName getParameterName() {
		return this.parameterName;
	}

	@Override
	protected IContentProposal createProposalFromNamedItem(final INamedItem item, final String query) {
		return new SearchParameterProposal(item.getName(), this.parameterName, item.getDescription(),
			getDescriptionFromItem(item.getData()), null, query);
	}

	/*
	 * Retrieve long text information from description
	 */
	private String getDescriptionFromItem(final String description) {
		if (description == null || description.isEmpty()) {
			return null;
		}
		final String[] itemDescrComponents = description.split("@@##@@"); //$NON-NLS-1$
		if (itemDescrComponents.length < 2) {
			return null;
		}

		final String longText = itemDescrComponents[1];
		final String[] longTextParts = longText.split("="); //$NON-NLS-1$
		if (longTextParts == null || longTextParts.length < 2) {
			return null;
		}
		return longTextParts[1];
	}

	@Override
	public Image getImage() {
		return this.image;
	}

	@Override
	public String getLabel() {
		return this.parameterName.getLowerCaseKey();
	}

	@Override
	public String getDescription() {
		return NLS.bind(Messages.SearchPatternAnalyzer_DescriptionReleaseStateParameter_xmsg,
			new Object[] { getLabel(), "cloud" });
	}

	@Override
	public boolean supportsPatternValues() {
		return false;
	}

	@Override
	public boolean isBuffered() {
		return true;
	}

	@Override
	public boolean supportsMultipleValues() {
		return true;
	}

	@Override
	public boolean supportsNegatedValues() {
		return true;
	}
}
