package de.metas.ui.web.pporder;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import org.adempiere.ad.trx.api.ITrx;
import org.adempiere.model.InterfaceWrapperHelper;
import org.adempiere.util.Check;
import org.adempiere.util.Services;
import org.compiere.model.I_C_UOM;
import org.compiere.util.Env;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import de.metas.handlingunits.IHandlingUnitsDAO;
import de.metas.handlingunits.model.X_M_HU;
import de.metas.ui.web.exceptions.EntityNotFoundException;
import de.metas.ui.web.handlingunits.WEBUI_HU_Constants;
import de.metas.ui.web.view.DocumentViewCreateRequest;
import de.metas.ui.web.view.IDocumentView;
import de.metas.ui.web.view.IDocumentViewAttributes;
import de.metas.ui.web.view.IDocumentViewSelection;
import de.metas.ui.web.view.IDocumentViewsRepository;
import de.metas.ui.web.view.ViewId;
import de.metas.ui.web.view.json.JSONViewDataType;
import de.metas.ui.web.window.datatypes.DocumentId;
import de.metas.ui.web.window.datatypes.DocumentPath;
import de.metas.ui.web.window.datatypes.WindowId;
import de.metas.ui.web.window.datatypes.json.JSONLookupValue;
import lombok.NonNull;
import lombok.ToString;

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

@ToString
public class PPOrderLineRow implements IDocumentView, IPPOrderBOMLine
{
	public static final Builder builder(final ViewId viewId)
	{
		return new Builder(viewId);
	}

	public static final PPOrderLineRow cast(final IDocumentView viewRecord)
	{
		return (PPOrderLineRow)viewRecord;
	}

	private final DocumentPath documentPath;
	private final DocumentId documentId;
	private final PPOrderLineType type;

	private final Map<String, Object> values;

	private final Supplier<? extends IDocumentViewAttributes> attributesSupplier;

	private final List<PPOrderLineRow> includedDocuments;

	private final ViewId viewId;
	private final boolean processed;
	private final int ppOrderId;
	private final int ppOrderBOMLineId;
	private final int ppOrderQtyId;

	private volatile ViewId husToIssueViewId = null; // lazy

	private PPOrderLineRow(final Builder builder)
	{
		documentPath = builder.getDocumentPath();

		documentId = documentPath.getDocumentId();
		type = builder.getType();

		values = ImmutableMap.copyOf(builder.values);

		attributesSupplier = builder.attributesSupplier;
		includedDocuments = builder.buildIncludedDocuments();

		viewId = builder.viewId;

		processed = builder.processed;

		ppOrderId = builder.ppOrderId;
		ppOrderBOMLineId = builder.ppOrderBOMLineId;

		ppOrderQtyId = builder.ppOrderQtyId;
	}

	public ViewId getViewId()
	{
		return viewId;
	}

	public int getPP_Order_ID()
	{
		return ppOrderId;
	}

	public int getPP_Order_BOMLine_ID()
	{
		return ppOrderBOMLineId;
	}

	public int getPP_Order_Qty_ID()
	{
		return ppOrderQtyId;
	}

	@Override
	public DocumentPath getDocumentPath()
	{
		return documentPath;
	}

	@Override
	public DocumentId getDocumentId()
	{
		return documentId;
	}

	@Override
	public Set<String> getFieldNames()
	{
		return values.keySet();
	}

	@Override
	public Object getFieldValueAsJson(final String fieldName)
	{
		return values.get(fieldName);
	}

	@Override
	public Map<String, Object> getFieldNameAndJsonValues()
	{
		return values;
	}

	@Override
	public boolean isProcessed()
	{
		return processed;
	}

	@Override
	public PPOrderLineType getType()
	{
		return type;
	}

	public String getBOMLineType()
	{
		return (String)getFieldValueAsJson(IPPOrderBOMLine.COLUMNNAME_BOMType);
	}

	public JSONLookupValue getProduct()
	{
		return (JSONLookupValue)getFieldValueAsJson(IPPOrderBOMLine.COLUMNNAME_M_Product_ID);
	}

	public int getM_Product_ID()
	{
		final JSONLookupValue product = getProduct();
		return product == null ? -1 : product.getKeyAsInt();
	}

	public JSONLookupValue getUOM()
	{
		return (JSONLookupValue)getFieldValueAsJson(IPPOrderBOMLine.COLUMNNAME_C_UOM_ID);
	}

	public int getC_UOM_ID()
	{
		final JSONLookupValue uom = getUOM();
		return uom == null ? -1 : uom.getKeyAsInt();
	}

	public I_C_UOM getC_UOM()
	{
		final int uomId = getC_UOM_ID();
		return InterfaceWrapperHelper.load(uomId, I_C_UOM.class);
	}

	public BigDecimal getQty()
	{
		return (BigDecimal)getFieldValueAsJson(IPPOrderBOMLine.COLUMNNAME_Qty);
	}

	public BigDecimal getQtyPlan()
	{
		return (BigDecimal)getFieldValueAsJson(IPPOrderBOMLine.COLUMNNAME_QtyPlan);
	}

	public boolean isReceipt()
	{
		return getType().canReceive();
	}

	public boolean isIssue()
	{
		return getType().canIssue();
	}

	public boolean isNotProcessedCandidate()
	{
		return !isProcessed() && getPP_Order_Qty_ID() > 0;
	}

	@Override
	public List<PPOrderLineRow> getIncludedDocuments()
	{
		return includedDocuments;
	}

	@Override
	public boolean hasIncludedView()
	{
		return isIssue();
	}

	@Override
	public boolean hasAttributes()
	{
		return attributesSupplier != null;
	}

	@Override
	public IDocumentViewAttributes getAttributes() throws EntityNotFoundException
	{
		if (attributesSupplier == null)
		{
			throw new EntityNotFoundException("Document does not support attributes");
		}

		final IDocumentViewAttributes attributes = attributesSupplier.get();
		if (attributes == null)
		{
			throw new EntityNotFoundException("Document does not support attributes");
		}
		return attributes;
	}

	@Override
	public IDocumentViewSelection getCreateIncludedView(final IDocumentViewsRepository viewsRepo)
	{
		if (isIssue())
		{
			return getCreateHUsToIssueView(viewsRepo);
		}
		else
		{
			throw new EntityNotFoundException("Line " + this + " does not have an included view");
		}
	}

	private synchronized IDocumentViewSelection getCreateHUsToIssueView(final IDocumentViewsRepository viewsRepo)
	{
		if (husToIssueViewId != null)
		{
			final IDocumentViewSelection existingView = viewsRepo.getViewIfExists(husToIssueViewId);
			if (existingView != null)
			{
				return existingView;
			}
		}

		//
		// Create new view
		final IDocumentViewSelection newView = createHUsToIssueView(viewsRepo);
		husToIssueViewId = newView.getViewId();
		return newView;
	}

	private IDocumentViewSelection createHUsToIssueView(final IDocumentViewsRepository viewsRepo)
	{
		// TODO: move it to DAO/Repository
		// TODO: rewrite the whole shit, this is just prototyping
		final List<Integer> huIdsToAvailableToIssue = Services.get(IHandlingUnitsDAO.class)
				.createHUQueryBuilder()
				.setContext(Env.getCtx(), ITrx.TRXNAME_ThreadInherited)
				//
				.addHUStatusToInclude(X_M_HU.HUSTATUS_Active)
				.addOnlyWithProductId(getM_Product_ID())
				.setOnlyTopLevelHUs()
				//
				.createQuery()
				.listIds();
		if (huIdsToAvailableToIssue.isEmpty())
		{
			throw new EntityNotFoundException("No HUs to issue found");
		}

		return viewsRepo.createView(DocumentViewCreateRequest.builder(WEBUI_HU_Constants.WEBUI_HU_Window_ID, JSONViewDataType.grid)
				.setParentViewId(getViewId())
				.setFilterOnlyIds(huIdsToAvailableToIssue)
				.build());
	}

	public static final class Builder
	{
		private final WindowId windowId;
		private DocumentId _documentId;
		private PPOrderLineType type;

		private final Map<String, Object> values = new LinkedHashMap<>();

		private List<PPOrderLineRow> includedDocuments = null;

		private Supplier<? extends IDocumentViewAttributes> attributesSupplier;

		private final ViewId viewId;

		private int ppOrderId;
		private int ppOrderBOMLineId;
		private int ppOrderQtyId;
		private boolean processed = false;

		private Builder(@NonNull final ViewId viewId)
		{
			this.viewId = viewId;
			windowId = viewId.getWindowId();
		}

		public PPOrderLineRow build()
		{
			return new PPOrderLineRow(this);
		}

		public Builder ppOrder(final int ppOrderId)
		{
			this.ppOrderId = ppOrderId;
			ppOrderBOMLineId = -1;
			return this;
		}

		public Builder ppOrderBOMLineId(final int ppOrderId, final int ppOrderBOMLineId)
		{
			this.ppOrderId = ppOrderId;
			this.ppOrderBOMLineId = ppOrderBOMLineId;
			return this;
		}

		public Builder ppOrderQtyId(final int ppOrderQtyId)
		{
			this.ppOrderQtyId = ppOrderQtyId;
			return this;
		}

		public Builder processed(final boolean processed)
		{
			this.processed = processed;
			return this;
		}

		private DocumentPath getDocumentPath()
		{
			final DocumentId documentId = getDocumentId();
			return DocumentPath.rootDocumentPath(windowId, documentId);
		}

		public Builder setDocumentId(final DocumentId documentId)
		{
			_documentId = documentId;
			return this;
		}

		/** @return view row ID */
		private DocumentId getDocumentId()
		{
			Check.assumeNotNull(_documentId, "Parameter _documentId is not null");
			return _documentId;
		}

		private PPOrderLineType getType()
		{
			return type;
		}

		public Builder setType(final PPOrderLineType type)
		{
			this.type = type;
			return this;
		}

		public Builder setProcessed(final boolean processed)
		{
			this.processed = processed;
			return this;
		}

		public Builder putFieldValue(final String fieldName, final Object jsonValue)
		{
			if (jsonValue == null)
			{
				values.remove(fieldName);
			}
			else
			{
				values.put(fieldName, jsonValue);
			}

			return this;
		}

		public Builder setAttributesSupplier(Supplier<? extends IDocumentViewAttributes> attributesSupplier)
		{
			this.attributesSupplier = attributesSupplier;
			return this;
		}

		public Builder addIncludedDocument(final PPOrderLineRow includedDocument)
		{
			if (includedDocuments == null)
			{
				includedDocuments = new ArrayList<>();
			}

			includedDocuments.add(includedDocument);

			return this;
		}

		private List<PPOrderLineRow> buildIncludedDocuments()
		{
			if (includedDocuments == null || includedDocuments.isEmpty())
			{
				return ImmutableList.of();
			}

			return ImmutableList.copyOf(includedDocuments);
		}
	}

}
