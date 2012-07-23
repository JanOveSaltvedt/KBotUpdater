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
import com.hdupdater.hooks.FieldHook;
import com.hdupdater.utils.TypeCounter;
import com.sun.org.apache.bcel.internal.classfile.Method;
import com.sun.org.apache.bcel.internal.generic.*;
import com.sun.org.apache.bcel.internal.util.InstructionFinder;

import java.util.Iterator;

/**
 * Created by IntelliJ IDEA.
 * User: Jan Ove Saltvedt
 * Date: Sep 19, 2009
 * Time: 12:26:11 AM
 * To change this template use File | Settings | File Templates.
 */
public class CameraTransformer extends AbstractTransformer {
    @Override
    public Class<?>[] getDependencies() {
        return new Class<?>[]{CharacterTransformer.class};
    }

    public void run() {
        FieldHook baseY = hookHandler.getFieldHook("Client", "baseY");
        for (ClassGen cG : classes) {
            ConstantPoolGen cpg = cG.getConstantPool();
            if (cpg.lookupUtf8("getcamerapos") == -1) {
                continue;
            }
            for (Method method : cG.getMethods()) {
                if (!method.isStatic()) {
                    continue;
                }
                if (!method.getReturnType().equals(Type.VOID)) {
                    continue;
                }
                if (TypeCounter.getCount(method.getArgumentTypes(), Type.STRING) != 1) {
                    continue;
                }

                boolean foundGetCameraPos = false;
                InstructionFinder instructionFinder = new InstructionFinder(new InstructionList(method.getCode().getCode()));
                for (Iterator<InstructionHandle[]> ldcIterator = instructionFinder.search("ldc"); ldcIterator.hasNext();) {
                    LDC ldc = (LDC) ldcIterator.next()[0].getInstruction();
                    if (ldc.getValue(cpg).equals("getcamerapos")) {
                        foundGetCameraPos = true;
                    }
                }

                if (!foundGetCameraPos) {
                    continue;
                }

                // currentPlane
                for (Iterator<InstructionHandle[]> ldcIterator = instructionFinder.search("ldc"); ldcIterator.hasNext();) {
                    InstructionHandle[] firstHandleArray = ldcIterator.next();
                    LDC ldc = (LDC) firstHandleArray[0].getInstruction();
                    if (ldc.getValue(cpg).equals("getcamerapos")) {
                        for (Iterator<InstructionHandle[]> ldcIterator2 = instructionFinder.search("ldc", firstHandleArray[0]); ldcIterator2.hasNext();) {
                            InstructionHandle[] secondHandleArray = ldcIterator2.next();
                            ldc = (LDC) secondHandleArray[0].getInstruction();
                            if (ldc.getValue(cpg).equals(",")) {
                                Iterator<InstructionHandle[]> fieldIterator = instructionFinder.search("getstatic", secondHandleArray[0]);
                                InstructionHandle[] ih = fieldIterator.next();
                                FieldInstruction fieldInstruction = (FieldInstruction) ih[0].getInstruction();
                                if(fieldInstruction.getClassName(cpg).equals(baseY.getClassName()) && fieldInstruction.getFieldName(cpg).equals(baseY.getFieldName())){
                                    continue;
                                }
                                hookHandler.addClientHook(fieldInstruction, cpg, "cameraZ");
                                break;
                            }

                        }
                    }

                }


            }
        }
        /*for (ClassGen cG : classes) {
            ConstantPoolGen cPool = cG.getConstantPool();
            if (cPool.lookupDouble(2607.5945876176133D) != -1) {
                for (Method m : cG.getMethods()) {
                    if (!m.isStatic())
                        continue;
                    Type[] args = m.getArgumentTypes();
                    if (args.length < 5 || args.length > 6)
                        continue;
                    int intCount = 0;
                    for (Type arg : args) {
                        if (arg == Type.INT || arg == Type.BYTE || arg == Type.BOOLEAN) {
                            intCount++;
                        }
                    }
                    if (intCount < 5 || intCount > 6)
                        continue;


                    // We should have a potential method now.
                    // The method should contain SIPUSHes
                    {
                        boolean found1024 = false;
                        boolean found3072 = false;
                        InstructionFinder instructionFinder = new InstructionFinder(new InstructionList(m.getCode().getCode()));
                        for(Iterator<InstructionHandle[]> iterator = instructionFinder.search("sipush"); iterator.hasNext();){
                            SIPUSH sipush = (SIPUSH) iterator.next()[0].getInstruction();
                            if (sipush.getValue().intValue() == 1024) {
                                found1024 = true;
                            }
                            if (sipush.getValue().intValue() == 3072) {
                                found3072 = true;
                            }
                        }
                        if (!found1024 || !found3072)
                            continue;
                    }

                    //Debug.writeLine("Found in "+cG.getClassName());
                    String pattern = "(IfInstruction)(Instruction)+?(INVOKESTATIC)(GETSTATIC)?(ArithmeticInstruction)+(ISTORE)";
                    InstructionFinder instructionFinder = new InstructionFinder(new InstructionList(m.getCode().getCode()));
                    int z_local_index = -1;
                    for (Iterator<InstructionHandle[]> it = instructionFinder.search(pattern); it.hasNext();) {
                        InstructionHandle[] ihs = it.next();
                        for(InstructionHandle instructionHandle: ihs){
                            if(instructionHandle.getInstruction() instanceof ISTORE){
                                z_local_index = ((ISTORE)instructionHandle.getInstruction()).getIndex();
                            }
                        }
                    }

                    Instruction[][] buff = new Instruction[3][2];
                    pattern = "(ILOAD)?(GETSTATIC)(INEG)?(ILOAD)?(ArithmeticInstruction)+(ISTORE)";
                    int count = 0;
                    for (Iterator<InstructionHandle[]> it = instructionFinder.search(pattern); it.hasNext();) {
                        InstructionHandle[] ihs = it.next();
                        boolean correctIload = false;
                        GETSTATIC getIns = null;
                        ISTORE istore = null;
                        boolean foundILOAD = false;
                        for(InstructionHandle instructionHandle: ihs){
                            if(instructionHandle.getInstruction() instanceof ILOAD){
                                if(((ILOAD)instructionHandle.getInstruction()).getIndex() == z_local_index)
                                    correctIload = true;
                                foundILOAD = true;
                            }
                            else if(instructionHandle.getInstruction() instanceof ISTORE){
                                istore = (ISTORE) instructionHandle.getInstruction();
                            }
                            else if(instructionHandle.getInstruction() instanceof GETSTATIC){
                                getIns = (GETSTATIC) instructionHandle.getInstruction();
                            }
                        }
                        if(!foundILOAD){
                            continue;
                        }
                        //Debug.writeLine(""+getIns.getClassName(cPool)+"."+getIns.getFieldName(cPool));
                        buff[count][0] = getIns;
                        buff[count][1] = istore;
                        if(correctIload){
                            hookHandler.addClientHook(getIns, cPool, "cameraY");
                        }
                        count++;
                    }

                    pattern = "(ILOAD)(I2D)(ILOAD)(I2D)(INVOKESTATIC)";
                    count = 0;
                    for (Iterator<InstructionHandle[]> it = instructionFinder.search(pattern); it.hasNext();) {
                        InstructionHandle[] ihs = it.next();
                        if(!((INVOKESTATIC)ihs[4].getInstruction()).getMethodName(cPool).contains("atan2")){
                            continue;
                        }
                        if(count == 1){
                            for(Instruction[] i: buff){
                                ISTORE istore = (ISTORE) i[1];
                                GETSTATIC getstatic = (GETSTATIC) i[0];
                                if(istore.getIndex() == ((ILOAD)ihs[0].getInstruction()).getIndex()){
                                    hookHandler.addClientHook(getstatic, cPool, "cameraX");
                                }
                                if(istore.getIndex() == ((ILOAD)ihs[2].getInstruction()).getIndex()){
                                    hookHandler.addClientHook(getstatic, cPool, "cameraZ");
                                }
                            }
                        }
                        count++;
                        // Only the first hit.
                    }
                    InstructionSearcher aSearcher = new InstructionSearcher(cG, m);
                    Matcher matcher = new Matcher() {
                        @Override
                        public String getName() {
                            return "Find storages";
                        }

                        int count = 0;

                        @Override
                        public boolean matches(InstructionHandle i) {
                            Instruction instruction = i.getInstruction();
                            if(instruction instanceof INVOKESTATIC){
                                INVOKESTATIC invokestatic = (INVOKESTATIC) instruction;
                                if(invokestatic.getMethodName(cpg).contains("atan2")){
                                    for(InstructionHandle ih = i; ih != null; ih = ih.getNext()){
                                        if(ih.getInstruction() instanceof PUTSTATIC){
                                            matches.put("match"+(++count), ih);
                                            return true;
                                        }
                                    }
                                }
                            }
                            return false;  //To change body of implemented methods use File | Settings | File Templates.
                        }
                    };
                    aSearcher.addMatcher(matcher);
                    aSearcher.search();
                    FieldInstruction fieldInstruction = (FieldInstruction) matcher.getMatches()[0].getInstruction();
                    hookHandler.addClientHook(fieldInstruction, cPool, "cameraCurveY");
                    fieldInstruction = (FieldInstruction) matcher.getMatches()[1].getInstruction();
                    hookHandler.addClientHook(fieldInstruction, cPool, "cameraCurveX");

                }
            }
        }*/
    }

    public String[] getFieldHooks() {
        return new String[]{
                /*"Client::cameraX",
                "Client::cameraY",*/
                "Client::cameraZ",
                /*"Client::cameraCurveX",
                "Client::cameraCurveY",*/
        };  //To change body of implemented methods use File | Settings | File Templates.
    }
}
