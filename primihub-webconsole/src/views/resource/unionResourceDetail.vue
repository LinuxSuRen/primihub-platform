<template>
  <div class="app-container">
    <h2>资源详情</h2>
    <div class="detail">
      <el-descriptions title="资源信息" :column="2" label-class-name="detail-title">
        <el-descriptions-item label="资源名称">{{ resource.resourceName }}</el-descriptions-item>
        <el-descriptions-item label="标签">
          <el-tag v-for="(tag,index) in resource.resourceTag" :key="index" type="primary" size="mini">{{ tag }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="授权方式">
          <span>{{ resource.resourceAuthType | authTypeFilter }}</span>
        </el-descriptions-item>
      </el-descriptions>
    </div>
    <div class="detail">
      <el-descriptions title="数据信息" :column="2" label-class-name="detail-title">
        <el-descriptions-item label="数据来源">{{ resource.resourceAuthType | sourceFilter }}</el-descriptions-item>
        <el-descriptions-item label="特征量">{{ resource.resourceRowsCount }}</el-descriptions-item>
        <el-descriptions-item label="样本量">{{ resource.resourceColumnCount }}</el-descriptions-item>
        <el-descriptions-item label="正例样本数量">{{ resource.resourceYRowsCount }}</el-descriptions-item>
        <el-descriptions-item label="正例样本比例">{{ resource.resourceYRatio }}%</el-descriptions-item>
        <el-descriptions-item label="是否包含Y值">{{ resource.resourceContainsY === 1? '是': '否' }}</el-descriptions-item>
        <el-descriptions-item label="字段信息">{{ resource.resourceColumnNameList }}</el-descriptions-item>
      </el-descriptions>
    </div>
  </div>
</template>

<script>
import { getDataResource } from '@/api/fusionResource'

export default {
  data() {
    return {
      resource: {},
      resourceAuthType: 1,
      authType: '私有',
      originName: '',
      originList: [],
      resourceId: this.$route.params.id,
      serverAddress: this.$route.query.serverAddress || ''
    }
  },
  async created() {
    await this.getDataResource()
  },
  methods: {
    async getDataResource() {
      const res = await getDataResource({
        resourceId: this.resourceId,
        serverAddress: this.serverAddress
      })
      if (res.code === 0) {
        this.resource = res.result
        this.resourceAuthType = this.resource.resourceAuthType
      }
    }

  }
}
</script>

<style lang="scss" scoped>
@import "~@/styles/variables.scss";
::v-deep .el-tag{
  margin:  0 3px;
}
::v-deep .detail-title{
  width: 100px;
  text-align: right;
  justify-content: flex-end;
}
::v-deep .el-descriptions__title, h3{
  font-size: 18px;
  border-left: 3px solid $mainColor;
  padding-left: 10px;
}
::v-deep .el-descriptions__body{
  color: rgba(0,0,0,.85);
}

.justify-content-center{
  justify-content: center;
}
.justify-content-between{
  justify-content: space-between;
}
.detail {
  padding: 20px 0 20px 20px;
  border-top: 1px solid #f0f0f0;
}
.auth-dialog{
  width: 100%;
  text-align: center;
}
.origin-select{
  margin-top: 20px;
}
.button{
  margin-left: 30px;
}
</style>
