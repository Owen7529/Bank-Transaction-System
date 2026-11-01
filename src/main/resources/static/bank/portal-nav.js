/* portal-nav：銀行管理介面的前端互動、資料驗證與 API 呼叫。 */
"use strict";
/** 建立銀行系統的共用導覽列，並標示目前所在的頁面。 */
(() => {
  const links = [
    ["index.html", "系統首頁"],
    ["customers.html", "客戶管理"],
    ["accounts.html", "帳戶管理"],
    ["transactions.html", "交易作業／紀錄"],
    ["dashboard.html", "資金儀表板"]
  ];
  const current = location.pathname.split("/").pop() || "index.html";
  const header = document.createElement("div");
  header.className = "portal-nav";
  const inner = document.createElement("div");
  inner.className = "portal-nav-inner";
  const brand = document.createElement("a");
  brand.href = "./index.html";
  brand.className = "portal-nav-brand";
  brand.setAttribute("aria-label", "返回銀行系統首頁");
  const mark = document.createElement("span");
  mark.className = "portal-nav-mark";
  mark.textContent = "B";
  const brandText = document.createElement("span");
  brandText.className = "portal-nav-brand-text";
  const en = document.createElement("strong"); en.textContent = "BANK PORTAL";
  const zh = document.createElement("small"); zh.textContent = "銀行帳戶交易管理系統";
  brandText.append(en, zh);
  brand.append(mark, brandText);
  const nav = document.createElement("nav");
  nav.className = "portal-nav-links";
  nav.setAttribute("aria-label", "主要功能導覽");
  links.forEach(([page, label]) => {
    const a = document.createElement("a");
    a.href = `./${page}`;
    a.textContent = label;
    if (current === page || (current === "management.html" && page === "customers.html")) {
      a.setAttribute("aria-current", "page");
    }
    nav.append(a);
  });
  inner.append(brand, nav);
  header.append(inner);
  document.body.classList.add("portal-nav-active");
  document.body.prepend(header);
})();
