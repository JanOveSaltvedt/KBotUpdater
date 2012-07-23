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
 * Date: Dec 6, 2009
 * Time: 1:58:11 PM
 * To change this template use File | Settings | File Templates.
 */
public class NodeCacheTransformer extends AbstractTransformer {
    @Override
    public Class<?>[] getDependencies() {
        return new Class<?>[]{NodeTransformer.class};
    }

    public void run() {
        ClassGen nodeCG = hookHandler.getClassByNick("Node");
        for(ClassGen cG: classes){
            boolean foundNodes = false;
            for(Field field: cG.getFields()){
                if(field.isStatic()){
                    continue;
                }
                if(field.getType().getSignature().equals("[L"+nodeCG.getClassName()+";")){
                    foundNodes = true;
                }
            }
            if(!foundNodes){
                continue;  
            }

            hookHandler.addClassNick("NodeCache", cG);
            for(Field field: cG.getFields()){
                if(field.isStatic()){
                    continue;
                }
                if(field.getType().getSignature().equals("[L"+nodeCG.getClassName()+";")){
                    hookHandler.addFieldHook("NodeCache", cG, field, "nodes", TypeBuilder.createHookArrayType("Node", 1));
                }
            }
        }
    }

    public String[] getFieldHooks() {
        return new String[]{
                "NodeCache::nodes",
        };
    }
}
