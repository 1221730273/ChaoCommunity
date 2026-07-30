package com.ljc.chaocommunity.service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ljc.chaocommunity.mapper.FileRecordMapper;
import com.ljc.chaocommunity.pojo.entity.FileRecord;
import com.ljc.chaocommunity.service.FileService;
import com.ljc.chaocommunity.util.OssUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
public class FileServiceImpl implements FileService {

    @Autowired
    private FileRecordMapper fileRecordMapper;

    @Autowired
    private OssUtil ossUtil;

    @Override
    public void cleanTempFile() {
        // 1. 查询24小时前创建的、状态为0（未使用）的文件
        LocalDateTime expireTime = LocalDateTime.now().minusHours(24);
        LambdaQueryWrapper<FileRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FileRecord::getStatus, 0)
                .lt(FileRecord::getCreateTime, expireTime);
        List<FileRecord> records = fileRecordMapper.selectList(wrapper);

        if (records.isEmpty()) {
            log.info("没有需要清理的过期临时文件");
            return;
        }

        log.info("开始清理过期临时文件，共{}条", records.size());

        for (FileRecord record : records) {
            try {
                // 2. 删除OSS上的文件
                ossUtil.delete(record.getFilePath());
                log.info("已删除OSS文件: {}", record.getFilePath());

                // 3. 物理删除数据库记录
                fileRecordMapper.deleteById(record.getId());
                log.info("已删除file_record记录: id={}", record.getId());
            } catch (Exception e) {
                log.error("清理文件失败: filePath={}, id={}", record.getFilePath(), record.getId(), e);
            }
        }

        log.info("过期临时文件清理完成");
    }
}
