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



package com.hdupdater;

import com.hdupdater.hooks.*;
import com.hdupdater.utils.VersionFinder;
import com.sun.org.apache.bcel.internal.classfile.Field;
import com.sun.org.apache.bcel.internal.generic.*;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: Jan Ove / Kosaki
 * Date: 18.jul.2009
 * Time: 19:34:59
 */
public class HookHandler {
    public List<Hook> hooks = new ArrayList<Hook>();
    public HashMap<String, ClassGen> classes = new HashMap<String, ClassGen>();
    public HashMap<String, ClassGen> classNicks = new HashMap<String, ClassGen>();
    public List<String[]> classNickList = new ArrayList<String[]>(); // FORMAT: new String[]{"classNick", "className"};

    public HashMap<String, Object[]> dataMap = new HashMap<String, Object[]>();

    public HookHandler(ClassGen[] classArray) {
        for (ClassGen cG : classArray) {
            classes.put(cG.getClassName(), cG);
        }
    }

    public void addFieldHook(String group, FieldInstruction fieldInstruction, ConstantPoolGen cpg, String fieldNick, Type returnType) {
        addFieldHook(group, fieldInstruction.getClassName(cpg), fieldInstruction.getFieldName(cpg), fieldNick, returnType);
    }

    public void addFieldHook(String group, String className, String fieldName, String fieldNick, Type returnType) {
        ClassGen cG = getFieldsClass(className, fieldName);
        for (Field field : cG.getFields()) {
            if (field.getName().equals(fieldName)) {
                new FieldHook(this, group, cG, field, fieldNick, returnType, null);
                break;
            }
        }
    }

    public void addClientHook(FieldInstruction fieldInstruction, ConstantPoolGen cpg, String fieldNick, Type returnType) {
        addClientHook(fieldInstruction.getClassName(cpg), fieldInstruction.getFieldName(cpg), fieldNick, returnType);
    }

    public void addClientHook(String className, String fieldName, String fieldNick, Type returnType) {
        ClassGen classGen = getFieldsClass(className, fieldName);
        for (Field field : classGen.getFields()) {
            if (field.getName().equals(fieldName)) {
                new FieldHook(this, "Client", classGen, field, fieldNick, returnType==null?field.getType():returnType, classes.get("client"));
                return;
            }
        }
    }

    public ClassGen getFieldsClass(String className, String fieldName) {
        ClassGen currentCG = classes.get(className);
        while (currentCG != null) {
            for (Field field : currentCG.getFields()) {
                if (field.getName().equals(fieldName)) {
                    return currentCG;
                }
            }
            currentCG = classes.get(currentCG.getSuperclassName());
        }
        return null;
    }
    public void addClientHook(ClassGen fieldCG, Field field, String fieldNick) {
        new FieldHook(this, "Client", fieldCG, field, fieldNick, null, classes.get("client"));
    }

    public void addClientHook(ClassGen fieldCG, Field field, String fieldNick, Type returnType) {
        new FieldHook(this, "Client", fieldCG, field, fieldNick, returnType, classes.get("client"));
    }


    public String buildXML() {
        int rsVersion = new VersionFinder(classes.values().toArray(new ClassGen[1])).getVersion();
        Document doc = new Document(new Element("client"));
        Element root = doc.getRootElement();
        root.addContent(new Element("RSVersion").setText("" + rsVersion));
        final boolean enableKBot = true;
        root.addContent(new Element("enableKBot").setText("" + enableKBot));
        root.addContent(new Element("message").setText(enableKBot ? "" : "This version of RS is not yet supported."));

        /*root.addContent(new Element("fields"));

        for (Hook hook : hooks) {
            if (hook instanceof FieldHook) {
                hook.applyXMLForReflection(doc);
            }
        }
        Element classNicks = new Element("classNicks");
        root.addContent(classNicks);
        for(String[] strings: classNickList){
            if(strings[0].startsWith("#%")){
                continue;
            }
            Element node = new Element("class");
            node.addContent(new Element("classNick").setText(strings[0]));
            node.addContent(new Element("className").setText(strings[1]));
            classNicks.addContent(node);
        }*/

        Element classesNode = new Element("classes");
        root.addContent(classesNode);
        for (Hook hook : hooks) {
            if (hook instanceof InjectionHook) {
                hook.applyXMLForInjection(doc);
            }
        }

        for(String[] strings: classNickList){
            if(strings[0].startsWith("#%")){
                continue;
            }
            Element classNode = null;
            for(Element classNode2: (List<Element>)classesNode.getChildren("class")){
                if(classNode2 == null || classNode2.getChild("className") == null){
                    continue;
                }
                if(classNode2.getChildText("className").equals(strings[1])){
                    classNode = classNode2;
                    break;
                }
            }

            if(classNode == null){
                classNode = new Element("class");
                classesNode.addContent(classNode);
                classNode.addContent(new Element("className").setText(strings[1]));
            }

            Element taskNode = new Element("task");
            classNode.addContent(taskNode);
            taskNode.addContent(new Element("type").setText("injectInterface"));
            Element dataNode = new Element("data");
            taskNode.addContent(dataNode);
            dataNode.addContent(new Element("interfaceClassName").setText(Constants.DIRECTORY_HOOK+"/"+strings[0]));
        }

        XMLOutputter xmlOutputter = new XMLOutputter(Format.getPrettyFormat());
        String xml = xmlOutputter.outputString(doc);
        return xml;
    }

    public void addClassNick(String classNick, ClassGen cG) {
        classNicks.put(classNick, cG);
        classNickList.add(new String[]{classNick, cG.getClassName()});
    }

    public void addClassNick(String classNick, String className) {
        ClassGen cG = classes.get(className);
        addClassNick(classNick, cG);
    }

    public ClassGen getClassByNick(String classNick) {
        return classNicks.get(classNick);
    }

    public FieldHook getFieldHook(String transformer, String fieldNick) {
        for (Hook hook : hooks) {
            if (hook instanceof FieldHook) {
                FieldHook fieldHook = (FieldHook) hook;
                if (fieldHook.getGroup().equals(transformer) && fieldHook.getFieldNick().equals(fieldNick))
                    return fieldHook;
            }
        }
        return null;
    }

    public void checkFieldHooks(AbstractTransformer[] transformers) {
        System.out.println();
        List<String> hooks = new ArrayList<String>();
        for (AbstractTransformer transformer : transformers) {
            final String[] fieldHooks = transformer.getFieldHooks();
            if (fieldHooks != null)
                hooks.addAll(Arrays.asList(fieldHooks));
        }
        for (String identifier : hooks) {
            int hits = 0;
            for (Hook hook : this.hooks) {
                if (hook instanceof FieldHook) {
                    FieldHook fieldHook = (FieldHook) hook;
                    if (identifier.equals(fieldHook.getGroup() + "::" + fieldHook.getFieldNick())) {
                        hits++;
                    }
                }
                else if (hook instanceof Level2FieldHook) {
                    Level2FieldHook fieldHook = (Level2FieldHook) hook;
                    if (identifier.equals(fieldHook.getGroup() + "::" + fieldHook.getFieldNick())) {
                        hits++;
                    }
                }
                else if (hook instanceof ReturnSelfHook) {
                    ReturnSelfHook hook2 = (ReturnSelfHook) hook;
                    if (identifier.equals(hook2.getGroup() + "::" + hook2.getFieldNick())) {
                        hits++;
                    }
                }
                else if (hook instanceof VirtualFieldGetterHook) {
                    VirtualFieldGetterHook hook2 = (VirtualFieldGetterHook) hook;
                    if (identifier.equals(hook2.getGroup() + "::" + hook2.getFieldNick())) {
                        hits++;
                    }
                }

            }
            if (hits == 0) {
                System.out.println(identifier + " not hooked!!!");
            } else if (hits > 1) {
                System.out.println(identifier + " "+(hits == 2?"double hooked!!":(hits==3?"triple hooked!!!":"was hooked "+hits+" times!!!")));
            }
        }
    }

    public int postToDB(String xml, int rsVersion, boolean devMode) {
        try {
            Class.forName("com.mysql.jdbc.Driver").newInstance();
            Connection conn = DriverManager.getConnection("jdbc:mysql://kbot.info:3306/removed",
                    "removed", "removed");
            Statement stat = conn.createStatement();
            String query = "INSERT INTO `kbotpro`.`hooks` (`UID`, `clientVersion`, `xml`, `devOnly`) VALUES (NULL, '" + rsVersion + "', '" + xml + "', '"+devMode+"');";
            int result = stat.executeUpdate(query);
            conn.close();
            return result;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        catch (InstantiationException e) {
            e.printStackTrace();
        }
        catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        return -1;
    }

    public void addFieldHook(String group, ClassGen cG, Field field, String methodName, Type returnType) {
        new FieldHook(this, group, cG, field, methodName, returnType, null);
    }

    public void addClientHook(FieldInstruction fieldInstruction, ConstantPoolGen cpg, String fieldNick) {
        addClientHook(fieldInstruction, cpg, fieldNick, null);
    }

    public void addFieldHook(String group, String className, String fieldName, String fieldNick) {
        addFieldHook(group, className, fieldName, fieldNick, null);
    }

    public void addFieldHook(String group, ClassGen cG, Field field, String fieldNick) {
        addFieldHook(group, cG, field, fieldNick, null);
    }

    public void addFieldHook(String group, FieldInstruction fieldInstruction, ConstantPoolGen cpg, String fieldNick) {
        addFieldHook(group, fieldInstruction, cpg, fieldNick, null);
    }

    public void injectField(ClassGen cG, String fieldName, Type fieldType, int accessflags){
        new InjectFieldHook(this, cG, fieldName, fieldType.getSignature(), accessflags);
    }

    public void addSetter(ClassGen cG, String fieldName, Type fieldType, int fieldAccessflags, String argumentsSignature, int methodAccessFlags){
        new SetterHook(this, cG, fieldName, fieldType.getSignature(), fieldAccessflags, methodAccessFlags, null, argumentsSignature);
    }

    public Field getFieldInClassGen(String fieldName, ClassGen cG){
        for(Field field: cG.getFields()){
            if(field.getName().equals(fieldName)){
                return field;
            }
        }
        return null;
    }

    public Field getFieldInClassGen(String fieldName, String className){
        return getFieldInClassGen(fieldName, classes.get(className));        
    }

    public void addInterfaceHook(ClassGen cG, String endName){
        new InterfaceHook(this, cG, endName);
    }

    public void addReturnIntegerHook(String group, String methodName, ClassGen cG, int value) {
        new ReturnIntegerHook(this, methodName, cG, value);
    }

    public void addReturnNullHook(String group, String methodName, ClassGen cG, Type returnType) {
        new ReturnNullHook(this, methodName, cG, returnType);
    }
}
