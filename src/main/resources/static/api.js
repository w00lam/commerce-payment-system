(function () {
    "use strict";

    var config = window.RoviqConfig;

    function token() {
        return (localStorage.getItem(config.TOKEN_KEY) || "").replace(/^Bearer\s+/i, "").trim();
    }

    function setToken(value) {
        var next = (value || "").replace(/^Bearer\s+/i, "").trim();
        if (next) {
            localStorage.setItem(config.TOKEN_KEY, next);
        } else {
            localStorage.removeItem(config.TOKEN_KEY);
        }
    }

    function request(method, path, options) {
        var headers = { Accept: "application/json" };
        var body = options && options.body;

        if (body !== undefined) {
            headers["Content-Type"] = "application/json";
        }

        var accessToken = token();
        if (accessToken) {
            headers.Authorization = "Bearer " + accessToken;
        }

        return fetch(config.DEFAULT_API_BASE_URL + path, {
            method: method,
            headers: headers,
            body: body === undefined ? undefined : JSON.stringify(body)
        }).then(function (response) {
            return response.text().then(function (text) {
                var payload = text ? JSON.parse(text) : null;
                if (!response.ok || (payload && payload.success === false)) {
                    var message = payload && (payload.message || payload.errorMessage);
                    throw new Error(message || "요청을 처리하지 못했습니다.");
                }
                return payload && Object.prototype.hasOwnProperty.call(payload, "data")
                    ? payload.data
                    : payload;
            });
        });
    }

    function pageItems(payload) {
        if (!payload) {
            return [];
        }
        return payload.content || payload.items || payload.data || [];
    }

    window.RoviqApi = {
        token: token,
        setToken: setToken,
        pageItems: pageItems,
        auth: {
            login: function (body) { return request("POST", "/api/auth/login", { body: body }); },
            signup: function (body) { return request("POST", "/api/auth/signup", { body: body }); },
            logout: function () { return request("POST", "/api/auth/logout"); }
        },
        products: {
            list: function () { return request("GET", "/api/products?page=0&size=20"); },
            detail: function (id) { return request("GET", "/api/products/" + encodeURIComponent(id)); },
            create: function (body) { return request("POST", "/api/products", { body: body }); },
            update: function (id, body) { return request("PUT", "/api/products/" + encodeURIComponent(id), { body: body }); },
            remove: function (id) { return request("DELETE", "/api/products/" + encodeURIComponent(id)); }
        },
        cart: {
            get: function () { return request("GET", "/api/carts"); },
            add: function (body) { return request("POST", "/api/carts/items", { body: body }); },
            update: function (id, body) { return request("PUT", "/api/carts/items/" + encodeURIComponent(id), { body: body }); },
            remove: function (id) { return request("DELETE", "/api/carts/items/" + encodeURIComponent(id)); },
            clear: function () { return request("DELETE", "/api/carts/items"); }
        },
        orders: {
            create: function (body) { return request("POST", "/api/orders", { body: body }); },
            preview: function (body) { return request("POST", "/api/orders/preview", { body: body }); },
            list: function () { return request("GET", "/api/orders?page=0&size=20"); },
            detail: function (id) { return request("GET", "/api/orders/" + encodeURIComponent(id)); },
            cancel: function (id) { return request("PATCH", "/api/orders/" + encodeURIComponent(id) + "/cancel"); }
        },
        payments: {
            confirm: function (paymentId) { return request("POST", "/api/payments/" + encodeURIComponent(paymentId) + "/confirm"); }
        },
        refunds: {
            create: function (paymentId, body) {
                return request("POST", "/api/payments/" + encodeURIComponent(paymentId) + "/refunds", { body: body });
            }
            // TODO: Refund history and refund availability endpoints are not exposed by the backend yet.
        },
        points: {
            me: function () { return request("GET", "/api/points"); },
            histories: function () { return request("GET", "/api/points/histories?page=0&size=20"); }
        },
        memberships: {
            me: function () { return request("GET", "/api/memberships/me"); },
            grades: function () { return request("GET", "/api/memberships/grades"); },
            recalculate: function () { return request("POST", "/api/memberships/recalculate"); }
        },
        subscriptions: {
            registerPaymentMethod: function (body) { return request("POST", "/api/subscriptions/payment-methods", { body: body }); },
            start: function (body) { return request("POST", "/api/subscriptions", { body: body }); },
            me: function () { return request("GET", "/api/subscriptions/me"); },
            cancel: function (id) { return request("POST", "/api/subscriptions/cancel/" + encodeURIComponent(id)); }
            // TODO: Plan list and invoice list endpoints are not exposed by the backend yet.
        }
    };
})();
