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
import com.hdupdater.utils.InstructionUtils;
import com.hdupdater.utils.TypeBuilder;
import com.sun.org.apache.bcel.internal.generic.*;
import com.sun.org.apache.bcel.internal.classfile.Method;
import com.sun.org.apache.bcel.internal.classfile.Field;
import com.sun.org.apache.bcel.internal.util.InstructionFinder;

import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;

/**
 * Created by IntelliJ IDEA.
 * User: Jan Ove Saltvedt
 * Date: Oct 5, 2009
 * Time: 11:45:40 AM
 * To change this template use File | Settings | File Templates.
 */
public class RendererTransformer extends AbstractTransformer {
    @Override
    public Class<?>[] getDependencies() {
        return new Class<?>[]{RenderVarsTransformer.class};
    }

    public void run() {
        for (ClassGen cG : classes) {
            ConstantPoolGen cpg = cG.getConstantPool();
            if (cpg.lookupUtf8("GL_ARB_multitexture") == -1) {
                continue;
            }
            boolean foundString = false;
            for(Method method: cG.getMethods()){
                if(method.isStatic()){
                    continue;
                }
                if(method.isAbstract()){
                    continue;
                }
                if(method.getName().equals("<init>")){
                    continue;
                }
                InstructionList instructionList = new InstructionList(method.getCode().getCode());
                InstructionFinder instructionFinder = new InstructionFinder(instructionList);
                for (Iterator<InstructionHandle[]> iterator = instructionFinder.search("ldc"); iterator.hasNext();) {
                    InstructionHandle[] ih = iterator.next();
                    LDC ldc = (LDC) ih[0].getInstruction();
                    if(ldc.getValue(cpg).equals("GL_ARB_multitexture")){
                        foundString = true;
                    }
                }

            }
            if(!foundString){
                continue;
            }

            hookHandler.addClassNick("Renderer", cG.getSuperclassName());
            hookHandler.addClassNick("RendererGL", cG);

            classLoop:
            for (ClassGen cG2 : classes) {

                if (cG2.getSuperclassName().equals(cG.getSuperclassName()) && !cG2.getClassName().equals(cG.getClassName())) {
                     for(Method method: cG2.getMethods()){
                        if(method.isNative()){
                            continue classLoop;
                        }
                    }
                    ConstantPoolGen cpg2 = cG2.getConstantPool();
                    if(cpg2.lookupClass("jagex3.graphics2.hw.NativeInterface") != -1){
                        continue;
                    }
                    hookHandler.addClassNick("RendererSD", cG2);
                }
            }
        }

        ClassGen rendererCG = hookHandler.getClassByNick("Renderer");

        String methodName = null;
        String methodSignature = null;
        int factor1ParamNum = -1;
        int factor2ParamNum = -1;

        // Find the method that sets the renderer screen factors
        for (ClassGen cG : classes) {
            ConstantPoolGen cpg = cG.getConstantPool();
            if (cpg.lookupClass(rendererCG.getClassName()) == -1) {
                continue;
            }
            for (Method method : cG.getMethods()) {
                if (!method.isStatic()) {
                    continue;
                }
                if (!method.getReturnType().equals(Type.VOID)) {
                    continue;
                }

                Type[] args = method.getArgumentTypes();
                if (args.length < 5 || args.length > 7) {
                    continue;
                }
                int objectTypes = TypeCounter.getObjectCount(args);
                if (objectTypes != 0) {
                    continue;
                }

                InstructionFinder instructionFinder = new InstructionFinder(new InstructionList(method.getCode().getCode()));
                for (Iterator<InstructionHandle[]> iterator = instructionFinder.search("invokevirtual"); iterator.hasNext();) {
                    InstructionHandle[] ih = iterator.next();
                    INVOKEVIRTUAL invokevirtual = (INVOKEVIRTUAL) ih[0].getInstruction();
                    if (!invokevirtual.getClassName(cpg).equals(rendererCG.getClassName())) {
                        continue;
                    }

                    Type[] invokeParamTypes = invokevirtual.getArgumentTypes(cpg);
                    if (invokeParamTypes.length < 4 || invokeParamTypes.length > 5) {
                        continue;
                    }
                    objectTypes = TypeCounter.getBasicTypeCount(invokeParamTypes);
                    if (objectTypes < 4 || objectTypes > 5) {
                        continue;
                    }

                    InstructionHandle[][] params = InstructionUtils.getParameters(ih[0], cpg, 1);

                    if (!(params[params.length - 1][0].getInstruction() instanceof GETSTATIC)) {
                        continue;
                    }


                    for (int i = 0; i < params.length; i++) {
                        InstructionHandle[] param = params[i];
                        if (param.length == 3) {
                            if (param[1].getInstruction() instanceof LDC
                                    && ((LDC) param[1].getInstruction()).getValue(cpg).equals(1)
                                    && param[2].getInstruction() instanceof ISHL
                                    && param[0].getInstruction() instanceof GETSTATIC) {
                                methodName = invokevirtual.getMethodName(cpg);
                                methodSignature = invokevirtual.getSignature(cpg);
                                if (hookHandler.getFieldHook("Client", "gameRenderer") == null) {
                                    FieldInstruction fieldInstruction = (FieldInstruction) params[params.length - 1][0].getInstruction();
                                    hookHandler.addClientHook(fieldInstruction, cpg, "gameRenderer", TypeBuilder.createHookType("Renderer"));
                                }
                                if (hookHandler.getFieldHook("Client", "screenFactor") == null) {
                                    FieldInstruction fieldInstruction = (FieldInstruction) param[0].getInstruction();
                                    hookHandler.addClientHook(fieldInstruction, cpg, "screenFactor");
                                }
                                if (factor1ParamNum == -1) {
                                    factor1ParamNum = invokeParamTypes.length - i - 1;
                                } else if (factor2ParamNum == -1) {
                                    factor2ParamNum = invokeParamTypes.length - i - 1;
                                }
                            }
                        }
                    }

                }
            }
        }

        if (methodName == null || methodSignature == null || factor1ParamNum == -1 || factor2ParamNum == -1) {
            throw new RuntimeException("Could not find the method to set screen factors.");
        }

        ClassGen rendererSDCG = hookHandler.getClassByNick("RendererSD");
        for (Method method : rendererSDCG.getMethods()) {
            if (!method.getName().equals(methodName)
                    || !method.getSignature().equals(methodSignature)) {
                continue;
            }
            if(method.isNative()){
                continue;
            }
            if(method.getCode() == null){
                continue;
            }
            InstructionFinder instructionFinder = new InstructionFinder(new InstructionList(method.getCode().getCode()));
            for (Iterator<InstructionHandle[]> iterator = instructionFinder.search("iload putfield"); iterator.hasNext();) {
                InstructionHandle[] ih = iterator.next();
                ILOAD iload = (ILOAD) ih[0].getInstruction();
                FieldInstruction fieldInstruction = (FieldInstruction) ih[1].getInstruction();
                if (iload.getIndex() == factor1ParamNum+1) {
                    hookHandler.addFieldHook("RendererSD", fieldInstruction, rendererSDCG.getConstantPool(), "screenFactorX");
                } else if (iload.getIndex() == factor2ParamNum+1) {
                    hookHandler.addFieldHook("RendererSD", fieldInstruction, rendererSDCG.getConstantPool(), "screenFactorY");
                }
            }

        }

        ClassGen rendererGLCG = hookHandler.getClassByNick("RendererGL");
        for (Method method : rendererGLCG.getMethods()) {
            if (!method.getName().equals(methodName)
                    || !method.getSignature().equals(methodSignature)) {
                continue;
            }
            InstructionFinder instructionFinder = new InstructionFinder(new InstructionList(method.getCode().getCode()));
            for (Iterator<InstructionHandle[]> iterator = instructionFinder.search("iload putfield"); iterator.hasNext();) {
                InstructionHandle[] ih = iterator.next();
                ILOAD iload = (ILOAD) ih[0].getInstruction();
                FieldInstruction fieldInstruction = (FieldInstruction) ih[1].getInstruction();
                if (iload.getIndex() == factor1ParamNum) {
                    hookHandler.addFieldHook("RendererGL", fieldInstruction, rendererGLCG.getConstantPool(), "screenFactorX");
                } else if (iload.getIndex() == factor2ParamNum) {
                    hookHandler.addFieldHook("RendererGL", fieldInstruction, rendererGLCG.getConstantPool(), "screenFactorY");
                }
            }

        }
        hookRendererSDFields();
    }

    public void hookRendererSDFields(){
        ClassGen cG = hookHandler.getClassByNick("RendererSD");
        ConstantPoolGen cpg =  cG.getConstantPool();
        ClassGen renderVarsSDCG = hookHandler.getClassByNick("RenderVarsSD");
        for(Field field: cG.getFields()){
            if(field.isStatic()){
                continue;
            }
            if(field.getType().toString().equals(renderVarsSDCG.getClassName())){
                hookHandler.addFieldHook("RendererSD", cG, field, "renderVars", TypeBuilder.createHookType("RenderVars"));
            }
        }
        for(Method method: cG.getMethods()){
            if(method.isStatic()){
                continue;
            }
            if(!method.getReturnType().equals(Type.VOID)){
                continue;
            }
            if(method.isAbstract() || method.isNative()){
                continue;
            }

            Type[] args = method.getArgumentTypes();
            if (args.length < 4 || args.length > 5) {
                continue;
            }

            int intCount = TypeCounter.getIntCount(args);
            if (intCount < 3) {
                continue;
            }
            int intArrayCount = TypeCounter.getCount(args, Type.getType(int[].class));
            if (intArrayCount != 1) {
                continue;
            }

            List<LocalVarContainer> localVarContainers = new ArrayList<LocalVarContainer>();
            InstructionFinder instructionFinder = new InstructionFinder(new InstructionList(method.getCode().getCode()));
            for (Iterator<InstructionHandle[]> iterator = instructionFinder.search("iastore"); iterator.hasNext();) {
                InstructionHandle[] ih = iterator.next();

                InstructionHandle[][] iastoreParameters = InstructionUtils.getParameters(ih[0], cpg, 1);
                if(!(iastoreParameters[1][0].getInstruction() instanceof ConstantPushInstruction)){
                    continue;
                }
                int arrayIndex = ((ConstantPushInstruction) iastoreParameters[1][0].getInstruction()).getValue().intValue();
                InstructionHandle[] arithmetic = iastoreParameters[0];
                if(arithmetic.length != 4){
                    continue;
                }
                int iloadIndex = -1;
                String nick = null;
                for(InstructionHandle instructionHandle: arithmetic){
                    if(instructionHandle.getInstruction() instanceof ILOAD){
                        iloadIndex = ((ILOAD)instructionHandle.getInstruction()).getIndex();
                    }
                    if(instructionHandle.getInstruction() instanceof GETFIELD){
                        FieldInstruction fieldInstruction = (FieldInstruction) instructionHandle.getInstruction();
                        if(arrayIndex == 0){
                            hookHandler.addFieldHook("RendererSD", fieldInstruction, cpg, "minX");
                            nick = "x";
                        }
                        else if(arrayIndex == 1){
                            hookHandler.addFieldHook("RendererSD", fieldInstruction, cpg, "minY");
                            nick = "y";
                        }
                    }
                }
                if(iloadIndex != -1 && nick != null){
                    LocalVarContainer localVarContainer = new LocalVarContainer();
                    localVarContainer.nick =  nick;
                    localVarContainer.index = iloadIndex;
                    localVarContainers.add(localVarContainer);
                }
            }

            for (Iterator<InstructionHandle[]> iterator = instructionFinder.search("IF_ICMPGT"); iterator.hasNext();) {
                InstructionHandle[] ih = iterator.next();

                InstructionHandle[][] parameters = InstructionUtils.getParameters(ih[0], cpg, 1);
                FieldInstruction fieldInstruction = null;
                if(parameters[0].length == 2
                    && parameters[0][1].getInstruction() instanceof GETFIELD){
                    fieldInstruction = (FieldInstruction) parameters[0][1].getInstruction();
                }
                else if(parameters[1].length == 2
                    && parameters[1][1].getInstruction() instanceof GETFIELD){
                    fieldInstruction = (FieldInstruction) parameters[1][1].getInstruction();
                }

                if(fieldInstruction == null){
                    continue;
                }

                if(parameters[0].length == 1
                    && parameters[0][0].getInstruction() instanceof ILOAD){
                    ILOAD iload = (ILOAD) parameters[0][0].getInstruction();
                    for(LocalVarContainer localVarContainer: localVarContainers){
                        if(localVarContainer.index == iload.getIndex()){
                            if(localVarContainer.nick.equals("x")){
                                hookHandler.addFieldHook("RendererSD", fieldInstruction, cpg, "maxX");
                            }
                            else if(localVarContainer.nick.equals("y")){
                                hookHandler.addFieldHook("RendererSD", fieldInstruction, cpg, "maxY");
                            }
                        }
                    }
                }
                else if(parameters[1].length == 1
                    && parameters[1][0].getInstruction() instanceof ILOAD){
                    ILOAD iload = (ILOAD) parameters[1][0].getInstruction();
                    for(LocalVarContainer localVarContainer: localVarContainers){
                        if(localVarContainer.index == iload.getIndex()){
                            if(localVarContainer.nick.equals("x")){
                                hookHandler.addFieldHook("RendererSD", fieldInstruction, cpg, "maxX");
                            }
                            else if(localVarContainer.nick.equals("y")){
                                hookHandler.addFieldHook("RendererSD", fieldInstruction, cpg, "maxY");
                            }
                        }
                    }
                }
            }
        }
    }

    public String[] getFieldHooks() {
        return new String[]{
                "RendererSD::screenFactorX",
                "RendererSD::screenFactorY",
                //"RendererGL::screenFactorX",
                //"RendererGL::screenFactorY",
                "Client::gameRenderer",
                "Client::screenFactor",
                "RendererSD::minX",
                "RendererSD::maxX",
                "RendererSD::minY",
                "RendererSD::minY",
                "RendererSD::renderVars",
        };
    }

    private class LocalVarContainer{
        public String nick;
        public int index;
    }
}
