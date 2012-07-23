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



package com.hdupdater.hooks;

import org.jdom.Document;
import org.jdom.Element;

import java.util.List;

import com.hdupdater.Constants;
import com.hdupdater.HookHandler;
import com.sun.org.apache.bcel.internal.generic.ClassGen;

/**
 * Created by IntelliJ IDEA.
 * User: Jan Ove Saltvedt
 * Date: Oct 18, 2009
 * Time: 3:24:22 PM
 * To change this template use File | Settings | File Templates.
 */
public class InterfaceHook extends Hook implements InjectionHook {
    private ClassGen cG;
    private String name;

    public InterfaceHook(HookHandler hookHandler, ClassGen cG, String name) {
        this.cG = cG;
        this.name = name;
        hookHandler.hooks.add(this);
    }

    public void applyXMLForReflection(Document doc) {

    }

    public void applyXMLForInjection(Document doc) {
        Element root = doc.getRootElement();
        Element classesNode = root.getChild("classes");
        Element classNode = null;
        for (Element classNode2 : (List<Element>) classesNode.getChildren("class")) {
            if (classNode2 == null || classNode2.getChild("className") == null) {
                continue;
            }
            if (classNode2.getChildText("className").equals(cG.getClassName())) {
                classNode = classNode2;
                break;
            }
        }

        if (classNode == null) {
            classNode = new Element("class");
            classesNode.addContent(classNode);
            classNode.addContent(new Element("className").setText(cG.getClassName()));
        }

        Element taskNode = new Element("task");
        classNode.addContent(taskNode);
        taskNode.addContent(new Element("type").setText("injectInterface"));
        Element dataNode = new Element("data");
        taskNode.addContent(dataNode);
        dataNode.addContent(new Element("interfaceClassName").setText(Constants.DIRECTORY_HOOK + "/" + name));
    }
}
