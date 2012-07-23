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
import com.sun.org.apache.bcel.internal.generic.ClassGen;
import com.sun.org.apache.bcel.internal.generic.BasicType;
import com.sun.org.apache.bcel.internal.generic.Type;
import org.jdom.Document;
import org.jdom.Element;

import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: Jan Ove Saltvedt
 * Date: Oct 21, 2009
 * Time: 5:31:32 PM
 * To change this template use File | Settings | File Templates.
 */
public class VirtualFieldGetterHook extends Hook implements InjectionHook{
    private ClassGen cg;
    private String fieldName;
    private Type type;
    private Type returnType;
    private String methodName;
    private String group;

    public VirtualFieldGetterHook(HookHandler hookHandler, ClassGen cg, String fieldName, Type type) {

        this.cg = cg;
        this.fieldName = fieldName;
        this.type = type;

        String methodName = fieldName;
        if (Character.isLowerCase(methodName.charAt(0))) {
            methodName = Character.toUpperCase(methodName.charAt(0)) + methodName.substring(1, methodName.length());
        }
        if (type.equals(Type.BOOLEAN) || type.equals(Type.getType(Boolean.class))) {
            methodName = "is" + methodName;
        } else {
            methodName = "get" + methodName;
        }
        this.methodName = methodName;
        hookHandler.hooks.add(this);
        returnType = type;
    }

    public VirtualFieldGetterHook(HookHandler hookHandler, ClassGen cg, String fieldName, Type type, Type returnType, String group) {
        this(hookHandler, cg, fieldName, type);
        this.group = group;
        this.returnType = returnType;

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
            if(classNode2.getChildText("className").equals(cg.getClassName())){
                classNode = classNode2;
                break;
            }
        }

        if(classNode == null){
            classNode = new Element("class");
            classesNode.addContent(classNode);
            classNode.addContent(new Element("className").setText(cg.getClassName()));
        }

        Element taskNode = new Element("task");
        classNode.addContent(taskNode);
        taskNode.addContent(new Element("type").setText("getter"));
        Element dataNode = new Element("data");
        taskNode.addContent(dataNode);
        dataNode.addContent(new Element("methodName").setText(methodName));
        dataNode.addContent(new Element("fieldClassName").setText(cg.getClassName()));
        dataNode.addContent(new Element("fieldName").setText(fieldName));
        dataNode.addContent(new Element("fieldSignature").setText(type.getSignature()));
        dataNode.addContent(new Element("fieldAccessFlags").setText("0"));
        dataNode.addContent(new Element("returnSignature").setText(returnType.getSignature()));
    }

    public String getGroup() {
        return group;
    }

    public String getFieldNick() {
        return fieldName;
    }
}
