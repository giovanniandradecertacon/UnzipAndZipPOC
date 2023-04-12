package br.com.certacon.poczipfiles.service;

import com.github.junrar.Archive;
import com.github.junrar.exception.RarException;
import com.github.junrar.rarfile.FileHeader;
import org.apache.commons.io.FilenameUtils;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

import static java.nio.file.StandardCopyOption.ATOMIC_MOVE;

@Service
public class UnzipAndZipService {

    public static void extractZip(File zipFile, File outputFolder) throws IOException {
        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFile))) {
            ZipEntry entry = zis.getNextEntry();
            while (entry != null) {
                String entryName = entry.getName();
                File entryFile = new File(outputFolder, entryName);
                if (entry.isDirectory()) {
                    entryFile.mkdirs();

                } else if (FilenameUtils.getExtension(entryName).equals("zip")) {
                    entryFile.getParentFile().mkdirs();
                    extractFile(zis, entryFile);
                    extractZip(entryFile, outputFolder);
                } else {
                    entryFile.getParentFile().mkdirs();
                    extractFile(zis, entryFile);
                }
                entry = zis.getNextEntry();
            }
        }
    }

    private static void extractFolder(File folder, File destDir) throws IOException {
        File[] files = folder.listFiles();
        for (File file : files) {
            Path filePath = Path.of(file.getPath());
            if (FilenameUtils.getExtension(file.getName()).equals("zip")) {
                Path destPath = Path.of(destDir.getPath() + "\\" + file.getName());

                Files.move(filePath, destPath, ATOMIC_MOVE);
            }
            if (file.isFile()) {
                Path parentFolder = Path.of(folder.getParentFile().toPath() + "\\" + file.getName());

                Files.move(filePath, parentFolder, ATOMIC_MOVE);
            }
        }
        deleteFolder(folder);

    }

    private static void moveZip(File sourceDir, File destDir) throws IOException {
        File[] fileList = sourceDir.listFiles();
        Files.move(Path.of(fileList[0].getPath()), destDir.toPath());
        fileList[0].delete();
    }

    private static void extractLoop(File[] sourceDirList, File destDir) throws IOException {
        for (int i = 0; i < sourceDirList.length; i++) {
            extractZip(sourceDirList[i], destDir);
        }
    }

    private static void deleteFolder(File folder) throws IOException {
        if (folder.isDirectory()) {
            File[] files = folder.listFiles();
            if (files != null) {
                for (File file : files) {
                    deleteFolder(file);

                }
            }
        }
        Files.deleteIfExists(folder.toPath());
    }

    private static void extractFile(InputStream inputStream, File outputFile) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(outputFile)) {
            byte[] buffer = new byte[1024];
            int len;
            while ((len = inputStream.read(buffer)) > 0) {
                fos.write(buffer, 0, len);
            }
        }
    }

    private static boolean isArchiveFile(File file) {
        return isZipFile(file) || isRarFile(file);
    }

    private static boolean isZipFile(File file) {
        return FilenameUtils.getExtension(file.getName()).equals("zip");
    }

    private static boolean isRarFile(File file) {
        return FilenameUtils.getExtension(file.getName()).equals("rar");
    }

    public static List<String> listFiles(File file, boolean extract) throws IOException {
        List<String> files = new ArrayList<>();
        if (isArchiveFile(file)) {
            if (isZipFile(file)) {
                files.addAll(listZipFiles(file, extract));
            } else if (isRarFile(file)) {
                files.addAll(listRarFiles(file, extract));
            } else {
                throw new UnsupportedOperationException("Unsupported archive format");
            }
        } else {
            files.add(file.getName());
        }
        System.out.println(files);
        return files;
    }

    private static List<String> listZipFiles(File file, boolean extract) throws IOException {
        List<String> files = new ArrayList<>();
        try (ZipFile zipFile = new ZipFile(file)) {
            zipFile.stream().forEach(entry -> {
                if (!entry.isDirectory()) {
                    String fileName = entry.getName();
                    files.add(fileName);
                    if (extract) {
                        try (FileOutputStream fos = new FileOutputStream(new File(file.getParent(), fileName))) {
                            fos.write(zipFile.getInputStream(entry).readAllBytes());
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
                try {
                    zipFile.close();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
        }
        return files;
    }

    private static List<String> listRarFiles(File file, boolean extract) throws IOException {
        List<String> files = new ArrayList<>();
        try (Archive archive = new Archive(new FileInputStream(file))) {
            FileHeader fileHeader = archive.nextFileHeader();
            while (fileHeader != null) {
                if (!fileHeader.isDirectory()) {
                    String fileName = fileHeader.getFileNameString().trim();
                    files.add(fileName);
                    if (extract) {
                        try (FileOutputStream fos = new FileOutputStream(new File(file.getParent(), fileName))) {
                            archive.extractFile(fileHeader, fos);
                        } catch (RarException e) {
                            e.printStackTrace();
                        }
                    }
                }
                fileHeader = archive.nextFileHeader();
            }
        } catch (RarException e) {
            throw new RuntimeException(e);
        }
        return files;
    }

    public List<String> UnzipFiles(File path) throws IOException {

        Path compactedDir = Path.of("D:\\BASE TESTES AUTOMAÇÃO\\Compactados");
        Path descompactedDir = Path.of("D:\\BASE TESTES AUTOMAÇÃO\\Descompactados");
        File[] compactedList;

        do {
            File[] pathList = path.listFiles();
            if (!compactedDir.toFile().exists()) Files.createDirectory(compactedDir);
            if (!descompactedDir.toFile().exists()) Files.createDirectory(descompactedDir);

            if (pathList.length > 0) {
                if (FilenameUtils.getExtension(pathList[0].getName()).equals("zip")) {
                    moveZip(path, new File(compactedDir.toFile() + "\\" + pathList[0].getName()));
                }
            }

            compactedList = compactedDir.toFile().listFiles();
            extractLoop(compactedList, descompactedDir.toFile());


            File[] descompactedList = descompactedDir.toFile().listFiles();
            for (int i = 0; i < descompactedList.length; i++) {
                if (FilenameUtils.getExtension(descompactedList[i].getName()).equals("zip")) {

                    Files.move(descompactedDir, compactedDir, ATOMIC_MOVE);
                    descompactedList = descompactedDir.toFile().listFiles();
                } else if (descompactedList[i].isDirectory()) {
                    extractFolder(descompactedList[i], compactedDir.toFile());
                }
            }

            compactedList = compactedDir.toFile().listFiles();
        } while (compactedList.length != 0);

        return listFiles(compactedDir.toFile(), true);

    }
}
