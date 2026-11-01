/* dashboard：銀行管理介面的前端互動、資料驗證與 API 呼叫。 */
"use strict";

// 儀表板透過帳戶查詢與交易歷史 API 取得分析所需資料。
const API = Object.freeze({
    account: (id) => `/api/accounts/${id}`,
    history: (id, page, size) =>
        `/api/accounts/${id}/transactions?page=${page}&size=${size}`
});

const PAGE_SIZE = 50;      // 交易紀錄查詢服務允許的分頁最大值
const MAX_PAGES = 200;     // 最多讀取 10,000 筆，避免瀏覽器無限制抓取

// 依 HTML id 取得 DOM 元素，減少重複查詢程式碼。
const byId = (id) => document.getElementById(id);
// 依台灣地區格式顯示 TWD 金額。
const money = (value) => new Intl.NumberFormat("zh-TW", {
    style: "currency", currency: "TWD", minimumFractionDigits: 2,
    maximumFractionDigits: 2
}).format(value);

// 更新使用者可見的狀態訊息與 CSS 樣式。
function setStatus(message, kind = "") {
    const status = byId("statusMessage");
    status.textContent = message;
    status.className = `message ${kind}`;
}

// 將伺服器錯誤與非 JSON 回覆轉成可理解的錯誤訊息。

// 發送 HTTP 請求、判斷登入狀態及解析 JSON，並統一處理錯誤。
async function fetchJson(url) {
    let response;

    try {
        // 使用相同來源的登入工作階段執行 GET 查詢
        response = await fetch(url, {
            method: "GET",
            credentials: "same-origin",
            cache: "no-store",
            headers: {
                Accept: "application/json"
            }
        });
    } catch (_) {
        // 網路無法連線時，提供使用者可以理解的訊息
        throw new Error(
            "無法連線到伺服器，請確認 Spring Boot 已啟動及網路狀態。"
        );
    }

    // 判斷後端是否回傳 JSON
    const contentType =
        response.headers.get("content-type") || "";

    const isJson =
        contentType.includes("application/json") ||
        contentType.includes("+json");

    let payload = null;

    if (isJson) {
        try {
            payload = await response.json();
        } catch (_) {
            if (response.ok) {
                throw new Error(
                    "API 回傳的 JSON 格式無法解析，請檢查後端回應。"
                );
            }
        }
    }

    // 未登入或 Session 失效
    if (response.status === 401) {
        throw new Error(
            "尚未登入或登入狀態已失效，請重新整理頁面並登入。"
        );
    }

    // 可能為權限不足或安全驗證失敗
    if (response.status === 403) {
        throw new Error(
            "存取遭拒（403）：請檢查使用者權限與安全設定。"
        );
    }

    // 避免把重新導向後的 HTML 登入頁當成 JSON
    if (response.redirected && !isJson) {
        throw new Error(
            "API 被重新導向至網頁，可能是登入已失效，請重新登入。"
        );
    }

    // 其他 HTTP 錯誤
    if (!response.ok) {
        const detail =
            payload && typeof payload.message === "string"
                ? payload.message
                : `HTTP ${response.status}`;

        throw new Error(
            `帳戶或交易查詢失敗：${detail}`
        );
    }

    // HTTP 成功但回應格式與預期不符
    if (!isJson || payload === null) {
        throw new Error(
            "API 未回傳預期的 JSON，請檢查瀏覽器 Network 面板。"
        );
    }

    return payload;
}


// 依照 TransactionHistoryPageResponse 的 items / hasNext 分頁取得明細。
async function getEntries(accountId) {
    const entries = [];
    for (let page = 0; page < MAX_PAGES; page++) {
        const data = await fetchJson(API.history(accountId, page, PAGE_SIZE));
        if (!Array.isArray(data.items) || typeof data.hasNext !== "boolean") {
            throw new Error("交易紀錄 API 回傳格式錯誤：缺少 items 或 hasNext。");
        }
        entries.push(...data.items);
        if (!data.hasNext) return { entries, truncated: false };
    }
    return { entries, truncated: true };
}

// 最近 N 天包含今天；例如最近7天為今天與往前6個日曆日。
function cutoffOf(period) {
    if (period === "all") return null;
    const days = Number(period);
    const cutoff = new Date();
    cutoff.setHours(0, 0, 0, 0);
    cutoff.setDate(cutoff.getDate() - (days - 1));
    return cutoff;
}

// 將交易日期轉換為有效的 Date 物件；無法解析時回傳 null。
function toValidDate(value) {
    const date = new Date(value);
    return Number.isNaN(date.getTime()) ? null : date;
}

// 運用 AccountEntry.amountChange 的正負號，而不只看交易名稱。
// 轉帳可能分別留下 WITHDRAWAL 和 DEPOSIT 明細，正負號仍然正確。
function analyzeEntries(entries, period) {
    const cutoff = cutoffOf(period);
    let inflow = 0;
    let outflow = 0;
    const selected = [];
    for (const item of entries) {
        const date = toValidDate(item.createdAt);
        const amount = Number(item.amountChange);
        if (!date || !Number.isFinite(amount)) {
            throw new Error("交易資料缺少有效的 createdAt 或 amountChange，請檢查交易紀錄 DTO。");
        }
        if (cutoff && date < cutoff) continue;
        selected.push(item);
        if (amount > 0) inflow += amount;
        else if (amount < 0) outflow += -amount;
    }
    return { selected, inflow, outflow, net: inflow - outflow, count: selected.length };
}

// 將帳號、幣別、狀態與目前餘額顯示在帳戶資訊區。
function renderAccount(account) {
    byId("accountNumber").textContent = account.accountNumber ?? "—";
    byId("accountCurrency").textContent = account.currency ?? "—";
    byId("accountStatus").textContent = account.status ?? "—";
    const balance = Number(account.balance);
    byId("accountBalance").textContent = Number.isFinite(balance) ? money(balance) : "—";
}

// 顯示期間流入、流出、淨流量與明細筆數。
function renderSummary(stats) {
    byId("totalInflow").textContent = money(stats.inflow);
    byId("totalOutflow").textContent = money(stats.outflow);
    byId("netFlow").textContent = money(stats.net);
    byId("entryCount").textContent = stats.count.toLocaleString("zh-TW");
}

// 把日期轉成使用者瀏覽器的本地日期代碼。
function localDayKey(value) {
    const date = value instanceof Date ? value : toValidDate(value);
    if (!date) return null;
    const y = date.getFullYear();
    const m = String(date.getMonth() + 1).padStart(2, "0");
    const d = String(date.getDate()).padStart(2, "0");
    return `${y}-${m}-${d}`;
}

// 建立帶有文字內容的 DOM 元素；使用 textContent 避免將資料解讀為 HTML。
function appendText(tag, className, textValue, parent) {
    const element = document.createElement(tag);
    element.className = className;
    element.textContent = textValue; // 避免把交易資料當成 HTML 執行
    parent.appendChild(element);
    return element;
}

// 不使用第三方圖表套件，避免 CDN 或版本變更影響系統可執行性。
function renderChart(selected, period) {
    const host = byId("dailyChart");
    host.replaceChildren();
    const dayCount = period === "all" ? 14 : Math.min(Number(period), 14);
    const days = [];
    const totals = new Map();
    const today = new Date();
    today.setHours(0, 0, 0, 0);
    for (let i = dayCount - 1; i >= 0; i--) {
        const date = new Date(today);
        date.setDate(today.getDate() - i);
        const key = localDayKey(date);
        days.push(key);
        totals.set(key, { inflow: 0, outflow: 0 });
    }
    for (const item of selected) {
        const key = localDayKey(item.createdAt);
        if (!totals.has(key)) continue;
        const value = Number(item.amountChange);
        if (value >= 0) totals.get(key).inflow += value;
        else totals.get(key).outflow += -value;
    }
    const maxValue = Math.max(1, ...days.flatMap(day => [
        totals.get(day).inflow, totals.get(day).outflow
    ]));
    for (const day of days) {
        const { inflow, outflow } = totals.get(day);
        const row = appendText("div", "chart-row", "", host);
        appendText("span", "chart-date", day.slice(5), row);
        const bars = appendText("div", "bar-area", "", row);
        for (const [value, type] of [[inflow, "in"], [outflow, "out"]]) {
            const track = appendText("div", "track", "", bars);
            const bar = appendText("div", `bar ${type}`, "", track);
            bar.style.width = `${(value / maxValue) * 100}%`;
        }
        appendText("span", "chart-values",
            `入 ${money(inflow)} / 出 ${money(outflow)}`, row);
    }
}

// 將所選期間的交易明細依異動 ID 排序，顯示最近 20 筆。
function renderHistory(selected) {
    const tbody = byId("historyBody");
    tbody.replaceChildren();
    // 交易紀錄查詢依 AccountEntry.id DESC 排序；這裡再排序確保顯示最近明細。
    const latest = [...selected].sort((a, b) => Number(b.entryId) - Number(a.entryId)).slice(0, 20);
    if (latest.length === 0) {
        const row = document.createElement("tr");
        const cell = document.createElement("td");
        cell.colSpan = 5;
        cell.textContent = "所選期間沒有交易明細。";
        row.appendChild(cell);
        tbody.appendChild(row);
        return;
    }
    for (const entry of latest) {
        const row = document.createElement("tr");
        const date = toValidDate(entry.createdAt);
        const amount = Number(entry.amountChange);
        const cells = [
            date ? date.toLocaleString("zh-TW") : "—",
            entry.transactionNumber ?? "—",
            entry.transactionType ?? "—",
            `${amount >= 0 ? "+" : "−"}${money(Math.abs(amount))}`,
            money(Number(entry.balanceAfter))
        ];
        cells.forEach((value, index) => {
            const cell = document.createElement("td");
            cell.textContent = value;
            if (index === 3) cell.className = amount >= 0 ? "money-in" : "money-out";
            row.appendChild(cell);
        });
        tbody.appendChild(row);
    }
}

// 由帳戶管理頁帶入帳戶ID
const selectedId =
  new URLSearchParams(location.search)
    .get("accountId");

if (
  selectedId &&
  /^[1-9]\d*$/.test(selectedId)
) {

  byId("accountId").value = selectedId;
}

byId("queryForm").addEventListener("submit", async (event) => {
    event.preventDefault();
    const rawId = byId("accountId").value.trim();
    const accountId = Number(rawId);
    if (!/^\d+$/.test(rawId) || !Number.isSafeInteger(accountId) || accountId <= 0) {
        setStatus("帳戶 ID 必須是大於 0 的整數。", "error");
        return;
    }
    const period = byId("period").value;
    const button = byId("loadButton");
    button.disabled = true;
    setStatus("正在查詢帳戶與交易明細……");
    try {
        // 先驗證帳戶存在，再以分頁取得所有可用異動紀錄。
        const account = await fetchJson(API.account(accountId));
        const { entries, truncated } = await getEntries(accountId);
        const stats = analyzeEntries(entries, period);
        renderAccount(account);
        renderSummary(stats);
        renderChart(stats.selected, period);
        renderHistory(stats.selected);
        if (truncated) {
            setStatus(`已讀取最近 ${entries.length} 筆明細，超過讀取上限；統計為部分結果，並非全帳戶總量。`, "warning");
        } else {
            setStatus(`查詢完成：共讀取 ${entries.length} 筆明細，所選期間計入 ${stats.count} 筆。`);
        }
    } catch (error) {
        setStatus(error instanceof Error ? error.message : "查詢發生未知錯誤。", "error");
    } finally {
        button.disabled = false;
    }
});
