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



package com.hdupdater.utils;

import com.sun.org.apache.bcel.internal.generic.*;
import com.sun.org.apache.bcel.internal.util.InstructionFinder;
import com.sun.org.apache.bcel.internal.Constants;

import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * Created by IntelliJ IDEA.
 * User: Jan Ove Saltvedt
 * Date: Sep 26, 2009
 * Time: 6:06:24 PM
 * To change this template use File | Settings | File Templates.
 */
public class InstructionUtils {
    public static InstructionHandle[] getBranchBlock(InstructionHandle branchIH) {
        if (!(branchIH.getInstruction() instanceof BranchInstruction)) {
            throw new IllegalArgumentException("InstructionHandle did not contain a BranchInstruction");
        }
        InstructionHandle target = ((BranchInstruction) branchIH.getInstruction()).getTarget();
        InstructionHandle first = branchIH.getNext();
        InstructionHandle last = target;
        if (target.getPosition() < branchIH.getPosition()) {
            first = target;
            last = branchIH;
            return null; // Jumps backwards in code, most likely a loop. Not supported yet
        }
        List<InstructionHandle> ihList = new ArrayList<InstructionHandle>();
        for (InstructionHandle current = first; current != null && current != last; current = current.getNext()) {
            ihList.add(current);
        }
        return ihList.toArray(new InstructionHandle[1]);
    }

    public static Instruction getInstruction(String pattern, InstructionHandle[] ihs) {
        return getInstruction(pattern, 1, ihs);
    }

    public static Instruction getInstruction(String pattern, int matchNum, InstructionHandle[] ihs) {
        InstructionList iList = new InstructionList();
        for (InstructionHandle ih : ihs) {
            if (ih.getInstruction() instanceof BranchInstruction) {
                iList.append((BranchInstruction) ih.getInstruction());
            } else {
                iList.append(ih.getInstruction());
            }
        }
        int match = 0;
        InstructionFinder instructionFinder = new InstructionFinder(iList);
        for (Iterator<InstructionHandle[]> handleIterator = (Iterator<InstructionHandle[]>) instructionFinder.search(pattern); handleIterator.hasNext();) {
            ihs = handleIterator.next();
            match++;
            if (match == matchNum) {
                return ihs[0].getInstruction();
            }
        }
        return null;
    }

    public static Instruction getInstruction(String pattern, int matchNum, InstructionHandle[] ihs, int insNum) {
        InstructionList iList = new InstructionList();
        for (InstructionHandle ih : ihs) {
            if (ih.getInstruction() instanceof BranchInstruction) {
                iList.append((BranchInstruction) ih.getInstruction());
            } else {
                iList.append(ih.getInstruction());
            }
        }
        int match = 0;
        InstructionFinder instructionFinder = new InstructionFinder(iList);
        for (Iterator<InstructionHandle[]> handleIterator = (Iterator<InstructionHandle[]>) instructionFinder.search(pattern); handleIterator.hasNext();) {
            ihs = handleIterator.next();
            match++;
            if (match == matchNum) {
                return ihs[insNum].getInstruction();
            }
        }
        return null;
    }

    public static InstructionHandle[] findBlockICMPNE(ConstantPoolGen cpg, InstructionFinder instructionFinder, final int opCode){
        for(Iterator<InstructionHandle[]> iterator = instructionFinder.search("((((ConstantPushInstruction)|(ldc)) iload)|(iload((ConstantPushInstruction)|(ldc)))) if_icmpne"); iterator.hasNext();){
            InstructionHandle[] ih = iterator.next();
            for(InstructionHandle i: ih){
                if(i.getInstruction() instanceof LDC){
                    LDC ldc = (LDC) i.getInstruction();
                    if(ldc.getValue(cpg) instanceof Integer){
                        if(((Integer)ldc.getValue(cpg)) == opCode){
                            return InstructionUtils.getBranchBlock(ih[ih.length-1]);
                        }
                    }
                }
                else if(i.getInstruction() instanceof ConstantPushInstruction){
                    ConstantPushInstruction ins = (ConstantPushInstruction) i.getInstruction();
                    if(ins.getValue().intValue() == opCode){
                        return InstructionUtils.getBranchBlock(ih[ih.length-1]);
                    }
                }
            }

        }
        return null;
    }

    /**
     * Gets the bytecode instructions for each parameter
     *
     * @param ih                the InstructionHandle to get the parameters to.
     * @param cpg
     * @param wordsPerParameter the number of words in size each parameter is
     * @return an array of InstructionHandles for each instruction handle.
     *         Lets say we had this bytecode:
     *         iconst_1 iconst_m1 iconst_2 imul iadd
     *         In java would be:
     *         1+(-1*2)
     *         <p/>
     *         Results:
     *         ret[0] = {iconst_m1, iconst_2, imul);
     *         ret[1] = {iconst_1};
     */
    public static InstructionHandle[][] getParameters(InstructionHandle ih, ConstantPoolGen cpg, int wordsPerParameter) {
        Instruction instruction = ih.getInstruction();
        int stackConsumedByIH = instruction.consumeStack(cpg);
        if (stackConsumedByIH == Constants.UNPREDICTABLE) {
            if (instruction instanceof FieldInstruction) {
                FieldInstruction fieldInstruction = (FieldInstruction) instruction;
                if (fieldInstruction.getType(cpg).equals(Type.LONG) || fieldInstruction.getType(cpg).equals(Type.DOUBLE)) {
                    stackConsumedByIH = 2;
                } else {
                    stackConsumedByIH = 1;
                }
            } else if (instruction instanceof InvokeInstruction) {
                InvokeInstruction invokeInstruction = (InvokeInstruction) instruction;
                if (invokeInstruction instanceof INVOKESTATIC) {
                    stackConsumedByIH = invokeInstruction.getSignature(cpg).length();
                } else if (invokeInstruction instanceof INVOKEVIRTUAL
                        || invokeInstruction instanceof INVOKESPECIAL
                        || invokeInstruction instanceof INVOKEINTERFACE) {
                    stackConsumedByIH = invokeInstruction.getSignature(cpg).length();
                    stackConsumedByIH++; // OBJ REF
                }
            } else if (instruction instanceof MULTIANEWARRAY) {
                MULTIANEWARRAY multianewarray = (MULTIANEWARRAY) instruction;
                stackConsumedByIH = multianewarray.getDimensions();
                stackConsumedByIH++; // sizeN
            }
        }
        if (stackConsumedByIH < 0) {
            throw new IllegalArgumentException("Could not figure out how many parameters the instruction takes.");
        }

        int parameterCount = stackConsumedByIH / wordsPerParameter;
        int curParameter = parameterCount;
        List<InstructionHandle[]> paramList = new ArrayList<InstructionHandle[]>();
        InstructionHandle curHandle = ih;
        while (curParameter > 0) {
            List<InstructionHandle> instructionList = new ArrayList<InstructionHandle>();

            int curStack = -wordsPerParameter;

            curHandle = curHandle.getPrev();
            while (curHandle != null) {
                Instruction ins = curHandle.getInstruction();
                if (ins.consumeStack(cpg) != 0) {
                    int stackConsumeWords = ins.consumeStack(cpg);
                    if (stackConsumeWords == Constants.UNPREDICTABLE) {
                        if (ins instanceof FieldInstruction) {
                            FieldInstruction fieldInstruction = (FieldInstruction) ins;
                            if (fieldInstruction.getType(cpg).equals(Type.LONG) || fieldInstruction.getType(cpg).equals(Type.DOUBLE)) {
                                stackConsumeWords = 2;
                            } else {
                                stackConsumeWords = 1;
                            }
                        } else if (ins instanceof InvokeInstruction) {
                            InvokeInstruction invokeInstruction = (InvokeInstruction) ins;
                            if (invokeInstruction instanceof INVOKESTATIC) {
                                stackConsumeWords = invokeInstruction.getSignature(cpg).length();
                            } else if (invokeInstruction instanceof INVOKEVIRTUAL
                                    || invokeInstruction instanceof INVOKESPECIAL
                                    || invokeInstruction instanceof INVOKEINTERFACE) {
                                stackConsumeWords = invokeInstruction.getSignature(cpg).length();
                                stackConsumeWords++; // OBJ REF
                            }
                        } else if (ins instanceof MULTIANEWARRAY) {
                            MULTIANEWARRAY multianewarray = (MULTIANEWARRAY) ins;
                            stackConsumeWords = multianewarray.getDimensions();
                            stackConsumeWords++; // sizeN
                        } else {
                            throw new RuntimeException("Could not figure out stack consumed by instruction.");
                        }
                    }
                    curStack -= stackConsumeWords;
                }
                /*
                Think
                stack -1; // IfInstruction
                stack -= 2; // ixor
                // stack = -3;
                stack++; //ixor
                //stack = -2;
                // check

                stack++ //iconst_m1
                //stack = -1;
                // check

                stack--; // arraylength
                //stack = -2;
                stack++ // arrayLength
                //stack = -1;
                // check

                stack--; // getfield
                //stack = -2;
                stack++; //getfield
                //stack = -1;
                // check
                stack++; //aload
                //stack = 0;
                // check

                // DONE

                 */


                if (ins.produceStack(cpg) != 0) {
                    int stackProduceWords = ins.produceStack(cpg);
                    if (stackProduceWords == Constants.UNPREDICTABLE) {
                        if (ins instanceof FieldInstruction) {
                            FieldInstruction fieldInstruction = (FieldInstruction) ins;
                            if (fieldInstruction.getType(cpg).equals(Type.LONG) || fieldInstruction.getType(cpg).equals(Type.DOUBLE)) {
                                stackProduceWords = 2;
                            } else {
                                stackProduceWords = 1;
                            }
                        } else if (ins instanceof InvokeInstruction) {
                            InvokeInstruction invokeInstruction = (InvokeInstruction) ins;
                            if (invokeInstruction.getReturnType(cpg).equals(Type.LONG) || invokeInstruction.getReturnType(cpg).equals(Type.DOUBLE)) {
                                stackProduceWords = 2;
                            } else {
                                stackProduceWords = 1;
                            }
                        } else {
                            throw new RuntimeException("Could not figure out stack consumed by instruction.");
                        }

                    }
                    curStack += stackProduceWords;

                }

                if(ins instanceof GOTO
                        && curHandle.getNext().getInstruction().equals(InstructionConstants.ICONST_1)
                        && curHandle.getPrev().getInstruction().equals(InstructionConstants.ICONST_0)
                        && curHandle.getPrev().getPrev().getInstruction() instanceof IFEQ
                        && curHandle.getPrev().getPrev().getPrev().getInstruction().produceStack(cpg) != 0){
                    instructionList.add(curHandle);
                    curHandle = curHandle.getPrev();
                    instructionList.add(curHandle);
                    curHandle = curHandle.getPrev();
                    instructionList.add(curHandle);
                    curHandle = curHandle.getPrev();
                }else{
                     instructionList.add(curHandle);
                }

                if (curStack == 0) {
                    break;
                }
                curHandle = curHandle.getPrev();
            }

            List<InstructionHandle> reversedList = new ArrayList<InstructionHandle>();
            for(int i = instructionList.size()-1; i >= 0; i--){
                reversedList.add(instructionList.get(i));
            }
            paramList.add(reversedList.toArray(new InstructionHandle[1]));
            curParameter--;
        }
        return paramList.toArray(new InstructionHandle[1][1]);   
    }
}
