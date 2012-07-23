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
import com.hdupdater.hooks.*;
import com.hdupdater.utils.TypeCounter;
import com.hdupdater.utils.InstructionUtils;
import com.hdupdater.utils.TypeBuilder;
import com.sun.org.apache.bcel.internal.Constants;
import com.sun.org.apache.bcel.internal.generic.*;
import com.sun.org.apache.bcel.internal.classfile.Method;
import com.sun.org.apache.bcel.internal.classfile.Field;
import com.sun.org.apache.bcel.internal.util.InstructionFinder;

import java.util.Iterator;

/**
 * Created by IntelliJ IDEA.
 * User: Jan Ove Saltvedt
 * Date: Dec 8, 2009
 * Time: 5:29:13 PM
 * To change this template use File | Settings | File Templates.
 */
public class GroundObjectsTransformer extends AbstractTransformer {
    @Override
    public Class<?>[] getDependencies() {
        return new Class<?>[]{NodeCacheTransformer.class, TileDataTransformer.class, RenderableTransformer.class, ModelTransformer.class};
    }

    public void run() {
        ClassGen renderableCG = hookHandler.getClassByNick("Renderable");
        ClassGen modelCG = hookHandler.getClassByNick("Model");
        ClassGen groundObjectCG = null;
        for(ClassGen cG: classes){
            if(!cG.getSuperclassName().equals(renderableCG.getClassName())){
                continue;
            }

            int subCount = 0;
            for(ClassGen cG2: classes){
                if(cG2.getSuperclassName().equals(cG.getClassName())){
                    subCount++;
                }
            }
            if(subCount != 1){
                continue;
            }
            hookHandler.addClassNick("GroundObject", cG);
            groundObjectCG = cG;
            for(ClassGen cG2: normalClasses){
                ConstantPoolGen cpg = cG2.getConstantPool();
                if(cG2.getSuperclassName().equals(cG.getClassName())){
                    Type modelType = Type.getType("L" + modelCG.getClassName() + ";");
                    hookHandler.injectField(cG2, "model1", modelType, Constants.ACC_PUBLIC);
                    hookHandler.injectField(cG2, "model2", modelType, Constants.ACC_PUBLIC);
                    hookHandler.injectField(cG2, "model3", modelType, Constants.ACC_PUBLIC);
                    new VirtualFieldGetterHook(hookHandler, cG2, "model1", modelType, TypeBuilder.createHookType("Model"), "GroundObject");
                    new VirtualFieldGetterHook(hookHandler, cG2, "model2", modelType, TypeBuilder.createHookType("Model"), "GroundObject");
                    new VirtualFieldGetterHook(hookHandler, cG2, "model3", modelType, TypeBuilder.createHookType("Model"), "GroundObject");

                    for(Method method: cG2.getMethods()){
                        if(method.isStatic()){
                            continue;
                        }
                        if(!method.getReturnType().equals(Type.BOOLEAN)){
                            continue;
                        }
                        /*
                        if(!(method.getReturnType() instanceof ObjectType)){
                            continue;
                        } */
                        Type[] args = method.getArgumentTypes();
                        if(args.length < 3 || args.length > 4){
                            continue;
                        }
                        if(TypeCounter.getObjectCount(args) != 1){
                            continue;
                        }
                        InstructionFinder instructionFinder = new InstructionFinder(new InstructionList(method.getCode().getCode()));
                        int count = 0;
                        int byteOffset = 0;
                        for (Iterator<InstructionHandle[]> iterator = instructionFinder.search("invokevirtual astore"); iterator.hasNext();) {
                            InstructionHandle[] ih = iterator.next();
                            InvokeInstruction invokeInstruction = (InvokeInstruction) ih[0].getInstruction();
                            if(!invokeInstruction.getReturnType(cpg).equals(modelType)){
                                continue;
                            }
                            count++;
                            ASTORE astore = (ASTORE) ih[1].getInstruction();
                            new ModelDumperHook(hookHandler, cG2, method, ih[1].getPosition()+byteOffset, modelCG, astore.getIndex(), "model"+count);
                            InstructionList newInstructionList = new InstructionList();
                            newInstructionList.append(InstructionConstants.ALOAD_0);
                            newInstructionList.append(new ALOAD(astore.getIndex()));
                            newInstructionList.append(new InstructionFactory(cpg).createFieldAccess(cG2.getClassName(),"model", TypeBuilder.createHookType("Model"), Constants.PUTFIELD));
                            newInstructionList.setPositions();
                            byteOffset += newInstructionList.getByteCode().length;
                            InstructionHandle[][] parameters = InstructionUtils.getParameters(ih[0], cpg, 1);
                            for(InstructionHandle handle: parameters[parameters.length-1]){
                                if(handle.getInstruction() instanceof INVOKEVIRTUAL){
                                    InstructionHandle[][] parameters2 = InstructionUtils.getParameters(handle, cpg, 1);
                                    for(InstructionHandle[] handles: parameters2){
                                        for(InstructionHandle i: handles){
                                            if(i.getInstruction() instanceof GETFIELD){
                                                FieldInstruction fieldInstruction = (FieldInstruction) i.getInstruction();
                                                hookHandler.addFieldHook("GroundObject", fieldInstruction, cpg, "ID"+count);
                                            }
                                        }
                                    }
                                }
                            }
                        }

                    }

                    Method setOffsets2Method = (Method) hookHandler.dataMap.get("RenderVars::setOffsets2")[0];
                    for(Method method: cG2.getMethods()){
                        if(method.isStatic()){
                            continue;
                        }
                        if(method.getArgumentTypes().length < 3 || TypeCounter.getObjectCount(method.getArgumentTypes()) != 1){
                            continue;
                        }
                        InstructionFinder instructionFinder = new InstructionFinder(new InstructionList(method.getCode().getCode()));
                        SEARCH_LOOP:
                        for (Iterator<InstructionHandle[]> iterator = instructionFinder.search("aload_0 getfield aload_0 getfield aload_0 getfield InvokeInstruction"); iterator.hasNext();) {
                            InstructionHandle[] ih = iterator.next();
                            InvokeInstruction invokeInstruction = (InvokeInstruction) ih[ih.length-1].getInstruction();
                            if(!invokeInstruction.getMethodName(cpg).equals(setOffsets2Method.getName())
                                    || !invokeInstruction.getSignature(cpg).equals(setOffsets2Method.getSignature())){
                                continue;
                            }


                            FieldInstruction fieldInstruction = (FieldInstruction) ih[1].getInstruction();
                            for(Hook hook: hookHandler.hooks){
                                if(hook instanceof FieldHook){
                                    FieldHook fieldHook = (FieldHook) hook;
                                    if(fieldHook.getFieldName().equals(fieldInstruction.getFieldName(cpg))
                                            && fieldHook.getFieldCG().getClassName().equals(fieldInstruction.getClassName(cpg))){
                                        continue SEARCH_LOOP;
                                    }
                                }
                            }
                            hookHandler.addFieldHook("GroundObject", fieldInstruction, cpg, "posX");
                            fieldInstruction = (FieldInstruction) ih[5].getInstruction();
                            hookHandler.addFieldHook("GroundObject", fieldInstruction, cpg, "posY");
                            fieldInstruction = (FieldInstruction) ih[3].getInstruction();
                            hookHandler.addFieldHook("GroundObject", fieldInstruction, cpg, "posZ");
                        }
                    }
                }
            }
        }
        if(groundObjectCG == null){
            return;
        }

        ClassGen tileDataCG = hookHandler.getClassByNick("TileData");
        for(Field field: tileDataCG.getFields()){
            if(field.isStatic()){
                continue;
            }
            if(field.getSignature().equals("L"+groundObjectCG.getClassName()+";")){
                hookHandler.addFieldHook("TileData", tileDataCG, field, "groundObject", TypeBuilder.createHookType("GroundObject"));
            }
        }


        ClassGen nodeCacheCG = hookHandler.getClassByNick("NodeCache");

        for (ClassGen cG : classes) {
            ConstantPoolGen cpg = cG.getConstantPool();
            for (Method method : cG.getMethods()) {
                if (!method.isStatic()) {
                    continue;
                }
                if (!method.getReturnType().equals(Type.VOID)) {
                    continue;
                }
                if(method.isAbstract() || method.getCode() == null){
                    continue;
                }
                Type[] args = method.getArgumentTypes();
                if (args.length < 2 || args.length > 3) {
                    continue;
                }
                if (TypeCounter.getIntCount(args) > 2) {
                    continue;
                }
                if (TypeCounter.getObjectCount(args) != 1) {
                    continue;
                }
                InstructionHandle ishl14Handle = null;
                int count14 = 0;
                int count28 = 0;
                InstructionHandle ishl28Handle = null;
                InstructionFinder instructionFinder = new InstructionFinder(new InstructionList(method.getCode().getCode()));
                for (Iterator<InstructionHandle[]> iterator = instructionFinder.search("ishl"); iterator.hasNext();) {
                    InstructionHandle[] ih = iterator.next();

                    if (ih[0].getPrev().getInstruction() instanceof ConstantPushInstruction) {
                        ConstantPushInstruction pushInstruction = (ConstantPushInstruction) ih[0].getPrev().getInstruction();
                        if (pushInstruction.getValue().intValue() == 14) {
                            ishl14Handle = ih[0];
                            count14++;
                        } else if (pushInstruction.getValue().intValue() == 28) {
                            ishl28Handle = ih[0];
                            count28++;
                        }
                    } else if (ih[0].getPrev().getInstruction() instanceof LDC) {
                        LDC ldc = (LDC) ih[0].getPrev().getInstruction();
                        if (ldc.getType(cpg).equals(Type.INT)) {
                            if ((Integer) ldc.getValue(cpg) == 14) {
                                ishl14Handle = ih[0];
                                count14++;
                            } else if ((Integer) ldc.getValue(cpg) == 28) {
                                ishl28Handle = ih[0];
                                count28++;
                            }
                        }
                    }
                }
                if (ishl14Handle == null || ishl28Handle == null) {
                    continue;
                }
                if (count14 < 2 || count14 > 4) {
                    continue;
                }
                if (count28 < 2 || count28 > 4) {
                    continue;
                }
                Iterator<InstructionHandle[]> iterator = instructionFinder.search("InvokeInstruction", ishl14Handle);
                if (!iterator.hasNext()) {
                    continue;
                }
                InstructionHandle[] ih = iterator.next();
                InvokeInstruction invokeInstruction = (InvokeInstruction) ih[0].getInstruction();
                if (!invokeInstruction.getClassName(cpg).equals(nodeCacheCG.getClassName())) {
                    continue;
                }
                InstructionHandle current = ih[0].getPrev();
                while(current != null){
                    if(current.getInstruction() instanceof GETSTATIC){
                        FieldInstruction fieldInstruction = (FieldInstruction) current.getInstruction();
                        if(fieldInstruction.getFieldType(cpg).getSignature().equals("L"+nodeCacheCG.getClassName()+";")){
                            hookHandler.addClientHook(fieldInstruction, cpg, "groundObjectCache", TypeBuilder.createHookType("NodeCache"));
                            break;
                        }
                    }
                    current = current.getPrev();
                }

            }
        }

    }

    public String[] getFieldHooks() {
        return new String[]{
                "TileData::groundObject",
                "GroundObject::model1",
                "GroundObject::model2",
                "GroundObject::model3",
                "GroundObject::ID1",
                "GroundObject::ID2",
                "GroundObject::ID3",
                "GroundObject::posX",
                "GroundObject::posY",
                "GroundObject::posZ",
                "Client::groundObjectCache",
        };  //To change body of implemented methods use File | Settings | File Templates.
    }
}
