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
import com.hdupdater.utils.instructionSearcher.InstructionSearcher;
import com.hdupdater.utils.instructionSearcher.Matcher;
import com.hdupdater.utils.instructionSearcher.matchers.FieldInstructionMatcher;
import com.sun.org.apache.bcel.internal.classfile.Method;
import com.sun.org.apache.bcel.internal.generic.*;
import com.sun.org.apache.bcel.internal.util.InstructionFinder;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: Jan Ove Saltvedt
 * Date: Sep 27, 2009
 * Time: 7:21:54 PM
 * To change this template use File | Settings | File Templates.
 */
public class MenuTransformer extends AbstractTransformer {

    @Override
    public Class<?>[] getDependencies() {
        return new Class<?>[]{MenuElementNodeTransformer.class, NodeListTransformer.class, MLStringTransformer.class};
    }

    public void run() {
        ClassGen MLStringCG = hookHandler.getClassByNick("MLString");
        ClassGen menuNodeCG = hookHandler.getClassByNick("MenuElementNode");
        ClassGen nodeListCG = hookHandler.getClassByNick("NodeList");

        String className = null;
        String fieldName = null;
        for (ClassGen cG : classes) {
            ConstantPoolGen cpg = cG.getConstantPool();
            if (cpg.lookupUtf8("Choose Option") != -1) {
                for (Method method : cG.getMethods()) {
                    if (!method.getName().equals("<clinit>"))
                        continue;
                    InstructionFinder instructionFinder = new InstructionFinder(new InstructionList(method.getCode().getCode()));
                    for (Iterator<InstructionHandle[]> iterator = instructionFinder.search("new dup ldc ldc ldc ldc invokespecial putstatic"); iterator.hasNext();) {
                        InstructionHandle[] ih = iterator.next();
                        NEW newIns = (NEW) ih[0].getInstruction();
                        if(!newIns.getLoadClassType(cpg).toString().equals(MLStringCG.getClassName())){
                            continue;
                        }

                        LDC ldcEnglish = (LDC) ih[2].getInstruction();
                        if(ldcEnglish.getValue(cpg).equals("Choose Option")){
                            FieldInstruction fieldInstruction = (FieldInstruction) ih[ih.length-1].getInstruction();
                            className = fieldInstruction.getClassName(cpg);
                            fieldName = fieldInstruction.getFieldName(cpg);
                        }
                    }
                }
            }
        }
        //Debug.writeLine("" + className + "." + fieldName);
        if (className == null || fieldName == null)
            return;
        for (ClassGen cG : classes) {
            ConstantPoolGen cpg = cG.getConstantPool();
            if (cpg.lookupFieldref(className, fieldName, "L"+MLStringCG.getClassName()+";") == -1)
                continue;
            for (Method method : cG.getMethods()) {
                if (!method.isStatic())
                    continue;
                if (method.getReturnType() != Type.VOID)
                    continue;
                Type[] args = method.getArgumentTypes();
                if (args.length < 1 || args.length > 2)
                    continue;
                int objCount = 0;
                for (Type arg : args) {
                    if (arg instanceof ObjectType) {
                        if (hookHandler.classes.containsKey(arg.toString())) {
                            objCount++;
                        }
                    }
                }
                if (objCount != 1) {
                    continue;
                }


                InstructionSearcher searcher = new InstructionSearcher(cG, method);
                FieldInstructionMatcher fieldInstructionMatcher = new FieldInstructionMatcher("FieldInsMatcher", className, fieldName, true);
                searcher.runMatcher(fieldInstructionMatcher);
                if (fieldInstructionMatcher.getFirstMatch() == null)
                    continue;

                // Get the NodeList
                InstructionFinder instructionFinder = new InstructionFinder(new InstructionList(method.getCode().getCode()));
                for (Iterator<InstructionHandle[]> iterator = instructionFinder.search("getstatic" /* (ConstantPushInstruction)? invokevirtual checkcast astore"*/); iterator.hasNext();) {
                    InstructionHandle[] ih = iterator.next();
                    FieldInstruction fieldInstruction = (FieldInstruction) ih[0].getInstruction();
                    if (!fieldInstruction.getFieldType(cpg).getSignature().equals("L" + nodeListCG.getClassName() + ";")) {
                        continue;
                    }
                    if (hookHandler.getFieldHook("Client", "menuNodeList") == null) {
                        hookHandler.addClientHook(fieldInstruction, cpg, "menuNodeList", TypeBuilder.createHookType("NodeList"));
                    }
                    break;
                }

                List<FieldContainer> possibleFields = new ArrayList<FieldContainer>();
                for (Iterator<InstructionHandle[]> iterator = instructionFinder.search("InvokeStatic"); iterator.hasNext();) {

                    InstructionHandle[] ih = iterator.next();
                    InvokeInstruction invokeInstruction = (InvokeInstruction) ih[0].getInstruction();
                    if(!invokeInstruction.getReturnType(cpg).equals(Type.VOID)){
                        continue;
                    }
                    if(invokeInstruction.getArgumentTypes(cpg).length < 4 || invokeInstruction.getArgumentTypes(cpg).length > 5){
                        continue;
                    }
                    InstructionHandle[][] parameters = InstructionUtils.getParameters(ih[0], cpg, 1);
                    for (InstructionHandle[] insList : parameters) {
                        if (insList.length != 1)
                            continue;
                        if (!(insList[0].getInstruction() instanceof GETSTATIC)) {
                            continue;
                        }
                        FieldContainer fContainer = new FieldContainer();
                        fContainer.fieldInstruction = (FieldInstruction) insList[0].getInstruction();
                        possibleFields.add(fContainer);
                    }
                }

                if(possibleFields.isEmpty() || possibleFields.size() != 4){
                    continue;
                }

                for (Iterator<InstructionHandle[]> iterator = instructionFinder.search("getstatic istore"); iterator.hasNext();) {
                    InstructionHandle[] ih = iterator.next();
                    FieldInstruction fieldInstruction = (FieldInstruction) ih[0].getInstruction();
                    for (FieldContainer fieldContainer : possibleFields) {
                        if (!fieldInstruction.getClassName(cpg).equals(fieldContainer.fieldInstruction.getClassName(cpg))) {
                            continue;
                        }
                        if (!fieldInstruction.getFieldName(cpg).equals(fieldContainer.fieldInstruction.getFieldName(cpg))) {
                            continue;
                        }
                        fieldContainer.localVarIndex = ((ISTORE) ih[1].getInstruction()).getIndex();
                    }
                }

                // menuOptionsCount
                for (Iterator<InstructionHandle[]> iterator = instructionFinder.search("istore"); iterator.hasNext();) {
                    InstructionHandle[] ih = iterator.next();
                    InstructionHandle[][] parameters = InstructionUtils.getParameters(ih[0], cpg, 1);

                    List<InstructionHandle> instructionHandles = new ArrayList<InstructionHandle>();
                    InstructionHandle current = parameters[parameters.length-1][0];
                    InstructionHandle last = parameters[0][parameters[0].length-1];
                    while(current != null && current != last.getNext()){
                        instructionHandles.add(current);
                        current = current.getNext();
                    }

                    boolean found16 = false;
                    boolean found31 = false;
                    for(InstructionHandle instructionHandle: instructionHandles){
                        Instruction ins = instructionHandle.getInstruction();
                        if(ins instanceof ConstantPushInstruction){
                            ConstantPushInstruction pushInstruction = (ConstantPushInstruction) ins;
                            if(pushInstruction.getValue().intValue() == 16){
                                found16 = true;
                            }
                            if(pushInstruction.getValue().intValue() == 31 || pushInstruction.getValue().intValue() == -31){
                                found31 = true;
                            }
                        }
                    }

                    if(found16 && found31){
                        for(InstructionHandle instructionHandle: instructionHandles){
                            Instruction ins = instructionHandle.getInstruction();
                            if(ins instanceof GETSTATIC){
                                FieldInstruction fieldInstruction = (FieldInstruction) ins;
                                hookHandler.addClientHook(fieldInstruction,cpg,"menuOptionsCount");
                            }
                            if(ins instanceof ILOAD){
                                ILOAD iload = (ILOAD) ins;
                                for (FieldContainer fieldContainer : possibleFields) {
                                    if (fieldContainer.localVarIndex == iload.getIndex()) {
                                        fieldContainer.nick = "menuY";
                                        hookHandler.addClientHook(fieldContainer.fieldInstruction, cpg, "menuY");
                                    }
                                }
                            }
                        }
                    }
                }

                SEARCH_LOOP:
                for (Iterator<InstructionHandle[]> iterator = instructionFinder.search("iload iload IfInstruction"); iterator.hasNext();) {
                    InstructionHandle[] ih = iterator.next();
                    IfInstruction ifInstruction = (IfInstruction) ih[2].getInstruction();
                    //if (ifInstruction instanceof IF_ICMPGT || ifInstruction instanceof IF_ICMPLE) {
                        // one of these should be menuX
                        ILOAD iload = (ILOAD) ih[0].getInstruction();
                        for (FieldContainer fieldContainer : possibleFields) {
                            if (iload.getIndex() == fieldContainer.localVarIndex) {
                                fieldContainer.nick = "menuX";
                                if(hookHandler.getFieldHook("Client", "menuX") == null){
                                    hookHandler.addClientHook(fieldContainer.fieldInstruction, cpg, "menuX");
                                    continue SEARCH_LOOP;
                                }
                            }
                        }
                        iload = (ILOAD) ih[1].getInstruction();
                        for (FieldContainer fieldContainer : possibleFields) {
                            if (iload.getIndex() == fieldContainer.localVarIndex) {
                                fieldContainer.nick = "menuX";
                                if(hookHandler.getFieldHook("Client", "menuX") == null){
                                    hookHandler.addClientHook(fieldContainer.fieldInstruction, cpg, "menuX");
                                    continue SEARCH_LOOP;
                                }
                            }
                        }
                    //}
                }

                SEARCH_LOOP:
                for (Iterator<InstructionHandle[]> iterator = instructionFinder.search("IfInstruction"); iterator.hasNext();) {
                    InstructionHandle[] ih = iterator.next();

                    IfInstruction ifInstruction = (IfInstruction) ih[0].getInstruction();
                    if (ifInstruction instanceof IF_ICMPLT || ifInstruction instanceof IF_ICMPGE || ifInstruction instanceof IF_ICMPGT) {
                        InstructionHandle[][] parameters = InstructionUtils.getParameters(ih[0], cpg, 1);
                        InstructionHandle iaddHandle = null;
                        if(parameters[0][parameters[0].length-1].getInstruction() instanceof IADD){
                            iaddHandle = parameters[0][parameters[0].length-1];
                        }
                        else if(parameters[1][parameters[1].length-1].getInstruction() instanceof IADD){
                            iaddHandle = parameters[1][parameters[1].length-1];
                        }
                        if(iaddHandle == null){
                            continue;
                        }
                        parameters = InstructionUtils.getParameters(iaddHandle, cpg, 1);
                        InstructionHandle[] iloads = new InstructionHandle[2];
                        if(parameters[0].length == 1){
                            iloads[0] = parameters[0][0];
                        }
                        if(parameters[1].length == 1){
                            iloads[1] = parameters[1][0];
                        }

                        if(iloads[0] == null || iloads[1] == null){
                            continue;
                        }

                        if(iloads[0].getInstruction() instanceof ILOAD && iloads[1].getInstruction() instanceof ILOAD){
                            ILOAD iload = (ILOAD) iloads[0].getInstruction();
                            for (FieldContainer fieldContainer : possibleFields) {
                                if (iload.getIndex() == fieldContainer.localVarIndex
                                        && fieldContainer.nick != null
                                        && fieldContainer.nick.equals("menuX")) {
                                    iload = (ILOAD) iloads[1].getInstruction();
                                    for (FieldContainer fieldContainer2 : possibleFields) {
                                        if (iload.getIndex() == fieldContainer2.localVarIndex){
                                            fieldContainer.nick = "menuWidth";
                                             if(hookHandler.getFieldHook("Client", "menuWidth") == null){
                                                hookHandler.addClientHook(fieldContainer2.fieldInstruction, cpg, "menuWidth");
                                                continue SEARCH_LOOP;
                                            }
                                        }
                                    }
                                }
                            }

                            iload = (ILOAD) iloads[1].getInstruction();
                            for (FieldContainer fieldContainer : possibleFields) {
                                if (iload.getIndex() == fieldContainer.localVarIndex
                                        && fieldContainer.nick != null
                                        && fieldContainer.nick.equals("menuX")) {
                                    iload = (ILOAD) iloads[0].getInstruction();
                                    for (FieldContainer fieldContainer2 : possibleFields) {
                                        if (iload.getIndex() == fieldContainer2.localVarIndex){
                                            fieldContainer.nick = "menuWidth";
                                             if(hookHandler.getFieldHook("Client", "menuWidth") == null){
                                                hookHandler.addClientHook(fieldContainer2.fieldInstruction, cpg, "menuWidth");
                                                continue SEARCH_LOOP;
                                            }
                                        }
                                    }
                                }
                            }

                        }

                    }
                }

                for(FieldContainer fieldContainer: possibleFields){
                    if(fieldContainer.nick == null){
                        fieldContainer.nick = "menuHeight";
                        if(hookHandler.getFieldHook("Client", "menuHeight") == null){
                            hookHandler.addClientHook(fieldContainer.fieldInstruction, cpg, "menuHeight");
                        }
                    }
                }
            }

        }
        hookIsOpen();
    }

    private void hookIsOpen() {

        FieldHook menuX = hookHandler.getFieldHook("Client", "menuX");
        FieldHook menuY = hookHandler.getFieldHook("Client", "menuY");
        FieldHook menuNodeList = hookHandler.getFieldHook("Client", "menuNodeList");
        for(ClassGen cG: classes){
            ConstantPoolGen cpg = cG.getConstantPool();
            if(cpg.lookupFieldref(menuNodeList.getClassName(), menuNodeList.getFieldName(), menuNodeList.getFieldType().getSignature()) == -1){
                continue;
            }
            for(Method method: cG.getMethods()){
                if(!method.isStatic()){
                    continue;
                }
                //if(!method.getReturnType().equals(Type.VOID)){
                //    continue;
                //}
                Type[] args = method.getArgumentTypes();
                if(args.length < 2 || args.length > 3){
                    continue;
                }
                if(TypeCounter.getIntCount(args) < 2 || TypeCounter.getObjectCount(args) != 0){
                    continue;
                }
                boolean foundPutX = false;
                boolean foundPutY = false;

                InstructionFinder instructionFinder = new InstructionFinder(new InstructionList(method.getCode().getCode()));
                for(Iterator<InstructionHandle[]> iterator = instructionFinder.search("putstatic"); iterator.hasNext();){
                    InstructionHandle[] ih = iterator.next();
                    FieldInstruction fieldInstruction = (FieldInstruction) ih[0].getInstruction();
                    if(fieldInstruction.getClassName(cpg).equals(menuX.getClassName())
                            && fieldInstruction.getFieldName(cpg).equals(menuX.getFieldName())){
                        foundPutX = true;
                    }
                    else if(fieldInstruction.getClassName(cpg).equals(menuY.getClassName())
                            && fieldInstruction.getFieldName(cpg).equals(menuY.getFieldName())){
                        foundPutY = true;
                    }
                }
                if(!foundPutX || !foundPutY){
                    continue;
                }
                for(Iterator<InstructionHandle[]> iterator = instructionFinder.search("putstatic"); iterator.hasNext();){
                    InstructionHandle[] ih = iterator.next();
                    FieldInstruction fieldInstruction = (FieldInstruction) ih[0].getInstruction();
                    if(fieldInstruction.getFieldType(cpg).equals(Type.BOOLEAN)){
                        hookHandler.addClientHook(fieldInstruction, cpg, "menuOpen");
                    }
                }
            }
        }
    }

    public String[] getFieldHooks() {
        return new String[]{
                "Client::menuX",
                "Client::menuY",
                "Client::menuWidth",
                "Client::menuHeight",
                "Client::menuNodeList",
                "Client::menuOptionsCount",
                "Client::menuOpen",
        };  //To change body of implemented methods use File | Settings | File Templates.
    }

    private class FieldContainer {
        public FieldInstruction fieldInstruction;
        public int localVarIndex;
        public String nick;
    }
}
