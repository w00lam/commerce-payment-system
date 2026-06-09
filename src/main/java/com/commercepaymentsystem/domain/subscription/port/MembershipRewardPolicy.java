package com.commercepaymentsystem.domain.subscription.port;

public record MembershipRewardPolicy(
	String gradeName,
	Integer pointRewardRate
) {

	private static final MembershipRewardPolicy DEFAULT = new MembershipRewardPolicy("NORMAL", 1);

	public static MembershipRewardPolicy defaultPolicy() {
		return DEFAULT;
	}
}
