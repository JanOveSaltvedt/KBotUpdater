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
import com.hdupdater.hooks.Hook;
import com.hdupdater.utils.TypeBuilder;
import com.hdupdater.hooks.ReturnSelfHook;
import com.sun.org.apache.bcel.internal.generic.*;
import com.sun.org.apache.bcel.internal.classfile.Field;
import com.sun.org.apache.bcel.internal.classfile.Method;
import com.sun.org.apache.bcel.internal.util.InstructionFinder;

import java.util.Iterator;

/**
 * Created by IntelliJ IDEA.
 * User: Jan Ove Saltvedt
 * Date: Sep 20, 2009
 * Time: 2:06:41 PM
 * To change this template use File | Settings | File Templates.
 */
public class InteractivePhysicalObjectTransformer extends AbstractTransformer {

    @Override
    public Class<?>[] getDependencies() {
        return new Class<?>[]{GameObjectTransformer.class};
    }

    public void run() {
        ClassGen gameObjectCG = hookHandler.getClassByNick("GameObject");
        for(ClassGen cG: classes){
            final ConstantPoolGen cpg = cG.getConstantPool();
            if(!cG.getSuperclassName().equals(gameObjectCG.getClassName())){
                continue;   
            }
            if(cG.getInterfaceNames().length != 1){
                continue;
            }
            int nonStaticFiedCount = 0;
            for(Field field: cG.getFields()){
                if(field.isStatic())
                    continue;
                nonStaticFiedCount++;
            }
            if(nonStaticFiedCount < 6){
                continue;
            }

            hookHandler.addClassNick("InteractivePhysicalObject", cG);
            new ReturnSelfHook(hookHandler, "InteractivePhysicalObject", cG, "dataContainer", TypeBuilder.createHookType("DataContainer"));
            hookHandler.dataMap.put("ModelledObjectInterface", new Object[]{hookHandler.classes.get(cG.getInterfaceNames()[0])});
            for(Method method: cG.getMethods()){
                if(method.isStatic()){
                    continue;
                }
                if(!method.getReturnType().equals(Type.INT)){
                    continue;
                }
                if(method.getArgumentTypes().length > 2){
                    continue;
                }
                InstructionFinder instructionFinder = new InstructionFinder(new InstructionList(method.getCode().getCode()));
                InstructionFinder.CodeConstraint codeConstraint = new InstructionFinder.CodeConstraint() {
                    public boolean checkCode(InstructionHandle[] match) {
                        for(InstructionHandle insH: match){
                            if(insH.getInstruction() instanceof LDC){
                                if(((LDC)insH.getInstruction()).getValue(cpg).equals(0xFFFF)){
                                    return true;
                                }
                            }
                            else if(insH.getInstruction() instanceof SIPUSH){
                                if(((SIPUSH)insH.getInstruction()).getValue().intValue() == 0xFFFF) {
                                    return true;
                                }
                            }
                        }
                        return false;  //To change body of implemented methods use File | Settings | File Templates.
                    }
                };
                for(Iterator<InstructionHandle[]> iterator = instructionFinder.search("(((ldc)|(sipush)) aload getfield)|(aload getfield((ldc)|(sipush))) iand ireturn", codeConstraint); iterator.hasNext();){
                    InstructionHandle[] ih = iterator.next();
                    for(InstructionHandle i: ih){
                        if(i.getInstruction() instanceof GETFIELD){
                            FieldInstruction fieldInstruction = (FieldInstruction) i.getInstruction();
                            if(!hasIDHook(cG)){
                                hookHandler.addFieldHook("InteractivePhysicalObject", fieldInstruction, cpg, "ID");
                            }
                            hookHandler.dataMap.put("ModelledObjectInterface::getID()", new Object[]{method});
                            hookHandler.addInterfaceHook(cG, "DataContainer");
                        }
                    }
                }
            }
        }
    }

    private boolean hasIDHook(ClassGen cG) {
        for(Hook hook: hookHandler.hooks){
            if(hook instanceof FieldHook){
                FieldHook fieldHook = (FieldHook) hook;
                if(fieldHook.getFieldNick().equals("ID") && fieldHook.getFieldCG().getClassName().equals(cG.getClassName())){
                    return true;
                }
            }
        }
        return false;
    }

    public String[] getFieldHooks() {
        return new String[]{
                "InteractivePhysicalObject::ID",
                "InteractivePhysicalObject::dataContainer",
        };  //To change body of implemented methods use File | Settings | File Templates.
    }
}
