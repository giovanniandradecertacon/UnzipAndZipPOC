package br.com.certacon.poczipfiles.service;

import br.com.certacon.poczipfiles.model.ZipModel;
import br.com.certacon.poczipfiles.repository.ZipRepository;
import br.com.certacon.poczipfiles.utils.StatusZip;
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
    private final ZipRepository zipRepository;

    public UnzipAndZipService(ZipRepository zipRepository) {
        this.zipRepository = zipRepository;
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

    private StatusZip extractFolder(File directory, File destDir) throws IOException {
        File[] directoryList = readFolder(directory);
        for (File file : directoryList) {
            Path filePath = Path.of(file.getPath());
            if (file.isDirectory()) extractFolder(file, destDir);
            if (FilenameUtils.getExtension(file.getName()).equals("zip")) {
                Path destPath = Path.of(destDir.getPath() + "\\" + file.getName());
                Files.move(filePath, destPath, ATOMIC_MOVE);
            } else {
                Path parentFolder = Path.of(file.getParentFile() + "\\" + file.getName());

                Files.move(filePath, parentFolder, ATOMIC_MOVE);

            }

        }

        return StatusZip.MOVED;
    }

    private Boolean deleteFolders(File directory) throws IOException {
        File[] files = readFolder(directory);
        if (files != null) {
            for (File file : files) {
                if (directory.isDirectory()) Files.deleteIfExists(file.toPath());
            }
        }

        return Boolean.TRUE;
    }

    private Boolean deleteFile(File fileToDelete) throws IOException {
        Files.deleteIfExists(Path.of(fileToDelete.getPath()));
        return Boolean.TRUE;
    }

    private File[] readFolder(File folderPath) {
        File[] sourceFiles = folderPath.listFiles();
        return sourceFiles;
    }

    private StatusZip moveFile(File fileToMove, Path destiny) throws IOException {
        Files.move(Path.of(fileToMove.getPath()), destiny, ATOMIC_MOVE);
        return StatusZip.MOVED;
    }

    private Path createDirectory(Path directory) throws IOException {
        if (!directory.toFile().exists()) Files.createDirectory(directory);
        return directory;
    }

    private StatusZip unzipFile(File toDescompact, Path destinyDir) {
        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(toDescompact))) {
            ZipEntry entry = zis.getNextEntry();
            while (entry != null) {
                String entryName = entry.getName();
                File entryFile = new File(destinyDir.toFile(), entryName);
                if (entry.isDirectory()) {
                    File[] fileList = readFolder(entryFile);
                    for (File file : fileList) {
                        if (FilenameUtils.getExtension(file.getName()).equals("zip")) unzipFile(file, destinyDir);
                    }

                } else if (FilenameUtils.getExtension(entryName).equals("zip")) {
                    entryFile.getParentFile().mkdirs();
                    extractFile(zis, entryFile);
                    unzipFile(entryFile, destinyDir);
                } else {
                    entryFile.getParentFile().mkdirs();
                    extractFile(zis, entryFile);
                }
                entry = zis.getNextEntry();
            }
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return StatusZip.UNZIPPED;
    }

    public List<String> UnzipFiles(ZipModel zipModel) throws IOException {
        Path uuidFile = Path.of("D:\\" + zipModel.getId().toString());
        Path compactedDir = Path.of(uuidFile + "\\Compactados");
        Path descompactedDir = Path.of(uuidFile + "\\Descompactados");
        File[] compactedList;
        File[] pathList;

        do {
            pathList = readFolder(new File(zipModel.getSourceFile().getPath()));

            uuidFile = createDirectory(uuidFile);
            compactedDir = createDirectory(compactedDir);
            descompactedDir = createDirectory(descompactedDir);

            if (pathList.length > 0) {
                if (FilenameUtils.getExtension(pathList[0].getName()).equals("zip")) {
                    Path movedFilePath = Path.of(compactedDir + "\\" + pathList[0].getName());
                    StatusZip moveStatus = moveFile(pathList[0], movedFilePath);
                    zipModel.setZipStatus(moveStatus);
                    if (zipModel.getZipStatus().equals(StatusZip.MOVED)) {
                        StatusZip unzipStatus = unzipFile(movedFilePath.toFile(), descompactedDir);
                        zipModel.setZipStatus(unzipStatus);
                    }
                    if (zipModel.getZipStatus().equals(StatusZip.UNZIPPED)) {
                        deleteFile(movedFilePath.toFile());
                    }
                }

            }

        } while (readFolder(new File(zipModel.getSourceFile().getPath())).length > 0);

        do {
            File[] descompactedList = readFolder(descompactedDir.toFile());
            for (int i = 0; i < descompactedList.length; i++) {
                if (descompactedList[i].isDirectory()) {
                    extractFolder(descompactedList[i], compactedDir.toFile());
                    deleteFolders(descompactedList[i]);
                }
            }
            for (int i = 0; i < descompactedList.length; i++) {
                if (FilenameUtils.getExtension(descompactedList[i].getName()).equals("zip")) {
                    moveFile(descompactedList[i], compactedDir).equals(StatusZip.MOVED);
                }
            }
            compactedList = readFolder(compactedDir.toFile());
            for (int k = 0; k < compactedList.length; k++) {
                unzipFile(compactedList[k], descompactedDir);
            }

        } while (compactedList.length != 0);

        return null;
    }
}

