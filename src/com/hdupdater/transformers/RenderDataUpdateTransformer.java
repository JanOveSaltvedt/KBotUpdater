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
import com.hdupdater.hooks.UpdateRenderDataHook;
import com.hdupdater.utils.TypeCounter;
import com.sun.org.apache.bcel.internal.generic.*;
import com.sun.org.apache.bcel.internal.classfile.Method;
import com.sun.org.apache.bcel.internal.util.InstructionFinder;

import java.util.Iterator;

/**
 * Created by IntelliJ IDEA.
 * User: Jan Ove Saltvedt
 * Date: Oct 17, 2009
 * Time: 6:35:10 PM
 * To change this template use File | Settings | File Templates.
 */
public class RenderDataUpdateTransformer extends AbstractTransformer {
    public void run() {
        for(ClassGen cG: classes){
            ConstantPoolGen cpg = new ConstantPoolGen();
            for(Method method: cG.getMethods()){
                if(!method.isStatic()){
                    continue; 
                }
                if(method.isAbstract() || method.getCode() == null){
                    continue;
                }
                if(!method.getReturnType().equals(Type.VOID)){
                    continue;
                }
                Type[] args = method.getArgumentTypes();
                if(args.length < 8 || args.length > 10){
                    continue;
                }

                if(TypeCounter.getIntCount(args) < 8 || TypeCounter.getObjectCount(args) != 0){
                    continue;
                }

                int count128 = 0;
                InstructionFinder instructionFinder = new InstructionFinder(new InstructionList(method.getCode().getCode()));
                for(Iterator<InstructionHandle[]> iterator = instructionFinder.search("ConstantPushInstruction"); iterator.hasNext();){
                    InstructionHandle[] ih = iterator.next();
                    ConstantPushInstruction pushInstruction = (ConstantPushInstruction) ih[0].getInstruction();
                    if(pushInstruction.getValue().intValue() == 512){
                        count128++;
                    }

                }

                if(count128 < 3 || count128 > 4){
                    continue;
                }
                System.out.println("RenderData updater hooked!");
                new UpdateRenderDataHook(hookHandler, cG, method);
            }
        }
    }

    public String[] getFieldHooks() {
        return new String[0];  //To change body of implemented methods use File | Settings | File Templates.
    }
}
