package com.ljc.chaocommunity.controller;

import com.ljc.chaocommunity.mapper.FileRecordMapper;
import com.ljc.chaocommunity.pojo.entity.FileRecord;
import com.ljc.chaocommunity.pojo.result.Result;
import com.ljc.chaocommunity.pojo.vo.UploadVO;
import com.ljc.chaocommunity.util.OssUtil;
import com.ljc.chaocommunity.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/file")
@Tag(name = "文件上传")
public class FileController {

    @Autowired
    private OssUtil ossUtil;

    @Autowired
    private FileRecordMapper fileRecordMapper;

    @PostMapping("/upload")
    @Operation(summary = "上传文件到临时目录")
    public Result<UploadVO> upload(@RequestParam("file") MultipartFile file) {
        OssUtil.UploadResult result = ossUtil.upload(file, "temp");

        FileRecord record = new FileRecord();
        record.setUserId(SecurityUtils.getCurrentUserId());
        record.setFileName(file.getOriginalFilename());
        record.setFilePath(result.objectKey());
        record.setUrl(result.url());
        record.setBizType("temp");
        record.setStatus(0);
        fileRecordMapper.insert(record);

        UploadVO vo = new UploadVO();
        vo.setFileId(record.getId());
        vo.setUrl(result.url());

        return Result.success(vo);
    }

    //TODO 修改涉及到图片上传的业务 把tmp目录下的文件移动到指定目录
    
    //TODO 定期清理临时目录

    //TODO 以后读写返回签名链接 
}
