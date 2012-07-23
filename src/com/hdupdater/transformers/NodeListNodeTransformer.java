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
 * Date: Dec 10, 2009
 * Time: 7:41:39 PM
 * To change this template use File | Settings | File Templates.
 */
public class NodeListNodeTransformer extends AbstractTransformer {
    @Override
    public Class<?>[] getDependencies() {
        return new Class<?>[]{NodeTransformer.class, NodeListTransformer.class};
    }

    public void run() {
        ClassGen nodeCG = hookHandler.getClassByNick("Node");
        ClassGen nodeListCG = hookHandler.getClassByNick("NodeList");

        for(ClassGen cG: classes){
            if(!cG.getSuperclassName().equals(nodeCG.getClassName())){
                continue;
            }
            for(Field field: cG.getFields()){
                if(field.isStatic()){
                    continue;
                }
                if(field.getSignature().equals("L"+nodeListCG.getClassName()+";")){
                    hookHandler.addFieldHook("NodeListNode", cG, field, "nodeList", TypeBuilder.createHookType("NodeList"));
                    hookHandler.addClassNick("NodeListNode", cG);
                }
            }
        }
    }

    public String[] getFieldHooks() {
        return new String[]{
                "NodeListNode::nodeList"
        };  //To change body of implemented methods use File | Settings | File Templates.
    }
}
