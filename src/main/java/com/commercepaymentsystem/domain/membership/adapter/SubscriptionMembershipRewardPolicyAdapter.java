package com.commercepaymentsystem.domain.membership.adapter;

import com.commercepaymentsystem.domain.membership.repository.MemberMembershipRepository;
import com.commercepaymentsystem.domain.subscription.port.MembershipRewardPolicy;
import com.commercepaymentsystem.domain.subscription.port.MembershipRewardPolicyPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SubscriptionMembershipRewardPolicyAdapter implements MembershipRewardPolicyPort {

	private final MemberMembershipRepository memberMembershipRepository;

	@Override
	public MembershipRewardPolicy getRewardPolicy(Long memberId) {
		return memberMembershipRepository.findByMemberId(memberId)
			.map(memberMembership -> new MembershipRewardPolicy(
				memberMembership.getMembershipGrade().getName(),
				memberMembership.getMembershipGrade().getPointRewardRate()
			))
			.orElse(MembershipRewardPolicy.defaultPolicy());
	}
}
