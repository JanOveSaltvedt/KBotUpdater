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



package com.hdupdater.utils;

import com.sun.org.apache.bcel.internal.generic.ClassGen;
import com.sun.org.apache.bcel.internal.classfile.ClassParser;

import java.util.LinkedList;
import java.util.Enumeration;
import java.util.jar.JarFile;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.io.IOException;
import java.io.File;
import java.io.FileOutputStream;

/**
 * Created by IntelliJ IDEA.
 * User: Jan Ove / Kosaki
 * Date: 19.jul.2009
 * Time: 11:40:00
 */
public class JARTools {
    public ClassGen[] loadClasses(File f) {
        try {
            LinkedList<ClassGen> classes = new LinkedList<ClassGen>();
            JarFile file = new JarFile(f);
            Enumeration<JarEntry> entries = file.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                if (entry.getName().endsWith(".class")) {
                    ClassGen cG = new ClassGen(new ClassParser(file.getInputStream(entry), entry.getName().replaceAll(".class", "")).parse());
                    classes.add(cG);
                }
            }
            return classes.toArray(new ClassGen[classes.size()]);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void dumpClasses(File file, ClassGen[] classes) {
        try {
            if(file.exists()){
                file.createNewFile();
            }
            JarOutputStream out = new JarOutputStream(new FileOutputStream(file));
            for (ClassGen cG : classes) {
                JarEntry entry = new JarEntry(cG.getClassName() + ".class");
                out.putNextEntry(entry);
                byte[] b = cG.getJavaClass().getBytes();
                out.write(b);
            }
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}