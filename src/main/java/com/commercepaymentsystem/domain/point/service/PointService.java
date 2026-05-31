package com.commercepaymentsystem.domain.point.service;

import com.commercepaymentsystem.domain.member.entity.Member;
import com.commercepaymentsystem.domain.member.repository.MemberRepository;
import com.commercepaymentsystem.domain.point.dto.PointHistoryResponse;
import com.commercepaymentsystem.domain.point.dto.PointResponse;
import com.commercepaymentsystem.domain.point.repository.PointHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PointService {

	private final MemberRepository memberRepository;
	private final PointHistoryRepository pointHistoryRepository;

	// 1. 현재 잔액 조회: Member 엔티티에서 pointBalance 조회
	public PointResponse getMyPoint(Long memberId) {
		Member member = memberRepository.findById(memberId)
			.orElseThrow(() -> new IllegalArgumentException("회원을 찾을 수 없습니다."));

		return new PointResponse(member.getPointBalance());
	}

	// 2. 거래 내역 조회: PointHistoryRepository 사용
	public List<PointHistoryResponse> getMyPointHistories(Long memberId) {
		return pointHistoryRepository.findByMemberIdOrderByCreatedAtDesc(memberId)
			.stream()
			.map(history -> new PointHistoryResponse(
				history.getType().name(),
				history.getAmount(),
				history.getCreatedAt()
			))
			.toList();
	}
}
