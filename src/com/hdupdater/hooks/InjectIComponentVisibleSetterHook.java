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

import com.hdupdater.HookHandler;
import com.sun.jmx.mbeanserver.MBeanSupport;
import com.sun.org.apache.bcel.internal.classfile.Method;
import com.sun.org.apache.bcel.internal.generic.ClassGen;
import org.jdom.Document;
import org.jdom.Element;

import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: Jan Ove Saltvedt
 * Date: Jan 11, 2010
 * Time: 7:50:23 PM
 * To change this template use File | Settings | File Templates.
 */
public class InjectIComponentVisibleSetterHook extends Hook implements InjectionHook{
    private ClassGen cG;
    private Method method;
    private String className;
    private String fieldName;
    private int aloadIndex;
    private ClassGen iCompCG;
    private String loopClassName;
    private String loopFieldName;

    public InjectIComponentVisibleSetterHook(HookHandler hookHandler, ClassGen cG, Method m, String className, String fieldName, int aloadIndex, ClassGen iCompCG, String loopClassName, String loopFieldName) {
        this.cG = cG;
        method = m;
        this.className = className;
        this.fieldName = fieldName;
        this.aloadIndex = aloadIndex;
        this.iCompCG = iCompCG;
        this.loopClassName = loopClassName;
        this.loopFieldName = loopFieldName;

        hookHandler.hooks.add(this);
    }

    @Override
    public void applyXMLForReflection(Document doc) {
    }

    @Override
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
        taskNode.addContent(new Element("type").setText("IComponentVisibleHook"));
        Element dataNode = new Element("data");
        taskNode.addContent(dataNode);
        dataNode.addContent(new Element("methodName").setText(method.getName()));
        dataNode.addContent(new Element("methodSignature").setText(method.getSignature()));
        dataNode.addContent(new Element("IComponentClassName").setText(iCompCG.getClassName()));
        dataNode.addContent(new Element("aloadIndex").setText(""+aloadIndex));
        dataNode.addContent(new Element("fieldClassName").setText(className));
        dataNode.addContent(new Element("fieldFieldName").setText(fieldName));
        dataNode.addContent(new Element("loopClassName").setText(loopClassName));
        dataNode.addContent(new Element("loopFieldName").setText(loopFieldName));
    }
}
