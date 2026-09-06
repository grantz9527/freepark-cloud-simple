# 管理平台新页面构建指南（临时工作文档，完成后删除）

## 任务背景
管理平台 freepark-cloud-simple-frontend-mnt（Vue3 + Element Plus + TS strict + vue-i18n + pinia）需要按
参考项目 d:\code\freepark\local_frontend 实现「车场管理 / 通行管理 / 停车管理」三组后台页面。
后端模块已就绪：d:\code\freepark-cloud-simple\freepark-cloud-simple-backend\freepark-cloud-simple-parking。

## 你需要负责的文件（只允许创建/覆盖这些，禁止改动其它任何文件、禁止运行 npm/node/vite 命令）
- (按任务分配)

## 已存在且可直接复用的基础设施（务必先阅读，避免重造）
- 全局注册 Element Plus 组件（模板可直接使用 <el-table> 等，但 <script> 中使用的 API 如 ElMessage、
  ElMessageBox、ElFormInstance、ElUploadInstance 仍需显式 import type / import）。
- API 请求封装：src/utils/request.ts，导出 default axios 实例。**响应拦截器已解包 ApiResult.data**，
  因此调用后直接拿到业务数据（失败则 reject，message 已是后端本地化文案，展示即可）。
- 双语字典：src/utils/biText.ts 提供 BiDict 与 useBiText(dict)；页面可见文案必须双语。
  在页面内写 const d: BiDict = { key: { 'zh-CN': '中文', en: 'English' } }；
  const { t, locale } = useBiText(d)；模板中用 {{ t('key') }}。不要使用全局 vue-i18n 的 t 来加新 key。
  需要按语言选择枚举文案时用 locale 判断：locale.value === 'en' ? 'Blue' : '蓝牌'。
- 文件下载：src/utils/download.ts 的 downloadFile(path, fallbackName)。path 以 / 开头（不含 /api 前缀），
  如 '/lots/1/internal-vehicles/export?plate='。
- 鉴权 token 由拦截器自动携带；Excel 模板下载使用 downloadFile；Excel 上传用
  const fd = new FormData(); fd.append('file', file); await request.post(url, fd)。
- 参考页面代码规范：src/views/UserManageView.vue（表格 + 筛选 + 分页 + 弹窗表单 + ElMessage 反馈）。
- 布局视觉：内容区使用浅色 .page-card 风格（白底卡片、圆角 var(--fp-radius)、内边距 20px、
  可选 box-shadow: var(--fp-shadow-soft)）。工具条用 flex gap。表格放在卡片内。
- 页面文件位于 src/views 下，所以导入工具的相对路径是 '../utils/request'、'../utils/biText'、
  '../utils/download'。

## 后端字段/接口：**禁止臆造**。涉及接口时先读对应 Controller/DTO/Service/Entity/枚举确认精确字段与取值。
DTO 目录：freepark-cloud-simple-parking/src/main/java/com/freepark/cloud/simple/parking/dto
Web 目录：.../parking/web（含所有 @RequestMapping 路径）
约定：分页 PageResult 形如 { list, total, page, size }，page 从 1 开始。
View 中枚举已序列化为字符串（如 status 为 "OPEN"，plateColor 为 "BLUE"）。

## 页面验收要求
- 每个页面支持中英文切换（用 useBiText）；表格列名、按钮、弹窗、确认框、ElMessage 全部走 dict。
- 列表页包含：顶部筛选（关键词等）、新增按钮、分页（el-pagination layout="total, prev, pager, next"）、
  行内操作（编辑/删除等）。删除前 ElMessageBox.confirm 确认，成功后 ElMessage.success 并刷新。
- 所有异步操作 try/catch 捕获，错误用 ElMessage.error(e?.message ?? fallback)（e 为 Error，可能携带 message）。
- 类型安全：接口字段用 TS interface 定义；接口入参命名用驼峰；数量字段保持 number 语义（totalSpaces 等）。
- 不要写 .style 全局变量、不要使用 Element Plus 未注册组件。
- vue-tsc strict 已开启：不要遗留未使用 import/变量；import type 的类型要用 `import type`。
- 时间字段使用字符串展示，直接原样渲染（后端 LocalDateTime 已序列化为字符串）。

## 提交前自查清单
1. 只新增了你负责的 .vue 文件；2. 每个用户可见文案均有 zh-CN/en 词条；3. 无未使用 import；
4. 所有 API 字段名与后端 DTO 一致；5. 表单提交后重置并重新拉取列表。

完成后，在最终回复里给出：创建的文件清单、每个页面用到的后端接口清单、你对后端字段/语义的任何疑问或假设。
