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

import com.sun.org.apache.bcel.internal.generic.Instruction;
import com.sun.org.apache.bcel.internal.generic.InstructionHandle;
import com.hdupdater.utils.instructionSearcher.Matcher;

/**
 * Created by IntelliJ IDEA.
 * User: Jan Ove Saltvedt
 * Date: Sep 19, 2009
 * Time: 12:29:26 PM
 * To change this template use File | Settings | File Templates.
 */
public class SimpleInstructionMatcher extends Matcher {
    private String name;
    private Class<? extends Instruction> instructionClass;
    private int count = 0;

    public SimpleInstructionMatcher(String name, Class<? extends Instruction> instructionClass) {
        this.name = name;
        this.instructionClass = instructionClass;
    }

    public String getName() {
        return name;
    }

    public boolean matches(InstructionHandle i) {
        boolean found = instructionClass.isInstance(i.getInstruction());
        if(!found)
            return false;
        matches.put("Match"+(count++), i);
        return found;
    }
}