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

import java.net.URL;
import java.net.URLConnection;
import java.io.*;

/**
 * Created by IntelliJ IDEA.
 * User: Jan Ove / Kosaki
 * Date: 18.jul.2009
 * Time: 16:23:46
 */
public class Downloader {
    public static File downloadFile(URL url, String filename, String fileType){
        try {
            File file = File.createTempFile(filename, fileType);
            final FileOutputStream fos = new FileOutputStream(file);
            fos.write(download(url));
            fos.close();
            return file;
        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            return null;
        }
    }

    private static byte[] download(URL url) throws IOException {
        System.out.println("Downloading: " + url.toString());
        long start = System.currentTimeMillis();
        URLConnection uc = url.openConnection();
        int len = uc.getContentLength();
        InputStream is = new BufferedInputStream(uc.getInputStream());
        try {
            byte[] data = new byte[len];
            int offset = 0;
            while (offset < len) {
                int read = is.read(data, offset, data.length - offset);
                if (read < 0) {
                    break;
                }
                offset += read;
            }
            if (offset < len) {
                throw new IOException(
                        String.format("Read %d bytes; expected %d", offset, len));
            }
            long time = System.currentTimeMillis() - start;
            System.out.println("Downloaded in " + time + " ms.\n");
            return data;
        } finally {
            is.close();
        }
    }
}
