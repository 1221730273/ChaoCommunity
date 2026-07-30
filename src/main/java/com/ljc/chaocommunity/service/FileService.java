package com.ljc.chaocommunity.service;

public interface FileService {

    /**
     * 清理过期临时文件（status=0 且创建超过24小时的文件）
     */
    void cleanTempFile();
}
