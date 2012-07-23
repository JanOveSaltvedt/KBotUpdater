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
import com.hdupdater.utils.TypeBuilder;
import com.sun.org.apache.bcel.internal.generic.*;
import com.sun.org.apache.bcel.internal.classfile.Field;
import com.sun.org.apache.bcel.internal.classfile.Method;
import com.sun.org.apache.bcel.internal.util.InstructionFinder;

import java.util.Iterator;

/**
 * Created by IntelliJ IDEA.
 * User: Jan Ove Saltvedt
 * Date: Sep 19, 2009
 * Time: 11:50:03 PM
 * To change this template use File | Settings | File Templates.
 */
public class TileDataTransformer extends AbstractTransformer {
    @Override
    public Class<?>[] getDependencies() {
        return new Class<?>[]{GameObjectNodeTransformer.class, GameObjectTransformer.class, RenderableTransformer.class};
    }

    public void run() {
        ClassGen gameObjectNodeCG = hookHandler.getClassByNick("GameObjectNode");
        ClassGen gameObjectCG = hookHandler.getClassByNick("GameObject");
        for(ClassGen cG: classes){
            int nonStaticFieldCount = 0;
            int gameObjectNodeCount = 0;
            int gameObjectCount = 0;
            for(Field field: cG.getFields()){
                if(field.isStatic())
                    continue;
                nonStaticFieldCount++;
                if(field.getType().toString().equals(gameObjectCG.getClassName())){
                    gameObjectCount++;
                }
                else if(field.getType().toString().equals(gameObjectNodeCG.getClassName())){
                    gameObjectNodeCount++;
                }
            }
            if(nonStaticFieldCount < 20 || nonStaticFieldCount > 25 || gameObjectNodeCount != 1)
                continue;

            hookHandler.addClassNick("TileData", cG);
            for(Field field: cG.getFields()){
                if(field.isStatic())
                    continue;
                else if(field.getType().toString().equals(gameObjectNodeCG.getClassName())){
                    hookHandler.addFieldHook("TileData", cG, field, "gameObjectNodeHeader", TypeBuilder.createHookType("GameObjectNode"));
                }
            }
        }

        ClassGen tileDataCG = hookHandler.getClassByNick("TileData");
        for(ClassGen cG: classes){
            ConstantPoolGen cpg = cG.getConstantPool();
            if(cpg.lookupClass(tileDataCG.getClassName()) == -1){
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
                if(args.length < 1 || args.length > 2){
                    continue;
                }
                int gameObjectCount = TypeCounter.getCount(args, Type.getType("L"+gameObjectCG.getClassName()+";"));
                if(gameObjectCount != 1){
                    continue;
                }
                InstructionFinder instructionFinder = new InstructionFinder(new InstructionList(method.getCode().getCode()));
                for (Iterator<InstructionHandle[]> iterator = instructionFinder.search("getstatic"); iterator.hasNext();) {
                    InstructionHandle[] ih = iterator.next();
                    FieldInstruction fieldInstruction = (FieldInstruction) ih[0].getInstruction();
                    Type type = fieldInstruction.getType(cpg);
                    if(type instanceof ArrayType){
                        ArrayType arrayType = (ArrayType) type;
                        if(arrayType.getDimensions() == 3){
                            if(arrayType.getBasicType().toString().equals(tileDataCG.getClassName())){
                                hookHandler.addClientHook(fieldInstruction, cpg, "tileDataArray", TypeBuilder.createHookArrayType("TileData", 3));
                            }
                        }
                    }
                }
            }
        }

        ClassGen renderableCG = hookHandler.getClassByNick("Renderable");

        for(ClassGen cG: classes){
            ConstantPoolGen cpg = cG.getConstantPool();
            for(Method method: cG.getMethods()){
                if(!method.isStatic()){
                    continue;
                }
                Type[] args = method.getArgumentTypes();
                if(args.length < 2 || args.length > 3){
                    continue;
                }
                if(TypeCounter.getCount(args, Type.getType("[[[L"+tileDataCG.getClassName()+";")) != 1){
                    continue;
                }
                if(TypeCounter.getBasicTypeCount(args) < 1){
                    continue;
                }
                int hit = 0;
                InstructionFinder instructionFinder = new InstructionFinder(new InstructionList(method.getCode().getCode()));
                for (Iterator<InstructionHandle[]> iterator = instructionFinder.search("aload getfield"); iterator.hasNext();) {
                    InstructionHandle[] ih = iterator.next();
                    FieldInstruction fieldInstruction = (FieldInstruction) ih[1].getInstruction();
                    Type type = fieldInstruction.getType(cpg);
                    ClassGen typeCG = hookHandler.classes.get(type.toString());
                    if(typeCG == null){
                        continue;
                    }

                    ClassGen superClass = hookHandler.classes.get(typeCG.getClassName());
                    if(superClass == null){
                        continue;
                    }

                    if(superClass.getClassName().equals(renderableCG.getClassName()) || superClass.getSuperclassName().equals(renderableCG.getClassName())){
                        hit++;
                        if(hit == 1){
                            hookHandler.addFieldHook("TileData", fieldInstruction, cpg, "decorationObject", TypeBuilder.createHookType("PhysicalObject"));
                        }
                        else if(hit == 3){
                            hookHandler.addFieldHook("TileData", fieldInstruction, cpg, "object2", TypeBuilder.createHookType("PhysicalObject"));
                        }
                        else if(hit == 5){
                            hookHandler.addFieldHook("TileData", fieldInstruction, cpg, "boundingObject1", TypeBuilder.createHookType("PhysicalObject"));
                        }
                        else if(hit == 7){
                            hookHandler.addFieldHook("TileData", fieldInstruction, cpg, "boundingObject2", TypeBuilder.createHookType("PhysicalObject"));
                        }
                        else if(hit == 9){
                            hookHandler.addFieldHook("TileData", fieldInstruction, cpg, "object5", TypeBuilder.createHookType("PhysicalObject"));
                        }
                    }
                }

            }
        }
    }

    public String[] getFieldHooks() {
        return new String[]{
                "TileData::gameObjectNodeHeader",
                "Client::tileDataArray",
                "TileData::decorationObject",
                "TileData::object2",
                "TileData::boundingObject1",
                "TileData::boundingObject2",
                "TileData::object5",

                
        };
    }
}
