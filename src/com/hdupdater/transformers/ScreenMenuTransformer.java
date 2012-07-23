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
import com.sun.org.apache.bcel.internal.util.InstructionFinder;

import java.util.Iterator;

/**
 * Created by IntelliJ IDEA.
 * User: Jan Ove Saltvedt
 * Date: Nov 30, 2009
 * Time: 5:08:29 PM
 * To change this template use File | Settings | File Templates.
 */
public class ScreenMenuTransformer extends AbstractTransformer {

    @Override
    public Class<?>[] getDependencies() {
        return new Class<?>[]{IComponentTransformer.class};
    }

    public void run() {
        ClassGen iCompCG = hookHandler.getClassByNick("IComponent");
        for(ClassGen cG: classes){
            ConstantPoolGen cpg = cG.getConstantPool();
            if(cpg.lookupUtf8(" -> ") == -1){
                continue;
            }
            for(Method method: cG.getMethods()){
                if(!method.isStatic()){
                    continue;
                }
                if(!method.getReturnType().equals(Type.VOID)){
                    continue;
                }
                Type[] args = method.getArgumentTypes();
                if(args.length < 3 || args.length > 4){
                    continue;
                }

                if(TypeCounter.getObjectCount(args) != 1){
                    continue;
                }
                if(TypeCounter.getCount(args, Type.getType("L"+iCompCG.getClassName()+";")) != 1){
                    continue;
                }

                InstructionFinder instructionFinder = new InstructionFinder(new InstructionList(method.getCode().getCode()));
                for(Iterator<InstructionHandle[]> iterator = instructionFinder.search("getstatic ((ifeq)|(ifne))"); iterator.hasNext();){
                    InstructionHandle[] ih = iterator.next();
                    FieldInstruction fieldInstruction = (FieldInstruction) ih[0].getInstruction();
                    InstructionHandle[] block = InstructionUtils.getBranchBlock(ih[1]);
                    if(ih[1].getInstruction() instanceof IFNE){
                        for(Iterator<InstructionHandle[]> iterator2 = instructionFinder.search("ldc", block[block.length-1]); iterator2.hasNext();){
                            InstructionHandle[] ih2 = iterator2.next();
                            LDC ldc = (LDC) ih2[0].getInstruction();
                            if(ldc.getValue(cpg).equals(" -> ")){
                                hookHandler.addClientHook(fieldInstruction, cpg, "screenMenuItemSelected");
                            }
                        }
                    }
                    boolean foundLdc = false;
                    for(InstructionHandle handle: block){
                        if(handle.getInstruction() instanceof LDC){
                            LDC ldc = (LDC) handle.getInstruction();
                            if(ldc.getValue(cpg).equals(" -> ")){
                                foundLdc = true;
                            }
                        }
                    }
                    if(!foundLdc){
                        continue;
                    }

                    hookHandler.addClientHook(fieldInstruction, cpg, "screenMenuItemSelected");
                }

            }
        }
    }

    public String[] getFieldHooks() {
        return new String[]{
                "Client::screenMenuItemSelected"
        };  //To change body of implemented methods use File | Settings | File Templates.
    }
}
