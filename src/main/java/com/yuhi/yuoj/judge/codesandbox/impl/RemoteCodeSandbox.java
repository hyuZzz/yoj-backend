package com.yuhi.yuoj.judge.codesandbox.impl;

import com.yuhi.yuoj.judge.codesandbox.CodeSandbox;
import com.yuhi.yuoj.judge.codesandbox.model.ExecuteCodeRequest;
import com.yuhi.yuoj.judge.codesandbox.model.ExecuteCodeResponse;
/**
 * 远程代码沙箱（仅为了跑通业务流程）
 */
public class RemoteCodeSandbox implements CodeSandbox {
    @Override
    public ExecuteCodeResponse executeCode(ExecuteCodeRequest executeCodeRequest) {
        System.out.println("远程代码沙箱");
        return null;
    }
}
