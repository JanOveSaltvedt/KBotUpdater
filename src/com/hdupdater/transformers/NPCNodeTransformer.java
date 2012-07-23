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
import com.hdupdater.utils.TypeBuilder;
import com.hdupdater.utils.TypeCounter;
import com.sun.org.apache.bcel.internal.classfile.Field;
import com.sun.org.apache.bcel.internal.classfile.Method;
import com.sun.org.apache.bcel.internal.generic.*;
import com.sun.org.apache.bcel.internal.util.InstructionFinder;

import java.util.Iterator;

/**
 * Created by IntelliJ IDEA.
 * User: Jan Ove Saltvedt
 * Date: 29.mar.2010
 * Time: 22:34:01
 * To change this template use File | Settings | File Templates.
 */
public class NPCNodeTransformer extends AbstractTransformer {
    @Override
    public Class<?>[] getDependencies() {
        return new Class<?>[]{NPCTransformer.class, NodeTransformer.class, NodeCacheTransformer.class};
    }

    @Override
    public void run() {
        ClassGen nodeCG = hookHandler.getClassByNick("Node");
        ClassGen npcCG = hookHandler.getClassByNick("NPC");
        for(ClassGen cG: classes){
            ConstantPoolGen cpg = cG.getConstantPool();
            if(!cG.getSuperclassName().equals(nodeCG.getClassName())){
                continue;
            }

            int nonStaticFieldCount = 0;
            int npcFieldCount = 0;
            for(Field field: cG.getFields()){
                if(field.isStatic()){
                    continue;
                }
                nonStaticFieldCount++;
                if(field.getType().toString().equals(npcCG.getClassName())){
                    npcFieldCount++;
                }
            }
            if(nonStaticFieldCount != 1 || npcFieldCount != 1){
                continue;
            }

            hookHandler.addClassNick("NPCNode", cG);
            for(Field field: cG.getFields()){
                if(field.isStatic()){
                    continue;
                }
                if(field.getType().toString().equals(npcCG.getClassName())){
                    hookHandler.addFieldHook("NPCNode", cG, field, "NPC", TypeBuilder.createHookType("NPC"));
                }
            }

        }
        ClassGen npcNodeCG = hookHandler.getClassByNick("NPCNode");
        for(ClassGen cG: classes){
            for(Field field: cG.getFields()){
                if(!field.isStatic()){
                    continue;
                }
                if(field.getType().getSignature().equals("[L"+npcNodeCG.getClassName()+";")){
                    hookHandler.addClientHook(cG, field, "NPCNodes", TypeBuilder.createHookArrayType("NPCNode", 1));
                }
            }
        }
        ClassGen nodeCacheCG = hookHandler.getClassByNick("NodeCache");
        FieldHook fieldHook = hookHandler.getFieldHook("NPCNode", "NPC");
        for(ClassGen cG: classes){
            ConstantPoolGen cpg = cG.getConstantPool();
            if(cpg.lookupClass(nodeCacheCG.getClassName()) == -1){
                continue;
            }
            if(cpg.lookupFieldref(fieldHook.getClassName(), fieldHook.getFieldName(), fieldHook.getFieldType().getSignature()) == -1){
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
                if(args.length < 13 || args.length > 14){
                    continue;
                }
                if(TypeCounter.getObjectCount(args) != 0){
                    continue;
                }
                InstructionList iList = new InstructionList(method.getCode().getCode());
                InstructionFinder instructionFinder = new InstructionFinder(iList);
                for(Iterator<InstructionHandle[]> iterator = instructionFinder.search("getstatic"); iterator.hasNext();){
                    InstructionHandle[] ih = iterator.next();
                    FieldInstruction fieldInstruction = (FieldInstruction) ih[0].getInstruction();
                    if(fieldInstruction.getFieldType(cpg).toString().equals(nodeCacheCG.getClassName())){
                        hookHandler.addClientHook(fieldInstruction, cpg, "NPCNodeCache", TypeBuilder.createHookType("NodeCache"));
                    }
                }

            }
        }

    }

    @Override
    public String[] getFieldHooks() {
        return new String[]{
                "NPCNode::NPC",
                "Client::NPCNodes",
                "Client::NPCNodeCache"
        };  //To change body of implemented methods use File | Settings | File Templates.
    }
}
