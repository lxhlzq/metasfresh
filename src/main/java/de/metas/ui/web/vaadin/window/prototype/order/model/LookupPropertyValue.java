package de.metas.ui.web.vaadin.window.prototype.order.model;

import java.util.Map;
import java.util.Set;

import com.google.common.base.MoreObjects;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableSet;
import com.google.gwt.thirdparty.guava.common.collect.ImmutableMap;

import de.metas.ui.web.vaadin.window.descriptor.DataFieldLookupDescriptor;
import de.metas.ui.web.vaadin.window.prototype.order.PropertyDescriptor;
import de.metas.ui.web.vaadin.window.prototype.order.PropertyName;
import de.metas.ui.web.vaadin.window.prototype.order.WindowConstants;

/*
 * #%L
 * metasfresh-webui
 * %%
 * Copyright (C) 2016 metas GmbH
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 2 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-2.0.html>.
 * #L%
 */

/**
 * Lookup values provider as {@link PropertyValue} node.
 * 
 * @author metas-dev <dev@metasfresh.com>
 *
 */
public class LookupPropertyValue implements PropertyValue
{
	private final PropertyName propertyName;
	/** i.e. the value */
	private final LookupDataSource lookupDataSource;

	LookupPropertyValue(final PropertyDescriptor descriptor)
	{
		super();
		final PropertyName propertyName = descriptor.getPropertyName();
		this.propertyName = WindowConstants.lookupValuesName(propertyName);

		final DataFieldLookupDescriptor sqlLookupDescriptor = descriptor.getSqlLookupDescriptor();
		this.lookupDataSource = new SqlLazyLookupDataSource(sqlLookupDescriptor);
	}

	@Override
	public String toString()
	{
		return MoreObjects.toStringHelper(this)
				.add("name", propertyName)
				.toString();
	}

	@Override
	public PropertyName getName()
	{
		return propertyName;
	}

	@Override
	public Set<PropertyName> getDependsOnPropertyNames()
	{
		return ImmutableSet.of();
	}

	@Override
	public void onDependentPropertyValueChanged(final PropertyValueCollection values, final PropertyName changedPropertyName)
	{
		// TODO: update lookup datasource in case it's filtering depends on some other properties
	}

	@Override
	public Object getValue()
	{
		return lookupDataSource;
	}

	@Override
	public Optional<String> getValueAsString()
	{
		return Optional.absent();
	}

	@Override
	public void setValue(final Object value)
	{
	}

	public Object getInitialValue()
	{
		return null;
	}

	@Override
	public Map<PropertyName, PropertyValue> getChildPropertyValues()
	{
		return ImmutableMap.of();
	}

	@Override
	public String getComposedValuePartName()
	{
		return null;
	}

	@Override
	public boolean isChanged()
	{
		return false;
	}
	
	@Override
	public boolean isReadOnlyForUser()
	{
		return true;
	}
}
