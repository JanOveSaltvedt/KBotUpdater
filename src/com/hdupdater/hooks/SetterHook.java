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

import com.sun.org.apache.bcel.internal.generic.ClassGen;
import com.hdupdater.HookHandler;

/**
 * Created by IntelliJ IDEA.
 * User: Jan Ove Saltvedt
 * Date: Oct 17, 2009
 * Time: 3:33:46 PM
 * To change this template use File | Settings | File Templates.
 */
public class SetterHook extends Hook implements InjectionHook {
    private ClassGen injectCG;
    private String fieldName;
    private String fieldSignature;
    private int fieldAccessFlags;
    private int methodAccessFlags;
    private String methodName;
    private String argumentsSignature;

    public SetterHook(HookHandler hookHandler, ClassGen injectCG, String fieldName, String fieldSignature, int fieldAccessFlags, int methodAccessFlags, String methodName, String methodParameters) {
        this.injectCG = injectCG;
        this.fieldName = fieldName;
        this.fieldSignature = fieldSignature;
        this.fieldAccessFlags = fieldAccessFlags;
        this.methodAccessFlags = methodAccessFlags;
        this.argumentsSignature = methodParameters;

        hookHandler.hooks.add(this);

        if(methodName == null){
            methodName = fieldName;
        }
        if (Character.isLowerCase(methodName.charAt(0))) {
            methodName = Character.toUpperCase(methodName.charAt(0)) + methodName.substring(1, methodName.length());
        }
        methodName = "set" + methodName;
        this.methodName = methodName;
    }

    public void applyXMLForReflection(Document doc) {

    }

    public void applyXMLForInjection(Document doc){
        Element root = doc.getRootElement();
        Element classesNode = root.getChild("classes");
        Element classNode = null;
        for(Element classNode2: (List<Element>)classesNode.getChildren("class")){
            if(classNode2 == null || classNode2.getChild("className") == null){
                continue;
            }
            if(classNode2.getChildText("className").equals(injectCG.getClassName())){
                classNode = classNode2;
                break;
            }
        }

        if(classNode == null){
            classNode = new Element("class");
            classesNode.addContent(classNode);
            classNode.addContent(new Element("className").setText(injectCG.getClassName()));
        }

        Element taskNode = new Element("task");
        classNode.addContent(taskNode);
        taskNode.addContent(new Element("type").setText("setter"));
        Element dataNode = new Element("data");
        taskNode.addContent(dataNode);
        dataNode.addContent(new Element("methodName").setText(methodName));
        dataNode.addContent(new Element("argumentsSignature").setText(argumentsSignature));
        dataNode.addContent(new Element("fieldName").setText(fieldName));
        dataNode.addContent(new Element("fieldSignature").setText(fieldSignature));
        dataNode.addContent(new Element("fieldAccessFlags").setText(""+fieldAccessFlags));
        dataNode.addContent(new Element("methodAccessFlags").setText(""+methodAccessFlags));
    }
}
