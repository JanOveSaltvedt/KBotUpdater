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

import com.sun.org.apache.bcel.internal.generic.*;
import com.sun.org.apache.bcel.internal.classfile.Method;
import com.hdupdater.utils.instructionSearcher.matchers.SimpleInstructionMatcher;

import java.util.Map;
import java.util.HashMap;

/**
 * Created by IntelliJ IDEA.
 * User: Jan Ove Saltvedt
 * Date: Sep 19, 2009
 * Time: 12:27:20 PM
 * To change this template use File | Settings | File Templates.
 */
public class InstructionSearcher {
    private Map<String, Matcher> matchers = new HashMap<String, Matcher>();
    public ClassGen cG;
    public Method method;
    public MethodGen methodGen;
    public InstructionList instructionList;
    private int position;

    public InstructionSearcher(ClassGen cG, Method method) {
        this.cG = cG;
        this.method = method;
        methodGen = new MethodGen(method, cG.getClassName(), cG.getConstantPool());
        instructionList = methodGen.getInstructionList();
        position = 0;
    }

    public void addMatcher(Matcher matcher){
        matcher.searcher = this;
        matcher.cG = cG;
        matcher.cpg = cG.getConstantPool();
        matchers.put(matcher.getName(), matcher);
    }

    public void removeMatcher(String name){
        Matcher matcher = matchers.get(name);
        matcher.searcher = null;
        matcher.cG = null;
        matcher.cpg = null;
        matchers.remove(name);
    }

    public void clearMatchers(){
        for(Matcher matcher: matchers.values()){
            matcher.searcher = null;
            matcher.cG = null;
            matcher.cpg = null;
        }
        matchers.clear();
    }

    public static final int POSITION_START = 0;
    public static final int POSITION_END = 1;
    public void setPosition(int POSITION){
        switch (POSITION){
            case POSITION_START:
                position = 0;
                break;
            case POSITION_END:
                position = instructionList.getLength()-1;
                break;
        }
    }

    public void setPosition(InstructionHandle ih){
        InstructionHandle[] handles = instructionList.getInstructionHandles();
        for(int i = 0; i < handles.length; i++){
            if(handles[i] == ih){
                position = i;
                return;
            }
        }
    }

    /**
     * Sets the instruction list to search in.
     * Also sets positon to start
     * @param instructionList a valid instruction list within the method.
     */
    public void setInstructionList(InstructionList instructionList){
        this.instructionList = instructionList;
        this.position = 0;
    }

    public boolean search(){
        InstructionHandle[] instructionHandles = instructionList.getInstructionHandles();

        for(InstructionHandle handle = instructionHandles[position]; position < instructionHandles.length; handle = instructionHandles[position++]){
            for(Matcher matcher: matchers.values()){
                matcher.matches(handle);
            }
        }
        boolean found = true;
        for(Matcher matcher: matchers.values()){
            if(matcher.matches.isEmpty()){
                found = false;
                break;
            }
        }
        return found;
    }

    public boolean runMatcher(Matcher matcher){
        matcher.searcher = this;
        matcher.cG = cG;
        matcher.cpg = cG.getConstantPool();
        InstructionHandle[] instructionHandles = instructionList.getInstructionHandles();
        for(InstructionHandle handle = instructionHandles[position]; position < instructionHandles.length; handle = instructionHandles[position++]){
            if(matcher.matches(handle))
                return true;
        }
        return false;
    }

    public boolean runMatcherBackWards(Matcher matcher){
        matcher.searcher = this;
        matcher.cG = cG;
        matcher.cpg = cG.getConstantPool();
        InstructionHandle[] instructionHandles = instructionList.getInstructionHandles();
        for(InstructionHandle handle = instructionHandles[position-1]; position < instructionHandles.length; handle = instructionHandles[--position]){
            if(matcher.matches(handle))
                return true;
        }
        return false;
    }

    public boolean gotoNext(Class<? extends Instruction> type){
        InstructionHandle[] instructionHandles = instructionList.getInstructionHandles();
        for(InstructionHandle handle = instructionHandles[position]; position < instructionHandles.length; handle = instructionHandles[++position]){
            if(type.isInstance(handle.getInstruction()))
                return true;
        }
        return false;
    }

    public boolean gotoPrev(Class<? extends Instruction> type){
        InstructionHandle[] instructionHandles = instructionList.getInstructionHandles();
        for(InstructionHandle handle = instructionHandles[position-1]; position >= 0; handle = instructionHandles[--position]){
            if(type.isInstance(handle.getInstruction()))
                return true;
        }
        return false;
    }

    public Matcher getMatcher(String name){
        return matchers.get(name);
    }

    public Matcher getNext(Class<? extends Instruction> aInstruction){
        Matcher matcher = new SimpleInstructionMatcher("Simple Matcher", aInstruction);
        InstructionHandle[] instructionHandles = instructionList.getInstructionHandles();
        for(InstructionHandle handle = instructionHandles[position]; position < instructionHandles.length; handle = instructionHandles[++position]){
            if(matcher.matches(handle)){
                return matcher;
            }
        }
        return null;
    }

    public InstructionHandle getCurrentHandle() {
        return instructionList.getInstructionHandles()[position];
    }
}
