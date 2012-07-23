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
import com.hdupdater.utils.TypeBuilder;
import com.hdupdater.hooks.FieldHook;
import com.sun.org.apache.bcel.internal.generic.*;
import com.sun.org.apache.bcel.internal.classfile.Method;
import com.sun.org.apache.bcel.internal.classfile.Field;
import com.sun.org.apache.bcel.internal.util.InstructionFinder;

import java.util.Iterator;

/**
 * Created by IntelliJ IDEA.
 * User: Jan Ove Saltvedt
 * Date: Sep 28, 2009
 * Time: 7:08:47 PM
 * To change this template use File | Settings | File Templates.
 */
public class NodeTransformer extends AbstractTransformer {
    public void run() {
        for (ClassGen cG : classes) {
            if (!cG.getSuperclassName().equals(Object.class.getName()))
                continue;
            int nsMethods = 0;
            for (Method m : cG.getMethods())
                if (!m.isStatic())
                    nsMethods++;
            int longCount = 0;
            int selfCount = 0;
            for (Field field : cG.getFields()) {
                if (!field.isStatic()) {
                    if (field.getSignature().equals("L" + cG.getClassName() + ";"))
                        selfCount++;
                    if (field.getType().equals(Type.LONG))
                        longCount++;
                }
            }
            if (nsMethods != 3 || longCount != 1 || selfCount != 2)
                continue;
            hookField(cG);
        }
    }

    private void hookField(ClassGen cG) {
        hookHandler.addClassNick("Node", cG);
        ConstantPoolGen cpg = cG.getConstantPool();
        for (Method m : cG.getMethods()) {
            if (m.isStatic())
                continue;
            if(m.getReturnType() != Type.BOOLEAN)
                continue;
            String pattern = "(GETFIELD)(BranchInstruction)";
            InstructionFinder instructionFinder = new InstructionFinder(new InstructionList(m.getCode().getCode()));
            for (Iterator<InstructionHandle[]> it = instructionFinder.search(pattern); it.hasNext();) {
                InstructionHandle[] ih = it.next();
                GETFIELD getfield = (GETFIELD) ih[0].getInstruction();

                final String fieldname = getfield.getFieldName(cpg);
                hookHandler.addFieldHook("Node", getfield, cpg, "nextNode", TypeBuilder.createHookType("Node"));
                for(Field f: cG.getFields()){
                    if(f.isStatic())
                        continue;
                    if(f.getSignature().equals("L"+cG.getClassName()+";")){
                        if(!f.getName().equals(fieldname)){
                            hookHandler.addFieldHook("Node", cG, f, "prevNode", TypeBuilder.createHookType("Node"));
                        }
                    }
                }
                break;
            }
        }
        for (Field field : cG.getFields()) {
            if (field.isStatic())
                continue;
            if (field.getType() == Type.LONG)
                hookHandler.addFieldHook("Node", cG, field, "nodeID");
        }
    }


    public String[] getFieldHooks() {
        return new String[]{
                "Node::nextNode",
                "Node::prevNode",
                "Node::nodeID",
        };
    }
}
