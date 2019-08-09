package logicpackage;

import org.apache.commons.codec.digest.DigestUtils;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;


public class FilesManagement {

   /* private static Path getProjectPath() {
        return Paths.get(System.getProperty("user.dir"));
    }*/


//    static String getSha1(String path) {
//        String sha1 = null;
//        try {
//            InputStream is = new FileInputStream(path);
//            sha1 = DigestUtils.sha1Hex(is);
//            System.out.println("Digest          = " + sha1);
//            is.close();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//        return sha1;
//    }

    /*public static String getSa1FromString(String fileContent) {
        return DigestUtils.sha1Hex(fileContent);
    }*/

    public static void CreateFolder(Path path, String name) {
        Path newDirectoryPath = Paths.get(path.toString() + "/" + name);

        File directory = new File(newDirectoryPath.toString());
        if (!directory.exists()) {
            directory.mkdir();
        }
    }

    public static String ConvertLongToSimpleDateTime(long i_Time) {
        Date date = new Date(i_Time);
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd.mm.yyyy-hh:mm:ss:sss");
        String dateText = dateFormat.format(date);

        return dateText;
    }

    //input: c:\\..\\[repositoryName]\\[nameFile.txt]
    public static BlobData CreateSimpleFileDescription(Path repositoryPath, Path filePathOrigin, String i_UserName) {
        return createTemporaryFileDescription(repositoryPath, filePathOrigin, i_UserName);
    }

    private static BlobData createTemporaryFileDescription(Path repositoryPath, Path i_FilePath, String i_UserName) {
        File file = i_FilePath.toFile();
        Path parentFolder = i_FilePath.getParent();
        //repositoryPath.toString() + "\\.magit\\objects\\" + zipFileName
        String fileDescriptionFilePathString = repositoryPath.toString() + "\\.magit\\objects\\" + file.getName(); //+ ".txt";
        String descriptionStringForGenerateSha1 = "";
        String description = "";
        FileWriter outputFile = null;
        String sha1 = "";
        String type = file.isFile() ? "file" : "folder";
        BlobData simpleBlob = null;

        try {
            outputFile = new FileWriter(fileDescriptionFilePathString);
            BufferedWriter bf = new BufferedWriter(outputFile);
            description = readLineByLine(i_FilePath.toString());
            descriptionStringForGenerateSha1 = String.format("%s,%s,%s", file.getAbsolutePath(), type, description);
            bf.write(String.format("%s\n", description));
            bf.close();
            sha1 = DigestUtils.sha1Hex(descriptionStringForGenerateSha1);
            createZipFileIntoObjectsFolder(repositoryPath, Paths.get(fileDescriptionFilePathString), sha1);
            Paths.get(fileDescriptionFilePathString).toFile().delete();
            simpleBlob = new BlobData(file.getAbsolutePath(), i_UserName, ConvertLongToSimpleDateTime(file.lastModified()), false, sha1);
        } catch (IOException e) {

        }
        return simpleBlob;
    }


    private static void createZipFileIntoObjectsFolder(Path repositoryPath, Path filePath, String sha1) {
        try {
            File file = filePath.toFile();
            String zipFileName = sha1.concat(".zip");
            FileOutputStream fos = new FileOutputStream(repositoryPath.toString() + "\\.magit\\objects\\" + zipFileName);
            ZipOutputStream zos = new ZipOutputStream(fos);
            zos.putNextEntry(new ZipEntry(file.getName()));
            byte[] bytes = Files.readAllBytes(filePath);
            zos.write(bytes, 0, bytes.length);
            zos.closeEntry();
            zos.close();
        } catch (FileNotFoundException ex) {
            System.err.format("createZipFile: The file %s does not exist", filePath.toString());
        } catch (IOException ex) {
            System.err.println("createZipFile: I/O error: " + ex);
        }
    }

    public static String CreateCommitDescriptionFile(Commit i_Commit, Path i_SaveFolderPath) {
        String commitDescriptionFileString = i_SaveFolderPath.toString() + "\\" + i_Commit.getCommitComment() + ".txt";
        FileWriter outputFile = null;
        String commitInformationString = "";

        try {
            outputFile = new FileWriter(commitDescriptionFileString);
            BufferedWriter bf = new BufferedWriter(outputFile);

            commitInformationString = String.format(
                    "%s,%s,%s,%s",
                    i_Commit.getRootSHA1(),
                    i_Commit.getCommitComment(),
                    i_Commit.getCreationDate(),
                    i_Commit.getCreatedBy());

            try {
                bf.write(commitInformationString + "\n");
            } catch (IOException e) {
                e.printStackTrace();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

        return "";
    }

    public static Boolean IsFileOrDirectoryEmpty(File file) {
        System.out.println("in isFileOrDirectoryEmpty: "
                + "!file.isDirectory()&&isFileEmpty(file) || file.isDirectory()&&isDirectoryEmpty(file): "
                + (!file.isDirectory() && IsFileEmpty(file) || file.isDirectory() && IsDirectoryEmpty(file)));
        return !file.isDirectory() && IsFileEmpty(file) || file.isDirectory() && IsDirectoryEmpty(file);
    }

    public static Boolean IsFileEmpty(File file) {
        boolean isEmpty = false;
        if (!file.isDirectory()) {
            if (file.length() == 0) {
                isEmpty = true;
            }
        }
        System.out.println("isFileEmpty: " + isEmpty);
        return isEmpty;
    }

    public static Boolean IsDirectoryEmpty(File file) {
        boolean isEmpty = false;
        if (file.isDirectory()) {
            if (file.list().length == 0) {
                isEmpty = true;
            }
        }
        System.out.println("isDirectoryEmpty: " + isEmpty + "ile.list().length: " + file.list().length);
        return isEmpty;
    }

    private static String getCurrentBasicData(File file, BlobData i_Blob) {
        Folder currentFolder = i_Blob.getCurrentFolder();
        File currentFileInFolder = file;
        List<BlobData> blobList = currentFolder.getBlobList();
        String sha1String = "";
        String basicDataString = "";
        for (BlobData blob : blobList) {
            if (blob.getPath().equals(currentFileInFolder.toString())) {
                sha1String = blob.getSHA1();
                break;
            }
        }
        String type = currentFileInFolder.isFile() ? "file" : "folder";

        basicDataString = String.format(
                "%s,%s,%s",
                currentFileInFolder.getName(),
                type,
                sha1String
        );
        return basicDataString;
    }

    private static String getFolderDescriptionFilePathStaring(Path folderPath) {
        return folderPath.toString() + "\\" + folderPath.toFile().getName() + ".txt";
    }

    private static String getStringForFolderSHA1(BlobData i_Blob, Path repositoryPath, Path folderPath, String userName, String folderDescriptionFilePathString) {
        File currentFolder = folderPath.toFile();
        String stringForSha1 = "";
        String basicDataString = "";
        String fullDataString = "";
        FileWriter outputFile = null;

        try {
            outputFile = new FileWriter(folderDescriptionFilePathString);
            BufferedWriter bf = new BufferedWriter(outputFile);

            for (File file : currentFolder.listFiles()) {
                if (isFileValidForScanning(file, folderDescriptionFilePathString, folderPath)) {
                    basicDataString = getCurrentBasicData(file, i_Blob);
                    fullDataString = fullDataString.concat(basicDataString + "," + userName + "," +
                            ConvertLongToSimpleDateTime(file.lastModified()) + '\n');
                    stringForSha1 = stringForSha1.concat(basicDataString);
                }
            }
            try {
                bf.write(String.format("%s\n", fullDataString));
            } catch (IOException e) {
                e.printStackTrace();
            }

            bf.close();
        } catch (IOException e) {
        }

        return stringForSha1;
    }


    public static String CreateFolderDescriptionFile(BlobData i_Blob, Path repositoryPath, Path folderPath, String userName) {
        String folderDescriptionFilePathString = getFolderDescriptionFilePathStaring(folderPath);
        String stringForSha1 = getStringForFolderSHA1(i_Blob, repositoryPath, folderPath, userName, folderDescriptionFilePathString);
        String sha1String = DigestUtils.sha1Hex(stringForSha1);
        createZipFileIntoObjectsFolder(repositoryPath, Paths.get(folderDescriptionFilePathString), sha1String);
        Paths.get(folderDescriptionFilePathString).toFile().delete();

        return sha1String;
    }

    private static boolean isFileValidForScanning(File file, String folderDescriptionFilePathString, Path folderPath) {
        return (!IsFileOrDirectoryEmpty(file) && !file.toString().equals(folderPath.toString() + "\\.magit")//!isFileOrDirectoryEmpty(file)!!!!!! remove maybe
                && (!(file.toString()).equals(folderDescriptionFilePathString)));
    }

    private static String readLineByLine(String filePath) {
        StringBuilder contentBuilder = new StringBuilder();
        try (Stream<String> stream = Files.lines(Paths.get(filePath), StandardCharsets.UTF_8)) {
            stream.forEach(s -> contentBuilder.append(s).append("\n"));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return contentBuilder.toString();
    }
}