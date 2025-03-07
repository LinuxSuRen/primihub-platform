<template>
  <div class="app-container">
    <h2><span v-if="isEditPage">编辑</span><span v-else>新建</span>资源</h2>
    <el-form
      ref="dataForm"
      :model="dataForm"
      :rules="dataRules"
      label-width="100px"
      class="demo-dataForm"
    >
      <el-form-item label="资源名称" prop="resourceName">
        <div class="item-wrap-normal">
          <el-input
            v-model="dataForm.resourceName"
            maxlength="20"
            minlength="3"
            show-word-limit
          />
        </div>
      </el-form-item>
      <el-form-item label="资源描述" prop="resourceDesc">
        <div class="item-wrap-normal">
          <el-input
            v-model="dataForm.resourceDesc"
            type="textarea"
            maxlength="200"
            minlength="3"
            show-word-limit
          />
        </div>
      </el-form-item>
      <el-form-item label="关键词" prop="tags">
        <div class="item-wrap-normal">
          <el-tag
            v-for="(tag,index) in dataForm.tags"
            :key="index"
            closable
            @close="handleClose(tag)"
          >
            {{ tag }}
          </el-tag>
          <el-input
            v-if="tagInputVisible"
            ref="saveTagInput"
            v-model="tagInputValue"
            class="input-new-tag"
            size="small"
            maxlength="10"
            minlength="1"
            show-word-limit
            @keyup.enter.native="handleInputConfirm"
            @blur="handleInputConfirm"
          />
          <el-button class="button-new-tag" size="small" icon="el-icon-plus" type="primary" plain @click="showInput">添加关键词</el-button>
          <p class="tips">关键词个数不能超过5个</p>
        </div>
      </el-form-item>
      <el-form-item label="授权方式" prop="resourceAuthType">
        <div class="item-wrap-normal auth-container">
          <el-radio-group v-model="dataForm.resourceAuthType" @change="handleAuthTypeChange">
            <el-radio :label="1">公开</el-radio>
            <el-radio :label="2">私有</el-radio>
            <el-radio :label="3">指定机构可见</el-radio>
          </el-radio-group>
          <template v-if="dataForm.resourceAuthType === 3">
            <Cascader
              v-if="showCascader"
              ref="connectRef"
              :show-all-levels="false"
              clearable
              :value="cascaderValue"
              @change="handleOrganChange"
            />
          </template>
        </div>
      </el-form-item>
      <el-form-item label="资源来源" prop="resourceSource">
        <template v-if="!isEditPage">
          <div class="item-wrap-normal">
            <el-radio-group v-model="dataForm.resourceSource">
              <el-radio :label="1">文件上传</el-radio>
            </el-radio-group>
          </div>
          <template v-if="dataForm.resourceSource === 1">
            <upload :max-size="fileMaxSize" :single="true" @success="handleUploadSuccess" />
          </template>
        </template>
        <template v-else>
          <span>{{ dataForm.resourceSource | sourceFilter }}</span>
        </template>
      </el-form-item>
      <el-row
        :gutter="20"
        class="data-container"
        element-loading-text="加载中"
        element-loading-spinner="el-icon-loading"
      >
        <el-col v-if="fieldList.length > 0" :span="12">
          <h3>字段信息</h3>
          <EditResourceTable
            border
            height="500"
            :data="fieldList"
            :is-edit-page="isEditPage"
            @change="handleResourceChange"
          />
        </el-col>
        <el-col v-if="dataList.length >0" :span="12">
          <h3>数据资源预览</h3>
          <ResourcePreviewTable :data="dataList" height="500" />
        </el-col>
      </el-row>
      <el-form-item>
        <el-button
          type="primary"
          @click="submitForm"
        >
          保存
        </el-button>
        <el-button @click="goBack">取消</el-button>
      </el-form-item>
    </el-form>
  </div>
</template>

<script>
import Upload from '@/components/Upload'
import EditResourceTable from '@/components/EditResourceTable'
import ResourcePreviewTable from '@/components/ResourcePreviewTable'
import Cascader from '@/components/Cascader'
import { saveResource, getResourceDetail, resourceFilePreview } from '@/api/resource'

export default {
  components: {
    Upload,
    EditResourceTable,
    ResourcePreviewTable,
    Cascader
  },
  filters: {
    sourceFilter(source) {
      const sourceMap = {
        1: '文件上传',
        2: '数据库'
      }
      return sourceMap[source]
    }
  },
  data() {
    return {
      isEditPage: false,
      fileUrl: '',
      dataForm: {
        resourceName: '',
        resourceDesc: '',
        tags: [],
        resourceSource: 1,
        resourceAuthType: 1,
        fileId: -1, // 文件id
        fieldList: [],
        fusionOrganList: []
      },
      dataRules: {
        resourceName: [
          { required: true, message: '请输入资源名称', trigger: 'blur' },
          { min: 3, max: 20, message: '长度在 3 到 20 个字符', trigger: 'blur' }
        ],
        resourceDesc: [
          { required: true, message: '请输入资源描述', trigger: 'blur' },
          { min: 0, max: 200, message: '长度200字符以内', trigger: 'blur' }
        ],
        tags: [
          { required: true, message: '请输入关键词', trigger: 'blur' }
        ],
        resourceSource: [
          { required: true, message: '请选择来源', trigger: 'change' }
        ],
        resourceAuthType: [
          { required: true, message: '请选择授权方式', trigger: 'change' }
        ]
      },
      tagInputVisible: false,
      tagInputValue: '',
      fileMaxSize: 1024 * 1024 * 1, // file limit 1MB
      resource: null,
      loading: false,
      fieldTypeList: [{
        value: 1,
        label: 'Integer'
      }, {
        value: 2,
        label: 'Long'
      }, {
        value: 3,
        label: 'Double'
      }, {
        value: 4,
        label: 'Enum'
      }, {
        value: 5,
        label: 'String'
      }],
      dataList: [], // resource preview
      fieldList: [], // resource field info
      cascaderValue: [],
      showCascader: false,
      props: {
        multiple: true,
        checkStrictly: false,
        emitPath: true,
        lazyLoad: this.lazyLoad,
        leaf: 'leaf',
        lazy: true
      },
      authOrganList: []
    }
  },
  async created() {
    this.isEditPage = this.$route.name === 'ResourceEdit'
    this.resourceId = this.$route.params.id
    if (this.isEditPage) {
      await this.getResourceDetail()
    }
  },
  methods: {
    handleResourceChange(data) {
      this.fieldList = data
      this.dataForm.fieldList = this.formatParams()
    },
    handleUploadSuccess(fileId) {
      this.dataForm.fileId = fileId
      this.resourceFilePreview()
    },
    resourceFilePreview() {
      this.fieldListLoading = true
      resourceFilePreview({ fileId: this.dataForm.fileId }).then(res => {
        this.dataList = res.result.dataList
        this.fieldList = res.result.fieldList
        this.dataForm.fieldList = this.formatParams()
        this.fieldListLoading = false
      })
    },
    formatParams() {
      const fieldList = []
      this.fieldList && this.fieldList.forEach(item => {
        const { fieldId, fieldName, fieldType, fieldDesc = '', relevance, grouping, protectionStatus } = item
        fieldList.push({
          fieldId,
          fieldName,
          fieldType: this.fieldTypeList.filter(item => item.value === fieldType)[0]?.label.toLowerCase() || fieldType,
          fieldDesc,
          relevance: relevance === true ? 1 : 0,
          grouping: grouping === true ? 1 : 0,
          protectionStatus: protectionStatus === true ? 1 : 0
        })
      })
      return fieldList
    },
    handleClose(tag) {
      this.dataForm.tags.splice(this.dataForm.tags.indexOf(tag), 1)
    },
    showInput() {
      if (this.dataForm.tags.length > 4) {
        this.$message({
          message: '关键词个数不能超过5个',
          type: 'warning'
        })
        return
      }
      this.tagInputVisible = true
    },
    handleInputConfirm() {
      if (this.tagInputValue) {
        this.dataForm.tags.push(this.tagInputValue)
      }
      this.tagInputVisible = false
      this.tagInputValue = ''
    },
    handelTagsInputFocus() {
      if (this.dataForm.tags.length > 4) {
        this.$message({
          message: '关键词个数不能超过5个',
          type: 'warning'
        })
      }
    },
    async submitForm() {
      this.$refs['dataForm'].validate(async(valid) => {
        if (valid) {
          if (this.dataForm.fileId === -1) {
            this.$message({
              message: '请先上传文件',
              type: 'warning'
            })
            this.loading = false
            return
          }
          if (this.isEditPage) {
            Object.assign(this.dataForm, {
              resourceId: this.$route.params.id
            })
          }
          this.loading = true
          saveResource(JSON.stringify(this.dataForm)).then(res => {
            this.loading = false
            if (res.code === 0) {
              this.toResourceDetail(res.result.resourceId)
            } else {
              this.loading = false
            }
          })
        } else {
          this.loading = false
          console.log('error submit!!')
          return false
        }
      })
    },
    toResourceDetail(id) {
      this.$router.push({
        name: 'ResourceDetail',
        params: { id }
      })
    },
    async getResourceDetail() {
      this.loading = true
      const { result = {}} = await getResourceDetail(this.resourceId)
      const { resource, dataList, fieldList, authOrganList } = result
      const { resourceName, resourceDesc, resourceAuthType, resourceSource, tags, fileId, url } = resource
      this.resource = resource
      this.dataForm.resourceName = resourceName
      this.dataForm.resourceDesc = resourceDesc
      this.dataForm.resourceAuthType = resourceAuthType
      this.dataForm.resourceSource = resourceSource
      this.dataForm.fileId = fileId
      this.dataList = dataList || []
      this.fieldList = fieldList || []
      this.authOrganList = authOrganList || []
      this.dataForm.fieldList = this.formatParams()
      tags.forEach(item => {
        this.dataForm.tags.push(item.tagName)
      })
      this.fileUrl = url
      this.loading = false
      if (this.authOrganList.length) {
        this.getCascaderValue()
      }
    },
    goBack() {
      this.$router.replace({
        name: 'ResourceList'
      })
    },
    async handleAuthTypeChange(value) {
      if (value === 3) {
        this.showCascader = true
        this.resourceAuthType = value
      }
    },
    getCascaderValue() {
      for (let index = 0; index < this.authOrganList.length; index++) {
        const item = this.authOrganList[index]
        const organGlobalId = item.organGlobalId
        const current = [item.organServerAddress, organGlobalId]
        this.cascaderValue.push(current)
      }
      this.showCascader = false
      setTimeout(() => {
        this.showCascader = true
      }, 30)
      this.listLoading = false
    },
    handleOrganChange(data) {
      this.cascaderValue = data.valueList
      this.dataForm.fusionOrganList = data.fusionOrganList
    }
  }
}
</script>
<style lang="scss" scoped>
::v-deep .el-tag{
  margin: 0 3px;
}
::v-deep .el-upload-dragger{
  width: 250px;
  height: 100px;
  .el-icon-upload{
    margin: 20px 0;
  }
  // padding: 20px;
}
.item-wrap-normal {
  width: 660px;
}
.resource-box {
  display: flex;
  flex-wrap: wrap;
  .item {
    flex-shrink: 0;
    margin-bottom: 20px;
    margin-right: 20px;
  }
}
.el-upload__text{
  display: inline-block;
}
.el-tag + .el-tag {
  // max-width: 50px;
  overflow: hidden;
  vertical-align: middle;
  // margin-left: 10px;
}
.button-new-tag {
  margin-left: 10px;
  height: 32px;
  line-height: 30px;
  padding-top: 0;
  padding-bottom: 0;
}
.input-new-tag {
  width: 90px;
  margin-left: 10px;
  vertical-align: bottom;
}
.data-container{
  padding: 0 30px 30px;
}
.tips{
  font-size: 12px;
  color: #999;
  line-height: 1;
}
.auth-container{
  position: relative;
}
::v-deep .el-cascader{
  width: 300px;
  position: absolute;
  top: 00px;
  left: 290px;
  z-index: 12;
}
::v-deep .el-table,::v-deep .el-divider__text, .el-link{
  font-size: 12px;
}
</style>
