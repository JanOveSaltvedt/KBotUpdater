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
import com.hdupdater.utils.TypeCounter;
import com.hdupdater.utils.InstructionUtils;
import com.sun.org.apache.bcel.internal.generic.*;
import com.sun.org.apache.bcel.internal.classfile.Field;
import com.sun.org.apache.bcel.internal.classfile.Method;
import com.sun.org.apache.bcel.internal.util.InstructionFinder;

import java.util.Iterator;

/**
 * Created by IntelliJ IDEA.
 * User: Jan Ove / Kosaki
 * Date: 28.aug.2009
 * Time: 17:05:54
 */
public class NPCDefTransformer extends AbstractTransformer {
    @Override
    public Class<?>[] getDependencies() {
        return new Class<?>[]{NPCTransformer.class};
    }

    public void run() {
        ClassGen cG = hookHandler.getClassByNick("NPCDef");
        hookNameAndActions(cG);
        hookID(cG);
    }

    private void hookNameAndActions(ClassGen cG) {
        for (Field f : cG.getFields()) {
            if (!f.isStatic()) {
                if (f.getType().getSignature().equals("Ljava/lang/String;")) {
                    hookHandler.addFieldHook("NPCDef", cG.getClassName(), f.getName(), "name");
                }
                if (f.getType().getSignature().equals("[Ljava/lang/String;")) {
                    hookHandler.addFieldHook("NPCDef", cG.getClassName(), f.getName(), "actions");
                }
            }
        }
    }

    private void hookID(ClassGen cG) {
        ConstantPoolGen cpg = cG.getConstantPool();
        for (Method m : cG.getMethods()) {
            Type[] argTypes = m.getArgumentTypes();
            if (m.isStatic() || argTypes.length < 8 || argTypes.length > 9) {
                continue;
            }
            int objTypes = TypeCounter.getObjectCount(argTypes);
            if (objTypes != 4) {
                continue;
            }

            InstructionFinder instructionFinder = new InstructionFinder(new InstructionList(m.getCode().getCode()));
            for (Iterator<InstructionHandle[]> iterator = instructionFinder.search("ior"); iterator.hasNext();) {
                InstructionHandle[] ih = iterator.next();
                InstructionHandle[][] parameters = InstructionUtils.getParameters(ih[0], cpg, 1);
                // anInt2721 | class34.anInt484 << 16
                if (parameters[0].length == 4
                        && parameters[1].length == 2
                        && parameters[1][1].getInstruction() instanceof GETFIELD) {
                    FieldInstruction fieldInstruction = (FieldInstruction) parameters[1][1].getInstruction();
                    if(hookHandler.getFieldHook("NPCDef", "ID") == null){
                        hookHandler.addFieldHook("NPCDef", fieldInstruction, cpg, "ID");
                    }
                } else if (parameters[1].length == 4
                        && parameters[0].length == 2
                        && parameters[0][1].getInstruction() instanceof GETFIELD) {
                    FieldInstruction fieldInstruction = (FieldInstruction) parameters[0][1].getInstruction();
                    if(hookHandler.getFieldHook("NPCDef", "ID") == null){
                        hookHandler.addFieldHook("NPCDef", fieldInstruction, cpg, "ID");
                    }
                }
            }

        }


    }

    public String[] getFieldHooks() {
        return new String[]{
                "NPCDef::name",
                "NPCDef::actions",
                "NPCDef::ID",
        };  //To change body of implemented methods use File | Settings | File Templates.
    }
}
