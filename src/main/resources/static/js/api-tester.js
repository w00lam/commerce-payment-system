(function () {
    "use strict";

    var storageKeys = {
        baseUrl: "commerce-api-tester-base-url",
        token: "commerce-api-tester-token"
    };

    var presets = [
        { name: "Health Check", method: "GET", path: "/health" },
        {
            name: "회원가입",
            method: "POST",
            path: "/api/auth/signup",
            body: {
                email: "tester@example.com",
                password: "password1234",
                name: "테스터",
                phone: "010-1234-5678"
            }
        },
        {
            name: "로그인",
            method: "POST",
            path: "/api/auth/login",
            body: {
                email: "tester@example.com",
                password: "password1234"
            }
        },
        { name: "로그아웃", method: "POST", path: "/api/auth/logout", auth: true },
        { name: "상품 목록", method: "GET", path: "/api/products", query: "page=0&size=20" },
        { name: "상품 상세", method: "GET", path: "/api/products/1" },
        {
            name: "상품 등록",
            method: "POST",
            path: "/api/products",
            body: {
                name: "테스트 상품",
                price: 10000,
                stock: 50,
                description: "API 테스트용 상품",
                status: "ON_SALE",
                category: "ETC"
            }
        },
        {
            name: "상품 수정",
            method: "PUT",
            path: "/api/products/1",
            body: {
                name: "수정된 테스트 상품",
                price: 12000,
                stock: 40,
                description: "API 테스트용 수정 상품",
                status: "ON_SALE",
                category: "ETC"
            }
        },
        { name: "상품 삭제", method: "DELETE", path: "/api/products/1" },
        {
            name: "장바구니 담기",
            method: "POST",
            path: "/api/carts/items",
            auth: true,
            body: {
                productId: 1,
                quantity: 1
            }
        },
        { name: "장바구니 조회", method: "GET", path: "/api/carts", auth: true },
        {
            name: "장바구니 수량 변경",
            method: "PUT",
            path: "/api/carts/items/1",
            auth: true,
            body: {
                quantity: 2
            }
        },
        { name: "장바구니 상품 삭제", method: "DELETE", path: "/api/carts/items/1", auth: true },
        { name: "장바구니 비우기", method: "DELETE", path: "/api/carts/items", auth: true },
        {
            name: "주문 미리보기",
            method: "POST",
            path: "/api/orders/preview",
            auth: true,
            body: {
                cartItemIds: [1]
            }
        },
        {
            name: "주문 생성",
            method: "POST",
            path: "/api/orders",
            auth: true,
            body: {
                cartItemIds: [1],
                usedPointAmount: 0
            }
        },
        { name: "주문 목록", method: "GET", path: "/api/orders", auth: true, query: "page=0&size=20" },
        { name: "주문 상세", method: "GET", path: "/api/orders/1", auth: true },
        { name: "주문 취소", method: "PATCH", path: "/api/orders/1/cancel", auth: true },
        { name: "결제 확정", method: "POST", path: "/api/payments/payment-id/confirm", auth: true },
        {
            name: "환불 요청",
            method: "POST",
            path: "/api/payments/payment-id/refunds",
            auth: true,
            body: {
                reason: "테스트 환불",
                items: [
                    {
                        orderItemId: 1,
                        quantity: 1
                    }
                ]
            }
        },
        { name: "포인트 조회", method: "GET", path: "/api/points", auth: true },
        { name: "포인트 내역", method: "GET", path: "/api/points/histories", auth: true, query: "page=0&size=20" },
        {
            name: "회원 탈퇴",
            method: "DELETE",
            path: "/api/members/signout",
            auth: true,
            body: {
                password: "password1234"
            }
        }
    ];

    var elements = {};
    var lastResponseText = "";

    document.addEventListener("DOMContentLoaded", function () {
        elements = {
            baseUrl: document.getElementById("baseUrlInput"),
            authState: document.getElementById("authState"),
            presetList: document.getElementById("presetList"),
            method: document.getElementById("methodInput"),
            path: document.getElementById("pathInput"),
            token: document.getElementById("tokenInput"),
            query: document.getElementById("queryInput"),
            body: document.getElementById("bodyInput"),
            bodyLabel: document.getElementById("bodyLabel"),
            status: document.getElementById("requestStatus"),
            send: document.getElementById("sendBtn"),
            format: document.getElementById("formatBtn"),
            copyResponse: document.getElementById("copyResponseBtn"),
            applyToken: document.getElementById("applyTokenBtn"),
            clearToken: document.getElementById("clearTokenBtn"),
            resetStorage: document.getElementById("resetStorageBtn"),
            requestUrl: document.getElementById("requestUrl"),
            elapsedTime: document.getElementById("elapsedTime"),
            responseOutput: document.getElementById("responseOutput")
        };

        restoreState();
        renderPresets();
        bindEvents();
        refreshRequestUrl();
        refreshBodyVisibility();
        refreshAuthState();
    });

    function restoreState() {
        var savedBaseUrl = localStorage.getItem(storageKeys.baseUrl);
        var savedToken = localStorage.getItem(storageKeys.token);

        if (savedBaseUrl) {
            elements.baseUrl.value = savedBaseUrl;
        }

        if (savedToken) {
            elements.token.value = savedToken;
        }
    }

    function bindEvents() {
        elements.send.addEventListener("click", sendRequest);
        elements.format.addEventListener("click", formatBody);
        elements.copyResponse.addEventListener("click", copyResponse);
        elements.applyToken.addEventListener("click", saveTokenFromResponse);
        elements.clearToken.addEventListener("click", clearToken);
        elements.resetStorage.addEventListener("click", resetStorage);

        [elements.baseUrl, elements.path, elements.query].forEach(function (element) {
            element.addEventListener("input", function () {
                if (element === elements.baseUrl) {
                    localStorage.setItem(storageKeys.baseUrl, element.value.trim());
                }
                refreshRequestUrl();
            });
        });

        elements.method.addEventListener("change", function () {
            refreshBodyVisibility();
            refreshRequestUrl();
        });

        elements.token.addEventListener("input", function () {
            localStorage.setItem(storageKeys.token, normalizeToken(elements.token.value));
            refreshAuthState();
        });
    }

    function renderPresets() {
        elements.presetList.innerHTML = "";

        presets.forEach(function (preset) {
            var button = document.createElement("button");
            button.type = "button";
            button.className = "preset";
            button.innerHTML = "<strong>" + preset.method + "</strong><span>" + preset.name + "<br>" + preset.path + "</span>";
            button.addEventListener("click", function () {
                selectPreset(preset);
            });
            elements.presetList.appendChild(button);
        });
    }

    function selectPreset(preset) {
        elements.method.value = preset.method;
        elements.path.value = preset.path;
        elements.query.value = preset.query || "";
        elements.body.value = preset.body ? JSON.stringify(preset.body, null, 2) : "{}";

        if (preset.auth && !elements.token.value.trim()) {
            setStatus("토큰 필요", "error");
        } else {
            setStatus("대기", "");
        }

        refreshBodyVisibility();
        refreshRequestUrl();
    }

    async function sendRequest() {
        var method = elements.method.value;
        var url = buildUrl();
        var headers = {};
        var options = {
            method: method,
            headers: headers
        };

        var token = normalizeToken(elements.token.value);
        if (token) {
            headers.Authorization = "Bearer " + token;
            localStorage.setItem(storageKeys.token, token);
        }

        if (hasBody(method)) {
            headers["Content-Type"] = "application/json";
            var bodyText = elements.body.value.trim();
            if (bodyText) {
                try {
                    JSON.parse(bodyText);
                } catch (error) {
                    setStatus("JSON 오류", "error");
                    writeResponse({ message: "JSON Body 형식이 올바르지 않습니다.", detail: error.message });
                    return;
                }
                options.body = bodyText;
            }
        }

        elements.send.disabled = true;
        elements.elapsedTime.textContent = "";
        setStatus("요청 중", "");
        refreshRequestUrl();

        var startedAt = performance.now();

        try {
            var response = await fetch(url, options);
            var elapsed = Math.round(performance.now() - startedAt);
            var text = await response.text();
            var payload = parseResponseText(text);

            elements.elapsedTime.textContent = elapsed + " ms";
            setStatus(response.status + " " + response.statusText, response.ok ? "ok" : "error");
            writeResponse(payload, text);
            saveLoginTokenIfPresent(payload);
        } catch (error) {
            elements.elapsedTime.textContent = "";
            setStatus("요청 실패", "error");
            writeResponse({
                message: "요청을 보낼 수 없습니다.",
                detail: error.message,
                hint: "Base URL과 서버 실행 상태를 확인하세요."
            });
        } finally {
            elements.send.disabled = false;
            refreshAuthState();
        }
    }

    function buildUrl() {
        var baseUrl = elements.baseUrl.value.trim().replace(/\/+$/, "");
        var path = elements.path.value.trim();
        var query = elements.query.value.trim();

        if (!path.startsWith("/")) {
            path = "/" + path;
        }

        return baseUrl + path + (query ? "?" + query.replace(/^\?+/, "") : "");
    }

    function refreshRequestUrl() {
        elements.requestUrl.textContent = buildUrl();
    }

    function hasBody(method) {
        return ["POST", "PUT", "PATCH", "DELETE"].indexOf(method) >= 0;
    }

    function refreshBodyVisibility() {
        elements.bodyLabel.classList.toggle("hidden", !hasBody(elements.method.value));
    }

    function formatBody() {
        var text = elements.body.value.trim();
        if (!text) {
            elements.body.value = "{}";
            return;
        }

        try {
            elements.body.value = JSON.stringify(JSON.parse(text), null, 2);
            setStatus("JSON 정렬 완료", "ok");
        } catch (error) {
            setStatus("JSON 오류", "error");
            writeResponse({ message: "JSON Body 형식이 올바르지 않습니다.", detail: error.message });
        }
    }

    function parseResponseText(text) {
        if (!text) {
            return null;
        }

        try {
            return JSON.parse(text);
        } catch (error) {
            return text;
        }
    }

    function writeResponse(payload) {
        if (typeof payload === "string") {
            lastResponseText = payload;
        } else {
            lastResponseText = JSON.stringify(payload, null, 2);
        }

        elements.responseOutput.textContent = lastResponseText || "(응답 본문 없음)";
    }

    function saveLoginTokenIfPresent(payload) {
        var token = findToken(payload);
        if (!token) {
            return;
        }

        elements.token.value = token;
        localStorage.setItem(storageKeys.token, normalizeToken(token));
        setStatus("토큰 저장됨", "ok");
    }

    function saveTokenFromResponse() {
        var payload = parseResponseText(lastResponseText);
        var token = findToken(payload);

        if (!token) {
            setStatus("토큰 없음", "error");
            return;
        }

        elements.token.value = token;
        localStorage.setItem(storageKeys.token, normalizeToken(token));
        refreshAuthState();
        setStatus("토큰 저장됨", "ok");
    }

    function findToken(value) {
        if (!value || typeof value !== "object") {
            return "";
        }

        if (typeof value.accessToken === "string") {
            return value.accessToken;
        }

        if (value.data) {
            return findToken(value.data);
        }

        if (value.result) {
            return findToken(value.result);
        }

        return "";
    }

    function normalizeToken(value) {
        return value.trim().replace(/^Bearer\s+/i, "");
    }

    function refreshAuthState() {
        var token = normalizeToken(elements.token.value);
        var hasToken = Boolean(token);
        elements.authState.classList.toggle("has-token", hasToken);
        elements.authState.textContent = hasToken ? "토큰 저장됨: " + token : "저장된 토큰 없음";
    }

    function clearToken() {
        elements.token.value = "";
        localStorage.removeItem(storageKeys.token);
        refreshAuthState();
        setStatus("토큰 삭제됨", "");
    }

    function resetStorage() {
        localStorage.removeItem(storageKeys.baseUrl);
        localStorage.removeItem(storageKeys.token);
        elements.baseUrl.value = "http://localhost:8080";
        clearToken();
        refreshRequestUrl();
    }

    async function copyResponse() {
        if (!lastResponseText) {
            setStatus("복사할 응답 없음", "error");
            return;
        }

        try {
            await navigator.clipboard.writeText(lastResponseText);
            setStatus("응답 복사됨", "ok");
        } catch (error) {
            setStatus("복사 실패", "error");
        }
    }

    function setStatus(text, type) {
        elements.status.textContent = text;
        elements.status.className = "status" + (type ? " " + type : "");
    }
})();
