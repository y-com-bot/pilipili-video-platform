(function () {
    const SESSION_KEY = "studio-session";
    const LIKE_TARGET = {
        VIDEO: 0,
        COMMENT: 1,
        DYNAMIC: 2
    };
    const CONTENT_TARGET = {
        VIDEO: 0,
        DYNAMIC: 1
    };
    const FAVORITE_TARGET = {
        VIDEO: 0,
        DYNAMIC: 1
    };

    function normalizePath(path) {
        if (!path) {
            return "./";
        }
        if (/^(https?:)?\/\//.test(path) || path.startsWith("./") || path.startsWith("../") || path.startsWith("/")) {
            return path;
        }
        return "./" + path;
    }

    function escapeHtml(value) {
        return String(value ?? "")
            .replace(/&/g, "&amp;")
            .replace(/</g, "&lt;")
            .replace(/>/g, "&gt;")
            .replace(/"/g, "&quot;")
            .replace(/'/g, "&#39;");
    }

    function formatCount(value) {
        const number = Number(value || 0);
        if (number >= 10000) {
            return (number / 10000).toFixed(1).replace(/\.0$/, "") + "w";
        }
        return String(number);
    }

    function formatDate(value) {
        if (!value) {
            return "--";
        }
        const date = value instanceof Date ? value : new Date(value);
        if (Number.isNaN(date.getTime())) {
            return String(value);
        }
        const pad = function (part) {
            return String(part).padStart(2, "0");
        };
        return date.getFullYear()
            + "-" + pad(date.getMonth() + 1)
            + "-" + pad(date.getDate())
            + " " + pad(date.getHours())
            + ":" + pad(date.getMinutes());
    }

    function safeJsonParse(raw) {
        if (!raw) {
            return {};
        }
        try {
            return JSON.parse(raw);
        } catch (error) {
            return raw;
        }
    }

    function readSession() {
        try {
            const session = JSON.parse(localStorage.getItem(SESSION_KEY) || "{}");
            return {
                token: session.token || "",
                role: session.role || "guest",
                userId: session.userId ?? null
            };
        } catch (error) {
            return {
                token: "",
                role: "guest",
                userId: null
            };
        }
    }

    function saveSession(payload) {
        const session = {
            token: payload.token || "",
            role: payload.role || "user",
            userId: payload.userId ?? null
        };
        localStorage.setItem(SESSION_KEY, JSON.stringify(session));
        return session;
    }

    function clearSession() {
        localStorage.removeItem(SESSION_KEY);
    }

    function isLoggedIn() {
        return Boolean(readSession().token);
    }

    function isAdmin() {
        return readSession().role === "admin";
    }

    async function request(path, options = {}) {
        const session = readSession();
        const headers = Object.assign({}, options.headers || {});
        const method = (options.method || "GET").toUpperCase();
        let body = options.body;

        if (session.token) {
            headers.Authorization = session.token;
        }

        if (options.formData) {
            body = options.formData;
        } else if (options.form) {
            body = new URLSearchParams();
            Object.keys(options.form).forEach(function (key) {
                const value = options.form[key];
                if (value !== undefined && value !== null && value !== "") {
                    body.append(key, value);
                }
            });
            headers["Content-Type"] = "application/x-www-form-urlencoded;charset=UTF-8";
        }

        const response = await fetch(normalizePath(path), {
            method: method,
            headers: headers,
            body: body,
            credentials: "same-origin"
        });

        const raw = await response.text();
        const data = safeJsonParse(raw);
        const message = extractMessage(data) || ("请求失败，状态码 " + response.status);

        if (!response.ok) {
            if (response.status === 401) {
                clearSession();
            }
            throw new Error(message);
        }

        if (data && typeof data === "object" && !Array.isArray(data)) {
            if (data.success === false || (typeof data.code === "number" && data.code >= 400)) {
                if (data.code === 401) {
                    clearSession();
                }
                throw new Error(extractMessage(data) || "请求失败");
            }
        }

        return data;
    }

    function extractMessage(data) {
        if (!data) {
            return "";
        }
        if (typeof data === "string") {
            return data;
        }
        if (typeof data.message === "string") {
            return data.message;
        }
        if (typeof data.error === "string") {
            return data.error;
        }
        return "";
    }

    function ensureToastRoot() {
        let root = document.querySelector(".toast-stack");
        if (!root) {
            root = document.createElement("div");
            root.className = "toast-stack";
            document.body.appendChild(root);
        }
        return root;
    }

    function showToast(message, type) {
        const toast = document.createElement("div");
        toast.className = "toast" + (type ? " " + type : "");
        toast.textContent = message;
        ensureToastRoot().appendChild(toast);

        window.setTimeout(function () {
            toast.style.opacity = "0";
            toast.style.transform = "translateY(6px)";
            window.setTimeout(function () {
                toast.remove();
            }, 240);
        }, 2600);
    }

    function attachSessionUi() {
        const session = readSession();
        const statusNodes = document.querySelectorAll("[data-session-label]");
        const adminNodes = document.querySelectorAll("[data-admin-only]");
        const guestNodes = document.querySelectorAll("[data-guest-only]");
        const userNodes = document.querySelectorAll("[data-user-only]");

        statusNodes.forEach(function (node) {
            if (!session.token) {
                node.textContent = "游客模式";
                return;
            }
            const roleText = session.role === "admin" ? "管理员" : "创作者";
            node.textContent = roleText + " · 用户 #" + session.userId;
        });

        adminNodes.forEach(function (node) {
            node.classList.toggle("hidden", !isAdmin());
        });

        guestNodes.forEach(function (node) {
            node.classList.toggle("hidden", isLoggedIn());
        });

        userNodes.forEach(function (node) {
            node.classList.toggle("hidden", !isLoggedIn());
        });
    }

    function requireLogin(redirectPath) {
        if (isLoggedIn()) {
            return true;
        }
        showToast("请先登录后再继续操作", "error");
        window.setTimeout(function () {
            window.location.href = normalizePath(redirectPath || "auth.html");
        }, 500);
        return false;
    }

    function requireAdmin(redirectPath) {
        if (isAdmin()) {
            return true;
        }
        showToast("该页面仅管理员可访问", "error");
        window.setTimeout(function () {
            window.location.href = normalizePath(redirectPath || "index.html");
        }, 500);
        return false;
    }

    function mountLogout(buttonSelector) {
        document.querySelectorAll(buttonSelector || "[data-logout]").forEach(function (button) {
            button.addEventListener("click", function () {
                clearSession();
                showToast("已退出当前登录状态", "success");
                window.setTimeout(function () {
                    window.location.href = normalizePath("index.html");
                }, 380);
            });
        });
    }

    window.StudioApp = {
        LIKE_TARGET: LIKE_TARGET,
        CONTENT_TARGET: CONTENT_TARGET,
        FAVORITE_TARGET: FAVORITE_TARGET,
        escapeHtml: escapeHtml,
        formatCount: formatCount,
        formatDate: formatDate,
        request: request,
        readSession: readSession,
        saveSession: saveSession,
        clearSession: clearSession,
        isLoggedIn: isLoggedIn,
        isAdmin: isAdmin,
        showToast: showToast,
        attachSessionUi: attachSessionUi,
        requireLogin: requireLogin,
        requireAdmin: requireAdmin,
        mountLogout: mountLogout,
        normalizePath: normalizePath
    };
}());
