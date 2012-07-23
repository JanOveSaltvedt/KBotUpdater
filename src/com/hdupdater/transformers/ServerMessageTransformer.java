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
import com.hdupdater.hooks.ServerMessageHook;
import com.sun.org.apache.bcel.internal.generic.*;
import com.sun.org.apache.bcel.internal.classfile.Method;
import com.sun.org.apache.bcel.internal.classfile.ExceptionTable;
import com.sun.org.apache.bcel.internal.util.InstructionFinder;

import java.util.Iterator;

/**
 * Created by IntelliJ IDEA.
 * User: Jan Ove Saltvedt
 * Date: Oct 17, 2009
 * Time: 4:51:32 PM
 * To change this template use File | Settings | File Templates.
 */
public class ServerMessageTransformer extends AbstractTransformer {
    public void run() {
        ClassGen nodeCG = hookHandler.getClassByNick("Node");
        final FieldHook canvasHook = hookHandler.getFieldHook("Client", "canvas");
        // First we need to find the stringBuilder method
        ClassGen stringBuilderClass = null;
        Method stringBuilderMethod = null;
        ClassGen getStringClass = null;
        Method getStringMethod = null;
        for(ClassGen cG: classes){
            ConstantPoolGen cpg = cG.getConstantPool();
            for(Method method: cG.getMethods()){
                if(!method.isStatic()){
                    continue;
                }

                if(!method.getReturnType().equals(Type.getType(String.class))){
                    continue;
                }
                // Method should have at least 3 args, may be 4.
                // byte array
                // and two ints
                Type[] args = method.getArgumentTypes();
                if(args.length < 3 || args.length > 4)
                    continue;

                int intCount = 0;
                int byteArrayCount = 0;
                for(Type arg: args){
                    if(arg.equals(Type.INT) || arg.equals(Type.BYTE)){
                        intCount++;
                    }
                    if(arg.equals(Type.getType(byte[].class))){
                        byteArrayCount++;
                    }
                }
                if(intCount < 2 || intCount > 3)
                    continue;
                if(byteArrayCount != 1){
                    continue;
                }

                InstructionFinder instructionFinder = new InstructionFinder(new InstructionList(method.getCode().getCode()));
                int ldcCount = 0;
                for(Iterator<InstructionHandle[]> ihIterator = instructionFinder.search("LDC"); ihIterator.hasNext();){
                    InstructionHandle[] ih = ihIterator.next();
                    LDC ldc = (LDC) ih[0].getInstruction();
                    if(ldc.getValue(cpg).equals("Illegal first sequence byte in UTF-8 encoding ")){
                        ldcCount++;
                    }
                }
                if(ldcCount != 0)
                    continue;
                //Debug.writeLine("found in "+cG.getClassName());
                stringBuilderClass = cG;
                stringBuilderMethod = method;
            }
        }

        if(stringBuilderClass == null || stringBuilderMethod == null){
            System.err.println("Could not create the Server Message Callback");
            return;
        }
        for(ClassGen cG: classes){

            ConstantPoolGen cpg = cG.getConstantPool();

            if(!cG.getSuperclassName().equals(nodeCG.getClassName())){
                continue;
            }
            if(cpg.lookupUtf8("Bad version number in gjstr2") == -1){
                continue;
            }
            //Debug.writeLine("found "+cG.getClassName());
            for(Method method: cG.getMethods()){
                if(method.isStatic())
                    continue;
                if(!method.getReturnType().equals(Type.getType(String.class))){
                    continue;
                }
                final Type[] args = method.getArgumentTypes();
                if(args.length > 1)
                    continue;
                MethodGen methodGen = new MethodGen(method, cG.getClassName(), cpg);
                InstructionList iList = methodGen.getInstructionList();
                InstructionFinder instructionFinder = new InstructionFinder(iList);
                boolean foundRightInvoke = false;
                for(Iterator<InstructionHandle[]> ihIterator = instructionFinder.search("INVOKESTATIC ARETURN"); ihIterator.hasNext();){
                    InstructionHandle[] ih = ihIterator.next();
                    InvokeInstruction invokeInstruction = (InvokeInstruction) ih[0].getInstruction();

                    if(invokeInstruction.getClassName(cpg).equals(stringBuilderClass.getClassName())
                            && invokeInstruction.getMethodName(cpg).equals(stringBuilderMethod.getName())
                            && invokeInstruction.getSignature(cpg).equals(stringBuilderMethod.getSignature())){
                        foundRightInvoke = true;
                    }
                }
                if(!foundRightInvoke)
                    continue;
                int ldcCount = 0;
                for(Iterator<InstructionHandle[]> ihIterator = instructionFinder.search("LDC"); ihIterator.hasNext();){
                    InstructionHandle[] ih = ihIterator.next();
                    LDC ldc = (LDC) ih[0].getInstruction();
                    if(ldc.getValue(cpg).equals("Bad version number in gjstr2")){
                        ldcCount++;
                    }
                }
                if(ldcCount != 0)
                    continue;
                //Debug.writeLine("found");
                getStringClass = cG;
                getStringMethod = method;
            }
        }
        if(getStringClass == null || getStringMethod == null){
            System.err.println("Could not create the Server Message Callback");
            return;
        }
        //Debug.writeLine("found");
        for (ClassGen cG : normalClasses) {
            ConstantPoolGen cpg = cG.getConstantPool();
            for(Method m: cG.getMethods()){
                if(!m.isStatic())
                    continue;
                if(!m.getReturnType().equals(Type.BOOLEAN))
                    continue;
                final ExceptionTable exceptionTable = m.getExceptionTable();
                if(exceptionTable == null)
                    continue;
                if(exceptionTable.getNumberOfExceptions() != 1){
                    continue;
                }
                if(exceptionTable.getExceptionNames()[0].contains("IOException")){
                    MethodGen methodGen = new MethodGen(m, cG.getClassName(), cpg);
                    InstructionList iList = methodGen.getInstructionList();
                    InstructionFinder instructionFinder = new InstructionFinder(iList);
                    for(Iterator<InstructionHandle[]> ihIterator = instructionFinder.search("((getstatic)|(getstatic iconst_m1 ixor)) ((getstatic)|(getstatic iconst_m1 ixor)) if_acmpne"); ihIterator.hasNext();){
                        InstructionHandle[] ih = ihIterator.next();
                        final InstructionHandle startHandle = ih[ih.length - 1];
                        BranchInstruction branchInstruction = (BranchInstruction) startHandle.getInstruction();
                        final InstructionHandle stopHandle = branchInstruction.getTarget();
                        InstructionList block = new InstructionList();
                        InstructionHandle current = startHandle.getNext();
                        while (current != stopHandle){
                            final Instruction instruction = current.getInstruction();
                            if(instruction instanceof BranchInstruction){
                                block.append((BranchInstruction)instruction);
                            }
                            else{
                                block.append(instruction);
                            }
                            current = current.getNext();
                        }
                        block.setPositions();
                        InstructionFinder blockFinder = new InstructionFinder(block);
                        int readStrings = 0;
                        for(Iterator<InstructionHandle[]> iterator = blockFinder.search("INVOKEVIRTUAL ASTORE"); iterator.hasNext();){
                            InstructionHandle[] bIH = iterator.next();
                            InvokeInstruction invokeInstruction = (InvokeInstruction) bIH[0].getInstruction();
                            if(invokeInstruction.getClassName(cpg).equals(getStringClass.getClassName())
                                    && invokeInstruction.getMethodName(cpg).equals(getStringMethod.getName())
                                    && invokeInstruction.getSignature(cpg).equals(getStringMethod.getSignature())) {
                                readStrings++;
                            }
                        }
                        if(readStrings != 3){
                            continue;
                        }
                        int aloadEmpty1Index = -1;
                        int aloadEmpty2Index = -1;
                        int aloadSeverMessageIndex = -1;
                        for(Iterator<InstructionHandle[]> iterator = blockFinder.search("LDC ASTORE"); iterator.hasNext();){
                            InstructionHandle[] bIH = iterator.next();
                            LDC ldc = (LDC) bIH[0].getInstruction();
                            if(!ldc.getValue(cpg).equals(""))
                                continue;
                            aloadEmpty1Index = ((ASTORE)bIH[1].getInstruction()).getIndex();
                        }
                        if(aloadEmpty1Index == -1){
                            continue;
                        }
                        for(Iterator<InstructionHandle[]> iterator = blockFinder.search("ALOAD ASTORE"); iterator.hasNext();){
                            InstructionHandle[] bIH = iterator.next();
                            ALOAD aload = (ALOAD) bIH[0].getInstruction();
                            ASTORE astore = (ASTORE) bIH[1].getInstruction();
                            if(aload.getIndex() == astore.getIndex())
                                continue;
                            if(aload.getIndex() == aloadEmpty1Index){
                                aloadEmpty2Index = astore.getIndex();
                            }
                        }
                        if(aloadEmpty2Index == -1){
                            continue;
                        }
                        int injectionPos = -1;
                        for(Iterator<InstructionHandle[]> iterator = blockFinder.search("INVOKEVIRTUAL ASTORE"); iterator.hasNext();){
                            InstructionHandle[] bIH = iterator.next();
                            InvokeInstruction invokeInstruction = (InvokeInstruction) bIH[0].getInstruction();
                            ASTORE astore = (ASTORE) bIH[1].getInstruction();
                            if(invokeInstruction.getClassName(cpg).equals(getStringClass.getClassName())
                                    && invokeInstruction.getMethodName(cpg).equals(getStringMethod.getName())
                                    && invokeInstruction.getSignature(cpg).equals(getStringMethod.getSignature())) {
                                if(astore.getIndex() == aloadEmpty1Index || astore.getIndex() == aloadEmpty2Index)
                                    continue;
                                aloadSeverMessageIndex = astore.getIndex();
                                injectionPos = bIH[1].getPosition();
                            }
                        }
                        injectionPos += startHandle.getNext().getPosition();
                        if(injectionPos == -1 || aloadSeverMessageIndex == -1){
                            continue;
                        }
                        System.out.println("Hooked Server Messages");
                        new ServerMessageHook(hookHandler, cG, m, aloadSeverMessageIndex, injectionPos);
                        //Debug.writeLine(""+startHandle.getPosition()+":"+aloadSeverMessageIndex+":"+injectionPos);
                    }
                }
            }
        }
    }

    public String[] getFieldHooks() {
        return new String[0];  //To change body of implemented methods use File | Settings | File Templates.
    }
}
