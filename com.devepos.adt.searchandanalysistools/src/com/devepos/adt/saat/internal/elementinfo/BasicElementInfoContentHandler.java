package com.devepos.adt.saat.internal.elementinfo;

import com.devepos.adt.saat.internal.CdsSourceType;
import com.devepos.adt.saat.internal.IDataSourceType;
import com.devepos.adt.tools.base.elementinfo.IAdtObjectReferenceElementInfo;
import com.devepos.adt.tools.base.util.IXmlElement;
import com.devepos.adt.tools.base.util.IXmlTags;
import com.sap.adt.communication.message.IMessageBody;

/**
 * Content handler which returns the basic information about an ADT object
 *
 * @author stockbal
 */
public class BasicElementInfoContentHandler extends AdtObjectElementInfoContentHandlerBase {

	public BasicElementInfoContentHandler(final String destinationId) {
		super(destinationId);
	}

	@Override
	public IAdtObjectReferenceElementInfo deserialize(final IMessageBody messageBody,
		final Class<? extends IAdtObjectReferenceElementInfo> arg1) {
		try {
			createElementInfo(this.utility.parseXML(messageBody, IXmlTags.EL_PROPERTY));
			return this.elementInfo;
		} catch (final Exception e) {
		}
		return null;
	}

	@Override
	protected void createElementInfo(final IXmlElement rootElement) {
		super.createElementInfo(rootElement);
		if (this.elementInfo == null || !rootElement.hasChild(IXmlTags.EL_PROPERTIES)) {
			return;
		}
		final IXmlElement propertiesEl = rootElement.getChildren()
			.stream()
			.filter(c -> c.getName().equals(IXmlTags.EL_PROPERTIES))
			.findFirst()
			.get();

		IDataSourceType sourceType = null;
		String apiState = null;
		for (final IXmlElement propertyEl : propertiesEl.getChildren()) {
			final String key = propertyEl.getAttributeValue("key");
			switch (key) {
			case "API_STATE":
				apiState = propertyEl.getText();
				break;
			case "SOURCE_TYPE":
				sourceType = CdsSourceType.getFromId(propertyEl.getText());
				break;
			}
		}
		if (sourceType != null || apiState != null) {
			final ExtendedAdtObjectInfo extendedInfo = new ExtendedAdtObjectInfo();
			extendedInfo.setSourceType(sourceType);
			extendedInfo.setApiState(apiState);
			this.elementInfo.setAdditionalInfo(extendedInfo);
		}
	}

}
