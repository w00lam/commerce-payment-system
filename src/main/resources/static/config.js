(function () {
    "use strict";

    // API Base URL policy:
    // - Local browser development calls the local Spring Boot backend.
    // - Deployed frontend calls the public ALB domain. Do not call EC2 IPs from the browser.
    var DEFAULT_API_BASE_URL =
        window.location.hostname === "localhost" ||
        window.location.hostname === "127.0.0.1"
            ? "http://localhost:8080"
            : "https://roviq.click";

    window.RoviqConfig = {
        DEFAULT_API_BASE_URL: DEFAULT_API_BASE_URL,
        TOKEN_KEY: "roviq-market-access-token",
        MEMBER_KEY: "roviq-market-member",
        getPortOnePaymentConfig: function () {
            var runtime = window.__PORTONE_PAYMENT__ || {};
            var storeMeta = document.querySelector('meta[name="store-e18740d2-1f6b-4e5c-8a6a-492733c922cd"]');
            var channelMeta = document.querySelector('meta[name="channel-key-de40b17c-7a19-47c8-a788-41fc62288076"]');

            return {
                storeId: runtime.storeId || (storeMeta && storeMeta.content) || "",
                channelKey: runtime.channelKey || (channelMeta && channelMeta.content) || ""
            };
        }
    };
})();
