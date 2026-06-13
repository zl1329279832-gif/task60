# 物业费 feiyongDelete 软删与列表过滤说明

> 本文档从代码反推，描述物业费（`feiyong`）的软删除机制及列表接口的过滤策略。
> **已知缺口**以 `[GAP]` 标注，不要误当已实现功能。

---

## 1. 软删除字段

**字段定义**：`FeiyongEntity.java:110-116`

| 数据库列 | Java 字段 | 类型 | 含义 |
|----------|-----------|------|------|
| `feiyong_delete` | `feiyongDelete` | `Integer` | 逻辑删除标记 |

| 值 | 含义 |
|----|------|
| **1** | 有效（未删除） |
| **2** | 已软删除 |

---

## 2. 创建时初始化

新记录创建时，`feiyongDelete` 被强制设为 `1`：

| 接口 | 代码位置 | 赋值 |
|------|---------|------|
| `/feiyong/save`（后端创建） | `FeiyongController.java:160` | `feiyong.setFeiyongDelete(1)` |
| `/feiyong/add`（前端创建） | `FeiyongController.java:338` | `feiyong.setFeiyongDelete(1)` |

---

## 3. 删除操作（`/feiyong/delete`）

**位置**：`FeiyongController.delete()` (`:193-209`)

### 3.1 执行方式

```java
for(Integer id : ids){
    FeiyongEntity feiyongEntity = new FeiyongEntity();
    feiyongEntity.setId(id);
    feiyongEntity.setFeiyongDelete(2);   // 标记为已删除
    list.add(feiyongEntity);
}
feiyongService.updateBatchById(list);    // 批量 UPDATE，非 DELETE
```

**关键点**：
- 这是**软删除**，执行的是 `UPDATE feiyong SET feiyong_delete=2 WHERE id=?`
- **不是**物理删除（`DELETE FROM feiyong`），数据仍留在数据库
- 仅设置了 `id` 和 `feiyongDelete` 两个字段，其余字段不受影响

### 3.2 流程图

```
用户点击删除按钮
       │
       ▼
POST /feiyong/delete  [id1, id2, ...]
       │
       ▼
遍历 ids，构造 Entity (id=?, feiyongDelete=2)
       │
       ▼
updateBatchById → UPDATE feiyong SET feiyong_delete=2 WHERE id IN (...)
       │
       ▼
数据仍在数据库，feiyong_delete 由 1 变为 2
```

---

## 4. 列表查询与软删过滤

系统有两个列表接口，**过滤策略不一致**：

### 4.1 后端列表 `/feiyong/page` — 服务端过滤（安全）

**位置**：`FeiyongController.page()` (`:85-106`)

```java
// 第 95 行 — 硬编码服务端过滤
params.put("feiyongDeleteStart", 1);
params.put("feiyongDeleteEnd", 1);
```

该代码在查询参数中**强制注入** `feiyongDeleteStart=1, feiyongDeleteEnd=1`，MyBatis mapper 生成的 SQL 条件为：

```sql
AND a.feiyong_delete >= 1 AND a.feiyong_delete <= 1
```

等效于 `feiyong_delete = 1`，**已删除记录一定被过滤**，无论客户端传什么参数。

### 4.2 前端列表 `/feiyong/list` — 仅客户端过滤（不安全）[GAP-1]

**位置**：`FeiyongController.list()` (`:275-289`)

```java
@IgnoreAuth
@RequestMapping("/list")
public R list(@RequestParam Map<String, Object> params, HttpServletRequest request){
    CommonUtil.checkMap(params);
    PageUtils page = feiyongService.queryPage(params);
    // ... 返回结果
}
```

**问题**：
1. **没有**服务端注入 `feiyongDelete` 过滤条件
2. 标注 `@IgnoreAuth`，**无需登录**即可调用
3. 过滤完全依赖客户端是否传入 `feiyongDelete=1`

### 4.3 对比总结

| 维度 | `/feiyong/page`（后端） | `/feiyong/list`（前端） |
|------|------------------------|------------------------|
| 软删过滤 | **服务端强制**（`:95`） | 无服务端保护 **[GAP-1]** |
| 鉴权 | 需要 Token | 免鉴权 `@IgnoreAuth` |
| 角色数据隔离 | 用户仅看自己的 | 无隔离 |
| 客户端是否也传了过滤 | 是（`list.vue:740`），但冗余 | 是（`list.html:242`），是唯一屏障 |

---

## 5. MyBatis Mapper 中的条件

**位置**：`FeiyongDao.xml:62-70`

```xml
<if test="params.feiyongDeleteStart != null and params.feiyongDeleteStart != ''">
    and a.feiyong_delete >= #{params.feiyongDeleteStart}
</if>
<if test="params.feiyongDeleteEnd != null and params.feiyongDeleteEnd != ''">
    and a.feiyong_delete <= #{params.feiyongDeleteEnd}
</if>
<if test="params.feiyongDelete != null and params.feiyongDelete != ''">
    and a.feiyong_delete = #{params.feiyongDelete}
</if>
```

三个条件均为 `<if>` 可选块。如果请求参数中不包含上述任一字段，**不会添加任何 `feiyong_delete` 过滤条件**，查询将返回全量数据（含已删除记录）。

---

## 6. 前端实际传参

三个前端页面都在请求参数中携带了 `feiyongDelete: 1`：

| 前端页面 | 位置 | 调用接口 | 传参 |
|----------|------|---------|------|
| 后台管理 `list.vue` | `:740` | `/feiyong/page` | `params['feiyongDelete'] = 1` |
| 用户端 `list.html` | `:242` | `/feiyong/list` | `feiyongDelete: 1` |
| 用户端 `list2.html` | `:237` | `/feiyong/list` | `feiyongDelete: 1` |

正常使用 UI 时，已删除记录不会显示。但对于 `/feiyong/list`，保护**仅存在于客户端**。

---

## 7. 已知缺口 [GAP]

### GAP-1：`/feiyong/list` 无服务端软删过滤

**位置**：`FeiyongController.list()` (`:275-289`)

攻击者（甚至无需登录）可直接调用：

```
GET /feiyong/list?page=1&limit=100&sort=id&order=desc
```

不传 `feiyongDelete` 参数，返回结果将**包含已软删除的记录**（`feiyong_delete=2`）。

**修复建议**：在 `CommonUtil.checkMap(params)` 前增加服务端强制过滤，与 `/page` 保持一致：

```java
params.put("feiyongDeleteStart", 1);
params.put("feiyongDeleteEnd", 1);
```

### GAP-2：`/feiyong/list` 免鉴权

**位置**：`FeiyongController.list()` (`:275`)

标注 `@IgnoreAuth`，任何人无需 Token 即可访问。结合 GAP-1，可导致未授权访问全量物业费数据（含已删除数据、关联用户信息）。

### GAP-3：`/feiyong/update` 可覆写 `feiyongDelete` 字段

**位置**：`FeiyongController.update()` (`:173-186`)

`/update` 接口直接将请求体中的 Entity 通过 `updateById()` 写入数据库，**没有保护 `feiyongDelete` 字段**。调用者可以：
- 将已删除记录的 `feiyongDelete` 改回 `1`，使其"复活"
- 将未删除记录的 `feiyongDelete` 改为 `2`，绕过正常删除流程

### GAP-4：软删除不回滚关联数据

删除操作仅将 `feiyong_delete` 设为 2，不做任何关联处理。如果物业费记录关联了缴费状态、用户余额等外部状态，这些状态不会被回滚或调整。

---

## 8. 口径对齐：删除 vs 列表的关系

| 问题 | 答案 |
|------|------|
| 删除是物理删除还是逻辑删除？ | **逻辑删除**（`feiyong_delete` 设为 2） |
| 删除后数据还在数据库吗？ | **在**，只是标记值从 1 变为 2 |
| 后台列表能看到已删除的记录吗？ | **不能**（`/page` 服务端强制过滤 `feiyong_delete=1`） |
| 前端列表能看到已删除的记录吗？ | 正常使用 UI 不能，但**绕过客户端可以** [GAP-1] |
| 删除后能恢复吗？ | 数据库层面可以（将 `feiyong_delete` 改回 1），但**系统不提供恢复接口** |
| 不同接口的过滤口径一致吗？ | **不一致** — `/page` 服务端强制，`/list` 仅客户端传参 |
