package com.primihub.application.data;


import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.restdocs.headers.HeaderDocumentation.headerWithName;
import static org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.*;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.requestParameters;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@AutoConfigureRestDocs(outputDir = "target/generated-snippets")
@RunWith(SpringRunner.class)
@SpringBootTest
public class DataPsiControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Test
    public  void testGetPsiResourceList() throws Exception {
        // 项目添加
        this.mockMvc.perform(get("/psi/getPsiResourceList")
                .param("pageNo","1")
                .param("pageSize","5")
                .param("organId","1000")
                .param("resourceName",""))
                .andExpect(status().isOk())
                .andDo(document("getPsiResourceList",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        requestParameters(
                                parameterWithName("pageNo").description("第几页"),
                                parameterWithName("pageSize").description("每页条数"),
                                parameterWithName("organId").description("机构id"),
                                parameterWithName("resourceName").description("资源名称")
                        ),
                        responseFields(
                                fieldWithPath("code").description("返回码"),
                                fieldWithPath("msg").description("返回码描述"),
                                fieldWithPath("result").description("返回码结果"),
                                fieldWithPath("result.total").description("总共的数据量"),
                                fieldWithPath("result.pageSize").description("每页显示多少条"),
                                fieldWithPath("result.totalPage").description("共有多少页"),
                                fieldWithPath("result.index").description("当前是第几页"),
                                fieldWithPath("result.data").description("数据"),
                                fieldWithPath("result.data[].resourceId").description("资源id"),
                                fieldWithPath("result.data[].resourceName").description("资源名称"),
                                fieldWithPath("result.data[].resourceDesc").description("资源描述"),
                                fieldWithPath("result.data[].resourceAuthType").description("授权类型 1.公开 2.私有 2.授权"),
                                fieldWithPath("result.data[].resourceSource").description("资源来源 文件上传 数据库链接"),
                                fieldWithPath("result.data[].resourceNum").description("资源数"),
                                fieldWithPath("result.data[].fileId").description("文件id"),
                                fieldWithPath("result.data[].fileSize").description("文件大小"),
                                fieldWithPath("result.data[].fileSuffix").description("文件后缀"),
                                fieldWithPath("result.data[].fileRows").description("文件行数"),
                                fieldWithPath("result.data[].fileColumns").description("文件列数"),
                                fieldWithPath("result.data[].fileHandleField").description("文件字段"),
                                fieldWithPath("result.data[].fileHandleStatus").description("文件处理状态 0 未处理 1处理中 2处理完成"),
                                fieldWithPath("result.data[].fileContainsY").optional().description("文件字段中是否包含y字段 0否 1是"),
                                fieldWithPath("result.data[].fileYRows").optional().description("文件y字段有效行数"),
                                fieldWithPath("result.data[].fileYRatio").optional().description("文件y字段有效行数占总行数的比例"),
                                fieldWithPath("result.data[].dbId").description("数据库id"),
//                                fieldWithPath("result.data[].url").description("文件url"),
                                fieldWithPath("result.data[].userId").description("用户id"),
                                fieldWithPath("result.data[].userName").description("用户名称"),
                                fieldWithPath("result.data[].organId").description("机构id"),
                                fieldWithPath("result.data[].organName").description("机构名称"),
                                fieldWithPath("result.data[].createDate").description("创建时间"),
                                fieldWithPath("result.data[].tags[]").description("标签数据"),
                                fieldWithPath("result.data[].tags[].tagId").description("标签id"),
                                fieldWithPath("result.data[].tags[].tagName").description("标签名称"),
                                fieldWithPath("extra").description("额外信息")
                        )
                ));
    }
    @Test
    public  void testSaveDataPsi() throws Exception {
        this.mockMvc.perform(post("/psi/saveDataPsi")
                .param("ownOrganId","d881601a-0d51-4313-8813-382e758d7861")
                .param("ownResourceId","1")
                .param("ownKeyword","id")
                .param("otherOrganId","945908dc-bef5-4e39-be1a-40bdea66c03b")
                .param("otherResourceId","47")
                .param("otherKeyword","1")
                .param("resultName","huahuahau")
                .param("outputFilePathType","0")
                .param("outputNoRepeat","0")
                .param("outputContent","0")
                .param("psiTag","0")
                .param("outputFormat","csv")
                .param("remarks","")
                .param("resultOrganIds","1")
                .param("serverAddress","http://localhost:8099")
                .header("userId","1"))
                .andExpect(status().isOk())
                .andDo(document("saveDataPsi",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        requestHeaders(
                                headerWithName("userId").description("用户id 前端不用传")
                        ),
                        requestParameters(
                                parameterWithName("ownOrganId").description("本机构id"),
                                parameterWithName("ownResourceId").description("本机构资源id"),
                                parameterWithName("ownKeyword").description("本机构资源关键字"),
                                parameterWithName("otherOrganId").description("其他机构id"),
                                parameterWithName("otherResourceId").description("其他机构资源id"),
                                parameterWithName("otherKeyword").description("其他机构资源关键字"),
                                parameterWithName("outputFilePathType").description("文件路径输出类型 0默认 自动生成"),
                                parameterWithName("outputNoRepeat").description("输出内容是否不去重 默认0 不去重 1去重"),
                                parameterWithName("psiTag").description("是否对\"可统计\"的附加列做全表统计 默认0 是 1不是"),
                                parameterWithName("resultName").description("结果名称"),
                                parameterWithName("outputContent").description("输出内容 默认0 0交集 1差集"),
                                parameterWithName("outputFormat").description("输出格式"),
                                parameterWithName("resultOrganIds").description("结果获取方 多机构\",\"号间隔"),
                                parameterWithName("serverAddress").description("中心节点"),
                                parameterWithName("remarks").description("备注")
                        ),
                        responseFields(
                                fieldWithPath("code").description("返回码"),
                                fieldWithPath("msg").description("返回码描述"),
                                fieldWithPath("result").description("返回码结果"),
                                fieldWithPath("result.dataPsi").description("psi"),
                                fieldWithPath("result.dataPsi.id").description("psiId"),
                                fieldWithPath("result.dataPsi.ownOrganId").description("本机构id"),
                                fieldWithPath("result.dataPsi.ownResourceId").description("本机构资源id"),
                                fieldWithPath("result.dataPsi.ownKeyword").description("本机构资源关键字"),
                                fieldWithPath("result.dataPsi.otherOrganId").description("其他机构id"),
                                fieldWithPath("result.dataPsi.otherResourceId").description("其他机构资源id"),
                                fieldWithPath("result.dataPsi.otherKeyword").description("其他机构资源关键字"),
                                fieldWithPath("result.dataPsi.outputFilePathType").description("文件路径输出类型 0默认 自动生成"),
                                fieldWithPath("result.dataPsi.outputNoRepeat").description("输出内容是否不去重 默认0 不去重 1去重"),
                                fieldWithPath("result.dataPsi.resultName").description("结果名称"),
                                fieldWithPath("result.dataPsi.outputContent").description("输出内容 默认0 0交集 1差集"),
                                fieldWithPath("result.dataPsi.outputFormat").description("输出格式 默认csv"),
                                fieldWithPath("result.dataPsi.resultOrganIds").description("结果获取方 多机构\",\"号间隔"),
                                fieldWithPath("result.dataPsi.tag").description("后端psi协议"),
                                fieldWithPath("result.dataPsi.serverAddress").description("中心节点地址"),
                                fieldWithPath("result.dataPsi.remarks").description("备注"),
                                fieldWithPath("result.dataPsi.userId").description("用户id"),
                                fieldWithPath("result.dataPsiTask").description("psi 任务信息"),
                                fieldWithPath("result.dataPsiTask.psiId").description("psi id"),
                                fieldWithPath("result.dataPsiTask.taskId").description("psi任务id"),
                                fieldWithPath("result.dataPsiTask.taskIdName").description("对外展示的任务uuid 同时也是文件名称"),
                                fieldWithPath("result.dataPsiTask.taskState").description("运行状态 0未运行 1完成 2运行中 3失败 默认0"),
                                fieldWithPath("result.dataPsiTask.ascription").description("结果归属"),
                                fieldWithPath("result.dataPsiTask.ascriptionType").description("0一方 1双方"),
                                fieldWithPath("result.dataPsiTask.createDate").description("创建时间"),
                                fieldWithPath("extra").description("额外信息")
                        )
                ));
    }

    @Test
    public  void testGetPsiResourceDataList() throws Exception {
        // 项目添加
        this.mockMvc.perform(get("/psi/getPsiResourceAllocationList")
                .param("pageNo","2")
                .param("pageSize","5")
                .param("serverAddress","http://localhost:8099")
                .param("resourceName","")
                .param("organId",""))
                .andExpect(status().isOk())
                .andDo(document("getPsiResourceAllocationList",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        requestParameters(
                                parameterWithName("pageNo").description("第几页"),
                                parameterWithName("pageSize").description("每页条数"),
                                parameterWithName("serverAddress").description("中心节点地址"),
                                parameterWithName("resourceName").description("资源名称"),
                                parameterWithName("organId").description("机构id")
                        ),
                        responseFields(
                                fieldWithPath("code").description("返回码"),
                                fieldWithPath("msg").description("返回码描述"),
                                fieldWithPath("result").description("返回码结果"),
                                fieldWithPath("result.total").description("总共的数据量"),
                                fieldWithPath("result.pageSize").description("每页显示多少条"),
                                fieldWithPath("result.totalPage").description("共有多少页"),
                                fieldWithPath("result.index").description("当前是第几页"),
                                fieldWithPath("result.data").description("数据"),
                                fieldWithPath("result.data[].resourceId").description("资源id"),
                                fieldWithPath("result.data[].resourceName").description("资源名称"),
                                fieldWithPath("result.data[].organId").description("机构id"),
                                fieldWithPath("result.data[].keywordList[]").description("关键字"),
                                fieldWithPath("extra").description("额外信息")
                        )
                ));
    }

    @Test
    public  void testGetOrganPsiTask() throws Exception {
        // 项目添加
        this.mockMvc.perform(get("/psi/getOrganPsiTask")
                .param("pageNo","1")
                .param("pageSize","5")
                .param("organId","1")
                .header("organId","1")
                .header("userId","1"))
                .andExpect(status().isOk())
                .andDo(document("getOrganPsiTask",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        requestHeaders(
                                headerWithName("organId").description("机构id 前端不用传"),
                                headerWithName("userId").description("用户id 前端不用传")
                        ),
                        requestParameters(
                                parameterWithName("pageNo").description("第几页"),
                                parameterWithName("pageSize").description("每页条数"),
                                parameterWithName("organId").description("机构id")
                        ),
                        responseFields(
                                fieldWithPath("code").description("返回码"),
                                fieldWithPath("msg").description("返回码描述"),
                                fieldWithPath("result").description("返回码结果"),
                                fieldWithPath("result.total").description("总共的数据量"),
                                fieldWithPath("result.pageSize").description("每页显示多少条"),
                                fieldWithPath("result.totalPage").description("共有多少页"),
                                fieldWithPath("result.index").description("当前是第几页"),
                                fieldWithPath("result.data").description("数据"),
                                fieldWithPath("result.data[].dataPsiId").description("psiId"),
                                fieldWithPath("result.data[].resultName").description("结果表名"),
                                fieldWithPath("result.data[].fileRows").optional().description("总行数"),
                                fieldWithPath("result.data[].fileColumns").description("总列数"),
                                fieldWithPath("result.data[].taskId").description("任务id 真实id"),
                                fieldWithPath("result.data[].taskIdName").description("展示的任务id"),
                                fieldWithPath("result.data[].createDate").description("创建日期"),
                                fieldWithPath("result.data[].updateDate").description("修改日期"),
                                fieldWithPath("extra").description("额外信息")
                        )
                ));
    }

    @Test
    public  void testGetPsiTaskList() throws Exception {
        // 项目添加
        this.mockMvc.perform(get("/psi/getPsiTaskList")
                .param("pageNo","1")
                .param("pageSize","5")
                .header("userId","1"))
                .andExpect(status().isOk())
                .andDo(document("getPsiTaskList",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        requestHeaders(
                                headerWithName("userId").description("用户id 前端不用传")
                        ),
                        requestParameters(
                                parameterWithName("pageNo").description("第几页"),
                                parameterWithName("pageSize").description("每页条数")
                        ),
                        responseFields(
                                fieldWithPath("code").description("返回码"),
                                fieldWithPath("msg").description("返回码描述"),
                                fieldWithPath("result").description("返回码结果"),
                                fieldWithPath("result.total").description("总共的数据量"),
                                fieldWithPath("result.pageSize").description("每页显示多少条"),
                                fieldWithPath("result.totalPage").description("共有多少页"),
                                fieldWithPath("result.index").description("当前是第几页"),
                                fieldWithPath("result.data").description("数据"),
                                fieldWithPath("result.data[].dataPsiId").description("psiId"),
                                fieldWithPath("result.data[].resultName").description("结果表名"),
                                fieldWithPath("result.data[].taskId").description("任务id 真实id"),
                                fieldWithPath("result.data[].taskIdName").description("展示的任务id"),
                                fieldWithPath("result.data[].taskState").description("运行状态 0未运行 1完成 2运行中 3失败 默认0"),
                                fieldWithPath("result.data[].ascription").description("归属"),
                                fieldWithPath("result.data[].createDate").description("创建日期"),
                                fieldWithPath("extra").description("额外信息")
                        )
                ));
    }

    @Test
    public  void testGetPsiTaskDetails() throws Exception {
        // 项目添加
        this.mockMvc.perform(get("/psi/getPsiTaskDetails")
                .param("taskId","12")
                .header("userId","1"))
                .andExpect(status().isOk())
                .andDo(document("getPsiTaskDetails",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        requestHeaders(
                                headerWithName("userId").description("用户id 前端不用传")
                        ),
                        requestParameters(
                                parameterWithName("taskId").description("任务id")
                        ),
                        responseFields(
                                fieldWithPath("code").description("返回码"),
                                fieldWithPath("msg").description("返回码描述"),
                                fieldWithPath("result").description("返回码结果"),
                                fieldWithPath("result").description("数据"),
                                fieldWithPath("result.id").description("psiId"),
                                fieldWithPath("result.ownOrganId").description("本机构id"),
                                fieldWithPath("result.ownOrganName").description("本机构名称"),
                                fieldWithPath("result.ownResourceId").description("本机构资源id"),
                                fieldWithPath("result.ownResourceName").description("本机构资源名称"),
                                fieldWithPath("result.ownKeyword").description("本机构资源关键字"),
                                fieldWithPath("result.otherOrganId").description("其他机构id"),
                                fieldWithPath("result.otherOrganName").description("其他机构名称"),
                                fieldWithPath("result.otherResourceId").description("其他机构资源id"),
                                fieldWithPath("result.otherResourceName").description("其他机构资源名称"),
                                fieldWithPath("result.otherKeyword").description("其他机构资源关键字"),
                                fieldWithPath("result.outputFilePathType").description("文件路径输出类型 0默认 自动生成"),
                                fieldWithPath("result.outputNoRepeat").description("输出内容是否不去重 默认0 不去重 1去重"),
                                fieldWithPath("result.tag").description("psi 后端协议"),
                                fieldWithPath("result.resultName").description("结果名称"),
                                fieldWithPath("result.outputContent").description("输出内容 默认0 0交集 1差集"),
                                fieldWithPath("result.outputFormat").description("输出格式"),
                                fieldWithPath("result.resultOrganIds").description("结果获取方 多机构id\",\"号间隔"),
                                fieldWithPath("result.resultOrganName").description("结果获取方 多机构名称\",\"号间隔"),
                                fieldWithPath("result.remarks").description("备注"),
                                fieldWithPath("result.taskId").description("真实任务id"),
                                fieldWithPath("result.taskState").description("任务状态 0未运行 1完成 2运行中 3失败 4取消 默认0 "),
                                fieldWithPath("result.taskIdName").description("展示任务id"),
                                fieldWithPath("extra").description("额外信息")
                        )
                ));
    }
    @Test
    public  void testDelPsiTask() throws Exception {
        // 项目添加
        this.mockMvc.perform(get("/psi/delPsiTask")
                .param("taskId","134")
                .header("userId","1"))
                .andExpect(status().isOk())
                .andDo(document("delPsiTask",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        requestHeaders(
                                headerWithName("userId").description("用户id 前端不用传")
                        ),
                        requestParameters(
                                parameterWithName("taskId").description("psi 任务id")
                        ),
                        responseFields(
                                fieldWithPath("code").description("返回码"),
                                fieldWithPath("msg").description("返回码描述"),
                                fieldWithPath("result").description("返回码结果"),
                                fieldWithPath("extra").description("额外信息")
                        )
                ));
    }

    @Test
    public  void testCancelPsiTask() throws Exception {
        // 项目添加
        this.mockMvc.perform(get("/psi/cancelPsiTask")
                .param("taskId","2")
                .header("userId","1"))
                .andExpect(status().isOk())
                .andDo(document("cancelPsiTask",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        requestHeaders(
                                headerWithName("userId").description("用户id 前端不用传")
                        ),
                        requestParameters(
                                parameterWithName("taskId").description("psi 任务id")
                        ),
                        responseFields(
                                fieldWithPath("code").description("返回码"),
                                fieldWithPath("msg").description("返回码描述"),
                                fieldWithPath("result").description("返回码结果"),
                                fieldWithPath("extra").description("额外信息")
                        )
                ));
    }
    @Test
    public  void testRetryPsiTask() throws Exception {
        // 项目添加
        this.mockMvc.perform(get("/psi/retryPsiTask")
                .param("taskId","2")
                .header("userId","1"))
                .andExpect(status().isOk())
                .andDo(document("retryPsiTask",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        requestHeaders(
                                headerWithName("userId").description("用户id 前端不用传")
                        ),
                        requestParameters(
                                parameterWithName("taskId").description("psi 任务id")
                        ),
                        responseFields(
                                fieldWithPath("code").description("返回码"),
                                fieldWithPath("msg").description("返回码描述"),
                                fieldWithPath("result").description("返回码结果"),
                                fieldWithPath("extra").description("额外信息")
                        )
                ));
    }
}
