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
import com.hdupdater.hooks.ConditionalVoidDisablerHook;
import com.hdupdater.utils.TypeCounter;
import com.sun.org.apache.bcel.internal.classfile.Field;
import com.sun.org.apache.bcel.internal.classfile.Method;
import com.sun.org.apache.bcel.internal.generic.ClassGen;
import com.sun.org.apache.bcel.internal.generic.ObjectType;
import com.sun.org.apache.bcel.internal.generic.Type;

/**
 * Created by IntelliJ IDEA.
 * User: Jan Ove Saltvedt
 * Date: Mar 24, 2010
 * Time: 3:27:41 PM
 * To change this template use File | Settings | File Templates.
 */
public class RenderingRemoval extends AbstractTransformer {
    @Override
    public Class<?>[] getDependencies() {
        return new Class<?>[]{RendererTransformer.class};
    }

    @Override
    public void run() {
        ClassGen renderer = hookHandler.getClassByNick("RendererSD");
        for(ClassGen superCG: classes){
            if(!superCG.isAbstract()){
                continue;
            }
            int subclassCount = 0;
            for(ClassGen cG2: classes){
                if(!cG2.getSuperclassName().equals(superCG.getClassName())){
                    continue;
                }
                subclassCount++;
            }
            if(subclassCount != 4){
                continue;
            }
            for(ClassGen cG: classes){
                if(!cG.getSuperclassName().equals(superCG.getClassName())){
                    continue;
                }
                boolean foundField = false;
                Type rendererType = new ObjectType(renderer.getClassName());
                for(Field field: cG.getFields()){
                    if(field.isStatic()){
                        continue;
                    }
                    if(field.getType().equals(rendererType)){
                        foundField = true;
                    }
                }
                if(!foundField){
                    continue;
                }
                boolean foundConstructor = false;
                for(Method method: cG.getMethods()){
                    if(!method.getName().equals("<init>")){
                        continue;
                    }
                    if(TypeCounter.getCount(method.getArgumentTypes(), rendererType) == 1){
                        foundConstructor = true;
                    }
                }
                if(!foundConstructor){
                    continue;
                }

                // We should have the two classes we are looking for now

                hookGround(cG);
                hookObjects(cG);

                
            }

        }
    }

    private void hookObjects(ClassGen cG) {
        for(Method method: cG.getMethods()){
            if(method.isStatic()){
                continue;
            }
            if(!method.getReturnType().equals(Type.VOID)){
                continue;
            }
            final Type[] args = method.getArgumentTypes();
            if(args.length < 5 || method.getArgumentTypes().length > 6){
                continue;
            }
            if(TypeCounter.getCount(args, Type.BOOLEAN) != 3){
                continue;
            }
            if(TypeCounter.getIntCount(args) + TypeCounter.getByteCount(args) + 3 != args.length){
                continue;
            }
            System.out.println("Hooked renderObjects  method: "+cG.getClassName()+" "+method);
            new ConditionalVoidDisablerHook(hookHandler, cG, method, "disableObjectsRender");
        }

    }

    private void hookGround(ClassGen cG) {
        for(Method method: cG.getMethods()){
            if(method.isStatic()){
                continue;
            }
            if(!method.getReturnType().equals(Type.VOID)){
                continue;
            }
            final Type[] args = method.getArgumentTypes();
            if(args.length < 7 || method.getArgumentTypes().length > 8){
                continue;
            }
            if(TypeCounter.getCount(args, Type.getType(int[].class)) != 2){
                continue;
            }
            if(TypeCounter.getObjectCount(args) != 1){
                continue;
            }
            if(TypeCounter.getCount(args, Type.BOOLEAN) != 1){
                continue;
            }
            if(TypeCounter.getIntCount(args) + TypeCounter.getByteCount(args) + 4 != args.length){
                continue;
            }
            System.out.println("Hooked renderGround method: "+cG.getClassName()+" "+method);
            new ConditionalVoidDisablerHook(hookHandler, cG, method, "disableGroundRender");
        }
    }

    @Override
    public String[] getFieldHooks() {
        return new String[0];
    }
}
