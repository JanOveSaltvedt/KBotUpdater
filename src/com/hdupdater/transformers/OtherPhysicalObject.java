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
import com.hdupdater.utils.TypeBuilder;
import com.hdupdater.utils.TypeCounter;
import com.sun.org.apache.bcel.internal.classfile.Field;
import com.sun.org.apache.bcel.internal.classfile.Method;
import com.sun.org.apache.bcel.internal.generic.*;
import com.sun.org.apache.bcel.internal.util.InstructionFinder;

import java.util.Iterator;

/**
 * Created by IntelliJ IDEA.
 * User: Jan Ove Saltvedt
 * Date: Oct 18, 2009
 * Time: 12:31:54 PM
 * To change this template use File | Settings | File Templates.
 */
public class OtherPhysicalObject extends AbstractTransformer {
    @Override
    public Class<?>[] getDependencies() {
        return new Class<?>[]{ModelTransformer.class, RenderableTransformer.class};
    }

    private int hits = 0;

    public void run() {
        ClassGen modelCG = hookHandler.getClassByNick("Model");
        ClassGen renderableCG = hookHandler.getClassByNick("Renderable");
        ClassGen gameObjectCG = hookHandler.getClassByNick("GameObject");
        ClassGen IPOCG = hookHandler.getClassByNick("InteractivePhysicalObject");
        ClassGen modelledObjectCG = (ClassGen) hookHandler.dataMap.get("ModelledObjectInterface")[0];
        Method getIDMethod = (Method) hookHandler.dataMap.get("ModelledObjectInterface::getID()")[0];
        Method getModelMethod = null;
        Method setOffsets2Method = (Method) hookHandler.dataMap.get("RenderVars::setOffsets2")[0];
        for (Method method : modelledObjectCG.getMethods()) {
            if (method.getReturnType().getSignature().equals("L" + modelCG.getClassName() + ";")) {
                getModelMethod = method;
            }
        }
        for (ClassGen cG : classes) {
            final ConstantPoolGen cpg = cG.getConstantPool();
            String[] interfaces = cG.getInterfaceNames();
            if (interfaces.length < 1) {
                continue;
            }
            boolean isModelledObject = false;
            for (String face : interfaces) {
                if (face.equals(modelledObjectCG.getClassName())) {
                    isModelledObject = true;
                }
            }
            if (!isModelledObject) {
                continue;
            }
            if (cG.equals(IPOCG)) {
                continue;
            }

            hits++;
            hookHandler.addInterfaceHook(cG, "PhysicalObject");
            boolean hookedModel = false;
            for (Field field : cG.getFields()) {
                if (field.isStatic()) {
                    continue;
                }
                if (field.getSignature().equals("L" + modelCG.getClassName() + ";")) {
                    hookHandler.addFieldHook("PhysicalObject" + (hits), cG, field, "model", TypeBuilder.createHookType("Model"));
                    hookedModel = true;
                }
            }

            boolean hookedID = false;
            for (Method method : cG.getMethods()) {
                if (method.isStatic()) {
                    continue;
                }
                if (method.getName().equals(getIDMethod.getName())
                        && method.getSignature().equals(getIDMethod.getSignature())) {
                    InstructionFinder instructionFinder = new InstructionFinder(new InstructionList(method.getCode().getCode()));
                    InstructionFinder.CodeConstraint codeConstraint = new InstructionFinder.CodeConstraint() {
                        public boolean checkCode(InstructionHandle[] match) {
                            for (InstructionHandle insH : match) {
                                if (insH.getInstruction() instanceof LDC) {
                                    if (((LDC) insH.getInstruction()).getValue(cpg).equals(0xFFFF)) {
                                        return true;
                                    }
                                } else if (insH.getInstruction() instanceof SIPUSH) {
                                    if (((SIPUSH) insH.getInstruction()).getValue().intValue() == 0xFFFF) {
                                        return true;
                                    }
                                }
                            }
                            return false;  //To change body of implemented methods use File | Settings | File Templates.
                        }
                    };
                    for (Iterator<InstructionHandle[]> iterator = instructionFinder.search("(((ldc)|(sipush)) aload getfield)|(aload getfield((ldc)|(sipush))) iand ireturn", codeConstraint); iterator.hasNext();) {
                        InstructionHandle[] ih = iterator.next();
                        for (InstructionHandle i : ih) {
                            if (i.getInstruction() instanceof GETFIELD) {
                                FieldInstruction fieldInstruction = (FieldInstruction) i.getInstruction();
                                if (!hasIDHook(cG)) {
                                    hookHandler.addFieldHook("PhysicalObject" + (hits), fieldInstruction, cpg, "ID");
                                }
                                new ReturnSelfHook(hookHandler, "PhysicalObject" + (hits), cG, "dataContainer", TypeBuilder.createHookType("DataContainer"));
                                hookHandler.addInterfaceHook(cG, "DataContainer");
                                hookedID = true;
                            }
                        }
                    }
                    if (!hookedID) {
                        for (Iterator<InstructionHandle[]> iterator = instructionFinder.search("aload_0 getfield getfield ireturn"); iterator.hasNext();) {
                            InstructionHandle[] ih = iterator.next();

                            FieldInstruction containerFieldInstruction = (FieldInstruction) ih[1].getInstruction();

                            FieldInstruction fieldInstruction = (FieldInstruction) ih[2].getInstruction();

                            //hookHandler.addFieldHook("PhysicalObject" + (hits), containerFieldInstruction, cpg, "dataContainer", TypeBuilder.createHookType("DataContainer"));
                            //ClassGen field2CG = hookHandler.classes.get(fieldInstruction.getClassName(cpg));
                            /*if (hookHandler.getFieldHook("DataContainer", "ID") == null) {
                                hookHandler.addFieldHook("DataContainer", fieldInstruction, cpg, "ID", Type.SHORT);

                                hookHandler.addInterfaceHook(field2CG, "DataContainer");
                                if (!hookedModel) {
                                    new InjectFieldHook(hookHandler, field2CG, "model", "L" + modelCG.getClassName() + ";", 0);
                                    Type modelType = Type.getType("L" + modelCG.getClassName() + ";");
                                    new VirtualFieldGetterHook(hookHandler, field2CG, "model", modelType, TypeBuilder.createHookType("Model"), "DataContainer");
                                    ClassGen unobbedCG = null;
                                    for(ClassGen cG2: normalClasses){
                                        if(cG2.getClassName().equals(field2CG.getClassName())){
                                            unobbedCG = cG2;
                                        }
                                    }
                                    for (Method method2 : unobbedCG.getMethods()) {
                                        if (method2.isStatic()) {
                                            continue;
                                        }
                                        if (!method2.getReturnType().getSignature().equals(modelType.getSignature())) {
                                            continue;
                                        }
                                        if (method2.getArgumentTypes().length < 6 || method2.getArgumentTypes().length > 8) {
                                            continue;
                                        }

                                        InstructionFinder instructionFinder2 = new InstructionFinder(new InstructionList(method2.getCode().getCode()));
                                        for (Iterator<InstructionHandle[]> iterator2 = instructionFinder2.search("aload areturn"); iterator2.hasNext();) {
                                            InstructionHandle[] ih2 = iterator2.next();
                                            ALOAD aload = (ALOAD) ih2[0].getInstruction();
                                            int injectionPos = ih2[0].getPrev().getPosition();
                                            new ModelDumperHook(hookHandler, unobbedCG, method2, injectionPos, modelCG, aload.getIndex(), "model");
                                        }
                                    }
                                    hookedModel = true;
                                    /*for(Field field: field2CG.getFields()){
                                        if (field.getSignature().equals("L" + modelCG.getClassName() + ";")) {
                                            hookHandler.addFieldHook("DataContainer", field2CG, field, "model", TypeBuilder.createHookType("Model"));
                                            hookedModel = true;
                                        }
                                    }*/
                            /*
                                }
                            }*/


                            hookAnimatedObject(cG, getModelMethod, "PhysicalObject" + (hits));
                            final ClassGen field2CG = hookHandler.classes.get(fieldInstruction.getClassName(cpg));
                            new Level2FieldHook(hookHandler, "PhysicalObject" + (hits), cG, field2CG, hookHandler.getFieldInClassGen(containerFieldInstruction.getFieldName(cpg), cG), hookHandler.getFieldInClassGen(fieldInstruction.getFieldName(cpg), field2CG), "ID", Type.SHORT, cG);
                            hookedID = true;
                        }
                    }

                    if (!hookedID) {
                        new ReturnSelfHook(hookHandler, "PhysicalObject" + (hits), cG, "dataContainer", TypeBuilder.createHookType("DataContainer"));
                        if (!hookedID) {
                            hookHandler.addReturnIntegerHook("DataContainer", "getID", cG, 0);
                        }
                        hookedID = true;
                        hookHandler.addReturnNullHook("DataContainer", "getModel", cG, TypeBuilder.createHookType("Model"));
                    }
                }
            }

            if (!cG.getSuperclassName().equals(gameObjectCG.getClassName())) {
                for (Method method : cG.getMethods()) {
                    if (method.isStatic()) {
                        continue;
                    }
                    if (method.getArgumentTypes().length < 3 || TypeCounter.getObjectCount(method.getArgumentTypes()) != 1) {
                        continue;
                    }
                    InstructionFinder instructionFinder = new InstructionFinder(new InstructionList(method.getCode().getCode()));
                    SEARCH_LOOP:
                    for (Iterator<InstructionHandle[]> iterator = instructionFinder.search("aload_0 getfield aload_0 getfield aload_0 getfield InvokeInstruction"); iterator.hasNext();) {
                        InstructionHandle[] ih = iterator.next();
                        InvokeInstruction invokeInstruction = (InvokeInstruction) ih[ih.length - 1].getInstruction();
                        if (!invokeInstruction.getMethodName(cpg).equals(setOffsets2Method.getName())
                                || !invokeInstruction.getSignature(cpg).equals(setOffsets2Method.getSignature())) {
                            continue;
                        }


                        FieldInstruction fieldInstruction = (FieldInstruction) ih[1].getInstruction();
                        for (Hook hook : hookHandler.hooks) {
                            if (hook instanceof FieldHook) {
                                FieldHook fieldHook = (FieldHook) hook;
                                if (fieldHook.getFieldName().equals(fieldInstruction.getFieldName(cpg))
                                        && fieldHook.getFieldCG().getClassName().equals(fieldInstruction.getClassName(cpg))) {
                                    continue SEARCH_LOOP;
                                }
                            }
                        }
                        hookHandler.addFieldHook("PhysicalObject" + (hits), fieldInstruction, cpg, "posX");
                        fieldInstruction = (FieldInstruction) ih[5].getInstruction();
                        hookHandler.addFieldHook("PhysicalObject" + (hits), fieldInstruction, cpg, "posY");
                    }
                }
            }
        }
    }

    private void hookAnimatedObject(ClassGen cG, Method getModelMethod, String classNick) {
        hookHandler.addInterfaceHook(cG, "DataContainer");
        new ReturnSelfHook(hookHandler, "PhysicalObject" + (hits), cG, "dataContainer", TypeBuilder.createHookType("DataContainer"));

        new InjectFieldHook(hookHandler, cG, "model", TypeBuilder.createHookType("ModelWrapper").getSignature(), 0);
        new VirtualFieldGetterHook(hookHandler, cG, "model", TypeBuilder.createHookType("ModelWrapper"), TypeBuilder.createHookType("Model"), classNick);
        ConstantPoolGen cpg = cG.getConstantPool();

        ClassGen modelCG = hookHandler.getClassByNick("Model");
        for (ClassGen cG2 : normalClasses) {
            if (cG2.getClassName().equals(cG.getClassName())) {
                cG = cG2;
                break;
            }
        }
        for (Method method : cG.getMethods()) {
            if (method.isStatic()) {
                continue;
            }
            if (method.getName().equals(getModelMethod.getName()) && method.getSignature().equals(getModelMethod.getSignature())) {
                continue;
            }
            if (!(method.getReturnType() instanceof ObjectType)) {
                continue;
            }
            final Type[] args = method.getArgumentTypes();
            if (args.length > 2 || args.length < 1) {
                continue;
            }
            InstructionList instructionList = new InstructionList(method.getCode().getCode());
            InstructionFinder instructionFinder = new InstructionFinder(instructionList);
            for (Iterator<InstructionHandle[]> iterator = instructionFinder.search("invokevirtual astore"); iterator.hasNext();) {
                InstructionHandle[] ih = iterator.next();
                InvokeInstruction invokeInstruction = (InvokeInstruction) ih[0].getInstruction();
                if(!invokeInstruction.getReturnType(cpg).toString().equals(modelCG.getClassName())){
                    continue;
                }
                final Type[] invokeArgs = invokeInstruction.getArgumentTypes(cpg);
                if(invokeArgs.length < 6 || invokeArgs.length > 7){
                    continue;
                }

                ASTORE astore = (ASTORE) ih[1].getInstruction();

                new CharacterModelDumperOnceHook(hookHandler, cG, method, ih[1].getPosition(), modelCG, astore.getIndex());
            }
        }
        for (Method method : cG.getMethods()) {
            if (method.isStatic()) {
                continue;
            }
            if (method.getName().equals(getModelMethod.getName()) && method.getSignature().equals(getModelMethod.getSignature())) {
                continue;
            }
            if (!method.getReturnType().equals(Type.BOOLEAN)) {
                continue;
            }
            final Type[] args = method.getArgumentTypes();
            if (args.length > 4 || args.length < 3) {
                continue;
            }
            InstructionList instructionList = new InstructionList(method.getCode().getCode());
            InstructionFinder instructionFinder = new InstructionFinder(instructionList);
            for (Iterator<InstructionHandle[]> iterator = instructionFinder.search("invokevirtual astore"); iterator.hasNext();) {
                InstructionHandle[] ih = iterator.next();
                InvokeInstruction invokeInstruction = (InvokeInstruction) ih[0].getInstruction();
                if(!invokeInstruction.getReturnType(cpg).toString().equals(modelCG.getClassName())){
                    continue;
                }
                final Type[] invokeArgs = invokeInstruction.getArgumentTypes(cpg);
                if(invokeArgs.length < 6 || invokeArgs.length > 7){
                    continue;
                }

                ASTORE astore = (ASTORE) ih[1].getInstruction();

                new CharacterModelDumperHook(hookHandler, cG, method, ih[1].getPosition(), modelCG, astore.getIndex());
            }
        }
    }

    private boolean hasIDHook(ClassGen cG) {
        for (Hook hook : hookHandler.hooks) {
            if (hook instanceof FieldHook) {
                FieldHook fieldHook = (FieldHook) hook;
                if (fieldHook.getFieldNick().equals("ID") && fieldHook.getFieldCG().getClassName().equals(cG.getClassName())) {
                    return true;
                }
            }
        }
        return false;
    }

    public String[] getFieldHooks() {
        return new String[]{
                "DataContainer::ID",
                "DataContainer::model",
                "PhysicalObject1::dataContainer",
                "PhysicalObject2::dataContainer",
                "PhysicalObject3::dataContainer",
                "PhysicalObject4::dataContainer",
                "PhysicalObject5::dataContainer",
                "PhysicalObject6::dataContainer",
                "PhysicalObject7::dataContainer",
        };
    }
}
