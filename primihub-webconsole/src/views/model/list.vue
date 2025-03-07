<template>
  <div v-loading="listLoading" class="container">
    <div class="search-area">
      <el-form :model="query" :inline="true" @keyup.enter.native="search">
        <el-form-item label="模型ID">
          <el-input v-model.number="query.modelId" size="small" placeholder="请输入" clearable @clear="handleClear('modelId')" />
        </el-form-item>
        <el-form-item label="模型名称">
          <el-input v-model="query.modelName" size="small" placeholder="请输入" clearable @clear="handleClear('modelName')" />
        </el-form-item>
        <el-form-item label="建模完成时间">
          <el-date-picker
            v-model="query.successDate"
            size="small"
            clearable
            type="date"
            placeholder="请选择"
            value-format="yyyy-MM-dd HH:mm:ss"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" icon="el-icon-search" size="small" @click="search">查询</el-button>
          <el-button icon="el-icon-search" size="small" @click="reset">重置</el-button>
        </el-form-item>
      </el-form>
    </div>
    <div class="model-list">
      <el-table
        :data="modelList"
      >
        <el-table-column
          prop="modelId"
          label="模型ID"
          align="center"
        >
          <template slot-scope="{row}">
            <el-link type="primary" @click="toModelDetail(row)">{{ row.modelId }}</el-link>
          </template>
        </el-table-column>
        <el-table-column
          prop="modelName"
          label="模型名称"
        />
        <el-table-column
          prop="taskIdName"
          label="任务ID"
          min-width="120"
        >
          <template slot-scope="{row}">
            <el-link type="primary" @click="toModelTaskDetail(row)">{{ row.taskIdName }}</el-link>
          </template>
        </el-table-column>
        <el-table-column
          prop="taskName"
          label="任务名称"
        />
        <el-table-column
          prop="projectName"
          label="所属项目"
        />
        <el-table-column
          prop="taskEndDate"
          label="建模完成时间"
          min-width="120"
        />
        <el-table-column
          label="机构名称"
          min-width="110"
        >
          <template slot-scope="{row}">
            <span>发起方: {{ row.createdOrgan }}</span><br>
            <div>协作方:
              <span v-for="(item,index) in row.providerOrgans" :key="item.organId">
                <span>{{ item.organName }}<span v-if="index === 0 && row.providerOrgans.length>1">，</span></span>
              </span>
            </div>
          </template>
        </el-table-column>
        <el-table-column
          prop="resourceNum"
          label="所用资源数"
          align="center"
        />
      </el-table>
    </div>
    <pagination v-show="pageCount>1" :limit.sync="pageSize" :page.sync="pageNo" :total="total" layout="total, prev, pager, next, jumper" @pagination="handlePagination" />
  </div>
</template>

<script>
import { getModelTaskSuccessList } from '@/api/model'
import Pagination from '@/components/Pagination'

export default {
  components: {
    Pagination
  },
  data() {
    return {
      listLoading: false,
      query: {
        modelId: '',
        modelName: '',
        state: '',
        successDate: ''
      },
      modelList: null,
      pageNo: 1,
      pageSize: 10,
      total: 0,
      pageCount: 0
    }
  },
  computed: {
    hasModelViewPermission() {
      return this.$store.getters.buttonPermissionList.includes('ModelView')
    }
  },
  created() {
    this.fetchData()
  },
  methods: {
    search() {
      this.pageNo = 1
      if (this.query.modelId !== '' && isNaN(this.query.modelId)) {
        this.$message({
          message: '模型id为数字',
          type: 'warning'
        })
        return
      }
      this.fetchData()
    },
    reset() {
      console.log('reset')
      this.query.modelId = ''
      this.query.modelName = ''
      this.query.state = ''
      this.query.successDate = ''
      this.pageNo = 1
      this.fetchData()
    },
    handleClear(name) {
      console.log('清空', name)
      this.query[name] = ''
      this.fetchData()
    },
    toModelDetail(row) {
      this.$router.push({
        path: `/model/detail/${row.modelId}`,
        query: { taskId: row.taskId }
      })
    },
    toModelTaskDetail(row) {
      this.$router.push({
        path: `/project/detail/${row.projectId}/task/${row.taskId}`
      })
    },
    fetchData() {
      this.modelList = []
      const { modelId, modelName, successDate } = this.query
      this.listLoading = true
      const params = {
        modelId,
        modelName: modelName.toString(),
        successDate,
        pageNo: this.pageNo,
        pageSize: this.pageSize
      }
      console.log('fetchData', params)
      getModelTaskSuccessList(params).then((response) => {
        console.log('response.data', response.result)
        const { result } = response
        this.modelList = result.data
        this.total = result.total
        this.pageCount = result.totalPage
        setTimeout(() => {
          this.listLoading = false
        }, 200)
      }).catch(() => {
        this.listLoading = false
      })
    },
    statusStyle(status) {
      return status === 0 ? 'status-default' : status === 1 ? 'status-processing' : status === 2 ? 'status-end' : 'status-error'
    },
    handlePagination(data) {
      this.pageNo = data.page
      this.fetchData()
    }
  }
}
</script>
<style lang="scss" scoped>
.search-area {
  padding: 30px 0px 10px 20px;
  background-color: #fff;
  display: flex;
  flex-wrap: wrap;
}
.form-wrap{
  padding-top: 20px;
  background-color: #fff;
}
.model-list {
  margin-top: 20px;
  border-top: 1px solid #eee;
  background-color: #fff;
}
.status-default,.status-processing,.status-error,.status-end{
  width: 6px;
  height: 6px;
  border-radius: 50%;
  display: inline-block;
  vertical-align: middle;
  margin-right: 3px;
}
.status-default{
  background-color: #409EFF;
}
.status-end{
  background-color: #909399;
}
.status-processing{
  background-color: #67C23A;
}
.status-error{
  background-color: #F56C6C;
}
.pagination {
  padding: 50px;
  display: flex;
  justify-content: center;
}
</style>
