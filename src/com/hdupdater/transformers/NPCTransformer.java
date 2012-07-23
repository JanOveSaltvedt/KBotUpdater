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
import com.hdupdater.utils.InstructionUtils;
import com.sun.org.apache.bcel.internal.generic.*;
import com.sun.org.apache.bcel.internal.classfile.Field;
import com.sun.org.apache.bcel.internal.classfile.Method;
import com.sun.org.apache.bcel.internal.util.InstructionFinder;

import java.util.Iterator;

/**
 * Created by IntelliJ IDEA.
 * User: Jan Ove / Kosaki
 * Date: 28.aug.2009
 * Time: 16:58:53
 */
public class NPCTransformer extends AbstractTransformer {
    @Override
    public Class<?>[] getDependencies() {
        return new Class<?>[]{CharacterTransformer.class};
    }

    public void run() {
        ClassGen charCG = hookHandler.getClassByNick("Character");
        ClassGen playerCG = hookHandler.getClassByNick("Player");
        for(ClassGen cG: classes){
            if(!cG.getSuperclassName().equals(charCG.getClassName()))
                continue;
            if(cG.getClassName().equals(playerCG.getClassName()))
                continue;
            hookHandler.addClassNick("NPC", cG);
            for (Field f : cG.getFields()) {
                if (!f.isStatic() && f.getType() instanceof ObjectType) {
                    hookHandler.addClassNick("NPCDef", f.getType().toString());
                    hookHandler.addFieldHook("NPC", cG.getClassName(), f.getName(), "NPCDef", TypeBuilder.createHookType("NPCDef"));
                }
            }
            hookLocation(cG);
        }
    }

    private void hookLocation(final ClassGen npcCG){
        final FieldHook walkingX = hookHandler.getFieldHook("Character", "walkingX");
        final FieldHook walkingY = hookHandler.getFieldHook("Character", "walkingY");
        final ConstantPoolGen cpg = npcCG.getConstantPool();
        for (Method method : npcCG.getMethods()) {
            if (method.isStatic()){
                continue;
            }
            if(!method.getReturnType().equals(Type.VOID)){
                continue;
            }
            Type[] args = method.getArgumentTypes();
            int basicTypeCount = TypeCounter.getBasicTypeCount(args);
            if(basicTypeCount < 5 || basicTypeCount > 6){
                continue;
            }
            MethodGen methodGen = new MethodGen(method, npcCG.getClassName(), cpg);
            InstructionList iList = methodGen.getInstructionList();
            InstructionFinder instructionFinder = new InstructionFinder(iList);
            int i = 0;
            i = i << 9;
            for(Iterator<InstructionHandle[]> iterator = instructionFinder.search("putfield"); iterator.hasNext();){
                InstructionHandle[] ih = iterator.next();
                FieldInstruction fieldInstruction = (FieldInstruction) ih[0].getInstruction();
                boolean found128 = false;
                boolean found64 = false;
                InstructionHandle[][] parameters = InstructionUtils.getParameters(ih[0], cpg, 1);
                for(InstructionHandle instructionHandle: parameters[0]){
                    if(instructionHandle.getInstruction() instanceof ConstantPushInstruction){
                        ConstantPushInstruction pushInstruction = (ConstantPushInstruction) instructionHandle.getInstruction();
                        if(pushInstruction.getValue().intValue() == 128){
                            found128 = true;
                        }
                        else if(pushInstruction.getValue().intValue() == 64){
                            found64 = true;
                        }
                        if(pushInstruction.getValue().intValue() == 7){
                            found128 = true;
                        }
                        else if(pushInstruction.getValue().intValue() == 6){
                            found64 = true;
                        }
                        if(pushInstruction.getValue().intValue() == 8){
                            found128 = true;
                        }
                        else if(pushInstruction.getValue().intValue() == 9){
                            found64 = true;
                        }

                    }
                }

                for(InstructionHandle instructionHandle: parameters[0]){
                    if(instructionHandle.getInstruction() instanceof LDC){
                        LDC ldc = (LDC) instructionHandle.getInstruction();
                        if(!ldc.getType(cpg).equals(Type.INT)){
                            continue;
                        }
                        int value = (Integer)ldc.getValue(cpg);
                        if(value == 128){
                            found128 = true;
                        }
                        else if(value == 64){
                            found64 = true;
                        }
                        if(value == 7){
                            found128 = true;
                        }
                        else if(value == 6){
                            found64 = true;
                        }
                        if(value == 8){
                            found128 = true;
                        }
                        else if(value == 9){
                            found64 = true;
                        }

                    }
                }

                if(found128 && found64){
                    for(InstructionHandle instructionHandle: parameters[0]){
                        if(instructionHandle.getInstruction() instanceof GETFIELD){
                            FieldInstruction walkingFieldInstruction = (FieldInstruction) instructionHandle.getInstruction();
                            if(walkingFieldInstruction.getClassName(cpg).equals(walkingX.getClassName())
                                    && walkingFieldInstruction.getFieldName(cpg).equals(walkingX.getFieldName())) {
                                hookHandler.addFieldHook("GameObject", fieldInstruction,cpg, "posX");
                            }
                            else if(walkingFieldInstruction.getClassName(cpg).equals(walkingY.getClassName())
                                    && walkingFieldInstruction.getFieldName(cpg).equals(walkingY.getFieldName())) {
                                hookHandler.addFieldHook("GameObject", fieldInstruction,cpg, "posY");
                            }
                        }
                    }

                }
            }
        }
        for (Method m : npcCG.getMethods()) {
            if (m.isStatic())
                continue;
            if(!m.getReturnType().equals(Type.VOID)){
                continue;
            }
            Type[] args = m.getArgumentTypes();
            if (args.length < 5 || args.length > 7)
                continue;
            int intCount = 0;
            int boolCount = 0;
            for (Type arg : args) {
                if (arg == Type.INT || arg == Type.BYTE)
                    intCount++;
                if (arg == Type.BOOLEAN)
                    boolCount++;
            }
            if (intCount < 3 || intCount > 5 || boolCount < 1 || boolCount > 2)
                continue;

            String pattern = "(ICONST_M1)(PUTFIELD)";
            InstructionFinder instructionFinder = new InstructionFinder(new InstructionList(m.getCode().getCode()));
            for (Iterator<InstructionHandle[]> it = instructionFinder.search(pattern); it.hasNext();) {
                InstructionHandle[] ih = it.next();
                FieldInstruction fieldIns = (FieldInstruction) ih[1].getInstruction();
                hookHandler.addFieldHook("Character", fieldIns, cpg, "animation");
                break;
            }

            pattern = "(GETFIELD)(ICONST_1)(IADD)(PUTFIELD)";
            instructionFinder = new InstructionFinder(new InstructionList(m.getCode().getCode()));
            for (Iterator<InstructionHandle[]> it = instructionFinder.search(pattern); it.hasNext();) {
                InstructionHandle[] ih = it.next();
                FieldInstruction fieldIns = (FieldInstruction) ih[0].getInstruction();
                hookHandler.addFieldHook("Character", fieldIns, cpg, "motion");
                break;
            }
        }
    }

    public String[] getFieldHooks() {
        return new String[]{
                "NPC::NPCDef",
                "GameObject::posX",
                "GameObject::posY",
                "Character::animation",
                "Character::motion",
        };  //To change body of implemented methods use File | Settings | File Templates.
    }
}
