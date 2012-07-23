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

import com.hdupdater.packing.Unpacker;
import com.hdupdater.utils.Downloader;
import com.hdupdater.utils.JARTools;
import com.sun.org.apache.bcel.internal.generic.ClassGen;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;


/**
 * Created by IntelliJ IDEA.
 * User: Jan Ove / Kosaki
 * Date: 18.jul.2009
 * Time: 16:30:47
 */
public class Updater implements Runnable{

    private List<AbstractTransformer> transformers = new ArrayList<AbstractTransformer>();
    private List<AbstractDeobber> deobbers = new ArrayList<AbstractDeobber>();
    private HookHandler hookHandler;
    
    public void run(){
        // Download pack200
        try {
            File pack200 = /*new File("./files/main_file_cache.dat2");*/ Downloader.downloadFile(new URL("http://world169.runescape.com/runescape_gl_-1339624492.pack200"), "runescape", "pack200");
            ClassGen[] classes = Unpacker.unpack(pack200);
            System.out.println("Loaded "+classes.length+" from pack200");
            new JARTools().dumpClasses(new File("./files/runescapeHD-raw.jar"), classes);

            ClassGen[] normalClasses = Unpacker.unpack(pack200);

            // Get transformers
            getTransformers();
            transformers = organize(transformers);

            // getDeobbers
            getDeobbers();
            deobbers = organizeDeobbers(deobbers);
            System.out.println("Deobbing classes...");
            for(AbstractDeobber deobber:deobbers){
                deobber.classes = classes;
            }
            runDeobber();
            System.out.println("Deobbing done, dumping deobeedHD.jar...\n");

            new JARTools().dumpClasses(new File("./files/deobbedHD.jar"), classes);

            // set classes and hookHandler
            hookHandler = new HookHandler(classes);
            System.out.println("Starting updater...");
            for(AbstractTransformer transformer: transformers){
                transformer.classes = classes;
                transformer.normalClasses = normalClasses;
                transformer.hookHandler = hookHandler;
            }
            // run updater
            runUpdater();

            System.out.println("Hooked "+hookHandler.hooks.size());
            hookHandler.checkFieldHooks(transformers.toArray(new AbstractTransformer[1]));
            final String xml = hookHandler.buildXML();
            System.out.println(xml);
            // TODO reverse
            //
            //System.out.println("Database result: "+hookHandler.postToDB(xml, new VersionFinder(classes).getVersion(), false));
        } catch (MalformedURLException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }

    }

    private void runUpdater() {
        for(AbstractTransformer transformer: transformers){
            try{
                transformer.run();
            } catch (Exception e){
                e.printStackTrace();
            }
        }        
    }

    private void runDeobber() {
        for(AbstractDeobber deobber: deobbers){
            deobber.run();
        }
    }

    private void getTransformers() {
        File directory = new File(Constants.DIRECTORY_TRANSFORM);
        for (File file : directory.listFiles()) {
            if (!file.getName().equals(".svn")) {
                if (!file.getName().equals(file.getName().replaceAll("\\$[a-zA-Z0-9_]+\\.class", ".fail"))) {
                    continue;
                }
                String name = file.getName().replaceAll(".class", "");
                try {
                    AbstractTransformer transformer = (AbstractTransformer) Class.forName(Constants.PACKAGE_TRANSFORM + "." + name).newInstance();
                    if (AbstractTransformer.class.isAssignableFrom(transformer.getClass())) {
                        transformers.add(transformer);
                    }
                } catch (InstantiationException e) {
                    e.printStackTrace();
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }
            }
        }

    }

    private void getDeobbers() {
        File directory = new File(Constants.DIRECTORY_DEOBBER);
        for (File file : directory.listFiles()) {
            if (!file.getName().equals(".svn")) {
                if (!file.getName().equals(file.getName().replaceAll("\\$[a-zA-Z0-9_]+\\.class", ".fail"))) {
                    continue;
                }
                String name = file.getName().replaceAll(".class", "");
                try {
                    AbstractDeobber deobber = (AbstractDeobber) Class.forName(Constants.PACKAGE_DEOBBER + "." + name).newInstance();
                    if (AbstractDeobber.class.isAssignableFrom(deobber.getClass())) {
                        deobbers.add(deobber);
                    }
                } catch (InstantiationException e) {
                    e.printStackTrace();
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }
            }
        }

    }

    private List<AbstractTransformer> organize(List<AbstractTransformer> before) {
        List<AbstractTransformer> after = new LinkedList<AbstractTransformer>();
        while (!before.isEmpty()) {
            for (int i = 0; i < before.size(); i++) {
                AbstractTransformer transformer = before.get(i);
                if (!after.contains(transformer)) {
                    if (canRun(transformer)) {
                        after.add(transformer);
                        before.remove(transformer);
                    }
                }
            }
        }
        return after;
    }

    private List<AbstractDeobber> organizeDeobbers(List<AbstractDeobber> before) {
        List<AbstractDeobber> after = new LinkedList<AbstractDeobber>();
        while (!before.isEmpty()) {
            for (int i = 0; i < before.size(); i++) {
                AbstractDeobber deoobber = before.get(i);
                if (!after.contains(deoobber)) {
                    if (canRun(deoobber)) {
                        after.add(deoobber);
                        before.remove(deoobber);
                    }
                }
            }
        }
        return after;
    }

    private boolean canRun(AbstractTransformer transformer) {
        Class<?>[] dependencies = transformer.getDependencies();
        if(dependencies == null){
            return true;
        }
        for (Class<?> dependency : dependencies) {
            for (AbstractTransformer t : transformers) {
                if (t.getClass().equals(dependency)) {
                    if (!t.hasRun()) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    private boolean canRun(AbstractDeobber deobber) {
        Class<?>[] dependencies = deobber.getDependencies();
        if(dependencies == null){
            return true;
        }
        for (Class<?> dependency : dependencies) {
            for (AbstractDeobber t : deobbers) {
                if (t.getClass().equals(dependency)) {
                    if (!t.hasRun()) {
                        return false;
                    }
                }
            }
        }
        return true;
    }
}
