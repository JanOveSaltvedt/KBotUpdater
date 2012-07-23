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

import com.sun.org.apache.bcel.internal.classfile.Method;
import com.sun.org.apache.bcel.internal.generic.*;
import com.sun.org.apache.bcel.internal.util.InstructionFinder;
import com.hdupdater.AbstractTransformer;
import com.hdupdater.utils.InstructionUtils;
import com.hdupdater.utils.TypeCounter;
import com.hdupdater.hooks.FieldHook;

import java.util.Iterator;
import java.awt.event.MouseEvent;

/**
 * Created by IntelliJ IDEA.
 * User: Jan Ove / Kosaki
 * Date: 21.mar.2009
 * Time: 22:29:46
 */
public class KeyboardTransformer extends AbstractTransformer {

    public void run() {
        for (ClassGen cG : classes) {
            ConstantPoolGen cpg = cG.getConstantPool();
            String[] interfaces = cG.getInterfaceNames();
            if (interfaces.length < 1)
                continue;
            boolean foundInterface = false;
            for (String inter : interfaces) {
                if (inter.contains("KeyListener")) {
                    foundInterface = true;
                }
            }
            if (!foundInterface)
                continue;
            hookHandler.addClassNick("Keyboard", cG);
            return;
        }
    }

    public String[] getFieldHooks() {
        return null;
    }
}