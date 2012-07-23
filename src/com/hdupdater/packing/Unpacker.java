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



package com.hdupdater.packing;

import java.io.*;
import java.util.jar.JarOutputStream;
import java.util.jar.Pack200;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import com.sun.org.apache.bcel.internal.generic.ClassGen;
import com.sun.org.apache.bcel.internal.classfile.ClassParser;
import com.sun.xml.internal.messaging.saaj.util.ByteInputStream;

/**
 * Created by IntelliJ IDEA.
 * User: Jan Ove / Kosaki
 * Date: 18.jul.2009
 * Time: 16:14:51
 */
public class Unpacker {
    public static void unscrambleFile(File input, File output){
        try {
            int size = (int) input.length();
            RandomAccessFile raf = new RandomAccessFile(input, "rw");
            byte bytes[] = new byte[size];

            raf.readFully(bytes);
            byte unscrambled[] = new byte[2 + bytes.length];
            unscrambled[0] = 31;
		    unscrambled[1] = -117;
            System.arraycopy(bytes, 0, unscrambled, 2, bytes.length);
            if(output.exists()){
                output.createNewFile();
            }
            final FileOutputStream fileOutputStream = new FileOutputStream(output);
            fileOutputStream.write(unscrambled);
            fileOutputStream.close();
        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }
    private static byte[] unscrambler(byte abyte0[]) throws IOException {
		byte abyte1[] = new byte[2 + abyte0.length];
		ByteArrayOutputStream bytearrayoutputstream;
		abyte1[0] = 31;
		abyte1[1] = -117;
		System.arraycopy(abyte0, 0, abyte1, 2, abyte0.length);
		bytearrayoutputstream = new ByteArrayOutputStream();
		Pack200.newUnpacker().unpack(
				new GZIPInputStream(new ByteArrayInputStream(abyte1)),
				new JarOutputStream(bytearrayoutputstream));
		return bytearrayoutputstream.toByteArray();
	}

    public static void unpack(File f, File newFile) throws IOException {
        if(!newFile.exists())
            newFile.createNewFile();
		int size = (int) f.length();
		RandomAccessFile raf = new RandomAccessFile(f, "rw");
		byte b[] = new byte[size];
		raf.readFully(b);

		ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(
				unscrambler(b)));
        ZipOutputStream zipOutputStream = new ZipOutputStream(new FileOutputStream(newFile));
		byte buffer[] = new byte[1000];
		do {
			ZipEntry entry = zis.getNextEntry();
			if (entry == null)
				break;
            zipOutputStream.putNextEntry(new ZipEntry(entry.getName()));
			String name = entry.getName();
			if (name.endsWith(".class")) {
				name = name.substring(0, name.length() - 6);
				ByteArrayOutputStream bos = new ByteArrayOutputStream();
				do {
					int i = zis.read(buffer, 0, 1000);
					if (i == -1)
						break;
					bos.write(buffer, 0, i);
				} while (true);

				byte fileBytes[] = bos.toByteArray();
                zipOutputStream.write(fileBytes);
                zipOutputStream.closeEntry();
			}
		} while (true);
		zis.close();
        zipOutputStream.close();
	}

	public static ClassGen[] unpack(File f) throws IOException {
		int size = (int) f.length();
		RandomAccessFile raf = new RandomAccessFile(f, "rw");
		byte b[] = new byte[size];
		raf.readFully(b);

		ClassGen[] classes = new ClassGen[0];

		ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(
				unscrambler(b)));
		byte buffer[] = new byte[1000];
		do {
			ZipEntry entry = zis.getNextEntry();
			if (entry == null)
				break;
			String name = entry.getName();
			if (name.endsWith(".class")) {
				name = name.substring(0, name.length() - 6);
				ByteArrayOutputStream bos = new ByteArrayOutputStream();
				do {
					int i = zis.read(buffer, 0, 1000);
					if (i == -1)
						break;
					bos.write(buffer, 0, i);
				} while (true);

				byte fileBytes[] = bos.toByteArray();

				ClassGen[] temp = new ClassGen[classes.length + 1];
				System.arraycopy(classes, 0, temp, 0, classes.length);
				temp[classes.length] =  new ClassGen(new ClassParser(new ByteInputStream(fileBytes, size),name).parse());
				classes = temp;
			}
		} while (true);
		zis.close();
		return classes;
	}
}
