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
import com.hdupdater.hooks.FieldHook;
import com.hdupdater.utils.TypeCounter;
import com.sun.org.apache.bcel.internal.generic.*;
import com.sun.org.apache.bcel.internal.classfile.Field;
import com.sun.org.apache.bcel.internal.classfile.Method;
import com.sun.org.apache.bcel.internal.util.InstructionFinder;

import java.util.Iterator;

/**
 * Created by IntelliJ IDEA.
 * User: Jan Ove Saltvedt
 * Date: Oct 3, 2009
 * Time: 2:34:46 PM
 * To change this template use File | Settings | File Templates.
 */
public class PlaneTransformer extends AbstractTransformer {
    @Override
    public Class<?>[] getDependencies() {
        return new Class<?>[]{RendererTransformer.class};
    }

    public void run() {
        /*CLASS_LOOP:
        for(ClassGen cG: classes){
            ConstantPoolGen cpg = cG.getConstantPool();
            int subClassesCount = 0;
            for(ClassGen subCG: classes){
                if(subCG.getSuperclassName().equals(cG.getClassName())){
                    subClassesCount++;
                }
            }

            if(subClassesCount != 2){
                continue;
            }

            int intFieldCount = 0;
            int nsFieldCount = 0;
            for(Field f: cG.getFields()){
                if(f.isStatic()){
                    continue;
                }
                nsFieldCount++;
                if(f.getType().equals(Type.INT)){
                    intFieldCount++;
                }
            }

            if(intFieldCount < 2 || intFieldCount > 4){
                continue;
            }
            if(nsFieldCount < 2 || nsFieldCount > 4){
                continue;
            }

            for(Method m: cG.getMethods()){
                if(!m.getName().equals("<init>")){
                    continue;
                }
                if(m.getArgumentTypes().length != 2){
                    continue CLASS_LOOP;
                }
            }
            hookHandler.addClassNick("Plane", cG);
        }*/

        ClassGen rendererCG = hookHandler.getClassByNick("Renderer");
        for(Method method: rendererCG.getMethods()){
            if(method.isStatic()){
                continue;
            }
            if(!(method.getReturnType() instanceof ObjectType)){
                continue;
            }
            Type[] args = method.getArgumentTypes();
            if(args.length < 7 || args.length > 8){
                continue;
            }

            if(TypeCounter.getCount(args, Type.getType(int[][].class)) != 2){
                continue;
            }

            Type returnType = method.getReturnType();
            String className = returnType.toString();
            ClassGen cG = hookHandler.classes.get(className);
            hookHandler.addClassNick("Plane", cG);
        }

        if(hookHandler.getClassByNick("Plane") != null){
            for(ClassGen cG:classes){
                ConstantPoolGen cpg = cG.getConstantPool();
                for(Method method: cG.getMethods()){
                    if(!method.isStatic()){
                        continue;
                    }
                    // static final int getHeight(int x, int y, int plane, byte ignore)

                    if(!method.getReturnType().equals(Type.INT)){
                        continue;
                    }

                    Type[] args = method.getArgumentTypes();
                    if(args.length < 3 || args.length > 4){
                        continue;
                    }

                    int intCount = 0;
                    int objectCount = 0;
                    for(Type arg:  args){
                        if(arg.equals(Type.INT)){
                            intCount++;
                        }
                        else if(arg instanceof ObjectType){
                            objectCount++;
                        }
                    }
                    if(objectCount != 0 || intCount < 3 || intCount > 4){
                        continue;
                    }
                    boolean foundPlaneArray = false;
                    InstructionFinder instructionFinder = new InstructionFinder(new InstructionList(method.getCode().getCode()));
                    for(Iterator<InstructionHandle[]> iterator = instructionFinder.search("GETSTATIC IFNONNULL"); iterator.hasNext();){
                        InstructionHandle[] ih = iterator.next();
                        FieldInstruction fieldInstruction = (FieldInstruction) ih[0].getInstruction();

                        hookHandler.addClientHook(fieldInstruction, cpg, "planeArray", TypeBuilder.createHookArrayType("Plane",1));
                        foundPlaneArray = true;
                    }
                    if(!foundPlaneArray){
                        continue;
                    }

                    for(Iterator<InstructionHandle[]> iterator = instructionFinder.search("getstatic iconst_1 aaload iload aaload iload baload"); iterator.hasNext();){
                        InstructionHandle[] ih = iterator.next();
                        FieldInstruction fieldInstruction = (FieldInstruction) ih[0].getInstruction();

                        hookHandler.addClientHook(fieldInstruction, cpg, "groundSettingsArray");
                    }

                    ClassGen planeCG = hookHandler.getClassByNick("Plane");
                    FieldHook planeArrayHook = hookHandler.getFieldHook("Client", "planeArray");

                    for(Iterator<InstructionHandle[]> iterator = instructionFinder.search("invokevirtual ireturn"); iterator.hasNext();){
                        InstructionHandle[] ih = iterator.next();
                        InvokeInstruction invokeInstruction = (InvokeInstruction) ih[0].getInstruction();
                        if(invokeInstruction.getClassName(cpg).equals(planeCG.getClassName())){
                            if(!invokeInstruction.getReturnType(cpg).equals(Type.INT)){
                                continue;
                            }
                            Type[] params = invokeInstruction.getArgumentTypes(cpg);
                            if(params.length < 2 || params.length > 3){
                                continue;
                            }

                            hookHandler.dataMap.put("Plane::getHeight()", new Object[]{invokeInstruction.getMethodName(cpg), invokeInstruction.getSignature(cpg)});
                        }
                    }
                }
            }
        }
    }

    public String[] getFieldHooks() {
        return new String[]{
                "Client::planeArray",
                "Client::groundSettingsArray",
                
        };  //To change body of implemented methods use File | Settings | File Templates.
    }
}
