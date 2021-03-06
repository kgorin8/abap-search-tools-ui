package com.devepos.adt.saat.internal.search.contentassist;

import java.util.Locale;

import org.eclipse.jface.fieldassist.IContentProposal;

import com.devepos.adt.saat.internal.search.QueryParameterName;
import com.devepos.adt.tools.base.ObjectType;

/**
 * Search parameter for the DB object search <br>
 * A parameter is for a specific filter value i.e. <strong>owner:smith</strong>
 *
 * @author stockbal
 */
public class SearchParameterProposal implements IContentProposal {
	private final String key;
	private final QueryParameterName paramName;
	private final String description;
	private final String wordToComplete;
	private final ObjectType type;
	private final String longText;

	/**
	 * Creates new search filter proposal with the given name and description
	 *
	 * @param key         the key of the proposal
	 * @param paramName   the parameter name for the parameter proposal e.g.
	 *                    <strong>owner</strong>
	 * @param description the description of the proposal
	 */
	public SearchParameterProposal(final String key, final QueryParameterName paramName, final String description,
		final String wordToComplete) {
		this(key, paramName, description, null, null, wordToComplete);
	}

	/**
	 * Creates new search filter proposal with the given name and description
	 *
	 * @param key         the key of the proposal
	 * @param paramName   the parameter name for the parameter proposal e.g.
	 *                    <strong>owner</strong>
	 * @param description the description of the proposal
	 * @param type        the specific type of the parameter proposal
	 */
	public SearchParameterProposal(final String key, final QueryParameterName paramName, final String description,
		final ObjectType type, final String wordToComplete) {
		this(key, paramName, description, null, type, wordToComplete);
	}

	/**
	 * Creates new search filter proposal with the given name and description
	 *
	 * @param key         the key of the proposal
	 * @param paramName   the parameter name for the parameter proposal e.g.
	 *                    <strong>owner</strong>
	 * @param description the description of the proposal
	 * @param longText    the long text of the parameter proposal
	 * @param type        the specific type of the parameter proposal
	 */
	public SearchParameterProposal(final String key, final QueryParameterName paramName, final String description,
		final String longText, final ObjectType type, final String wordToComplete) {
		this.key = key;
		this.paramName = paramName;
		this.longText = longText;
		this.description = description;
		this.type = type;
		this.wordToComplete = wordToComplete;
	}

	@Override
	public String getContent() {
		String content = String.valueOf(this.key.toLowerCase(Locale.ENGLISH));
		if (this.wordToComplete != null && !this.wordToComplete.isEmpty()
			&& content.startsWith(this.wordToComplete.toLowerCase())) {
			content = content.substring(this.wordToComplete.length());
		}
		return content;
	}

	/**
	 * Retrieve the lexeme which triggered the proposals
	 *
	 * @return
	 */
	public String getLexeme() {
		return this.wordToComplete;
	}

	@Override
	public int getCursorPosition() {
		return getContent().length();
	}

	@Override
	public String getLabel() {
		return this.key;
	}

	/**
	 * Retrieve the key String of the proposal
	 *
	 * @return
	 */
	public String getKey() {
		return this.key;
	}

	/**
	 * Retrieve a short description of the proposal
	 *
	 * @return
	 */
	public String getShortText() {
		return this.description;
	}

	@Override
	public String getDescription() {
		return this.longText;
	}

	/**
	 * Returns the type of this parameter proposal
	 *
	 * @return the QueryParameterName of the parameter proposal
	 */
	public QueryParameterName getParameterName() {
		return this.paramName;
	}

	/**
	 * Retrieves the specific type of the parameter proposal
	 *
	 * @return the String type of the proposal
	 */
	public ObjectType getType() {
		return this.type;
	}
}
