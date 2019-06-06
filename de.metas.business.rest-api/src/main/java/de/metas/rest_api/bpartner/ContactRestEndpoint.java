package de.metas.rest_api.bpartner;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import de.metas.util.rest.MetasfreshRestAPIConstants;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

/*
 * #%L
 * de.metas.business.rest-api
 * %%
 * Copyright (C) 2019 metas GmbH
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

@RequestMapping(MetasfreshRestAPIConstants.ENDPOINT_API + "/contact")
public interface ContactRestEndpoint
{
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = "Successfully retrieved contact"),
			@ApiResponse(code = 401, message = "You are not authorized to view the resource"),
			@ApiResponse(code = 403, message = "Accessing the resource you were trying to reach is forbidden"),
			@ApiResponse(code = 404, message = "The resource you were trying to reach is not found")
	})
	@GetMapping("{contactIdentifier}")
	ResponseEntity<JsonContact> retrieveContact(
			@ApiParam(value = "Identifier of the bpartner's contact in question. Can be a plain `AD_User_ID` or something like `ext-YourExternalId`", allowEmptyValue = false) //
			@PathVariable("contactIdentifier") //
			String contactIdentifier);

	@ApiResponses(value = {
			@ApiResponse(code = 200, message = "Successfully retrieved contact(s)"),
			@ApiResponse(code = 401, message = "You are not authorized to view the resource"),
			@ApiResponse(code = 403, message = "Accessing the resource you were trying to reach is forbidden"),
			@ApiResponse(code = 404, message = "There is no page for the given 'next' value")
	})
	@GetMapping
	ResponseEntity<JsonContactList> retrieveContactsSince(
			@ApiParam("Optional epoch timestamp in ms. The enpoint returns all resources that were created or modified *after* the given time.") //
			@RequestParam(name = "since", required = false) //
			Long epochTimestampMillis,

			@ApiParam("Optional identifier for the next page that was provided to the client in the previous page.\n"
					+ "If provided, any `since` value is ignored") //
			@RequestParam(name = "next", required = false) //
			String next);

	@ApiResponses(value = {
			@ApiResponse(code = 201, message = "Successfully created or updated contact"),
			@ApiResponse(code = 401, message = "You are not authorized to create or update the resource"),
			@ApiResponse(code = 403, message = "Accessing the resource you were trying to reach is forbidden")
	})
	@PostMapping
	ResponseEntity<JsonUpsertResponse> createOrUpdateContact(JsonContactUpsertRequest contacts);
}