/* management：銀行管理介面的前端互動、資料驗證與 API 呼叫。 */
"use strict";

/**
 * 管理介面：新增客戶、開立帳戶與查詢帳戶前端。
 * 透過客戶與帳戶管理 API 執行新增及查詢操作。
 * API 請求使用從網站根路徑開始的 /api/* 相對路徑。
 */

/** 取得DOM元素（如果ID拼錯，直接顯示錯誤以利除錯）。 */
function element(id) {
  const node = document.getElementById(id);
  if (!node) throw new Error(`找不到HTML元素：${id}`);
  return node;
}

/** 顯示成功或錯誤，不使用innerHTML，避免顯示惡意HTML。 */
function showMessage(id, message, success) {
  const target = element(id);
  target.textContent = message;
  target.className = `feedback ${success ? "success" : "error"}`;
  target.hidden = false;
}

/** 將伺服器回傳的欄位安全顯示為列表。 */
function showResult(id, title, data) {
  const target = element(id);
  target.replaceChildren();
  const heading = document.createElement("h3");
  heading.textContent = title;
  const list = document.createElement("dl");
  for (const [key, value] of Object.entries(data)) {
    const label = document.createElement("dt");
    const content = document.createElement("dd");
    label.textContent = key;
    content.textContent = value === null || value === undefined
      ? "（無）"
      : typeof value === "object" ? JSON.stringify(value) : String(value);
    list.append(label, content);
  }
  target.append(heading, list);
  target.hidden = false;
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
 * 傳送客戶與帳戶管理 API 請求並解析 JSON 回應。
 * 修改資料時附加 CSRF Token；失敗時回傳後端驗證訊息供表單顯示。
 */
async function callApi(url, options = {}) {
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

  const rawText = await response.text();
  let data = {};
  if (rawText) {
    try {
      data = JSON.parse(rawText);
    } catch {
      data = { message: rawText.slice(0, 250) };
    }
  }

  if (!response.ok) {
    const details = [];
    if (typeof data.message === "string") details.push(data.message);
    if (typeof data.detail === "string") details.push(data.detail);
    if (data.errors && typeof data.errors === "object") {
      details.push(JSON.stringify(data.errors));
    }
    const reason = details.filter(Boolean).join("；") || response.statusText;
    throw new Error(`HTTP ${response.status}：${reason || "請求失敗"}`);
  }
  return data;
}

/** 將使用者輸入的ID轉換成Java Long可接受的正整數。 */
function requiredPositiveId(raw) {
  const value = raw.trim();
  if (!/^\d+$/.test(value) || !Number.isSafeInteger(Number(value)) || Number(value) <= 0) {
    throw new Error("ID 必須是大於 0 的整數");
  }
  return Number(value);
}

/** 建立客戶：POST /api/customers */
element("customer-form").addEventListener("submit", async (event) => {
  event.preventDefault();
  const button = element("create-customer-btn");
  const customerCode = element("customer-code").value.trim();
  const name = element("customer-name").value.trim();
  const email = element("customer-email").value.trim();

  if (!customerCode || !name) {
    showMessage("customer-status", "客戶編號及姓名不得為空白。", false);
    return;
  }

  button.disabled = true;
  element("customer-result").hidden = true;
  try {
    // 依 CreateCustomerRequest 的欄位格式組合新增客戶請求。
    const created = await callApi("/api/customers", {
      method: "POST",
      body: JSON.stringify({ customerCode, name, email })
    });
    showMessage("customer-status", "客戶建立成功！", true);
    showResult("customer-result", "已建立的客戶", created);

    // 後端回傳id時，自動帶入開戶表單。
    if (created.id !== undefined && created.id !== null) {
      element("account-customer-id").value = String(created.id);
      element("account-customer-id").focus();
    }
  } catch (error) {
    showMessage("customer-status", `建立客戶失敗：${error.message}`, false);
  } finally {
    button.disabled = false;
  }
});

/** 開立帳戶：POST /api/accounts */
element("account-form").addEventListener("submit", async (event) => {
  event.preventDefault();
  const button = element("create-account-btn");
  let customerId;
  try {
    customerId = requiredPositiveId(element("account-customer-id").value);
  } catch (error) {
    showMessage("account-status", error.message, false);
    return;
  }
  const accountNumber = element("account-number").value.trim();
  if (!accountNumber) {
    showMessage("account-status", "銀行帳號不得為空白。", false);
    return;
  }

  button.disabled = true;
  element("account-result").hidden = true;
  try {
    // CreateAccountRequest 僅包含customerId與accountNumber。
    // 餘額及幣別由後端設定，前端不自行填寫。
    const created = await callApi("/api/accounts", {
      method: "POST",
      body: JSON.stringify({ customerId, accountNumber })
    });
    showMessage("account-status", "銀行帳戶開立成功！", true);
    showResult("account-result", "已建立的銀行帳戶", created);

    // 自動帶入查詢區，可立即驗證剛建立的帳戶。
    if (created.id !== undefined && created.id !== null) {
      element("search-account-id").value = String(created.id);
    }
  } catch (error) {
    showMessage("account-status", `開戶失敗：${error.message}`, false);
  } finally {
    button.disabled = false;
  }
});

/** 帳戶查詢：GET /api/accounts/{id} */
element("search-form").addEventListener("submit", async (event) => {
  event.preventDefault();
  const button = element("search-btn");
  let accountId;
  try {
    accountId = requiredPositiveId(element("search-account-id").value);
  } catch (error) {
    showMessage("search-status", error.message, false);
    return;
  }

  button.disabled = true;
  element("search-result").hidden = true;
  try {
    const account = await callApi(`/api/accounts/${accountId}`);
    showMessage("search-status", "帳戶查詢成功。", true);
    showResult("search-result", "帳戶詳細資料", account);
  } catch (error) {
    showMessage("search-status", `查詢失敗：${error.message}`, false);
  } finally {
    button.disabled = false;
  }
});
