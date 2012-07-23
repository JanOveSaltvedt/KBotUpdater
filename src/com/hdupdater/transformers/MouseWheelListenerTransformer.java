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
import com.sun.org.apache.bcel.internal.classfile.Method;
import com.sun.org.apache.bcel.internal.generic.*;
import com.sun.org.apache.bcel.internal.util.InstructionFinder;

import java.awt.*;
import java.awt.event.MouseWheelListener;
import java.util.Iterator;

/**
 * Created by IntelliJ IDEA.
 * User: Jan Ove Saltvedt
 * Date: Mar 10, 2010
 * Time: 5:21:36 PM
 * To change this template use File | Settings | File Templates.
 */
public class MouseWheelListenerTransformer extends AbstractTransformer {
    @Override
    public void run() {
        /*for(ClassGen cG: classes){
            ConstantPoolGen cpg = cG.getConstantPool();
            if(cG.getInterfaceNames().length != 1){
                continue;
            }
            if(!cG.getInterfaceNames()[0].contains("MouseWheelListener")){
                continue;
            }
            // Find the add listener method
            for(Method method: cG.getMethods()){
                if(method.isStatic()){
                    continue;
                }
                if(method.getName().equals("mouseWheelMoved")){
                    continue;
                }
                boolean foundComponent = false;
                for(Type arg: method.getArgumentTypes()){
                    if(arg.equals(Type.getType(Component.class))){
                        foundComponent = true;
                    }
                }
                if(!foundComponent){
                    continue;
                }

                boolean isAddListener = false;
                for(Iterator<InstructionHandle[]> iterator = new InstructionFinder(new InstructionList(method.getCode().getCode())).search("InvokeInstruction"); iterator.hasNext();){
                    InstructionHandle[] ih = iterator.next();
                    InvokeInstruction invokeInstruction = (InvokeInstruction) ih[0].getInstruction();
                    if(invokeInstruction.getMethodName(cpg).equals("addMouseWheelListener")){
                        isAddListener = true;
                        break;
                    }
                }

                if(!isAddListener){
                    continue;
                }

                for(ClassGen cG2: classes){
                    final ConstantPoolGen cpg2 = cG2.getConstantPool();
                    if(cpg2.lookupMethodref(cG.getSuperclassName(), method.getName(), method.getSignature()) == -1){
                        continue;
                    }
                    /*if(cpg2.lookupUtf8("127.0.0.1") == -1 || cpg2.lookupUtf8("Jagex") == -1){
                        continue;
                    }*/
        /*
                    for(Method m: cG2.getMethods()){
                        for(Iterator<InstructionHandle[]> iterator = new InstructionFinder(new InstructionList(m.getCode().getCode())).search("InvokeInstruction"); iterator.hasNext();){
                            InstructionHandle[] ih = iterator.next();
                            InvokeInstruction invokeInstruction = (InvokeInstruction) ih[0].getInstruction();

                            if(!invokeInstruction.getClassName(cpg2).equals(cG.getSuperclassName())){
                                continue;
                            }
                            if(!invokeInstruction.getMethodName(cpg2).equals(m.getName())){
                                continue;
                            }

                            InstructionHandle[][] parameters = InstructionUtils.getParameters(ih[0], cpg2, 1);
                            InstructionHandle last = parameters[parameters.length-1][0];
                            if(last.getInstruction() instanceof GETSTATIC){
                                FieldInstruction fieldInstruction = (FieldInstruction) last.getInstruction();
                                hookHandler.addClientHook(fieldInstruction, cpg2, "getMouseWheelListener", Type.getType(MouseWheelListener.class));
                            }

                        }
                    }

                }

            }

        }*/

    }

    @Override
    public String[] getFieldHooks() {
        return new String[]{
                "Client::getMouseWheelListener"
        };
    }
}
