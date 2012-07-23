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

import com.sun.org.apache.bcel.internal.generic.ClassGen;
import com.sun.org.apache.bcel.internal.generic.InstructionList;
import com.sun.org.apache.bcel.internal.generic.InstructionHandle;
import com.sun.org.apache.bcel.internal.generic.SIPUSH;
import com.sun.org.apache.bcel.internal.classfile.Method;
import com.sun.org.apache.bcel.internal.util.InstructionFinder;

import java.util.Iterator;

/**
 * Created by IntelliJ IDEA.
 * User: Jan Ove / Kosaki
 * Date: 27.aug.2009
 * Time: 20:21:50
 */
public class VersionFinder {
    public ClassGen[] classes;

    public VersionFinder(ClassGen[] classes) {
        this.classes = classes;
    }

    public int getVersion(){
        for (ClassGen cG : classes) {
            if (cG.getClassName().equals("client")) {
                for (Method m : cG.getMethods()) {
                    if (!m.getName().equals("main")) {
                        continue;
                    }
                    InstructionList iList = new InstructionList(m.getCode().getCode());
                    InstructionFinder instructionFinder = new InstructionFinder(iList);
                    InstructionFinder.CodeConstraint codeConstraint = new InstructionFinder.CodeConstraint() {
                        public boolean checkCode(InstructionHandle[] match) {
                            int value = ((SIPUSH)match[0].getInstruction()).getValue().intValue();
                            return value >= 550 && value <= 700;
                        }
                    };
                    for(Iterator<InstructionHandle[]> iterator = instructionFinder.search("sipush", codeConstraint); iterator.hasNext();){
                        InstructionHandle[] ih = iterator.next();
                        int value = ((SIPUSH)ih[0].getInstruction()).getValue().intValue();
                        return value;
                    }
                }
            }
        }
        return -1;
    }
}
