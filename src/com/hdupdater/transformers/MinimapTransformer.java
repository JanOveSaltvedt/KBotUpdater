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
import com.hdupdater.utils.InstructionUtils;
import com.sun.org.apache.bcel.internal.generic.*;
import com.sun.org.apache.bcel.internal.classfile.Method;
import com.sun.org.apache.bcel.internal.util.InstructionFinder;

import java.util.Iterator;

/**
 * Created by IntelliJ IDEA.
 * User: Jan Ove Saltvedt
 * Date: Oct 4, 2009
 * Time: 2:19:01 PM
 * To change this template use File | Settings | File Templates.
 */
public class MinimapTransformer extends AbstractTransformer {
    public void run() {
        for (ClassGen cG : classes) {
            ConstantPoolGen cpg = cG.getConstantPool();
            for (Method method : cG.getMethods()) {
                if (!method.isStatic()) {
                    continue;
                }
                if (!Type.VOID.equals(method.getReturnType())) {
                    continue;
                }
                Type[] args = method.getArgumentTypes();
                if (args.length < 6 || args.length > 8) {
                    continue;
                }

                int intCount = TypeCounter.getIntCount(args) + TypeCounter.getByteCount(args);
                int objCount = TypeCounter.getObjectCount(args);

                if (objCount != 3) {
                    continue;
                }
                if (intCount < 4 || intCount > 6) {
                    continue;
                }


                boolean foundMinimapSetting = false;
                boolean foundCompassAngle = false;
                boolean foundMinimapScale = false;

                InstructionFinder instructionFinder = new InstructionFinder(new InstructionList(method.getCode().getCode()));
                for (Iterator<InstructionHandle[]> iterator = instructionFinder.search("if_icmpne | if_icmpeq"); iterator.hasNext();) {
                    InstructionHandle[] ih = iterator.next();
                    InstructionHandle[][] parameters = InstructionUtils.getParameters(ih[0], cpg, 1);
                    boolean foundConst = false;
                    for (InstructionHandle[] param : parameters) {
                        if (param.length == 1 && param[0].getInstruction() instanceof ConstantPushInstruction) {
                            ConstantPushInstruction pushInstruction = (ConstantPushInstruction) param[0].getInstruction();
                            if (pushInstruction.getValue().intValue() == 4) {
                                foundConst = true;
                            }
                        }
                    }

                    if (!foundConst) {
                        continue;
                    }

                    for (InstructionHandle[] param : parameters) {
                        if (param.length == 1 && param[0].getInstruction() instanceof GETSTATIC) {
                            FieldInstruction fieldInstruction = (FieldInstruction) param[0].getInstruction();
                            if (hookHandler.getFieldHook("Client", "minimapSetting") == null) {
                                hookHandler.addClientHook(fieldInstruction, cpg, "minimapSetting");
                            }
                            foundMinimapSetting = true;
                        }
                    }

                    InstructionHandle[] block = InstructionUtils.getBranchBlock(ih[0]);
                    for (InstructionHandle current : block) {
                        if (current.getInstruction() instanceof GETSTATIC) {
                            FieldInstruction fieldInstruction = (FieldInstruction) current.getInstruction();
                            if (hookHandler.getFieldHook("Client", "compassAngle") == null && fieldInstruction.getFieldType(cpg).equals(Type.FLOAT)) {
                                hookHandler.addClientHook(fieldInstruction, cpg, "compassAngle");
                            }
                            foundCompassAngle = true;
                            break;
                        }
                    }

                }

                for (Iterator<InstructionHandle[]> iterator = instructionFinder.search("getstatic f2i"); iterator.hasNext();) {
                    InstructionHandle[] ih = iterator.next();
                    FieldInstruction fieldInstruction = (FieldInstruction) ih[0].getInstruction();
                    if (hookHandler.getFieldHook("Client", "compassAngle") == null && fieldInstruction.getFieldType(cpg).equals(Type.FLOAT)) {
                        hookHandler.addClientHook(fieldInstruction, cpg, "compassAngle");
                    }
                }


                if (!foundCompassAngle || !foundMinimapSetting) {
                    continue;
                }

                boolean foundConst = false;
                for (Iterator<InstructionHandle[]> iterator = instructionFinder.search("if_icmpeq"); iterator.hasNext();) {
                    InstructionHandle[] ih = iterator.next();
                    InstructionHandle[][] parameters = InstructionUtils.getParameters(ih[0], cpg, 1);

                    for (InstructionHandle[] param : parameters) {
                        if (param.length == 1 && param[0].getInstruction() instanceof ConstantPushInstruction) {
                            ConstantPushInstruction pushInstruction = (ConstantPushInstruction) param[0].getInstruction();
                            if (pushInstruction.getValue().intValue() == 4) {
                                foundConst = true;
                            }
                        }
                    }
                }

                if (!foundConst) {
                    continue;
                }


                for (Iterator<InstructionHandle[]> iterator = instructionFinder.search("ArithmeticInstruction"); iterator.hasNext();) {

                    InstructionHandle[] ih = iterator.next();
                    final Instruction instruction = ih[0].getInstruction();
                    if (!(instruction instanceof IADD) && !(instruction instanceof ISUB)) {
                        continue;
                    }
                    InstructionHandle[][] parameters = InstructionUtils.getParameters(ih[0], cpg, 1);
                    boolean found256 = false;
                    for (InstructionHandle[] param : parameters) {
                        for (InstructionHandle handle : param) {
                            if (handle.getInstruction() instanceof ConstantPushInstruction) {
                                if (((ConstantPushInstruction) handle.getInstruction()).getValue().intValue() == 256) {
                                    found256 = true;
                                }
                            }
                        }
                    }
                    if (found256) {
                        for (InstructionHandle[] param : parameters) {
                            for (InstructionHandle handle : param) {
                                if (handle.getInstruction() instanceof GETSTATIC) {
                                    FieldInstruction fieldInstruction = (FieldInstruction) handle.getInstruction();
                                    if (hookHandler.getFieldHook("Client", "minimapScale") == null && fieldInstruction.getFieldType(cpg).equals(Type.INT)) {
                                        hookHandler.addClientHook(fieldInstruction, cpg, "minimapScale");
                                        foundMinimapScale = true;
                                    }
                                }
                            }
                        }

                    }
                }

                if (!foundMinimapScale) {
                    continue;
                }

                FieldHook compassAngleHook = hookHandler.getFieldHook("Client", "compassAngle");
                for (Iterator<InstructionHandle[]> iterator = instructionFinder.search("iadd"); iterator.hasNext();) {
                    InstructionHandle[] ih = iterator.next();
                    InstructionHandle[][] parameters = InstructionUtils.getParameters(ih[0], cpg, 1);

                    boolean foundCompassAngleInternal = false;
                    for (InstructionHandle[] param : parameters) {
                        if (param.length == 2 && param[0].getInstruction() instanceof GETSTATIC && param[1].getInstruction() instanceof F2I) {
                            FieldInstruction fieldInstruction = (FieldInstruction) param[0].getInstruction();
                            if (fieldInstruction.getClassName(cpg).equals(compassAngleHook.getClassName()) && fieldInstruction.getFieldName(cpg).equals(compassAngleHook.getFieldName())) {
                                foundCompassAngleInternal = true;
                            }
                        }
                    }

                    if (!foundCompassAngleInternal) {
                        continue;
                    }

                    for (InstructionHandle[] param : parameters) {
                        if (param.length == 1 && param[0].getInstruction() instanceof GETSTATIC) {
                            FieldInstruction fieldInstruction = (FieldInstruction) param[0].getInstruction();
                            if (!fieldInstruction.getClassName(cpg).equals(compassAngleHook.getClassName()) && !fieldInstruction.getFieldName(cpg).equals(compassAngleHook.getFieldName())) {
                                if (hookHandler.getFieldHook("Client", "minimapOffset") == null) {
                                    hookHandler.addClientHook(fieldInstruction, cpg, "minimapOffset");
                                }
                            }
                        }
                    }
                }
                if (hookHandler.getFieldHook("Client", "minimapOffset") == null) {
                    for (Iterator<InstructionHandle[]> iterator = instructionFinder.search("iand"); iterator.hasNext();) {
                        InstructionHandle[] ih = iterator.next();
                        InstructionHandle[][] parameters = InstructionUtils.getParameters(ih[0], cpg, 1);
                        boolean isValid = false;
                        for (InstructionHandle[] handles : parameters) {
                            if (handles.length != 1) {
                                continue;
                            }

                            final Instruction instruction = handles[0].getInstruction();
                            if (instruction instanceof ConstantPushInstruction) {
                                if (((ConstantPushInstruction) instruction).getValue().intValue() == 0x3fff) {
                                    isValid = true;
                                }
                            }
                        }

                        if (!isValid) {
                            continue;
                        }

                        for (InstructionHandle[] handles : parameters) {
                            if (handles.length == 1) {
                                continue;
                            }
                            for (InstructionHandle handle : handles) {
                                if (!(handle.getInstruction() instanceof GETSTATIC)) {
                                    continue;
                                }

                                GETSTATIC getstatic = (GETSTATIC) handle.getInstruction();
                                if (getstatic.getFieldType(cpg).equals(Type.INT)) {
                                    hookHandler.addClientHook(getstatic, cpg, "minimapOffset");
                                }
                            }
                        }


                    }
                }
            }
        }
    }

    public String[] getFieldHooks() {
        return new String[]{
                "Client::minimapSetting",
                "Client::minimapScale",
                "Client::minimapOffset",
                "Client::compassAngle",
        };  //To change body of implemented methods use File | Settings | File Templates.
    }
}
