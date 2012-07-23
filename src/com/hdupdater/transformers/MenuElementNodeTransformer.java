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
import com.hdupdater.utils.InstructionUtils;
import com.sun.org.apache.bcel.internal.generic.*;
import com.sun.org.apache.bcel.internal.classfile.Field;
import com.sun.org.apache.bcel.internal.classfile.Method;
import com.sun.org.apache.bcel.internal.util.InstructionFinder;

import java.util.Iterator;

/**
 * Created by IntelliJ IDEA.
 * User: Jan Ove Saltvedt
 * Date: Sep 28, 2009
 * Time: 7:41:59 PM
 * To change this template use File | Settings | File Templates.
 */
public class MenuElementNodeTransformer extends AbstractTransformer {
    @Override
    public Class<?>[] getDependencies() {
        return new Class<?>[]{NodeTransformer.class};
    }

    public void run() {
        ClassGen nodeCG = hookHandler.getClassByNick("Node");
        for(ClassGen cG:classes){
            if(!cG.getSuperclassName().equals(nodeCG.getClassName())){
                continue;
            }
            // Should have no subclasses
            int subCGCount = 0;
            for(ClassGen subCG: classes){
                if(subCG.getSuperclassName().equals(cG.getClassName())){
                    subCGCount++;
                }
            }
            if(subCGCount != 0){
                continue;
            }

            /*
            Fields: 2 Strings
                    1 long
                    2 ints
             */
            int stringCount = 0;
            int longCount = 0;
            for(Field field: cG.getFields()){
                if(field.isStatic()){
                    continue;
                }
                if(field.getType().equals(Type.STRING)){
                    stringCount++;
                }
                else if(field.getType().equals(Type.LONG)){
                    longCount++;  
                }
            }
            if(longCount != 1 || stringCount != 2){
                continue;
            }
            hookHandler.addClassNick("MenuElementNode", cG);

            hookFields(cG);
        }
    }

    private void hookFields(ClassGen menuNodeCG) {
        for(ClassGen cG: classes){
            ConstantPoolGen cpg = cG.getConstantPool();
            for(Method method: cG.getMethods()){
                if(!method.isStatic())
                    continue;
                if(!method.getReturnType().equals(Type.STRING)){
                    continue;
                }
                Type[] args = method.getArgumentTypes();
                if(args.length < 1 || args.length > 2){
                    continue;
                }
                int menuNodeCount = 0;
                for(Type arg: args){
                    if(arg.toString().equals(menuNodeCG.getClassName())){
                        menuNodeCount++;
                    }
                }
                if(menuNodeCount != 1)
                    continue;
                InstructionFinder instructionFinder = new InstructionFinder(new InstructionList(method.getCode().getCode()));
                String pattern = "areturn";
                for(Iterator<InstructionHandle[]> iterator = instructionFinder.search(pattern); iterator.hasNext();){
                    InstructionHandle[] ih = iterator.next();
                    InstructionHandle[][] parameters = InstructionUtils.getParameters(ih[0], cpg, 1);
                    if(parameters[0].length < 3){
                        continue;
                    }
                    int hit = 0;
                    for(InstructionHandle instructionHandle: parameters[0]){
                        if(instructionHandle.getInstruction() instanceof GETFIELD){
                            hit++;
                            FieldInstruction fieldInstruction = (FieldInstruction) instructionHandle.getInstruction();
                            if(hit == 1){
                                hookHandler.addFieldHook("MenuElementNode", fieldInstruction, cpg, "action");
                            }
                            else if(hit == 2){
                                hookHandler.addFieldHook("MenuElementNode", fieldInstruction, cpg, "option");
                            }
                        }
                    }
                }
            }
        }
    }

    public String[] getFieldHooks() {
        return new String[]{
                "MenuElementNode::action",
                "MenuElementNode::option",
        };  //To change body of implemented methods use File | Settings | File Templates.
    }
}
