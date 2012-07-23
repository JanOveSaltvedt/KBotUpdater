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
import com.sun.org.apache.bcel.internal.generic.*;
import com.sun.org.apache.bcel.internal.classfile.Method;
import com.sun.org.apache.bcel.internal.util.InstructionFinder;

import java.util.Iterator;

/**
 * Created by IntelliJ IDEA.
 * User: Jan Ove Saltvedt
 * Date: Oct 13, 2009
 * Time: 6:24:44 PM
 * To change this template use File | Settings | File Templates.
 */
public class ModelSDTransformer extends AbstractTransformer {

    @Override
    public Class<?>[] getDependencies() {
        return new Class<?>[]{ModelTransformer.class, RenderVarsTransformer.class};
    }

    public void run() {
        ClassGen cG = hookHandler.getClassByNick("ModelSD");
        ConstantPoolGen cpg = cG.getConstantPool();
        ClassGen renderVarsCg = hookHandler.getClassByNick("RenderVars");
        for (Method method : cG.getMethods()) {
            if (method.isStatic()) {
                continue;
            }
            // final boolean isPointWithinBounds(int compareX, int compareY, RenderVars renderVars, boolean justCheckBox)
            if (!method.getReturnType().equals(Type.BOOLEAN)) {
                continue;
            }

            Type[] args = method.getArgumentTypes();
            if (args.length < 4 || args.length > 5) {
                continue;
            }
            int intCount = TypeCounter.getIntCount(args);
            int byteCount = TypeCounter.getByteCount(args);
            if (intCount < 2 || intCount + byteCount < 2 || intCount + byteCount > 3) {
                continue;
            }
            int booleanCount = TypeCounter.getCount(args, Type.BOOLEAN);
            if (booleanCount != 1 || TypeCounter.getCount(args, Type.getType("L" + renderVarsCg.getClassName() + ";")) != 1) {
                continue;
            }
            InstructionFinder instructionFinder = new InstructionFinder(new InstructionList(method.getCode().getCode()));
            String pattern = "getfield ConstantPushInstruction aload getfield iastore";
            int minHits = 0;
            int maxHits = 0;
            InstructionHandle lastBoundIH = null;
            for (Iterator<InstructionHandle[]> it = (Iterator<InstructionHandle[]>) instructionFinder.search(pattern); it.hasNext();) {
                InstructionHandle[] ih = it.next();
                FieldInstruction arrayFieldInstruction = (FieldInstruction) ih[0].getInstruction();
                if(!arrayFieldInstruction.getFieldType(cpg).equals(Type.getType(int[].class))){
                    continue;
                }
                FieldInstruction boundFieldInstruction = (FieldInstruction) ih[3].getInstruction();
                if(!boundFieldInstruction.getFieldType(cpg).equals(Type.SHORT)){
                    continue;
                }
                ConstantPushInstruction pushInstruction = (ConstantPushInstruction) ih[1].getInstruction();
                if(pushInstruction.getValue().intValue() == 0){
                    minHits++;
                    if(minHits == 1){
                        hookHandler.addFieldHook("ModelSD", boundFieldInstruction, cpg, "minX");
                    }
                    else if(minHits == 2){
                        hookHandler.addFieldHook("ModelSD", boundFieldInstruction, cpg, "minY");
                    }
                    else if(minHits == 3){
                        hookHandler.addFieldHook("ModelSD", boundFieldInstruction, cpg, "minZ");
                    }
                }
                else if(pushInstruction.getValue().intValue() == 7){
                    maxHits++;
                    if(maxHits == 1){
                        hookHandler.addFieldHook("ModelSD", boundFieldInstruction, cpg, "maxX");
                    }
                    else if(maxHits == 2){
                        hookHandler.addFieldHook("ModelSD", boundFieldInstruction, cpg, "maxY");
                    }
                    else if(maxHits == 3){
                        hookHandler.addFieldHook("ModelSD", boundFieldInstruction, cpg, "maxZ");
                        lastBoundIH = ih[4];
                    }
                }


            }

            if(lastBoundIH == null){
                continue;
            }

            int hit = 0;
            for (Iterator<InstructionHandle[]> it = (Iterator<InstructionHandle[]>) instructionFinder.search("getfield iload iaload istore", lastBoundIH); it.hasNext();) {
                InstructionHandle[] ih = it.next();
                FieldInstruction fieldInstruction = (FieldInstruction) ih[0].getInstruction();
                if(!fieldInstruction.getType(cpg).equals(Type.getType(int[].class))){
                    continue;
                }
                hit++;
                if(hit == 4){
                    if(hookHandler.getFieldHook("ModelSD", "xPoints") == null){
                        hookHandler.addFieldHook("ModelSD", fieldInstruction, cpg, "xPoints");
                    }
                }
                else if(hit == 5){
                    if(hookHandler.getFieldHook("ModelSD", "yPoints") == null){
                        hookHandler.addFieldHook("ModelSD", fieldInstruction, cpg, "yPoints");
                    }
                }
                else if(hit == 6){
                    if(hookHandler.getFieldHook("ModelSD", "zPoints") == null){
                        hookHandler.addFieldHook("ModelSD", fieldInstruction, cpg, "zPoints");
                    }
                }
            }
            hit = 0;
            for (Iterator<InstructionHandle[]> it = (Iterator<InstructionHandle[]>) instructionFinder.search("getfield aload getfield iload saload iaload"); it.hasNext();) {
                InstructionHandle[] ih = it.next();
                FieldInstruction fieldInstruction = (FieldInstruction) ih[2].getInstruction();
                if(!fieldInstruction.getType(cpg).equals(Type.getType(short[].class))){
                    continue;
                }
                hit++;
                if(hit == 1){
                    if(hookHandler.getFieldHook("ModelSD", "indices1") == null){
                        hookHandler.addFieldHook("ModelSD", fieldInstruction, cpg, "indices1");
                    }
                }
                else if(hit == 2){
                    if(hookHandler.getFieldHook("ModelSD", "indices2") == null){
                        hookHandler.addFieldHook("ModelSD", fieldInstruction, cpg, "indices2");
                    }
                }
                else if(hit == 3){
                    if(hookHandler.getFieldHook("ModelSD", "indices3") == null){
                        hookHandler.addFieldHook("ModelSD", fieldInstruction, cpg, "indices3");
                    }
                }
            }
        }
    }

    public String[] getFieldHooks() {
        return new String[]{
                "ModelSD::minX",
                "ModelSD::minY",
                "ModelSD::minZ",
                "ModelSD::maxX",
                "ModelSD::maxY",
                "ModelSD::maxZ",
                "ModelSD::xPoints",
                "ModelSD::yPoints",
                "ModelSD::zPoints",
                "ModelSD::indices1",
                "ModelSD::indices2",
                "ModelSD::indices3",
        };  //To change body of implemented methods use File | Settings | File Templates.
    }
}
