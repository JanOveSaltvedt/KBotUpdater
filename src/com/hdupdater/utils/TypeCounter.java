/*	
	Copyright 2012 Jan Ove Saltvedt
	
	This file is part of KBot.

    KBot is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    KBot is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with KBot.  If not, see <http://www.gnu.org/licenses/>.
	
*/



package com.hdupdater.utils;

import com.sun.org.apache.bcel.internal.generic.Type;
import com.sun.org.apache.bcel.internal.generic.BasicType;
import com.sun.org.apache.bcel.internal.generic.ObjectType;

/**
 * Created by IntelliJ IDEA.
 * User: Jan Ove Saltvedt
 * Date: Sep 19, 2009
 * Time: 6:45:14 PM
 * To change this template use File | Settings | File Templates.
 */
public class TypeCounter {
    public static int getCount(Type[] types, Type compareType){
        int count = 0;
        for(Type type: types){
            if(type.equals(compareType)){
                count++;
            }
        }
        return count;
    }

    public static int getIntCount(Type[] types){
        return getCount(types, Type.INT);
    }

    public static int getByteCount(Type[] types){
        return getCount(types, Type.BYTE);
    }

    public static int getBasicTypeCount(Type[] types){
        int count = 0;
        for(Type type: types){
            if(type instanceof BasicType){
                count++;
            }
        }
        return count;
    }

    public static int getObjectCount(Type[] types) {
        int objectCount = 0;
        for(Type arg: types){
            if(arg instanceof ObjectType){
                objectCount++;
            }
        }
        return objectCount;
    }
}
