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



package com.hdupdater.utils.instructionSearcher.matchers;

import com.sun.org.apache.bcel.internal.generic.InstructionHandle;
import com.sun.org.apache.bcel.internal.generic.FieldInstruction;
import com.sun.org.apache.bcel.internal.generic.GETSTATIC;
import com.hdupdater.utils.instructionSearcher.Matcher;

/**
 * Created by IntelliJ IDEA.
 * User: Jan Ove Saltvedt
 * Date: Sep 19, 2009
 * Time: 12:29:04 PM
 * To change this template use File | Settings | File Templates.
 */
public class FieldInstructionMatcher extends Matcher {
    private String name;
    private boolean GETSTATIC;
    private String className;
    private String fieldName;
    private int count = 0;

    public FieldInstructionMatcher(String name, String className, String fieldName, boolean GETSTATIC) {
        this.name = name;
        this.GETSTATIC = GETSTATIC;
        this.className = className;
        this.fieldName = fieldName;
    }

    public String getName() {
        return name;
    }

    public boolean matches(InstructionHandle i) {
        if(i.getInstruction() instanceof FieldInstruction){

            FieldInstruction fieldInstruction = (FieldInstruction) i.getInstruction();
            if(GETSTATIC){
                if(!(fieldInstruction instanceof GETSTATIC)){
                    return false;
                }
            }
            if(fieldInstruction.getClassName(cpg).equals(className) &&
                    fieldInstruction.getFieldName(cpg).equals(fieldName)){
                matches.put("match"+(count++), i);
                return true;
            }
        }
        return false;
    }
}
