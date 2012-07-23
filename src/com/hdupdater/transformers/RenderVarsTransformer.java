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
import com.sun.org.apache.bcel.internal.classfile.Field;
import com.sun.org.apache.bcel.internal.classfile.Method;
import com.sun.org.apache.bcel.internal.util.InstructionFinder;

import java.util.Iterator;

/**
 * Created by IntelliJ IDEA.
 * User: Jan Ove Saltvedt
 * Date: Oct 5, 2009
 * Time: 9:10:41 PM
 * To change this template use File | Settings | File Templates.
 */
public class RenderVarsTransformer extends AbstractTransformer {
    public void run() {
        for (ClassGen cG : classes) {
            ConstantPoolGen cpg = cG.getConstantPool();

            ClassGen superClass = hookHandler.classes.get(cG.getSuperclassName());
            if (superClass == null) {
                continue;
            }

            int intFieldCount = 0;
            int fieldCount = 0;
            int constructorCount = 0;
            int noArgumentsConstructorCount = 0;

            for (Field field : cG.getFields()) {
                if (!field.isStatic()) {
                    intFieldCount++;
                }
                fieldCount++;
            }

            for (Method method : cG.getMethods()) {
                if (method.getName().equals("<init>")) {
                    constructorCount++;
                    if (method.getArgumentTypes().length == 0) {
                        noArgumentsConstructorCount++;
                    }
                }
            }

            if (fieldCount != 12 || intFieldCount != 12) {
                continue;
            }

            if (constructorCount != 1 || noArgumentsConstructorCount != 1) {
                continue;
            }

            int subClasses = 0;
            for (ClassGen cG2 : classes) {
                if (cG2.getSuperclassName().equals(superClass.getClassName())) {
                    subClasses++;
                }
            }

            if (subClasses != 4) {
                continue;
            }

            hookHandler.addClassNick("RenderVars", superClass);
            hookHandler.addClassNick("RenderVarsSD", cG);
            /*for (ClassGen cG2 : classes) {
                if (cG2.getSuperclassName().equals(superClass.getClassName())) {
                    if (!cG2.getClassName().equals(cG.getClassName())) {
                        hookHandler.addClassNick("RenderVarsGL", cG2);
                    }
                }
            }*/
        }
        hookSDFields();
        hookStaticField();
    }

    public void hookSDFields() {
        ClassGen cG = hookHandler.getClassByNick("RenderVarsSD");
        ConstantPoolGen cpg = cG.getConstantPool();
        for (Method method : cG.getMethods()) {
            if (method.isStatic()) {
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
            //    0    0:aload           4
            //    1    2:iconst_0
            //    2    3:aload_0
            //    3    4:getfield        #59  <Field int k>
            //    4    7:aload_0
            //    5    8:getfield        #61  <Field int m>
            //    6   11:iload_1
            //    7   12:imul
            //    8   13:aload_0
            //    9   14:getfield        #65  <Field int q>
            //   10   17:iload_2
            //   11   18:imul
            //   12   19:iadd
            //   13   20:aload_0
            //   14   21:getfield        #55  <Field int g>
            //   15   24:iload_3
            //   16   25:imul
            //   17   26:iadd
            //   18   27:bipush          15
            //   19   29:ishr
            //   20   30:iadd
            //   21   31:iastore
            InstructionFinder instructionFinder = new InstructionFinder(new InstructionList(method.getCode().getCode()));
            for (Iterator<InstructionHandle[]> iterator = instructionFinder.search("IADD iastore"); iterator.hasNext();) {
                InstructionHandle[] ih = iterator.next();

                InstructionHandle[][] iastoreParameters = InstructionUtils.getParameters(ih[1], cpg, 1);
                int arrayIndex = ((ConstantPushInstruction) iastoreParameters[1][0].getInstruction()).getValue().intValue();
                int num = arrayIndex + 1;
                InstructionHandle[] arithmetic = iastoreParameters[0];
                InstructionList iList = new InstructionList();
                for (InstructionHandle instructionHandle : arithmetic) {
                    iList.append(instructionHandle.getInstruction());
                }
                InstructionFinder instructionFinder2 = new InstructionFinder(iList);
                int hitCount = 0;
                for (Iterator<InstructionHandle[]> iterator2 = instructionFinder2.search("getfield"); iterator2.hasNext();) {
                    InstructionHandle[] ih2 = iterator2.next();
                    FieldInstruction fieldInstruction = (FieldInstruction) ih2[0].getInstruction();
                    if (hitCount == 0) {
                        hookHandler.addFieldHook("RenderVarsSD", fieldInstruction, cpg, (arrayIndex == 0 ? "xOff" : (arrayIndex == 1 ? "yOff" : "zOff")), Type.FLOAT);
                    } else if (hitCount == 1) {
                        hookHandler.addFieldHook("RenderVarsSD", fieldInstruction, cpg, "x" + num, Type.FLOAT);
                    } else if (hitCount == 2) {
                        hookHandler.addFieldHook("RenderVarsSD", fieldInstruction, cpg, "y" + num, Type.FLOAT);
                    } else if (hitCount == 3) {
                        hookHandler.addFieldHook("RenderVarsSD", fieldInstruction, cpg, "z" + num, Type.FLOAT);
                    }
                    hitCount++;
                }
            }
        }
        for(Method method: cG.getMethods()){
            if(method.isStatic()){
                continue;
            }
            if(!method.getReturnType().equals(Type.VOID)){
                continue;
            }
            Type[] args = method.getArgumentTypes();
            if(args.length != 3 || TypeCounter.getIntCount(args) != 3){
                continue;
            }
            InstructionFinder instructionFinder = new InstructionFinder(new InstructionList(method.getCode().getCode()));
            for (Iterator<InstructionHandle[]> iterator = instructionFinder.search("ConstantPushInstruction"); iterator.hasNext();) {
                InstructionHandle[] ih = iterator.next();
                ConstantPushInstruction pushInstruction = (ConstantPushInstruction) ih[0].getInstruction();
                if(pushInstruction.getValue().intValue() == 16384){
                    hookHandler.dataMap.put("RenderVars::setOffsets2", new Object[]{method});
                }
            }
            for (Iterator<InstructionHandle[]> iterator = instructionFinder.search("LDC"); iterator.hasNext();) {
                InstructionHandle[] ih = iterator.next();
                LDC ldc = (LDC) ih[0].getInstruction();
                if(ldc.getValue(cpg) instanceof Number && ((Number)ldc.getValue(cpg)).intValue() == 16384){
                    hookHandler.dataMap.put("RenderVars::setOffsets2", new Object[]{method});
                }
            }
        }
    }

    public void hookStaticField(){
        ClassGen renderVarsCG = hookHandler.getClassByNick("RenderVars");
        for(ClassGen cG: classes){
            ConstantPoolGen cpg = cG.getConstantPool();
            for(Method method: cG.getMethods()){
                if(!method.isStatic()){
                    continue;
                }
                if(!method.getReturnType().equals(Type.VOID)){
                    continue;
                }
                if(method.isAbstract() || method.getCode() == null){
                    continue;
                }
                Type[] args = method.getArgumentTypes();
                int basicTypes = TypeCounter.getBasicTypeCount(args);
                if(basicTypes != args.length || basicTypes < 8 || basicTypes > 10){
                    continue;
                }
                boolean found128Push = false;
                InstructionFinder instructionFinder = new InstructionFinder(new InstructionList(method.getCode().getCode()));
                for (Iterator<InstructionHandle[]> iterator = instructionFinder.search("ConstantPushInstruction"); iterator.hasNext();) {
                    InstructionHandle[] ih = iterator.next();

                    ConstantPushInstruction pushInstruction = (ConstantPushInstruction) ih[0].getInstruction();
                    if(pushInstruction.getValue().intValue() == 512){
                        found128Push = true;
                        break;
                    }
                }

                if(!found128Push){
                    continue;
                }

                for (Iterator<InstructionHandle[]> iterator = instructionFinder.search("InvokeVirtual"); iterator.hasNext();) {
                    InstructionHandle[] ih = iterator.next();
                    InvokeInstruction invokeInstruction = (InvokeInstruction) ih[0].getInstruction();
                    if(!invokeInstruction.getReturnType(cpg).equals(Type.VOID)){
                        continue;
                    }
                    if(invokeInstruction.getArgumentTypes(cpg).length < 3 || invokeInstruction.getArgumentTypes(cpg).length > 4){
                        continue;
                    }
                    if(!invokeInstruction.getClassName(cpg).equals(renderVarsCG.getClassName())){
                        continue;
                    }
                    InstructionHandle[][] parameters = InstructionUtils.getParameters(ih[0], cpg, 1);
                    if(parameters[parameters.length-1][0].getInstruction() instanceof GETSTATIC){
                        FieldInstruction fieldInstruction = (FieldInstruction) parameters[parameters.length-1][0].getInstruction();

                        if(hookHandler.getFieldHook("Client", "gameRenderVars") == null) {
                            hookHandler.addClientHook(fieldInstruction, cpg, "gameRenderVars", TypeBuilder.createHookType("RenderVars"));
                        }
                    }
                }

            }
        }
    }

    public String[] getFieldHooks() {
        return new String[]{
                "RenderVarsSD::xOff",
                "RenderVarsSD::yOff",
                "RenderVarsSD::zOff",
                "RenderVarsSD::x1",
                "RenderVarsSD::y1",
                "RenderVarsSD::z1",
                "RenderVarsSD::x2",
                "RenderVarsSD::y2",
                "RenderVarsSD::z2",
                "RenderVarsSD::x3",
                "RenderVarsSD::y3",
                "RenderVarsSD::z3",
                "Client::gameRenderVars",
        };  //To change body of implemented methods use File | Settings | File Templates.
    }
}
