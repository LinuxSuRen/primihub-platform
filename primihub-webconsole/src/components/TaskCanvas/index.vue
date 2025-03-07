<template>
  <div class="canvas">
    <div class="canvas-container">
      <!--工具栏-->
      <ToolBar :options="options.toolbarOptions" @save="toolBarSave" @zoomIn="zoomInFn" @zoomOut="zoomOutFn" @run="run" @clear="clearFn" @reset="resetFn" />
      <div id="flowContainer" ref="containerRef" class="container" />
      <div v-if="options.showMinimap" ref="mapContainerRef" class="minimap-container" />
    </div>
    <!--右侧工具栏-->
    <right-drawer v-if="showDataConfig" ref="drawerRef" :node-data="nodeData" :options="drawerOptions" @change="handleChange" />
  </div>
</template>

<script>
import { Graph, FunctionExt, Shape } from '@antv/x6'
import '@antv/x6-vue-shape'
import DagNodeComponent from './nodes/DagNode.vue'
import StartNodeComponent from './nodes/StartNode.vue'
import ToolBar from './ToolBar'
import RightDrawer from './RightDrawer'

import { getModelComponent, saveModelAndComponent, getModelComponentDetail, getProjectResourceData, runTaskModel, getTaskModelComponent, getProjectResourceOrgan, restartTaskModel } from '@/api/model'

const lineAttr = { // 线样式
  'line': {
    'stroke': '#A2B1C3',
    'targetMarker': {
      'name': 'block',
      'width': 12,
      'height': 8
    }
  }
}
const ports = { // 圆点样式
  'groups': {
    'in': {
      'position': 'top',
      'attrs': {
        'circle': {
          'r': 4,
          'magnet': true,
          'stroke': '#5F95FF',
          'strokeWidth': 1,
          'fill': '#fff',
          'style': {
            'visibility': 'hidden'
          }
        }
      }
    },
    'out': {
      'position': 'bottom',
      'attrs': {
        'circle': {
          'r': 4,
          'magnet': true,
          'stroke': '#5F95FF',
          'strokeWidth': 1,
          'fill': '#fff',
          'style': {
            'visibility': 'hidden'
          }
        }
      }
    }
  },
  'items': [
    {
      'group': 'in',
      'id': 'port1'
    },
    {
      'group': 'out',
      'id': 'port2'
    }
  ]
}

export default {
  name: 'TaskCanvas',
  components: {
    ToolBar,
    RightDrawer
  },
  props: {
    modelId: {
      type: Number,
      default: 0
    },
    state: {
      type: Number,
      default: 0
    },
    options: { // 可配置项
      type: Object,
      default: () => {
        return {
          toolbarOptions: {
            position: 'center',
            background: false,
            buttons: ['zoomIn', 'zoomOut', 'reset']
          },
          isRun: false, // 是否运行任务
          showTime: true,
          showSaveButton: false, // 是否展示右侧信息面板保存按钮
          isEditable: true, // 是否可编辑
          showMinimap: true, // 是否展示小地图
          center: true,
          showDraft: true, // 是否展示草稿
          showComponentsDetails: false
        }
      }
    },
    modelData: {
      type: Array,
      default: () => []
    },
    restartRun: {
      type: Boolean,
      default: false
    }
  },
  data() {
    return {
      showDataConfig: false,
      container: null,
      width: 0,
      projectId: '',
      graph: null,
      nodeData: null,
      saveParams: {}, // 保存接口需要参数
      modelComponents: [], // 草稿详情
      currentTaskId: 0,
      graphData: { cells: [] },
      isComplete: false,
      modelStartRun: false,
      selectComponentList: [],
      organs: [],
      startNode: {},
      startData: [],
      components: [],
      componentsList: [],
      modelRunValidated: true,
      showDraft: false,
      taskTimer: null,
      taskId: '',
      isDraft: 0,
      isLoadHistory: true, // 是否已加载详情
      drawerOptions: {
        showSaveButton: this.options.showSaveButton,
        isEditable: this.options.isEditable
      }
    }
  },
  computed: {
    isCopy() {
      return this.$route.query.isCopy
    }
  },
  watch: {
    async restartRun(newVal) {
      console.log('restartRun', newVal)
      if (newVal) {
        await this.restartTaskModel()
      }
    }
  },
  async mounted() {
    this.taskId = this.$route.params.taskId
    await this.init()
    await this.getModelComponentDetail()

    // 画布可编辑，初始操作按钮事件
    if (this.options.isEditable) {
      this.initToolBarEvent()
    }
    if (this.restartRun) {
      await this.restartTaskModel()
    } else {
      // 任务运行中，轮询任务状态
      if (this.state === 2) {
        this.taskTimer = window.setInterval(() => {
          setTimeout(this.getTaskModelComponent(), 0)
        }, 1500)
      }
    }
  },
  destroyed() {
    clearTimeout(this.taskTimer)
    this.selectComponentList = []
    this.destroyed = true
    this.graph.dispose()
    console.log('destroyed model')
  },
  methods: {
    // 工具组件事件
    // 放大
    zoomInFn() {
      this.graph.zoom(0.1)
    },
    // 缩小
    zoomOutFn() {
      const Num = Number(this.graph.zoom().toFixed(1))

      if (Num > 0.1) {
        this.graph.zoom(-0.1)
      }
    },
    // 清除画布
    clearFn() {
      const { cells } = this.graph.toJSON()
      if (this.modelStartRun) { // 模型运行中不可操作
        this.$message({
          message: '模型运行中',
          type: 'warning'
        })
        return
      }
      if (cells.length <= 1 && this.isLoadHistory) {
        this.$message({
          message: '当前画布为空，无可清除组件',
          type: 'warning'
        })
        return
      }
      this.startData = cells.filter(item => item.componentCode === 'start')[0]
      this.nodeData = this.startNode
      this.graphData.cells = []
      this.graphData.cells.push(this.startData)
      this.graph.fromJSON(this.graphData)
      this.selectComponentList = []
      this.$emit('selectComponents', this.selectComponentList)
      this.isClear = true

      this.saveFn()
    },
    // 重置画布
    resetFn() {
      this.graph.centerContent()
      this.graph.zoomTo(1)
    },
    toolBarSave() {
      // this.isDraft = this.isCopy ? 0 : 1
      this.saveFn()
    },
    handleChange(data) {
      const { cells } = this.graph.toJSON()
      const posIndex = cells.findIndex(item => item.componentCode === data.componentCode)
      cells[posIndex].data = data
      this.graph.fromJSON(cells)
      this.saveFn()
    },
    async init() {
      Graph.registerNode(
        'dag-node',
        {
          inherit: 'vue-shape',
          width: 180,
          height: 50,
          component: {
            template: `<dag-node-component />`,
            components: {
              DagNodeComponent
            }
          },
          data: {
            showTime: this.options.showTime
          },
          ports: {
            ...ports
          }
        },
        true
      )

      // 获取左侧组件列表
      await this.getModelComponentsInfo()
      // 初始化画布
      this.container = this.$refs.containerRef
      const minimapContainer = this.$refs.mapContainerRef
      this.width = this.container.scrollWidth || 800
      const height = this.container.scrollHeight || 600
      const options = {
        container: this.container,
        width: this.width,
        height: height,
        autoResize: true,
        snapline: true,
        scroller: {
          enabled: true,
          pageBreak: false,
          pannable: true
        },
        minimap: {
          enabled: true,
          container: minimapContainer,
          scalable: false,
          width: 200,
          height: 160,
          padding: 10
        },
        // panning: true,
        background: {
          color: '#F5F7FA'
        },
        grid: {
          type: 'doubleMesh',
          size: 10,
          visible: true,
          args: [
            {
              color: '#E7E8EA',
              thickness: 1
            },
            {
              color: '#CBCED3',
              thickness: 1,
              factor: 5
            }
          ]
        },
        mousewheel: {
          enabled: true,
          modifiers: ['ctrl', 'meta'],
          minScale: 0.5,
          maxScale: 2
        },
        highlighting: {
          magnetAdsorbed: {
            name: 'stroke',
            args: {
              attrs: {
                fill: '#fff',
                stroke: '#31d0c6',
                strokeWidth: 4
              }
            }
          }
        },
        connecting: {
          router: {
            name: 'manhattan',
            args: {
              padding: 1
            }
          },
          connector: {
            name: 'rounded',
            args: {
              radius: 8
            }
          },
          anchor: 'center',
          connectionPoint: 'anchor',
          allowBlank: false,
          snap: {
            radius: 20
          },
          validateMagnet({ magnet }) {
            return magnet.getAttribute('port-group') !== 'in'
          },
          createEdge() {
            return new Shape.Edge({
              attrs: {
                line: {
                  stroke: '#A2B1C3',
                  strokeWidth: 2,
                  targetMarker: {
                    name: 'block',
                    width: 12,
                    height: 8
                  }
                }
              },
              zIndex: 0
            })
          }
        },
        selecting: {
          enabled: true,
          multiple: true,
          rubberEdge: true,
          rubberNode: true,
          modifiers: 'shift',
          rubberband: true
        },
        history: true,
        clipboard: true,
        keyboard: true
      }
      this.graph = new Graph(options)
      window.graph = this.graph
      if (this.componentsList) {
        this.registerStartNode()
        this.addStartNode()
      }

      // 画布事件初始化
      this.initEvent()
      // 居中显示画布
      // this.graph.centerContent({ padding: { left: 200, top: -200 }})
      this.projectId = Number(this.$route.params.id) || 0
    },
    registerStartNode() {
      this.startNode = this.components.filter(item => item.componentCode === 'start')[0]
      this.nodeData = this.startNode
      this.showDataConfig = true
      Graph.registerNode(
        'start-node',
        {
          inherit: 'vue-shape',
          width: 120,
          height: 40,
          component: {
            template: `<start-node-component />`,
            components: {
              StartNodeComponent
            }
          },
          data: {
            showTime: this.options.showTime
          },
          ports: { ...ports }
        },
        true
      )
    },
    addStartNode() {
      // 60 = start node width
      const x = this.width * 0.5 - 60
      this.startData = this.components.filter(item => item.componentCode === 'start')[0]
      this.graph.addNode({
        x: x,
        y: 100,
        shape: 'start-node',
        componentCode: this.startData.componentCode,
        label: this.startData.componentName,
        data: this.startData,
        ports
      })
    },
    showPorts(ports, show) {
      for (let i = 0, len = ports.length; i < len; i = i + 1) {
        ports[i].style.visibility = show ? 'visible' : 'hidden'
      }
    },
    // 画布事件初始化
    initEvent() {
      const { graph } = this
      graph.on('node:click', async({ node }) => {
        this.nodeData = node.store.data.data
      })
      graph.on('node:change:data', ({ node }) => {
        const edges = graph.getIncomingEdges(node)
        const { componentState } = node.getData()
        // TODO line Animation ?
        edges && edges.forEach((edge) => {
          if (componentState === 2 || componentState === 0) {
            edge.attr('line/strokeDasharray', 5)
            edge.attr('line/style/animation', 'running-line 30s infinite linear')
          } else {
            edge.attr('line/strokeDasharray', '')
            edge.attr('line/style/animation', '')
          }
        })
      })

      // 画布不可编辑只可点击
      if (!this.options.isEditable) return
      graph.on('node:mouseenter', FunctionExt.debounce(() => {
        const ports = this.container.querySelectorAll(
          '.x6-port-body'
        )
        this.showPorts(ports, true)
      }),
      500
      )
      graph.on('node:mouseleave', () => {
        const ports = this.container.querySelectorAll(
          '.x6-port-body'
        )
        this.showPorts(ports, false)
      })

      graph.on('node:collapse', ({ node, e }) => {
        e.stopPropagation()
        node.toggleCollapse()
        const collapsed = node.isCollapsed()
        const cells = node.getDescendants()
        cells.forEach((n) => {
          if (collapsed) {
            n.hide()
          } else {
            n.show()
          }
        })
      })
      graph.bindKey('backspace', () => {
        const cells = graph.getSelectedCells()
        if (cells.length) {
          const currentCode = this.nodeData.componentCode
          // remove duplicates
          this.selectComponentList = [...new Set(this.selectComponentList)]
          const index = this.selectComponentList.indexOf(currentCode)
          if (index !== -1) {
            this.selectComponentList.splice(index, 1)
            this.$emit('selectComponents', this.selectComponentList)
          }
          graph.removeCells(cells)
        }
        this.nodeData = this.startNode
      })

      graph.on('edge:connected', ({ edge }) => {
        edge.attr({
          line: {
            strokeDasharray: ''
          }
        })
      })
    },
    initToolBarEvent() {
      // 画布不可编辑只可点击
      if (!this.options.isEditable) return
      const { history } = this.graph
      history.on('change', () => {
        this.canUndo = history.canUndo()
        this.canRedo = history.canRedo()
        // model is running or destroyed, don't save the model history
        if (!this.modelStartRun && !this.destroyed) {
          this.saveFn()
        }
      })
      this.graph.bindKey(['ctrl+z', 'command+z'], () => {
        if (history.canUndo()) {
          history.undo()
        }
        return false
      })
    },
    checkRunValidated() {
      const data = this.graph.toJSON()
      const { cells } = data
      const startCom = cells.filter(item => item.componentCode === 'start')[0]
      const modelSelectCom = cells.filter(item => item.componentCode === 'model')[0]
      const taskName = startCom.data.componentTypes.filter(item => item.typeCode === 'taskName')[0].inputValue
      const modelName = modelSelectCom?.data.componentTypes.filter(item => item.typeCode === 'modelName')[0].inputValue
      const dataSetCom = cells.filter(item => item.componentCode === 'dataSet')
      const value = dataSetCom.length && dataSetCom[0]?.data.componentTypes[0].inputValue !== '' ? JSON.parse(dataSetCom[0]?.data.componentTypes[0].inputValue) : ''
      const initiateResource = value && value.filter(v => v.participationIdentity === 1)[0]
      const providerResource = value && value.filter(v => v.participationIdentity === 2)[0]
      // model is running, can't run again
      if (this.modelStartRun) {
        this.$message({
          message: '模型运行中',
          type: 'warning'
        })
        this.modelRunValidated = false
      } else if (!this.modelId && this.isCopy) { // copy model task, must save
        this.$message({
          message: '模型未保存，请保存后再次尝试',
          type: 'warning'
        })
        this.modelRunValidated = false
      } else if (cells.length === 1) { // model is empty or cleared, can't run
        this.$message({
          message: '当前画布为空，无法运行，请绘制',
          type: 'warning'
        })
        this.modelRunValidated = false
      } else if (taskName === '') {
        this.$message({
          message: `运行失败：请输入任务名称`,
          type: 'error'
        })
        this.modelRunValidated = false
      } else if (modelName === '') {
        this.$message({
          message: `运行失败：请输入模型名称`,
          type: 'error'
        })
        this.modelRunValidated = false
      } else if (!dataSetCom) {
        this.$message({
          message: `请选择数据集`,
          type: 'error'
        })
        this.modelRunValidated = false
      } else if (!(initiateResource && providerResource)) {
        const msg = !initiateResource ? '请选择发起方数据集' : !providerResource ? '请选择协作方数据集' : '请选择数据集'
        this.$message({
          message: msg,
          type: 'error'
        })
        this.modelRunValidated = false
      } else {
        this.modelRunValidated = true
      }
    },
    async run() {
      // 运行前触发保存
      this.isDraft = 1
      await this.saveFn()
      this.checkRunValidated()
      if (!this.modelRunValidated) {
        this.isDraft = 0
        return
      }
      runTaskModel({ modelId: this.currentModelId }).then(res => {
        if (res.code !== 0) {
          this.$message({
            message: res.msg,
            type: 'error'
          })
          return
        } else {
          this.currentTaskId = res.result.taskId
          this.modelStartRun = true
          this.$notify.closeAll()
          this.$notify({
            message: '开始运行',
            type: 'info',
            duration: 5000
          })
          // to model task detail page
          setTimeout(() => {
            this.toModelDetail(this.currentTaskId)
          }, 1000)
        }
      })
    },
    async restartTaskModel() {
      const res = await restartTaskModel({ taskId: this.taskId })
      if (res.code === 0) {
        this.modelStartRun = true
        this.taskTimer = window.setInterval(() => {
          setTimeout(this.getTaskModelComponent(), 0)
        }, 1500)
      }
    },
    getTaskModelComponent() {
      getTaskModelComponent({ taskId: this.taskId }).then(res => {
        const result = res.result.components
        const nodes = this.graph.getNodes()
        const taskResult = []
        result && result.forEach((item) => {
          const { componentCode, componentState, timeConsuming, componentName } = item
          const node = nodes.find(item => item.store.data.data.componentCode === componentCode)
          node.setData({
            label: componentName,
            componentCode,
            componentState: 2,
            timeConsuming: timeConsuming / 1000
          })
          if (componentState === 1) {
            taskResult.push(componentCode)
            node.setData({
              label: componentName,
              componentCode,
              componentState,
              timeConsuming: timeConsuming / 1000
            })
            if (taskResult.length === result.length) { // 所有任务运行完成，停止轮询
              clearInterval(this.taskTimer)
              this.$notify.closeAll()
              this.$notify({
                message: '运行完成',
                type: 'success',
                duration: 3000
              })
              this.$emit('complete')
            }
          } else if (componentState === 3) {
            clearInterval(this.taskTimer)
            this.$notify.closeAll()
            node.setData({
              label: componentName,
              componentCode,
              componentState,
              timeConsuming: timeConsuming / 1000
            })
            this.$notify({
              message: '运行失败',
              type: 'error',
              duration: 3000
            })
            this.$emit('complete')
            this.modelStartRun = false
            return
          }
        })
      })
    },
    toModelDetail(id) {
      this.$router.push({
        name: `ModelTaskDetail`,
        params: { taskId: id },
        query: { from: '0' } // 跳转携带参数，用于预览tab展示 0 创建任务 1 生成模型
      })
    },
    initGraphShape() {
      // 60 = start node width
      const x = this.width * 0.5 - (this.options.showTime ? 120 : 60)
      this.modelComponents && this.modelComponents.forEach(item => {
        const { frontComponentId, componentCode, coordinateX, coordinateY, width, height, componentName, shape } = item
        const current = this.modelData.filter(v => v.componentCode === componentCode)[0]
        const timeConsuming = current?.timeConsuming
        const componentState = current?.componentState
        const { componentTypes, isMandatory } = this.components.find(item => {
          return item.componentCode === componentCode
        })
        this.graphData.cells.push({
          id: frontComponentId,
          frontComponentId,
          label: componentName,
          componentCode,
          position: {
            x: this.options.center ? x : coordinateX,
            y: coordinateY
          },
          size: {
            width,
            height
          },
          shape,
          ports,
          data: {
            componentState,
            componentCode,
            componentName,
            componentTypes,
            isMandatory,
            timeConsuming
          },
          zIndex: 1
        })
      })

      this.modelPointComponents?.forEach(item => {
        if (item.shape === 'edge') {
          this.graphData.cells.push({
            id: item.frontComponentId,
            'shape': 'edge',
            'attrs': lineAttr,
            'zIndex': 0,
            'source': item.input,
            'target': item.output
          })
        }
      })
      this.graph.fromJSON(this.graphData)
    },
    filterFn(item, edgeList, cells) {
      // 1. 在target中找，
      // 找到:有输入 source.cell input 存放
      // 没找到 :
      // 2. 在source中找，
      // 找到: 有输出 target.cell output 存放
      // 没找到: 没有输入 input =[]
      // 同时找到
      // 有输入 source.cell 有输出 target.cell
      const { shape } = item
      const obj = {
        input: [],
        output: []
      }
      if (shape !== 'edge') {
        const frontComponentId = item.frontComponentId
        const inputRes = edgeList.find(item => {
          return item.source.cell === frontComponentId
        })
        const outputRes = edgeList.find(item => {
          return item.target.cell === frontComponentId
        })
        if (inputRes && outputRes) {
          const inputId = outputRes.source.cell
          const outputId = inputRes.target.cell
          const outputData = cells.find(item => item.id === outputId)
          const inputData = cells.find(item => item.id === inputId)
          obj.output.push({
            frontComponentId: outputId,
            componentCode: outputData.data.componentCode,
            componentName: outputData.data.componentName,
            portId: outputRes.target.port,
            pointType: 'edge',
            pointJson: ''
          })
          obj.input.push({
            frontComponentId: inputId,
            componentCode: inputData.data.componentCode,
            componentName: inputData.data.componentName,
            portId: inputRes.source.port,
            pointType: 'edge',
            pointJson: ''
          })
        } else if (inputRes) {
          const outputId = inputRes.target.cell
          const outputData = cells.find(item => item.id === outputId)
          obj.output.push({
            frontComponentId: outputId,
            componentCode: outputData.data.componentCode,
            componentName: outputData.data.componentName,
            portId: inputRes.target.port,
            pointType: 'edge',
            pointJson: ''

          })
        } else if (outputRes) {
          const inputId = outputRes.source.cell
          const inputData = cells.find(item => item.id === inputId)
          obj.input.push({
            frontComponentId: inputId,
            componentCode: inputData.data.componentCode,
            componentName: inputData.data.componentName,
            portId: outputRes.source.port,
            pointType: 'edge',
            pointJson: ''
          })
        }
      }
      return obj
    },
    // 保存模板文件
    async saveFn() { // 0 草稿
      const data = this.graph.toJSON()
      const { cells } = data
      if (this.modelStartRun) { // 模型运行中不可操作
        this.$message({
          message: '模型运行中',
          type: 'warning'
        })
        return
      }
      if (this.isDraft && cells.length <= 1) {
        this.$message({
          message: '当前画布为空，无法保存，请绘制',
          type: 'warning'
        })
        return
      }
      this.saveParams.param = {
        projectId: this.projectId,
        modelId: this.currentModelId,
        isDraft: this.isDraft
      }
      this.saveParams.param.modelComponents = this.modelComponents

      const modelComponents = [] // 块数据
      const modelPointComponents = [] // 线数据

      const edgeList = cells.filter(item => item.shape === 'edge')
      for (let i = 0; i < cells.length; i++) {
        const item = cells[i]
        const { position, data, size, shape } = item
        const componentValues = []
        item.frontComponentId = item.id
        if (shape === 'edge') {
          modelPointComponents.push({
            frontComponentId: item.frontComponentId,
            shape,
            input: item.source,
            output: item.target
          })
        } else {
          const { componentCode, componentName, componentTypes } = data
          const { input, output } = this.filterFn(item, edgeList, cells)

          for (let i = 0; i < componentTypes.length; i++) {
            const item = componentTypes[i]
            componentValues.push({
              key: item.typeCode,
              val: item.inputValue
            })
          }
          // format 参数
          modelComponents.push({
            frontComponentId: item.frontComponentId,
            coordinateX: position.x,
            coordinateY: position.y,
            width: size.width,
            height: size.height,
            shape,
            componentCode,
            componentName,
            componentValues,
            input: input,
            output: output
          })
        }
      }
      this.saveParams.param.modelComponents = modelComponents

      this.saveParams.param.modelPointComponents = modelPointComponents
      if (this.isClear) {
        const startParams = modelComponents.filter(item => item.componentCode === 'start')[0]
        this.saveParams.param.modelComponents = []
        this.saveParams.param.modelComponents.push(startParams)
      }

      const res = await saveModelAndComponent(JSON.stringify(this.saveParams))
      if (res.code === 0) {
        this.currentModelId = res.result.modelId
        this.$notify.closeAll()
        this.$notify({
          message: '保存成功',
          type: 'success',
          duration: '1000'
        })
      } else {
        this.$message({
          message: res.msg,
          type: 'error'
        })
      }
      this.isClear = false
    },
    getProjectResourceData() {
      getProjectResourceData({ projectId: this.projectId }).then(res => {
        this.projectName = res.result.projectName
        this.resourceList = res.result.resource
      })
    },
    async getProjectResourceOrgan() {
      const res = await getProjectResourceOrgan({ projectId: this.projectId })
      if (res.code === 0) {
        this.organs = res.result
      }
    },
    // 获取左侧组件
    async getModelComponentsInfo() {
      const { result, code } = await getModelComponent()
      if (code === 0) {
        this.components = result
        this.componentsList = result.slice(1)
      }
    },
    // 获取模型组件详情
    async getModelComponentDetail() {
      const params = {
        modelId: this.modelId,
        projectId: this.projectId
      }
      const res = await getModelComponentDetail(params)
      const modelComponents = res.result && res.result.modelComponents
      // 生成模型后给对应组件设置耗时
      modelComponents?.length > 0 && modelComponents.map(item => {
        const current = item && this.modelData.filter(v => v.componentCode === item.componentCode)[0]
        if (current) {
          item.componentState = current?.componentState
          item.timeConsuming = current?.timeConsuming
        }
      })
      if (this.isCopy && modelComponents && modelComponents.length > 0) {
        this.setComponentsDetail(res.result)
      } else if (this.options.showDraft && modelComponents && modelComponents.length > 1) {
        this.$confirm('当前用户存在历史记录，是否加载?', '提示', {
          confirmButtonText: '是',
          cancelButtonText: '否',
          closeOnClickModal: false,
          type: 'warning'
        }).then(() => {
          this.setComponentsDetail(res.result)
        }).catch(() => {
          this.isLoadHistory = false
          this.clearFn()
        })
      } else if (this.options.showComponentsDetails) {
        this.setComponentsDetail(res.result)
      }
    },
    setComponentsDetail(data) {
      const { modelComponents, modelPointComponents } = data
      // 复制任务，需重置重新生成modelId
      if (this.isCopy) {
        this.currentModelId = 0
        this.isDraft = 0
      }
      this.modelPointComponents = modelPointComponents
      this.modelComponents = modelComponents
      for (let index = 0; index < this.modelComponents.length; index++) {
        const item = this.modelComponents[index]
        // save the selected components
        this.selectComponentList.push(item.componentCode)
        this.$emit('selectComponents', this.selectComponentList)
        const posIndex = this.components.findIndex(c => c.componentCode === item.componentCode)
        item.componentValues.map(item => {
          this.components[posIndex].componentTypes.map(c => {
            if (c.typeCode === item.key && item.val !== '') {
              c.inputValue = item.val
            }
          })
        })
      }
      this.initGraphShape()
    }
  }
}
</script>

<style lang="scss" scoped>
::v-deep .x6-graph-scroller{
  overflow-x: hidden;
}
.canvas{
  display: flex;
  overflow: hidden;
  height: 100%;
  width: 100%;
}
.canvas-container{
  display: flex;
  position: relative;
  width: calc(100% - 300px);
  height: 100%;
}
.container{
  flex: 1;
}
.not-clickable{
  cursor: default;
  pointer-events: none;
}
.minimap-container{
  position: absolute;
  bottom: 1px;
  right: 0;
}
</style>
