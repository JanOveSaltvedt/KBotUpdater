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
 * Date: Sep 19, 2009
 * Time: 7:03:37 PM
 * To change this template use File | Settings | File Templates.
 *
 * Game object is the superclass to character and such.
 */
public class GameObjectTransformer extends AbstractTransformer {

    @Override
    public Class<?>[] getDependencies() {
        return new Class<?>[]{CharacterTransformer.class};
    }

    public void run() {
        ClassGen charCG = hookHandler.getClassByNick("Character");
        ClassGen gameObjectCG = hookHandler.classes.get(charCG.getSuperclassName());
        hookHandler.addClassNick("GameObject", gameObjectCG);
    }



    public String[] getFieldHooks() {
        return new String[]{};  //To change body of implemented methods use File | Settings | File Templates.
    }
}
