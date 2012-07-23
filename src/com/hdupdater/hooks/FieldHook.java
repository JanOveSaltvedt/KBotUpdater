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

import com.sun.org.apache.bcel.internal.generic.*;
import com.sun.org.apache.bcel.internal.classfile.Field;
import com.hdupdater.HookHandler;

import org.jdom.Document;
import org.jdom.Element;

import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: Jan Ove / Kosaki
 * Date: 18.jul.2009
 * Time: 19:36:46
 */
public class FieldHook extends Hook implements InjectionHook{
    public FieldHook(HookHandler hookHandler, String group, ClassGen fieldCG, Field field, String fieldNick, Type returnType, ClassGen methodCG) {
        this.hookHandler = hookHandler;
        this.group = group;
        this.fieldCG = fieldCG;
        this.className = this.fieldCG.getClassName();
        this.field = field;
        this.fieldType = field.getType();
        if (returnType == null) {
            this.returnType = fieldType;
        } else {
            this.returnType = returnType;
        }
        if(methodCG == null){
            this.methodCG = fieldCG;
        }
        else{
            this.methodCG = methodCG;
        }
        this.fieldName = this.field.getName();
        this.fieldNick = fieldNick;
        hookHandler.hooks.add(this);
        String methodName = fieldNick;
        if (Character.isLowerCase(methodName.charAt(0))) {
            methodName = Character.toUpperCase(methodName.charAt(0)) + methodName.substring(1, methodName.length());
        }
        if (fieldType.equals(Type.BOOLEAN) || fieldType.equals(Type.getType(Boolean.class))) {
            methodName = "is" + methodName;
        } else {
            methodName = "get" + methodName;
        }
        this.methodName = methodName;
    }

    public String getFieldNick() {
        return fieldNick;
    }

    public String getClassName() {
        return className;
    }

    public String getFieldName() {
        return fieldName;
    }

    public ClassGen getFieldCG() {
        return fieldCG;
    }

    public Field getField() {
        return field;
    }


    public String getGroup() {
        return group;
    }

    private String fieldNick;
    private HookHandler hookHandler;
    String group;
    private String className;
    private String fieldName;
    private String methodName;

    private ClassGen fieldCG;
    private ClassGen methodCG;
    private Field field;
    private Type fieldType;
    private Type returnType;

    public String getName() {
        return group;
    }

    public void applyXMLForReflection(Document doc) {
        Element root = doc.getRootElement();
        Element fieldsNode = root.getChild("fields");

        Element fieldNode = new Element("field");
        fieldsNode.addContent(fieldNode);

        fieldNode.addContent(new Element("fieldGroup").setText(group));
        fieldNode.addContent(new Element("fieldNick").setText(fieldNick));
        fieldNode.addContent(new Element("className").setText(fieldCG.getClassName()));
        fieldNode.addContent(new Element("fieldName").setText(field.getName()));
        fieldNode.addContent(new Element("fieldSignature").setText(field.getSignature()));
        fieldNode.addContent(new Element("fieldIsStatic").setText("" + field.isStatic()));
    }

    public void applyXMLForInjection(Document doc){
        Element root = doc.getRootElement();
        Element classesNode = root.getChild("classes");
        Element classNode = null;
        for(Element classNode2: (List<Element>)classesNode.getChildren("class")){
            if(classNode2 == null || classNode2.getChild("className") == null){
                continue;
            }
            if(classNode2.getChildText("className").equals(methodCG.getClassName())){
                classNode = classNode2;
                break;
            }
        }

        if(classNode == null){
            classNode = new Element("class");
            classesNode.addContent(classNode);
            classNode.addContent(new Element("className").setText(methodCG.getClassName()));
        }

        Element taskNode = new Element("task");
        classNode.addContent(taskNode);
        taskNode.addContent(new Element("type").setText("getter"));
        Element dataNode = new Element("data");
        taskNode.addContent(dataNode);
        dataNode.addContent(new Element("methodName").setText(methodName));
        dataNode.addContent(new Element("fieldClassName").setText(fieldCG.getClassName()));
        dataNode.addContent(new Element("fieldName").setText(fieldName));
        dataNode.addContent(new Element("fieldSignature").setText(fieldType.getSignature()));
        dataNode.addContent(new Element("fieldAccessFlags").setText(""+field.getAccessFlags()));
        dataNode.addContent(new Element("returnSignature").setText(returnType.getSignature()));
    }

    public Type getReturnType() {
        return returnType;
    }

    public Type getFieldType() {
        return fieldType;
    }
}
