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



package com.hdupdater.deobbers;

import com.sun.org.apache.bcel.internal.generic.*;
import com.sun.org.apache.bcel.internal.classfile.Method;
import com.sun.org.apache.bcel.internal.util.InstructionFinder;
import com.hdupdater.AbstractDeobber;
import com.hdupdater.Constants;

import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;

/**
 * Created by IntelliJ IDEA.
 * User: Jan Ove Saltvedt
 * Date: Sep 17, 2009
 * Time: 6:05:06 PM
 * To change this template use File | Settings | File Templates.
 */
public class RemoveZKM extends AbstractDeobber {

    public void run() {
        if(!Constants.REMOVE_ZKM)
            return;
        System.out.print("Removing ZKM obfuscation...");

        FieldID ctrlField = null;
        firstLoop:
        for(ClassGen classGen: classes){
            ConstantPoolGen cpg = classGen.getConstantPool();
            for(Method method: classGen.getMethods()){
                if(method.isAbstract())
                    continue;
                MethodGen methodGen = new MethodGen(method,  classGen.getClassName(), cpg);
                InstructionList iList = methodGen.getInstructionList();
                if(iList == null){
                    continue;
                }
                InstructionFinder instructionFinder = new InstructionFinder(iList);
                for(Iterator<InstructionHandle[]> instructionHandleIterator = instructionFinder.search("(GETSTATIC IFEQ ILOAD IFEQ ICONST GOTO ICONST PUTSTATIC) | (GETSTATIC IFEQ IINC ILOAD PUTSTATIC RETURN)"); instructionHandleIterator.hasNext();){
                    InstructionHandle[] ih = instructionHandleIterator.next();
                    FieldInstruction fieldinstruction = (FieldInstruction) ih[0].getInstruction();
                    ctrlField = new FieldID(fieldinstruction.getName(cpg), fieldinstruction.getSignature(cpg), fieldinstruction.getClassName(cpg));
                    break firstLoop;
                }
            }
        }

        if (ctrlField == null) {
            System.out.println("FAILED");
            System.out.println("Control Field not found.");
            return;
        }

        final List<FieldID> bad_fields = new ArrayList<FieldID>();

        for(ClassGen cG: classes){
            final ConstantPoolGen cpg = cG.getConstantPool();
            for(Method method: cG.getMethods()){
                if(method.isAbstract()){
                    continue;
                }
                MethodGen methodGen = new MethodGen(method, cG.getClassName(), cpg);
                InstructionList iList = methodGen.getInstructionList();
                if(iList == null){
                    continue;
                }
                InstructionFinder instructionFinder = new InstructionFinder(iList);


                final FieldID ctrlField3 = ctrlField;
                InstructionFinder.CodeConstraint codeConstraint = new InstructionFinder.CodeConstraint() {
                    public boolean checkCode(InstructionHandle[] match) {
                        FieldInstruction fieldInstruction = (FieldInstruction) match[0].getInstruction();
                        FieldID fieldID = new FieldID(fieldInstruction.getName(cpg), fieldInstruction.getSignature(cpg), fieldInstruction.getClassName(cpg));
                        return fieldID.equals(ctrlField3);
                    }
                };
                for(Iterator<InstructionHandle[]> instructionHandleIterator = instructionFinder.search("GETSTATIC IFEQ ((IINC ILOAD) | (ILOAD IFEQ ICONST GOTO ICONST)) PUTSTATIC", codeConstraint); instructionHandleIterator.hasNext();){
                    InstructionHandle[] ih = instructionHandleIterator.next();
                    FieldInstruction fieldInstruction = (FieldInstruction) ih[ih.length-1].getInstruction();
                    bad_fields.add(new FieldID(fieldInstruction.getName(cpg), fieldInstruction.getSignature(cpg), fieldInstruction.getClassName(cpg)));
                }
            }
        }

        if (bad_fields.isEmpty()) {
            System.out.println("FAILED");
            System.out.println("No bad fields found.");
            return;
        }
        for(ClassGen cG: classes){
            final ConstantPoolGen cpg = cG.getConstantPool();
            for(Method method: cG.getMethods()){
                if(method.isAbstract())
                    continue;
                MethodGen methodGen = new MethodGen(method, cG.getClassName(), cpg);
                final InstructionList iList = methodGen.getInstructionList();
                if(iList == null){
                    continue;
                }
                InstructionFinder instructionFinder = new InstructionFinder(iList);
                final FieldID ctrlField1 = ctrlField;
                InstructionFinder.CodeConstraint codeConstraint = new InstructionFinder.CodeConstraint() {
                    public boolean checkCode(InstructionHandle[] match) {
                        if(match.length >= 3)
                            return false;
                        if(match[0].getInstruction() instanceof GETSTATIC){
                            FieldInstruction fieldInstruction = (FieldInstruction) match[0].getInstruction();
                            FieldID current = new FieldID(fieldInstruction.getName(cpg), fieldInstruction.getSignature(cpg), fieldInstruction.getClassName(cpg));
                            for(FieldID fieldID: bad_fields){
                                if(fieldID.equals(current))
                                    return true;
                            }
                            return false;
                        }

                        int index = ((LocalVariableInstruction)match[0].getInstruction()).getIndex();
                        InstructionFinder instructionFinder = new InstructionFinder(iList);
                        for(Iterator<InstructionHandle[]> ihIterator = instructionFinder.search("getstatic istore"); ihIterator.hasNext();){
                            InstructionHandle[] ih = ihIterator.next();
                            FieldInstruction fieldInstruction = (FieldInstruction) ih[0].getInstruction();
                            FieldID current = new FieldID(fieldInstruction.getName(cpg), fieldInstruction.getSignature(cpg), fieldInstruction.getClassName(cpg));
                            for(FieldID fieldID: bad_fields){
                                if(fieldID.equals(current)){
                                    return index == ((LocalVariableInstruction)ih[1].getInstruction()).getIndex();
                                }
                            }
                        }
                        if(match[0].getInstruction() instanceof GETSTATIC){
                            FieldInstruction fieldInstruction = (FieldInstruction) match[0].getInstruction();
                            return ctrlField1.equals(new FieldID(fieldInstruction.getName(cpg), fieldInstruction.getSignature(cpg), fieldInstruction.getClassName(cpg)));
                        }
                        else{
                            return false;
                        }
                    }
                };
                for(Iterator<InstructionHandle[]> ihIterator = instructionFinder.search("(GETSTATIC | ILOAD) (ISTORE | (IFEQ | IFNE)) (((IINC ILOAD) | ((ILOAD IFEQ)? ICONST GOTO ICONST)) PUTSTATIC)?", codeConstraint); ihIterator.hasNext();){
                    InstructionHandle[] ih = ihIterator.next();
                    InstructionHandle first = ih[0];
                    InstructionHandle last = ih[ih.length-1];
                    InstructionHandle afterLast = last.getNext();
                    if(afterLast == null){
                        break;
                    }
                    if(ih.length < 3){
                        if((last.getInstruction() instanceof ISTORE) || (last.getInstruction() instanceof IFNE)){
                            try{
                                iList.delete(first, last);
                            }catch (TargetLostException e){
                                InstructionHandle[] targets = e.getTargets();
                                for(InstructionHandle instructionHandle: targets){
                                    InstructionTargeter[] instructionTargeters = instructionHandle.getTargeters();
                                    for(InstructionTargeter instructionTargeter: instructionTargeters){
                                        instructionTargeter.updateTarget(instructionHandle, afterLast);
                                    }
                                }

                            }

                        }
                        else if(last.getInstruction() instanceof IFEQ){
                            InstructionHandle branchHandle = ((BranchHandle)last).getTarget();
                            last.setInstruction(new GOTO(branchHandle));
                            try{
                                iList.delete(first, last);
                            }catch (TargetLostException e){
                                InstructionHandle[] targets = e.getTargets();
                                for(InstructionHandle instructionHandle: targets){
                                    InstructionTargeter[] instructionTargeters = instructionHandle.getTargeters();
                                    for(InstructionTargeter instructionTargeter: instructionTargeters){
                                        instructionTargeter.updateTarget(instructionHandle, afterLast);
                                    }
                                }
                            }
                        }
                    } else{
                        try{
                            iList.delete(first, last);
                        }catch (TargetLostException e){
                            InstructionHandle[] targets = e.getTargets();
                            for(InstructionHandle instructionHandle: targets){
                                InstructionTargeter[] instructionTargeters = instructionHandle.getTargeters();
                                for(InstructionTargeter instructionTargeter: instructionTargeters){
                                    instructionTargeter.updateTarget(instructionHandle, afterLast);
                                }
                            }
                       }
                    }
                }
                methodGen.setInstructionList(iList);
                methodGen.setMaxLocals();
                methodGen.setMaxStack();
                cG.replaceMethod(method,  methodGen.getMethod());
            }
        }
        System.out.println("DONE");
    }

    class FieldID {

        public boolean equals(Object obj) {
            if (!(obj instanceof FieldID)) {
                return false;
            } else {
                FieldID fieldid = (FieldID) obj;
                return name.equals(fieldid.name) && sig.equals(fieldid.sig) && clsname.equals(fieldid.clsname);
            }
        }

        String name;
        String sig;
        String clsname;

        FieldID(String name, String sig, String clsname) {
            this.name = name;
            this.sig = sig;
            this.clsname = clsname;
        }
    }
}
