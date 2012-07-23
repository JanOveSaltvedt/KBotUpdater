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
import com.sun.org.apache.bcel.internal.Constants;

import java.util.Iterator;

/**
 * Created by IntelliJ IDEA.
 * User: Jan Ove / Kosaki
 * Date: 08.sep.2009
 * Time: 18:27:44
 */
public class IXORDeobber extends AbstractDeobber {
    public void run() {
        int fixedCount = 0;
        for(ClassGen cG: classes){
            ConstantPoolGen cpg = cG.getConstantPool();
            for(Method m: cG.getMethods()){
                if(m.isAbstract()){
                    continue;
                }
                MethodGen methodGen = new MethodGen(m, cG.getClassName(), cpg);
                InstructionList iList = methodGen.getInstructionList();
                if(iList == null){
                    continue;
                }
                InstructionFinder instructionFinder = new InstructionFinder(iList);
                /*
                Remove:
                if(~(something+something-something) = -14)
                and change to
                if(something+something-something = 13)

                 */
                for(Iterator<InstructionHandle[]> iterator = instructionFinder.search("IfInstruction"); iterator.hasNext();){
                    InstructionHandle[] ih = iterator.next();
                    if(ih[ih.length-1].getInstruction().consumeStack(cpg) != 2){
                        continue;
                    }
                    InstructionHandle[][] parameters = InstructionUtils.getParameters(ih[0], cpg, 1);
                    int ixorCount = 0;
                    for (InstructionHandle[] insList : parameters) {
                        if(insList[insList.length-1].getInstruction() instanceof IXOR){
                            if(insList[insList.length-2].getInstruction().equals(InstructionConstants.ICONST_M1)){
                                ixorCount++;
                            }
                        }
                    }
                    if(ixorCount != 1){
                        continue;
                    }
                    InstructionHandle param = null;
                    int paramIndex = -1;
                    if(parameters[0].length == 1 && parameters[1].length != 1){
                        param = parameters[0][0];
                        paramIndex = 1;
                    }
                    else if(parameters[1].length == 1 && parameters[0].length != 1){
                        param = parameters[1][0];
                        paramIndex = 0;
                    }

                    if(param == null || !(param.getInstruction() instanceof ConstantPushInstruction)){
                        continue;
                    }

                    ConstantPushInstruction pushInstruction = (ConstantPushInstruction) param.getInstruction();
                    int newValue = ~pushInstruction.getValue().intValue();

                    // check if iconst_m1 or ixor has targetters
                    if(parameters[paramIndex][parameters[paramIndex].length-1].hasTargeters()
                            || parameters[paramIndex][parameters[paramIndex].length-2].hasTargeters()){
                        continue;
                    }

                    if(pushInstruction instanceof ICONST){
                        if(newValue < -1 || newValue > 5){
                            param.setInstruction(new BIPUSH((byte) newValue));
                        }
                        else{
                            param.setInstruction(new ICONST(newValue));
                        }
                    }
                    else if(pushInstruction instanceof BIPUSH){
                        param.setInstruction(new BIPUSH((byte) newValue));
                    }
                    else if(pushInstruction instanceof SIPUSH){
                        param.setInstruction(new SIPUSH((short) newValue));
                    }
                    else{
                        continue;
                    }

                    // remove iconst_m1 and ixor
                    try {
                        iList.delete(parameters[paramIndex][parameters[paramIndex].length-1]);
                        iList.delete(parameters[paramIndex][parameters[paramIndex].length-2]);
                        fixedCount++;
                    } catch (TargetLostException e) {
                        e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                    }
                    IfInstruction ifInstruction = (IfInstruction) ih[ih.length-1].getInstruction();
                    if(shallNegate(ifInstruction)){
                        ih[ih.length-1].setInstruction(ifInstruction.negate());
                    }
                }

                instructionFinder.reread();
                // NEW set of deobbers for a more general removal
                /*
                 remove ~(something) == ~(somethingElse)
                  */
                for(Iterator<InstructionHandle[]> iterator = instructionFinder.search("iconst_m1 ixor IfInstruction"); iterator.hasNext();){
                    InstructionHandle[] ih = iterator.next();
                    if(ih[ih.length-1].getInstruction().consumeStack(cpg) != 2){
                        continue;
                    }
                    InstructionHandle[][] parameters = InstructionUtils.getParameters(ih[2], cpg, 1);
                    int ixorCount = 0;
                    for (InstructionHandle[] insList : parameters) {
                        if(insList[insList.length-1].getInstruction() instanceof IXOR){
                            if(insList[insList.length-2].getInstruction().equals(InstructionConstants.ICONST_M1)){
                                ixorCount++;
                            }
                        }
                    }
                    if(ixorCount != 2){
                        continue;
                    }
                    // remove iconst_m1 and ixor

                    // check for targetters
                    int param0Length = parameters[0].length;
                    int param1Length = parameters[1].length;
                    if(parameters[0][param0Length-1].hasTargeters()
                            || parameters[0][param0Length-2].hasTargeters()
                            || parameters[1][param1Length-1].hasTargeters()
                            || parameters[1][param1Length-2].hasTargeters()){
                        continue;
                        // This should not really happen
                    }

                    try {
                        iList.delete(parameters[0][param0Length-1]);
                        iList.delete(parameters[0][param0Length-2]);
                        iList.delete(parameters[1][param1Length-1]);
                        iList.delete(parameters[1][param1Length-2]);
                        fixedCount++;
                    } catch (TargetLostException e) {
                        e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                    }
                    IfInstruction ifInstructions = (IfInstruction) ih[ih.length-1].getInstruction();
                    if(shallNegate(ifInstructions)){
                        ih[ih.length-1].setInstruction(ifInstructions.negate());
                    }
                }

                instructionFinder.reread();
                /*
                ~Class190_Sub4.anInt4954 != 0
                 */
                for(Iterator<InstructionHandle[]> iterator = instructionFinder.search("ICONST_M1 IXOR IFEQ"); iterator.hasNext();){
                    InstructionHandle[] ih = iterator.next();

                    if(ih[0].hasTargeters() || ih[1].hasTargeters() || ih[2].hasTargeters()){
                        continue;
                    }

                    InstructionHandle target = ((BranchInstruction)ih[2].getInstruction()).getTarget();      
                    iList.append(ih[2], new IF_ICMPEQ(target));
                    iList.append(ih[2], InstructionConstants.ICONST_M1);

                    try {
                        iList.delete(ih[0]);
                        iList.delete(ih[1]);
                        iList.delete(ih[2]);
                    } catch (TargetLostException e) {
                        e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                    }
                }

                methodGen.setInstructionList(iList);
                methodGen.setMaxLocals();
                methodGen.setMaxStack();
                cG.replaceMethod(m, methodGen.getMethod());
            }


        }
        System.out.println("Removed "+fixedCount+" ixors.");
    }

    private boolean shallNegate(IfInstruction ifInstruction){
        if(ifInstruction instanceof IF_ICMPGE
                || ifInstruction instanceof IF_ICMPGT
                || ifInstruction instanceof IF_ICMPLE
                || ifInstruction instanceof IF_ICMPLT){
            return true;
        }
        else
            return false;
    }
}
