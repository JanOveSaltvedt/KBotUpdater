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
import com.hdupdater.utils.instructionSearcher.InstructionSearcher;
import com.hdupdater.utils.instructionSearcher.Matcher;
import com.hdupdater.utils.TypeBuilder;
import com.sun.org.apache.bcel.internal.generic.*;
import com.sun.org.apache.bcel.internal.classfile.Field;
import com.sun.org.apache.bcel.internal.classfile.Method;
import com.sun.org.apache.bcel.internal.Constants;
import com.sun.org.apache.bcel.internal.util.InstructionFinder;

import java.awt.*;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.KeyListener;
import java.util.Iterator;

/**
 * Created by IntelliJ IDEA.
 * User: Jan Ove / Kosaki
 * Date: 27.aug.2009
 * Time: 20:42:27
 */
public class ClassLoopTransformer extends AbstractTransformer {

    @Override
    public Class<?>[] getDependencies() {
        return new Class<?>[]{CharacterTransformer.class, MouseTransformer.class, IComponentTransformer.class, RendererTransformer.class, RenderVarsTransformer.class};
    }

    public void run() {
        ClassGen iCompCG = hookHandler.getClassByNick("IComponent");
        ClassGen playerCG = hookHandler.getClassByNick("Player");
        ClassGen npcCG = hookHandler.getClassByNick("NPC");
        ClassGen mouseCG = hookHandler.getClassByNick("Mouse");
        ClassGen keyCG = hookHandler.getClassByNick("Keyboard");
        ClassGen renderVarsCG = hookHandler.getClassByNick("Renderer");
        for (ClassGen cG : classes) {
            if (cG.getClassName().equals("client")) {
                hookHandler.addClassNick("Client", cG);
                Type type = Type.getType("Lcom/kbotpro/interfaces/ClientCallback;");
                hookHandler.injectField(cG, "callback", type, Constants.ACC_STATIC);
                hookHandler.addSetter(cG, "callback", type, Constants.ACC_STATIC, "Lcom/kbotpro/interfaces/ClientCallback;", Constants.ACC_PUBLIC);
            }
            ConstantPoolGen cpg = cG.getConstantPool();
            // Field loop
            for (Field field : cG.getFields()) {
                // Array types
                if (field.getType() instanceof ArrayType) {
                    if (field.isStatic() && field.getType().getSignature().equals("[L" + playerCG.getClassName() + ";")) {
                        hookHandler.addClientHook(cG, field, "players", TypeBuilder.createHookArrayType("Player", 1));
                    }/* else if (field.isStatic() && field.getType().getSignature().equals("[L" + npcCG.getClassName() + ";")) {
                        hookHandler.addClientHook(cG, field, "npcs", TypeBuilder.createHookArrayType("NPC", 1));
                    } *//*else if (field.isStatic() && field.getType().getSignature().equals("[[L" + iCompCG.getClassName() + ";")) {
                        hookHandler.addClientHook(cG, field, "IComponentArray", TypeBuilder.createHookArrayType("IComponent", 2));
                    } */
                }
                // Regulars
                else {
                    if (field.isStatic() && field.getType().equals(Type.getType(Canvas.class))) {
                        hookHandler.addClientHook(cG, field, "canvas");
                    }
                    if (field.isStatic() && field.getType().toString().equals(mouseCG.getSuperclassName())) {
                        hookHandler.addClientHook(cG, field, "mouse", TypeBuilder.createHookType("Mouse"));
                        hookHandler.addClientHook(cG, field, "mouseListener", Type.getType(MouseListener.class));
                        hookHandler.addClientHook(cG, field, "mouseMotionListener", Type.getType(MouseMotionListener.class));
                    }
                    if (field.isStatic() && field.getType().toString().equals(keyCG.getSuperclassName())) {
                        hookHandler.addClientHook(cG, field, "keyListener", Type.getType(KeyListener.class));
                    }
                    //if(field.isStatic() && field.getType().toString().equals(renderVarsCG.getClassName())){
                    //    System.err.println("Class: "+cG.getClassName()+" Field: "+field.toString());
                    //}

                }
            }

            // Method loop
            for (Method method : cG.getMethods()) {
                if (method.isStatic()) {
                    // validInterfaceArray
                    {
                        Type[] args = method.getArgumentTypes();
                        int objectCount = 0;
                        Type objectType = null;
                        int matches = 0;
                        for (Type arg : args) {
                            if (arg instanceof ObjectType) {
                                if (objectCount == 0)
                                    objectType = arg;
                                objectCount++;
                                if (arg.equals(objectType)) {
                                    matches++;
                                }
                            }
                        }
                        if (objectCount == 4 && matches == 4) {
                            InstructionList iList = new InstructionList(method.getCode().getCode());
                            InstructionFinder instructionFinder = new InstructionFinder(iList);
                            FieldInstruction lastFieldIns = null;
                            for (Iterator<InstructionHandle[]> iterator = instructionFinder.search("putstatic"); iterator.hasNext();) {
                                InstructionHandle[] ih = iterator.next();
                                lastFieldIns = (FieldInstruction) ih[0].getInstruction();
                                if (lastFieldIns.getFieldType(cpg).getSignature().equals("[Z")) {
                                    hookHandler.addClientHook(lastFieldIns, cpg, "validInterfaceArray");
                                }
                            }
                            if (lastFieldIns == null) {
                                continue;
                            }

                        }
                    }
                } else {
                    // game state
                    {
                        if (cG.getClassName().equals("client") && method.getReturnType().equals(Type.VOID)) {
                            InstructionSearcher searcher = new InstructionSearcher(cG, method);
                            Matcher matcher = new Matcher() {
                                @Override
                                public String getName() {
                                    return "Matcher";
                                }

                                @Override
                                public boolean matches(InstructionHandle i) {
                                    if (!(i.getInstruction() instanceof LDC))
                                        return false;
                                    LDC ldc = (LDC) i.getInstruction();
                                    if (ldc.getValue(cpg).equals("js5connect_outofdate")) {
                                        for (InstructionHandle current = i; current != null; current = current.getNext()) {
                                            if (current.getInstruction() instanceof PUTSTATIC) {
                                                PUTSTATIC putstatic = (PUTSTATIC) current.getInstruction();
                                                hookHandler.addClientHook(putstatic, cpg, "gameState");
                                                return true;
                                            }
                                        }
                                    }
                                    return false;  //To change body of implemented methods use File | Settings | File Templates.
                                }
                            };
                            searcher.runMatcher(matcher);
                        }
                    }
                }
            }
        }
    }

    public String[] getFieldHooks() {
        return new String[]{
                "Client::players",
                //"Client::npcs",
                "Client::mouse",
                "Client::mouseListener",
                "Client::mouseMotionListener",
                "Client::keyListener",
                "Client::canvas",
                "Client::gameState",
                //"Client::IComponentArray",
                "Client::validInterfaceArray"
        };  //To change body of implemented methods use File | Settings | File Templates.
    }
}
