// Public, stateless TLP Transfer network connectivity test.
// No storage, no GitHub access, no credentials.
const headers = {
  "content-type": "application/json; charset=utf-8",
  "cache-control": "no-store",
  "access-control-allow-origin": "*",
  "access-control-allow-methods": "GET, HEAD, POST, OPTIONS",
  "access-control-allow-headers": "content-type",
};
function response(data, status = 200) {
  return new Response(JSON.stringify(data), { status, headers });
}
export default {
  async fetch(request) {
    if (request.method === "OPTIONS") return new Response(null, {status:204,headers});
    const {pathname} = new URL(request.url);
    if (request.method === "GET" || request.method === "HEAD") {
      const body = pathname === "/" || pathname === "/test"
        ? {ok:true,service:"TLP Transfer connectivity test",method:"GET",stored:false}
        : {ok:false,error:"Not found"};
      return request.method === "HEAD" ? new Response(null,{status:body.ok?200:404,headers}) : response(body,body.ok?200:404);
    }
    if (request.method === "POST" && pathname === "/test") {
      const type=request.headers.get("content-type")||"";
      if (!type.includes("application/json")) return response({ok:false,error:"application/json required"},415);
      const raw=await request.text();
      if (raw.length > 512) return response({ok:false,error:"Payload too large"},413);
      let data;
      try { data=JSON.parse(raw); } catch { return response({ok:false,error:"Invalid JSON"},400); }
      if (data?.test !== "TLP Transfer 网络连通性测试") return response({ok:false,error:"Fixed test phrase required"},400);
      return response({ok:true,method:"POST",received_test:true,stored:false});
    }
    return response({ok:false,error:"Method not allowed"},405);
  },
};
