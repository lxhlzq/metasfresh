package de.metas.ui.web.letter;

import java.io.File;

import de.metas.ui.web.window.datatypes.DocumentPath;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

/*
 * #%L
 * metasfresh-webui-api
 * %%
 * Copyright (C) 2017 metas GmbH
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 2 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program. If not, see
 * <http://www.gnu.org/licenses/gpl-2.0.html>.
 * #L%
 */

@Builder(toBuilder = true)
@Value
public class WebuiLetter
{
	@NonNull
	private final String letterId;
	private final int ownerUserId;

	private final boolean processed;
	/** File PDF file; set when the letter is marked as processed too */
	private final File temporaryPDFFile;

	private final int persistentLetterId;
	
	private final String content;
	private final String subject;
	@NonNull
	private final DocumentPath contextDocumentPath;
}
