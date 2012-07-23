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
import com.sun.org.apache.bcel.internal.generic.*;
import com.sun.org.apache.bcel.internal.classfile.Method;
import com.sun.org.apache.bcel.internal.classfile.Field;
import com.sun.org.apache.bcel.internal.util.InstructionFinder;

import java.util.Iterator;

/**
 * Created by IntelliJ IDEA.
 * User: Jan Ove Saltvedt
 * Date: Oct 4, 2009
 * Time: 11:48:56 AM
 * To change this template use File | Settings | File Templates.
 */
public class Plane2DTransformer extends AbstractTransformer {
    @Override
    public Class<?>[] getDependencies() {
        return new Class<?>[]{PlaneTransformer.class};
    }

    public void run() {
        ClassGen planeCG = hookHandler.getClassByNick("Plane");
        if (planeCG == null) {
            new RuntimeException("Plane class not found.");
        }
        String getHeightMethodName = (String) hookHandler.dataMap.get("Plane::getHeight()")[0];
        String getHeightMethodSignature = (String) hookHandler.dataMap.get("Plane::getHeight()")[1];
        for (ClassGen cG : classes) {
            ConstantPoolGen cpg = cG.getConstantPool();
            if (!cG.getSuperclassName().equals(planeCG.getClassName())) {
                continue;
            }

            int noOf3DArrays = 0;
            for(Field field: cG.getFields()){
                if(field.isStatic()){
                    continue;
                }
                if(field.getType() instanceof ArrayType){
                    ArrayType arrayType = (ArrayType) field.getType();
                    if(arrayType.getDimensions() == 3){
                        noOf3DArrays++;
                    }
                }
            }

            if(noOf3DArrays != 0){
                continue;
            }

            hookHandler.addClassNick("Plane2D", cG);

            for (Method method : cG.getMethods()) {
                if (method.isStatic()) {
                    continue;
                }
                if(method.isAbstract() || method.isNative()){
                    continue;
                }
                if (!method.getName().equals(getHeightMethodName)) {
                    continue;
                }
                if (!method.getSignature().equals(getHeightMethodSignature)) {
                    continue;
                }

                InstructionFinder instructionFinder = new InstructionFinder(new InstructionList(method.getCode().getCode()));
                for (Iterator<InstructionHandle[]> iterator = instructionFinder.search("getfield iload aaload iload iaload"); iterator.hasNext();) {
                    InstructionHandle[] ih = iterator.next();
                    FieldInstruction fieldInstruction = (FieldInstruction) ih[0].getInstruction();
                    hookHandler.addFieldHook("Plane2D", fieldInstruction, cpg, "tileHeights");
                }
            }
        }
    }

    public String[] getFieldHooks() {
        return new String[]{
                "Plane2D::tileHeights",
        };  //To change body of implemented methods use File | Settings | File Templates.
    }
}
