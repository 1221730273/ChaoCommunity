package com.ljc.chaocommunity.task;

import com.ljc.chaocommunity.service.FileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;


@Component
public class FileCleanTask {


    @Autowired
    private FileService fileService;


    /**
     * 每天凌晨3点清理临时文件
     */
    @Scheduled(cron = "0 0 3 * * ?")
    public void cleanTempFile(){

        fileService.cleanTempFile();

    }

}