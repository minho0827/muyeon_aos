package com.muyeon.domain.model.member

/**
 * 회원유형 관리 화면(OnboardingActivity.startRoleManage)을 여는 데 필요한 값 — 웹 util/openMemberType.js 와 같다.
 *   [rolesJson] 은 `GET /me/roles` 응답 원문(그 화면이 held·roles 를 직접 읽는다). [heroImage] 는 없을 수 있다.
 */
data class RoleManageLaunch(val rolesJson: String, val heroImage: String?)
