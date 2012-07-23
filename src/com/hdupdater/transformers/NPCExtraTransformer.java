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
import com.sun.org.apache.bcel.internal.classfile.Method;
import com.sun.org.apache.bcel.internal.generic.*;
import com.sun.org.apache.bcel.internal.util.InstructionFinder;

import java.util.Iterator;


/**
 * Created by IntelliJ IDEA.
 * User: Jan Ove Saltvedt
 * Date: Jun 1, 2010
 * Time: 5:49:53 PM
 * To change this template use File | Settings | File Templates.
 */
public class NPCExtraTransformer extends AbstractTransformer {
    @Override
    public Class<?>[] getDependencies() {
        return new Class<?>[]{RendererTransformer.class, OtherPhysicalObject.class, NPCTransformer.class};
    }

    @Override
    public void run() {
        Method setOffsets2Method = (Method) hookHandler.dataMap.get("RenderVars::setOffsets2")[0];
        ClassGen cG = hookHandler.getClassByNick("NPC");
        ConstantPoolGen cpg = cG.getConstantPool();
        ClassGen npcDefCG = hookHandler.getClassByNick("NPCDef");
        ClassGen renderCG = hookHandler.getClassByNick("Renderer");
        for (Method method : cG.getMethods()) {
            if (method.isStatic()) {
                continue;
            }
            if (!method.getReturnType().equals(Type.BOOLEAN)) {
                continue;
            }
            final Type[] args = method.getArgumentTypes();
            if (args.length < 3 || args.length > 4) {
                continue;
            }
            if (TypeCounter.getObjectCount(args) != 1) {
                continue;
            }
            if (TypeCounter.getCount(args, new ObjectType(renderCG.getClassName())) != 1) {
                continue;
            }
            InstructionFinder instructionFinder = new InstructionFinder(new InstructionList(method.getCode().getCode()));
            for (Iterator<InstructionHandle[]> iterator = instructionFinder.search("((ConstantPushInstruction aload getfield getfield)|(aload getfield getfield ConstantPushInstruction)) IfInstruction"); iterator.hasNext();) {
                InstructionHandle[] ih = iterator.next();
                int hit = 0;
                boolean foundNpcDef = false;
                for (InstructionHandle handle : ih) {
                    if (handle.getInstruction() instanceof GETFIELD) {
                        hit++;
                        if (hit == 1) {
                            FieldInstruction fieldInstruction = (FieldInstruction) handle.getInstruction();
                            if (fieldInstruction.getFieldType(cpg).equals(new ObjectType(npcDefCG.getClassName()))) {
                                foundNpcDef = true;
                            }
                        } else if (hit == 2 && foundNpcDef) {
                            FieldInstruction fieldInstruction = (FieldInstruction) handle.getInstruction();
                            hookHandler.addFieldHook("NPCDef", fieldInstruction, cpg, "collisionDetailLevel");
                        }
                    }
                }
            }

            for (Iterator<InstructionHandle[]> iterator = instructionFinder.search("aload_0 getfield aload_0 getfield aload_0 getfield InvokeInstruction"); iterator.hasNext();) {
                InstructionHandle[] ih = iterator.next();
                InvokeInstruction invokeInstruction = (InvokeInstruction) ih[ih.length - 1].getInstruction();
                if (/*!invokeInstruction.getMethodName(cpg).equals(setOffsets2Method.getName())
                        || */!invokeInstruction.getSignature(cpg).equals(setOffsets2Method.getSignature())) {
                    continue;
                }


                FieldInstruction fieldInstruction = (FieldInstruction) ih[3].getInstruction();
                hookHandler.addFieldHook("GameObject", fieldInstruction, cpg, "posZ");
            }
        }
    }


    @Override
    public String[] getFieldHooks() {
        return new String[]{
                "NPCDef::collisionDetailLevel",
                "GameObject::posZ"

        };  //To change body of implemented methods use File | Settings | File Templates.
    }
}
