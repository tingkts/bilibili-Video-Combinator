package ting.bili.combine.video;

import java.io.*;

public class Main {
    final static String SOURCE_VIDEO_DIR = "D:\\bilibili\\download"; // the root directory mapped to "Android/data/com.bilibili.app.in/download"
    final static String FFMPEG_EXE = "\"D:\\Program Portable\\ffmpeg-20200824-3477feb-win64-static\\bin\\ffmpeg.exe\"";
    final static String FLV_FILE_LIST_NAME = "ff.txt";

    final static StringBuilder commands = new StringBuilder();

    public static void main(String[] args) {
        System.out.println("");

        go();

        System.out.println("\n\n");
        System.out.println("Commands:\n");
        System.out.println(commands);
        System.out.println("\n\n");
    }

    private static void go() {
        File file = new File(SOURCE_VIDEO_DIR);
        File[] results = file.listFiles();
        if (results != null) {
            for (File result : results) {
                if (result.isFile())
                    continue;
                doOneSeriesOrSingleProgram(result);
            }
        }
    }

    // 627196265\1\80
    private static void doOneSeriesOrSingleProgram(File seriesOrSingleProgramDir) { // e.g. 627196265
        File[] indexDirs = seriesOrSingleProgramDir.listFiles();
        if (indexDirs.length == 0) {
            System.out.println(seriesOrSingleProgramDir + " is empty\n");
            return;
        }
        boolean isSingleProgram = (indexDirs.length == 1) ? true : false;
        System.out.println(seriesOrSingleProgramDir + (isSingleProgram ? " has single file" : " have multiple files"));
        String title = null;
        for (File indexDir : indexDirs) { // 1, 2, 3, ... , N
            if (indexDir.isFile())
                continue;
            // get title
            for (File file : indexDir.listFiles()) { // e.g. 80 or lua.flv720.bili2api.64, danmaku.xml, entry.json
                if (file.getName().contains("entry.json")) {
                    // parse entry.json to get title
                    System.out.println("parse " + file);
                    try {
                        FileReader fr = new FileReader(file);
                        BufferedReader br = new BufferedReader(fr);
                        while (br.ready()) {
                            String line = br.readLine();
                            System.out.println(line);
                            String[] splits = line.split(",");
                            for (String split : splits) {
                                /*if (split.contains("index_title")) {
                                    String indexTitle = split.substring(15, split.length() - 1);
                                    indexTitle.strip();
                                    System.out.println("index_title: " + indexTitle);
                                    title += "_" + indexTitle;
                                    break;
                                } else */if (split.contains("title")) {
                                    title = split.substring(9, split.length() - 1);
                                    title.strip();
                                    title = title.replace('\\', '_');
                                    title = title.replace('/', '_');
                                    title = title.replace('|', '_');
                                    title = title.replace(' ', '_');
                                    System.out.println("title: " + title);
                                }
                            }
                        }
                        fr.close();
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    break;
                }
            }

            File seriesOutputDir = null;
            if (!isSingleProgram && title != null) {
                (seriesOutputDir = new File(SOURCE_VIDEO_DIR + "\\" + seriesOrSingleProgramDir.getName() + "_" + title)).mkdir();
            }

            // combine video
            for (File file : indexDir.listFiles()) { // e.g. 80 or lua.flv720.bili2api.64, danmaku.xml, entry.json
                if (file.isDirectory()) {
                    File videoM4s = null, audioM4s = null;
                    File ffTxt = null;
                    String outputFile;
                    if (isSingleProgram) {
                        outputFile = "\"" + SOURCE_VIDEO_DIR + "\\" + seriesOrSingleProgramDir.getName() + (title == null ? "" : " " + title) + ".mp4" + "\"";
                    } else {
                        outputFile = seriesOutputDir.toString() + "\\" + indexDir.getName() + ".mp4";
                    }
                    for (File mediaFile : file.listFiles()) {
                        if (mediaFile.getName().contains("video.m4s")) {
                            videoM4s = mediaFile;
                        } else if (mediaFile.getName().contains("audio.m4s")) {
                            audioM4s = mediaFile;
                        } else if (mediaFile.getName().contains(".flv")) {
                            if (ffTxt == null) {
                                ffTxt = new File(file.toString() + "\\" + FLV_FILE_LIST_NAME);
                                if (!ffTxt.exists()){
                                    try {
                                        ffTxt.createNewFile();
                                        ffTxt.setWritable(true);
                                        ffTxt.setReadable(true);
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }
                                }
                            }
                            writeFlvFileList(ffTxt, String.format("file \'%s\'\n", mediaFile.toString()));
                        }
                        if (videoM4s != null && audioM4s != null) {
                            // do combine video
                            System.out.println(videoM4s + " + " + audioM4s);
                            // "C:\ffmpeg-20200821-412d63f-win64-static\bin\ffmpeg.exe" -i %%x\64\video.m4s -i %%x\64\audio.m4s -c copy %%x.mp4

                            String cmd = FFMPEG_EXE + " -i " + videoM4s + " -i " + audioM4s + " -c copy " + outputFile;
                            System.out.println(cmd);
                            commands.append(cmd);
                            commands.append(" & ");
//                            try {
//                                System.out.println("execute: " + cmd);
//                                Process process = Runtime.getRuntime().exec(cmd);
//                                boolean isExited = process.waitFor(60*5, TimeUnit.SECONDS);
//                                if (!isExited) {
//                                    System.out.println("execute: " + cmd + " timeout！");
//                                }
//                            } catch (IOException/* | InterruptedException*/ e) {
//                                e.printStackTrace();
//                            }

                        }
                    }
                    if (ffTxt != null) {
                        System.out.println("combine flv: " + ffTxt);
                        // "C:\ffmpeg-20200821-412d63f-win64-static\bin\ffmpeg.exe" -f concat -i ff.txt -c copy output.mp4
                        String cmd = FFMPEG_EXE + " -f concat -safe 0 -i " + ffTxt.toString() + " -c copy " + outputFile;
                        System.out.println(cmd);
                        commands.append(cmd);
                        commands.append(" & ");
                    }
                    break;
                }
            }
            System.out.println("");
        }

        System.out.println("");
        System.out.println("");
    }

    static boolean writeFlvFileList(File file, String content) {
//        try{
//            BufferedWriter bw = new BufferedWriter(new FileWriter(file.getName()));
//            bw.write(content);
//            bw.newLine();
//            bw.flush();
//            bw.close();
//            System.out.println("write " + file + " done");
//        } catch(IOException e) {
//            e.printStackTrace();
//            System.out.println("write " + file + " failed!");
//            return false;
//        }
//        return true;

        FileOutputStream fop = null;
        try {
            fop = new FileOutputStream(file, true);
            byte[] contentInBytes = content.getBytes();
            fop.write(contentInBytes);
            fop.flush();
            fop.close();
            System.out.println("write " + file + " done");
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("write " + file + " failed!");
            return false;
        } finally {
            try {
                if (fop != null) {
                    fop.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
                System.out.println("write " + file + " failed!");
                return false;
            }
        }
        return true;
    }
}