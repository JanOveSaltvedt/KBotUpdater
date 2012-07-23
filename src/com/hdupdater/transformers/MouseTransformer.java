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
import com.hdupdater.utils.InstructionUtils;
import com.sun.org.apache.bcel.internal.classfile.Method;
import com.sun.org.apache.bcel.internal.generic.*;
import com.sun.org.apache.bcel.internal.util.InstructionFinder;

import java.util.Iterator;

/**
 * Created by IntelliJ IDEA.
 * User: Jan Ove / Kosaki
 * Date: 21.mar.2009
 * Time: 22:29:46
 */
public class MouseTransformer extends AbstractTransformer {

    public void run() {
        for (ClassGen cG : classes) {
            ConstantPoolGen cpg = cG.getConstantPool();
            String[] interfaces = cG.getInterfaceNames();
            if (interfaces.length < 1)
                continue;
            boolean foundInterface = false;
            for (String inter : interfaces) {
                if (inter.contains("MouseListener") || inter.contains("MouseMotionListener")) {
                    foundInterface = true;
                }
            }
            if (!foundInterface)
                continue;
            hookHandler.addClassNick("Mouse", cG);
            for (Method m : cG.getMethods()) {
                if (!m.getName().equals("mouseMoved")) {
                    continue;
                }
                InstructionFinder instructionFinder = new InstructionFinder(new InstructionList(m.getCode().getCode()));
                for (Iterator<InstructionHandle[]> iterator = instructionFinder.search("invokespecial"); iterator.hasNext();) {
                    InstructionHandle[] ih = iterator.next();
                    InvokeInstruction invokeInstruction = (InvokeInstruction) ih[0].getInstruction();
                    if(!invokeInstruction.getReturnType(cpg).equals(Type.VOID)){
                        continue;
                    }
                    if(!invokeInstruction.getClassName(cpg).equals(cG.getClassName())){
                        continue;
                    }
                    InstructionHandle[][] parameters = InstructionUtils.getParameters(ih[0], cpg, 1);
                    int paramIndex = invokeInstruction.getArgumentTypes(cpg).length;
                    int getXIndex = -1;
                    int getYIndex = -1;
                    for (InstructionHandle[] parameter : parameters) {
                        paramIndex--;
                        if (parameter.length != 2) {
                            continue;
                        }
                        if (!(parameter[1].getInstruction() instanceof InvokeInstruction)) {
                            continue;
                        }
                        InvokeInstruction getInvoke = (InvokeInstruction) parameter[1].getInstruction();
                        if (getInvoke.getMethodName(cpg).equals("getX")) {
                            getXIndex = paramIndex;
                        } else if (getInvoke.getMethodName(cpg).equals("getY")) {
                            getYIndex = paramIndex;
                        }
                    }
                    for (Method method2 : cG.getMethods()) {
                        if (method2.isStatic()) {
                            continue;
                        }
                        if (!method2.getName().equals(invokeInstruction.getMethodName(cpg))) {
                            continue;
                        }
                        if (!method2.getSignature().equals(invokeInstruction.getSignature(cpg))) {
                            continue;
                        }
                        InstructionFinder instructionFinder2 = new InstructionFinder(new InstructionList(method2.getCode().getCode()));
                        for (Iterator<InstructionHandle[]> iterator2 = instructionFinder2.search("iload putfield"); iterator2.hasNext();) {
                            InstructionHandle[] ih2 = iterator2.next();
                            ILOAD iload = (ILOAD) ih2[0].getInstruction();
                            FieldInstruction fieldInstruction = (FieldInstruction) ih2[1].getInstruction();

                            if (iload.getIndex() == getXIndex+1) {
                                hookHandler.addFieldHook("Mouse", fieldInstruction, cpg, "mouseX");
                            } else if (iload.getIndex() == getYIndex+1) {
                                hookHandler.addFieldHook("Mouse", fieldInstruction, cpg, "mouseY");
                            }
                        }

                    }
                }
            }
            /*for (Method m : cG.getMethods()) {
                InstructionFinder instructionFinder = new InstructionFinder(new InstructionList(m.getCode().getCode()));

                if (!m.getName().startsWith("Mouse")
                        && m.getReturnType().equals(Type.VOID)) {
                    int mouseEventCount = TypeCounter.getCount(m.getArgumentTypes(), Type.getType(MouseEvent.class));
                    if (mouseEventCount == 1 && m.getArgumentTypes().length < 3) {
                        for (Iterator<InstructionHandle[]> iterator = instructionFinder.search("invokevirtual putfield"); iterator.hasNext();) {
                            InstructionHandle[] ih = iterator.next();
                            INVOKEVIRTUAL invoke = (INVOKEVIRTUAL) ih[0].getInstruction();
                            FieldInstruction fieldInstruction = (FieldInstruction) ih[1].getInstruction();
                            if (invoke.getMethodName(cpg).contains("getX")) {
                                hookHandler.addFieldHook("Mouse", fieldInstruction, cpg, "mouseX");
                            }
                            if (invoke.getMethodName(cpg).contains("getY")) {
                                hookHandler.addFieldHook("Mouse", fieldInstruction, cpg, "mouseY");
                            }
                        }
                    }
                }
            }*/
            hookClientMouse(cG, classes);
        }
    }

    private void hookClientMouse(ClassGen cG, ClassGen[] classes) {
        ConstantPoolGen cpg = cG.getConstantPool();
        FieldHook mouseX = hookHandler.getFieldHook("Mouse", "mouseX");
        FieldHook mouseY = hookHandler.getFieldHook("Mouse", "mouseY");
        for (Method method : cG.getMethods()) {
            if (method.isStatic()) {
                continue;
            }
            if (!method.isSynchronized()) {
                continue;
            }
            if (method.getArgumentTypes().length > 1) {
                continue;
            }
            if (!(method.getArgumentTypes()[0] instanceof BasicType)) {
                continue;
            }

            boolean foundMethod = false;
            InstructionFinder instructionFinder = new InstructionFinder(new InstructionList(method.getCode().getCode()));
            for (Iterator<InstructionHandle[]> it = instructionFinder.search("getfield putfield"); it.hasNext();) {
                InstructionHandle[] ih = it.next();
                GETFIELD getfield = (GETFIELD) ih[0].getInstruction();
                PUTFIELD putfield = (PUTFIELD) ih[1].getInstruction();
                if (getfield.getFieldType(cpg) != Type.INT)
                    continue;
                final String className = getfield.getClassName(cpg);
                final String fieldName = getfield.getFieldName(cpg);
                if (className.equals(mouseX.getClassName()) && fieldName.equals(mouseX.getFieldName())) {
                    hookHandler.addFieldHook("Mouse", putfield, cpg, "clientMouseX");
                    foundMethod = true;
                } else if (className.equals(mouseY.getClassName()) && fieldName.equals(mouseY.getFieldName())) {
                    hookHandler.addFieldHook("Mouse", putfield, cpg, "clientMouseY");
                    foundMethod = true;
                }
            }
            if (foundMethod)
                return;
        }
    }


    public String[] getFieldHooks() {
        return new String[]{
                "Mouse::mouseX",
                "Mouse::mouseY",
                "Mouse::clientMouseX",
                "Mouse::clientMouseY",
        };
    }
}
