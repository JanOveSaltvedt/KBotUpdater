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
import com.sun.org.apache.bcel.internal.generic.*;
import com.sun.org.apache.bcel.internal.classfile.Method;
import com.sun.org.apache.bcel.internal.util.InstructionFinder;

import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;

/**
 * Created by IntelliJ IDEA.
 * User: Jan Ove / Kosaki
 * Date: 08.sep.2009
 * Time: 18:14:00
 */
public class BasicArithmeticFixDeobber extends AbstractDeobber {
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
                // ints
                for(Iterator<InstructionHandle[]> iterator = instructionFinder.search("ineg iadd"); iterator.hasNext();){
                    InstructionHandle[] ih = iterator.next();
                    if(ih[1].hasTargeters()){
                        continue;
                    }
                    ih[0].setInstruction(new ISUB()); 
                    try {
                        iList.delete(ih[1]);
                        fixedCount++;
                    } catch (TargetLostException e) {
                        e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                    }
                }
                for(Iterator<InstructionHandle[]> iterator = instructionFinder.search("ineg isub"); iterator.hasNext();){
                    InstructionHandle[] ih = iterator.next();
                    if(ih[1].hasTargeters()){
                        continue;
                    }
                    ih[0].setInstruction(new IADD());
                    try {
                        iList.delete(ih[1]);
                        fixedCount++;
                    } catch (TargetLostException e) {
                        e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                    }
                }

                // longs
                for(Iterator<InstructionHandle[]> iterator = instructionFinder.search("lneg ladd"); iterator.hasNext();){
                    InstructionHandle[] ih = iterator.next();
                    if(ih[1].hasTargeters()){
                        continue;
                    }
                    ih[0].setInstruction(new LSUB());
                    try {
                        iList.delete(ih[1]);
                        fixedCount++;
                    } catch (TargetLostException e) {
                        e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                    }
                }
                for(Iterator<InstructionHandle[]> iterator = instructionFinder.search("lneg lsub"); iterator.hasNext();){
                    InstructionHandle[] ih = iterator.next();
                    ih[0].setInstruction(new LADD());
                    try {
                        iList.delete(ih[1]);
                        fixedCount++;
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
        System.out.println("Fixed "+fixedCount+" basic arithmetic instructions.");
    }

    
}
