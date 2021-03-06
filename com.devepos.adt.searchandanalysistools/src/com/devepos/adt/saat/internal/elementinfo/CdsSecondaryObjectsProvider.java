package com.devepos.adt.saat.internal.elementinfo;

import java.util.List;

import org.eclipse.osgi.util.NLS;

import com.devepos.adt.saat.internal.messages.Messages;
import com.devepos.adt.tools.base.elementinfo.IElementInfo;
import com.devepos.adt.tools.base.elementinfo.IElementInfoCollection;
import com.devepos.adt.tools.base.elementinfo.IElementInfoProvider;

/**
 * Provider for secondary objects of CDS view
 *
 * @author stockbal
 */
public class CdsSecondaryObjectsProvider implements IElementInfoProvider {

	private final String cdsViewName;
	private final String destinationId;

	public CdsSecondaryObjectsProvider(final String destinationId, final String cdsViewName) {
		this.cdsViewName = cdsViewName;
		this.destinationId = destinationId;
	}

	@Override
	public List<IElementInfo> getElements() {
		final IElementInfoRetrievalService service = ElementInfoRetrievalServiceFactory.createService();
		final IElementInfoCollection collection = service.retrieveCDSSecondaryElements(this.destinationId, this.cdsViewName);
		if (collection != null) {
			return collection.getChildren();
		}
		return null;
	}

	@Override
	public String getProviderDescription() {
		return NLS.bind(Messages.ObjectSearch_LoadingRelatedObjectsProviderDescription_xmsg, this.cdsViewName);
	}

}
