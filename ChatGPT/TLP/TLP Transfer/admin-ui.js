/* TLP Transfer admin: context menu + GitHub API writes with in-memory-only user token. */
(()=>{"use strict";
const owner="TLP1998", repository="OpenAI", api="https://api.github.com/repos/"+owner+"/"+repository;
const base="ChatGPT/TLP/TLP Transfer/";
let ctx=null, token="", popup=null, dialog=null, resolveAuth=null, busy=false;
const $=(s)=>document.querySelector(s);
function make(tag,content,cls){const e=document.createElement(tag);if(content!==undefined)e.textContent=content;if(cls)e.className=cls;return e}
function hide(){if(popup){popup.remove();popup=null}}
function note(msg,fail=false){let n=$("#tlp-admin-note");if(!n){n=make("div",undefined,"tlp-admin-note");n.id="tlp-admin-note";document.body.append(n)}n.textContent=msg;n.style.background=fail?"#872c2c":"#23567a";n.hidden=false;clearTimeout(note.timer);note.timer=setTimeout(()=>n.hidden=true,7500)}
async function req(route,method="GET",payload=null,authenticated=true){
 const heads={"Accept":"application/vnd.github+json","X-GitHub-Api-Version":"2022-11-28"};
 if(authenticated){if(!token)throw Error("请先提供 GitHub 仓库专用授权令牌");heads.Authorization="Bearer "+token}
 if(payload!==null)heads["Content-Type"]="application/json";
 let res;try{res=await fetch(route.startsWith("https://")?route:api+route,{method,headers:heads,body:payload===null?undefined:JSON.stringify(payload),cache:"no-store"})}catch(e){throw Error("无法访问 api.github.com；请检查当前网络是否允许该地址")}
 if(!res.ok){let info="";try{const j=await res.json();info=j.message||""}catch(e){}throw Error("GitHub HTTP "+res.status+(info?"："+info:""))}
 if(res.status===204)return null;return res.json()
}
function b64utf8(t){const bytes=new TextEncoder().encode(t);let binary="";for(let i=0;i<bytes.length;i+=8192)binary+=String.fromCharCode(...bytes.subarray(i,i+8192));return btoa(binary)}
function utf8b64(t){const binary=atob(t.replace(/\s/g,""));const b=Uint8Array.from(binary,c=>c.charCodeAt(0));return new TextDecoder().decode(b)}
function dialogEl(title,description){
 if(dialog)dialog.remove();
 dialog=make("div",undefined,"tlp-admin-overlay");const panel=make("section",undefined,"tlp-admin-dialog");panel.setAttribute("role","dialog");panel.setAttribute("aria-modal","true");panel.append(make("h3",title),make("p",description));
 dialog.append(panel);document.body.append(dialog);return panel
}
function closeDialog(){if(dialog){dialog.remove();dialog=null}}
function actionButton(label,callback,cls){const b=make("button",label,cls||"btn secondary");b.type="button";b.onclick=callback;return b}
async function authorize(){
 if(token)return true;
 return new Promise(resolve=>{
  resolveAuth=resolve;const p=dialogEl("GitHub 写入授权","仅在允许使用个人 GitHub 凭证的私人设备操作。请填写仅限 TLP1998/OpenAI 仓库、具有 Contents 和 Issues 写入权限的细粒度 Token。令牌只保存在当前页面内存，关闭或刷新即清除；不会上传到 GitHub Pages。公司沙箱请勿输入个人 Token。");
  const input=make("input");input.type="password";input.autocomplete="off";input.placeholder="github_pat_…";input.setAttribute("aria-label","GitHub Token");input.style.width="100%";
  const buttons=make("div",undefined,"buttons");buttons.append(actionButton("确认本次授权",()=>{
   if(!input.value.trim()){note("请先输入有效的 Token",true);return}token=input.value.trim();resolveAuth=null;closeDialog();resolve(true);
  },"btn"),actionButton("取消",()=>{resolveAuth=null;closeDialog();resolve(false)}));p.append(input,buttons);
  input.focus();
 })
}
async function manualConfirm(title,desc){
 return new Promise(resolve=>{const p=dialogEl(title,desc),buttons=make("div",undefined,"buttons");
 buttons.append(actionButton("确认删除",()=>{closeDialog();resolve(true)},"btn danger"),actionButton("取消",()=>{closeDialog();resolve(false)}));p.append(buttons)})
}
async function deleteReply(obj){
 if(busy)return;
 if(!await manualConfirm("删除指定回复","仅删除评论 ID："+obj.commentId+"（所属 Issue #"+obj.issue+"），不会删除原始消息和其他回复。删除后 GitHub 不提供常规撤销。"))return;
 if(!await authorize())return;
 busy=true;try{
 await req("/issues/comments/"+obj.commentId,"DELETE");
 obj.node.remove();note("回复已从 GitHub 删除。刷新后其他设备也将同步。")
 }catch(e){note("删除回复失败："+e.message,true)}finally{busy=false}
}
const indexFile=base+"index.html";
async function getContent(path){return req("/contents/"+path.split("/").map(encodeURIComponent).join("/"))}
async function removeFile(path){let f;try{f=await getContent(path)}catch(e){if(/HTTP 404/.test(e.message))return;throw e}
 await req("/contents/"+path.split("/").map(encodeURIComponent).join("/"),"DELETE",{message:"TLP Transfer: remove "+path,sha:f.sha,branch:"main"})}
function removeRecord(html,id){
 const start=html.indexOf("const messages=[");const end=html.indexOf("];",start);
 if(start<0||end<0)throw Error("无法定位中转站消息索引");
 const body=html.slice(start+"const messages=[".length,end);
 const marker='id:"'+id+'"';const position=body.indexOf(marker);
 if(position<0)throw Error("索引中不存在该消息，请刷新页面核对");
 const first=body.lastIndexOf("{",position);
 const issuePos=body.indexOf("issueNumber:",position);
 if(first<0||issuePos<0)throw Error("消息格式不符合预期");
 const last=body.indexOf("}",issuePos);
 if(last<0)throw Error("消息记录不完整");
 const trailing=body[last+1]===","?1:0;
 const next=body.slice(0,first)+body.slice(last+1+trailing);
 return html.slice(0,start+"const messages=[".length)+next+html.slice(end)
}
async function deleteMessage(obj){
 if(busy)return;
 const m=obj.message;
 if(!await manualConfirm("删除中转站消息 "+m.id,"将从收件箱索引删除该消息，并删除专属 HTML 正文；如果 Issue 仍存在，将把它关闭（非永久删除）。附件默认保留以免误删共享文件。操作涉及多次 GitHub 提交，可能部分成功，请查看结果提示。"))return;
 if(!await authorize())return;
 busy=true;let stages=[];
 try{
   // Close first: prevents active discussion while index and document changes take effect.
   try{await req("/issues/"+m.issueNumber,"PATCH",{state:"closed",state_reason:"not_planned"});stages.push("Issue 已关闭")}catch(e){if(/HTTP 404/.test(e.message))stages.push("Issue 已不存在");else throw e}
   const file=await getContent(indexFile);const old=utf8b64(file.content);const changed=removeRecord(old,m.id);
   await req("/contents/"+indexFile.split("/").map(encodeURIComponent).join("/"),"PUT",{message:"TLP Transfer: remove message "+m.id+" from inbox",content:b64utf8(changed),sha:file.sha,branch:"main"});stages.push("收件箱索引已移除");
   if(m.page){await removeFile(base+m.page);stages.push("HTML 已删除")}
   obj.data.splice(obj.data.findIndex(x=>x.id===m.id),1);obj.render();hide();note(m.id+" 清理完成："+stages.join("、")+"。附件未删除，GitHub Pages 更新可能延迟。");
 }catch(e){note(m.id+" 部分执行结果："+stages.join("、")+"；异常："+e.message,true)}
 finally{busy=false}
}
function showMenu(e,obj){
 hide();ctx=obj;popup=make("div",undefined,"tlp-admin-context");popup.setAttribute("role","menu");
 const title=make("div",obj.kind==="reply"?"回复操作":"消息操作","tlp-admin-context-title");popup.append(title);
 popup.append(actionButton(obj.kind==="reply"?"删除这条回复":"删除这条中转站消息",()=>{hide();obj.kind==="reply"?deleteReply(obj):deleteMessage(obj)},"tlp-admin-context-action"));
 popup.append(actionButton("取消",hide,"tlp-admin-context-action"));
 popup.style.left=Math.min(e.clientX||12,window.innerWidth-235)+"px";popup.style.top=Math.min(e.clientY||12,window.innerHeight-125)+"px";
 document.body.append(popup);
}
function attachContext(node,obj){
 if(node.dataset.tlpAdminBound)return;node.dataset.tlpAdminBound="1";
 node.addEventListener("contextmenu",e=>{e.preventDefault();showMenu(e,obj)});
 let timeout=0,start=null;
 node.addEventListener("touchstart",e=>{if(e.touches.length!==1)return;const t=e.touches[0];start={x:t.clientX,y:t.clientY};timeout=setTimeout(()=>{showMenu({clientX:start.x,clientY:start.y},obj);timeout=0},650)},{passive:true});
 node.addEventListener("touchmove",e=>{if(!start||!e.touches.length)return;const t=e.touches[0];if(Math.abs(t.clientX-start.x)>12||Math.abs(t.clientY-start.y)>12){clearTimeout(timeout);timeout=0}},{passive:true});
 for(const ev of ["touchend","touchcancel"])node.addEventListener(ev,()=>{clearTimeout(timeout);timeout=0},{passive:true});
}
function start(opts){
 const {messages,showList}=opts;
 const list=$("#list"),detail=$("#detail");
 const observer=new MutationObserver(()=>{
  list.querySelectorAll("[data-tlp-message]").forEach(n=>{
   const message=messages.find(m=>m.id===n.dataset.tlpMessage);
   if(message)attachContext(n,{kind:"message",message,data:messages,render:showList});
  });
  detail.querySelectorAll("[data-tlp-comment]").forEach(n=>{
   if(n.dataset.tlpAdminBound)return;
   const issue=Number(n.dataset.tlpIssue),commentId=Number(n.dataset.tlpComment);
   if(commentId)attachContext(n,{kind:"reply",issue,commentId,node:n});
  });
 });
 observer.observe(list,{childList:true,subtree:true});observer.observe(detail,{childList:true,subtree:true});
 list.querySelectorAll("[data-tlp-message]").forEach(n=>{const m=messages.find(x=>x.id===n.dataset.tlpMessage);if(m)attachContext(n,{kind:"message",message:m,data:messages,render:showList})});
 document.addEventListener("click",e=>{if(popup&&!popup.contains(e.target))hide()});
 document.addEventListener("keydown",e=>{if(e.key==="Escape")hide()});
 // A deleted Issue returns 404. Never hide on network failure or an authorization/rate-limit error.
 const check=async()=>{
   const removed=[];
   for(const m of [...messages]){
    try{const r=await fetch(api+"/issues/"+m.issueNumber,{headers:{Accept:"application/vnd.github+json"},cache:"no-store"});if(r.status===404)removed.push(m.id)}
    catch(e){}
   }
   if(removed.length){
    for(const id of removed){const i=messages.findIndex(m=>m.id===id);if(i!==-1)messages.splice(i,1)}
    showList();note(removed.length+" 条已删除 GitHub Issue 的消息已从当前列表隐藏；原始 HTML 文件仍需单独清理。")
   }
 };
 check();
}
window.TLPAdmin={start};
})();
