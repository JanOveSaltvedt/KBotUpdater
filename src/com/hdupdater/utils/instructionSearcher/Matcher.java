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



package com.hdupdater.utils.instructionSearcher;

import com.sun.org.apache.bcel.internal.generic.ClassGen;
import com.sun.org.apache.bcel.internal.generic.ConstantPoolGen;
import com.sun.org.apache.bcel.internal.generic.InstructionHandle;

import java.util.Map;
import java.util.HashMap;

/**
 * Created by IntelliJ IDEA.
 * User: Jan Ove Saltvedt
 * Date: Sep 19, 2009
 * Time: 12:28:28 PM
 * To change this template use File | Settings | File Templates.
 */
public abstract class Matcher {
    public InstructionSearcher searcher;
    public ClassGen cG;
    public ConstantPoolGen cpg;
    public Map<String, InstructionHandle> matches = new HashMap<String, InstructionHandle>();

    public abstract String getName();
    public abstract boolean matches(InstructionHandle i);

    public InstructionHandle[] getMatches(){
        return (InstructionHandle[]) matches.values().toArray(new InstructionHandle[1]);
    }

    public InstructionHandle getFirstMatch(){
        InstructionHandle[] matches = getMatches();
        if(matches == null || matches.length == 0 )
            return null;
        return matches[0];
    }
}
