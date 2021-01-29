package de.metas.document.references;

import com.google.common.collect.ImmutableList;
import de.metas.cache.CCache;
import de.metas.document.references.generic.DefaultGenericZoomInfoDescriptorsRepository;
import de.metas.document.references.generic.GenericZoomInfoDescriptor;
import de.metas.document.references.generic.GenericZoomInfoDescriptorsRepository;
import de.metas.document.references.generic.LegacyGenericZoomInfoDescriptorsRepository;
import de.metas.logging.LogManager;
import de.metas.util.Check;
import de.metas.util.Services;
import de.metas.util.lang.Priority;
import lombok.NonNull;
import org.adempiere.ad.element.api.AdWindowId;
import org.adempiere.ad.trx.api.ITrx;
import org.adempiere.service.ISysConfigBL;
import org.compiere.model.I_AD_Column;
import org.compiere.model.I_AD_Table;
import org.compiere.model.I_AD_Window;
import org.compiere.model.I_M_RMA;
import org.compiere.model.MQuery;
import org.compiere.model.MQuery.Operator;
import org.compiere.util.DB;
import org.slf4j.Logger;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Generic provider of zoom targets.
 *
 * @author Tobias Schoeneberg, www.metas.de - FR [ 2897194 ] Advanced Zoom and RelationTypes
 */
/* package */ class GenericZoomProvider implements IZoomProvider
{
	public static final GenericZoomProvider instance = new GenericZoomProvider();

	private static final Logger logger = LogManager.getLogger(GenericZoomProvider.class);

	private final CCache<String, ImmutableList<GenericZoomInfoDescriptor>> descriptorsBySourceKeyColumnName = CCache.<String, ImmutableList<GenericZoomInfoDescriptor>>builder()
			.cacheMapType(CCache.CacheMapType.LRU)
			.initialCapacity(1000) // NOTE this is confusing BUT in case of LRU, initialCapacity is used as maxSize
			.expireMinutes(CCache.EXPIREMINUTES_Never)
			.tableName(I_AD_Window.Table_Name)
			.additionalTableNameToResetFor(I_AD_Table.Table_Name)
			.additionalTableNameToResetFor(I_AD_Column.Table_Name)
			.build();

	private static final String SYSCONFIG_UseLegacyRepository = "de.metas.document.references.GenericZoomProvider.UseLegacyRepository";
	private final CCache<Integer, GenericZoomInfoDescriptorsRepository> genericZoomInfoDescriptorsRepositoryHolder = CCache.newCache("genericZoomInfoDescriptorsRepositoryHolder", 1, 0);

	private final Priority zoomInfoPriority = Priority.LOWEST;

	private GenericZoomProvider()
	{
	}

	@Override
	public ImmutableList<ZoomInfoCandidate> retrieveZoomInfos(
			@NonNull final IZoomSource source,
			@Nullable final AdWindowId targetWindowId)
	{
		final List<GenericZoomInfoDescriptor> zoomInfoDescriptors = getZoomInfoDescriptors(source.getKeyColumnNameOrNull());
		if (zoomInfoDescriptors.isEmpty())
		{
			return ImmutableList.of();
		}

		final ImmutableList.Builder<ZoomInfoCandidate> result = ImmutableList.builder();
		for (final GenericZoomInfoDescriptor zoomInfoDescriptor : zoomInfoDescriptors)
		{
			final AdWindowId windowId = zoomInfoDescriptor.getTargetAD_Window_ID();
			if (targetWindowId != null && !AdWindowId.equals(targetWindowId, windowId))
			{
				continue;
			}

			final MQuery query = buildMQuery(zoomInfoDescriptor, source);
			final ZoomInfoRecordsCountSupplier recordsCountSupplier = createRecordsCountSupplier(query, zoomInfoDescriptor, source.getTableName());

			result.add(ZoomInfoCandidate.builder()
					.id(ZoomInfoId.ofString("generic-" + windowId.getRepoId()))
					.internalName(zoomInfoDescriptor.getTargetWindowInternalName())
					.targetWindow(ZoomTargetWindow.ofAdWindowId(windowId))
					.priority(zoomInfoPriority)
					.query(query)
					.destinationDisplay(zoomInfoDescriptor.getName())
					.recordsCountSupplier(recordsCountSupplier)
					.build());
		}

		return result.build();
	}

	private List<GenericZoomInfoDescriptor> getZoomInfoDescriptors(final String sourceKeyColumnName)
	{
		if (sourceKeyColumnName == null)
		{
			return ImmutableList.of();
		}

		return descriptorsBySourceKeyColumnName.getOrLoad(sourceKeyColumnName, this::retrieveZoomInfoDescriptors);
	}

	private ImmutableList<GenericZoomInfoDescriptor> retrieveZoomInfoDescriptors(@NonNull final String sourceKeyColumnName)
	{
		final GenericZoomInfoDescriptorsRepository repository = genericZoomInfoDescriptorsRepositoryHolder.getOrLoad(0, this::createGenericZoomInfoDescriptorsRepository);
		return ImmutableList.copyOf(repository.getZoomInfoDescriptors(sourceKeyColumnName));
	}

	private GenericZoomInfoDescriptorsRepository createGenericZoomInfoDescriptorsRepository()
	{
		final ISysConfigBL sysConfigBL = Services.get(ISysConfigBL.class);
		final boolean useLegacyRepository = sysConfigBL.getBooleanValue(SYSCONFIG_UseLegacyRepository, false);
		return useLegacyRepository
				? new LegacyGenericZoomInfoDescriptorsRepository()
				: new DefaultGenericZoomInfoDescriptorsRepository();
	}

	private static MQuery buildMQuery(final GenericZoomInfoDescriptor zoomInfoDescriptor, final IZoomSource source)
	{
		final String targetTableName = zoomInfoDescriptor.getTargetTableName();
		final String targetColumnName = zoomInfoDescriptor.getTargetColumnName();

		//
		// Zoom by dynamic references AD_Table_ID/Record_ID
		// task "Zoomable Record_IDs" (03921)
		if (zoomInfoDescriptor.isDynamicTargetColumnName())
		{
			final MQuery query = new MQuery(targetTableName);
			query.addRestriction("AD_Table_ID", Operator.EQUAL, source.getAD_Table_ID());
			query.addRestriction("Record_ID", Operator.EQUAL, source.getRecord_ID());
			query.setZoomTableName(targetTableName);
			// query.setZoomColumnName(po.get_KeyColumns()[0]);
			query.setZoomValue(source.getRecord_ID());

			return query;
		}
		else
		{
			final MQuery query = new MQuery(targetTableName);
			if (zoomInfoDescriptor.isVirtualTargetColumnName())
			{
				// TODO: find a way to specify restriction's ColumnName and ColumnSql
				final String columnSql = zoomInfoDescriptor.getVirtualTargetColumnSql();
				query.addRestriction("(" + columnSql + ") = " + source.getRecord_ID());
			}
			else
			{
				query.addRestriction(targetColumnName, Operator.EQUAL, source.getRecord_ID());
			}

			if (!Check.isBlank(zoomInfoDescriptor.getTabSqlWhereClause()))
			{
				query.addRestriction(zoomInfoDescriptor.getTabSqlWhereClause());
			}

			query.setZoomTableName(targetTableName);
			query.setZoomColumnName(source.getKeyColumnNameOrNull());
			query.setZoomValue(source.getRecord_ID());

			return query;
		}
	}

	private static ZoomInfoRecordsCountSupplier createRecordsCountSupplier(
			final MQuery query,
			final GenericZoomInfoDescriptor zoomInfoDescriptor,
			final String sourceTableName)
	{
		final String sql = buildCountSQL(query, zoomInfoDescriptor, sourceTableName);
		return () -> {
			try
			{
				return DB.getSQLValueEx(ITrx.TRXNAME_None, sql);
			}
			catch (final Exception ex)
			{
				logger.warn("Failed counting records in {} for {} using SQL: {}", sourceTableName, zoomInfoDescriptor, sql, ex);
				return 0;
			}
		};
	}

	private static String buildCountSQL(
			final MQuery query,
			final GenericZoomInfoDescriptor zoomInfoDescriptor,
			final String sourceTableName)
	{
		String sqlCount = "SELECT COUNT(1) FROM " + query.getTableName() + " WHERE " + query.getWhereClause(false);

		Boolean isSO = zoomInfoDescriptor.getIsSOTrx();
		if (isSO != null && zoomInfoDescriptor.isTargetHasIsSOTrxColumn())
		{
			//
			// For RMA, Material Receipt window should be loaded for
			// IsSOTrx=true and Shipment for IsSOTrx=false
			// TODO: fetch the additional SQL from window's first tab where clause
			final AdWindowId AD_Window_ID = zoomInfoDescriptor.getTargetAD_Window_ID();
			if (I_M_RMA.Table_Name.equals(sourceTableName) && (AD_Window_ID.getRepoId() == 169 || AD_Window_ID.getRepoId() == 184))
			{
				isSO = !isSO;
			}

			// TODO: handle the case when IsSOTrx is a virtual column

			sqlCount += " AND IsSOTrx=" + DB.TO_BOOLEAN(isSO);
		}

		return sqlCount;
	}

}
