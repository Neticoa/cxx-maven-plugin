package org.apache.maven.plugin.cxx.utils;

/*
 * Copyright (C) 2011-2016, Neticoa SAS France - Tous droits réservés.
 * Author(s) : Franck Bonin, Neticoa SAS France
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
 
import java.lang.reflect.Field;

/**
 * TODO
 * 
 * @version 8 mars 2016
 * @author fbonin
 */
public class ClassAccessHelper
{
    public static <T> T getFieldValue( Object o, String fieldName, Class<T> cls )
        throws NoSuchFieldException, IllegalAccessException
    {
        Field field = o.getClass().getDeclaredField( fieldName );
        if ( null != field )
        {
            field.setAccessible( true );
            Object value = field.get( o );
            if ( null != value && cls.isInstance( value ) )
            {
                return (T)value;
            }
        }
        return null;
    }
    
    public static <T> void setFieldValue( Object o, String fieldName, T value )
        throws NoSuchFieldException, IllegalAccessException
    {
        Field field = o.getClass().getDeclaredField( fieldName );
        if ( null != field )
        {
            field.setAccessible( true );
            field.set( o, value );
        }
    }
}
