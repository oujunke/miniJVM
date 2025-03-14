/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.mini.fs;

import org.mini.net.SocketNative;
import org.mini.reflect.vm.RefNative;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;

/**
 *
 *
 *
 * <pre>
 *
 *
 *    void t15() {
 *
 *        try {
 *            String s = "这是一个测试";
 *            System.out.println(s);
 *            File test = new File("./a.txt");
 *            StreamWriter writer = new UTF_8_Writer();
 *            writer.open(test.getOutputStream(false), "utf-8");
 *            writer.write(s);
 *            writer.close();
 *
 *            StreamReader reader = new UTF_8_Reader();
 *            reader.open(test.getInputStream(), "utf-8");
 *            char[] buf = new char[100];
 *            int len = reader.read(buf, 0, 100);
 *            reader.close();
 *            String r = new String(buf, 0, len);
 *            System.out.println(r);
 *
 *        } catch (IOException ex) {
 *            System.out.println(ex.getMessage());
 *        }
 *        t15_1();
 *    }
 *
 *    void t15_1() {
 *        File file = new File(".");
 *        System.out.println("isDir:" + file.isDirectory());
 *        String[] files = file.list();
 *        for (int i = 0; i < files.length; i++) {
 *            System.out.println(files[i]);
 *        }
 *    }
 *
 *    void t16() {
 *        try {
 *            File b = new File("./b.txt");
 *            String r = "这是一个测试";
 *            System.out.println(r);
 *            DataOutputStream dos = new DataOutputStream(b.getOutputStream(true));
 *            dos.writeUTF(r);
 *            dos.close();
 *            DataInputStream dis = new DataInputStream(b.getInputStream());
 *            String s = dis.readUTF();
 *            System.out.println(s);
 *            dis.close();
 *        } catch (IOException ex) {
 *            System.out.println(ex.getMessage());
 *        }
 *    }
 *
 * </pre>
 *
 *
 * @author gust
 */
public class InnerFile {

    InnerFileStat fd;
    long filePointer;
    String path;
    String mode;

    public InnerFile(String path) {
        this.path = path;
        fd = new InnerFileStat();
        int ret = loadFS(SocketNative.toCStyle(path), fd);
    }

    protected InnerFile() {
    }

    public boolean isFile() {
        return fd.isFile();
    }

    public boolean isDirectory() {
        return fd.isDirectory();
    }

    public boolean exists() {
        return fd.exists;
    }

    public long length() {
        return fd.st_size;
    }

    public String[] list() {
        return listDir(SocketNative.toCStyle(path));
    }

    public OutputStream getOutputStream(boolean append) throws IOException {
        if (filePointer != 0) {
            closeFile(filePointer);
            filePointer = 0;
        }
        filePointer = openFile(SocketNative.toCStyle(path), append ? SocketNative.toCStyle("a+b") : SocketNative.toCStyle("w+b"));;
        if (filePointer == 0) {
            throw new IOException("open file error:" + path);
        }
        return new InnerFileOutputStream(filePointer);
    }

    public InputStream getInputStream() throws IOException {
        if (filePointer != 0) {
            closeFile(filePointer);
            filePointer = 0;
        }
        filePointer = openFile(SocketNative.toCStyle(path), SocketNative.toCStyle("rb"));
        if (filePointer == 0) {
            throw new IOException("open file error:" + path);
        }
        return new InnerFileInputStream(filePointer);
    }

    public class InnerFileInputStream extends InputStream {

        public InnerFileInputStream(long fileHandle) {

        }

        public InnerFile getInnerFile() {
            return InnerFile.this;
        }

        public int available() throws IOException {
            return available0(getFilePointer());
        }

        @Override
        public int read() throws IOException {
            return read0(getFilePointer());
        }

        @Override
        public int read(byte b[], int off, int len) throws IOException {
            return readbuf(getFilePointer(), b, off, len);
        }

        @Override
        public void close() throws IOException {
            closeFile(getFilePointer());
            filePointer = 0;
        }

    }

    public class InnerFileOutputStream extends OutputStream {

        public InnerFileOutputStream(long fileHandle) {

        }

        public InnerFile getInnerFile() {
            return InnerFile.this;
        }

        @Override
        public void write(int b) throws IOException {
            int ret = write0(getFilePointer(), b);
            if (ret < 0) {
                throw new IOException("write file error: " + path);
            }
        }

        @Override
        public void write(byte[] b, int offset, int len) {
            int wrote = 0;
            while (wrote < len) {
                wrote += writebuf(getFilePointer(), b, offset + wrote, len - wrote);
            }
        }

        @Override
        public void close() throws IOException {
            closeFile(getFilePointer());
            filePointer = 0;
        }
    }

    public int read() throws IOException {
        int ret = read0(filePointer);
        if (ret < 0) {
            throw new IOException("read file error: " + path);
        }
        return ret;
    }

    public void write(int b) throws IOException {
        int ret = write0(filePointer, b);
        if (ret < 0) {
            throw new IOException("write file error: " + path);
        }
    }

    public void close() throws IOException {
        closeFile(filePointer);
        filePointer = 0;
    }

    /**
     * @return the filePointer
     */
    public long getFilePointer() {
        return filePointer;
    }

    public void setFilePointer(long fd) {
        filePointer = fd;
    }


    public static native int getOS();

    public static native int loadFS(byte[] filePath, InnerFileStat fd);

    public static native String getcwd();

    public static native int mkdir0(byte[] pathbuf);

    public static native int delete0(byte[] pathbuf);

    public static native int chmod(byte[] pathbuf, int mode);

    public static native int rename0(byte[] oldpath, byte[] newpath);

    public static native String[] listDir(byte[] filePath);

    public static native long openFile(byte[] filePath, byte[] mode);

    public static native int closeFile(long fileHandle);

    public static native int flush0(long fileHandle);

    public static native int read0(long fileHandle);

    public static native int write0(long fileHandle, int b);

    public static native int readbuf(long fileHandle, byte[] b, int off, int len);

    public static native int writebuf(long fileHandle, byte[] b, int off, int len);

    public static native int available0(long fileHandle);

    public static native int seek0(long fileHandle, long pos);

    public static native int setLength0(long fileHandle, long len);
    
    public static native String getTmpDir();

    public static native String listWinDrivers();
}
