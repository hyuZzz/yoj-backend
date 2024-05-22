package com.yuhi.yojbackendjudgeservice.judge.codesandbox.impl;


import com.yuhi.yojbackendjudgeservice.judge.codesandbox.CodeSandBox;
import com.yuhi.yojbackendmodel.model.codesandbox.ExecuteCodeRequest;
import com.yuhi.yojbackendmodel.model.codesandbox.ExecuteCodeResponse;

/**
 * 第三方代码沙箱（调用网上现成的代码沙箱）
 */
public class ThirdPartyCodeSandbox implements CodeSandBox {
    @Override
    public ExecuteCodeResponse executeCode(ExecuteCodeRequest executeCodeRequest) {
        System.out.println("第三方代码沙箱");
        return null;
    }
}
