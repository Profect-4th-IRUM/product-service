package com.irum.productservice.global.util;

import com.irum.global.advice.exception.CommonException;
import com.irum.global.advice.exception.errorcode.GlobalErrorCode;
import com.irum.global.context.MemberAuthContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import openfeign.member.client.MemberClient;
import openfeign.member.dto.response.MemberDto;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class MemberUtil {

    private final MemberClient memberClient;

    public MemberDto getCurrentMember() {
        return memberClient.getMember(getCurrentMemberId());
    } // 로그인 된 유저 정보 조회

    public void assertMemberResourceAccess(Long memberId) {
        if (!memberId.equals(getCurrentMember().memberId()))
            throw new CommonException(GlobalErrorCode.EMPTY_REQUEST);
    }

    private Long getCurrentMemberId() {
        return MemberAuthContext.getMemberId();
    } // 로그인 된 아이디 반환
}
