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
import com.hdupdater.hooks.FieldHook;
import com.sun.org.apache.bcel.internal.generic.*;
import com.sun.org.apache.bcel.internal.classfile.Method;
import com.sun.org.apache.bcel.internal.util.InstructionFinder;

import java.util.Iterator;

/**
 * Created by IntelliJ IDEA.
 * User: Jan Ove Saltvedt
 * Date: Nov 28, 2009
 * Time: 11:21:45 AM
 * To change this template use File | Settings | File Templates.
 */
public class GameUITransformer extends AbstractTransformer {
    @Override
    public Class<?>[] getDependencies() {
        return new Class<?>[]{ClassLoopTransformer.class, IComponentTransformer.class};
    }

    public void run() {
        //ClassGen iCompCG = hookHandler.getClassByNick("IComponent");
        //hookIndices(iCompCG);

        //hookMainIndex();
    }

    private void hookMainIndex() {
        FieldHook fieldHook = hookHandler.getFieldHook("Client", "mouseMotionListener");
        for(ClassGen cG: classes){
            ConstantPoolGen cpg = cG.getConstantPool();
            for(Method method: cG.getMethods()){
                if(!method.isStatic()){
                    return;
                }
                /*if(!method.getReturnType().equals(Type.VOID)){
                    return;
                }*/ 
                /*final Type[] args = method.getArgumentTypes();
                if(args.length > 1){
                    continue;
                }
                if(TypeCounter.getObjectCount(args) != 0){
                    continue;
                }*/

                int count = 0;
                InstructionFinder instructionFinder = new InstructionFinder(new InstructionList(method.getCode().getCode()));
                for(Iterator<InstructionHandle[]> iterator = instructionFinder.search("getstatic"); iterator.hasNext();){
                    InstructionHandle[] ih = iterator.next();
                    FieldInstruction fieldInstruction = (FieldInstruction) ih[0].getInstruction();
                    if(fieldInstruction.getClassName(cpg).equals(fieldHook.getClassName()) && fieldInstruction.getFieldName(cpg).equals(fieldHook.getFieldName()) ){
                        count++;
                    }
                }

                if(count != 2){
                    continue;
                }
                System.out.println("HIT");
            }
        }
    }

    private void hookIndices(ClassGen iCompCG) {
        ClassGen cG = hookHandler.classes.get("client");
        FieldHook fieldHook = hookHandler.getFieldHook("Client", "gameState");
        ConstantPoolGen cpg = cG.getConstantPool();
        for(Method method: cG.getMethods()){
            if(method.isStatic() || method.isAbstract()){
                continue;
            }
            Type[] args = method.getArgumentTypes();
            if(args.length > 2){
                continue;
            }
            if(TypeCounter.getObjectCount(args) != 0){
                continue;
            }
            boolean foundString = false;
            InstructionFinder instructionFinder = new InstructionFinder(new InstructionList(method.getCode().getCode()));
            for (Iterator<InstructionHandle[]> iterator = instructionFinder.search("ldc"); iterator.hasNext();) {
                InstructionHandle[] ih = iterator.next();
                LDC ldc = (LDC) ih[0].getInstruction();
                if(ldc.getValue(cpg).equals("1.1")){
                    foundString = true;
                    break;
                }
            }
            if(!foundString){
                continue;
            }
            /*
            if(Class46.aBoolean866 && Class78.gameState == 10 && Class181_Sub32_Sub17_Sub2.ourWantedInt != -1)
            {
                Class46.aBoolean866 = false;
                Class157.method1418(i ^ -6, Class98.aClass112_1496);
                return;
            }

            Example bytecode
            //  479 1157:ifeq            1217
            //  480 1160:getstatic       #2168 <Field int gq.v>
            //  481 1163:bipush          10
            //  482 1165:icmpne          1217
            //  483 1168:getstatic       #2475 <Field int wo.Q>
            //  484 1171:iconst_m1
            //  485 1172:icmpeq          1217

             */
            for (Iterator<InstructionHandle[]> iterator = instructionFinder.search("(IFEQ)|(IF_ICMPNE)|(IF_ICMPEQ)"); iterator.hasNext();) {
                InstructionHandle[] ih = iterator.next();
                ifeqCount = 0;
                icmpneCount = 0;
                icmpeqCount = 0;

                // We are going to find 3 ifs that has the same target
                InstructionHandle ifHandle1 = ih[0];
                IfInstruction ifInstruction1 = (IfInstruction) ifHandle1.getInstruction();
                InstructionHandle ifHandle2 = null;
                IfInstruction ifInstruction2 = null;
                InstructionHandle ifHandle3 = null;
                IfInstruction ifInstruction3 = null;

                if(!verifyIf(ifHandle1, ifInstruction1, cG, fieldHook)){
                    continue;
                }
                InstructionHandle target = ifInstruction1.getTarget();

                // Lets get the next one
                Iterator<InstructionHandle[]> iterator2 = instructionFinder.search("(IFEQ)|(IF_ICMPNE)|(IF_ICMPEQ)", ifHandle1.getNext());
                if(!iterator2.hasNext()){
                    continue;
                }
                ifHandle2 = iterator2.next()[0];
                ifInstruction2 = (IfInstruction) ifHandle2.getInstruction();
                if(!verifyIf(ifHandle2, ifInstruction2, cG, fieldHook)){
                    continue;
                }
                if(!ifInstruction2.getTarget().equals(target)){
                    continue;
                }

                Iterator<InstructionHandle[]> iterator3 = instructionFinder.search("(IFEQ)|(IF_ICMPNE)|(IF_ICMPEQ)", ifHandle2.getNext());
                if(!iterator3.hasNext()){
                    continue;
                }
                ifHandle3 = iterator3.next()[0];
                ifInstruction3 = (IfInstruction) ifHandle3.getInstruction();
                if(!verifyIf(ifHandle3, ifInstruction3, cG, fieldHook)){
                    continue;
                }
                if(!ifInstruction3.getTarget().equals(target)){
                    continue;
                }

                if(icmpeqCount != 1 || ifeqCount != 1 || icmpneCount != 1){
                    continue;
                }

                if(ifInstruction1 instanceof IF_ICMPEQ){
                    hookIndex(ifHandle1, ifInstruction1, cG);
                }
                else if(ifInstruction2 instanceof IF_ICMPEQ){
                    hookIndex(ifHandle2, ifInstruction2, cG);
                }
                else if(ifInstruction3 instanceof IF_ICMPEQ){
                    hookIndex(ifHandle3, ifInstruction3, cG);
                }

            }
        }
    }

    private void hookIndex(InstructionHandle ih, IfInstruction ifInstruction, ClassGen cG) {
        ConstantPoolGen cpg = cG.getConstantPool();
        if(ifInstruction instanceof IF_ICMPEQ){
            InstructionHandle[][] parameters = InstructionUtils.getParameters(ih, cpg, 1);
            if(parameters[0].length != 1){
                return;
            }
            if(parameters[1].length != 1){
                return;
            }
            if(parameters[0][0].getInstruction() instanceof GETSTATIC &&
                    parameters[1][0].getInstruction() instanceof ConstantPushInstruction){
                FieldInstruction fieldInstruction = (FieldInstruction) parameters[0][0].getInstruction();
                if(!fieldInstruction.getFieldType(cpg).equals(Type.INT)){
                    return;
                }
                hookHandler.addClientHook(fieldInstruction, cpg, "mainUIInterfaceIndex");
            }
            else if(parameters[1][0].getInstruction() instanceof GETSTATIC &&
                    parameters[0][0].getInstruction() instanceof ConstantPushInstruction){
                FieldInstruction fieldInstruction = (FieldInstruction) parameters[1][0].getInstruction();
                if(!fieldInstruction.getFieldType(cpg).equals(Type.INT)){
                    return;
                }
                hookHandler.addClientHook(fieldInstruction, cpg, "mainUIInterfaceIndex");
            }
        }
    }

    private int ifeqCount = 0;
    private int icmpneCount = 0;
    private int icmpeqCount = 0;

    private boolean verifyIf(InstructionHandle ih, IfInstruction ifInstruction, ClassGen cG, FieldHook fieldHook) {
        return verifyIF_ICMPNE(ih, ifInstruction, fieldHook, cG) || verifyIF_ICMPEQ(ih, ifInstruction, cG) || verifyIFEQ(ih, ifInstruction);
    }

    private boolean verifyIF_ICMPEQ(InstructionHandle ih, IfInstruction ifInstruction, ClassGen cG) {
        ConstantPoolGen cpg = cG.getConstantPool();
        if(ifInstruction instanceof IF_ICMPEQ){
            InstructionHandle[][] parameters = InstructionUtils.getParameters(ih, cpg, 1);
            if(parameters[0].length != 1){
                return false;
            }
            if(parameters[1].length != 1){
                return false;
            }
            if(parameters[0][0].getInstruction() instanceof GETSTATIC &&
                    parameters[1][0].getInstruction() instanceof ConstantPushInstruction){
                FieldInstruction fieldInstruction = (FieldInstruction) parameters[0][0].getInstruction();
                if(!fieldInstruction.getFieldType(cpg).equals(Type.INT)){
                    return false;
                }
                ConstantPushInstruction pushInstruction = (ConstantPushInstruction) parameters[1][0].getInstruction();
                boolean b = pushInstruction.getValue().intValue() == -1;
                if(b){
                    icmpeqCount++;
                }
                return b;
            }
            else if(parameters[1][0].getInstruction() instanceof GETSTATIC &&
                    parameters[0][0].getInstruction() instanceof ConstantPushInstruction){
                FieldInstruction fieldInstruction = (FieldInstruction) parameters[1][0].getInstruction();
                if(!fieldInstruction.getFieldType(cpg).equals(Type.INT)){
                    return false;
                }
                ConstantPushInstruction pushInstruction = (ConstantPushInstruction) parameters[0][0].getInstruction();
                boolean b = pushInstruction.getValue().intValue() == -1;
                if(b){
                    icmpeqCount++;
                }
                return b;
            }
        }
        return false;
    }

    private boolean verifyIFEQ(InstructionHandle ih, IfInstruction ifInstruction){
        if(ifInstruction instanceof IFEQ){
            if(ih.getPrev().getInstruction() instanceof GETFIELD){
                ifeqCount++;
                return true;
            }
        }
        return false;
    }

    private boolean verifyIF_ICMPNE(InstructionHandle ih, IfInstruction ifInstruction, FieldHook fieldHook, ClassGen cG){
        ConstantPoolGen cpg = cG.getConstantPool();
        if(ifInstruction instanceof IF_ICMPNE){
            InstructionHandle[][] parameters = InstructionUtils.getParameters(ih, cpg, 1);
            if(parameters[0].length != 1){
                return false;
            }
            if(parameters[1].length != 1){
                return false;
            }
            if(parameters[0][0].getInstruction() instanceof GETSTATIC &&
                    parameters[1][0].getInstruction() instanceof ConstantPushInstruction){
                FieldInstruction fieldInstruction = (FieldInstruction) parameters[0][0].getInstruction();
                if(!fieldInstruction.getClassName(cpg).equals(fieldHook.getClassName()) || !fieldInstruction.getFieldName(cpg).equals(fieldHook.getFieldName())){
                    return false;
                }
                ConstantPushInstruction pushInstruction = (ConstantPushInstruction) parameters[1][0].getInstruction();
                boolean b = pushInstruction.getValue().intValue() == 10;
                if(b){
                    icmpneCount++;
                }
                return b;
            }
            else if(parameters[1][0].getInstruction() instanceof GETSTATIC &&
                    parameters[0][0].getInstruction() instanceof ConstantPushInstruction){
                FieldInstruction fieldInstruction = (FieldInstruction) parameters[1][0].getInstruction();
                if(!fieldInstruction.getClassName(cpg).equals(fieldHook.getClassName()) || !fieldInstruction.getFieldName(cpg).equals(fieldHook.getFieldName())){
                    return false;
                }
                ConstantPushInstruction pushInstruction = (ConstantPushInstruction) parameters[0][0].getInstruction();
                boolean b = pushInstruction.getValue().intValue() == 10;
                if(b){
                    icmpneCount++;
                }
                return b;
            }
        }
        return false;
    }

    public String[] getFieldHooks() {
        return new String[]{
                "Client::mainUIInterfaceIndex",
        };
    }
}
