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
import com.hdupdater.hooks.FieldHook;
import com.sun.org.apache.bcel.internal.classfile.Method;
import com.sun.org.apache.bcel.internal.generic.*;
import com.sun.org.apache.bcel.internal.util.InstructionFinder;

import java.util.Iterator;

/**
 * Created by IntelliJ IDEA.
 * User: Jan Ove Saltvedt
 * Date: Mar 17, 2010
 * Time: 8:58:30 PM
 * To change this template use File | Settings | File Templates.
 */
public class MenuCallbackTranformer extends AbstractTransformer {
    @Override
    public Class<?>[] getDependencies() {
        return new Class<?>[]{MenuTransformer.class, MenuElementNodeTransformer.class};
    }

    @Override
    public void run() {
        FieldHook fieldHook = hookHandler.getFieldHook("Client", "menuOptionsCount");
        ClassGen menuElementNodeCG = hookHandler.getClassByNick("MenuElementNode");
        for(ClassGen cG: normalClasses) {
            ConstantPoolGen cpg = cG.getConstantPool();
            if(cpg.lookupFieldref(fieldHook.getClassName(), fieldHook.getFieldName(), fieldHook.getFieldType().getSignature()) == -1){
                continue;
            }
            for(Method method: cG.getMethods()){
                if(!method.isStatic()){
                    continue;
                }
                MethodGen methodGen = new MethodGen(method, cG.getClassName(), cpg);
                InstructionList iList = methodGen.getInstructionList();
                InstructionFinder instructionFinder = new InstructionFinder(iList);
                for(Iterator<InstructionHandle[]> iterator = instructionFinder.search("GETSTATIC ICONST_1 ((IADD)|(INEG ISUB)) PUTSTATIC"); iterator.hasNext();){
                    InstructionHandle[] ih = iterator.next();
                    FieldInstruction fieldInstruction = (FieldInstruction) ih[0].getInstruction();
                    if(!fieldInstruction.getClassName(cpg).equals(fieldHook.getClassName()) || !fieldInstruction.getFieldName(cpg).equals(fieldHook.getFieldName())){
                        continue;
                    }
                    fieldInstruction = (FieldInstruction) ih[ih.length-1].getInstruction();
                    if(!fieldInstruction.getClassName(cpg).equals(fieldHook.getClassName()) || !fieldInstruction.getFieldName(cpg).equals(fieldHook.getFieldName())){
                        continue;
                    }
                    System.out.println("Found menuOptionsCount++");
                }

                for(Iterator<InstructionHandle[]> iterator = instructionFinder.search("GETSTATIC ICONST_1 ((ISUB)|(INEG IADD)) PUTSTATIC"); iterator.hasNext();){
                    InstructionHandle[] ih = iterator.next();
                    FieldInstruction fieldInstruction = (FieldInstruction) ih[0].getInstruction();
                    if(!fieldInstruction.getClassName(cpg).equals(fieldHook.getClassName()) || !fieldInstruction.getFieldName(cpg).equals(fieldHook.getFieldName())){
                        continue;
                    }
                    fieldInstruction = (FieldInstruction) ih[ih.length-1].getInstruction();
                    if(!fieldInstruction.getClassName(cpg).equals(fieldHook.getClassName()) || !fieldInstruction.getFieldName(cpg).equals(fieldHook.getFieldName())){
                        continue;
                    }
                    System.out.println("Found menuOptionsCount--");
                }


                for(Iterator<InstructionHandle[]> iterator = instructionFinder.search("ICONST_0 PUTSTATIC"); iterator.hasNext();){
                    InstructionHandle[] ih = iterator.next();
                    FieldInstruction fieldInstruction = (FieldInstruction) ih[1].getInstruction();
                    if(!fieldInstruction.getClassName(cpg).equals(fieldHook.getClassName()) || !fieldInstruction.getFieldName(cpg).equals(fieldHook.getFieldName())){
                        continue;
                    }
                    System.out.println("Found menuOptionsCount = 0;");
                }
            }
        }

    }

    @Override
    public String[] getFieldHooks() {
        return new String[0];  //To change body of implemented methods use File | Settings | File Templates.
    }
}
