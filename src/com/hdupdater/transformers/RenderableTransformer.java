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
import com.sun.org.apache.bcel.internal.generic.ClassGen;

/**
 * Created by IntelliJ IDEA.
 * User: Jan Ove / Kosaki
 * Date: 28.aug.2009
 * Time: 16:36:26
 */
public class RenderableTransformer extends AbstractTransformer {
    @Override
    public Class<?>[] getDependencies() {
        return new Class<?>[]{GameObjectTransformer.class};
    }

    public void run() {
        ClassGen gameObjectCG = hookHandler.getClassByNick("GameObject");
        hookHandler.addClassNick("Renderable", gameObjectCG.getSuperclassName());
    }

    public String[] getFieldHooks() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }
}
