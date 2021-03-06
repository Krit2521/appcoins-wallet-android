package com.appcoins.wallet.gamification.repository.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.math.BigDecimal

@Entity
data class PromotionEntity(
    @PrimaryKey val id: String,
    val priority: Int,
    val bonus: Double? = null,
    @ColumnInfo(name = "total_spend")
    val totalSpend: BigDecimal? = null,
    @ColumnInfo(name = "total_earned")
    val totalEarned: BigDecimal? = null,
    val level: Int? = null,
    @ColumnInfo(name = "next_level_amount")
    val nextLevelAmount: BigDecimal? = null,
    val status: PromotionsResponse.Status? = null,
    @ColumnInfo(name = "max_amount")
    val maxAmount: BigDecimal? = null,
    val available: Int? = null,
    val bundle: Boolean? = null,
    val completed: Int? = null,
    val currency: String? = null,
    val symbol: String? = null,
    val invited: Boolean? = null,
    val link: String? = null,
    @ColumnInfo(name = "pending_amount")
    val pendingAmount: BigDecimal? = null,
    @ColumnInfo(name = "received_amount")
    val receivedAmount: BigDecimal? = null,
    @ColumnInfo(name = "user_status")
    val userStatus: ReferralResponse.UserStatus? = null,
    @ColumnInfo(name = "min_amount")
    val minAmount: BigDecimal? = null,
    val amount: BigDecimal? = null,
    @ColumnInfo(name = "current_progress")
    val currentProgress: BigDecimal? = null,
    val description: String? = null,
    @ColumnInfo(name = "end_date")
    val endDate: Long? = null,
    val icon: String? = null,
    @ColumnInfo(name = "linked_promotion_id")
    val linkedPromotionId: String? = null,
    @ColumnInfo(name = "objective_progress")
    val objectiveProgress: BigDecimal? = null,
    @ColumnInfo(name = "start_date")
    val startDate: Long? = null,
    val title: String? = null,
    @ColumnInfo(name = "view_type")
    val viewType: String? = null,
    @ColumnInfo(name = "details_link")
    val detailsLink: String? = null
)