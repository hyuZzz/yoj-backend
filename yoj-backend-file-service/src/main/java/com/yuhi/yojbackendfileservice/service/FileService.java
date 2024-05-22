package com.yuhi.yojbackendfileservice.service;

import org.springframework.web.multipart.MultipartFile;

/**
 * @author yuhi
 */
public interface FileService {
    /**
     * 上传头像到OSS
     *
     * @param file
     * @return
     */
    String uploadFileAvatar(MultipartFile file);
}
