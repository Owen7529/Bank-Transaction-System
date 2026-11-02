/* app：銀行管理介面的前端互動、資料驗證與 API 呼叫。 */
"use strict";

// =======================================
// 1. 後端 API 路徑
// =======================================

const API = {
  account: (id) =>
    `/api/accounts/${encodeURIComponent(id)}`,

  transactions: (id) =>
    `/api/accounts/${encodeURIComponent(id)}/transactions?page=0&size=20`,

  deposit: "/api/transactions/deposits",

  withdrawal: "/api/transactions/withdrawals",

  transfer: "/api/transfers"
};

// 取得 HTML 元素
const el = (id) => document.getElementById(id);

// 目前選取的帳戶
let selectedAccountId = null;

// 保存尚未確認完成交易的 requestKey
let pendingTransaction = null;
let pendingTransfer = null;

// =======================================
// 2. 共用工具方法
// =======================================

// 顯示成功或錯誤訊息
function message(id, text, type = "") {
  const target = el(id);
  target.textContent = text;
  target.className = `message ${type}`;
}

/**
 * 取得目前登入工作階段的 CSRF Token。
 * Spring Security 使用此 Token 驗證修改資料的 HTTP 請求。
 */
async function csrfHeaders() {
  const response = await fetch("/api/csrf", {
    method: "GET",
    credentials: "same-origin",
    cache: "no-store",
    headers: { Accept: "application/json" }
  });

  if (!response.ok) {
    throw new Error(`無法取得 CSRF Token（HTTP ${response.status}）`);
  }

  const csrf = await response.json();
  if (!csrf.headerName || !csrf.token) {
    throw new Error("伺服器未提供有效的 CSRF Token");
  }

  return { [csrf.headerName]: csrf.token };
}

/**
 * 傳送銀行交易 API 請求並解析 JSON 回應。
 * 寫入操作附加 CSRF Token；錯誤回應保留欄位驗證資訊供畫面顯示。
 */
async function requestJson(url, options = {}) {
  const method = (options.method || "GET").toUpperCase();
  const writeRequest = !["GET", "HEAD", "OPTIONS", "TRACE"].includes(method);
  const headers = {
    Accept: "application/json",
    ...(options.body ? { "Content-Type": "application/json" } : {}),
    ...(options.headers || {})
  };
  if (writeRequest) {
    Object.assign(headers, await csrfHeaders());
  }

  const response = await fetch(url, {
    ...options,
    credentials: "same-origin",
    headers
  });

  const raw = await response.text();

  let result = {};

  if (raw) {
    try {
      result = JSON.parse(raw);
    } catch {
      throw new Error(
        `伺服器未回傳有效 JSON（HTTP ${response.status}）`
      );
    }
  }

  if (!response.ok) {

    // 對應現有 ApiErrorResponse.fieldErrors
    const fields = result.fieldErrors || {};

    const details = Object.entries(fields)
      .map(([name, detail]) => `${name}：${detail}`)
      .join("；");

    throw new Error(
      [result.message || `HTTP ${response.status}`, details]
        .filter(Boolean)
        .join("；")
    );
  }

  return result;
}

// 檢查帳戶 ID
function positiveId(value) {

  const text = String(value).trim();
  const number = Number(text);

  if (
    !/^[1-9]\d*$/.test(text) ||
    !Number.isSafeInteger(number)
  ) {
    throw new Error("帳戶 ID 必須是正整數");
  }

  return number;
}

// 檢查金額
// 使用字串傳給 Java BigDecimal，避免JS浮點換算誤差
function amountText(value) {

  const text = String(value).trim();

  if (
    !/^(?:0|[1-9]\d{0,16})(?:\.\d{1,2})?$/.test(text) ||
    Number(text) <= 0
  ) {
    throw new Error(
      "金額須大於0，且最多17位整數與2位小數"
    );
  }

  return text;
}

// 產生並保留交易請求識別碼
// 同一筆未確認成功的請求重試時使用相同key
function requestIdentity(previous, contents) {

  const fingerprint = JSON.stringify(contents);

  if (
    previous &&
    previous.fingerprint === fingerprint
  ) {
    return previous;
  }

  return {
    fingerprint,
    key: `WEB-${crypto.randomUUID()}`
  };
}

// =======================================
// 3. 帳戶查詢
// =======================================

// 將帳戶資料顯示在網頁
function renderAccount(account) {

  el("account-number").textContent =
    account.accountNumber ?? "—";

  el("currency").textContent =
    account.currency ?? "—";

  el("account-status").textContent =
    account.status ?? "—";

  el("balance").textContent =
    `${account.currency || "TWD"} ${account.balance ?? "—"}`;

  el("account-summary").hidden = false;
}

// 重新讀取帳戶資料
async function reloadAccount(id) {

  const account = await requestJson(API.account(id));

  renderAccount(account);
}

// 查詢條件變動時，取消原本的選取狀態
el("account-id").addEventListener("input", () => {

  selectedAccountId = null;

  el("refresh-button").disabled = true;
  el("account-summary").hidden = true;
  el("history-body").replaceChildren();
});

// 從帳戶管理頁進入時，自動帶入帳戶ID
const queryId =
  new URLSearchParams(location.search).get("accountId");

if (queryId && /^[1-9]\d*$/.test(queryId)) {
  el("account-id").value = queryId;
}

// 查詢帳戶按鈕
el("account-form").addEventListener(
  "submit",
  async (event) => {

    event.preventDefault();

    const button = el("search-button");
    button.disabled = true;

    try {

      const id = positiveId(el("account-id").value);

      await reloadAccount(id);

      selectedAccountId = id;

      el("refresh-button").disabled = false;

      message(
        "account-message",
        "帳戶查詢成功",
        "success"
      );

      await reloadHistory(id);

    } catch (error) {

      message(
        "account-message",
        error.message,
        "error"
      );

    } finally {
      button.disabled = false;
    }
  }
);

// =======================================
// 4. 交易歷史查詢
// =======================================

// 重新查詢指定帳戶的交易紀錄，並更新明細表格。
async function reloadHistory(id) {

  const data =
    await requestJson(API.transactions(id));

  if (!Array.isArray(data.items)) {
    throw new Error(
      "交易紀錄回應缺少 items 陣列"
    );
  }

  const tbody = el("history-body");

  tbody.replaceChildren();

  // 沒有交易紀錄
  if (data.items.length === 0) {

    const row = document.createElement("tr");
    const cell = document.createElement("td");

    cell.colSpan = 5;
    cell.textContent = "目前沒有交易紀錄";

    row.append(cell);
    tbody.append(row);
  }

  // 逐筆建立表格內容
  for (const item of data.items) {

    const row = document.createElement("tr");

    const values = [
      item.transactionNumber,
      item.transactionType,
      item.amountChange,
      item.balanceAfter,
      item.createdAt
    ];

    for (const value of values) {

      const cell = document.createElement("td");

      cell.textContent =
        value == null ? "—" : String(value);

      row.append(cell);
    }

    tbody.append(row);
  }

  message(
    "history-message",
    `已載入最近 ${data.items.length} 筆交易紀錄`,
    "success"
  );
}

// 重新整理按鈕
el("refresh-button").addEventListener(
  "click",
  async () => {

    if (selectedAccountId == null) return;

    try {

      await reloadAccount(selectedAccountId);

      await reloadHistory(selectedAccountId);

    } catch (error) {

      message(
        "history-message",
        error.message,
        "error"
      );
    }
  }
);

// =======================================
// 5. 存款與提款
// =======================================

el("transaction-form").addEventListener(
  "submit",
  async (event) => {

    event.preventDefault();

    const button = el("submit-transaction");

    try {

      if (selectedAccountId == null) {
        throw new Error("請先查詢帳戶");
      }

      // 取得選擇的交易類型
      const type = document.querySelector(
        'input[name="type"]:checked'
      ).value;

      const amount = amountText(
        el("amount").value
      );

      // 對應目前 WithdrawalService 的限制
      if (
        type === "WITHDRAWAL" &&
        Number(amount) > 50000
      ) {
        throw new Error(
          "單筆提款不可超過 50,000 元"
        );
      }

      const contents = {
        type,
        accountId: selectedAccountId,
        amount,
        description: el("description").value.trim()
      };

      pendingTransaction = requestIdentity(
        pendingTransaction,
        contents
      );

      button.disabled = true;

      const url =
        type === "DEPOSIT"
          ? API.deposit
          : API.withdrawal;

      // 執行存款或提款
      const result = await requestJson(url, {

        method: "POST",

        body: JSON.stringify({
          requestKey: pendingTransaction.key,
          accountId: contents.accountId,
          amount: contents.amount,
          description: contents.description
        })
      });

      // 成功才清除requestKey
      pendingTransaction = null;

      el("amount").value = "";

      message(
        "transaction-message",
        `交易成功！編號：${result.transactionNumber || "—"}；餘額：${result.balanceAfter ?? "—"}`,
        "success"
      );

      // 交易成功後更新帳戶和明細
      try {

        await reloadAccount(selectedAccountId);
        await reloadHistory(selectedAccountId);

      } catch (error) {

        message(
          "history-message",
          `交易已成功，但資料重新整理失敗：${error.message}`,
          "error"
        );
      }

    } catch (error) {

      message(
        "transaction-message",
        `交易未確認成功：${error.message}。如需重試，請保留相同資料。`,
        "error"
      );

    } finally {

      button.disabled = false;
    }
  }
);

// =======================================
// 6. 帳戶間轉帳
// =======================================

el("transfer-form").addEventListener(
  "submit",
  async (event) => {

    event.preventDefault();

    const button = el("transfer-button");

    try {

      const fromAccountId = positiveId(
        el("transfer-from-id").value
      );

      const toAccountId = positiveId(
        el("transfer-to-id").value
      );

      const amount = amountText(
        el("transfer-amount").value
      );

      if (fromAccountId === toAccountId) {
        throw new Error(
          "來源與目標帳戶不可相同"
        );
      }

      // 現有轉帳會呼叫提款服務
      if (Number(amount) > 50000) {
        throw new Error(
          "依目前提款規則，單筆轉帳不可超過50,000元"
        );
      }

      const contents = {
        fromAccountId,
        toAccountId,
        amount
      };

      pendingTransfer = requestIdentity(
        pendingTransfer,
        contents
      );

      // 轉帳前再次確認
      const confirmed = window.confirm(
        `確定從帳戶 ${fromAccountId} 轉出 ${amount} 元到帳戶 ${toAccountId}？`
      );

      if (!confirmed) return;

      button.disabled = true;

      const result = await requestJson(
        API.transfer,
        {
          method: "POST",
          body: JSON.stringify({
            requestKey: pendingTransfer.key,
            ...contents
          })
        }
      );

      pendingTransfer = null;

      el("transfer-amount").value = "";

      message(
        "transfer-message",
        `轉帳成功，狀態：${result.status || "已完成"}`,
        "success"
      );

      // 如果正在查看其中一個相關帳戶
      // 就更新帳戶餘額和交易明細
      if (
        selectedAccountId === fromAccountId ||
        selectedAccountId === toAccountId
      ) {

        try {

          await reloadAccount(selectedAccountId);
          await reloadHistory(selectedAccountId);

        } catch (error) {

          message(
            "history-message",
            `轉帳完成，但資料更新失敗：${error.message}`,
            "error"
          );
        }
      }

    } catch (error) {

      message(
        "transfer-message",
        `轉帳未確認成功：${error.message}。如需重試，請保留相同資料。`,
        "error"
      );

    } finally {

      button.disabled = false;
    }
  }
);
