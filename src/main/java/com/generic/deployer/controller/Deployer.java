package com.generic.deployer.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.beans.factory.annotation.*;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;

@RestController
@RequestMapping("/deploy")
public class Deployer
{
    public static String uploadFolder;

    public static String sftp;

    public static String identityFile;

    public static String usr;

    public static String passwd;

    String os = System.getProperty("os.name").toUpperCase();

    private static final Logger logger = LoggerFactory.getLogger (Deployer.class);

    private void removeTemporaryFile(String uploadFolder, String fileName)
    {
        String path = uploadFolder+"/"+fileName;
        logger.info("Preparing to delete file " + path);
        File deleteFile = new File (path);
        deleteFile.delete();
        logger.info("Deleted file " + path);
    }

    private void createTemporaryFile(String uploadFolder, String fileName, MultipartFile file) throws Exception
    {
        FileOutputStream localFile = new FileOutputStream(uploadFolder+"/"+fileName);
        localFile.write(file.getBytes());
        localFile.close();
        changeMode("777", uploadFolder+fileName);
        logger.info("file " + uploadFolder+"/"+fileName + " created");
    }

    private String getUpLoadFileName (String file, String ext)
    {
        return "Upload_" + file + ext;
    }

    private String getStartUpFileName (String file, String ext)
    {
        return "Start_" + file + ext;
    }

    private String getStopFileName (String file, String ext)
    {
        return "Stop_" + file + ext;
    }

    private int startLocalProcess (String program)
    {
        int ret = -1;

        try
        {
            logger.info("Spawning process for " + program);
            Process p = Runtime.getRuntime().exec(program);
            ret = p.waitFor();
        }
        catch(Exception err)
        {
            logger.info("failed to start process " + program );
        }

        return ret;
    }

    private int startRemoteProcess (String scriptFile, String machine, String user, String identityFile)
    {

        String command = identityFile.equalsIgnoreCase("") ? "ssh " : "ssh -i " + identityFile;
        command = command + " " + user + "@" + machine +  " " + scriptFile;
        int ret = -1;

        try
        {
            logger.info("Spawning process for " + command);
            Process p = Runtime.getRuntime().exec(command);
            ret = p.waitFor();
        }
        catch(Exception err)
        {
            logger.info("failed to start remote process ");
        }

        return ret;
    }

    private int startSFTP (String user, String password, String machines, String script, String identityFile)
    {
        String add = identityFile.equalsIgnoreCase("") ? "" : " -i " + identityFile;
        String command = sftp + " -b " + script + " " + add + " " + user + "@" + machines;
        int ret = startLocalProcess(command);
        return ret;
    }

    private void createStartStopServiceScript (String localPath, String program, int operation)
            throws Exception
    {
        String scriptName;
        String command;
        if (1 == operation)
        {
            scriptName = getStartUpFileName(program,".sh");
            command = " start";
        }
        else {
            scriptName = getStopFileName(program, ".sh");
            command = "stop";
        }
        String script = localPath + scriptName;
        BufferedWriter newUpload = new BufferedWriter(new FileWriter(script));
        newUpload.write("sudo service " + program + " " + command);
        newUpload.write("\n");
        newUpload.close();
        logger.info ("startup script file " + script + " created");
        changeMode ("777", script);
    }

    private void createStartUpScript (String localPath, String program, String remotePath) throws Exception
    {
        String scriptName = getStartUpFileName(program,".sh");
        String script = localPath + scriptName;
        BufferedWriter newUpload = new BufferedWriter(new FileWriter(script));
        newUpload.write("nohup java -jar " + remotePath + program + " > " + "/tmp" + "/" + program + ".log 2>&1 &");
        newUpload.write("\n");
        newUpload.write("rm " + scriptName);
        newUpload.write("\n");
        newUpload.close();
        logger.info ("startup script file " + script + " created");
        changeMode ("777", script);
    }

    private void createStopScript (String localPath, String program, String remotePath) throws Exception
    {
        String scriptName = getStopFileName(program,".sh");
        String script = localPath + scriptName;
        BufferedWriter newUpload = new BufferedWriter(new FileWriter(script));
        newUpload.write("ps -ef | grep " + "'." + program + ".' | grep java | awk '{print $2}' | xargs kill -9 ");
        newUpload.write("\n");
        newUpload.write("rm " + scriptName);
        newUpload.write("\n");
        newUpload.close();
        logger.info ("Stop script file " + script + " created");
        changeMode ("777", script);
    }

    private void createUloadScript (String localPath, String file, String remotePath, Boolean isWindows,
                                    int flow)
            throws Exception
    {
        String ext = ".sh";
        if (isWindows)
            ext = ".bat";

        String script = localPath + getUpLoadFileName(file,ext);
        BufferedWriter newUpload = new BufferedWriter(new FileWriter(script));
        newUpload.write("lcd " + localPath);
        newUpload.write("\n");
        newUpload.write("cd " + remotePath);
        newUpload.write("\n");
        switch (flow)
        {
            case 0:
                newUpload.write("put " + file );
                break;
            case 1:
                newUpload.write("put " + getStartUpFileName(file,".sh"));
                break;
            case 2:
                newUpload.write("put " + getStopFileName(file,".sh"));
                break;
            default:
                break;
        }

        newUpload.write("\n");
        newUpload.close();
        changeMode("777", script);
        logger.info("Upload script " + script + " created");
    }

    private void changeMode (String mode, String file)
    {
        if (false == os.contains("WINDOWS"))
        {
            String chngMode = "chmod " + mode +  " " + file;
            logger.info("Setting mode " + mode + " on file " + file);
            startLocalProcess(chngMode);
        }
    }

    private int upLoad (String localPath, String file, String machines, String user, String password,
                        String remotePath, int flow ) throws Exception
    {
        //now SFTP and upload to server
        String ext = ".sh";
        Boolean isWindows = false;
        if ( os.contains("WINDOWS"))
        {
            ext = ".bat";
            isWindows = true;
        }

        createUloadScript(localPath,file,remotePath,isWindows,flow);

        int ret = startSFTP(user,password,machines,localPath + getUpLoadFileName(file,ext), identityFile);

        return ret;
    }

    @RequestMapping (value="/upload", method = RequestMethod.POST)
    public void upload ( @RequestParam("file") MultipartFile file,
                         @RequestHeader("id1")  String key,
                         @RequestHeader("id2")  String iv,
                         @RequestHeader("deployto") String machines,
                         @RequestHeader("remotepath") String remotePath)
    {
        //Copy the file locally
        //upload to server
        //start the server
        //delete the local copy
        String fileName = file.getOriginalFilename();
        FileOutputStream localFile = null;
        int ret = -1;
        try
        {
            createTemporaryFile (uploadFolder, fileName, file);
            upLoad(uploadFolder,fileName,machines,usr, passwd, remotePath,0);
            removeTemporaryFile (uploadFolder,fileName);
        }
        catch(Exception err)
        {

        }
        finally
        {
            System.out.println(ret);
            try {
                localFile.close();
            }
            catch(Exception err)
            {

            }
        }
    }

    @RequestMapping (value="/start", method = RequestMethod.POST)
    public void start (  @RequestHeader("id1")  String key,
                         @RequestHeader("id2")  String iv,
                         @RequestHeader("program")String program,
                         @RequestHeader("deployto") String machines,
                         @RequestHeader("remotepath") String remotePath
                      )
    {
        //Copy the file locally
        //upload to server
        //start the server
        //delete the local copy
        if (true == os.toUpperCase().contains("WINDOWS"))
        {
            logger.info ("no support for windows");
            return;
        }

        try
        {
            createStartUpScript (uploadFolder,program,remotePath);
            upLoad(uploadFolder,program ,machines,usr,passwd,remotePath,1);
            removeTemporaryFile(uploadFolder,getUpLoadFileName(program,os.contains("WINDOWS")?".bat":".sh"));
            startRemoteProcess(remotePath + getStartUpFileName(program,".sh"), machines,
                    usr, identityFile);
        }
        catch (Exception err)
        {

        }

    }

    @RequestMapping (value="/stop", method = RequestMethod.POST)
    public void stop (  @RequestHeader("id1")  String key,
                        @RequestHeader("id2")  String iv,
                        @RequestHeader("program")String program,
                        @RequestHeader("deployto") String machines,
                        @RequestHeader("remotepath") String remotePath)
    {
        //Copy the file locally
        //upload to server
        //start the server
        //delete the local copy
        if (true == os.toUpperCase().contains("WINDOWS"))
        {
            logger.info ("no support for windows");
            return;
        }

        try
        {
            createStopScript (uploadFolder,program,remotePath);
            upLoad(uploadFolder,program ,machines,usr,passwd,remotePath,2);
            removeTemporaryFile(uploadFolder,getUpLoadFileName(program,os.contains("WINDOWS")?".bat":".sh"));
            startRemoteProcess(remotePath + getStopFileName(program,".sh"), machines,
                    usr, identityFile);
        }
        catch (Exception err)
        {

        }
    }

    @RequestMapping (value="/start-stop", method = RequestMethod.POST)
    public void start (  @RequestHeader("id1")  String key,
                         @RequestHeader("id2")  String iv,
                         @RequestHeader("service")String service,
                         @RequestHeader("deployto") String machines,
                         @RequestHeader("remotepath") String remotePath,
                         @RequestHeader("operation") int operation
                      )
    {
        //Copy the file locally
        //upload to server
        //start the server
        //delete the local copy
        if (true == os.toUpperCase().contains("WINDOWS"))
        {
            logger.info ("no support for windows");
            return;
        }

        try
        {
            createStartStopServiceScript (uploadFolder,service,operation);
            upLoad(uploadFolder,service ,machines,usr,passwd,remotePath,operation);
            if (1 == operation)
                startRemoteProcess(remotePath + getStartUpFileName(service,".sh"), machines,
                    usr, identityFile);
            else
                startRemoteProcess(remotePath + getStopFileName(service,".sh"), machines,
                        usr, identityFile);
        }
        catch (Exception err)
        {

        }

    }

}
