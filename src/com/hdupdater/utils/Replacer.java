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

import com.sun.org.apache.bcel.internal.generic.*;
import com.sun.org.apache.bcel.internal.classfile.Method;
import com.sun.org.apache.bcel.internal.util.InstructionFinder;

import java.net.URL;
import java.net.URLConnection;
import java.net.MalformedURLException;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.File;
import java.util.*;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * Created by IntelliJ IDEA.
 * User: Jan Ove / Kosaki
 * Date: 19.jul.2009
 * Time: 14:36:44
 */
public class Replacer {
    public String getParams(){
        try{
            URL url = new URL("http://world169.runescape.com/plugin.js?param=o0,a0,m0,s0");

			URLConnection uc;

			uc = url.openConnection( );

			// Setup request.
			uc.addRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
			uc.addRequestProperty("Accept-Charset", "ISO-8859-1,utf-8;q=0.7,*;q=0.7");
	    	uc.addRequestProperty("Accept-Encoding", "gzip,deflate");
	    	uc.addRequestProperty("Accept-Language", "en-us,en;q=0.5");
	    	uc.addRequestProperty("Connection", "keep-alive");
	    	uc.addRequestProperty("Host", url.getHost());
	    	uc.addRequestProperty("Keep-Alive", "300");
	    	uc.addRequestProperty("User-Agent",
                    "Mozilla/5.0 (Windows; U; Windows NT 5.1; en-US; rv:1.9.0.10) Gecko/2009042316 Firefox/3.0.10");
	    	uc.addRequestProperty("Referer", "runescape.com");

	    	// Read entire page.
	    	DataInputStream di = new DataInputStream( uc.getInputStream( ) );
	    	byte[] buffer = new byte[ uc.getContentLength( ) ];
	    	di.readFully( buffer );
	    	di.close( );

	    	// Convert to string.
	        return new String( buffer );
        }catch(IOException e){
            e.printStackTrace();
        }
        return null;
    }

    public ClassGen[] replaceClasses(ClassGen[] classes){
        try {
            String html = getParams();
            Pattern regex = Pattern.compile(
                        "archive=(loader_gl-[0-9]+\\.jar) code=loader\\.class",
                        Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

            Matcher regexMatcher = regex.matcher( html );
            String filename = "";
            while(regexMatcher.find()){
                filename = regexMatcher.group(1);
            }

            File loader = Downloader.downloadFile(new URL("http://world169.runescape.com/"+filename), filename.replaceAll("\\.jar", ""), "jar");
            ClassGen[] loaderClasses = new JARTools().loadClasses(loader);
            String[] classNames = findClasses(loaderClasses);
            classes = replaceClasses(classes, loaderClasses, classNames);
        } catch (MalformedURLException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
        return classes;
    }

    private String[] findClasses(ClassGen[] loaderClasses){
        List<String> classNames = new ArrayList<String>();
        for(ClassGen cG: loaderClasses){
            if(!cG.getClassName().equals("loader"))
                continue;
            final ConstantPoolGen cpg = cG.getConstantPool();
            for(Method method: cG.getMethods()){
                if(!method.getName().equals("run")){
                    continue;
                }

                MethodGen methodGen = new MethodGen(method, cG.getClassName(), cpg);
                InstructionList iList = methodGen.getInstructionList();
                InstructionFinder instructionFinder = new InstructionFinder(iList);
                InstructionFinder.CodeConstraint constraint = new InstructionFinder.CodeConstraint() {
                    public boolean checkCode(InstructionHandle[] match) {
                        INVOKESTATIC invokestatic = (INVOKESTATIC) match[1].getInstruction();
                        return invokestatic.getClassName(cpg).contains("Class") && invokestatic.getMethodName(cpg).equals("forName");
                    }
                };
                for(Iterator<InstructionHandle[]> iterator = instructionFinder.search("ldc invokestatic astore", constraint); iterator.hasNext();){
                    InstructionHandle[] ih = iterator.next();
                    LDC ldc = (LDC) ih[0].getInstruction();
                    classNames.add((String) ldc.getValue(cpg));    
                }
            }
        }
        return classNames.toArray(new String[1]);
    }

    private ClassGen[] replaceClasses(ClassGen[] clientClasses, ClassGen[] loaderClasses, String[] names) {
        System.out.println("Replacing classes:");
        Map<String, ClassGen> loaderClassMap = new HashMap<String, ClassGen>();
        for (ClassGen cG : loaderClasses) {
            loaderClassMap.put(cG.getClassName(), cG);
        }
        int replaceCount = 0;
        int addCount = 0;
        nameLoop:
        for (String name : names) {
            if(name.equals("unpack"))
                continue;
            for(int i = 0; i < clientClasses.length; i++){
                ClassGen cG = clientClasses[i];
                if (cG.getClassName().equals(name)) {
                    System.out.println("\t- Replaced class " + name);
                    replacedClasses.add(name);
                    clientClasses[i] = loaderClassMap.get(name);
                    replaceCount++;
                    continue nameLoop;
                }
            }
            ClassGen[] temp = new ClassGen[clientClasses.length + 1];
			System.arraycopy(clientClasses, 0, temp, 0, clientClasses.length);
            temp[clientClasses.length] = loaderClassMap.get(name);
            clientClasses = temp;
            System.out.println("\t- Added class " + name);
            addedClasses.add(name);
            addCount++;
        }
        System.out.println("Replaced " + replaceCount + "/4 classes");
        System.out.println("Added " + addCount + "/1 classes\n");
        return clientClasses;
    }

    public List<String> replacedClasses = new ArrayList<String>();
    public List<String> addedClasses = new ArrayList<String>();
}
