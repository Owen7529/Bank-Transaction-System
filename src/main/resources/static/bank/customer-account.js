/* customer-account：銀行管理介面的前端互動、資料驗證與 API 呼叫。 */
"use strict";
/** 客戶與帳戶頁共用表單驗證、API 呼叫與結果顯示邏輯。 */
(() => {
  // 依 HTML id 取得 DOM 元素，減少重複查詢程式碼。
  const byId = (id) => document.getElementById(id);

  // 在指定區塊顯示操作成功或失敗的提示訊息。
  function showMessage(id, message, success) {
    const target = byId(id);
    if (!target) return;
    target.textContent = message;
    target.className = `feedback ${success ? "success" : "error"}`;
    target.hidden = false;
  }

  // 將 API 的回傳物件轉換成畫面可閱讀的欄位。
  function showResult(id, headingText, data) {
    const box = byId(id);
    if (!box) return;
    box.replaceChildren();
    const heading = document.createElement("h3");
    heading.textContent = headingText;
    const dl = document.createElement("dl");
    for (const [key, rawValue] of Object.entries(data || {})) {
      const dt = document.createElement("dt");
      dt.textContent = key;
      const dd = document.createElement("dd");
      dd.textContent = rawValue == null ? "（無）" : typeof rawValue === "object" ? JSON.stringify(rawValue) : String(rawValue);
      dl.append(dt, dd);
    }
    box.append(heading, dl);
    box.hidden = false;
  }

  // 統一封裝前端 API 請求、驗證錯誤回應與認證資訊。
  async function callApi(path, options = {}) {
    const response = await fetch(path, {
      ...options,
      headers: {
        Accept: "application/json",
        ...(options.body ? { "Content-Type": "application/json" } : {}),
        ...(options.headers || {})
      }
    });
    const raw = await response.text();
    let result = {};
    if (raw) {
      try { result = JSON.parse(raw); }
      catch { result = { message: raw.slice(0, 200) }; }
    }
    if (!response.ok) {
      const pieces = [];
      if (typeof result.message === "string") pieces.push(result.message);
      if (typeof result.detail === "string") pieces.push(result.detail);
	  // 顯示後端DTO欄位驗證錯誤
	  if (
	    result.fieldErrors &&
	    typeof result.fieldErrors === "object"
	  ) {

	    const details = Object.entries(result.fieldErrors)
	      .map(([field, error]) => `${field}：${error}`)
	      .join("；");

	    pieces.push(details);
	  }
      throw new Error(`HTTP ${response.status}：${pieces.join("；") || response.statusText || "請求失敗"}`);
    }
    return result;
  }

  // 將輸入值轉成正整數 ID；格式錯誤時提供明確錯誤訊息。
  function positiveId(input) {
    const value = String(input).trim();
    if (!/^\d+$/.test(value) || !Number.isSafeInteger(Number(value)) || Number(value) <= 0) {
      throw new Error("ID 必須是大於 0 的整數");
    }
    return Number(value);
  }
  
  // 設定後續操作連結
  function setAccountLinks(accountId) {

    const id = positiveId(accountId);

    const transactionLink =
      byId("go-transactions");

    const dashboardLink =
      byId("go-dashboard");

    if (transactionLink) {

      transactionLink.href =
        `./transactions.html?accountId=${encodeURIComponent(id)}`;
    }

    if (dashboardLink) {

      dashboardLink.href =
        `./dashboard.html?accountId=${encodeURIComponent(id)}`;
    }
  }

  // 由新增客戶畫面跳轉 /accounts.html?customerId=12 時，帶入客戶ID。
  const accountCustomer = byId("account-customer-id");
  if (accountCustomer) {
    const param = new URLSearchParams(location.search).get("customerId");
    if (param != null) {
      try { accountCustomer.value = String(positiveId(param)); }
      catch { /* 無效網址參數不自動帶入；由使用者自行輸入 */ }
    }
  }

  const customerForm = byId("customer-form");
  if (customerForm) {
    customerForm.addEventListener("submit", async (event) => {
      event.preventDefault();
      const button = byId("create-customer-btn");
      const next = byId("go-create-account");
      if (next) next.hidden = true;
      const customerCode = byId("customer-code").value.trim();
      const name = byId("customer-name").value.trim();
      const email = byId("customer-email").value.trim();
      if (!customerCode || !name) { showMessage("customer-status", "客戶編號與姓名不可空白。", false); return; }
      button.disabled = true;
      byId("customer-result").hidden = true;
      try {
        const data = await callApi("/api/customers", {
          method: "POST",
          body: JSON.stringify({ customerCode, name, email })
        });
        showResult("customer-result", "建立成功的客戶資料", data);
        showMessage("customer-status", "客戶建立成功。", true);
        if (next && data.id != null) {
          const id = positiveId(String(data.id));
          next.href = `./accounts.html?customerId=${encodeURIComponent(id)}`;
          next.hidden = false;
        }
      } catch (error) {
        showMessage("customer-status", `建立客戶失敗：${error.message}`, false);
      } finally { button.disabled = false; }
    });
  }

  const accountForm = byId("account-form");
  if (accountForm) {
    accountForm.addEventListener("submit", async (event) => {
      event.preventDefault();
      const button = byId("create-account-btn");
      let customerId;
      try { customerId = positiveId(accountCustomer.value); }
      catch (error) { showMessage("account-status", error.message, false); return; }
      const accountNumber = byId("account-number").value.trim();
      if (!accountNumber) { showMessage("account-status", "銀行帳號不可空白。", false); return; }
      button.disabled = true;
      byId("account-result").hidden = true;
	  try {

	    // 呼叫後端API建立銀行帳戶
	    const data = await callApi("/api/accounts", {
	      method: "POST",
	      body: JSON.stringify({
	        customerId,
	        accountNumber
	      })
	    });

	    // 檢查後端是否回傳帳戶ID
	    if (data.id == null) {
	      throw new Error(
	        "伺服器未回傳帳戶ID，請先查詢是否已建立，勿直接重複送出。"
	      );
	    }

	    const accountId = positiveId(String(data.id));

	    // 將帳戶ID帶入查詢框
	    byId("search-account-id").value =
	      String(accountId);

	    // 更新交易及儀表板連結
	    setAccountLinks(accountId);

	    // 顯示成功結果
	    showResult(
	      "account-result",
	      "開戶結果",
	      data
	    );

	    showMessage(
	      "account-status",
	      `銀行帳戶開立成功！帳戶ID：${accountId}`,
	      true
	    );

	  } catch (error) {

	    // 顯示錯誤訊息
	    showMessage(
	      "account-status",
	      `開戶未確認成功：${error.message}`,
	      false
	    );

	  } finally {
	    // 不論成功或失敗，恢復按鈕
	    button.disabled = false;
	  }
    });
  }

  const searchForm = byId("search-form");
  if (searchForm) {
    searchForm.addEventListener("submit", async (event) => {
      event.preventDefault();
      const button = byId("search-btn");
      let id;
      try { id = positiveId(byId("search-account-id").value); }
      catch (error) { showMessage("search-status", error.message, false); return; }
      button.disabled = true;
      byId("search-result").hidden = true;
      try {
        const account = await callApi(`/api/accounts/${id}`);
		setAccountLinks(account.id);
        showResult("search-result", "帳戶詳細資料", account);
        showMessage("search-status", "帳戶查詢成功。", true);
      } catch (error) {
        showMessage("search-status", `查詢失敗：${error.message}`, false);
      } finally { button.disabled = false; }
    });
  }
})();
