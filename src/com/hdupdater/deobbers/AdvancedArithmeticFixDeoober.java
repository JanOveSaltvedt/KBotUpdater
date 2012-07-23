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

import com.hdupdater.AbstractDeobber;
import com.hdupdater.utils.InstructionUtils;
import com.sun.org.apache.bcel.internal.generic.*;
import com.sun.org.apache.bcel.internal.classfile.Method;
import com.sun.org.apache.bcel.internal.util.InstructionFinder;

import java.util.Iterator;

/**
 * Created by IntelliJ IDEA.
 * User: Jan Ove Saltvedt
 * Date: Sep 30, 2009
 * Time: 7:48:56 PM
 * To change this template use File | Settings | File Templates.
 */
public class AdvancedArithmeticFixDeoober extends AbstractDeobber {
    @Override
    public Class<?>[] getDependencies() {
        return new Class<?>[]{BasicArithmeticFixDeobber.class};
    }

    public void run() {
        int fixedCount = 0;
        for (ClassGen cG : classes) {
            ConstantPoolGen cpg = cG.getConstantPool();
            for (Method m : cG.getMethods()) {
                if (m.isAbstract()) {
                    continue;
                }
                MethodGen methodGen = new MethodGen(m, cG.getClassName(), cpg);
                InstructionList iList = methodGen.getInstructionList();
                if (iList == null) {
                    continue;
                }
                InstructionFinder instructionFinder = new InstructionFinder(iList);


                // Change i = i - -16; to i = i + 16;
                for (Iterator<InstructionHandle[]> iterator = instructionFinder.search("ConstantPushInstruction isub"); iterator.hasNext();) {
                    InstructionHandle[] ih = iterator.next();
                    InstructionHandle arithHandle = ih[1];

                    InstructionHandle pushHandle = ih[0];
                    ConstantPushInstruction pushInstruction = (ConstantPushInstruction) pushHandle.getInstruction();
                    if (pushInstruction.getValue().intValue() < 0) {
                        int newValue = -pushInstruction.getValue().intValue();
                        boolean negateArithmeticIns = false;
                        if (pushInstruction instanceof ICONST) {
                            pushHandle.setInstruction(new ICONST(newValue));
                            negateArithmeticIns = true;
                        } else if (pushInstruction instanceof BIPUSH) {
                            pushHandle.setInstruction(new BIPUSH((byte) newValue));
                            negateArithmeticIns = true;
                        } else if (pushInstruction instanceof SIPUSH) {
                            pushHandle.setInstruction(new SIPUSH((short) newValue));
                            negateArithmeticIns = true;
                        }

                        if (negateArithmeticIns) {
                            fixedCount++;
                            arithHandle.setInstruction(new IADD());
                        }
                    }
                }

                /*
                 Change:
                  i = i + -16; to i = i - 16;
                  */
                for (Iterator<InstructionHandle[]> iterator = instructionFinder.search("ConstantPushInstruction iadd"); iterator.hasNext();) {
                    InstructionHandle[] ih = iterator.next();
                    InstructionHandle arithHandle = ih[1];

                    InstructionHandle pushHandle = ih[0];
                    ConstantPushInstruction pushInstruction = (ConstantPushInstruction) pushHandle.getInstruction();
                    if (pushInstruction.getValue().intValue() < 0) {
                        int newValue = -pushInstruction.getValue().intValue();
                        boolean negateArithmeticIns = false;
                        if (pushInstruction instanceof ICONST) {
                            pushHandle.setInstruction(new ICONST(newValue));
                            negateArithmeticIns = true;
                        } else if (pushInstruction instanceof BIPUSH) {
                            pushHandle.setInstruction(new BIPUSH((byte) newValue));
                            negateArithmeticIns = true;
                        } else if (pushInstruction instanceof SIPUSH) {
                            pushHandle.setInstruction(new SIPUSH((short) newValue));
                            negateArithmeticIns = true;
                        }

                        if (negateArithmeticIns) {
                            fixedCount++;
                            arithHandle.setInstruction(new ISUB());
                        }
                    }


                }

                instructionFinder.reread();
                /**
                 *
                 * fix: i = -1 + i;
                 * make it i = i - 1;
                 *
                 */

                SEARCH_LOOP:
                for (Iterator<InstructionHandle[]> iterator = instructionFinder.search("iadd"); iterator.hasNext();) {
                    InstructionHandle[] ih = iterator.next();
                    InstructionHandle[][] parameters = InstructionUtils.getParameters(ih[0], cpg, 1);
                    if (parameters[1].length != 1) {
                        continue;
                    }
                    if (!(parameters[1][0].getInstruction() instanceof ConstantPushInstruction)) {
                        continue;
                    }
                    ConstantPushInstruction pushInstruction = (ConstantPushInstruction) parameters[1][0].getInstruction();
                    int value = pushInstruction.getValue().intValue();
                    if (value >= 0) {
                        continue;
                    }

                    // now we can switch sides :)
                    InstructionList newIList = new InstructionList();
                    for (InstructionHandle instructionHandle : parameters[0]) {
                        if (instructionHandle.getInstruction() instanceof BranchInstruction
                                || instructionHandle.hasTargeters()) {
                            continue SEARCH_LOOP;
                        }
                        newIList.append(instructionHandle.getInstruction());
                    }
                    int newValue = -value;
                    if (pushInstruction instanceof ICONST) {
                        if (newValue < -1 || newValue > 5) {
                            newIList.append(new BIPUSH((byte) newValue));
                        } else {
                            newIList.append(new ICONST(newValue));
                        }
                    } else if (pushInstruction instanceof BIPUSH) {
                        newIList.append(new BIPUSH((byte) newValue));
                    } else if (pushInstruction instanceof SIPUSH) {
                        newIList.append(new SIPUSH((short) newValue));
                    } else {
                        continue;
                    }

                    // remove old parameters
                    if(parameters[1][0].hasTargeters()){
                        // set that tageter to the new first instruction
                        for(InstructionTargeter targeter: parameters[1][0].getTargeters()){
                            targeter.updateTarget(parameters[1][0], newIList.getStart());
                        }
                    }



                    try {
                        iList.delete(parameters[1][0]);
                        for(InstructionHandle instructionHandle: parameters[0]){
                            iList.delete(instructionHandle);
                        }
                    } catch (TargetLostException e) {
                        e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                    }
                    // negate the arithemetic instruction
                    ih[0].setInstruction(new ISUB());
                    iList.insert(ih[0], newIList);
                    fixedCount++;
                }

                instructionFinder.reread();

                /**
                 *
                 * fix: i = -(someExpression|) + (someExpression1);
                 * make it i = (someExpression2) - (someExpression1);
                 *
                 */
                SEARCH_LOOP:
                for (Iterator<InstructionHandle[]> iterator = instructionFinder.search("iadd"); iterator.hasNext();) {
                    InstructionHandle[] ih = iterator.next();
                    InstructionHandle[][] parameters = InstructionUtils.getParameters(ih[0], cpg, 1);
                    if (!(parameters[1][parameters[1].length-1].getInstruction() instanceof INEG)) {
                        continue;
                    }

                    InstructionHandle inegHandle = parameters[1][parameters[1].length-1];
                    if(inegHandle.hasTargeters()){
                        continue;
                    }

                    // now we can switch sides :)
                    InstructionList newIList = new InstructionList();
                    for (InstructionHandle instructionHandle : parameters[0]) {
                        if (instructionHandle.getInstruction() instanceof BranchInstruction
                                || instructionHandle.hasTargeters()) {
                            continue SEARCH_LOOP;
                        }
                        newIList.append(instructionHandle.getInstruction());
                    }
                    for (int i = 0; i < parameters[1].length-1; i++) {
                        InstructionHandle instructionHandle = parameters[1][i];
                        if (instructionHandle.getInstruction() instanceof BranchInstruction
                                || instructionHandle.hasTargeters()) {
                            continue SEARCH_LOOP;
                        }
                        newIList.append(instructionHandle.getInstruction());
                    }

                    // remove old parameters
                    if(parameters[1][0].hasTargeters()){
                        // set that tageter to the new first instruction
                        for(InstructionTargeter targeter: parameters[1][0].getTargeters()){
                            targeter.updateTarget(parameters[1][0], newIList.getStart());
                        }
                    }



                    try {
                        for(InstructionHandle instructionHandle: parameters[0]){
                            iList.delete(instructionHandle);
                        }
                        for(InstructionHandle instructionHandle: parameters[1]){
                            iList.delete(instructionHandle);
                        }
                    } catch (TargetLostException e) {
                        e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                    }
                    // negate the arithemetic instruction
                    ih[0].setInstruction(new ISUB());
                    iList.insert(ih[0], newIList);
                    fixedCount++;
                }



                methodGen.setInstructionList(iList);
                methodGen.setMaxLocals();
                methodGen.setMaxStack();
                cG.replaceMethod(m, methodGen.getMethod());
            }
        }
        System.out.println("Fixed " + fixedCount + " advanced arithmetic instructions.");
    }
}
