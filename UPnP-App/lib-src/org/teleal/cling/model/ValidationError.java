/*
 * Copyright (C) 2010 Teleal GmbH, Switzerland
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.teleal.cling.model;


public class ValidationError
{
    private Class clazz;
    private String propertyName;
    private String message;

    public ValidationError(Class clazz, String message)
    {
        this.clazz = clazz;
        this.message = message;
    }

    public ValidationError(Class clazz, String propertyName, String message)
    {
        this.clazz = clazz;
        this.propertyName = propertyName;
        this.message = message;
    }

    public Class getClazz()
    {
        return clazz;
    }

    public String getPropertyName()
    {
        return propertyName;
    }

    public String getMessage()
    {
        return message;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName()
                + " (Class: " + getClazz().getSimpleName()
                + ", propertyName: " + getPropertyName() + "): "
                + message;
    }
}