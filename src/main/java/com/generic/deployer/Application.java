package com.generic.deployer;

import lombok.Getter;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.beans.factory.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.impl.StaticLoggerBinder;

//import org.springframework.stereotype.Controller;
import javax.imageio.ImageIO;
import javax.servlet.http.HttpServletResponse;

import java.awt.image.BufferedImage;
import java.io.BufferedWriter;
import java.io.File;
import java.util.Optional;
import java.util.Random;
import java.util.List;
import java.util.ArrayList;
import java.util.stream.Stream;
import java.io.StringReader;

import com.generic.deployer.controller.Deployer;

/**
 * Created by Hpatki on 12/22/2016.
 */

@SpringBootApplication
@ComponentScan ({"com.generic.deployer.controller",
                 "com.generic.deployer.interceptors",
                 "com.generic.deployer.errors"})
public class Application
{
    private static final Logger logger = LoggerFactory.getLogger (Application.class);
    @Getter static ConfigurableApplicationContext cntxt;

    public static void main(String[] args)
    {
        String os = System.getProperty("os.name");
        Deployer.uploadFolder = System.getenv("DEPLOY_DIR");
        Deployer.sftp = System.getenv("DEPLOY_SFTP_EXE");
        Deployer.identityFile = System.getenv("DEPLOY_IDENTITY_FILE");
        Deployer.usr = System.getenv("DEPLOY_REMOTE_USER");
        Deployer.passwd = System.getenv("DEPLOY_REMOTE_PASSWD");
        cntxt = SpringApplication.run(Application.class, args);
        logger.info ("Server has been created and is running now ...");
    }
}
