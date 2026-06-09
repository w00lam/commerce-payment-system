(function () {
    "use strict";

    var api = window.RoviqApi;
    var config = window.RoviqConfig;
    var root = document.getElementById("viewRoot");
    var state = {
        view: "home",
        products: [],
        selectedProduct: null,
        cart: null,
        orders: [],
        orderDetail: null,
        point: null,
        pointHistories: [],
        membership: null,
        grades: [],
        subscription: null,
        loading: false
    };

    var fallbackPlans = [
        { id: 1, name: "베이직", monthlyAmount: 9900, description: "가벼운 정기 배송" },
        { id: 2, name: "스탠다드", monthlyAmount: 19900, description: "포인트 적립 강화" },
        { id: 3, name: "프리미엄", monthlyAmount: 29900, description: "정기 결제 혜택 집중" }
    ];

    document.addEventListener("DOMContentLoaded", function () {
        bindGlobalEvents();
        hydrateMember();
        render();
        refreshHome();
    });

    function bindGlobalEvents() {
        document.body.addEventListener("click", function (event) {
            var viewButton = event.target.closest("[data-view]");
            var actionButton = event.target.closest("[data-action]");
            if (viewButton) {
                openView(viewButton.dataset.view);
            }
            if (actionButton) {
                handleAction(actionButton.dataset.action, actionButton.dataset);
            }
        });

        document.getElementById("loginForm").addEventListener("submit", function (event) {
            event.preventDefault();
            login();
        });
        document.getElementById("signupBtn").addEventListener("click", signup);
        document.getElementById("logoutBtn").addEventListener("click", logout);
        document.getElementById("keepShopping").addEventListener("click", closeCartModal);
        document.getElementById("openCart").addEventListener("click", function () {
            closeCartModal();
            openView("cart");
        });
        document.getElementById("searchInput").addEventListener("input", render);
    }

    function openView(view) {
        state.view = view;
        render();
        if (view === "home") refreshHome();
        if (view === "cart") refreshCart();
        if (view === "orders") refreshOrders();
        if (view === "points") refreshPoints();
        if (view === "membership") refreshMembership();
        if (view === "subscriptions") refreshSubscription();
    }

    function handleAction(action, data) {
        var id = data.id;
        if (action === "select-product") selectProduct(id);
        if (action === "add-cart") addToCart(id);
        if (action === "buy-now") buyNow(id);
        if (action === "update-cart") updateCartItem(id);
        if (action === "remove-cart") removeCartItem(id);
        if (action === "checkout") checkout();
        if (action === "confirm-payment") confirmPayment(id);
        if (action === "order-detail") loadOrderDetail(id);
        if (action === "cancel-order") confirmThen("주문을 취소할까요?", function () { cancelOrder(id); });
        if (action === "refund") refundPayment(id);
        if (action === "recalculate-membership") recalculateMembership();
        if (action === "register-payment-method") registerPaymentMethod();
        if (action === "start-subscription") startSubscription(id);
        if (action === "cancel-subscription") confirmThen("구독을 해지할까요?", function () { cancelSubscription(id); });
        if (action === "save-product") saveProduct();
        if (action === "delete-product") confirmThen("상품을 판매 목록에서 내릴까요?", function () { deleteProduct(id); });
    }

    function hydrateMember() {
        var member = readMember();
        updateMemberUi(member);
    }

    function login() {
        withLoading(api.auth.login({
            email: value("emailInput"),
            password: value("passwordInput")
        })).then(function (member) {
            api.setToken(member.accessToken);
            localStorage.setItem(config.MEMBER_KEY, JSON.stringify(member.member || {}));
            updateMemberUi(member.member || {});
            toast("반갑습니다. 쇼핑을 이어갈게요.", "ok");
            refreshHome();
        }).catch(showError);
    }

    function signup() {
        withLoading(api.auth.signup({
            email: value("emailInput"),
            password: value("passwordInput"),
            name: value("nameInput") || "테스터",
            phone: value("phoneInput") || "010-1234-5678"
        })).then(function () {
            toast("가입이 완료되었습니다. 바로 로그인해주세요.", "ok");
        }).catch(showError);
    }

    function logout() {
        api.auth.logout().catch(function () { return null; }).then(function () {
            api.setToken("");
            localStorage.removeItem(config.MEMBER_KEY);
            updateMemberUi(null);
            toast("로그아웃되었습니다.", "ok");
            render();
        });
    }

    function refreshHome() {
        withLoading(api.products.list()).then(function (payload) {
            state.products = api.pageItems(payload);
            render();
        }).catch(showError);
        refreshCart(true);
        refreshPoints(true);
        refreshMembership(true);
        refreshSubscription(true);
    }

    function refreshCart(quiet) {
        if (!api.token()) return Promise.resolve();
        return api.cart.get().then(function (cart) {
            state.cart = cart;
            updateSummary();
            if (!quiet) render();
        }).catch(function (error) {
            if (!quiet) showError(error);
        });
    }

    function refreshOrders() {
        if (!api.token()) {
            renderLoginEmpty("주문 내역은 로그인 후 확인할 수 있습니다.");
            return;
        }
        withLoading(api.orders.list()).then(function (payload) {
            state.orders = api.pageItems(payload);
            render();
        }).catch(showError);
    }

    function refreshPoints(quiet) {
        if (!api.token()) return Promise.resolve();
        return Promise.all([api.points.me(), api.points.histories()]).then(function (results) {
            state.point = results[0];
            state.pointHistories = api.pageItems(results[1]);
            updateSummary();
            if (!quiet) render();
        }).catch(function (error) {
            if (!quiet) showError(error);
        });
    }

    function refreshMembership(quiet) {
        var calls = [api.memberships.grades()];
        if (api.token()) calls.push(api.memberships.me());
        return Promise.all(calls).then(function (results) {
            state.grades = results[0] || [];
            state.membership = results[1] || null;
            updateSummary();
            if (!quiet) render();
        }).catch(function (error) {
            if (!quiet) showError(error);
        });
    }

    function refreshSubscription(quiet) {
        if (!api.token()) return Promise.resolve();
        return api.subscriptions.me().then(function (subscription) {
            state.subscription = subscription;
            updateSummary();
            if (!quiet) render();
        }).catch(function () {
            state.subscription = null;
            updateSummary();
            if (!quiet) render();
        });
    }

    function selectProduct(id) {
        withLoading(api.products.detail(id)).then(function (product) {
            state.selectedProduct = product;
            render();
        }).catch(showError);
    }

    function addToCart(id) {
        requireLogin(function () {
            withLoading(api.cart.add({ productId: Number(id), quantity: 1 })).then(function () {
                refreshCart(true);
                document.getElementById("cartModal").classList.remove("hidden");
            }).catch(showError);
        });
    }

    function buyNow(id) {
        requireLogin(function () {
            api.cart.clear().catch(function () { return null; }).then(function () {
                return api.cart.add({ productId: Number(id), quantity: 1 });
            }).then(function () {
                return refreshCart(true);
            }).then(function () {
                openView("cart");
            }).catch(showError);
        });
    }

    function updateCartItem(id) {
        var quantity = Number(value("qty-" + id) || 1);
        withLoading(api.cart.update(id, { quantity: quantity })).then(function () {
            toast("수량을 변경했습니다.", "ok");
            refreshCart();
        }).catch(showError);
    }

    function removeCartItem(id) {
        withLoading(api.cart.remove(id)).then(function () {
            toast("장바구니에서 삭제했습니다.", "ok");
            refreshCart();
        }).catch(showError);
    }

    function checkout() {
        var itemIds = cartItems().map(function (item) { return item.cartItemId; });
        if (!itemIds.length) {
            toast("결제할 상품을 먼저 담아주세요.", "error");
            return;
        }
        var usedPointAmount = Number(value("usedPointAmount") || 0);
        withLoading(api.orders.create({ cartItemIds: itemIds, usedPointAmount: usedPointAmount })).then(function (order) {
            state.orderDetail = null;
            state.orders.unshift(order);
            state.view = "orders";
            render();
            if (!order.paymentId || Number(order.finalPaymentAmount || 0) <= 0) {
                toast("주문이 완료되었습니다.", "ok");
                refreshOrders();
                refreshPoints(true);
                refreshMembership(true);
                return null;
            }
            toast("결제창을 여는 중입니다.", "ok");
            return requestPortOnePayment(order).then(function (paymentResult) {
                if (paymentResult.cancelled) {
                    toast("결제가 취소되었습니다.", "error");
                    return null;
                }
                return confirmPayment(order.paymentId, { fromPortOne: true });
            });
        }).catch(showError);
    }

    function requestPortOnePayment(order) {
        return Promise.resolve().then(function () {
            var paymentConfig = config.getPortOnePaymentConfig ? config.getPortOnePaymentConfig() : {};
            if (!paymentConfig || !paymentConfig.storeId || !paymentConfig.channelKey) {
                throw new Error("결제창 설정을 불러오지 못했습니다.");
            }
            if (!window.PortOne || typeof window.PortOne.requestPayment !== "function") {
                throw new Error("포트원 결제창을 불러오지 못했습니다. 잠시 후 다시 시도해주세요.");
            }

            var member = readMember() || {};
            var request = {
                storeId: paymentConfig.storeId,
                channelKey: paymentConfig.channelKey,
                paymentId: order.paymentId,
                orderName: order.paymentOrderName || ("order-" + order.orderId),
                totalAmount: Number(order.finalPaymentAmount || 0),
                currency: "KRW",
                payMethod: "CARD",
                customer: {
                    customerId: String(order.memberId || member.memberId || ""),
                    fullName: member.name || "Roviq 고객",
                    email: member.email || value("emailInput") || "tester@example.com"
                }
            };

            return window.PortOne.requestPayment(request).then(function (response) {
                if (isPortOneCancelled(response)) {
                    return { cancelled: true, response: response };
                }
                return { cancelled: false, response: response };
            }).catch(function (error) {
                if (isPortOneCancelled(error)) {
                    return { cancelled: true, response: error };
                }
                throw error;
            });
        });
    }

    function isPortOneCancelled(value) {
        var text = JSON.stringify(value || {}).toLowerCase();
        return text.indexOf("cancel") >= 0 || text.indexOf("취소") >= 0;
    }

    function confirmPayment(paymentId, options) {
        if (!paymentId || paymentId === "undefined") {
            toast("확인할 결제 정보가 없습니다.", "error");
            return Promise.resolve(null);
        }
        return withLoading(api.payments.confirm(paymentId)).then(function (result) {
            toast(options && options.fromPortOne ? "결제가 승인되어 주문이 완료되었습니다." : "결제가 완료되었습니다.", "ok");
            refreshOrders();
            refreshPoints(true);
            refreshMembership(true);
            state.view = "orders";
            render();
        }).catch(function (error) {
            toast("결제 승인을 확인 중입니다. 카드 승인 후 다시 주문을 확인해주세요.", "error");
            showError(error);
        });
    }

    function loadOrderDetail(id) {
        withLoading(api.orders.detail(id)).then(function (detail) {
            state.orderDetail = detail;
            render();
        }).catch(showError);
    }

    function cancelOrder(id) {
        withLoading(api.orders.cancel(id)).then(function () {
            toast("주문을 취소했습니다.", "ok");
            refreshOrders();
        }).catch(showError);
    }

    function refundPayment(paymentId) {
        var items = (state.orderDetail && state.orderDetail.orderItems || []).map(function (item) {
            return { orderItemId: item.orderItemId, quantity: item.quantity };
        });
        if (!items.length) {
            toast("환불할 주문 상세를 먼저 열어주세요.", "error");
            return;
        }
        withLoading(api.refunds.create(paymentId, { reason: value("refundReason") || "고객 요청", items: items })).then(function () {
            toast("환불 요청이 접수되었습니다.", "ok");
        }).catch(showError);
    }

    function recalculateMembership() {
        withLoading(api.memberships.recalculate()).then(function () {
            toast("멤버십 혜택을 새로 반영했습니다.", "ok");
            refreshMembership();
        }).catch(showError);
    }

    function registerPaymentMethod() {
        requireLogin(function () {
            withLoading(api.subscriptions.registerPaymentMethod({
                portoneBillingKey: value("billingKey"),
                cardCompanyName: value("cardCompany")
            })).then(function () {
                toast("정기 결제수단을 등록했습니다.", "ok");
            }).catch(showError);
        });
    }

    function startSubscription(planId) {
        requireLogin(function () {
            withLoading(api.subscriptions.start({
                planId: Number(planId),
                paymentMethodId: Number(value("paymentMethodId") || 1)
            })).then(function (subscription) {
                state.subscription = subscription;
                toast("구독이 시작되었습니다.", "ok");
                render();
                updateSummary();
            }).catch(showError);
        });
    }

    function cancelSubscription(id) {
        withLoading(api.subscriptions.cancel(id)).then(function () {
            state.subscription = null;
            toast("구독을 해지했습니다.", "ok");
            refreshSubscription();
        }).catch(showError);
    }

    function saveProduct() {
        var product = {
            name: value("productName"),
            price: Number(value("productPrice") || 0),
            stock: Number(value("productStock") || 0),
            description: value("productDescription"),
            status: value("productStatus") || "ON_SALE",
            category: value("productCategory") || "FOOD"
        };
        var id = value("productId");
        var call = id ? api.products.update(id, product) : api.products.create(product);
        withLoading(call).then(function () {
            toast("상품 정보가 저장되었습니다.", "ok");
            refreshHome();
        }).catch(showError);
    }

    function deleteProduct(id) {
        withLoading(api.products.remove(id)).then(function () {
            toast("상품을 내렸습니다.", "ok");
            refreshHome();
        }).catch(showError);
    }

    function render() {
        document.querySelectorAll(".tabs button").forEach(function (button) {
            button.classList.toggle("active", button.dataset.view === state.view);
        });
        updateSummary();
        if (state.view === "home") renderHome();
        if (state.view === "cart") renderCart();
        if (state.view === "orders") renderOrders();
        if (state.view === "points") renderPoints();
        if (state.view === "membership") renderMembership();
        if (state.view === "subscriptions") renderSubscriptions();
        if (state.view === "seller") renderSeller();
    }

    function renderHome() {
        var query = value("searchInput").toLowerCase();
        var products = state.products.filter(function (product) {
            return !query || String(product.name || "").toLowerCase().indexOf(query) >= 0;
        });
        root.innerHTML =
            '<div class="view-grid">' +
                '<div class="panel"><h2>추천 상품</h2>' +
                    (products.length ? '<div class="product-grid">' + products.map(productCard).join("") + '</div>' : empty("판매 중인 상품이 없습니다.")) +
                '</div>' +
                '<aside class="panel">' + productDetail() + '</aside>' +
            '</div>';
    }

    function renderCart() {
        var items = cartItems();
        root.innerHTML =
            '<div class="view-grid">' +
                '<div class="panel"><h2>장바구니</h2>' +
                    (items.length ? items.map(cartLine).join("") : empty("담긴 상품이 없습니다.")) +
                '</div>' +
                '<aside class="panel"><h3>주문 확인</h3>' +
                    '<p class="muted">포인트를 사용할 수 있고, 결제 후 주문 완료 화면으로 이어집니다.</p>' +
                    '<label class="label">사용할 포인트</label><input id="usedPointAmount" type="number" min="0" value="0">' +
                    '<p class="price">합계 ' + money(state.cart && state.cart.totalAmount) + '</p>' +
                    '<button type="button" class="btn primary full" data-action="checkout">결제하기</button>' +
                '</aside>' +
            '</div>';
    }

    function renderOrders() {
        root.innerHTML =
            '<div class="view-grid">' +
                '<div class="panel"><h2>주문 내역</h2>' +
                    (state.orders.length ? state.orders.map(orderLine).join("") : empty("주문 내역이 없습니다.")) +
                '</div>' +
                '<aside class="panel"><h3>주문 상세</h3>' + orderDetailHtml() + '</aside>' +
            '</div>';
    }

    function renderPoints() {
        root.innerHTML =
            '<div class="view-grid">' +
                '<div class="panel"><h2>포인트</h2><p class="price">' + money(state.point && state.point.pointBalance) + '</p>' +
                    '<h3>내역</h3>' + (state.pointHistories.length ? state.pointHistories.map(function (item) {
                        return '<div class="order-line"><div><strong>' + escapeHtml(item.type) + '</strong><p class="muted">' + dateText(item.createdAt) + '</p></div><strong>' + money(item.amount) + '</strong></div>';
                    }).join("") : empty("포인트 내역이 없습니다.")) +
                '</div>' +
                '<aside class="panel"><h3>포인트 안내</h3><p class="muted">주문 결제, 환불, 구독 결제에 따라 포인트가 자동으로 반영됩니다.</p></aside>' +
            '</div>';
    }

    function renderMembership() {
        root.innerHTML =
            '<div class="view-grid">' +
                '<div class="panel"><h2>멤버십</h2>' + membershipHtml() + '</div>' +
                '<aside class="panel"><h3>등급 혜택</h3>' + (state.grades.length ? state.grades.map(function (grade) {
                    return '<div class="order-line"><div><strong>' + escapeHtml(grade.name) + '</strong><p class="muted">누적 ' + money(grade.minCumulativePaymentAmount) + ' 이상</p></div><span class="badge ok">' + grade.pointRewardRate + '% 적립</span></div>';
                }).join("") : empty("등급 정보가 없습니다.")) + '</aside>' +
            '</div>';
    }

    function renderSubscriptions() {
        root.innerHTML =
            '<div class="view-grid">' +
                '<div class="panel"><h2>정기 구독</h2>' + subscriptionHtml() +
                    '<h3>플랜 선택</h3>' + fallbackPlans.map(planCard).join("") +
                '</div>' +
                '<aside class="panel"><h3>결제수단</h3>' +
                    '<label class="label">빌링키</label><input id="billingKey" value="billing-key-demo">' +
                    '<label class="label">카드사</label><input id="cardCompany" value="Roviq Card">' +
                    '<label class="label">결제수단 ID</label><input id="paymentMethodId" type="number" value="1">' +
                    '<button type="button" class="btn primary full" data-action="register-payment-method">결제수단 등록</button>' +
                    '<p class="muted">플랜 목록과 청구서 조회는 백엔드 조회 API가 추가되면 실제 데이터로 바뀝니다.</p>' +
                '</aside>' +
            '</div>';
    }

    function renderSeller() {
        root.innerHTML =
            '<div class="view-grid">' +
                '<div class="panel"><h2>상품 관리</h2><div class="form-grid">' +
                    input("productId", "상품 ID", "") +
                    input("productName", "상품명", "햇반 210g 24개입") +
                    input("productPrice", "가격", "25000", "number") +
                    input("productStock", "재고", "100", "number") +
                    input("productCategory", "카테고리", "FOOD") +
                    input("productStatus", "상태", "ON_SALE") +
                    '<textarea id="productDescription" class="full" placeholder="상품 설명">매일 먹기 좋은 즉석밥 세트</textarea>' +
                    '<button type="button" class="btn primary full" data-action="save-product">상품 저장</button>' +
                '</div></div>' +
                '<aside class="panel"><h3>판매 상품</h3>' + (state.products.length ? state.products.map(function (product) {
                    return '<div class="order-line"><div><strong>' + escapeHtml(product.name) + '</strong><p class="muted">' + money(product.price) + '</p></div><button class="btn danger" data-action="delete-product" data-id="' + product.id + '">내리기</button></div>';
                }).join("") : empty("상품이 없습니다.")) + '</aside>' +
            '</div>';
    }

    function productCard(product) {
        return '<article class="product-card">' +
            '<div class="product-image">' + escapeHtml(String(product.name || "R").charAt(0)) + '</div>' +
            '<div><strong>' + escapeHtml(product.name) + '</strong><p class="muted">' + escapeHtml(product.category || "상품") + '</p></div>' +
            '<div class="price">' + money(product.price) + '</div>' +
            '<div class="actions">' +
                '<button class="btn secondary" data-action="select-product" data-id="' + product.id + '">상세보기</button>' +
                '<button class="btn primary" data-action="add-cart" data-id="' + product.id + '">담기</button>' +
                '<button class="btn secondary" data-action="buy-now" data-id="' + product.id + '">바로구매</button>' +
            '</div>' +
        '</article>';
    }

    function productDetail() {
        var product = state.selectedProduct;
        if (!product) return '<h3>상품 상세</h3>' + empty("상품을 선택하면 상세 정보가 표시됩니다.");
        return '<h3>' + escapeHtml(product.name) + '</h3><p class="price">' + money(product.price) + '</p>' +
            '<p>' + escapeHtml(product.description || "상품 설명이 없습니다.") + '</p>' +
            '<p class="muted">재고 ' + (product.stock || "-") + '개</p>' +
            '<button class="btn primary" data-action="buy-now" data-id="' + product.id + '">구매하기</button>';
    }

    function cartLine(item) {
        return '<div class="cart-line"><div><strong>' + escapeHtml(item.productName) + '</strong><p class="muted">' + money(item.price) + '</p></div>' +
            '<div class="actions"><input id="qty-' + item.cartItemId + '" type="number" min="1" value="' + item.quantity + '" style="width:82px">' +
            '<button class="btn secondary" data-action="update-cart" data-id="' + item.cartItemId + '">변경</button>' +
            '<button class="btn danger" data-action="remove-cart" data-id="' + item.cartItemId + '">삭제</button></div></div>';
    }

    function orderLine(order) {
        var status = order.status || order.orderStatus || "-";
        return '<div class="order-line"><div><strong>' + escapeHtml(order.orderNumber || ("주문 #" + order.orderId)) + '</strong><p class="muted">' + dateText(order.orderedAt) + '</p></div>' +
            '<div class="actions"><span class="badge ' + statusClass(status) + '">' + escapeHtml(status) + '</span>' +
            '<button class="btn secondary" data-action="order-detail" data-id="' + order.orderId + '">상세보기</button>' +
            '<button class="btn danger" data-action="cancel-order" data-id="' + order.orderId + '">취소</button></div></div>';
    }

    function orderDetailHtml() {
        var detail = state.orderDetail;
        if (!detail) return empty("주문을 선택하면 상세 정보가 표시됩니다.");
        var payment = detail.payment || {};
        return '<p><span class="label">주문번호</span><strong>' + escapeHtml(detail.orderNumber) + '</strong></p>' +
            '<p><span class="label">주문 상태</span><span class="badge ' + statusClass(detail.orderStatus) + '">' + escapeHtml(detail.orderStatus) + '</span></p>' +
            '<p><span class="label">결제 상태</span><span class="badge ' + statusClass(payment.status) + '">' + escapeHtml(payment.status || "-") + '</span></p>' +
            '<p class="price">' + money(payment.finalPaymentAmount || detail.totalPrice) + '</p>' +
            '<input id="refundReason" value="고객 요청" aria-label="환불 사유">' +
            '<div class="actions"><button class="btn primary" data-action="confirm-payment" data-id="' + payment.paymentId + '">결제 확인</button>' +
            '<button class="btn danger" data-action="refund" data-id="' + payment.paymentId + '">환불 요청</button></div>';
    }

    function membershipHtml() {
        if (!state.membership) return empty("로그인 후 멤버십을 확인할 수 있습니다.");
        var grade = state.membership.grade || {};
        return '<p class="price">' + escapeHtml(grade.name || "-") + '</p>' +
            '<p>누적 결제 금액 ' + money(state.membership.cumulativePaymentAmount) + '</p>' +
            '<button class="btn primary" data-action="recalculate-membership">혜택 새로고침</button>';
    }

    function subscriptionHtml() {
        if (!state.subscription) return '<p class="muted">현재 이용 중인 구독이 없습니다.</p>';
        return '<div class="order-line"><div><strong>' + escapeHtml(state.subscription.planName) + '</strong><p class="muted">다음 결제일 ' + dateText(state.subscription.nextBillingDate) + '</p></div>' +
            '<button class="btn danger" data-action="cancel-subscription" data-id="' + state.subscription.id + '">해지</button></div>';
    }

    function planCard(plan) {
        return '<div class="plan order-line"><div><strong>' + escapeHtml(plan.name) + '</strong><p class="muted">' + escapeHtml(plan.description) + '</p></div>' +
            '<div><p class="price">' + money(plan.monthlyAmount) + '</p><button class="btn primary" data-action="start-subscription" data-id="' + plan.id + '">구독하기</button></div></div>';
    }

    function updateSummary() {
        setText("cartSummary", cartItems().length + "개");
        setText("pointSummary", state.point ? money(state.point.pointBalance) : "-");
        setText("membershipSummary", state.membership && state.membership.grade ? state.membership.grade.name : "-");
        setText("subscriptionSummary", state.subscription ? state.subscription.status : "-");
    }

    function updateMemberUi(member) {
        var loggedIn = Boolean(api.token() && member);
        document.getElementById("profileRow").classList.toggle("hidden", !loggedIn);
        document.getElementById("loginFields").classList.toggle("hidden", loggedIn);
        if (loggedIn) {
            setText("profileName", member.name || "회원");
            setText("profileEmail", member.email || "");
            setText("profileAvatar", String(member.name || member.email || "R").charAt(0));
        }
    }

    function cartItems() {
        return state.cart && Array.isArray(state.cart.items) ? state.cart.items : [];
    }

    function requireLogin(next) {
        if (!api.token()) {
            toast("로그인 후 이용할 수 있습니다.", "error");
            return;
        }
        next();
    }

    function withLoading(promise) {
        state.loading = true;
        return promise.finally(function () {
            state.loading = false;
        });
    }

    function confirmThen(message, next) {
        document.getElementById("confirmMessage").textContent = message;
        document.getElementById("confirmModal").classList.remove("hidden");
        document.getElementById("confirmOk").onclick = function () {
            document.getElementById("confirmModal").classList.add("hidden");
            next();
        };
        document.getElementById("confirmCancel").onclick = function () {
            document.getElementById("confirmModal").classList.add("hidden");
        };
    }

    function closeCartModal() {
        document.getElementById("cartModal").classList.add("hidden");
    }

    function renderLoginEmpty(message) {
        root.innerHTML = '<div class="panel">' + empty(message) + '</div>';
    }

    function empty(message) {
        return '<div class="empty">' + escapeHtml(message) + '</div>';
    }

    function toast(message, type) {
        var node = document.createElement("div");
        node.className = "toast " + (type || "");
        node.textContent = message;
        document.getElementById("toastHost").appendChild(node);
        setTimeout(function () { node.remove(); }, 3200);
    }

    function showError(error) {
        toast(error && error.message ? error.message : "처리 중 문제가 생겼습니다.", "error");
    }

    function readMember() {
        try {
            return JSON.parse(localStorage.getItem(config.MEMBER_KEY) || "null");
        } catch (error) {
            return null;
        }
    }

    function value(id) {
        var element = document.getElementById(id);
        return element ? element.value.trim() : "";
    }

    function setText(id, text) {
        var element = document.getElementById(id);
        if (element) element.textContent = text;
    }

    function input(id, label, value, type) {
        return '<label><span class="label">' + label + '</span><input id="' + id + '" type="' + (type || "text") + '" value="' + escapeHtml(value) + '"></label>';
    }

    function money(value) {
        var number = Number(value || 0);
        return number.toLocaleString("ko-KR") + "원";
    }

    function dateText(value) {
        return value ? String(value).replace("T", " ").slice(0, 16) : "-";
    }

    function statusClass(status) {
        var normalized = String(status || "").toUpperCase();
        if (normalized.indexOf("CONFIRMED") >= 0 || normalized.indexOf("SUCCESS") >= 0 || normalized.indexOf("PAID") >= 0) return "ok";
        if (normalized.indexOf("CANCEL") >= 0 || normalized.indexOf("FAIL") >= 0 || normalized.indexOf("REFUND") >= 0) return "danger";
        return "warn";
    }

    function escapeHtml(value) {
        return String(value || "")
            .replace(/&/g, "&amp;")
            .replace(/</g, "&lt;")
            .replace(/>/g, "&gt;")
            .replace(/"/g, "&quot;")
            .replace(/'/g, "&#039;");
    }
})();
