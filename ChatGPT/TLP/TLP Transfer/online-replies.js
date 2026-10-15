/* TLP Transfer Cloud Replies (Supabase Free). No passwords or service-role secrets in this file. */
(function(){
  "use strict";
  const config=window.TLP_TRANSFER_CONFIG||{};
  const STORE="tlp_transfer_auth_v1";
  let session=null, panel=null, messageId=null, pollTimer=null, fetchSeq=0;

  function el(tag,text,cls){
    const n=document.createElement(tag);
    if(text!==undefined && text!==null)n.textContent=text;
    if(cls)n.className=cls;
    return n;
  }
  function validSetup(){
    try {
      const url=new URL(config.supabaseUrl||"");
      return url.protocol==="https:" &&
        !!config.supabasePublishableKey && !!config.accountEmail &&
        /^\S+@\S+\.\S+$/.test(config.accountEmail);
    } catch(e){return false;}
  }
  function endpoint(path){
    return String(config.supabaseUrl).replace(/\/$/,"")+path;
  }
  function saveAuth(obj){
    session=obj;
    try{sessionStorage.setItem(STORE,JSON.stringify(obj));}catch(e){}
  }
  function clearAuth(){
    session=null;
    try{sessionStorage.removeItem(STORE);}catch(e){}
  }
  function restoreAuth(){
    try{const s=JSON.parse(sessionStorage.getItem(STORE)||"null");
      if(s && s.access_token && s.refresh_token)session=s;
    }catch(e){clearAuth();}
  }
  restoreAuth();

  function errorMessage(response,data){
    const reason=(data&&(data.msg||data.message||data.error_description||data.error))||"服务暂时不可用";
    return String(reason).slice(0,180)+"（HTTP "+response.status+"）";
  }
  async function authCall(grant,body){
    const response=await fetch(endpoint("/auth/v1/token?grant_type="+grant),{
      method:"POST",headers:{"apikey":config.supabasePublishableKey,"Content-Type":"application/json"},
      body:JSON.stringify(body)
    });
    let result={};
    try{result=await response.json();}catch(e){}
    if(!response.ok||!result.access_token)throw new Error(errorMessage(response,result));
    saveAuth({access_token:result.access_token,refresh_token:result.refresh_token,
      expires_at:Math.floor(Date.now()/1000)+Number(result.expires_in||3600)});
  }
  async function ensureAuth(){
    if(!session)throw new Error("请先登录");
    if(session.expires_at>Date.now()/1000+60)return;
    try{await authCall("refresh_token",{refresh_token:session.refresh_token});}
    catch(e){clearAuth();throw new Error("登录已过期，请重新输入密码");}
  }
  async function api(path,options){
    await ensureAuth();
    const headers={"apikey":config.supabasePublishableKey,
      "Authorization":"Bearer "+session.access_token,
      "Accept":"application/json","Content-Type":"application/json"};
    const response=await fetch(endpoint("/rest/v1/"+path),
      Object.assign({},options||{},{headers:Object.assign(headers,(options||{}).headers||{})}));
    let data=null;
    try{data=await response.json();}catch(e){}
    if(!response.ok){
      if(response.status===401){clearAuth();throw new Error("登录已过期，请重新登录");}
      throw new Error(errorMessage(response,data));
    }
    return data;
  }
  function statusText(node,text,isError){
    node.textContent=text;
    node.className=isError?"online-feedback error":"online-feedback";
  }
  function button(label,cls){
    const b=el("button",label,cls||"button");b.type="button";return b;
  }
  function renderLogin(root){
    const form=el("form");
    const info=el("p","登录后可在网页上直接发布非敏感文字，并在其他已登录设备查看。","hint");
    const line=el("div",undefined,"online-login-row");
    const pass=el("input");pass.type="password";pass.required=true;pass.minLength=1;
    pass.autocomplete="current-password";pass.placeholder="输入中转站密码";
    pass.setAttribute("aria-label","中转站密码");
    const submit=button("登录中转站");
    submit.type="submit";
    const feedback=el("div",undefined,"online-feedback");
    line.append(pass,submit);form.append(info,line,feedback);
    form.addEventListener("submit",async function(e){
      e.preventDefault();
      const password=pass.value;
      submit.disabled=true;
      statusText(feedback,"正在验证密码…");
      try{
        await authCall("password",{email:config.accountEmail,password});
        pass.value="";
        if(panel===root)renderPanel(root,messageId);
      }catch(err){statusText(feedback,"登录失败："+err.message,true);}
      finally{submit.disabled=false;}
    });
    root.append(form);
  }
  async function loadReplies(root,id,feedback,items){
    if(!session||!root.isConnected)return;
    const seq=++fetchSeq;
    statusText(feedback,"正在刷新云端回复…");
    try{
      const params=new URLSearchParams({
        select:"id,body,created_at",
        message_id:"eq."+id,
        order:"created_at.asc",limit:"200"
      });
      const rows=await api("tlp_transfer_replies?"+params.toString(),{method:"GET"});
      if(seq!==fetchSeq||panel!==root||messageId!==id)return;
      items.replaceChildren();
      if(!Array.isArray(rows)||rows.length===0)items.append(el("p","暂无在线回复。","hint"));
      else for(const record of rows){
        const div=el("div",undefined,"online-entry");
        const timestamp=record.created_at ? new Date(record.created_at).toLocaleString("zh-CN") : "时间未知";
        div.append(el("div",timestamp,"hint"),el("p",record.body||""));
        items.append(div);
      }
      statusText(feedback,"已同步 "+(Array.isArray(rows)?rows.length:0)+" 条在线回复 · "+new Date().toLocaleTimeString("zh-CN"));
    }catch(e){
      if(seq!==fetchSeq||panel!==root)return;
      statusText(feedback,"刷新失败："+e.message,true);
      if(!session)renderPanel(root,id);
    }
  }
  function renderCompose(root,id){
    const toolbar=el("div",undefined,"online-toolbar");
    const out=button("退出登录","button secondary");
    out.addEventListener("click",function(){clearAuth();if(panel===root)renderPanel(root,id);});
    toolbar.append(el("span","已登录 · 允许发布非敏感文字","hint"),out);
    const feedback=el("div",undefined,"online-feedback");
    const list=el("div",undefined,"online-history");
    const refresh=button("刷新在线回复","button secondary");
    const label=el("h4","回复到 "+id);
    const ta=el("textarea");ta.maxLength=1000;
    ta.placeholder="输入允许对外提交的非敏感文字（最多 1000 字）";
    ta.setAttribute("aria-label","在线回复内容");
    const send=button("确认发送到云端");
    const sendStatus=el("div",undefined,"online-feedback");
    refresh.addEventListener("click",()=>loadReplies(root,id,feedback,list));
    send.addEventListener("click",async function(){
      const body=ta.value.trim();
      if(!body){statusText(sendStatus,"请先输入回复内容",true);return;}
      if(body.length>1000){statusText(sendStatus,"回复不能超过 1000 字",true);return;}
      send.disabled=true;statusText(sendStatus,"正在提交，尚未保存…");
      try{
        const result=await api("tlp_transfer_replies",{
          method:"POST",headers:{"Prefer":"return=representation"},
          body:JSON.stringify({message_id:id,body})
        });
        if(!Array.isArray(result)||result.length!==1||!result[0].id)throw new Error("服务器未确认保存，请检查记录后再重试");
        ta.value="";
        statusText(sendStatus,"发布成功！已保存到云端，其他设备登录后即可查看。");
        await loadReplies(root,id,feedback,list);
      }catch(e){
        statusText(sendStatus,"发布失败，文字仍保留："+e.message,true);
        if(!session)renderPanel(root,id);
      }finally{send.disabled=false;}
    });
    const note=el("p","在线回复保存在云端，不需要再复制口令。不会自动通知或触发 GPT。","hint");
    root.append(toolbar,refresh,feedback,list,label,ta,send,sendStatus,note);
    loadReplies(root,id,feedback,list);
  }
  function renderPanel(root,id){
    if(!root||!root.isConnected)return;
    root.replaceChildren();
    root.append(el("h3","在线回复 · 跨设备同步"));
    if(!validSetup()){
      const msg=el("div","在线回复功能尚未激活：需要先配置免费的 Supabase 云端服务。原有消息、附件和手动回复仍可正常使用。","online-notice");
      const guide=el("a","查看免费启用说明 →");
      guide.href="https://github.com/TLP1998/OpenAI/blob/main/ChatGPT/TLP/TLP%20Transfer/ONLINE-SETUP.md";
      guide.target="_blank";guide.rel="noopener noreferrer";
      root.append(msg,guide);
    } else if(!session)renderLogin(root);
    else renderCompose(root,id);
  }
  function attach(detail,message){
    const old=detail.querySelector(".online-panel");if(old)old.remove();
    if(pollTimer)clearInterval(pollTimer);
    messageId=message.id;
    const wrap=el("section",undefined,"online-panel");
    const manual=detail.querySelector(".reply");
    if(manual)detail.insertBefore(wrap,manual);else detail.append(wrap);
    panel=wrap;
    renderPanel(wrap,message.id);
    pollTimer=setInterval(function(){
      if(!panel||!panel.isConnected||!session)return;
      const entries=panel.querySelector(".online-history");
      const fb=panel.querySelector(".online-feedback");
      if(entries&&fb)loadReplies(panel,messageId,fb,entries);
    },45000);
  }
  window.TLPOnline={attach};
})();
