(function () {
    "use strict";

    window.APP_CONFIG = {
        API_BASE_URL:
            location.hostname === "localhost" ||
            location.hostname === "127.0.0.1"
                ? "http://localhost:8080"
                : "https://roviq.click",

        PORTONE_STORE_ID:
            "store-e18740d2-1f6b-4e5c-8a6a-492733c922cd",

        PORTONE_PAYMENT_CHANNEL_KEY:
            "channel-key-de40b17c-7a19-47c8-a788-41fc62288076",

        PORTONE_BILLING_CHANNEL_KEY:
            "channel-key-88ba0ec5-97fd-44c2-9e62-7016e869ff66"
    };

    window.RoviqConfig = {
        DEFAULT_API_BASE_URL: window.APP_CONFIG.API_BASE_URL,
        TOKEN_KEY: "roviq-market-access-token",
        MEMBER_KEY: "roviq-market-member",
        getPortOnePaymentConfig: function () {
            var runtime = window.__PORTONE_PAYMENT__ || {};
            return {
                storeId: runtime.storeId || window.APP_CONFIG.PORTONE_STORE_ID,
                channelKey: runtime.channelKey || window.APP_CONFIG.PORTONE_PAYMENT_CHANNEL_KEY
            };
        },
        getPortOneBillingConfig: function () {
            var runtime = window.__PORTONE_PAYMENT__ || {};
            return {
                storeId: runtime.storeId || window.APP_CONFIG.PORTONE_STORE_ID,
                channelKey: runtime.billingChannelKey || window.APP_CONFIG.PORTONE_BILLING_CHANNEL_KEY
            };
        }
    };
})();
