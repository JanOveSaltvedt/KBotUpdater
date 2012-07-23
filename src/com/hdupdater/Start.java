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
import com.hdupdater.utils.JARTools;
import com.sun.org.apache.bcel.internal.generic.ClassGen;

import java.io.*;

/**
 * Created by IntelliJ IDEA.
 * User: Jan Ove / Kosaki
 * Date: 18.jul.2009
 * Time: 16:14:36
 */
public class Start {
    public static PrintStream outStream;
    public static PrintStream hookedStream;

    public static void main(String[] args){
        outStream = System.out;
        hookedStream = new PrintStream(new OutputStream() {
            @Override
            public void write(int b) throws IOException {
                outStream.write(b);
            }
        });
        System.setOut(hookedStream);
        /*try {
            Unpacker.unpack(new File("./files/jaggl_-594181187.pack200"), new File("./files/jagl.jar"));
        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }*/
        Updater updater = new Updater();
        updater.run();
    }
}
