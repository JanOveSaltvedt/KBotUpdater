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
import com.sun.org.apache.bcel.internal.generic.ClassGen;
import com.sun.org.apache.bcel.internal.classfile.Field;

/**
 * Created by IntelliJ IDEA.
 * User: Jan Ove Saltvedt
 * Date: Sep 19, 2009
 * Time: 11:39:59 PM
 * To change this template use File | Settings | File Templates.
 */
public class GameObjectNodeTransformer extends AbstractTransformer {
    @Override
    public Class<?>[] getDependencies() {
        return new Class<?>[]{GameObjectTransformer.class};
    }

    public void run() {
        ClassGen gameObjectCG = hookHandler.getClassByNick("GameObject");
        for(ClassGen cG: classes){
            int nonStaticFields = 0;
            int gameObjectCount = 0;
            int selfCount = 0;
            for(Field field: cG.getFields()){
                if(field.isStatic())
                    continue;
                nonStaticFields++;
                if(field.getType().toString().equals(gameObjectCG.getClassName())){
                    gameObjectCount++;
                }
                else if(field.getType().toString().equals(cG.getClassName())){
                    selfCount++;
                }
            }
            if(nonStaticFields != 3 || gameObjectCount != 1 || selfCount != 1)
                continue;
            hookHandler.addClassNick("GameObjectNode", cG);
            for(Field field: cG.getFields()){
                if(field.isStatic())
                    continue;
                if(field.getType().toString().equals(gameObjectCG.getClassName())){
                    hookHandler.addFieldHook("GameObjectNode", cG, field, "containedGameObject", TypeBuilder.createHookType("GameObject"));
                }
                else if(field.getType().toString().equals(cG.getClassName())){
                    hookHandler.addFieldHook("GameObjectNode", cG, field, "nextNode", TypeBuilder.createHookType("GameObjectNode"));
                }
            }
        }
    }

    public String[] getFieldHooks() {
        return new String[]{
                "GameObjectNode::nextNode",
                "GameObjectNode::containedGameObject"
        };
    }
}
