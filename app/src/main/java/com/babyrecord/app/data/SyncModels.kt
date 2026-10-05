package com.babyrecord.app.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/**
 * 家庭同步的网络传输模型（与服务端 API 契约对齐，服务端 http://192.168.1.100:8443）。
 *
 * 宽松字段说明：push/pull 的行数据各表结构不同，统一用 JsonElement 承载，
 * 由同步引擎负责各表 Entity ↔ JsonElement 的字段映射，避免为 9 张表各写一套 DTO。
 */

// ---- 认证 ----

/** 创建家庭 */
@Serializable
data class AuthCreateReq(val familyName: String, val deviceName: String)

/** 加入家庭（凭邀请码） */
@Serializable
data class AuthJoinReq(val inviteCode: String, val deviceName: String)

@Serializable
data class AuthResp(
    val token: String,
    val familyId: Long,
    val memberId: Long,
    val inviteCode: String? = null,
    val serverTime: Long
)

// ---- 推送（本地 → 服务端） ----

@Serializable
data class PushReq(
    val table: String,
    val rows: List<PushRow>
)

/** 单行待推数据；payload 为该实体的全字段宽松 JSON */
@Serializable
data class PushRow(
    val id: String,
    val updatedAt: Long,
    val deletedAt: Long? = null,
    val payload: JsonElement
)

@Serializable
data class PushResp(
    val serverTime: Long,
    val results: List<PushResult>
)

@Serializable
data class PushResult(val id: String, val status: String) // accepted / conflict-lost

// ---- 拉取（服务端 → 本地） ----

@Serializable
data class PullResp(
    val serverTime: Long,
    /** 各表增量行，key 为表名（babies/feedings/...），行内容为宽松 JSON */
    val tables: Map<String, List<JsonElement>>,
    /** 分页游标；JSON key 为 continue（Kotlin 关键字，用 @SerialName 映射） */
    @SerialName("continue") val continueCursors: Map<String, ContinueCursor>? = null
)

@Serializable
data class ContinueCursor(val since: Long, val id: String)
