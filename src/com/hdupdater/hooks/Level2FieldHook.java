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
import com.sun.org.apache.bcel.internal.generic.Type;
import com.sun.org.apache.bcel.internal.classfile.ConstantNameAndType;
import com.sun.org.apache.bcel.internal.classfile.AccessFlags;
import com.sun.org.apache.bcel.internal.classfile.Field;
import com.hdupdater.HookHandler;

/**
 * Created by IntelliJ IDEA.
 * User: Jan Ove Saltvedt
 * Date: Oct 18, 2009
 * Time: 1:58:28 PM
 * To change this template use File | Settings | File Templates.
 */
public class Level2FieldHook extends Hook implements InjectionHook {
    private ClassGen methodCG;
    private String methodName;
    private String group;
    private ClassGen field1CG;
    private String field1Name;
    private Type field1Type;
    private Field field1;
    private Type returnType;
    private Field field2;
    private String fieldNick;
    private Type field2Type;
    private String field2Name;
    private ClassGen field2CG;

    public Level2FieldHook(HookHandler hookHandler, String group, ClassGen field1CG, ClassGen field2CG,  Field field1, Field field2, String fieldNick, Type returnType, ClassGen methodCG){
        this.group = group;
        this.field1CG = field1CG;
        this.field2CG = field2CG;
        this.field1 = field1;
        this.field2 = field2;
        this.fieldNick = fieldNick;
        this.field1Type = field1.getType();
        this.field2Type = field2.getType();
        if(returnType == null){
            returnType = field2.getType();
        }
        this.returnType = returnType;
        if(methodCG == null){
            this.methodCG = field1CG;
        }
        else{
            this.methodCG = methodCG;
        }

        field1Name = field1.getName();
        field2Name = field2.getName();

        String methodName = fieldNick;
        if (Character.isLowerCase(methodName.charAt(0))) {
            methodName = Character.toUpperCase(methodName.charAt(0)) + methodName.substring(1, methodName.length());
        }
        if (field2Type.equals(Type.BOOLEAN) || field2Type.equals(Type.getType(Boolean.class))) {
            methodName = "is" + methodName;
        } else {
            methodName = "get" + methodName;
        }
        this.methodName = methodName;

        hookHandler.hooks.add(this);
    }

    public String getGroup() {
        return group;
    }

    public String getFieldNick() {
        return fieldNick;
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
        taskNode.addContent(new Element("type").setText("2LevelGetter"));
        Element dataNode = new Element("data");
        taskNode.addContent(dataNode);
        dataNode.addContent(new Element("methodName").setText(methodName));
        dataNode.addContent(new Element("field1ClassName").setText(field1CG.getClassName()));
        dataNode.addContent(new Element("field1Name").setText(field1Name));
        dataNode.addContent(new Element("field1Signature").setText(field1Type.getSignature()));
        dataNode.addContent(new Element("field1AccessFlags").setText(""+field1.getAccessFlags()));

        dataNode.addContent(new Element("field2ClassName").setText(field2CG.getClassName()));
        dataNode.addContent(new Element("field2Name").setText(field2Name));
        dataNode.addContent(new Element("field2Signature").setText(field2Type.getSignature()));
        dataNode.addContent(new Element("field2AccessFlags").setText(""+field2.getAccessFlags()));

        dataNode.addContent(new Element("returnSignature").setText(returnType.getSignature()));
    }
}
