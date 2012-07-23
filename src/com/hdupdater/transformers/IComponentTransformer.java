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
import com.hdupdater.hooks.MasterXYHook;
import com.hdupdater.hooks.VirtualFieldGetterHook;
import com.hdupdater.utils.InstructionUtils;
import com.hdupdater.utils.TypeBuilder;
import com.hdupdater.utils.TypeCounter;
import com.sun.org.apache.bcel.internal.classfile.Field;
import com.sun.org.apache.bcel.internal.classfile.Method;
import com.sun.org.apache.bcel.internal.generic.*;
import com.sun.org.apache.bcel.internal.util.InstructionFinder;

import java.awt.*;
import java.util.Iterator;

/**
 * Created by IntelliJ IDEA.
 * User: Jan Ove Saltvedt
 * Date: Sep 26, 2009
 * Time: 6:06:07 PM
 * To change this template use File | Settings | File Templates.
 */
public class IComponentTransformer extends AbstractTransformer {

    @Override
    public Class<?>[] getDependencies() {
        return new Class<?>[]{RendererTransformer.class};
    }

    public void run() {
        ClassGen iCompCG = null;
        ClassGen iCCCG = null;
        Method iCCMethod = null;
        // Lets first get the IComponent class
        for (ClassGen cG : classes) {
            {
                int objectCount = 0;
                for (Field field : cG.getFields()) {
                    if (field.isStatic()) {
                        continue;
                    }
                    if (field.getType().getSignature().equals("[Ljava/lang/Object;")) {
                        objectCount++;
                    }
                }
                if (objectCount > 15) {
                    //Debug.writeLine(""+cG.getClassName());
                    hookHandler.addClassNick("IComponent", cG);
                    iCompCG = cG;
                }
            }

            // Get the IComponentConstructor constructor method
            ConstantPoolGen cpg = cG.getConstantPool();
            if (cpg.lookupUtf8("slow") != -1) {
                iCCCG = cG;
                long lLongest = 0;
                Method mLongest = null;
                for (Method method : cG.getMethods()) {
                    if (!method.isStatic()) {
                        continue;
                    }
                    if (method == null || method.getCode() == null)
                        continue;
                    if (method.getCode().getLength() > lLongest) {
                        lLongest = method.getCode().getLength();
                        mLongest = method;
                    }
                }
                iCCMethod = mLongest;
            }
        }
        if (iCompCG == null) {
            throw new RuntimeException("Could not find IComponent class.");
        } else if (iCCCG == null) {
            throw new RuntimeException("Could not find IComponentConstructor class");
        } else if (iCCMethod == null) {
            throw new RuntimeException("Could not find IComponentConstructor method");
        }

        hookStaticArray(iCompCG);
        hookInICConstructor(iCCCG, iCCMethod);
        hookMainUIIndex(iCCCG);
        hookFromIComp(iCompCG);
        hookBounds(iCompCG);
        hookVisible(iCompCG);
        hookRendered(iCompCG);
        hookMastersAndVisible(iCompCG);
        hookSpecialType(iCompCG);
    }

    private void hookMainUIIndex(ClassGen iCCCG) {
        for(Method method: iCCCG.getMethods()){
            if(!method.isStatic()){
                continue;
            }
            /*if(method.getArgumentTypes().length > 2){
                continue;
            } */
            ConstantPoolGen cpg = iCCCG.getConstantPool();
            InstructionList iList = new InstructionList(method.getCode().getCode());

            InstructionFinder finder = new InstructionFinder(iList);

            InstructionHandle[] block = findBlock(cpg, finder, 6700);
            if (block != null) {
                FieldInstruction fieldInstruction = (FieldInstruction) InstructionUtils.getInstruction("getstatic", block);
                if(fieldInstruction == null || fieldInstruction.getFieldType(cpg) instanceof ObjectType){
                    fieldInstruction = (FieldInstruction) InstructionUtils.getInstruction("getstatic", 2, block);
                }
                if (fieldInstruction != null) {
                    hookHandler.addClientHook(fieldInstruction, cpg, "mainUIInterfaceIndex");
                }
            }
        }
    }

    private void hookRendered(ClassGen iCompCG) {
        ClassGen clientCG = hookHandler.classes.get("client");
        ConstantPoolGen cpg = clientCG.getConstantPool();
        for(Method method: clientCG.getMethods()){
            if(method.isStatic()){
                continue;
            }
            if(method.getArgumentTypes().length > 1){
                continue;
            }
            if(TypeCounter.getObjectCount(method.getArgumentTypes()) != 0){
                continue;
            }

            // Lets find a string here to verify
            boolean foundString = false;
            InstructionFinder instructionFinder = new InstructionFinder(new InstructionList(method.getCode().getCode()));
            for(Iterator<InstructionHandle[]> iterator = instructionFinder.search("ldc"); iterator.hasNext();){
                InstructionHandle[] ih = iterator.next();
                LDC ldc = (LDC) ih[0].getInstruction();
                if(ldc.getValue(cpg).equals("<br>(")){
                    foundString = true;
                }
            }

            if(!foundString){
                continue;
            }
            for(Iterator<InstructionHandle[]> iterator = instructionFinder.search("getstatic iload baload ifeq"); iterator.hasNext();){
                InstructionHandle[] ih = iterator.next();
                FieldInstruction fieldInstruction = (FieldInstruction) ih[0].getInstruction();
                if(!fieldInstruction.getType(cpg).equals(Type.getType(boolean[].class))){
                    continue;
                }
                                
                int getCount = 0;
                for(Iterator<InstructionHandle[]> iterator2 = instructionFinder.search("getstatic"); iterator2.hasNext();){
                    InstructionHandle[] ih2 = iterator2.next();
                    GETSTATIC getstatic = (GETSTATIC) ih2[0].getInstruction();
                    if(getstatic.getClassName(cpg).equals(fieldInstruction.getClassName(cpg)) && getstatic.getFieldName(cpg).equals(fieldInstruction.getFieldName(cpg))){
                        getCount++;
                    }
                }

                if(getCount != 1){
                    continue;
                }
                //hookHandler.addClientHook(fieldInstruction, cpg, "renderedIComponentArray");

            }

        }
    }

    private void hookStaticArray(ClassGen iCompCG) {
        for(ClassGen cG: classes){
            ConstantPoolGen cpg = cG.getConstantPool();
            for(Method method: cG.getMethods()){
                if(!method.isStatic()){
                    continue;
                }
                if(!method.getReturnType().equals(Type.VOID)){
                    continue;
                }
                Type[] args = method.getArgumentTypes();
                if(args.length < 4 || args.length > 5){
                    continue;
                }
                if(TypeCounter.getObjectCount(args) != 4){
                    continue;
                }
                if(TypeCounter.getBasicTypeCount(args) > 1){
                    continue;
                }

                ObjectType objectArgType = null;
                int count = 0;
                for(Type arg: args){
                    if(arg instanceof ObjectType){
                        if(objectArgType == null){
                            objectArgType = (ObjectType) arg;
                            count++;
                        }
                        else if(objectArgType.equals(arg)){
                            count++;
                        }
                    }
                }

                if(count != 4){
                    continue;
                }

                InstructionFinder instructionFinder = new InstructionFinder(new InstructionList(method.getCode().getCode()));
                for(Iterator<InstructionHandle[]> iterator = instructionFinder.search("putstatic"); iterator.hasNext();){
                    InstructionHandle[] ih = iterator.next();
                    FieldInstruction fieldInstruction = (FieldInstruction) ih[0].getInstruction();
                    if(fieldInstruction.getFieldType(cpg).equals(Type.getType("[[L"+iCompCG.getClassName()+";"))){
                        hookHandler.addClientHook(fieldInstruction, cpg, "IComponentArray", TypeBuilder.createHookArrayType("IComponent", 2));
                    }

                }
            }
        }
    }

    private void hookSpecialType(ClassGen iCompCG) {
        for(ClassGen cG: classes){
            ConstantPoolGen cpg = cG.getConstantPool();
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
                if(TypeCounter.getObjectCount(args) != 1){
                    continue;
                }
                if(TypeCounter.getCount(args, Type.getType("L"+iCompCG.getClassName()+";")) != 1){
                    continue;
                }

                InstructionFinder instructionFinder = new InstructionFinder(new InstructionList(method.getCode().getCode()));
                boolean found256D = false;
                boolean found150 = false;
                boolean found40D = false;
                boolean found2047 = false;
                for (Iterator<InstructionHandle[]> iterator = instructionFinder.search("ConstantPushInstruction"); iterator.hasNext();) {
                    InstructionHandle[] ih = iterator.next();
                    ConstantPushInstruction pushInstruction = (ConstantPushInstruction) ih[0].getInstruction();
                    if(pushInstruction.getValue().intValue() == 150){
                        found150 = true;
                    }
                    else if(pushInstruction.getValue().intValue() == 2047){
                        found2047 = true;
                    }
                }

                for (Iterator<InstructionHandle[]> iterator = instructionFinder.search("ldc2_w"); iterator.hasNext();) {
                    InstructionHandle[] ih = iterator.next();
                    LDC2_W ldc = (LDC2_W) ih[0].getInstruction();
                    if(ldc.getValue(cpg).equals(256D)){
                        found256D = true;
                    }
                    else if(ldc.getValue(cpg).equals(40D)){
                        found40D = true;
                    }
                }
                if(!found150 || !found256D || !found40D || !found2047){
                    continue;
                }

                for (Iterator<InstructionHandle[]> iterator = instructionFinder.search("((getstatic aload getfield)|(aload getfield getstatic)) IF_ICMPNE"); iterator.hasNext();) {
                    InstructionHandle[] ih = iterator.next();
                    FieldInstruction fieldInstruction = null;
                    for(InstructionHandle handle: ih){
                        if(handle.getInstruction() instanceof GETFIELD){
                            fieldInstruction = (FieldInstruction) handle.getInstruction();
                        }
                    }

                    if(fieldInstruction.getClassName(cpg).equals(iCompCG.getClassName()) && fieldInstruction.getFieldType(cpg).equals(Type.INT)){
                        hookHandler.addFieldHook("IComponent", fieldInstruction, cpg, "specialType");
                    }
                }
            }
        }
    }


    private void hookVisible(ClassGen iCompCG) {
        FieldHook fieldHook = hookHandler.getFieldHook("IComponent", "visibleArrayIndex");
        for (ClassGen cG : classes) {
            ConstantPoolGen cpg = cG.getConstantPool();
            if(cpg.lookupFieldref(fieldHook.getClassName(), fieldHook.getFieldName(), fieldHook.getFieldType().getSignature()) == -1){
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
                if(TypeCounter.getCount(args, Type.getType("L"+iCompCG.getClassName()+";")) != 1){
                    continue;
                }

                InstructionFinder instructionFinder = new InstructionFinder(new InstructionList(method.getCode().getCode()));
                for (Iterator<InstructionHandle[]> iterator = instructionFinder.search("getstatic aload getfield"); iterator.hasNext();) {
                    InstructionHandle[] ih = iterator.next();
                    FieldInstruction staticFieldInstruction = (FieldInstruction) ih[0].getInstruction();
                    FieldInstruction fieldInstruction = (FieldInstruction) ih[2].getInstruction();
                    if(fieldInstruction.getClassName(cpg).equals(fieldHook.getClassName())
                            && fieldInstruction.getFieldName(cpg).equals(fieldHook.getFieldName())
                            && staticFieldInstruction.getFieldType(cpg).equals(Type.getType(boolean[].class))){
                        hookHandler.addClientHook(staticFieldInstruction, cpg, "visibleIComponents");
                    }
                }

            }
        }
    }

    private void hookBounds(ClassGen iCompCG) {
        for (ClassGen cG : classes) {
            ConstantPoolGen cpg = cG.getConstantPool();
            if (cpg.lookupUtf8("Offheap:") == -1) {
                continue;
            }
            for (Method method : cG.getMethods()) {
                if(!method.isStatic()){
                    continue;
                }
                boolean foundString = false;
                InstructionFinder instructionFinder = new InstructionFinder(new InstructionList(method.getCode().getCode()));
                for (Iterator<InstructionHandle[]> iterator = instructionFinder.search("ldc"); iterator.hasNext();) {
                    InstructionHandle[] ih = iterator.next();
                    LDC ldc = (LDC) ih[0].getInstruction();
                    if (ldc.getValue(cpg).equals("Offheap:")) {
                        foundString = true;
                        break;
                    }
                }

                if (!foundString) {
                    continue;
                }

                for (Iterator<InstructionHandle[]> iterator = instructionFinder.search("getstatic getstatic aaload"); iterator.hasNext();) {
                    InstructionHandle[] ih = iterator.next();
                    FieldInstruction arrayFieldInstruction = (FieldInstruction) ih[0].getInstruction();
                    if (!arrayFieldInstruction.getType(cpg).equals(Type.getType(Rectangle[].class))) {
                        continue;
                    }

                    hookHandler.addClientHook(arrayFieldInstruction, cpg, "interfaceBounds");

                    FieldInstruction staticFieldInstruction = (FieldInstruction) ih[1].getInstruction();
                    String fieldClassName = staticFieldInstruction.getClassName(cpg);
                    String fieldName = staticFieldInstruction.getFieldName(cpg);

                    int localIndex = -1;

                    for (Iterator<InstructionHandle[]> iterator2 = instructionFinder.search("istore", ih[2]); iterator2.hasNext();) {
                        InstructionHandle[] ih2 = iterator2.next();
                        InstructionHandle[] param = InstructionUtils.getParameters(ih2[0], cpg, 1)[0];
                        for (InstructionHandle instructionHandle : param) {
                            if (instructionHandle.getInstruction() instanceof GETSTATIC) {
                                FieldInstruction fieldInstruction = (FieldInstruction) instructionHandle.getInstruction();
                                if (fieldInstruction.getClassName(cpg).equals(fieldClassName) && fieldInstruction.getFieldName(cpg).equals(fieldName)) {
                                    ISTORE istore = (ISTORE) ih2[0].getInstruction();
                                    localIndex = istore.getIndex();
                                }
                            }
                        }

                    }

                    if (localIndex == -1) {
                        continue;
                    }

                    for (Iterator<InstructionHandle[]> iterator2 = instructionFinder.search("iload putfield", ih[2]); iterator2.hasNext();) {
                        InstructionHandle[] ih2 = iterator2.next();

                        ILOAD iload = (ILOAD) ih2[0].getInstruction();
                        if (iload.getIndex() == localIndex) {
                            FieldInstruction fieldInstruction = (FieldInstruction) ih2[1].getInstruction();
                            if (fieldInstruction.getClassName(cpg).equals(iCompCG.getClassName())) {
                                hookHandler.addFieldHook("IComponent", fieldInstruction, cpg, "visibleArrayIndex");
                            }
                        }
                    }


                }


            }
        }
    }

    private void hookFromIComp(ClassGen iCompCG) {
        ConstantPoolGen cpg = iCompCG.getConstantPool();
        ClassGen rendererCG = hookHandler.getClassByNick("Renderer");
        for (Field field : iCompCG.getFields()) {
            if (!field.isStatic()) {
                // getCache
                if (field.getType().getSignature().equals("[L" + iCompCG.getClassName() + ";")) {
                    hookHandler.addFieldHook("IComponent", iCompCG, field, "children", TypeBuilder.createHookArrayType("IComponent", 1));
                }
            }
        }
    }

    private void hookInICConstructor(ClassGen iCCCG, Method iCCMethod) {
        ConstantPoolGen cpg = iCCCG.getConstantPool();
        InstructionList iList = new InstructionList(iCCMethod.getCode().getCode());

        InstructionFinder finder = new InstructionFinder(iList);

        InstructionHandle[] block = findBlock(cpg, finder, 2700);
        if (block != null) {
            FieldInstruction fieldInstruction = (FieldInstruction) InstructionUtils.getInstruction("getfield", block);
            if (fieldInstruction != null) {
                hookHandler.addFieldHook("IComponent", fieldInstruction, cpg, "elementID");
            }
        }

        block = findBlock(cpg, finder, 2701);
        if (block != null) {
            FieldInstruction fieldInstruction = (FieldInstruction) InstructionUtils.getInstruction("getfield", 2, block);
            if (fieldInstruction != null) {
                hookHandler.addFieldHook("IComponent", fieldInstruction, cpg, "elementStackSize");
            }
        }

        block = findBlock(cpg, finder, 1305);
        if (block != null) {
            FieldInstruction fieldInstruction = (FieldInstruction) InstructionUtils.getInstruction("putfield", block);
            if (fieldInstruction != null) {
                hookHandler.addFieldHook("IComponent", fieldInstruction, cpg, "elementName");
            }
        }

        block = findBlock(cpg, finder, 2500);
        if (block != null) {
            FieldInstruction fieldInstruction = (FieldInstruction) InstructionUtils.getInstruction("getfield", block);
            if (fieldInstruction != null) {
                hookHandler.addFieldHook("IComponent", fieldInstruction, cpg, "x");
            }
        }

        block = findBlock(cpg, finder, 2501);
        if (block != null) {
            FieldInstruction fieldInstruction = (FieldInstruction) InstructionUtils.getInstruction("getfield", block);
            if (fieldInstruction != null) {
                hookHandler.addFieldHook("IComponent", fieldInstruction, cpg, "y");
            }
        }

        block = findBlock(cpg, finder, 1001);
        if (block != null) {
            FieldInstruction fieldInstruction = (FieldInstruction) InstructionUtils.getInstruction("putfield", block);
            if (fieldInstruction != null) {
                hookHandler.addFieldHook("IComponent", fieldInstruction, cpg, "minWidth");
            }
            fieldInstruction = (FieldInstruction) InstructionUtils.getInstruction("putfield", 2, block);
            if (fieldInstruction != null) {
                hookHandler.addFieldHook("IComponent", fieldInstruction, cpg, "minHeight");
            }
        }

        block = findBlock(cpg, finder, 1502);
        if (block != null) {
            FieldInstruction fieldInstruction = (FieldInstruction) InstructionUtils.getInstruction("getfield", block);
            if (fieldInstruction != null) {
                hookHandler.addFieldHook("IComponent", fieldInstruction, cpg, "width");
            }
        }

        block = findBlock(cpg, finder, 1503);
        if (block != null) {
            FieldInstruction fieldInstruction = (FieldInstruction) InstructionUtils.getInstruction("getfield", block);
            if (fieldInstruction != null) {
                hookHandler.addFieldHook("IComponent", fieldInstruction, cpg, "height");
            }
        }

        block = findBlock(cpg, finder, 1307);
        if (block != null) {
            FieldInstruction fieldInstruction = (FieldInstruction) InstructionUtils.getInstruction("putfield", block);
            if (fieldInstruction != null) {
                hookHandler.addFieldHook("IComponent", fieldInstruction, cpg, "actions");
            }
        }

        block = findBlock(cpg, finder, 1602);
        if (block != null) {
            FieldInstruction fieldInstruction = (FieldInstruction) InstructionUtils.getInstruction("getfield", block);
            if (fieldInstruction != null) {
                hookHandler.addFieldHook("IComponent", fieldInstruction, cpg, "text");
            }
        }

        block = findBlock(cpg, finder, 1105);
        if (block != null) {
            FieldInstruction fieldInstruction = (FieldInstruction) InstructionUtils.getInstruction("putfield", block);
            if (fieldInstruction != null) {
                hookHandler.addFieldHook("IComponent", fieldInstruction, cpg, "textureID");
            }
        }

        block = findBlock(cpg, finder, 1101);
        if (block != null) {
            FieldInstruction fieldInstruction = (FieldInstruction) InstructionUtils.getInstruction("putfield", block);
            if (fieldInstruction != null) {
                hookHandler.addFieldHook("IComponent", fieldInstruction, cpg, "textColor");
            }
        }

        block = findBlock(cpg, finder, 1123);
        if (block != null) {
            FieldInstruction fieldInstruction = (FieldInstruction) InstructionUtils.getInstruction("putfield", block);
            if (fieldInstruction != null) {
                hookHandler.addFieldHook("IComponent", fieldInstruction, cpg, "modelZoom");
            }
        }


        block = findBlock(cpg, finder, 1101);
        if (block != null) {
            FieldInstruction fieldInstruction = (FieldInstruction) InstructionUtils.getInstruction("getfield",2, block);
            if (fieldInstruction != null) {
                hookHandler.addFieldHook("IComponent", fieldInstruction, cpg, "UID");
            }
        }

        block = findBlock(cpg, finder, 1108);
        if (block != null) {
            FieldInstruction fieldInstruction = (FieldInstruction) InstructionUtils.getInstruction("iaload putfield",1, block,1);
            if (fieldInstruction != null) {
                hookHandler.addFieldHook("IComponent", fieldInstruction, cpg, "modelID");
            }
            fieldInstruction = (FieldInstruction) InstructionUtils.getInstruction("ConstantPushInstruction putfield",1, block,1);
            if (fieldInstruction != null) {
                hookHandler.addFieldHook("IComponent", fieldInstruction, cpg, "modelType");
            }
        }

        block = findBlock(cpg, finder, 1003);
        if (block != null) {
            FieldInstruction fieldInstruction = (FieldInstruction) InstructionUtils.getInstruction("aload getfield",1, block, 1);
            if (fieldInstruction != null) {
                hookHandler.addFieldHook("IComponent", fieldInstruction, cpg, "elementVisible");
            }
        }

        /*
        if(i1 == 5426)
                    {
                        j -= 2;
                        Class181_Sub1_Sub39.anInt6368 = anIntArray3270[j];
                        Class181_Sub32_Sub1_Sub2.anInt6894 = anIntArray3270[j + 1];
                        continue;
                    }

         */

        /*
        Wrong!

        block = findBlock(cpg, finder, 100);
        if (block != null) {
            InstructionList instructionList = new InstructionList();
            for(InstructionHandle handle: block){
                if(handle.getInstruction() instanceof BranchInstruction){
                    instructionList.append((BranchInstruction)handle.getInstruction());
                }
                else{
                    instructionList.append(handle.getInstruction());
                }
            }

            int localIndex = -1;

            InstructionFinder instructionFinder = new InstructionFinder(instructionList);
            for (Iterator<InstructionHandle[]> it = instructionFinder.search("iaload istore"); it.hasNext();) {
                InstructionHandle[] ih = it.next();
                InstructionHandle[][] params = InstructionUtils.getParameters(ih[0], cpg, 1);
                for(InstructionHandle handle: params[0]){
                    if(handle.getInstruction() instanceof ConstantPushInstruction){
                        ConstantPushInstruction pushInstruction = (ConstantPushInstruction) handle.getInstruction();
                        if(pushInstruction.getValue().intValue() == 2 || pushInstruction.getValue().intValue() == -2){
                            localIndex = ((ISTORE)ih[1].getInstruction()).getIndex();
                        }
                    }
                }
            }
            if(localIndex != -1){
                for (Iterator<InstructionHandle[]> it = instructionFinder.search("aload iload putfield"); it.hasNext();) {
                    InstructionHandle[] ih = it.next();
                    ILOAD iload = (ILOAD) ih[1].getInstruction();
                    if(iload.getIndex() == localIndex){
                        FieldInstruction fieldInstruction = (FieldInstruction) ih[2].getInstruction();
                        hookHandler.addFieldHook("IComponent", fieldInstruction, cpg, "specialType");
                    }
                }
            }
        }
        */
    }

    private InstructionHandle[] findBlock(ConstantPoolGen cpg, InstructionFinder instructionFinder, final int opCode) {
        for (Iterator<InstructionHandle[]> iterator = instructionFinder.search("((((ConstantPushInstruction)|(ldc)) iload)|(iload((ConstantPushInstruction)|(ldc)))) if_icmpne"); iterator.hasNext();) {
            InstructionHandle[] ih = iterator.next();
            for (InstructionHandle i : ih) {
                if (i.getInstruction() instanceof LDC) {
                    LDC ldc = (LDC) i.getInstruction();
                    if (ldc.getValue(cpg) instanceof Integer) {
                        if (((Integer) ldc.getValue(cpg)) == opCode) {
                            return InstructionUtils.getBranchBlock(ih[ih.length - 1]);
                        }
                    }
                } else if (i.getInstruction() instanceof ConstantPushInstruction) {
                    ConstantPushInstruction ins = (ConstantPushInstruction) i.getInstruction();
                    if (ins.getValue().intValue() == opCode) {
                        return InstructionUtils.getBranchBlock(ih[ih.length - 1]);
                    }
                }
            }

        }
        return null;
    }

    private void hookMastersAndVisible(ClassGen ICompCG){
        hookHandler.injectField(ICompCG, "masterX",Type.INT, 0);
        hookHandler.injectField(ICompCG, "masterY",Type.INT, 0);
        //hookHandler.injectField(ICompCG, "visibleLoopCycleStatus",Type.INT, 0);
        FieldHook getXHook = hookHandler.getFieldHook("IComponent", "x");
        FieldHook getYHook = hookHandler.getFieldHook("IComponent", "y");
        //FieldHook getRenderedArrayHook = hookHandler.getFieldHook("Client", "renderedIComponentArray");
        FieldHook getLoopCycle = hookHandler.getFieldHook("Client", "loopCycle");
        new VirtualFieldGetterHook(hookHandler, ICompCG, "masterX", Type.INT);
        new VirtualFieldGetterHook(hookHandler, ICompCG, "masterY", Type.INT);
        //new VirtualFieldGetterHook(hookHandler, ICompCG, "visibleLoopCycleStatus", Type.INT);

        for(ClassGen cG: normalClasses){
            ConstantPoolGen cPool = cG.getConstantPool();
            if(cPool.lookupUtf8("Fps:") == -1)
                continue;
            if(cPool.lookupUtf8("Cache:") == -1)
                continue;
            for(Method m: cG.getMethods()){
                if(!m.isStatic())
                    continue;
                Type[] args =  m.getArgumentTypes();
                if(args.length < 9 || args.length > 11)
                    continue;
                int intCount = 0;
                int objArrayCount = 0;
                for(Type arg: args){
                    if(arg instanceof ArrayType){
                        if(arg.getSignature().equals("[L"+ICompCG.getClassName()+";")){
                            objArrayCount++;
                        }
                    }
                    else if(arg == Type.INT || arg == Type.BYTE){
                        intCount++;
                    }
                }
                if(intCount < 8 || intCount > 10 || objArrayCount != 1)
                    continue;
                //Debug.writeLine("Found in "+cG.getClassName()+" "+m.toString());
                // Dirty hack!

                MethodGen methodGen = new MethodGen(m, cG.getClassName(), cPool);
                InstructionList mInstructionList = methodGen.getInstructionList();
                InstructionList tempInstructionList = new InstructionList();
                InstructionFactory iFac = new InstructionFactory(cG, cPool);
                InstructionHandle tempHandle = null;
                String pattern = "(ILOAD)?(ALOAD)(FieldInstruction)(ILOAD)?(ArithmeticInstruction)";
                InstructionFinder instructionFinder = new InstructionFinder(mInstructionList);
                int hitX = 0;
                int hitY = 0;

                int iloadXIndex = -1;
                int iloadYIndex = -1;
                int aloadIndex = -1;
                boolean first = true;
                for (Iterator<InstructionHandle[]> it = instructionFinder.search(pattern); it.hasNext();) {
                    InstructionHandle[] ih = it.next();
                    InstructionHandle iload = null;
                    InstructionHandle aload = null;
                    FieldInstruction fieldInstruction = null;
                    InstructionHandle fieldHandle = null;
                    for(InstructionHandle i: ih){
                        if(i.getInstruction() instanceof ILOAD){
                            iload = i;
                        }
                        else if(i.getInstruction() instanceof ALOAD){
                            aload = i;
                        }
                        else if(i.getInstruction() instanceof FieldInstruction){
                            fieldInstruction = (FieldInstruction) i.getInstruction();
                            fieldHandle = i;
                        }
                    }
                    if(iload == null)
                        continue;
                    if(!fieldInstruction.getClassName(cPool).equals(ICompCG.getClassName())){
                        continue;
                    }
                    //Debug.writeLine(""+fieldInstruction.getClassName(cPool)+"."+fieldInstruction.getFieldName(cPool)+" :"+fieldHandle.getPosition());

                    if(fieldInstruction.getFieldName(cPool).equals(getXHook.getFieldName())){
                        hitX++;
                        if(hitX == 1)
                            continue;
                        //Debug.writeLine(""+fieldInstruction.getClassName(cPool)+"."+fieldInstruction.getFieldName(cPool)+" :"+fieldHandle.getPosition());
                        aloadIndex = ((ALOAD)aload.getInstruction()).getIndex();
                        iloadXIndex = ((ILOAD)iload.getInstruction()).getIndex();

                        /*tempInstructionList.append(new ILOAD(((ILOAD)iload.getInstruction()).getIndex()));
                        tempInstructionList.append(iFac.createPutField(interfaceCG.getClassName(), "masterX", Type.INT));
                        tempInstructionList.append(new ALOAD(((ALOAD)aload.getInstruction()).getIndex()));
                        */
                        if(first){
                            tempHandle = fieldHandle.getNext();
                            while(!(tempHandle.getInstruction() instanceof ALOAD))
                                tempHandle = tempHandle.getNext();
                            first = false;
                        }
                        //new TextInfoHook("\t- Modified method to put masterX");
                    }
                    else if(fieldInstruction.getFieldName(cPool).equals(getYHook.getFieldName())){
                        hitY++;
                        if(hitY == 1)
                            continue;
                        //Debug.writeLine(""+fieldInstruction.getClassName(cPool)+"."+fieldInstruction.getFieldName(cPool)+" :"+fieldHandle.getPosition());
                        aloadIndex = ((ALOAD)aload.getInstruction()).getIndex();
                        iloadYIndex = ((ILOAD)iload.getInstruction()).getIndex();

                        /*tempInstructionList.append(new ILOAD(((ILOAD)iload.getInstruction()).getIndex()));
                        tempInstructionList.append(iFac.createPutField(interfaceCG.getClassName(), "masterY", Type.INT));
                        tempInstructionList.append(new ALOAD(((ALOAD)aload.getInstruction()).getIndex()));    */

                        if(first){
                            tempHandle = fieldHandle.getNext();
                            while(!(tempHandle.getInstruction() instanceof ALOAD))
                                tempHandle = tempHandle.getNext();
                            first = false;
                        }
                        //new TextInfoHook("\t- Modified method to put masterY");
                    }
                }
                new MasterXYHook(hookHandler, cG, m, ICompCG, aloadIndex, iloadXIndex, iloadYIndex, tempHandle.getPosition());
                System.out.println("Injected MASTER-X-Y Hook");

                for (Iterator<InstructionHandle[]> it = instructionFinder.search("getstatic putfield"); it.hasNext();) {
                    InstructionHandle[] ih = it.next();
                    FieldInstruction staticFI = (FieldInstruction) ih[0].getInstruction();
                    if(staticFI.getClassName(cPool).equals(getLoopCycle.getClassName()) && staticFI.getFieldName(cPool).equals(getLoopCycle.getFieldName())){
                        FieldInstruction fieldInstruction = (FieldInstruction) ih[1].getInstruction();
                        if(fieldInstruction.getClassName(cPool).equals(ICompCG.getClassName())){
                            hookHandler.addFieldHook("IComponent", fieldInstruction, cPool, "loopCycleStatus");
                        }
                    }

                }
                //new InjectIComponentVisibleSetterHook(hookHandler, cG, m, getRenderedArrayHook.getClassName(), getRenderedArrayHook.getFieldName(), aloadIndex, ICompCG, getLoopCycle.getClassName(), getLoopCycle.getFieldName());
                //System.out.println("Injected Visible-Hook");
            }
        }
    }

    public String[] getFieldHooks() {
        return new String[]{
                "IComponent::elementID",
                "IComponent::elementStackSize",
                "IComponent::elementName",
                "IComponent::x",
                "IComponent::y",
                "IComponent::width",
                "IComponent::height",
                "IComponent::minWidth",
                "IComponent::minHeight",
                "IComponent::actions",
                "IComponent::text",
                "IComponent::textureID",
                "IComponent::textColor",
                "IComponent::modelZoom",
                "IComponent::modelID",
                "IComponent::modelType",
                "IComponent::children",
                "IComponent::visibleArrayIndex",
                "IComponent::specialType",
                "IComponent::UID",
                "IComponent::loopCycleStatus",
                "Client::interfaceBounds",
                "Client::visibleIComponents",
                "Client::IComponentArray",
                "IComponent::elementVisible",
                //"Client::renderedIComponentArray",

        };  //To change body of implemented methods use File | Settings | File Templates.
    }
}
