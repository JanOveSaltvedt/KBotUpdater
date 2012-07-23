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
import com.sun.org.apache.bcel.internal.generic.*;
import com.sun.org.apache.bcel.internal.classfile.Field;
import com.sun.org.apache.bcel.internal.classfile.Method;
import com.sun.org.apache.bcel.internal.util.InstructionFinder;

import java.util.Iterator;

/**
 * Created by IntelliJ IDEA.
 * User: Jan Ove Saltvedt
 * Date: Sep 28, 2009
 * Time: 7:18:27 PM
 * To change this template use File | Settings | File Templates.
 */
public class NodeListTransformer extends AbstractTransformer {
    @Override
    public Class<?>[] getDependencies() {
        return new Class<?>[]{NodeTransformer.class};
    }

    public void run() {
        ClassGen nodeCG = hookHandler.getClassByNick("Node");
        for(ClassGen cG: classes){
            ConstantPoolGen cpg = cG.getConstantPool();
            int nonstatic = 0;
            int nodeFields = 0;
            for(Field field: cG.getFields()){
                if(field.isStatic())
                    continue;
                nonstatic++;
                if(field.getSignature().equals("L"+nodeCG.getClassName()+";"))
                    nodeFields++;
            }
            if(nonstatic != 2 || nodeFields != 2)
                    continue;
            String fieldHeadName = "";
            for(Method m: cG.getMethods()){
                if(m.getName().equals("<init>")){
                    InstructionFinder instructionFinder = new InstructionFinder(new InstructionList(m.getCode().getCode()));
                    Iterator<InstructionHandle[]> iterator = instructionFinder.search("putfield");
                    if(iterator.hasNext()) {
                        fieldHeadName = ((FieldInstruction)iterator.next()[0].getInstruction()).getFieldName(cpg);
                    }
                }
            }
            hookHandler.addClassNick("NodeList", cG);
            for(Field field: cG.getFields()){
                if(field.isStatic())
                    continue;
                if(field.getName().equals(fieldHeadName)){
                    hookHandler.addFieldHook("NodeList", cG, field, "headNode", TypeBuilder.createHookType("Node"));
                }
                else{
                    hookHandler.addFieldHook("NodeList", cG, field, "currentNode", TypeBuilder.createHookType("Node"));
                }
            }
        }
    }

    public String[] getFieldHooks() {
        return new String[]{
                "NodeList::headNode",
                "NodeList::currentNode",
        };  //To change body of implemented methods use File | Settings | File Templates.
    }
}
