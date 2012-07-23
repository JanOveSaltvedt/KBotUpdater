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
import com.hdupdater.utils.InstructionUtils;
import com.hdupdater.utils.TypeBuilder;
import com.hdupdater.utils.TypeCounter;
import com.hdupdater.utils.instructionSearcher.InstructionSearcher;
import com.hdupdater.utils.instructionSearcher.matchers.FieldInstructionMatcher;
import com.sun.org.apache.bcel.internal.classfile.Method;
import com.sun.org.apache.bcel.internal.generic.*;
import com.sun.org.apache.bcel.internal.util.InstructionFinder;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: Jan Ove Saltvedt
 * Date: Sep 27, 2009
 * Time: 7:21:54 PM
 * To change this template use File | Settings | File Templates.
 */
public class LoadingStringTransformer extends AbstractTransformer {

    @Override
    public Class<?>[] getDependencies() {
        return new Class<?>[]{MLStringTransformer.class};
    }

    public void run() {
        ClassGen MLStringCG = hookHandler.getClassByNick("MLString");
        String className = null;
        String fieldName = null;
        for (ClassGen cG : classes) {
            ConstantPoolGen cpg = cG.getConstantPool();
            if (cpg.lookupUtf8("Starting 3D Library") != -1) {
                for (Method method : cG.getMethods()) {
                    if (!method.getName().equals("<clinit>"))
                        continue;
                    InstructionFinder instructionFinder = new InstructionFinder(new InstructionList(method.getCode().getCode()));
                    for (Iterator<InstructionHandle[]> iterator = instructionFinder.search("new dup ldc ldc ldc ldc invokespecial putstatic"); iterator.hasNext();) {
                        InstructionHandle[] ih = iterator.next();
                        NEW newIns = (NEW) ih[0].getInstruction();
                        if (!newIns.getLoadClassType(cpg).toString().equals(MLStringCG.getClassName())) {
                            continue;
                        }

                        LDC ldcEnglish = (LDC) ih[2].getInstruction();
                        if (ldcEnglish.getValue(cpg).equals("Starting 3D Library")) {
                            FieldInstruction fieldInstruction = (FieldInstruction) ih[ih.length - 1].getInstruction();
                            className = fieldInstruction.getClassName(cpg);
                            fieldName = fieldInstruction.getFieldName(cpg);
                        }
                    }
                }
            }
        }
        //Debug.writeLine("" + className + "." + fieldName);
        if (className == null || fieldName == null)
            return;
        for (ClassGen cG : classes) {
            ConstantPoolGen cpg = cG.getConstantPool();
            if (cpg.lookupFieldref(className, fieldName, "L" + MLStringCG.getClassName() + ";") == -1)
                continue;
            for (Method method : cG.getMethods()) {
                if (method.isStatic())
                    continue;
                if (!method.getReturnType().equals(Type.VOID))
                    continue;
                if (method.getArgumentTypes().length > 2) {
                    continue;
                }

                InstructionFinder instructionFinder = new InstructionFinder(new InstructionList(method.getCode().getCode()));
                for (Iterator<InstructionHandle[]> iterator = instructionFinder.search("invokevirtual putstatic"); iterator.hasNext();) {
                    InstructionHandle[] ih = iterator.next();
                    InvokeInstruction invokeInstruction = (InvokeInstruction) ih[0].getInstruction();
                    if(!invokeInstruction.getClassName(cpg).equals(MLStringCG.getClassName()) || !invokeInstruction.getReturnType(cpg).equals(Type.STRING)){
                        continue;
                    }
                    InstructionHandle[][] params = InstructionUtils.getParameters(ih[0], cpg, 1);
                    if(params[params.length-1][0].getInstruction() instanceof GETSTATIC){
                        FieldInstruction fieldInstruction = (FieldInstruction) params[params.length-1][0].getInstruction();
                        if(fieldInstruction.getClassName(cpg).equals(className) && fieldInstruction.getFieldName(cpg).equals(fieldName)){
                            fieldInstruction = (FieldInstruction) ih[1].getInstruction();
                            hookHandler.addClientHook(fieldInstruction, cpg, "loadingString");
                        }
                    }

                }
            }
        }
    }


    public String[] getFieldHooks() {
        return new String[]{
                "Client::loadingString",
        };  //To change body of implemented methods use File | Settings | File Templates.
    }
}