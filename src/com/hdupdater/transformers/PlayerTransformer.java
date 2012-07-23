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
import com.sun.org.apache.bcel.internal.generic.*;
import com.sun.org.apache.bcel.internal.classfile.Field;
import com.sun.org.apache.bcel.internal.classfile.Method;
import com.sun.org.apache.bcel.internal.util.InstructionFinder;

import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;

/**
 * Created by IntelliJ IDEA.
 * User: Jan Ove Saltvedt
 * Date: Oct 12, 2009
 * Time: 5:55:40 PM
 * To change this template use File | Settings | File Templates.
 */
public class PlayerTransformer extends AbstractTransformer {
    @Override
    public Class<?>[] getDependencies() {
        return new Class<?>[]{CharacterTransformer.class, GameObjectTransformer.class};
    }

    public void run() {
        ClassGen playerCG = hookHandler.getClassByNick("Player");
        for(Field field: playerCG.getFields()){
            if(field.isStatic()){
                continue;
            }
            if(field.getType().equals(Type.STRING)){
                if(hookHandler.getFieldHook("Player", "displayName") == null){
                    hookHandler.addFieldHook("Player", playerCG, field, "displayName");
                }
            }
        }

        hookHeight();
    }

    public void hookHeight(){
        ClassGen playerCG = hookHandler.getClassByNick("Player");
        ClassGen charCG = hookHandler.getClassByNick("Character");
        ClassGen gOCG = hookHandler.getClassByNick("GameObject");

        List<Method> possibleMethods = new ArrayList<Method>();
        for (Method m : gOCG.getMethods()) {
            if (m.isAbstract() && m.getArgumentTypes().length <= 1 && m.getReturnType().equals(Type.INT)) {
                int intParams = TypeCounter.getIntCount(m.getArgumentTypes()) + TypeCounter.getByteCount(m.getArgumentTypes()) + TypeCounter.getCount(m.getArgumentTypes(), Type.BOOLEAN);
                if(m.getArgumentTypes().length != intParams){
                    continue;
                }
                possibleMethods.add(m);
            }
        }
        for (Method m : playerCG.getMethods()) {

            if (m.isStatic())
                continue;
            for(Method getHeightMethod: possibleMethods){
                if (!m.getName().equals(getHeightMethod.getName()) || !m.getSignature().equals(getHeightMethod.getSignature())
                        || !m.getReturnType().equals(getHeightMethod.getReturnType()))
                    continue;
                String pattern = "(GETFIELD)(IRETURN)";
                InstructionFinder instructionFinder = new InstructionFinder(new InstructionList(m.getCode().getCode()));
                //noinspection LoopStatementThatDoesntLoop
                for (Iterator<InstructionHandle[]> it = (Iterator<InstructionHandle[]> )instructionFinder.search(pattern); it.hasNext();) {
                    InstructionHandle[] ih = it.next();
                    GETFIELD getIns = (GETFIELD) ih[0].getInstruction();
                    if(!getIns.getClassName(playerCG.getConstantPool()).equals(charCG.getClassName())){
                        continue;
                    }
                    hookHandler.addFieldHook("Character", getIns, playerCG.getConstantPool(), "height");
                }
            }
        }
    }

    public String[] getFieldHooks() {
        return new String[]{
                "Player::displayName",
                "Character::height",
        };
    }
}
