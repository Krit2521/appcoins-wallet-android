package com.appcoins.wallet.gamification.repository

import com.appcoins.wallet.gamification.repository.entity.*
import io.reactivex.Completable
import io.reactivex.Single
import java.io.IOException
import java.math.BigDecimal
import java.net.UnknownHostException

class BdsPromotionsRepository(
    private val api: GamificationApi,
    private val local: GamificationLocalData) : PromotionsRepository {

  private fun getUserStats(wallet: String): Single<UserStatusResponse> {
    return api.getUserStats(wallet)
        .flatMap { userStats ->
          local.deletePromotions()
              .andThen(local.insertPromotions(userStats.promotions))
              .toSingle { userStats }
        }
        .onErrorResumeNext { t ->
          local.getPromotions()
              .map { mapErrorToUserStatsModel(it, t) }
              .onErrorReturn { mapErrorToUserStatsModel(t) }
        }
  }

  override fun getLastShownLevel(wallet: String, screen: String): Single<Int> {
    return local.getLastShownLevel(wallet, screen)
  }

  override fun shownLevel(wallet: String, level: Int, screen: String): Completable {
    return local.saveShownLevel(wallet, level, screen)
  }

  override fun getForecastBonus(wallet: String, packageName: String,
                                amount: BigDecimal): Single<ForecastBonus> {
    return api.getForecastBonus(wallet, packageName, amount, "APPC")
        .map { map(it) }
        .onErrorReturn { mapForecastError(it) }
  }

  private fun mapForecastError(throwable: Throwable): ForecastBonus {
    throwable.printStackTrace()
    return if (isNoNetworkException(throwable)) {
      ForecastBonus(ForecastBonus.Status.NO_NETWORK)
    } else {
      ForecastBonus(ForecastBonus.Status.UNKNOWN_ERROR)
    }
  }

  private fun map(bonusResponse: ForecastBonusResponse): ForecastBonus {
    if (bonusResponse.status == ForecastBonusResponse.Status.ACTIVE) {
      return ForecastBonus(ForecastBonus.Status.ACTIVE, bonusResponse.bonus)
    }
    return ForecastBonus(ForecastBonus.Status.INACTIVE)
  }

  override fun getGamificationStats(wallet: String): Single<GamificationStats> {
    return getUserStats(wallet)
        .map { mapToGamificationStats(it) }
        .flatMap {
          local.setGamificationLevel(it.level)
              .toSingle { it }
        }
  }

  private fun map(status: Status): GamificationStats {
    return if (status == Status.NO_NETWORK) {
      GamificationStats(GamificationStats.Status.NO_NETWORK)
    } else {
      GamificationStats(GamificationStats.Status.UNKNOWN_ERROR)
    }
  }

  private fun mapErrorToUserStatsModel(throwable: Throwable): UserStatusResponse {
    throwable.printStackTrace()
    return if (isNoNetworkException(throwable)) {
      UserStatusResponse(emptyList(), Status.NO_NETWORK)
    } else {
      UserStatusResponse(emptyList(), Status.UNKNOWN_ERROR)
    }
  }

  private fun mapErrorToUserStatsModel(promotions: List<PromotionsResponse>,
                                       throwable: Throwable): UserStatusResponse {
    return if (promotions.isEmpty()) {
      throwable.printStackTrace()
      return if (isNoNetworkException(throwable)) {
        UserStatusResponse(emptyList(), Status.NO_NETWORK)
      } else {
        UserStatusResponse(emptyList(), Status.UNKNOWN_ERROR)
      }
    } else {
      UserStatusResponse(promotions, null)
    }
  }

  private fun mapToGamificationStats(response: UserStatusResponse): GamificationStats {
    return if (response.error != null) {
      map(response.error)
    } else {
      val gamification =
          response.promotions.firstOrNull { it is GamificationResponse } as GamificationResponse?
      if (gamification == null) {
        GamificationStats(GamificationStats.Status.UNKNOWN_ERROR)
      } else {
        GamificationStats(GamificationStats.Status.OK, gamification.level,
            gamification.nextLevelAmount, gamification.bonus, gamification.totalSpend,
            gamification.totalEarned,
            PromotionsResponse.Status.ACTIVE == gamification.status)
      }
    }
  }

  override fun getLevels(wallet: String): Single<Levels> {
    return api.getLevels(wallet)
        .map { map(it) }
        .onErrorReturn { mapLevelsError(it) }
  }

  private fun mapLevelsError(throwable: Throwable): Levels {
    throwable.printStackTrace()
    return if (isNoNetworkException(throwable)) {
      Levels(Levels.Status.NO_NETWORK)
    } else {
      Levels(Levels.Status.UNKNOWN_ERROR)
    }
  }

  private fun map(response: LevelsResponse): Levels {
    val list = response.list.map { Levels.Level(it.amount, it.bonus, it.level) }
    return Levels(Levels.Status.OK, list, LevelsResponse.Status.ACTIVE == response.status,
        response.updateDate)
  }

  override fun getUserStatus(wallet: String): Single<UserStatusResponse> {
    return getUserStats(wallet)
        .flatMap { userStatusResponse ->
          val gamification =
              userStatusResponse.promotions.firstOrNull { it is GamificationResponse } as GamificationResponse?
          if (userStatusResponse.error != null || gamification == null) {
            Single.just(userStatusResponse)
          } else {
            local.setGamificationLevel(gamification.level)
                .toSingle { userStatusResponse }
          }
        }
        .doOnError {
          it.printStackTrace()
        }
  }

  override fun getReferralUserStatus(wallet: String): Single<ReferralResponse> {
    return getUserStats(wallet)
        .flatMap {
          val gamification =
              it.promotions.firstOrNull { promotions -> promotions is GamificationResponse } as GamificationResponse?
          val referral =
              it.promotions.firstOrNull { promotions -> promotions is ReferralResponse } as ReferralResponse?
          if (gamification != null) {
            local.setGamificationLevel(gamification.level)
                .toSingle { referral }
          } else {
            Single.just(referral)
          }
        }
        .map { it }
  }

  override fun getReferralInfo(): Single<ReferralResponse> {
    return api.getReferralInfo()
  }

  private fun isNoNetworkException(throwable: Throwable): Boolean {
    return throwable is IOException ||
        throwable.cause != null && throwable.cause is IOException ||
        throwable is UnknownHostException
  }
}