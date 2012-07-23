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
import com.hdupdater.hooks.Level2FieldHook;
import com.hdupdater.utils.TypeBuilder;
import com.hdupdater.utils.TypeCounter;
import com.sun.org.apache.bcel.internal.classfile.Field;
import com.sun.org.apache.bcel.internal.generic.*;
import com.sun.org.apache.bcel.internal.classfile.Method;
import com.sun.org.apache.bcel.internal.util.InstructionFinder;

import java.util.Iterator;

/**
 * Created by IntelliJ IDEA.
 * User: Jan Ove Saltvedt
 * Date: Oct 5, 2009
 * Time: 11:08:10 AM
 * To change this template use File | Settings | File Templates.
 */
public class LocalCMDTransformer extends AbstractTransformer {
    @Override
    public Class<?>[] getDependencies() {
        return new Class<?>[]{RendererTransformer.class};
    }

    public void run() {
        for (ClassGen cG : classes) {
            ConstantPoolGen cpg = cG.getConstantPool();
            if (cpg.lookupUtf8("getcamerapos") == -1) {
                continue;
            }
            for (Method method : cG.getMethods()) {
                if (!method.isStatic()) {
                    continue;
                }
                if (!method.getReturnType().equals(Type.VOID)) {
                    continue;
                }
                if (TypeCounter.getCount(method.getArgumentTypes(), Type.STRING) != 1) {
                    continue;
                }

                boolean foundGetCameraPos = false;
                InstructionFinder instructionFinder = new InstructionFinder(new InstructionList(method.getCode().getCode()));
                for (Iterator<InstructionHandle[]> ldcIterator = instructionFinder.search("ldc"); ldcIterator.hasNext();) {
                    LDC ldc = (LDC) ldcIterator.next()[0].getInstruction();
                    if (ldc.getValue(cpg).equals("getcamerapos")) {
                        foundGetCameraPos = true;
                    }
                }

                if (!foundGetCameraPos) {
                    continue;
                }

                // currentPlane
                for (Iterator<InstructionHandle[]> ldcIterator = instructionFinder.search("ldc"); ldcIterator.hasNext();) {
                    InstructionHandle[] firstHandleArray = ldcIterator.next();
                    LDC ldc = (LDC) firstHandleArray[0].getInstruction();
                    if (ldc.getValue(cpg).equals("getcamerapos")) {
                        for (Iterator<InstructionHandle[]> ldcIterator2 = instructionFinder.search("ldc", firstHandleArray[0]); ldcIterator2.hasNext();) {
                            InstructionHandle[] secondHandleArray = ldcIterator2.next();
                            ldc = (LDC) secondHandleArray[0].getInstruction();
                            if (ldc.getValue(cpg).equals("Look: ")) {
                                Iterator<InstructionHandle[]> fieldIterator = instructionFinder.search("getstatic getfield", secondHandleArray[0]);
                                InstructionHandle[] ih = fieldIterator.next();
                                FieldInstruction first = (FieldInstruction) ih[0].getInstruction();
                                FieldInstruction second = (FieldInstruction) ih[1].getInstruction();
                                new Level2FieldHook(hookHandler, "Client",
                                        hookHandler.classes.get(first.getClassName(cpg)),
                                        hookHandler.classes.get(second.getClassName(cpg)),
                                        hookHandler.getFieldInClassGen(first.getFieldName(cpg), first.getClassName(cpg)),
                                        hookHandler.getFieldInClassGen(second.getFieldName(cpg), second.getClassName(cpg)),
                                        "currentPlane", Type.INT,
                                        hookHandler.classes.get("client"));
                            }
                        }
                    }

                }


                // colMap
                for (Iterator<InstructionHandle[]> ldcIterator = instructionFinder.search("ldc | ldc_w"); ldcIterator.hasNext();) {
                    InstructionHandle[] firstHandleArray = ldcIterator.next();

                    final Instruction instruction = firstHandleArray[0].getInstruction();
                    if(instruction instanceof LDC){
                        LDC ldc = (LDC) instruction;
                        if (ldc.getValue(cpg).equals("showcolmap")) {
                            for (Iterator<InstructionHandle[]> ldcIterator2 = instructionFinder.search("ICONST_1 PUTSTATIC", firstHandleArray[0]); ldcIterator2.hasNext();) {
                                InstructionHandle[] secondHandleArray = ldcIterator2.next();
                                FieldInstruction fieldInstruction = (FieldInstruction) secondHandleArray[1].getInstruction();
                                doColMapHook(fieldInstruction.getClassName(cpg), fieldInstruction.getFieldName(cpg));
                                break;
                            }
                        }
                    }
                    if(instruction instanceof LDC_W){
                        LDC_W ldc = (LDC_W) instruction;
                        if (ldc.getValue(cpg).equals("showcolmap")) {
                            for (Iterator<InstructionHandle[]> ldcIterator2 = instructionFinder.search("ICONST_1 PUTSTATIC", firstHandleArray[0]); ldcIterator2.hasNext();) {
                                InstructionHandle[] secondHandleArray = ldcIterator2.next();
                                FieldInstruction fieldInstruction = (FieldInstruction) secondHandleArray[1].getInstruction();
                                doColMapHook(fieldInstruction.getClassName(cpg), fieldInstruction.getFieldName(cpg));
                                break;
                            }
                        }
                    }

                }
            }

        }
    }

    private void doColMapHook(String fieldClassName, String fieldName) {
        for(ClassGen cG: classes){
            ConstantPoolGen cpg = cG.getConstantPool();
            for(Method method: cG.getMethods()){
                if(!method.isStatic()){
                    continue;
                }
                if(!method.getReturnType().equals(Type.BOOLEAN)){
                    continue;
                }

                final Type[] args = method.getArgumentTypes();
                if(args.length < 2 || args.length > 4){
                    continue;    
                }

                InstructionFinder instructionFinder = new InstructionFinder(new InstructionList(method.getCode().getCode()));
                for (Iterator<InstructionHandle[]> iterator = instructionFinder.search("getstatic ifeq"); iterator.hasNext();) {
                    InstructionHandle[] ih = iterator.next();
                    FieldInstruction fieldInstruction = (FieldInstruction) ih[0].getInstruction();
                    if(!fieldInstruction.getClassName(cpg).equals(fieldClassName) || !fieldInstruction.getFieldName(cpg).equals(fieldName)){
                        continue;
                    }
                    for (Iterator<InstructionHandle[]> iterator2 = instructionFinder.search("getstatic iload aaload astore"); iterator2.hasNext();) {
                        InstructionHandle[] ih2 = iterator2.next();
                        fieldInstruction = (FieldInstruction) ih2[0].getInstruction();
                        ArrayType arrayType = (ArrayType) fieldInstruction.getFieldType(cpg);
                        if(arrayType.getDimensions() != 1){
                            continue;
                        }
                        Type type = arrayType.getBasicType();
                        if(!(type instanceof ObjectType)){
                            continue;
                        }

                        if(!hookHandler.classes.containsKey(type.toString())){
                            continue;
                        }

                        ClassGen mapDataCG = hookHandler.classes.get(type.toString());
                        hookHandler.addInterfaceHook(mapDataCG, "MapData");    // TODO REVERSE TO MAPDATA
                        hookHandler.addClientHook(fieldInstruction, cpg, "mapDataArray", TypeBuilder.createHookArrayType("MapData", 1));  // TODO REVERSE TO MAPDATA
                        for(Field field: mapDataCG.getFields()){
                            if(field.isStatic()){
                                continue;
                            }
                            if(field.getType().equals(Type.getType(int[][].class))){
                                hookHandler.addFieldHook("MapData", mapDataCG, field, "tileData"); // TODO REVERSE TO MAPDATA
                            }
                        }
                        break;

                    }

                }


            }
             
        }

    }

    public String[] getFieldHooks() {
        return new String[]{
                "Client::currentPlane",
                "Client::mapDataArray",
                "MapData::tileData" // TODO REVERSE TO MAPDATA
        };  //To change body of implemented methods use File | Settings | File Templates.
    }
}
