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
import com.hdupdater.hooks.InjectFieldHook;
import com.hdupdater.hooks.CharacterModelDumperHook;
import com.hdupdater.hooks.VirtualFieldGetterHook;
import com.hdupdater.utils.TypeBuilder;
import com.hdupdater.utils.TypeCounter;
import com.sun.org.apache.bcel.internal.generic.*;
import com.sun.org.apache.bcel.internal.classfile.Field;
import com.sun.org.apache.bcel.internal.classfile.Method;
import com.sun.org.apache.bcel.internal.util.InstructionFinder;

import java.util.Iterator;

/**
 * Created by IntelliJ IDEA.
 * User: Jan Ove Saltvedt
 * Date: Oct 13, 2009
 * Time: 6:08:01 PM
 * To change this template use File | Settings | File Templates.
 */
public class ModelTransformer extends AbstractTransformer {

    @Override
    public Class<?>[] getDependencies() {
        return new Class<?>[]{InteractivePhysicalObjectTransformer.class, CharacterTransformer.class};
    }

    public void run() {
        ClassGen POCG = hookHandler.getClassByNick("InteractivePhysicalObject");
        for(Field field: POCG.getFields()){
            if(field.isStatic()){
                continue;
            }

            if(!(field.getType() instanceof ObjectType)){
                continue;
            }

            Type type = field.getType();
            if(hookHandler.classes.get(type.toString()) == null){
                continue;
            }

            ClassGen cG = hookHandler.classes.get(type.toString());
            if(hookHandler.classes.get(cG.getSuperclassName()) != null){
                continue;
            }

            int subclasscount = 0;
            for(ClassGen cG2: classes){
                if(cG2.getSuperclassName().equals(cG.getClassName())){
                    subclasscount++;
                }
            }

            if(subclasscount != 4){
                continue;
            }

            hookHandler.addClassNick("Model", cG);
            hookHandler.addFieldHook("InteractivePhysicalObject", POCG, field, "model", TypeBuilder.createHookType("Model"));
        }

        ClassGen modelCG = hookHandler.getClassByNick("Model");
        for(ClassGen cG: classes){
            if(!cG.getSuperclassName().equals(modelCG.getClassName())){
                continue;
            }
            boolean containsFloats = false;
            for(Field field: cG.getFields()){
                if(field.isStatic()){
                    continue;
                }
                if(field.getType().equals(Type.getType(float[].class))){
                    containsFloats = true;
                    break;
                }
            }
            if(containsFloats){
                hookHandler.addClassNick("ModelGL", cG);
            }
            else{
                boolean containNatives = false;
                 for(Method method: cG.getMethods()){
                    if(method.isNative()){
                        containNatives = true;
                    }
                }
                if(!containNatives){
                    hookHandler.addClassNick("ModelSD", cG);
                }
            }
        }
        hookFromCharacter(modelCG);

    }

    private void hookFromCharacter(ClassGen modelCG) {
        ClassGen charCG = hookHandler.getClassByNick("Character");
        for(Field field: charCG.getFields()){
            if(field.isStatic()){
                continue;
            }
            if(field.getType().getSignature().equals("[L"+modelCG.getClassName()+";")){
                hookHandler.addFieldHook("Character", charCG, field, "models", TypeBuilder.createHookArrayType("Model", 1));
            }
        }

        Method modelMethod = null;
        for(Method method: charCG.getMethods()){
            if(method.isStatic()){
                continue;
            }
            Type[] args = method.getArgumentTypes();
            if(args.length < 3 || args.length > 4){
                continue;
            }

            if(TypeCounter.getCount(args, Type.getType("[L"+modelCG.getClassName()+";")) != 1){
                continue;
            }

            InstructionFinder instructionFinder = new InstructionFinder(new InstructionList(method.getCode().getCode()));
            for (Iterator<InstructionHandle[]> iterator = instructionFinder.search("aload iconst_0 aaload astore"); iterator.hasNext();) {
                InstructionHandle[] ih = iterator.next();
                modelMethod = method;
            }
        }
        if(modelMethod == null){
            return;
        }
        for(ClassGen cG: normalClasses){
            if(!cG.getClassName().equals(charCG.getClassName())){
                continue;
            }
            for(Method method: cG.getMethods()){
                if(method.isStatic()){
                    continue;
                }
                if(!method.getName().equals(modelMethod.getName())){
                    continue;
                }
                if(!method.getSignature().equals(modelMethod.getSignature())){
                    continue;
                }
                InstructionFinder instructionFinder = new InstructionFinder(new InstructionList(method.getCode().getCode()));
                for (Iterator<InstructionHandle[]> iterator = instructionFinder.search("aload iconst_0 aaload astore"); iterator.hasNext();) {
                    InstructionHandle[] ih = iterator.next();
                    new InjectFieldHook(hookHandler, charCG, "model", TypeBuilder.createHookType("ModelWrapper").getSignature(), 0);
                    new VirtualFieldGetterHook(hookHandler,charCG, "model", TypeBuilder.createHookType("ModelWrapper"), TypeBuilder.createHookType("Model"), "Character");
                    ASTORE astore = (ASTORE) ih[3].getInstruction();

                    new CharacterModelDumperHook(hookHandler, charCG, method, ih[3].getPosition(), modelCG, astore.getIndex());
                }

            }
        }
    }

    public String[] getFieldHooks() {
        return new String[]{
                "InteractivePhysicalObject::model",
                "Character::models",
                "Character::model",
        };  //To change body of implemented methods use File | Settings | File Templates.
    }
}
