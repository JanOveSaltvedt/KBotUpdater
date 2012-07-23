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
import com.hdupdater.utils.InstructionUtils;
import com.hdupdater.utils.TypeBuilder;
import com.hdupdater.utils.TypeCounter;
import com.sun.org.apache.bcel.internal.classfile.Field;
import com.sun.org.apache.bcel.internal.classfile.Method;
import com.sun.org.apache.bcel.internal.generic.*;
import com.sun.org.apache.bcel.internal.util.InstructionFinder;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: Jan Ove / Kosaki
 * Date: 18.jul.2009
 * Time: 19:04:56
 */
public class CharacterTransformer extends AbstractTransformer {
    public void run() {
        for (ClassGen cG : classes) {
            final ConstantPoolGen cpg = cG.getConstantPool();
            if (cpg.lookupUtf8("T2 - ") == -1) {
                continue;
            }
            for (Method method : cG.getMethods()) {
                if (!method.isStatic())
                    continue;
                if (!method.getReturnType().equals(Type.VOID) && !method.getReturnType().equals(Type.BOOLEAN))
                    continue;
                final Type[] args = method.getArgumentTypes();
                if (args.length > 2 || args.length < 1)
                    continue;
                MethodGen methodGen = new MethodGen(method, cG.getClassName(), cpg);
                InstructionList iList = methodGen.getInstructionList();
                InstructionFinder instructionFinder = new InstructionFinder(iList);

                InstructionFinder.CodeConstraint codeConstraint = new InstructionFinder.CodeConstraint() {
                    public boolean checkCode(InstructionHandle[] match) {
                        LDC ldc = (LDC) match[0].getInstruction();
                        return ldc.getValue(cpg).equals("T2 - ");
                    }
                };
                for (Iterator<InstructionHandle[]> iterator = instructionFinder.search("ldc", codeConstraint); iterator.hasNext();) {
                    InstructionHandle[] ih = iterator.next();
                    //System.out.println("Found");

                    int i = 0;
                    boolean hookedMyPlayer = false;
                    for (Iterator<InstructionHandle[]> it = instructionFinder.search("(FieldInstruction)+ ConstantPushInstruction ArrayInstruction (FieldInstruction)? (ArithmeticInstruction)+"); it.hasNext();) {
                        InstructionHandle[] ihs = it.next();
                        for (InstructionHandle instruction : ihs) {
                            if (instruction.getInstruction() instanceof FieldInstruction) {
                                FieldInstruction fieldInstruction = ((FieldInstruction) instruction.getInstruction());
                                Type fieldType = fieldInstruction.getFieldType(cpg);
                                if (fieldType instanceof ObjectType) {
                                    if (!hookedMyPlayer) {
                                        hookHandler.addClientHook(fieldInstruction, cpg, "myPlayer", TypeBuilder.createHookType("Player"));
                                        hookHandler.addClassNick("Player", fieldType.toString());
                                        hookedMyPlayer = true;
                                    }
                                }
                                if (fieldType.getSignature().equals("[I")) {
                                    if (hookHandler.getClassByNick("Character") == null) {
                                        ClassGen charCG = hookHandler.getFieldsClass(fieldInstruction.getClassName(cpg), fieldInstruction.getFieldName(cpg));
                                        hookHandler.addClassNick("Character", charCG);
                                    }
                                    hookHandler.addFieldHook("Character", fieldInstruction, cpg, "walking" + (i == 0 ? "X" : "Y"));
                                } else if (fieldType == Type.INT) {
                                    hookHandler.addClientHook(fieldInstruction, cpg, "base" + (i == 0 ? "X" : "Y"));
                                }
                            }
                        }
                        i++;
                    }
                }
            }
        }
        hookFromCharacter();
        hookFromClasses();
        hookLoopCycle();
        hookDestination();
        hookHitArrays();
        hookUID();

    }

    private void hookUID() {
        ClassGen charCG = hookHandler.getClassByNick("Character");
        for (ClassGen cG : classes) {
            ConstantPoolGen cpg = cG.getConstantPool();
            if (cpg.lookupUtf8(" -> <col=ffffff>") == -1) {
                continue;
            }
            for (Method method : cG.getMethods()) {
                if (!method.isStatic()) {
                    continue;
                }
                if (!method.getReturnType().equals(Type.VOID)) {
                    continue;
                }
                if (method.getArgumentTypes().length < 2 || method.getArgumentTypes().length > 3) {
                    continue;
                }
                if(TypeCounter.getObjectCount(method.getArgumentTypes()) != 1){
                    continue;
                }
                InstructionFinder instructionFinder = new InstructionFinder(new InstructionList(method.getCode().getCode()));
                for (Iterator<InstructionHandle[]> it = instructionFinder.search("ldc"); it.hasNext();) {
                    InstructionHandle[] ih = it.next();
                    LDC ldc = (LDC) ih[0].getInstruction();
                    if(!ldc.getValue(cpg).equals(" -> <col=ffffff>")){
                        continue;  
                    }
                    InstructionHandle curHandle = ih[0].getNext();
                    while(curHandle != null && !(curHandle.getInstruction() instanceof InvokeInstruction)){
                        curHandle = curHandle.getNext();
                    }
                    curHandle = curHandle.getPrev();
                    while(curHandle != null){
                        if(curHandle.getInstruction() instanceof GETFIELD){
                            FieldInstruction fieldInstruction = (FieldInstruction) curHandle.getInstruction();
                            if(fieldInstruction.getClassName(cpg).equals(charCG.getClassName())){
                                hookHandler.addFieldHook("Character", fieldInstruction, cpg, "UID");
                            }
                        }
                        curHandle = curHandle.getPrev();
                    }

                }

            }
        }
    }

    private void hookHitArrays() {
        ClassGen cG = hookHandler.getClassByNick("Character");
        ConstantPoolGen cpg = cG.getConstantPool();

        for (Method method : cG.getMethods()) {
            if (method.isStatic()) {
                continue;
            }
            if (!method.getReturnType().equals(Type.VOID)) {
                continue;
            }
            final Type[] types = method.getArgumentTypes();
            if (types.length < 3 || types.length > 4) {
                continue;
            }
            if (TypeCounter.getObjectCount(types) != 0) {
                continue;
            }

            InstructionFinder instructionFinder = new InstructionFinder(new InstructionList(method.getCode().getCode()));
            for (Iterator<InstructionHandle[]> it = instructionFinder.search("IfInstruction"); it.hasNext();) {
                InstructionHandle[] ih = it.next();
                InstructionHandle[][] parameters = InstructionUtils.getParameters(ih[0], cpg, 1);
                boolean found = false;
                for (InstructionHandle[] param : parameters) {
                    for (InstructionHandle handle : param) {
                        if (handle.getInstruction() instanceof GETFIELD) {
                            FieldInstruction fieldInstruction = (FieldInstruction) handle.getInstruction();
                            if (fieldInstruction.getType(cpg).equals(Type.getType(int[].class))) {
                                hookHandler.addFieldHook("Character", fieldInstruction, cpg, "hitLoopCycleArray");
                            }
                        }
                    }

                }
            }


        }
    }

    private void hookDestination() {
        FieldHook myPlayerHook = hookHandler.getFieldHook("Client", "myPlayer");
        ClassGen playerCG = hookHandler.getClassByNick("Player");
        for (ClassGen cG : classes) {
            ConstantPoolGen cpg = cG.getConstantPool();
            if (cpg.lookupFieldref(myPlayerHook.getClassName(), myPlayerHook.getFieldName(), myPlayerHook.getFieldType().getSignature()) == -1) {
                continue;
            }

            for (Method method : cG.getMethods()) {
                if (!method.isStatic()) {
                    continue;
                }
                if (!method.getReturnType()
                        .equals(Type.BOOLEAN)) {
                    continue;
                }

                final Type[] types = method.getArgumentTypes();
                if (types.length < 8 || types.length > 9) {
                    continue;
                }

                if (TypeCounter.getObjectCount(types) != 0) {
                    continue;
                }

                int walkingArrayHitCount = 0;
                InstructionFinder instructionFinder = new InstructionFinder(new InstructionList(method.getCode().getCode()));
                for (Iterator<InstructionHandle[]> it = instructionFinder.search("getstatic getfield (iconst_0|iload) iaload istore"); it.hasNext();) {
                    InstructionHandle[] ih = it.next();
                    FieldInstruction staticFieldInstruction = (FieldInstruction) ih[0].getInstruction();
                    if (!staticFieldInstruction.getClassName(cpg).equals(myPlayerHook.getClassName()) || !staticFieldInstruction.getFieldName(cpg).equals(myPlayerHook.getFieldName())) {
                        continue;
                    }

                    FieldInstruction fieldInstruction = (FieldInstruction) ih[1].getInstruction();
                    if (!fieldInstruction.getFieldType(cpg).equals(Type.getType(int[].class))) {
                        continue;
                    }
                    walkingArrayHitCount++;
                }

                if (walkingArrayHitCount != 2) {
                    continue;
                }

                int hitCount = 0;
                for (Iterator<InstructionHandle[]> it = instructionFinder.search("getstatic (((iload iconst)|(iconst iload)|(iload iload)) (ineg)? ArithmeticInstruction iaload putstatic) "); it.hasNext();) {
                    InstructionHandle[] ih = it.next();
                    FieldInstruction fieldInstruction = (FieldInstruction) ih[ih.length - 1].getInstruction();
                    if (hitCount == 0) {
                        hookHandler.addClientHook(fieldInstruction, cpg, "destX");
                    } else if (hitCount == 1) {
                        hookHandler.addClientHook(fieldInstruction, cpg, "destY");
                    }
                    hitCount++;
                }

                for (Iterator<InstructionHandle[]> it = instructionFinder.search("iconst_0 putstatic"); it.hasNext();) {
                    InstructionHandle[] ih = it.next();
                    FieldInstruction fieldInstruction = (FieldInstruction) ih[1].getInstruction();
                    hookHandler.addClientHook(fieldInstruction, cpg, "destSet");
                }
            }
        }
    }

    public void hookFromClasses() {
        ClassGen charCG = hookHandler.getClassByNick("Character");
        ClassGen playerCG = hookHandler.getClassByNick("Player");
        Type arrayType = Type.getType("[L" + playerCG.getClassName() + ";");
        for (ClassGen cG : classes) {
            ConstantPoolGen cpg = cG.getConstantPool();
            for (Method method : cG.getMethods()) {
                if (!method.isStatic()) {
                    continue;
                }
                Type[] args = method.getArgumentTypes();
                if (args.length < 1 || args.length > 3) {
                    continue;
                }
                if (TypeCounter.getCount(args, Type.getType("L" + charCG.getClassName() + ";")) != 1) {
                    continue;
                }

                InstructionFinder instructionFinder = new InstructionFinder(new InstructionList(method.getCode().getCode()));
                for (Iterator<InstructionHandle[]> iterator = instructionFinder.search("getstatic ((aload getfield ldc)|(ldc aload getfield)) (ArithmeticInstruction)+ aaload astore"); iterator.hasNext();) {
                    InstructionHandle[] ih = iterator.next();
                    FieldInstruction fieldInstructionStatic = (FieldInstruction) ih[0].getInstruction();

                    if (!fieldInstructionStatic.getFieldType(cpg).equals(arrayType)) {
                        continue;
                    }


                    FieldInstruction getFieldInstruction = null;
                    for (int i = 1; i < ih.length; i++) {
                        if (ih[i].getInstruction() instanceof GETFIELD) {
                            getFieldInstruction = (FieldInstruction) ih[i].getInstruction();
                            break;
                        }
                    }
                    if (getFieldInstruction.getClassName(cpg).equals(charCG.getClassName())) {
                        hookHandler.addFieldHook("Character", getFieldInstruction, cpg, "interactingCharacterIndex");
                    }
                }
            }
            for (Method method : cG.getMethods()) {
                if (!method.isStatic()) {
                    continue;
                }
                Type[] args = method.getArgumentTypes();
                if (args.length < 1 || args.length > 3) {
                    continue;
                }
                if (TypeCounter.getCount(args, Type.getType("L" + charCG.getClassName() + ";")) != 1) {
                    continue;
                }

                InstructionFinder instructionFinder = new InstructionFinder(new InstructionList(method.getCode().getCode()));
                for (Iterator<InstructionHandle[]> iterator = instructionFinder.search("IfInstruction"); iterator.hasNext();) {
                    InstructionHandle[] ih = iterator.next();
                    InstructionHandle[][] parameters = InstructionUtils.getParameters(ih[0], cpg, 1);
                    if (parameters.length != 2) {
                        continue;
                    }
                    // aload getfield aload getfield iaload
                    if (parameters[0].length == 5) {
                        boolean found1 = false;
                        for (InstructionHandle instructionHandle : parameters[1]) {
                            if (instructionHandle.getInstruction() instanceof ConstantPushInstruction) {
                                ConstantPushInstruction pushInstruction = (ConstantPushInstruction) instructionHandle.getInstruction();
                                int value = pushInstruction.getValue().intValue();
                                if (value == 1 || value == -1) {
                                    found1 = true;
                                }
                            }
                        }
                        if (found1) {
                            InstructionList iList = new InstructionList();

                            for (InstructionHandle instructionHandle : parameters[0]) {
                                if (instructionHandle.getInstruction() instanceof BranchInstruction) {
                                    iList.append((BranchInstruction) instructionHandle.getInstruction());
                                } else {
                                    iList.append(instructionHandle.getInstruction());
                                }
                            }

                            InstructionFinder instructionFinder2 = new InstructionFinder(iList);
                            for (Iterator<InstructionHandle[]> iterator2 = instructionFinder2.search("aload getfield aload getfield iaload"); iterator2.hasNext();) {
                                InstructionHandle[] ih2 = iterator2.next();
                                FieldInstruction arrayFieldInstruction = (FieldInstruction) ih2[1].getInstruction();

                                if (!arrayFieldInstruction.getFieldType(cpg).equals(Type.getType(int[].class))) {
                                    continue;
                                }

                                FieldInstruction getFieldInstruction = (FieldInstruction) ih2[3].getInstruction();
                                if (getFieldInstruction.getClassName(cpg).equals(charCG.getClassName())) {
                                    hookHandler.addFieldHook("Character", getFieldInstruction, cpg, "currentAnimationFrame");
                                }
                            }
                        }
                    } else if (parameters[1].length == 5) {
                        boolean found1 = false;
                        for (InstructionHandle instructionHandle : parameters[0]) {
                            if (instructionHandle.getInstruction() instanceof ConstantPushInstruction) {
                                ConstantPushInstruction pushInstruction = (ConstantPushInstruction) instructionHandle.getInstruction();
                                int value = pushInstruction.getValue().intValue();
                                if (value == 1 || value == -1) {
                                    found1 = true;
                                }
                            }
                        }
                        if (found1) {
                            InstructionList iList = new InstructionList();

                            for (InstructionHandle instructionHandle : parameters[1]) {
                                if (instructionHandle.getInstruction() instanceof BranchInstruction) {
                                    iList.append((BranchInstruction) instructionHandle.getInstruction());
                                } else {
                                    iList.append(instructionHandle.getInstruction());
                                }
                            }

                            InstructionFinder instructionFinder2 = new InstructionFinder(iList);
                            for (Iterator<InstructionHandle[]> iterator2 = instructionFinder2.search("aload getfield aload getfield iaload"); iterator2.hasNext();) {
                                InstructionHandle[] ih2 = iterator2.next();
                                FieldInstruction arrayFieldInstruction = (FieldInstruction) ih2[1].getInstruction();

                                if (!arrayFieldInstruction.getFieldType(cpg).equals(Type.getType(int[].class))) {
                                    continue;
                                }

                                FieldInstruction getFieldInstruction = (FieldInstruction) ih2[3].getInstruction();
                                if (getFieldInstruction.getClassName(cpg).equals(charCG.getClassName())) {
                                    hookHandler.addFieldHook("Character", getFieldInstruction, cpg, "currentAnimationFrame");
                                }
                            }
                        }
                    }
                }
            }
            for (Method method : cG.getMethods()) {
                if (!method.isStatic()) {
                    continue;
                }
                if (!method.getReturnType().equals(Type.VOID)) {
                    continue;
                }
                if (method.isAbstract() || method.getCode() == null) {
                    continue;
                }
                Type[] args = method.getArgumentTypes();
                if (args.length < 6 || args.length > 8) {
                    continue;
                }
                if (TypeCounter.getObjectCount(args) != 0) {
                    continue;
                }

                InstructionFinder instructionFinder = new InstructionFinder(new InstructionList(method.getCode().getCode()));
                for (Iterator<InstructionHandle[]> iterator = instructionFinder.search("ConstantPushInstruction idiv"); iterator.hasNext();) {
                    InstructionHandle[] ih = iterator.next();
                    ConstantPushInstruction pushInstruction = (ConstantPushInstruction) ih[0].getInstruction();
                    if (pushInstruction.getValue().intValue() != 255) {
                        continue;
                    }
                    InstructionHandle[][] parameters = InstructionUtils.getParameters(ih[1], cpg, 1);
                    for (InstructionHandle handle : parameters[1]) {
                        if (handle.getInstruction() instanceof GETFIELD) {
                            FieldInstruction fieldInstruction = (FieldInstruction) handle.getInstruction();
                            if (!fieldInstruction.getFieldType(cpg).equals(Type.INT)) {
                                continue;
                            }
                            if (!fieldInstruction.getClassName(cpg).equals(charCG.getClassName())) {
                                continue;
                            }
                            hookHandler.addFieldHook("Character", fieldInstruction, cpg, "HPRatio");
                        }
                    }

                }
            }
        }

    }

    public void hookFromCharacter() {
        ClassGen cG = hookHandler.getClassByNick("Character");
        //hookHandler.addFieldHook("Character",cG, hookHandler.getFieldInClassGen("rb", cG), "orientation");
        ConstantPoolGen cpg = cG.getConstantPool();
        for (Field field : cG.getFields()) {
            if (field.isStatic()) {
                continue;
            }

            if (field.getType().equals(Type.STRING)) {
                hookHandler.addFieldHook("Character", cG, field, "message");
            }
        }
        for (Method method : cG.getMethods()) {
            if (method.isStatic()) {
                continue;
            }
            if (method.isAbstract()) {
                continue;
            }
            if (!method.getReturnType().equals(Type.VOID)) {
                continue;
            }
            Type[] args = method.getArgumentTypes();
            if (args.length < 2 || args.length > 3) {
                continue;
            }
            InstructionFinder instructionFinder = new InstructionFinder(new InstructionList(method.getCode().getCode()));
            for (Iterator<InstructionHandle[]> iterator = instructionFinder.search("iand putfield"); iterator.hasNext();) {
                InstructionHandle[] ih = iterator.next();
                InstructionHandle[][] parameters = InstructionUtils.getParameters(ih[0], cpg, 1);
                boolean valid = false;
                if (parameters[0].length != 1 || parameters[1].length != 1) {
                    continue;
                }
                if (parameters[0][0].getInstruction() instanceof SIPUSH
                        && parameters[1][0].getInstruction() instanceof ILOAD) {
                    SIPUSH sipush = (SIPUSH) parameters[0][0].getInstruction();
                    if (sipush.getValue().intValue() == 0x3fff) {
                        valid = true;
                    }
                } else if (parameters[1][0].getInstruction() instanceof SIPUSH
                        && parameters[0][0].getInstruction() instanceof ILOAD) {
                    SIPUSH sipush = (SIPUSH) parameters[1][0].getInstruction();
                    if (sipush.getValue().intValue() == 0x3fff) {
                        valid = true;
                    }
                }
                if (parameters[0][0].getInstruction() instanceof ILOAD
                        && parameters[1][0].getInstruction() instanceof ILOAD) {
                    valid = true;
                }

                if (valid) {
                    FieldInstruction fieldInstruction = (FieldInstruction) ih[1].getInstruction();
                    hookHandler.addFieldHook("Character", fieldInstruction, cpg, "orientation");
                }
            }
        }
        /*
        for(Method method: cG.getMethods()){
            if(method.isStatic()){
                continue;
            }
            Type[] args = method.getArgumentTypes();
            if(args.length > 1){
                continue;
            }
            if(TypeCounter.getObjectCount(args) != 0){
                continue;
            }
            if(method.isAbstract() || method.getCode() == null || method.getCode().getLength() == 0){
                continue;
            }
            InstructionFinder instructionFinder = new InstructionFinder(new InstructionList(method.getCode().getCode()));
            int integersFounds = 0;
            for (Iterator<InstructionHandle[]> iterator = instructionFinder.search("sipush"); iterator.hasNext();) {
                InstructionHandle[] ih = iterator.next();
                SIPUSH sipush = (SIPUSH) ih[0].getInstruction();
                int value = sipush.getValue().intValue();
                if(value == 5120 || value == ~5120
                        || value == 13312 || value == ~13312
                        || value == 11264 || value == ~11264
                        || value == 3072 || value == ~3072){
                    integersFounds++;
                }

            }
            if(integersFounds < 3){
                continue;
            }
            for (Iterator<InstructionHandle[]> iterator = instructionFinder.search("aload getfield ifeq"); iterator.hasNext();) {
                InstructionHandle[] ih = iterator.next();
                FieldInstruction fieldInstruction = (FieldInstruction) ih[1].getInstruction();
                if(fieldInstruction.getType(cpg).equals(Type.BOOLEAN)){
                    hookHandler.addFieldHook("Character", fieldInstruction, cpg, "interacting");
                }
            }
        }*/
    }

    private void hookLoopCycle() {
        ClassGen charCG = hookHandler.getClassByNick("Character");
        for (ClassGen cG : classes) {
            ConstantPoolGen cpg = cG.getConstantPool();
            if (cpg.lookupUtf8("gnpov1") == -1) {
                continue;
            }

            for (Method method : cG.getMethods()) {
                if (!method.isStatic()) {
                    continue;
                }
                if (!method.getReturnType().equals(Type.VOID)) {
                    continue;
                }
                Type[] args = method.getArgumentTypes();
                if (args.length > 1) {
                    continue;
                }
                if (TypeCounter.getObjectCount(args) != 0) {
                    continue;
                }
                List<Container> statics = new ArrayList<Container>();
                List<Container> fields = new ArrayList<Container>();
                InstructionFinder instructionFinder = new InstructionFinder(new InstructionList(method.getCode().getCode()));
                for (Iterator<InstructionHandle[]> iterator = instructionFinder.search("getstatic putfield"); iterator.hasNext();) {
                    InstructionHandle[] ih = iterator.next();
                    FieldInstruction staticFieldInstruction = (FieldInstruction) ih[0].getInstruction();
                    FieldInstruction fieldInstruction = (FieldInstruction) ih[1].getInstruction();
                    if (!fieldInstruction.getType(cpg).equals(Type.INT) || !staticFieldInstruction.getFieldType(cpg).equals(Type.INT)) {
                        continue;
                    }
                    int index = -1;
                    for (int i = 0; i < statics.size(); i++) {
                        Container staticContainer = statics.get(i);
                        Container fieldContainer = fields.get(i);

                        if (staticContainer.className.equals(staticFieldInstruction.getClassName(cpg))
                                && staticContainer.fieldName.equals(staticFieldInstruction.getFieldName(cpg))
                                && fieldContainer.className.equals(fieldInstruction.getClassName(cpg))
                                && fieldContainer.fieldName.equals(fieldInstruction.getFieldName(cpg))) {
                            index = i;
                        }
                    }
                    if (index == -1) {
                        Container staticContainer = new Container();
                        Container fieldContainer = new Container();
                        staticContainer.className = staticFieldInstruction.getClassName(cpg);
                        staticContainer.fieldName = staticFieldInstruction.getFieldName(cpg);
                        fieldContainer.className = fieldInstruction.getClassName(cpg);
                        fieldContainer.fieldName = fieldInstruction.getFieldName(cpg);
                        staticContainer.count++;
                        fieldContainer.count++;
                        statics.add(staticContainer);
                        fields.add(fieldContainer);
                    } else {
                        Container staticContainer = statics.get(index);
                        Container fieldContainer = fields.get(index);
                        staticContainer.count++;
                        fieldContainer.count++;
                    }
                }

                for (int i = 0; i < statics.size(); i++) {
                    Container staticContainer = statics.get(i);
                    Container fieldContainer = fields.get(i);
                    if (staticContainer.count == 4) {
                        hookHandler.addClientHook(staticContainer.className, staticContainer.fieldName, "loopCycle", Type.INT);
                        hookHandler.addFieldHook("Character", fieldContainer.className, fieldContainer.fieldName, "loopCycleStatus", Type.INT);
                    }
                }
            }

        }
    }

    private class Container {
        String fieldName;
        String className;
        int count = 0;
    }


    public String[] getFieldHooks() {
        return new String[]{
                "Client::myPlayer",
                "Client::baseX",
                "Client::baseY",
                "Client::loopCycle",
                "Character::loopCycleStatus",
                "Character::walkingX",
                "Character::walkingY",
                "Character::message",
                "Character::interactingCharacterIndex",
                "Character::orientation",
                "Character::currentAnimationFrame",
                "Character::HPRatio",
                "Client::destX",
                "Client::destY",
                "Client::destSet",
                "Character::hitLoopCycleArray",
                "Character::UID",
        };
    }
}
