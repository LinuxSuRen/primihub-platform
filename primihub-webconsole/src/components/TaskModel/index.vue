<template>
  <div v-loading="listLoading" class="container" :class="{'disabled': task.taskState === 5, 'model': type === 'model'}">
    <div class="section">
      <h3>模型信息</h3>
      <el-descriptions>
        <el-descriptions-item label="模型名称">{{ model.modelName }}</el-descriptions-item>
        <el-descriptions-item label="模型描述">{{ model.modelDesc }}</el-descriptions-item>
        <el-descriptions-item label="模型模版">V-XGBoost</el-descriptions-item>
        <template v-if="type==='model'">
          <el-descriptions-item label="任务ID"> <el-link type="primary" @click="toModelTaskDetail">{{ task.taskIdName }}</el-link></el-descriptions-item>
        </template>
        <el-descriptions-item label="模型ID">{{ model.modelId }}</el-descriptions-item>
        <template v-if="type==='model'">
          <el-descriptions-item label="角色">{{ task.isCooperation === 1 ? '参与方' : '发起方' }}</el-descriptions-item>
        </template>
        <template v-if="model.yvalueColumn">
          <el-descriptions-item label="Y值字段"><el-tag type="mini" size="mini">{{ model.yvalueColumn }}</el-tag></el-descriptions-item>
        </template>
        <template v-if="type==='model'">
          <el-descriptions-item label="建模完成时间">{{ task.taskEndDate }}</el-descriptions-item>
        </template>
      </el-descriptions>
      <div v-if="type === 'model' && task.taskState !== 5 && task.isCooperation === 0" class="buttons">
        <el-button type="danger" icon="el-icon-delete" @click="deleteModelTask">删除模型</el-button>
      </div>
    </div>

    <div class="section">
      <h3>模型资源</h3>
      <el-table
        :data="modelResources"
        border
      >
        <el-table-column
          prop="resourceName"
          label="资源名称"
        />
        <el-table-column
          prop="organName"
          label="所属机构"
        />
        <el-table-column
          prop="fileNum"
          label="原始记录数"
        />
        <el-table-column
          prop="alignmentNum"
          label="对齐后记录数量"
        />
        <el-table-column
          prop="primitiveParamNum"
          label="原始变量数量"
        />
        <el-table-column
          prop="modelParamNum"
          label="入模变量数量"
        />
      </el-table>
    </div>
    <div class="section">
      <h3>模型耗时</h3>
      <el-descriptions :column="1" label-class-name="time-consuming-label" :colon="false">
        <el-descriptions-item label-class-name="total-time-label">
          <span class="el-icon-time" />
          <span class="total-time-label">总耗时:</span>
          <span v-if="model.totalTime" class="total-time">{{ model.totalTime | timeFilter }}</span>
        </el-descriptions-item>
        <el-descriptions-item v-for="item in modelComponent" :key="item.componentId" :label="item.componentName">
          <el-progress :percentage="item.timeRatio * 100" status="success" class="progress-line" />
          <span class="el-icon-time" />
          <span>耗时 {{ item.timeConsuming | timeFilter }}</span>
        </el-descriptions-item>
      </el-descriptions>
    </div>
    <div class="section">
      <h3>模型评估指标</h3>
      <el-descriptions class="quotas" :column="1" border :label-style="{'width':'300px'}">
        <el-descriptions-item label="① ROOT MEAN SQUARED ERROR">{{ anotherQuotas.rootMeanSquaredError }}</el-descriptions-item>
        <el-descriptions-item label="② MEAN SQUARED ERROR">{{ anotherQuotas.meanSquaredError }}</el-descriptions-item>
        <el-descriptions-item label="③ EXPLAINED VARIANCE">{{ anotherQuotas.explainedVariance }}</el-descriptions-item>
        <el-descriptions-item label="④ MEAN SQUARED LOG ERROR">{{ anotherQuotas.meanSquaredLogError }}</el-descriptions-item>
        <el-descriptions-item label="⑤ R2 SCORE">{{ anotherQuotas.r2Score }}</el-descriptions-item>
        <el-descriptions-item label="⑥ MEAN ABSOLUTE ERROR">{{ anotherQuotas.meanAbsoluteError }}</el-descriptions-item>
        <el-descriptions-item label="⑦ MEDIAN ABSOLUTE ERROR">{{ anotherQuotas.medianAbsoluteError }}</el-descriptions-item>
      </el-descriptions>
    </div>
  </div>
</template>

<script>
import { getModelDetail } from '@/api/model'
import { deleteTask } from '@/api/task'

export default {
  filters: {
    quotaTypeFilter(type) {
      const typeMap = {
        1: '训练样本集',
        2: '测试样本集'
      }
      return typeMap[type]
    }
  },
  props: {
    state: {
      type: Number,
      default: 0
    },
    projectStatus: {
      type: Number,
      default: 0
    },
    type: {
      type: String,
      default: 'task'
    }
  },
  data() {
    return {
      listLoading: false,
      model: {},
      modelQuotas: [],
      modelResources: [],
      modelId: 0,
      modelComponent: [],
      lineChartData: [],
      anotherQuotas: [],
      taskState: null,
      projectId: 0,
      task: {}
    }
  },
  created() {
    this.modelId = this.$route.query.modelId
    this.taskId = this.$route.query.taskId || this.$route.params.taskId
    this.fetchData()
  },
  methods: {
    toModelTaskDetail() {
      this.$router.push({
        path: `/project/detail/${this.projectId}/task/${this.taskId}`
      })
    },
    deleteModelTask() {
      this.$confirm('此操作将永久删除该模型, 是否继续?', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        this.listLoading = true
        deleteTask(this.taskId).then(res => {
          if (res.code === 0) {
            this.$message({
              message: '删除成功',
              type: 'success',
              duration: 1000
            })
            this.fetchData()
          }
          this.listLoading = false
        }).catch(() => {
          this.listLoading = false
        })
      })
    },
    fetchData() {
      this.listLoading = true
      getModelDetail({ taskId: this.taskId }).then((response) => {
        this.listLoading = false
        console.log('response.data', response.result)
        const { model, modelQuotas, modelResources, modelComponent, anotherQuotas, taskState, task, project } = response.result
        this.task = task
        this.model = model
        this.anotherQuotas = anotherQuotas
        this.modelQuotas = modelQuotas
        this.modelResources = modelResources
        this.modelComponent = modelComponent
        this.taskState = taskState
        this.projectId = project.projectId
      })
    }
  }
}
</script>
<style lang="scss" scoped>
::v-deep .time-consuming-label {
  width: 100px;
}
h3{
  font-size: 16px;
}
.container {
  &.disabled{
    filter:progid:DXImageTransform.Microsoft.BasicImage(graysale=1);
    -webkit-filter: grayscale(100%);
  }
  .total-time-label{
    font-size: 18px;
    margin-right: 10px;
  }
  .total-time{
    font-size: 20px;
  }
  .progress-line {
    width: 400px;
    margin-left: 10px;
  }
  .el-icon-time {
    color: #409EFF;
    margin-right: 3px;
  }
  .section {
    background-color: #fff;
    margin-bottom: 30px;
    padding-bottom: 20px;
  }
  .container.model .section{
    padding: 20px;
  }
}
::v-deep .el-table th{
  background: #fafafa;
}
::v-deep .el-descriptions-item__container{
  align-items: center;
}
.download-button{
  margin-left: 5px;
}
.quotas{
  max-width: 800px;
}
.buttons{
  display: flex;
  justify-content: flex-start;
}
</style>
