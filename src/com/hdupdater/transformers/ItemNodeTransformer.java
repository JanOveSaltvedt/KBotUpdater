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
import com.sun.org.apache.bcel.internal.generic.*;
import com.sun.org.apache.bcel.internal.classfile.Method;
import com.sun.org.apache.bcel.internal.classfile.Field;
import com.sun.org.apache.bcel.internal.util.InstructionFinder;

import java.util.Iterator;

/**
 * Created by IntelliJ IDEA.
 * User: Jan Ove Saltvedt
 * Date: Dec 10, 2009
 * Time: 7:52:17 PM
 * To change this template use File | Settings | File Templates.
 */
public class ItemNodeTransformer extends AbstractTransformer {
    @Override
    public Class<?>[] getDependencies() {
        return new Class<?>[]{NodeTransformer.class};
    }

    public void run() {
        ClassGen nodeCG = hookHandler.getClassByNick("Node");
        for (ClassGen cG : classes) {
            ConstantPoolGen cpg = cG.getConstantPool();
            if (cpg.lookupUtf8(" -> <col=ff9040>") == -1) {
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
                if (args.length < 3 || args.length > 4) {
                    continue;
                }
                if (TypeCounter.getObjectCount(args) != 1) {
                    continue;
                }

                InstructionFinder instructionFinder = new InstructionFinder(new InstructionList(method.getCode().getCode()));
                for (Iterator<InstructionHandle[]> iterator = instructionFinder.search("ldc | LDC_W"); iterator.hasNext();) {

                    InstructionHandle[] ih = iterator.next();
                    if(ih[0].getInstruction() instanceof LDC){
                        LDC ldc = (LDC) ih[0].getInstruction();
                        if(!ldc.getValue(cpg).equals(" -> <col=ff9040>")){
                            continue;
                        }
                    }
                    else{
                        LDC_W ldc = (LDC_W) ih[0].getInstruction();
                        if(!ldc.getValue(cpg).equals(" -> <col=ff9040>")){
                            continue;
                        }

                    }
                    Iterator<InstructionHandle[]> iterator2 = instructionFinder.search("invokestatic", ih[0]);
                    if(!iterator2.hasNext()){
                        continue;
                    }
                    InstructionHandle invokeIH = iterator2.next()[0];
                    InstructionHandle[][] parameters = InstructionUtils.getParameters(invokeIH, cpg, 1);
                    for(InstructionHandle[] param: parameters){
                        for(InstructionHandle handle: param){
                            if(handle.getInstruction() instanceof GETFIELD){
                                FieldInstruction fieldInstruction = (FieldInstruction) handle.getInstruction();
                                ClassGen fieldCG = hookHandler.classes.get(fieldInstruction.getClassName(cpg));
                                if(fieldCG == null){
                                    continue;
                                }
                                if(!fieldCG.getSuperclassName().equals(nodeCG.getClassName())){
                                    continue;
                                }
                                hookHandler.addClassNick("Item", fieldCG);
                                hookHandler.addFieldHook("Item", fieldInstruction, cpg, "ID");
                                for(Field field: fieldCG.getFields()){
                                    if(field.isStatic()){
                                        continue;
                                    }
                                    if(!field.getType().equals(Type.INT)){
                                        continue;
                                    }
                                    if(!field.getName().equals(fieldInstruction.getFieldName(cpg))){
                                        hookHandler.addFieldHook("Item", fieldCG, field, "stackSize");
                                    }
                                }
                            }
                        }
                    }
                }

            }
        }
    }

    public String[] getFieldHooks() {
        return new String[]{
                "Item::ID",
                "Item::stackSize",
        };  //To change body of implemented methods use File | Settings | File Templates.
    }
}
