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
import com.sun.org.apache.bcel.internal.generic.ClassGen;
import com.sun.org.apache.bcel.internal.classfile.Method;
import com.hdupdater.HookHandler;

import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: Jan Ove Saltvedt
 * Date: Oct 17, 2009
 * Time: 7:10:53 PM
 * To change this template use File | Settings | File Templates.
 */
public class UpdateRenderDataHook extends Hook implements InjectionHook{
    private ClassGen cG;
    private Method method;

    public UpdateRenderDataHook(HookHandler hookHandler, ClassGen cG, Method method) {
        this.cG = cG;
        this.method = method;
        hookHandler.hooks.add(this);
    }

    public void applyXMLForReflection(Document doc) {

    }

    public void applyXMLForInjection(Document doc) {
        Element root = doc.getRootElement();
        Element classesNode = root.getChild("classes");
        Element classNode = null;
        for(Element classNode2: (List<Element>)classesNode.getChildren("class")){
            if(classNode2 == null || classNode2.getChild("className") == null){
                continue;
            }
            if(classNode2.getChildText("className").equals(cG.getClassName())){
                classNode = classNode2;
                break;
            }
        }

        if(classNode == null){
            classNode = new Element("class");
            classesNode.addContent(classNode);
            classNode.addContent(new Element("className").setText(cG.getClassName()));
        }

        Element taskNode = new Element("task");
        classNode.addContent(taskNode);
        taskNode.addContent(new Element("type").setText("updateRenderDataCallback"));
        Element dataNode = new Element("data");
        taskNode.addContent(dataNode);
        dataNode.addContent(new Element("methodName").setText(method.getName()));
        dataNode.addContent(new Element("methodSignature").setText(method.getSignature()));
    }
}
