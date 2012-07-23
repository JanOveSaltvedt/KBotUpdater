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
import com.sun.org.apache.bcel.internal.generic.*;
import com.sun.org.apache.bcel.internal.classfile.Method;
import com.sun.org.apache.bcel.internal.classfile.Field;
import com.sun.org.apache.bcel.internal.util.InstructionFinder;

import java.util.Iterator;

/**
 * Created by IntelliJ IDEA.
 * User: Jan Ove Saltvedt
 * Date: Oct 4, 2009
 * Time: 1:10:07 PM
 * To change this template use File | Settings | File Templates.
 */
public class SettingsTransformer extends AbstractTransformer {
    public void run() {
        ClassGen iCCCG = null;
        Method iCCMethod = null;
        // Lets first get the IComponent class
        for(ClassGen cG: classes){
            // Get the IComponentConstructor constructor method
            ConstantPoolGen cpg = cG.getConstantPool();
            if (cpg.lookupUtf8("slow") != -1) {
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
                        if(ldc.getValue(cpg).equals("slow")){
                            iCCMethod = method;
                        }
                    }
                }
            }
        }

        if(iCCCG == null){
            throw new RuntimeException("Could not find IComponentConstructor class");
        }
        else if(iCCMethod == null){
            throw new RuntimeException("Could not find IComponentConstructor method");
        }
        hookInICConstructor(iCCCG, iCCMethod);
    }

    private void hookInICConstructor(ClassGen iCCCG, Method iCCMethod) {
        ConstantPoolGen cpg = iCCCG.getConstantPool();
        InstructionList iList = new InstructionList(iCCMethod.getCode().getCode());

        InstructionFinder finder = new InstructionFinder(iList);

        InstructionHandle[] block = findBlock(cpg, finder, 1);
        if(block != null){
            InstructionList blockList = new InstructionList();
            for(InstructionHandle instructionHandle: block){
                if(instructionHandle.getInstruction() instanceof BranchInstruction){
                    BranchInstruction branchInstruction = (BranchInstruction) instructionHandle.getInstruction();
                    blockList.append(branchInstruction);
                } else {
                    blockList.append(instructionHandle.getInstruction());
                }
            }
            InstructionFinder blockFinder = new InstructionFinder(blockList);
            Iterator iterator = blockFinder.search("getstatic getfield");
            if(!iterator.hasNext()){
                return;
            }
            InstructionHandle[] match = (InstructionHandle[]) iterator.next();

            FieldInstruction first = (FieldInstruction) match[0].getInstruction();
            FieldInstruction second = (FieldInstruction) match[1].getInstruction();
            if(first != null && second != null){
                new Level2FieldHook(hookHandler, "Client",
                                        hookHandler.classes.get(first.getClassName(cpg)),
                                        hookHandler.classes.get(second.getClassName(cpg)),
                                        hookHandler.getFieldInClassGen(first.getFieldName(cpg), first.getClassName(cpg)),
                                        hookHandler.getFieldInClassGen(second.getFieldName(cpg), second.getClassName(cpg)),
                                        "settingsArray", Type.getType(int[].class),
                                        hookHandler.classes.get("client"));
            }
        }
    }

    private InstructionHandle[] findBlock(ConstantPoolGen cpg, InstructionFinder instructionFinder, final int opCode){
        for(Iterator<InstructionHandle[]> iterator = instructionFinder.search("((((ConstantPushInstruction)|(ldc)) iload)|(iload((ConstantPushInstruction)|(ldc)))) if_icmpne"); iterator.hasNext();){
            InstructionHandle[] ih = iterator.next();
            for(InstructionHandle i: ih){
                if(i.getInstruction() instanceof LDC){
                    LDC ldc = (LDC) i.getInstruction();
                    if(ldc.getValue(cpg) instanceof Integer){
                        if(((Integer)ldc.getValue(cpg)) == opCode){
                            return InstructionUtils.getBranchBlock(ih[ih.length-1]);
                        }
                    }
                }
                else if(i.getInstruction() instanceof ConstantPushInstruction){
                    ConstantPushInstruction ins = (ConstantPushInstruction) i.getInstruction();
                    if(ins.getValue().intValue() == opCode){
                        return InstructionUtils.getBranchBlock(ih[ih.length-1]);
                    }
                }
            }

        }
        return null;
    }

    public String[] getFieldHooks() {
        return new String[]{
                "Client::settingsArray"
        };  //To change body of implemented methods use File | Settings | File Templates.
    }
}
