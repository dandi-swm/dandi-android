package com.dandi.nyummy.common.domain.helper

/**
 * 이 기기의 푸시(FCM) 토큰을 서버에 알리고 지우는 통로.
 *
 * 서버는 사용자당 토큰 하나를 두고 같은 사용자가 다시 보내면 교체하므로, 앱을 켤 때와 로그인할 때마다 보내도 된다.
 */
interface PushTokenHelper {

    /**
     * 로그인한 상태면 [token](없으면 지금 기기의 토큰)을 서버에 등록한다.
     * 화면과 상관없이 백그라운드에서 보내고, 실패해도 알리지 않는다(다음 실행 때 다시 보낸다).
     */
    fun register(token: String? = null)

    /**
     * 이 기기의 푸시 토큰을 지운다(로그아웃). 서버 로그아웃이 실패해도 이 토큰으로는 더 이상 푸시가 오지 않는다.
     * 다음 로그인 때는 새 토큰이 만들어져 [register]된다. 화면 이동을 막지 않게 백그라운드에서 지우고,
     * 바로 이어지는 [register]와 순서가 섞이지 않게 한다.
     */
    fun deleteToken()
}
