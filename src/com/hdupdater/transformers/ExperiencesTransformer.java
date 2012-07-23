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
import com.sun.org.apache.bcel.internal.generic.*;
import com.sun.org.apache.bcel.internal.classfile.Method;
import com.sun.org.apache.bcel.internal.classfile.ExceptionTable;
import com.sun.org.apache.bcel.internal.util.InstructionFinder;

import java.util.Iterator;

/**
 * Created by IntelliJ IDEA.
 * User: Jan Ove Saltvedt
 * Date: Oct 4, 2009
 * Time: 12:16:27 PM
 * To change this template use File | Settings | File Templates.
 */
public class ExperiencesTransformer extends AbstractTransformer {
    public void run() {
        for (ClassGen cG : classes) {
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
                    InstructionFinder instructionFinder = new InstructionFinder(new InstructionList(m.getCode().getCode()));
                    String pattern = "getstatic iload iload iastore " +
                            "getstatic iload iload iastore " +
                            "getstatic iload iconst iastore";
                    for (Iterator<InstructionHandle[]> iterator = instructionFinder.search(pattern); iterator.hasNext();) {
                        InstructionHandle[] ih = iterator.next();
                        FieldInstruction fieldInstruction = (FieldInstruction) ih[0].getInstruction();
                        hookHandler.addClientHook(fieldInstruction, cpg, "experiences");
                        fieldInstruction = (FieldInstruction) ih[4].getInstruction();
                        hookHandler.addClientHook(fieldInstruction, cpg, "levels");
                        fieldInstruction = (FieldInstruction) ih[8].getInstruction();
                        hookHandler.addClientHook(fieldInstruction, cpg, "maxLevels");
                        Iterator<InstructionHandle[]> iterator2 = instructionFinder.search("getstatic iload iaload", ih[ih.length-1]);
                        if(iterator2.hasNext()){
                            ih = iterator2.next();
                            fieldInstruction = (FieldInstruction) ih[0].getInstruction();
                            hookHandler.addClientHook(fieldInstruction, cpg, "maxExperiences");
                        }
                    }
                }
            }
        }
    }

    public String[] getFieldHooks() {
        return new String[]{
                "Client::maxExperiences",
                "Client::experiences",
                "Client::maxLevels",
                "Client::levels",
        };
    }
}
