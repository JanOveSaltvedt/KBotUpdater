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
import com.hdupdater.utils.InstructionUtils;
import com.hdupdater.utils.TypeBuilder;
import com.hdupdater.utils.TypeCounter;
import com.sun.org.apache.bcel.internal.generic.*;
import com.sun.org.apache.bcel.internal.classfile.Method;
import com.sun.org.apache.bcel.internal.classfile.Field;
import com.sun.org.apache.bcel.internal.util.InstructionFinder;

import java.util.Iterator;

/**
 * Created by IntelliJ IDEA.
 * User: Jan Ove Saltvedt
 * Date: Dec 6, 2009
 * Time: 7:51:39 PM
 * To change this template use File | Settings | File Templates.
 */
public class ViewSettingsTransformer extends AbstractTransformer {
    public void run() {
        ClassGen iCompCG = null;
        ClassGen iCCCG = null;
        Method iCCMethod = null;
        // Lets first get the IComponent class
        for (ClassGen cG : classes) {
            {
                int objectCount = 0;
                for (Field field : cG.getFields()) {
                    if (field.isStatic()) {
                        continue;
                    }
                    if (field.getType().getSignature().equals("[Ljava/lang/Object;")) {
                        objectCount++;
                    }
                }
                if (objectCount > 15) {
                    //Debug.writeLine(""+cG.getClassName());
                    iCompCG = cG;
                }
            }

            // Get the IComponentConstructor constructor method
            ConstantPoolGen cpg = cG.getConstantPool();
            if (cpg.lookupUtf8("bad command") != -1) {
                iCCCG = cG;
                for (Method method : cG.getMethods()) {
                    if (!method.isStatic()) {
                        continue;
                    }
                    if (method == null || method.getCode() == null)
                        continue;
                    InstructionFinder instructionFinder = new InstructionFinder(new InstructionList(method.getCode().getCode()));
                    for (Iterator<InstructionHandle[]> iterator = instructionFinder.search("ldc"); iterator.hasNext();) {
                        InstructionHandle[] ih = iterator.next();
                        LDC ldc = (LDC) ih[0].getInstruction();
                        if(ldc.getValue(cpg).equals("bad command")){
                            iCCMethod = method;
                        }
                    }
                }
            }
        }
        if (iCompCG == null) {
            throw new RuntimeException("Could not find IComponent class.");
        } else if (iCCCG == null) {
            throw new RuntimeException("Could not find IComponentConstructor class");
        } else if (iCCMethod == null) {
            throw new RuntimeException("Could not find IComponentConstructor method");
        }
        hookLayout(iCCCG, iCCMethod);
        hookCPUUsage();
    }

    private void hookCPUUsage() {
        for (ClassGen cG : classes) {
            ConstantPoolGen cpg = cG.getConstantPool();
            if (cpg.lookupUtf8("cpuusage=") == -1) {
                continue;
            }
            if (cpg.lookupUtf8("cpuusage=") == -1) {
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

                boolean foundCPUUsage = false;
                InstructionFinder instructionFinder = new InstructionFinder(new InstructionList(method.getCode().getCode()));
                for (Iterator<InstructionHandle[]> ldcIterator = instructionFinder.search("ldc"); ldcIterator.hasNext();) {
                    LDC ldc = (LDC) ldcIterator.next()[0].getInstruction();
                    if (ldc.getValue(cpg).equals("cpuusage=")) {
                        foundCPUUsage = true;
                    }
                }

                if (!foundCPUUsage) {
                    continue;
                }

                // currentPlane
                for (Iterator<InstructionHandle[]> ldcIterator = instructionFinder.search("ldc"); ldcIterator.hasNext();) {
                    InstructionHandle[] firstHandleArray = ldcIterator.next();
                    LDC ldc = (LDC) firstHandleArray[0].getInstruction();
                    if (ldc.getValue(cpg).equals("cpuusage=")) {

                        Iterator<InstructionHandle[]> fieldIterator = instructionFinder.search("getstatic getfield", firstHandleArray[0]);
                        InstructionHandle[] ih = fieldIterator.next();
                        FieldInstruction second = (FieldInstruction) ih[1].getInstruction();
                        hookHandler.addFieldHook("ViewSettings", second, cpg, "CPUUsage");
                    }

                }
            }

        }
    }

    public String[] getFieldHooks() {
        return new String[]{
                "Client::viewSettings",
                "ViewSettings::layout",
                "ViewSettings::CPUUsage",
        };
    }

    private void hookLayout(ClassGen iCCCG, Method iCCMethod) {
        ConstantPoolGen cpg = iCCCG.getConstantPool();
        InstructionList iList = new InstructionList(iCCMethod.getCode().getCode());

        InstructionFinder finder = new InstructionFinder(iList);

        InstructionHandle[] block = findBlock(cpg, finder, 5308);
        if (block != null) {
            FieldInstruction staticFieldInstruction = (FieldInstruction) InstructionUtils.getInstruction("getstatic getfield", block);
            FieldInstruction fieldInstruction = (FieldInstruction) InstructionUtils.getInstruction("getfield", block);
            if (staticFieldInstruction != null && fieldInstruction != null) {
                hookHandler.addClassNick("ViewSettings", fieldInstruction.getClassName(cpg));
                hookHandler.addClientHook(staticFieldInstruction, cpg, "viewSettings", TypeBuilder.createHookType("ViewSettings"));
                hookHandler.addFieldHook("ViewSettings", fieldInstruction, cpg, "layout");
            }
        }
    }

    private InstructionHandle[] findBlock(ConstantPoolGen cpg, InstructionFinder instructionFinder, final int opCode) {
        for (Iterator<InstructionHandle[]> iterator = instructionFinder.search("((((ConstantPushInstruction)|(ldc)) iload)|(iload((ConstantPushInstruction)|(ldc)))) if_icmpne"); iterator.hasNext();) {
            InstructionHandle[] ih = iterator.next();
            for (InstructionHandle i : ih) {
                if (i.getInstruction() instanceof LDC) {
                    LDC ldc = (LDC) i.getInstruction();
                    if (ldc.getValue(cpg) instanceof Integer) {
                        if (((Integer) ldc.getValue(cpg)) == opCode) {
                            return InstructionUtils.getBranchBlock(ih[ih.length - 1]);
                        }
                    }
                } else if (i.getInstruction() instanceof ConstantPushInstruction) {
                    ConstantPushInstruction ins = (ConstantPushInstruction) i.getInstruction();
                    if (ins.getValue().intValue() == opCode) {
                        return InstructionUtils.getBranchBlock(ih[ih.length - 1]);
                    }
                }
            }

        }
        return null;
    }

}
