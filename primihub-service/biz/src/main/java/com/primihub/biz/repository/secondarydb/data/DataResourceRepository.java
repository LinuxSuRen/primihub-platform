package com.primihub.biz.repository.secondarydb.data;

import com.primihub.biz.entity.data.po.*;
import com.primihub.biz.entity.data.po.*;
import com.primihub.biz.entity.data.vo.DataResourceAuthRecordVo;
import com.primihub.biz.entity.data.vo.DataResourceRecordVo;
import com.primihub.biz.entity.data.vo.ResourceTagListVo;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Repository
public interface DataResourceRepository {
    /**
     * 查询所以的资源标签
     * @return
     */
    List<String> queryAllResourceTag();

    List<DataResource> queryDataResource(Map<String,Object> paramMap);

    List<DataResource> queryDataResourceByResourceIds(@Param("resourceIds")Set<Long> resourceIds);

    List<DataResourceTag> queryTagsByResourceId(Long resourceId);

    DataResource queryDataResourceById(Long resourceId);

    DataResource queryDataResourceByResourceFusionId(String resourceFusionId);

    Integer queryDataResourceCount(Map<String,Object> paramMap);

    List<Long> queryResourceTagRelation(Long resourceId);

    List<DataResourceRecordVo> queryDataResourceByIds(@Param("resourceIds") Set<Long> resourceIds);

    List<ResourceTagListVo> queryDataResourceListTags(@Param("resourceIds") List<Long> resourceIds);

    Integer queryResourceProjectRelationCount(String resourceId);

    List<DataFileField> queryDataFileField(Map<String,Object> paramMap);

    List<DataFileField> queryDataFileFieldByFileId(@Param("fileId")Long fileId,@Param("resourceId")Long resourceId);

    Integer queryDataFileFieldCount(Map<String,Object> paramMap);

    List<DataResource> findCopyResourceList(@Param("startOffset")Long startOffset, @Param("endOffset")Long endOffset);

    List<Long> queryDataResourceIds(@Param("pageSize") Integer pageSize,@Param("pId")Long pId);

    Long findMaxDataResource();

    List<DataResourceVisibilityAuth> findAuthOrganByResourceId(@Param("resourceIds") List<Long> resourceIds);
}
