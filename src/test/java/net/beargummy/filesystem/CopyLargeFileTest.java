package net.beargummy.filesystem;

import org.junit.Test;

import java.io.*;
import java.nio.file.Files;
import java.util.Random;

import static org.junit.Assert.assertTrue;

public class CopyLargeFileTest {
  @Test
  public void test() throws IOException {
    java.io.File fsFile = Files.createTempFile("filesystem", ".dat").toFile();
    java.io.File srcFile = Files.createTempFile("source", ".dat").toFile();

    Writer fw = new BufferedWriter(new FileWriter(srcFile), 8192);

    long size = 1024 * 1024;
    Random rnd = new Random();
    while (srcFile.length() < size) {
      final int[] ints = rnd.ints(1024).toArray();
      for (int i=0; i<ints.length; i++)
        fw.write(i);
    }

    fw.close();

    int blockSize = 8192;
    int blockCount = Math.round(srcFile.length() / blockSize);

    FileSystem fileSystem = FileSystemManager.getInstance()
        .create(fsFile, blockSize, blockCount);

    File newFile = fileSystem.createFile("/file.dat");

    byte[] buffer = new byte[1111];
    InputStream is = new BufferedInputStream(new FileInputStream(srcFile));
    try {
      while (true) {
        int numRead = is.read(buffer);
        if (numRead <= 0) break;

        newFile.append(buffer, 0, numRead);
      }
    } finally {
      is.close();
    }

    assertTrue(fsFile.delete());
    assertTrue(srcFile.delete());
  }
}
