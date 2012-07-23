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



package com.hdupdater.transformers;

import com.hdupdater.AbstractTransformer;
import com.hdupdater.utils.TypeCounter;
import com.sun.org.apache.bcel.internal.generic.ClassGen;
import com.sun.org.apache.bcel.internal.generic.ConstantPoolGen;
import com.sun.org.apache.bcel.internal.generic.Type;
import com.sun.org.apache.bcel.internal.classfile.Field;
import com.sun.org.apache.bcel.internal.classfile.Method;

/**
 * Created by IntelliJ IDEA.
 * User: Jan Ove Saltvedt
 * Date: Oct 7, 2009
 * Time: 1:44:14 PM
 * To change this template use File | Settings | File Templates.
 */
public class MLStringTransformer extends AbstractTransformer {
    public void run() {
        for(ClassGen cG: classes){
            ConstantPoolGen cpg = cG.getConstantPool();
            int fieldCount = 0;
            int stringArrayCount = 0;
            for(Field field: cG.getFields()){
                if(field.isStatic()){
                    continue;
                }
                fieldCount++;
                if(field.getType().equals(Type.getType(String[].class))){
                    stringArrayCount++;
                }
            }
            if(fieldCount != 1 && stringArrayCount != 1){
                continue;
            }

            int constructorCount = 0;
            int stringConstructors = 0;
            for(Method method: cG.getMethods()){
                if(!method.getName().equals("<init>")){
                    continue;
                }
                constructorCount++;
                if(method.getArgumentTypes().length == 4){
                    int stringCount = TypeCounter.getCount(method.getArgumentTypes(), Type.STRING);
                    if(stringCount == 4){
                        stringConstructors++;
                    }
                }
            }

            if(constructorCount != 1 || stringConstructors != 1){
                continue;
            }

            hookHandler.addClassNick("MLString", cG);

            for(Field field: cG.getFields()){
                if(field.isStatic()){
                    continue;
                }
                if(field.getType().equals(Type.getType(String[].class))){
                    hookHandler.addFieldHook("MLString", cG, field, "stringArray");
                }
            }

        }
    }

    public String[] getFieldHooks() {
        return new String[]{
                "MLString::stringArray",
        };  //To change body of implemented methods use File | Settings | File Templates.
    }
}
