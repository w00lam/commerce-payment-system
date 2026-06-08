package com.commercepaymentsystem.domain.subscription.port;

public interface MembershipRewardPolicyPort {

	MembershipRewardPolicy getRewardPolicy(Long memberId);
}
