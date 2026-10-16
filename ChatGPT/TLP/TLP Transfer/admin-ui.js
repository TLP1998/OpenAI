/* TLP Transfer: token-free GitHub management menu. No credentials or write requests. */
(()=>{"use strict";
const repo="TLP1998/OpenAI",api="https://api.github.com/repos/"+repo;
let popup=null, longPressUntil=0;
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
 // Attach invisibly, measure its real size, then reveal at a single stable position.
 // Prefer below the finger/cursor; flip above when close to the viewport bottom.
 popup.style.visibility="hidden";
 popup.style.left="0px";popup.style.top="0px";
 document.body.append(popup);
 const rect=popup.getBoundingClientRect();
 const viewport=window.visualViewport;
 const leftEdge=viewport?viewport.offsetLeft:0, topEdge=viewport?viewport.offsetTop:0;
 const width=viewport?viewport.width:window.innerWidth, height=viewport?viewport.height:window.innerHeight;
 const gap=8, inset=10;
 const x=Number.isFinite(e.clientX)?e.clientX:leftEdge+inset;
 const y=Number.isFinite(e.clientY)?e.clientY:topEdge+inset;
 const newX=Math.max(leftEdge+inset,Math.min(x,leftEdge+width-rect.width-inset));
 const below=y+gap;
 const newY=below+rect.height+inset<=topEdge+height
   ? below : Math.max(topEdge+inset,y-rect.height-gap);
 popup.style.left=Math.round(newX)+"px";
 popup.style.top=Math.round(newY)+"px";
 popup.style.visibility="visible";
}
function bind(node,object){
 if(node.dataset.tlpAdminBound)return;node.dataset.tlpAdminBound="1";
 node.addEventListener("contextmenu",e=>{
   e.preventDefault();
   // Some mobile browsers fire a native contextmenu after our touch timer.
   if(Date.now()<longPressUntil)return;
   menu({clientX:e.clientX,clientY:e.clientY},object);
 });
 let timer=null,start=null,triggered=false;
 function clear(){if(timer!==null)clearTimeout(timer);timer=null}
 node.addEventListener("touchstart",e=>{
   clear();triggered=false;
   if(e.touches.length!==1)return;
   const t=e.touches[0];start={x:t.clientX,y:t.clientY};
   timer=setTimeout(()=>{
     timer=null;triggered=true;longPressUntil=Date.now()+1000;
     menu({clientX:start.x,clientY:start.y},object);
   },650);
 },{passive:true});
 node.addEventListener("touchmove",e=>{
   if(!start||!e.touches.length)return;
   const t=e.touches[0];
   if(Math.abs(t.clientX-start.x)>10||Math.abs(t.clientY-start.y)>10)clear();
 },{passive:true});
 node.addEventListener("touchend",clear,{passive:true});
 node.addEventListener("touchcancel",clear,{passive:true});
 // Do not activate the underlying message after the long-press menu appears.
 node.addEventListener("click",e=>{
   if(triggered&&Date.now()<longPressUntil){e.preventDefault();e.stopPropagation();triggered=false}
 },true);
}

function start({messages,showList}){
 const list=document.getElementById("list"),detail=document.getElementById("detail");
 function apply(){
  list.querySelectorAll("[data-tlp-message]").forEach(n=>{const m=messages.find(x=>x.id===n.dataset.tlpMessage);if(m)bind(n,{kind:"message",issue:m.issueNumber,message:m})});
  detail.querySelectorAll("[data-tlp-comment]").forEach(n=>{const issue=Number(n.dataset.tlpIssue),commentId=Number(n.dataset.tlpComment);if(commentId)bind(n,{kind:"reply",issue,commentId})})
 }
 const obs=new MutationObserver(apply);obs.observe(list,{subtree:true,childList:true});obs.observe(detail,{subtree:true,childList:true});apply();
 document.addEventListener("click",e=>{if(popup&&!popup.contains(e.target))hide()});document.addEventListener("keydown",e=>{if(e.key==="Escape")hide()});
 // Recheck when returning from the GitHub tab as well as on first load.
 // Only 404/410 confirms deletion. Network failures and rate limits never delete.
 let lastCheck=0, checking=false;
 async function sync(force=false){
   if(checking||(!force&&Date.now()-lastCheck<20000))return;
   checking=true;lastCheck=Date.now();const removed=[];
   try{
     for(const m of [...messages]){
       try{
         const r=await fetch(api+"/issues/"+m.issueNumber+"?tlp_sync="+Date.now(),{
           headers:{Accept:"application/vnd.github+json"},cache:"no-store"
         });
         if(r.status===404||r.status===410)removed.push(m.id);
       }catch(e){/* Keep messages if connection to GitHub API fails. */}
     }
     if(removed.length){
       for(const id of removed){const index=messages.findIndex(m=>m.id===id);if(index>=0)messages.splice(index,1)}
       showList();notify(removed.length+" 条已删除 Issue 对应的消息已同步隐藏。静态文件将由 GitHub Actions 清理。");
     }else if(force){notify("检查完成：未发现新删除的 GitHub Issue。")}
   }finally{checking=false}
 }
 window.TLPAdmin.sync=()=>sync(true);
 window.addEventListener("focus",()=>sync());
 window.addEventListener("pageshow",()=>sync());
 document.addEventListener("visibilitychange",()=>{if(document.visibilityState==="visible")sync()});
 sync(true);
}
window.TLPAdmin={start};
})();
