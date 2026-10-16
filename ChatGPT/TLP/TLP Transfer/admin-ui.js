/* TLP Transfer: token-free GitHub management menu. No credentials or write requests. */
(()=>{"use strict";
const repo="TLP1998/OpenAI",api="https://api.github.com/repos/"+repo;
let popup=null;
const el=(tag,txt,cls)=>{const n=document.createElement(tag);if(txt!==undefined)n.textContent=txt;if(cls)n.className=cls;return n};
const hide=()=>{if(popup){popup.remove();popup=null}};
function notify(message){let n=document.getElementById("tlp-admin-note");if(!n){n=el("div",undefined,"tlp-admin-note");n.id="tlp-admin-note";document.body.append(n)}n.textContent=message;n.style.background="#23567a";n.hidden=false;clearTimeout(notify.timer);notify.timer=setTimeout(()=>n.hidden=true,7500)}
function linkButton(label,url){const b=el("button",label,"tlp-admin-context-action");b.type="button";b.onclick=()=>{hide();window.open(url,"_blank","noopener,noreferrer")};return b}
function menu(e,object){hide();popup=el("div",undefined,"tlp-admin-context");popup.setAttribute("role","menu");
 popup.append(el("div",object.kind==="reply"?"回复管理":"消息管理","tlp-admin-context-title"));
 const issueUrl="https://github.com/"+repo+"/issues/"+object.issue;
 if(object.kind==="reply"){
  popup.append(linkButton("在 GitHub 删除这条回复 ↗",issueUrl+"#issuecomment-"+object.commentId));
 }else{
  popup.append(linkButton("在 GitHub 删除整个 Issue ↗",issueUrl));
  popup.append(linkButton("打开消息 HTML 文件 ↗","https://github.com/"+repo+"/blob/main/ChatGPT/TLP/TLP%20Transfer/"+object.message.page.split("/").map(encodeURIComponent).join("/")));
  popup.append(linkButton("编辑中转站索引 ↗","https://github.com/"+repo+"/blob/main/ChatGPT/TLP/TLP%20Transfer/index.html"));
 }
 const copy=el("button","复制同步清理指令","tlp-admin-context-action");copy.type="button";copy.onclick=async()=>{
  const m=object.message;const txt=object.kind==="reply"?
   "请在 GitHub Issue #"+object.issue+" 删除评论 #issuecomment-"+object.commentId+"；保留其他回复与原消息。":
   "请同步清理 TLP Transfer 消息 "+m.id+"：在 GitHub 删除 Issue #"+m.issueNumber+"，从 index.html 消息数组移除对应条目，删除 "+m.page+"；附件仅在确认专属后删除。";
  try{await navigator.clipboard.writeText(txt);notify("已复制清理指令")}catch(e){notify("复制失败，请使用 GitHub 管理入口")}
  hide()
 };popup.append(copy);
 const cancel=el("button","取消","tlp-admin-context-action");cancel.type="button";cancel.onclick=hide;popup.append(cancel);
 document.body.append(popup);popup.style.left=Math.max(8,Math.min(e.clientX||12,window.innerWidth-238))+"px";popup.style.top=Math.max(8,Math.min(e.clientY||12,window.innerHeight-190))+"px";
}
function bind(node,object){
 if(node.dataset.tlpAdminBound)return;node.dataset.tlpAdminBound="1";
 node.addEventListener("contextmenu",e=>{e.preventDefault();menu(e,object)});
 let timer=null,start=null;
 node.addEventListener("touchstart",e=>{if(e.touches.length!==1)return;const t=e.touches[0];start={x:t.clientX,y:t.clientY};timer=setTimeout(()=>{timer=null;menu(start,object)},650)},{passive:true});
 node.addEventListener("touchmove",e=>{if(!start||!e.touches.length)return;const t=e.touches[0];if(Math.abs(t.clientX-start.x)>12||Math.abs(t.clientY-start.y)>12){clearTimeout(timer);timer=null}},{passive:true});
 for(const event of ["touchend","touchcancel"])node.addEventListener(event,()=>{clearTimeout(timer);timer=null},{passive:true});
}
function start({messages,showList}){
 const list=document.getElementById("list"),detail=document.getElementById("detail");
 function apply(){
  list.querySelectorAll("[data-tlp-message]").forEach(n=>{const m=messages.find(x=>x.id===n.dataset.tlpMessage);if(m)bind(n,{kind:"message",issue:m.issueNumber,message:m})});
  detail.querySelectorAll("[data-tlp-comment]").forEach(n=>{const issue=Number(n.dataset.tlpIssue),commentId=Number(n.dataset.tlpComment);if(commentId)bind(n,{kind:"reply",issue,commentId})})
 }
 const obs=new MutationObserver(apply);obs.observe(list,{subtree:true,childList:true});obs.observe(detail,{subtree:true,childList:true});apply();
 document.addEventListener("click",e=>{if(popup&&!popup.contains(e.target))hide()});document.addEventListener("keydown",e=>{if(e.key==="Escape")hide()});
 // Hide only on a definitive deleted-Issue response. Do not treat 403/network failures as deletion.
 (async()=>{const deleted=[];for(const m of [...messages]){try{const r=await fetch(api+"/issues/"+m.issueNumber,{headers:{Accept:"application/vnd.github+json"},cache:"no-store"});if(r.status===410||r.status===404)deleted.push(m.id)}catch(e){}}
 if(deleted.length){for(const id of deleted){const index=messages.findIndex(m=>m.id===id);if(index>=0)messages.splice(index,1)}showList();notify(deleted.length+" 条已删除 Issue 对应的消息已从当前收件箱隐藏（静态文件未删除）。")}})();
}
window.TLPAdmin={start};
})();
